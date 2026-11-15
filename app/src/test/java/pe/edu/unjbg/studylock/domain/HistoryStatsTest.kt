package pe.edu.unjbg.studylock.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.DayHistory

class HistoryStatsTest {

    private fun day(key: String, minutes: Int = 0, questions: Int = 0) =
        DayHistory(
            dayKey = key,
            dayLabel = key,
            minutes = minutes,
            earned = 0,
            spent = 0,
            questions = questions,
            correct = questions,
        )

    @Test
    fun `best streak counts consecutive study days`() {
        val days = listOf(
            day("2026-11-01", questions = 5),
            day("2026-11-02", questions = 3),
            day("2026-11-03"),
            day("2026-11-04", questions = 5),
            day("2026-11-05", questions = 5),
            day("2026-11-06", questions = 5),
        )
        assertEquals(3, HistoryStats.bestStreak(days))
    }

    @Test
    fun `best streak is zero without questions`() {
        val days = listOf(day("2026-11-01", minutes = 10), day("2026-11-02", minutes = 4))
        assertEquals(0, HistoryStats.bestStreak(days))
    }

    @Test
    fun `active days count minutes or questions`() {
        val days = listOf(
            day("2026-11-01", minutes = 10),
            day("2026-11-02", questions = 2),
            day("2026-11-03"),
        )
        assertEquals(2, HistoryStats.activeDays(days))
    }
}
