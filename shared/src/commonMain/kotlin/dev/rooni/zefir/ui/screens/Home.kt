package dev.rooni.zefir.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.Tab
import dev.rooni.zefir.model.TunnelState
import dev.rooni.zefir.ui.formatBytes
import dev.rooni.zefir.ui.formatPing

/** Главный экран (ui/screens/Home.kt): большая кнопка + виджеты. */
@Composable
fun HomeScreen(controller: AppController, onAdd: () -> Unit, onServers: () -> Unit, onSettings: () -> Unit) {
    val settings = controller.settings
    val tunnel = controller.tunnel
    val hasProfile = controller.nodes.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Edit, contentDescription = controller.s("home_edit"), modifier = Modifier.size(20.dp))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectHero(
                state = tunnel,
                hasProfile = hasProfile,
                label = when {
                    tunnel is TunnelState.Failed -> controller.s("home_failed")
                    tunnel is TunnelState.On -> controller.s("home_on")
                    tunnel is TunnelState.Starting -> controller.s("home_starting")
                    tunnel is TunnelState.Stopping -> controller.s("home_stopping")
                    else -> controller.s("home_off")
                },
                hint = when {
                    tunnel is TunnelState.On -> controller.s("home_on_hint")
                    tunnel is TunnelState.Failed -> controller.s("home_failed_hint")
                    !hasProfile -> controller.s("home_no_profile")
                    else -> controller.s("home_off_hint")
                },
                onClick = {
                    if (!hasProfile) onAdd() else controller.toggleTunnel()
                },
            )

            Spacer(Modifier.height(24.dp))

            if (hasProfile) {
                ServerWidget(controller, onServers)
                Spacer(Modifier.height(10.dp))
                SubscriptionWidget(controller, onServers)
            } else {
                NavCard(
                    icon = Icons.Rounded.Dns,
                    title = controller.s("w_server"),
                    subtitle = controller.s("w_server_none"),
                    onClick = onServers,
                )
                Spacer(Modifier.height(10.dp))
                NavCard(
                    icon = Icons.Rounded.Add,
                    title = controller.s("add_action"),
                    subtitle = controller.s("servers_empty_text"),
                    onClick = onAdd,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConnectHero(
    state: TunnelState,
    hasProfile: Boolean,
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        if (state is TunnelState.On) primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "hero",
    )
    val content by animateColorAsState(
        if (state is TunnelState.On) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        label = "heroIcon",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
        Box(
            Modifier
                .size(196.dp)
                .clip(CircleShape)
                .background(container)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(76.dp),
            )
            if (state is TunnelState.Starting || state is TunnelState.Stopping) {
                Canvas(Modifier.size(196.dp)) {
                    drawCircle(
                        color = primary.copy(alpha = 0.35f),
                        radius = size.minDimension / 2 - 6f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun ServerWidget(controller: AppController, onServers: () -> Unit) {
    val node = controller.activeNode
    NavCard(
        icon = Icons.Rounded.Public,
        title = controller.s("w_server"),
        subtitle = node?.name ?: controller.s("w_server_none"),
        trailing = node?.let { formatPing(it.latency) },
        onClick = onServers,
    )
}

@Composable
private fun SubscriptionWidget(controller: AppController, onOpen: () -> Unit) {
    val profile = controller.activeProfile
    val subtitle = when {
        profile == null -> controller.s("w_server_none")
        profile.hasTrafficLimit -> controller.sf("w_sub_used", formatBytes(profile.used), formatBytes(profile.total))
        else -> controller.s("w_sub_unlimited")
    }
    NavCard(
        icon = Icons.Rounded.Dns,
        title = controller.s("w_subscription"),
        subtitle = subtitle,
        onClick = onOpen,
    )
}

@Composable
fun NavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}