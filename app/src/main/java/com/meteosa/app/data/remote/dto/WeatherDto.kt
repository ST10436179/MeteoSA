package com.meteosa.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Matches OpenWeatherMap's free "Current Weather Data" endpoint:
// https://api.openweathermap.org/data/2.5/weather
data class CurrentWeatherResponse(
    val name: String,
    val weather: List<WeatherCondition>,
    val main: MainWeather,
    val wind: Wind,
    val dt: Long
)

// Matches OpenWeatherMap's free "5 Day / 3 Hour Forecast" endpoint:
// https://api.openweathermap.org/data/2.5/forecast
data class ForecastResponse(
    val city: ForecastCity,
    val list: List<ForecastItem>
)

data class ForecastCity(val name: String)

data class ForecastItem(
    val dt: Long,
    @SerializedName("dt_txt") val dtText: String,
    val main: MainWeather,
    val weather: List<WeatherCondition>
)

data class MainWeather(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    val humidity: Int
)

data class WeatherCondition(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(val speed: Double)
