package dev.rooni.zefir.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.rooni.zefir.model.Tab

/** Маршруты второго уровня (Route в оригинале). */
sealed interface Route {
    data object Tabs : Route
    data object Add : Route
    data object WidgetsScreen : Route
    data object Basic : Route
    data object Advanced : Route
    data object Dns : Route
    data object Apps : Route
    data object Rules : Route
    data object AutoRules : Route
    data object Notifications : Route
    data object IdleOff : Route
    data object Help : Route
    data object About : Route
    data object Logs : Route
    data object Connections : Route
    data object Diagnostics : Route
    data object AppSettingsScreen : Route
    data object MergeProfiles : Route
    data class EditProfile(val profileId: String) : Route
}

/** Стек навигации + выбранная вкладка (ui/Navigator.kt). */
class Navigator(private val onTabChange: (Tab) -> Unit, initialTab: Tab = Tab.Home) {
    val stack = mutableStateListOf<Route>(Route.Tabs)
    val reselects = mutableStateMapOf<Tab, Int>()
    var tab by mutableStateOf(initialTab)
        private set

    val current: Route get() = stack.last()

    fun push(route: Route) {
        if (route == Route.Tabs) return
        stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun popAll() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun select(tab: Tab) {
        if (this.tab == tab && stack.size == 1) {
            reselects[tab] = (reselects[tab] ?: 0) + 1
        }
        this.tab = tab
        onTabChange(tab)
    }
}
