package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.Section
import com.openmgmt.android.ui.components.TaskCard
import com.openmgmt.android.ui.components.formatFullDate
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.startOfToday

/** Daily planning page: today's hero, today's focus, and what's coming up. */
@Composable
fun DailyOpsScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()

    val open = tasks.filter { it.status != TaskStatus.DONE }
    val overdue = open.filter { isOverdue(it) }
    val dueToday = open.filter { it.dueAt != null && !isOverdue(it) && it.dueAt < startOfToday() + 86_400_000 }
    val focus = (overdue + dueToday).distinctBy { it.id }
    val upcoming = open.filter { it.dueAt != null && it.dueAt >= startOfToday() + 86_400_000 }
        .sortedBy { it.dueAt }
        .take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatFullDate(System.currentTimeMillis()),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "Today's operations",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${focus.size} tasks need you today · ${open.size} open overall",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            PageHeader(
                eyebrow = "DAILY OPERATIONS",
                title = "Plan your day",
                description = "Overdue and due-today tasks, then a look at what's coming up.",
            )
        }

        item {
            Section("Today's focus") {
                if (focus.isEmpty()) {
                    EmptyState("All clear", "Nothing is due today. Enjoy the quiet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        focus.forEach { task ->
                            TaskCard(
                                task = task,
                                projectName = viewModel.projectName(task.projectId),
                                onToggleDone = {
                                    viewModel.setTaskStatus(task, TaskStatus.DONE)
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Section("Coming up") {
                if (upcoming.isEmpty()) {
                    EmptyState("Nothing scheduled", "Tasks with due dates will appear here.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcoming.forEach { task ->
                            TaskCard(
                                task = task,
                                projectName = viewModel.projectName(task.projectId),
                                onToggleDone = {
                                    viewModel.setTaskStatus(task, TaskStatus.DONE)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
