package com.ahsen.hava.data.remote

import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.DailyForecast
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.HourlyForecast
import com.ahsen.hava.domain.model.PrecipitationSlot
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * /v1/forecast yanıtının ham hali.
 *
 * Servis veriyi SÜTUN biçiminde gönderir: her alan ayrı bir liste,
 * aynı index aynı saate ait:
 *   "time":           ["2026-10-25T00:00", "2026-10-25T01:00", ...]
 *   "temperature_2m": [14.2,               13.8,               ...]
 * [toForecast] bunları ekranın kullandığı satırlara çevirir.
 *
 * Alanlar nullable: JSON'da eksik gelen bir alan uygulamayı çökertmesin.
 */
internal data class ForecastResponse(
    val current: CurrentResponse? = null,
    val hourly: HourlyResponse? = null,
    val daily: DailyResponse? = null,
    @SerializedName("minutely_15") val minutely15: Minutely15Response? = null,
)

internal data class Minutely15Response(
    val time: List<String>? = null,
    val precipitation: List<Double?>? = null,
)

internal data class CurrentResponse(
    val time: String? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("is_day") val isDay: Int? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerializedName("relative_humidity_2m") val relativeHumidity: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Double? = null,
    @SerializedName("pressure_msl") val pressure: Double? = null,
    val visibility: Double? = null,
    @SerializedName("dew_point_2m") val dewPoint: Double? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
)

internal data class HourlyResponse(
    val time: List<String>? = null,
    @SerializedName("temperature_2m") val temperature: List<Double?>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int?>? = null,
    @SerializedName("is_day") val isDay: List<Int?>? = null,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @SerializedName("uv_index") val uvIndex: List<Double?>? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerializedName("wind_speed_10m") val windSpeed: List<Double?>? = null,
    /** Saatlik yağış miktarı (mm); rota boyunca hava için. */
    val precipitation: List<Double?>? = null,
    /** Rüzgârın geldiği yön (derece); yan rüzgâr hesabı için. */
    @SerializedName("wind_direction_10m") val windDirection: List<Double?>? = null,
)

internal data class DailyResponse(
    val time: List<String>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int?>? = null,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    val sunrise: List<String?>? = null,
    val sunset: List<String?>? = null,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double?>? = null,
)

/**
 * Ağ modelini ekran modeline çevirir. Zorunlu "current" bilgileri yoksa
 * IllegalStateException fırlatır; ViewModel bunu Error durumuna çevirir.
 */
internal fun ForecastResponse.toForecast(): Forecast = Forecast(
    current = toCurrentWeather(),
    hourly = hourly.toHourlyForecasts(),
    daily = daily.toDailyForecasts(),
    minutely15 = minutely15.toPrecipitationSlots(),
)

/** 15 dakikalık yağış dilimleri. Değeri olmayan dilim 0 mm sayılır. */
private fun Minutely15Response?.toPrecipitationSlots(): List<PrecipitationSlot> {
    val times = this?.time ?: return emptyList()
    return times.indices.map { i ->
        PrecipitationSlot(
            time = LocalDateTime.parse(times[i]),
            precipitationMm = precipitation?.getOrNull(i) ?: 0.0,
        )
    }
}

/** Sadece anlık hava bloğunu çevirir (tahmin ekranı ve favori kartları ortak kullanır). */
internal fun ForecastResponse.toCurrentWeather(): CurrentWeather {
    val c = current ?: error("Response has no current block")
    return CurrentWeather(
        temperatureC = c.temperature ?: error("Missing current temperature"),
        weatherCode = c.weatherCode ?: error("Missing current weather code"),
        isDay = c.isDay != 0,
        observedAt = LocalDateTime.parse(c.time ?: error("Missing current time")),
        apparentTemperatureC = c.apparentTemperature,
        humidityPercent = c.relativeHumidity,
        windSpeedKmh = c.windSpeed,
        windDirectionDeg = c.windDirection,
        pressureHpa = c.pressure,
        visibilityMeters = c.visibility,
        dewPointC = c.dewPoint,
        cloudCoverPercent = c.cloudCover,
    )
}

/** Anlık hava + bugünün en düşük/en yüksek sıcaklığı (şehir kartları). */
internal fun ForecastResponse.toSnapshot(): WeatherSnapshot {
    val today = daily.toDailyForecasts().firstOrNull()
    return WeatherSnapshot(
        current = toCurrentWeather(),
        todayMinC = today?.minTemperatureC,
        todayMaxC = today?.maxTemperatureC,
    )
}

/** Sütun -> satır. Zorunlu alanı eksik satır atlanır, ekranın tamamı çökmez. */
private fun HourlyResponse?.toHourlyForecasts(): List<HourlyForecast> {
    val times = this?.time ?: return emptyList()
    return times.indices.mapNotNull { i ->
        val temperature = temperature?.getOrNull(i) ?: return@mapNotNull null
        val code = weatherCode?.getOrNull(i) ?: return@mapNotNull null
        HourlyForecast(
            time = LocalDateTime.parse(times[i]),
            temperatureC = temperature,
            weatherCode = code,
            isDay = isDay?.getOrNull(i) != 0,
            precipitationProbability = precipitationProbability?.getOrNull(i),
            uvIndex = uvIndex?.getOrNull(i),
            apparentTemperatureC = apparentTemperature?.getOrNull(i),
            windSpeedKmh = windSpeed?.getOrNull(i),
        )
    }
}

private fun DailyResponse?.toDailyForecasts(): List<DailyForecast> {
    val times = this?.time ?: return emptyList()
    return times.indices.mapNotNull { i ->
        val max = temperatureMax?.getOrNull(i) ?: return@mapNotNull null
        val min = temperatureMin?.getOrNull(i) ?: return@mapNotNull null
        val code = weatherCode?.getOrNull(i) ?: return@mapNotNull null
        DailyForecast(
            date = LocalDate.parse(times[i]),
            minTemperatureC = min,
            maxTemperatureC = max,
            weatherCode = code,
            precipitationProbability = precipitationProbabilityMax?.getOrNull(i),
            sunrise = sunrise?.getOrNull(i)?.let { LocalDateTime.parse(it) },
            sunset = sunset?.getOrNull(i)?.let { LocalDateTime.parse(it) },
            uvIndexMax = uvIndexMax?.getOrNull(i),
        )
    }
}
