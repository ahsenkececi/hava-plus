package com.ahsen.hava.ui.trip

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.ui.common.label
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/* Rutin metinleri: "Hafta içi · 08:15 · Bisiklet" gibi. */

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@StringRes
internal fun DayOfWeek.shortLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_short_mon
    DayOfWeek.TUESDAY -> R.string.day_short_tue
    DayOfWeek.WEDNESDAY -> R.string.day_short_wed
    DayOfWeek.THURSDAY -> R.string.day_short_thu
    DayOfWeek.FRIDAY -> R.string.day_short_fri
    DayOfWeek.SATURDAY -> R.string.day_short_sat
    DayOfWeek.SUNDAY -> R.string.day_short_sun
}

/** "Her gün", "Hafta içi", "Hafta sonu" ya da "Pzt, Çar, Cum". */
@Composable
internal fun routineDaysText(days: Set<DayOfWeek>): String = when {
    days.size == DayOfWeek.entries.size -> stringResource(R.string.routine_days_every)
    days == Routine.WEEKDAYS -> stringResource(R.string.routine_days_weekdays)
    days == WEEKEND -> stringResource(R.string.routine_days_weekend)
    else -> DayOfWeek.entries.filter { it in days }.map { stringResource(it.shortLabelRes()) }.joinToString(", ")
}

@Composable
internal fun routineScheduleText(routine: Routine): String = stringResource(
    R.string.routine_schedule,
    routineDaysText(routine.days),
    routine.departure.format(CLOCK),
    routine.mode.label(),
)
