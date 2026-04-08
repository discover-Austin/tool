package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.ui.viewmodel.takeoffScopeOrNull

internal val orderedTradeScopes = listOf(
    TakeoffScope.DRYWALL,
    TakeoffScope.CONCRETE,
    TakeoffScope.GRAVEL_MULCH,
    TakeoffScope.PAINT
)

internal fun BlueprintDocument.filteredToFloor(level: BlueprintFloorLevel): BlueprintDocument {
    val allWallsById = walls.associateBy { it.id }
    val floorWalls = walls.filter { wall -> wall.isOnFloor(level) }
    val floorWallIds = floorWalls.mapTo(mutableSetOf()) { wall -> wall.id }
    val floorRooms = rooms.filter { room ->
        room.isOnFloor(level) || room.referencesVisibleWalls(floorWallIds)
    }
    val floorOpenings = openings.filter { opening ->
        opening.wallId in floorWallIds && opening.isOnFloor(level, allWallsById)
    }
    return copy(
        walls = floorWalls,
        rooms = floorRooms,
        openings = floorOpenings
    )
}

internal fun BlueprintDocument.visibleTradeScopes(): List<TakeoffScope> {
    val wallsById = walls.associateBy { it.id }
    return orderedTradeScopes.filter { scope ->
        walls.any { wall -> wall.scopeFromTag() == scope } ||
            rooms.any { room ->
                room.tags.takeoffScopeOrNull() == scope ||
                    room.referencedTradeScopes(wallsById).singleOrNull() == scope
            } ||
            openings.any { opening ->
                opening.tags.takeoffScopeOrNull() == scope ||
                    wallsById[opening.wallId]?.scopeFromTag() == scope
            }
    }
}

private fun com.tradesketch.estimator.domain.model.Room.referencesVisibleWalls(
    visibleWallIds: Set<String>
): Boolean {
    return (wallSegmentIds + wallLoopRef).any { wallId -> wallId in visibleWallIds }
}

private fun com.tradesketch.estimator.domain.model.Room.referencedTradeScopes(
    wallsById: Map<String, com.tradesketch.estimator.domain.model.WallSegment>
): Set<TakeoffScope> {
    return (wallSegmentIds + wallLoopRef)
        .asSequence()
        .distinct()
        .mapNotNull { wallId -> wallsById[wallId]?.scopeFromTag() }
        .toSet()
}
