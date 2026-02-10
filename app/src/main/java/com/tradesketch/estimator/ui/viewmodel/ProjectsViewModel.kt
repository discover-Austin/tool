package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.usecase.CreateProjectFromTemplateUseCase
import com.tradesketch.estimator.domain.usecase.DeleteProjectUseCase
import com.tradesketch.estimator.domain.usecase.GetProjectsUseCase
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Projects List screen.
 * Manages project list, creation, and deletion.
 */
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val createFromTemplateUseCase: CreateProjectFromTemplateUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()
    
    init {
        loadProjects()
    }
    
    private fun loadProjects() {
        viewModelScope.launch {
            getProjectsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message ?: "Failed to load projects") }
                }
                .collect { projects ->
                    _uiState.update { 
                        it.copy(
                            projects = projects.sortedByDescending { p -> p.updatedAt },
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
    
    fun createBlankProject(name: String) {
        viewModelScope.launch {
            try {
                val project = ProjectTemplate.BLANK.createProject(name)
                saveProjectUseCase(project)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create project: ${e.message}") }
            }
        }
    }
    
    fun createFromTemplate(template: ProjectTemplate, customName: String? = null) {
        viewModelScope.launch {
            try {
                val project = createFromTemplateUseCase(template, customName)
                saveProjectUseCase(project)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create project: ${e.message}") }
            }
        }
    }
    
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                deleteProjectUseCase(projectId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete project: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
