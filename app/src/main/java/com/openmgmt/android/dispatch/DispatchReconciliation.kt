package com.openmgmt.android.dispatch

import kotlinx.serialization.json.*

/** Reject unknown envelopes and incomplete entries rather than interpreting them as no sessions. */
internal fun validatedDispatches(response: JsonElement): JsonArray {
    val sessions = (response as? JsonArray)
        ?: ((response as? JsonObject)?.get("dispatches") as? JsonArray)
        ?: error("Invalid dispatch list")
    sessions.forEach { entry ->
        val obj = entry as? JsonObject ?: error("Invalid dispatch entry")
        val id = obj["id"] as? JsonPrimitive
        val state = obj["state"] as? JsonPrimitive
        require(id?.isString == true && !id.content.isBlank()) { "Missing dispatch id" }
        require(state?.isString == true && AgentState.entries.any { it != AgentState.NONE && it.wire == state.content }) {
            "Unknown dispatch state"
        }
    }
    return sessions
}
