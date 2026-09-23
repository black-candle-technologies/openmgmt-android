package com.openmgmt.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared building blocks mirroring the desktop components:
 * page headers with eyebrows, sections, metric cards, task cards,
 * badges, and empty states.
 */

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    description: String,
    actions: @Composable () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        actions()
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
fun MetricCard(label: String, value: String, tone: MetricTone = MetricTone.Neutral) {
    val container = when (tone) {
        MetricTone.Neutral -> MaterialTheme.colorScheme.surface
        MetricTone.Accent -> MaterialTheme.colorScheme.primary
        MetricTone.Danger -> MaterialTheme.colorScheme.errorContainer
        MetricTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (tone) {
        MetricTone.Neutral -> MaterialTheme.colorScheme.onSurface
        MetricTone.Accent -> MaterialTheme.colorScheme.onPrimary
        MetricTone.Danger -> MaterialTheme.colorScheme.onErrorContainer
        MetricTone.Caution -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = content)
        }
    }
}

enum class MetricTone { Neutral, Accent, Danger, Caution }

@Composable
fun TaskCard(
    task: TaskEntity,
    projectName: String?,
    onToggleDone: () -> Unit,
    onClick: () -> Unit = {},
) {
    val done = task.status == TaskStatus.DONE
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(checked = done, onCheckedChange = { onToggleDone() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (projectName != null) {
                        Badge(projectName, MetricTone.Neutral)
                    }
                    StatusBadge(task.status)
                    task.dueAt?.let {
                        Text(
                            formatDue(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue(task)) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(label: String, tone: MetricTone = MetricTone.Neutral) {
    val (container, content) = when (tone) {
        MetricTone.Accent -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        MetricTone.Danger -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        MetricTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        MetricTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        Modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, tone) = when (status) {
        TaskStatus.OPEN -> "Open" to MetricTone.Neutral
        TaskStatus.IN_PROGRESS -> "In progress" to MetricTone.Accent
        TaskStatus.BLOCKED -> "Blocked" to MetricTone.Danger
        TaskStatus.DONE -> "Done" to MetricTone.Caution
        else -> status to MetricTone.Neutral
    }
    Badge(label, tone)
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                        onDismiss()
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun isOverdue(task: TaskEntity): Boolean {
    val due = task.dueAt ?: return false
    if (task.status == TaskStatus.DONE) return false
    return due < startOfToday()
}

fun startOfToday(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDue(dueAt: Long): String {
    val date = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

fun formatFullDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
