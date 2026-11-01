package dev.rooni.zefir

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.ui.AppTheme
import dev.rooni.zefir.ui.screens.AboutScreen
import dev.rooni.zefir.ui.screens.AddProfileScreen
import dev.rooni.zefir.ui.screens.BasicSettingsScreen
import dev.rooni.zefir.ui.screens.HomeScreen
import dev.rooni.zefir.ui.screens.LookScreen
import dev.rooni.zefir.ui.screens.OnboardingScreen
import dev.rooni.zefir.ui.screens.ServersScreen
import dev.rooni.zefir.ui.screens.SettingsScreen

private sealed interface Dest {
    data object Tabs : Dest
    data object Add : Dest
    data object Basic : Dest
    data object Look : Dest
    data object About : Dest
}

@Composable
fun ZefirApp(controller: AppController) {
    AppTheme(controller.settings, controller.platform) {
        if (!controller.settings.onboarded) {
            OnboardingScreen(controller)
            return@AppTheme
        }
        var dest by remember { mutableStateOf<Dest>(Dest.Tabs) }

        AnimatedContent(
            targetState = dest,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "route",
        ) { current ->
            when (current) {
                Dest.Tabs -> TabsScaffold(
                    controller = controller,
                    onAdd = { dest = Dest.Add },
                )
                Dest.Add -> AddProfileScreen(
                    controller = controller,
                    onBack = { dest = Dest.Tabs },
                    onDone = { dest = Dest.Tabs },
                )
                Dest.Basic -> BasicSettingsScreen(controller) { dest = Dest.Tabs }
                Dest.Look -> LookScreen(controller) { dest = Dest.Tabs }
                Dest.About -> AboutScreen(controller) { dest = Dest.Tabs }
            }
        }
    }
}

@Composable
private fun TabsScaffold(controller: AppController, onAdd: () -> Unit) {
    var sub by remember { mutableStateOf<Dest?>(null) }

    val current = sub
    if (current != null) {
        when (current) {
            Dest.Basic -> BasicSettingsScreen(controller) { sub = null }
            Dest.Look -> LookScreen(controller) { sub = null }
            Dest.About -> AboutScreen(controller) { sub = null }
            Dest.Add -> AddProfileScreen(controller, onBack = { sub = null }, onDone = { sub = null })
            else -> Unit
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavItem(Tab.Home, Icons.Rounded.Home, controller)
                NavItem(Tab.Servers, Icons.Rounded.Dns, controller)
                NavItem(Tab.Settings, Icons.Rounded.Settings, controller)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            when (controller.tab) {
                Tab.Home -> HomeScreen(
                    controller = controller,
                    onAdd = { sub = Dest.Add },
                    onServers = { controller.selectTab(Tab.Servers) },
                    onSettings = { controller.selectTab(Tab.Settings) },
                )
                Tab.Servers -> ServersScreen(controller, onAdd = { sub = Dest.Add })
                Tab.Settings -> SettingsScreen(
                    controller = controller,
                    onOpenBasic = { sub = Dest.Basic },
                    onOpenLook = { sub = Dest.Look },
                    onOpenAbout = { sub = Dest.About },
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    tab: Tab,
    icon: ImageVector,
    controller: AppController,
) {
    val selected = controller.tab == tab
    NavigationBarItem(
        selected = selected,
        onClick = {
            controller.platform.haptic()
            controller.selectTab(tab)
        },
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Text(
                when (tab) {
                    Tab.Home -> controller.s("tab_home")
                    Tab.Servers -> controller.s("tab_servers")
                    Tab.Settings -> controller.s("tab_settings")
                },
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}