package com.ahsen.hava.testutil

import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.GeoPoint
import com.ahsen.hava.domain.model.HourlyForecast
import com.ahsen.hava.domain.model.PrecipitationSlot
import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.RouteWeatherPoint
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.TravelMode
import java.time.LocalDateTime

/**
 * Testlerde tekrar tekrar kurulan nesneler. Her test sadece kendisi için önemli olan
 * değeri verir, gerisi makul bir varsayılandan gelir; böylece testler kısa ve okunur kalır.
 */
object Fixtures {

    /** Bütün testlerin "şimdi"si: 25 Ekim 2026, 14:00. Sabit saat = tekrarlanabilir test. */
    val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 25, 14, 0)

    fun route(mode: TravelMode, minutes: Int, km: Double): Route = Route(
        mode = mode,
        distanceMeters = km * 1000,
        durationSeconds = minutes * 60.0,
        path = listOf(GeoPoint(38.42, 27.14), GeoPoint(38.46, 27.21)),
    )

    fun point(
        minute: Int,
        wet: Boolean = false,
        fraction: Float = 0f,
        feelsC: Double = 18.0,
        windKmh: Double = 5.0,
        probability: Int = if (wet) 80 else 0,
        windFromDeg: Double? = null,
        headingDeg: Double? = null,
        past6hMm: Double? = null,
    ): RouteWeatherPoint = RouteWeatherPoint(
        fraction = fraction,
        minutesFromStart = minute,
        time = NOW.plusMinutes(minute.toLong()),
        temperatureC = feelsC,
        apparentTemperatureC = feelsC,
        precipitationMm = if (wet) 0.6 else 0.0,
        precipitationProbability = probability,
        windKmh = windKmh,
        weatherCode = if (wet) 61 else 1,
        windDirectionDeg = windFromDeg,
        travelBearingDeg = headingDeg,
        precipitationPast6hMm = past6hMm,
    )

    /** Hiç yağmur olmayan, tek noktalı bir rota havası. */
    fun dryWeather(totalMinutes: Int, feelsC: Double = 18.0, windKmh: Double = 5.0): RouteWeather =
        RouteWeather(listOf(point(0, feelsC = feelsC, windKmh = windKmh)), totalMinutes)

    /** İlk [wetMinutes] dakikası yağmurlu, gerisi kuru bir rota havası. */
    fun weatherWetFor(wetMinutes: Int, totalMinutes: Int): RouteWeather = when {
        wetMinutes <= 0 -> dryWeather(totalMinutes)
        wetMinutes >= totalMinutes -> RouteWeather(listOf(point(0, wet = true)), totalMinutes)
        else -> RouteWeather(listOf(point(0, wet = true), point(wetMinutes)), totalMinutes)
    }

    /** Çıkış saatine göre ne kadar yağmur yeneceğini önceden bilen sahte çizelge. */
    class FakeTimeline(
        private val totalMinutes: Int = 30,
        override val baseTime: LocalDateTime = NOW,
        private val wetMinutesAt: (offset: Int) -> Int,
    ) : RouteWeatherTimeline {
        override fun at(offsetMinutes: Int): RouteWeather = weatherWetFor(wetMinutesAt(offsetMinutes), totalMinutes)
    }

    fun current(at: LocalDateTime = NOW, temperatureC: Double = 18.0, code: Int = 1) =
        CurrentWeather(temperatureC = temperatureC, weatherCode = code, isDay = true, observedAt = at)

    fun hour(at: LocalDateTime, probability: Int = 0, uv: Double? = null) = HourlyForecast(
        time = at,
        temperatureC = 18.0,
        weatherCode = 1,
        isDay = true,
        precipitationProbability = probability,
        uvIndex = uv,
    )

    /** 15 dakikalık yağış dilimleri; listedeki her değer bir dilim (mm). */
    fun slots(vararg mm: Double, from: LocalDateTime = NOW): List<PrecipitationSlot> =
        mm.mapIndexed { i, v -> PrecipitationSlot(from.plusMinutes(15L * i), v) }

    fun forecast(
        minutely15: List<PrecipitationSlot> = emptyList(),
        hourly: List<HourlyForecast> = emptyList(),
    ) = Forecast(current = current(), hourly = hourly, daily = emptyList(), minutely15 = minutely15)
}
