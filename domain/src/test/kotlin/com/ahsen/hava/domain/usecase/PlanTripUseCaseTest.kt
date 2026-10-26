package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.testutil.FakeRouteRepository
import com.ahsen.hava.testutil.FakeRouteWeatherRepository
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.testutil.Fixtures.route
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlanTripUseCaseTest {

    private val from = Place("Konak", "İzmir", 38.4189, 27.1287)
    private val to = Place("Bornova", "İzmir", 38.4697, 27.2211)

    private val fastCar = route(TravelMode.Car, minutes = 15, km = 9.0)
    private val slowCar = route(TravelMode.Car, minutes = 22, km = 11.0)
    private val bike = route(TravelMode.Bike, minutes = 30, km = 8.5)

    private val trip = TripRoutes(mapOf(TravelMode.Car to listOf(fastCar, slowCar), TravelMode.Bike to listOf(bike)))

    private val weatherRepository = FakeRouteWeatherRepository { FakeTimeline { 0 } }
    private val useCase = PlanTripUseCase(FakeRouteRepository(trip), weatherRepository, clock = { NOW })

    @Test
    fun `rotalar repository den gelir`() = runTest {
        assertEquals(trip, useCase.findRoutes(from, to))
    }

    @Test
    fun `hava sadece her turun en iyi rotasi icin ve simdiki saatle istenir`() = runTest {
        val timelines = useCase.weatherAlong(trip)

        assertEquals(setOf(fastCar, bike), weatherRepository.requestedRoutes.toSet())
        assertEquals(NOW, weatherRepository.requestedAt)
        assertEquals(setOf(TravelMode.Car, TravelMode.Bike), timelines.keys)
    }

    @Test
    fun `bisiklet varsa ve havasi geldiyse cikis onerisi de hazirlanir`() {
        val timelines = mapOf(TravelMode.Bike to FakeTimeline { 0 })
        val weather = timelines.mapValues { it.value.at(0) }

        val plan = useCase.evaluate(trip, weather, timelines)

        assertNotNull(plan.recommendation.best)
        assertEquals(TravelMode.Bike, plan.departureAdvice?.mode)
    }

    @Test
    fun `acikta giden secenek yoksa cikis onerisi olmaz`() {
        val carOnly = TripRoutes(mapOf(TravelMode.Car to listOf(fastCar)))

        val plan = useCase.evaluate(carOnly, emptyMap(), emptyMap())

        assertNull(plan.departureAdvice)
    }

    @Test
    fun `hava henuz gelmediyse oneri yine de hazirlanir`() {
        val plan = useCase.evaluate(trip, emptyMap(), emptyMap())

        assertNotNull(plan.recommendation.best)
        assertNull(plan.departureAdvice)
    }
}
