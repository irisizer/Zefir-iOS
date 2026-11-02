package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.DnsMode
import dev.rooni.zefir.model.PingMethod
import dev.rooni.zefir.model.RoutingMode
import dev.rooni.zefir.model.ThemeMode
import dev.rooni.zefir.res.Strings
import dev.rooni.zefir.ui.AccentChoices
import dev.rooni.zefir.ui.AppDialog
import dev.rooni.zefir.ui.ChoiceButtons
import dev.rooni.zefir.ui.GroupTitle
import dev.rooni.zefir.ui.MultiPalettes
import dev.rooni.zefir.ui.Navigator
import dev.rooni.zefir.ui.Route
import dev.rooni.zefir.ui.SectionGroup
import dev.rooni.zefir.ui.SettingRow
import dev.rooni.zefir.ui.SubScreen
import dev.rooni.zefir.ui.SwitchRow
import dev.rooni.zefir.ui.positionShape

@Composable
fun SettingsHubScreen(controller: AppController, navigator: Navigator) {
    Column(Modifier.fillMaxSize()) {
        Text(
            controller.s("tab_settings"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SectionGroup {
                SettingRow(
                    shape = positionShape(0, 3),
                    title = controller.s("set_basic"),
                    description = controller.s("set_basic_desc"),
                    icon = Icons.Rounded.PowerSettingsNew,
                    onClick = { navigator.push(Route.Basic) },
                )
                SettingRow(
                    shape = positionShape(1, 3),
                    title = controller.s("set_app"),
                    description = controller.s("set_app_desc"),
                    icon = Icons.Rounded.Palette,
                    onClick = { navigator.push(Route.AppSettingsScreen) },
                )
                SettingRow(
                    shape = positionShape(2, 3),
                    title = controller.s("apps_title"),
                    description = controller.s("apps_desc"),
                    icon = Icons.Rounded.WifiTethering,
                    onClick = { navigator.push(Route.Apps) },
                )
                SettingRow(
                    shape = positionShape(3, 3),
                    title = controller.s("set_advanced"),
                    description = controller.s("set_advanced_desc"),
                    icon = Icons.Rounded.Tune,
                    onClick = { navigator.push(Route.Advanced) },
                )
            }

            SectionGroup(controller.s("group_updates")) {
                SettingRow(
                    shape = positionShape(0, 3),
                    title = controller.s("rules_title"),
                    description = controller.s("rules_desc"),
                    icon = Icons.Rounded.Map,
                    onClick = { navigator.push(Route.Rules) },
                )
                SettingRow(
                    shape = positionShape(1, 3),
                    title = controller.s("auto_title"),
                    description = controller.s("auto_short"),
                    icon = Icons.Rounded.Router,
                    onClick = { navigator.push(Route.AutoRules) },
                )
                SettingRow(
                    shape = positionShape(2, 3),
                    title = controller.s("notif_title"),
                    description = controller.s("notif_desc"),
                    icon = Icons.Rounded.Notifications,
                    onClick = { navigator.push(Route.Notifications) },
                )
                SettingRow(
                    shape = positionShape(3, 3),
                    title = controller.s("idle_off"),
                    description = controller.s("idle_off_desc"),
                    icon = Icons.Rounded.Speed,
                    value = if (controller.settings.idleOff) controller.sf("idle_minutes", controller.settings.idleOffMinutes) else null,
                    onClick = { navigator.push(Route.IdleOff) },
                )
            }

            SectionGroup(controller.s("group_diag")) {
                SettingRow(
                    shape = positionShape(0, 3),
                    title = controller.s("diag_title"),
                    description = controller.s("diag_desc"),
                    icon = Icons.Rounded.Insights,
                    onClick = { navigator.push(Route.Diagnostics) },
                )
                SettingRow(
                    shape = positionShape(1, 3),
                    title = controller.s("conn_title"),
                    description = controller.s("conn_desc"),
                    icon = Icons.Rounded.Anchor,
                    onClick = { navigator.push(Route.Connections) },
                )
                SettingRow(
                    shape = positionShape(2, 3),
                    title = controller.s("logs"),
                    description = controller.s("logs_desc"),
                    icon = Icons.Rounded.BarChart,
                    onClick = { navigator.push(Route.Logs) },
                )
                SettingRow(
                    shape = positionShape(3, 3),
                    title = controller.s("merge_title"),
                    description = controller.s("merge_desc"),
                    icon = Icons.Rounded.Cached,
                    onClick = { navigator.push(Route.MergeProfiles) },
                )
            }

            SectionGroup(controller.s("group_home")) {
                SettingRow(
                    shape = positionShape(0, 1),
                    title = controller.s("widgets_title"),
                    description = controller.s("widgets_desc"),
                    icon = Icons.Rounded.Widgets,
                    onClick = { navigator.push(Route.WidgetsScreen) },
                )
            }

            SectionGroup {
                SettingRow(
                    shape = positionShape(0, 2),
                    title = controller.s("help_title"),
                    description = controller.s("about_faq"),
                    icon = Icons.Rounded.Info,
                    onClick = { navigator.push(Route.Help) },
                )
                SettingRow(
                    shape = positionShape(1, 2),
                    title = controller.s("set_about"),
                    description = controller.s("set_about_desc"),
                    icon = Icons.Rounded.Cloud,
                    onClick = { navigator.push(Route.About) },
                )
            }
        }
    }
}

@Composable
fun BasicScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("set_basic"), onBack = onBack) {
        GroupTitle(controller.s("mode_title"))
        Column(Modifier.padding(horizontal = 12.dp)) {
            RoutingMode.entries.forEachIndexed { index, mode ->
                val selected = settings.mode == mode
                SettingRow(
                    shape = positionShape(index, RoutingMode.entries.lastIndex),
                    title = when (mode) {
                        RoutingMode.Smart -> controller.s("mode_smart")
                        RoutingMode.Global -> controller.s("mode_global")
                        RoutingMode.Direct -> controller.s("mode_direct")
                    },
                    description = when (mode) {
                        RoutingMode.Smart -> controller.s("mode_smart_desc")
                        RoutingMode.Global -> controller.s("mode_global_desc")
                        RoutingMode.Direct -> controller.s("mode_direct_desc")
                    },
                    onClick = { controller.update { it.copy(mode = mode) } },
                    trailing = {
                        if (selected) {
                            Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
        }

        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 2),
                title = controller.s("connect_on_start"),
                description = controller.s("connect_on_start_desc"),
                checked = settings.connectOnStart,
                icon = Icons.Rounded.PowerSettingsNew,
            ) { value -> controller.update { it.copy(connectOnStart = value) } }
            SwitchRow(
                shape = positionShape(1, 2),
                title = controller.s("auto_switch"),
                description = controller.s("auto_switch_desc"),
                checked = settings.autoSwitch,
                icon = Icons.Rounded.Cached,
            ) { value -> controller.update { it.copy(autoSwitch = value) } }
        }

        SectionGroup(controller.s("group_network")) {
            SettingRow(
                shape = positionShape(0, 2),
                title = controller.s("ping_method"),
                description = controller.s("ping_tcp_desc"),
                icon = Icons.Rounded.Speed,
                value = if (settings.pingMethod == PingMethod.Tcp) controller.s("ping_tcp") else controller.s("ping_url"),
                onClick = {
                    controller.update {
                        it.copy(pingMethod = if (it.pingMethod == PingMethod.Tcp) PingMethod.Url else PingMethod.Tcp)
                    }
                },
            )
            SwitchRow(
                shape = positionShape(1, 2),
                title = controller.s("unified_delay"),
                description = controller.s("unified_delay_desc"),
                checked = settings.unifiedDelay,
                icon = Icons.Rounded.Timer,
            ) { value -> controller.update { it.copy(unifiedDelay = value) } }
        }
    }
}

@Composable
fun AppearanceScreen(controller: AppController, navigator: Navigator, onBack: () -> Unit) {
    val settings = controller.settings
    var resetDialog by remember { mutableStateOf(false) }

    SubScreen(title = controller.s("set_app_screen"), onBack = onBack) {
        GroupTitle(controller.s("theme"))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = settings.theme == mode
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f).clickable { controller.update { it.copy(theme = mode) } },
                ) {
                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when (mode) {
                                ThemeMode.System -> controller.s("theme_system")
                                ThemeMode.Light -> controller.s("theme_light")
                                ThemeMode.Dark -> controller.s("theme_dark")
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        GroupTitle(controller.s("accent"))
        AccentRow(controller, settings.accent, settings.palette)
        GroupTitle(controller.s("theme_multi"))
        PaletteRow(controller, settings.palette)

        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 3),
                title = controller.s("dynamic_color"),
                description = controller.s("dynamic_color_desc"),
                checked = settings.dynamicColor,
                icon = Icons.Rounded.Palette,
            ) { value -> controller.update { it.copy(dynamicColor = value) } }
            SwitchRow(
                shape = positionShape(1, 3),
                title = controller.s("amoled"),
                description = controller.s("amoled_desc"),
                checked = settings.amoled,
                icon = Icons.Rounded.Settings,
            ) { value -> controller.update { it.copy(amoled = value) } }
            SwitchRow(
                shape = positionShape(2, 3),
                title = controller.s("haptics"),
                description = controller.s("haptics_desc"),
                checked = settings.haptics,
                icon = Icons.Rounded.SportsScore,
            ) { value -> controller.update { it.copy(haptics = value) } }
        }

        GroupTitle(controller.s("group_home"))
        SectionGroup {
            SettingRow(
                shape = positionShape(0, 1),
                title = controller.s("widgets_title"),
                description = controller.s("widgets_desc"),
                icon = Icons.Rounded.Widgets,
                onClick = { navigator.push(Route.WidgetsScreen) },
            )
        }

        GroupTitle(controller.s("region"))
        Column(Modifier.padding(horizontal = 12.dp)) {
            val options = listOf(
                "" to controller.s("region_off"),
                "ru" to controller.s("region_ru"),
            )
            options.forEachIndexed { index, (code, label) ->
                SettingRow(
                    shape = positionShape(index, options.lastIndex),
                    title = label,
                    icon = Icons.Rounded.Map,
                    onClick = { controller.update { it.copy(region = code) } },
                    trailing = {
                        if (settings.region == code) {
                            Icon(Icons.Rounded.Cloud, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    },
                )
            }
        }

        GroupTitle(Strings.ru["group_updates"] ?: "")
        SectionGroup {
            SettingRow(
                shape = positionShape(0, 1),
                title = controller.s("auto_update"),
                description = controller.s("auto_update_desc"),
                icon = Icons.Rounded.CloudDownload,
                value = when (settings.autoUpdate) {
                    0 -> controller.s("auto_update_off")
                    6 -> controller.s("auto_update_6")
                    24 -> controller.s("auto_update_24")
                    else -> controller.s("auto_update_72")
                },
                onClick = {
                    val next = when (settings.autoUpdate) {
                        0 -> 6
                        6 -> 24
                        24 -> 72
                        else -> 0
                    }
                    controller.update { it.copy(autoUpdate = next) }
                },
            )
        }

        GroupTitle(Strings.ru["group_look"] ?: "")
        SectionGroup {
            SettingRow(
                shape = positionShape(0, 1),
                title = controller.s("reset"),
                description = controller.s("reset_desc"),
                icon = Icons.Rounded.RestartAlt,
                onClick = { resetDialog = true },
            )
        }
    }

    if (resetDialog) {
        AppDialog(
            title = controller.s("reset_q"),
            text = controller.s("reset_desc"),
            confirm = controller.s("reset_action"),
            dismiss = controller.s("cancel"),
            onConfirm = { controller.resetSettings(); resetDialog = false },
            onDismiss = { resetDialog = false },
        )
    }
}

@Composable
private fun AccentRow(controller: AppController, accent: Long, palette: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccentChoices.take(6).forEach { color ->
            AccentDot(color, selected = palette < 0 && accent == color) {
                controller.update { it.copy(accent = color, palette = -1, dynamicColor = false) }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccentChoices.drop(6).forEach { color ->
            AccentDot(color, selected = palette < 0 && accent == color) {
                controller.update { it.copy(accent = color, palette = -1, dynamicColor = false) }
            }
        }
    }
}

@Composable
private fun AccentDot(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(Color(color), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(14.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
private fun PaletteRow(controller: AppController, palette: Int) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        MultiPalettes.forEachIndexed { index, colors ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { controller.update { it.copy(palette = index, dynamicColor = false) } },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colors.forEach { color ->
                    Box(Modifier.size(if (palette == index) 30.dp else 26.dp).background(Color(color), CircleShape))
                }
                Spacer(Modifier.height(0.dp))
                if (palette == index) {
                    Text(
                        controller.s("applied"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedScreen(controller: AppController, navigator: Navigator, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("set_advanced"), onBack = onBack) {
        MessageBannerSafe(controller.s("adv_warning"))
        SectionGroup {
            SettingRow(
                shape = positionShape(0, 3),
                title = controller.s("group_dns"),
                description = controller.s("dns_desc"),
                icon = Icons.Rounded.Dns,
                onClick = { navigator.push(Route.Dns) },
            )
            SettingRow(
                shape = positionShape(1, 3),
                title = controller.s("rules_raw"),
                description = controller.s("rules_raw_desc"),
                icon = Icons.Rounded.Link,
                value = "${settings.rawRules.size}",
                onClick = { navigator.push(Route.Rules) },
            )
            SettingRow(
                shape = positionShape(2, 3),
                title = controller.s("logs"),
                description = controller.s("logs_desc"),
                icon = Icons.Rounded.BarChart,
                onClick = { navigator.push(Route.Logs) },
            )
        }

        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 5),
                title = controller.s("ipv6"),
                description = controller.s("ipv6_desc"),
                checked = settings.ipv6,
                icon = Icons.Rounded.Cloud,
            ) { value -> controller.update { it.copy(ipv6 = value) } }
            SwitchRow(
                shape = positionShape(1, 5),
                title = controller.s("fake_ip"),
                description = controller.s("fake_ip_desc"),
                checked = settings.fakeIp,
                icon = Icons.Rounded.Bolt,
            ) { value -> controller.update { it.copy(fakeIp = value) } }
            SwitchRow(
                shape = positionShape(2, 5),
                title = controller.s("sniffer"),
                description = controller.s("sniffer_desc"),
                checked = settings.sniffer,
                icon = Icons.Rounded.Insights,
            ) { value -> controller.update { it.copy(sniffer = value) } }
            SwitchRow(
                shape = positionShape(3, 5),
                title = controller.s("tcp_concurrent"),
                description = controller.s("tcp_concurrent_desc"),
                checked = settings.tcpConcurrent,
                icon = Icons.Rounded.Shield,
            ) { value -> controller.update { it.copy(tcpConcurrent = value) } }
            SwitchRow(
                shape = positionShape(4, 5),
                title = controller.s("skip_cert"),
                description = controller.s("skip_cert_desc"),
                checked = settings.skipCertVerify,
                icon = Icons.Rounded.Shield,
            ) { value -> controller.update { it.copy(skipCertVerify = value) } }
        }

        GroupTitle(controller.s("stack"))
        Column(Modifier.padding(horizontal = 12.dp)) {
            listOf("system", "gvisor").forEachIndexed { index, stack ->
                SettingRow(
                    shape = positionShape(index, 1),
                    title = stack,
                    description = controller.s("stack_desc"),
                    onClick = { controller.update { it.copy(stack = stack) } },
                    trailing = {
                        if (settings.stack == stack) Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }

        GroupTitle(controller.s("mtu"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            var mtu by remember { mutableStateOf(settings.mtu.toString()) }
            OutlinedTextField(
                value = mtu,
                onValueChange = {
                    mtu = it
                    it.toIntOrNull()?.let { value -> controller.update { s -> s.copy(mtu = value) } }
                },
                label = { Text(controller.s("mtu")) },
                supportingText = { Text(controller.s("mtu_desc")) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GroupTitle(controller.s("test_url"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            var url by remember { mutableStateOf(settings.testUrl) }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(controller.s("test_url")) },
                supportingText = { Text(controller.s("test_url_desc")) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 2),
                title = controller.s("detailed_errors"),
                description = controller.s("detailed_errors_desc"),
                checked = settings.detailedErrors,
                icon = Icons.Rounded.Info,
            ) { value -> controller.update { it.copy(detailedErrors = value) } }
            SettingRow(
                shape = positionShape(1, 2),
                title = controller.s("reset"),
                description = controller.s("reset_desc"),
                icon = Icons.Rounded.RestartAlt,
                onClick = { controller.resetSettings() },
            )
        }
    }
}

@Composable
private fun MessageBannerSafe(text: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        dev.rooni.zefir.ui.MessageBanner(text)
    }
}

@Composable
fun DnsScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("group_dns"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            val options = listOf(
                DnsMode.FromProfile to (controller.s("dns_profile") to controller.s("dns_profile_desc")),
                DnsMode.Secure to (controller.s("dns_secure") to controller.s("dns_secure_desc")),
                DnsMode.Custom to (controller.s("dns_custom") to controller.s("dns_custom_desc")),
            )
            options.forEachIndexed { index, (mode, texts) ->
                SettingRow(
                    shape = positionShape(index, options.lastIndex),
                    title = texts.first,
                    description = texts.second,
                    onClick = { controller.update { it.copy(dnsMode = mode) } },
                    trailing = {
                        if (settings.dnsMode == mode) {
                            Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    },
                )
            }
        }
        if (settings.dnsMode == DnsMode.Custom) {
            Column(Modifier.padding(16.dp)) {
                var dns by remember { mutableStateOf(settings.customDns) }
                OutlinedTextField(
                    value = dns,
                    onValueChange = { dns = it; controller.update { s -> s.copy(customDns = it) } },
                    label = { Text(controller.s("dns_custom")) },
                    placeholder = { Text("https://1.1.1.1/dns-query") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 1),
                title = controller.s("dns_hijack_all"),
                description = controller.s("dns_hijack_all_desc"),
                checked = settings.dnsHijackAll,
                icon = Icons.Rounded.Shield,
            ) { value -> controller.update { it.copy(dnsHijackAll = value) } }
        }
    }
}
