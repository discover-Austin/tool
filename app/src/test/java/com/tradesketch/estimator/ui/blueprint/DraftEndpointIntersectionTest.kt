package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftEndpointIntersectionTest {

    @Test
    fun returnsTrueWhenPreviewEndsOnExistingEndpoint() {
        val result = isDraftEndpointIntersectingExistingGeometry(
            drawingStart = PointMm(0, 0),
            previewEnd = PointMm(1000, 600),
            walls = listOf(
                wall(
                    id = "wall-a",
                    start = PointMm(1000, -300),
                    end = PointMm(1000, 600)
                )
            ),
            chainOrigin = null,
            thresholdMm = 140L
        )

        assertTrue(result)
    }

    @Test
    fun returnsTrueWhenPreviewRunsIntoExistingWallFace() {
        val result = isDraftEndpointIntersectingExistingGeometry(
            drawingStart = PointMm(0, 0),
            previewEnd = PointMm(980, 0),
            walls = listOf(
                wall(
                    id = "wall-b",
                    start = PointMm(1000, -500),
                    end = PointMm(1000, 500)
                )
            ),
            chainOrigin = null,
            thresholdMm = 140L
        )

        assertTrue(result)
    }

    @Test
    fun returnsFalseWhenPreviewOnlyHoversNearTheStartingGeometry() {
        val result = isDraftEndpointIntersectingExistingGeometry(
            drawingStart = PointMm(0, 0),
            previewEnd = PointMm(50, 0),
            walls = listOf(
                wall(
                    id = "wall-c",
                    start = PointMm(0, 0),
                    end = PointMm(1200, 0)
                )
            ),
            chainOrigin = null,
            thresholdMm = 140L
        )

        assertFalse(result)
    }

    @Test
    fun returnsTrueWhenPreviewClosesBackToChainOrigin() {
        val result = isDraftEndpointIntersectingExistingGeometry(
            drawingStart = PointMm(0, 0),
            previewEnd = PointMm(30, 980),
            walls = listOf(
                wall(
                    id = "wall-d",
                    start = PointMm(0, 1000),
                    end = PointMm(900, 1000)
                )
            ),
            chainOrigin = PointMm(0, 1000),
            thresholdMm = 140L
        )

        assertTrue(result)
    }

    private fun wall(
        id: String,
        start: PointMm,
        end: PointMm
    ): WallSegment {
        return WallSegment(
            id = id,
            start = start,
            end = end
        )
    }
}
