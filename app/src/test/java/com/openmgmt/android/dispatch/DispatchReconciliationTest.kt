package com.openmgmt.android.dispatch

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DispatchReconciliationTest {
    @Test fun acceptsOnlyExplicitLists() {
        listOf("[]", "{\"dispatches\":[]}").forEach {
            assertEquals(0, validatedDispatches(Json.parseToJsonElement(it)).size)
        }
        assertEquals(1, validatedDispatches(Json.parseToJsonElement(
            """{"dispatches":[{"id":"abc","state":"running"}]}"""
        )).size)
    }

    @Test fun malformedResponsesCannotAuthorizeAnotherDispatch() {
        listOf("{}", "null", "{\"sessions\":[]}", "{\"dispatches\":null}",
            "[{}]", "[null]", """[{"id":"abc","state":"unknown"}]""",
            """[{"id":"","state":"running"}]""",
            """[{"id":123,"state":"done"}]""").forEach {
            assertThrows(RuntimeException::class.java) { validatedDispatches(Json.parseToJsonElement(it)) }
        }
    }
}
