package com.meteosa.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A locally cached copy of a community report, so the Reports feed still has content offline. */
@Entity(tableName = "report_cache")
data class ReportCacheEntity(
    @PrimaryKey val reportId: String,
    val userId: String,
    val displayName: String?,
    val latitude: Double,
    val longitude: Double,
    val reportType: String,
    val description: String,
    val createdAt: String
)
