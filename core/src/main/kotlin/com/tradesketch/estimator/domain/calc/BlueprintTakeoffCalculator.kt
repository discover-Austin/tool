package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.TakeoffTrace
import kotlin.math.ceil

/**
 * Authoritative takeoff calculator for the pro 2D blueprint model.
 * All quantities are derived from blueprint geometry and params.
 */
object BlueprintTakeoffCalculator {

    fun drywallTakeoff(
        document: BlueprintDocument,
        sheetAreaSqFt: Double,
        wastePercent: Double,
        screwsPerSheet: Int,
        mudGallonsPer100SqFt: Double,
        includeCeilings: Boolean,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val wallAreas = wallAreaByIdSqFt(document)
        val openingAreas = openingAreaByWallIdSqFt(document)
        val traces = mutableListOf<TakeoffTrace>()

        val netWallArea = wallAreas.entries.sumOf { (wallId, gross) ->
            val openingArea = openingAreas[wallId] ?: 0.0
            traces += TakeoffTrace(
                metric = "wall_area",
                value = gross,
                unit = "sq_ft",
                wallId = wallId
            )
            if (openingArea > 0.0) {
                traces += TakeoffTrace(
                    metric = "opening_subtraction",
                    value = -openingArea,
                    unit = "sq_ft",
                    wallId = wallId
                )
            }
            (gross - openingArea).coerceAtLeast(0.0)
        }

        val ceilingArea = if (includeCeilings) {
            document.rooms
                .asSequence()
                .filter { room -> room.ceiling.enabled }
                .sumOf { room ->
                val area = room.areaSqFt()
                traces += TakeoffTrace(
                    metric = "ceiling_area",
                    value = area,
                    unit = "sq_ft",
                    roomId = room.id
                )
                area
            }
        } else {
            0.0
        }

        val paintableArea = (netWallArea + ceilingArea).coerceAtLeast(0.0)
        val adjustedArea = applyWaste(paintableArea, wastePercent)
        val safeSheetArea = sheetAreaSqFt.coerceAtLeast(1.0)
        val safeScrews = screwsPerSheet.coerceAtLeast(0)
        val safeMudRate = mudGallonsPer100SqFt.coerceAtLeast(0.0)

        val sheets = ceil(adjustedArea / safeSheetArea).coerceAtLeast(0.0)
        val screws = ceil(sheets * safeScrews).coerceAtLeast(0.0)
        val mudGallons = (adjustedArea / 100.0) * safeMudRate

        traces += TakeoffTrace(metric = "drywall_adjusted_area", value = adjustedArea, unit = "sq_ft")
        traces += TakeoffTrace(metric = "drywall_sheets", value = sheets, unit = "sheets")
        traces += TakeoffTrace(metric = "drywall_screws", value = screws, unit = "screws")
        traces += TakeoffTrace(metric = "drywall_mud", value = mudGallons, unit = "gallons")

        val items = listOf(
            TakeoffLine("Drywall sheets", sheets, "sheets"),
            TakeoffLine("Drywall screws", screws, "screws"),
            TakeoffLine("Joint compound", mudGallons, "gallons")
        )

        return withCosting(items = items, costing = costing, traces = traces)
    }

    fun concreteTakeoff(
        document: BlueprintDocument,
        thicknessFeet: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val traces = mutableListOf<TakeoffTrace>()
        val areaSqFt = document.rooms.sumOf { room ->
            val area = room.areaSqFt()
            traces += TakeoffTrace(
                metric = "concrete_area",
                value = area,
                unit = "sq_ft",
                roomId = room.id
            )
            area
        }

        val thickness = thicknessFeet.coerceAtLeast(0.0)
        val volumeCuFt = areaSqFt * thickness
        val withWaste = applyWaste(volumeCuFt, wastePercent)
        val yards = withWaste / 27.0

        traces += TakeoffTrace(metric = "concrete_volume", value = yards, unit = "cubic_yards")
        val items = listOf(TakeoffLine("Concrete volume", yards, "cubic yards"))
        return withCosting(items = items, costing = costing, traces = traces)
    }

    fun gravelMulchTakeoff(
        document: BlueprintDocument,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val traces = mutableListOf<TakeoffTrace>()
        val targetRooms = document.rooms.filter { room ->
            room.tags.any { tag -> tag == "gravel" || tag == "mulch" || tag == "bed" }
        }.ifEmpty { document.rooms }

        val areaSqFt = targetRooms.sumOf { room ->
            val area = room.areaSqFt()
            traces += TakeoffTrace(
                metric = "gravel_area",
                value = area,
                unit = "sq_ft",
                roomId = room.id
            )
            area
        }

        val depth = depthFeet.coerceAtLeast(0.0)
        val density = densityTonsPerYard.coerceAtLeast(0.0)
        val volumeCuFt = areaSqFt * depth
        val volumeWithWaste = applyWaste(volumeCuFt, wastePercent)
        val yards = volumeWithWaste / 27.0
        val tons = yards * density

        traces += TakeoffTrace(metric = "gravel_volume", value = yards, unit = "cubic_yards")
        traces += TakeoffTrace(metric = "gravel_weight", value = tons, unit = "tons")

        val items = listOf(
            TakeoffLine("Material volume", yards, "cubic yards"),
            TakeoffLine("Material weight", tons, "tons")
        )
        return withCosting(items = items, costing = costing, traces = traces)
    }

    fun paintTakeoff(
        document: BlueprintDocument,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        val wallAreas = wallAreaByIdSqFt(document)
        val openingAreas = openingAreaByWallIdSqFt(document)
        val traces = mutableListOf<TakeoffTrace>()

        val netArea = wallAreas.entries.sumOf { (wallId, area) ->
            val opening = openingAreas[wallId] ?: 0.0
            val net = (area - opening).coerceAtLeast(0.0)
            traces += TakeoffTrace(metric = "paintable_wall_area", value = net, unit = "sq_ft", wallId = wallId)
            net
        }

        val safeCoverage = coverageSqFtPerGallon.coerceAtLeast(1.0)
        val safeCoats = coats.coerceAtLeast(0)
        val coverageArea = applyWaste(netArea * safeCoats, wastePercent)
        val gallons = coverageArea / safeCoverage

        traces += TakeoffTrace(metric = "paint_coverage_area", value = coverageArea, unit = "sq_ft")
        traces += TakeoffTrace(metric = "paint_gallons", value = gallons, unit = "gallons")

        val items = listOf(TakeoffLine("Paint", gallons, "gallons"))
        return withCosting(items = items, costing = costing, traces = traces)
    }

    fun wallAreaByIdSqFt(document: BlueprintDocument): Map<String, Double> {
        return document.walls.associate { wall ->
            val lengthFeet = Millimeters(wall.lengthMillimeters()).toFeet()
            val heightFeet = Millimeters(wall.heightMm).toFeet()
            wall.id to (lengthFeet * heightFeet)
        }
    }

    fun openingAreaByWallIdSqFt(document: BlueprintDocument): Map<String, Double> {
        return document.openings
            .groupBy { it.wallId }
            .mapValues { (_, openings) ->
                openings.sumOf { opening ->
                    val width = Millimeters(opening.widthMm)
                    val height = Millimeters(opening.heightMm)
                    width.toFeet() * height.toFeet()
                }
            }
    }

    private fun applyWaste(value: Double, wastePercent: Double): Double {
        val waste = wastePercent.coerceAtLeast(0.0)
        return value * (1.0 + (waste / 100.0))
    }

    private fun withCosting(
        items: List<TakeoffLine>,
        costing: CostingInputs,
        traces: List<TakeoffTrace>
    ): TakeoffResult {
        val pricedItems = items.map { item ->
            val unitCost = costing.unitCostByLineName[item.name]?.coerceAtLeast(0.0)
            item.copy(unitCost = unitCost)
        }

        val hasCosting = pricedItems.any { it.unitCost != null }
        if (!hasCosting) {
            return TakeoffResult(items = pricedItems, totalCost = null, traces = traces)
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
            taxCost = taxCost,
            traces = traces
        )
    }
}
