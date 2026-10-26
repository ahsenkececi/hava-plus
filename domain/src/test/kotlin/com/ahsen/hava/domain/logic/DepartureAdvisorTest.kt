package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import com.ahsen.hava.testutil.Fixtures.NOW
import org.junit.Assert.assertEquals
import org.junit.Test

class DepartureAdvisorTest {

    @Test
    fun `iki saat boyunca kuruysa simdi cikmak en iyisidir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Bike, FakeTimeline { 0 })

        assertEquals(0, advice.bestOffsetMinutes)
        assertEquals(DepartureMessage.DryForTwoHours, advice.message)
    }

    @Test
    fun `simdi kuru ama yakinda yagacaksa acele etmesi soylenir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Walk, FakeTimeline { offset -> if (offset >= 30) 12 else 0 })

        assertEquals(0, advice.bestOffsetMinutes)
        assertEquals(DepartureMessage.LeaveNowToStayDry(wetFromOffsetMinutes = 30), advice.message)
    }

    @Test
    fun `simdi yagiyorsa yagmurun bittigi ilk cikis onerilir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Bike, FakeTimeline { offset -> if (offset < 45) 20 else 0 })

        assertEquals(45, advice.bestOffsetMinutes)
        val message = advice.message as DepartureMessage.WaitToStayDry
        assertEquals(20, message.wetNowMinutes)
        assertEquals(NOW.plusMinutes(45), message.best.time)
    }

    @Test
    fun `yagmur dinmiyorsa korunakli ulasim onerilir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Walk, FakeTimeline { 20 })

        assertEquals(0, advice.bestOffsetMinutes)
        assertEquals(DepartureMessage.RainDoesNotStop, advice.message)
    }

    @Test
    fun `her cikis secenegi icin dogru saat hesaplanir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Walk, FakeTimeline { 0 })

        assertEquals(DepartureAdvisor.OFFSETS, advice.slots.map { it.offsetMinutes })
        assertEquals(NOW.plusMinutes(90), advice.slots.first { it.offsetMinutes == 90 }.time)
    }

    @Test
    fun `yagmur yarim yarim azaliyorsa beklemek onerilir`() {
        val advice = DepartureAdvisor.advise(TravelMode.Walk, FakeTimeline { offset -> if (offset < 60) 20 else 6 })

        assertEquals(60, advice.bestOffsetMinutes)
        assertEquals(20, (advice.message as DepartureMessage.WaitForLessRain).wetNowMinutes)
    }
}
