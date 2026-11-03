package dev.rooni.zefir.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.platform.Platform
import dev.rooni.zefir.ui.screens.AboutScreen
import dev.rooni.zefir.ui.screens.AddSubscriptionScreen
import dev.rooni.zefir.ui.screens.AdvancedScreen
import dev.rooni.zefir.ui.screens.AppsScreen
import dev.rooni.zefir.ui.screens.AutoRulesScreen
import dev.rooni.zefir.ui.screens.BasicScreen
import dev.rooni.zefir.ui.screens.ConnectionsScreen
import dev.rooni.zefir.ui.screens.DiagnosticsScreen
import dev.rooni.zefir.ui.screens.DnsScreen
import dev.rooni.zefir.ui.screens.EditProfileScreen
import dev.rooni.zefir.ui.screens.HelpScreen
import dev.rooni.zefir.ui.screens.HomeScreen
import dev.rooni.zefir.ui.screens.IdleOffScreen
import dev.rooni.zefir.ui.screens.LogsScreen
import dev.rooni.zefir.ui.screens.NotificationsScreen
import dev.rooni.zefir.ui.screens.OnboardingScreen
import dev.rooni.zefir.ui.screens.RulesScreen
import dev.rooni.zefir.ui.screens.ServersScreen
import dev.rooni.zefir.ui.screens.SettingsHubScreen
import dev.rooni.zefir.ui.screens.WidgetsScreen

@Composable
fun ZefirApp(controller: AppController, initialTab: Tab = Tab.Home) {
    AppTheme(controller.settings, controller.platform) {
        // Без корневого Surface LocalContentColor остаётся чёрным, и любой текст
        // без явного цвета превращается в чёрный на тёмном фоне.
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            MainFlow(controller, initialTab)
        }
    }
}

@Composable
fun MainFlow(controller: AppController, initialTab: Tab = Tab.Home) {
    val navigator = remember { Navigator({ controller.tab = it }, initialTab) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(controller.message) {
        val text = controller.message
        if (text != null) {
            snackbar.showSnackbar(text)
            controller.message = null
        }
    }

    if (!controller.settings.onboarded) {
        OnboardingScreen(controller) { controller.update { it.copy(onboarded = true) } }
        SnackbarHost(snackbar, Modifier.padding(top = 8.dp))
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            WideLayout(controller, navigator, snackbar)
        } else {
            CompactLayout(controller, navigator, snackbar)
        }
    }
}

@Composable
private fun WideLayout(controller: AppController, navigator: Navigator, snackbar: SnackbarHostState) {
    val settings = controller.settings
    val railState = rememberWideNavigationRailState(
        if (settings.railExpanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )
    Row(Modifier.fillMaxSize()) {
        WideNavigationRail(
            state = railState,
            modifier = Modifier,
        ) {
            Tab.entries.forEach { tab ->
                WideNavigationRailItem(
                    railExpanded = railState.targetValue == WideNavigationRailValue.Expanded,
                    icon = { Icon(if (navigator.tab == tab) tab.selectedIcon() else tab.icon(), null, Modifier.size(24.dp)) },
                    label = { Text(controller.s(tab.id)) },
                    selected = navigator.tab == tab,
                    onClick = { navigator.select(tab) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxSize()) {
            TabsContent(controller, navigator)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@Composable
private fun CompactLayout(controller: AppController, navigator: Navigator, snackbar: SnackbarHostState) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = navigator.tab == tab,
                        onClick = { navigator.select(tab) },
                        icon = { Icon(if (navigator.tab == tab) tab.selectedIcon() else tab.icon(), null) },
                        label = { Text(controller.s(tab.id)) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            TabsContent(controller, navigator)
        }
    }
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Home -> Icons.Outlined.Home
    Tab.Servers -> Icons.Outlined.Dns
    Tab.Settings -> Icons.Outlined.Settings
}

private fun Tab.selectedIcon(): ImageVector = when (this) {
    Tab.Home -> Icons.Rounded.Home
    Tab.Servers -> Icons.Rounded.Dns
    Tab.Settings -> Icons.Rounded.Settings
}

@Composable
private fun TabsContent(controller: AppController, navigator: Navigator) {
    val route = navigator.current
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val fadeSpec = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
            val slideSpec = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 380f)
            if (targetState == Route.Tabs) {
                (fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { -it / 5 } + scaleIn(fadeSpec, initialScale = 0.96f)) togetherWith
                    (fadeOut(fadeSpec) + slideOutHorizontally(slideSpec) { it / 5 })
            } else {
                (fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { it / 4 } + scaleIn(fadeSpec, initialScale = 0.97f)) togetherWith
                    (fadeOut(fadeSpec) + slideOutHorizontally(slideSpec) { -it / 8 })
            }
        },
        label = "route",
    ) { current ->
        when (current) {
            Route.Tabs -> TabsScreen(controller, navigator)
            Route.Add -> AddSubscriptionScreen(controller) { navigator.pop() }
            Route.WidgetsScreen -> WidgetsScreen(controller) { navigator.pop() }
            Route.Basic -> BasicScreen(controller) { navigator.pop() }
            Route.Advanced -> AdvancedScreen(controller, navigator) { navigator.pop() }
            Route.Dns -> DnsScreen(controller) { navigator.pop() }
            Route.Apps -> AppsScreen(controller) { navigator.pop() }
            Route.Rules -> RulesScreen(controller) { navigator.pop() }
            Route.AutoRules -> AutoRulesScreen(controller) { navigator.pop() }
            Route.Notifications -> NotificationsScreen(controller) { navigator.pop() }
            Route.IdleOff -> IdleOffScreen(controller) { navigator.pop() }
            Route.Help -> HelpScreen(controller) { navigator.pop() }
            Route.About -> AboutScreen(controller) { navigator.pop() }
            Route.Logs -> LogsScreen(controller) { navigator.pop() }
            Route.Connections -> ConnectionsScreen(controller) { navigator.pop() }
            Route.Diagnostics -> DiagnosticsScreen(controller) { navigator.pop() }
            Route.AppSettingsScreen -> dev.rooni.zefir.ui.screens.AppearanceScreen(controller, navigator) { navigator.pop() }
            Route.MergeProfiles -> dev.rooni.zefir.ui.screens.MergeProfilesScreen(controller) { navigator.pop() }
            is Route.EditProfile -> EditProfileScreen(controller, current.profileId) { navigator.pop() }
        }
    }
}

@Composable
private fun TabsScreen(controller: AppController, navigator: Navigator) {
    val tab = navigator.tab
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            val fadeSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 500f)
            (fadeIn(fadeSpec) + scaleIn(fadeSpec, initialScale = 0.98f)) togetherWith fadeOut(fadeSpec)
        },
        label = "tab",
    ) { current ->
        when (current) {
            Tab.Home -> HomeScreen(controller, navigator)
            Tab.Servers -> ServersScreen(controller, navigator)
            Tab.Settings -> SettingsHubScreen(controller, navigator)
        }
    }
}

/** Общая колонка для экранов: одинаковые отступы по краям. */
@Composable
fun ScreenColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .widthIn(max = 720.dp),
        content = content,
    )
}
