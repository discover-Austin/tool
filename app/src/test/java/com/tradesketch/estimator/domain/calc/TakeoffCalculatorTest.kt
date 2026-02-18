package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Opening
import com.tradesketch.estimator.domain.model.Space
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TakeoffCalculatorTest {
    @Test
    fun `drywall takeoff applies waste and rounds sheets`() {
        val wall = Space(
            id = "wall-1",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(12.0),
                height = Millimeters.fromFeet(8.0)
            )
        )

        val result = TakeoffCalculator.drywallTakeoff(
            walls = listOf(wall),
            sheetAreaSqFt = 32.0,
            wastePercent = 10.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5
        )

        val sheets = result.items.first { it.name == "Drywall sheets" }.quantity
        assertEquals(4.0, sheets)
        val screws = result.items.first { it.name == "Drywall screws" }.quantity
        assertEquals(128.0, screws)
        val mud = result.items.first { it.name == "Joint compound" }.quantity
        assertTrue(mud > 0.0)
    }

    @Test
    fun `drywall takeoff subtracts openings`() {
        val wall = Space(
            id = "wall-3",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(10.0),
                height = Millimeters.fromFeet(8.0)
            ),
            openings = listOf(
                Opening(
                    width = Millimeters.fromFeet(3.0),
                    height = Millimeters.fromFeet(7.0),
                    count = 1
                )
            )
        )

        val result = TakeoffCalculator.drywallTakeoff(
            walls = listOf(wall),
            sheetAreaSqFt = 32.0,
            wastePercent = 0.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5
        )

        val sheets = result.items.first { it.name == "Drywall sheets" }.quantity
        assertEquals(2.0, sheets)
    }

    @Test
    fun `drywall takeoff for 10x10 room returns practical sheet and screw counts`() {
        val wallHeight = Millimeters.fromFeet(8.0)
        val wallLength = Millimeters.fromFeet(10.0)
        val walls = listOf(
            Space(
                id = "room-wall-1",
                name = "Wall 1",
                geometry = Geometry.Wall(length = wallLength, height = wallHeight),
                openings = listOf(
                    Opening(
                        width = Millimeters.fromFeet(3.0),
                        height = Millimeters.fromFeet(7.0),
                        count = 1
                    ),
                    Opening(
                        width = Millimeters.fromFeet(4.0),
                        height = Millimeters.fromFeet(3.0),
                        count = 1
                    )
                )
            ),
            Space(
                id = "room-wall-2",
                name = "Wall 2",
                geometry = Geometry.Wall(length = wallLength, height = wallHeight)
            ),
            Space(
                id = "room-wall-3",
                name = "Wall 3",
                geometry = Geometry.Wall(length = wallLength, height = wallHeight)
            ),
            Space(
                id = "room-wall-4",
                name = "Wall 4",
                geometry = Geometry.Wall(length = wallLength, height = wallHeight)
            )
        )

        val result = TakeoffCalculator.drywallTakeoff(
            walls = walls,
            sheetAreaSqFt = 32.0,
            wastePercent = 10.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5
        )

        val sheets = result.items.first { it.name == "Drywall sheets" }.quantity
        val screws = result.items.first { it.name == "Drywall screws" }.quantity

        assertEquals(10.0, sheets)
        assertEquals(320.0, screws)
    }

    @Test
    fun `concrete takeoff returns cubic yards`() {
        val slab = Space(
            id = "slab-1",
            name = "Driveway",
            geometry = Geometry.Slab(
                length = Millimeters.fromFeet(20.0),
                width = Millimeters.fromFeet(10.0),
                thickness = Millimeters.fromFeet(0.5)
            )
        )

        val result = TakeoffCalculator.concreteTakeoff(
            slabSpaces = listOf(slab),
            thicknessFeet = 0.5,
            wastePercent = 5.0
        )

        val yards = result.items.first().quantity
        assertTrue(yards > 0.0)
    }

    @Test
    fun `gravel takeoff handles density`() {
        val yard = Space(
            id = "yard-1",
            name = "Yard Bed",
            geometry = Geometry.Rect(
                length = Millimeters.fromFeet(12.0),
                width = Millimeters.fromFeet(8.0)
            )
        )

        val result = TakeoffCalculator.gravelMulchTakeoff(
            spaces = listOf(yard),
            depthFeet = 0.5,
            densityTonsPerYard = 1.4,
            wastePercent = 10.0
        )

        val tons = result.items.first { it.unit == "tons" }.quantity
        assertTrue(tons > 0.0)
    }

    @Test
    fun `paint takeoff respects coats`() {
        val wall = Space(
            id = "wall-2",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(10.0),
                height = Millimeters.fromFeet(9.0)
            )
        )

        val result = TakeoffCalculator.paintTakeoff(
            spaces = listOf(wall),
            coverageSqFtPerGallon = 350.0,
            coats = 2,
            wastePercent = 0.0
        )

        val gallons = result.items.first().quantity
        assertTrue(gallons > 0.0)
    }

    @Test
    fun `paint takeoff subtracts wall openings`() {
        val wall = Space(
            id = "wall-paint-openings",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(10.0),
                height = Millimeters.fromFeet(8.0)
            ),
            openings = listOf(
                Opening(
                    width = Millimeters.fromFeet(3.0),
                    height = Millimeters.fromFeet(7.0),
                    count = 1
                )
            )
        )

        val result = TakeoffCalculator.paintTakeoff(
            spaces = listOf(wall),
            coverageSqFtPerGallon = 350.0,
            coats = 1,
            wastePercent = 0.0
        )
        val expectedGallons = (80.0 - 21.0) / 350.0
        val actualGallons = result.items.first().quantity
        assertTrue(abs(actualGallons - expectedGallons) < 0.0001)
    }

    @Test
    fun `takeoffs clamp negative inputs`() {
        val wall = Space(
            id = "wall-4",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(10.0),
                height = Millimeters.fromFeet(8.0)
            )
        )

        val drywall = TakeoffCalculator.drywallTakeoff(
            walls = listOf(wall),
            sheetAreaSqFt = -1.0,
            wastePercent = -10.0,
            screwsPerSheet = -4,
            mudGallonsPer100SqFt = -2.0
        )
        assertTrue(drywall.items.all { it.quantity >= 0.0 })

        val concrete = TakeoffCalculator.concreteTakeoff(
            slabSpaces = listOf(wall.copy(geometry = Geometry.Slab(
                length = Millimeters.fromFeet(10.0),
                width = Millimeters.fromFeet(10.0),
                thickness = Millimeters.fromFeet(0.0)
            ))),
            thicknessFeet = -2.0,
            wastePercent = -5.0
        )
        assertTrue(concrete.items.first().quantity >= 0.0)

        val paint = TakeoffCalculator.paintTakeoff(
            spaces = listOf(wall),
            coverageSqFtPerGallon = -350.0,
            coats = -1,
            wastePercent = -5.0
        )
        assertTrue(paint.items.first().quantity >= 0.0)
    }

    @Test
    fun `pricing inputs produce grand total and breakdown`() {
        val wall = Space(
            id = "wall-priced",
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters.fromFeet(12.0),
                height = Millimeters.fromFeet(8.0)
            )
        )

        val result = TakeoffCalculator.drywallTakeoff(
            walls = listOf(wall),
            sheetAreaSqFt = 32.0,
            wastePercent = 10.0,
            screwsPerSheet = 32,
            mudGallonsPer100SqFt = 0.5,
            costing = CostingInputs(
                unitCostByLineName = mapOf(
                    "Drywall sheets" to 18.0,
                    "Drywall screws" to 0.01,
                    "Joint compound" to 10.0
                ),
                laborPercent = 20.0,
                markupPercent = 10.0,
                taxPercent = 8.0
            )
        )

        assertTrue(result.items.all { it.unitCost != null })
        assertTrue((result.materialSubtotal ?: 0.0) > 0.0)
        assertTrue((result.laborCost ?: 0.0) > 0.0)
        assertTrue((result.markupCost ?: 0.0) > 0.0)
        assertTrue((result.taxCost ?: 0.0) > 0.0)
        assertTrue((result.totalCost ?: 0.0) > (result.materialSubtotal ?: 0.0))
    }
}
