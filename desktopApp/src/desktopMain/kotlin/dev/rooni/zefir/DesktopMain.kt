package dev.rooni.zefir

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.DesktopPlatform
import kotlinx.coroutines.MainScope

fun main() = application {
    val platform = DesktopPlatform(java.io.File(System.getProperty("user.home"), ".zefir-ios"))
    val controller = AppController(platform, MainScope())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Zefir",
        state = rememberWindowState(size = DpSize(412.dp, 892.dp)),
    ) {
        ZefirApp(controller)
    }
}