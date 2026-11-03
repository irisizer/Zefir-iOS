package dev.rooni.zefir.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelOptions
import dev.rooni.zefir.model.TunnelState

/**
 * Всё, что различается между iOS и десктопом.
 * Общий Compose-код ничего не знает о платформе, кроме этого интерфейса.
 */
interface Platform {
    val name: String

    /** Есть ли в системе VPN-туннель вообще (iOS без подписи — нет). */
    val vpnAvailable: Boolean

    fun loadSettings(): AppSettings?
    fun saveSettings(settings: AppSettings)

    fun loadProfiles(): List<Profile>
    fun saveProfiles(profiles: List<Profile>)

    fun clipboardText(): String?
    fun setClipboardText(text: String)

    fun haptic()
    fun openUrl(url: String)

    fun deviceModel(): String
    fun deviceOs(): String
    fun hwid(): String

    fun tunnelState(): TunnelState
    fun traffic(): Traffic
    suspend fun startTunnel(options: TunnelOptions, configJson: String): Result<Unit>
    suspend fun stopTunnel(): Result<Unit>

    /** Задержка до сервера: TCP-коннект или настоящий запрос через туннель. */
    suspend fun pingTcp(host: String, port: Int): Int?

    fun saveBackup(text: String): String?
    fun readBackup(path: String): String?

    @Composable
    fun systemColorScheme(dark: Boolean): ColorScheme?

    fun localizationOverride(): String? = null
}

expect fun createPlatform(): Platform

expect fun nowMillis(): Long
