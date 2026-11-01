package dev.rooni.zefir.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.model.TunnelState
import dev.rooni.zefir.platform.Platform
import dev.rooni.zefir.res.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Ошибки импорта — как ImportError в оригинале. */
enum class ImportError { Empty, Network, Unsupported, NoServers, Blocked }

class ImportException(val error: ImportError) : Exception(error.name)

/**
 * Состояние приложения и действия. Порт app/AppController.kt (упрощённый, но с той же логикой:
 * настройки, профили, выбор сервера, туннель, импорт подписок и ссылок).
 */
class AppController(
    val platform: Platform,
    private val scope: CoroutineScope,
) {
    var settings by mutableStateOf(platform.loadSettings() ?: AppSettings())
        private set

    var profiles by mutableStateOf(platform.loadProfiles())
        private set

    var tunnel by mutableStateOf<TunnelState>(platform.tunnelState())
        private set

    var tab by mutableStateOf(if (profiles.isEmpty()) Tab.Servers else Tab.Home)
    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
    var language: String = "ru"

    val strings: Map<String, String>
        get() = if (language == "ru") Strings.ru else Strings.en

    fun s(key: String): String = strings[key] ?: Strings.ru[key] ?: key

    fun sf(key: String, vararg args: Any?): String = Strings.format(s(key), *args)

    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.id == settings.activeProfile } ?: profiles.firstOrNull()

    val nodes: List<ProxyNode>
        get() = activeProfile?.nodes.orEmpty()

    val activeNode: ProxyNode?
        get() = nodes.firstOrNull { it.name == settings.activeServer } ?: nodes.firstOrNull()

    val isOn: Boolean get() = tunnel is TunnelState.On

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(settings)
        settings = next
        platform.saveSettings(next)
    }

    fun selectTab(t: Tab) {
        tab = t
    }

    fun selectNode(node: ProxyNode) {
        update { it.copy(activeServer = node.name) }
        if (isOn) {
            scope.launch { restartTunnel() }
        }
    }

    // ---------- импорт ----------

    fun addFromText(raw: String, done: (Boolean) -> Unit = {}) {
        val text = raw.trim()
        if (text.isEmpty()) {
            error = s("add_err_empty")
            done(false)
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val result = withContext(Dispatchers.Default) { importBlocking(text) }
                applyImport(result)
                done(true)
            } catch (e: ImportException) {
                error = messageFor(e.error)
                done(false)
            } catch (e: Throwable) {
                error = messageFor(ImportError.Unsupported)
                done(false)
            } finally {
                busy = false
            }
        }
    }

    fun refresh(profileOrNull: Profile? = null) {
        val profile = profileOrNull ?: activeProfile ?: return
        if (profile.url.isEmpty()) return
        scope.launch {
            busy = true
            try {
                val body = SubscriptionClient.fetch(profile.url, profile.client, profile.customClient)
                val parsed = withContext(Dispatchers.Default) { Links.parseMany(body) }
                if (parsed.isEmpty()) throw ImportException(ImportError.NoServers)
                val updated = profile.copy(
                    nodes = parsed,
                    servers = parsed.size,
                    updatedAt = nowMillis(),
                    name = if (profile.name.isBlank()) defaultProfileName() else profile.name,
                )
                profiles = profiles.map { if (it.id == profile.id) updated else it }
                if (settings.activeProfile == null) update { it.copy(activeProfile = updated.id) }
                platform.saveProfiles(profiles)
            } catch (e: Throwable) {
                error = messageFor(ImportError.Network)
            } finally {
                busy = false
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        profiles = profiles.filterNot { it.id == profile.id }
        platform.saveProfiles(profiles)
        if (settings.activeProfile == profile.id) {
            update { it.copy(activeProfile = profiles.firstOrNull()?.id, activeServer = null) }
        }
        if (profiles.isEmpty()) tab = Tab.Servers
    }

    fun renameProfile(profile: Profile, name: String) {
        profiles = profiles.map { if (it.id == profile.id) it.copy(name = name) else it }
        platform.saveProfiles(profiles)
    }

    private suspend fun importBlocking(text: String): Profile {
        val nodes: List<ProxyNode>
        var url = ""
        var name = ""
        if (Links.looksLikeHttp(text)) {
            url = text
            val body = SubscriptionClient.fetch(text, null, null)
            nodes = Links.parseMany(body)
            name = defaultProfileName()
        } else {
            nodes = Links.parseMany(text)
        }
        if (nodes.isEmpty()) {
            throw ImportException(if (Links.looksLikeLink(text)) ImportError.Unsupported else ImportError.Unsupported)
        }
        return Profile(
            id = newId(),
            name = name.ifEmpty { nodes.firstOrNull { it.flag != null }?.country?.let { "Мои серверы" } ?: "Мои серверы" },
            url = url,
            updatedAt = nowMillis(),
            servers = nodes.size,
            nodes = nodes,
        )
    }

    private fun applyImport(profile: Profile) {
        val existing = profiles.firstOrNull { it.url.isNotEmpty() && it.url == profile.url }
        profiles = if (existing != null) {
            profiles.map { if (it.id == existing.id) profile.copy(id = existing.id) else it }
        } else {
            profiles + profile
        }
        platform.saveProfiles(profiles)
        update {
            it.copy(
                activeProfile = existing?.id ?: profile.id,
                activeServer = null,
                onboarded = true,
            )
        }
        tab = Tab.Home
    }

    fun messageFor(e: ImportError): String = when (e) {
        ImportError.Empty -> s("add_err_empty")
        ImportError.Network -> s("add_err_network")
        ImportError.Unsupported -> s("add_err_unsupported")
        ImportError.NoServers -> s("add_err_no_servers")
        ImportError.Blocked -> s("add_err_blocked")
    }

    // ---------- туннель ----------

    fun toggleTunnel() {
        if (busy) return
        if (nodes.isEmpty()) {
            error = s("msg_no_profile")
            return
        }
        scope.launch {
            busy = true
            try {
                if (isOn) {
                    tunnel = TunnelState.Stopping
                    platform.stopTunnel()
                    tunnel = TunnelState.Off
                } else {
                    tunnel = TunnelState.Starting
                    val links = nodes.map { it.link }
                    val res = platform.startTunnel(settings, links)
                    tunnel = if (res.isSuccess) {
                        TunnelState.On(nowMillis(), dev.rooni.zefir.model.Traffic())
                    } else {
                        TunnelState.Failed(res.exceptionOrNull()?.message)
                    }
                    if (res.isFailure) {
                        error = res.exceptionOrNull()?.message ?: s("err_vpn")
                        tunnel = TunnelState.Off
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    private suspend fun restartTunnel() {
        platform.stopTunnel()
        val res = platform.startTunnel(settings, nodes.map { it.link })
        tunnel = if (res.isSuccess) TunnelState.On(tunnel.let { (it as? TunnelState.On)?.since ?: nowMillis() }, dev.rooni.zefir.model.Traffic()) else TunnelState.Off
    }

    fun pingAll() {
        scope.launch {
            val profile = activeProfile ?: return@launch
            busy = true
            try {
                val updated = profile.nodes.map { node ->
                    val ms = TcpPing.ping(node.host, node.port)
                    node.copy(latency = ms)
                }
                profiles = profiles.map { if (it.id == profile.id) it.copy(nodes = updated) else it }
                platform.saveProfiles(profiles)
            } finally {
                busy = false
            }
        }
    }

    fun copy(text: String) {
        platform.setClipboardText(text)
        platform.haptic()
    }

    private fun defaultProfileName(): String = s("servers_default_name")

    private fun newId(): String = "p" + nowMillis().toString(36) + Random.nextInt(1000, 9999)

    fun completeOnboarding() {
        update { it.copy(onboarded = true) }
    }
}