package com.openmgmt.android.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.data.metadata
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.ProjectStatus
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.Badge
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.LocalSnackbarHostState
import com.openmgmt.android.ui.components.MetricTone
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.TextInputDialog
import com.openmgmt.android.ui.components.plural
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.components.showUndo

/** Project statuses, matching the desktop app's ProjectStatus options. */
private val PROJECT_STATUSES = listOf(
    ProjectStatus.ACTIVE to "Active",
    ProjectStatus.PAUSED to "Paused",
    ProjectStatus.COMPLETED to "Completed",
)

private fun projectStatusLabel(status: String) =
    PROJECT_STATUSES.firstOrNull { it.first == status }?.second
        ?: status.replaceFirstChar { it.uppercase() }

/** Projects: card list with per-project progress and open-task counts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: MainViewModel, initialProjectId: String? = null) {
    val projects by viewModel.projects.collectAsState()
    val organizations by viewModel.organizations.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var showNew by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(initialProjectId) {
        if (initialProjectId != null) { selectedId = initialProjectId; editingId = initialProjectId }
    }
    val actions = LocalTaskActions.current
    ScreenFab("New project") { showNew = true }

    // Active work first; the DAO already sorts by name within each group.
    val sorted = projects.sortedBy { it.status != ProjectStatus.ACTIVE }
    val orgNames = organizations.associate { it.id to it.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sorted.isEmpty()) {
            item(key = "empty") {
                EmptyState("No projects yet", "Group tasks into projects to track work.")
            }
        } else {
            items(sorted, key = { it.id }) { project ->
                val projectTasks = tasks.filter { it.projectId == project.id }
                val doneCount = projectTasks.count { it.status == TaskStatus.DONE }
                StructureCard(
                    title = project.name,
                    subtitle = listOfNotNull(
                        plural(projectTasks.size - doneCount, "open task"),
                        orgNames[project.organizationId],
                    ).joinToString(" · "),
                    badge = projectStatusLabel(project.status),
                    badgeTone = if (project.status == ProjectStatus.ACTIVE) MetricTone.Success else MetricTone.Neutral,
                    openCount = projectTasks.size - doneCount,
                    onClick = { selectedId = project.id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showNew) {
        TextInputDialog(
            title = "New project",
            label = "Project name",
            confirmLabel = "Create",
            onDismiss = { showNew = false },
            onConfirm = { viewModel.saveProject(ProjectEntity(name = it)) },
        )
    }
    projects.firstOrNull { it.id == selectedId }?.let { project ->
        ModalBottomSheet(onDismissRequest = { selectedId = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            val projectTasks = tasks.filter { it.projectId == project.id }
            LazyColumn(contentPadding = screenPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(project.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${projectTasks.count { it.status != TaskStatus.DONE }} open · ${projectTasks.count { it.status == TaskStatus.DONE }} done")
                    Text(project.metadata("description") ?: "No description")
                    project.metadata("deadline")?.let { Text("Deadline · $it") }
                    TextButton(onClick = { editingId = project.id }) {
                        Text("Repository · ${project.repoOwner?.let { "$it/${project.repoName} · ${project.repoBranch}" } ?: "Not bound"}")
                    }
                    TextButton(onClick = { editingId = project.id }) { Text("Edit project") }
                }
                items(projectTasks, key = { it.id }) { task ->
                    TaskCard(task, null, { actions.toggleDone(task) }, { selectedId = null; actions.edit(task) })
                }
            }
        }
    }
    projects.firstOrNull { it.id == editingId }?.let { project ->
        ProjectEditorDialog(
            project = project,
            organizations = organizations,
            onDismiss = { editingId = null },
            onSave = { viewModel.saveProject(it.copy(updatedAt = System.currentTimeMillis())) },
            onDelete = {
                viewModel.deleteProject(project.id)
                snackbar.showUndo(scope, "Project deleted") { viewModel.saveProject(project) }
            },
        )
    }
}

/** Organizations: card list with project and open-task counts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationsScreen(viewModel: MainViewModel) {
    val organizations by viewModel.organizations.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var showNew by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedOrg by rememberSaveable { mutableStateOf<String?>(null) }
    val actions = LocalTaskActions.current
    ScreenFab("New organization") { showNew = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (organizations.isEmpty()) {
            item(key = "empty") {
                EmptyState("No organizations yet", "Organizations are top-level containers for projects.")
            }
        } else {
            items(organizations, key = { it.id }) { org ->
                val orgProjects = projects.filter { it.organizationId == org.id }.map { it.id }.toSet()
                val openCount = tasks.count { it.projectId in orgProjects && it.status != TaskStatus.DONE }
                StructureCard(
                    title = org.name,
                    openCount = openCount,
                    subtitle = "${plural(orgProjects.size, "project")} · ${plural(openCount, "open task")}",
                    onClick = { selectedOrg = org.id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showNew) {
        TextInputDialog(
            title = "New organization",
            label = "Organization name",
            confirmLabel = "Create",
            onDismiss = { showNew = false },
            onConfirm = { viewModel.saveOrganization(OrganizationEntity(name = it)) },
        )
    }
    organizations.firstOrNull { it.id == selectedOrg }?.let { org ->
        val projectIds = projects.filter { it.organizationId == org.id }.map { it.id }.toSet()
        val orgTasks = tasks.filter { it.projectId in projectIds }
        ModalBottomSheet(onDismissRequest = { selectedOrg = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            LazyColumn(contentPadding = screenPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(org.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${orgTasks.count { it.status != TaskStatus.DONE }} open · ${projectIds.size} projects")
                    TextButton(onClick = { editingId = org.id }) { Text("Edit organization") }
                }
                items(orgTasks, key = { it.id }) { task ->
                    TaskCard(task, projects.find { it.id == task.projectId }?.name,
                        { actions.toggleDone(task) }, { selectedOrg = null; actions.edit(task) })
                }
            }
        }
    }
    organizations.firstOrNull { it.id == editingId }?.let { org ->
        NameEditorDialog(
            title = "Edit organization",
            initialName = org.name,
            onDismiss = { editingId = null },
            onSave = {
                viewModel.saveOrganization(org.copy(name = it, updatedAt = System.currentTimeMillis()))
            },
            onDelete = {
                viewModel.deleteOrganization(org.id)
                snackbar.showUndo(scope, "Organization deleted") { viewModel.saveOrganization(org) }
            },
        )
    }
}

@Composable
private fun StructureCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeTone: MetricTone = MetricTone.Neutral,
    openCount: Int = 0,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (badge != null) Badge(badge, badgeTone)
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("$openCount open", style = MaterialTheme.typography.headlineSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectEditorDialog(
    project: ProjectEntity,
    organizations: List<OrganizationEntity>,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(project.id) { mutableStateOf(project.name) }
    var status by rememberSaveable(project.id) { mutableStateOf(project.status) }
    var orgId by rememberSaveable(project.id) { mutableStateOf(project.organizationId) }
    var orgMenu by remember { mutableStateOf(false) }
    var repoOwner by rememberSaveable(project.id) { mutableStateOf(project.repoOwner.orEmpty()) }
    var repoName by rememberSaveable(project.id) { mutableStateOf(project.repoName.orEmpty()) }
    var repoBranch by rememberSaveable(project.id) { mutableStateOf(project.repoBranch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit project") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Repository")
                OutlinedTextField(repoOwner, { repoOwner = it }, label = { Text("Owner") }, singleLine = true)
                OutlinedTextField(repoName, { repoName = it }, label = { Text("Repository name") }, singleLine = true)
                OutlinedTextField(repoBranch, { repoBranch = it }, label = { Text("Branch") }, singleLine = true)
                TextButton(onClick = { repoOwner = ""; repoName = ""; repoBranch = "main" }) { Text("Clear repository") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PROJECT_STATUSES.forEach { (value, label) ->
                        FilterChip(
                            selected = status == value,
                            onClick = { status = value },
                            label = { Text(label) },
                        )
                    }
                }
                Box {
                    OutlinedButton(
                        onClick = { orgMenu = true },
                        enabled = organizations.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            // Projects without one are filed under Personal when saved.
                            organizations.firstOrNull { it.id == orgId }?.name ?: "Personal",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = orgMenu, onDismissRequest = { orgMenu = false }) {
                        organizations.forEach { org ->
                            DropdownMenuItem(
                                text = { Text(org.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { orgId = org.id; orgMenu = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(project.copy(name = name.trim(), status = status, organizationId = orgId,
                        repoOwner = repoOwner.trim().takeIf { it.isNotEmpty() },
                        repoName = repoName.trim().takeIf { it.isNotEmpty() }, repoBranch = repoBranch.trim().ifEmpty { "main" }))
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun NameEditorDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()); onDismiss() },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
