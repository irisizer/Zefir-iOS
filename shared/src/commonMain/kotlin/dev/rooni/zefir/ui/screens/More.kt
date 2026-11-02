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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.HomeWidget
import dev.rooni.zefir.model.RuleAction
import dev.rooni.zefir.model.SplitMode
import dev.rooni.zefir.model.UserRule
import dev.rooni.zefir.platform.nowMillis
import dev.rooni.zefir.ui.ActionButton
import dev.rooni.zefir.ui.AppTextField
import dev.rooni.zefir.ui.ChoiceButtons
import dev.rooni.zefir.ui.GroupTitle
import dev.rooni.zefir.ui.MessageBanner
import dev.rooni.zefir.ui.SectionGroup
import dev.rooni.zefir.ui.SettingRow
import dev.rooni.zefir.ui.SubScreen
import dev.rooni.zefir.ui.SwitchRow
import dev.rooni.zefir.ui.formatBytes
import dev.rooni.zefir.ui.positionShape

// ---------- приложения (split tunneling) ----------

@Composable
fun AppsScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("apps_title"), onBack = onBack) {
        if (!controller.platform.vpnAvailable) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                MessageBanner(controller.s("apps_unavailable"), error = false)
            }
        }
        GroupTitle(controller.s("apps_mode_off"))
        Column(Modifier.padding(horizontal = 12.dp)) {
            val options = listOf(
                SplitMode.Off to (controller.s("apps_mode_off") to controller.s("apps_mode_off_hint")),
                SplitMode.Only to (controller.s("apps_mode_only") to controller.s("apps_mode_only_hint")),
                SplitMode.Exclude to (controller.s("apps_mode_exclude") to controller.s("apps_mode_exclude_hint")),
            )
            options.forEachIndexed { index, (mode, texts) ->
                SettingRow(
                    shape = positionShape(index, options.lastIndex),
                    title = texts.first,
                    description = texts.second,
                    onClick = { controller.update { it.copy(splitMode = mode) } },
                    trailing = {
                        if (settings.splitMode == mode) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("apps_restart_note"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GroupTitle(controller.s("apps_presets"))
        Column(Modifier.padding(horizontal = 12.dp)) {
            val packs = listOf(
                "banks" to "pack_banks",
                "food" to "pack_food",
                "gov" to "pack_gov",
                "operators" to "pack_operators",
                "school" to "pack_school",
                "shops" to "pack_shops",
                "social" to "pack_social",
                "transport" to "pack_transport",
                "yandex" to "pack_yandex",
            )
            packs.forEachIndexed { index, (id, titleKey) ->
                val applied = id in settings.appliedPacks
                SettingRow(
                    shape = positionShape(index, packs.lastIndex),
                    title = controller.s(titleKey),
                    onClick = {
                        controller.update {
                            if (applied) it.copy(appliedPacks = it.appliedPacks - id)
                            else it.copy(appliedPacks = it.appliedPacks + id, regionDirect = true)
                        }
                    },
                    trailing = {
                        if (applied) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                )
            }
        }
    }
}

// ---------- правила ----------

@Composable
fun RulesScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    var domain by remember { mutableStateOf("") }
    var raw by remember { mutableStateOf("") }

    SubScreen(title = controller.s("rules_title"), onBack = onBack) {
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 1),
                title = controller.s("rules_ads"),
                description = controller.s("rules_ads_desc"),
                checked = settings.blockAds,
                icon = Icons.Rounded.Block,
            ) { value -> controller.update { it.copy(blockAds = value) } }
        }

        GroupTitle(controller.s("rules_mine"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AppTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = controller.s("rules_domain"),
                        placeholder = "example.com",
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (domain.isNotBlank()) {
                            controller.update {
                                it.copy(rules = it.rules + UserRule(domain.trim(), RuleAction.Direct))
                            }
                            domain = ""
                        }
                    }
                ) { Icon(Icons.Rounded.Add, null) }
            }
        }
        if (settings.rules.isEmpty()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    controller.s("rules_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(Modifier.padding(horizontal = 12.dp)) {
                settings.rules.forEachIndexed { index, rule ->
                    Surface(
                        shape = positionShape(index, settings.rules.lastIndex),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rule.value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when (rule.action) {
                                    RuleAction.Direct -> controller.s("rule_direct")
                                    RuleAction.Proxy -> controller.s("rule_proxy")
                                    RuleAction.Block -> controller.s("rule_block")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val next = when (rule.action) {
                                        RuleAction.Direct -> RuleAction.Proxy
                                        RuleAction.Proxy -> RuleAction.Block
                                        RuleAction.Block -> RuleAction.Direct
                                    }
                                    controller.update { s ->
                                        s.copy(rules = s.rules.map { if (it == rule) it.copy(action = next) else it })
                                    }
                                },
                            )
                            IconButton(
                                onClick = { controller.update { s -> s.copy(rules = s.rules - rule) } }
                            ) { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }

        GroupTitle(controller.s("rules_raw"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            AppTextField(
                value = raw,
                onValueChange = { raw = it },
                label = controller.s("rules_raw_hint"),
                placeholder = "DOMAIN-KEYWORD,google,DIRECT",
            )
            Spacer(Modifier.height(8.dp))
            ActionButton(text = controller.s("add_action"), enabled = raw.isNotBlank()) {
                controller.update { it.copy(rawRules = it.rawRules + raw.trim()) }
                raw = ""
            }
            settings.rawRules.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { controller.update { it.copy(rawRules = it.rawRules - line) } }) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ---------- автоправила ----------

@Composable
fun AutoRulesScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("auto_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MessageBanner(controller.s("always_on_hint"))
        }
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 3),
                title = controller.s("auto_master"),
                description = controller.s("auto_short"),
                checked = settings.autoRules,
                icon = Icons.Rounded.Sync,
            ) { value -> controller.update { it.copy(autoRules = value) } }
            SwitchRow(
                shape = positionShape(1, 3),
                title = controller.s("auto_connect"),
                description = controller.s("auto_connect_desc"),
                checked = settings.autoConnectElsewhere,
                enabled = settings.autoRules,
            ) { value -> controller.update { it.copy(autoConnectElsewhere = value) } }
            SwitchRow(
                shape = positionShape(2, 3),
                title = controller.s("auto_switch"),
                description = controller.s("auto_switch_desc"),
                checked = settings.autoSwitch,
            ) { value -> controller.update { it.copy(autoSwitch = value) } }
        }
        GroupTitle(controller.s("auto_trusted"))
        Column(Modifier.padding(16.dp)) {
            Text(
                controller.s("auto_need_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (settings.trustedWifi.isEmpty()) {
                Text(
                    controller.s("auto_need_location"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                settings.trustedWifi.forEach { ssid ->
                    Text(ssid, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ---------- уведомления ----------

@Composable
fun NotificationsScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("notif_title"), onBack = onBack) {
        if (!controller.platform.vpnAvailable) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                MessageBanner(controller.s("apps_unavailable"))
            }
        }
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 4),
                title = controller.s("notif_master"),
                description = controller.s("notif_desc"),
                checked = settings.notifEnabled,
            ) { value -> controller.update { it.copy(notifEnabled = value) } }
            SwitchRow(
                shape = positionShape(1, 4),
                title = controller.s("notif_speed"),
                description = controller.s("notif_speed_desc"),
                checked = settings.notifLiveSpeed,
                enabled = settings.notifEnabled,
            ) { value -> controller.update { it.copy(notifLiveSpeed = value) } }
            SwitchRow(
                shape = positionShape(2, 4),
                title = controller.s("notif_pause"),
                description = controller.s("notif_pause_desc"),
                checked = settings.notifPauseButton,
                enabled = settings.notifEnabled,
            ) { value -> controller.update { it.copy(notifPauseButton = value) } }
            SwitchRow(
                shape = positionShape(3, 4),
                title = controller.s("notif_stop"),
                description = controller.s("notif_stop_desc"),
                checked = settings.notifStopButton,
                enabled = settings.notifEnabled,
            ) { value -> controller.update { it.copy(notifStopButton = value) } }
        }
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 2),
                title = controller.s("battery"),
                description = controller.s("battery_desc"),
                checked = settings.battery,
            ) { value -> controller.update { it.copy(battery = value) } }
            SwitchRow(
                shape = positionShape(1, 2),
                title = controller.s("reminders"),
                description = controller.s("reminders_desc"),
                checked = settings.reminders,
            ) { value -> controller.update { it.copy(reminders = value) } }
        }
    }
}

// ---------- простой ----------

@Composable
fun IdleOffScreen(controller: AppController, onBack: () -> Unit) {
    val settings = controller.settings
    SubScreen(title = controller.s("idle_off"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("idle_off_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionGroup {
            SwitchRow(
                shape = positionShape(0, 2),
                title = controller.s("idle_off"),
                description = controller.s("idle_off_desc"),
                checked = settings.idleOff,
            ) { value -> controller.update { it.copy(idleOff = value) } }
            SwitchRow(
                shape = positionShape(1, 2),
                title = controller.s("idle_night"),
                description = controller.s("idle_night_desc"),
                checked = settings.idleNightOnly,
                enabled = settings.idleOff,
            ) { value -> controller.update { it.copy(idleNightOnly = value) } }
        }
        GroupTitle(controller.s("idle_off_delay"))
        ChoiceButtons(
            options = listOf(5, 15, 30, 60).map { it to controller.sf("idle_minutes", it) },
            selected = settings.idleOffMinutes,
        ) { minutes -> controller.update { it.copy(idleOffMinutes = minutes) } }
    }
}

// ---------- помощь / о приложении ----------

@Composable
fun HelpScreen(controller: AppController, onBack: () -> Unit) {
    SubScreen(title = controller.s("help_title"), onBack = onBack) {
        listOf(
            "faq_sub_q" to "faq_sub_a",
            "faq_bank_q" to "faq_bank_a",
            "faq_battery_q" to "faq_battery_a",
        ).forEach { (q, a) ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(controller.s(q), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    controller.s(a),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AboutScreen(controller: AppController, onBack: () -> Unit) {
    SubScreen(title = controller.s("set_about"), onBack = onBack) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Dns, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Zefir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                controller.sf("about_version", "1.1.5 (iOS)"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionGroup {
            SettingRow(
                shape = positionShape(0, 3),
                title = controller.s("about_core"),
                value = "mihomo",
            )
            SettingRow(
                shape = positionShape(1, 3),
                title = controller.s("about_license"),
                value = controller.s("about_source"),
            )
            SettingRow(
                shape = positionShape(2, 3),
                title = controller.s("about_author"),
                value = "rooni",
            )
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                controller.s("about_text"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- журнал / соединения / диагностика ----------

@Composable
fun LogsScreen(controller: AppController, onBack: () -> Unit) {
    SubScreen(title = controller.s("logs"), onBack = onBack) {
        Column(Modifier.padding(16.dp)) {
            Text(
                controller.s("logs_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                controller.s("logs_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ConnectionsScreen(controller: AppController, onBack: () -> Unit) {
    SubScreen(title = controller.s("conn_title"), onBack = onBack) {
        Column(Modifier.padding(16.dp)) {
            Text(
                controller.s("conn_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                controller.s("conn_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DiagnosticsScreen(controller: AppController, onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }

    SubScreen(
        title = controller.s("diag_title"),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                if (!running) {
                    running = true
                    controller.refresh(null, silent = true)
                }
            }) { Icon(Icons.Rounded.Refresh, null) }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("diag_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            val steps = listOf(
                "diag_sub" to (controller.activeProfile?.let { controller.sf("servers_count", it.servers) }
                    ?: controller.s("diag_v_sub")),
                "diag_server" to (controller.activeNode?.let { dev.rooni.zefir.app.Links.withoutFlag(it.name) }
                    ?: controller.s("w_server_none")),
                "diag_net" to controller.s("diag_v_ok"),
            )
            steps.forEachIndexed { index, (titleKey, value) ->
                SettingRow(
                    shape = positionShape(index, steps.lastIndex),
                    title = controller.s(titleKey),
                    value = value,
                    trailing = {
                        if (running) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            ActionButton(text = controller.s("diag_again")) {
                running = false
                results = emptyMap()
            }
        }
    }
}

// ---------- виджеты главного экрана ----------

@Composable
fun WidgetsScreen(controller: AppController, onBack: () -> Unit) {
    val widgets = controller.settings.widgets
    val hidden = HomeWidget.entries.filterNot { it in widgets }
    val titles = mapOf(
        HomeWidget.Traffic to "w_traffic",
        HomeWidget.Server to "w_server",
        HomeWidget.Subscription to "w_subscription",
        HomeWidget.Speed to "w_speed",
        HomeWidget.Ip to "w_ip",
        HomeWidget.Mode to "w_mode",
        HomeWidget.SplitApps to "w_apps",
    )

    SubScreen(title = controller.s("widgets_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("widgets_text"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            widgets.forEachIndexed { index, widget ->
                Surface(
                    shape = positionShape(index, widgets.lastIndex),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.DragHandle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            controller.s(titles.getValue(widget)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val list = widgets.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                                controller.update { it.copy(widgets = list) }
                            },
                        ) { Icon(Icons.Rounded.KeyboardArrowUp, null, modifier = Modifier.size(18.dp)) }
                        IconButton(
                            enabled = index < widgets.lastIndex,
                            onClick = {
                                val list = widgets.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                                controller.update { it.copy(widgets = list) }
                            },
                        ) { Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.size(18.dp)) }
                        IconButton(
                            onClick = { controller.update { it.copy(widgets = widgets - widget) } }
                        ) { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        if (hidden.isNotEmpty()) {
            GroupTitle(controller.s("widgets_hidden"))
            Column(Modifier.padding(horizontal = 12.dp)) {
                hidden.forEachIndexed { index, widget ->
                    SettingRow(
                        shape = positionShape(index, hidden.lastIndex),
                        title = controller.s(titles.getValue(widget)),
                        icon = Icons.Rounded.Add,
                        onClick = { controller.update { it.copy(widgets = it.widgets + widget) } },
                    )
                }
            }
        }
    }
}

// ---------- подписка: правка / объединение ----------

@Composable
fun EditProfileScreen(controller: AppController, profileId: String, onBack: () -> Unit) {
    val profile = controller.profiles.firstOrNull { it.id == profileId }
    if (profile == null) {
        onBack()
        return
    }
    var name by remember(profile.id) { mutableStateOf(profile.name) }

    SubScreen(title = controller.s("edit_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            AppTextField(value = name, onValueChange = { name = it }, label = controller.s("edit_name"))
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = profile.url,
                onValueChange = { },
                label = controller.s("edit_url"),
                singleLine = false,
                minLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            ActionButton(text = controller.s("save")) {
                controller.renameProfile(profile, name.trim().ifBlank { profile.name })
                onBack()
            }
            Spacer(Modifier.height(16.dp))
            SettingRow(
                shape = RoundedCornerShape(22.dp),
                title = controller.s("servers_update"),
                icon = Icons.Rounded.Refresh,
                onClick = {
                    controller.refresh(profile)
                    onBack()
                },
            )
            Spacer(Modifier.height(8.dp))
            SettingRow(
                shape = RoundedCornerShape(22.dp),
                title = controller.s("delete"),
                icon = Icons.Rounded.Delete,
                onClick = {
                    controller.deleteProfile(profile)
                    onBack()
                },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                controller.s("edit_config_note"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MergeProfilesScreen(controller: AppController, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(controller.profiles.map { it.id }.toSet()) }

    SubScreen(title = controller.s("merge_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("merge_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            controller.profiles.forEachIndexed { index, profile ->
                val checked = profile.id in selected
                SettingRow(
                    shape = positionShape(index, controller.profiles.lastIndex),
                    title = profile.name,
                    description = controller.sf("servers_count", profile.servers),
                    onClick = {
                        selected = if (checked) selected - profile.id else selected + profile.id
                    },
                    trailing = {
                        if (checked) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            ActionButton(text = controller.s("merge_action"), enabled = selected.size >= 2) {
                val chosen = controller.profiles.filter { it.id in selected }
                val merged = dev.rooni.zefir.model.Profile(
                    id = controller.newId(),
                    name = controller.s("merge_title"),
                    nodes = chosen.flatMap { it.nodes }.distinctBy { it.key },
                    servers = chosen.sumOf { it.nodes.size },
                    updatedAt = nowMillis(),
                    isMerged = true,
                )
                controller.addProfile(merged)
                onBack()
            }
        }
    }
}
