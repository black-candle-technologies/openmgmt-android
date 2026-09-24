package com.openmgmt.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Task interactions shared by every screen that lists tasks: open the
 * editor, create a task with context defaults, and toggle done with undo.
 */
class TaskActions(
    val edit: (TaskEntity) -> Unit,
    val create: (defaults: TaskEntity) -> Unit,
    val toggleDone: (TaskEntity) -> Unit,
)

val LocalTaskActions = staticCompositionLocalOf<TaskActions> {
    error("TaskActions not provided; wrap content in TaskActionsHost")
}

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

/** Shows a snackbar, replacing any visible one so messages don't queue up. */
fun SnackbarHostState.showUndo(
    scope: CoroutineScope,
    message: String,
    onUndo: () -> Unit,
) {
    currentSnackbarData?.dismiss()
    scope.launch {
        val result = showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
}

@Composable
fun TaskActionsHost(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    // The task being edited and whether it is new; null when the sheet is closed.
    var editing by remember { mutableStateOf<Pair<TaskEntity, Boolean>?>(null) }

    val actions = remember(viewModel, snackbarHostState) {
        TaskActions(
            edit = { editing = it to false },
            create = { editing = it to true },
            toggleDone = { task ->
                if (task.status == TaskStatus.DONE) {
                    // Reopened work goes back to "ready" rather than the inbox.
                    viewModel.setTaskStatus(task, TaskStatus.READY)
                } else {
                    viewModel.setTaskStatus(task, TaskStatus.DONE)
                    snackbarHostState.showUndo(scope, "Completed “${task.title}”") {
                        viewModel.saveTask(task)
                    }
                }
            },
        )
    }

    CompositionLocalProvider(LocalTaskActions provides actions) {
        content()
    }

    editing?.let { (task, isNew) ->
        TaskEditorSheet(
            initial = task,
            isNew = isNew,
            projects = projects,
            onDismiss = { editing = null },
            onSave = { viewModel.saveTask(it) },
            onDelete = {
                viewModel.deleteTask(task.id)
                snackbarHostState.showUndo(scope, "Task deleted") { viewModel.saveTask(task) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initial: TaskEntity,
    isNew: Boolean,
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes) }
    var status by rememberSaveable(initial.id) { mutableStateOf(initial.status) }
    var dueAt by rememberSaveable(initial.id) { mutableStateOf(initial.dueAt) }
    var projectId by rememberSaveable(initial.id) { mutableStateOf(initial.projectId) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Animate the sheet away before removing it from composition.
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isNew) "New task" else "Edit task",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("Status")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatus.selectable.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { status = option },
                        label = { Text(statusLabel(option)) },
                        leadingIcon = if (status == option) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
            }

            FieldLabel("Due")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                val dueDate = dueAt?.let(::localDate)
                FilterChip(
                    selected = dueDate == today,
                    onClick = { dueAt = startOfDay(today) },
                    label = { Text("Today") },
                )
                FilterChip(
                    selected = dueDate == today.plusDays(1),
                    onClick = { dueAt = startOfDay(today.plusDays(1)) },
                    label = { Text("Tomorrow") },
                )
                val custom = dueAt != null && dueDate != today && dueDate != today.plusDays(1)
                FilterChip(
                    selected = custom,
                    onClick = { pickingDate = true },
                    label = { Text(if (custom) formatDue(dueAt!!) else "Pick date") },
                    leadingIcon = {
                        Icon(Icons.Filled.DateRange, null, Modifier.size(FilterChipDefaults.IconSize))
                    },
                )
                if (dueAt != null) {
                    InputChip(
                        selected = false,
                        onClick = { dueAt = null },
                        label = { Text("Clear") },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, "Clear due date", Modifier.size(FilterChipDefaults.IconSize))
                        },
                    )
                }
            }

            FieldLabel("Project")
            ProjectPicker(
                projects = projects,
                selectedId = projectId,
                onSelect = { projectId = it },
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isNew) {
                    TextButton(
                        onClick = { close(onDelete) },
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { close() }) { Text("Cancel") }
                Spacer(Modifier.size(8.dp))
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val saved = initial.copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            status = status,
                            dueAt = dueAt,
                            projectId = projectId,
                        )
                        close { onSave(saved) }
                    },
                ) { Text(if (isNew) "Add task" else "Save") }
            }
        }
    }

    if (pickingDate) {
        // DatePicker works in UTC midnights; due dates are stored as local start-of-day.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dueAt?.let(::localDate) ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        dueAt = startOfDay(
                            java.time.Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            // On short windows (phones in landscape) the full picker is taller
            // than the screen and its grid gets squeezed; drop the title and
            // headline, which only repeat the date selected in the grid.
            if (LocalConfiguration.current.screenHeightDp < 560) {
                DatePicker(state = pickerState, title = null, headline = null, showModeToggle = false)
            } else {
                DatePicker(state = pickerState)
            }
        }
    }

    if (isNew) LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ProjectPicker(
    projects: List<ProjectEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = projects.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = projects.isNotEmpty(),
        ) {
            Text(
                // Unassigned tasks are filed in Inbox when saved.
                selected?.name ?: "Inbox",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected != null) FontWeight.Medium else null,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelect(project.id); open = false },
                    trailingIcon = if (project.id == selectedId) {
                        { Icon(Icons.Filled.Check, null) }
                    } else null,
                )
            }
        }
    }
}
