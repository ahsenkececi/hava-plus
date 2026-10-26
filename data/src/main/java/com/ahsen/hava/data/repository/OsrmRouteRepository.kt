package com.ahsen.hava.data.repository

import com.ahsen.hava.data.remote.OsrmResponse
import com.ahsen.hava.data.remote.RoutingApi
import com.ahsen.hava.data.remote.apiCall
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.domain.model.GeoPoint
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.repository.RouteRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** [RouteRepository]'nin OSRM (OpenStreetMap rota motoru) ile çalışan hali. */
internal class OsrmRouteRepository(
    private val api: RoutingApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RouteRepository {

    /**
     * Üç türü AYNI ANDA sorar. Biri başarısız olursa diğerleri yine gelir; hepsi başarısızsa
     * hata fırlatır (ViewModel Error durumuna çevirir). Çok uzak mesafelerde yürüyüş/bisiklet
     * hiç sorulmaz: 80 km yürüme rotası kimsenin işine yaramaz.
     */
    override suspend fun routes(from: Place, to: Place): TripRoutes = withContext(ioDispatcher) {
        val straightKm = from.distanceKmTo(to)
        val modes = buildList {
            if (straightKm <= MAX_WALK_KM) add(TravelMode.Walk)
            if (straightKm <= MAX_BIKE_KM) add(TravelMode.Bike)
            add(TravelMode.Car)
        }
        val coordinates = listOf(from, to).joinToString(";") {
            String.format(Locale.US, "%.6f,%.6f", it.longitude, it.latitude)
        }
        val results = coroutineScope {
            modes.map { mode ->
                async {
                    mode to try {
                        apiCall {
                            api.route(
                                server = serverFor(mode),
                                coordinates = coordinates,
                                alternatives = mode == TravelMode.Car,
                            )
                        }.toRoutes(mode)
                    } catch (e: DataError) {
                        null // bu tür alınamadı; diğerleri yine gösterilir
                    }
                }
            }.awaitAll()
        }
        val found = results.mapNotNull { (mode, routes) -> routes?.takeIf { it.isNotEmpty() }?.let { mode to it } }.toMap()
        if (found.isEmpty()) throw DataError.Network()
        TripRoutes(byMode = found, unavailable = modes.toSet() - found.keys)
    }

    private fun serverFor(mode: TravelMode) = when (mode) {
        TravelMode.Walk -> "routed-foot"
        TravelMode.Bike -> "routed-bike"
        TravelMode.Car, TravelMode.Transit -> "routed-car"
    }

    private companion object {
        const val MAX_WALK_KM = 25.0
        const val MAX_BIKE_KM = 60.0
    }
}

private fun OsrmResponse.toRoutes(mode: TravelMode): List<Route> {
    if (code != "Ok") return emptyList()
    return routes.orEmpty().mapNotNull { r ->
        val distance = r.distance ?: return@mapNotNull null
        val duration = r.duration ?: return@mapNotNull null
        val path = r.geometry?.coordinates.orEmpty().mapNotNull { c ->
            if (c.size >= 2) GeoPoint(latitude = c[1], longitude = c[0]) else null
        }
        Route(mode = mode, distanceMeters = distance, durationSeconds = duration, path = path)
    }
}
