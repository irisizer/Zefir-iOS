package dev.rooni.zefir

import androidx.compose.ui.window.ComposeUIViewController
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.createPlatform
import dev.rooni.zefir.ui.ZefirApp
import kotlinx.coroutines.MainScope
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        // Compose проверяет Info.plist и abort()-ит, если ключа нет.
        // Для unsigned .ipa (сторонняя подпись перезаписывает plist) это
        // единственный надёжный способ не падать на старте.
        enforceStrictPlistSanityCheck = false
    },
) {
    val controller = AppController(platform = createPlatform(), scope = MainScope())
    ZefirApp(controller)
}
