package com.openmgmt.android.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Task statuses, matching the desktop app's TaskStatus (openmgmt-core models.rs). */
object TaskStatus {
    const val INBOX = "inbox"
    const val BACKLOG = "backlog"
    const val SCHEDULED = "scheduled"
    const val READY = "ready"
    const val IN_PROGRESS = "in_progress"
    const val BLOCKED = "blocked"
    const val WAITING = "waiting"
    const val DONE = "done"

    /** Archived tasks; hidden everywhere (the desktop's "archive" for tasks). */
    const val CANCELED = "canceled"

    /** Statuses a user can pick, in the desktop's order. */
    val selectable = listOf(INBOX, BACKLOG, SCHEDULED, READY, IN_PROGRESS, BLOCKED, WAITING, DONE)
}

/** Project statuses, matching the desktop app's ProjectStatus. */
object ProjectStatus {
    const val ACTIVE = "active"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val ARCHIVED = "archived"
}

/** Desktop priorities run P1 (highest) .. P5 (lowest). */
const val DEFAULT_PRIORITY = 3

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val status: String = TaskStatus.INBOX,
    val priority: Int = DEFAULT_PRIORITY,
    /** Epoch millis, null = unscheduled. */
    val dueAt: Long? = null,
    /** Null only until saved: the repository files project-less tasks in Inbox. */
    val projectId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    /** Last full sync entity; keeps desktop-only fields intact on round trips. */
    val remoteJson: String? = null,
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Null only until saved: the repository files org-less projects in Personal. */
    val organizationId: String? = null,
    val status: String = ProjectStatus.ACTIVE,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null,
    val remoteJson: String? = null,
)

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null,
    val remoteJson: String? = null,
)

/**
 * A local change waiting to be pushed (omgp/1 sync event). [sequence] is
 * never reused, so it stays unique per device id even across sign-outs.
 * The device id is stamped at push time.
 */
@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
)

/** Remote events already replayed, so re-pulled pages are idempotent. */
@Entity(tableName = "applied_remote_events")
data class AppliedRemoteEventEntity(
    @PrimaryKey val eventId: String,
    val appliedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status != 'canceled' ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE archivedAt IS NULL AND status != 'archived' ORDER BY name ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations WHERE archivedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<OrganizationEntity>>

    @Query("SELECT * FROM organizations")
    suspend fun getAll(): List<OrganizationEntity>

    @Query("SELECT * FROM organizations WHERE id = :id")
    suspend fun get(id: String): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(organization: OrganizationEntity)
}

@Dao
interface SyncDao {
    @Insert
    suspend fun insertEvent(event: SyncEventEntity): Long

    @Query("SELECT * FROM sync_events WHERE syncedAt IS NULL ORDER BY sequence ASC LIMIT :limit")
    suspend fun unsyncedEvents(limit: Int): List<SyncEventEntity>

    @Query("SELECT COUNT(*) FROM sync_events WHERE syncedAt IS NULL")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE sync_events SET syncedAt = :syncedAt WHERE eventId IN (:eventIds)")
    suspend fun markSynced(eventIds: List<String>, syncedAt: Long)

    @Query("DELETE FROM sync_events WHERE syncedAt IS NULL")
    suspend fun deleteUnsynced()

    @Query(
        "SELECT EXISTS(SELECT 1 FROM sync_events WHERE syncedAt IS NULL " +
            "AND entityType = :entityType AND entityId = :entityId)"
    )
    suspend fun hasPending(entityType: String, entityId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM applied_remote_events WHERE eventId = :eventId)")
    suspend fun isApplied(eventId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markApplied(event: AppliedRemoteEventEntity)

    @Query("SELECT value FROM sync_state WHERE `key` = :key")
    suspend fun getState(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setState(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE `key` = :key")
    suspend fun clearState(key: String)
}

/** sync_state keys. */
object SyncStateKeys {
    /** Opaque server checkpoint from the last fully applied pull page. */
    const val SERVER_CHECKPOINT = "server_checkpoint"

    /** Set when the whole local state must be re-pushed (upgrade, new account). */
    const val SNAPSHOT_PENDING = "snapshot_pending"
}

/**
 * v2 → v3: sync support. Adds created/archived timestamps and the
 * round-trip entity JSON, the event log, and sync bookkeeping; maps the
 * old Android-only "open" status and unset priority to desktop values;
 * and queues a snapshot so existing data is pushed on the first sync.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN startedAt INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN remoteJson TEXT")
        db.execSQL("UPDATE tasks SET createdAt = updatedAt")
        db.execSQL("UPDATE tasks SET status = 'inbox' WHERE status = 'open'")
        db.execSQL("UPDATE tasks SET priority = $DEFAULT_PRIORITY WHERE priority NOT BETWEEN 1 AND 5")
        db.execSQL("UPDATE tasks SET completedAt = updatedAt WHERE status = 'done'")

        db.execSQL("ALTER TABLE projects ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE projects ADD COLUMN archivedAt INTEGER")
        db.execSQL("ALTER TABLE projects ADD COLUMN remoteJson TEXT")
        db.execSQL("UPDATE projects SET createdAt = updatedAt")

        db.execSQL("ALTER TABLE organizations ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE organizations ADD COLUMN archivedAt INTEGER")
        db.execSQL("ALTER TABLE organizations ADD COLUMN remoteJson TEXT")
        db.execSQL("UPDATE organizations SET createdAt = updatedAt")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_events` (" +
                "`sequence` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`eventId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                "`operation` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `syncedAt` INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `applied_remote_events` (" +
                "`eventId` TEXT NOT NULL, `appliedAt` INTEGER NOT NULL, PRIMARY KEY(`eventId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_state` (" +
                "`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))"
        )
        db.execSQL(
            "INSERT OR REPLACE INTO sync_state (`key`, `value`) " +
                "VALUES ('${SyncStateKeys.SNAPSHOT_PENDING}', '1')"
        )
    }
}

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        OrganizationEntity::class,
        SyncEventEntity::class,
        AppliedRemoteEventEntity::class,
        SyncStateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openmgmt.db",
                )
                    .addMigrations(MIGRATION_2_3)
                    // v1 predates any shipped build; nothing to preserve.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                    .also { instance = it }
            }
    }
}
