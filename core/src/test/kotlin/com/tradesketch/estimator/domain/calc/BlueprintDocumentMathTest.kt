package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintDocumentMathTest {

    @Test
    fun applySnapping_snapsToEndpointAndAngle() {
        val wall = WallSegment(
            id = "w1",
            start = PointMm(0, 0),
            end = PointMm(5000, 0)
        )
        val settings = BlueprintSnapSettings(
            gridEnabled = false,
            endpointEnabled = true,
            midpointEnabled = false,
            angleEnabled = true,
            angleIncrementDegrees = 90,
            thresholdFeet = 0.5
        )

        val snappedEndpoint = BlueprintSnapMath.applySnapping(
            rawPoint = PointMm(30, -20),
            drawingStart = null,
            settings = settings,
            walls = listOf(wall)
        )
        assertEquals(PointMm(0, 0), snappedEndpoint)

        val snappedAngle = BlueprintSnapMath.applySnapping(
            rawPoint = PointMm(3200, 300),
            drawingStart = PointMm(0, 0),
            settings = settings,
            walls = emptyList()
        )
        assertEquals(0L, snappedAngle.y)
        assertTrue(snappedAngle.x > 0L)
    }

    @Test
    fun detectRooms_findsClosedRectangleLoop() {
        val walls = listOf(
            WallSegment("w1", PointMm(0, 0), PointMm(4000, 0)),
            WallSegment("w2", PointMm(4000, 0), PointMm(4000, 3000)),
            WallSegment("w3", PointMm(4000, 3000), PointMm(0, 3000)),
            WallSegment("w4", PointMm(0, 3000), PointMm(0, 0))
        )

        val rooms = RoomLoopDetector.detectRooms(walls)

        assertEquals(1, rooms.size)
        assertEquals(4, rooms.first().polygon.size)
        assertEquals(12_000_000L, rooms.first().areaSquareMillimeters())
    }

    @Test
    fun roomAreaAndPerimeter_areDeterministicMillimeterMath() {
        val room = Room(
            id = "room-1",
            name = "Room 1",
            polygon = listOf(
                PointMm(0, 0),
                PointMm(5000, 0),
                PointMm(5000, 3000),
                PointMm(0, 3000)
            )
        )

        assertEquals(15_000_000L, room.areaSquareMillimeters())
        assertEquals(16_000L, room.perimeterMillimeters())
    }

    @Test
    fun openingsSubtractFromPaintAndDrywallQuantities() {
        val wall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(Millimeters.fromFeet(10.0).value, 0),
            height = Millimeters.fromFeet(9.0)
        )
        val door = BlueprintOpening(
            id = "door-1",
            wallId = wall.id,
            t = 0.5,
            widthMm = Millimeters.fromFeet(3.0).value,
            heightMm = Millimeters.fromFeet(7.0).value,
            sillMm = 0L,
            type = OpeningType.DOOR
        )
        val document = BlueprintDocument(
            projectId = "p1",
            params = BlueprintParams(),
            walls = listOf(wall),
            openings = listOf(door)
        )

        val paint = BlueprintTakeoffCalculator.paintTakeoff(
            document = document,
            coverageSqFtPerGallon = 350.0,
            coats = 1,
            wastePercent = 0.0
        )

        val netPaintableTrace = paint.traces.first { it.metric == "paintable_wall_area" }
        // 10x9 wall = 90 sqft, opening 3x7 = 21 sqft
        assertEquals(69.0, netPaintableTrace.value, 0.05)
    }
}
