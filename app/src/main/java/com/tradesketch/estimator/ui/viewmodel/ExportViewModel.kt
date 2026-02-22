package com.tradesketch.estimator.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.utils.EstimateExportManager
import com.tradesketch.estimator.utils.ExportFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val uxMetricsRepository: UxMetricsRepository,
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
            projectAndSettingsFlow(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                projectId = projectId
            ).collect { (project, settings) ->
                if (project == null) {
                    _uiState.update {
                        it.copy(
                            project = null,
                            isLoading = false,
                            error = "Project not found"
                        )
                    }
                    return@collect
                }
                val selectedType = _uiState.value.selectedType
                    ?: project.takeoffSession.takeIf { it != ProjectTakeoffSession() }
                        ?.selectedScope
                        ?.toTakeoffType()
                    ?: TakeoffType.DRYWALL
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

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val project = state.project ?: return
        val settings = state.settings
        val selectedType = state.selectedType ?: TakeoffType.DRYWALL
        val inputs = buildTakeoffInputs(project = project, settings = state.settings)
        val result = try {
            calculateTakeoffUseCase.calculateForType(
                project = project,
                type = selectedType,
                inputs = inputs
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to calculate export: ${e.message}") }
            return
        }

        val label = selectedType.displayLabel
        _uiState.update {
            it.copy(
                result = result,
                takeoffType = label,
                textContent = ExportFormatter.formatAsText(project, settings, label, result),
                summaryContent = ExportFormatter.formatAsSummary(project, settings, label, result),
                csvContent = ExportFormatter.formatAsCSV(project, settings, label, result),
                jsonContent = ExportFormatter.formatAsJson(project, settings, label, result),
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
        val subjectSuffix = if (shareCsv) "CSV" else state.takeoffType
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${state.project?.name} - $subjectSuffix")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        return Intent.createChooser(intent, if (shareCsv) "Share CSV" else "Share Estimate")
    }

    suspend fun createEstimatePdfShareIntent(): Intent? {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_share_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val state = _uiState.value
        val project = state.project ?: return null
        val result = state.result ?: return null
        return runCatching {
            EstimateExportManager.createEstimatePdfShareIntent(
                context = context,
                projectName = project.name,
                takeoffType = state.takeoffType.ifBlank { state.selectedType?.displayLabel ?: "Estimate" },
                settings = state.settings,
                result = result,
                blueprintDocument = project.authoritativeBlueprint()
            )
        }.getOrElse { error ->
            _uiState.update { it.copy(error = "Could not prepare estimate PDF: ${error.message}") }
            null
        }
    }

    suspend fun saveEstimatePdfToDownloads(): Uri? {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("export_download_pdf")
            uxMetricsRepository.recordTap("export_output_shared")
        }
        val state = _uiState.value
        val project = state.project ?: return null
        val result = state.result ?: return null
        return runCatching {
            EstimateExportManager.saveEstimatePdfToDownloads(
                context = context,
                projectName = project.name,
                takeoffType = state.takeoffType.ifBlank { state.selectedType?.displayLabel ?: "Estimate" },
                settings = state.settings,
                result = result,
                blueprintDocument = project.authoritativeBlueprint()
            )
        }.onSuccess { uri ->
            if (uri != null) {
                _uiState.update { it.copy(lastAction = "Estimate PDF downloaded") }
            }
        }.getOrElse { error ->
            _uiState.update { it.copy(error = "Could not save estimate PDF: ${error.message}") }
            null
        }
    }

    fun csvContent(): String = _uiState.value.csvContent

    suspend fun buildEstimatePdfBytes(): ByteArray? {
        val state = _uiState.value
        val project = state.project ?: return null
        val result = state.result ?: return null
        return runCatching {
            EstimateExportManager.buildEstimatePdfBytes(
                projectName = project.name,
                takeoffType = state.takeoffType.ifBlank { state.selectedType?.displayLabel ?: "Estimate" },
                settings = state.settings,
                result = result,
                blueprintDocument = project.authoritativeBlueprint()
            )
        }.onFailure { error ->
            _uiState.update { it.copy(error = "Could not build PDF bytes: ${error.message}") }
        }.getOrNull()
    }

    fun clearLastAction() {
        _uiState.update { it.copy(lastAction = null, error = null) }
    }

    fun jsonContent(): String = _uiState.value.jsonContent

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
    val jsonContent: String = "",
    val lastAction: String? = null,
    val error: String? = null,
    val isLoading: Boolean = true
)
