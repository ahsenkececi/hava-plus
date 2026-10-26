package com.ahsen.hava.domain.model

/**
 * Bir yerin kısa hava özeti: şu an + bugünün en düşük/en yüksek sıcaklığı.
 * Şehir kartlarında kullanılır; tam tahmine (saatlik, günlük) göre çok daha hafif bir istektir.
 */
data class WeatherSnapshot(
    val current: CurrentWeather,
    val todayMinC: Double? = null,
    val todayMaxC: Double? = null,
)
