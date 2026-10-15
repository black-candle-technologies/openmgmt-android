package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.metadata
import com.openmgmt.android.data.ProjectStatus
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
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.nav.Destination

/** Calm overview: four metrics, three attention cards, and active projects. */
@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigate: (Destination) -> Unit) {
    com.openmgmt.android.dispatch.RefreshAgentsOnResume(viewModel)
    val tasks by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val defaultProjectId by viewModel.defaultProjectId.collectAsState()
    val actions = LocalTaskActions.current

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val overdue = open.filter { isOverdue(it) }
    val inProgress = open.filter { it.status == TaskStatus.IN_PROGRESS }
    val blocked = open.filter { it.status == TaskStatus.BLOCKED || it.status == TaskStatus.WAITING }
    val activeProjects = projects.filter { it.status == ProjectStatus.ACTIVE }
    // Most urgent first: overdue, then in progress, then the next due dates.
    val attention = (overdue.sortedBy { it.dueAt } + inProgress +
        open.filter { it.dueAt != null }.sortedBy { it.dueAt })
        .distinctBy { it.id }
        .take(3)

    ScreenFab("New task") { actions.create(TaskEntity(title = "")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Equal-height cards even when a label wraps at large font scales.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        "In progress", inProgress.size.toString(), Modifier.weight(1f).fillMaxHeight(),
                        MetricTone.Accent,
                    ) { onNavigate(Destination.Board) }
                    MetricCard(
                        "Overdue", overdue.size.toString(), Modifier.weight(1f).fillMaxHeight(),
                        if (overdue.isEmpty()) MetricTone.Neutral else MetricTone.Danger,
                    ) { onNavigate(Destination.Schedule) }
                }
                // Equal-height cards even when a label wraps at large font scales.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Due soon", open.count {
                        it.dueAt != null && !isOverdue(it) && it.dueAt <= System.currentTimeMillis() + 7 * 86_400_000L
                    }.toString(), Modifier.weight(1f).fillMaxHeight()) { onNavigate(Destination.Schedule) }
                    MetricCard("Blocked", blocked.size.toString(), Modifier.weight(1f).fillMaxHeight()) {
                        onNavigate(Destination.Board)
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
                    inDefaultProject = task.projectId != null && task.projectId == defaultProjectId,
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
                            Column {
                                Badge("$count open")
                                project.metadata("deadline")?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }


    }

}
