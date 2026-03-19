package com.tradesketch.estimator.domain.model

data class TakeoffLine(
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitCost: Double? = null
) {
    val extendedCost: Double? = unitCost?.let { it * quantity }
}

data class CostingInputs(
    val unitCostByLineName: Map<String, Double> = emptyMap(),
    val laborPercent: Double = 0.0,
    val markupPercent: Double = 0.0,
    val taxPercent: Double = 0.0
) {
    companion object {
        val NONE = CostingInputs()
    }
}

data class TakeoffTrace(
    val metric: String,
    val value: Double,
    val unit: String,
    val roomId: String? = null,
    val wallId: String? = null,
    val openingId: String? = null
)

data class TakeoffResult(
    val items: List<TakeoffLine>,
    val totalCost: Double?,
    val materialSubtotal: Double? = null,
    val laborCost: Double? = null,
    val markupCost: Double? = null,
    val taxCost: Double? = null,
    val traces: List<TakeoffTrace> = emptyList()
)

fun TakeoffResult.nonZeroItems(): List<TakeoffLine> = items.filter { it.quantity > 0.0 }

fun TakeoffResult.hasMeasuredQuantities(): Boolean = items.any { it.quantity > 0.0 }
