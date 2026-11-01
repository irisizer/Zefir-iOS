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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.ui.formatPing
import dev.rooni.zefir.ui.positionShape

/** Экран серверов (ui/screens/Servers.kt). */
@Composable
fun ServersScreen(controller: AppController, onAdd: () -> Unit) {
    val profile = controller.activeProfile
    val nodes = controller.nodes
    val sorted = when (controller.settings.serverSort) {
        dev.rooni.zefir.model.ServerSort.Name -> nodes.sortedBy { it.name }
        dev.rooni.zefir.model.ServerSort.Ping -> nodes.sortedBy { it.latency ?: Int.MAX_VALUE }
        else -> nodes
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (nodes.isEmpty()) {
            EmptyServers(controller, onAdd)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp,
                ),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                profile?.name ?: controller.s("tab_servers"),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                controller.s("servers_count").replace("%1\$d", nodes.size.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { controller.pingAll() },
                        ) {
                            Icon(
                                Icons.Rounded.Speed,
                                contentDescription = controller.s("servers_test"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp).size(22.dp),
                            )
                        }
                    }
                }
                items(sorted) { node ->
                    val index = sorted.indexOf(node)
                    ServerRow(
                        node = node,
                        selected = controller.activeNode?.name == node.name,
                        shape = positionShape(index, sorted.lastIndex),
                        onClick = { controller.selectNode(node) },
                    )
                }
            }
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(controller.s("servers_add")) },
            )
        }
    }
}

@Composable
private fun ServerRow(node: ProxyNode, selected: Boolean, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlagBox(node.flag)
            Column(Modifier.weight(1f)) {
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${node.type.uppercase()} · ${node.host}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatPing(node.latency),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FlagBox(flag: String?) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(flag ?: "•", fontSize = 20.sp)
    }
}

@Composable
private fun EmptyServers(controller: AppController, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            controller.s("servers_empty_title"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            controller.s("servers_empty_text"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp).clickable { onAdd() },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    controller.s("servers_add"),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}