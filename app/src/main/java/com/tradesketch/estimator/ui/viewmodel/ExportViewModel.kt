package com.tradesketch.estimator.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.utils.ExportFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * ViewModel for Export screen.
 * Prepares shareable output from the current project + selected takeoff type.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateTakeoffUseCase: CalculateTakeoffUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

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
                projectRepository.getProjects().map { projects ->
                    projects.find { it.id == projectId }
                },
                settingsRepository.getSettings()
            ) { project, settings ->
                Pair(project, settings)
            }.collect { (project, settings) ->
                if (project == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Project not found"
                        )
                    }
                    return@collect
                }
                val selectedType = _uiState.value.selectedType ?: TakeoffType.DRYWALL
                _uiState.update {
                    it.copy(
                        project = project,
                        settings = settings,
                        selectedType = selectedType,
                        isLoading = false,
                        error = null
                    )
                }
                recalculate()
            }
        }
    }

    fun selectTakeoffType(type: TakeoffType) {
        _uiState.update { it.copy(selectedType = type) }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val project = state.project ?: return
        val settings = state.settings
        val selectedType = state.selectedType ?: TakeoffType.DRYWALL
        val result = try {
            when (selectedType) {
                TakeoffType.DRYWALL -> {
                    val walls = project.spaces.filter { it.geometry is Geometry.Wall }
                    calculateTakeoffUseCase.calculateDrywall(
                        walls = walls,
                        sheetAreaSqFt = settings.defaultDrywallSheetArea,
                        wastePercent = settings.defaultWastePercent,
                        screwsPerSheet = settings.defaultScrewsPerSheet,
                        mudGallonsPer100SqFt = settings.defaultMudGallonsPer100SqFt,
                        costing = CostingInputs(
                            unitCostByLineName = mapOf(
                                "Drywall sheets" to settings.drywallSheetUnitCost,
                                "Drywall screws" to settings.drywallScrewUnitCost,
                                "Joint compound" to settings.drywallMudUnitCost
                            ),
                            laborPercent = settings.laborPercent,
                            markupPercent = settings.markupPercent,
                            taxPercent = settings.taxPercent
                        )
                    )
                }
                TakeoffType.CONCRETE -> {
                    val slabs = project.spaces.filter { it.geometry is Geometry.Slab }
                    calculateTakeoffUseCase.calculateConcrete(
                        slabSpaces = slabs,
                        thicknessFeet = 0.33,
                        wastePercent = settings.defaultWastePercent,
                        costing = CostingInputs(
                            unitCostByLineName = mapOf(
                                "Concrete volume" to settings.concreteYardUnitCost
                            ),
                            laborPercent = settings.laborPercent,
                            markupPercent = settings.markupPercent,
                            taxPercent = settings.taxPercent
                        )
                    )
                }
                TakeoffType.GRAVEL_MULCH -> {
                    calculateTakeoffUseCase.calculateGravelMulch(
                        spaces = project.spaces,
                        depthFeet = 0.25,
                        densityTonsPerYard = 1.4,
                        wastePercent = settings.defaultWastePercent,
                        costing = CostingInputs(
                            unitCostByLineName = mapOf(
                                "Material volume" to settings.gravelYardUnitCost,
                                "Material weight" to settings.gravelTonUnitCost
                            ),
                            laborPercent = settings.laborPercent,
                            markupPercent = settings.markupPercent,
                            taxPercent = settings.taxPercent
                        )
                    )
                }
                TakeoffType.PAINT -> {
                    val paintable = project.spaces.filter {
                        it.geometry is Geometry.Wall || it.geometry is Geometry.Rect
                    }
                    calculateTakeoffUseCase.calculatePaint(
                        spaces = paintable,
                        coverageSqFtPerGallon = settings.defaultCoveragePerGallon,
                        coats = settings.defaultCoatsOfPaint,
                        wastePercent = settings.defaultWastePercent,
                        costing = CostingInputs(
                            unitCostByLineName = mapOf(
                                "Paint" to settings.paintGallonUnitCost
                            ),
                            laborPercent = settings.laborPercent,
                            markupPercent = settings.markupPercent,
                            taxPercent = settings.taxPercent
                        )
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to calculate export: ${e.message}") }
            return
        }

        val label = selectedType.displayName()
        _uiState.update {
            it.copy(
                result = result,
                takeoffType = label,
                textContent = ExportFormatter.formatAsText(project, label, result),
                summaryContent = ExportFormatter.formatAsSummary(project, label, result),
                csvContent = ExportFormatter.formatAsCSV(project, label, result),
                error = null
            )
        }
    }

    fun copySummaryToClipboard(): Boolean {
        return copyToClipboard(
            label = "TradeSketch Estimate (Summary)",
            content = _uiState.value.summaryContent,
            successMessage = "Summary copied"
        )
    }

    fun copyReportToClipboard(): Boolean {
        return copyToClipboard(
            label = "TradeSketch Estimate (Full Report)",
            content = _uiState.value.textContent,
            successMessage = "Full report copied"
        )
    }

    fun copyCSVToClipboard(): Boolean {
        return copyToClipboard(
            label = "TradeSketch Estimate (CSV)",
            content = _uiState.value.csvContent,
            successMessage = "CSV copied"
        )
    }

    fun createShareIntent(shareCsv: Boolean = false): Intent {
        val state = _uiState.value
        val content = if (shareCsv) state.csvContent else state.textContent
        val subjectSuffix = if (shareCsv) "CSV" else state.takeoffType
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${state.project?.name} - $subjectSuffix")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        return Intent.createChooser(intent, if (shareCsv) "Share CSV" else "Share Estimate")
    }

    fun clearLastAction() {
        _uiState.update { it.copy(lastAction = null, error = null) }
    }

    private fun copyToClipboard(
        label: String,
        content: String,
        successMessage: String
    ): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, content)
            clipboard.setPrimaryClip(clip)
            _uiState.update { it.copy(lastAction = successMessage) }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Clipboard failed: ${e.message}") }
            false
        }
    }
}

data class ExportUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val selectedType: TakeoffType? = null,
    val takeoffType: String = "",
    val result: TakeoffResult? = null,
    val textContent: String = "",
    val summaryContent: String = "",
    val csvContent: String = "",
    val lastAction: String? = null,
    val error: String? = null,
    val isLoading: Boolean = true
)

private fun TakeoffType.displayName(): String {
    return when (this) {
        TakeoffType.DRYWALL -> "Drywall"
        TakeoffType.CONCRETE -> "Concrete"
        TakeoffType.GRAVEL_MULCH -> "Gravel / Mulch"
        TakeoffType.PAINT -> "Paint"
    }
}
