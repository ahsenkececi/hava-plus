package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.testutil.FakeRouteRepository
import com.ahsen.hava.testutil.FakeRouteWeatherRepository
import com.ahsen.hava.testutil.FakeRoutineRepository
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.testutil.Fixtures.route
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckUpcomingRoutineUseCaseTest {

    // NOW = Pazar 14:00
    private val routine = Routine(
        from = Place("Ev", "Bornova", 38.46, 27.21),
        to = Place("Okul", "Çiğli", 38.49, 27.06),
        mode = TravelMode.Bike,
        days = setOf(DayOfWeek.SUNDAY),
        departure = LocalTime.of(15, 0),
    )
    private val trip = TripRoutes(mapOf(TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 30, km = 8.0))))
    private val weather = FakeRouteWeatherRepository { FakeTimeline { 0 } }

    private fun useCase(saved: Routine?) = CheckUpcomingRoutineUseCase(
        FakeRoutineRepository(saved),
        CheckRoutineUseCase(PlanTripUseCase(FakeRouteRepository(trip), weather, clock = { NOW }), clock = { NOW }),
        clock = { NOW },
    )

    @Test
    fun `cikis yakinsa degerlendirme doner`() = runTest {
        val upcoming = useCase(routine)()

        assertEquals(NOW.withHour(15), upcoming?.departure)
        assertEquals(TravelMode.Bike, upcoming?.report?.mode)
    }

    @Test
    fun `cikis uc saatten uzaksa rota hic istenmez`() = runTest {
        assertNull(useCase(routine.copy(departure = LocalTime.of(18, 0)))())
        assertEquals(emptyList<Any>(), weather.requestedRoutes)
    }

    @Test
    fun `rutin yoksa ya da kapaliysa null`() = runTest {
        assertNull(useCase(null)())
        assertNull(useCase(routine.copy(enabled = false))())
    }
}
