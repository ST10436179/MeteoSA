package com.meteosa.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationId = 'last_known' LIMIT 1")
    suspend fun get(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: WeatherCacheEntity)
}
