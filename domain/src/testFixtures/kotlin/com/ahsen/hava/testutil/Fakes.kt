package com.ahsen.hava.testutil

import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.ahsen.hava.domain.repository.CityRepository
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.PlaceRepository
import com.ahsen.hava.domain.repository.RouteRepository
import com.ahsen.hava.domain.repository.RouteWeatherRepository
import com.ahsen.hava.domain.repository.RoutineRepository
import com.ahsen.hava.domain.repository.WeatherRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/*
 * Sahte repository'ler: ağa, diske ya da GPS'e gitmeden istenen cevabı verirler.
 * ViewModel ve use case'ler sadece arayüzleri tanıdığı için gerçeklerin yerine bunlar takılabilir.
 * (Katmanlı mimarinin testte kazandırdığı şey tam olarak bu.)
 */

class FakeCityRepository : CityRepository {
    var result: List<City> = emptyList()
    var error: Exception? = null
    val queries = mutableListOf<String>()

    override fun popularCities(): List<City> = PopularCities.all

    override suspend fun search(query: String): List<City> {
        queries += query
        error?.let { throw it }
        return result
    }
}

class FakeWeatherRepository(var forecast: Forecast = Fixtures.forecast()) : WeatherRepository {
    var calls = 0

    override suspend fun getForecast(latitude: Double, longitude: Double): Forecast {
        calls++
        return forecast
    }

    override suspend fun getSnapshot(latitude: Double, longitude: Double): WeatherSnapshot =
        WeatherSnapshot(forecast.current, forecast.daily.firstOrNull()?.minTemperatureC, forecast.daily.firstOrNull()?.maxTemperatureC)
}

class FakeLocationRepository(var city: City?, var lastKnown: City? = null) : LocationRepository {
    override suspend fun currentCity(): City? = city

    override suspend fun lastKnownCity(): City? = lastKnown
}

class FakeFavoritesRepository : FavoritesRepository {
    private val state = MutableStateFlow<List<City>>(emptyList())
    override val favorites: Flow<List<City>> = state

    override suspend fun toggle(city: City) {
        state.value = if (state.value.any { it.id == city.id }) state.value - city else listOf(city) + state.value
    }
}

class FakeRouteRepository(var trip: TripRoutes) : RouteRepository {
    override suspend fun routes(from: Place, to: Place): TripRoutes = trip
}

class FakeRouteWeatherRepository(
    private val timelineFor: (Route) -> RouteWeatherTimeline,
) : RouteWeatherRepository {
    var requestedRoutes: List<Route> = emptyList()
    var requestedAt: LocalDateTime? = null

    override suspend fun timelines(routes: List<Route>, now: LocalDateTime): Map<Route, RouteWeatherTimeline> {
        requestedRoutes = routes
        requestedAt = now
        return routes.associateWith(timelineFor)
    }
}

class FakePlaceRepository(var result: List<Place> = emptyList()) : PlaceRepository {
    override suspend fun search(query: String, near: Place?): List<Place> = result
}

class FakeRoutineRepository(initial: Routine? = null) : RoutineRepository {
    private val state = MutableStateFlow(initial)
    override val routine: Flow<Routine?> = state

    /** Son kaydedilen değer (testte doğrulamak için). */
    val current: Routine? get() = state.value

    override suspend fun save(routine: Routine) {
        state.value = routine
    }

    override suspend fun clear() {
        state.value = null
    }
}
