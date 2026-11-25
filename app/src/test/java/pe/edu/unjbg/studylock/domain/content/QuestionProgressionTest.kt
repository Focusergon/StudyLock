package pe.edu.unjbg.studylock.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.QuestionTier

class QuestionProgressionTest {

    private fun progress(
        tier: QuestionTier,
        asked: Int,
        correct: Int,
        streak: Int = correct,
    ) = QuestionProgress(
        tier = tier,
        conceptKey = "clave-primaria",
        timesAsked = asked,
        timesCorrect = correct,
        correctStreak = streak,
    )

    @Test
    fun `only concepts are unlocked at the start`() {
        val unlocked = QuestionProgression.unlockedTiers(listOf(progress(QuestionTier.CONCEPTO, 0, 0)))
        assertEquals(setOf(QuestionTier.CONCEPTO), unlocked)
    }

    @Test
    fun `five correct answers with high accuracy unlock processes`() {
        val unlocked = QuestionProgression.unlockedTiers(
            listOf(progress(QuestionTier.CONCEPTO, asked = 6, correct = 5)),
        )
        assertTrue(QuestionTier.PROCESO in unlocked)
        assertFalse(QuestionTier.GENERAL in unlocked)
    }

    @Test
    fun `five correct answers with low accuracy do not unlock processes`() {
        // 5 aciertos de 10 = 50 % de precisión: no basta.
        val unlocked = QuestionProgression.unlockedTiers(
            listOf(progress(QuestionTier.CONCEPTO, asked = 10, correct = 5)),
        )
        assertEquals(setOf(QuestionTier.CONCEPTO), unlocked)
    }

    @Test
    fun `four correct answers do not unlock processes`() {
        val unlocked = QuestionProgression.unlockedTiers(
            listOf(progress(QuestionTier.CONCEPTO, asked = 4, correct = 4)),
        )
        assertEquals(setOf(QuestionTier.CONCEPTO), unlocked)
    }

    @Test
    fun `mastering a process unlocks general questions`() {
        val unlocked = QuestionProgression.unlockedTiers(
            listOf(
                progress(QuestionTier.CONCEPTO, asked = 6, correct = 5),
                progress(QuestionTier.PROCESO, asked = 5, correct = 5),
            ),
        )
        assertEquals(
            setOf(QuestionTier.CONCEPTO, QuestionTier.PROCESO, QuestionTier.GENERAL),
            unlocked,
        )
    }

    @Test
    fun `a different question with enough hits counts too`() {
        // Dos preguntas de concepto separadas: ninguna llega a 5, no se desbloquea.
        val unlocked = QuestionProgression.unlockedTiers(
            listOf(
                progress(QuestionTier.CONCEPTO, asked = 3, correct = 3),
                progress(QuestionTier.CONCEPTO, asked = 3, correct = 3),
            ),
        )
        assertEquals(setOf(QuestionTier.CONCEPTO), unlocked)
    }

    @Test
    fun `correctMissing reports how many hits are left`() {
        val progress = listOf(progress(QuestionTier.CONCEPTO, asked = 3, correct = 2))
        assertEquals(3, QuestionProgression.correctMissing(QuestionTier.PROCESO, progress))
        assertEquals(0, QuestionProgression.correctMissing(QuestionTier.CONCEPTO, progress))
    }

    @Test
    fun `correctMissing is zero when the source tier is mastered`() {
        val progress = listOf(progress(QuestionTier.CONCEPTO, asked = 5, correct = 5))
        assertEquals(0, QuestionProgression.correctMissing(QuestionTier.PROCESO, progress))
    }

    @Test
    fun `accuracy handles zero answers`() {
        assertEquals(0, QuestionProgression.accuracy(progress(QuestionTier.CONCEPTO, 0, 0)))
    }
}
