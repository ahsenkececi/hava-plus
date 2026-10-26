package com.ahsen.hava.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.repository.RoutineRepository
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Rutin, favoriler gibi tek bir anahtarda JSON olarak durur. Biçimi [RoutineCodec] belirler. */
internal class DataStoreRoutineRepository(
    private val dataStore: DataStore<Preferences>,
    private val codec: RoutineCodec = RoutineCodec(Gson()),
) : RoutineRepository {

    override val routine: Flow<Routine?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> codec.decode(prefs[KEY]) }

    override suspend fun save(routine: Routine) {
        dataStore.edit { prefs -> prefs[KEY] = codec.encode(routine) }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("routine")
    }
}
