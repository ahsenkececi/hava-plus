package com.ahsen.hava.domain.model

/**
 * Arama kutusu boşken gösterilen sabit şehir listesi.
 * İnternete ihtiyaç duymaz; uygulama açılır açılmaz ekranda bir şey olur.
 */
object PopularCities {

    private const val TR = "TR"
    private const val TURKIYE = "Türkiye"

    val all: List<City> = listOf(
        City(745044, "İstanbul", 41.0138, 28.9497, TR, TURKIYE, "İstanbul", plate = 34),
        City(323786, "Ankara", 39.9199, 32.8543, TR, TURKIYE, "Ankara", plate = 6),
        City(311046, "İzmir", 38.4127, 27.1384, TR, TURKIYE, "İzmir", plate = 35),
        City(750269, "Bursa", 40.1956, 29.0601, TR, TURKIYE, "Bursa", plate = 16),
        City(323777, "Antalya", 36.9081, 30.6956, TR, TURKIYE, "Antalya", plate = 7),
        City(325363, "Adana", 37.0017, 35.3289, TR, TURKIYE, "Adana", plate = 1),
        City(306571, "Konya", 37.8716, 32.4846, TR, TURKIYE, "Konya", plate = 42),
        City(314830, "Gaziantep", 37.0594, 37.3825, TR, TURKIYE, "Gaziantep", plate = 27),
        City(738648, "Trabzon", 41.0050, 39.7269, TR, TURKIYE, "Trabzon", plate = 61),
        City(315202, "Eskişehir", 39.7767, 30.5206, TR, TURKIYE, "Eskişehir", plate = 26),
        City(316541, "Diyarbakır", 37.9144, 40.2306, TR, TURKIYE, "Diyarbakır", plate = 21),
        City(740264, "Samsun", 41.2867, 36.3300, TR, TURKIYE, "Samsun", plate = 55),
        City(315368, "Erzurum", 39.9086, 41.2769, TR, TURKIYE, "Erzurum", plate = 25),
        City(304184, "Muğla", 37.2153, 28.3636, TR, TURKIYE, "Muğla", plate = 48),
    )
}
