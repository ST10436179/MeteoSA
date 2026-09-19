package com.meteosa.app.data.remote

import com.meteosa.app.data.remote.dto.CurrentWeatherResponse
import com.meteosa.app.data.remote.dto.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Talks to OpenWeatherMap's free tier endpoints (base URL https://api.openweathermap.org/).
 * We deliberately use the classic "Current Weather" + "5 Day / 3 Hour Forecast" endpoints
 * rather than One Call 3.0, since One Call 3.0 requires enrolling a card even for its free
 * quota - the classic endpoints need only a free API key.
 */
interface WeatherApi {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): CurrentWeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
}
