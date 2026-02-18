package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.usecase.GetSettingsUseCase
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import com.tradesketch.estimator.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val saveProjectUseCase: SaveProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun completeRitual(projectName: String, trade: PrimaryTrade) {
        val normalizedName = projectName.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(error = "Project name is required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, completedProjectId = null) }
            runCatching {
                val settings = getSettingsUseCase().first()
                val template = templateForTrade(trade)
                val baseProject = template.createProject(normalizedName)
                val project = baseProject.withTradeScope(trade)
                saveProjectUseCase(project)
                saveSettingsUseCase(
                    settings.copy(
                        primaryTrade = trade,
                        firstRun = false,
                        hasCompletedTradeOnboarding = true
                    )
                )
                project.id
            }.onSuccess { projectId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        completedProjectId = projectId,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "Could not complete ritual"
                    )
                }
            }
        }
    }

    fun clearCompletion() {
        _uiState.update { it.copy(completedProjectId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun templateForTrade(trade: PrimaryTrade): ProjectTemplate {
        return when (trade) {
            PrimaryTrade.DRYWALL -> ProjectTemplate.BEDROOM
            PrimaryTrade.CONCRETE -> ProjectTemplate.GARAGE
            PrimaryTrade.PAINT -> ProjectTemplate.BEDROOM
            PrimaryTrade.GRAVEL_MULCH -> ProjectTemplate.YARD_BED
            PrimaryTrade.MULTI -> ProjectTemplate.BLANK
        }
    }
}

private fun Project.withTradeScope(trade: PrimaryTrade): Project {
    val mappedScope = when (trade) {
        PrimaryTrade.DRYWALL -> TakeoffScope.DRYWALL
        PrimaryTrade.CONCRETE -> TakeoffScope.CONCRETE
        PrimaryTrade.PAINT -> TakeoffScope.PAINT
        PrimaryTrade.GRAVEL_MULCH -> TakeoffScope.GRAVEL_MULCH
        PrimaryTrade.MULTI -> takeoffSession.selectedScope
    }
    val blueprint = if (blueprintDocument.projectId == id) {
        blueprintDocument
    } else {
        BlueprintDocument.fromLegacySpaces(projectId = id, spaces = spaces)
    }
    return copy(
        takeoffSession = takeoffSession.copy(
            selectedScope = mappedScope,
            selectedPlaybook = trade.name
        ),
        blueprintDocument = blueprint
    )
}

data class OnboardingUiState(
    val isSaving: Boolean = false,
    val completedProjectId: String? = null,
    val error: String? = null
)
