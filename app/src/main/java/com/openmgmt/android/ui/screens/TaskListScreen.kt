package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.TextInputDialog

private enum class TaskFilter(val label: String) {
    ALL("All"), OPEN("Open"), IN_PROGRESS("In progress"), DONE("Done")
}

/** Full task list with filters and a new-task action. */
@Composable
fun TaskListScreen(viewModel: MainViewModel, onOpenSync: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var showNewTask by remember { mutableStateOf(false) }

    val visible = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.OPEN -> tasks.filter { it.status == TaskStatus.OPEN }
        TaskFilter.IN_PROGRESS -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        TaskFilter.DONE -> tasks.filter { it.status == TaskStatus.DONE }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PageHeader(
                    eyebrow = "WORKSPACE",
                    title = "Tasks",
                    description = "Everything on your plate, synced across devices.",
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskFilter.entries.forEach { entry ->
                        FilterChip(
                            selected = filter == entry,
                            onClick = { filter = entry },
                            label = { Text(entry.label) },
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        "No tasks here",
                        if (tasks.isEmpty()) "Create your first task to get started."
                        else "Nothing matches this filter.",
                    )
                }
            } else {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        projectName = viewModel.projectName(task.projectId),
                        onToggleDone = {
                            viewModel.setTaskStatus(
                                task,
                                if (task.status == TaskStatus.DONE) TaskStatus.OPEN else TaskStatus.DONE,
                            )
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { showNewTask = true }, modifier = Modifier.weight(1f)) {
                Text("New task")
            }
            Button(onClick = onOpenSync, modifier = Modifier.weight(1f)) {
                Text("Sync settings")
            }
        }
    }

    if (showNewTask) {
        TextInputDialog(
            title = "New task",
            label = "Task title",
            onDismiss = { showNewTask = false },
            onConfirm = { viewModel.saveTask(TaskEntity(title = it)) },
        )
    }
}
