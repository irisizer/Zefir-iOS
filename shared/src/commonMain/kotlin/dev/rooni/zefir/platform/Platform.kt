package dev.rooni.zefir.platform

import androidx.compose.runtime.Composable
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.TunnelState

/**
 * Платформенный слой. В оригинале (platform/Platform.kt) — expect/actual:
 * Android (VpnService + libgojni), desktop (mihomo.exe), iOS (NetworkExtension).
 */
interface Platform {
    val name: String

    fun loadSettings(): AppSettings?
    fun saveSettings(settings: AppSettings)

    fun loadProfiles(): List<Profile>
    fun saveProfiles(profiles: List<Profile>)

    fun clipboardText(): String?
    fun setClipboardText(text: String)

    fun haptic() {}

    /** Запуск/остановка туннеля. */
    suspend fun startTunnel(settings: AppSettings, nodes: List<String>): Result<Unit>
    suspend fun stopTunnel(): Result<Unit>

    fun tunnelState(): TunnelState

    /** Цвета системы (dynamic color). На iOS недоступно — null. */
    @Composable
    fun systemColorScheme(dark: Boolean): androidx.compose.material3.ColorScheme? = null

    fun platformName(): String
}