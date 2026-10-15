package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.Urgency
import com.openmgmt.android.data.urgency
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun BoardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val actions = LocalTaskActions.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val preset = context.getSharedPreferences("board", 0).getString("preset", "Balanced")
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(60_000) } }
    val groups = tasks.groupBy { urgency(it, now) }
    ScreenFab("New task") { actions.create(TaskEntity(title = "")) }
    Column(Modifier.fillMaxSize()) {
        FilterRow(
            options = listOf("All") + Urgency.entries.map { it.name },
            selected = selected ?: "All",
            label = { key -> Urgency.entries.find { it.name == key }?.let {
                "${it.label} ${groups[it].orEmpty().size}"
            } ?: "All" },
            onSelect = { selected = it.takeUnless { key -> key == "All" } },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = screenPadding(hasFab = true),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Urgency.entries.filter { selected == null || it.name == selected }.forEach { bucket ->
                val visible = groups[bucket].orEmpty().sortedWith(
                    if (preset == "Focus") compareBy<TaskEntity> { it.priority }.thenBy { it.dueAt ?: Long.MAX_VALUE }
                    else compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE }.thenBy { it.priority }
                )
                item(key = bucket.name) { SectionHeader(bucket.label, visible.size) }
                items(visible, key = { it.id }) { task ->
                    TaskCard(task, projectNames[task.projectId], { actions.toggleDone(task) }, { actions.edit(task) })
                }
            }
        }
    }
}
