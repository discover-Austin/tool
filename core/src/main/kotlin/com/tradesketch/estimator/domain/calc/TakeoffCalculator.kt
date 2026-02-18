package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import kotlin.math.ceil

object TakeoffCalculator {
    fun drywallTakeoff(
        walls: List<Space>,
        sheetAreaSqFt: Double,
        wastePercent: Double,
        screwsPerSheet: Int,
        mudGallonsPer100SqFt: Double,
        costing: CostingInputs = CostingInputs.NONE
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
        return finalizeResult(items, costing)
    }

    fun concreteTakeoff(
        slabSpaces: List<Space>,
        thicknessFeet: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val areaSqFt = slabSpaces.sumOf { it.geometry.areaSqFt() }.coerceAtLeast(0.0)
        val thickness = thicknessFeet.coerceAtLeast(0.0)
        val volumeCuFt = areaSqFt * thickness
        val volumeWithWaste = applyWaste(volumeCuFt, wastePercent)
        val yards = volumeWithWaste / 27.0
        val items = listOf(
            TakeoffLine("Concrete volume", yards, "cubic yards")
        )
        return finalizeResult(items, costing)
    }

    fun gravelMulchTakeoff(
        spaces: List<Space>,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
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
        return finalizeResult(items, costing)
    }

    fun paintTakeoff(
        spaces: List<Space>,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val areaSqFt = spaces.sumOf { it.geometry.areaSqFt() }.coerceAtLeast(0.0)
        val coatsSafe = coats.coerceAtLeast(0)
        val coverage = coverageSqFtPerGallon.coerceAtLeast(1.0)
        val totalCoverage = applyWaste(areaSqFt * coatsSafe, wastePercent)
        val gallons = totalCoverage / coverage
        val items = listOf(
            TakeoffLine("Paint", gallons, "gallons")
        )
        return finalizeResult(items, costing)
    }

    private fun applyWaste(value: Double, wastePercent: Double): Double {
        val waste = wastePercent.coerceAtLeast(0.0)
        return value * (1 + waste / 100.0)
    }

    private fun finalizeResult(
        items: List<TakeoffLine>,
        costing: CostingInputs
    ): TakeoffResult {
        val pricedItems = items.map { item ->
            val unitCost = costing.unitCostByLineName[item.name]?.coerceAtLeast(0.0)
            item.copy(unitCost = unitCost)
        }

        val hasCosting = pricedItems.any { it.unitCost != null }
        if (!hasCosting) {
            return TakeoffResult(items = pricedItems, totalCost = null)
        }

        val materialSubtotal = pricedItems.sumOf { it.extendedCost ?: 0.0 }
        val laborCost = materialSubtotal * (costing.laborPercent.coerceAtLeast(0.0) / 100.0)
        val markupBase = materialSubtotal + laborCost
        val markupCost = markupBase * (costing.markupPercent.coerceAtLeast(0.0) / 100.0)
        val taxableSubtotal = markupBase + markupCost
        val taxCost = taxableSubtotal * (costing.taxPercent.coerceAtLeast(0.0) / 100.0)
        val grandTotal = taxableSubtotal + taxCost

        return TakeoffResult(
            items = pricedItems,
            totalCost = grandTotal,
            materialSubtotal = materialSubtotal,
            laborCost = laborCost,
            markupCost = markupCost,
            taxCost = taxCost
        )
    }
}
