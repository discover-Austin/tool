package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.displayLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportViewModelPayloadTest {

    @Test
    fun `estimate export payload keeps the selected trade blueprint`() {
        val project = mixedTradeProject()
        val resolvedBlueprints = resolveExportBlueprints(project = project, selectedType = TakeoffType.CONCRETE)

        val payload = buildEstimateExportPayload(
            ExportUiState(
                project = project,
                selectedType = TakeoffType.CONCRETE,
                takeoffType = TakeoffType.CONCRETE.displayLabel,
                generatedAtMillis = 1234L,
                result = takeoffResult(),
                selectedTradeBlueprint = resolvedBlueprints.selectedTradeBlueprint,
                projectBlueprint = resolvedBlueprints.projectBlueprint
            )
        )

        requireNotNull(payload)
        assertEquals(4, payload.blueprint.walls.size)
        assertEquals(1, payload.blueprint.rooms.size)
        assertEquals(resolvedBlueprints.selectedTradeBlueprint, payload.blueprint)
    }

    @Test
    fun `blueprint export payload keeps the full project blueprint`() {
        val project = mixedTradeProject()
        val resolvedBlueprints = resolveExportBlueprints(project = project, selectedType = TakeoffType.CONCRETE)

        val payload = buildBlueprintExportPayload(
            ExportUiState(
                project = project,
                selectedType = TakeoffType.CONCRETE,
                result = takeoffResult(),
                selectedTradeBlueprint = resolvedBlueprints.selectedTradeBlueprint,
                projectBlueprint = resolvedBlueprints.projectBlueprint
            )
        )

        requireNotNull(payload)
        assertEquals(8, payload.blueprint.walls.size)
        assertEquals(0, payload.blueprint.rooms.size)
        assertEquals(resolvedBlueprints.projectBlueprint, payload.blueprint)
    }

    private fun mixedTradeProject(): Project {
        return Project(
            id = "project-export",
            name = "Mixed Export Project",
            blueprintDocument = BlueprintDocument(
                projectId = "project-export",
                walls = rectangleWalls(
                    prefix = "conc",
                    scope = TakeoffScope.CONCRETE,
                    origin = PointMm(0, 0)
                ) + rectangleWalls(
                    prefix = "paint",
                    scope = TakeoffScope.PAINT,
                    origin = PointMm(Millimeters.fromFeet(20.0).value, 0)
                )
            )
        )
    }

    private fun takeoffResult(): TakeoffResult {
        return TakeoffResult(
            items = listOf(
                TakeoffLine(
                    name = "Concrete volume",
                    quantity = 12.0,
                    unit = "cu yd",
                    unitCost = 165.0
                )
            ),
            totalCost = 1980.0
        )
    }

    private fun rectangleWalls(
        prefix: String,
        scope: TakeoffScope,
        origin: PointMm
    ): List<WallSegment> {
        val width = Millimeters.fromFeet(10.0).value
        val height = Millimeters.fromFeet(10.0).value
        val tags = setOf(scope.tradeScopeTag())
        val p1 = origin
        val p2 = PointMm(origin.x + width, origin.y)
        val p3 = PointMm(origin.x + width, origin.y + height)
        val p4 = PointMm(origin.x, origin.y + height)
        return listOf(
            WallSegment(id = "${prefix}-1", start = p1, end = p2, tags = tags),
            WallSegment(id = "${prefix}-2", start = p2, end = p3, tags = tags),
            WallSegment(id = "${prefix}-3", start = p3, end = p4, tags = tags),
            WallSegment(id = "${prefix}-4", start = p4, end = p1, tags = tags)
        )
    }
}
