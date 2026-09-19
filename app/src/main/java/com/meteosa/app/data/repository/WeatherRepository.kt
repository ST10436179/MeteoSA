package com.meteosa.app.data.repository

import com.meteosa.app.BuildConfig
import com.meteosa.app.data.remote.WeatherApi
import com.meteosa.app.data.remote.dto.CurrentWeatherResponse
import com.meteosa.app.data.remote.dto.ForecastResponse

class WeatherRepository(private val api: WeatherApi) {

    suspend fun getCurrentWeather(lat: Double, lon: Double): CurrentWeatherResponse =
        api.getCurrentWeather(lat, lon, BuildConfig.OPEN_WEATHER_API_KEY)

    suspend fun getForecast(lat: Double, lon: Double): ForecastResponse =
        api.getForecast(lat, lon, BuildConfig.OPEN_WEATHER_API_KEY)
}
