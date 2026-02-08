package com.yourcompany.tradesketch.domain.calc

import com.yourcompany.tradesketch.domain.model.Space
import com.yourcompany.tradesketch.domain.model.TakeoffLine
import com.yourcompany.tradesketch.domain.model.TakeoffResult
import com.yourcompany.tradesketch.domain.model.areaSqFt
import com.yourcompany.tradesketch.domain.model.openingsAreaSqFt
import kotlin.math.ceil

object TakeoffCalculator {
    fun drywallTakeoff(
        walls: List<Space>,
        sheetAreaSqFt: Double,
        wastePercent: Double,
        screwsPerSheet: Int,
        mudGallonsPer100SqFt: Double
    ): TakeoffResult {
        val wallArea = walls.sumOf { it.geometry.areaSqFt() - it.openingsAreaSqFt() }.coerceAtLeast(0.0)
        val adjustedArea = applyWaste(wallArea, wastePercent)
        val sheetArea = sheetAreaSqFt.coerceAtLeast(1.0)
        val screwsPerSheetSafe = screwsPerSheet.coerceAtLeast(0)
        val mudRate = mudGallonsPer100SqFt.coerceAtLeast(0.0)
        val sheets = ceil(adjustedArea / sheetArea).coerceAtLeast(0.0)
        val screws = ceil(sheets * screwsPerSheetSafe).coerceAtLeast(0.0)
        val mud = (adjustedArea / 100.0) * mudRate

        val items = listOf(
            TakeoffLine("Drywall sheets", sheets, "sheets"),
            TakeoffLine("Drywall screws", screws, "screws"),
            TakeoffLine("Joint compound", mud, "gallons")
        )
        return TakeoffResult(items = items, totalCost = null)
    }

    fun concreteTakeoff(
        slabSpaces: List<Space>,
        thicknessFeet: Double,
        wastePercent: Double
    ): TakeoffResult {
        val areaSqFt = slabSpaces.sumOf { it.geometry.areaSqFt() }.coerceAtLeast(0.0)
        val thickness = thicknessFeet.coerceAtLeast(0.0)
        val volumeCuFt = areaSqFt * thickness
        val volumeWithWaste = applyWaste(volumeCuFt, wastePercent)
        val yards = volumeWithWaste / 27.0
        val items = listOf(
            TakeoffLine("Concrete volume", yards, "cubic yards")
        )
        return TakeoffResult(items = items, totalCost = null)
    }

    fun gravelMulchTakeoff(
        spaces: List<Space>,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double
    ): TakeoffResult {
        val areaSqFt = spaces.sumOf { it.geometry.areaSqFt() }.coerceAtLeast(0.0)
        val depth = depthFeet.coerceAtLeast(0.0)
        val density = densityTonsPerYard.coerceAtLeast(0.0)
        val volumeCuFt = areaSqFt * depth
        val volumeWithWaste = applyWaste(volumeCuFt, wastePercent)
        val yards = volumeWithWaste / 27.0
        val tons = yards * density
        val items = listOf(
            TakeoffLine("Material volume", yards, "cubic yards"),
            TakeoffLine("Material weight", tons, "tons")
        )
        return TakeoffResult(items = items, totalCost = null)
    }

    fun paintTakeoff(
        spaces: List<Space>,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double
    ): TakeoffResult {
        val areaSqFt = spaces.sumOf { it.geometry.areaSqFt() }.coerceAtLeast(0.0)
        val coatsSafe = coats.coerceAtLeast(0)
        val coverage = coverageSqFtPerGallon.coerceAtLeast(1.0)
        val totalCoverage = applyWaste(areaSqFt * coatsSafe, wastePercent)
        val gallons = totalCoverage / coverage
        val items = listOf(
            TakeoffLine("Paint", gallons, "gallons")
        )
        return TakeoffResult(items = items, totalCost = null)
    }

    private fun applyWaste(value: Double, wastePercent: Double): Double {
        val waste = wastePercent.coerceAtLeast(0.0)
        return value * (1 + waste / 100.0)
    }
}
