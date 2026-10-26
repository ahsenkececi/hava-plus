package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RoutineAlert
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.testutil.FakeRouteRepository
import com.ahsen.hava.testutil.FakeRouteWeatherRepository
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.testutil.Fixtures.route
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckRoutineUseCaseTest {

    private val home = Place("Ev", "Bornova", 38.46, 27.21)
    private val school = Place("Okul", "Çiğli", 38.49, 27.06)
    private val routine = Routine(home, school, TravelMode.Bike, Routine.WEEKDAYS, LocalTime.of(14, 30))
    private val departure = NOW.plusMinutes(30)

    private val trip = TripRoutes(
        mapOf(
            TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 30, km = 8.0)),
            TravelMode.Car to listOf(route(TravelMode.Car, minutes = 18, km = 10.0)),
        ),
    )

    /** Bisiklet yolunda yağmur [wetAt]'e göre; araba hep kuru. */
    private fun useCase(wetAt: (Int) -> Int): CheckRoutineUseCase {
        val weather = FakeRouteWeatherRepository { r -> if (r.mode == TravelMode.Bike) FakeTimeline(wetMinutesAt = wetAt) else FakeTimeline { 0 } }
        return CheckRoutineUseCase(PlanTripUseCase(FakeRouteRepository(trip), weather, clock = { NOW }), clock = { NOW })
    }

    @Test
    fun `hava simdiye gore degil cikis saatine gore degerlendirilir`() = runTest {
        // Şimdi (offset 0) yağmurlu, çıkışta (offset 30) kuru: bildirim yok.
        val report = useCase { offset -> if (offset < 30) 20 else 0 }.check(routine, departure)

        assertEquals(0, report?.wetMinutes)
        assertFalse(report!!.isNotable)
    }

    @Test
    fun `cikista yagmur varsa rapor bildirime deger`() = runTest {
        val report = useCase { offset -> if (offset >= 30) 10 else 0 }.check(routine, departure)!!

        assertTrue(report.isNotable)
        assertTrue(RoutineAlert.Rain(10) in report.alerts)
        assertTrue(report.alerts.any { it is RoutineAlert.ShiftDeparture && it.wetMinutes == 0 })
    }

    @Test
    fun `cikis gectiyse kontrol yapilmaz`() = runTest {
        assertNull(useCase { 0 }.check(routine, NOW.minusMinutes(1)))
    }

    @Test
    fun `rutin ulasimi icin rota yoksa rapor olmaz`() = runTest {
        assertNull(useCase { 0 }.check(routine.copy(mode = TravelMode.Walk), departure))
    }
}
