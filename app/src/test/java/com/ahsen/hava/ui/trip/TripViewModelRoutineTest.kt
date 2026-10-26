package com.ahsen.hava.ui.trip

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.usecase.PlanTripUseCase
import com.ahsen.hava.testutil.FakeLocationRepository
import com.ahsen.hava.testutil.FakePlaceRepository
import com.ahsen.hava.testutil.FakeRouteRepository
import com.ahsen.hava.testutil.FakeRouteWeatherRepository
import com.ahsen.hava.testutil.FakeRoutineRepository
import com.ahsen.hava.testutil.Fixtures.FakeTimeline
import com.ahsen.hava.testutil.Fixtures.NOW
import com.ahsen.hava.testutil.Fixtures.route
import com.ahsen.hava.testutil.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripViewModelRoutineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val home = Place("Ev", "Bornova", 38.46, 27.21)
    private val school = Place("Okul", "Çiğli", 38.49, 27.06)
    private val saved = Routine(home, school, TravelMode.Walk, setOf(DayOfWeek.MONDAY), LocalTime.of(7, 40))

    private val trip = TripRoutes(mapOf(TravelMode.Bike to listOf(route(TravelMode.Bike, minutes = 30, km = 8.0))))

    private fun viewModel(routines: FakeRoutineRepository) = TripViewModel(
        placeRepository = FakePlaceRepository(),
        locationRepository = FakeLocationRepository(null),
        planTrip = PlanTripUseCase(FakeRouteRepository(trip), FakeRouteWeatherRepository { FakeTimeline { 0 } }, clock = { NOW }),
        routineRepository = routines,
    )

    @Test
    fun `rutine dokununca rutin yolculugu ekrana gelir`() = runTest {
        val vm = viewModel(FakeRoutineRepository(saved))

        vm.onOpenRoutine()

        assertEquals(home, vm.origin.value)
        assertEquals(school, vm.destination.value)
        assertNull("Arama kapanmalı", vm.activeField.value)
    }

    @Test
    fun `ekrandaki yolculuk icin yeni rutin hafta ici varsayilaniyla acilir`() = runTest {
        val vm = viewModel(FakeRoutineRepository())
        vm.onOpenRoutineFor(home, school)

        vm.onEditRoutine()

        val editor = vm.routineEditor.value!!
        assertEquals(home, editor.initial.from)
        assertEquals(school, editor.initial.to)
        assertEquals(Routine.WEEKDAYS, editor.initial.days)
        assertFalse(editor.isExisting)
    }

    @Test
    fun `ayni yolculuk icin kayitli rutin duzenlenir`() = runTest {
        val vm = viewModel(FakeRoutineRepository(saved))
        vm.onOpenRoutine()

        vm.onEditRoutine()

        val editor = vm.routineEditor.value!!
        assertTrue(editor.isExisting)
        assertEquals(TravelMode.Walk, editor.initial.mode)
        assertEquals(LocalTime.of(7, 40), editor.initial.departure)
    }

    @Test
    fun `kaydetmek rutini acik olarak yazar ve sayfayi kapatir`() = runTest {
        val routines = FakeRoutineRepository()
        val vm = viewModel(routines)

        vm.onSaveRoutine(saved.copy(enabled = false))

        assertEquals(saved.copy(enabled = true), routines.current)
        assertNull(vm.routineEditor.value)
    }

    @Test
    fun `anahtar rutini kapatir, silmek tamamen kaldirir`() = runTest {
        val routines = FakeRoutineRepository(saved)
        val vm = viewModel(routines)

        vm.onRoutineEnabledChange(false)
        assertEquals(false, routines.current?.enabled)

        vm.onDeleteRoutine()
        assertNull(routines.current)
    }

    /** Testte yolculuğu arama ekranından geçmeden kurmanın kısa yolu. */
    private fun TripViewModel.onOpenRoutineFor(from: Place, to: Place) {
        onFieldClick(TripField.Origin)
        onPlaceSelected(from)
        onFieldClick(TripField.Destination)
        onPlaceSelected(to)
    }
}
