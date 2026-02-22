package com.tradesketch.estimator.domain.model

fun PrimaryTrade.defaultTakeoffScope(fallback: TakeoffScope = TakeoffScope.DRYWALL): TakeoffScope {
    return when (this) {
        PrimaryTrade.DRYWALL -> TakeoffScope.DRYWALL
        PrimaryTrade.CONCRETE -> TakeoffScope.CONCRETE
        PrimaryTrade.PAINT -> TakeoffScope.PAINT
        PrimaryTrade.GRAVEL_MULCH -> TakeoffScope.GRAVEL_MULCH
        PrimaryTrade.MULTI -> fallback
    }
}

fun PrimaryTrade.defaultQuickStartTemplate(): ProjectTemplate {
    return when (this) {
        PrimaryTrade.DRYWALL -> ProjectTemplate.BEDROOM
        PrimaryTrade.CONCRETE -> ProjectTemplate.GARAGE
        PrimaryTrade.PAINT -> ProjectTemplate.BEDROOM
        PrimaryTrade.GRAVEL_MULCH -> ProjectTemplate.YARD_BED
        PrimaryTrade.MULTI -> ProjectTemplate.BLANK
    }
}
