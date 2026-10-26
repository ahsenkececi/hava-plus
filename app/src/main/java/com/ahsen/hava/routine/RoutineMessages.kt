package com.ahsen.hava.routine

import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RoutineAlert
import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.ui.common.UiText
import com.ahsen.hava.ui.common.inlineLabelRes
import com.ahsen.hava.ui.common.labelRes
import com.ahsen.hava.ui.common.uiText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/*
 * Rutin bildiriminin metinleri. Composable değil, [UiText] döner: bildirim Compose dışında
 * kurulur ve metni Context ile çözer. Saf olduğu için birim testle doğrulanır.
 */

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "08:15 · Okul yolculuğun" */
fun routineTitle(routine: Routine, departure: LocalDateTime): UiText =
    UiText(R.string.routine_notification_title, departure.format(CLOCK), routine.to.name)

/** Bildirimin satırları; ilki kapalı bildirimde görünen özet. */
fun RoutineReport.lines(): List<UiText> = alerts.map { it.uiText(routineMode = mode) }

/** [routineMode]: rutinin ulaşım türü; yağmur cümlesi onunla başlar ("Bisiklet ile 9 dk..."). */
fun RoutineAlert.uiText(routineMode: TravelMode): UiText = when (this) {
    is RoutineAlert.Rain -> UiText(R.string.routine_alert_rain, UiText(routineMode.labelRes()), wetMinutes)
    is RoutineAlert.ShiftDeparture -> {
        val earlier = shiftMinutes < 0
        val id = when {
            earlier && wetMinutes == 0 -> R.string.routine_shift_earlier_dry
            earlier -> R.string.routine_shift_earlier_less
            wetMinutes == 0 -> R.string.routine_shift_later_dry
            else -> R.string.routine_shift_later_less
        }
        if (wetMinutes == 0) UiText(id, abs(shiftMinutes)) else UiText(id, abs(shiftMinutes), wetMinutes)
    }
    is RoutineAlert.Hazard -> hazard.uiText()
    is RoutineAlert.SwitchMode -> UiText(R.string.routine_switch_mode, UiText(mode.inlineLabelRes()))
}
