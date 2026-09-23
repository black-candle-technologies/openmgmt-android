package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.Section
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.startOfToday

/** Schedule: overdue, today, this week, and unscheduled tasks. */
@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val today = startOfToday()
    val weekEnd = today + 7 * 86_400_000L

    val overdue = open.filter { isOverdue(it) }.sortedBy { it.dueAt }
    val todayTasks = open.filter { it.dueAt != null && it.dueAt >= today && it.dueAt < today + 86_400_000 }
    val thisWeek = open.filter { it.dueAt != null && it.dueAt >= today + 86_400_000 && it.dueAt < weekEnd }
        .sortedBy { it.dueAt }
    val unscheduled = open.filter { it.dueAt == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "WORKSPACE",
                title = "Schedule",
                description = "When everything is due, at a glance.",
            )
        }
        item {
            ScheduleSection(
                title = "Overdue",
                tasks = overdue,
                emptyTitle = "Nothing overdue",
                emptyHint = "You're caught up.",
                viewModel = viewModel,
            )
        }
        item {
            ScheduleSection(
                title = "Today",
                tasks = todayTasks,
                emptyTitle = "Nothing due today",
                emptyHint = "The day is yours.",
                viewModel = viewModel,
            )
        }
        item {
            ScheduleSection(
                title = "This week",
                tasks = thisWeek,
                emptyTitle = "Nothing this week",
                emptyHint = "No upcoming due dates.",
                viewModel = viewModel,
            )
        }
        item {
            ScheduleSection(
                title = "Unscheduled",
                tasks = unscheduled,
                emptyTitle = "Nothing unscheduled",
                emptyHint = "Every task has a due date.",
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun ScheduleSection(
    title: String,
    tasks: List<TaskEntity>,
    emptyTitle: String,
    emptyHint: String,
    viewModel: MainViewModel,
) {
    Section(title) {
        if (tasks.isEmpty()) {
            EmptyState(emptyTitle, emptyHint)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        projectName = viewModel.projectName(task.projectId),
                        onToggleDone = { viewModel.setTaskStatus(task, TaskStatus.DONE) },
                    )
                }
            }
        }
    }
}
