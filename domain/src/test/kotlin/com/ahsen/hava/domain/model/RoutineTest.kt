package com.ahsen.hava.domain.model

import com.ahsen.hava.testutil.Fixtures.NOW
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineTest {

    // NOW = 25 Ekim 2026 Pazar, 14:00
    private val home = Place("Ev", "Bornova", 38.46, 27.21)
    private val school = Place("Okul", "Çiğli", 38.49, 27.06)
    private val weekdays815 = Routine(home, school, TravelMode.Bike, Routine.WEEKDAYS, LocalTime.of(8, 15))

    @Test
    fun `pazar ogleden sonra hafta ici rutinin siradaki cikisi pazartesi sabahidir`() {
        assertEquals(NOW.plusDays(1).toLocalDate().atTime(8, 15), weekdays815.nextDeparture(after = NOW))
    }

    @Test
    fun `bugunun saati gecmediyse siradaki cikis bugundur`() {
        val sunday1500 = weekdays815.copy(days = setOf(DayOfWeek.SUNDAY), departure = LocalTime.of(15, 0))
        assertEquals(NOW.withHour(15), sunday1500.nextDeparture(after = NOW))
    }

    @Test
    fun `tam cikis anindan sonrasi sorulursa bir sonraki haftaya gecer`() {
        val sunday1400 = weekdays815.copy(days = setOf(DayOfWeek.SUNDAY), departure = LocalTime.of(14, 0))
        assertEquals(NOW.plusWeeks(1), sunday1400.nextDeparture(after = NOW))
    }

    @Test
    fun `kapali ya da gunsuz rutinin cikisi yoktur`() {
        assertNull(weekdays815.copy(enabled = false).nextDeparture(NOW))
        assertNull(weekdays815.copy(days = emptySet()).nextDeparture(NOW))
    }

    @Test
    fun `kontrol cikistan otuz dakika once yapilir`() {
        val departure = NOW.withHour(8).withMinute(15)
        assertEquals(NOW.withHour(7).withMinute(45), weekdays815.checkTimeFor(departure))
    }
}
