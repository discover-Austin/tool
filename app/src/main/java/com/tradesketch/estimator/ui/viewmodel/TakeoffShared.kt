package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.ConcreteSessionParams
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.DrywallSessionParams
import com.tradesketch.estimator.domain.model.GravelSessionParams
import com.tradesketch.estimator.domain.model.PaintSessionParams
import com.tradesketch.estimator.domain.model.PricingSessionParams
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase

internal object TakeoffLineItemNames {
    const val DRYWALL_SHEETS = "Drywall sheets"
    const val DRYWALL_SCREWS = "Drywall screws"
    const val JOINT_COMPOUND = "Joint compound"
    const val CONCRETE_VOLUME = "Concrete volume"
    const val MATERIAL_VOLUME = "Material volume"
    const val MATERIAL_WEIGHT = "Material weight"
    const val PAINT = "Paint"
}

internal fun Settings.defaultDrywallParams(): DrywallParams {
    return DrywallParams(
        sheetAreaSqFt = defaultDrywallSheetArea,
        wastePercent = defaultWastePercent,
        screwsPerSheet = defaultScrewsPerSheet,
        mudGallonsPer100SqFt = defaultMudGallonsPer100SqFt,
        includeCeilings = true
    )
}

internal fun Settings.defaultConcreteParams(): ConcreteParams {
    return ConcreteParams(
        thicknessFeet = 0.33,
        wastePercent = defaultWastePercent
    )
}

internal fun Settings.defaultGravelParams(): GravelParams {
    return GravelParams(
        depthFeet = 0.25,
        densityTonsPerYard = 1.4,
        wastePercent = defaultWastePercent
    )
}

internal fun Settings.defaultPaintParams(): PaintParams {
    return PaintParams(
        coverageSqFtPerGallon = defaultCoveragePerGallon,
        coats = defaultCoatsOfPaint,
        wastePercent = defaultWastePercent
    )
}

internal fun Settings.defaultPricingParams(): PricingParams = PricingParams.fromSettings(this)

internal fun DrywallSessionParams.toUiParams(): DrywallParams = DrywallParams(
    sheetAreaSqFt = sheetAreaSqFt,
    wastePercent = wastePercent,
    screwsPerSheet = screwsPerSheet,
    mudGallonsPer100SqFt = mudGallonsPer100SqFt,
    includeCeilings = includeCeilings
)

internal fun ConcreteSessionParams.toUiParams(): ConcreteParams = ConcreteParams(
    thicknessFeet = thicknessFeet,
    wastePercent = wastePercent
)

internal fun GravelSessionParams.toUiParams(): GravelParams = GravelParams(
    depthFeet = depthFeet,
    densityTonsPerYard = densityTonsPerYard,
    wastePercent = wastePercent
)

internal fun PaintSessionParams.toUiParams(): PaintParams = PaintParams(
    coverageSqFtPerGallon = coverageSqFtPerGallon,
    coats = coats,
    wastePercent = wastePercent
)

internal fun PricingSessionParams.toUiParams(): PricingParams = PricingParams(
    drywallSheetCost = drywallSheetCost,
    drywallScrewCost = drywallScrewCost,
    drywallMudCost = drywallMudCost,
    concreteYardCost = concreteYardCost,
    gravelYardCost = gravelYardCost,
    gravelTonCost = gravelTonCost,
    paintGallonCost = paintGallonCost,
    laborPercent = laborPercent,
    markupPercent = markupPercent,
    taxPercent = taxPercent
)

internal fun PricingParams.toCostingInputs(unitCostByLineName: Map<String, Double>): CostingInputs {
    return CostingInputs(
        unitCostByLineName = unitCostByLineName,
        laborPercent = laborPercent,
        markupPercent = markupPercent,
        taxPercent = taxPercent
    )
}

internal fun TakeoffScope.toTakeoffType(): TakeoffType {
    return when (this) {
        TakeoffScope.DRYWALL -> TakeoffType.DRYWALL
        TakeoffScope.CONCRETE -> TakeoffType.CONCRETE
        TakeoffScope.GRAVEL_MULCH -> TakeoffType.GRAVEL_MULCH
        TakeoffScope.PAINT -> TakeoffType.PAINT
    }
}

internal fun TakeoffType.toTakeoffScope(): TakeoffScope {
    return when (this) {
        TakeoffType.DRYWALL -> TakeoffScope.DRYWALL
        TakeoffType.CONCRETE -> TakeoffScope.CONCRETE
        TakeoffType.GRAVEL_MULCH -> TakeoffScope.GRAVEL_MULCH
        TakeoffType.PAINT -> TakeoffScope.PAINT
    }
}

internal data class TakeoffCalculationInputs(
    val drywall: DrywallParams,
    val concrete: ConcreteParams,
    val gravel: GravelParams,
    val paint: PaintParams,
    val pricing: PricingParams
)

internal fun CalculateTakeoffUseCase.calculateForType(
    project: Project,
    type: TakeoffType,
    inputs: TakeoffCalculationInputs
): TakeoffResult {
    val blueprint = project.authoritativeBlueprint()
    return when (type) {
        TakeoffType.DRYWALL -> BlueprintTakeoffCalculator.drywallTakeoff(
            document = blueprint,
            sheetAreaSqFt = inputs.drywall.sheetAreaSqFt,
            wastePercent = inputs.drywall.wastePercent,
            screwsPerSheet = inputs.drywall.screwsPerSheet,
            mudGallonsPer100SqFt = inputs.drywall.mudGallonsPer100SqFt,
            includeCeilings = inputs.drywall.includeCeilings,
            costing = inputs.pricing.toCostingInputs(
                unitCostByLineName = mapOf(
                    TakeoffLineItemNames.DRYWALL_SHEETS to inputs.pricing.drywallSheetCost,
                    TakeoffLineItemNames.DRYWALL_SCREWS to inputs.pricing.drywallScrewCost,
                    TakeoffLineItemNames.JOINT_COMPOUND to inputs.pricing.drywallMudCost
                )
            )
        )
        TakeoffType.CONCRETE -> BlueprintTakeoffCalculator.concreteTakeoff(
            document = blueprint,
            thicknessFeet = inputs.concrete.thicknessFeet,
            wastePercent = inputs.concrete.wastePercent,
            costing = inputs.pricing.toCostingInputs(
                unitCostByLineName = mapOf(
                    TakeoffLineItemNames.CONCRETE_VOLUME to inputs.pricing.concreteYardCost
                )
            )
        )
        TakeoffType.GRAVEL_MULCH -> BlueprintTakeoffCalculator.gravelMulchTakeoff(
            document = blueprint,
            depthFeet = inputs.gravel.depthFeet,
            densityTonsPerYard = inputs.gravel.densityTonsPerYard,
            wastePercent = inputs.gravel.wastePercent,
            costing = inputs.pricing.toCostingInputs(
                unitCostByLineName = mapOf(
                    TakeoffLineItemNames.MATERIAL_VOLUME to inputs.pricing.gravelYardCost,
                    TakeoffLineItemNames.MATERIAL_WEIGHT to inputs.pricing.gravelTonCost
                )
            )
        )
        TakeoffType.PAINT -> BlueprintTakeoffCalculator.paintTakeoff(
            document = blueprint,
            coverageSqFtPerGallon = inputs.paint.coverageSqFtPerGallon,
            coats = inputs.paint.coats,
            wastePercent = inputs.paint.wastePercent,
            costing = inputs.pricing.toCostingInputs(
                unitCostByLineName = mapOf(
                    TakeoffLineItemNames.PAINT to inputs.pricing.paintGallonCost
                )
            )
        )
    }
}
