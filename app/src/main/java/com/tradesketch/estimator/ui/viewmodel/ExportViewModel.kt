package com.tradesketch.estimator.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.utils.ExportFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for Export screen.
 * Handles copy, share, CSV, and PDF export operations.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    fun prepareExport(project: Project, takeoffType: String, result: TakeoffResult) {
        val textContent = ExportFormatter.formatAsText(project, takeoffType, result)
        val summary = ExportFormatter.formatAsSummary(project, takeoffType, result)
        val csvContent = ExportFormatter.formatAsCSV(project, takeoffType, result)
        
        _uiState.update {
            it.copy(
                project = project,
                takeoffType = takeoffType,
                result = result,
                textContent = textContent,
                summaryContent = summary,
                csvContent = csvContent
            )
        }
    }
    
    fun copyToClipboard(): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TradeSketch Estimate", _uiState.value.summaryContent)
            clipboard.setPrimaryClip(clip)
            _uiState.update { it.copy(lastAction = "Copied to clipboard") }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to copy: ${e.message}") }
            false
        }
    }
    
    fun createShareIntent(): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${_uiState.value.project?.name} - ${_uiState.value.takeoffType}")
            putExtra(Intent.EXTRA_TEXT, _uiState.value.textContent)
        }
        return Intent.createChooser(intent, "Share Estimate")
    }
    
    fun getCSVContent(): String {
        return _uiState.value.csvContent
    }
    
    fun getCSVFileName(): String {
        val projectName = _uiState.value.project?.name?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "estimate"
        val takeoffType = _uiState.value.takeoffType.replace(" ", "_")
        return "${projectName}_${takeoffType}_${System.currentTimeMillis()}.csv"
    }
    
    fun clearLastAction() {
        _uiState.update { it.copy(lastAction = null, error = null) }
    }
}

data class ExportUiState(
    val project: Project? = null,
    val takeoffType: String = "",
    val result: TakeoffResult? = null,
    val textContent: String = "",
    val summaryContent: String = "",
    val csvContent: String = "",
    val lastAction: String? = null,
    val error: String? = null
)
