package dev.rooni.zefir.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.Traffic
import dev.rooni.zefir.model.TunnelOptions
import dev.rooni.zefir.model.TunnelState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.File

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun createPlatform(): Platform = DesktopPlatform(
    File(System.getProperty("user.home"), ".zefir-ios"),
)

/**
 * Десктопная платформа — нужна для локальной проверки UI под Xvfb
 * и как «эталон» поведения. Туннеля здесь нет: состояние имитируется,
 * чтобы экраны можно было смотреть в обоих состояниях.
 */
class DesktopPlatform(private val dir: File) : Platform {
    override val name: String = "desktop"
    override val vpnAvailable: Boolean = false

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private var tunnel: TunnelState = TunnelState.Off
    private var startedAt = 0L
    private var fakeUp = 0L
    private var fakeDown = 0L
    private var lastTick = 0L

    init {
        dir.mkdirs()
    }

    private fun file(name: String) = File(dir, "$name.json")

    override fun loadSettings(): AppSettings? = runCatching {
        val f = file("settings")
        if (!f.exists()) null else json.decodeFromString<AppSettings>(f.readText())
    }.getOrNull()

    override fun saveSettings(settings: AppSettings) {
        runCatching { file("settings").writeText(json.encodeToString(settings)) }
    }

    override fun loadProfiles(): List<Profile> = runCatching {
        val f = file("profiles")
        if (!f.exists()) emptyList() else json.decodeFromString<List<Profile>>(f.readText())
    }.getOrDefault(emptyList())

    override fun saveProfiles(profiles: List<Profile>) {
        runCatching { file("profiles").writeText(json.encodeToString(profiles)) }
    }

    override fun clipboardText(): String? = runCatching {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
    }.getOrNull()

    override fun setClipboardText(text: String) {
        runCatching {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        }
    }

    override fun haptic() = Unit

    override fun openUrl(url: String) {
        runCatching {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        }
    }

    override fun deviceModel(): String = "Desktop"
    override fun deviceOs(): String = System.getProperty("os.name") ?: "Linux"
    override fun hwid(): String = "desktop"

    override fun tunnelState(): TunnelState = tunnel

    override fun traffic(): Traffic {
        val now = nowMillis()
        val dt = if (lastTick == 0L) 1.0 else (now - lastTick) / 1000.0
        lastTick = now
        if (tunnel !is TunnelState.On) return Traffic()
        val upSpeed = 24_000L
        val downSpeed = 480_000L
        fakeUp += (upSpeed * dt).toLong()
        fakeDown += (downSpeed * dt).toLong()
        return Traffic(up = fakeUp, down = fakeDown, upSpeed = upSpeed, downSpeed = downSpeed)
    }

    override suspend fun startTunnel(options: TunnelOptions, nodes: List<String>): Result<Unit> {
        delay(700)
        tunnel = TunnelState.On
        startedAt = nowMillis()
        lastTick = 0
        return Result.success(Unit)
    }

    override suspend fun stopTunnel(): Result<Unit> {
        delay(400)
        tunnel = TunnelState.Off
        fakeUp = 0
        fakeDown = 0
        return Result.success(Unit)
    }

    override suspend fun pingTcp(host: String, port: Int): Int? {
        val start = nowMillis()
        return runCatching {
            val address = java.net.InetSocketAddress(host.trim(), port)
            java.net.Socket().use { socket ->
                socket.connect(address, 2500)
                socket.close()
            }
            (nowMillis() - start).toInt()
        }.getOrNull()
    }

    override fun saveBackup(text: String): String? = runCatching {
        val f = File(dir, "zefir-backup.json")
        f.writeText(text)
        f.absolutePath
    }.getOrNull()

    override fun readBackup(path: String): String? = runCatching { File(path).readText() }.getOrNull()

    @Composable
    override fun systemColorScheme(dark: Boolean): ColorScheme? = null
}

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            followRedirects(true)
        }
    }
}
