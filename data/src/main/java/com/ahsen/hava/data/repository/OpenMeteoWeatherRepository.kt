package com.ahsen.hava.data.repository

import com.ahsen.hava.data.remote.ForecastApi
import com.ahsen.hava.data.remote.apiCall
import com.ahsen.hava.data.remote.toForecast
import com.ahsen.hava.data.remote.toSnapshot
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.ahsen.hava.domain.repository.WeatherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [WeatherRepository]'nin Open-Meteo ile çalışan hali. */
internal class OpenMeteoWeatherRepository(
    private val api: ForecastApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {

    override suspend fun getForecast(latitude: Double, longitude: Double): Forecast =
        withContext(ioDispatcher) {
            apiCall { api.getForecast(latitude = latitude, longitude = longitude).toForecast() }
        }

    override suspend fun getSnapshot(latitude: Double, longitude: Double): WeatherSnapshot =
        withContext(ioDispatcher) {
            apiCall { api.getSnapshot(latitude = latitude, longitude = longitude).toSnapshot() }
        }
}
