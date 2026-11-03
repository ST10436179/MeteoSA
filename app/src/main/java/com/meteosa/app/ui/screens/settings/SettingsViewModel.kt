package com.meteosa.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.R
import com.meteosa.app.data.local.Session
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val session: StateFlow<Session?> = sessionManager.session

    private val _avatarUploadState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    /** Profile picture upload (Part 3 User Defined feature). */
    val avatarUploadState: StateFlow<UiState<Unit>> = _avatarUploadState.asStateFlow()

    init {
        // Pull the latest profile (including up-to-date gamification points) from the backend.
        viewModelScope.launch { runCatching { authRepository.refreshProfile() } }
    }

    fun uploadAvatar(imageBase64: String, contentType: String) {
        _avatarUploadState.value = UiState.Loading
        viewModelScope.launch {
            _avatarUploadState.value = try {
                authRepository.uploadAvatar(imageBase64, contentType)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetAvatarUploadState() {
        _avatarUploadState.value = UiState.Idle
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
enum class BadgeTier { LEGEND, VETERAN, SCOUT, ROOKIE, NONE }

fun badgeTierForPoints(points: Int): BadgeTier = when {
    points >= 500 -> BadgeTier.LEGEND
    points >= 200 -> BadgeTier.VETERAN
    points >= 50 -> BadgeTier.SCOUT
    points > 0 -> BadgeTier.ROOKIE
    else -> BadgeTier.NONE
}

/** String resource id for a badge tier's localised label. */
fun badgeLabelRes(tier: BadgeTier): Int = when (tier) {
    BadgeTier.LEGEND -> R.string.badge_legend
    BadgeTier.VETERAN -> R.string.badge_veteran
    BadgeTier.SCOUT -> R.string.badge_scout
    BadgeTier.ROOKIE -> R.string.badge_rookie
    BadgeTier.NONE -> R.string.badge_none
}
