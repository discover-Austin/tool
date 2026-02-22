package com.tradesketch.estimator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.tradesketch.estimator.domain.model.PrimaryTrade
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
        private val PRIMARY_TRADE = stringPreferencesKey("primary_trade")
        private val SIMPLIFIED_HOME = booleanPreferencesKey("simplified_home")
        private val CALM_MODE_ENABLED = booleanPreferencesKey("calm_mode_enabled")
        private val WORKFLOW_AIDS_ENABLED = booleanPreferencesKey("workflow_aids_enabled")
        private val REDUCED_MOTION_ENABLED = booleanPreferencesKey("reduced_motion_enabled")
        private val FIRST_RUN = booleanPreferencesKey("first_run")
        private val HAS_COMPLETED_TRADE_ONBOARDING = booleanPreferencesKey("has_completed_trade_onboarding")
        private val DEFAULT_WASTE_PERCENT = doublePreferencesKey("default_waste_percent")
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val DEFAULT_DRYWALL_SHEET_AREA = doublePreferencesKey("default_drywall_sheet_area")
        private val DEFAULT_SCREWS_PER_SHEET = intPreferencesKey("default_screws_per_sheet")
        private val DEFAULT_MUD_GALLONS_PER_100_SQFT = doublePreferencesKey("default_mud_gallons_per_100_sqft")
        private val DEFAULT_COVERAGE_PER_GALLON = doublePreferencesKey("default_coverage_per_gallon")
        private val DEFAULT_COATS_OF_PAINT = intPreferencesKey("default_coats_of_paint")
        private val DRYWALL_SHEET_UNIT_COST = doublePreferencesKey("drywall_sheet_unit_cost")
        private val DRYWALL_SCREW_UNIT_COST = doublePreferencesKey("drywall_screw_unit_cost")
        private val DRYWALL_MUD_UNIT_COST = doublePreferencesKey("drywall_mud_unit_cost")
        private val CONCRETE_YARD_UNIT_COST = doublePreferencesKey("concrete_yard_unit_cost")
        private val GRAVEL_YARD_UNIT_COST = doublePreferencesKey("gravel_yard_unit_cost")
        private val GRAVEL_TON_UNIT_COST = doublePreferencesKey("gravel_ton_unit_cost")
        private val PAINT_GALLON_UNIT_COST = doublePreferencesKey("paint_gallon_unit_cost")
        private val LABOR_PERCENT = doublePreferencesKey("labor_percent")
        private val MARKUP_PERCENT = doublePreferencesKey("markup_percent")
        private val TAX_PERCENT = doublePreferencesKey("tax_percent")
        private val BUSINESS_NAME = stringPreferencesKey("business_name")
        private val BUSINESS_PHONE = stringPreferencesKey("business_phone")
        private val BUSINESS_EMAIL = stringPreferencesKey("business_email")
        private val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
        private val BUSINESS_LICENSE = stringPreferencesKey("business_license")
    }

    val settings: Flow<Settings> = context.settingsDataStore.data
        .map { preferences ->
            val defaults = Settings.DEFAULT
            val storedTrade = preferences[PRIMARY_TRADE]
            val primaryTrade = storedTrade
                ?.let { raw -> runCatching { PrimaryTrade.valueOf(raw) }.getOrNull() }
                ?: defaults.primaryTrade
            Settings(
                primaryTrade = primaryTrade,
                simplifiedHome = preferences[SIMPLIFIED_HOME] ?: defaults.simplifiedHome,
                calmModeEnabled = preferences[CALM_MODE_ENABLED] ?: defaults.calmModeEnabled,
                workflowAidsEnabled = preferences[WORKFLOW_AIDS_ENABLED] ?: defaults.workflowAidsEnabled,
                reducedMotionEnabled = preferences[REDUCED_MOTION_ENABLED] ?: defaults.reducedMotionEnabled,
                firstRun = preferences[FIRST_RUN] ?: defaults.firstRun,
                hasCompletedTradeOnboarding = preferences[HAS_COMPLETED_TRADE_ONBOARDING]
                    ?: defaults.hasCompletedTradeOnboarding,
                defaultWastePercent = preferences[DEFAULT_WASTE_PERCENT] ?: defaults.defaultWastePercent,
                useMetric = preferences[USE_METRIC] ?: defaults.useMetric,
                defaultDrywallSheetArea = preferences[DEFAULT_DRYWALL_SHEET_AREA]
                    ?: defaults.defaultDrywallSheetArea,
                defaultScrewsPerSheet = preferences[DEFAULT_SCREWS_PER_SHEET]
                    ?: defaults.defaultScrewsPerSheet,
                defaultMudGallonsPer100SqFt = preferences[DEFAULT_MUD_GALLONS_PER_100_SQFT]
                    ?: defaults.defaultMudGallonsPer100SqFt,
                defaultCoveragePerGallon = preferences[DEFAULT_COVERAGE_PER_GALLON]
                    ?: defaults.defaultCoveragePerGallon,
                defaultCoatsOfPaint = preferences[DEFAULT_COATS_OF_PAINT] ?: defaults.defaultCoatsOfPaint,
                drywallSheetUnitCost = preferences[DRYWALL_SHEET_UNIT_COST] ?: defaults.drywallSheetUnitCost,
                drywallScrewUnitCost = preferences[DRYWALL_SCREW_UNIT_COST] ?: defaults.drywallScrewUnitCost,
                drywallMudUnitCost = preferences[DRYWALL_MUD_UNIT_COST] ?: defaults.drywallMudUnitCost,
                concreteYardUnitCost = preferences[CONCRETE_YARD_UNIT_COST] ?: defaults.concreteYardUnitCost,
                gravelYardUnitCost = preferences[GRAVEL_YARD_UNIT_COST] ?: defaults.gravelYardUnitCost,
                gravelTonUnitCost = preferences[GRAVEL_TON_UNIT_COST] ?: defaults.gravelTonUnitCost,
                paintGallonUnitCost = preferences[PAINT_GALLON_UNIT_COST] ?: defaults.paintGallonUnitCost,
                laborPercent = preferences[LABOR_PERCENT] ?: defaults.laborPercent,
                markupPercent = preferences[MARKUP_PERCENT] ?: defaults.markupPercent,
                taxPercent = preferences[TAX_PERCENT] ?: defaults.taxPercent,
                businessName = preferences[BUSINESS_NAME] ?: defaults.businessName,
                businessPhone = preferences[BUSINESS_PHONE] ?: defaults.businessPhone,
                businessEmail = preferences[BUSINESS_EMAIL] ?: defaults.businessEmail,
                businessAddress = preferences[BUSINESS_ADDRESS] ?: defaults.businessAddress,
                businessLicense = preferences[BUSINESS_LICENSE] ?: defaults.businessLicense
            )
        }

    suspend fun saveSettings(settings: Settings) {
        context.settingsDataStore.edit { preferences ->
            preferences[PRIMARY_TRADE] = settings.primaryTrade.name
            preferences[SIMPLIFIED_HOME] = settings.simplifiedHome
            preferences[CALM_MODE_ENABLED] = settings.calmModeEnabled
            preferences[WORKFLOW_AIDS_ENABLED] = settings.workflowAidsEnabled
            preferences[REDUCED_MOTION_ENABLED] = settings.reducedMotionEnabled
            preferences[FIRST_RUN] = settings.firstRun
            preferences[HAS_COMPLETED_TRADE_ONBOARDING] = settings.hasCompletedTradeOnboarding
            preferences[DEFAULT_WASTE_PERCENT] = settings.defaultWastePercent
            preferences[USE_METRIC] = settings.useMetric
            preferences[DEFAULT_DRYWALL_SHEET_AREA] = settings.defaultDrywallSheetArea
            preferences[DEFAULT_SCREWS_PER_SHEET] = settings.defaultScrewsPerSheet
            preferences[DEFAULT_MUD_GALLONS_PER_100_SQFT] = settings.defaultMudGallonsPer100SqFt
            preferences[DEFAULT_COVERAGE_PER_GALLON] = settings.defaultCoveragePerGallon
            preferences[DEFAULT_COATS_OF_PAINT] = settings.defaultCoatsOfPaint
            preferences[DRYWALL_SHEET_UNIT_COST] = settings.drywallSheetUnitCost
            preferences[DRYWALL_SCREW_UNIT_COST] = settings.drywallScrewUnitCost
            preferences[DRYWALL_MUD_UNIT_COST] = settings.drywallMudUnitCost
            preferences[CONCRETE_YARD_UNIT_COST] = settings.concreteYardUnitCost
            preferences[GRAVEL_YARD_UNIT_COST] = settings.gravelYardUnitCost
            preferences[GRAVEL_TON_UNIT_COST] = settings.gravelTonUnitCost
            preferences[PAINT_GALLON_UNIT_COST] = settings.paintGallonUnitCost
            preferences[LABOR_PERCENT] = settings.laborPercent
            preferences[MARKUP_PERCENT] = settings.markupPercent
            preferences[TAX_PERCENT] = settings.taxPercent
            preferences[BUSINESS_NAME] = settings.businessName
            preferences[BUSINESS_PHONE] = settings.businessPhone
            preferences[BUSINESS_EMAIL] = settings.businessEmail
            preferences[BUSINESS_ADDRESS] = settings.businessAddress
            preferences[BUSINESS_LICENSE] = settings.businessLicense
        }
    }

    suspend fun resetSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
