package pe.edu.unjbg.studylock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.ClassBlock

class SmartReminderTest {

    private val now = 1_700_000_000_000L
    private val longAgo = now - SmartReminder.MIN_GAP_MS - 1

    private fun block(day: Int, start: Int, end: Int, name: String = "Cálculo") =
        ClassBlock(
            id = "b-$day-$start",
            courseId = "c",
            courseName = name,
            dayOfWeek = day,
            startMinute = start,
            endMinute = end,
        )

    @Test
    fun `notifies during a free window`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 11 * 60,
            blocks = listOf(block(1, 8 * 60, 10 * 60)),
            minutesEarnedToday = 0,
            dailyGoalMinutes = 30,
            lastNotifiedAt = longAgo,
            remindersEnabled = true,
        )
        assertTrue(decision.notify)
    }

    @Test
    fun `does not notify while a class is in progress`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 9 * 60,
            blocks = listOf(block(1, 8 * 60, 10 * 60)),
            minutesEarnedToday = 0,
            dailyGoalMinutes = 30,
            lastNotifiedAt = longAgo,
            remindersEnabled = true,
        )
        assertFalse(decision.notify)
        assertEquals("en clase", decision.reason)
    }

    @Test
    fun `does not notify when a class starts soon`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 9 * 60 + 55,
            blocks = listOf(block(1, 10 * 60, 12 * 60)),
            minutesEarnedToday = 0,
            dailyGoalMinutes = 30,
            lastNotifiedAt = longAgo,
            remindersEnabled = true,
        )
        assertFalse(decision.notify)
        assertEquals("clase por comenzar", decision.reason)
    }

    @Test
    fun `does not notify when the daily goal is reached`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 11 * 60,
            blocks = emptyList(),
            minutesEarnedToday = 30,
            dailyGoalMinutes = 30,
            lastNotifiedAt = longAgo,
            remindersEnabled = true,
        )
        assertFalse(decision.notify)
        assertEquals("meta diaria alcanzada", decision.reason)
    }

    @Test
    fun `does not notify twice within the minimum gap`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 11 * 60,
            blocks = emptyList(),
            minutesEarnedToday = 0,
            dailyGoalMinutes = 30,
            lastNotifiedAt = now - 10 * 60 * 1000L,
            remindersEnabled = true,
        )
        assertFalse(decision.notify)
        assertEquals("aviso reciente", decision.reason)
    }

    @Test
    fun `does not notify when reminders are disabled`() {
        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = 1,
            minuteOfDay = 11 * 60,
            blocks = emptyList(),
            minutesEarnedToday = 0,
            dailyGoalMinutes = 30,
            lastNotifiedAt = longAgo,
            remindersEnabled = false,
        )
        assertFalse(decision.notify)
    }

    @Test
    fun `next class label points to the following block`() {
        val blocks = listOf(block(1, 8 * 60, 10 * 60, "Cálculo"), block(1, 14 * 60, 16 * 60, "Física"))
        assertEquals("Física a las 14:00", SmartReminder.nextClassLabel(1, 11 * 60, blocks))
        assertNull(SmartReminder.nextClassLabel(1, 18 * 60, blocks))
    }

    @Test
    fun `minutes until next class is computed`() {
        val blocks = listOf(block(1, 14 * 60, 16 * 60, "Física"))
        assertEquals(60, SmartReminder.minutesUntilNextClass(1, 13 * 60, blocks))
    }
}
