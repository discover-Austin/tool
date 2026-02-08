package com.yourcompany.tradesketch.data.repository

import com.yourcompany.tradesketch.data.local.SettingsDataStore
import com.yourcompany.tradesketch.domain.model.Settings
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
