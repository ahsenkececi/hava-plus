package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.testutil.FakeLocationRepository
import com.ahsen.hava.testutil.FakeWeatherRepository
import com.ahsen.hava.testutil.Fixtures.forecast
import com.ahsen.hava.testutil.Fixtures.slots
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetLocalWeatherUseCaseTest {

    private val gemlik = City(-1, "Gemlik", 40.43, 29.15, "TR", "Türkiye", "Bursa")

    @Test
    fun `konum bulunamazsa hava istenmez`() = runTest {
        val weather = FakeWeatherRepository()
        val useCase = GetLocalWeatherUseCase(FakeLocationRepository(city = null), weather)

        assertNull(useCase())
        assertEquals(0, weather.calls)
    }

    @Test
    fun `konum bulununca o yerin havasi ve yagmur cumlesi doner`() = runTest {
        val rainy = forecast(minutely15 = slots(0.0, 0.0, 0.0, 0.8, 0.0))
        val useCase = GetLocalWeatherUseCase(FakeLocationRepository(gemlik), FakeWeatherRepository(rainy))

        val report = useCase()!!

        assertEquals(gemlik, report.city)
        assertEquals(rainy.current, report.current)
        assertEquals(RainOutlook.StartsIn(minutes = 45, durationMinutes = 15, lastsBeyondForecast = false), report.rainOutlook)
    }
}
