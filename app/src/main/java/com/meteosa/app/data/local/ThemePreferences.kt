package com.meteosa.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meteosa_settings")

/** User-adjustable app settings that don't belong on the backend (device-local UI prefs). */
class ThemePreferences(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SEVERE_ALERTS = booleanPreferencesKey("severe_alerts_enabled")
        val DAILY_FORECAST_NOTIFS = booleanPreferencesKey("daily_forecast_notifs_enabled")
        val FORCE_DATA_SAVER = booleanPreferencesKey("force_data_saver_enabled")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: false }
    val severeAlertsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SEVERE_ALERTS] ?: true }
    val dailyForecastNotifsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_FORECAST_NOTIFS] ?: true }
    /** Manual override for Data-Saver Mode, used when neither automatic signal is currently true. */
    val forceDataSaverEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.FORCE_DATA_SAVER] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setSevereAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEVERE_ALERTS] = enabled }
    }

    suspend fun setDailyForecastNotifsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_FORECAST_NOTIFS] = enabled }
    }

    suspend fun setForceDataSaverEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_DATA_SAVER] = enabled }
    }
}
