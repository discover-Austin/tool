package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.domain.model.PrimaryTrade
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
                val project = createStarterProjectForTrade(
                    trade = trade,
                    name = normalizedName
                )
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
}

data class OnboardingUiState(
    val isSaving: Boolean = false,
    val completedProjectId: String? = null,
    val error: String? = null
)
