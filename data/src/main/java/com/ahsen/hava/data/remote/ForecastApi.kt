package com.ahsen.hava.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Hava tahmini servisi. Şehir aramasından farklı sunucu: api.open-meteo.com
 * Örnek: GET https://api.open-meteo.com/v1/forecast?latitude=39.92&longitude=32.85&current=...&timezone=auto
 */
internal interface ForecastApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("forecast_days") forecastDays: Int = FORECAST_DAYS,
        // 15 dakikalık yağış: "Yağmur 40 dk sonra başlıyor" cümlesi için. 8 x 15 dk = önümüzdeki 2 saat.
        @Query("minutely_15") minutely15: String = MINUTELY_FIELDS,
        @Query("forecast_minutely_15") forecastMinutely15: Int = 8,
        // "auto": zamanlar şehrin yerel saatine göre gelir.
        @Query("timezone") timezone: String = "auto",
    ): ForecastResponse

    /**
     * Kısa özet: anlık hava + sadece BUGÜNÜN en düşük/en yüksek sıcaklığı (şehir kartları için).
     * Saatlik veri ve diğer günler istenmediği için yanıt çok daha küçük ve hızlı.
     */
    @GET("v1/forecast")
    suspend fun getSnapshot(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto",
    ): ForecastResponse

    /**
     * Rota boyunca hava: birden çok nokta TEK istekte ("40.4,40.2" gibi virgülle ayrılmış).
     * Birden çok koordinat sorulunca Open-Meteo bir dizi döner: her nokta için bir yanıt, istek sırasıyla.
     */
    @GET("v1/forecast")
    suspend fun getAlongRoute(
        @Query(value = "latitude", encoded = true) latitudes: String,
        @Query(value = "longitude", encoded = true) longitudes: String,
        @Query("hourly") hourly: String = ROUTE_HOURLY_FIELDS,
        @Query("minutely_15") minutely15: String = MINUTELY_FIELDS,
        @Query("forecast_hours") forecastHours: Int = 12,
        // Son 6 saatin yağışı: "kuru dönemden sonraki ilk yağmur" (kaygan zemin) uyarısı için.
        @Query("past_hours") pastHours: Int = 6,
        // 16 x 15 dk = 4 saat: 2 saat sonra çıkıp 2 saat yol gitmeye yeter.
        @Query("forecast_minutely_15") forecastMinutely15: Int = 16,
        @Query("timezone") timezone: String = "auto",
    ): List<ForecastResponse>

    companion object {
        const val ROUTE_HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,precipitation_probability,precipitation,wind_speed_10m,weather_code," +
                "wind_direction_10m"
        const val CURRENT_FIELDS =
            "temperature_2m,weather_code,is_day,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_direction_10m," +
                "pressure_msl,visibility,dew_point_2m,cloud_cover"
        const val HOURLY_FIELDS = "temperature_2m,weather_code,is_day,precipitation_probability,uv_index,apparent_temperature,wind_speed_10m"
        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max"
        const val MINUTELY_FIELDS = "precipitation"
        const val FORECAST_DAYS = 7
    }
}
