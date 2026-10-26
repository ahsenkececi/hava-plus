package com.ahsen.hava.domain.logic

import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.testutil.Fixtures.forecast
import com.ahsen.hava.testutil.Fixtures.hour
import com.ahsen.hava.testutil.Fixtures.slots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherInsightsTest {

    @Test
    fun `yaklasan yagmurun baslangici ve suresi soylenir`() {
        // 14:00 kuru, 14:15 kuru, 14:30 kuru, 14:45 ve 15:00 yağmur, 15:15 kuru
        val f = forecast(minutely15 = slots(0.0, 0.0, 0.0, 0.8, 1.2, 0.0))

        assertEquals(RainOutlook.StartsIn(minutes = 45, durationMinutes = 30, lastsBeyondForecast = false), WeatherInsights.rainOutlook(f))
    }

    @Test
    fun `yagmur su an yagiyorsa ne zaman dinecegi soylenir`() {
        val f = forecast(minutely15 = slots(1.0, 0.5, 0.0, 0.0))

        assertEquals(RainOutlook.RainingStopsIn(30), WeatherInsights.rainOutlook(f))
    }

    @Test
    fun `yagmur hic dinmiyorsa en az iki saat denir`() {
        val f = forecast(minutely15 = slots(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0))

        assertEquals(RainOutlook.RainingForAWhile, WeatherInsights.rainOutlook(f))
    }

    @Test
    fun `yakin zamanda yagmur yoksa saatlik tahmine bakilir`() {
        val hours = (0..6).map { h -> hour(NOW.plusHours(h.toLong()), probability = if (h == 3) 70 else 10) }
        val f = forecast(minutely15 = slots(0.0, 0.0, 0.0), hourly = hours)

        assertEquals(RainOutlook.ExpectedAround(NOW.plusHours(3), 70), WeatherInsights.rainOutlook(f))
    }

    @Test
    fun `hic yagmur yoksa mesaj gosterilmez`() {
        val hours = (0..6).map { h -> hour(NOW.plusHours(h.toLong()), probability = 10) }

        assertNull(WeatherInsights.rainOutlook(forecast(minutely15 = slots(0.0, 0.0), hourly = hours)))
    }

    @Test
    fun `uv seviyeleri DSO olcegine uyar`() {
        assertEquals(UvLevel.Low, UvLevel.from(2.9))
        assertEquals(UvLevel.Moderate, UvLevel.from(3.0))
        assertEquals(UvLevel.High, UvLevel.from(7.9))
        assertEquals(UvLevel.VeryHigh, UvLevel.from(10.0))
        assertEquals(UvLevel.Extreme, UvLevel.from(11.0))
    }

    @Test
    fun `anlik uv su anki saatten okunur`() {
        val hours = listOf(hour(NOW.minusHours(1), uv = 2.0), hour(NOW, uv = 5.5), hour(NOW.plusHours(1), uv = 6.0))

        assertEquals(5.5, WeatherInsights.currentUv(forecast(hourly = hours))!!, 0.0)
    }

    @Test
    fun `ruzgar yonu sekiz ana yone yuvarlanir`() {
        assertEquals(CompassPoint.N, WeatherInsights.compassPoint(0.0))
        assertEquals(CompassPoint.N, WeatherInsights.compassPoint(350.0))
        assertEquals(CompassPoint.NE, WeatherInsights.compassPoint(45.0))
        assertEquals(CompassPoint.S, WeatherInsights.compassPoint(200.0))
        assertEquals(CompassPoint.W, WeatherInsights.compassPoint(-90.0))
    }

    @Test
    fun `yagis siddeti 15 dakikalik miktara gore siniflanir`() {
        assertEquals(RainIntensity.None, RainIntensity.from(0.05))
        assertEquals(RainIntensity.Light, RainIntensity.from(0.3))
        assertEquals(RainIntensity.Moderate, RainIntensity.from(1.0))
        assertEquals(RainIntensity.Heavy, RainIntensity.from(2.5))
    }

    @Test
    fun `onumuzdeki iki saat gecmis dilimleri atlar ve sekiz dilim alir`() {
        // 14:00'dan önceki bir dilim + 10 dilim; ilki atlanmalı, sonra 8 tane alınmalı
        val f = forecast(minutely15 = slots(9.0, 0.0, 0.0, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, from = NOW.minusMinutes(15)))

        val next = WeatherInsights.nextTwoHours(f)

        assertEquals(8, next.size)
        assertEquals(NOW, next.first().time)
        assertTrue(WeatherInsights.hasRainSoon(f))
    }

    @Test
    fun `iki saat kuruysa grafik gosterilmez`() {
        assertFalse(WeatherInsights.hasRainSoon(forecast(minutely15 = slots(0.0, 0.0, 0.05, 0.0))))
    }

    @Test
    fun `gorus mesafesi anlamlandirilir`() {
        assertEquals(VisibilityLevel.Fog, VisibilityLevel.from(600.0))
        assertEquals(VisibilityLevel.Haze, VisibilityLevel.from(2_500.0))
        assertEquals(VisibilityLevel.Good, VisibilityLevel.from(8_000.0))
        assertEquals(VisibilityLevel.Excellent, VisibilityLevel.from(24_000.0))
    }

    @Test
    fun `cig noktasi bunalti hissine cevrilir`() {
        assertEquals(DewPointComfort.Dry, DewPointComfort.from(4.0))
        assertEquals(DewPointComfort.Comfortable, DewPointComfort.from(13.0))
        assertEquals(DewPointComfort.Humid, DewPointComfort.from(18.0))
        assertEquals(DewPointComfort.Oppressive, DewPointComfort.from(22.0))
    }
}
