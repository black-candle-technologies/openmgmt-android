package com.openmgmt.android.data

import kotlinx.coroutines.flow.Flow

/** Thin data-access layer over the Room DAOs. */
class MainRepository(private val db: AppDatabase) {

    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeAll()
    val openTasks: Flow<List<TaskEntity>> = db.taskDao().observeOpen()
    val openTaskCount: Flow<Int> = db.taskDao().observeOpenCount()
    val projects: Flow<List<ProjectEntity>> = db.projectDao().observeAll()
    val activeProjectCount: Flow<Int> = db.projectDao().observeActiveCount()
    val organizations: Flow<List<OrganizationEntity>> = db.organizationDao().observeAll()
    val organizationCount: Flow<Int> = db.organizationDao().observeCount()

    suspend fun saveTask(task: TaskEntity) = db.taskDao().upsert(task.copy(updatedAt = System.currentTimeMillis()))
    suspend fun setTaskStatus(task: TaskEntity, status: String) =
        db.taskDao().update(task.copy(status = status, updatedAt = System.currentTimeMillis()))
    suspend fun deleteTask(id: String) = db.taskDao().delete(id)

    suspend fun saveProject(project: ProjectEntity) = db.projectDao().upsert(project)
    suspend fun deleteProject(id: String) = db.projectDao().delete(id)

    suspend fun saveOrganization(organization: OrganizationEntity) = db.organizationDao().upsert(organization)
    suspend fun deleteOrganization(id: String) = db.organizationDao().delete(id)
}
