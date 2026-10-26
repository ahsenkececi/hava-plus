package com.ahsen.hava.data.remote

import com.ahsen.hava.domain.error.DataError
import com.google.gson.JsonParseException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

/**
 * Her ağ isteği bunun içinden geçer: kütüphanelerin hatalarını domain'in [DataError]'ına çevirir.
 *
 * Bu sayede hata türlerini bilen tek bir yer var (DRY). Önceden aynı try/catch bloğu
 * dokuz farklı yerde tekrar ediyordu.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    // Coroutine iptali bir hata değildir; aynen yukarı iletilmeli.
    throw e
} catch (e: DataError) {
    throw e
} catch (e: IOException) {
    throw DataError.Network(e)
} catch (e: HttpException) {
    throw DataError.Server(e.code(), e)
} catch (e: JsonParseException) {
    throw DataError.InvalidResponse(e)
} catch (e: IllegalStateException) {
    // toForecast() gibi dönüştürücüler zorunlu alan eksikse bunu fırlatır.
    throw DataError.InvalidResponse(e)
}
