package com.ahsen.hava.routine

import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RideHazard
import com.ahsen.hava.domain.logic.RoutineAlert
import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.ui.common.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineMessagesTest {

    @Test
    fun `yagmur cumlesi rutinin ulasim turuyle baslar`() {
        assertEquals(
            UiText(R.string.routine_alert_rain, UiText(R.string.mode_bike), 9),
            RoutineAlert.Rain(9).uiText(routineMode = TravelMode.Bike),
        )
    }

    @Test
    fun `erken ve gec cikis dogru cumleyi ve pozitif dakikayi secer`() {
        assertEquals(
            UiText(R.string.routine_shift_earlier_dry, 15),
            RoutineAlert.ShiftDeparture(shiftMinutes = -15, wetMinutes = 0).uiText(TravelMode.Bike),
        )
        assertEquals(
            UiText(R.string.routine_shift_later_less, 30, 4),
            RoutineAlert.ShiftDeparture(shiftMinutes = 30, wetMinutes = 4).uiText(TravelMode.Walk),
        )
    }

    @Test
    fun `tur onerisi cumle ici kucuk harfli etiketi kullanir`() {
        assertEquals(
            UiText(R.string.routine_switch_mode, UiText(R.string.mode_transit_inline)),
            RoutineAlert.SwitchMode(TravelMode.Transit).uiText(TravelMode.Bike),
        )
    }

    @Test
    fun `rapor satirlari uyari sirasini korur`() {
        val wind = RideHazard.CrossWind(minutesFromStart = 0, crossWindKmh = 42, severe = true)
        val report = RoutineReport(
            mode = TravelMode.Bike,
            wetMinutes = 0,
            alerts = listOf(RoutineAlert.Hazard(wind), RoutineAlert.SwitchMode(TravelMode.Car)),
        )

        val lines = report.lines()

        assertEquals(
            UiText(R.string.hazard_crosswind_severe, 42, UiText(R.string.hazard_at_start)),
            lines[0],
        )
        assertEquals(UiText(R.string.routine_switch_mode, UiText(R.string.mode_car_inline)), lines[1])
    }
}
