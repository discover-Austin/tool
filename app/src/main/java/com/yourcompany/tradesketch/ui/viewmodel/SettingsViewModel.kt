package com.yourcompany.tradesketch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.tradesketch.domain.model.Settings
import com.yourcompany.tradesketch.domain.usecase.GetSettingsUseCase
import com.yourcompany.tradesketch.domain.usecase.SaveSettingsUseCase
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
