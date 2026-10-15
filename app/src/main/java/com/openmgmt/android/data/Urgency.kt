package com.openmgmt.android.data

import java.time.Instant
import java.time.ZoneId

enum class Urgency(val label: String) {
    OVERDUE("Overdue"), BLOCKED("Blocked"), NOW("Now"), NEXT("Next"),
    DUE_SOON("Due soon"), WAITING("Waiting"), LATER("Later")
}

/** Approximation of the desktop urgency engine, not its ranking algorithm.
 * Date-only deadlines become overdue after local midnight; precedence is exclusive.
 */
fun urgency(task: TaskEntity, now: Long, zone: ZoneId = ZoneId.systemDefault()): Urgency? {
    if (task.status in setOf(TaskStatus.DONE, TaskStatus.CANCELED)) return null
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val due = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    return when {
        due != null && due < today -> Urgency.OVERDUE
        task.status == TaskStatus.BLOCKED -> Urgency.BLOCKED
        task.status == TaskStatus.IN_PROGRESS || due == today -> Urgency.NOW
        task.status == TaskStatus.WAITING -> Urgency.WAITING
        task.status == TaskStatus.READY -> Urgency.NEXT
        due != null && due <= today.plusDays(7) -> Urgency.DUE_SOON
        else -> Urgency.LATER
    }
}
