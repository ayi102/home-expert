package com.facts.homedashboard.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.math.roundToInt

data class WeatherNow(
    val tempF: Int,
    val feelsF: Int,
    val humidity: Int,
    val windMph: Int,
    val code: Int,
)

data class WeatherDay(
    val date: LocalDate,
    val code: Int,
    val maxF: Int,
    val minF: Int,
)

data class WeatherData(val now: WeatherNow, val days: List<WeatherDay>)

/**
 * Weather via Open-Meteo — free, no API key, no account. Uses the tablet's
 * auto-detected location.
 */
object WeatherClient {

    suspend fun fetch(lat: Double, lng: Double): WeatherData = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=6"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)

        val cur = json.getJSONObject("current")
        val now = WeatherNow(
            tempF = cur.getDouble("temperature_2m").roundToInt(),
            feelsF = cur.getDouble("apparent_temperature").roundToInt(),
            humidity = cur.getInt("relative_humidity_2m"),
            windMph = cur.getDouble("wind_speed_10m").roundToInt(),
            code = cur.getInt("weather_code"),
        )

        val daily = json.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val days = (0 until times.length()).map { i ->
            WeatherDay(
                date = LocalDate.parse(times.getString(i)),
                code = codes.getInt(i),
                maxF = maxs.getDouble(i).roundToInt(),
                minF = mins.getDouble(i).roundToInt(),
            )
        }
        WeatherData(now, days)
    }

    /** WMO weather-code → short description. */
    fun label(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm, hail"
        else -> "—"
    }

    /** WMO weather-code → emoji glyph. */
    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}
