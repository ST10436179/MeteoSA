package com.meteosa.app.data.repository

import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.remote.BackendApi
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportRequest
import com.meteosa.app.data.remote.dto.SubmitReportResponse

class ReportsRepository(
    private val api: BackendApi,
    private val sessionManager: SessionManager
) {
    suspend fun getReports(lat: Double? = null, lon: Double? = null): List<ReportDto> =
        api.getReports(lat, lon, radiusKm = if (lat != null) 50.0 else null)

    suspend fun submitReport(
        latitude: Double,
        longitude: Double,
        reportType: String,
        description: String
    ): SubmitReportResponse {
        val response = api.submitReport(ReportRequest(latitude, longitude, reportType, description))
        sessionManager.updatePoints(response.points)
        return response
    }
}
