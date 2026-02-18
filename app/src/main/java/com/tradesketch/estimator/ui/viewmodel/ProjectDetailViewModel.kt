package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Project Detail screen.
 * Manages current project observation and basic project operations.
 * Note: Blueprint editing happens in BlueprintEditorViewModel.
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val uxMetricsRepository: UxMetricsRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        currentProjectId?.let { observeProject(it) }
    }

    fun setProjectId(projectId: String) {
        if (currentProjectId == projectId && projectObserverJob != null) return
        currentProjectId = projectId
        savedStateHandle["projectId"] = projectId
        observeProject(projectId)
    }

    private fun observeProject(projectId: String) {
        projectObserverJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        projectObserverJob = viewModelScope.launch {
            combine(
                repository.getProjects().map { projects -> projects.find { it.id == projectId } },
                settingsRepository.getSettings()
            ) { project, settings -> project to settings }
                .collect { (project, settings) ->
                    if (project != null) {
                        _uiState.update {
                            it.copy(
                                project = project,
                                settings = settings,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                project = null,
                                settings = settings,
                                error = "Project not found",
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun updateProjectName(name: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_rename_project")
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun applyProjectEstimateProfile(primaryTrade: PrimaryTrade) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val mappedScope = when (primaryTrade) {
                PrimaryTrade.DRYWALL -> TakeoffScope.DRYWALL
                PrimaryTrade.CONCRETE -> TakeoffScope.CONCRETE
                PrimaryTrade.PAINT -> TakeoffScope.PAINT
                PrimaryTrade.GRAVEL_MULCH -> TakeoffScope.GRAVEL_MULCH
                PrimaryTrade.MULTI -> currentProject.takeoffSession.selectedScope
            }
            val updatedSession = currentProject.takeoffSession.copy(
                selectedScope = mappedScope,
                selectedPlaybook = primaryTrade.name,
                snapSettings = snapSettingsProfileForTrade(primaryTrade)
            )
            if (updatedSession == currentProject.takeoffSession) return@launch
            saveProjectUseCase(
                currentProject.copy(
                    takeoffSession = updatedSession,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }
}

private fun snapSettingsProfileForTrade(trade: PrimaryTrade): BlueprintSnapSettings {
    return when (trade) {
        PrimaryTrade.DRYWALL -> BlueprintSnapSettings(
            gridEnabled = true,
            endpointEnabled = true,
            midpointEnabled = true,
            angleEnabled = true,
            closureEnabled = true,
            gridStepFeet = 1.0,
            angleIncrementDegrees = 15,
            thresholdFeet = 0.75
        )
        PrimaryTrade.CONCRETE -> BlueprintSnapSettings(
            gridEnabled = true,
            endpointEnabled = true,
            midpointEnabled = false,
            angleEnabled = true,
            closureEnabled = true,
            gridStepFeet = 0.5,
            angleIncrementDegrees = 45,
            thresholdFeet = 0.6
        )
        PrimaryTrade.PAINT -> BlueprintSnapSettings(
            gridEnabled = true,
            endpointEnabled = true,
            midpointEnabled = true,
            angleEnabled = true,
            closureEnabled = true,
            gridStepFeet = 1.0,
            angleIncrementDegrees = 15,
            thresholdFeet = 0.5
        )
        PrimaryTrade.GRAVEL_MULCH -> BlueprintSnapSettings(
            gridEnabled = true,
            endpointEnabled = true,
            midpointEnabled = true,
            angleEnabled = false,
            closureEnabled = true,
            gridStepFeet = 1.0,
            angleIncrementDegrees = 0,
            thresholdFeet = 0.5
        )
        PrimaryTrade.MULTI -> BlueprintSnapSettings(
            gridEnabled = true,
            endpointEnabled = true,
            midpointEnabled = true,
            angleEnabled = true,
            closureEnabled = true,
            gridStepFeet = 1.0,
            angleIncrementDegrees = 15,
            thresholdFeet = 0.75
        )
    }
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null
)
