package com.tradesketch.estimator.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.ui.defaultTakeoffTypeForTrade
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.utils.BlueprintExportManager
import com.tradesketch.estimator.utils.EstimateExportManager
import com.tradesketch.estimator.utils.EstimateIdentity
import com.tradesketch.estimator.utils.ExportResult
import com.tradesketch.estimator.utils.ExportStorage
import com.tradesketch.estimator.utils.SavedExport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val uxMetricsRepository: UxMetricsRepository,
    private val calculateTakeoffUseCase: CalculateTakeoffUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null
    private var recalculateJob: Job? = null
    private var recalculateGeneration = 0L

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        currentProjectId?.let(::observeProject)
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
            projectAndSettingsFlow(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                projectId = projectId
            ).collect { (project, settings) ->
                if (project == null) {
                    clearComputedOutput(error = "Project not found")
                    _uiState.update { it.copy(project = null, settings = settings, isLoading = false) }
                    return@collect
                }
                val fallbackType = defaultTakeoffTypeForTrade(settings.primaryTrade) ?: TakeoffType.DRYWALL
                val selectedType = _uiState.value.selectedType
                    ?: project.takeoffSession.takeIf { it != ProjectTakeoffSession() }
                        ?.selectedScope
                        ?.toTakeoffType()
                    ?: fallbackType
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
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_select_scope")
        }
        _uiState.update { it.copy(selectedType = type) }
        recalculate()
    }

    fun setBlueprintGridExport(enabled: Boolean) {
        _uiState.update { it.copy(blueprintExportShowGrid = enabled) }
    }

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

    private fun recalculate() {
        val stateSnapshot = _uiState.value
        val project = stateSnapshot.project ?: return
        val settings = stateSnapshot.settings
        val selectedType = stateSnapshot.selectedType ?: TakeoffType.DRYWALL
        val generation = ++recalculateGeneration
        recalculateJob?.cancel()
        recalculateJob = viewModelScope.launch {
            val exportSnapshot = runCatching {
                withContext(Dispatchers.Default) {
                    val inputs = buildTakeoffInputs(project = project, settings = settings)
                    val previewBlueprint = project.authoritativeBlueprint()
                    val selectedTradeBlueprint = projectBlueprintForType(
                        project = project,
                        type = selectedType
                    )
                    val result = calculateTakeoffUseCase.calculateForType(
                        project = project,
                        type = selectedType,
                        inputs = inputs
                    )
                    val presentTradeSections = buildPresentTradeExportSections(
                        project = project,
                        inputs = inputs
                    )
                    val label = selectedType.displayLabel
                    val generatedAtMillis = System.currentTimeMillis()
                    val estimateId = EstimateIdentity.buildEstimateId(
                        project = project,
                        generatedAtMillis = generatedAtMillis
                    )
                    ExportComputation(
                        result = result,
                        previewBlueprint = previewBlueprint,
                        selectedTradeHasGeometry = selectedTradeBlueprint.hasGeometry(),
                        presentTradeLabels = presentTradeSections.map(CombinedExportSection::takeoffTypeLabel),
                        takeoffType = label,
                        estimateId = estimateId,
                        generatedAtMillis = generatedAtMillis,
                        textContent = CombinedExportFormatter.formatAsText(
                            project = project,
                            settings = settings,
                            sections = presentTradeSections,
                            generatedAtMillis = generatedAtMillis,
                            estimateId = estimateId
                        ),
                        summaryContent = CombinedExportFormatter.formatAsSummary(
                            project = project,
                            settings = settings,
                            sections = presentTradeSections,
                            generatedAtMillis = generatedAtMillis,
                            estimateId = estimateId
                        ),
                        csvContent = CombinedExportFormatter.formatAsCSV(
                            project = project,
                            settings = settings,
                            sections = presentTradeSections,
                            generatedAtMillis = generatedAtMillis,
                            estimateId = estimateId
                        ),
                        jsonContent = CombinedExportFormatter.formatAsJson(
                            project = project,
                            settings = settings,
                            sections = presentTradeSections,
                            generatedAtMillis = generatedAtMillis,
                            estimateId = estimateId
                        )
                    )
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                clearComputedOutput(error = "Failed to calculate export: ${error.message}")
                return@launch
            }

            if (generation != recalculateGeneration) return@launch
            _uiState.update {
                it.copy(
                    result = exportSnapshot.result,
                    previewBlueprint = exportSnapshot.previewBlueprint,
                    selectedTradeHasGeometry = exportSnapshot.selectedTradeHasGeometry,
                    presentTradeLabels = exportSnapshot.presentTradeLabels,
                    takeoffType = exportSnapshot.takeoffType,
                    estimateId = exportSnapshot.estimateId,
                    generatedAtMillis = exportSnapshot.generatedAtMillis,
                    textContent = exportSnapshot.textContent,
                    summaryContent = exportSnapshot.summaryContent,
                    csvContent = exportSnapshot.csvContent,
                    jsonContent = exportSnapshot.jsonContent,
                    error = null
                )
            }
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

    fun copyCustomTextToClipboard(
        label: String,
        content: String,
        successMessage: String
    ): Boolean {
        return copyToClipboard(
            label = label,
            content = content,
            successMessage = successMessage
        )
    }

    fun createShareIntent(shareCsv: Boolean = false): Intent {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(if (shareCsv) "export_share_csv" else "export_share_report")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val state = _uiState.value
        val content = if (shareCsv) state.csvContent else state.textContent
        val subjectSuffix = if (shareCsv) {
            "CSV"
        } else if (state.presentTradeLabels.size > 1) {
            "All Present Trades"
        } else {
            state.takeoffType
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${state.project?.name} - $subjectSuffix")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        return Intent.createChooser(intent, if (shareCsv) "Share CSV" else "Share Estimate")
    }

    suspend fun createEstimatePdfShareIntent(): ExportActionResult {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_share_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val exportPayload = buildEstimateExportPayload()
            ?: return failAction("Open Materials & Pricing and generate quantities before exporting.")
        return runCatching {
            EstimateExportManager.createEstimatePdfShareIntent(
                context = context,
                projectId = exportPayload.project.id,
                projectName = exportPayload.project.name,
                takeoffType = exportPayload.takeoffTypeLabel,
                settings = exportPayload.settings,
                result = exportPayload.result,
                generatedAtMillis = exportPayload.generatedAtMillis,
                estimateId = exportPayload.estimateId,
                blueprintDocument = exportPayload.blueprint
            )
        }.fold(
            onSuccess = ::applyShareResult,
            onFailure = { error ->
                if (error is CancellationException) throw error
                failAction("Could not prepare estimate PDF: ${error.message}")
            }
        )
    }

    suspend fun saveEstimatePdfToDownloads(): ExportActionResult {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_download_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val exportPayload = buildEstimateExportPayload()
            ?: return failAction("Open Materials & Pricing and generate quantities before exporting.")
        return runCatching {
            EstimateExportManager.saveEstimatePdfToDownloads(
                context = context,
                projectId = exportPayload.project.id,
                projectName = exportPayload.project.name,
                takeoffType = exportPayload.takeoffTypeLabel,
                settings = exportPayload.settings,
                result = exportPayload.result,
                generatedAtMillis = exportPayload.generatedAtMillis,
                estimateId = exportPayload.estimateId,
                blueprintDocument = exportPayload.blueprint
            )
        }.fold(
            onSuccess = ::applyWriteResult,
            onFailure = { error ->
                if (error is CancellationException) throw error
                failAction("Could not save estimate PDF: ${error.message}")
            }
        )
    }

    suspend fun saveEstimatePdfToUri(uri: Uri): ExportActionResult {
        val exportPayload = buildEstimateExportPayload()
            ?: return failAction("Open Materials & Pricing and generate quantities before exporting.")
        val fileName = ExportStorage.buildFileName(
            projectName = exportPayload.project.name,
            suffix = "estimate",
            extension = "pdf"
        )
        val bytes = buildEstimatePdfBytes()
            ?: return failAction("Could not prepare the estimate PDF.")
        return applyWriteResult(
            ExportStorage.writeBytesToDocument(
                context = context,
                uri = uri,
                bytes = bytes,
                fileName = fileName,
                exportLabel = "estimate PDF"
            )
        )
    }

    fun csvContent(): String = _uiState.value.csvContent

    suspend fun buildEstimatePdfBytes(): ByteArray? {
        val exportPayload = buildEstimateExportPayload() ?: return null
        return runCatching {
            withContext(Dispatchers.Default) {
                EstimateExportManager.buildEstimatePdfBytes(
                    projectId = exportPayload.project.id,
                    projectName = exportPayload.project.name,
                    takeoffType = exportPayload.takeoffTypeLabel,
                    settings = exportPayload.settings,
                    result = exportPayload.result,
                    generatedAtMillis = exportPayload.generatedAtMillis,
                    estimateId = exportPayload.estimateId,
                    blueprintDocument = exportPayload.blueprint
                )
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _uiState.update { it.copy(error = "Could not build PDF bytes: ${error.message}") }
        }.getOrNull()
    }

    suspend fun buildBlueprintPngBytes(): ByteArray? {
        val blueprintPayload = buildBlueprintExportPayload() ?: return null
        return runCatching {
            withContext(Dispatchers.Default) {
                val bitmap = BlueprintExportManager.renderBlueprintBitmap(
                    projectName = blueprintPayload.project.name,
                    document = blueprintPayload.blueprint,
                    includeGrid = blueprintPayload.includeGrid
                )
                try {
                    ByteArrayOutputStream().use { output ->
                        val wroteImage = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                        require(wroteImage) { "Failed to compress blueprint PNG." }
                        output.toByteArray()
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _uiState.update { it.copy(error = "Could not build blueprint PNG: ${error.message}") }
        }.getOrNull()
    }

    suspend fun saveBlueprintPngToUri(uri: Uri): ExportActionResult {
        val blueprintPayload = buildBlueprintExportPayload()
            ?: return failAction("Add at least one wall, room, or opening before exporting a blueprint.")
        val fileName = ExportStorage.buildFileName(
            projectName = blueprintPayload.project.name,
            suffix = if (blueprintPayload.includeGrid) "blueprint-grid" else "blueprint-no-grid",
            extension = "png"
        )
        val bytes = buildBlueprintPngBytes()
            ?: return failAction("Could not prepare the blueprint PNG.")
        return applyWriteResult(
            ExportStorage.writeBytesToDocument(
                context = context,
                uri = uri,
                bytes = bytes,
                fileName = fileName,
                exportLabel = "blueprint PNG"
            )
        )
    }

    suspend fun createBlueprintPdfShareIntent(): ExportActionResult {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_share_blueprint_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val blueprintPayload = buildBlueprintExportPayload()
            ?: return failAction("Add at least one wall, room, or opening before exporting a blueprint.")
        return runCatching {
            BlueprintExportManager.createBlueprintPdfShareIntent(
                context = context,
                projectName = blueprintPayload.project.name,
                document = blueprintPayload.blueprint,
                includeGrid = blueprintPayload.includeGrid
            )
        }.fold(
            onSuccess = ::applyShareResult,
            onFailure = { error ->
                if (error is CancellationException) throw error
                failAction("Could not prepare blueprint PDF: ${error.message}")
            }
        )
    }

    suspend fun saveBlueprintPdfToDownloads(): ExportActionResult {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_download_blueprint_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val blueprintPayload = buildBlueprintExportPayload()
            ?: return failAction("Add at least one wall, room, or opening before exporting a blueprint.")
        return runCatching {
            BlueprintExportManager.saveBlueprintPdfToDownloads(
                context = context,
                projectName = blueprintPayload.project.name,
                document = blueprintPayload.blueprint,
                includeGrid = blueprintPayload.includeGrid
            )
        }.fold(
            onSuccess = ::applyWriteResult,
            onFailure = { error ->
                if (error is CancellationException) throw error
                failAction("Could not save blueprint PDF: ${error.message}")
            }
        )
    }

    suspend fun saveBlueprintPdfToUri(uri: Uri): ExportActionResult {
        val blueprintPayload = buildBlueprintExportPayload()
            ?: return failAction("Add at least one wall, room, or opening before exporting a blueprint.")
        val fileName = ExportStorage.buildFileName(
            projectName = blueprintPayload.project.name,
            suffix = if (blueprintPayload.includeGrid) "blueprint-grid" else "blueprint-no-grid",
            extension = "pdf"
        )
        val bytes = buildBlueprintPdfBytes()
        if (bytes == null || bytes.isEmpty()) {
            return failAction("Could not prepare the blueprint PDF.")
        }
        return applyWriteResult(
            ExportStorage.writeBytesToDocument(
                context = context,
                uri = uri,
                bytes = bytes,
                fileName = fileName,
                exportLabel = "blueprint PDF"
            )
        )
    }

    suspend fun buildBlueprintPdfBytes(): ByteArray? {
        val blueprintPayload = buildBlueprintExportPayload() ?: return null
        return runCatching {
            withContext(Dispatchers.Default) {
                BlueprintExportManager.buildBlueprintPdfBytes(
                    projectName = blueprintPayload.project.name,
                    document = blueprintPayload.blueprint,
                    includeGrid = blueprintPayload.includeGrid
                )
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _uiState.update { it.copy(error = "Could not build blueprint PDF: ${error.message}") }
        }.getOrNull()
    }

    suspend fun saveCsvToUri(uri: Uri): ExportActionResult {
        val project = _uiState.value.project
            ?: return failAction("Open a project before exporting CSV.")
        val fileName = ExportStorage.buildFileName(
            projectName = project.name,
            suffix = "quantities",
            extension = "csv"
        )
        val bytes = buildCsvBytes()
        return applyWriteResult(
            ExportStorage.writeBytesToDocument(
                context = context,
                uri = uri,
                bytes = bytes,
                fileName = fileName,
                exportLabel = "CSV"
            )
        )
    }

    suspend fun saveJsonToUri(uri: Uri): ExportActionResult {
        val project = _uiState.value.project
            ?: return failAction("Open a project before exporting JSON.")
        val fileName = ExportStorage.buildFileName(
            projectName = project.name,
            suffix = "backup",
            extension = "json"
        )
        val bytes = buildJsonBytes()
        return applyWriteResult(
            ExportStorage.writeBytesToDocument(
                context = context,
                uri = uri,
                bytes = bytes,
                fileName = fileName,
                exportLabel = "JSON backup"
            )
        )
    }

    suspend fun buildCsvBytes(): ByteArray = withContext(Dispatchers.Default) {
        _uiState.value.csvContent.toByteArray(Charsets.UTF_8)
    }

    suspend fun buildJsonBytes(): ByteArray = withContext(Dispatchers.Default) {
        _uiState.value.jsonContent.toByteArray(Charsets.UTF_8)
    }

    fun clearLastAction() {
        _uiState.update { it.copy(lastAction = null, error = null) }
    }

    fun reportExternalFailure(message: String) {
        _uiState.update { it.copy(error = message, lastAction = null) }
    }

    fun jsonContent(): String = _uiState.value.jsonContent

    private fun buildEstimateExportPayload(): EstimateExportPayload? {
        val state = _uiState.value
        val project = state.project ?: return null
        val result = state.result ?: return null
        val selectedType = state.selectedType ?: TakeoffType.DRYWALL
        val generatedAtMillis = state.generatedAtMillis ?: System.currentTimeMillis()
        val blueprint = state.previewBlueprint
            ?: projectBlueprintForType(project = project, type = selectedType)
        return EstimateExportPayload(
            project = project,
            settings = state.settings,
            result = result,
            blueprint = blueprint,
            takeoffTypeLabel = state.takeoffType.ifBlank { state.selectedType?.displayLabel ?: "Estimate" },
            generatedAtMillis = generatedAtMillis,
            estimateId = state.estimateId.ifBlank { null }
        )
    }

    private fun buildBlueprintExportPayload(): BlueprintExportPayload? {
        val state = _uiState.value
        val project = state.project ?: return null
        val blueprint = state.previewBlueprint
            ?: project.authoritativeBlueprint()
        val hasGeometry = blueprint.walls.isNotEmpty() || blueprint.rooms.isNotEmpty() || blueprint.openings.isNotEmpty()
        if (!hasGeometry) return null
        return BlueprintExportPayload(
            project = project,
            blueprint = blueprint,
            includeGrid = state.blueprintExportShowGrid
        )
    }

    private fun applyShareResult(result: ExportResult<Intent>): ExportActionResult {
        return when (result) {
            is ExportResult.Success -> {
                _uiState.update {
                    it.copy(
                        lastAction = result.userMessage,
                        error = null
                    )
                }
                ExportActionResult.Success(
                    message = result.userMessage,
                    intent = result.value
                )
            }
            is ExportResult.Failure -> failAction(result.userMessage)
        }
    }

    private fun applyWriteResult(result: ExportResult<SavedExport>): ExportActionResult {
        return when (result) {
            is ExportResult.Success -> {
                val message = result.userMessage ?: "Saved ${result.value.fileName}."
                _uiState.update { it.copy(lastAction = message, error = null) }
                ExportActionResult.Success(message = message, uri = result.value.uri)
            }
            is ExportResult.Failure -> failAction(result.userMessage)
        }
    }

    private fun failAction(message: String): ExportActionResult {
        _uiState.update { it.copy(error = message, lastAction = null) }
        return ExportActionResult.Failure(message)
    }

    private fun clearComputedOutput(error: String?) {
        _uiState.update {
            it.copy(
                result = null,
                previewBlueprint = null,
                selectedTradeHasGeometry = false,
                presentTradeLabels = emptyList(),
                takeoffType = "",
                estimateId = "",
                generatedAtMillis = null,
                textContent = "",
                summaryContent = "",
                csvContent = "",
                jsonContent = "",
                lastAction = null,
                error = error
            )
        }
    }

    private fun copyToClipboard(
        label: String,
        content: String,
        successMessage: String
    ): Boolean {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_copy")
        }
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, content)
            clipboard.setPrimaryClip(clip)
            _uiState.update { it.copy(lastAction = successMessage, error = null) }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Clipboard failed: ${e.message}") }
            false
        }
    }

    private fun buildPresentTradeExportSections(
        project: Project,
        inputs: TakeoffCalculationInputs
    ): List<CombinedExportSection> {
        return TakeoffType.entries.mapNotNull { type ->
            val blueprint = projectBlueprintForType(project = project, type = type)
            if (!blueprint.hasGeometry()) {
                return@mapNotNull null
            }
            CombinedExportSection(
                takeoffTypeLabel = type.displayLabel,
                result = calculateTakeoffUseCase.calculateForType(
                    project = project,
                    type = type,
                    inputs = inputs
                )
            )
        }
    }
}

private data class ExportComputation(
    val result: TakeoffResult,
    val previewBlueprint: BlueprintDocument,
    val selectedTradeHasGeometry: Boolean,
    val presentTradeLabels: List<String>,
    val takeoffType: String,
    val estimateId: String,
    val generatedAtMillis: Long,
    val textContent: String,
    val summaryContent: String,
    val csvContent: String,
    val jsonContent: String
)

private data class EstimateExportPayload(
    val project: Project,
    val settings: Settings,
    val result: TakeoffResult,
    val blueprint: BlueprintDocument,
    val takeoffTypeLabel: String,
    val generatedAtMillis: Long,
    val estimateId: String?
)

private data class BlueprintExportPayload(
    val project: Project,
    val blueprint: BlueprintDocument,
    val includeGrid: Boolean
)

sealed interface ExportActionResult {
    data class Success(
        val message: String? = null,
        val intent: Intent? = null,
        val uri: Uri? = null
    ) : ExportActionResult

    data class Failure(val message: String) : ExportActionResult
}

data class ExportUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val selectedType: TakeoffType? = null,
    val takeoffType: String = "",
    val presentTradeLabels: List<String> = emptyList(),
    val estimateId: String = "",
    val generatedAtMillis: Long? = null,
    val result: TakeoffResult? = null,
    val previewBlueprint: BlueprintDocument? = null,
    val selectedTradeHasGeometry: Boolean = false,
    val textContent: String = "",
    val summaryContent: String = "",
    val csvContent: String = "",
    val jsonContent: String = "",
    val blueprintExportShowGrid: Boolean = true,
    val lastAction: String? = null,
    val error: String? = null,
    val isLoading: Boolean = true
)

private fun BlueprintDocument.hasGeometry(): Boolean {
    return walls.isNotEmpty() || rooms.isNotEmpty() || openings.isNotEmpty()
}
