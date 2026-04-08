package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintLayerVisibilityTest {

    @Test
    fun filteredToFloor_keepsGeometryFromMultipleTradesOnSameCanvasFloor() {
        val groundDrywallWalls = rectangleWalls(
            prefix = "dry",
            scope = TakeoffScope.DRYWALL,
            origin = PointMm(0, 0),
            floor = FLOOR_GROUND_LEVEL
        )
        val groundConcreteWalls = rectangleWalls(
            prefix = "conc",
            scope = TakeoffScope.CONCRETE,
            origin = PointMm(Millimeters.fromFeet(20.0).value, 0),
            floor = FLOOR_GROUND_LEVEL
        )
        val upperPaintWalls = rectangleWalls(
            prefix = "paint",
            scope = TakeoffScope.PAINT,
            origin = PointMm(0, Millimeters.fromFeet(20.0).value),
            floor = 1
        )
        val document = BlueprintDocument(
            projectId = "project-floor-1",
            walls = groundDrywallWalls + groundConcreteWalls + upperPaintWalls,
            rooms = listOf(
                roomForWalls("dry-room", groundDrywallWalls),
                roomForWalls("conc-room", groundConcreteWalls),
                roomForWalls("paint-room", upperPaintWalls)
            ),
            openings = listOf(
                BlueprintOpening(
                    id = "conc-door",
                    wallId = groundConcreteWalls.first().id,
                    t = 0.5,
                    widthMm = Millimeters.fromFeet(3.0).value,
                    heightMm = Millimeters.fromFeet(7.0).value,
                    sillMm = 0L,
                    tags = setOf(FLOOR_GROUND_LEVEL.floorTag())
                ),
                BlueprintOpening(
                    id = "paint-door",
                    wallId = upperPaintWalls.first().id,
                    t = 0.5,
                    widthMm = Millimeters.fromFeet(3.0).value,
                    heightMm = Millimeters.fromFeet(7.0).value,
                    sillMm = 0L,
                    tags = setOf(1.floorTag())
                )
            )
        )

        val groundFloorDocument = document.filteredToFloor(FLOOR_GROUND_LEVEL)

        assertEquals(8, groundFloorDocument.walls.size)
        assertEquals(2, groundFloorDocument.rooms.size)
        assertEquals(1, groundFloorDocument.openings.size)
        assertEquals(
            listOf(TakeoffScope.DRYWALL, TakeoffScope.CONCRETE),
            groundFloorDocument.visibleTradeScopes()
        )
    }

    @Test
    fun visibleTradeScopes_includesRoomOnlyTradeGeometry() {
        val document = BlueprintDocument(
            projectId = "project-room-only",
            rooms = listOf(
                Room(
                    id = "gravel-room",
                    name = "Bed",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(Millimeters.fromFeet(8.0).value, 0),
                        PointMm(Millimeters.fromFeet(8.0).value, Millimeters.fromFeet(6.0).value),
                        PointMm(0, Millimeters.fromFeet(6.0).value)
                    ),
                    tags = setOf(
                        TakeoffScope.GRAVEL_MULCH.wallScopeTag(),
                        FLOOR_GROUND_LEVEL.floorTag()
                    )
                )
            )
        )

        val visibleScopes = document.filteredToFloor(FLOOR_GROUND_LEVEL).visibleTradeScopes()

        assertEquals(listOf(TakeoffScope.GRAVEL_MULCH), visibleScopes)
    }

    private fun rectangleWalls(
        prefix: String,
        scope: TakeoffScope,
        origin: PointMm,
        floor: BlueprintFloorLevel
    ): List<WallSegment> {
        val width = Millimeters.fromFeet(10.0).value
        val height = Millimeters.fromFeet(10.0).value
        val tags = setOf(scope.wallScopeTag(), floor.floorTag())
        val p1 = origin
        val p2 = PointMm(origin.x + width, origin.y)
        val p3 = PointMm(origin.x + width, origin.y + height)
        val p4 = PointMm(origin.x, origin.y + height)
        return listOf(
            WallSegment(id = "${prefix}-1", start = p1, end = p2, tags = tags),
            WallSegment(id = "${prefix}-2", start = p2, end = p3, tags = tags),
            WallSegment(id = "${prefix}-3", start = p3, end = p4, tags = tags),
            WallSegment(id = "${prefix}-4", start = p4, end = p1, tags = tags)
        )
    }

    private fun roomForWalls(id: String, walls: List<WallSegment>): Room {
        return Room(
            id = id,
            name = id,
            polygon = listOf(walls[0].start, walls[0].end, walls[1].end, walls[2].end),
            wallSegmentIds = walls.map { wall -> wall.id },
            tags = setOf(walls.first().tags.resolveFloorLevelOrDefault().floorTag())
        )
    }
}
