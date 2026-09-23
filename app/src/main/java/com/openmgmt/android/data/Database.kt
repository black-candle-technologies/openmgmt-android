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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object TaskStatus {
    const val OPEN = "open"
    const val IN_PROGRESS = "in_progress"
    const val BLOCKED = "blocked"
    const val DONE = "done"

    val all = listOf(OPEN, IN_PROGRESS, BLOCKED, DONE)
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val status: String = TaskStatus.OPEN,
    val priority: Int = 0,
    /** Epoch millis, null = unscheduled. */
    val dueAt: Long? = null,
    val projectId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val organizationId: String? = null,
    val status: String = "active",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'done' ORDER BY dueAt ASC")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'done'")
    fun observeOpenCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects WHERE status = 'active'")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations ORDER BY name ASC")
    fun observeAll(): Flow<List<OrganizationEntity>>

    @Query("SELECT COUNT(*) FROM organizations")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(organization: OrganizationEntity)

    @Query("DELETE FROM organizations WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [TaskEntity::class, ProjectEntity::class, OrganizationEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun organizationDao(): OrganizationDao

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
                    // Pre-release scaffold: no shipped data to migrate yet.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
