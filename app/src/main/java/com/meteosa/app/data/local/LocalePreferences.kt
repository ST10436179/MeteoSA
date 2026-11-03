package com.meteosa.app.data.local

import android.content.Context

/**
 * The user's chosen UI language ("en", "zu", "af"), in plain SharedPreferences rather than
 * DataStore: MainActivity.attachBaseContext needs to read it synchronously, before the
 * Activity - and any coroutine scope - exists.
 */
object LocalePreferences {
    private const val PREFS_NAME = "meteosa_locale"
    private const val KEY_LANGUAGE_TAG = "language_tag"
    const val DEFAULT_LANGUAGE_TAG = "en"

    val SUPPORTED_LANGUAGES = listOf(
        "en" to "English",
        "zu" to "isiZulu",
        "af" to "Afrikaans"
    )

    fun getLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG) ?: DEFAULT_LANGUAGE_TAG
    }

    fun setLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, tag)
            .apply()
    }
}
