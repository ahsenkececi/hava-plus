package com.ahsen.hava.data.local

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.google.gson.Gson
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineCodecTest {

    private val codec = RoutineCodec(Gson())

    private val routine = Routine(
        from = Place("Ev", "Bornova, İzmir", 38.46, 27.21),
        to = Place("Okul", "Çiğli, İzmir", 38.49, 27.06),
        mode = TravelMode.Bike,
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        departure = LocalTime.of(8, 15),
        enabled = false,
    )

    @Test
    fun `yazilan rutin aynen geri okunur`() {
        assertEquals(routine, codec.decode(codec.encode(routine)))
    }

    @Test
    fun `kayit okunur metin bicimindedir`() {
        val json = codec.encode(routine)
        assertTrue(json.contains("\"mode\":\"Bike\""))
        assertTrue(json.contains("\"departure\":\"08:15\""))
        assertTrue(json.contains("\"MONDAY\""))
    }

    @Test
    fun `bos ya da bozuk kayit cokertmez`() {
        assertNull(codec.decode(null))
        assertNull(codec.decode(""))
        assertNull(codec.decode("{bozuk"))
        assertNull(codec.decode("""{"mode":"Ucak","departure":"08:15"}"""))
    }

    @Test
    fun `eksik alanli kayit null doner`() {
        assertNull(codec.decode("""{"mode":"Bike","days":["MONDAY"],"departure":"08:15"}"""))
    }

    @Test
    fun `enabled alani yoksa rutin acik sayilir`() {
        val json = codec.encode(routine).replace(",\"enabled\":false", "")
        assertEquals(true, codec.decode(json)?.enabled)
    }
}
