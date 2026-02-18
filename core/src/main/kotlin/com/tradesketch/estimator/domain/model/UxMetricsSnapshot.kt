package com.tradesketch.estimator.domain.model

data class UxMetricsSnapshot(
    val firstEstimateCount: Int = 0,
    val firstEstimateTotalMs: Long = 0L,
    val totalTapCount: Int = 0,
    val backtrackCount: Int = 0,
    val tapsByTask: Map<String, Int> = emptyMap(),
    val lastEventAt: Long = 0L
) {
    val averageFirstEstimateSeconds: Double
        get() = if (firstEstimateCount <= 0) 0.0 else {
            (firstEstimateTotalMs.toDouble() / firstEstimateCount.toDouble()) / 1000.0
        }
}
