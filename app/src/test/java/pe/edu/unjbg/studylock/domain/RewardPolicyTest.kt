package pe.edu.unjbg.studylock.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.SessionKind

class RewardPolicyTest {

    @Test
    fun `review with 80 percent or more awards 5 minutes`() {
        assertEquals(5, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 4))
        assertEquals(5, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 5))
    }

    @Test
    fun `review between 50 and 79 percent awards 3 minutes`() {
        assertEquals(3, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 3))
    }

    @Test
    fun `review below 50 percent still awards 2 minutes`() {
        assertEquals(2, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 2))
        assertEquals(2, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 1))
        assertEquals(2, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 5, correctCount = 0))
    }

    @Test
    fun `review without questions awards nothing`() {
        assertEquals(0, RewardPolicy.minutesFor(SessionKind.REVIEW, questionCount = 0, correctCount = 0))
    }

    @Test
    fun `quick challenge only rewards 5 when both answers are correct`() {
        assertEquals(5, RewardPolicy.minutesFor(SessionKind.CHALLENGE, questionCount = 2, correctCount = 2))
        assertEquals(3, RewardPolicy.minutesFor(SessionKind.CHALLENGE, questionCount = 2, correctCount = 1))
        assertEquals(3, RewardPolicy.minutesFor(SessionKind.CHALLENGE, questionCount = 2, correctCount = 0))
    }

    @Test
    fun `emergency always awards its fixed minutes`() {
        assertEquals(
            RewardPolicy.EMERGENCY_MINUTES,
            RewardPolicy.minutesFor(SessionKind.EMERGENCY, questionCount = 0, correctCount = 0),
        )
    }

    @Test
    fun `practice questions use the same reward table as a review`() {
        assertEquals(5, RewardPolicy.minutesFor(SessionKind.PRUEBA, questionCount = 5, correctCount = 4))
        assertEquals(3, RewardPolicy.minutesFor(SessionKind.PRUEBA, questionCount = 5, correctCount = 3))
        assertEquals(2, RewardPolicy.minutesFor(SessionKind.PRUEBA, questionCount = 5, correctCount = 1))
        assertEquals(0, RewardPolicy.minutesFor(SessionKind.PRUEBA, questionCount = 0, correctCount = 0))
        assertEquals(5, RewardPolicy.PRACTICE_QUESTIONS)
    }

    @Test
    fun `practice sessions are excluded from statistics but keep their minutes`() {
        assertEquals(false, RewardPolicy.countsForStats(SessionKind.PRUEBA))
        assertEquals(true, RewardPolicy.countsForStats(SessionKind.REVIEW))
        assertEquals(true, RewardPolicy.countsForStats(SessionKind.CHALLENGE))
    }

    @Test
    fun `study minutes are at least one per non empty session`() {
        assertEquals(0, RewardPolicy.studyMinutes(0))
        assertEquals(1, RewardPolicy.studyMinutes(1))
        assertEquals(5, RewardPolicy.studyMinutes(5))
    }
}
