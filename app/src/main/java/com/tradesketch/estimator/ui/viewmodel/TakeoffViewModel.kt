package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.ui.defaultTakeoffTypeForTrade
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
    private val uxMetricsRepository: UxMetricsRepository,
    private val calculateTakeoffUseCase: CalculateTakeoffUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null
    private val trackedFirstEstimateProjectIds = mutableSetOf<String>()
    private val trackedGeneratedEstimateKeys = mutableSetOf<String>()

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
            projectAndSettingsFlow(
                projectRepository = repository,
                settingsRepository = settingsRepository,
                projectId = projectId
            ).collect { (project, settings) ->
                if (project != null) {
                    val session = project.takeoffSession
                    val hasPersistedSession = session != ProjectTakeoffSession()
                    val alreadyInitializedForProject =
                        !_uiState.value.isLoading && _uiState.value.project?.id == project.id
                    _uiState.update {
                        it.copy(
                            project = project,
                            settings = settings,
                            selectedType = if (alreadyInitializedForProject) {
                                it.selectedType
                            } else if (hasPersistedSession) {
                                session.selectedScope.toTakeoffType()
                            } else {
                                defaultTakeoffTypeForTrade(settings.primaryTrade) ?: TakeoffType.DRYWALL
                            },
                            selectedPlaybook = if (alreadyInitializedForProject) {
                                it.selectedPlaybook
                            } else if (hasPersistedSession) {
                                runCatching { TakeoffPlaybook.valueOf(session.selectedPlaybook) }
                                    .getOrDefault(TakeoffPlaybook.BALANCED)
                            } else {
                                TakeoffPlaybook.BALANCED
                            },
                            drywallParams = if (alreadyInitializedForProject) {
                                it.drywallParams
                            } else if (hasPersistedSession) {
                                session.drywall.toUiParams()
                            } else {
                                settings.defaultDrywallParams()
                            },
                            concreteParams = if (alreadyInitializedForProject) {
                                it.concreteParams
                            } else if (hasPersistedSession) {
                                session.concrete.toUiParams()
                            } else {
                                settings.defaultConcreteParams()
                            },
                            gravelParams = if (alreadyInitializedForProject) {
                                it.gravelParams
                            } else if (hasPersistedSession) {
                                session.gravel.toUiParams()
                            } else {
                                settings.defaultGravelParams()
                            },
                            paintParams = if (alreadyInitializedForProject) {
                                it.paintParams
                            } else if (hasPersistedSession) {
                                session.paint.toUiParams()
                            } else {
                                settings.defaultPaintParams()
                            },
                            pricingParams = if (alreadyInitializedForProject) {
                                it.pricingParams
                            } else if (hasPersistedSession) {
                                session.pricing.toUiParams()
                            } else {
                                settings.defaultPricingParams()
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
                    _uiState.update {
                        it.copy(
                            project = null,
                            isLoading = false,
                            error = "Project not found"
                        )
                    }
                }
            }
        }
    }
    
    fun selectTakeoffType(type: TakeoffType) {
        _uiState.update { it.copy(selectedType = type) }
        calculate()
    }

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

    fun applyPlaybook(playbook: TakeoffPlaybook) {
        val state = _uiState.value
        val settings = state.settings
        val selectedType = state.selectedType
            ?: defaultTakeoffTypeForTrade(settings.primaryTrade)
            ?: TakeoffType.DRYWALL
        val basePricing = settings.defaultPricingParams()

        val tunedPricing = when (playbook) {
            TakeoffPlaybook.FAST_BID -> basePricing.copy(
                laborPercent = (basePricing.laborPercent - 4.0).coerceAtLeast(8.0),
                markupPercent = (basePricing.markupPercent - 3.0).coerceAtLeast(8.0),
                taxPercent = basePricing.taxPercent
            )
            TakeoffPlaybook.BALANCED -> basePricing
            TakeoffPlaybook.SAFETY_FIRST -> basePricing.copy(
                laborPercent = (basePricing.laborPercent + 5.0).coerceAtMost(40.0),
                markupPercent = (basePricing.markupPercent + 4.0).coerceAtMost(35.0),
                taxPercent = basePricing.taxPercent
            )
        }

        _uiState.update {
            when (selectedType) {
                TakeoffType.DRYWALL -> {
                    val base = settings.defaultDrywallParams()
                    val tuned = when (playbook) {
                        TakeoffPlaybook.FAST_BID -> base.copy(
                            wastePercent = (base.wastePercent - 3.0).coerceAtLeast(4.0),
                            screwsPerSheet = (base.screwsPerSheet - 4).coerceAtLeast(12),
                            mudGallonsPer100SqFt = (base.mudGallonsPer100SqFt - 0.1).coerceAtLeast(0.2)
                        )
                        TakeoffPlaybook.BALANCED -> base
                        TakeoffPlaybook.SAFETY_FIRST -> base.copy(
                            wastePercent = (base.wastePercent + 4.0).coerceAtMost(30.0),
                            screwsPerSheet = (base.screwsPerSheet + 4).coerceAtMost(80),
                            mudGallonsPer100SqFt = (base.mudGallonsPer100SqFt + 0.15).coerceAtMost(1.6)
                        )
                    }
                    it.copy(
                        selectedType = selectedType,
                        selectedPlaybook = playbook,
                        drywallParams = tuned,
                        pricingParams = tunedPricing
                    )
                }
                TakeoffType.CONCRETE -> {
                    val base = settings.defaultConcreteParams()
                    val tuned = when (playbook) {
                        TakeoffPlaybook.FAST_BID -> base.copy(
                            thicknessFeet = 0.30,
                            wastePercent = (base.wastePercent - 2.0).coerceAtLeast(3.0)
                        )
                        TakeoffPlaybook.BALANCED -> base
                        TakeoffPlaybook.SAFETY_FIRST -> base.copy(
                            thicknessFeet = 0.36,
                            wastePercent = (base.wastePercent + 3.0).coerceAtMost(25.0)
                        )
                    }
                    it.copy(
                        selectedType = selectedType,
                        selectedPlaybook = playbook,
                        concreteParams = tuned,
                        pricingParams = tunedPricing
                    )
                }
                TakeoffType.GRAVEL_MULCH -> {
                    val base = settings.defaultGravelParams()
                    val tuned = when (playbook) {
                        TakeoffPlaybook.FAST_BID -> base.copy(
                            depthFeet = 0.22,
                            wastePercent = (base.wastePercent - 2.0).coerceAtLeast(3.0)
                        )
                        TakeoffPlaybook.BALANCED -> base
                        TakeoffPlaybook.SAFETY_FIRST -> base.copy(
                            depthFeet = 0.30,
                            wastePercent = (base.wastePercent + 3.0).coerceAtMost(30.0),
                            densityTonsPerYard = (base.densityTonsPerYard + 0.1).coerceAtMost(2.2)
                        )
                    }
                    it.copy(
                        selectedType = selectedType,
                        selectedPlaybook = playbook,
                        gravelParams = tuned,
                        pricingParams = tunedPricing
                    )
                }
                TakeoffType.PAINT -> {
                    val base = settings.defaultPaintParams()
                    val tuned = when (playbook) {
                        TakeoffPlaybook.FAST_BID -> base.copy(
                            coats = (base.coats - 1).coerceAtLeast(1),
                            wastePercent = (base.wastePercent - 2.0).coerceAtLeast(2.0)
                        )
                        TakeoffPlaybook.BALANCED -> base
                        TakeoffPlaybook.SAFETY_FIRST -> base.copy(
                            coats = (base.coats + 1).coerceAtMost(4),
                            wastePercent = (base.wastePercent + 2.0).coerceAtMost(20.0)
                        )
                    }
                    it.copy(
                        selectedType = selectedType,
                        selectedPlaybook = playbook,
                        paintParams = tuned,
                        pricingParams = tunedPricing
                    )
                }
            }
        }
        calculate()
    }
    
    fun updateDrywallParams(
        sheetAreaSqFt: Double? = null,
        wastePercent: Double? = null,
        screwsPerSheet: Int? = null,
        mudGallonsPer100SqFt: Double? = null,
        includeCeilings: Boolean? = null
    ) {
        val current = _uiState.value.drywallParams
        _uiState.update {
            it.copy(
                drywallParams = current.copy(
                    sheetAreaSqFt = sheetAreaSqFt ?: current.sheetAreaSqFt,
                    wastePercent = wastePercent ?: current.wastePercent,
                    screwsPerSheet = screwsPerSheet ?: current.screwsPerSheet,
                    mudGallonsPer100SqFt = mudGallonsPer100SqFt ?: current.mudGallonsPer100SqFt,
                    includeCeilings = includeCeilings ?: current.includeCeilings
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
        val sessionSnapshot = state.toTakeoffSession(type)
        
        try {
            val result = calculateTakeoffUseCase.calculateForType(
                project = project,
                type = type,
                inputs = TakeoffCalculationInputs(
                    drywall = state.drywallParams,
                    concrete = state.concreteParams,
                    gravel = state.gravelParams,
                    paint = state.paintParams,
                    pricing = pricing
                )
            )
            _uiState.update { it.copy(result = result, error = null) }
            val generatedEstimateKey = "${project.id}:${type.name}"
            if (trackedGeneratedEstimateKeys.add(generatedEstimateKey)) {
                viewModelScope.launch {
                    uxMetricsRepository.recordTap("takeoff_estimate_generated")
                }
            }
            if (trackedFirstEstimateProjectIds.add(project.id)) {
                val timeToFirstEstimateMs = (System.currentTimeMillis() - project.createdAt).coerceAtLeast(0L)
                viewModelScope.launch {
                    uxMetricsRepository.recordTimeToFirstEstimate(timeToFirstEstimateMs)
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Calculation failed: ${e.message}") }
        }
        persistTakeoffSessionIfNeeded(project, sessionSnapshot)
    }

    private fun persistTakeoffSessionIfNeeded(
        project: Project,
        session: ProjectTakeoffSession
    ) {
        if (project.takeoffSession == session) return
        viewModelScope.launch {
            runCatching {
                repository.saveProject(
                    project.copy(
                        takeoffSession = session,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { error ->
                _uiState.update { it.copy(error = "Failed to save takeoff session: ${error.message}") }
            }
        }
    }
}

data class TakeoffUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val selectedType: TakeoffType? = null,
    val selectedPlaybook: TakeoffPlaybook = TakeoffPlaybook.BALANCED,
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

enum class TakeoffPlaybook {
    FAST_BID,
    BALANCED,
    SAFETY_FIRST
}

private val DEFAULT_DRYWALL_SESSION = DrywallSessionParams()
private val DEFAULT_CONCRETE_SESSION = ConcreteSessionParams()
private val DEFAULT_GRAVEL_SESSION = GravelSessionParams()
private val DEFAULT_PAINT_SESSION = PaintSessionParams()
private val DEFAULT_PRICING_SESSION = PricingSessionParams()

data class DrywallParams(
    val sheetAreaSqFt: Double = DEFAULT_DRYWALL_SESSION.sheetAreaSqFt,
    val wastePercent: Double = DEFAULT_DRYWALL_SESSION.wastePercent,
    val screwsPerSheet: Int = DEFAULT_DRYWALL_SESSION.screwsPerSheet,
    val mudGallonsPer100SqFt: Double = DEFAULT_DRYWALL_SESSION.mudGallonsPer100SqFt,
    val includeCeilings: Boolean = DEFAULT_DRYWALL_SESSION.includeCeilings
)

data class ConcreteParams(
    val thicknessFeet: Double = DEFAULT_CONCRETE_SESSION.thicknessFeet,
    val wastePercent: Double = DEFAULT_CONCRETE_SESSION.wastePercent
)

data class GravelParams(
    val depthFeet: Double = DEFAULT_GRAVEL_SESSION.depthFeet,
    val densityTonsPerYard: Double = DEFAULT_GRAVEL_SESSION.densityTonsPerYard,
    val wastePercent: Double = DEFAULT_GRAVEL_SESSION.wastePercent
)

data class PaintParams(
    val coverageSqFtPerGallon: Double = DEFAULT_PAINT_SESSION.coverageSqFtPerGallon,
    val coats: Int = DEFAULT_PAINT_SESSION.coats,
    val wastePercent: Double = DEFAULT_PAINT_SESSION.wastePercent
)

data class PricingParams(
    val drywallSheetCost: Double = DEFAULT_PRICING_SESSION.drywallSheetCost,
    val drywallScrewCost: Double = DEFAULT_PRICING_SESSION.drywallScrewCost,
    val drywallMudCost: Double = DEFAULT_PRICING_SESSION.drywallMudCost,
    val concreteYardCost: Double = DEFAULT_PRICING_SESSION.concreteYardCost,
    val gravelYardCost: Double = DEFAULT_PRICING_SESSION.gravelYardCost,
    val gravelTonCost: Double = DEFAULT_PRICING_SESSION.gravelTonCost,
    val paintGallonCost: Double = DEFAULT_PRICING_SESSION.paintGallonCost,
    val laborPercent: Double = DEFAULT_PRICING_SESSION.laborPercent,
    val markupPercent: Double = DEFAULT_PRICING_SESSION.markupPercent,
    val taxPercent: Double = DEFAULT_PRICING_SESSION.taxPercent
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

private fun TakeoffUiState.toTakeoffSession(selectedType: TakeoffType): ProjectTakeoffSession {
    return ProjectTakeoffSession(
        selectedScope = selectedType.toTakeoffScope(),
        selectedPlaybook = selectedPlaybook.name,
        drywall = DrywallSessionParams(
            sheetAreaSqFt = drywallParams.sheetAreaSqFt,
            wastePercent = drywallParams.wastePercent,
            screwsPerSheet = drywallParams.screwsPerSheet,
            mudGallonsPer100SqFt = drywallParams.mudGallonsPer100SqFt,
            includeCeilings = drywallParams.includeCeilings
        ),
        concrete = ConcreteSessionParams(
            thicknessFeet = concreteParams.thicknessFeet,
            wastePercent = concreteParams.wastePercent
        ),
        gravel = GravelSessionParams(
            depthFeet = gravelParams.depthFeet,
            densityTonsPerYard = gravelParams.densityTonsPerYard,
            wastePercent = gravelParams.wastePercent
        ),
        paint = PaintSessionParams(
            coverageSqFtPerGallon = paintParams.coverageSqFtPerGallon,
            coats = paintParams.coats,
            wastePercent = paintParams.wastePercent
        ),
        pricing = PricingSessionParams(
            drywallSheetCost = pricingParams.drywallSheetCost,
            drywallScrewCost = pricingParams.drywallScrewCost,
            drywallMudCost = pricingParams.drywallMudCost,
            concreteYardCost = pricingParams.concreteYardCost,
            gravelYardCost = pricingParams.gravelYardCost,
            gravelTonCost = pricingParams.gravelTonCost,
            paintGallonCost = pricingParams.paintGallonCost,
            laborPercent = pricingParams.laborPercent,
            markupPercent = pricingParams.markupPercent,
            taxPercent = pricingParams.taxPercent
        )
    )
}
