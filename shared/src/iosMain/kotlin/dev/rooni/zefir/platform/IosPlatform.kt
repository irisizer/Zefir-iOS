package dev.rooni.zefir.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.rooni.zefir.app.nowMillis
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelState
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIPasteboard

/**
 * iOS-реализация Platform.
 *
 * Туннель обслуживает NEPacketTunnelProvider-расширение (iosApp/ZefirTunnel):
 * Kotlin-слой дергает его через [TunnelBridge], который наполняется из Swift.
 */
class IosPlatform : Platform {
    override val name: String = "ios"

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
    }.getOrDefault(emptyList())

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

    override suspend fun startTunnel(settings: AppSettings, nodes: List<String>): Result<Unit> {
        val bridge = TunnelBridge.handler
            ?: return Result.failure(IllegalStateException(TunnelBridge.NOT_CONFIGURED))
        return bridge.start(nodes.joinToString("\n"))
    }

    override suspend fun stopTunnel(): Result<Unit> {
        val bridge = TunnelBridge.handler ?: return Result.failure(IllegalStateException(TunnelBridge.NOT_CONFIGURED))
        return bridge.stop()
    }

    override fun tunnelState(): TunnelState = tunnel

    @Composable
    override fun systemColorScheme(dark: Boolean): ColorScheme? = null

    override fun platformName(): String = "iOS"

    companion object {
        private const val KEY_SETTINGS = "zefir.settings"
        private const val KEY_PROFILES = "zefir.profiles"
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
}