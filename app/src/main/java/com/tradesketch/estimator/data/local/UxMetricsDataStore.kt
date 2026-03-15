package com.tradesketch.estimator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.domain.model.UxMetricsSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.uxMetricsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ux_metrics",
    corruptionHandler = preferencesCorruptionHandler()
)

@Singleton
class UxMetricsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val preferencesFlow = context.uxMetricsDataStore.data.recoverPreferences()

    private companion object {
        private val FIRST_ESTIMATE_COUNT = intPreferencesKey("first_estimate_count")
        private val FIRST_ESTIMATE_TOTAL_MS = longPreferencesKey("first_estimate_total_ms")
        private val TOTAL_TAP_COUNT = intPreferencesKey("total_tap_count")
        private val TAPS_BY_TASK_JSON = stringPreferencesKey("taps_by_task_json")
        private val LAST_EVENT_AT = longPreferencesKey("last_event_at")
    }

    val metrics: Flow<UxMetricsSnapshot> = preferencesFlow.map { preferences ->
        UxMetricsSnapshot(
            firstEstimateCount = preferences[FIRST_ESTIMATE_COUNT] ?: 0,
            firstEstimateTotalMs = preferences[FIRST_ESTIMATE_TOTAL_MS] ?: 0L,
            totalTapCount = preferences[TOTAL_TAP_COUNT] ?: 0,
            tapsByTask = parseTaskMap(preferences[TAPS_BY_TASK_JSON]),
            lastEventAt = preferences[LAST_EVENT_AT] ?: 0L
        )
    }

    suspend fun recordTap(task: String) {
        val normalizedTask = task.trim().ifBlank { "general" }
        context.uxMetricsDataStore.edit { preferences ->
            val taskMap = parseTaskMap(preferences[TAPS_BY_TASK_JSON]).toMutableMap()
            taskMap[normalizedTask] = (taskMap[normalizedTask] ?: 0) + 1
            preferences[TOTAL_TAP_COUNT] = (preferences[TOTAL_TAP_COUNT] ?: 0) + 1
            preferences[TAPS_BY_TASK_JSON] = gson.toJson(taskMap)
            preferences[LAST_EVENT_AT] = System.currentTimeMillis()
        }
    }

    suspend fun recordTimeToFirstEstimate(timeMs: Long) {
        if (timeMs <= 0L) return
        context.uxMetricsDataStore.edit { preferences ->
            preferences[FIRST_ESTIMATE_COUNT] = (preferences[FIRST_ESTIMATE_COUNT] ?: 0) + 1
            preferences[FIRST_ESTIMATE_TOTAL_MS] = (preferences[FIRST_ESTIMATE_TOTAL_MS] ?: 0L) + timeMs
            preferences[LAST_EVENT_AT] = System.currentTimeMillis()
        }
    }

    private fun parseTaskMap(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
        }.getOrDefault(emptyMap())
    }
}
