package com.openmgmt.android.data

import kotlinx.serialization.json.*

/** Desktop-only metadata already retained by sync; don't invent dates for local projects. */
fun ProjectEntity.metadata(key: String): String? = runCatching {
    remoteJson?.let { Json.parseToJsonElement(it).jsonObject[key]?.jsonPrimitive?.contentOrNull }
}.getOrNull()
