package com.meteosa.app.data.remote

import com.meteosa.app.data.remote.dto.CurrentWeatherResponse
import com.meteosa.app.data.remote.dto.ForecastResponse
import com.meteosa.app.data.remote.dto.OneCallResponse
import retrofit2.http.GET
import retrofit2.http.Query
/**
 * Talks to OpenWeatherMap (base URL https://api.openweathermap.org/). Current weather uses the
 * free 2.5 endpoint; the daily forecast prefers One Call 3.0 (needs that plan on the key), with
 * the 5 Day / 3 Hour endpoint as a fallback.
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

    /** One Call 3.0 daily forecast (up to 8 days). Needs the One Call subscription on the key. */
    @GET("data/3.0/onecall")
    suspend fun getDailyForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("exclude") exclude: String = "minutely,hourly,alerts"
    ): OneCallResponse
}
