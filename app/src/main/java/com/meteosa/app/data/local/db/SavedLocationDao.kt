package com.meteosa.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SavedLocationEntity>>

    @Insert
    suspend fun insert(location: SavedLocationEntity): Long

    @Delete
    suspend fun delete(location: SavedLocationEntity)
}
