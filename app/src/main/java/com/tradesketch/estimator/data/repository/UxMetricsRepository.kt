package com.tradesketch.estimator.data.repository

import com.tradesketch.estimator.domain.model.UxMetricsSnapshot
import kotlinx.coroutines.flow.Flow

interface UxMetricsRepository {
    fun getMetrics(): Flow<UxMetricsSnapshot>

    suspend fun recordTap(task: String)

    suspend fun recordTimeToFirstEstimate(timeMs: Long)
}
