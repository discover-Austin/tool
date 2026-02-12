package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null

    private val _uiState = MutableStateFlow(TakeoffUiState())
    val uiState: StateFlow<TakeoffUiState> = _uiState.asStateFlow()

    init {
        currentProjectId?.let { observeProjectAndSettings(it) }
    }

    fun setProjectId(projectId: String) {
        if (currentProjectId == projectId && projectObserverJob != null) return
        currentProjectId = projectId
        savedStateHandle["projectId"] = projectId
        observeProjectAndSettings(projectId)
    }

    private fun observeProjectAndSettings(projectId: String) {
        projectObserverJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        projectObserverJob = viewModelScope.launch {
            combine(
                repository.getProjects().map { it.find { p -> p.id == projectId } },
                settingsRepository.getSettings()
            ) { project, settings ->
                Pair(project, settings)
            }.collect { (project, settings) ->
                if (project != null) {
                    val alreadyInitializedForProject =
                        !_uiState.value.isLoading && _uiState.value.project?.id == project.id
                    _uiState.update {
                        it.copy(
                            project = project,
                            settings = settings,
                            selectedType = it.selectedType ?: defaultTakeoffTypeForTrade(settings.primaryTrade),
                            drywallParams = if (alreadyInitializedForProject) {
                                it.drywallParams
                            } else {
                                DrywallParams(
                                    sheetAreaSqFt = settings.defaultDrywallSheetArea,
                                    wastePercent = settings.defaultWastePercent,
                                    screwsPerSheet = settings.defaultScrewsPerSheet,
                                    mudGallonsPer100SqFt = settings.defaultMudGallonsPer100SqFt
                                )
                            },
                            concreteParams = if (alreadyInitializedForProject) {
                                it.concreteParams
                            } else {
                                ConcreteParams(
                                    thicknessFeet = 0.33,
                                    wastePercent = settings.defaultWastePercent
                                )
                            },
                            gravelParams = if (alreadyInitializedForProject) {
                                it.gravelParams
                            } else {
                                GravelParams(
                                    depthFeet = 0.25,
                                    densityTonsPerYard = 1.4,
                                    wastePercent = settings.defaultWastePercent
                                )
                            },
                            paintParams = if (alreadyInitializedForProject) {
                                it.paintParams
                            } else {
                                PaintParams(
                                    coverageSqFtPerGallon = settings.defaultCoveragePerGallon,
                                    coats = settings.defaultCoatsOfPaint,
                                    wastePercent = settings.defaultWastePercent
                                )
                            },
                            pricingParams = if (alreadyInitializedForProject) {
                                it.pricingParams
                            } else {
                                PricingParams.fromSettings(settings)
                            },
                            isLoading = false,
                            error = null
                        )
                    }
                    // Auto-calculate if type is selected
                    if (_uiState.value.selectedType != null) {
                        calculate()
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Project not found") }
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

    fun updatePricingParams(
        drywallSheetCost: Double? = null,
        drywallScrewCost: Double? = null,
        drywallMudCost: Double? = null,
        concreteYardCost: Double? = null,
        gravelYardCost: Double? = null,
        gravelTonCost: Double? = null,
        paintGallonCost: Double? = null,
        laborPercent: Double? = null,
        markupPercent: Double? = null,
        taxPercent: Double? = null
    ) {
        val current = _uiState.value.pricingParams
        _uiState.update {
            it.copy(
                pricingParams = current.copy(
                    drywallSheetCost = drywallSheetCost ?: current.drywallSheetCost,
                    drywallScrewCost = drywallScrewCost ?: current.drywallScrewCost,
                    drywallMudCost = drywallMudCost ?: current.drywallMudCost,
                    concreteYardCost = concreteYardCost ?: current.concreteYardCost,
                    gravelYardCost = gravelYardCost ?: current.gravelYardCost,
                    gravelTonCost = gravelTonCost ?: current.gravelTonCost,
                    paintGallonCost = paintGallonCost ?: current.paintGallonCost,
                    laborPercent = laborPercent ?: current.laborPercent,
                    markupPercent = markupPercent ?: current.markupPercent,
                    taxPercent = taxPercent ?: current.taxPercent
                )
            )
        }
        calculate()
    }
    
    private fun calculate() {
        val state = _uiState.value
        val project = state.project ?: return
        val type = state.selectedType ?: return
        val pricing = state.pricingParams
        
        try {
            val result = when (type) {
                TakeoffType.DRYWALL -> {
                    val walls = project.spaces.filter { it.geometry is Geometry.Wall }
                    calculateTakeoffUseCase.calculateDrywall(
                        walls,
                        state.drywallParams.sheetAreaSqFt,
                        state.drywallParams.wastePercent,
                        state.drywallParams.screwsPerSheet,
                        state.drywallParams.mudGallonsPer100SqFt,
                        CostingInputs(
                            unitCostByLineName = mapOf(
                                "Drywall sheets" to pricing.drywallSheetCost,
                                "Drywall screws" to pricing.drywallScrewCost,
                                "Joint compound" to pricing.drywallMudCost
                            ),
                            laborPercent = pricing.laborPercent,
                            markupPercent = pricing.markupPercent,
                            taxPercent = pricing.taxPercent
                        )
                    )
                }
                TakeoffType.CONCRETE -> {
                    val slabs = project.spaces.filter { it.geometry is Geometry.Slab }
                    calculateTakeoffUseCase.calculateConcrete(
                        slabs,
                        state.concreteParams.thicknessFeet,
                        state.concreteParams.wastePercent,
                        CostingInputs(
                            unitCostByLineName = mapOf(
                                "Concrete volume" to pricing.concreteYardCost
                            ),
                            laborPercent = pricing.laborPercent,
                            markupPercent = pricing.markupPercent,
                            taxPercent = pricing.taxPercent
                        )
                    )
                }
                TakeoffType.GRAVEL_MULCH -> {
                    calculateTakeoffUseCase.calculateGravelMulch(
                        project.spaces,
                        state.gravelParams.depthFeet,
                        state.gravelParams.densityTonsPerYard,
                        state.gravelParams.wastePercent,
                        CostingInputs(
                            unitCostByLineName = mapOf(
                                "Material volume" to pricing.gravelYardCost,
                                "Material weight" to pricing.gravelTonCost
                            ),
                            laborPercent = pricing.laborPercent,
                            markupPercent = pricing.markupPercent,
                            taxPercent = pricing.taxPercent
                        )
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
                        state.paintParams.wastePercent,
                        CostingInputs(
                            unitCostByLineName = mapOf(
                                "Paint" to pricing.paintGallonCost
                            ),
                            laborPercent = pricing.laborPercent,
                            markupPercent = pricing.markupPercent,
                            taxPercent = pricing.taxPercent
                        )
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
    val pricingParams: PricingParams = PricingParams(),
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

data class PricingParams(
    val drywallSheetCost: Double = 17.5,
    val drywallScrewCost: Double = 0.01,
    val drywallMudCost: Double = 9.5,
    val concreteYardCost: Double = 165.0,
    val gravelYardCost: Double = 52.0,
    val gravelTonCost: Double = 36.0,
    val paintGallonCost: Double = 38.0,
    val laborPercent: Double = 20.0,
    val markupPercent: Double = 15.0,
    val taxPercent: Double = 8.0
) {
    companion object {
        fun fromSettings(settings: Settings): PricingParams {
            return PricingParams(
                drywallSheetCost = settings.drywallSheetUnitCost,
                drywallScrewCost = settings.drywallScrewUnitCost,
                drywallMudCost = settings.drywallMudUnitCost,
                concreteYardCost = settings.concreteYardUnitCost,
                gravelYardCost = settings.gravelYardUnitCost,
                gravelTonCost = settings.gravelTonUnitCost,
                paintGallonCost = settings.paintGallonUnitCost,
                laborPercent = settings.laborPercent,
                markupPercent = settings.markupPercent,
                taxPercent = settings.taxPercent
            )
        }
    }
}

private fun defaultTakeoffTypeForTrade(primaryTrade: PrimaryTrade): TakeoffType? {
    return when (primaryTrade) {
        PrimaryTrade.DRYWALL -> TakeoffType.DRYWALL
        PrimaryTrade.CONCRETE -> TakeoffType.CONCRETE
        PrimaryTrade.PAINT -> TakeoffType.PAINT
        PrimaryTrade.GRAVEL_MULCH -> TakeoffType.GRAVEL_MULCH
        PrimaryTrade.MULTI -> null
    }
}
