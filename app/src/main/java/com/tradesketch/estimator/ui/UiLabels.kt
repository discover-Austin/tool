package com.tradesketch.estimator.ui

import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.ui.viewmodel.TakeoffType

internal fun PrimaryTrade.displayLabel(): String = when (this) {
    PrimaryTrade.DRYWALL -> "Drywall"
    PrimaryTrade.CONCRETE -> "Concrete"
    PrimaryTrade.PAINT -> "Paint"
    PrimaryTrade.GRAVEL_MULCH -> "Gravel/Mulch"
    PrimaryTrade.MULTI -> "Multi-Trade"
}

private fun TakeoffType.toPrimaryTrade(): PrimaryTrade = when (this) {
    TakeoffType.DRYWALL -> PrimaryTrade.DRYWALL
    TakeoffType.CONCRETE -> PrimaryTrade.CONCRETE
    TakeoffType.GRAVEL_MULCH -> PrimaryTrade.GRAVEL_MULCH
    TakeoffType.PAINT -> PrimaryTrade.PAINT
}

internal val TakeoffType.displayLabel: String
    get() = toPrimaryTrade().displayLabel()

internal fun defaultTakeoffTypeForTrade(primaryTrade: PrimaryTrade): TakeoffType? =
    TakeoffType.entries.firstOrNull { it.toPrimaryTrade() == primaryTrade }

private data class TradeTemplateProfile(
    val quickStart: ProjectTemplate,
    val focused: List<ProjectTemplate>
)

private val allTradeTemplates: List<ProjectTemplate> =
    ProjectTemplate.entries.filter { it != ProjectTemplate.BLANK }

private val tradeTemplateProfiles: Map<PrimaryTrade, TradeTemplateProfile> = mapOf(
    PrimaryTrade.DRYWALL to TradeTemplateProfile(
        quickStart = ProjectTemplate.BEDROOM,
        focused = listOf(ProjectTemplate.BEDROOM)
    ),
    PrimaryTrade.CONCRETE to TradeTemplateProfile(
        quickStart = ProjectTemplate.GARAGE,
        focused = listOf(ProjectTemplate.GARAGE, ProjectTemplate.DRIVEWAY)
    ),
    PrimaryTrade.PAINT to TradeTemplateProfile(
        quickStart = ProjectTemplate.BEDROOM,
        focused = listOf(ProjectTemplate.BEDROOM)
    ),
    PrimaryTrade.GRAVEL_MULCH to TradeTemplateProfile(
        quickStart = ProjectTemplate.YARD_BED,
        focused = listOf(ProjectTemplate.YARD_BED)
    ),
    PrimaryTrade.MULTI to TradeTemplateProfile(
        quickStart = ProjectTemplate.BEDROOM,
        focused = allTradeTemplates
    )
)

internal fun PrimaryTrade.quickStartTemplate(): ProjectTemplate {
    return tradeTemplateProfiles[this]?.quickStart ?: ProjectTemplate.BEDROOM
}

internal fun PrimaryTrade.focusedTemplates(): List<ProjectTemplate> {
    return tradeTemplateProfiles[this]?.focused ?: allTradeTemplates
}
