package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.testutil.FakeFavoritesRepository
import com.ahsen.hava.testutil.FakeLocationRepository
import com.ahsen.hava.testutil.FakeWeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWidgetWeatherUseCaseTest {

    private val bornova = City(-1, "Bornova", 38.46, 27.21, "TR", "Türkiye", "İzmir")
    private val ankara = PopularCities.all.first { it.name == "Ankara" }
    private val favorites = FakeFavoritesRepository()

    private fun useCase(lastKnown: City?) = GetWidgetWeatherUseCase(
        // currentCity verilse bile kullanılmamalı: widget GPS'e gitmez.
        FakeLocationRepository(city = ankara, lastKnown = lastKnown),
        favorites,
        FakeWeatherRepository(),
    )

    @Test
    fun `en son bulunan konum varsa o gosterilir`() = runTest {
        favorites.toggle(ankara)
        assertEquals(bornova, useCase(lastKnown = bornova)().city)
    }

    @Test
    fun `konum hic bulunmadiysa ilk favori gosterilir`() = runTest {
        favorites.toggle(ankara)
        assertEquals(ankara, useCase(lastKnown = null)().city)
    }

    @Test
    fun `konum da favori de yoksa istanbul gosterilir`() = runTest {
        assertEquals("İstanbul", useCase(lastKnown = null)().city.name)
    }
}
