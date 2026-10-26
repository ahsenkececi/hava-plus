package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.TravelMode

/** Rutin yolculuk için kullanıcıya söylenmeye değer bir durum. Metni ekran ve bildirim kurar (strings.xml). */
sealed interface RoutineAlert {
    /** Seçili ulaşımla yolun [wetMinutes] dakikası yağmurda geçecek. */
    data class Rain(val wetMinutes: Int) : RoutineAlert

    /**
     * Çıkışı [shiftMinutes] kaydırmak yağmuru azaltıyor (eksi = erken çık, artı = geç çık).
     * O saatte çıkılırsa [wetMinutes] dakika ıslanılır (0 = hiç).
     */
    data class ShiftDeparture(val shiftMinutes: Int, val wetMinutes: Int) : RoutineAlert

    /** Yol güvenliği riski (yan rüzgâr, ilk yağmurun kayganlığı). */
    data class Hazard(val hazard: RideHazard) : RoutineAlert

    /** Bugün [mode] daha iyi bir seçenek. */
    data class SwitchMode(val mode: TravelMode) : RoutineAlert
}

/** Rutin kontrolünün sonucu. [alerts] boşsa her şey yolunda: bildirim gönderilmez. */
data class RoutineReport(
    val mode: TravelMode,
    val wetMinutes: Int,
    val alerts: List<RoutineAlert>,
) {
    val isNotable: Boolean get() = alerts.isNotEmpty()
}

/**
 * Rutin için karar kuralları. Saf Kotlin: puanlamayı [TripScorer] yapar, burası sadece
 * "kullanıcıyı rahatsız etmeye değer mi, ne söylemeli?" sorusunu cevaplar.
 */
object RoutineAdvisor {

    /** Denenen çıkış kaydırmaları (dk). Yakın olan önce: eşitlikte daha az kaydırma kazanır. */
    val SHIFTS = listOf(-15, 15, -30, 30)

    private val OUTDOOR = setOf(TravelMode.Walk, TravelMode.Bike)

    /**
     * @param option kullanıcının rutin ulaşımının bu çıkıştaki değerlendirmesi
     * @param best o çıkıştaki en yüksek puanlı seçenek
     * @param timeline rutin ulaşımının hava çizelgesi (kaydırmaları denemek için)
     * @param departureOffset şimdiden çıkışa kalan dakika
     */
    fun advise(
        option: ModeOption,
        best: ModeOption?,
        timeline: RouteWeatherTimeline?,
        departureOffset: Int,
    ): RoutineReport {
        val wet = option.wetExposureMinutes
        val alerts = buildList {
            if (wet > 0) add(RoutineAlert.Rain(wet))
            if (option.mode in OUTDOOR && wet > 0 && timeline != null) {
                bestShift(timeline, departureOffset, wet)?.let(::add)
            }
            option.hazards.forEach { add(RoutineAlert.Hazard(it)) }
            val inTrouble = wet > 0 || option.hazards.any { it is RideHazard.CrossWind && it.severe }
            if (best != null && best.mode != option.mode && inTrouble) add(RoutineAlert.SwitchMode(best.mode))
        }
        return RoutineReport(option.mode, wet, alerts)
    }

    /** Yağmuru tamamen bitiren ya da en az yarıya (ve 3 dk+) indiren en yakın kaydırma. */
    private fun bestShift(timeline: RouteWeatherTimeline, departureOffset: Int, wetNow: Int): RoutineAlert.ShiftDeparture? =
        SHIFTS.filter { departureOffset + it >= 0 }
            .map { shift -> RoutineAlert.ShiftDeparture(shift, timeline.at(departureOffset + shift).wetMinutes) }
            .minByOrNull { it.wetMinutes }
            ?.takeIf { it.wetMinutes == 0 || (it.wetMinutes <= wetNow / 2 && wetNow - it.wetMinutes >= 3) }
}
