package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlueprintDraftIntersectionTest {
    @Test
    fun detectsEndpointHitOnExistingWall() {
        val walls = listOf(
            WallSegment(
                id = "target",
                start = PointMm(1_000L, 0L),
                end = PointMm(1_000L, 1_000L)
            )
        )

        assertTrue(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = PointMm(0L, 0L),
                previewEnd = PointMm(1_000L, 0L),
                walls = walls,
                chainOrigin = null,
                thresholdMm = 80L
            )
        )
    }

    @Test
    fun detectsProjectionHitInsideWallBody() {
        val walls = listOf(
            WallSegment(
                id = "target",
                start = PointMm(1_000L, -400L),
                end = PointMm(1_000L, 600L)
            )
        )

        assertTrue(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = PointMm(0L, 0L),
                previewEnd = PointMm(1_000L, 125L),
                walls = walls,
                chainOrigin = null,
                thresholdMm = 70L
            )
        )
    }

    @Test
    fun detectsRoomClosureHit() {
        assertTrue(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = PointMm(1_000L, 1_000L),
                previewEnd = PointMm(0L, 1_000L),
                walls = emptyList(),
                chainOrigin = PointMm(0L, 1_000L),
                thresholdMm = 70L
            )
        )
    }

    @Test
    fun ignoresFreeSpacePreview() {
        val walls = listOf(
            WallSegment(
                id = "target",
                start = PointMm(1_000L, 0L),
                end = PointMm(1_000L, 1_000L)
            )
        )

        assertFalse(
            isDraftEndpointIntersectingExistingGeometry(
                drawingStart = PointMm(0L, 0L),
                previewEnd = PointMm(720L, 140L),
                walls = walls,
                chainOrigin = null,
                thresholdMm = 50L
            )
        )
    }
}
