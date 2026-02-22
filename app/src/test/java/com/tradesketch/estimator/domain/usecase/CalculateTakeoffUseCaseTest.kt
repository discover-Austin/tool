package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.test.Test
import kotlin.test.assertTrue

class CalculateTakeoffUseCaseTest {

    private val useCase = CalculateTakeoffUseCase()

    @Test
    fun `calculateDrywall returns result from blueprint`() {
        val wall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(Millimeters.fromFeet(10.0).value, 0),
            height = Millimeters.fromFeet(8.0)
        )
        val document = BlueprintDocument(
            projectId = "test",
            walls = listOf(wall)
        )

        val result = useCase.calculateDrywall(
            document = document,
            sheetAreaSqFt = 32.0,
            wastePercent = 10.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5
        )

        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.any { it.name == "Drywall sheets" })
    }

    @Test
    fun `calculateConcrete returns result from blueprint`() {
        val room = Room(
            id = "room-1",
            name = "Garage",
            polygon = listOf(
                PointMm(0, 0),
                PointMm(Millimeters.fromFeet(10.0).value, 0),
                PointMm(Millimeters.fromFeet(10.0).value, Millimeters.fromFeet(10.0).value),
                PointMm(0, Millimeters.fromFeet(10.0).value)
            )
        )
        val document = BlueprintDocument(
            projectId = "test",
            rooms = listOf(room)
        )

        val result = useCase.calculateConcrete(
            document = document,
            thicknessFeet = 0.33,
            wastePercent = 5.0
        )

        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.any { it.unit == "cubic yards" })
    }

    @Test
    fun `calculatePaint returns result from blueprint`() {
        val wall = WallSegment(
            id = "wall-1",
            start = PointMm(0, 0),
            end = PointMm(Millimeters.fromFeet(12.0).value, 0),
            height = Millimeters.fromFeet(9.0)
        )
        val document = BlueprintDocument(
            projectId = "test",
            walls = listOf(wall)
        )

        val result = useCase.calculatePaint(
            document = document,
            coverageSqFtPerGallon = 350.0,
            coats = 2,
            wastePercent = 10.0
        )

        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.any { it.name == "Paint" })
    }
}
