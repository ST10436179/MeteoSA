package com.meteosa.app.data.remote

import com.meteosa.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the stored JWT (if any) as a Bearer token to every request to our backend. */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.token
        val request = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
