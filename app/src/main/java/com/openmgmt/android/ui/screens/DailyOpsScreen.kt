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
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.LocalTaskActions
import com.openmgmt.android.ui.components.ScreenFab
import com.openmgmt.android.ui.components.formatFullDate
import com.openmgmt.android.ui.components.isOverdue
import com.openmgmt.android.ui.components.plural
import com.openmgmt.android.ui.components.screenPadding
import com.openmgmt.android.ui.components.startOfToday

/** Daily planning page: today's hero, today's focus, and what's coming up. */
@Composable
fun DailyOpsScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val projectNames by viewModel.projectNames.collectAsState()
    val actions = LocalTaskActions.current

    val tomorrow = startOfToday() + 86_400_000
    val open = tasks.filter { it.status != TaskStatus.DONE }
    val overdue = open.filter { isOverdue(it) }.sortedBy { it.dueAt }
    val dueToday = open.filter { it.dueAt != null && !isOverdue(it) && it.dueAt < tomorrow }
    val focus = overdue + dueToday
    val upcoming = open.filter { it.dueAt != null && it.dueAt >= tomorrow }
        .sortedBy { it.dueAt }
        .take(5)

    ScreenFab("Add for today") {
        actions.create(TaskEntity(title = "", dueAt = startOfToday()))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(hasFab = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "hero") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatFullDate(System.currentTimeMillis()).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "Today's operations",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        buildString {
                            append(if (focus.isEmpty()) "Nothing due today" else "${plural(focus.size, "task")} need you today")
                            if (overdue.isNotEmpty()) append(" (${overdue.size} overdue)")
                            append(" · ${open.size} open overall")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        taskSection(
            key = "focus",
            title = "Today's focus",
            tasks = focus,
            projectNames = projectNames,
            actions = actions,
            emptyTitle = "All clear",
            emptyHint = "Nothing is due today. Enjoy the quiet.",
        )
        taskSection(
            key = "upcoming",
            title = "Coming up",
            tasks = upcoming,
            projectNames = projectNames,
            actions = actions,
            emptyTitle = "Nothing scheduled",
            emptyHint = "Tasks with due dates will appear here.",
        )
    }
}
