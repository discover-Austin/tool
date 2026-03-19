package com.tradesketch.estimator.ui.blueprint

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftCommitSnapTest {

    @Test
    fun wallCommit_prefersVisiblePreviewOverResnappingTap() {
        val previewEnd = PointMm(1_000L, 0L)
        val resnappedTap = PointMm(5_000L, 5_000L)
        val walls = listOf(
            WallSegment(
                id = "target",
                start = PointMm(1_000L, 0L),
                end = PointMm(1_000L, 2_000L)
            )
        )

        val committed = resolveDraftWallCommitEnd(
            previewEnd = previewEnd,
            snappedTap = resnappedTap,
            walls = walls,
            snapThresholdFeet = 0.75,
            endpointSnappingEnabled = true,
            closureEnabled = true,
            chainOrigin = PointMm(0L, 0L)
        )

        assertEquals(previewEnd, committed)
    }

    @Test
    fun wallCommit_fallsBackToLegacySnappingWhenPreviewIsMissing() {
        val endpoint = PointMm(1_000L, 0L)
        val walls = listOf(
            WallSegment(
                id = "target",
                start = endpoint,
                end = PointMm(1_000L, 2_000L)
            )
        )

        val committed = resolveDraftWallCommitEnd(
            previewEnd = null,
            snappedTap = PointMm(1_080L, 15L),
            walls = walls,
            snapThresholdFeet = 0.5,
            endpointSnappingEnabled = true,
            closureEnabled = false,
            chainOrigin = null
        )

        assertEquals(endpoint, committed)
    }

    @Test
    fun boxCommit_prefersVisiblePreviewOverResnappingTap() {
        val previewEnd = PointMm(2_500L, 1_500L)
        val committed = resolveDraftBoxCommitEnd(
            previewEnd = previewEnd,
            snappedTap = PointMm(9_000L, 9_000L),
            walls = listOf(
                WallSegment(
                    id = "target",
                    start = PointMm(2_500L, 1_500L),
                    end = PointMm(2_500L, 3_500L)
                )
            ),
            snapThresholdFeet = 0.75,
            endpointSnappingEnabled = true
        )

        assertEquals(previewEnd, committed)
    }
}
