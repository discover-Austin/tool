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
    val defaultCoatsOfPaint: Int = 2
) {
    companion object {
        val DEFAULT = Settings()
    }
}
