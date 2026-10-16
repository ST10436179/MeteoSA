package com.meteosa.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A community report submitted while offline, queued here until WorkManager can sync it to the
 * backend. Mirrors the "Offline Sync" sequence diagram from the Part 1 design document: UI saves
 * to RoomDB with isSynced=false, WorkManager later detects network, POSTs each pending report,
 * and removes it from this table once the server confirms it.
 */
@Entity(tableName = "pending_reports")
data class PendingReportEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val reportType: String,
    val description: String,
    val createdAtLocal: Long
)
