package com.ahsen.hava.data.remote

import com.ahsen.hava.domain.error.DataError
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Kütüphane hataları, uygulamanın anladığı tek dile ([DataError]) çevriliyor mu? */
class ApiCallTest {

    private suspend fun errorOf(block: suspend () -> Unit): Throwable =
        runCatching { apiCall(block) }.exceptionOrNull() ?: error("Hata bekleniyordu")

    @Test
    fun `basarili cagri degeri aynen dondurur`() = runTest {
        assertEquals(42, apiCall { 42 })
    }

    @Test
    fun `baglanti hatasi Network olur`() = runTest {
        assertTrue(errorOf { throw IOException("timeout") } is DataError.Network)
    }

    @Test
    fun `HTTP hatasi kodu ile birlikte Server olur`() = runTest {
        val http = HttpException(Response.error<Unit>(503, "".toResponseBody()))

        val error = errorOf { throw http }

        assertEquals(503, (error as DataError.Server).code)
    }

    @Test
    fun `okunamayan yanit InvalidResponse olur`() = runTest {
        assertTrue(errorOf { throw JsonParseException("bozuk") } is DataError.InvalidResponse)
        assertTrue(errorOf { error("zorunlu alan yok") } is DataError.InvalidResponse)
    }
}
