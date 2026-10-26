package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.repository.RoutineRepository
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/** Yaklaşan rutin çıkışı ve o çıkışın değerlendirmesi. */
data class UpcomingRoutine(
    val routine: Routine,
    val departure: LocalDateTime,
    val report: RoutineReport,
)

/**
 * "Rutin çıkışım yakında mı, nasıl görünüyor?" Widget'taki Yolculuk+ satırı için.
 * Çıkış [WINDOW] içinde değilse rota ve hava hiç istenmez (gereksiz ağ trafiği yok).
 */
class CheckUpcomingRoutineUseCase(
    private val routineRepository: RoutineRepository,
    private val checkRoutine: CheckRoutineUseCase,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    /** Rutin yoksa, kapalıysa, çıkış uzaksa ya da değerlendirilemediyse null. */
    suspend operator fun invoke(): UpcomingRoutine? {
        val routine = routineRepository.routine.first() ?: return null
        val now = clock()
        val departure = routine.nextDeparture(after = now) ?: return null
        if (Duration.between(now, departure) > WINDOW) return null
        val report = checkRoutine.check(routine, departure) ?: return null
        return UpcomingRoutine(routine, departure, report)
    }

    companion object {
        val WINDOW: Duration = Duration.ofHours(3)
    }
}
