package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.abs

private const val TRADE_SCOPE_TAG_PREFIX = "trade_scope:"
private const val FLOOR_TAG_PREFIX = "floor:"
private const val FLOOR_DEFAULT_TAG = "${FLOOR_TAG_PREFIX}0"
private const val FLOOR_LEGACY_LOWER_TAG = "${FLOOR_TAG_PREFIX}lower"
private const val FLOOR_LEGACY_UPPER_TAG = "${FLOOR_TAG_PREFIX}upper"
internal fun BlueprintDocument.scopedToTakeoffScope(scope: TakeoffScope): BlueprintDocument {
    val hasExplicitTradeScopes = hasExplicitTradeScopeTags()
    val wallsById = walls.associateBy { it.id }
    val scopedWalls = walls.filter { wall ->
        wall.matchesTakeoffScope(scope = scope, hasExplicitTradeScopes = hasExplicitTradeScopes)
    }
    val scopedWallIds = scopedWalls.mapTo(mutableSetOf()) { it.id }
    val scopedRooms = detectRoomsByFloorAndScope(
        walls = scopedWalls,
        existingRooms = rooms.filter { room ->
            room.matchesTakeoffScope(
                scope = scope,
                wallsById = wallsById,
                hasExplicitTradeScopes = hasExplicitTradeScopes
            ) && (
                room.hasExplicitTradeScopeFor(scope) ||
                    room.referencesOnlyWalls(scopedWallIds)
                )
        },
        preferredScope = scope
    )
    val scopedOpenings = openings.filter { opening ->
        opening.wallId in scopedWallIds &&
            opening.matchesTakeoffScope(
                scope = scope,
                wallsById = wallsById,
                hasExplicitTradeScopes = hasExplicitTradeScopes
            )
    }
    return copy(
        walls = scopedWalls,
        rooms = scopedRooms,
        openings = scopedOpenings
    )
}

internal fun BlueprintDocument.assignUnscopedGeometryTo(scope: TakeoffScope): BlueprintDocument {
    val originalWallsById = walls.associateBy { it.id }
    val updatedWalls = walls.map { wall ->
        if (wall.takeoffScopeOrNull() != null) {
            wall
        } else {
            wall.copy(tags = wall.tags.withTradeScope(scope))
        }
    }
    val updatedWallsById = updatedWalls.associateBy { it.id }
    val updatedRooms = rooms.map { room ->
        val resolvedScope = room.takeoffScopeOrNull(updatedWallsById)
            ?: room.takeoffScopeOrNull(originalWallsById)
            ?: scope
        room.copy(tags = room.tags.withTradeScope(resolvedScope))
    }
    val updatedOpenings = openings.map { opening ->
        val resolvedScope = opening.takeoffScopeOrNull(updatedWallsById)
            ?: opening.takeoffScopeOrNull(originalWallsById)
            ?: scope
        opening.copy(tags = opening.tags.withTradeScope(resolvedScope))
    }
    return copy(
        walls = updatedWalls,
        rooms = updatedRooms,
        openings = updatedOpenings
    )
}

internal fun detectRoomsByFloorAndScope(
    walls: List<WallSegment>,
    existingRooms: List<Room>,
    preferredScope: TakeoffScope? = null
): List<Room> {
    val wallsById = walls.associateBy { it.id }
    if (walls.isEmpty()) {
        return existingRooms
            .filterNot(Room::isAutoDetectedRoom)
            .map { room ->
                room.copy(
                    tags = room.tags
                        .withNormalizedFloorTag(room.normalizedFloorTag())
                        .withTradeScope(room.takeoffScopeOrNull(wallsById))
                )
            }
    }

    val existingByFloor = existingRooms.groupBy(Room::normalizedFloorTag)
    val wallsByFloor = walls.groupBy(WallSegment::normalizedFloorTag)
    val allFloors = (existingByFloor.keys + wallsByFloor.keys)
        .distinct()
        .sortedBy(::floorSortValue)

    return allFloors.flatMap { floorTag ->
        val floorWalls = wallsByFloor[floorTag].orEmpty()
        val floorWallsById = floorWalls.associateBy { it.id }
        val floorExisting = existingByFloor[floorTag].orEmpty()
        val preservedManualRooms = floorExisting
            .filterNot(Room::isAutoDetectedRoom)
            .map { room ->
                room.copy(
                    tags = room.tags
                        .withNormalizedFloorTag(floorTag)
                        .withTradeScope(
                            room.tags.takeoffScopeOrNull()
                                ?: room.takeoffScopeOrNull(wallsById)
                        )
                )
            }
        val preservedTaggedAutoRooms = floorExisting
            .filter { room ->
                room.isAutoDetectedRoom() && room.tags.hasExplicitTradeScopeTag()
            }

        if (floorWalls.isEmpty()) {
            return@flatMap preservedManualRooms + preservedTaggedAutoRooms
        }

        val detected = RoomLoopDetector.detectRooms(floorWalls)
        if (detected.isEmpty()) {
            return@flatMap preservedManualRooms + preservedTaggedAutoRooms
        }

        val detectedWithMetadata = mergeRoomMetadata(
            existing = floorExisting,
            detected = detected
        ).map { detectedRoom ->
            val existingMatch = floorExisting.firstOrNull { known ->
                known.isAutoDetectedRoom() && roomSignature(known) == roomSignature(detectedRoom)
            }
            val resolvedScope = existingMatch?.tags?.takeoffScopeOrNull()
                ?: existingMatch?.takeoffScopeOrNull(floorWallsById)
                ?: detectedRoom.preferredTakeoffScope(
                    wallsById = floorWallsById,
                    preferredScope = preferredScope
                )
            detectedRoom.copy(
                tags = (existingMatch?.tags ?: detectedRoom.tags)
                    .withNormalizedFloorTag(floorTag)
                    .withTradeScope(resolvedScope),
                ceiling = existingMatch?.ceiling ?: detectedRoom.ceiling,
                overrides = existingMatch?.overrides ?: detectedRoom.overrides
            )
        }
        val detectedSignatures = detectedWithMetadata.mapTo(mutableSetOf(), ::roomSignature)
        val preservedExplicitAutoRooms = preservedTaggedAutoRooms
            .filterNot { room -> roomSignature(room) in detectedSignatures }
            .map { room ->
                room.copy(
                    tags = room.tags
                        .withNormalizedFloorTag(floorTag)
                        .withTradeScope(room.tags.takeoffScopeOrNull())
                )
            }
        preservedManualRooms + preservedExplicitAutoRooms + detectedWithMetadata
    }
}

internal fun TakeoffScope.tradeScopeTag(): String = when (this) {
    TakeoffScope.DRYWALL -> "${TRADE_SCOPE_TAG_PREFIX}drywall"
    TakeoffScope.CONCRETE -> "${TRADE_SCOPE_TAG_PREFIX}concrete"
    TakeoffScope.GRAVEL_MULCH -> "${TRADE_SCOPE_TAG_PREFIX}gravel_mulch"
    TakeoffScope.PAINT -> "${TRADE_SCOPE_TAG_PREFIX}paint"
}

internal fun Set<String>.takeoffScopeOrNull(): TakeoffScope? = when {
    contains(TakeoffScope.DRYWALL.tradeScopeTag()) -> TakeoffScope.DRYWALL
    contains(TakeoffScope.CONCRETE.tradeScopeTag()) -> TakeoffScope.CONCRETE
    contains(TakeoffScope.GRAVEL_MULCH.tradeScopeTag()) -> TakeoffScope.GRAVEL_MULCH
    contains(TakeoffScope.PAINT.tradeScopeTag()) -> TakeoffScope.PAINT
    contains("drywall") -> TakeoffScope.DRYWALL
    contains("concrete") || contains("slab") -> TakeoffScope.CONCRETE
    contains("gravel") || contains("mulch") || contains("bed") -> TakeoffScope.GRAVEL_MULCH
    contains("paint") -> TakeoffScope.PAINT
    else -> null
}

internal fun WallSegment.takeoffScopeOrNull(): TakeoffScope? = tags.takeoffScopeOrNull()

internal fun Room.takeoffScopeOrNull(wallsById: Map<String, WallSegment>): TakeoffScope? {
    tags.takeoffScopeOrNull()?.let { return it }
    val referencedScopes = (wallSegmentIds + wallLoopRef)
        .asSequence()
        .distinct()
        .mapNotNull { wallId -> wallsById[wallId]?.takeoffScopeOrNull() }
        .distinct()
        .toList()
    return referencedScopes.singleOrNull()
}

internal fun BlueprintOpening.takeoffScopeOrNull(wallsById: Map<String, WallSegment>): TakeoffScope? {
    return tags.takeoffScopeOrNull() ?: wallsById[wallId]?.takeoffScopeOrNull()
}

private fun BlueprintDocument.hasExplicitTradeScopeTags(): Boolean {
    return walls.any { wall -> wall.tags.hasExplicitTradeScopeTag() } ||
        rooms.any { room -> room.tags.hasExplicitTradeScopeTag() } ||
        openings.any { opening -> opening.tags.hasExplicitTradeScopeTag() }
}

private fun Set<String>.hasExplicitTradeScopeTag(): Boolean {
    return any { tag -> tag.startsWith(TRADE_SCOPE_TAG_PREFIX) }
}

private fun WallSegment.matchesTakeoffScope(
    scope: TakeoffScope,
    hasExplicitTradeScopes: Boolean
): Boolean {
    return takeoffScopeOrNull().matchesTakeoffScope(scope, hasExplicitTradeScopes)
}

private fun Room.matchesTakeoffScope(
    scope: TakeoffScope,
    wallsById: Map<String, WallSegment>,
    hasExplicitTradeScopes: Boolean
): Boolean {
    return takeoffScopeOrNull(wallsById).matchesTakeoffScope(scope, hasExplicitTradeScopes)
}

private fun BlueprintOpening.matchesTakeoffScope(
    scope: TakeoffScope,
    wallsById: Map<String, WallSegment>,
    hasExplicitTradeScopes: Boolean
): Boolean {
    return takeoffScopeOrNull(wallsById).matchesTakeoffScope(scope, hasExplicitTradeScopes)
}

private fun TakeoffScope?.matchesTakeoffScope(
    scope: TakeoffScope,
    hasExplicitTradeScopes: Boolean
): Boolean {
    return when (this) {
        scope -> true
        null -> !hasExplicitTradeScopes
        else -> false
    }
}

private fun Room.referencesOnlyWalls(visibleWallIds: Set<String>): Boolean {
    val references = (wallSegmentIds + wallLoopRef).toSet()
    return references.isEmpty() || references.all(visibleWallIds::contains)
}

private fun Room.hasExplicitTradeScopeFor(scope: TakeoffScope): Boolean {
    return tags.hasExplicitTradeScopeTag() && tags.takeoffScopeOrNull() == scope
}

private fun Room.preferredTakeoffScope(
    wallsById: Map<String, WallSegment>,
    preferredScope: TakeoffScope?
): TakeoffScope? {
    val referencedScopes = referencedWallScopes(wallsById)
    return when {
        referencedScopes.size == 1 -> referencedScopes.single()
        preferredScope == null -> null
        referencedScopes.isEmpty() -> preferredScope
        preferredScope in referencedScopes -> preferredScope
        else -> null
    }
}

private fun Room.referencedWallScopes(
    wallsById: Map<String, WallSegment>
): Set<TakeoffScope> {
    return (wallSegmentIds + wallLoopRef)
        .asSequence()
        .distinct()
        .mapNotNull { wallId -> wallsById[wallId]?.takeoffScopeOrNull() }
        .toSet()
}

private fun mergeRoomMetadata(
    existing: List<Room>,
    detected: List<Room>
): List<Room> {
    if (detected.isEmpty()) return existing
    val namesBySignature = existing.associateBy(
        keySelector = ::roomSignature,
        valueTransform = Room::name
    )
    return detected.mapIndexed { index, room ->
        val signature = roomSignature(room)
        val existingName = namesBySignature[signature]
        room.copy(name = existingName ?: "Room ${index + 1}")
    }
}

private fun roomSignature(room: Room): String {
    val sorted = room.polygon.sortedWith(
        compareBy<com.tradesketch.estimator.domain.model.PointMm> { it.x }
            .thenBy { it.y }
    )
    return sorted.joinToString(separator = "|") { "${it.x}:${it.y}" }
}

private fun Room.isAutoDetectedRoom(): Boolean {
    return wallSegmentIds.isNotEmpty() ||
        wallLoopRef.isNotEmpty() ||
        id.startsWith("room-auto-")
}

private fun WallSegment.normalizedFloorTag(): String {
    return tags.normalizedFloorTag()
}

private fun Room.normalizedFloorTag(): String {
    return tags.normalizedFloorTag()
}

private fun Set<String>.normalizedFloorTag(): String {
    return canonicalFloorTag(firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) })
}

private fun Set<String>.withNormalizedFloorTag(floorTag: String): Set<String> {
    return filterNot { tag -> tag.startsWith(FLOOR_TAG_PREFIX) }
        .toSet() + canonicalFloorTag(floorTag)
}

internal fun Set<String>.withTradeScope(scope: TakeoffScope?): Set<String> {
    val base = filterNot { tag -> tag.startsWith(TRADE_SCOPE_TAG_PREFIX) }.toSet()
    return if (scope == null) base else base + scope.tradeScopeTag()
}

private fun canonicalFloorTag(rawTag: String?): String {
    val normalized = rawTag?.trim() ?: return FLOOR_DEFAULT_TAG
    if (!normalized.startsWith(FLOOR_TAG_PREFIX)) return FLOOR_DEFAULT_TAG
    if (normalized.equals(FLOOR_LEGACY_LOWER_TAG, ignoreCase = true)) return FLOOR_DEFAULT_TAG
    if (normalized.equals(FLOOR_LEGACY_UPPER_TAG, ignoreCase = true)) return "${FLOOR_TAG_PREFIX}1"
    val numeric = normalized.removePrefix(FLOOR_TAG_PREFIX).toIntOrNull() ?: return FLOOR_DEFAULT_TAG
    return "${FLOOR_TAG_PREFIX}$numeric"
}

private fun floorSortValue(floorTag: String): Int {
    return canonicalFloorTag(floorTag).removePrefix(FLOOR_TAG_PREFIX).toIntOrNull() ?: 0
}
