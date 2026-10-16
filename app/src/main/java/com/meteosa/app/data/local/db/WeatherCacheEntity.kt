package com.meteosa.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches the most recently fetched weather so Home has something to show when there's no
 * network. Matches the WeatherCache entity from the Part 1 design doc's class diagram
 * (locationId, temperature, condition, timestamp), extended with the fields Home actually
 * renders. Only the single most recent fetch is kept (fixed primary key) - this app shows one
 * location's weather at a time, so there's nothing to gain from caching more than that.
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationId: String = "last_known",
    val locationName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val conditionDescription: String,
    val conditionIcon: String,
    val conditionId: Int,
    /** JSON-serialized List<CachedDailyForecast> - see WeatherCacheMappers.kt. */
    val dailyForecastJson: String,
    val timestamp: Long
)

data class CachedDailyForecast(
    val dayLabel: String,
    val icon: String,
    val minTemp: Int,
    val maxTemp: Int
)
