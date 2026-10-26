package com.ahsen.hava.widget

import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.logic.RoutineAlert
import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.DailyForecast
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.usecase.UpcomingRoutine
import com.ahsen.hava.domain.usecase.WidgetWeather
import com.ahsen.hava.testutil.Fixtures
import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.ui.common.UiText
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {

    private val city = City(-1, "Bornova", 38.46, 27.21, "TR", "Türkiye", "İzmir")
    private val hours = (0..7).map { Fixtures.hour(NOW.plusHours(it.toLong()), probability = it * 10) }
    private val forecast = Fixtures.forecast(hourly = hours).copy(
        daily = listOf(DailyForecast(NOW.toLocalDate(), minTemperatureC = 10.6, maxTemperatureC = 17.4, weatherCode = 61)),
    )
    private val routine = Routine(
        from = Place("Ev", "Bornova", 38.46, 27.21),
        to = Place("Okul", "Çiğli", 38.49, 27.06),
        mode = TravelMode.Bike,
        days = Routine.WEEKDAYS,
        departure = LocalTime.of(8, 15),
    )

    private fun upcoming(vararg alerts: RoutineAlert) =
        UpcomingRoutine(routine, NOW.withHour(8).withMinute(15), RoutineReport(TravelMode.Bike, wetMinutes = 9, alerts = alerts.toList()))

    @Test
    fun `sehir, derece ve en yuksek-en dusuk yuvarlanarak gelir`() {
        val state = widgetState(WidgetWeather(city, forecast, rainOutlook = null), upcoming = null)

        assertEquals("Bornova", state.cityName)
        assertEquals(18, state.temperatureC)
        assertEquals(UiText(R.string.high_low_short, 17, 11), state.highLow)
        assertEquals(WIDGET_HOURS, state.hours.size)
    }

    @Test
    fun `rutin yakin degilse satir yagmur durumunu soyler ve yolculuk+ i acar`() {
        val dry = widgetState(WidgetWeather(city, forecast, rainOutlook = null), upcoming = null).tripLine
        assertEquals(UiText(R.string.widget_trip_dry), dry.text)
        assertFalse(dry.opensRoutine)

        val rainy = widgetState(WidgetWeather(city, forecast, RainOutlook.RainingForAWhile), upcoming = null).tripLine
        assertEquals(UiText(R.string.rain_for_a_while), rainy.text)
    }

    @Test
    fun `rutin satiri once ne yapilacagini soyler`() {
        val shift = RoutineAlert.ShiftDeparture(shiftMinutes = -15, wetMinutes = 0)
        val line = widgetState(WidgetWeather(city, forecast, null), upcoming(RoutineAlert.Rain(9), shift)).tripLine

        assertEquals(
            UiText(R.string.widget_trip_routine, "Okul", "08:15", UiText(R.string.routine_shift_earlier_dry, 15)),
            line.text,
        )
        assertTrue(line.opensRoutine)
    }

    @Test
    fun `rutinde sorun yoksa yol temiz denir`() {
        val line = widgetState(WidgetWeather(city, forecast, null), upcoming()).tripLine

        assertEquals(UiText(R.string.widget_trip_routine, "Okul", "08:15", UiText(R.string.widget_trip_clear)), line.text)
    }
}
