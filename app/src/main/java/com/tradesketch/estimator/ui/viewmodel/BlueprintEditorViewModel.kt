package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintCommand
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintDocumentCommand
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class BlueprintEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val WALL_SCOPE_TAG_PREFIX = "trade_scope:"
        private const val FLOOR_TAG_PREFIX = "floor:"
        private const val FLOOR_DEFAULT_TAG = "${FLOOR_TAG_PREFIX}0"
        private const val FLOOR_LEGACY_LOWER_TAG = "${FLOOR_TAG_PREFIX}lower"
        private const val FLOOR_LEGACY_UPPER_TAG = "${FLOOR_TAG_PREFIX}upper"
    }

    private var currentProjectId: String? = savedStateHandle["projectId"]
    private var observerJob: Job? = null
    private val undoStack = ArrayDeque<BlueprintCommand>()
    private val redoStack = ArrayDeque<BlueprintCommand>()
    private val saveMutex = Mutex()

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
        updateDocument(label = "Add Wall") { document ->
            val updatedWalls = document.walls + wall
            val rooms = detectRoomsByFloor(
                walls = updatedWalls,
                existingRooms = document.rooms
            )
            document.copy(
                walls = updatedWalls,
                rooms = rooms
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun addWalls(walls: List<WallSegment>) {
        if (walls.isEmpty()) return
        updateDocument(label = "Add Walls") { document ->
            val updatedWalls = document.walls + walls
            val rooms = detectRoomsByFloor(
                walls = updatedWalls,
                existingRooms = document.rooms
            )
            document.copy(
                walls = updatedWalls,
                rooms = rooms
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun replaceWalls(walls: List<WallSegment>) {
        updateDocument(label = "Replace Walls") { document ->
            val rooms = detectRoomsByFloor(
                walls = walls,
                existingRooms = document.rooms
            )
            val openingsOnKnownWalls = document.openings.filter { opening ->
                walls.any { wall -> wall.id == opening.wallId }
            }
            val sanitizedOpenings = sanitizeOpeningsForWalls(
                walls = walls,
                openings = openingsOnKnownWalls
            )
            document.copy(
                walls = walls,
                rooms = rooms,
                openings = sanitizedOpenings
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun addOpening(opening: BlueprintOpening) {
        val document = _uiState.value.document ?: return
        val sanitized = opening.normalized()
        val placementError = openingPlacementError(document = document, opening = sanitized)
        if (placementError != null) {
            _uiState.update { it.copy(error = placementError) }
            return
        }
        updateDocument(label = "Add Opening") { document ->
            document.copy(
                openings = (document.openings + sanitized).distinctBy { it.id }
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun removeOpening(openingId: String) {
        updateDocument(label = "Remove Opening") { document ->
            document.copy(
                openings = document.openings.filterNot { it.id == openingId }
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun updateParams(params: BlueprintParams) {
        updateDocument(label = "Update Parameters") { document ->
            document.copy(params = params)
        }
    }

    fun updateWallHeight(heightMm: Long) {
        val sanitizedHeight = heightMm.coerceAtLeast(1L)
        updateDocument(label = "Update Wall Height") { document ->
            val updatedWalls = document.walls.map { wall ->
                wall.copy(height = Millimeters(sanitizedHeight))
            }
            val sanitizedOpenings = sanitizeOpeningsForWalls(
                walls = updatedWalls,
                openings = document.openings
            )
            document.copy(
                params = document.params.copy(wallHeightMm = sanitizedHeight),
                walls = updatedWalls,
                openings = sanitizedOpenings
            )
        }
    }

    fun updateDrywallSessionParams(
        sheetAreaSqFt: Double? = null,
        wastePercent: Double? = null,
        screwsPerSheet: Int? = null,
        mudGallonsPer100SqFt: Double? = null,
        includeCeilings: Boolean? = null
    ) {
        updateTakeoffSession(errorMessage = "Failed to update drywall settings") { session ->
            session.copy(
                drywall = session.drywall.copy(
                    sheetAreaSqFt = sheetAreaSqFt?.coerceAtLeast(1.0) ?: session.drywall.sheetAreaSqFt,
                    wastePercent = wastePercent?.coerceAtLeast(0.0) ?: session.drywall.wastePercent,
                    screwsPerSheet = screwsPerSheet?.coerceAtLeast(0) ?: session.drywall.screwsPerSheet,
                    mudGallonsPer100SqFt = mudGallonsPer100SqFt?.coerceAtLeast(0.0) ?: session.drywall.mudGallonsPer100SqFt,
                    includeCeilings = includeCeilings ?: session.drywall.includeCeilings
                )
            )
        }
    }

    fun updateConcreteSessionParams(thicknessFeet: Double? = null, wastePercent: Double? = null) {
        updateTakeoffSession(errorMessage = "Failed to update concrete settings") { session ->
            session.copy(
                concrete = session.concrete.copy(
                    thicknessFeet = thicknessFeet?.coerceAtLeast(0.0) ?: session.concrete.thicknessFeet,
                    wastePercent = wastePercent?.coerceAtLeast(0.0) ?: session.concrete.wastePercent
                )
            )
        }
    }

    fun updateGravelSessionParams(
        depthFeet: Double? = null,
        densityTonsPerYard: Double? = null,
        wastePercent: Double? = null
    ) {
        updateTakeoffSession(errorMessage = "Failed to update gravel settings") { session ->
            session.copy(
                gravel = session.gravel.copy(
                    depthFeet = depthFeet?.coerceAtLeast(0.0) ?: session.gravel.depthFeet,
                    densityTonsPerYard = densityTonsPerYard?.coerceAtLeast(0.0) ?: session.gravel.densityTonsPerYard,
                    wastePercent = wastePercent?.coerceAtLeast(0.0) ?: session.gravel.wastePercent
                )
            )
        }
    }

    fun updatePaintSessionParams(
        coverageSqFtPerGallon: Double? = null,
        coats: Int? = null,
        wastePercent: Double? = null
    ) {
        updateTakeoffSession(errorMessage = "Failed to update paint settings") { session ->
            session.copy(
                paint = session.paint.copy(
                    coverageSqFtPerGallon = coverageSqFtPerGallon?.coerceAtLeast(1.0) ?: session.paint.coverageSqFtPerGallon,
                    coats = coats?.coerceAtLeast(1) ?: session.paint.coats,
                    wastePercent = wastePercent?.coerceAtLeast(0.0) ?: session.paint.wastePercent
                )
            )
        }
    }

    fun expandScopeWithPaint() {
        updateDocument(label = "Scope Expansion: Paint") { document ->
            val updatedRooms = document.rooms.map { room ->
                room.copy(tags = room.tags + "paint")
            }
            document.copy(
                rooms = updatedRooms
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun ensureRoomDetection() {
        val document = _uiState.value.document ?: return
        val detected = detectRoomsByFloor(
            walls = document.walls,
            existingRooms = document.rooms
        )
        if (detected.isEmpty()) return
        updateDocument(label = "Detect Rooms") {
            it.copy(rooms = detected)
        }
    }

    fun undo() {
        val currentDoc = _uiState.value.document ?: return
        if (undoStack.isEmpty()) return
        val command = undoStack.removeLast()
        val previous = command.undo(currentDoc)
        redoStack.addLast(command)
        _uiState.update {
            val updatedProject = it.project?.copy(blueprintDocument = previous)
            it.copy(
                project = updatedProject,
                document = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistDocument(previous)
    }

    fun redo() {
        val currentDoc = _uiState.value.document ?: return
        if (redoStack.isEmpty()) return
        val command = redoStack.removeLast()
        val next = command.apply(currentDoc)
        undoStack.addLast(command)
        _uiState.update {
            val updatedProject = it.project?.copy(blueprintDocument = next)
            it.copy(
                project = updatedProject,
                document = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistDocument(next)
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
        updateDocument(label = "Delete Wall") { document ->
            val updatedWalls = document.walls.filterNot { it.id == wallId }
            val rooms = detectRoomsByFloor(
                walls = updatedWalls,
                existingRooms = document.rooms
            )
            document.copy(
                walls = updatedWalls,
                rooms = rooms,
                openings = document.openings.filterNot { it.wallId == wallId }
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
        _uiState.update { it.copy(selectedWallId = null) }
    }

    fun deleteSelectedOpening() {
        val openingId = _uiState.value.selectedOpeningId ?: return
        removeOpening(openingId)
        _uiState.update { it.copy(selectedOpeningId = null) }
    }

    fun clearAllGeometry() {
        updateDocument(label = "Clear Blueprint") { document ->
            if (document.walls.isEmpty() && document.openings.isEmpty() && document.rooms.isEmpty()) {
                document
            } else {
                document.copy(
                    walls = emptyList(),
                    openings = emptyList(),
                    rooms = emptyList()
                ).withUndoMeta(undoDepth = undoStack.size + 1)
            }
        }
        _uiState.update {
            it.copy(
                selectedWallId = null,
                selectedOpeningId = null,
                selectedRoomId = null
            )
        }
    }

    fun updateWall(wallId: String, updatedWall: WallSegment) {
        updateDocument(label = "Update Wall") { document ->
            val updatedWalls = document.walls.map { if (it.id == wallId) updatedWall else it }
            val rooms = detectRoomsByFloor(
                walls = updatedWalls,
                existingRooms = document.rooms
            )
            val sanitizedOpenings = sanitizeOpeningsForWalls(
                walls = updatedWalls,
                openings = document.openings
            )
            document.copy(
                walls = updatedWalls,
                rooms = rooms,
                openings = sanitizedOpenings
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
    }

    fun splitWall(wallId: String, splitPoint: com.tradesketch.estimator.domain.model.PointMm) {
        updateDocument(label = "Split Wall") { document ->
            val wall = document.walls.find { it.id == wallId } ?: return@updateDocument document
            val wall1 = wall.copy(id = java.util.UUID.randomUUID().toString(), end = splitPoint)
            val wall2 = wall.copy(id = java.util.UUID.randomUUID().toString(), start = splitPoint)
            val updatedWalls = document.walls.filterNot { it.id == wallId } + wall1 + wall2
            val rooms = detectRoomsByFloor(
                walls = updatedWalls,
                existingRooms = document.rooms
            )
            document.copy(
                walls = updatedWalls,
                rooms = rooms,
                openings = document.openings.filterNot { it.wallId == wallId }
            ).withUndoMeta(undoDepth = undoStack.size + 1)
        }
        _uiState.update { it.copy(selectedWallId = null) }
    }

    fun updateTakeoffScope(scope: TakeoffScope) {
        val previousScope = _uiState.value.project?.takeoffSession?.selectedScope ?: return
        if (previousScope != scope) {
            stampUnscopedWalls(previousScope)
        }
        updateTakeoffSession(errorMessage = "Failed to update scope") { session ->
            if (session.selectedScope == scope) {
                session
            } else {
                session.copy(selectedScope = scope)
            }
        }
    }

    private fun stampUnscopedWalls(scope: TakeoffScope) {
        val scopeTag = scope.wallScopeTag()
        updateDocument(label = "Assign Wall Trade", trackUndo = false) { document ->
            val updatedWalls = document.walls.map { wall ->
                if (wall.tags.any { it.startsWith(WALL_SCOPE_TAG_PREFIX) }) {
                    wall
                } else {
                    wall.copy(tags = wall.tags + scopeTag)
                }
            }
            if (updatedWalls == document.walls) {
                document
            } else {
                document.copy(walls = updatedWalls)
            }
        }
    }

    private fun updateTakeoffSession(
        errorMessage: String,
        transform: (ProjectTakeoffSession) -> ProjectTakeoffSession
    ) {
        val project = _uiState.value.project ?: return
        val updatedSession = transform(project.takeoffSession)
        if (updatedSession == project.takeoffSession) return
        val updatedProject = project.copy(
            takeoffSession = updatedSession,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.update { it.copy(project = updatedProject) }
        persistProject(errorMessage = errorMessage) { current ->
            current.copy(takeoffSession = transform(current.takeoffSession))
        }
    }

    private fun updateDocument(
        label: String = "Edit Blueprint",
        trackUndo: Boolean = true,
        transform: (BlueprintDocument) -> BlueprintDocument
    ) {
        val state = _uiState.value
        val project = state.project ?: return
        val current = state.document ?: return
        val updated = transform(current)
        if (updated == current) return

        // Any successful document mutation invalidates redo history, even if undo tracking is disabled.
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
        }
        if (trackUndo) {
            undoStack.addLast(
                BlueprintDocumentCommand(
                    label = label,
                    before = current,
                    after = updated
                )
            )
            if (undoStack.size > 100) {
                undoStack.removeFirst()
            }
        }

        _uiState.update {
            val updatedProject = project.copy(
                blueprintDocument = updated,
                updatedAt = System.currentTimeMillis()
            )
            val nextSelectedWallId = it.selectedWallId?.takeIf { selectedId ->
                updated.walls.any { wall -> wall.id == selectedId }
            }
            val nextSelectedOpeningId = it.selectedOpeningId?.takeIf { selectedId ->
                updated.openings.any { opening -> opening.id == selectedId }
            }
            val nextSelectedRoomId = it.selectedRoomId?.takeIf { selectedId ->
                updated.rooms.any { room -> room.id == selectedId }
            }
            it.copy(
                project = updatedProject,
                document = updated,
                selectedWallId = nextSelectedWallId,
                selectedOpeningId = nextSelectedOpeningId,
                selectedRoomId = nextSelectedRoomId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistDocument(updated)
    }

    private fun persistDocument(updated: BlueprintDocument) {
        persistProject(errorMessage = "Failed to save blueprint") { project ->
            project.copy(blueprintDocument = updated)
        }
    }

    private fun persistProject(
        errorMessage: String,
        transform: (Project) -> Project
    ) {
        viewModelScope.launch {
            saveMutex.withLock {
                val project = _uiState.value.project ?: return@withLock
                val updatedProject = transform(project).copy(updatedAt = System.currentTimeMillis())
                runCatching {
                    saveProjectUseCase(updatedProject)
                }.onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: errorMessage) }
                }
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

    private fun detectRoomsByFloor(
        walls: List<WallSegment>,
        existingRooms: List<Room>
    ): List<Room> {
        if (walls.isEmpty()) {
            // Preserve manually defined rooms (slabs/beds/custom spaces) when no wall loops are present.
            return existingRooms
                .filterNot { it.isAutoDetectedRoom() }
                .map { room -> room.copy(tags = room.tags.withFloorTag(room.floorTag())) }
        }
        val existingByFloor = existingRooms.groupBy { room -> room.floorTag() }
        val wallsByFloor = walls.groupBy { wall -> wall.floorTag() }
        val allFloorTags = (existingByFloor.keys + wallsByFloor.keys).toSortedSet()
        return allFloorTags
            .flatMap { floorTag ->
                val floorWalls = wallsByFloor[floorTag].orEmpty()
                val floorExisting = existingByFloor[floorTag].orEmpty()
                val preservedManualRooms = floorExisting
                    .filterNot { room -> room.isAutoDetectedRoom() }
                    .map { room -> room.copy(tags = room.tags.withFloorTag(floorTag)) }
                if (floorWalls.isEmpty()) {
                    return@flatMap preservedManualRooms
                }
                val detected = RoomLoopDetector.detectRooms(floorWalls)
                if (detected.isEmpty()) {
                    preservedManualRooms
                } else {
                    val detectedWithMetadata = mergeRoomNames(existing = floorExisting, detected = detected)
                        .map { detectedRoom ->
                            val existingMatch = floorExisting.firstOrNull { known ->
                                known.isAutoDetectedRoom() && signature(known) == signature(detectedRoom)
                            }
                            detectedRoom.copy(
                                tags = (existingMatch?.tags ?: detectedRoom.tags).withFloorTag(floorTag),
                                ceiling = existingMatch?.ceiling ?: detectedRoom.ceiling,
                                overrides = existingMatch?.overrides ?: detectedRoom.overrides
                            )
                        }
                    preservedManualRooms + detectedWithMetadata
                }
            }
    }

    private fun signature(room: Room): String {
        val sorted = room.polygon
            .sortedWith(compareBy<com.tradesketch.estimator.domain.model.PointMm> { it.x }.thenBy { it.y })
        return sorted.joinToString(separator = "|") { "${it.x}:${it.y}" }
    }

    private fun WallSegment.floorTag(): String {
        return canonicalFloorTag(tags.firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) })
    }

    private fun Room.floorTag(): String {
        return canonicalFloorTag(tags.firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) })
    }

    private fun Set<String>.withFloorTag(floorTag: String): Set<String> {
        return filterNot { tag -> tag.startsWith(FLOOR_TAG_PREFIX) }
            .toSet() + canonicalFloorTag(floorTag)
    }

    private fun canonicalFloorTag(rawTag: String?): String {
        val normalized = rawTag?.trim() ?: return FLOOR_DEFAULT_TAG
        if (!normalized.startsWith(FLOOR_TAG_PREFIX)) return FLOOR_DEFAULT_TAG
        if (normalized.equals(FLOOR_LEGACY_LOWER_TAG, ignoreCase = true)) return FLOOR_DEFAULT_TAG
        if (normalized.equals(FLOOR_LEGACY_UPPER_TAG, ignoreCase = true)) return "${FLOOR_TAG_PREFIX}1"
        val numeric = normalized.removePrefix(FLOOR_TAG_PREFIX).toIntOrNull() ?: return FLOOR_DEFAULT_TAG
        return "${FLOOR_TAG_PREFIX}$numeric"
    }

    private fun Room.isAutoDetectedRoom(): Boolean {
        return wallSegmentIds.isNotEmpty() ||
            wallLoopRef.isNotEmpty() ||
            id.startsWith("room-auto-")
    }

    private fun openingPlacementError(
        document: BlueprintDocument,
        opening: BlueprintOpening
    ): String? {
        val wall = document.walls.firstOrNull { wall -> wall.id == opening.wallId }
            ?: return "Cannot place opening because the wall no longer exists."
        val wallLengthMm = wall.lengthMillimeters().coerceAtLeast(1L)
        if (opening.widthMm >= wallLengthMm) {
            return "Opening width must be smaller than the wall length."
        }
        if (opening.heightMm + opening.sillMm > wall.heightMm) {
            return "Opening height and sill exceed the wall height."
        }
        val halfT = (opening.widthMm.toDouble() / wallLengthMm.toDouble()) / 2.0
        val minT = opening.t - halfT
        val maxT = opening.t + halfT
        if (minT < 0.02 || maxT > 0.98) {
            return "Opening must stay clear of wall endpoints."
        }
        val overlapsExisting = document.openings.any { existing ->
            if (existing.wallId != opening.wallId || existing.id == opening.id) return@any false
            val existingHalfT = (existing.widthMm.toDouble() / wallLengthMm.toDouble()) / 2.0
            val existingMinT = existing.t - existingHalfT
            val existingMaxT = existing.t + existingHalfT
            minT < existingMaxT && maxT > existingMinT
        }
        if (overlapsExisting) {
            return "Opening overlaps an existing opening on this wall."
        }
        return null
    }

    private fun sanitizeOpeningsForWalls(
        walls: List<WallSegment>,
        openings: List<BlueprintOpening>
    ): List<BlueprintOpening> {
        if (openings.isEmpty()) return emptyList()
        val baseline = BlueprintDocument(
            projectId = _uiState.value.project?.id ?: "",
            walls = walls,
            openings = emptyList()
        )
        val accepted = mutableListOf<BlueprintOpening>()
        openings.forEach { opening ->
            val normalized = opening.normalized()
            val candidate = baseline.copy(openings = accepted.toList())
            if (openingPlacementError(document = candidate, opening = normalized) == null) {
                accepted += normalized
            }
        }
        return accepted
    }

    private fun BlueprintDocument.withUndoMeta(
        undoDepth: Int,
        redoDepth: Int = 0
    ): BlueprintDocument {
        return copy(
            undoStackMeta = undoStackMeta.copy(
                undoDepth = undoDepth,
                redoDepth = redoDepth,
                revision = undoStackMeta.revision + 1
            )
        )
    }

    private fun TakeoffScope.wallScopeTag(): String = when (this) {
        TakeoffScope.DRYWALL -> "${WALL_SCOPE_TAG_PREFIX}drywall"
        TakeoffScope.CONCRETE -> "${WALL_SCOPE_TAG_PREFIX}concrete"
        TakeoffScope.GRAVEL_MULCH -> "${WALL_SCOPE_TAG_PREFIX}gravel_mulch"
        TakeoffScope.PAINT -> "${WALL_SCOPE_TAG_PREFIX}paint"
    }
}

enum class BlueprintDraftTool {
    SELECT,
    DRAW_WALL,
    DRAW_BOX,
    PLACE_DOOR,
    PLACE_WINDOW,
    PLACE_STAIR_UP,
    PLACE_STAIR_DOWN,
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
