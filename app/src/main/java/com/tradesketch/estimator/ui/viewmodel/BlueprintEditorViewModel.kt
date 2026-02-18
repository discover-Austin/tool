package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.model.toLegacySpaces
import com.tradesketch.estimator.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BlueprintEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var observerJob: Job? = null
    private val undoStack = ArrayDeque<BlueprintDocument>()
    private val redoStack = ArrayDeque<BlueprintDocument>()

    private val _uiState = MutableStateFlow(BlueprintEditorUiState())
    val uiState: StateFlow<BlueprintEditorUiState> = _uiState.asStateFlow()

    init {
        currentProjectId?.let(::setProjectId)
    }

    fun setProjectId(projectId: String) {
        if (currentProjectId == projectId && observerJob != null) return
        currentProjectId = projectId
        savedStateHandle["projectId"] = projectId
        undoStack.clear()
        redoStack.clear()
        observerJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        observerJob = viewModelScope.launch {
            projectRepository.getProjects()
                .map { projects -> projects.firstOrNull { it.id == projectId } }
                .collect { project ->
                    if (project == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Project not found",
                                project = null,
                                document = null,
                                canUndo = false,
                                canRedo = false
                            )
                        }
                    } else {
                        val document = project.authoritativeBlueprint()
                            .copy(projectId = project.id)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                project = project,
                                document = document,
                                canUndo = undoStack.isNotEmpty(),
                                canRedo = redoStack.isNotEmpty()
                            )
                        }
                    }
                }
        }
    }

    fun addWall(wall: WallSegment) {
        updateDocument { document ->
            val updatedWalls = document.walls + wall
            val rooms = RoomLoopDetector.detectRooms(updatedWalls)
            document.copy(
                walls = updatedWalls,
                rooms = mergeRoomNames(existing = document.rooms, detected = rooms),
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun replaceWalls(walls: List<WallSegment>) {
        updateDocument { document ->
            val rooms = RoomLoopDetector.detectRooms(walls)
            document.copy(
                walls = walls,
                rooms = mergeRoomNames(existing = document.rooms, detected = rooms),
                openings = document.openings.filter { opening -> walls.any { it.id == opening.wallId } },
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun addOpening(opening: BlueprintOpening) {
        updateDocument { document ->
            val sanitized = opening.normalized()
            document.copy(
                openings = (document.openings + sanitized).distinctBy { it.id },
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun removeOpening(openingId: String) {
        updateDocument { document ->
            document.copy(
                openings = document.openings.filterNot { it.id == openingId },
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun updateParams(params: BlueprintParams) {
        updateDocument(trackUndo = false) { document ->
            document.copy(params = params)
        }
    }

    fun expandScopeWithPaint() {
        updateDocument { document ->
            val updatedRooms = document.rooms.map { room ->
                room.copy(tags = room.tags + "paint")
            }
            document.copy(
                rooms = updatedRooms,
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun ensureRoomDetection() {
        val document = _uiState.value.document ?: return
        val detected = RoomLoopDetector.detectRooms(document.walls)
        if (detected.isEmpty()) return
        updateDocument(trackUndo = false) {
            it.copy(rooms = mergeRoomNames(existing = it.rooms, detected = detected))
        }
    }

    fun undo() {
        val currentDoc = _uiState.value.document ?: return
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        redoStack.addLast(currentDoc)
        persistDocument(previous, pushToUndo = false)
    }

    fun redo() {
        val currentDoc = _uiState.value.document ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(currentDoc)
        persistDocument(next, pushToUndo = false)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setDraftTool(tool: BlueprintDraftTool) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun selectWall(wallId: String?) {
        _uiState.update { it.copy(selectedWallId = wallId, selectedOpeningId = null, selectedRoomId = null) }
    }

    fun selectOpening(openingId: String?) {
        _uiState.update { it.copy(selectedOpeningId = openingId, selectedWallId = null, selectedRoomId = null) }
    }

    fun selectRoom(roomId: String?) {
        _uiState.update { it.copy(selectedRoomId = roomId, selectedWallId = null, selectedOpeningId = null) }
    }

    fun deleteSelectedWall() {
        val wallId = _uiState.value.selectedWallId ?: return
        updateDocument { document ->
            val updatedWalls = document.walls.filterNot { it.id == wallId }
            val rooms = RoomLoopDetector.detectRooms(updatedWalls)
            document.copy(
                walls = updatedWalls,
                rooms = mergeRoomNames(existing = document.rooms, detected = rooms),
                openings = document.openings.filterNot { it.wallId == wallId },
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
        _uiState.update { it.copy(selectedWallId = null) }
    }

    fun deleteSelectedOpening() {
        val openingId = _uiState.value.selectedOpeningId ?: return
        removeOpening(openingId)
        _uiState.update { it.copy(selectedOpeningId = null) }
    }

    fun updateWall(wallId: String, updatedWall: WallSegment) {
        updateDocument { document ->
            val updatedWalls = document.walls.map { if (it.id == wallId) updatedWall else it }
            val rooms = RoomLoopDetector.detectRooms(updatedWalls)
            document.copy(
                walls = updatedWalls,
                rooms = mergeRoomNames(existing = document.rooms, detected = rooms),
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
    }

    fun splitWall(wallId: String, splitPoint: com.tradesketch.estimator.domain.model.PointMm) {
        updateDocument { document ->
            val wall = document.walls.find { it.id == wallId } ?: return@updateDocument document
            val wall1 = wall.copy(id = java.util.UUID.randomUUID().toString(), end = splitPoint)
            val wall2 = wall.copy(id = java.util.UUID.randomUUID().toString(), start = splitPoint)
            val updatedWalls = document.walls.filterNot { it.id == wallId } + wall1 + wall2
            val rooms = RoomLoopDetector.detectRooms(updatedWalls)
            document.copy(
                walls = updatedWalls,
                rooms = mergeRoomNames(existing = document.rooms, detected = rooms),
                openings = document.openings.filterNot { it.wallId == wallId },
                undoStackMeta = document.undoStackMeta.copy(
                    undoDepth = undoStack.size + 1,
                    redoDepth = 0,
                    revision = document.undoStackMeta.revision + 1
                )
            )
        }
        _uiState.update { it.copy(selectedWallId = null) }
    }

    private fun updateDocument(
        trackUndo: Boolean = true,
        transform: (BlueprintDocument) -> BlueprintDocument
    ) {
        val state = _uiState.value
        val project = state.project ?: return
        val current = state.document ?: return
        val updated = transform(current)
        if (updated == current) return

        if (trackUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 100) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }

        persistDocument(updated, pushToUndo = false)
        _uiState.update {
            it.copy(
                project = project,
                document = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    private fun persistDocument(updated: BlueprintDocument, pushToUndo: Boolean) {
        val project = _uiState.value.project ?: return
        if (pushToUndo) {
            _uiState.value.document?.let { undoStack.addLast(it) }
        }
        viewModelScope.launch {
            runCatching {
                saveProjectUseCase(
                    project.copy(
                        blueprintDocument = updated,
                        spaces = updated.toLegacySpaces(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Failed to save blueprint") }
            }
        }
    }

    private fun mergeRoomNames(existing: List<Room>, detected: List<Room>): List<Room> {
        if (detected.isEmpty()) return existing
        val namesBySignature = existing.associateBy(
            keySelector = { room -> signature(room) },
            valueTransform = { room -> room.name }
        )
        return detected.mapIndexed { index, room ->
            val signature = signature(room)
            val existingName = namesBySignature[signature]
            room.copy(name = existingName ?: "Room ${index + 1}")
        }
    }

    private fun signature(room: Room): String {
        val sorted = room.polygon
            .sortedWith(compareBy<com.tradesketch.estimator.domain.model.PointMm> { it.x }.thenBy { it.y })
        return sorted.joinToString(separator = "|") { "${it.x}:${it.y}" }
    }
}

enum class BlueprintDraftTool {
    SELECT,
    DRAW_WALL,
    PLACE_DOOR,
    PLACE_WINDOW,
    PAN,
    MEASURE
}

data class BlueprintEditorUiState(
    val project: Project? = null,
    val document: BlueprintDocument? = null,
    val activeTool: BlueprintDraftTool = BlueprintDraftTool.DRAW_WALL,
    val selectedWallId: String? = null,
    val selectedOpeningId: String? = null,
    val selectedRoomId: String? = null,
    val isLoading: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null
)
