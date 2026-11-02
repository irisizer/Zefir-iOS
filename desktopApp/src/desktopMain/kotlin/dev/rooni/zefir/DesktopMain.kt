package dev.rooni.zefir

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.createPlatform
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.ui.ZefirApp
import kotlinx.coroutines.MainScope

/** Десктоп-запуск для проверки UI (в CI не используется). */
fun main() = application {
    val controller = AppController(createPlatform(), MainScope())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Zefir",
        state = rememberWindowState(size = DpSize(412.dp, 892.dp)),
    ) {
        val initialTab = when ((System.getenv("ZEFIR_TAB") ?: "").lowercase()) {
            "servers" -> Tab.Servers
            "settings" -> Tab.Settings
            else -> Tab.Home
        }
        ZefirApp(controller, initialTab)
    }
}
