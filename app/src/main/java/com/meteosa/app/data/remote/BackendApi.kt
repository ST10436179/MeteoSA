package com.meteosa.app.data.remote

import com.meteosa.app.data.remote.dto.AuthResponse
import com.meteosa.app.data.remote.dto.DeleteReportResponse
import com.meteosa.app.data.remote.dto.LoginRequest
import com.meteosa.app.data.remote.dto.RegisterRequest
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportRequest
import com.meteosa.app.data.remote.dto.SubmitReportResponse
import com.meteosa.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("api/users/profile")
    suspend fun getProfile(): UserDto

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
