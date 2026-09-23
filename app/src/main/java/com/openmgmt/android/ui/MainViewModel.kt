package com.openmgmt.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openmgmt.android.data.MainRepository
import com.openmgmt.android.data.OrganizationEntity
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MainRepository) : ViewModel() {

    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val openTasks = repository.openTasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val openTaskCount = repository.openTaskCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeProjectCount = repository.activeProjectCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val organizations = repository.organizations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val organizationCount = repository.organizationCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun saveTask(task: TaskEntity) = viewModelScope.launch { repository.saveTask(task) }
    fun setTaskStatus(task: TaskEntity, status: String) =
        viewModelScope.launch { repository.setTaskStatus(task, status) }
    fun deleteTask(id: String) = viewModelScope.launch { repository.deleteTask(id) }

    fun saveProject(project: ProjectEntity) = viewModelScope.launch { repository.saveProject(project) }
    fun deleteProject(id: String) = viewModelScope.launch { repository.deleteProject(id) }

    fun saveOrganization(organization: OrganizationEntity) =
        viewModelScope.launch { repository.saveOrganization(organization) }
    fun deleteOrganization(id: String) = viewModelScope.launch { repository.deleteOrganization(id) }

    fun projectName(id: String?): String? =
        projects.value.firstOrNull { it.id == id }?.name

    class Factory(private val repository: MainRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}
