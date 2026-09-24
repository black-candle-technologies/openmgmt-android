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
import com.openmgmt.android.ui.components.LocalContentGutter
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.components.statusLabel

/** Filter chips: All, then each status (null = all). */
private val TASK_FILTERS: List<String?> = listOf(null) + TaskStatus.selectable

private fun filterLabel(status: String?) = status?.let(::statusLabel) ?: "All"

/** Full task list with filters and a new-task action. */
@Composable
fun TaskListScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val defaultProjectId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    // Stable order (edits and check-offs don't make cards jump): open work
    // before done, then soonest due, then title.
    val visible = tasks
        .filter { filter == null || it.status == filter }
        .sortedWith(
            compareBy<TaskEntity>(
                { it.status == TaskStatus.DONE },
                { it.dueAt ?: Long.MAX_VALUE },
                { it.title.lowercase() },
            )
        )

    ScreenFab("New task") {
        actions.create(TaskEntity(title = "", status = filter ?: TaskStatus.INBOX))
    }

    Column(Modifier.fillMaxSize()) {
        FilterRow(
            options = TASK_FILTERS,
            selected = filter,
            label = { status ->
                val count = if (status == null) tasks.size else tasks.count { it.status == status }
                "${filterLabel(status)} $count"
            },
            onSelect = { filter = it },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = screenPadding(hasFab = true, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (tasks.isEmpty()) "No tasks yet" else "No ${filterLabel(filter).lowercase()} tasks",
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
                        inDefaultProject = task.projectId != null && task.projectId == defaultProjectId,
                        showStatus = filter == null,
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
        contentPadding = PaddingValues(horizontal = LocalContentGutter.current, vertical = 4.dp),
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
