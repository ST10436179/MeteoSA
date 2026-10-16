package com.meteosa.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReportCacheDao {
    @Query("SELECT * FROM report_cache ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReportCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportCacheEntity>)

    @Query("DELETE FROM report_cache")
    suspend fun clear()
}
