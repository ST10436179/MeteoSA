package com.meteosa.app.data.repository

import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.remote.BackendApi
import com.meteosa.app.data.remote.dto.LoginRequest
import com.meteosa.app.data.remote.dto.RegisterRequest

/**
 * Note on password handling: the app sends the plaintext password to the backend over HTTPS
 * (TLS protects it in transit, same as any login form). The backend then hashes it with BCrypt
 * (see backend/src/routes/auth.js) before it ever touches the database, and the plaintext is
 * never stored or logged. This is the standard, correct place to BCrypt-hash a password -
 * hashing it on the device first would mean the server can no longer choose/verify its own
 * salt, which defeats the point of BCrypt.
 */
class AuthRepository(
    private val api: BackendApi,
    private val sessionManager: SessionManager
) {
    suspend fun register(email: String, password: String, displayName: String) {
        val response = api.register(RegisterRequest(email, password, displayName))
        sessionManager.save(response.token, response.user)
    }

    suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email, password))
        sessionManager.save(response.token, response.user)
    }

    suspend fun refreshProfile() {
        val token = sessionManager.token ?: return
        val user = api.getProfile()
        sessionManager.save(token, user)
    }

    fun logout() {
        sessionManager.clear()
    }
}
