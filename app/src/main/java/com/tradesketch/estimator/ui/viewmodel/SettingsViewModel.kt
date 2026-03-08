package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.UxMetricsSnapshot
import com.tradesketch.estimator.domain.usecase.GetSettingsUseCase
import com.tradesketch.estimator.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings/About screen.
 * Manages app settings and preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val uxMetricsRepository: UxMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeMetrics()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message ?: "Failed to load settings") }
                }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            settings = settings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun updateDefaultWaste(wastePercent: Double) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(current.copy(defaultWastePercent = wastePercent))
        }
    }

    fun updateUseMetric(useMetric: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(current.copy(useMetric = useMetric))
        }
    }

    private fun observeMetrics() {
        viewModelScope.launch {
            uxMetricsRepository.getMetrics().collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
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

    fun updateReducedMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(current.copy(reducedMotionEnabled = enabled))
        }
    }

    fun setAppTutorialCompleted(completed: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            if (current.hasCompletedAppTutorial == completed) return@launch
            saveSettingsUseCase(current.copy(hasCompletedAppTutorial = completed))
        }
    }

    fun setTouchModeQuickToolsTutorialSeen(seen: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            if (current.hasSeenTouchModeQuickToolsTutorial == seen) return@launch
            saveSettingsUseCase(current.copy(hasSeenTouchModeQuickToolsTutorial = seen))
        }
    }

    fun updateDrywallDefaults(sheetArea: Double? = null, screwsPerSheet: Int? = null, mudGallons: Double? = null) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    defaultDrywallSheetArea = sheetArea ?: current.defaultDrywallSheetArea,
                    defaultScrewsPerSheet = screwsPerSheet ?: current.defaultScrewsPerSheet,
                    defaultMudGallonsPer100SqFt = mudGallons ?: current.defaultMudGallonsPer100SqFt
                )
            )
        }
    }

    fun updatePaintDefaults(coverage: Double? = null, coats: Int? = null) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    defaultCoveragePerGallon = coverage ?: current.defaultCoveragePerGallon,
                    defaultCoatsOfPaint = coats ?: current.defaultCoatsOfPaint
                )
            )
        }
    }

    fun updatePricingDefaults(
        drywallSheetCost: Double? = null,
        drywallScrewCost: Double? = null,
        drywallMudCost: Double? = null,
        concreteYardCost: Double? = null,
        gravelYardCost: Double? = null,
        gravelTonCost: Double? = null,
        paintGallonCost: Double? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    drywallSheetUnitCost = drywallSheetCost ?: current.drywallSheetUnitCost,
                    drywallScrewUnitCost = drywallScrewCost ?: current.drywallScrewUnitCost,
                    drywallMudUnitCost = drywallMudCost ?: current.drywallMudUnitCost,
                    concreteYardUnitCost = concreteYardCost ?: current.concreteYardUnitCost,
                    gravelYardUnitCost = gravelYardCost ?: current.gravelYardUnitCost,
                    gravelTonUnitCost = gravelTonCost ?: current.gravelTonUnitCost,
                    paintGallonUnitCost = paintGallonCost ?: current.paintGallonUnitCost
                )
            )
        }
    }

    fun updateBusinessDefaults(
        laborPercent: Double? = null,
        markupPercent: Double? = null,
        taxPercent: Double? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    laborPercent = laborPercent ?: current.laborPercent,
                    markupPercent = markupPercent ?: current.markupPercent,
                    taxPercent = taxPercent ?: current.taxPercent
                )
            )
        }
    }

    fun updateBusinessIdentity(
        businessName: String? = null,
        businessPhone: String? = null,
        businessEmail: String? = null,
        businessAddress: String? = null,
        businessLicense: String? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    businessName = businessName ?: current.businessName,
                    businessPhone = businessPhone ?: current.businessPhone,
                    businessEmail = businessEmail ?: current.businessEmail,
                    businessAddress = businessAddress ?: current.businessAddress,
                    businessLicense = businessLicense ?: current.businessLicense
                )
            )
        }
    }

    fun updateBlueprintSnapDefaults(
        gridEnabled: Boolean? = null,
        endpointEnabled: Boolean? = null,
        midpointEnabled: Boolean? = null,
        angleEnabled: Boolean? = null,
        closureEnabled: Boolean? = null,
        thresholdFeet: Double? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    blueprintSnapGridEnabled = gridEnabled ?: current.blueprintSnapGridEnabled,
                    blueprintSnapEndpointEnabled = endpointEnabled ?: current.blueprintSnapEndpointEnabled,
                    blueprintSnapMidpointEnabled = midpointEnabled ?: current.blueprintSnapMidpointEnabled,
                    blueprintSnapAngleEnabled = angleEnabled ?: current.blueprintSnapAngleEnabled,
                    blueprintSnapClosureEnabled = closureEnabled ?: current.blueprintSnapClosureEnabled,
                    blueprintSnapThresholdFeet = thresholdFeet ?: current.blueprintSnapThresholdFeet
                )
            )
        }
    }

    fun updateBlueprintControlDefaults(
        dualJoysticksEnabled: Boolean? = null,
        joystickSensitivity: Float? = null,
        joystickDeadzone: Float? = null,
        largeCursorEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettingsUseCase(
                current.copy(
                    blueprintDualJoysticksEnabled = dualJoysticksEnabled ?: current.blueprintDualJoysticksEnabled,
                    blueprintJoystickSensitivity = joystickSensitivity ?: current.blueprintJoystickSensitivity,
                    blueprintJoystickDeadzone = joystickDeadzone ?: current.blueprintJoystickDeadzone,
                    blueprintLargeCursorEnabled = largeCursorEnabled ?: current.blueprintLargeCursorEnabled
                )
            )
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            saveSettingsUseCase(Settings.DEFAULT)
        }
    }

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

}

data class SettingsUiState(
    val settings: Settings = Settings.DEFAULT,
    val metrics: UxMetricsSnapshot = UxMetricsSnapshot(),
    val isLoading: Boolean = true,
    val error: String? = null
)
