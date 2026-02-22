package com.tradesketch.estimator.data.repository

import com.tradesketch.estimator.domain.model.Settings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for application settings.
 */
interface SettingsRepository {
    /**
     * Get settings as a Flow for reactive updates.
     */
    fun getSettings(): Flow<Settings>

    /**
     * Save settings.
     */
    suspend fun saveSettings(settings: Settings)

    /**
     * Reset settings to defaults.
     */
    suspend fun resetSettings()
}
