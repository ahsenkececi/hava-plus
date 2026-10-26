package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import kotlin.math.roundToInt

/** Maliyet kademesi: rakam değil, çünkü yakıt ve bilet fiyatı şehre ve aya göre değişir. */
enum class CostLevel(val score: Int) {
    Free(100),
    Ticket(70),
    Fuel(35),
}

/** Bir seçeneğin puanını açıklayan kısa gerekçe. Cümleyi ekran kurar. */
sealed interface TripReason {
    /** Seçenekler arasında en kısa süre. */
    data object Fastest : TripReason

    /** En hızlısından [minutes] dakika uzun. */
    data class SlowerBy(val minutes: Int) : TripReason

    /** Yürürken/pedal çevirirken [minutes] dakika yağmurda kalınır. */
    data class WetFor(val minutes: Int) : TripReason

    /** Yol boyunca yağmur yok. */
    data object DryAllAlong : TripReason

    /** Bisiklette zorlayan rüzgâr. */
    data class StrongWind(val kmh: Int) : TripReason

    data class Cold(val feelsLikeC: Int) : TripReason

    data class Hot(val feelsLikeC: Int) : TripReason

    data object LongWalk : TripReason

    data object LongRide : TripReason

    /** Arabada yağmurdan etkilenilmez. */
    data object ShelteredFromRain : TripReason

    /** Araca yürüme + park için eklenen pay (trafik hariç). */
    data class CarOverhead(val minutes: Int) : TripReason

    /** Toplu taşıma süresi tahmin: durağa yürüme ve bekleme dahil. */
    data object TransitEstimate : TripReason

    /** Durak yolunda [minutes] dakika yağmur. 0 ise durak yolu kuru. */
    data class TransitWalkWet(val minutes: Int) : TripReason
}

/** Önerinin tek cümlelik özeti. Cümleyi ekran kurar. */
sealed interface TripHeadline {
    data object NoOptions : TripHeadline

    /** Önerilen hem en hızlısı hem de kuru. */
    data class FastestAndComfortable(val mode: TravelMode) : TripHeadline

    /** Önerilen en hızlısı ama [wetMinutes] dk yağmur var. */
    data class FastestButWet(val mode: TravelMode, val wetMinutes: Int) : TripHeadline

    /** Önerilen [extraMinutes] dk daha uzun ama en hızlısıyla [quickestWetMinutes] dk ıslanılırdı. */
    data class SlowerButDrier(
        val mode: TravelMode,
        val extraMinutes: Int,
        val quickest: TravelMode,
        val quickestWetMinutes: Int,
    ) : TripHeadline

    /** Önerilen [extraMinutes] dk daha uzun ama süre, hava ve maliyet dengesinde en iyisi. */
    data class SlowerButBalanced(val mode: TravelMode, val extraMinutes: Int) : TripHeadline
}

/** Bir ulaşım seçeneğinin değerlendirmesi. */
data class ModeOption(
    val mode: TravelMode,
    val durationMinutes: Int,
    val distanceKm: Double,
    val score: Int,
    val cost: CostLevel,
    /** Puanı açıklayan kısa gerekçeler (en hızlı mı, ıslanır mı, rüzgâr...). */
    val reasons: List<TripReason>,
    /** Gerçek rota değil, tahmin mi? (Toplu taşıma) */
    val isEstimate: Boolean,
    /** Yol boyunca hava (toplu taşımada araba güzergâhınınki). */
    val weather: RouteWeather?,
    /** Yağmurda açıkta geçirilecek tahmini dakika. */
    val wetExposureMinutes: Int,
    /** Yol güvenliği riskleri (sadece bisiklet için hesaplanır). */
    val hazards: List<RideHazard> = emptyList(),
)

data class TripRecommendation(
    /** Puana göre sıralı. */
    val options: List<ModeOption>,
    /** Önerinin özeti: "Bisiklet 6 dk uzun ama yağmura hiç yakalanmazsın." gibi bir cümleye dönüşür. */
    val headline: TripHeadline,
) {
    val best: ModeOption? get() = options.firstOrNull()
}

/**
 * Açıklanabilir karar motoru: her seçeneğe puan verir ve NEDENİNİ söyler.
 *
 * Puan (0–100) = süre %45 + hava %35 + maliyet %10 + efor %10.
 * Bilerek basit, ağırlıklı ve kural tabanlı: her sayının bir anlamı var, kullanıcıya açıklanabilir.
 * Saf Kotlin: ağa ya da ekrana bağımlı değil, test etmesi kolay.
 */
object TripScorer {

    private const val TRANSIT_ACCESS_MINUTES = 10 // durağa ve duraktan yürüme (toplam)
    private const val TRANSIT_WAIT_MINUTES = 6
    private const val TRANSIT_SPEED_FACTOR = 1.5 // aynı yolu toplu taşıma arabadan ~1,5 kat yavaş gider
    private const val MIN_TRANSIT_KM = 1.5
    private const val CAR_OVERHEAD_MINUTES = 5 // araca yürüme + park yeri bulma

    fun recommend(trip: TripRoutes, weather: Map<TravelMode, RouteWeather>): TripRecommendation {
        val candidates = buildList {
            trip.best(TravelMode.Walk)?.let { add(walkOrBike(it, weather[TravelMode.Walk])) }
            trip.best(TravelMode.Bike)?.let { add(walkOrBike(it, weather[TravelMode.Bike])) }
            trip.best(TravelMode.Car)?.let { car ->
                add(car(car, weather[TravelMode.Car]))
                if (car.distanceKm >= MIN_TRANSIT_KM) add(transit(car, weather[TravelMode.Car]))
            }
        }
        if (candidates.isEmpty()) return TripRecommendation(emptyList(), TripHeadline.NoOptions)

        val fastest = candidates.minOf { it.durationMinutes }
        val scored = candidates.map { c -> c.copy(score = scoreOf(c, fastest), reasons = timeReason(c, fastest) + c.reasons) }
            .sortedByDescending { it.score }
        return TripRecommendation(scored, headline(scored, fastest))
    }

    // ------------------------------------------------------------------ seçenekler

    private fun walkOrBike(route: Route, weather: RouteWeather?): ModeOption {
        val reasons = mutableListOf<TripReason>()
        val wet = weather?.wetMinutes ?: 0
        if (weather != null) {
            reasons += if (wet > 0) TripReason.WetFor(wet) else TripReason.DryAllAlong
            val wind = weather.maxWindKmh.roundToInt()
            if (route.mode == TravelMode.Bike && wind >= 25) reasons += TripReason.StrongWind(wind)
            val feels = weather.minApparentC.roundToInt()
            if (feels <= 5) reasons += TripReason.Cold(feels)
            if (feels >= 30) reasons += TripReason.Hot(feels)
        }
        if (route.mode == TravelMode.Walk && route.durationMinutes > 40) reasons += TripReason.LongWalk
        if (route.mode == TravelMode.Bike && route.durationMinutes > 50) reasons += TripReason.LongRide
        return ModeOption(
            mode = route.mode,
            durationMinutes = route.durationMinutes,
            distanceKm = route.distanceKm,
            score = 0,
            cost = CostLevel.Free,
            reasons = reasons,
            isEstimate = false,
            weather = weather,
            wetExposureMinutes = wet,
            hazards = if (route.mode == TravelMode.Bike && weather != null) RideSafety.assess(weather) else emptyList(),
        )
    }

    private fun car(route: Route, weather: RouteWeather?): ModeOption {
        val reasons = mutableListOf<TripReason>()
        if ((weather?.wetMinutes ?: 0) > 0) reasons += TripReason.ShelteredFromRain
        reasons += TripReason.CarOverhead(CAR_OVERHEAD_MINUTES)
        return ModeOption(
            mode = TravelMode.Car,
            durationMinutes = route.durationMinutes + CAR_OVERHEAD_MINUTES,
            distanceKm = route.distanceKm,
            score = 0,
            cost = CostLevel.Fuel,
            reasons = reasons,
            isEstimate = false,
            weather = weather,
            wetExposureMinutes = 0,
        )
    }

    /**
     * Toplu taşıma TAHMİNİ: araba güzergâhından türetilir. Açıkta kalınan süre sadece
     * durağa yürüme; başta ve sonda yağmur varsa o kısımlarda ıslanırsın.
     */
    private fun transit(carRoute: Route, weather: RouteWeather?): ModeOption {
        val minutes = (carRoute.durationMinutes * TRANSIT_SPEED_FACTOR).roundToInt() + TRANSIT_ACCESS_MINUTES + TRANSIT_WAIT_MINUTES
        val half = TRANSIT_ACCESS_MINUTES / 2
        val wet = if (weather == null) {
            0
        } else {
            (if (weather.points.firstOrNull()?.isWet == true) half else 0) +
                (if (weather.points.lastOrNull()?.isWet == true) half else 0)
        }
        val reasons = mutableListOf<TripReason>(TripReason.TransitEstimate)
        if (weather != null) reasons += TripReason.TransitWalkWet(wet)
        return ModeOption(
            mode = TravelMode.Transit,
            durationMinutes = minutes,
            distanceKm = carRoute.distanceKm,
            score = 0,
            cost = CostLevel.Ticket,
            reasons = reasons,
            isEstimate = true,
            weather = weather,
            wetExposureMinutes = wet,
        )
    }

    // ------------------------------------------------------------------ puan

    private fun scoreOf(o: ModeOption, fastest: Int): Int {
        val time = 100.0 * fastest / o.durationMinutes.coerceAtLeast(1)
        val weather = weatherScore(o)
        val effort = when {
            o.mode == TravelMode.Walk && o.durationMinutes > 40 -> 100.0 - (o.durationMinutes - 40) * 2.5
            o.mode == TravelMode.Bike && o.durationMinutes > 50 -> 100.0 - (o.durationMinutes - 50) * 2.0
            else -> 100.0
        }.coerceIn(0.0, 100.0)
        val total = 0.45 * time + 0.35 * weather + 0.10 * o.cost.score + 0.10 * effort
        return total.roundToInt().coerceIn(0, 100)
    }

    /** Islanma, rüzgâr ve aşırı sıcaklık cezaları. Araba hava koşullarından neredeyse etkilenmez. */
    private fun weatherScore(o: ModeOption): Double {
        val w = o.weather ?: return 100.0
        if (o.mode == TravelMode.Car) return 100.0
        var score = 100.0
        // Yağmurda geçen her dakika ciddi bir eksi: 10 dk ıslanmak = -60
        score -= o.wetExposureMinutes * 6.0
        val outdoors = o.mode == TravelMode.Walk || o.mode == TravelMode.Bike
        if (outdoors) {
            if (o.mode == TravelMode.Bike) score -= ((w.maxWindKmh - 20).coerceAtLeast(0.0)) * 2.0
            val feels = w.minApparentC
            if (feels < 5) score -= (5 - feels) * 4
            if (feels > 30) score -= (feels - 30) * 4
        }
        // Yol güvenliği: savrulma ve kayma riski, ıslanmaktan bile önemli.
        o.hazards.forEach { hazard ->
            score -= when (hazard) {
                is RideHazard.CrossWind -> if (hazard.severe) 30.0 else 12.0
                is RideHazard.SlipperyFirstRain -> 15.0
            }
        }
        return score.coerceIn(0.0, 100.0)
    }

    private fun timeReason(o: ModeOption, fastest: Int): List<TripReason> {
        val diff = o.durationMinutes - fastest
        return listOf(if (diff <= 0) TripReason.Fastest else TripReason.SlowerBy(diff))
    }

    // ------------------------------------------------------------------ öneri özeti

    private fun headline(sorted: List<ModeOption>, fastest: Int): TripHeadline {
        val best = sorted.first()
        val quickest = sorted.minByOrNull { it.durationMinutes } ?: best
        val diff = best.durationMinutes - fastest
        return when {
            best.mode == quickest.mode && best.wetExposureMinutes == 0 -> TripHeadline.FastestAndComfortable(best.mode)
            best.mode == quickest.mode -> TripHeadline.FastestButWet(best.mode, best.wetExposureMinutes)
            quickest.wetExposureMinutes > best.wetExposureMinutes ->
                TripHeadline.SlowerButDrier(best.mode, diff, quickest.mode, quickest.wetExposureMinutes)
            else -> TripHeadline.SlowerButBalanced(best.mode, diff)
        }
    }
}
