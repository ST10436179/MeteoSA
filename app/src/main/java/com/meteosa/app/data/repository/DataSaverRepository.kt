package com.meteosa.app.data.repository

import android.content.Context
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.util.DataSaverDetector
import kotlinx.coroutines.flow.Flow

/**
 * Combines every signal behind Part 1's "Data-Saver Mode": the device's real network/system
 * Data Saver state, a best-effort national load-shedding check, and a manual override so the
 * feature can always be demonstrated even when neither automatic signal is currently true (e.g.
 * on an unmetered Wi-Fi network with no load-shedding happening).
 */
class DataSaverRepository(
    private val appContext: Context,
    themePreferences: ThemePreferences,
    private val loadSheddingRepository: LoadSheddingRepository
) {
    val manualOverride: Flow<Boolean> = themePreferences.forceDataSaverEnabled

    fun isRestrictedNetwork(): Boolean = DataSaverDetector.isRestrictedNetwork(appContext)

    suspend fun isLoadSheddingActive(): Boolean = loadSheddingRepository.isLoadSheddingActive()
}
