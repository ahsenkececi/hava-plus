package com.ahsen.hava.ui.city

import androidx.lifecycle.SavedStateHandle
import com.ahsen.hava.R
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.usecase.GetLocalWeatherUseCase
import com.ahsen.hava.testutil.FakeCityRepository
import com.ahsen.hava.testutil.FakeFavoritesRepository
import com.ahsen.hava.testutil.FakeLocationRepository
import com.ahsen.hava.testutil.FakeWeatherRepository
import com.ahsen.hava.testutil.MainDispatcherRule
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cities = FakeCityRepository()
    private val weather = FakeWeatherRepository()
    private val gemlik = City(-1, "Gemlik", 40.43, 29.15, "TR", "Türkiye", "Bursa")

    private fun viewModel(location: City? = gemlik) = CityViewModel(
        savedStateHandle = SavedStateHandle(),
        repository = cities,
        favoritesRepository = FakeFavoritesRepository(),
        weatherRepository = weather,
        getLocalWeather = GetLocalWeatherUseCase(FakeLocationRepository(location), weather),
    )

    @Test
    fun `acilista populer sehirler gosterilir`() {
        assertEquals(LoadState.Success(PopularCities.all), viewModel().uiState.value)
    }

    @Test
    fun `yazmayi birakinca arama yapilir`() = runTest {
        val izmir = PopularCities.all.first { it.name == "İzmir" }
        cities.result = listOf(izmir)
        val vm = viewModel()

        vm.onQueryChange("izm")
        advanceTimeBy(300)
        assertTrue("400 ms dolmadan istek atılmamalı", cities.queries.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf("izm"), cities.queries)
        assertEquals(LoadState.Success(listOf(izmir)), vm.uiState.value)
    }

    @Test
    fun `hizli yazarken sadece son metin aranir`() = runTest {
        val vm = viewModel()

        vm.onQueryChange("an")
        vm.onQueryChange("ank")
        vm.onQueryChange("anka")
        advanceUntilIdle()

        assertEquals(listOf("anka"), cities.queries)
    }

    @Test
    fun `sonuc yoksa bos durum gosterilir`() = runTest {
        val vm = viewModel()

        vm.onQueryChange("zzzz")
        advanceUntilIdle()

        assertEquals(LoadState.Empty, vm.uiState.value)
    }

    @Test
    fun `baglanti yoksa anlasilir bir hata mesaji gosterilir`() = runTest {
        cities.error = DataError.Network()
        val vm = viewModel()

        vm.onQueryChange("izmir")
        advanceUntilIdle()

        assertEquals(LoadState.Error(UiText(R.string.error_network)), vm.uiState.value)
    }

    @Test
    fun `konum izni verilince konumdaki hava karta gelir`() = runTest {
        val vm = viewModel()

        vm.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        val state = vm.localWeather.value
        assertTrue(state is CityViewModel.LocalWeather.Ready)
        assertEquals(gemlik, (state as CityViewModel.LocalWeather.Ready).city)
    }

    @Test
    fun `konum bulunamazsa kart nedenini soyler`() = runTest {
        val vm = viewModel(location = null)

        vm.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        assertTrue(vm.localWeather.value is CityViewModel.LocalWeather.Failed)
    }

    @Test
    fun `izin verilmezse kart izin ister`() {
        val vm = viewModel()

        vm.onLocationPermissionResult(granted = false)

        assertEquals(CityViewModel.LocalWeather.NeedsPermission, vm.localWeather.value)
    }
}
