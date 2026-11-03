package com.meteosa.app.data.remote

import com.meteosa.app.data.remote.dto.AuthResponse
import com.meteosa.app.data.remote.dto.AvatarUploadRequest
import com.meteosa.app.data.remote.dto.AvatarUploadResponse
import com.meteosa.app.data.remote.dto.DeleteReportResponse
import com.meteosa.app.data.remote.dto.GoogleLoginRequest
import com.meteosa.app.data.remote.dto.LoginRequest
import com.meteosa.app.data.remote.dto.RegisterRequest
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.FcmTokenRequest
import com.meteosa.app.data.remote.dto.ReportRequest
import com.meteosa.app.data.remote.dto.SubmitReportResponse
import com.meteosa.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Talks to our own custom REST API (Node.js/Express, backed by Supabase Postgres, hosted
 * on Render). See /backend in the repo root for the server implementation. Authenticated
 * calls get their "Authorization: Bearer <jwt>" header injected by AuthInterceptor.
 */
interface BackendApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginRequest): AuthResponse

    @GET("api/users/profile")
    suspend fun getProfile(): UserDto

    @PUT("api/users/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest)

    @PUT("api/users/avatar")
    suspend fun uploadAvatar(@Body body: AvatarUploadRequest): AvatarUploadResponse

    @GET("api/reports")
    suspend fun getReports(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null
    ): List<ReportDto>

    @POST("api/reports")
    suspend fun submitReport(@Body body: ReportRequest): SubmitReportResponse

    @DELETE("api/reports/{reportId}")
    suspend fun deleteReport(@Path("reportId") reportId: String): DeleteReportResponse
}
