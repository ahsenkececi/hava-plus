package com.ahsen.hava.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rota servisi: OSRM, OpenStreetMap verisiyle (routing.openstreetmap.de, FOSSGIS topluluğu).
 * Her ulaşım türü için ayrı sunucu var: routed-car, routed-bike, routed-foot.
 *
 * Örnek: GET https://routing.openstreetmap.de/routed-foot/route/v1/driving/29.15,40.43;29.06,40.19?overview=simplified&geometries=geojson
 * Not: Koordinatlar "boylam,enlem" sırasıyla yazılır.
 */
internal interface RoutingApi {

    @GET("{server}/route/v1/driving/{coordinates}")
    suspend fun route(
        @Path("server") server: String,
        // "lon,lat;lon,lat": virgül ve noktalı virgül kaçışlanmasın diye encoded = true
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "simplified",
        @Query("geometries") geometries: String = "geojson",
        @Query("alternatives") alternatives: Boolean = false,
        @Query("steps") steps: Boolean = false,
    ): OsrmResponse
}

internal data class OsrmResponse(
    val code: String? = null,
    val routes: List<OsrmRoute>? = null,
)

internal data class OsrmRoute(
    /** metre */
    val distance: Double? = null,
    /** saniye */
    val duration: Double? = null,
    val geometry: OsrmGeometry? = null,
)

/** GeoJSON çizgi: [[boylam, enlem], ...] */
internal data class OsrmGeometry(val coordinates: List<List<Double>>? = null)
