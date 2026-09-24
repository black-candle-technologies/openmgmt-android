package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.openmgmt.android.ui.components.screenPadding

/** A board column groups statuses; new tasks in it get [newStatus]. */
private class BoardColumn(val key: String, val label: String, val statuses: Set<String>, val newStatus: String)

private val BOARD_COLUMNS = listOf(
    BoardColumn(
        "todo", "To do",
        setOf(TaskStatus.INBOX, TaskStatus.BACKLOG, TaskStatus.SCHEDULED, TaskStatus.READY),
        TaskStatus.INBOX,
    ),
    BoardColumn("in_progress", "In progress", setOf(TaskStatus.IN_PROGRESS), TaskStatus.IN_PROGRESS),
    BoardColumn("blocked", "Blocked", setOf(TaskStatus.BLOCKED, TaskStatus.WAITING), TaskStatus.BLOCKED),
    BoardColumn("done", "Done", setOf(TaskStatus.DONE), TaskStatus.DONE),
)

/** Board: tasks grouped into status columns, like the desktop TV board. */
@Composable
fun BoardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val defaultProjectId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current
    var columnKey by rememberSaveable { mutableStateOf(BOARD_COLUMNS.first().key) }
    val column = BOARD_COLUMNS.first { it.key == columnKey }

    ScreenFab("New task") { actions.create(TaskEntity(title = "", status = column.newStatus)) }

    Column(Modifier.fillMaxSize()) {
        FilterRow(
            options = BOARD_COLUMNS,
            selected = column,
            label = { c -> "${c.label} ${tasks.count { it.status in c.statuses }}" },
            onSelect = { columnKey = it.key },
        )
        val visible = tasks.filter { it.status in column.statuses }.sortedBy { it.dueAt ?: Long.MAX_VALUE }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = screenPadding(hasFab = true, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState("Nothing in ${column.label}", "Tap a task to change its status.")
                }
            } else {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        projectName = projectNames[task.projectId],
                        onToggleDone = { actions.toggleDone(task) },
                        onClick = { actions.edit(task) },
                        inDefaultProject = task.projectId != null && task.projectId == defaultProjectId,
                        // Grouped columns show which status each card is in.
                        showStatus = column.statuses.size > 1,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
