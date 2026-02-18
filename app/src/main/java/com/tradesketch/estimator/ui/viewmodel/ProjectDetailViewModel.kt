package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.calc.BlueprintLayoutOptimizer
import com.tradesketch.estimator.domain.model.*
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val settingsRepository: SettingsRepository,
    private val uxMetricsRepository: UxMetricsRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        const val MAX_HISTORY_ENTRIES = 40
        const val HISTORY_PROJECT_ID_KEY = "blueprint_history_project_id"
        const val UNDO_HISTORY_KEY = "blueprint_undo_history_json"
        const val REDO_HISTORY_KEY = "blueprint_redo_history_json"
    }

    private val gson = Gson()
    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var projectObserverJob: Job? = null
    private var historyProjectId: String? = null
    private val undoHistory = ArrayDeque<List<Space>>()
    private val redoHistory = ArrayDeque<List<Space>>()

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(BlueprintHistoryUiState())
    val historyUiState: StateFlow<BlueprintHistoryUiState> = _historyUiState.asStateFlow()

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
        initializeHistory(projectId)
        _uiState.update { it.copy(isLoading = true, error = null) }
        projectObserverJob = viewModelScope.launch {
            combine(
                repository.getProjects().map { projects -> projects.find { it.id == projectId } },
                settingsRepository.getSettings()
            ) { project, settings -> project to settings }
                .collect { (project, settings) ->
                    if (project != null) {
                        ensureHistoryProject(project.id)
                        _uiState.update {
                            it.copy(
                                project = project,
                                settings = settings,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                project = null,
                                settings = settings,
                                error = "Project not found",
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun addSpace(space: Space) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_add_space")
            val currentProject = _uiState.value.project ?: return@launch
            val placedSpace = if (space.transform == SpaceTransform()) {
                space.copy(transform = suggestedTransformForIndex(currentProject.spaces.size))
            } else {
                space
            }
            val normalizedSpace = placedSpace.copy(
                name = resolveNextSpaceName(
                    space = placedSpace,
                    existingSpaces = currentProject.spaces
                )
            )
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = currentProject.spaces + normalizedSpace
            )
        }
    }

    fun addSpaces(spaces: List<Space>) {
        if (spaces.isEmpty()) return
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_add_spaces_bulk")
            val currentProject = _uiState.value.project ?: return@launch
            val startIndex = currentProject.spaces.size
            val placedSpaces = spaces.mapIndexed { index, space ->
                if (space.transform == SpaceTransform()) {
                    space.copy(transform = suggestedTransformForIndex(startIndex + index))
                } else {
                    space
                }
            }
            val normalizedSpaces = buildList {
                placedSpaces.forEach { space ->
                    add(
                        space.copy(
                            name = resolveNextSpaceName(
                                space = space,
                                existingSpaces = currentProject.spaces + this
                            )
                        )
                    )
                }
            }
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = currentProject.spaces + normalizedSpaces
            )
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
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = updatedSpaces
            )
        }
    }

    fun duplicateSpace(spaceId: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_duplicate_space")
            val currentProject = _uiState.value.project ?: return@launch
            val source = currentProject.spaces.find { it.id == spaceId } ?: return@launch
            val existingNames = currentProject.spaces.map { it.name }.toSet()
            val duplicate = source.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = ensureUniqueSpaceName("${source.name} Copy", existingNames),
                transform = duplicateTransform(
                    source = source.transform,
                    index = currentProject.spaces.size
                )
            )
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = currentProject.spaces + duplicate
            )
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
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = updatedSpaces
            )
        }
    }

    fun autoLayoutSpaces() {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_auto_layout")
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
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = arranged
            )
        }
    }

    fun flattenElevations() {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch
            val flattenedSpaces = currentProject.spaces.map { space ->
                val transform = space.transform
                if (transform.yFeet == 0.0) {
                    space
                } else {
                    space.copy(transform = transform.copy(yFeet = 0.0))
                }
            }
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = flattenedSpaces
            )
        }
    }

    fun snapLayoutToGrid(gridFeet: Double = 1.0) {
        if (gridFeet <= 0.0) return
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch
            val snappedSpaces = currentProject.spaces.map { space ->
                val transform = space.transform
                val snappedTransform = transform.copy(
                    xFeet = snapToStep(transform.xFeet, gridFeet),
                    yFeet = snapToStep(transform.yFeet, gridFeet).coerceAtLeast(0.0),
                    zFeet = snapToStep(transform.zFeet, gridFeet),
                    yawDegrees = normalizeDegrees(snapToStep(transform.yawDegrees, 15.0))
                )
                if (snappedTransform == transform) {
                    space
                } else {
                    space.copy(transform = snappedTransform)
                }
            }
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = snappedSpaces
            )
        }
    }

    fun optimizeBlueprintLayout(gridFeet: Double = 1.0) {
        if (gridFeet <= 0.0) return
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch
            val optimizedSpaces = BlueprintLayoutOptimizer.optimize(
                spaces = currentProject.spaces,
                gridFeet = gridFeet
            )
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = optimizedSpaces
            )
        }
    }

    fun centerLayoutAtOrigin() {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch

            val bounds = layoutBounds(currentProject.spaces)
            val centerX = (bounds.minX + bounds.maxX) / 2.0
            val centerZ = (bounds.minZ + bounds.maxZ) / 2.0
            if (kotlin.math.abs(centerX) < 0.01 && kotlin.math.abs(centerZ) < 0.01) return@launch

            val centeredSpaces = currentProject.spaces.map { space ->
                val transform = space.transform
                space.copy(
                    transform = transform.copy(
                        xFeet = snapToStep(transform.xFeet - centerX, 0.5),
                        zFeet = snapToStep(transform.zFeet - centerZ, 0.5)
                    )
                )
            }
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = centeredSpaces
            )
        }
    }

    fun alignLayoutToCardinal() {
        viewModelScope.launch {
            val currentProject = _uiState.value.project ?: return@launch
            if (currentProject.spaces.isEmpty()) return@launch
            val alignedSpaces = currentProject.spaces.map { space ->
                val transform = space.transform
                val alignedYaw = normalizeDegrees(snapToStep(transform.yawDegrees, 90.0))
                if (alignedYaw == transform.yawDegrees) {
                    space
                } else {
                    space.copy(transform = transform.copy(yawDegrees = alignedYaw))
                }
            }
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = alignedSpaces
            )
        }
    }

    fun undoBlueprintChange() {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("blueprint_undo")
            val currentProject = _uiState.value.project ?: return@launch
            ensureHistoryProject(currentProject.id)
            if (undoHistory.isEmpty()) return@launch
            val previousSnapshot = undoHistory.removeLast()
            redoHistory.addLast(currentProject.spaces.toList())
            trimHistory(redoHistory)
            updateHistoryUiState()
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = previousSnapshot,
                trackHistory = false
            )
        }
    }

    fun redoBlueprintChange() {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("blueprint_redo")
            val currentProject = _uiState.value.project ?: return@launch
            ensureHistoryProject(currentProject.id)
            if (redoHistory.isEmpty()) return@launch
            val nextSnapshot = redoHistory.removeLast()
            undoHistory.addLast(currentProject.spaces.toList())
            trimHistory(undoHistory)
            updateHistoryUiState()
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = nextSnapshot,
                trackHistory = false
            )
        }
    }

    fun deleteSpace(spaceId: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_delete_space")
            val currentProject = _uiState.value.project ?: return@launch
            saveSpacesChange(
                currentProject = currentProject,
                updatedSpaces = currentProject.spaces.filter { it.id != spaceId }
            )
        }
    }

    fun updateProjectName(name: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap("detail_rename_project")
            val currentProject = _uiState.value.project ?: return@launch
            val updatedProject = currentProject.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
            saveProjectUseCase(updatedProject)
        }
    }

    fun recordTap(task: String) {
        viewModelScope.launch {
            uxMetricsRepository.recordTap(task)
        }
    }

    private suspend fun saveSpacesChange(
        currentProject: Project,
        updatedSpaces: List<Space>,
        trackHistory: Boolean = true
    ) {
        if (updatedSpaces == currentProject.spaces) return
        ensureHistoryProject(currentProject.id)
        if (trackHistory) {
            undoHistory.addLast(currentProject.spaces.toList())
            trimHistory(undoHistory)
            redoHistory.clear()
            updateHistoryUiState()
        }
        val updatedProject = currentProject.copy(
            spaces = updatedSpaces,
            updatedAt = System.currentTimeMillis()
        )
        saveProjectUseCase(updatedProject)
    }

    private fun ensureHistoryProject(projectId: String) {
        if (historyProjectId == projectId) return
        initializeHistory(projectId)
    }

    private fun initializeHistory(projectId: String) {
        historyProjectId = projectId
        if (!restoreHistoryFromSavedState(projectId)) {
            undoHistory.clear()
            redoHistory.clear()
        }
        updateHistoryUiState()
    }

    private fun restoreHistoryFromSavedState(projectId: String): Boolean {
        val persistedProjectId: String = savedStateHandle[HISTORY_PROJECT_ID_KEY] ?: return false
        if (persistedProjectId != projectId) return false
        undoHistory.clear()
        redoHistory.clear()
        decodeHistory(savedStateHandle[UNDO_HISTORY_KEY]).forEach { snapshot ->
            undoHistory.addLast(snapshot)
        }
        decodeHistory(savedStateHandle[REDO_HISTORY_KEY]).forEach { snapshot ->
            redoHistory.addLast(snapshot)
        }
        trimHistory(undoHistory)
        trimHistory(redoHistory)
        return true
    }

    private fun encodeHistory(buffer: ArrayDeque<List<Space>>): String {
        return gson.toJson(buffer.map { SpaceHistorySnapshot.fromSpaces(it) })
    }

    private fun decodeHistory(raw: String?): List<List<Space>> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SpaceHistorySnapshot>>() {}.type
            val snapshots: List<SpaceHistorySnapshot> = gson.fromJson(raw, type) ?: emptyList()
            snapshots.map { it.toSpaces() }
        }.getOrDefault(emptyList())
    }

    private fun persistHistoryState() {
        savedStateHandle[HISTORY_PROJECT_ID_KEY] = historyProjectId
        savedStateHandle[UNDO_HISTORY_KEY] = encodeHistory(undoHistory)
        savedStateHandle[REDO_HISTORY_KEY] = encodeHistory(redoHistory)
    }

    private fun trimHistory(buffer: ArrayDeque<List<Space>>) {
        while (buffer.size > MAX_HISTORY_ENTRIES) {
            buffer.removeFirst()
        }
    }

    private fun updateHistoryUiState() {
        _historyUiState.value = BlueprintHistoryUiState(
            canUndo = undoHistory.isNotEmpty(),
            canRedo = redoHistory.isNotEmpty(),
            undoCount = undoHistory.size,
            redoCount = redoHistory.size
        )
        persistHistoryState()
    }
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val settings: Settings = Settings.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class BlueprintHistoryUiState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoCount: Int = 0,
    val redoCount: Int = 0
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

private fun snapToStep(value: Double, step: Double): Double {
    if (step <= 0.0) return value
    return kotlin.math.round(value / step) * step
}

private val genericSpaceNamePattern = Regex(
    pattern = "^(room|wall|slab|space|ceiling|gravel\\s*/?\\s*mulch\\s*bed|gravel\\s*bed|mulch\\s*bed|yard\\s*bed)(\\s+\\d+)?$",
    option = RegexOption.IGNORE_CASE
)

private fun resolveNextSpaceName(
    space: Space,
    existingSpaces: List<Space>
): String {
    val existingNames = existingSpaces.map { it.name }.toSet()
    val currentName = space.name.trim()
    val baseName = inferSemanticBaseName(space, currentName)
    return if (currentName.isEmpty() || genericSpaceNamePattern.matches(currentName)) {
        nextIndexedSpaceName(baseName, existingNames)
    } else {
        ensureUniqueSpaceName(currentName, existingNames)
    }
}

private fun inferSemanticBaseName(space: Space, currentName: String): String {
    val lowered = currentName.lowercase()
    val landscapeHint = lowered.contains("gravel") ||
        lowered.contains("mulch") ||
        lowered.contains("yard") ||
        lowered.contains("bed")
    return when (space.geometry) {
        is Geometry.Wall -> "Wall"
        is Geometry.Slab -> "Slab"
        else -> if (landscapeHint) "Gravel/Mulch Bed" else "Room"
    }
}

private fun nextIndexedSpaceName(
    baseName: String,
    existingNames: Set<String>
): String {
    var index = 1
    while (true) {
        val candidate = "$baseName $index"
        if (candidate !in existingNames) {
            return candidate
        }
        index += 1
    }
}

private fun ensureUniqueSpaceName(
    preferredName: String,
    existingNames: Set<String>
): String {
    if (preferredName !in existingNames) return preferredName
    var index = 2
    while (true) {
        val candidate = "$preferredName $index"
        if (candidate !in existingNames) {
            return candidate
        }
        index += 1
    }
}

private data class SpaceBounds(
    val minX: Double,
    val maxX: Double,
    val minZ: Double,
    val maxZ: Double
)

private fun layoutBounds(spaces: List<Space>): SpaceBounds {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    spaces.forEach { space ->
        val (width, depth) = layoutFootprintDimensions(space.geometry)
        val halfW = width / 2.0
        val halfD = depth / 2.0
        minX = kotlin.math.min(minX, space.transform.xFeet - halfW)
        maxX = kotlin.math.max(maxX, space.transform.xFeet + halfW)
        minZ = kotlin.math.min(minZ, space.transform.zFeet - halfD)
        maxZ = kotlin.math.max(maxZ, space.transform.zFeet + halfD)
    }

    if (spaces.isEmpty()) {
        return SpaceBounds(minX = -5.0, maxX = 5.0, minZ = -5.0, maxZ = 5.0)
    }
    return SpaceBounds(minX = minX, maxX = maxX, minZ = minZ, maxZ = maxZ)
}

private fun layoutFootprintDimensions(geometry: Geometry): Pair<Double, Double> {
    return when (geometry) {
        is Geometry.Rect -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Slab -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Wall -> geometry.length.toFeet() to 0.75
        is Geometry.Circle -> {
            val diameter = geometry.radius.toFeet() * 2.0
            diameter to diameter
        }
        is Geometry.LShape -> {
            val width = kotlin.math.max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet())
            val depth = kotlin.math.max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet())
            width to depth
        }
    }
}

private data class SpaceHistorySnapshot(
    val spaces: List<SpaceHistoryEntry>
) {
    fun toSpaces(): List<Space> = spaces.map { it.toSpace() }

    companion object {
        fun fromSpaces(spaces: List<Space>): SpaceHistorySnapshot {
            return SpaceHistorySnapshot(
                spaces = spaces.map { SpaceHistoryEntry.fromSpace(it) }
            )
        }
    }
}

private data class SpaceHistoryEntry(
    val id: String,
    val name: String,
    val geometry: GeometryHistoryEntry,
    val openings: List<OpeningHistoryEntry>,
    val transform: SpaceTransformHistoryEntry
) {
    fun toSpace() = Space(
        id = id,
        name = name,
        geometry = geometry.toGeometry(),
        openings = openings.map { it.toOpening() },
        transform = transform.toTransform()
    )

    companion object {
        fun fromSpace(space: Space) = SpaceHistoryEntry(
            id = space.id,
            name = space.name,
            geometry = GeometryHistoryEntry.fromGeometry(space.geometry),
            openings = space.openings.map { OpeningHistoryEntry.fromOpening(it) },
            transform = SpaceTransformHistoryEntry.fromTransform(space.transform)
        )
    }
}

private data class GeometryHistoryEntry(
    val type: String,
    val length: Long? = null,
    val width: Long? = null,
    val height: Long? = null,
    val thickness: Long? = null,
    val radius: Long? = null,
    val rectA: RectHistoryEntry? = null,
    val rectB: RectHistoryEntry? = null
) {
    fun toGeometry(): Geometry = when (type) {
        "Rect" -> Geometry.Rect(
            length = Millimeters(length ?: 0L),
            width = Millimeters(width ?: 0L)
        )
        "Wall" -> Geometry.Wall(
            length = Millimeters(length ?: 0L),
            height = Millimeters(height ?: 0L)
        )
        "Slab" -> Geometry.Slab(
            length = Millimeters(length ?: 0L),
            width = Millimeters(width ?: 0L),
            thickness = Millimeters(thickness ?: 0L)
        )
        "Circle" -> Geometry.Circle(radius = Millimeters(radius ?: 0L))
        "LShape" -> Geometry.LShape(
            rectA = rectA?.toRect() ?: Geometry.Rect(Millimeters(0), Millimeters(0)),
            rectB = rectB?.toRect() ?: Geometry.Rect(Millimeters(0), Millimeters(0))
        )
        else -> Geometry.Rect(Millimeters(0), Millimeters(0))
    }

    companion object {
        fun fromGeometry(geometry: Geometry): GeometryHistoryEntry = when (geometry) {
            is Geometry.Rect -> GeometryHistoryEntry(
                type = "Rect",
                length = geometry.length.value,
                width = geometry.width.value
            )
            is Geometry.Wall -> GeometryHistoryEntry(
                type = "Wall",
                length = geometry.length.value,
                height = geometry.height.value
            )
            is Geometry.Slab -> GeometryHistoryEntry(
                type = "Slab",
                length = geometry.length.value,
                width = geometry.width.value,
                thickness = geometry.thickness.value
            )
            is Geometry.Circle -> GeometryHistoryEntry(
                type = "Circle",
                radius = geometry.radius.value
            )
            is Geometry.LShape -> GeometryHistoryEntry(
                type = "LShape",
                rectA = RectHistoryEntry.fromRect(geometry.rectA),
                rectB = RectHistoryEntry.fromRect(geometry.rectB)
            )
        }
    }
}

private data class RectHistoryEntry(
    val length: Long,
    val width: Long
) {
    fun toRect() = Geometry.Rect(
        length = Millimeters(length),
        width = Millimeters(width)
    )

    companion object {
        fun fromRect(rect: Geometry.Rect) = RectHistoryEntry(
            length = rect.length.value,
            width = rect.width.value
        )
    }
}

private data class OpeningHistoryEntry(
    val width: Long,
    val height: Long,
    val count: Int
) {
    fun toOpening() = Opening(
        width = Millimeters(width),
        height = Millimeters(height),
        count = count
    )

    companion object {
        fun fromOpening(opening: Opening) = OpeningHistoryEntry(
            width = opening.width.value,
            height = opening.height.value,
            count = opening.count
        )
    }
}

private data class SpaceTransformHistoryEntry(
    val xFeet: Double,
    val yFeet: Double,
    val zFeet: Double,
    val yawDegrees: Double,
    val colorHex: Long
) {
    fun toTransform() = SpaceTransform(
        xFeet = xFeet,
        yFeet = yFeet,
        zFeet = zFeet,
        yawDegrees = yawDegrees,
        colorHex = colorHex
    )

    companion object {
        fun fromTransform(transform: SpaceTransform) = SpaceTransformHistoryEntry(
            xFeet = transform.xFeet,
            yFeet = transform.yFeet,
            zFeet = transform.zFeet,
            yawDegrees = transform.yawDegrees,
            colorHex = transform.colorHex
        )
    }
}
