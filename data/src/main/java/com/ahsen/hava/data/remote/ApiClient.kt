package com.ahsen.hava.data.remote

import com.ahsen.hava.data.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Uygulama boyunca tek bir Retrofit kurulumu (singleton).
 * `by lazy`: ilk istek atıldığında kurulur.
 */
internal object ApiClient {

    private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"
    private const val FORECAST_BASE_URL = "https://api.open-meteo.com/"
    private const val PLACE_BASE_URL = "https://photon.komoot.io/"
    private const val ROUTING_BASE_URL = "https://routing.openstreetmap.de/"

    // Bonus: OkHttp log'u. Debug derlemede her isteğin adresi ve sonucu Logcat'e düşer
    // (Logcat'te "okhttp" diye filtrele). Release'de kapalı.
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            // Açık servisler (Photon, OSM rota servisleri) kendini tanıtan bir User-Agent ister.
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .addInterceptor(logging)
            // Arşiv sunucusu bazen yavaş cevap veriyor; süreyi geniş tutuyoruz.
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val cityApi: CityApi by lazy { createApi(GEOCODING_BASE_URL) }

    val forecastApi: ForecastApi by lazy { createApi(FORECAST_BASE_URL) }

    /** Yolculuk: adres ve yer arama. */
    val placeApi: PlaceApi by lazy { createApi(PLACE_BASE_URL) }

    /** Yolculuk: yürüyüş, bisiklet ve araba rotaları. */
    val routingApi: RoutingApi by lazy { createApi(ROUTING_BASE_URL) }

    private const val USER_AGENT = "HavaPlus/1.0 (Android; Bilisim Kampi projesi)"

    private inline fun <reified T> createApi(baseUrl: String): T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(T::class.java)
}
