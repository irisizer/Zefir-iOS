package dev.rooni.zefir.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.platform.nowMillis
import dev.rooni.zefir.model.HomeWidget
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.model.TunnelState
import dev.rooni.zefir.ui.AppTheme
import dev.rooni.zefir.ui.LocalIconTints
import dev.rooni.zefir.ui.Navigator
import dev.rooni.zefir.ui.Route
import dev.rooni.zefir.ui.formatBytes
import dev.rooni.zefir.ui.formatPing
import dev.rooni.zefir.ui.formatSession
import dev.rooni.zefir.ui.rememberNow

@Composable
fun HomeScreen(controller: AppController, navigator: Navigator) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        HomeHeader(controller)
        Spacer(Modifier.height(8.dp))
        ConnectHero(controller)
        Spacer(Modifier.height(20.dp))
        androidx.compose.runtime.LaunchedEffect(Unit) { controller.refreshExitIp() }
        val widgets = controller.settings.widgets.ifEmpty { HomeWidget.defaultOrder }
        widgets.forEachIndexed { index, widget ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            WidgetFrame(index = index, widget = widget, controller = controller, navigator = navigator)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(controller: AppController) {
    val profile = controller.activeProfile
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                controller.s("app_title"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                profile?.name ?: controller.s("servers_empty_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (controller.profiles.size > 1) {
            Text(
                controller.sf("servers_count", controller.profiles.sumOf { it.servers }),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Главная кнопка подключения (HomeKt.ConnectHero): крупный «пузырь» с морфингом,
 * который меняет цвет по состоянию туннеля.
 */
@Composable
fun ConnectHero(controller: AppController) {
    val state = controller.tunnel
    val on = state is TunnelState.On
    val busy = state is TunnelState.Starting || state is TunnelState.Stopping
    val failed = state is TunnelState.Failed

    val targetColor = when {
        on -> MaterialTheme.colorScheme.primary
        failed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val container by animateColorAsState(targetColor, label = "heroColor")
    val contentColor = when {
        on -> MaterialTheme.colorScheme.onPrimary
        failed -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else if (on) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 780f),
        label = "heroScale",
    )
    // Морфинг формы: круг на выключенном, «капля» на включённом.
    val morph by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 240f),
        label = "heroMorph",
    )
    val shape = RoundedCornerShape(
        topStart = lerp(84.dp, 46.dp, morph),
        topEnd = lerp(84.dp, 84.dp, morph),
        bottomEnd = lerp(84.dp, 46.dp, morph),
        bottomStart = lerp(84.dp, 84.dp, morph),
    )
    val glowAlpha by animateFloatAsState(if (on) 0.30f else 0.10f, label = "glow")

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(248.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), Color.Transparent),
                        ),
                        CircleShape,
                    )
            )
            Box(
                Modifier
                    .size(196.dp)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                container.copy(alpha = 0.55f),
                                container,
                                container.copy(alpha = 0.75f),
                                container.copy(alpha = 0.55f),
                            ),
                        ),
                        shape,
                    )
            )
            Surface(
                shape = shape,
                color = Color.Transparent,
                contentColor = contentColor,
                modifier = Modifier
                    .size(172.dp)
                    .scale(scale)
                    .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) {
                        controller.toggle()
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = contentColor,
                            strokeWidth = 4.dp,
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                on -> Icons.Rounded.Check
                                failed -> Icons.Rounded.Bolt
                                else -> Icons.Rounded.PowerSettingsNew
                            },
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(60.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (state) {
                is TunnelState.On -> controller.s("home_on")
                is TunnelState.Starting -> controller.s("home_starting")
                is TunnelState.Stopping -> controller.s("home_stopping")
                is TunnelState.Failed -> controller.s("home_failed")
                else -> controller.s("home_off")
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (state) {
                is TunnelState.On -> controller.activeNode?.name?.let { dev.rooni.zefir.app.Links.withoutFlag(it) }
                    ?: controller.s("home_on_hint")
                is TunnelState.Failed -> state.message
                else -> if (controller.nodes.isEmpty()) controller.s("home_no_profile") else controller.s("home_off_hint")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WidgetFrame(index: Int, widget: HomeWidget, controller: AppController, navigator: Navigator) {
    val tint = LocalIconTints.current[index % LocalIconTints.current.size]
    when (widget) {
        HomeWidget.Traffic -> WidgetCard(index, controller.s("w_traffic"), Icons.Rounded.Timer) {
            TrafficBody(controller)
        }
        HomeWidget.Server -> WidgetCard(
            index,
            controller.s("w_server"),
            Icons.Rounded.Dns,
            onClick = {
                controller.tab = Tab.Servers
                navigator.select(Tab.Servers)
            },
        ) {
            ServerBody(controller)
        }
        HomeWidget.Subscription -> WidgetCard(index, controller.s("w_subscription"), Icons.Rounded.Cloud) {
            SubscriptionBody(controller)
        }
        HomeWidget.Speed -> WidgetCard(index, controller.s("w_speed"), Icons.Rounded.Speed) {
            SpeedBody(controller)
        }
        HomeWidget.Ip -> WidgetCard(
            index,
            controller.s("w_ip"),
            Icons.Rounded.Language,
            onClick = { controller.refreshExitIp() },
        ) {
            Text(
                text = when {
                    controller.ipLoading -> controller.s("w_ip_loading")
                    controller.exitIp != null -> controller.exitIp!!
                    else -> controller.s("w_ip_none")
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HomeWidget.Mode -> WidgetCard(
            index,
            controller.s("w_mode"),
            Icons.Rounded.Bolt,
            onClick = { navigator.push(Route.Basic) },
        ) {
            Text(
                text = when (controller.settings.mode) {
                    dev.rooni.zefir.model.RoutingMode.Smart -> controller.s("mode_smart")
                    dev.rooni.zefir.model.RoutingMode.Global -> controller.s("mode_global")
                    dev.rooni.zefir.model.RoutingMode.Direct -> controller.s("mode_direct")
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        HomeWidget.SplitApps -> WidgetCard(
            index,
            controller.s("w_apps"),
            Icons.Rounded.WifiTethering,
            onClick = { navigator.push(Route.Apps) },
        ) {
            Text(
                text = if (controller.settings.splitMode == dev.rooni.zefir.model.SplitMode.Off) {
                    controller.s("w_apps_off")
                } else {
                    controller.sf("w_apps_count", controller.settings.excludedApps.size)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun WidgetCard(
    index: Int,
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val tint = LocalIconTints.current[index % LocalIconTints.current.size]
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).background(tint.container, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = tint.onContainer, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun TrafficBody(controller: AppController) {
    val now = rememberNow()
    val start = controller.sessionStart
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatColumn(
            value = if (start != null) formatSession(now - start) else "—",
            label = controller.s("w_session_time"),
            modifier = Modifier.weight(1f),
        )
        StatColumn(
            value = "↑ ${formatBytes(controller.traffic.up)}",
            label = controller.s("w_upload"),
            modifier = Modifier.weight(1f),
        )
        StatColumn(
            value = "↓ ${formatBytes(controller.traffic.down)}",
            label = controller.s("w_download"),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ServerBody(controller: AppController) {
    val node = controller.activeNode
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = node?.flag ?: "🌐",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = node?.let { dev.rooni.zefir.app.Links.withoutFlag(it.name) } ?: controller.s("w_server_none"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val ping = node?.let { controller.pings[it.name] }
            Text(
                text = if (node == null) controller.s("servers_empty_title") else "${node.type.uppercase()} · ${formatPing(ping)} ${controller.s("servers_ms").replace("%1\$d", "").trim()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubscriptionBody(controller: AppController) {
    val profile = controller.activeProfile
    if (profile == null || (profile.total <= 0 && profile.expire <= 0)) {
        Text(
            controller.s("w_sub_unlimited"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        Text(
            controller.sf("w_sub_used", formatBytes(profile.used), formatBytes(profile.total)),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        val progress = if (profile.total > 0) (profile.used.toFloat() / profile.total).coerceIn(0f, 1f) else 0f
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (profile.expire > 0) {
                val days = ((profile.expire * 1000) - nowMillis()) / 86_400_000
                if (days < 0) controller.s("extra_expired") else controller.sf("stat_days", days) + " · " + controller.s("stat_left")
            } else {
                controller.s("stat_no_term")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SpeedBody(controller: AppController) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatColumn("↑ ${formatBytes(controller.traffic.upSpeed)}/с", controller.s("w_upload"), Modifier.weight(1f))
        StatColumn("↓ ${formatBytes(controller.traffic.downSpeed)}/с", controller.s("w_download"), Modifier.weight(1f))
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
