package com.openmgmt.android.ui.nav

/**
 * Navigation destinations mirroring the desktop sidebar
 * (apps/desktop/ui/src/app/mod.rs).
 */
sealed class Destination(
    val route: String,
    val title: String,
    val group: NavGroup?,
) {
    data object Dashboard : Destination("dashboard", "Dashboard", NavGroup.WORKSPACE)
    data object DailyOps : Destination("daily_ops", "Daily Operations", NavGroup.WORKSPACE)
    data object Tasks : Destination("tasks", "Tasks", NavGroup.WORKSPACE)
    data object Schedule : Destination("schedule", "Schedule", NavGroup.WORKSPACE)
    data object Projects : Destination("projects", "Projects", NavGroup.STRUCTURE)
    data object Organizations : Destination("organizations", "Organizations", NavGroup.STRUCTURE)
    data object Board : Destination("board", "Board", NavGroup.OPERATIONS)
    data object Sync : Destination("sync", "Sync", NavGroup.OPERATIONS)
    data object Settings : Destination("settings", "Settings", NavGroup.OPERATIONS)
}

enum class NavGroup(val label: String) {
    WORKSPACE("WORKSPACE"),
    STRUCTURE("STRUCTURE"),
    OPERATIONS("OPERATIONS"),
}

val ALL_DESTINATIONS: List<Destination> = listOf(
    Destination.Dashboard,
    Destination.DailyOps,
    Destination.Tasks,
    Destination.Schedule,
    Destination.Projects,
    Destination.Organizations,
    Destination.Board,
    Destination.Sync,
    Destination.Settings,
)
