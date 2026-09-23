package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.TaskCard

private enum class TaskFilter(val label: String, val status: String?) {
    ALL("All", null),
    OPEN("Open", TaskStatus.OPEN),
    IN_PROGRESS("In progress", TaskStatus.IN_PROGRESS),
    BLOCKED("Blocked", TaskStatus.BLOCKED),
    DONE("Done", TaskStatus.DONE),
}

/** Full task list with filters and a new-task action. */
@Composable
fun TaskListScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val actions = LocalTaskActions.current
    var filter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }

    // Stable order (edits and check-offs don't make cards jump): open work
    // before done, then soonest due, then title.
    val visible = tasks
        .filter { filter.status == null || it.status == filter.status }
        .sortedWith(
            compareBy<TaskEntity>(
                { it.status == TaskStatus.DONE },
                { it.dueAt ?: Long.MAX_VALUE },
                { it.title.lowercase() },
            )
        )

    ScreenFab("New task") {
        actions.create(TaskEntity(title = "", status = filter.status ?: TaskStatus.OPEN))
    }

    Column(Modifier.fillMaxSize()) {
        FilterRow(
            options = TaskFilter.entries,
            selected = filter,
            label = { entry ->
                val count = if (entry.status == null) tasks.size else tasks.count { it.status == entry.status }
                "${entry.label} $count"
            },
            onSelect = { filter = it },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (tasks.isEmpty()) "No tasks yet" else "No ${filter.label.lowercase()} tasks",
                        if (tasks.isEmpty()) "Tap New task to add your first one."
                        else "Nothing matches this filter.",
                    )
                }
            } else {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        projectName = projectNames[task.projectId],
                        onToggleDone = { actions.toggleDone(task) },
                        onClick = { actions.edit(task) },
                        showStatus = filter.status == null,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** Horizontally scrolling chip row, so labels never get squeezed on narrow phones. */
@Composable
fun <T> FilterRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}
