package com.ahsen.hava.di

import android.app.Application
import com.ahsen.hava.data.DataModule
import com.ahsen.hava.domain.repository.CityRepository
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.PlaceRepository
import com.ahsen.hava.domain.repository.RoutineRepository
import com.ahsen.hava.domain.repository.SettingsRepository
import com.ahsen.hava.domain.repository.WeatherRepository
import com.ahsen.hava.domain.usecase.CheckRoutineUseCase
import com.ahsen.hava.domain.usecase.CheckUpcomingRoutineUseCase
import com.ahsen.hava.domain.usecase.GetLocalWeatherUseCase
import com.ahsen.hava.domain.usecase.GetWidgetWeatherUseCase
import com.ahsen.hava.domain.usecase.PlanTripUseCase

/**
 * Elle bağımlılık enjeksiyonu (manual DI): uygulamanın "kompozisyon kökü".
 *
 * Modüller şöyle bağlanır:
 *   :app  ──>  :domain  <──  :data
 *     └──────────────────────┘ (sadece DataModule üzerinden)
 *
 * - Repository'leri `:data` modülünün tek açık kapısı [DataModule] verir; burada yalnızca
 *   domain arayüzleri görünür. Retrofit, Gson, DataStore bu modülden görülemez bile.
 * - Use case'ler burada repository'lerden kurulur.
 * - ViewModel'ler bu nesneleri varsayılan parametre olarak alır; testlerde sahteleri verilir.
 *
 * Hilt aynı işi otomatik yapar; bu boyutta bir uygulamada elle kurmak hem daha hızlı derlenir
 * hem de bağımlılıkların nereden geldiğini açıkça gösterir.
 * [com.ahsen.hava.HavaApp] açılışta [init] çağırır.
 */
object AppContainer {

    private lateinit var data: DataModule

    fun init(application: Application) {
        data = DataModule(application)
    }

    // ---- Repository'ler (arayüz olarak) ----

    val cityRepository: CityRepository get() = data.cityRepository

    val weatherRepository: WeatherRepository get() = data.weatherRepository

    val placeRepository: PlaceRepository get() = data.placeRepository

    val favoritesRepository: FavoritesRepository get() = data.favoritesRepository

    val settingsRepository: SettingsRepository get() = data.settingsRepository

    val locationRepository: LocationRepository get() = data.locationRepository

    val routineRepository: RoutineRepository get() = data.routineRepository

    // ---- Use case'ler (iş kuralları) ----

    val getLocalWeather: GetLocalWeatherUseCase by lazy {
        GetLocalWeatherUseCase(data.locationRepository, data.weatherRepository)
    }

    val planTrip: PlanTripUseCase by lazy {
        PlanTripUseCase(data.routeRepository, data.routeWeatherRepository)
    }

    val checkRoutine: CheckRoutineUseCase by lazy { CheckRoutineUseCase(planTrip) }

    val checkUpcomingRoutine: CheckUpcomingRoutineUseCase by lazy {
        CheckUpcomingRoutineUseCase(data.routineRepository, checkRoutine)
    }

    val getWidgetWeather: GetWidgetWeatherUseCase by lazy {
        GetWidgetWeatherUseCase(data.locationRepository, data.favoritesRepository, data.weatherRepository)
    }
}
