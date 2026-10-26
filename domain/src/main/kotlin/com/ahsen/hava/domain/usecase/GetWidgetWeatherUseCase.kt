package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.logic.WeatherInsights
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.first

/** Widget'ın göstereceği şehir ve tahmini. */
data class WidgetWeather(
    val city: City,
    val forecast: Forecast,
    val rainOutlook: RainOutlook?,
)

/**
 * Ana ekran widget'ının havası. Widget arka planda güncellendiği için GPS'e gitmez;
 * şehri şu sırayla seçer: en son bulunan konum → ilk favori → İstanbul.
 */
class GetWidgetWeatherUseCase(
    private val locationRepository: LocationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val weatherRepository: WeatherRepository,
) {
    /** Ağ hataları çağırana fırlatılır. */
    suspend operator fun invoke(): WidgetWeather {
        val city = locationRepository.lastKnownCity()
            ?: favoritesRepository.favorites.first().firstOrNull()
            ?: PopularCities.all.first()
        val forecast = weatherRepository.getForecast(city.latitude, city.longitude)
        return WidgetWeather(city, forecast, WeatherInsights.rainOutlook(forecast))
    }
}
