package com.ahsen.hava.data.local

import com.ahsen.hava.domain.model.City
import com.google.gson.annotations.SerializedName

/**
 * Şehrin diske yazılan biçimi (favoriler ve en son bulunan konum). Alan adları eski sürümle
 * aynı tutuldu: güncellemeden önce eklenen favoriler kaybolmaz. Domain modeli değişse bile kayıt biçimi bu sınıfta sabit kalır.
 */
internal data class StoredCity(
    val id: Long?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("country_code")
    val countryCode: String?,
    val country: String?,
    val admin1: String?,
    val plate: Int?,
) {
    fun toCity(): City? {
        return City(
            id = id ?: return null,
            name = name ?: return null,
            latitude = latitude ?: return null,
            longitude = longitude ?: return null,
            countryCode = countryCode,
            country = country,
            admin1 = admin1,
            plate = plate,
        )
    }

    companion object {
        fun from(city: City) = StoredCity(
            id = city.id,
            name = city.name,
            latitude = city.latitude,
            longitude = city.longitude,
            countryCode = city.countryCode,
            country = city.country,
            admin1 = city.admin1,
            plate = city.plate,
        )
    }
}
