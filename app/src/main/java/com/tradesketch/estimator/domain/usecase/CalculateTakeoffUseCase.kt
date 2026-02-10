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
        mudGallonsPer100SqFt: Double
    ): TakeoffResult {
        return calculator.drywallTakeoff(
            walls, sheetAreaSqFt, wastePercent, screwsPerSheet, mudGallonsPer100SqFt
        )
    }
    
    fun calculateConcrete(
        slabSpaces: List<Space>,
        thicknessFeet: Double,
        wastePercent: Double
    ): TakeoffResult {
        return calculator.concreteTakeoff(slabSpaces, thicknessFeet, wastePercent)
    }
    
    fun calculateGravelMulch(
        spaces: List<Space>,
        depthFeet: Double,
        densityTonsPerYard: Double,
        wastePercent: Double
    ): TakeoffResult {
        return calculator.gravelMulchTakeoff(spaces, depthFeet, densityTonsPerYard, wastePercent)
    }
    
    fun calculatePaint(
        spaces: List<Space>,
        coverageSqFtPerGallon: Double,
        coats: Int,
        wastePercent: Double
    ): TakeoffResult {
        return calculator.paintTakeoff(spaces, coverageSqFtPerGallon, coats, wastePercent)
    }
}
