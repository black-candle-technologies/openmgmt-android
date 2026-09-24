package com.openmgmt.android.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.openmgmt.android.ui.SyncViewModel
import com.openmgmt.android.ui.nav.ALL_DESTINATIONS
import com.openmgmt.android.ui.nav.Destination
import com.openmgmt.android.ui.nav.NavGroup
import com.openmgmt.android.ui.theme.DrawerColors
import kotlinx.coroutines.launch

/** A screen's primary action, rendered by [AppScaffold] as an extended FAB. */
class FabSpec(val label: String, val onClick: () -> Unit)

class FabController {
    var spec by mutableStateOf<FabSpec?>(null)
}

private val LocalFabController = staticCompositionLocalOf { FabController() }

/**
 * Declares the current screen's FAB. Hosting it in the shared Scaffold (not
 * inside the screen) keeps snackbars stacked above it instead of on top.
 */
@Composable
fun ScreenFab(label: String, onClick: () -> Unit) {
    val controller = LocalFabController.current
    val latest by rememberUpdatedState(onClick)
    DisposableEffect(controller, label) {
        val spec = FabSpec(label) { latest() }
        controller.spec = spec
        onDispose { if (controller.spec === spec) controller.spec = null }
    }
}

/**
 * App shell mirroring the desktop layout: a charcoal navigation drawer
 * (brand + grouped nav + "Local database" footer) and a top bar with the
 * page eyebrow and title, a sync status pill, and a Sync action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    current: Destination,
    onNavigate: (Destination) -> Unit,
    sync: SyncViewModel.State,
    onSync: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val fab = remember { FabController() }
    // Fresh scroll state per page, so a new page doesn't open "scrolled".
    val appBarState = key(current) { rememberTopAppBarState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)

    // The top bar reads these through State: a new topBar lambda alone isn't
    // re-subcomposed until Scaffold remeasures, which left the title stale
    // after navigating from page content.
    val currentState = rememberUpdatedState(current)
    val syncState = rememberUpdatedState(sync)

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    DrawerStatusBarIcons(drawerOpen = drawerState.targetValue == DrawerValue.Open)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 300.dp),
                drawerContainerColor = DrawerColors.background,
                drawerContentColor = DrawerColors.onBackground,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            ) {
                DrawerContent(
                    current = current,
                    canSync = sync.canSync,
                    onNavigate = {
                        onNavigate(it)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        val page = currentState.value
                        Column {
                            Text(
                                page.eyebrow,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                page.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation")
                        }
                    },
                    actions = {
                        val sync = syncState.value
                        StatusPill(sync, onClick = { onNavigate(Destination.Sync) })
                        if (sync.activity == SyncViewModel.Activity.Syncing) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        } else if (sync.canSync) {
                            IconButton(onClick = onSync, enabled = sync.activity == null) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
                            }
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                fab.spec?.let { spec ->
                    ExtendedFloatingActionButton(
                        onClick = spec.onClick,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(spec.label) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                CompositionLocalProvider(LocalFabController provides fab) {
                    content()
                }
            }
        }
    }
}

/** Light status-bar icons over the charcoal drawer; restore the theme's when it closes. */
@Composable
private fun DrawerStatusBarIcons(drawerOpen: Boolean) {
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    if (view.isInEditMode) return
    LaunchedEffect(drawerOpen, dark) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            !drawerOpen && !dark
    }
}

@Composable
private fun StatusPill(sync: SyncViewModel.State, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (text, container, content) = when {
        sync.activity == SyncViewModel.Activity.Syncing ->
            Triple("Syncing", scheme.surfaceVariant, scheme.onSurfaceVariant)
        sync.activity == SyncViewModel.Activity.SigningIn ->
            Triple("Signing in", scheme.surfaceVariant, scheme.onSurfaceVariant)
        sync.isError -> Triple("Error", scheme.errorContainer, scheme.onErrorContainer)
        !sync.canSync -> Triple("Local only", scheme.surfaceVariant, scheme.onSurfaceVariant)
        sync.lastSyncedAt != null -> Triple("Up to date", scheme.primary, scheme.onPrimary)
        else -> Triple("Not synced", scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(onClickLabel = "Open sync", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ColumnScope.DrawerContent(
    current: Destination,
    canSync: Boolean,
    onNavigate: (Destination) -> Unit,
) {
    // Brand: lime "OM" mark + OpenMgmt / OPERATIONS DESK, like the desktop sidebar.
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(DrawerColors.brandMarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text("OM", color = DrawerColors.brandMarkText, fontWeight = FontWeight.Bold)
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

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        var lastGroup: NavGroup? = null
        for (destination in ALL_DESTINATIONS) {
            if (destination.group != lastGroup) {
                lastGroup = destination.group
                Text(
                    lastGroup.label,
                    color = DrawerColors.muted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                )
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
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(DrawerColors.statusDot))
        Text(
            if (canSync) "Local database · sync on" else "Local database",
            color = DrawerColors.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DrawerItem(
    destination: Destination,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (active) DrawerColors.activeBackground else DrawerColors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lime marker on the active page, echoing the brand mark.
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(if (active) DrawerColors.brandMarkBackground else DrawerColors.background)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            destination.title,
            color = if (active) DrawerColors.onBackground else DrawerColors.muted,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
