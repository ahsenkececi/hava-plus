package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RoutineAdvisor
import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.logic.TripScorer
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import java.time.Duration
import java.time.LocalDateTime

/**
 * "Rutinimde bugün dikkat etmem gereken bir şey var mı?"
 * Arka plan görevi çıkıştan önce bunu çağırır; [RoutineReport.isNotable] ise bildirim gönderir.
 *
 * Rotayı ve havayı [PlanTripUseCase] ile alır (Yolculuk+ ile aynı akış), havayı
 * şimdiye göre değil ÇIKIŞ saatine göre değerlendirir.
 */
class CheckRoutineUseCase(
    private val planTrip: PlanTripUseCase,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    /** Çıkış geçmişse, tahminin kapsamadığı kadar uzaksa ya da rutin ulaşımı için rota bulunamadıysa null. */
    suspend fun check(routine: Routine, departure: LocalDateTime): RoutineReport? {
        val offset = Duration.between(clock(), departure).toMinutes().toInt()
        if (offset !in 0..MAX_LOOKAHEAD_MINUTES) return null

        val routes = planTrip.findRoutes(routine.from, routine.to)
        val timelines = planTrip.weatherAlong(routes)
        val weatherAtDeparture = timelines.mapValues { (_, timeline) -> timeline.at(offset) }

        val recommendation = TripScorer.recommend(routes, weatherAtDeparture)
        val option = recommendation.options.firstOrNull { it.mode == routine.mode } ?: return null
        // Toplu taşıma kendi rotasını değil arabanınkini kullanır.
        val weatherMode = if (routine.mode == TravelMode.Transit) TravelMode.Car else routine.mode
        return RoutineAdvisor.advise(option, recommendation.best, timelines[weatherMode], offset)
    }

    private companion object {
        /** Rota tahmini önümüzdeki 12 saati kapsar; yolun kendisine de pay bırakıyoruz. */
        const val MAX_LOOKAHEAD_MINUTES = 6 * 60
    }
}
