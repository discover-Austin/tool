package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.*
import javax.inject.Inject

/**
 * Use case for calculating material takeoffs from BlueprintDocument.
 * This is the authoritative calculator - all quantities derive from blueprint geometry.
 */
class CalculateTakeoffUseCase @Inject constructor() {

    fun calculateDrywall(
        document: BlueprintDocument,
        sheetAreaSqFt: Double,
        wastePercent: Double,
        screwsPerSheet: Int,
        mudGallonsPer100SqFt: Double,
        includeCeilings: Boolean = true,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return BlueprintTakeoffCalculator.drywallTakeoff(
            document = document,
            sheetAreaSqFt = sheetAreaSqFt,
            wastePercent = wastePercent,
            screwsPerSheet = screwsPerSheet,
            mudGallonsPer100SqFt = mudGallonsPer100SqFt,
            includeCeilings = includeCeilings,
            costing = costing
        )
    }

    fun calculateConcrete(
        document: BlueprintDocument,
        thicknessFeet: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return BlueprintTakeoffCalculator.concreteTakeoff(
            document = document,
            thicknessFeet = thicknessFeet,
            wastePercent = wastePercent,
            costing = costing
        )
    }

    fun calculateGravelMulch(
        document: BlueprintDocument,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return BlueprintTakeoffCalculator.gravelMulchTakeoff(
            document = document,
            depthFeet = depthFeet,
            densityTonsPerYard = densityTonsPerYard,
            wastePercent = wastePercent,
            costing = costing
        )
    }

    fun calculatePaint(
        document: BlueprintDocument,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return BlueprintTakeoffCalculator.paintTakeoff(
            document = document,
            coverageSqFtPerGallon = coverageSqFtPerGallon,
            coats = coats,
            wastePercent = wastePercent,
            costing = costing
        )
    }
}
