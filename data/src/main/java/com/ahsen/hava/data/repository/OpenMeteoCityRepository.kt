package com.ahsen.hava.data.repository

import com.ahsen.hava.data.remote.CityApi
import com.ahsen.hava.data.remote.apiCall
import com.ahsen.hava.data.remote.toCity
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.repository.CityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [CityRepository]'nin Open-Meteo geocoding servisiyle çalışan hali. */
internal class OpenMeteoCityRepository(
    private val api: CityApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CityRepository {

    override fun popularCities(): List<City> = PopularCities.all

    override suspend fun search(query: String): List<City> = withContext(ioDispatcher) {
        // Ağ işi ana thread'de yapılmaz: IO havuzuna geçiyoruz.
        apiCall { api.search(query.trim()) }.results.orEmpty()
            .mapNotNull { it.toCity() }
            // Güvenlik ağı: servis filtreyi uygulamasa bile listede yalnızca Türkiye kalsın.
            .filter { it.countryCode.equals("TR", ignoreCase = true) }
    }
}
