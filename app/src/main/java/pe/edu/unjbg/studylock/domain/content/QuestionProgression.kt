package pe.edu.unjbg.studylock.domain.content

import pe.edu.unjbg.studylock.domain.model.QuestionTier

/** Estadística mínima que necesita la progresión para decidir si un nivel ya está dominado. */
data class QuestionProgress(
    val tier: QuestionTier,
    val conceptKey: String,
    val timesAsked: Int,
    val timesCorrect: Int,
    val correctStreak: Int,
)

/**
 * Progresión de preguntas basada en estadísticas reales, no solo en el tema.
 *
 * La regla es la que pidió el estudiante: cuando una pregunta de concepto se ha respondido bien
 * varias veces (5 aciertos y al menos 80 % de precisión), se desbloquean las preguntas de proceso
 * relacionadas con ese mismo concepto. El mismo criterio abre el nivel «general» desde procesos.
 *
 * Es pura (sin Android ni SQLite) para poder probarla en JVM.
 */
object QuestionProgression {

    const val CORRECT_TO_UNLOCK = 5
    const val MIN_ACCURACY = 80

    fun accuracy(progress: QuestionProgress): Int =
        if (progress.timesAsked == 0) 0 else (progress.timesCorrect * 100) / progress.timesAsked

    fun isMastered(progress: QuestionProgress): Boolean =
        progress.timesCorrect >= CORRECT_TO_UNLOCK && accuracy(progress) >= MIN_ACCURACY

    /** Niveles ya disponibles para un conjunto de preguntas del mismo tema/concepto. */
    fun unlockedTiers(progress: List<QuestionProgress>): Set<QuestionTier> {
        val unlocked = mutableSetOf(QuestionTier.CONCEPTO)
        if (progress.any { it.tier == QuestionTier.CONCEPTO && isMastered(it) }) {
            unlocked += QuestionTier.PROCESO
        }
        if (progress.any { it.tier == QuestionTier.PROCESO && isMastered(it) }) {
            unlocked += QuestionTier.GENERAL
        }
        return unlocked
    }

    /** Cuántos aciertos faltan para abrir [tier]; cero cuando ya está abierto. */
    fun correctMissing(tier: QuestionTier, progress: List<QuestionProgress>): Int {
        val sourceTier = when (tier) {
            QuestionTier.CONCEPTO -> return 0
            QuestionTier.PROCESO -> QuestionTier.CONCEPTO
            QuestionTier.GENERAL -> QuestionTier.PROCESO
        }
        val best = progress.filter { it.tier == sourceTier }.maxByOrNull { it.timesCorrect } ?: return CORRECT_TO_UNLOCK
        if (isMastered(best)) return 0
        return (CORRECT_TO_UNLOCK - best.timesCorrect).coerceAtLeast(0)
    }
}
