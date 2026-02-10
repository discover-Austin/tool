package com.tradesketch.estimator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.tradesketch.estimator.domain.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DEFAULT_WASTE_PERCENT = doublePreferencesKey("default_waste_percent")
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val DEFAULT_DRYWALL_SHEET_AREA = doublePreferencesKey("default_drywall_sheet_area")
        private val DEFAULT_SCREWS_PER_SHEET = intPreferencesKey("default_screws_per_sheet")
        private val DEFAULT_MUD_GALLONS_PER_100_SQFT = doublePreferencesKey("default_mud_gallons_per_100_sqft")
        private val DEFAULT_COVERAGE_PER_GALLON = doublePreferencesKey("default_coverage_per_gallon")
        private val DEFAULT_COATS_OF_PAINT = intPreferencesKey("default_coats_of_paint")
    }
    
    val settings: Flow<Settings> = context.settingsDataStore.data
        .map { preferences ->
            Settings(
                defaultWastePercent = preferences[DEFAULT_WASTE_PERCENT] ?: 10.0,
                useMetric = preferences[USE_METRIC] ?: false,
                defaultDrywallSheetArea = preferences[DEFAULT_DRYWALL_SHEET_AREA] ?: 32.0,
                defaultScrewsPerSheet = preferences[DEFAULT_SCREWS_PER_SHEET] ?: 32,
                defaultMudGallonsPer100SqFt = preferences[DEFAULT_MUD_GALLONS_PER_100_SQFT] ?: 0.5,
                defaultCoveragePerGallon = preferences[DEFAULT_COVERAGE_PER_GALLON] ?: 350.0,
                defaultCoatsOfPaint = preferences[DEFAULT_COATS_OF_PAINT] ?: 2
            )
        }
    
    suspend fun saveSettings(settings: Settings) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_WASTE_PERCENT] = settings.defaultWastePercent
            preferences[USE_METRIC] = settings.useMetric
            preferences[DEFAULT_DRYWALL_SHEET_AREA] = settings.defaultDrywallSheetArea
            preferences[DEFAULT_SCREWS_PER_SHEET] = settings.defaultScrewsPerSheet
            preferences[DEFAULT_MUD_GALLONS_PER_100_SQFT] = settings.defaultMudGallonsPer100SqFt
            preferences[DEFAULT_COVERAGE_PER_GALLON] = settings.defaultCoveragePerGallon
            preferences[DEFAULT_COATS_OF_PAINT] = settings.defaultCoatsOfPaint
        }
    }
    
    suspend fun resetSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
