package com.tradesketch.estimator.data.repository

import com.tradesketch.estimator.data.local.UxMetricsDataStore
import com.tradesketch.estimator.domain.model.UxMetricsSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UxMetricsRepositoryImpl @Inject constructor(
    private val dataStore: UxMetricsDataStore
) : UxMetricsRepository {
    override fun getMetrics(): Flow<UxMetricsSnapshot> {
        return dataStore.metrics
    }

    override suspend fun recordTap(task: String) {
        dataStore.recordTap(task)
    }

    override suspend fun recordTimeToFirstEstimate(timeMs: Long) {
        dataStore.recordTimeToFirstEstimate(timeMs)
    }
}
