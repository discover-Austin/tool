package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Settings
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
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
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
    
    fun resetToDefaults() {
        viewModelScope.launch {
            saveSettingsUseCase(Settings.DEFAULT)
        }
    }
}

data class SettingsUiState(
    val settings: Settings = Settings.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null
)
