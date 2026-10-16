package com.meteosa.app.data.repository

import android.util.Log
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.db.PendingReportDao
import com.meteosa.app.data.local.db.PendingReportEntity
import com.meteosa.app.data.local.db.ReportCacheDao
import com.meteosa.app.data.local.db.ReportCacheEntity
import com.meteosa.app.data.remote.BackendApi
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportRequest
import com.meteosa.app.data.remote.dto.SubmitReportResponse
import java.io.IOException

private const val TAG = "ReportsRepository"

/** Result of attempting to submit a report - distinguishes a real submission from one that had
 *  to be queued locally because the device is offline right now. */
sealed class SubmitOutcome {
    data class Submitted(val response: SubmitReportResponse) : SubmitOutcome()
    data object QueuedOffline : SubmitOutcome()
}

class ReportsRepository(
    private val api: BackendApi,
    private val sessionManager: SessionManager,
    private val reportCacheDao: ReportCacheDao,
    private val pendingReportDao: PendingReportDao
) {
    /** Tries the network first; falls back to the local cache (offline mode) if that fails and
     *  there's something cached to show. A failure with nothing cached is rethrown as a normal
     *  error, rather than silently showing an empty list. */
    suspend fun getReports(lat: Double? = null, lon: Double? = null): List<ReportDto> {
        return try {
            val reports = api.getReports(lat, lon, radiusKm = if (lat != null) 50.0 else null)
            reportCacheDao.clear()
            reportCacheDao.insertAll(reports.map { it.toCacheEntity() })
            reports
        } catch (t: Throwable) {
            val cached = reportCacheDao.getAll().map { it.toDto() }
            if (cached.isNotEmpty()) {
                Log.i(TAG, "getReports: network failed, serving ${cached.size} cached report(s)")
                cached
            } else {
                throw t
            }
        }
    }

    /** Submits immediately if there's connectivity; otherwise queues the report in RoomDB for
     *  WorkManager to sync later (see ReportSyncWorker), matching the Part 1 design's offline
     *  sync sequence diagram. */
    suspend fun submitReport(
        latitude: Double,
        longitude: Double,
        reportType: String,
        description: String
    ): SubmitOutcome {
        return try {
            val response = api.submitReport(ReportRequest(latitude, longitude, reportType, description))
            sessionManager.updatePoints(response.points)
            SubmitOutcome.Submitted(response)
        } catch (t: Throwable) {
            if (t is IOException) {
                pendingReportDao.insert(
                    PendingReportEntity(
                        latitude = latitude,
                        longitude = longitude,
                        reportType = reportType,
                        description = description,
                        createdAtLocal = System.currentTimeMillis()
                    )
                )
                Log.i(TAG, "submitReport: offline, queued report for later sync")
                SubmitOutcome.QueuedOffline
            } else {
                throw t
            }
        }
    }

    /** Returns the user's updated point total after the report's points are reversed. */
    suspend fun deleteReport(reportId: String): Int {
        val response = api.deleteReport(reportId)
        sessionManager.updatePoints(response.points)
        return response.points
    }

    /** Called by ReportSyncWorker when connectivity is back. Returns how many pending reports
     *  were successfully synced; anything that still fails (e.g. still offline) stays queued. */
    suspend fun syncPendingReports(): Int {
        val pending = pendingReportDao.getAll()
        var syncedCount = 0
        for (report in pending) {
            try {
                val response = api.submitReport(
                    ReportRequest(report.latitude, report.longitude, report.reportType, report.description)
                )
                sessionManager.updatePoints(response.points)
                pendingReportDao.delete(report)
                syncedCount++
            } catch (t: Throwable) {
                Log.w(TAG, "syncPendingReports: report ${report.localId} still failed to sync", t)
            }
        }
        Log.i(TAG, "syncPendingReports: synced $syncedCount of ${pending.size} pending report(s)")
        return syncedCount
    }

    suspend fun pendingReportCount(): Int = pendingReportDao.count()
}

private fun ReportDto.toCacheEntity() = ReportCacheEntity(
    reportId = reportId,
    userId = userId,
    displayName = displayName,
    latitude = latitude,
    longitude = longitude,
    reportType = reportType,
    description = description,
    createdAt = createdAt
)

private fun ReportCacheEntity.toDto() = ReportDto(
    reportId = reportId,
    userId = userId,
    displayName = displayName,
    latitude = latitude,
    longitude = longitude,
    reportType = reportType,
    description = description,
    createdAt = createdAt
)
