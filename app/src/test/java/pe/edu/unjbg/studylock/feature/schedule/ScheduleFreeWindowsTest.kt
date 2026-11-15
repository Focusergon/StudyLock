package pe.edu.unjbg.studylock.feature.schedule

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.ClassBlock

class ScheduleFreeWindowsTest {

    private fun block(day: Int, start: Int, end: Int) =
        ClassBlock(
            id = "b-$start",
            courseId = "c",
            courseName = "Curso",
            dayOfWeek = day,
            startMinute = start,
            endMinute = end,
        )

    @Test
    fun `no classes yields the whole day`() {
        val windows = ScheduleUi.freeWindows(1, emptyList())
        assertEquals(listOf(ScheduleUi.DAY_START to ScheduleUi.DAY_END), windows)
    }

    @Test
    fun `classes split the day into windows`() {
        val windows = ScheduleUi.freeWindows(
            dayOfWeek = 1,
            blocks = listOf(block(1, 9 * 60, 11 * 60), block(1, 13 * 60, 15 * 60)),
        )
        assertEquals(
            listOf(
                ScheduleUi.DAY_START to 9 * 60,
                11 * 60 to 13 * 60,
                15 * 60 to ScheduleUi.DAY_END,
            ),
            windows,
        )
    }

    @Test
    fun `ignores other days and short gaps`() {
        val windows = ScheduleUi.freeWindows(
            dayOfWeek = 2,
            blocks = listOf(
                block(1, 9 * 60, 11 * 60),
                block(2, 8 * 60, 10 * 60),
                block(2, 10 * 60 + 5, 12 * 60),
            ),
        )
        // El bloque del lunes no aplica y el hueco de 5 minutos entre clases se descarta.
        assertEquals(
            listOf(ScheduleUi.DAY_START to 8 * 60, 12 * 60 to ScheduleUi.DAY_END),
            windows,
        )
    }
}
