package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.Badge
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.MetricCard
import com.openmgmt.android.ui.components.MetricTone
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.Section
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.TextInputDialog
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.nav.Destination

/** Command center: metrics, needs-attention list, active projects, quick actions. */
@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigate: (Destination) -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val organizations by viewModel.organizations.collectAsState()

    var showNewTask by remember { mutableStateOf(false) }
    var showNewProject by remember { mutableStateOf(false) }
    var showNewOrg by remember { mutableStateOf(false) }

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val overdue = open.filter { isOverdue(it) }
    val inProgress = open.filter { it.status == TaskStatus.IN_PROGRESS }
    val blocked = open.filter { it.status == TaskStatus.BLOCKED }
    val attention = (inProgress + overdue + open.filter { it.dueAt != null })
        .distinctBy { it.id }
        .take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "COMMAND CENTER",
                title = "Operations home",
                description = "A live view across every organization, project, and task on this device.",
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("In progress", inProgress.size.toString(), MetricTone.Accent)
                    MetricCard("Overdue", overdue.size.toString(), MetricTone.Danger)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Waiting / blocked", blocked.size.toString(), MetricTone.Caution)
                    MetricCard("Open tasks", open.size.toString())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        "Active projects",
                        projects.count { it.status == "active" }.toString(),
                    )
                    MetricCard("Organizations", organizations.size.toString())
                }
            }
        }

        item {
            Section("Needs attention now") {
                if (attention.isEmpty()) {
                    EmptyState(
                        "Nothing urgent",
                        "No tasks are due, overdue, or in progress right now.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        attention.forEach { task ->
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

        item {
            Section("Active projects") {
                val active = projects.filter { it.status == "active" }.take(6)
                if (active.isEmpty()) {
                    EmptyState("No active projects", "Create a project to start tracking work.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        active.forEach { project ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    project.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                val count = tasks.count {
                                    it.projectId == project.id && it.status != TaskStatus.DONE
                                }
                                Badge("$count open")
                            }
                        }
                    }
                }
            }
        }

        item {
            Section("Quick actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showNewTask = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("New task") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showNewProject = true },
                            modifier = Modifier.weight(1f),
                        ) { Text("New project") }
                        OutlinedButton(
                            onClick = { showNewOrg = true },
                            modifier = Modifier.weight(1f),
                        ) { Text("New organization") }
                    }
                    OutlinedButton(
                        onClick = { onNavigate(Destination.DailyOps) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Daily Operations") }
                }
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
    if (showNewProject) {
        TextInputDialog(
            title = "New project",
            label = "Project name",
            onDismiss = { showNewProject = false },
            onConfirm = {
                viewModel.saveProject(
                    ProjectEntity(name = it)
                )
            },
        )
    }
    if (showNewOrg) {
        TextInputDialog(
            title = "New organization",
            label = "Organization name",
            onDismiss = { showNewOrg = false },
            onConfirm = {
                viewModel.saveOrganization(
                    OrganizationEntity(name = it)
                )
            },
        )
    }
}
