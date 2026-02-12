package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.domain.calc.TakeoffCalculator
import com.tradesketch.estimator.domain.model.*
import javax.inject.Inject

class CalculateTakeoffUseCase @Inject constructor(
    private val calculator: TakeoffCalculator
) {
    fun calculateDrywall(
        walls: List<Space>,
        sheetAreaSqFt: Double,
        wastePercent: Double,
        screwsPerSheet: Int,
        mudGallonsPer100SqFt: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return calculator.drywallTakeoff(
            walls, sheetAreaSqFt, wastePercent, screwsPerSheet, mudGallonsPer100SqFt, costing
        )
    }
    
    fun calculateConcrete(
        slabSpaces: List<Space>,
        thicknessFeet: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return calculator.concreteTakeoff(slabSpaces, thicknessFeet, wastePercent, costing)
    }
    
    fun calculateGravelMulch(
        spaces: List<Space>,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return calculator.gravelMulchTakeoff(spaces, depthFeet, densityTonsPerYard, wastePercent, costing)
    }
    
    fun calculatePaint(
        spaces: List<Space>,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double,
        costing: CostingInputs = CostingInputs.NONE
    ): TakeoffResult {
        return calculator.paintTakeoff(spaces, coverageSqFtPerGallon, coats, wastePercent, costing)
    }
}
