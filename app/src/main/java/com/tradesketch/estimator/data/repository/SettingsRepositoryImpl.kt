package com.tradesketch.estimator.data.repository

import com.tradesketch.estimator.data.local.SettingsDataStore
import com.tradesketch.estimator.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of SettingsRepository using DataStore for persistence.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    override fun getSettings(): Flow<Settings> {
        return settingsDataStore.settings
    }
    
    override suspend fun saveSettings(settings: Settings) {
        settingsDataStore.saveSettings(settings)
    }
    
    override suspend fun resetSettings() {
        settingsDataStore.resetSettings()
    }
}
