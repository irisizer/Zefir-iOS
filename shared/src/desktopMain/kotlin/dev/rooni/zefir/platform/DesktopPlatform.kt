package dev.rooni.zefir.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelState
import kotlinx.serialization.json.Json
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * Desktop-реализация Platform — нужна только для локальной отладки интерфейса
 * на Linux/Windows/macOS. Туннель здесь не поднимается (ядро mihomo живёт в iOS-расширении).
 */
class DesktopPlatform(private val home: File = File(System.getProperty("user.home"), ".zefir")) : Platform {
    override val name: String = "desktop"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private var tunnel: TunnelState = TunnelState.Off

    init {
        home.mkdirs()
    }

    override fun loadSettings(): AppSettings? = runCatching {
        val f = File(home, "settings.json")
        if (!f.exists()) null else json.decodeFromString<AppSettings>(f.readText())
    }.getOrNull()

    override fun saveSettings(settings: AppSettings) {
        runCatching { File(home, "settings.json").writeText(json.encodeToString(settings)) }
    }

    override fun loadProfiles(): List<Profile> = runCatching {
        val f = File(home, "profiles.json")
        if (!f.exists()) emptyList() else json.decodeFromString<List<Profile>>(f.readText())
    }.getOrDefault(emptyList())

    override fun saveProfiles(profiles: List<Profile>) {
        runCatching { File(home, "profiles.json").writeText(json.encodeToString(profiles)) }
    }

    override fun clipboardText(): String? = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()

    override fun setClipboardText(text: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
    }

    override suspend fun startTunnel(settings: AppSettings, nodes: List<String>): Result<Unit> {
        if (nodes.isEmpty()) return Result.failure(IllegalStateException("Нет серверов"))
        tunnel = TunnelState.On(System.currentTimeMillis(), Traffic())
        return Result.success(Unit)
    }

    override suspend fun stopTunnel(): Result<Unit> {
        tunnel = TunnelState.Off
        return Result.success(Unit)
    }

    override fun tunnelState(): TunnelState = tunnel

    @Composable
    override fun systemColorScheme(dark: Boolean): ColorScheme? = null

    override fun platformName(): String = "Desktop"
}