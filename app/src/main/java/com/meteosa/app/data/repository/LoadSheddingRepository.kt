package com.meteosa.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Best-effort check against the unauthenticated status endpoint several public South African
 * load-shedding apps read from (loadshedding.eskom.co.za). No official, keyless API exists for
 * this within the project's scope, so this is the "secondary API check" Part 1's Data-Saver Mode
 * calls for. The endpoint has no documented SLA and no guarantee it stays online, so every
 * failure - timeout, malformed body, endpoint change - is treated as "not load-shedding" rather
 * than surfaced as an app error. Settings also offers a manual override for exactly the case
 * where this check can't reach the endpoint (or there's genuinely no load-shedding right now).
 */
class LoadSheddingRepository(private val client: OkHttpClient) {

    suspend fun isLoadSheddingActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://loadshedding.eskom.co.za/LoadShedding/GetStatus")
                .build()
            client.newCall(request).execute().use { response ->
                // The endpoint returns the current national stage encoded as stage + 1 as plain
                // text (e.g. "1" = Stage 0 / no load-shedding, "3" = Stage 2).
                val encodedStage = response.body?.string()?.trim()?.toIntOrNull() ?: 1
                encodedStage > 1
            }
        } catch (t: Throwable) {
            false
        }
    }
}
