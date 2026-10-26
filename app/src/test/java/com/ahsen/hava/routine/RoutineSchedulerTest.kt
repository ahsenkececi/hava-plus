package com.ahsen.hava.routine

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineSchedulerTest {

    // 26 Ekim 2026 Pazartesi
    private val monday0700 = LocalDateTime.of(2026, 10, 26, 7, 0)
    private val routine = Routine(
        from = Place("Ev", "Bornova", 38.46, 27.21),
        to = Place("Okul", "Çiğli", 38.49, 27.06),
        mode = TravelMode.Bike,
        days = Routine.WEEKDAYS,
        departure = LocalTime.of(8, 15),
    )

    @Test
    fun `kontrol cikistan otuz dakika once kurulur`() {
        val check = RoutineScheduler.nextCheck(routine, after = monday0700, now = monday0700)

        assertEquals(monday0700.withHour(8).withMinute(15), check?.departure)
        assertEquals(Duration.ofMinutes(45), check?.delay)
    }

    @Test
    fun `kontrol saati gecti ama cikis gelmediyse hemen kontrol edilir`() {
        val now = monday0700.withHour(8).withMinute(5)

        val check = RoutineScheduler.nextCheck(routine, after = now, now = now)

        assertEquals(Duration.ZERO, check?.delay)
    }

    @Test
    fun `kontrol bitince siradaki cikis bir sonraki is gunudur`() {
        val departure = monday0700.withHour(8).withMinute(15)
        val now = departure.minusMinutes(30)

        val check = RoutineScheduler.nextCheck(routine, after = departure, now = now)

        assertEquals(departure.plusDays(1), check?.departure)
    }

    @Test
    fun `kapali rutin icin kontrol kurulmaz`() {
        assertNull(RoutineScheduler.nextCheck(routine.copy(enabled = false), monday0700, monday0700))
    }
}
