package pe.edu.unjbg.studylock.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeUtilsTest {

    @Test
    fun `formatClock pads minutes and seconds`() {
        assertEquals("00:00", TimeUtils.formatClock(0))
        assertEquals("00:09", TimeUtils.formatClock(9))
        assertEquals("01:00", TimeUtils.formatClock(60))
        assertEquals("04:32", TimeUtils.formatClock(272))
        assertEquals("60:00", TimeUtils.formatClock(3600))
    }

    @Test
    fun `formatClock never shows negative time`() {
        assertEquals("00:00", TimeUtils.formatClock(-42))
    }

    @Test
    fun `formatMinutes switches to hours`() {
        assertEquals("0 min", TimeUtils.formatMinutes(0))
        assertEquals("45 min", TimeUtils.formatMinutes(45))
        assertEquals("1 h 0 m", TimeUtils.formatMinutes(60))
        assertEquals("2 h 15 m", TimeUtils.formatMinutes(135))
    }

    @Test
    fun `dayKey and startOfDay are consistent`() {
        val now = System.currentTimeMillis()
        val start = TimeUtils.startOfDay(now)
        assertTrue(start <= now)
        assertEquals(TimeUtils.dayKey(start), TimeUtils.dayKey(now))
        assertEquals(TimeUtils.todayKey(now), TimeUtils.dayKey(now))
    }

    @Test
    fun `lastNDays returns n consecutive days ending today`() {
        val now = Instant.parse("2026-11-12T15:00:00Z").toEpochMilli()
        val days = TimeUtils.lastNDays(7, now)
        assertEquals(7, days.size)
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        assertEquals(today, days.last())
        assertEquals(today.minusDays(6), days.first())
    }

    @Test
    fun `dayLabel is a single narrow uppercase letter`() {
        val label = TimeUtils.dayLabel(java.time.LocalDate.of(2026, 11, 12))
        assertEquals(1, label.length)
        assertEquals(label.uppercase(), label)
    }

    @Test
    fun `relativeDays handles empty and recent values`() {
        assertEquals("sin repasar", TimeUtils.relativeDays(0L))
        assertEquals("hoy", TimeUtils.relativeDays(System.currentTimeMillis() - 1_000L))
        assertEquals("ayer", TimeUtils.relativeDays(System.currentTimeMillis() - 30 * 60 * 60 * 1000L))
    }
}
