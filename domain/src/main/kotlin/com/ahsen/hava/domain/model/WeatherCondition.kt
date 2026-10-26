package com.ahsen.hava.domain.model

/**
 * WMO hava kodunun anlamı. Open-Meteo sadece bir sayı gönderir (0, 61, 95...).
 * Ekrandaki adı (strings.xml) ve görseli (WeatherIcon) arayüz katmanı belirler; domain metin taşımaz.
 */
enum class WeatherCondition {
    CLEAR,
    MAINLY_CLEAR,
    PARTLY_CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    RAIN_SHOWERS,
    SNOW_SHOWERS,
    THUNDERSTORM,
    UNKNOWN,
    ;

    companion object {
        fun fromWeatherCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67 -> RAIN
            71, 73, 75, 77 -> SNOW
            80, 81, 82 -> RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95, 96, 99 -> THUNDERSTORM
            // Tanınmayan kod gelirse uygulama çökmez.
            else -> UNKNOWN
        }
    }
}
