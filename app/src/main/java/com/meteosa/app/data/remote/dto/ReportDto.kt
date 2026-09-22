package com.meteosa.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** The report types the backend accepts; kept in sync with backend/src/routes/reports.js */
enum class ReportType(val apiValue: String, val label: String) {
    FLOOD("flood", "Flooding"),
    HAIL("hail", "Hail"),
    WIND("wind", "Strong wind"),
    FIRE("fire", "Fire"),
    OTHER("other", "Other")
}

data class ReportRequest(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("reportType") val reportType: String,
    val description: String
)

data class ReportDto(
    @SerializedName("reportId") val reportId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("displayName") val displayName: String?,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("reportType") val reportType: String,
    val description: String,
    @SerializedName("createdAt") val createdAt: String
)

data class SubmitReportResponse(
    val report: ReportDto,
    /** Total gamification points the user has after this submission. */
    val points: Int
)

data class DeleteReportResponse(
    /** Total gamification points the user has after the report's points were reversed. */
    val points: Int
)
