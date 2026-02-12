package com.tradesketch.estimator.domain.model

/**
 * Application settings model.
 * Stores user preferences for the app.
 */
data class Settings(
    val defaultWastePercent: Double = 10.0,
    val useMetric: Boolean = false,
    val defaultDrywallSheetArea: Double = 32.0, // 4'×8' = 32 sq ft
    val defaultScrewsPerSheet: Int = 32,
    val defaultMudGallonsPer100SqFt: Double = 0.5,
    val defaultCoveragePerGallon: Double = 350.0, // sq ft
    val defaultCoatsOfPaint: Int = 2,
    val drywallSheetUnitCost: Double = 17.5,
    val drywallScrewUnitCost: Double = 0.01,
    val drywallMudUnitCost: Double = 9.5,
    val concreteYardUnitCost: Double = 165.0,
    val gravelYardUnitCost: Double = 52.0,
    val gravelTonUnitCost: Double = 36.0,
    val paintGallonUnitCost: Double = 38.0,
    val laborPercent: Double = 20.0,
    val markupPercent: Double = 15.0,
    val taxPercent: Double = 8.0
) {
    companion object {
        val DEFAULT = Settings()
    }
}
