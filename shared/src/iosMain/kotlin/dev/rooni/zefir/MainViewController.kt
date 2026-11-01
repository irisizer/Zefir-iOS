package dev.rooni.zefir

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.IosPlatform
import kotlinx.coroutines.MainScope
import platform.UIKit.UIViewController

/**
 * Точка входа для iOS: Swift-обёртка (iosApp/iOSApp.swift) поднимает этот контроллер.
 * Ровно тот же Compose-интерфейс, что на Android/desktop.
 *
 * `enforceStrictPlistSanityCheck = false` — Compose на iOS в фоне проверяет
 * `CADisableMinimumFrameDurationOnPhone` в Info.plist и при несовпадении зовёт abort()
 * (не ловится: проверка идёт в dispatch-очереди). Ключ в Info.plist у нас есть, но
 * сайдлоад-подписанты перезаписывают plist при установке, поэтому проверку отключаем —
 * на рендер она не влияет, только на 120 Гц.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    },
) {
    val platform = IosPlatform()
    val controller = AppController(platform, MainScope())
    ZefirApp(controller)
}