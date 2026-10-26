package com.ahsen.hava.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Kullanıcının düzenli yolculuğu: "Hafta içi 08:15'te evden okula bisikletle."
 * Uygulama her çıkıştan [CHECK_LEAD_MINUTES] dakika önce bu yolun havasına bakar.
 */
data class Routine(
    val from: Place,
    val to: Place,
    val mode: TravelMode,
    val days: Set<DayOfWeek>,
    val departure: LocalTime,
    val enabled: Boolean = true,
) {
    /** [after] anından SONRAKİ ilk çıkış. Gün seçilmemişse ya da rutin kapalıysa null. */
    fun nextDeparture(after: LocalDateTime): LocalDateTime? {
        if (!enabled || days.isEmpty()) return null
        return (0L..7L).asSequence()
            .map { after.toLocalDate().plusDays(it) }
            .filter { it.dayOfWeek in days }
            .map { it.atTime(departure) }
            .firstOrNull { it.isAfter(after) }
    }

    /** [departure] çıkışı için kontrolün yapılacağı an. */
    fun checkTimeFor(departure: LocalDateTime): LocalDateTime = departure.minusMinutes(CHECK_LEAD_MINUTES)

    companion object {
        const val CHECK_LEAD_MINUTES = 30L
        val WEEKDAYS: Set<DayOfWeek> = DayOfWeek.entries.filter { it != DayOfWeek.SATURDAY && it != DayOfWeek.SUNDAY }.toSet()
    }
}
