package com.meteosa.app.ui.screens.home

import com.meteosa.app.data.remote.dto.ForecastResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DailyForecast(
    val dayLabel: String,
    val icon: String,
    val minTemp: Int,
    val maxTemp: Int
)

/**
 * OpenWeatherMap's free forecast endpoint returns one entry every 3 hours for 5 days (40
 * entries). We collapse that into one summary card per calendar day by taking the min/max
 * across that day's entries and the icon from the entry closest to midday.
 */
fun ForecastResponse.toDailyForecasts(): List<DailyForecast> {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val hourFormat = SimpleDateFormat("HH", Locale.getDefault())

    return list
        .groupBy { dayFormat.format(Date(it.dt * 1000)) }
        .entries
        .sortedBy { it.key }
        .take(5)
        .map { (_, entries) ->
            val middayEntry = entries.minByOrNull {
                val hour = hourFormat.format(Date(it.dt * 1000)).toInt()
                kotlin.math.abs(hour - 12)
            } ?: entries.first()
            DailyForecast(
                dayLabel = labelFormat.format(Date(entries.first().dt * 1000)),
                icon = middayEntry.weather.firstOrNull()?.icon ?: "01d",
                minTemp = entries.minOf { it.main.tempMin }.roundToInt(),
                maxTemp = entries.maxOf { it.main.tempMax }.roundToInt()
            )
        }
}

/** Maps an OpenWeatherMap icon code to a Material emoji-free label we can render with an Icon. */
fun weatherEmoji(iconCode: String): String = when {
    iconCode.startsWith("01") -> "☀️" // sun
    iconCode.startsWith("02") || iconCode.startsWith("03") -> "⛅" // sun behind cloud
    iconCode.startsWith("04") -> "☁️" // cloud
    iconCode.startsWith("09") || iconCode.startsWith("10") -> "🌧️" // rain
    iconCode.startsWith("11") -> "⛈️" // thunderstorm
    iconCode.startsWith("13") -> "❄️" // snow
    iconCode.startsWith("50") -> "🌫️" // fog
    else -> "☀️"
}
