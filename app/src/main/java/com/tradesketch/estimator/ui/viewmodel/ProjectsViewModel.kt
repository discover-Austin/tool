package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.UxMetricsRepository
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
    private val createFromTemplateUseCase: CreateProjectFromTemplateUseCase,
    private val uxMetricsRepository: UxMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ProjectsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProjectsEvent> = _events.asSharedFlow()

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
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to load projects",
                            isLoading = false
                        )
                    }
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
                    firstRun = false,
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
                    firstRun = false,
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

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

    fun createBlankProject(name: String) {
        viewModelScope.launch {
            try {
                uxMetricsRepository.recordTap("projects_create_blank")
                val project = ProjectTemplate.BLANK.createProject(
                    ensureUniqueProjectName(name)
                )
                saveProjectUseCase(project)
                _events.emit(ProjectsEvent.NavigateToProject(project.id))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create project: ${e.message}") }
            }
        }
    }

    fun createFromTemplate(template: ProjectTemplate, customName: String? = null) {
        viewModelScope.launch {
            try {
                uxMetricsRepository.recordTap("projects_create_template_${template.name.lowercase()}")
                val project = createFromTemplateUseCase(template, customName)
                saveProjectUseCase(
                    project.copy(name = ensureUniqueProjectName(project.name))
                )
                _events.emit(ProjectsEvent.NavigateToProject(project.id))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create project: ${e.message}") }
            }
        }
    }

    fun createEasyStartProject() {
        viewModelScope.launch {
            try {
                uxMetricsRepository.recordTap("projects_easy_start")
                val trade = _uiState.value.settings.primaryTrade
                val project = createStarterProjectForTrade(
                    trade = trade,
                    name = ensureUniqueProjectName(starterProjectNameForTrade(trade))
                )
                saveProjectUseCase(project)
                _events.emit(ProjectsEvent.NavigateToProject(project.id))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to start estimate: ${e.message}") }
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            val normalized = newName.trim()
            if (normalized.isEmpty()) return@launch
            val existing = _uiState.value.projects.firstOrNull { it.id == projectId } ?: return@launch
            val resolvedName = ensureUniqueProjectName(normalized, projectId)
            if (existing.name == resolvedName) return@launch
            runCatching {
                saveProjectUseCase(
                    existing.copy(
                        name = resolvedName,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { error ->
                _uiState.update { it.copy(error = "Failed to rename project: ${error.message}") }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                uxMetricsRepository.recordTap("projects_delete")
                deleteProjectUseCase(projectId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete project: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun ensureUniqueProjectName(
        requestedName: String,
        excludingProjectId: String? = null
    ): String {
        val base = requestedName.trim().ifEmpty { "Project" }
        val existingNames = _uiState.value.projects
            .asSequence()
            .filter { it.id != excludingProjectId }
            .map { it.name.trim().lowercase() }
            .toSet()
        if (base.lowercase() !in existingNames) return base
        var suffix = 2
        while (true) {
            val candidate = "$base ($suffix)"
            if (candidate.lowercase() !in existingNames) return candidate
            suffix++
        }
    }
}

sealed interface ProjectsEvent {
    data class NavigateToProject(val projectId: String) : ProjectsEvent
}

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val settings: Settings = Settings.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null
)
