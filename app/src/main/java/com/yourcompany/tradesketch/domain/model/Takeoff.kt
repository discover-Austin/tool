package com.yourcompany.tradesketch.domain.model

data class TakeoffLine(
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitCost: Double? = null
) {
    val extendedCost: Double? = unitCost?.let { it * quantity }
}

data class TakeoffResult(
    val items: List<TakeoffLine>,
    val totalCost: Double?
)
