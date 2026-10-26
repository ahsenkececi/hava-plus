package com.ahsen.hava.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Bir şehrin anlık durumu + saatlik + günlük tahmini. Zamanlar şehrin yerel saatinde. */
data class Forecast(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val minutely15: List<PrecipitationSlot> = emptyList(),
) {
    /** Şu andan itibaren önümüzdeki [count] saat. */
    fun upcomingHours(count: Int = 24): List<HourlyForecast> {
        val thisHour = current.observedAt.truncatedTo(ChronoUnit.HOURS)
        return hourly.filter { !it.time.isBefore(thisHour) }.take(count)
    }
}

data class CurrentWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val observedAt: LocalDateTime,
    val apparentTemperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Double? = null,
    /** Rüzgârın estiği yön, derece (0 = kuzeyden, 90 = doğudan). */
    val windDirectionDeg: Double? = null,
    /** Deniz seviyesine indirgenmiş hava basıncı (hPa). */
    val pressureHpa: Double? = null,
    /** Görüş mesafesi (metre). */
    val visibilityMeters: Double? = null,
    /** Çiy noktası (°C): havadaki nemin yoğunlaşacağı sıcaklık; sıcaklığa yaklaştıkça hava "ağırlaşır". */
    val dewPointC: Double? = null,
    /** Gökyüzünün bulutla kaplı yüzdesi. */
    val cloudCoverPercent: Int? = null,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWeatherCode(weatherCode)
}

/** 15 dakikalık bir zaman dilimindeki yağış miktarı (mm). */
data class PrecipitationSlot(val time: LocalDateTime, val precipitationMm: Double)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipitationProbability: Int? = null,
    val uvIndex: Double? = null,
    val apparentTemperatureC: Double? = null,
    val windSpeedKmh: Double? = null,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWeatherCode(weatherCode)
}

data class DailyForecast(
    val date: LocalDate,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
    val uvIndexMax: Double? = null,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWeatherCode(weatherCode)
}
