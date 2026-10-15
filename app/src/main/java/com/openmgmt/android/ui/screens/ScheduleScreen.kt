package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun ScheduleScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val names by viewModel.projectNames.collectAsState()
    val defaultId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current
    var undated by rememberSaveable { mutableStateOf(false) }
    val open = tasks.filter { it.status !in setOf(TaskStatus.DONE, TaskStatus.CANCELED) }
    val groups = open.filter { it.dueAt != null }.groupBy { localDate(it.dueAt!!) }.toSortedMap()
    val today = LocalDate.now()
    ScreenFab("New task") { actions.create(TaskEntity(title = "")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = screenPadding(hasFab = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.filterKeys { it < today }.forEach { (date, group) ->
            taskSection(date.toString(), date.toString(), group, names, defaultId, actions)
        }
        item("now") { SectionHeader("NOW · ${today}") }
        groups.filterKeys { it >= today }.forEach { (date, group) ->
            taskSection(date.toString(), date.toString(), group, names, defaultId, actions)
        }
        item("undated") {
            TextButton(onClick = { undated = !undated }) { Text("Undated (${open.count { it.dueAt == null }}) ${if (undated) "−" else "+"}") }
        }
        if (undated) taskSection("unscheduled", "Undated", open.filter { it.dueAt == null }, names, defaultId, actions)
    }
}
