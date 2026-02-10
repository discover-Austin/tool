package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Takeoff calculation screen.
 * Manages takeoff type selection, parameters, and calculation results.
 */
@HiltViewModel
class TakeoffViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateTakeoffUseCase: CalculateTakeoffUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    
    private val _uiState = MutableStateFlow(TakeoffUiState())
    val uiState: StateFlow<TakeoffUiState> = _uiState.asStateFlow()
    
    init {
        loadProjectAndSettings()
    }
    
    private fun loadProjectAndSettings() {
        viewModelScope.launch {
            combine(
                repository.getProjects().map { it.find { p -> p.id == projectId } },
                settingsRepository.getSettings()
            ) { project, settings ->
                Pair(project, settings)
            }.collect { (project, settings) ->
                if (project != null) {
                    _uiState.update {
                        it.copy(
                            project = project,
                            settings = settings,
                            isLoading = false
                        )
                    }
                    // Auto-calculate if type is selected
                    if (_uiState.value.selectedType != null) {
                        calculate()
                    }
                }
            }
        }
    }
    
    fun selectTakeoffType(type: TakeoffType) {
        _uiState.update { it.copy(selectedType = type) }
        calculate()
    }
    
    fun updateDrywallParams(
        sheetAreaSqFt: Double? = null,
        wastePercent: Double? = null,
        screwsPerSheet: Int? = null,
        mudGallonsPer100SqFt: Double? = null
    ) {
        val current = _uiState.value.drywallParams
        _uiState.update {
            it.copy(
                drywallParams = current.copy(
                    sheetAreaSqFt = sheetAreaSqFt ?: current.sheetAreaSqFt,
                    wastePercent = wastePercent ?: current.wastePercent,
                    screwsPerSheet = screwsPerSheet ?: current.screwsPerSheet,
                    mudGallonsPer100SqFt = mudGallonsPer100SqFt ?: current.mudGallonsPer100SqFt
                )
            )
        }
        calculate()
    }
    
    fun updateConcreteParams(thicknessFeet: Double? = null, wastePercent: Double? = null) {
        val current = _uiState.value.concreteParams
        _uiState.update {
            it.copy(
                concreteParams = current.copy(
                    thicknessFeet = thicknessFeet ?: current.thicknessFeet,
                    wastePercent = wastePercent ?: current.wastePercent
                )
            )
        }
        calculate()
    }
    
    fun updateGravelParams(
        depthFeet: Double? = null,
        densityTonsPerYard: Double? = null,
        wastePercent: Double? = null
    ) {
        val current = _uiState.value.gravelParams
        _uiState.update {
            it.copy(
                gravelParams = current.copy(
                    depthFeet = depthFeet ?: current.depthFeet,
                    densityTonsPerYard = densityTonsPerYard ?: current.densityTonsPerYard,
                    wastePercent = wastePercent ?: current.wastePercent
                )
            )
        }
        calculate()
    }
    
    fun updatePaintParams(
        coverageSqFtPerGallon: Double? = null,
        coats: Int? = null,
        wastePercent: Double? = null
    ) {
        val current = _uiState.value.paintParams
        _uiState.update {
            it.copy(
                paintParams = current.copy(
                    coverageSqFtPerGallon = coverageSqFtPerGallon ?: current.coverageSqFtPerGallon,
                    coats = coats ?: current.coats,
                    wastePercent = wastePercent ?: current.wastePercent
                )
            )
        }
        calculate()
    }
    
    private fun calculate() {
        val state = _uiState.value
        val project = state.project ?: return
        val type = state.selectedType ?: return
        
        try {
            val result = when (type) {
                TakeoffType.DRYWALL -> {
                    val walls = project.spaces.filter { it.geometry is Geometry.Wall }
                    calculateTakeoffUseCase.calculateDrywall(
                        walls,
                        state.drywallParams.sheetAreaSqFt,
                        state.drywallParams.wastePercent,
                        state.drywallParams.screwsPerSheet,
                        state.drywallParams.mudGallonsPer100SqFt
                    )
                }
                TakeoffType.CONCRETE -> {
                    val slabs = project.spaces.filter { it.geometry is Geometry.Slab }
                    calculateTakeoffUseCase.calculateConcrete(
                        slabs,
                        state.concreteParams.thicknessFeet,
                        state.concreteParams.wastePercent
                    )
                }
                TakeoffType.GRAVEL_MULCH -> {
                    calculateTakeoffUseCase.calculateGravelMulch(
                        project.spaces,
                        state.gravelParams.depthFeet,
                        state.gravelParams.densityTonsPerYard,
                        state.gravelParams.wastePercent
                    )
                }
                TakeoffType.PAINT -> {
                    val paintableSpaces = project.spaces.filter { 
                        it.geometry is Geometry.Wall || it.geometry is Geometry.Rect 
                    }
                    calculateTakeoffUseCase.calculatePaint(
                        paintableSpaces,
                        state.paintParams.coverageSqFtPerGallon,
                        state.paintParams.coats,
                        state.paintParams.wastePercent
                    )
                }
            }
            _uiState.update { it.copy(result = result, error = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Calculation failed: ${e.message}") }
        }
    }
}

data class TakeoffUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val selectedType: TakeoffType? = null,
    val drywallParams: DrywallParams = DrywallParams(),
    val concreteParams: ConcreteParams = ConcreteParams(),
    val gravelParams: GravelParams = GravelParams(),
    val paintParams: PaintParams = PaintParams(),
    val result: TakeoffResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

enum class TakeoffType {
    DRYWALL, CONCRETE, GRAVEL_MULCH, PAINT
}

data class DrywallParams(
    val sheetAreaSqFt: Double = 32.0,
    val wastePercent: Double = 10.0,
    val screwsPerSheet: Int = 32,
    val mudGallonsPer100SqFt: Double = 0.5
)

data class ConcreteParams(
    val thicknessFeet: Double = 0.33,
    val wastePercent: Double = 5.0
)

data class GravelParams(
    val depthFeet: Double = 0.25,
    val densityTonsPerYard: Double = 1.4,
    val wastePercent: Double = 10.0
)

data class PaintParams(
    val coverageSqFtPerGallon: Double = 350.0,
    val coats: Int = 2,
    val wastePercent: Double = 5.0
)
