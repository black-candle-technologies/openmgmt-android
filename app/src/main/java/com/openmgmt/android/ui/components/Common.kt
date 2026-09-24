package com.openmgmt.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.theme.LocalStatusColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared building blocks mirroring the desktop components:
 * sections, metric cards, task cards, badges, and empty states.
 * (Page titles and eyebrows live in the top bar; see AppScaffold.)
 */

/**
 * Content padding for a scrolling page: the page gutter (wider on wide
 * windows, see [LocalContentGutter]) and room for a FAB when present.
 */
@Composable
fun screenPadding(hasFab: Boolean = false, top: Dp = 12.dp): PaddingValues {
    val gutter = LocalContentGutter.current
    return PaddingValues(
        start = gutter,
        end = gutter,
        top = top,
        bottom = if (hasFab) 96.dp else 24.dp,
    )
}

@Composable
fun Section(
    title: String,
    count: Int? = null,
    titleColor: Color = Color.Unspecified,
    action: Pair<String, () -> Unit>? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, count, titleColor, action)
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    titleColor: Color = Color.Unspecified,
    action: Pair<String, () -> Unit>? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
        )
        if (count != null) Badge(count.toString())
        Box(Modifier.weight(1f))
        if (action != null) {
            TextButton(
                onClick = action.second,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) { Text(action.first) }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: MetricTone = MetricTone.Neutral,
    onClick: (() -> Unit)? = null,
) {
    val (container, content) = toneColors(tone, neutral = MaterialTheme.colorScheme.surface)
    val colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val body: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            // Two lines, so labels survive large font scales instead of truncating.
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, elevation = elevation) { body() }
    } else {
        Card(modifier = modifier, colors = colors, elevation = elevation) { body() }
    }
}

enum class MetricTone { Neutral, Accent, Danger, Caution, Success }

@Composable
private fun toneColors(
    tone: MetricTone,
    neutral: Color = MaterialTheme.colorScheme.surfaceVariant,
): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    return when (tone) {
        MetricTone.Neutral -> neutral to
            if (neutral == scheme.surface) scheme.onSurface else scheme.onSurfaceVariant
        MetricTone.Accent -> scheme.primary to scheme.onPrimary
        MetricTone.Danger -> scheme.errorContainer to scheme.onErrorContainer
        MetricTone.Caution -> status.cautionContainer to status.onCautionContainer
        MetricTone.Success -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    projectName: String?,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
) {
    val done = task.status == TaskStatus.DONE
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = done, onCheckedChange = { onToggleDone() })
            Column(
                Modifier.weight(1f).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                val due = task.dueAt
                if (projectName != null || showStatus || due != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (showStatus) {
                            StatusBadge(task.status, Modifier.align(Alignment.CenterVertically))
                        }
                        // Skip a project badge that just repeats the status ("Inbox" / "Inbox").
                        if (projectName != null &&
                            !(showStatus && projectName.equals(statusLabel(task.status), ignoreCase = true))
                        ) {
                            Badge(
                                projectName,
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .align(Alignment.CenterVertically),
                            )
                        }
                        if (due != null) {
                            val overdue = isOverdue(task)
                            Text(
                                if (overdue) "Overdue · ${formatDue(due)}" else formatDue(due),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (overdue) FontWeight.Medium else null,
                                color = if (overdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(label: String, tone: MetricTone = MetricTone.Neutral, modifier: Modifier = Modifier) {
    val (container, content) = toneColors(tone)
    Box(
        modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Human label for a status, like the desktop's `humanize` ("in_progress" → "In progress"). */
fun statusLabel(status: String): String =
    status.replace('_', ' ').replaceFirstChar { it.uppercase() }

fun statusTone(status: String): MetricTone = when (status) {
    TaskStatus.IN_PROGRESS -> MetricTone.Accent
    TaskStatus.BLOCKED, TaskStatus.WAITING -> MetricTone.Caution
    TaskStatus.DONE -> MetricTone.Success
    else -> MetricTone.Neutral
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    Badge(statusLabel(status), statusTone(status), modifier)
}

@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val submit = {
        if (text.isNotBlank()) {
            onConfirm(text.trim())
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "1 task", "3 tasks". */
fun plural(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"

fun isOverdue(task: TaskEntity): Boolean {
    val due = task.dueAt ?: return false
    if (task.status == TaskStatus.DONE) return false
    return due < startOfToday()
}

fun startOfToday(): Long = startOfDay(LocalDate.now())

fun startOfDay(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun localDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatDue(dueAt: Long): String {
    val date = localDate(dueAt)
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(
            DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, MMM d" else "MMM d, yyyy")
        )
    }
}

fun formatFullDate(epochMillis: Long): String =
    localDate(epochMillis).format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
