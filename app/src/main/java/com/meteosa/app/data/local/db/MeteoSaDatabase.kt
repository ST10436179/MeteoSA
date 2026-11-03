package com.meteosa.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeatherCacheEntity::class,
        PendingReportEntity::class,
        ReportCacheEntity::class,
        SavedLocationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MeteoSaDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun reportCacheDao(): ReportCacheDao
    abstract fun pendingReportDao(): PendingReportDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile private var instance: MeteoSaDatabase? = null

        fun getInstance(context: Context): MeteoSaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeteoSaDatabase::class.java,
                    "meteosa.db"
                )
                    // Caches/queues only - safe to drop and recreate on a schema bump rather
                    // than writing a migration for what's purely offline-sync convenience data.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
