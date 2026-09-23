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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.Badge
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.TextInputDialog

/** Projects: card list with per-project open-task counts. */
@Composable
fun ProjectsScreen(viewModel: MainViewModel) {
    val projects by viewModel.projects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PageHeader(
                    eyebrow = "STRUCTURE",
                    title = "Projects",
                    description = "Group tasks into projects to track work.",
                )
            }
            if (projects.isEmpty()) {
                item { EmptyState("No projects", "Create a project to start tracking work.") }
            } else {
                items(projects, key = { it.id }) { project ->
                    val openCount = tasks.count {
                        it.projectId == project.id && it.status != TaskStatus.DONE
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "$openCount open tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Badge(if (project.status == "active") "Active" else project.status)
                        }
                    }
                }
            }
        }
        Button(
            onClick = { showNew = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("New project") }
    }

    if (showNew) {
        TextInputDialog(
            title = "New project",
            label = "Project name",
            onDismiss = { showNew = false },
            onConfirm = { viewModel.saveProject(ProjectEntity(name = it)) },
        )
    }
}

/** Organizations: card list. */
@Composable
fun OrganizationsScreen(viewModel: MainViewModel) {
    val organizations by viewModel.organizations.collectAsState()
    val projects by viewModel.projects.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PageHeader(
                    eyebrow = "STRUCTURE",
                    title = "Organizations",
                    description = "Top-level containers for projects.",
                )
            }
            if (organizations.isEmpty()) {
                item {
                    EmptyState(
                        "No organizations",
                        "Create an organization to group projects.",
                    )
                }
            } else {
                items(organizations, key = { it.id }) { org ->
                    val projectCount = projects.count { it.organizationId == org.id }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                org.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "$projectCount projects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Button(
            onClick = { showNew = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("New organization") }
    }

    if (showNew) {
        TextInputDialog(
            title = "New organization",
            label = "Organization name",
            onDismiss = { showNew = false },
            onConfirm = { viewModel.saveOrganization(OrganizationEntity(name = it)) },
        )
    }
}
