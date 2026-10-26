package com.ahsen.hava.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.ahsen.hava.data.local.DataStoreFavoritesRepository
import com.ahsen.hava.data.local.DataStoreRoutineRepository
import com.ahsen.hava.data.local.DataStoreSettingsRepository
import com.ahsen.hava.data.remote.ApiClient
import com.ahsen.hava.data.repository.AndroidLocationRepository
import com.ahsen.hava.data.repository.OpenMeteoCityRepository
import com.ahsen.hava.data.repository.OpenMeteoRouteWeatherRepository
import com.ahsen.hava.data.repository.OpenMeteoWeatherRepository
import com.ahsen.hava.data.repository.OsrmRouteRepository
import com.ahsen.hava.data.repository.PhotonPlaceRepository
import com.ahsen.hava.domain.repository.CityRepository
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.PlaceRepository
import com.ahsen.hava.domain.repository.RouteRepository
import com.ahsen.hava.domain.repository.RoutineRepository
import com.ahsen.hava.domain.repository.RouteWeatherRepository
import com.ahsen.hava.domain.repository.SettingsRepository
import com.ahsen.hava.domain.repository.WeatherRepository

/**
 * `:data` modülünün dışarıya açılan TEK kapısı.
 *
 * Bu modüldeki bütün sınıflar (Retrofit servisleri, JSON modelleri, Open-Meteo / OSRM / Photon
 * uygulamaları, DataStore) `internal`: başka modül onları göremez, sadece buradan verilen
 * domain arayüzlerini görür. Yani uygulama "Retrofit kullanıyoruz" bilgisine hiç sahip değil;
 * yarın servis değişirse `:app` modülünde tek satır bile değişmez.
 *
 * Uygulama ömrü boyunca bir kez oluşturulmalı (DataStore aynı dosya için tek örnek ister).
 */
class DataModule(context: Context) {

    private val appContext = context.applicationContext

    val cityRepository: CityRepository by lazy { OpenMeteoCityRepository(ApiClient.cityApi) }

    val weatherRepository: WeatherRepository by lazy { OpenMeteoWeatherRepository(ApiClient.forecastApi) }

    val placeRepository: PlaceRepository by lazy { PhotonPlaceRepository(ApiClient.placeApi) }

    val routeRepository: RouteRepository by lazy { OsrmRouteRepository(ApiClient.routingApi) }

    val routeWeatherRepository: RouteWeatherRepository by lazy {
        OpenMeteoRouteWeatherRepository(ApiClient.forecastApi)
    }

    val favoritesRepository: FavoritesRepository by lazy {
        DataStoreFavoritesRepository(appContext.favoritesDataStore)
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.settingsDataStore)
    }

    val routineRepository: RoutineRepository by lazy {
        DataStoreRoutineRepository(appContext.routineDataStore)
    }

    val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(appContext, appContext.locationDataStore)
    }
}

/** Favoriler cihazda küçük bir anahtar-değer dosyasında saklanır. */
private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/** Tercihler ayrı bir dosyada. */
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Rutin (düzenli yolculuk) kendi dosyasında. */
private val Context.routineDataStore by preferencesDataStore(name = "routine")

/** En son bulunan konum (widget için). */
private val Context.locationDataStore by preferencesDataStore(name = "location")
