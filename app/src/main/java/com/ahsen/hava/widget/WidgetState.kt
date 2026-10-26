package com.ahsen.hava.widget

import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RoutineAlert
import com.ahsen.hava.domain.usecase.UpcomingRoutine
import com.ahsen.hava.domain.usecase.WidgetWeather
import com.ahsen.hava.routine.uiText
import com.ahsen.hava.ui.common.UiText
import com.ahsen.hava.ui.common.labelRes
import com.ahsen.hava.ui.common.uiText
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.skyFor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Eğrideki bir saat. */
data class WidgetHour(val time: LocalDateTime, val temperatureC: Int, val precipitationProbability: Int)

/** Widget'ın alt satırı ve dokununca nereye gideceği. */
data class WidgetTripLine(val text: UiText, val opensRoutine: Boolean)

/** Widget'ın göstereceği her şey. Metinler [UiText]: widget çizilirken Context ile çözülür. */
sealed interface WidgetState {
    data class Ready(
        val cityName: String,
        val temperatureC: Int,
        val condition: UiText,
        val highLow: UiText?,
        val sky: Sky,
        val hours: List<WidgetHour>,
        val tripLine: WidgetTripLine,
    ) : WidgetState

    /** Veri alınamadı (internet yok vb.). */
    data object Unavailable : WidgetState
}

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Eğride gösterilen saat sayısı ("Şimdi" + 4 saat). */
internal const val WIDGET_HOURS = 5

/**
 * Domain sonuçlarını widget durumuna çevirir. Saf fonksiyon: Android'e dokunmaz, birim testle doğrulanır.
 */
fun widgetState(weather: WidgetWeather, upcoming: UpcomingRoutine?): WidgetState.Ready {
    val current = weather.forecast.current
    val today = weather.forecast.daily.firstOrNull()
    return WidgetState.Ready(
        cityName = weather.city.name,
        temperatureC = current.temperatureC.roundToInt(),
        condition = UiText(current.condition.labelRes()),
        highLow = today?.let { UiText(R.string.high_low_short, it.maxTemperatureC.roundToInt(), it.minTemperatureC.roundToInt()) },
        sky = skyFor(current.condition, current.isDay),
        hours = weather.forecast.upcomingHours(WIDGET_HOURS).map {
            WidgetHour(it.time, it.temperatureC.roundToInt(), it.precipitationProbability ?: 0)
        },
        tripLine = upcoming?.let(::routineLine) ?: generalLine(weather),
    )
}

/** "Okul 08:15 · 15 dk erken çıkarsan kuru kalırsın." */
private fun routineLine(upcoming: UpcomingRoutine): WidgetTripLine {
    val report = upcoming.report
    // En faydalı uyarı önde: ne yapmalı (kaydır, tür değiştir) > ne olacak (yağmur, risk).
    val alert = report.alerts.firstOrNull { it is RoutineAlert.ShiftDeparture }
        ?: report.alerts.firstOrNull { it is RoutineAlert.SwitchMode }
        ?: report.alerts.firstOrNull()
    val body = alert?.uiText(routineMode = report.mode) ?: UiText(R.string.widget_trip_clear)
    return WidgetTripLine(
        text = UiText(R.string.widget_trip_routine, upcoming.routine.to.name, upcoming.departure.format(CLOCK), body),
        opensRoutine = true,
    )
}

/** Rutin yakın değilse: önümüzdeki 2 saatin yağmur durumu. */
private fun generalLine(weather: WidgetWeather): WidgetTripLine =
    WidgetTripLine(weather.rainOutlook?.uiText() ?: UiText(R.string.widget_trip_dry), opensRoutine = false)
