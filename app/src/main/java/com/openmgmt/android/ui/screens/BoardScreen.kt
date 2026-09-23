package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.TaskCard

/** Board: tasks grouped into status columns, like the desktop TV board. */
@Composable
fun BoardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var column by remember { mutableStateOf(TaskStatus.OPEN) }

    val columns = listOf(
        TaskStatus.OPEN to "To do",
        TaskStatus.IN_PROGRESS to "In progress",
        TaskStatus.BLOCKED to "Blocked",
        TaskStatus.DONE to "Done",
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader(
            eyebrow = "OPERATIONS",
            title = "Board",
            description = "Tasks by status.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { (status, label) ->
                val count = tasks.count { it.status == status }
                FilterChip(
                    selected = column == status,
                    onClick = { column = status },
                    label = { Text("$label ($count)") },
                )
            }
        }
        val visible = tasks.filter { it.status == column }
        if (visible.isEmpty()) {
            EmptyState("No tasks", "Nothing in this column.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}
