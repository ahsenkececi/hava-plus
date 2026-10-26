package com.ahsen.hava.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * DataStore: SharedPreferences'ın modern, coroutine dostu hali.
 * Şehir listesini Gson ile JSON metnine çevirip tek bir anahtarda tutuyoruz.
 */
internal class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson = Gson(),
) : FavoritesRepository {

    override val favorites: Flow<List<City>> = dataStore.data
        // Dosya okunamazsa uygulama çökmesin: boş listeyle devam.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[KEY]) }

    override suspend fun toggle(city: City) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            val updated = if (current.any { it.id == city.id }) {
                current.filterNot { it.id == city.id }
            } else {
                listOf(city) + current
            }
            prefs[KEY] = gson.toJson(updated.map { StoredCity.from(it) })
        }
    }

    private fun decode(json: String?): List<City> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<StoredCity>>(json, LIST_TYPE).orEmpty().mapNotNull { it.toCity() }
        } catch (e: JsonParseException) {
            emptyList()
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("favorite_cities")
        val LIST_TYPE = object : TypeToken<List<StoredCity>>() {}.type
    }
}
