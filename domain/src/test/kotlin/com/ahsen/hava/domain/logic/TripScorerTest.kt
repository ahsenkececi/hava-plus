package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.testutil.Fixtures.dryWeather
import com.ahsen.hava.testutil.Fixtures.route
import com.ahsen.hava.testutil.Fixtures.weatherWetFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripScorerTest {

    @Test
    fun `kuru havada kisa yolda en hizli secenek kazanir`() {
        val trip = TripRoutes(
            mapOf(
                TravelMode.Walk to listOf(route(TravelMode.Walk, minutes = 10, km = 0.8)),
                TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 4, km = 0.8)),
                TravelMode.Car to listOf(route(TravelMode.Car, minutes = 3, km = 0.8)),
            ),
        )
        val weather = TravelMode.entries.associateWith { dryWeather(totalMinutes = 10) }

        val result = TripScorer.recommend(trip, weather)

        assertEquals(TravelMode.Bike, result.best?.mode)
        assertEquals(TripHeadline.FastestAndComfortable(TravelMode.Bike), result.headline)
    }

    @Test
    fun `yagmurda yurumek arabanin gerisinde kalir ve nedeni yazilir`() {
        val trip = TripRoutes(
            mapOf(
                TravelMode.Walk to listOf(route(TravelMode.Walk, minutes = 20, km = 1.6)),
                TravelMode.Car to listOf(route(TravelMode.Car, minutes = 10, km = 5.0)),
            ),
        )
        val weather = mapOf(
            TravelMode.Walk to weatherWetFor(wetMinutes = 20, totalMinutes = 20),
            TravelMode.Car to weatherWetFor(wetMinutes = 10, totalMinutes = 10),
        )

        val result = TripScorer.recommend(trip, weather)
        val walk = result.options.first { it.mode == TravelMode.Walk }

        assertEquals(TravelMode.Car, result.best?.mode)
        assertEquals(20, walk.wetExposureMinutes)
        assertTrue(TripReason.WetFor(20) in walk.reasons)
        assertTrue(TripReason.ShelteredFromRain in result.best!!.reasons)
        // Araba hem en hızlısı hem de kuru: öneri cümlesi bunu söyler.
        assertEquals(TripHeadline.FastestAndComfortable(TravelMode.Car), result.headline)
    }

    @Test
    fun `arabaya park ve yurume payi eklenir`() {
        val trip = TripRoutes(mapOf(TravelMode.Car to listOf(route(TravelMode.Car, minutes = 12, km = 6.0))))

        val car = TripScorer.recommend(trip, emptyMap()).options.first { it.mode == TravelMode.Car }

        assertEquals(17, car.durationMinutes)
        assertEquals(CostLevel.Fuel, car.cost)
    }

    @Test
    fun `toplu tasima arabadan turetilen bir tahmindir`() {
        val trip = TripRoutes(mapOf(TravelMode.Car to listOf(route(TravelMode.Car, minutes = 10, km = 5.0))))

        val transit = TripScorer.recommend(trip, emptyMap()).options.first { it.mode == TravelMode.Transit }

        // 10 dk x 1,5 + 10 dk durak yolu + 6 dk bekleme
        assertEquals(31, transit.durationMinutes)
        assertTrue(transit.isEstimate)
        assertEquals(CostLevel.Ticket, transit.cost)
    }

    @Test
    fun `cok kisa mesafede toplu tasima onerilmez`() {
        val trip = TripRoutes(mapOf(TravelMode.Car to listOf(route(TravelMode.Car, minutes = 3, km = 1.0))))

        val modes = TripScorer.recommend(trip, emptyMap()).options.map { it.mode }

        assertFalse(TravelMode.Transit in modes)
    }

    @Test
    fun `secenekler puana gore buyukten kucuge siralidir`() {
        val trip = TripRoutes(
            mapOf(
                TravelMode.Walk to listOf(route(TravelMode.Walk, minutes = 35, km = 2.8)),
                TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 12, km = 2.8)),
                TravelMode.Car to listOf(route(TravelMode.Car, minutes = 8, km = 3.5)),
            ),
        )

        val scores = TripScorer.recommend(trip, emptyMap()).options.map { it.score }

        assertEquals(scores.sortedDescending(), scores)
        assertTrue(scores.all { it in 0..100 })
    }

    @Test
    fun `sert ruzgar bisikletin puanini dusurur`() {
        val trip = TripRoutes(mapOf(TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 15, km = 4.0))))

        val calm = TripScorer.recommend(trip, mapOf(TravelMode.Bike to dryWeather(15, windKmh = 5.0))).best!!
        val windy = TripScorer.recommend(trip, mapOf(TravelMode.Bike to dryWeather(15, windKmh = 40.0))).best!!

        assertTrue(windy.score < calm.score)
        assertTrue(TripReason.StrongWind(40) in windy.reasons)
    }

    @Test
    fun `rota yoksa bos oneri doner`() {
        val result = TripScorer.recommend(TripRoutes(emptyMap()), emptyMap())

        assertTrue(result.options.isEmpty())
        assertEquals(TripHeadline.NoOptions, result.headline)
    }

    @Test
    fun `yavas ama kuru secenek onerilince nedeni baslikta yazar`() {
        // Bisiklet 15 dk ama 12 dk yağmurda; araba 20 dk (15 + 5 park) ve kuru.
        val trip = TripRoutes(
            mapOf(
                TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 15, km = 4.0)),
                TravelMode.Car to listOf(route(TravelMode.Car, minutes = 15, km = 5.0)),
            ),
        )
        val weather = mapOf(
            TravelMode.Bike to weatherWetFor(wetMinutes = 12, totalMinutes = 15),
            TravelMode.Car to weatherWetFor(wetMinutes = 12, totalMinutes = 15),
        )

        val headline = TripScorer.recommend(trip, weather).headline

        assertEquals(TripHeadline.SlowerButDrier(TravelMode.Car, extraMinutes = 5, quickest = TravelMode.Bike, quickestWetMinutes = 12), headline)
    }
}
