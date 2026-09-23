package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val BOARD_COLUMNS = listOf(
    TaskStatus.OPEN to "To do",
    TaskStatus.IN_PROGRESS to "In progress",
    TaskStatus.BLOCKED to "Blocked",
    TaskStatus.DONE to "Done",
)

/** Board: tasks grouped into status columns, like the desktop TV board. */
@Composable
fun BoardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val actions = LocalTaskActions.current
    var column by rememberSaveable { mutableStateOf(TaskStatus.OPEN) }
    val columnLabel = BOARD_COLUMNS.first { it.first == column }.second

    ScreenFab("New task") { actions.create(TaskEntity(title = "", status = column)) }

    Column(Modifier.fillMaxSize()) {
        FilterRow(
            options = BOARD_COLUMNS.map { it.first },
            selected = column,
            label = { status ->
                "${BOARD_COLUMNS.first { it.first == status }.second} ${tasks.count { it.status == status }}"
            },
            onSelect = { column = it },
        )
        val visible = tasks.filter { it.status == column }.sortedBy { it.dueAt ?: Long.MAX_VALUE }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState("Nothing in $columnLabel", "Tap a task to change its status.")
                }
            } else {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        projectName = projectNames[task.projectId],
                        onToggleDone = { actions.toggleDone(task) },
                        onClick = { actions.edit(task) },
                        showStatus = false,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
