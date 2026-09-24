package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.ui.components.EmptyState
import com.openmgmt.android.ui.components.SectionHeader
import com.openmgmt.android.ui.components.TaskActions
import com.openmgmt.android.ui.components.TaskCard

/**
 * A titled group of task cards as lazy items (header + one item per task),
 * so long lists stay lazy and cards animate as they move.
 *
 * With a null [emptyTitle], an empty section is omitted entirely.
 */
fun LazyListScope.taskSection(
    key: String,
    title: String,
    tasks: List<TaskEntity>,
    projectNames: Map<String, String>,
    defaultProjectId: String?,
    actions: TaskActions,
    emptyTitle: String? = null,
    emptyHint: String = "",
    titleColor: Color = Color.Unspecified,
    showStatus: Boolean = true,
    first: Boolean = false,
) {
    if (tasks.isEmpty() && emptyTitle == null) return
    item(key = "$key-header") {
        SectionHeader(
            title,
            count = tasks.size.takeIf { it > 0 },
            titleColor = titleColor,
            modifier = Modifier.animateItem().padding(top = if (first) 0.dp else 12.dp),
        )
    }
    if (tasks.isEmpty()) {
        item(key = "$key-empty") { EmptyState(emptyTitle!!, emptyHint, Modifier.animateItem()) }
    } else {
        items(tasks, key = { "$key-${it.id}" }) { task ->
            TaskCard(
                task = task,
                projectName = projectNames[task.projectId],
                onToggleDone = { actions.toggleDone(task) },
                onClick = { actions.edit(task) },
                showStatus = showStatus,
                inDefaultProject = task.projectId != null && task.projectId == defaultProjectId,
                modifier = Modifier.animateItem(),
            )
        }
    }
}
