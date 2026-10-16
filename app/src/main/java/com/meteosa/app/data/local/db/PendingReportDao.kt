package com.meteosa.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingReportDao {
    @Insert
    suspend fun insert(report: PendingReportEntity): Long

    @Query("SELECT * FROM pending_reports")
    suspend fun getAll(): List<PendingReportEntity>

    @Query("SELECT COUNT(*) FROM pending_reports")
    suspend fun count(): Int

    @Delete
    suspend fun delete(report: PendingReportEntity)
}
