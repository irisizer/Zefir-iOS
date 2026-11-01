package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.ThemeMode
import dev.rooni.zefir.ui.AccentChoices
import dev.rooni.zefir.ui.ChoiceButtons
import dev.rooni.zefir.ui.GroupTitle
import dev.rooni.zefir.ui.SectionGroup
import dev.rooni.zefir.ui.SettingRow
import dev.rooni.zefir.ui.SwitchRow
import dev.rooni.zefir.ui.positionShape

/** Хаб настроек (ui/screens/SettingsHub.kt + Settings.kt). */
@Composable
fun SettingsScreen(
    controller: AppController,
    onOpenBasic: () -> Unit,
    onOpenLook: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val settings = controller.settings
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            controller.s("tab_settings"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
        )

        SectionGroup(controller.s("set_basic")) {
            SettingRow(
                shape = positionShape(0, 2),
                title = controller.s("set_basic"),
                description = controller.s("set_basic_desc"),
                icon = Icons.Rounded.PowerSettingsNew,
                onClick = onOpenBasic,
            )
            SettingRow(
                shape = positionShape(1, 2),
                title = controller.s("set_app"),
                description = controller.s("set_app_desc"),
                icon = Icons.Rounded.Palette,
                onClick = onOpenLook,
            )
            SettingRow(
                shape = positionShape(2, 2),
                title = controller.s("set_about"),
                description = controller.s("set_about_desc"),
                icon = Icons.Rounded.Info,
                value = "0.1.4",
                onClick = onOpenAbout,
            )
        }

        SectionGroup(controller.s("group_network")) {
            SwitchRow(
                shape = positionShape(0, 2),
                title = controller.s("ipv6"),
                description = controller.s("ipv6_desc"),
                checked = settings.ipv6,
                icon = Icons.Rounded.Tune,
            ) { v -> controller.update { it.copy(ipv6 = v) } }
            SwitchRow(
                shape = positionShape(1, 2),
                title = controller.s("rules_ads"),
                description = controller.s("rules_ads_desc"),
                checked = settings.blockAds,
                icon = Icons.Rounded.Apps,
            ) { v -> controller.update { it.copy(blockAds = v) } }
            SwitchRow(
                shape = positionShape(2, 2),
                title = controller.s("fake_ip"),
                description = controller.s("fake_ip_desc"),
                checked = settings.fakeIp,
                icon = Icons.Rounded.AutoAwesome,
            ) { v -> controller.update { it.copy(fakeIp = v) } }
        }

        GroupTitle(controller.s("mode_title"))
        ChoiceButtons(
            options = listOf(
                dev.rooni.zefir.model.RoutingMode.Smart to controller.s("mode_smart"),
                dev.rooni.zefir.model.RoutingMode.Global to controller.s("mode_global"),
                dev.rooni.zefir.model.RoutingMode.Direct to controller.s("mode_direct"),
            ),
            selected = settings.mode,
            onSelect = { mode -> controller.update { it.copy(mode = mode) } },
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Zefir 0.1.4 · ${controller.platform.platformName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(32.dp))
    }
}

/** Оформление (Settings.kt): тема, акцент, чёрный фон, цвет системы. */
@Composable
fun LookScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("set_app_screen"), onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroupTitle(controller.s("theme"))
            ChoiceButtons(
                options = listOf(
                    ThemeMode.System to controller.s("theme_system"),
                    ThemeMode.Light to controller.s("theme_light"),
                    ThemeMode.Dark to controller.s("theme_dark"),
                ),
                selected = settings.theme,
                onSelect = { mode -> controller.update { it.copy(theme = mode) } },
            )

            GroupTitle(controller.s("accent"))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccentChoices.take(6).forEach { seed ->
                    val selected = settings.accent == seed
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(seed.toULong()))
                            .clickable { controller.update { it.copy(accent = seed, dynamicColor = false, palette = -1) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionGroup {
                SwitchRow(
                    shape = positionShape(0, 2),
                    title = controller.s("amoled"),
                    description = controller.s("amoled_desc"),
                    checked = settings.amoled,
                    icon = Icons.Rounded.ColorLens,
                ) { v -> controller.update { it.copy(amoled = v) } }
                SwitchRow(
                    shape = positionShape(1, 2),
                    title = controller.s("dynamic_color"),
                    description = controller.s("dynamic_color_desc"),
                    checked = settings.dynamicColor,
                    icon = Icons.Rounded.Palette,
                ) { v -> controller.update { it.copy(dynamicColor = v) } }
                SwitchRow(
                    shape = positionShape(2, 2),
                    title = controller.s("haptics"),
                    description = controller.s("haptics_desc"),
                    checked = settings.haptics,
                    icon = Icons.Rounded.AutoAwesome,
                ) { v -> controller.update { it.copy(haptics = v) } }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Подключение: режим, DNS, MTU (Settings.kt: Basic). */
@Composable
fun BasicSettingsScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("set_basic"), onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroupTitle(controller.s("mode_title"))
            ChoiceButtons(
                options = listOf(
                    dev.rooni.zefir.model.RoutingMode.Smart to controller.s("mode_smart"),
                    dev.rooni.zefir.model.RoutingMode.Global to controller.s("mode_global"),
                    dev.rooni.zefir.model.RoutingMode.Direct to controller.s("mode_direct"),
                ),
                selected = settings.mode,
                onSelect = { mode -> controller.update { it.copy(mode = mode) } },
            )
            Text(
                when (settings.mode) {
                    dev.rooni.zefir.model.RoutingMode.Smart -> controller.s("mode_smart_desc")
                    dev.rooni.zefir.model.RoutingMode.Global -> controller.s("mode_global_desc")
                    dev.rooni.zefir.model.RoutingMode.Direct -> controller.s("mode_direct_desc")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            SectionGroup(controller.s("group_network")) {
                SwitchRow(
                    shape = positionShape(0, 2),
                    title = controller.s("auto_switch"),
                    description = controller.s("auto_switch_desc"),
                    checked = settings.autoSwitch,
                    icon = Icons.Rounded.Tune,
                ) { v -> controller.update { it.copy(autoSwitch = v) } }
                SwitchRow(
                    shape = positionShape(1, 2),
                    title = controller.s("connect_on_start"),
                    description = controller.s("connect_on_start_desc"),
                    checked = settings.connectOnStart,
                    icon = Icons.Rounded.PowerSettingsNew,
                ) { v -> controller.update { it.copy(connectOnStart = v) } }
                SwitchRow(
                    shape = positionShape(2, 2),
                    title = controller.s("ipv6"),
                    description = controller.s("ipv6_desc"),
                    checked = settings.ipv6,
                    icon = Icons.Rounded.Apps,
                ) { v -> controller.update { it.copy(ipv6 = v) } }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** О приложении (Settings.kt: About). */
@Composable
fun AboutScreen(controller: AppController, onBack: () -> Unit) {
    SubScreen(title = controller.s("set_about"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SectionGroup {
                SettingRow(positionShape(0, 3), controller.s("about_version").replace("%1\$s", "0.1.4"))
                SettingRow(positionShape(1, 3), controller.s("about_author"), value = "YouRooni")
                SettingRow(positionShape(2, 3), controller.s("about_core"), value = "mihomo")
                SettingRow(positionShape(3, 3), controller.s("about_source"), value = "GPL-3.0")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                controller.s("about_text"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "iOS port: Zefir-iOS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}