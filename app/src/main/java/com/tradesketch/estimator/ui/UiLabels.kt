package com.tradesketch.estimator.ui

import com.tradesketch.estimator.domain.model.PrimaryTrade
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
