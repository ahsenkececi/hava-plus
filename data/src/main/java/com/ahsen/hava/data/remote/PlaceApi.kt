package com.ahsen.hava.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Adres ve yer arama: Photon (OpenStreetMap verisi, ücretsiz, anahtar istemez).
 * Örnek: GET https://photon.komoot.io/api/?q=ege%20üniversitesi&limit=8&lat=38.4&lon=27.2&bbox=...
 *
 * Adil kullanım: yazarken her harfte değil, kullanıcı durunca (debounce) istek atıyoruz.
 */
internal interface PlaceApi {

    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 8,
        // Konum biliniyorsa yakındaki sonuçlar öne gelir. null olan parametreyi Retrofit hiç göndermez.
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null,
        // Türkiye'yi kaplayan dikdörtgen: batı, güney, doğu, kuzey.
        @Query("bbox") boundingBox: String = TURKEY_BBOX,
    ): PhotonResponse

    companion object {
        const val TURKEY_BBOX = "25.5,35.8,45.0,42.2"
    }
}

/** Photon yanıtı GeoJSON biçimindedir: her sonuç bir "feature". */
internal data class PhotonResponse(val features: List<PhotonFeature>? = null)

internal data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null,
)

/** GeoJSON'da koordinat sırası [boylam, enlem]: alışılanın tersi! */
internal data class PhotonGeometry(val coordinates: List<Double>? = null)

internal data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val district: String? = null,
    val city: String? = null,
    val county: String? = null,
    val state: String? = null,
    val countrycode: String? = null,
)
