package pe.edu.unjbg.studylock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.Mastery
import pe.edu.unjbg.studylock.domain.model.Topic

class ReviewSchedulerTest {

    private fun topic() = Topic(id = "t1", courseId = "c1", name = "Tema")

    @Test
    fun `first correct review schedules one day and keeps pending until accuracy is high`() {
        val result = ReviewScheduler.apply(topic(), correct = true, now = 1_000L)
        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repStreak)
        assertEquals(1, result.reviewCount)
        assertEquals(1, result.correctCount)
        assertEquals(1_000L, result.lastReviewAt)
        assertTrue(result.ease > 2.5)
        assertEquals(Mastery.LEARNED, result.mastery)
    }

    @Test
    fun `wrong answer resets streak and schedules next day`() {
        val reviewed = ReviewScheduler.apply(topic(), correct = true, now = 1_000L)
        val failed = ReviewScheduler.apply(reviewed, correct = false, now = 2_000L)
        assertEquals(0, failed.repStreak)
        assertEquals(1, failed.intervalDays)
        assertEquals(2, failed.reviewCount)
        assertEquals(1, failed.correctCount)
        assertTrue(failed.ease < reviewed.ease)
        assertEquals(Mastery.NEEDS_REVIEW, failed.mastery)
    }

    @Test
    fun `second correct review jumps to three days`() {
        val first = ReviewScheduler.apply(topic(), correct = true, now = 1_000L)
        val second = ReviewScheduler.apply(first, correct = true, now = 2_000L)
        assertEquals(2, second.repStreak)
        assertEquals(3, second.intervalDays)
    }

    @Test
    fun `interval is capped at thirty days`() {
        var current = topic()
        repeat(12) { current = ReviewScheduler.apply(current, correct = true, now = it.toLong()) }
        assertTrue(current.intervalDays in 1..30)
    }

    @Test
    fun `ease stays inside configured bounds`() {
        var current = topic()
        repeat(20) { current = ReviewScheduler.apply(current, correct = false, now = it.toLong()) }
        assertTrue(current.ease >= 1.3)

        var raising = topic()
        repeat(20) { raising = ReviewScheduler.apply(raising, correct = true, now = it.toLong()) }
        assertTrue(raising.ease <= 2.8)
    }

    @Test
    fun `sortByPriority puts weakest topics first`() {
        val weak = Topic(id = "weak", courseId = "c", name = "Débil", reviewCount = 2, correctCount = 0)
        val strong = Topic(
            id = "strong",
            courseId = "c",
            name = "Fuerte",
            reviewCount = 10,
            correctCount = 10,
            repStreak = 3,
            intervalDays = 9,
            lastReviewAt = 5_000L,
        )
        val sorted = ReviewScheduler.sortByPriority(listOf(strong, weak))
        assertEquals("weak", sorted.first().id)
    }
}
