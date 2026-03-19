package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
