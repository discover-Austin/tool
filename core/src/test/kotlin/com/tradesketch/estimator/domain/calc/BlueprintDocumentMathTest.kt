package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.CeilingSpec
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun applySnapping_anglePriority_prefersNinetyThenFortyFiveFamilies() {
        val settings = BlueprintSnapSettings(
            gridEnabled = false,
            endpointEnabled = false,
            midpointEnabled = false,
            angleEnabled = true,
            angleIncrementDegrees = 15,
            thresholdFeet = 2.0
        )
        val start = PointMm(0, 0)

        val nearNinety = BlueprintSnapMath.applySnapping(
            rawPoint = PointMm(460, 3960),
            drawingStart = start,
            settings = settings,
            walls = emptyList()
        )
        val ninetyAngle = Math.toDegrees(atan2(nearNinety.y.toDouble(), nearNinety.x.toDouble()))
        assertTrue(abs(90.0 - ninetyAngle) <= 0.5, "Expected strongest preference to snap near 90°.")

        val nearThirty = BlueprintSnapMath.applySnapping(
            rawPoint = PointMm(1030, 620),
            drawingStart = start,
            settings = settings,
            walls = emptyList()
        )
        val fortyFiveAngle = Math.toDegrees(atan2(nearThirty.y.toDouble(), nearThirty.x.toDouble()))
        assertTrue(abs(45.0 - fortyFiveAngle) <= 0.6, "Expected next preference to snap near 45°.")
    }

    @Test
    fun applySnapping_prefersPerpendicularFromConnectedWall() {
        val baseWall = WallSegment(
            id = "base",
            start = PointMm(0, 0),
            end = PointMm(4000, 0)
        )
        val settings = BlueprintSnapSettings(
            gridEnabled = false,
            endpointEnabled = false,
            midpointEnabled = false,
            angleEnabled = false,
            thresholdFeet = 0.75
        )

        val snapped = BlueprintSnapMath.applySnapping(
            rawPoint = PointMm(4300, 1800),
            drawingStart = PointMm(4000, 0),
            settings = settings,
            walls = listOf(baseWall)
        )

        // Near-perpendicular draw from wall end should bias to true 90deg.
        assertTrue(kotlin.math.abs(snapped.x - 4000L) <= 2L)
        assertTrue(snapped.y > 0L)
    }

    @Test
    fun applySnapping_softlyFavoursWholeFeetLengths() {
        val settings = BlueprintSnapSettings(
            gridEnabled = false,
            endpointEnabled = false,
            midpointEnabled = false,
            angleEnabled = false,
            thresholdFeet = 0.75
        )
        val start = PointMm(0, 0)
        val nearTenFeet = PointMm(Millimeters.fromFeet(10.0).value + 28L, 0L)

        val snapped = BlueprintSnapMath.applySnapping(
            rawPoint = nearTenFeet,
            drawingStart = start,
            settings = settings,
            walls = emptyList()
        )

        val rawDelta = kotlin.math.abs(nearTenFeet.x - Millimeters.fromFeet(10.0).value)
        val snappedDelta = kotlin.math.abs(snapped.x - Millimeters.fromFeet(10.0).value)
        assertTrue(snappedDelta < rawDelta)
    }

    @Test
    fun applySnapping_softlyFavoursParallelLengths() {
        val reference = WallSegment(
            id = "ref",
            start = PointMm(0, 0),
            end = PointMm(5000, 0)
        )
        val settings = BlueprintSnapSettings(
            gridEnabled = false,
            endpointEnabled = false,
            midpointEnabled = false,
            angleEnabled = false,
            thresholdFeet = 0.75
        )
        val start = PointMm(0, 2000)
        val raw = PointMm(4850, 2050)

        val snapped = BlueprintSnapMath.applySnapping(
            rawPoint = raw,
            drawingStart = start,
            settings = settings,
            walls = listOf(reference)
        )

        val rawLength = BlueprintSnapMath.distanceMillimeters(start, raw)
        val snappedLength = BlueprintSnapMath.distanceMillimeters(start, snapped)
        assertTrue(kotlin.math.abs(5000L - snappedLength) < kotlin.math.abs(5000L - rawLength))
    }

    @Test
    fun roomClosureSnap_canCloseToNearbyDanglingEndpoint() {
        val walls = listOf(
            WallSegment("w1", PointMm(0, 0), PointMm(4000, 0)),
            WallSegment("w2", PointMm(4000, 0), PointMm(4000, 3000))
        )
        val closure = BlueprintSnapMath.roomClosureSnap(
            candidateEnd = PointMm(40, 35),
            roomStart = PointMm(999_999, 999_999),
            thresholdMm = 60L,
            walls = walls
        )

        assertNotNull(closure)
        assertTrue(
            closure == PointMm(0, 0) || closure == PointMm(4000, 3000),
            "Expected closure to snap to one dangling endpoint."
        )
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
    fun detectRooms_handlesRectangleWithBranchingWall() {
        val walls = listOf(
            WallSegment("w1", PointMm(0, 0), PointMm(4000, 0)),
            WallSegment("w2", PointMm(4000, 0), PointMm(4000, 3000)),
            WallSegment("w3", PointMm(4000, 3000), PointMm(0, 3000)),
            WallSegment("w4", PointMm(0, 3000), PointMm(0, 0)),
            WallSegment("branch", PointMm(4000, 1500), PointMm(5200, 1500))
        )

        val rooms = RoomLoopDetector.detectRooms(walls)

        assertEquals(1, rooms.size)
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

    @Test
    fun drywallCeilingTakeoff_skipsRoomsWithDisabledCeilings() {
        val side = Millimeters.fromFeet(10.0).value
        val enabledRoom = Room(
            id = "room-enabled",
            name = "Room Enabled",
            polygon = listOf(
                PointMm(0, 0),
                PointMm(side, 0),
                PointMm(side, side),
                PointMm(0, side)
            ),
            ceiling = CeilingSpec(enabled = true)
        )
        val disabledRoom = Room(
            id = "room-disabled",
            name = "Room Disabled",
            polygon = listOf(
                PointMm(side + 1_000, 0),
                PointMm((side * 2) + 1_000, 0),
                PointMm((side * 2) + 1_000, side),
                PointMm(side + 1_000, side)
            ),
            ceiling = CeilingSpec(enabled = false)
        )
        val document = BlueprintDocument(
            projectId = "p1",
            walls = emptyList(),
            rooms = listOf(enabledRoom, disabledRoom)
        )

        val drywall = BlueprintTakeoffCalculator.drywallTakeoff(
            document = document,
            sheetAreaSqFt = 100.0,
            wastePercent = 0.0,
            screwsPerSheet = 0,
            mudGallonsPer100SqFt = 0.0,
            includeCeilings = true
        )

        val ceilingTraces = drywall.traces.filter { it.metric == "ceiling_area" }
        val adjustedArea = drywall.traces.first { it.metric == "drywall_adjusted_area" }.value

        assertEquals(1, ceilingTraces.size)
        assertEquals(100.0, adjustedArea, 0.2)
    }

    @Test
    fun concreteTakeoff_detectsClosedWallLoopWhenRoomsMissing() {
        val side = Millimeters.fromFeet(10.0).value
        val document = BlueprintDocument(
            projectId = "p1",
            walls = listOf(
                WallSegment("w1", PointMm(0, 0), PointMm(side, 0)),
                WallSegment("w2", PointMm(side, 0), PointMm(side, side)),
                WallSegment("w3", PointMm(side, side), PointMm(0, side)),
                WallSegment("w4", PointMm(0, side), PointMm(0, 0))
            )
        )

        val concrete = BlueprintTakeoffCalculator.concreteTakeoff(
            document = document,
            thicknessFeet = 0.54,
            wastePercent = 0.0
        )

        assertEquals(2.0, concrete.items.first().quantity, 0.05)
        assertTrue(concrete.traces.any { it.metric == "concrete_area" })
    }

    @Test
    fun drywallCeilingTakeoff_detectsClosedWallLoopWhenRoomsMissing() {
        val side = Millimeters.fromFeet(10.0).value
        val wallHeight = Millimeters.fromFeet(9.0)
        val document = BlueprintDocument(
            projectId = "p1",
            walls = listOf(
                WallSegment("w1", PointMm(0, 0), PointMm(side, 0), height = wallHeight),
                WallSegment("w2", PointMm(side, 0), PointMm(side, side), height = wallHeight),
                WallSegment("w3", PointMm(side, side), PointMm(0, side), height = wallHeight),
                WallSegment("w4", PointMm(0, side), PointMm(0, 0), height = wallHeight)
            )
        )

        val drywall = BlueprintTakeoffCalculator.drywallTakeoff(
            document = document,
            sheetAreaSqFt = 100.0,
            wastePercent = 0.0,
            screwsPerSheet = 0,
            mudGallonsPer100SqFt = 0.0,
            includeCeilings = true
        )

        val ceilingTrace = drywall.traces.firstOrNull { it.metric == "ceiling_area" }

        assertNotNull(ceilingTrace)
        assertEquals(100.0, ceilingTrace.value, 0.2)
    }

    @Test
    fun gravelTargetRooms_detectsClosedWallLoopWhenRoomsMissing() {
        val side = Millimeters.fromFeet(10.0).value
        val document = BlueprintDocument(
            projectId = "p1",
            walls = listOf(
                WallSegment("w1", PointMm(0, 0), PointMm(side, 0)),
                WallSegment("w2", PointMm(side, 0), PointMm(side, side)),
                WallSegment("w3", PointMm(side, side), PointMm(0, side)),
                WallSegment("w4", PointMm(0, side), PointMm(0, 0))
            )
        )

        val targetRooms = BlueprintTakeoffCalculator.gravelTargetRooms(document)

        assertEquals(1, targetRooms.size)
        assertEquals(100.0, targetRooms.first().areaSqFt(), 0.2)
    }

    @Test
    fun openingAreaByWallId_ignoresStairOpenings() {
        val wall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(Millimeters.fromFeet(10.0).value, 0),
            height = Millimeters.fromFeet(9.0)
        )
        val door = BlueprintOpening(
            id = "door-1",
            wallId = wall.id,
            t = 0.35,
            widthMm = Millimeters.fromFeet(3.0).value,
            heightMm = Millimeters.fromFeet(7.0).value,
            sillMm = 0L,
            type = OpeningType.DOOR
        )
        val stair = BlueprintOpening(
            id = "stair-1",
            wallId = wall.id,
            t = 0.7,
            widthMm = Millimeters.fromFeet(4.0).value,
            heightMm = Millimeters.fromFeet(10.0).value,
            sillMm = 0L,
            type = OpeningType.STAIR_UP
        )
        val document = BlueprintDocument(
            projectId = "p1",
            walls = listOf(wall),
            openings = listOf(door, stair)
        )

        val openingArea = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(document)

        assertEquals(21.0, openingArea[wall.id] ?: 0.0, 0.05)
    }

    @Test
    fun gravelTargetRooms_prefersTaggedCoverageRooms() {
        val taggedRoom = Room(
            id = "room-tagged",
            tags = setOf("gravel"),
            polygon = listOf(
                PointMm(0, 0),
                PointMm(1000, 0),
                PointMm(1000, 1000),
                PointMm(0, 1000)
            )
        )
        val untaggedRoom = Room(
            id = "room-untagged",
            polygon = listOf(
                PointMm(1500, 0),
                PointMm(2500, 0),
                PointMm(2500, 1000),
                PointMm(1500, 1000)
            )
        )
        val document = BlueprintDocument(
            projectId = "p1",
            rooms = listOf(taggedRoom, untaggedRoom)
        )

        val targetRooms = BlueprintTakeoffCalculator.gravelTargetRooms(document)

        assertEquals(1, targetRooms.size)
        assertEquals("room-tagged", targetRooms.first().id)
    }
}

