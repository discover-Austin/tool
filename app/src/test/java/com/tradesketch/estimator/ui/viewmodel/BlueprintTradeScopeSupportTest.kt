package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintTradeScopeSupportTest {

    @Test
    fun detectRoomsByFloorAndScope_keepsRoomsSeparatedByTrade() {
        val concreteWalls = rectangleWalls(
            prefix = "conc",
            scope = TakeoffScope.CONCRETE,
            origin = PointMm(0, 0)
        )
        val drywallWalls = rectangleWalls(
            prefix = "dry",
            scope = TakeoffScope.DRYWALL,
            origin = PointMm(Millimeters.fromFeet(20.0).value, 0)
        )

        val rooms = detectRoomsByFloorAndScope(
            walls = concreteWalls + drywallWalls,
            existingRooms = emptyList()
        )

        assertEquals(2, rooms.size)
        assertEquals(1, rooms.count { it.tags.takeoffScopeOrNull() == TakeoffScope.CONCRETE })
        assertEquals(1, rooms.count { it.tags.takeoffScopeOrNull() == TakeoffScope.DRYWALL })
    }

    @Test
    fun scopedToTakeoffScope_redetectsConcreteRoomsFromScopedWalls() {
        val concreteWalls = rectangleWalls(
            prefix = "conc",
            scope = TakeoffScope.CONCRETE,
            origin = PointMm(0, 0)
        )
        val drywallWalls = rectangleWalls(
            prefix = "dry",
            scope = TakeoffScope.DRYWALL,
            origin = PointMm(Millimeters.fromFeet(20.0).value, 0)
        )
        val document = BlueprintDocument(
            projectId = "project-1",
            walls = concreteWalls + drywallWalls
        )

        val concreteDocument = document.scopedToTakeoffScope(TakeoffScope.CONCRETE)
        val concreteTakeoff = BlueprintTakeoffCalculator.concreteTakeoff(
            document = concreteDocument,
            thicknessFeet = 1.0,
            wastePercent = 0.0
        )

        assertEquals(4, concreteDocument.walls.size)
        assertEquals(1, concreteDocument.rooms.size)
        assertTrue(concreteTakeoff.items.first().quantity > 0.0)
    }

    @Test
    fun assignUnscopedGeometryTo_tagsWallsRoomsAndOpeningsTogether() {
        val walls = rectangleWalls(
            prefix = "legacy",
            scope = null,
            origin = PointMm(0, 0)
        )
        val room = Room(
            id = "legacy-room",
            name = "Legacy Room",
            polygon = listOf(
                PointMm(0, 0),
                PointMm(Millimeters.fromFeet(10.0).value, 0),
                PointMm(Millimeters.fromFeet(10.0).value, Millimeters.fromFeet(10.0).value),
                PointMm(0, Millimeters.fromFeet(10.0).value)
            ),
            wallSegmentIds = walls.map(WallSegment::id)
        )
        val opening = BlueprintOpening(
            id = "legacy-door",
            wallId = walls.first().id,
            t = 0.5,
            widthMm = Millimeters.fromFeet(3.0).value,
            heightMm = Millimeters.fromFeet(7.0).value,
            sillMm = 0L
        )
        val document = BlueprintDocument(
            projectId = "project-2",
            walls = walls,
            rooms = listOf(room),
            openings = listOf(opening)
        )

        val scoped = document.assignUnscopedGeometryTo(TakeoffScope.CONCRETE)

        assertTrue(scoped.walls.all { it.tags.takeoffScopeOrNull() == TakeoffScope.CONCRETE })
        assertTrue(scoped.rooms.all { it.tags.takeoffScopeOrNull() == TakeoffScope.CONCRETE })
        assertTrue(scoped.openings.all { it.tags.takeoffScopeOrNull() == TakeoffScope.CONCRETE })
    }

    @Test
    fun projectBlueprintForType_returnsOnlyRequestedTradeGeometry() {
        val project = Project(
            id = "project-3",
            name = "Mixed Trades",
            blueprintDocument = BlueprintDocument(
                projectId = "project-3",
                walls = rectangleWalls(
                    prefix = "conc",
                    scope = TakeoffScope.CONCRETE,
                    origin = PointMm(0, 0)
                ) + rectangleWalls(
                    prefix = "paint",
                    scope = TakeoffScope.PAINT,
                    origin = PointMm(Millimeters.fromFeet(20.0).value, 0)
                )
            )
        )

        val concreteBlueprint = projectBlueprintForType(project = project, type = TakeoffType.CONCRETE)
        val paintBlueprint = projectBlueprintForType(project = project, type = TakeoffType.PAINT)

        assertEquals(4, concreteBlueprint.walls.size)
        assertEquals(1, concreteBlueprint.rooms.size)
        assertEquals(4, paintBlueprint.walls.size)
        assertEquals(1, paintBlueprint.rooms.size)
    }

    private fun rectangleWalls(
        prefix: String,
        scope: TakeoffScope?,
        origin: PointMm
    ): List<WallSegment> {
        val width = Millimeters.fromFeet(10.0).value
        val height = Millimeters.fromFeet(10.0).value
        val tags = scope?.let { setOf(it.tradeScopeTag()) } ?: emptySet()
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
}
