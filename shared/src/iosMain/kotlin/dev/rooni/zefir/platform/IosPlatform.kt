package dev.rooni.zefir.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelOptions
import dev.rooni.zefir.model.TunnelState
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIPasteboard

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun createPlatform(): Platform = IosPlatform()

/**
 * iOS-платформа.
 *
 * Туннель обслуживает NEPacketTunnelProvider-расширение, которому нужен
 * entitlement com.apple.developer.network-extension. В unsigned .ipa его нет,
 * поэтому [TunnelBridge.handler] остаётся null, и приложение честно сообщает,
 * что VPN недоступен, вместо немой кнопки.
 */
class IosPlatform : Platform {
    override val name: String = "ios"
    override val vpnAvailable: Boolean get() = TunnelBridge.handler != null

    private val defaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var tunnel: TunnelState = TunnelState.Off

    override fun loadSettings(): AppSettings? = runCatching {
        defaults.stringForKey(KEY_SETTINGS)?.let { json.decodeFromString<AppSettings>(it) }
    }.getOrNull()

    override fun saveSettings(settings: AppSettings) {
        defaults.setObject(json.encodeToString(settings), KEY_SETTINGS)
    }

    override fun loadProfiles(): List<Profile> = runCatching {
        defaults.stringForKey(KEY_PROFILES)?.let { json.decodeFromString<List<Profile>>(it) }
    }.getOrNull() ?: emptyList()

    override fun saveProfiles(profiles: List<Profile>) {
        defaults.setObject(json.encodeToString(profiles), KEY_PROFILES)
    }

    override fun clipboardText(): String? = UIPasteboard.generalPasteboard.string

    override fun setClipboardText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun haptic() {
        runCatching {
            UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
        }
    }

    override fun openUrl(url: String) {
        // Внешние ссылки открываются из Swift-обёртки, если понадобятся.
    }

    override fun deviceModel(): String = UIDevice.currentDevice.model
    override fun deviceOs(): String = UIDevice.currentDevice.systemVersion
    override fun hwid(): String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "ios"

    override fun tunnelState(): TunnelState = tunnel

    override fun traffic(): Traffic = TunnelBridge.handler?.traffic() ?: Traffic()

    override suspend fun startTunnel(options: TunnelOptions, nodes: List<String>): Result<Unit> {
        val bridge = TunnelBridge.handler
            ?: return Result.failure(IllegalStateException(TunnelBridge.NOT_CONFIGURED))
        tunnel = TunnelState.Starting
        val result = bridge.start(nodes.joinToString("\n"))
        tunnel = if (result.isSuccess) TunnelState.On else TunnelState.Failed(result.exceptionOrNull()?.message.orEmpty())
        return result
    }

    override suspend fun stopTunnel(): Result<Unit> {
        val bridge = TunnelBridge.handler
            ?: return Result.failure(IllegalStateException(TunnelBridge.NOT_CONFIGURED))
        tunnel = TunnelState.Stopping
        val result = bridge.stop()
        tunnel = TunnelState.Off
        return result
    }

    override suspend fun pingTcp(host: String, port: Int): Int? {
        val target = when {
            host.contains("://") -> host
            port == 443 || port == 0 -> "https://$host"
            else -> "https://$host:$port"
        }
        val started = nowMillis()
        val result = withTimeoutOrNull(3500) {
            runCatching { httpClient.get(target) }.isSuccess
        } ?: false
        return if (result) (nowMillis() - started).toInt() else null
    }

    override fun saveBackup(text: String): String? = null

    override fun readBackup(path: String): String? = null

    @Composable
    override fun systemColorScheme(dark: Boolean): ColorScheme? = null

    companion object {
        private const val KEY_SETTINGS = "zefir.settings"
        private const val KEY_PROFILES = "zefir.profiles"
        private val httpClient = HttpClient(Darwin)
    }
}

/** Мост в Swift: NetworkExtension не вызывается напрямую из Kotlin/Native. */
object TunnelBridge {
    const val NOT_CONFIGURED =
        "VPN требует подписи с entitlement com.apple.developer.network-extension"

    var handler: TunnelHandler? = null
}

interface TunnelHandler {
    fun start(config: String): Result<Unit>
    fun stop(): Result<Unit>
    fun traffic(): Traffic
}
