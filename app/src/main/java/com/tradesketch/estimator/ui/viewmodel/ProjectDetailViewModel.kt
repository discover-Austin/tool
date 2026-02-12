package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * ViewModel for Project Detail screen.
 * Manages current project, spaces, and model operations.
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

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
            repository.getProjects()
                .map { projects -> projects.find { it.id == projectId } }
                .collect { project ->
                    if (project != null) {
                        _uiState.update {
                            it.copy(
                                project = project,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(error = "Project not found", isLoading = false) }
                    }
                }
        }
    }
    
    fun addSpace(space: Space) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val placedSpace = if (space.transform == SpaceTransform()) {
                space.copy(transform = suggestedTransformForIndex(currentProject.spaces.size))
            } else {
                space
            }
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces + placedSpace,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun addSpaces(spaces: List<Space>) {
        if (spaces.isEmpty()) return
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val startIndex = currentProject.spaces.size
            val placedSpaces = spaces.mapIndexed { index, space ->
                if (space.transform == SpaceTransform()) {
                    space.copy(transform = suggestedTransformForIndex(startIndex + index))
                } else {
                    space
                }
            }
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces + placedSpaces,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun updateSpace(space: Space) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val existingSpace = currentProject.spaces.find { it.id == space.id } ?: return@launch
            val mergedTransform = if (
                space.transform == SpaceTransform() &&
                existingSpace.transform != SpaceTransform()
            ) {
                existingSpace.transform
            } else {
                space.transform
            }
            val mergedSpace = space.copy(transform = mergedTransform)
            val updatedSpaces = currentProject.spaces.map { 
                if (it.id == space.id) mergedSpace else it 
            }
            val updatedProject = currentProject.copy(
                spaces = updatedSpaces,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun duplicateSpace(spaceId: String) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val source = currentProject.spaces.find { it.id == spaceId } ?: return@launch
            val duplicate = source.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${source.name} Copy",
                transform = duplicateTransform(
                    source = source.transform,
                    index = currentProject.spaces.size
                )
            )
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces + duplicate,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun updateSpaceTransform(spaceId: String, transform: SpaceTransform) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedSpaces = currentProject.spaces.map { space ->
                if (space.id == spaceId) {
                    space.copy(transform = transform)
                } else {
                    space
                }
            }
            if (updatedSpaces == currentProject.spaces) return@launch
            val updatedProject = currentProject.copy(
                spaces = updatedSpaces,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun autoLayoutSpaces() {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch
            val total = currentProject.spaces.size
            val arranged = currentProject.spaces.mapIndexed { index, space ->
                val suggestion = autoLayoutTransformForIndex(index = index, total = total)
                space.copy(
                    transform = space.transform.copy(
                        xFeet = suggestion.xFeet,
                        zFeet = suggestion.zFeet,
                        yawDegrees = suggestion.yawDegrees,
                        colorHex = if (space.transform.colorHex == SpaceTransform().colorHex) {
                            suggestion.colorHex
                        } else {
                            space.transform.colorHex
                        }
                    )
                )
            }
            val updatedProject = currentProject.copy(
                spaces = arranged,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun deleteSpace(spaceId: String) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                spaces = currentProject.spaces.filter { it.id != spaceId },
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
    
    fun updateProjectName(name: String) {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

private val modelPalette = listOf(
    0xFF4E79A7L,
    0xFFE15759L,
    0xFF76B7B2L,
    0xFFF28E2BL,
    0xFF59A14FL,
    0xFFEDC948L
)

private fun suggestedTransformForIndex(index: Int): SpaceTransform {
    val columns = 4
    val spacingFeet = 18.0
    val row = index / columns
    val col = index % columns
    val centeredCol = col - ((columns - 1) / 2.0)
    return SpaceTransform(
        xFeet = centeredCol * spacingFeet,
        yFeet = 0.0,
        zFeet = row * spacingFeet,
        yawDegrees = 0.0,
        colorHex = modelPalette[index % modelPalette.size]
    )
}

private fun autoLayoutTransformForIndex(index: Int, total: Int): SpaceTransform {
    if (total <= 0) return SpaceTransform(colorHex = modelPalette[index % modelPalette.size])
    val columns = ceil(sqrt(total.toDouble())).toInt().coerceAtLeast(1)
    val rows = ceil(total.toDouble() / columns).toInt().coerceAtLeast(1)
    val spacingFeet = 16.0
    val row = index / columns
    val col = index % columns
    val centeredCol = col - ((columns - 1) / 2.0)
    val centeredRow = row - ((rows - 1) / 2.0)
    return SpaceTransform(
        xFeet = centeredCol * spacingFeet,
        yFeet = 0.0,
        zFeet = centeredRow * spacingFeet,
        yawDegrees = 0.0,
        colorHex = modelPalette[index % modelPalette.size]
    )
}

private fun duplicateTransform(source: SpaceTransform, index: Int): SpaceTransform {
    val offset = 6.0 + (index % 3) * 1.75
    return source.copy(
        xFeet = source.xFeet + offset,
        zFeet = source.zFeet + offset,
        yawDegrees = normalizeDegrees(source.yawDegrees + 12.0),
        colorHex = modelPalette[index % modelPalette.size]
    )
}

private fun normalizeDegrees(value: Double): Double {
    var normalized = value % 360.0
    if (normalized > 180.0) normalized -= 360.0
    if (normalized < -180.0) normalized += 360.0
    return normalized
}
