package com.meteosa.app.ui.screens.home

import com.meteosa.app.data.remote.dto.ForecastCity
import com.meteosa.app.data.remote.dto.ForecastItem
import com.meteosa.app.data.remote.dto.ForecastResponse
import com.meteosa.app.data.remote.dto.MainWeather
import com.meteosa.app.data.remote.dto.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyForecastTest {

    // Builds an epoch-seconds timestamp for "today + dayOffset" at the given hour, using the
    // JVM's own default timezone (same one toDailyForecasts() uses internally via
    // SimpleDateFormat), so this test is deterministic no matter which timezone it runs in.
    private fun epochAt(dayOffset: Int, hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000
    }

    private fun item(dayOffset: Int, hour: Int, tempMin: Double, tempMax: Double, icon: String = "01d") =
        ForecastItem(
            dt = epochAt(dayOffset, hour),
            dtText = "day$dayOffset-$hour:00",
            main = MainWeather(temp = (tempMin + tempMax) / 2, feelsLike = tempMax, tempMin = tempMin, tempMax = tempMax, humidity = 50),
            weather = listOf(WeatherCondition(id = 800, main = "Clear", description = "clear sky", icon = icon))
        )

    @Test
    fun `groups 3-hourly entries into one card per calendar day`() {
        val response = ForecastResponse(
            city = ForecastCity(name = "Johannesburg"),
            list = listOf(
                item(dayOffset = 0, hour = 12, tempMin = 14.0, tempMax = 22.0),
                item(dayOffset = 0, hour = 6, tempMin = 12.0, tempMax = 18.0),
                item(dayOffset = 1, hour = 12, tempMin = 15.0, tempMax = 25.0)
            )
        )

        val daily = response.toDailyForecasts()

        assertEquals(2, daily.size)
        // Day 0 should take the min across all its entries (12.0) and the max across all (22.0).
        assertEquals(12, daily[0].minTemp)
        assertEquals(22, daily[0].maxTemp)
        assertEquals(15, daily[1].minTemp)
        assertEquals(25, daily[1].maxTemp)
    }

    @Test
    fun `never returns more than 5 days even with a full 40-entry forecast`() {
        val entries = (0 until 40).map { i ->
            val dayOffset = i / 8 // 8 entries per day (every 3 hours)
            item(dayOffset = dayOffset, hour = (i % 8) * 3, tempMin = 10.0, tempMax = 20.0)
        }
        val response = ForecastResponse(city = ForecastCity(name = "Durban"), list = entries)

        val daily = response.toDailyForecasts()

        assertTrue(daily.size <= 5)
    }
}
