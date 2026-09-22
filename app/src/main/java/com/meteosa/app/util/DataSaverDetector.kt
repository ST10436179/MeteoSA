package com.meteosa.app.util

import android.content.Context
import android.net.ConnectivityManager

/**
 * Real, device-level signals for Part 1's "Data-Saver Mode" - no simulation involved. Android
 * itself exposes whether the user turned on system-wide Data Saver, and whether the active
 * network is metered (mobile data, or a metered hotspot); either one means we should switch to
 * the lightweight, text-only UI the design doc describes.
 */
object DataSaverDetector {
    fun isRestrictedNetwork(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val systemDataSaverOn =
            connectivityManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        val meteredConnection = connectivityManager.isActiveNetworkMetered
        return systemDataSaverOn || meteredConnection
    }
}
