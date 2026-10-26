package com.ahsen.hava.domain.model

/**
 * Bir şehir. Uygulamanın her katmanının konuştuğu saf model (JSON ayrıntısı bilmez).
 *
 * @param plate Sadece "Popüler şehirler" listesinde dolu (plaka kodu). API'den gelen
 *   şehirlerde null olur, rozette ülke kodu gösterilir.
 */
data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val country: String?,
    val admin1: String?,
    val plate: Int? = null,
) {
    /** Kartın alt yazısı: "İzmir, Türkiye" */
    val subtitle: String
        get() = listOfNotNull(admin1, country).distinct().joinToString(", ")

    /** Rozet metni: plaka varsa "06", yoksa ülke kodu "TR". */
    val badge: String
        get() = plate?.toString()?.padStart(2, '0') ?: countryCode ?: "?"
}
