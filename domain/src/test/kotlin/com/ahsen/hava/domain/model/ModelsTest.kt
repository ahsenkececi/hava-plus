package com.ahsen.hava.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun `WMO kodlari dogru hava durumuna cevrilir`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWeatherCode(0))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWeatherCode(48))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWeatherCode(63))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWeatherCode(95))
    }

    @Test
    fun `bilinmeyen kod uygulamayi cokertmez`() {
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWeatherCode(42))
    }

    @Test
    fun `iki yer arasindaki kus ucusu mesafe hesaplanir`() {
        val izmir = Place("İzmir", "", 38.4127, 27.1384)
        val ankara = Place("Ankara", "", 39.9199, 32.8543)

        assertEquals(521.0, izmir.distanceKmTo(ankara), 5.0)
        assertEquals(0.0, izmir.distanceKmTo(izmir), 0.0001)
    }

    @Test
    fun `rota birimleri dakika ve kilometreye cevrilir`() {
        val route = Route(TravelMode.Car, distanceMeters = 9_200.0, durationSeconds = 1_830.0, path = emptyList())

        assertEquals(30, route.durationMinutes)
        assertEquals(9.2, route.distanceKm, 0.0001)
        assertEquals(1, route.copy(durationSeconds = 20.0).durationMinutes)
    }

    @Test
    fun `sehir rozeti plaka varsa plakayi gosterir`() {
        val ankara = City(323786, "Ankara", 39.92, 32.85, "TR", "Türkiye", "Ankara", plate = 6)

        assertEquals("06", ankara.badge)
        assertEquals("TR", ankara.copy(plate = null).badge)
        assertEquals("Ankara, Türkiye", ankara.subtitle)
    }
}
