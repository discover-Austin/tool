package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.blueprint.CURVE_SHAPE_ARC_TAG
import com.tradesketch.estimator.ui.blueprint.CURVE_SHAPE_CIRCLE_TAG
import com.tradesketch.estimator.ui.blueprint.buildDraftCircleWalls
import com.tradesketch.estimator.ui.blueprint.buildMeasuredArcWalls
import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintEditorViewModelSelectionTest {

    @Test
    fun `grouped circle selection resolves to every segment id`() {
        val document = BlueprintDocument.empty("circle-delete")
        val walls = buildDraftCircleWalls(
            document = document,
            center = PointMm(0, 0),
            edge = PointMm(1_200, 0),
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_CIRCLE_TAG)
        )

        assertEquals(
            walls.mapTo(linkedSetOf()) { it.id },
            deletedWallIdsForSelection(
                document = document.copy(walls = walls),
                selectedWallId = walls.first().id
            )
        )
    }

    @Test
    fun `grouped measured arc selection resolves to every segment id`() {
        val document = BlueprintDocument.empty("arc-delete")
        val walls = buildMeasuredArcWalls(
            document = document,
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            riseMm = 400.0,
            scale = 1f,
            wallHeightMm = 2_743L,
            wallThicknessMm = 114L,
            tags = setOf("drawn", CURVE_SHAPE_ARC_TAG)
        )

        assertEquals(
            walls.mapTo(linkedSetOf()) { it.id },
            deletedWallIdsForSelection(
                document = document.copy(walls = walls),
                selectedWallId = walls.first().id
            )
        )
    }

    @Test
    fun `standalone wall selection resolves to only the selected id`() {
        val wall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(1_600, 0),
            height = Millimeters(2_743L),
            thickness = Millimeters(114L),
            tags = setOf("drawn")
        )
        val document = BlueprintDocument.empty("standalone-delete").copy(walls = listOf(wall))

        assertEquals(
            setOf("wall-1"),
            deletedWallIdsForSelection(
                document = document,
                selectedWallId = wall.id
            )
        )
    }
}
