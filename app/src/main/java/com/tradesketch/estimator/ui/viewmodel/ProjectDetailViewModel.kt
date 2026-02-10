package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Project Detail screen.
 * Manages current project, spaces, and model operations.
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    
    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadProject()
    }
    
    private fun loadProject() {
        viewModelScope.launch {
            repository.getProjects()
                .map { projects -> projects.find { it.id == projectId } }
                .collect { project ->
                    if (project != null) {
                        _uiState.update {
                            it.copy(
                                project = project,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(error = "Project not found", isLoading = false) }
                    }
                }
        }
    }
    
    fun addSpace(space: Space) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces + space,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun updateSpace(space: Space) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedSpaces = currentProject.spaces.map { 
                if (it.id == space.id) space else it 
            }
            val updatedProject = currentProject.copy(
                spaces = updatedSpaces,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun deleteSpace(spaceId: String) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces.filter { it.id != spaceId },
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun updateProjectName(name: String) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
