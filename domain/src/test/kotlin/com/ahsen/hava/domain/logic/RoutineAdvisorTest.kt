package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineAdvisorTest {

    private fun option(
        mode: TravelMode,
        wet: Int = 0,
        hazards: List<RideHazard> = emptyList(),
    ) = ModeOption(
        mode = mode,
        durationMinutes = 25,
        distanceKm = 7.0,
        score = 70,
        cost = CostLevel.Free,
        reasons = emptyList(),
        isEstimate = false,
        weather = null,
        wetExposureMinutes = wet,
        hazards = hazards,
    )

    private val dryTimeline = FakeTimeline { 0 }

    @Test
    fun `kuru ve risksiz yolculukta bildirim gerekmez`() {
        val bike = option(TravelMode.Bike)
        val report = RoutineAdvisor.advise(bike, best = bike, timeline = dryTimeline, departureOffset = 30)
        assertFalse(report.isNotable)
    }

    @Test
    fun `yagmur varsa ve erken cikmak kurtariyorsa ikisi de soylenir`() {
        // Çıkış 30 dk sonra (offset 30). 15 dk erken (offset 15) çıkılırsa kuru.
        val timeline = FakeTimeline { offset -> if (offset >= 30) 9 else 0 }
        val bike = option(TravelMode.Bike, wet = 9)

        val report = RoutineAdvisor.advise(bike, best = bike, timeline = timeline, departureOffset = 30)

        assertEquals(
            listOf(RoutineAlert.Rain(9), RoutineAlert.ShiftDeparture(shiftMinutes = -15, wetMinutes = 0)),
            report.alerts,
        )
    }

    @Test
    fun `gecmise kaydirma denenmez`() {
        // Çıkışa 10 dk var: -15 ve -30 denenemez; 15 dk geç çıkmak kurtarıyor.
        val timeline = FakeTimeline { offset -> if (offset < 20) 8 else 0 }
        val bike = option(TravelMode.Bike, wet = 8)

        val report = RoutineAdvisor.advise(bike, best = bike, timeline = timeline, departureOffset = 10)

        assertTrue(RoutineAlert.ShiftDeparture(shiftMinutes = 15, wetMinutes = 0) in report.alerts)
    }

    @Test
    fun `kaydirma yagmuru yeterince azaltmiyorsa onerilmez`() {
        val timeline = FakeTimeline { offset -> if (offset == 30) 10 else 7 }
        val walk = option(TravelMode.Walk, wet = 10)

        val report = RoutineAdvisor.advise(walk, best = walk, timeline = timeline, departureOffset = 30)

        assertEquals(listOf(RoutineAlert.Rain(10)), report.alerts)
    }

    @Test
    fun `islanacaksa ve baska tur daha iyiyse tur degistirmesi onerilir`() {
        val bike = option(TravelMode.Bike, wet = 12)
        val transit = option(TravelMode.Transit)

        val report = RoutineAdvisor.advise(bike, best = transit, timeline = null, departureOffset = 30)

        assertTrue(RoutineAlert.SwitchMode(TravelMode.Transit) in report.alerts)
    }

    @Test
    fun `sert yan ruzgar tek basina da bildirim ve tur onerisi uretir`() {
        val wind = RideHazard.CrossWind(minutesFromStart = 10, crossWindKmh = 45, severe = true)
        val bike = option(TravelMode.Bike, hazards = listOf(wind))
        val car = option(TravelMode.Car)

        val report = RoutineAdvisor.advise(bike, best = car, timeline = dryTimeline, departureOffset = 30)

        assertEquals(listOf(RoutineAlert.Hazard(wind), RoutineAlert.SwitchMode(TravelMode.Car)), report.alerts)
    }

    @Test
    fun `sorun yokken daha yuksek puanli tur olsa bile rahatsiz edilmez`() {
        val bike = option(TravelMode.Bike)
        val car = option(TravelMode.Car)

        val report = RoutineAdvisor.advise(bike, best = car, timeline = dryTimeline, departureOffset = 30)

        assertFalse(report.isNotable)
    }
}
