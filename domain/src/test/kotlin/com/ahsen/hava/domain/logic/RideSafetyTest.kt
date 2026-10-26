package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.GeoPoint
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.testutil.Fixtures.point
import com.ahsen.hava.testutil.Fixtures.route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSafetyTest {

    @Test
    fun `yol yonu pusula derecesiyle hesaplanir`() {
        val start = GeoPoint(40.0, 29.0)

        assertEquals(0.0, start.bearingTo(GeoPoint(40.1, 29.0)), 0.5) // kuzey
        assertEquals(90.0, start.bearingTo(GeoPoint(40.0, 29.1)), 0.5) // doğu
        assertEquals(180.0, start.bearingTo(GeoPoint(39.9, 29.0)), 0.5) // güney
    }

    @Test
    fun `ruzgar yolu dik kesiyorsa tamami yan ruzgardir`() {
        // Kuzeye gidiyoruz, rüzgâr batıdan (270°) esiyor: tam yandan.
        assertEquals(30.0, RideSafety.crossWindKmh(point(0, windKmh = 30.0, windFromDeg = 270.0, headingDeg = 0.0))!!, 0.01)
    }

    @Test
    fun `onden ya da arkadan esen ruzgar yan ruzgar degildir`() {
        assertEquals(0.0, RideSafety.crossWindKmh(point(0, windKmh = 30.0, windFromDeg = 0.0, headingDeg = 0.0))!!, 0.01)
        assertEquals(0.0, RideSafety.crossWindKmh(point(0, windKmh = 30.0, windFromDeg = 180.0, headingDeg = 0.0))!!, 0.01)
    }

    @Test
    fun `yon bilgisi yoksa yan ruzgar hesaplanmaz`() {
        assertNull(RideSafety.crossWindKmh(point(0, windKmh = 30.0)))
    }

    @Test
    fun `en sert yan ruzgar noktasi tek uyari olarak doner`() {
        val weather = RouteWeather(
            listOf(
                point(0, windKmh = 30.0, windFromDeg = 270.0, headingDeg = 0.0), // 30 yandan
                point(10, windKmh = 45.0, windFromDeg = 90.0, headingDeg = 0.0), // 45 yandan
                point(20, windKmh = 50.0, windFromDeg = 0.0, headingDeg = 0.0), // önden: sayılmaz
            ),
            totalMinutes = 30,
        )

        val hazard = RideSafety.assess(weather).single() as RideHazard.CrossWind

        assertEquals(10, hazard.minutesFromStart)
        assertEquals(45, hazard.crossWindKmh)
        assertTrue(hazard.severe)
    }

    @Test
    fun `hafif yan ruzgar uyari uretmez`() {
        val weather = RouteWeather(listOf(point(0, windKmh = 20.0, windFromDeg = 270.0, headingDeg = 0.0)), totalMinutes = 10)

        assertTrue(RideSafety.assess(weather).isEmpty())
    }

    @Test
    fun `kuru donemden sonraki ilk yagmur kaygan zemin uyarisi verir`() {
        val weather = RouteWeather(listOf(point(0), point(15, wet = true, past6hMm = 0.0)), totalMinutes = 30)

        assertEquals(listOf<RideHazard>(RideHazard.SlipperyFirstRain(15)), RideSafety.assess(weather))
    }

    @Test
    fun `saatlerdir yagan yagmurda ilk yagmur uyarisi verilmez`() {
        val weather = RouteWeather(listOf(point(0, wet = true, past6hMm = 6.0)), totalMinutes = 20)

        assertTrue(RideSafety.assess(weather).isEmpty())
    }

    @Test
    fun `sert yan ruzgarda bisikletin puani duser ve risk karta tasinir`() {
        val trip = TripRoutes(mapOf(TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 15, km = 4.0))))
        val calm = RouteWeather(listOf(point(0, windKmh = 10.0, windFromDeg = 270.0, headingDeg = 0.0)), totalMinutes = 15)
        val gusty = RouteWeather(listOf(point(0, windKmh = 45.0, windFromDeg = 270.0, headingDeg = 0.0)), totalMinutes = 15)

        val calmBike = TripScorer.recommend(trip, mapOf(TravelMode.Bike to calm)).best!!
        val gustyBike = TripScorer.recommend(trip, mapOf(TravelMode.Bike to gusty)).best!!

        assertTrue(gustyBike.score < calmBike.score)
        assertTrue(gustyBike.hazards.single() is RideHazard.CrossWind)
        assertTrue(calmBike.hazards.isEmpty())
    }
}
