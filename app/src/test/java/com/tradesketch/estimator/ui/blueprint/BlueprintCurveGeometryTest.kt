package com.tradesketch.estimator.ui.blueprint

import androidx.compose.ui.geometry.Offset
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueprintCurveGeometryTest {

    @Test
    fun `arc preview keeps the tapped anchors and bends off the straight chord`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val control = PointMm(800, 700)

        val previewWalls = buildDraftArcPreviewWalls(
            start = start,
            end = end,
            previewPoint = control
        )

        assertTrue(previewWalls.size >= 2)
        assertEquals(start, previewWalls.first().start)
        assertEquals(end, previewWalls.last().end)
        assertTrue(previewWalls.any { wall -> wall.start.y != 0L || wall.end.y != 0L })
    }

    @Test
    fun `arc dial projection round trips the control point`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val control = PointMm(1_050, 450)

        val projection = projectArcDraftControl(
            start = start,
            end = end,
            control = control
        )

        assertNotNull(projection)
        assertEquals(250.0, projection.shiftMm, 0.6)
        assertEquals(450.0, projection.bendMm, 0.6)
        assertEquals(
            control,
            pointFromArcDraftControl(
                start = start,
                end = end,
                shiftMm = projection.shiftMm,
                bendMm = projection.bendMm
            )
        )
    }

    @Test
    fun `arc draft measurements report span bend and longer than chord arc length`() {
        val measurements = measureArcDraft(
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            control = PointMm(800, 600)
        )

        assertEquals(1_600L, measurements.spanMm)
        assertEquals(600L, measurements.bendMm)
        assertTrue(measurements.arcLengthMm > measurements.spanMm)
        assertEquals(73.7, measurements.turnDegrees, 0.2)
    }

    @Test
    fun `arc turn angle mirrors cleanly across the chord`() {
        val upperTurn = measureArcDraftTurnDegrees(
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            control = PointMm(800, 600)
        )
        val lowerTurn = measureArcDraftTurnDegrees(
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            control = PointMm(800, -600)
        )

        assertEquals(upperTurn, lowerTurn, 0.01)
    }

    @Test
    fun `arc draft measurement tags preserve reproducible curve controls`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val control = PointMm(920, 600)

        val projection = projectArcDraftControl(
            start = start,
            end = end,
            control = control
        )
        val measurements = measureArcDraft(
            start = start,
            end = end,
            control = control
        )
        val stored = buildArcDraftMeasurementTags(
            start = start,
            end = end,
            control = control
        ).curveArcDraftMeasurements()

        assertNotNull(projection)
        assertNotNull(stored)
        assertEquals(projection.shiftMm.roundToLong(), stored.shiftMm)
        assertEquals(projection.bendMm.roundToLong(), stored.bendMm)
        assertEquals(measurements.spanMm, stored.spanMm)
        assertEquals(measurements.arcLengthMm, stored.arcLengthMm)
        assertEquals(measurements.turnDegrees, stored.turnDegrees, 0.1)
    }

    @Test
    fun `committed arc walls collapse into one full-curve length label`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val control = PointMm(800, 600)
        val document = BlueprintDocument.empty("curve-labels")
        val walls = buildDraftArcWalls(
            document = document,
            start = start,
            end = end,
            control = control,
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_ARC_TAG)
        )

        val labels = collectCommittedWallLengthLabels(
            document = document.copy(walls = walls),
            selectedWallId = walls.first().id,
            worldToScreen = { point -> Offset(point.x.toFloat(), point.y.toFloat()) }
        )

        assertEquals(1, labels.size)
        assertEquals(measureArcDraft(start, end, control).arcLengthMm, labels.single().lengthMm)
        assertEquals("Curve", labels.single().prefix)
    }

    @Test
    fun `measured arc reports reproducible chord rise radius and sweep`() {
        val measurements = measureMeasuredArcDraft(
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            riseMm = 400.0
        )

        assertEquals(1_600L, measurements.chordMm)
        assertEquals(400L, measurements.riseMm)
        assertEquals(1_000L, measurements.radiusMm)
        assertTrue(measurements.arcLengthMm > measurements.chordMm)
        assertEquals(106.3, measurements.sweepDegrees, 0.2)
    }

    @Test
    fun `measured arc tags preserve reproducible values`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val riseMm = 400.0

        val measurements = measureMeasuredArcDraft(
            start = start,
            end = end,
            riseMm = riseMm
        )
        val stored = buildMeasuredArcMeasurementTags(
            start = start,
            end = end,
            riseMm = riseMm
        ).measuredArcDraftMeasurements()

        assertNotNull(stored)
        assertEquals(measurements.chordMm, stored.chordMm)
        assertEquals(measurements.riseMm, stored.riseMm)
        assertEquals(measurements.radiusMm, stored.radiusMm)
        assertEquals(measurements.arcLengthMm, stored.arcLengthMm)
        assertEquals(measurements.sweepDegrees, stored.sweepDegrees, 0.1)
    }

    @Test
    fun `curve commit control falls back to third tap when preview is still the default midpoint`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val tap = PointMm(800, 400)

        val measuredArcControl = resolveCurveCommitControlPoint(
            start = start,
            end = end,
            previewPoint = midpointBetween(start, end),
            tap = tap,
            measuredArc = true
        )
        val sketchControl = resolveCurveCommitControlPoint(
            start = start,
            end = end,
            previewPoint = midpointBetween(start, end),
            tap = tap,
            measuredArc = false
        )

        assertEquals(tap, sketchControl)
        assertEquals(tap, measuredArcControl)
    }

    @Test
    fun `curve commit control keeps an adjusted preview instead of replacing it with the confirm tap`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val adjustedPreview = PointMm(800, 520)
        val confirmTap = PointMm(800, 300)

        val measuredArcControl = resolveCurveCommitControlPoint(
            start = start,
            end = end,
            previewPoint = adjustedPreview,
            tap = confirmTap,
            measuredArc = true
        )
        val sketchControl = resolveCurveCommitControlPoint(
            start = start,
            end = end,
            previewPoint = adjustedPreview,
            tap = confirmTap,
            measuredArc = false
        )

        assertEquals(adjustedPreview, sketchControl)
        assertEquals(adjustedPreview, measuredArcControl)
    }

    @Test
    fun `wall and box commit fall back to the tap when preview is still untouched`() {
        val start = PointMm(100, 100)
        val tap = PointMm(700, 300)

        val wallEnd = resolveDraftWallCommitEnd(
            previewEnd = start,
            untouchedPreviewPoint = start,
            snappedTap = tap,
            walls = emptyList(),
            snapThresholdFeet = 1.0,
            endpointSnappingEnabled = false,
            closureEnabled = false,
            chainOrigin = null
        )
        val boxEnd = resolveDraftBoxCommitEnd(
            previewEnd = start,
            untouchedPreviewPoint = start,
            snappedTap = tap,
            walls = emptyList(),
            snapThresholdFeet = 1.0,
            endpointSnappingEnabled = false
        )

        assertEquals(tap, wallEnd)
        assertEquals(tap, boxEnd)
    }

    @Test
    fun `wall and box commit keep an adjusted preview`() {
        val start = PointMm(100, 100)
        val adjustedPreview = PointMm(640, 280)
        val tap = PointMm(700, 300)

        val wallEnd = resolveDraftWallCommitEnd(
            previewEnd = adjustedPreview,
            untouchedPreviewPoint = start,
            snappedTap = tap,
            walls = emptyList(),
            snapThresholdFeet = 1.0,
            endpointSnappingEnabled = false,
            closureEnabled = false,
            chainOrigin = null
        )
        val boxEnd = resolveDraftBoxCommitEnd(
            previewEnd = adjustedPreview,
            untouchedPreviewPoint = start,
            snappedTap = tap,
            walls = emptyList(),
            snapThresholdFeet = 1.0,
            endpointSnappingEnabled = false
        )

        assertEquals(adjustedPreview, wallEnd)
        assertEquals(adjustedPreview, boxEnd)
    }

    @Test
    fun `circle commit falls back to the tap when preview is still at the center`() {
        val center = PointMm(100, 100)
        val tap = PointMm(600, 100)

        assertEquals(
            tap,
            resolveCircleCommitEdge(
                center = center,
                previewEdge = center,
                tap = tap
            )
        )
    }

    @Test
    fun `circle commit keeps an adjusted preview edge`() {
        val center = PointMm(100, 100)
        val adjustedPreview = PointMm(580, 120)
        val tap = PointMm(600, 100)

        assertEquals(
            adjustedPreview,
            resolveCircleCommitEdge(
                center = center,
                previewEdge = adjustedPreview,
                tap = tap
            )
        )
    }

    @Test
    fun `measured arc sweep round trips back into rise`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val measurements = measureMeasuredArcDraft(
            start = start,
            end = end,
            riseMm = 400.0
        )

        val recoveredRiseMm = riseFromMeasuredArcSweepDegrees(
            start = start,
            end = end,
            sweepDegrees = measurements.sweepDegrees,
            riseSign = 1.0
        )

        assertEquals(400.0, recoveredRiseMm, 0.2)
    }

    @Test
    fun `committed measured arcs collapse into one full-arc length label`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val document = BlueprintDocument.empty("measured-arc-labels")
        val walls = buildMeasuredArcWalls(
            document = document,
            start = start,
            end = end,
            riseMm = 400.0,
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_ARC_TAG)
        )

        val labels = collectCommittedWallLengthLabels(
            document = document.copy(walls = walls),
            selectedWallId = walls.first().id,
            worldToScreen = { point -> Offset(point.x.toFloat(), point.y.toFloat()) }
        )

        assertEquals(1, labels.size)
        assertEquals(measureMeasuredArcDraft(start, end, 400.0).arcLengthMm, labels.single().lengthMm)
        assertEquals("Arc", labels.single().prefix)
    }

    @Test
    fun `measured arc selection exposes guide geometry for the live overlay`() {
        val start = PointMm(0, 0)
        val end = PointMm(1_600, 0)
        val document = BlueprintDocument.empty("measured-arc-selection")
        val walls = buildMeasuredArcWalls(
            document = document,
            start = start,
            end = end,
            riseMm = 400.0,
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_ARC_TAG)
        )

        val selection = arcSelectionInfo(
            document = document.copy(walls = walls),
            selectedWallId = walls.first().id
        )

        assertNotNull(selection)
        assertEquals(CurveSelectionKind.MEASURED_ARC, selection.kind)
        assertEquals(PointMm(0, 0), selection.guideChordStart)
        assertEquals(PointMm(1_600, 0), selection.guideChordEnd)
        assertEquals(PointMm(800, 0), selection.guideChordMidpoint)
        assertEquals(PointMm(800, 400), selection.guideArcMidpoint)
        assertEquals(PointMm(800, -600), selection.guideCenter)
    }

    @Test
    fun `committed circles do not emit a segment label for every wall`() {
        val document = BlueprintDocument.empty("circle-labels")
        val walls = buildDraftCircleWalls(
            document = document,
            center = PointMm(0, 0),
            edge = PointMm(1_200, 0),
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_CIRCLE_TAG)
        )

        val labels = collectCommittedWallLengthLabels(
            document = document.copy(walls = walls),
            selectedWallId = null,
            worldToScreen = { point -> Offset(point.x.toFloat(), point.y.toFloat()) }
        )

        assertTrue(labels.isEmpty())
    }

    @Test
    fun `circle preview returns a closed segmented loop`() {
        val center = PointMm(0, 0)
        val edge = PointMm(1_200, 0)

        val previewWalls = buildDraftCirclePreviewWalls(
            center = center,
            edge = edge
        )

        assertTrue(previewWalls.size >= 12)
        assertEquals(0, previewWalls.size % 4)
        assertEquals(PointMm(0, 1_200), previewWalls.first().start)
        assertEquals(previewWalls.first().start, previewWalls.last().end)
    }

    @Test
    fun `circle preview keeps a repeatable orientation regardless of tapped radius direction`() {
        val center = PointMm(0, 0)

        val eastPreview = buildDraftCirclePreviewWalls(
            center = center,
            edge = PointMm(1_200, 0)
        )
        val northPreview = buildDraftCirclePreviewWalls(
            center = center,
            edge = PointMm(0, 1_200)
        )

        assertEquals(eastPreview.size, northPreview.size)
        assertEquals(eastPreview.first().start, northPreview.first().start)
        assertEquals(PointMm(0, 1_200), eastPreview.first().start)
    }

    @Test
    fun `corner hints ignore joints inside the same generated curve group`() {
        val curveGroup = "${CURVE_GROUP_TAG_PREFIX}demo"
        val walls = listOf(
            WallSegment(
                id = "curve-a",
                start = PointMm(0, 0),
                end = PointMm(1_000, 0),
                tags = setOf(curveGroup)
            ),
            WallSegment(
                id = "curve-b",
                start = PointMm(1_000, 0),
                end = PointMm(1_000, 1_000),
                tags = setOf(curveGroup)
            )
        )

        val hints = collectCornerAngleHints(
            walls = walls,
            highlightedWallId = null
        )

        assertTrue(hints.isEmpty())
        assertTrue(walls[0].sameCurveGroupAs(walls[1]))
    }

    @Test
    fun `standalone ninety degree corners still produce a highlight candidate`() {
        val walls = listOf(
            WallSegment(
                id = "wall-a",
                start = PointMm(0, 0),
                end = PointMm(1_000, 0)
            ),
            WallSegment(
                id = "wall-b",
                start = PointMm(1_000, 0),
                end = PointMm(1_000, 1_000)
            )
        )

        val hints = collectCornerAngleHints(
            walls = walls,
            highlightedWallId = null
        )

        assertEquals(1, hints.size)
        assertTrue(kotlin.math.abs(hints.first().angleDegrees - 90.0) <= RIGHT_ANGLE_MARKER_TOLERANCE_DEG)
        assertFalse(walls[0].sameCurveGroupAs(walls[1]))
    }

    @Test
    fun `nearby shared endpoints still compute the true corner angle`() {
        val walls = listOf(
            WallSegment(
                id = "wall-a",
                start = PointMm(0, 0),
                end = PointMm(1_000, 0)
            ),
            WallSegment(
                id = "wall-b",
                start = PointMm(1_020, 0),
                end = PointMm(1_020, 1_000)
            )
        )

        val hints = collectCornerAngleHints(
            walls = walls,
            highlightedWallId = null
        )

        assertEquals(1, hints.size)
        assertEquals(90.0, hints.first().angleDegrees, 0.05)
    }

    @Test
    fun `nearby shared endpoints preserve diagonal corner math`() {
        val walls = listOf(
            WallSegment(
                id = "wall-a",
                start = PointMm(0, 0),
                end = PointMm(1_000, 0)
            ),
            WallSegment(
                id = "wall-b",
                start = PointMm(1_020, 0),
                end = PointMm(2_020, 1_000)
            )
        )

        val hints = collectCornerAngleHints(
            walls = walls,
            highlightedWallId = null
        )

        assertEquals(1, hints.size)
        assertEquals(45.0, hints.first().angleDegrees, 0.05)
    }
}
