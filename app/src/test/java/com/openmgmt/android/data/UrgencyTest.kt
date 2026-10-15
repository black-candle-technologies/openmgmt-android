package com.openmgmt.android.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UrgencyTest {
    private val now = Instant.parse("2026-03-10T12:00:00Z").toEpochMilli()
    private fun bucket(status: String = TaskStatus.INBOX, offset: Long? = null) =
        urgency(TaskEntity(title = "Test", status = status, dueAt = offset?.let { now + it * 86_400_000 }), now, ZoneId.of("UTC"))

    @Test fun precedenceAndExclusion() {
        assertNull(bucket(TaskStatus.DONE, -1))
        assertNull(bucket(TaskStatus.CANCELED, -1))
        assertEquals(Urgency.OVERDUE, bucket(TaskStatus.BLOCKED, -1))
        assertEquals(Urgency.BLOCKED, bucket(TaskStatus.BLOCKED, 0))
        assertEquals(Urgency.NOW, bucket(offset = 0))
        assertEquals(Urgency.NOW, bucket(TaskStatus.IN_PROGRESS))
        assertEquals(Urgency.NEXT, bucket(TaskStatus.READY))
        assertEquals(Urgency.WAITING, bucket(TaskStatus.WAITING, 1))
        assertEquals(Urgency.DUE_SOON, bucket(offset = 7))
        assertEquals(Urgency.LATER, bucket(offset = 8))
        assertEquals(Urgency.LATER, bucket())
    }
}
