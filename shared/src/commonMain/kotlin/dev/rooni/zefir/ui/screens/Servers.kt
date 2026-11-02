package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.app.Links
import dev.rooni.zefir.model.Profile
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.model.ServerDensity
import dev.rooni.zefir.model.ServerLayout
import dev.rooni.zefir.model.ServerSort
import dev.rooni.zefir.ui.AppDialog
import dev.rooni.zefir.ui.AppSheet
import dev.rooni.zefir.ui.Navigator
import dev.rooni.zefir.ui.Route
import dev.rooni.zefir.ui.formatBytes
import dev.rooni.zefir.ui.formatPing
import dev.rooni.zefir.ui.rememberNow
import kotlin.math.roundToInt

@Composable
fun ServersScreen(controller: AppController, navigator: Navigator) {
    val settings = controller.settings
    var query by remember { mutableStateOf("") }
    var menuNode by remember { mutableStateOf<ProxyNode?>(null) }
    var menuProfile by remember { mutableStateOf<Profile?>(null) }
    var sortSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Profile?>(null) }

    val profile = controller.activeProfile
    val all = profile?.nodes.orEmpty()
    val filtered = remember(query, all) {
        if (query.isBlank()) all else all.filter { it.name.contains(query, ignoreCase = true) }
    }
    val sorted = remember(filtered, settings.serverSort, settings.serverSortDesc, controller.pings.toMap()) {
        val withPin: (List<ProxyNode>) -> List<ProxyNode> = { list ->
            list.sortedWith(compareByDescending<ProxyNode> { profile?.pinned?.contains(it.key) == true })
        }
        val base = when (settings.serverSort) {
            ServerSort.Default -> filtered
            ServerSort.Name -> filtered.sortedBy { Links.withoutFlag(it.name) }
            ServerSort.Ping -> filtered.sortedWith(
                compareBy { controller.pings[it.name] ?: Int.MAX_VALUE }
            )
        }
        val ordered = if (settings.serverSortDesc && settings.serverSort != ServerSort.Default) base.reversed() else base
        withPin(ordered)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                controller.s("tab_servers"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { navigator.push(Route.Add) }) {
                Icon(Icons.Rounded.Add, contentDescription = controller.s("servers_add"))
            }
            IconButton(onClick = { sortSheet = true }) {
                Icon(Icons.Rounded.Sort, contentDescription = controller.s("sort_title"))
            }
            IconButton(onClick = profile?.let { { controller.refresh(it) } } ?: {}) {
                Icon(Icons.Rounded.Refresh, contentDescription = controller.s("servers_update"))
            }
        }

        if (all.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(controller.s("search_hint")) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (profile != null) ProfileStrip(controller, profile, navigator, onDelete = { deleteTarget = it })

        when {
            all.isEmpty() -> EmptyServers(controller) { navigator.push(Route.Add) }
            else -> {
                AutoSelectRow(controller)
                if (settings.serverLayout == ServerLayout.Grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(sorted, key = { it.key }) { node ->
                            ServerTile(controller, node) { menuNode = it }
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(sorted, key = { it.key }) { node ->
                            ServerRow(controller, node) { menuNode = it }
                        }
                    }
                }
            }
        }
    }

    menuNode?.let { node ->
        AppSheet(title = Links.withoutFlag(node.name), onClose = { menuNode = null }) {
            SheetAction(Icons.Rounded.Check, controller.s("apply")) {
                controller.selectNode(node); menuNode = null
            }
            SheetAction(Icons.Rounded.ContentCopy, controller.s("server_copy_link")) {
                controller.copyLink(node); menuNode = null
            }
            SheetAction(Icons.Rounded.PushPin, controller.s("server_pin")) {
                profile?.let { controller.togglePin(it, node) }; menuNode = null
            }
            SheetAction(Icons.Rounded.Sort, controller.s("servers_test")) {
                controller.ping(node); menuNode = null
            }
        }
    }

    menuProfile?.let { p ->
        AppSheet(title = p.name, onClose = { menuProfile = null }) {
            SheetAction(Icons.Rounded.Edit, controller.s("servers_rename")) {
                navigator.push(Route.EditProfile(p.id)); menuProfile = null
            }
            SheetAction(Icons.Rounded.Refresh, controller.s("servers_update")) {
                controller.refresh(p); menuProfile = null
            }
            SheetAction(Icons.Rounded.Delete, controller.s("delete")) {
                deleteTarget = p; menuProfile = null
            }
        }
    }

    sortSheet?.let {
        SortSheet(controller) { sortSheet = false }
    }

    deleteTarget?.let { p ->
        AppDialog(
            title = controller.s("servers_delete_q"),
            text = controller.s("servers_delete_text"),
            confirm = controller.s("delete"),
            dismiss = controller.s("cancel"),
            onConfirm = { controller.deleteProfile(p); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SortSheet(controller: AppController, onClose: () -> Unit) {
    val settings = controller.settings
    AppSheet(title = controller.s("sort_title"), onClose = onClose) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(controller.s("sort_default"), settings.serverSort == ServerSort.Default) {
                controller.update { it.copy(serverSort = ServerSort.Default) }
            }
            SortChip(controller.s("sort_name"), settings.serverSort == ServerSort.Name) {
                controller.update { it.copy(serverSort = ServerSort.Name) }
            }
            SortChip(controller.s("sort_ping"), settings.serverSort == ServerSort.Ping) {
                controller.update { it.copy(serverSort = ServerSort.Ping) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(
                controller.s("servers_layout_list"),
                settings.serverLayout == ServerLayout.List,
            ) { controller.update { it.copy(serverLayout = ServerLayout.List) } }
            SortChip(
                controller.s("servers_layout_grid"),
                settings.serverLayout == ServerLayout.Grid,
            ) { controller.update { it.copy(serverLayout = ServerLayout.Grid) } }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(controller.s("servers_density_compact"), settings.serverDensity == ServerDensity.Compact) {
                controller.update { it.copy(serverDensity = ServerDensity.Compact) }
            }
            SortChip(controller.s("servers_density_normal"), settings.serverDensity == ServerDensity.Normal) {
                controller.update { it.copy(serverDensity = ServerDensity.Normal) }
            }
            SortChip(controller.s("servers_density_roomy"), settings.serverDensity == ServerDensity.Roomy) {
                controller.update { it.copy(serverDensity = ServerDensity.Roomy) }
            }
        }
    }
}

@Composable
private fun SortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileStrip(
    controller: AppController,
    active: Profile,
    navigator: Navigator,
    onDelete: (Profile) -> Unit,
) {
    val now = rememberNow(30_000)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        controller.profiles.forEach { profile ->
            val selected = profile.id == active.id
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .combinedClickable(
                        onClick = { controller.selectProfile(profile) },
                        onLongClick = { navigator.push(Route.EditProfile(profile.id)) },
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            controller.sf("servers_count", profile.servers),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (profile.total > 0) {
                        Text(
                            "${formatBytes(profile.total - profile.used)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSelectRow(controller: AppController) {
    val auto = controller.settings.autoSelect
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (auto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { controller.update { it.copy(autoSelect = !auto, activeServer = null) } },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                controller.s("servers_auto"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = if (auto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (auto) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyServers(controller: AppController, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            controller.s("servers_empty_title"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            controller.s("servers_empty_text"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onAdd() },
        ) {
            Text(
                controller.s("servers_add"),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(controller: AppController, node: ProxyNode, onMenu: (ProxyNode) -> Unit) {
    val selected = controller.settings.activeServer == node.name && !controller.settings.autoSelect
    val density = controller.settings.serverDensity
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .combinedClickable(
                onClick = { controller.selectNode(node) },
                onLongClick = { onMenu(node) },
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = (density.rowHeight / 5).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(node.flag ?: "🌐", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Links.withoutFlag(node.name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    node.type.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PingBadge(controller, node)
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerTile(controller: AppController, node: ProxyNode, onMenu: (ProxyNode) -> Unit) {
    val selected = controller.settings.activeServer == node.name && !controller.settings.autoSelect
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.combinedClickable(
            onClick = { controller.selectNode(node) },
            onLongClick = { onMenu(node) },
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(node.flag ?: "🌐", fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                Links.withoutFlag(node.name),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            PingBadge(controller, node)
        }
    }
}

@Composable
private fun PingBadge(controller: AppController, node: ProxyNode) {
    val ping = controller.pings[node.name]
    val color = when {
        ping == null -> MaterialTheme.colorScheme.onSurfaceVariant
        ping < 100 -> Color(0xFF2E7D32)
        ping < 250 -> Color(0xFFF9A825)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text = when {
                controller.pings.containsKey(node.name) && ping == null -> controller.s("servers_dead")
                ping == null -> "—"
                else -> controller.sf("servers_ms", ping)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
