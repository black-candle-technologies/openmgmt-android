package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.*
import java.time.LocalDate

@Composable
fun DailyOpsScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val names by viewModel.projectNames.collectAsState()
    val defaultId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current
    var showDone by rememberSaveable { mutableStateOf(false) }
    val today = startOfToday()
    val tomorrow = startOfDay(LocalDate.now().plusDays(1))
    val open = tasks.filter { it.status !in setOf(TaskStatus.DONE, TaskStatus.CANCELED) }
    val focus = open.filter { it.status !in setOf(TaskStatus.BLOCKED, TaskStatus.WAITING) }
        .sortedWith(compareBy<TaskEntity> { if (it.status == TaskStatus.IN_PROGRESS) 0 else 1 }.thenBy { it.dueAt ?: Long.MAX_VALUE }.thenBy { it.priority }).firstOrNull()
    val rest = open.filter { it.id != focus?.id }
    val overdue = rest.filter { isOverdue(it) }
    val waiting = rest.filter { !isOverdue(it) && it.status in setOf(TaskStatus.BLOCKED, TaskStatus.WAITING) }
    val scheduled = rest.filter { !isOverdue(it) && it !in waiting && it.dueAt != null && it.dueAt < tomorrow }
    val done = tasks.filter { it.status == TaskStatus.DONE && (it.completedAt ?: 0) >= today && (it.completedAt ?: 0) < tomorrow }
    ScreenFab("New task") { actions.create(TaskEntity(title = "", dueAt = today)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = screenPadding(hasFab = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item("now") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("NOW", style = MaterialTheme.typography.labelSmall)
                    Text(focus?.title ?: "All clear", style = MaterialTheme.typography.titleLarge)
                    if (focus != null) Row {
                        TextButton(onClick = { viewModel.setTaskStatus(focus, TaskStatus.IN_PROGRESS) }, enabled = focus.status != TaskStatus.IN_PROGRESS) { Text("Start") }
                        Button(onClick = { actions.toggleDone(focus) }) { Text("Complete") }
                    }
                }
            }
        }
        taskSection("overdue", "Overdue", overdue, names, defaultId, actions, "Nothing overdue", "")
        taskSection("scheduled", "Scheduled today", scheduled, names, defaultId, actions, "Nothing scheduled", "")
        taskSection("waiting", "Blocked / waiting", waiting, names, defaultId, actions, "No blocked work", "")
        item("done-toggle") { TextButton(onClick = { showDone = !showDone }) { Text("Done today (${done.size}) ${if (showDone) "−" else "+"}") } }
        if (showDone) taskSection("done", "Done today", done, names, defaultId, actions, "No completions yet", "")
    }
}
