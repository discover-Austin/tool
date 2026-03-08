package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueprintMathTest {
    @Test
    fun `snap math computes distance and closure threshold`() {
        val start = PointMm(0, 0)
        val candidate = PointMm(3000, 4000)
        assertEquals(5000L, BlueprintSnapMath.distanceMillimeters(start, candidate))

        val closePoint = PointMm(990, 1005)
        val closure = BlueprintSnapMath.roomClosureSnap(
            candidateEnd = closePoint,
            roomStart = PointMm(1000, 1000),
            thresholdMm = 20L
        )
        assertNotNull(closure)
        assertEquals(PointMm(1000, 1000), closure)
    }

    @Test
    fun `snap math projects point to wall axis`() {
        val wall = WallSegment(
            id = "w1",
            start = PointMm(0, 0),
            end = PointMm(4000, 0)
        )
        val t = BlueprintSnapMath.projectToWallT(PointMm(1000, 600), wall)
        assertTrue(t in 0.24..0.26)
        val distance = BlueprintSnapMath.pointToWallDistanceMm(PointMm(1000, 600), wall)
        assertEquals(600L, distance)
    }

    @Test
    fun `room loop detector identifies closed rectangle`() {
        val walls = listOf(
            WallSegment("w1", PointMm(0, 0), PointMm(4000, 0)),
            WallSegment("w2", PointMm(4000, 0), PointMm(4000, 3000)),
            WallSegment("w3", PointMm(4000, 3000), PointMm(0, 3000)),
            WallSegment("w4", PointMm(0, 3000), PointMm(0, 0))
        )

        val rooms = RoomLoopDetector.detectRooms(
            walls = walls,
            snapThresholdMm = 5L
        )

        assertEquals(1, rooms.size)
        assertEquals(12_000_000L, rooms.first().areaSquareMillimeters())
        assertEquals(14_000L, rooms.first().perimeterMillimeters())
    }

    @Test
    fun `room area perimeter and wall surface are deterministic in mm`() {
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
        assertTrue(room.wallSurfaceAreaSqFt(openingAreaSqFt = 12.0) > 0.0)
    }

    @Test
    fun `grid snapping uses finer sub steps to reduce endpoint gaps`() {
        val settings = BlueprintSnapSettings(
            gridEnabled = true,
            gridStepFeet = 1.0,
            endpointEnabled = false,
            midpointEnabled = false,
            angleEnabled = false,
            closureEnabled = false
        )
        val oneInchMm = Millimeters.fromInches(1.0).value
        val rawPoint = PointMm(
            x = Millimeters.fromInches(3.0).value,
            y = Millimeters.fromInches(7.0).value
        )
        val snapped = BlueprintSnapMath.applySnapping(
            rawPoint = rawPoint,
            drawingStart = null,
            settings = settings,
            walls = emptyList()
        )

        val expectedX = ((rawPoint.x.toDouble() / oneInchMm.toDouble()).roundToLong()) * oneInchMm
        val expectedY = ((rawPoint.y.toDouble() / oneInchMm.toDouble()).roundToLong()) * oneInchMm
        assertEquals(expectedX, snapped.x)
        assertEquals(expectedY, snapped.y)
    }
}
