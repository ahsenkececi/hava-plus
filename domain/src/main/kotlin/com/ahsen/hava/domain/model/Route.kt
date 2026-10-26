package com.ahsen.hava.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Ulaşım türleri. */
enum class TravelMode {
    Walk,
    Bike,
    Car,
    Transit,
}

/** Haritada bir nokta. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    /**
     * Bu noktadan [other]'a giderken izlenen yön (pusula derecesi): 0 = kuzey, 90 = doğu, 180 = güney.
     * Yan rüzgâr hesabında yolun hangi yöne gittiğini bulmak için.
     */
    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}

/** Bir ulaşım türüyle bir rota: mesafe, süre ve yol üzerindeki noktalar. */
data class Route(
    val mode: TravelMode,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val path: List<GeoPoint>,
) {
    val durationMinutes: Int get() = (durationSeconds / 60).toInt().coerceAtLeast(1)
    val distanceKm: Double get() = distanceMeters / 1000
}

/**
 * İki nokta arasındaki bütün seçenekler. Her türün ilk rotası en iyisidir, varsa
 * diğerleri alternatiftir. Alınamayan türler [unavailable] içinde (örn. servis cevap vermedi).
 */
data class TripRoutes(
    val byMode: Map<TravelMode, List<Route>>,
    val unavailable: Set<TravelMode> = emptySet(),
) {
    fun best(mode: TravelMode): Route? = byMode[mode]?.firstOrNull()
}
