package pe.edu.unjbg.studylock.domain

import pe.edu.unjbg.studylock.domain.model.SessionKind
import pe.edu.unjbg.studylock.domain.model.Topic

/**
 * Repetición espaciada ligera (SM-2 simplificado).
 * Mantiene el estado por tema y decide cuándo vuelve a ser prioritario repasarlo.
 */
object ReviewScheduler {

    fun apply(topic: Topic, correct: Boolean, now: Long = System.currentTimeMillis()): Topic {
        val ease = if (correct) {
            (topic.ease + 0.1).coerceAtMost(2.8)
        } else {
            (topic.ease - 0.2).coerceAtLeast(1.3)
        }
        val streak = if (correct) topic.repStreak + 1 else 0
        val interval = when {
            !correct -> 1
            streak == 1 -> 1
            streak == 2 -> 3
            else -> (topic.intervalDays.coerceAtLeast(1) * ease).toInt().coerceIn(1, 30)
        }
        return topic.copy(
            ease = ease,
            intervalDays = interval,
            repStreak = streak,
            lastReviewAt = now,
            reviewCount = topic.reviewCount + 1,
            correctCount = topic.correctCount + if (correct) 1 else 0,
        )
    }

    /** Ordena temas por urgencia de repaso (menor puntaje = más urgente). */
    fun sortByPriority(topics: List<Topic>): List<Topic> = topics.sortedBy { it.priority }
}

object RewardPolicy {

    const val REVIEW_QUESTIONS = 5
    const val PRACTICE_QUESTIONS = 5
    const val CHALLENGE_QUESTIONS = 2
    const val EMERGENCY_MINUTES = 2
    const val EMERGENCY_COOLDOWN_MINUTES = 20

    fun minutesFor(kind: SessionKind, questionCount: Int, correctCount: Int): Int = when (kind) {
        // Las preguntas de prueba usan la misma tabla que un repaso de 5, pero su resultado
        // no se registra en estadísticas (solo los minutos se acreditan como puntos).
        SessionKind.REVIEW, SessionKind.PRUEBA -> when {
            questionCount == 0 -> 0
            correctCount * 100 / questionCount >= 80 -> 5
            correctCount * 100 / questionCount >= 50 -> 3
            else -> 2
        }
        SessionKind.CHALLENGE -> when (correctCount) {
            0, 1 -> 3
            else -> 5
        }
        SessionKind.EMERGENCY -> EMERGENCY_MINUTES
    }

    /** Las preguntas de prueba dan puntos, pero no entran en contadores ni rachas. */
    fun countsForStats(kind: SessionKind): Boolean = kind != SessionKind.PRUEBA

    /** Tiempo de estudio estimado para las estadísticas (0.8 min por pregunta). */
    fun studyMinutes(questionCount: Int): Int = (questionCount * 0.8).toInt() + if (questionCount > 0) 1 else 0
}
