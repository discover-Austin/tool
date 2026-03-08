package com.tradesketch.estimator.domain.model

enum class TakeoffScope {
    DRYWALL,
    CONCRETE,
    GRAVEL_MULCH,
    PAINT
}

enum class TakeoffInputMode {
    BLUEPRINT,
    MANUAL
}

data class ProjectTakeoffSession(
    val selectedScope: TakeoffScope = TakeoffScope.DRYWALL,
    val selectedPlaybook: String = "BALANCED",
    val inputMode: TakeoffInputMode = TakeoffInputMode.BLUEPRINT,
    val snapSettings: BlueprintSnapSettings = BlueprintSnapSettings(),
    val manual: ManualTakeoffSessionParams = ManualTakeoffSessionParams(),
    val drywall: DrywallSessionParams = DrywallSessionParams(),
    val concrete: ConcreteSessionParams = ConcreteSessionParams(),
    val gravel: GravelSessionParams = GravelSessionParams(),
    val paint: PaintSessionParams = PaintSessionParams(),
    val pricing: PricingSessionParams = PricingSessionParams()
)

data class ManualTakeoffSessionParams(
    val drywallWallAreaSqFt: Double = 0.0,
    val drywallCeilingAreaSqFt: Double = 0.0,
    val concreteAreaSqFt: Double = 0.0,
    val gravelAreaSqFt: Double = 0.0,
    val paintAreaSqFt: Double = 0.0
)

data class DrywallSessionParams(
    val sheetAreaSqFt: Double = 32.0,
    val wastePercent: Double = 10.0,
    val screwsPerSheet: Int = 32,
    val mudGallonsPer100SqFt: Double = 0.5,
    val includeCeilings: Boolean = true
)

data class ConcreteSessionParams(
    val thicknessFeet: Double = 0.33,
    val wastePercent: Double = 5.0
)

data class GravelSessionParams(
    val depthFeet: Double = 0.25,
    val densityTonsPerYard: Double = 1.4,
    val wastePercent: Double = 10.0
)

data class PaintSessionParams(
    val coverageSqFtPerGallon: Double = 350.0,
    val coats: Int = 2,
    val wastePercent: Double = 5.0
)

data class PricingSessionParams(
    val drywallSheetCost: Double = 17.5,
    val drywallScrewCost: Double = 0.01,
    val drywallMudCost: Double = 9.5,
    val concreteYardCost: Double = 165.0,
    val gravelYardCost: Double = 52.0,
    val gravelTonCost: Double = 36.0,
    val paintGallonCost: Double = 38.0,
    val laborPercent: Double = 20.0,
    val markupPercent: Double = 15.0,
    val taxPercent: Double = 8.0
)
