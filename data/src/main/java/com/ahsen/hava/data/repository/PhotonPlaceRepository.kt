package com.ahsen.hava.data.repository

import com.ahsen.hava.data.remote.PhotonFeature
import com.ahsen.hava.data.remote.PlaceApi
import com.ahsen.hava.data.remote.apiCall
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.repository.PlaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [PlaceRepository]'nin Photon (OpenStreetMap adres arama) ile çalışan hali. */
internal class PhotonPlaceRepository(
    private val api: PlaceApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlaceRepository {

    override suspend fun search(query: String, near: Place?): List<Place> = withContext(ioDispatcher) {
        apiCall { api.search(query.trim(), latitude = near?.latitude, longitude = near?.longitude) }
            .features.orEmpty()
            .mapNotNull { it.toPlace() }
            // Aynı yer birden çok kez gelebiliyor (ör. aynı binanın iki girişi).
            .distinctBy { "${it.name}|${it.detail}" }
    }
}

private fun PhotonFeature.toPlace(): Place? {
    val p = properties ?: return null
    if (!p.countrycode.equals("TR", ignoreCase = true)) return null
    val coords = geometry?.coordinates ?: return null
    if (coords.size < 2) return null
    val streetLine = listOfNotNull(p.street, p.housenumber).joinToString(" ").ifBlank { null }
    val name = p.name ?: streetLine ?: return null
    val detail = listOfNotNull(
        streetLine?.takeIf { it != name },
        p.district,
        p.city ?: p.county,
        p.state,
    ).distinct().filter { it != name }.joinToString(", ")
    return Place(name = name, detail = detail, latitude = coords[1], longitude = coords[0])
}
