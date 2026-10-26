package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.DepartureAdvice
import com.ahsen.hava.domain.logic.DepartureAdvisor
import com.ahsen.hava.domain.logic.TripRecommendation
import com.ahsen.hava.domain.logic.TripScorer
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.repository.RouteRepository
import com.ahsen.hava.domain.repository.RouteWeatherRepository
import java.time.LocalDateTime

/** Ekranın göstereceği karar: hangi seçenek önde, açıkta gidiliyorsa ne zaman çıkılmalı. */
data class TripPlan(
    val recommendation: TripRecommendation,
    /** Yürüyüş ya da bisiklet seçenekte varsa ve rota havası geldiyse dolu. */
    val departureAdvice: DepartureAdvice?,
)

/**
 * Yolculuk planlamanın iş akışı. Üç adım, üçü de ayrı çağrılır çünkü ekran onları
 * farklı hızlarda gösterir: rotalar hemen, hava bir saniye sonra, karar her ikisi değiştikçe.
 *
 * 1. [findRoutes]: her ulaşım türü için rota
 * 2. [weatherAlong]: her türün en iyi rotası boyunca hava çizelgesi
 * 3. [evaluate]: puanlama + "ne zaman çıkmalıyım?" (ağa gitmez, saf hesap)
 */
class PlanTripUseCase(
    private val routeRepository: RouteRepository,
    private val routeWeatherRepository: RouteWeatherRepository,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    suspend fun findRoutes(from: Place, to: Place): TripRoutes = routeRepository.routes(from, to)

    suspend fun weatherAlong(trip: TripRoutes): Map<TravelMode, RouteWeatherTimeline> {
        val bestRoutes = trip.byMode.values.mapNotNull { it.firstOrNull() }
        return routeWeatherRepository.timelines(bestRoutes, clock())
            .mapKeys { (route, _) -> route.mode }
    }

    fun evaluate(
        trip: TripRoutes,
        weather: Map<TravelMode, RouteWeather>,
        timelines: Map<TravelMode, RouteWeatherTimeline>,
    ): TripPlan {
        val recommendation = TripScorer.recommend(trip, weather)
        // "Ne zaman çıkmalıyım?" en çok açıkta giden türler için anlamlı: yürüyüş ve bisikletten puanı yüksek olan.
        val outdoor = recommendation.options.firstOrNull { it.mode in OUTDOOR_MODES }
        val advice = outdoor?.let { option ->
            timelines[option.mode]?.let { DepartureAdvisor.advise(option.mode, it) }
        }
        return TripPlan(recommendation, advice)
    }

    private companion object {
        val OUTDOOR_MODES = setOf(TravelMode.Walk, TravelMode.Bike)
    }
}
