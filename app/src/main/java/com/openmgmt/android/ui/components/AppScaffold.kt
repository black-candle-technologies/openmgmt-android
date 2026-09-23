package com.openmgmt.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openmgmt.android.ui.nav.ALL_DESTINATIONS
import com.openmgmt.android.ui.nav.Destination
import com.openmgmt.android.ui.nav.NavGroup
import com.openmgmt.android.ui.theme.DrawerColors
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh

/**
 * App shell mirroring the desktop layout: a charcoal navigation drawer
 * (brand + grouped nav + "Local database" footer) and a top bar with the
 * page title, a status pill, and a Refresh action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    current: Destination,
    onNavigate: (Destination) -> Unit,
    status: SyncStatus = SyncStatus.UpToDate,
    onRefresh: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = current,
                onNavigate = {
                    onNavigate(it)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation")
                        }
                    },
                    actions = {
                        StatusPill(status)
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                content()
            }
        }
    }
}

enum class SyncStatus { UpToDate, Refreshing, Error }

@Composable
private fun StatusPill(status: SyncStatus) {
    val (text, container, content) = when (status) {
        SyncStatus.UpToDate -> Triple(
            "Up to date",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        SyncStatus.Refreshing -> Triple(
            "Refreshing",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SyncStatus.Error -> Triple(
            "Error",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DrawerContent(
    current: Destination,
    onNavigate: (Destination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(DrawerColors.background)
            .padding(vertical = 12.dp),
    ) {
        // Brand: lime "OM" mark + OpenMgmt / OPERATIONS DESK, like the desktop sidebar.
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(DrawerColors.brandMarkBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "OM",
                    color = DrawerColors.brandMarkText,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "OpenMgmt",
                    color = DrawerColors.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "OPERATIONS DESK",
                    color = DrawerColors.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            var lastGroup: NavGroup? = null
            for (destination in ALL_DESTINATIONS) {
                if (destination.group != lastGroup) {
                    lastGroup = destination.group
                    if (lastGroup != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lastGroup.label,
                            color = DrawerColors.muted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                DrawerItem(
                    destination = destination,
                    active = destination == current,
                    onClick = { onNavigate(destination) },
                )
            }
        }

        // Footer: local-database status, like the desktop sidebar foot.
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(DrawerColors.statusDot)
            )
            Text(
                "Local database",
                color = DrawerColors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DrawerItem(
    destination: Destination,
    active: Boolean,
    onClick: () -> Unit,
) {
    val background = if (active) DrawerColors.activeBackground else DrawerColors.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            destination.title,
            color = if (active) DrawerColors.onBackground else DrawerColors.muted,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
