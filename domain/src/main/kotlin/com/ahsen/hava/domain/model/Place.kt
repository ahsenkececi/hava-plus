package com.ahsen.hava.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Yolculuğun başlangıç ya da varış noktası: bir adres, bina, durak ya da semt.
 * (Şehirden daha ayrıntılı; "Ege Üniversitesi Rektörlüğü" gibi.)
 */
data class Place(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** Kuş uçuşu mesafe (km), Haversine formülüyle: dünyanın yuvarlaklığını hesaba katar. */
    fun distanceKmTo(other: Place): Double {
        val r = 6_371.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
