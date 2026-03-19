package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.CeilingSpec
import com.tradesketch.estimator.domain.model.ConcreteSessionParams
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.DrywallSessionParams
import com.tradesketch.estimator.domain.model.GravelSessionParams
import com.tradesketch.estimator.domain.model.ManualTakeoffSessionParams
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.PaintSessionParams
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.PricingSessionParams
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import kotlin.math.sqrt

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
    val defaults = DrywallSessionParams()
    return DrywallParams(
        sheetAreaSqFt = defaultDrywallSheetArea,
        wastePercent = defaultWastePercent,
        screwsPerSheet = defaultScrewsPerSheet,
        mudGallonsPer100SqFt = defaultMudGallonsPer100SqFt,
        includeCeilings = defaults.includeCeilings
    )
}

internal fun Settings.defaultConcreteParams(): ConcreteParams {
    val defaults = ConcreteSessionParams()
    return ConcreteParams(
        thicknessFeet = defaults.thicknessFeet,
        wastePercent = defaultWastePercent
    )
}

internal fun Settings.defaultGravelParams(): GravelParams {
    val defaults = GravelSessionParams()
    return GravelParams(
        depthFeet = defaults.depthFeet,
        densityTonsPerYard = defaults.densityTonsPerYard,
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

internal fun buildTakeoffInputs(
    project: Project,
    settings: Settings
): TakeoffCalculationInputs {
    val persistedSession = project.takeoffSession.takeIf { it != ProjectTakeoffSession() }
    return TakeoffCalculationInputs(
        drywall = persistedSession?.drywall?.toUiParams() ?: settings.defaultDrywallParams(),
        concrete = persistedSession?.concrete?.toUiParams() ?: settings.defaultConcreteParams(),
        gravel = persistedSession?.gravel?.toUiParams() ?: settings.defaultGravelParams(),
        paint = persistedSession?.paint?.toUiParams() ?: settings.defaultPaintParams(),
        pricing = persistedSession?.pricing?.toUiParams() ?: settings.defaultPricingParams()
    )
}

internal fun projectBlueprintForType(
    project: Project,
    type: TakeoffType,
    session: ProjectTakeoffSession = project.takeoffSession
): BlueprintDocument {
    return if (session.inputMode == TakeoffInputMode.MANUAL) {
        manualBlueprintForType(
            projectId = project.id,
            type = type,
            manualParams = session.manual,
            includeDrywallCeilings = session.drywall.includeCeilings
        )
    } else {
        project.authoritativeBlueprint().scopedToTakeoffScope(type.toTakeoffScope())
    }
}

internal fun CalculateTakeoffUseCase.calculateForType(
    project: Project,
    type: TakeoffType,
    inputs: TakeoffCalculationInputs,
    sessionOverride: ProjectTakeoffSession? = null
): TakeoffResult {
    val activeSession = sessionOverride ?: project.takeoffSession
    val blueprint = projectBlueprintForType(
        project = project,
        type = type,
        session = activeSession
    )
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

private fun manualBlueprintForType(
    projectId: String,
    type: TakeoffType,
    manualParams: ManualTakeoffSessionParams,
    includeDrywallCeilings: Boolean
): BlueprintDocument {
    val walls = mutableListOf<WallSegment>()
    val rooms = mutableListOf<Room>()

    when (type) {
        TakeoffType.DRYWALL -> {
            manualWallFromArea(
                id = "manual-drywall-wall",
                areaSqFt = manualParams.drywallWallAreaSqFt,
                scope = type.toTakeoffScope()
            )?.let(walls::add)

            if (includeDrywallCeilings) {
                manualSquareRoomFromArea(
                    id = "manual-drywall-ceiling",
                    areaSqFt = manualParams.drywallCeilingAreaSqFt,
                    ceilingEnabled = true,
                    scope = type.toTakeoffScope()
                )?.let(rooms::add)
            }
        }

        TakeoffType.CONCRETE -> {
            manualSquareRoomFromArea(
                id = "manual-concrete-area",
                areaSqFt = manualParams.concreteAreaSqFt,
                ceilingEnabled = false,
                scope = type.toTakeoffScope()
            )?.let(rooms::add)
        }

        TakeoffType.GRAVEL_MULCH -> {
            manualSquareRoomFromArea(
                id = "manual-gravel-area",
                areaSqFt = manualParams.gravelAreaSqFt,
                ceilingEnabled = false,
                scope = type.toTakeoffScope()
            )?.let(rooms::add)
        }

        TakeoffType.PAINT -> {
            manualWallFromArea(
                id = "manual-paint-wall",
                areaSqFt = manualParams.paintAreaSqFt,
                scope = type.toTakeoffScope()
            )?.let(walls::add)
        }
    }

    return BlueprintDocument(
        projectId = projectId,
        walls = walls,
        rooms = rooms
    )
}

private fun manualWallFromArea(
    id: String,
    areaSqFt: Double,
    scope: TakeoffScope
): WallSegment? {
    val normalizedArea = areaSqFt.coerceAtLeast(0.0)
    if (normalizedArea <= 0.0) return null

    val wallLengthMm = Millimeters.fromFeet(normalizedArea).value.coerceAtLeast(1L)
    return WallSegment(
        id = id,
        start = PointMm(x = 0L, y = 0L),
        end = PointMm(x = wallLengthMm, y = 0L),
        height = Millimeters.fromFeet(1.0),
        tags = setOf("manual", scope.tradeScopeTag())
    )
}

private fun manualSquareRoomFromArea(
    id: String,
    areaSqFt: Double,
    ceilingEnabled: Boolean,
    scope: TakeoffScope
): Room? {
    val normalizedArea = areaSqFt.coerceAtLeast(0.0)
    if (normalizedArea <= 0.0) return null

    val sideLengthFeet = sqrt(normalizedArea)
    val sideLengthMm = Millimeters.fromFeet(sideLengthFeet).value.coerceAtLeast(1L)
    val polygon = listOf(
        PointMm(0L, 0L),
        PointMm(sideLengthMm, 0L),
        PointMm(sideLengthMm, sideLengthMm),
        PointMm(0L, sideLengthMm)
    )

    return Room(
        id = id,
        name = "Manual Area",
        polygon = polygon,
        tags = setOf("manual", scope.tradeScopeTag()),
        ceiling = CeilingSpec(
            enabled = ceilingEnabled,
            height = Millimeters.fromFeet(1.0)
        )
    )
}
