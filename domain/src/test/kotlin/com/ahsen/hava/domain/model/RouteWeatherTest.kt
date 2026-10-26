package com.ahsen.hava.domain.model

import com.ahsen.hava.testutil.Fixtures.point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteWeatherTest {

    // Üç örnek nokta: 0., 10. ve 20. dakika; yol 30 dakika. Her nokta sonraki noktaya kadar olan parçayı temsil eder.
    private fun weather(vararg wet: Boolean) =
        RouteWeather(wet.mapIndexed { i, w -> point(minute = i * 10, wet = w) }, totalMinutes = 30)

    @Test
    fun `islak parcalarin suresi toplanir`() {
        assertEquals(0, weather(false, false, false).wetMinutes)
        assertEquals(20, weather(true, false, true).wetMinutes)
        assertEquals(30, weather(true, true, true).wetMinutes)
    }

    @Test
    fun `yagmurun yolun neresinde oldugu cumleye doker`() {
        assertEquals(RouteRain.FirstMinutes(10), weather(true, false, false).rain())
        assertEquals(RouteRain.LastMinutes(10), weather(false, false, true).rain())
        assertEquals(RouteRain.After(startMinute = 10, minutes = 10), weather(false, true, false).rain())
        assertEquals(RouteRain.AllAlong, weather(true, true, true).rain())
        assertEquals(18, weather(true, false, false).feelsLikeC)
    }

    @Test
    fun `kuru ama yagis ihtimali yuksekse uyarir`() {
        val risky = RouteWeather(listOf(point(0, probability = 55)), totalMinutes = 15)

        assertEquals(RouteRain.Dry(highProbability = 55), risky.rain())
        assertEquals(RouteRain.Dry(highProbability = null), weather(false, false, false).rain())
    }

    @Test
    fun `yagmur kodu varsa miktar dusuk olsa da islak sayilir`() {
        val drizzleCode = point(0).copy(precipitationMm = 0.0, weatherCode = 63)

        assertTrue(drizzleCode.isWet)
        assertFalse(point(0).isWet)
    }
}
