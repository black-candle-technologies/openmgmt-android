package com.openmgmt.android.update

import org.junit.Assert.*
import org.junit.Test

class ReleasePolicyTest {
    @Test fun onlyStrictlyNewerCodes() {
        assertFalse(isNewerVersion(7, 7))
        assertFalse(isNewerVersion(7, 6))
        assertTrue(isNewerVersion(7, 8))
        assertTrue(isNewerVersion(Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong() + 1))
    }
}
