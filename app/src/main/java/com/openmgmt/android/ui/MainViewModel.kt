package com.openmgmt.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openmgmt.android.data.MainRepository
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MainRepository) : ViewModel() {

    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val organizations = repository.organizations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Tasks in live projects; like the desktop, archived projects hide their tasks. */
    val tasks = combine(repository.tasks, repository.projects) { tasks, projects ->
        val live = projects.mapTo(HashSet()) { it.id }
        tasks.filter { it.projectId == null || it.projectId in live }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Local changes not yet pushed to the sync server. */
    val pendingChangeCount = repository.pendingChangeCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun saveTask(task: TaskEntity) = viewModelScope.launch { repository.saveTask(task) }
    fun setTaskStatus(task: TaskEntity, status: String) =
        viewModelScope.launch { repository.setTaskStatus(task, status) }
    fun deleteTask(id: String) = viewModelScope.launch { repository.deleteTask(id) }

    fun saveProject(project: ProjectEntity) = viewModelScope.launch { repository.saveProject(project) }
    fun deleteProject(id: String) = viewModelScope.launch { repository.deleteProject(id) }

    fun saveOrganization(organization: OrganizationEntity) =
        viewModelScope.launch { repository.saveOrganization(organization) }
    fun deleteOrganization(id: String) = viewModelScope.launch { repository.deleteOrganization(id) }

    /** Project id → name, observable so task cards update when projects load. */
    val projectNames = repository.projects
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    class Factory(private val repository: MainRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}
