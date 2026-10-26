package com.ahsen.hava.domain.usecase

import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.logic.WeatherInsights
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.WeatherRepository

/** "Konumunda şimdi" kartının içeriği. */
data class LocalWeatherReport(
    val city: City,
    val current: CurrentWeather,
    /** Yağmur yaklaşıyorsa ne zaman, ne kadar; yoksa null. */
    val rainOutlook: RainOutlook?,
    val todayMinC: Double? = null,
    val todayMaxC: Double? = null,
)

/**
 * Bulunduğun yerin şu anki havası: konumu bulur, o noktanın tahminini getirir, yağmur durumunu çıkarır.
 *
 * İki ayrı repository'yi tek bir iş adımında birleştirir; ViewModel bu sırayı bilmek zorunda kalmaz.
 */
class GetLocalWeatherUseCase(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
) {
    /** Konum bulunamazsa null. Ağ hataları çağırana fırlatılır. */
    suspend operator fun invoke(): LocalWeatherReport? {
        val city = locationRepository.currentCity() ?: return null
        val forecast = weatherRepository.getForecast(city.latitude, city.longitude)
        val today = forecast.daily.firstOrNull()
        return LocalWeatherReport(
            city = city,
            current = forecast.current,
            rainOutlook = WeatherInsights.rainOutlook(forecast),
            todayMinC = today?.minTemperatureC,
            todayMaxC = today?.maxTemperatureC,
        )
    }
}
