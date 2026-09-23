package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.Badge
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.MetricCard
import com.openmgmt.android.ui.components.MetricTone
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.SectionHeader
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.TextInputDialog
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.nav.Destination

/** Command center: metrics, needs-attention list, active projects, quick actions. */
@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigate: (Destination) -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val organizations by viewModel.organizations.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val actions = LocalTaskActions.current

    var showNewProject by rememberSaveable { mutableStateOf(false) }
    var showNewOrg by rememberSaveable { mutableStateOf(false) }

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val overdue = open.filter { isOverdue(it) }
    val inProgress = open.filter { it.status == TaskStatus.IN_PROGRESS }
    val blocked = open.filter { it.status == TaskStatus.BLOCKED }
    val activeProjects = projects.filter { it.status == "active" }
    // Most urgent first: overdue, then in progress, then the next due dates.
    val attention = (overdue.sortedBy { it.dueAt } + inProgress +
        open.filter { it.dueAt != null }.sortedBy { it.dueAt })
        .distinctBy { it.id }
        .take(6)

    ScreenFab("New task") { actions.create(TaskEntity(title = "")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        "In progress", inProgress.size.toString(), Modifier.weight(1f),
                        MetricTone.Accent,
                    ) { onNavigate(Destination.Board) }
                    MetricCard(
                        "Overdue", overdue.size.toString(), Modifier.weight(1f),
                        if (overdue.isEmpty()) MetricTone.Neutral else MetricTone.Danger,
                    ) { onNavigate(Destination.Schedule) }
                    MetricCard(
                        "Blocked", blocked.size.toString(), Modifier.weight(1f),
                        if (blocked.isEmpty()) MetricTone.Neutral else MetricTone.Caution,
                    ) { onNavigate(Destination.Board) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Open tasks", open.size.toString(), Modifier.weight(1f)) {
                        onNavigate(Destination.Tasks)
                    }
                    MetricCard("Projects", activeProjects.size.toString(), Modifier.weight(1f)) {
                        onNavigate(Destination.Projects)
                    }
                    MetricCard("Orgs", organizations.size.toString(), Modifier.weight(1f)) {
                        onNavigate(Destination.Organizations)
                    }
                }
            }
        }

        item(key = "attention-header") {
            SectionHeader(
                "Needs attention",
                count = attention.size.takeIf { it > 0 },
                action = "All tasks" to { onNavigate(Destination.Tasks) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (attention.isEmpty()) {
            item(key = "attention-empty") {
                EmptyState("Nothing urgent", "No tasks are due, overdue, or in progress.")
            }
        } else {
            items(attention, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    projectName = projectNames[task.projectId],
                    onToggleDone = { actions.toggleDone(task) },
                    onClick = { actions.edit(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item(key = "projects-header") {
            SectionHeader(
                "Active projects",
                action = "All projects" to { onNavigate(Destination.Projects) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "projects") {
            val shown = activeProjects.take(6)
            if (shown.isEmpty()) {
                EmptyState("No active projects", "Create a project to start tracking work.")
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    shown.forEachIndexed { index, project ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                project.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
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

        item(key = "quick-header") {
            SectionHeader("Quick actions", modifier = Modifier.padding(top = 12.dp))
        }
        item(key = "quick") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Project", Modifier.weight(1f)) { showNewProject = true }
                QuickAction("Organization", Modifier.weight(1f)) { showNewOrg = true }
            }
        }
    }

    if (showNewProject) {
        TextInputDialog(
            title = "New project",
            label = "Project name",
            confirmLabel = "Create",
            onDismiss = { showNewProject = false },
            onConfirm = { viewModel.saveProject(ProjectEntity(name = it)) },
        )
    }
    if (showNewOrg) {
        TextInputDialog(
            title = "New organization",
            label = "Organization name",
            confirmLabel = "Create",
            onDismiss = { showNewOrg = false },
            onConfirm = { viewModel.saveOrganization(OrganizationEntity(name = it)) },
        )
    }
}

@Composable
private fun QuickAction(label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
