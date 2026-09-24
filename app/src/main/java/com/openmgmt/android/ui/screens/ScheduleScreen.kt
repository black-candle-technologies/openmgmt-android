package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.components.startOfToday

/** Schedule: overdue, today, this week, later, and unscheduled tasks. */
@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val defaultProjectId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val today = startOfToday()
    val tomorrow = today + 86_400_000L
    val weekEnd = today + 7 * 86_400_000L

    val overdue = open.filter { isOverdue(it) }.sortedBy { it.dueAt }
    val todayTasks = open.filter { it.dueAt != null && it.dueAt >= today && it.dueAt < tomorrow }
    val thisWeek = open.filter { it.dueAt != null && it.dueAt >= tomorrow && it.dueAt < weekEnd }
        .sortedBy { it.dueAt }
    val later = open.filter { it.dueAt != null && it.dueAt >= weekEnd }.sortedBy { it.dueAt }
    val unscheduled = open.filter { it.dueAt == null }

    val overdueColor = if (overdue.isEmpty()) Color.Unspecified else MaterialTheme.colorScheme.error

    ScreenFab("New task") { actions.create(TaskEntity(title = "")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (open.isEmpty()) {
            item(key = "empty") {
                EmptyState("Nothing on the schedule", "Open tasks will show up here by due date.")
            }
        }
        // Overdue and Today always show so the day's status is explicit;
        // the rest only appear when they have tasks.
        taskSection(
            key = "overdue", title = "Overdue", tasks = overdue,
            projectNames = projectNames, defaultProjectId = defaultProjectId, actions = actions,
            emptyTitle = if (open.isEmpty()) null else "Nothing overdue",
            emptyHint = "You're caught up.",
            titleColor = overdueColor,
            first = true,
        )
        taskSection(
            key = "today", title = "Today", tasks = todayTasks,
            projectNames = projectNames, defaultProjectId = defaultProjectId, actions = actions,
            emptyTitle = if (open.isEmpty()) null else "Nothing due today",
            emptyHint = "The day is yours.",
        )
        taskSection(
            key = "week", title = "Next 7 days", tasks = thisWeek,
            projectNames = projectNames, defaultProjectId = defaultProjectId, actions = actions,
        )
        taskSection(
            key = "later", title = "Later", tasks = later,
            projectNames = projectNames, defaultProjectId = defaultProjectId, actions = actions,
        )
        taskSection(
            key = "unscheduled", title = "Unscheduled", tasks = unscheduled,
            projectNames = projectNames, defaultProjectId = defaultProjectId, actions = actions,
        )
    }
}
