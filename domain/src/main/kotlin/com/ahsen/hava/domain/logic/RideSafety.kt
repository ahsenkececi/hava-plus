package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.RouteWeatherPoint
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** İki tekerlekli (bisiklet, scooter) için yol boyunca bir risk. Metni ekran kurar (strings.xml). */
sealed interface RideHazard {
    /** Riskin yolun kaçıncı dakikasında başladığı. */
    val minutesFromStart: Int

    /** Yoldan yana doğru esen rüzgâr: iki tekerlekliyi savurur. */
    data class CrossWind(override val minutesFromStart: Int, val crossWindKmh: Int, val severe: Boolean) : RideHazard

    /** Uzun kuru dönemden sonraki ilk yağmur: yoldaki yağ ve toz suyla karışır, zemin en kaygan hâlindedir. */
    data class SlipperyFirstRain(override val minutesFromStart: Int) : RideHazard
}

/**
 * Yol güvenliği kuralları. Saf Kotlin: yolun gittiği yön, rüzgârın yönü ve yağış geçmişinden hesaplanır.
 *
 * Yan rüzgâr = rüzgâr hızı × |sin(rüzgâr yönü − yol yönü)|. Rüzgâr tam önden ya da arkadan
 * geliyorsa sıfır, yolu dik kesiyorsa rüzgârın tamamı.
 */
object RideSafety {

    const val CROSSWIND_CAUTION_KMH = 25.0
    const val CROSSWIND_SEVERE_KMH = 40.0

    /** Son 6 saatte bundan az yağış düştüyse yol "kuru dönemden" çıkıyor demektir. */
    const val DRY_SPELL_MM = 0.5

    /** Bir noktadaki yan rüzgâr (km/s). Yön bilgisi yoksa hesaplanamaz: null. */
    fun crossWindKmh(point: RouteWeatherPoint): Double? {
        val windFrom = point.windDirectionDeg ?: return null
        val heading = point.travelBearingDeg ?: return null
        return point.windKmh * abs(sin(Math.toRadians(windFrom - heading)))
    }

    /** Rotanın risklerini bulur: en sert yan rüzgâr noktası ve (varsa) ilk yağmurun başladığı nokta. */
    fun assess(weather: RouteWeather): List<RideHazard> = buildList {
        val worstWind = weather.points
            .mapNotNull { point -> crossWindKmh(point)?.let { point to it } }
            .maxByOrNull { it.second }
        if (worstWind != null && worstWind.second >= CROSSWIND_CAUTION_KMH) {
            add(
                RideHazard.CrossWind(
                    minutesFromStart = worstWind.first.minutesFromStart,
                    crossWindKmh = worstWind.second.roundToInt(),
                    severe = worstWind.second >= CROSSWIND_SEVERE_KMH,
                ),
            )
        }
        val firstRainAfterDrySpell = weather.points.firstOrNull { point ->
            val past = point.precipitationPast6hMm
            point.isWet && past != null && past < DRY_SPELL_MM
        }
        if (firstRainAfterDrySpell != null) add(RideHazard.SlipperyFirstRain(firstRainAfterDrySpell.minutesFromStart))
    }
}
