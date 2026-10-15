package com.openmgmt.android.update

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

class UpdateFilesTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun laterAttemptsNeverOverwriteVerifiedBytes() {
        val verified = newUpdateFile(temp.root).apply { writeText("verified APK") }
        val next = newUpdateFile(temp.root).apply { writeText("different APK") }
        assertNotEquals(verified, next)
        assertEquals("verified APK", verified.readText())
        assertEquals("apk", next.extension)
    }

    @Test fun cleanupKeepsRecentFilesAndRemovesStaleAttempts() {
        val now = System.currentTimeMillis()
        val stale = newUpdateFile(temp.root).apply {
            assertTrue(setLastModified(now - TimeUnit.DAYS.toMillis(8)))
        }
        val recent = newUpdateFile(temp.root, now)
        assertFalse(stale.exists())
        newUpdateFile(temp.root, now)
        assertTrue(recent.exists())
    }
}
