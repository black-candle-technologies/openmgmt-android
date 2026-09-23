package com.openmgmt.android.sync

import com.openmgmt.android.data.DEFAULT_PRIORITY
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.ProjectStatus
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.data.TaskStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Conversions between the local entities and the desktop app's entity JSON
 * (openmgmt-core models.rs: Organization, Project, Task), which is what
 * sync event payloads carry as `{"entity": {...}}`.
 *
 * Android models a subset of the desktop fields. Outgoing entities start
 * from the last JSON seen for that entity ([TaskEntity.remoteJson] etc.) so
 * desktop-only fields (tags, scheduling, colors, …) survive an Android edit.
 */
object SyncEntityType {
    const val ORGANIZATION = "organization"
    const val PROJECT = "project"
    const val TASK = "task"
}

object SyncOperation {
    const val CREATED = "created"
    const val UPDATED = "updated"
    const val ARCHIVED = "archived"
    const val TRANSITIONED = "transitioned"
}

internal val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun formatTimestamp(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

/** Parses RFC 3339 as written by chrono (`+00:00`, nanoseconds) or Java (`Z`). */
fun parseTimestamp(value: String): Long =
    OffsetDateTime.parse(value).toInstant().toEpochMilli()

/** Desktop-compatible slug (openmgmt-core `slugify`). */
fun slugify(value: String): String {
    val slug = StringBuilder()
    var separator = false
    for (c in value.trim().lowercase()) {
        if (c in 'a'..'z' || c in '0'..'9') {
            slug.append(c)
            separator = false
        } else if (slug.isNotEmpty() && !separator) {
            slug.append('-')
            separator = true
        }
    }
    return slug.toString().trimEnd('-')
}

/**
 * Slugs Android invents carry an id suffix: desktop organization slugs are
 * globally unique (projects per organization), and desktop installs seed
 * common names like "personal", so a bare slug would fail to replay there.
 */
private fun uniqueSlug(name: String, id: String): String {
    val base = slugify(name).ifEmpty { "item" }
    return "$base-${id.filter { it.isLetterOrDigit() }.take(6).lowercase()}"
}

private fun base(remoteJson: String?): MutableMap<String, JsonElement> =
    remoteJson?.let { runCatching { syncJson.parseToJsonElement(it).jsonObject }.getOrNull() }
        ?.toMutableMap() ?: mutableMapOf()

private fun time(ms: Long?): JsonElement = ms?.let { JsonPrimitive(formatTimestamp(it)) } ?: JsonNull

private fun MutableMap<String, JsonElement>.default(key: String, value: JsonElement) {
    if (key !in this) this[key] = value
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

private fun JsonObject.time(key: String): Long? = string(key)?.let(::parseTimestamp)

fun OrganizationEntity.toSyncJson(): JsonObject {
    val m = base(remoteJson)
    // Keep the slug the entity already has elsewhere; otherwise derive one.
    val slug = m["slug"]?.jsonPrimitive?.contentOrNull ?: uniqueSlug(name, id)
    m["id"] = JsonPrimitive(id)
    m["name"] = JsonPrimitive(name)
    m["slug"] = JsonPrimitive(slug)
    m.default("description", JsonNull)
    m.default("color", JsonNull)
    m.default("icon", JsonNull)
    m["created_at"] = time(createdAt)
    m["updated_at"] = time(updatedAt)
    m["archived_at"] = time(archivedAt)
    return JsonObject(m)
}

fun ProjectEntity.toSyncJson(): JsonObject {
    val m = base(remoteJson)
    val slug = m["slug"]?.jsonPrimitive?.contentOrNull ?: uniqueSlug(name, id)
    m["id"] = JsonPrimitive(id)
    m["organization_id"] = JsonPrimitive(requireNotNull(organizationId) { "project without organization" })
    m["name"] = JsonPrimitive(name)
    m["slug"] = JsonPrimitive(slug)
    m.default("description", JsonNull)
    m.default("project_type", JsonPrimitive("other"))
    m["status"] = JsonPrimitive(status)
    m.default("priority", JsonPrimitive(DEFAULT_PRIORITY))
    m.default("deadline", JsonNull)
    m.default("repo_url", JsonNull)
    m.default("notes", JsonNull)
    m["created_at"] = time(createdAt)
    m["updated_at"] = time(updatedAt)
    m["archived_at"] = time(archivedAt)
    return JsonObject(m)
}

fun TaskEntity.toSyncJson(): JsonObject {
    val m = base(remoteJson)
    m["id"] = JsonPrimitive(id)
    m["project_id"] = JsonPrimitive(requireNotNull(projectId) { "task without project" })
    m["title"] = JsonPrimitive(title)
    m["description"] = if (notes.isBlank()) JsonNull else JsonPrimitive(notes)
    m["status"] = JsonPrimitive(status)
    m["priority"] = JsonPrimitive(priority.takeIf { it in 1..5 } ?: DEFAULT_PRIORITY)
    m["due_at"] = time(dueAt)
    m.default("scheduled_at", JsonNull)
    m["started_at"] = time(startedAt)
    m["completed_at"] = time(completedAt)
    m.default("estimated_minutes", JsonNull)
    m.default("time_limit_minutes", JsonNull)
    m.default("pinned", JsonPrimitive(false))
    m.default("blocked_reason", JsonNull)
    m.default("tags", JsonArray(emptyList()))
    m["created_at"] = time(createdAt)
    m["updated_at"] = time(updatedAt)
    return JsonObject(m)
}

fun entityPayload(entity: JsonObject): JsonObject = buildJsonObject { put("entity", entity) }

fun archivePayload(id: String, archivedAt: Long): JsonObject = buildJsonObject {
    put("id", id)
    put("archived_at", formatTimestamp(archivedAt))
}

fun organizationFromSync(e: JsonObject): OrganizationEntity = OrganizationEntity(
    id = e.string("id")!!,
    name = e.string("name")!!,
    createdAt = e.time("created_at") ?: System.currentTimeMillis(),
    updatedAt = e.time("updated_at") ?: System.currentTimeMillis(),
    archivedAt = e.time("archived_at"),
    remoteJson = e.toString(),
)

fun projectFromSync(e: JsonObject): ProjectEntity = ProjectEntity(
    id = e.string("id")!!,
    name = e.string("name")!!,
    organizationId = e.string("organization_id"),
    status = e.string("status") ?: ProjectStatus.ACTIVE,
    createdAt = e.time("created_at") ?: System.currentTimeMillis(),
    updatedAt = e.time("updated_at") ?: System.currentTimeMillis(),
    archivedAt = e.time("archived_at"),
    remoteJson = e.toString(),
)

fun taskFromSync(e: JsonObject): TaskEntity = TaskEntity(
    id = e.string("id")!!,
    title = e.string("title")!!,
    notes = e.string("description").orEmpty(),
    status = e.string("status") ?: TaskStatus.INBOX,
    priority = (e["priority"] as? JsonPrimitive)?.intOrNull ?: DEFAULT_PRIORITY,
    dueAt = e.time("due_at"),
    projectId = e.string("project_id"),
    createdAt = e.time("created_at") ?: System.currentTimeMillis(),
    updatedAt = e.time("updated_at") ?: System.currentTimeMillis(),
    startedAt = e.time("started_at"),
    completedAt = e.time("completed_at"),
    remoteJson = e.toString(),
)
