package com.meteosa.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.data.local.Session
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val session: StateFlow<Session?> = sessionManager.session

    init {
        // Pull the latest profile (including up-to-date gamification points) from the backend.
        viewModelScope.launch { runCatching { authRepository.refreshProfile() } }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkTheme(enabled) }
    }

    fun setSevereAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setSevereAlertsEnabled(enabled) }
    }

    fun setDailyForecastNotifsEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDailyForecastNotifsEnabled(enabled) }
    }

    fun setForceDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setForceDataSaverEnabled(enabled) }
    }

    fun logout() {
        authRepository.logout()
    }
}

/** Simple "Storm Chaser" badge tiers based on accumulated community-report points. */
fun badgeForPoints(points: Int): String = when {
    points >= 500 -> "Storm Chaser: Legend"
    points >= 200 -> "Storm Chaser: Veteran"
    points >= 50 -> "Storm Chaser: Scout"
    points > 0 -> "Storm Chaser: Rookie"
    else -> "Not yet a Storm Chaser"
}
