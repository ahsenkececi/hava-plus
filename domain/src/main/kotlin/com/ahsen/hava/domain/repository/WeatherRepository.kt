package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.WeatherSnapshot

/** Hava tahmininin tek kapısı. CityRepository ile aynı desen. */
interface WeatherRepository {
    suspend fun getForecast(latitude: Double, longitude: Double): Forecast

    /** Şu anki hava + bugünün en düşük/en yüksek sıcaklığı (favori kartları için hafif istek). */
    suspend fun getSnapshot(latitude: Double, longitude: Double): WeatherSnapshot
}
