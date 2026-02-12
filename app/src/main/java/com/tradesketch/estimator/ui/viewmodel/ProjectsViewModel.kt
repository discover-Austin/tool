package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.usecase.CreateProjectFromTemplateUseCase
import com.tradesketch.estimator.domain.usecase.DeleteProjectUseCase
import com.tradesketch.estimator.domain.usecase.GetSettingsUseCase
import com.tradesketch.estimator.domain.usecase.GetProjectsUseCase
import com.tradesketch.estimator.domain.usecase.SaveSettingsUseCase
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
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
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
            combine(
                getProjectsUseCase(),
                getSettingsUseCase()
            ) { projects, settings ->
                projects to settings
            }
                .catch { error ->
                    _uiState.update { it.copy(error = error.message ?: "Failed to load projects") }
                }
                .collect { (projects, settings) ->
                    _uiState.update { 
                        it.copy(
                            projects = projects.sortedByDescending { p -> p.updatedAt },
                            settings = settings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun completeTradeOnboarding(primaryTrade: PrimaryTrade) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    primaryTrade = primaryTrade,
                    simplifiedHome = true,
                    hasCompletedTradeOnboarding = true
                )
            )
        }
    }

    fun updatePrimaryTrade(primaryTrade: PrimaryTrade) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    primaryTrade = primaryTrade,
                    hasCompletedTradeOnboarding = true
                )
            )
        }
    }

    fun updateSimplifiedHome(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(current.copy(simplifiedHome = enabled))
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
    val settings: Settings = Settings.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null
)
