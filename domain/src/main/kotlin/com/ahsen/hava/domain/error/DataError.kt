package com.ahsen.hava.domain.error

/**
 * Veri katmanından gelebilecek bütün hatalar, tek bir yerde ve kütüphaneden bağımsız.
 *
 * Veri katmanı Retrofit, Gson ya da OkHttp'nin kendi hatalarını yakalayıp bunlardan birine çevirir.
 * Böylece ViewModel'ler yalnızca bu üç durumu bilir; ağ kütüphanesi değişse bile ekran kodu değişmez.
 */
sealed class DataError(message: String, cause: Throwable?) : Exception(message, cause) {
    // Buradaki mesajlar sadece log içindir (geliştirici okur). Kullanıcının gördüğü cümle strings.xml'de.

    /** İnternet yok, zaman aşımı, sunucuya ulaşılamadı. */
    class Network(cause: Throwable? = null) : DataError("Network error", cause)

    /** Sunucu cevap verdi ama hata koduyla (4xx, 5xx). */
    class Server(val code: Int, cause: Throwable? = null) : DataError("Server error: $code", cause)

    /** Cevap geldi ama okunamadı ya da zorunlu bir alan eksik. */
    class InvalidResponse(cause: Throwable? = null) : DataError("Invalid response", cause)
}
