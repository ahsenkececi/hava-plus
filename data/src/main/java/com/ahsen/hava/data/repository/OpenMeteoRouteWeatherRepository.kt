package com.ahsen.hava.data.repository

import com.ahsen.hava.data.remote.ForecastApi
import com.ahsen.hava.data.remote.ForecastResponse
import com.ahsen.hava.data.remote.apiCall
import com.ahsen.hava.domain.model.GeoPoint
import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.RouteWeatherPoint
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.repository.RouteWeatherRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [RouteWeatherRepository]'nin Open-Meteo ile çalışan hali: rota üzerindeki noktaların tahmini tek istekte. */
internal class OpenMeteoRouteWeatherRepository(
    private val api: ForecastApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RouteWeatherRepository {

    override suspend fun timelines(routes: List<Route>, now: LocalDateTime): Map<Route, RouteWeatherTimeline> =
        withContext(ioDispatcher) {
            // 1) Her rotadan örnek noktalar ve o noktaya varış dakikası
            val samples = routes.associateWith { sample(it) }
            val all = samples.values.flatten()
            if (all.isEmpty()) return@withContext emptyMap()

            // 2) Bütün noktalar TEK istekte (Open-Meteo virgülle ayrılmış koordinat listesi kabul eder)
            val lats = all.joinToString(",") { String.format(Locale.US, "%.4f", it.point.latitude) }
            val lons = all.joinToString(",") { String.format(Locale.US, "%.4f", it.point.longitude) }
            val responses = apiCall { api.getAlongRoute(latitudes = lats, longitudes = lons) }

            // 3) Yanıtlar istek sırasıyla gelir: her örneğe kendi yanıtını eşle
            var index = 0
            samples.mapValues { (route, routeSamples) ->
                val paired = routeSamples.mapNotNull { s -> responses.getOrNull(index++)?.let { s to it } }
                Timeline(route, paired, now)
            }
        }

    /** Bir rotanın noktaları + her noktanın tahmini. [at] çağrıldıkça yeniden hesaplar, ağa gitmez. */
    private inner class Timeline(
        private val route: Route,
        private val samples: List<Pair<Sample, ForecastResponse>>,
        override val baseTime: LocalDateTime,
    ) : RouteWeatherTimeline {
        override fun at(offsetMinutes: Int): RouteWeather {
            val departure = baseTime.plusMinutes(offsetMinutes.toLong())
            val points = samples.mapNotNull { (s, response) ->
                response.weatherAt(departure.plusMinutes(s.minutesFromStart.toLong()), s)
            }
            return RouteWeather(points = points, totalMinutes = route.durationMinutes)
        }
    }

    /** [bearing]: yolun bu noktadaki yönü (derece). */
    private data class Sample(val point: GeoPoint, val fraction: Float, val minutesFromStart: Int, val bearing: Double?)

    /**
     * Yol üzerinde eşit aralıklı noktalar. Süre uzadıkça nokta sayısı artar
     * (yaklaşık 10 dakikada bir), ama 2 ile 8 arasında kalır.
     */
    private fun sample(route: Route): List<Sample> {
        val path = route.path
        if (path.isEmpty()) return emptyList()
        val count = ceil(route.durationMinutes / 10.0).toInt().plus(1).coerceIn(2, 8)
        val cumulative = DoubleArray(path.size)
        for (i in 1 until path.size) cumulative[i] = cumulative[i - 1] + distanceKm(path[i - 1], path[i])
        val total = cumulative.last()
        return (0 until count).map { k ->
            val f = k.toFloat() / (count - 1)
            val target = total * f
            val i = cumulative.indexOfFirst { it >= target }.let { if (it <= 0) 0 else it }
            val point = if (i == 0) {
                path.first()
            } else {
                // İki yol noktası arasında doğrusal ara değer
                val span = (cumulative[i] - cumulative[i - 1]).takeIf { it > 0 } ?: 1.0
                val t = ((target - cumulative[i - 1]) / span).coerceIn(0.0, 1.0)
                GeoPoint(
                    latitude = path[i - 1].latitude + (path[i].latitude - path[i - 1].latitude) * t,
                    longitude = path[i - 1].longitude + (path[i].longitude - path[i - 1].longitude) * t,
                )
            }
            // Yolun bu noktadaki yönü: içinde bulunduğumuz yol parçasının başından sonuna.
            val from = path[(if (i == 0) 0 else i - 1).coerceAtMost(path.lastIndex - 1).coerceAtLeast(0)]
            val to = path[(if (i == 0) 1 else i).coerceAtMost(path.lastIndex)]
            val bearing = if (from == to) null else from.bearingTo(to)
            Sample(point, f, (route.durationMinutes * f).toInt(), bearing)
        }
    }

    private fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * 6_371.0 * asin(sqrt(h))
    }

    /** [hourIndex]'ten önceki 6 saatin toplam yağışı. Geçmiş veri yoksa (yanıt yeni başlıyorsa) null. */
    private fun pastPrecipitation(hourly: List<Double?>?, hourIndex: Int): Double? {
        if (hourly == null || hourIndex < PAST_HOURS) return null
        return (hourIndex - PAST_HOURS until hourIndex).sumOf { hourly.getOrNull(it) ?: 0.0 }
    }

    /**
     * [time] anındaki hava: 15 dakikalık veri varsa ondan (daha hassas), yoksa saatlik veriden.
     */
    private fun ForecastResponse.weatherAt(time: LocalDateTime, s: Sample): RouteWeatherPoint? {
        val h = hourly ?: return null
        val hourTimes = h.time ?: return null
        // Tahmin aralığının dışındaysa (ör. 12 saatten sonrası) uydurma: bu noktayı atla.
        val hourIndex = hourTimes.indexOf(time.truncatedTo(ChronoUnit.HOURS).toString().take(16))
            .takeIf { it >= 0 } ?: return null
        val temp = h.temperature?.getOrNull(hourIndex) ?: return null
        val code = h.weatherCode?.getOrNull(hourIndex) ?: 0

        val minutely = minutely15
        val slotIndex = minutely?.time?.indexOfFirst { slot ->
            val t = LocalDateTime.parse(slot)
            !t.isBefore(time) && Duration.between(time, t).toMinutes() < 15
        } ?: -1
        val precipitation15 = if (slotIndex >= 0) {
            minutely?.precipitation?.getOrNull(slotIndex) ?: 0.0
        } else {
            (h.precipitation?.getOrNull(hourIndex) ?: 0.0) / 4
        }

        return RouteWeatherPoint(
            fraction = s.fraction,
            minutesFromStart = s.minutesFromStart,
            time = time,
            temperatureC = temp,
            apparentTemperatureC = h.apparentTemperature?.getOrNull(hourIndex) ?: temp,
            precipitationMm = precipitation15,
            precipitationProbability = h.precipitationProbability?.getOrNull(hourIndex) ?: 0,
            windKmh = h.windSpeed?.getOrNull(hourIndex) ?: 0.0,
            weatherCode = code,
            windDirectionDeg = h.windDirection?.getOrNull(hourIndex),
            travelBearingDeg = s.bearing,
            precipitationPast6hMm = pastPrecipitation(h.precipitation, hourIndex),
        )
    }

    private companion object {
        const val PAST_HOURS = 6
    }
}
