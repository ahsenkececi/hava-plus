package com.ahsen.hava.domain.model

import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Rotanın bir noktasında, oradan geçeceğin andaki hava. */
data class RouteWeatherPoint(
    /** Yolun yüzde kaçı (0 = başlangıç, 1 = varış). */
    val fraction: Float,
    val minutesFromStart: Int,
    val time: LocalDateTime,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    /** 15 dakikalık yağış (mm). */
    val precipitationMm: Double,
    val precipitationProbability: Int,
    val windKmh: Double,
    val weatherCode: Int,
    /** Rüzgârın geldiği yön (derece). */
    val windDirectionDeg: Double? = null,
    /** Yolun bu noktada gittiği yön (derece). Yan rüzgârı bulmak için rüzgâr yönüyle karşılaştırılır. */
    val travelBearingDeg: Double? = null,
    /** Bu noktadan geçmeden önceki 6 saatte düşen yağış (mm). Kuru dönem sonrası ilk yağmuru anlamak için. */
    val precipitationPast6hMm: Double? = null,
) {
    val isWet: Boolean get() = precipitationMm >= WET_MM_PER_15 || weatherCode in RAIN_CODES

    private companion object {
        const val WET_MM_PER_15 = 0.1
        val RAIN_CODES = setOf(61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    }
}

/** Yağmurun rotadaki yeri. */
sealed interface RouteRain {
    /** Kuru. Yağış ihtimali yüksekse (%40+) [highProbability] dolu. */
    data class Dry(val highProbability: Int?) : RouteRain

    data object AllAlong : RouteRain

    data class FirstMinutes(val minutes: Int) : RouteRain

    data class LastMinutes(val minutes: Int) : RouteRain

    data class After(val startMinute: Int, val minutes: Int) : RouteRain
}

/**
 * Bir rota boyunca hava. Saf Kotlin: noktaları okur, yağmurun yolun neresinde olduğunu bulur.
 */
data class RouteWeather(val points: List<RouteWeatherPoint>, val totalMinutes: Int) {

    /**
     * Her nokta, kendisinden sonraki noktaya kadar olan yol parçasını temsil eder.
     * Islak parçaların toplam süresi = yağmura maruz kalınan süre.
     */
    val wetMinutes: Int
        get() = segments().filter { it.first.isWet }.sumOf { it.second }

    val maxWindKmh: Double get() = points.maxOfOrNull { it.windKmh } ?: 0.0
    val minApparentC: Double get() = points.minOfOrNull { it.apparentTemperatureC } ?: 0.0
    val maxPrecipitationProbability: Int get() = points.maxOfOrNull { it.precipitationProbability } ?: 0

    /** (nokta, o parçanın dakikası) çiftleri. */
    fun segments(): List<Pair<RouteWeatherPoint, Int>> =
        points.mapIndexed { i, p ->
            val nextMinute = points.getOrNull(i + 1)?.minutesFromStart ?: totalMinutes
            p to (nextMinute - p.minutesFromStart).coerceAtLeast(0)
        }.filter { it.second > 0 || points.size == 1 }

    /** Yağmurun yolun neresinde olduğu: "Son 10 dk yağmur", "Yol boyunca kuru" gibi bir cümleye dönüşür. */
    fun rain(): RouteRain {
        val segs = segments()
        val wet = wetMinutes
        return when {
            wet == 0 -> RouteRain.Dry(maxPrecipitationProbability.takeIf { it >= 40 })
            wet >= totalMinutes -> RouteRain.AllAlong
            else -> {
                val firstWet = segs.indexOfFirst { it.first.isWet }
                val lastWet = segs.indexOfLast { it.first.isWet }
                val start = segs[firstWet].first.minutesFromStart
                when {
                    start == 0 -> RouteRain.FirstMinutes(wet)
                    lastWet == segs.lastIndex -> RouteRain.LastMinutes(wet)
                    else -> RouteRain.After(startMinute = start, minutes = wet)
                }
            }
        }
    }

    /** Yol boyunca en düşük hissedilen sıcaklık, yuvarlanmış. */
    val feelsLikeC: Int get() = minApparentC.roundToInt()
}

/**
 * Bir rotanın havasını farklı çıkış saatleri için verebilen nesne.
 * (Veri katmanı bunu bir kez indirdiği tahminden üretir; her çağrıda yeni istek atılmaz.)
 */
interface RouteWeatherTimeline {
    /** Bugünden [offsetMinutes] dakika sonra yola çıkılırsa yol boyunca hava. */
    fun at(offsetMinutes: Int): RouteWeather

    /** Hesaplamanın başladığı an ("şimdi"). */
    val baseTime: LocalDateTime
}
