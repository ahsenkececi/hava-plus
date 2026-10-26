package com.ahsen.hava.ui.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

/** Saatlik eğrinin yükseklik hesabı. */
class NormalizedTest {

    @Test
    fun `en dusuk sifir en yuksek bir olur`() {
        assertEquals(listOf(0f, 0.5f, 1f), normalized(listOf(10.0, 15.0, 20.0)))
    }

    @Test
    fun `butun degerler ayniysa egri ortada duz durur`() {
        assertEquals(listOf(0.5f, 0.5f, 0.5f), normalized(listOf(12.0, 12.0, 12.0)))
    }

    @Test
    fun `bos liste bos doner`() {
        assertEquals(emptyList<Float>(), normalized(emptyList()))
    }
}
