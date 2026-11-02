package dev.rooni.zefir.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelOptions
import dev.rooni.zefir.model.TunnelState
import dev.rooni.zefir.platform.Platform
import dev.rooni.zefir.platform.nowMillis
import dev.rooni.zefir.res.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Состояние приложения и все действия (порт app/AppController.kt).
 * Держит настройки, подписки, состояние туннеля, пинги и сообщения для снекбара.
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

    var traffic by mutableStateOf(Traffic())
        private set

    var busy by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
    var tab by mutableStateOf(Tab.Home)

    /** Задержки серверов: ключ — имя сервера. */
    val pings = mutableStateMapOf<String, Int?>()
    var testing by mutableStateOf(false)
        private set

    /** Время запуска туннеля (для виджета сеанса). */
    var sessionStart by mutableStateOf<Long?>(null)
        private set

    private var trafficJob: Job? = null

    // ---------- строки ----------

    val language: String get() = settings.language

    fun s(key: String): String =
        Strings.extra[key]?.get(language)
            ?: Strings.all[language]?.get(key)
            ?: Strings.extra[key]?.get("ru")
            ?: Strings.ru[key]
            ?: key

    fun sf(key: String, vararg args: Any?): String = Strings.format(s(key), *args)

    // ---------- подписки ----------

    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.id == settings.activeProfile } ?: profiles.firstOrNull()

    val nodes: List<ProxyNode>
        get() = activeProfile?.nodes.orEmpty()

    val activeNode: ProxyNode?
        get() = nodes.firstOrNull { it.name == settings.activeServer }
            ?: if (settings.autoSelect) nodes.firstOrNull() else nodes.firstOrNull()

    val isOn: Boolean get() = tunnel is TunnelState.On

    fun nodeByName(name: String?): ProxyNode? = nodes.firstOrNull { it.name == name }

    // ---------- настройки ----------

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(settings)
        settings = next
        platform.saveSettings(next)
    }

    fun setLanguage(code: String) = update { it.copy(language = code) }

    // ---------- туннель ----------

    fun toggle() {
        when (tunnel) {
            is TunnelState.On, is TunnelState.Starting -> stop()
            else -> start()
        }
    }

    fun start() {
        if (nodes.isEmpty()) {
            message = s("msg_no_profile")
            return
        }
        if (!settings.onboarded) update { it.copy(onboarded = true) }
        scope.launch {
            tunnel = TunnelState.Starting
            val options = TunnelOptions(
                mode = settings.mode,
                ipv6 = settings.ipv6,
                mtu = settings.mtu,
                splitMode = settings.splitMode,
                only = settings.onlyApps,
                excluded = settings.excludedApps,
            )
            val result = platform.startTunnel(options, nodes.map { it.link.ifBlank { it.name } })
            tunnel = if (result.isSuccess) {
                sessionStart = nowMillis()
                startTrafficLoop()
                TunnelState.On
            } else {
                TunnelState.Failed(result.exceptionOrNull()?.message ?: s("err_generic"))
            }
        }
    }

    fun stop() {
        scope.launch {
            val previous = tunnel
            tunnel = TunnelState.Stopping
            val result = platform.stopTunnel()
            trafficJob?.cancel()
            sessionStart = null
            traffic = Traffic()
            tunnel = if (result.isSuccess) TunnelState.Off else previous
        }
    }

    fun selectNode(node: ProxyNode) {
        update { it.copy(activeServer = node.name, autoSelect = false) }
        if (isOn) {
            stop()
            scope.launch {
                delay(400)
                start()
            }
        }
    }

    fun selectProfile(profile: Profile) {
        update { it.copy(activeProfile = profile.id, activeServer = null) }
        if (isOn) {
            stop()
            scope.launch { delay(400); start() }
        }
    }

    private fun startTrafficLoop() {
        trafficJob?.cancel()
        trafficJob = scope.launch {
            while (true) {
                traffic = platform.traffic()
                delay(1000)
            }
        }
    }

    // ---------- импорт ----------

    fun addFromText(raw: String, done: (Boolean) -> Unit = {}) {
        val text = raw.trim()
        if (text.isEmpty()) {
            message = s("add_err_empty")
            done(false)
            return
        }
        scope.launch {
            busy = true
            try {
                val profile = importProfile(text)
                applyImport(profile)
                done(true)
            } catch (e: ImportException) {
                message = messageFor(e.error)
                done(false)
            } catch (e: Throwable) {
                message = s("add_err_unsupported")
                done(false)
            } finally {
                busy = false
            }
        }
    }

    fun addFromClipboard(done: (Boolean) -> Unit = {}) {
        val text = platform.clipboardText()
        if (text.isNullOrBlank()) {
            message = s("add_clipboard_empty")
            done(false)
            return
        }
        addFromText(text, done)
    }

    fun refresh(profile: Profile? = null, silent: Boolean = false) {
        val target = profile ?: activeProfile ?: return
        if (target.url.isBlank()) return
        scope.launch {
            busy = true
            try {
                val fetched = SubscriptionClient.fetch(target.url, target.client, target.customClient)
                val parsed = withContext(Dispatchers.Default) { Links.parseMany(fetched.body) }
                if (parsed.isEmpty()) throw ImportException(ImportError.NoServers)
                val updated = target.copy(
                    nodes = parsed,
                    servers = parsed.size,
                    updatedAt = nowMillis(),
                    used = fetched.info.used,
                    total = fetched.info.total,
                    expire = fetched.info.expire,
                    webUrl = fetched.info.webUrl,
                    name = target.name.ifBlank { s("servers_default_name") },
                )
                profiles = profiles.map { if (it.id == target.id) updated else it }
                platform.saveProfiles(profiles)
                if (!silent) message = s("servers_updated")
            } catch (e: Throwable) {
                if (!silent) {
                    message = if (e is ImportException) messageFor(e.error) else s("servers_update_failed")
                }
            } finally {
                busy = false
            }
        }
    }

    fun addProfile(profile: Profile) {
        profiles = profiles + profile
        platform.saveProfiles(profiles)
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

    fun togglePin(profile: Profile, node: ProxyNode) {
        val pinned = if (node.key in profile.pinned) profile.pinned - node.key else profile.pinned + node.key
        profiles = profiles.map { if (it.id == profile.id) it.copy(pinned = pinned) else it }
        platform.saveProfiles(profiles)
    }

    fun copyLink(node: ProxyNode) {
        platform.setClipboardText(node.link.ifBlank { node.name })
        platform.haptic()
        message = s("server_copy_link")
    }

    private suspend fun importProfile(text: String): Profile {
        val nodes: List<ProxyNode>
        var url = ""
        var info = dev.rooni.zefir.model.SubscriptionInfo()
        if (Links.looksLikeHttp(text)) {
            url = text
            val fetched = SubscriptionClient.fetch(text)
            info = fetched.info
            nodes = withContext(Dispatchers.Default) { Links.parseMany(fetched.body) }
        } else {
            nodes = withContext(Dispatchers.Default) { Links.parseMany(text) }
        }
        if (nodes.isEmpty()) throw ImportException(ImportError.Unsupported)
        return Profile(
            id = newId(),
            name = s("servers_default_name"),
            url = url,
            updatedAt = nowMillis(),
            servers = nodes.size,
            nodes = nodes,
            used = info.used,
            total = info.total,
            expire = info.expire,
            webUrl = info.webUrl,
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

    fun messageFor(error: ImportError): String = when (error) {
        ImportError.Empty -> s("add_err_empty")
        ImportError.Network -> s("add_err_network")
        ImportError.Unsupported -> s("add_err_unsupported")
        ImportError.NoServers -> s("add_err_no_servers")
        ImportError.Blocked -> s("add_err_blocked")
    }

    // ---------- пинг ----------

    fun pingAll() {
        val list = nodes
        if (list.isEmpty() || testing) return
        testing = true
        scope.launch {
            list.forEach { node ->
                val ms = platform.pingTcp(node.server.ifBlank { node.name }, node.port.takeIf { it > 0 } ?: 443)
                pings[node.name] = ms
            }
            testing = false
        }
    }

    fun ping(node: ProxyNode) {
        scope.launch {
            pings[node.name] = null
            val ms = platform.pingTcp(node.server.ifBlank { node.name }, node.port.takeIf { it > 0 } ?: 443)
            pings[node.name] = ms
        }
    }

    // ---------- прочее ----------

    fun resetSettings() {
        val keep = profiles
        update { AppSettings(language = settings.language) }
        profiles = keep
        platform.saveProfiles(profiles)
        message = s("applied")
    }

    fun newId(): String = "p${nowMillis()}${(1000..9999).random()}"
}
