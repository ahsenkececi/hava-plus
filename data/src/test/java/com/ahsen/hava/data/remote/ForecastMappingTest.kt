package com.ahsen.hava.data.remote

import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Open-Meteo veriyi sütun sütun gönderir; uygulama satır satır kullanır.
 * Bu testler JSON alan adlarının ve sütun -> satır dönüşümünün doğru olduğunu kanıtlar.
 */
class ForecastMappingTest {

    private val json = """
        {
          "current": {
            "time": "2026-10-25T14:00", "temperature_2m": 18.4, "weather_code": 61, "is_day": 1,
            "apparent_temperature": 16.9, "relative_humidity_2m": 72, "wind_speed_10m": 14.0, "wind_direction_10m": 315.0,
            "pressure_msl": 1008.4, "visibility": 24140.0, "dew_point_2m": 13.2, "cloud_cover": 87
          },
          "hourly": {
            "time": ["2026-10-25T14:00", "2026-10-25T15:00", "2026-10-25T16:00"],
            "temperature_2m": [18.4, 17.9, null],
            "weather_code": [61, 3, 3],
            "is_day": [1, 1, 0],
            "precipitation_probability": [80, 40, 10],
            "uv_index": [2.5, 1.0, 0.0]
          },
          "daily": {
            "time": ["2026-10-25"],
            "weather_code": [61],
            "temperature_2m_max": [19.0],
            "temperature_2m_min": [11.5],
            "precipitation_probability_max": [85],
            "sunrise": ["2026-10-25T07:58"],
            "sunset": ["2026-10-25T18:24"],
            "uv_index_max": [3.1]
          },
          "minutely_15": {
            "time": ["2026-10-25T14:00", "2026-10-25T14:15"],
            "precipitation": [0.4, null]
          }
        }
    """.trimIndent()

    private val forecast = Gson().fromJson(json, ForecastResponse::class.java).toForecast()

    @Test
    fun `anlik hava JSON alan adlarindan okunur`() {
        with(forecast.current) {
            assertEquals(18.4, temperatureC, 0.0)
            assertEquals(61, weatherCode)
            assertTrue(isDay)
            assertEquals(LocalDateTime.of(2026, 10, 25, 14, 0), observedAt)
            assertEquals(16.9, apparentTemperatureC!!, 0.0)
            assertEquals(72, humidityPercent)
            assertEquals(315.0, windDirectionDeg!!, 0.0)
            assertEquals(1008.4, pressureHpa!!, 0.0)
            assertEquals(24140.0, visibilityMeters!!, 0.0)
            assertEquals(13.2, dewPointC!!, 0.0)
            assertEquals(87, cloudCoverPercent)
        }
    }

    @Test
    fun `saatlik sutunlar satirlara cevrilir ve eksik satir atlanir`() {
        // 16:00'da sıcaklık null geldi: o saat atlanır, ekranın geri kalanı çalışır.
        assertEquals(2, forecast.hourly.size)
        assertEquals(LocalDateTime.of(2026, 10, 25, 15, 0), forecast.hourly[1].time)
        assertEquals(40, forecast.hourly[1].precipitationProbability)
    }

    @Test
    fun `gunluk tahmin gun dogumu ve batimi ile birlikte gelir`() {
        val today = forecast.daily.single()

        assertEquals(LocalDate.of(2026, 10, 25), today.date)
        assertEquals(11.5, today.minTemperatureC, 0.0)
        assertEquals(LocalDateTime.of(2026, 10, 25, 18, 24), today.sunset)
    }

    @Test
    fun `kisa ozet bugunun en dusuk ve en yuksek sicakligini tasir`() {
        val snapshot = Gson().fromJson(json, ForecastResponse::class.java).toSnapshot()

        assertEquals(18.4, snapshot.current.temperatureC, 0.0)
        assertEquals(11.5, snapshot.todayMinC!!, 0.0)
        assertEquals(19.0, snapshot.todayMaxC!!, 0.0)
    }

    @Test
    fun `degeri olmayan 15 dakikalik dilim kuru sayilir`() {
        assertEquals(listOf(0.4, 0.0), forecast.minutely15.map { it.precipitationMm })
    }

    @Test(expected = IllegalStateException::class)
    fun `zorunlu anlik veri yoksa hata firlatilir`() {
        ForecastResponse(current = CurrentResponse(time = "2026-10-25T14:00")).toForecast()
    }

    @Test
    fun `gece bilgisi dogru okunur`() {
        val night = ForecastResponse(
            current = CurrentResponse(time = "2026-10-25T23:00", temperature = 9.0, weatherCode = 0, isDay = 0),
        ).toCurrentWeather()

        assertFalse(night.isDay)
    }
}
