package com.openmgmt.android.dispatch

import org.junit.Assert.*
import org.junit.Test

class AgentStateTest {
    @Test fun mapsEveryStateAndUnknownSafely() {
        AgentState.entries.forEach { assertEquals(it, AgentState.fromWire(it.wire)) }
        assertEquals(AgentState.NONE, AgentState.fromWire(null))
        assertEquals(AgentState.NONE, AgentState.fromWire("future_state"))
    }
}
