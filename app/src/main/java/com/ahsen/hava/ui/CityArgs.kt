package com.ahsen.hava.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ahsen.hava.domain.model.City

/**
 * Bir ekrana taşınan şehir bilgisi (tahmin ve zaman makinesi ekranları ortak kullanır).
 * Favoriye eklenebilmesi için kimlik ve rozet bilgisi de taşınır.
 */
data class CityInfo(
    val id: Long,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val plate: Int? = null,
) {
    /** Favorilere kaydetmek için tekrar [City]'ye çevirir. */
    fun toCity(): City = City(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        countryCode = countryCode,
        country = subtitle.ifBlank { null },
        admin1 = null,
        plate = plate,
    )

    companion object {
        fun from(city: City) = CityInfo(
            id = city.id,
            name = city.name,
            subtitle = city.subtitle,
            latitude = city.latitude,
            longitude = city.longitude,
            countryCode = city.countryCode,
            plate = city.plate,
        )

        fun from(handle: SavedStateHandle) = CityInfo(
            id = handle.get<String>(CityArgs.ID)?.toLongOrNull() ?: 0L,
            name = handle.get<String>(CityArgs.NAME).orEmpty(),
            subtitle = handle.get<String>(CityArgs.SUBTITLE).orEmpty(),
            latitude = handle.get<String>(CityArgs.LATITUDE)?.toDoubleOrNull() ?: 0.0,
            longitude = handle.get<String>(CityArgs.LONGITUDE)?.toDoubleOrNull() ?: 0.0,
            countryCode = handle.get<String>(CityArgs.COUNTRY_CODE)?.ifBlank { null },
            plate = handle.get<String>(CityArgs.PLATE)?.toIntOrNull(),
        )
    }
}

/** Rotalardaki şehir parametreleri. Değerler boş olabileceği için sorgu parametresi (?a=..) kullanılır. */
object CityArgs {
    const val ID = "id"
    const val NAME = "name"
    const val SUBTITLE = "subtitle"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val COUNTRY_CODE = "countryCode"
    const val PLATE = "plate"

    private val ALL = listOf(ID, NAME, SUBTITLE, LATITUDE, LONGITUDE, COUNTRY_CODE, PLATE)

    /** "id={id}&name={name}&..." : süslü parantezler çalışma anında dolar. */
    val TEMPLATE: String = ALL.joinToString("&") { "$it={$it}" }

    // Hepsi metin olarak taşınır; sayıya çevirme CityInfo.from içinde yapılır.
    val navArguments = ALL.map { key ->
        navArgument(key) {
            type = NavType.StringType
            defaultValue = ""
        }
    }

    /** Uri.encode: "İzmir, Türkiye" gibi değerler adreste bozulmasın. */
    fun query(info: CityInfo): String = listOf(
        ID to info.id.toString(),
        NAME to info.name,
        SUBTITLE to info.subtitle,
        LATITUDE to info.latitude.toString(),
        LONGITUDE to info.longitude.toString(),
        COUNTRY_CODE to info.countryCode.orEmpty(),
        PLATE to (info.plate?.toString() ?: ""),
    ).joinToString("&") { (key, value) -> "$key=${Uri.encode(value)}" }
}
