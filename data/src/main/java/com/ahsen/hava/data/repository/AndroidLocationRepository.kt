package com.ahsen.hava.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahsen.hava.data.R
import com.ahsen.hava.data.local.StoredCity
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.repository.LocationRepository
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Android'in kendi LocationManager'ı ile konum (ek kütüphane yok) +
 * Geocoder ile koordinatı şehir adına çevirme ("ters geocoding").
 *
 * İzin kontrolü ekranda yapılır; buraya ancak izin verildikten sonra gelinir.
 *
 * Bulunan son şehir [cache]'e yazılır: widget gibi arka planda çalışan yerler GPS'e gitmeden onu kullanır.
 */
internal class AndroidLocationRepository(
    private val context: Context,
    private val cache: DataStore<Preferences>,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun currentCity(): City? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { manager.isProviderEnabled(it) } ?: return null

        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { freshLocation(manager, provider) }
            ?: manager.getLastKnownLocation(provider)
            ?: return null

        val city = withContext(ioDispatcher) { toCity(location) }
        cache.edit { it[LAST_CITY] = gson.toJson(StoredCity.from(city)) }
        return city
    }

    override suspend fun lastKnownCity(): City? {
        val json = cache.data.first()[LAST_CITY] ?: return null
        return try {
            gson.fromJson(json, StoredCity::class.java)?.toCity()
        } catch (e: JsonParseException) {
            null
        }
    }

    /** Tek seferlik güncel konum. Android 11+ yeni API, öncesinde eski tek seferlik istek. */
    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            } else {
                val listener = LocationListener { location -> if (cont.isActive) cont.resume(location) }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        }

    /** Koordinatı "Bornova, İzmir" gibi okunur bir yere çevirir. Olmazsa genel bir ad verir (strings.xml). */
    private fun toCity(location: Location): City {
        val address = try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.forLanguageTag("tr"))
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
        val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: context.getString(R.string.location_fallback_name)
        return City(
            id = idFor(location.latitude, location.longitude),
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            countryCode = address?.countryCode,
            country = address?.countryName,
            admin1 = address?.adminArea?.takeIf { it != name },
        )
    }

    /** Konumdan gelen şehirlerin API kimliği yok; koordinattan sabit bir kimlik üretiyoruz. */
    private fun idFor(lat: Double, lon: Double): Long =
        -((lat * 100).roundToLong() * 100_000 + (lon * 100).roundToLong())

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
        val LAST_CITY = stringPreferencesKey("last_city")
    }
}
