package com.tradesketch.estimator.ui.blueprint

import androidx.compose.ui.geometry.Offset
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlueprintScreenGeometryTest {
    @Test
    fun `draft endpoint reports intersection when it lands on another wall`() {
        val drawingStart = PointMm(0, 0)
        val previewEnd = PointMm(4_000, 900)
        val walls = listOf(
            WallSegment(
                id = "wall-1",
                start = PointMm(4_000, -2_000),
                end = PointMm(4_000, 2_000)
            )
        )

        assertTrue(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = drawingStart,
                previewEnd = previewEnd,
                walls = walls,
                chainOrigin = null,
                thresholdMm = 45L
            )
        )
    }

    @Test
    fun `draft endpoint reports closure when it lands back on chain origin`() {
        val chainOrigin = PointMm(0, 0)
        val drawingStart = PointMm(3_000, 0)
        val walls = listOf(
            WallSegment(
                id = "wall-1",
                start = chainOrigin,
                end = drawingStart
            )
        )

        assertTrue(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = drawingStart,
                previewEnd = chainOrigin,
                walls = walls,
                chainOrigin = chainOrigin,
                thresholdMm = 45L
            )
        )
    }

    @Test
    fun `draft endpoint stays idle when still sitting on the starting anchor`() {
        val drawingStart = PointMm(0, 0)
        val walls = listOf(
            WallSegment(
                id = "wall-1",
                start = drawingStart,
                end = PointMm(4_000, 0)
            )
        )

        assertFalse(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = drawingStart,
                previewEnd = drawingStart,
                walls = walls,
                chainOrigin = null,
                thresholdMm = 80L
            )
        )
    }

    @Test
    fun `committed wall labels group matching parallel lengths`() {
        val document = BlueprintDocument(
            projectId = "project-1",
            walls = listOf(
                WallSegment(id = "w1", start = PointMm(0, 0), end = PointMm(4_000, 0)),
                WallSegment(id = "w2", start = PointMm(4_000, 0), end = PointMm(4_000, 2_000)),
                WallSegment(id = "w3", start = PointMm(4_000, 2_000), end = PointMm(0, 2_000)),
                WallSegment(id = "w4", start = PointMm(0, 2_000), end = PointMm(0, 0))
            )
        )

        val labels = collectCommittedWallLengthLabels(
            document = document,
            selectedWallId = null,
            worldToScreen = { point -> Offset(point.x.toFloat(), point.y.toFloat()) }
        )

        assertEquals(2, labels.size)
        assertEquals(setOf(2), labels.map(WallLengthLabelSpec::repeatCount).toSet())
    }
}
