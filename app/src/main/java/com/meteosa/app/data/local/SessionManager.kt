package com.meteosa.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meteosa.app.data.remote.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val points: Int
)

/**
 * Persists the logged-in user's JWT + profile in EncryptedSharedPreferences, which encrypts
 * both keys and values at rest using a key held in the Android Keystore. Satisfies the "store
 * the JWT securely" requirement from the Part 1 design doc without needing Room for it.
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "meteosa_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null
    val token: String? get() = _session.value?.token

    fun save(token: String, user: UserDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putInt(KEY_POINTS, user.points)
            .apply()
        _session.value = loadSession()
    }

    fun updatePoints(points: Int) {
        prefs.edit().putInt(KEY_POINTS, points).apply()
        _session.value = loadSession()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun loadSession(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(
            token = token,
            userId = prefs.getString(KEY_USER_ID, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            points = prefs.getInt(KEY_POINTS, 0)
        )
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_POINTS = "points"
    }
}
