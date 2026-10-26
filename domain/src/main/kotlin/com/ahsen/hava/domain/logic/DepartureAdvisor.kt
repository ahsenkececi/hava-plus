package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.TravelMode
import java.time.LocalDateTime

/** Zaman şeridindeki bir çıkış seçeneği. */
data class DepartureSlot(val offsetMinutes: Int, val wetMinutes: Int, val time: LocalDateTime)

/** "Ne zaman çıkmalıyım?" sorusunun cevabı. Cümleyi ekran kurar. */
sealed interface DepartureMessage {
    /** Önümüzdeki 2 saat bu yolda yağmur yok. */
    data object DryForTwoHours : DepartureMessage

    /** Şimdi kuru, ama [wetFromOffsetMinutes] dakika sonra çıkılırsa yağmura yakalanılır: acele et. */
    data class LeaveNowToStayDry(val wetFromOffsetMinutes: Int) : DepartureMessage

    /** Şimdi çıkarsan [wetNowMinutes] dk ıslanırsın; [best] saatinde çıkarsan hiç ıslanmazsın. */
    data class WaitToStayDry(val best: DepartureSlot, val wetNowMinutes: Int) : DepartureMessage

    /** Beklemek yağmuru tamamen bitirmiyor ama belirgin azaltıyor. */
    data class WaitForLessRain(val best: DepartureSlot, val wetNowMinutes: Int) : DepartureMessage

    /** Önümüzdeki 2 saatte yağmur dinmiyor: daha korunaklı bir ulaşım seç. */
    data object RainDoesNotStop : DepartureMessage
}

data class DepartureAdvice(
    val mode: TravelMode,
    val slots: List<DepartureSlot>,
    val bestOffsetMinutes: Int,
    val message: DepartureMessage,
)

/**
 * "Ne zaman çıkmalıyım?" Saf Kotlin: aynı rotayı zamanda kaydırıp her çıkış için
 * yağmurda geçen dakikayı hesaplar ve en iyisini seçer.
 */
object DepartureAdvisor {

    val OFFSETS = listOf(0, 15, 30, 45, 60, 90, 120)

    fun advise(mode: TravelMode, timeline: RouteWeatherTimeline): DepartureAdvice {
        val slots = OFFSETS.map { offset ->
            DepartureSlot(offset, timeline.at(offset).wetMinutes, timeline.baseTime.plusMinutes(offset.toLong()))
        }
        val now = slots.first()
        // En az ıslatan; eşitlikte en erken çıkış
        val best = slots.minWith(compareBy<DepartureSlot> { it.wetMinutes }.thenBy { it.offsetMinutes })
        val firstWetLater = slots.drop(1).firstOrNull { it.wetMinutes > 0 }

        val message = when {
            now.wetMinutes == 0 && firstWetLater != null && firstWetLater.offsetMinutes <= 45 ->
                DepartureMessage.LeaveNowToStayDry(firstWetLater.offsetMinutes)
            now.wetMinutes == 0 -> DepartureMessage.DryForTwoHours
            best.wetMinutes == 0 -> DepartureMessage.WaitToStayDry(best, now.wetMinutes)
            best.wetMinutes <= now.wetMinutes / 2 && now.wetMinutes - best.wetMinutes >= 3 ->
                DepartureMessage.WaitForLessRain(best, now.wetMinutes)
            else -> DepartureMessage.RainDoesNotStop
        }
        return DepartureAdvice(mode, slots, best.offsetMinutes, message)
    }
}
