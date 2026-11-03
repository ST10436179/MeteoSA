package com.meteosa.app.data.repository

import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.remote.BackendApi
import com.meteosa.app.data.remote.dto.FcmTokenRequest

/** Push notifications (Part 3): uploads the device's current FCM token to the backend so it can
 *  target this device with real-time alerts (see backend/src/push/sendPush.js). */
class NotificationsRepository(
    private val api: BackendApi,
    private val sessionManager: SessionManager
) {
    suspend fun updateFcmToken(token: String) {
        if (sessionManager.token == null) return
        api.updateFcmToken(FcmTokenRequest(token))
    }
}
