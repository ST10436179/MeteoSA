package com.meteosa.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.meteosa.app.MeteoSaApplication
import java.util.concurrent.TimeUnit

private const val TAG = "ReportSyncWorker"
private const val PERIODIC_WORK_NAME = "report-sync-periodic"
private const val ONE_TIME_WORK_NAME = "report-sync-now"

/**
 * Background half of the Part 1 "Offline Sync" design: once the device has a network
 * connection again, this uploads any community reports that were queued locally in RoomDB
 * while offline (see ReportsRepository.submitReport / PendingReportEntity).
 */
class ReportSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportsRepository = (applicationContext as MeteoSaApplication).container.reportsRepository
        return try {
            val syncedCount = reportsRepository.syncPendingReports()
            Log.i(TAG, "doWork: sync pass complete, $syncedCount report(s) uploaded")
            Result.success()
        } catch (t: Throwable) {
            Log.w(TAG, "doWork: sync pass failed, will retry on the next scheduled run", t)
            Result.retry()
        }
    }

    companion object {
        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Safety-net background sync: WorkManager's minimum periodic interval is 15 minutes. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReportSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Triggered immediately (e.g. when the Reports screen is opened) so a sync doesn't
         *  have to wait for the next 15-minute periodic window to demonstrate reconnecting. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReportSyncWorker>()
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
