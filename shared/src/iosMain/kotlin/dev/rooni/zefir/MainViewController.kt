package dev.rooni.zefir

import androidx.compose.ui.window.ComposeUIViewController
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.IosPlatform
import kotlinx.coroutines.MainScope
import platform.UIKit.UIViewController

/**
 * Точка входа для iOS: Swift-обёртка (iosApp/iOSApp.swift) поднимает этот контроллер.
 * Ровно тот же Compose-интерфейс, что на Android/desktop.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val platform = IosPlatform()
    val controller = AppController(platform, MainScope())
    ZefirApp(controller)
}