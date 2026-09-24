package com.openmgmt.android.data

import android.util.Log
import androidx.room.withTransaction
import com.openmgmt.android.sync.SyncEntityType
import com.openmgmt.android.sync.SyncEvent
import com.openmgmt.android.sync.SyncOperation
import com.openmgmt.android.sync.archivePayload
import com.openmgmt.android.sync.entityPayload
import com.openmgmt.android.sync.organizationFromSync
import com.openmgmt.android.sync.parseTimestamp
import com.openmgmt.android.sync.projectFromSync
import com.openmgmt.android.sync.taskFromSync
import com.openmgmt.android.sync.toSyncJson
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** Name of the organization / project that catch unassigned work, like the desktop's seed. */
private const val DEFAULT_ORGANIZATION_NAME = "Personal"
private const val DEFAULT_PROJECT_NAME = "Inbox"

/** The live organization unassigned projects are filed under, if it exists yet. */
fun defaultOrganizationOf(organizations: List<OrganizationEntity>): OrganizationEntity? =
    organizations.firstOrNull {
        it.archivedAt == null && it.name.equals(DEFAULT_ORGANIZATION_NAME, ignoreCase = true)
    }

/** The live project unassigned tasks are filed in, if it exists yet. */
fun defaultProjectOf(projects: List<ProjectEntity>, organizations: List<OrganizationEntity>): ProjectEntity? {
    val org = defaultOrganizationOf(organizations) ?: return null
    return projects.firstOrNull {
        it.organizationId == org.id && it.archivedAt == null &&
            it.status != ProjectStatus.ARCHIVED &&
            it.name.equals(DEFAULT_PROJECT_NAME, ignoreCase = true)
    }
}

/**
 * Data access over the Room DAOs. Every local mutation records its sync
 * event in the same transaction (the desktop's append_sync_event), so the
 * next sync pushes exactly what changed. Nothing is hard-deleted: like the
 * desktop, organizations and projects are archived and tasks canceled, so
 * the removal reaches other devices.
 */
class MainRepository(private val db: AppDatabase) {

    private val tasksDao = db.taskDao()
    private val projectsDao = db.projectDao()
    private val orgsDao = db.organizationDao()
    private val syncDao = db.syncDao()

    val tasks: Flow<List<TaskEntity>> = tasksDao.observeAll()
    val projects: Flow<List<ProjectEntity>> = projectsDao.observeAll()
    val organizations: Flow<List<OrganizationEntity>> = orgsDao.observeAll()
    val pendingChangeCount: Flow<Int> = syncDao.observeUnsyncedCount()

    // ---- Local mutations ----

    suspend fun saveTask(task: TaskEntity) = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = tasksDao.get(task.id)
        // A new task counts as entering its status, like the desktop's transition_task.
        val entered = existing == null || existing.status != task.status
        val saved = task.copy(
            projectId = task.projectId ?: ensureDefaultProject().id,
            createdAt = existing?.createdAt ?: task.createdAt,
            updatedAt = now,
            startedAt = task.startedAt ?: now.takeIf { entered && task.status == TaskStatus.IN_PROGRESS },
            completedAt = if (entered && task.status == TaskStatus.DONE) now else task.completedAt,
        )
        tasksDao.upsert(saved)
        // "transitioned" when the status is all that changed, as on desktop.
        val transitionOnly = existing != null && entered &&
            existing.copy(status = saved.status, updatedAt = 0, startedAt = null, completedAt = null) ==
            saved.copy(updatedAt = 0, startedAt = null, completedAt = null)
        record(
            SyncEntityType.TASK, saved.id,
            when {
                existing == null -> SyncOperation.CREATED
                transitionOnly -> SyncOperation.TRANSITIONED
                else -> SyncOperation.UPDATED
            },
            entityPayload(saved.toSyncJson()),
        )
    }

    suspend fun setTaskStatus(task: TaskEntity, status: String) = saveTask(task.copy(status = status))

    /** Archives the task (desktop: status → canceled). Undo by saving the original again. */
    suspend fun deleteTask(id: String) = db.withTransaction {
        val task = tasksDao.get(id) ?: return@withTransaction
        val now = System.currentTimeMillis()
        tasksDao.upsert(task.copy(status = TaskStatus.CANCELED, updatedAt = now))
        record(SyncEntityType.TASK, id, SyncOperation.ARCHIVED, archivePayload(id, now))
    }

    suspend fun saveProject(project: ProjectEntity) = db.withTransaction {
        val existing = projectsDao.get(project.id)
        val saved = project.copy(
            organizationId = project.organizationId ?: ensureDefaultOrganization().id,
            createdAt = existing?.createdAt ?: project.createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        projectsDao.upsert(saved)
        record(
            SyncEntityType.PROJECT, saved.id,
            if (existing == null) SyncOperation.CREATED else SyncOperation.UPDATED,
            entityPayload(saved.toSyncJson()),
        )
    }

    suspend fun deleteProject(id: String) = db.withTransaction {
        val project = projectsDao.get(id) ?: return@withTransaction
        val now = System.currentTimeMillis()
        projectsDao.upsert(project.copy(status = ProjectStatus.ARCHIVED, archivedAt = now, updatedAt = now))
        record(SyncEntityType.PROJECT, id, SyncOperation.ARCHIVED, archivePayload(id, now))
    }

    suspend fun saveOrganization(organization: OrganizationEntity) = db.withTransaction {
        val existing = orgsDao.get(organization.id)
        val saved = organization.copy(
            createdAt = existing?.createdAt ?: organization.createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        orgsDao.upsert(saved)
        record(
            SyncEntityType.ORGANIZATION, saved.id,
            if (existing == null) SyncOperation.CREATED else SyncOperation.UPDATED,
            entityPayload(saved.toSyncJson()),
        )
    }

    suspend fun deleteOrganization(id: String) = db.withTransaction {
        val org = orgsDao.get(id) ?: return@withTransaction
        val now = System.currentTimeMillis()
        orgsDao.upsert(org.copy(archivedAt = now, updatedAt = now))
        record(SyncEntityType.ORGANIZATION, id, SyncOperation.ARCHIVED, archivePayload(id, now))
    }

    // ---- Defaults: the desktop requires every task to have a project and
    // every project an organization. ----

    private suspend fun ensureDefaultOrganization(): OrganizationEntity {
        defaultOrganizationOf(orgsDao.getAll())?.let { return it }
        val org = OrganizationEntity(name = DEFAULT_ORGANIZATION_NAME)
        orgsDao.upsert(org)
        record(SyncEntityType.ORGANIZATION, org.id, SyncOperation.CREATED, entityPayload(org.toSyncJson()))
        return org
    }

    private suspend fun ensureDefaultProject(): ProjectEntity {
        val org = ensureDefaultOrganization()
        defaultProjectOf(projectsDao.getAll(), orgsDao.getAll())?.let { return it }
        val project = ProjectEntity(name = DEFAULT_PROJECT_NAME, organizationId = org.id)
        projectsDao.upsert(project)
        record(SyncEntityType.PROJECT, project.id, SyncOperation.CREATED, entityPayload(project.toSyncJson()))
        return project
    }

    private suspend fun record(entityType: String, entityId: String, operation: String, payload: JsonObject) {
        syncDao.insertEvent(
            SyncEventEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = payload.toString(),
            )
        )
    }

    // ---- Sync support (used by SyncManager) ----

    suspend fun unsyncedEvents(limit: Int): List<SyncEventEntity> = syncDao.unsyncedEvents(limit)

    suspend fun markSynced(eventIds: List<String>) {
        if (eventIds.isNotEmpty()) syncDao.markSynced(eventIds, System.currentTimeMillis())
    }

    suspend fun checkpoint(): String? = syncDao.getState(SyncStateKeys.SERVER_CHECKPOINT)

    /** Forget the server position and re-push everything on the next sync (new account/device). */
    suspend fun resetSyncState() = db.withTransaction {
        syncDao.clearState(SyncStateKeys.SERVER_CHECKPOINT)
        syncDao.setState(SyncStateEntity(SyncStateKeys.SNAPSHOT_PENDING, "1"))
    }

    /**
     * If a snapshot is pending, replaces the unsynced event log with one
     * full-state event per organization, project, and task (archived ones
     * included), so a server that has never seen this data gets all of it.
     * Must run before pulling, so pulled events can't overwrite local rows.
     */
    suspend fun snapshotIfPending() = db.withTransaction {
        if (syncDao.getState(SyncStateKeys.SNAPSHOT_PENDING) == null) return@withTransaction
        syncDao.deleteUnsynced()
        // Data from before sync support may lack parents; file it like new work.
        projectsDao.getAll().filter { it.organizationId == null }.forEach {
            projectsDao.upsert(it.copy(organizationId = ensureDefaultOrganization().id))
        }
        tasksDao.getAll().filter { it.projectId == null }.forEach {
            tasksDao.upsert(it.copy(projectId = ensureDefaultProject().id))
        }
        // ensureDefault* may have just logged "created" events; the full
        // snapshot below supersedes them.
        syncDao.deleteUnsynced()
        orgsDao.getAll().forEach {
            record(SyncEntityType.ORGANIZATION, it.id, SyncOperation.UPDATED, entityPayload(it.toSyncJson()))
        }
        projectsDao.getAll().forEach {
            record(SyncEntityType.PROJECT, it.id, SyncOperation.UPDATED, entityPayload(it.toSyncJson()))
        }
        tasksDao.getAll().forEach {
            record(SyncEntityType.TASK, it.id, SyncOperation.UPDATED, entityPayload(it.toSyncJson()))
        }
        syncDao.clearState(SyncStateKeys.SNAPSHOT_PENDING)
    }

    /**
     * Replays one pulled page, then stores [checkpoint], all in one
     * transaction: a crash either applies the page and advances, or neither.
     *
     * Conflicts are last-write-wins in server order. This device pushes
     * after it pulls, so an entity with unpushed local changes keeps them:
     * those changes will land later in server order and win everywhere.
     * Events from [localDeviceId] are echoes and are skipped. Malformed or
     * unsupported events are skipped (and logged) rather than wedging sync.
     */
    suspend fun applyRemotePage(
        events: List<SyncEvent>,
        checkpoint: String,
        localDeviceId: String,
    ): Int = db.withTransaction {
        var applied = 0
        val ordered = events.sortedBy {
            when (it.entityType) {
                SyncEntityType.ORGANIZATION -> 0
                SyncEntityType.PROJECT -> 1
                else -> 2
            }
        }
        for (event in ordered) {
            if (event.deviceId == localDeviceId || syncDao.isApplied(event.eventId)) continue
            if (!syncDao.hasPending(event.entityType, event.entityId)) {
                runCatching { applyRemote(event) }
                    .onSuccess { applied++ }
                    .onFailure { Log.w("OpenMGMT", "Skipping remote event ${event.eventId}", it) }
            }
            syncDao.markApplied(AppliedRemoteEventEntity(event.eventId))
        }
        if (checkpoint.isNotEmpty()) {
            syncDao.setState(SyncStateEntity(SyncStateKeys.SERVER_CHECKPOINT, checkpoint))
        }
        applied
    }

    private suspend fun applyRemote(event: SyncEvent) {
        val payload = event.payloadJson
        if (event.operation == SyncOperation.ARCHIVED) {
            val id = (payload["id"] as JsonPrimitive).content
            require(id == event.entityId) { "archive id mismatch" }
            val at = parseTimestamp((payload["archived_at"] as JsonPrimitive).content)
            when (event.entityType) {
                SyncEntityType.ORGANIZATION -> orgsDao.get(id)?.let {
                    orgsDao.upsert(it.copy(archivedAt = at, updatedAt = at))
                }
                SyncEntityType.PROJECT -> projectsDao.get(id)?.let {
                    projectsDao.upsert(it.copy(status = ProjectStatus.ARCHIVED, archivedAt = at, updatedAt = at))
                }
                SyncEntityType.TASK -> tasksDao.get(id)?.let {
                    tasksDao.upsert(it.copy(status = TaskStatus.CANCELED, updatedAt = at))
                }
                else -> error("unsupported entity type ${event.entityType}")
            }
            return
        }
        require(
            event.operation in setOf(SyncOperation.CREATED, SyncOperation.UPDATED, SyncOperation.TRANSITIONED)
        ) { "unsupported operation ${event.operation}" }
        val entity = payload["entity"]!!.jsonObject
        require((entity["id"] as? JsonPrimitive)?.contentOrNull == event.entityId) { "entity id mismatch" }
        when (event.entityType) {
            SyncEntityType.ORGANIZATION -> orgsDao.upsert(organizationFromSync(entity))
            SyncEntityType.PROJECT -> projectsDao.upsert(projectFromSync(entity))
            SyncEntityType.TASK -> tasksDao.upsert(taskFromSync(entity))
            else -> error("unsupported entity type ${event.entityType}")
        }
    }
}
