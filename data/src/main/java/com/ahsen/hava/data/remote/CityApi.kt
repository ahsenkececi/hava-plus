package com.ahsen.hava.data.remote

import com.ahsen.hava.domain.model.City
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Şehir arama servisi. Gövdeyi Retrofit yazar, biz sadece adresi tarif ederiz.
 * Örnek: GET https://geocoding-api.open-meteo.com/v1/search?name=Ankara&count=10&language=tr&countryCode=TR
 */
internal interface CityApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "tr",
        // Sadece Türkiye'deki yerler (ISO ülke kodu).
        @Query("countryCode") countryCode: String = "TR",
    ): CitySearchResponse
}

/**
 * Open-Meteo şehir arama yanıtı.
 * Sonuç bulunamazsa servis `results` alanını hiç göndermez; bu yüzden nullable.
 */
internal data class CitySearchResponse(
    val results: List<CityDto>?,
)

/** Servisin gönderdiği şehir. JSON adları sadece burada bilinir; uygulamaya [City] olarak geçer. */
internal data class CityDto(
    val id: Long?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("country_code")
    val countryCode: String?,
    val country: String?,
    val admin1: String?,
)

/** Eksik alanı olan kayıt (ör. koordinatsız) gösterilmez: null döner. */
internal fun CityDto.toCity(): City? {
    return City(
        id = id ?: return null,
        name = name ?: return null,
        latitude = latitude ?: return null,
        longitude = longitude ?: return null,
        countryCode = countryCode,
        country = country,
        admin1 = admin1,
    )
}
