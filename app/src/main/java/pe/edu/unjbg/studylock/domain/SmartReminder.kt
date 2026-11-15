package pe.edu.unjbg.studylock.domain

import pe.edu.unjbg.studylock.domain.model.ClassBlock

/** Decisión de la notificación inteligente, con el motivo para depurar y testear. */
data class ReminderDecision(val notify: Boolean, val reason: String)

/**
 * Decide si conviene enviar un recordatorio de repaso en este momento.
 *
 * El objetivo es **no molestar**: nunca durante una clase ni justo antes de empezar, como máximo
 * un aviso cada [MIN_GAP_MS] y solo mientras no se haya alcanzado la meta diaria. Es lógica pura
 * (sin Android) para poder probarla en JVM.
 */
object SmartReminder {

    /** Separación mínima entre dos avisos. */
    const val MIN_GAP_MS = 90 * 60 * 1000L

    /** No avisar si una clase comienza dentro de estos minutos. */
    const val CLASS_SOON_MINUTES = 10

    fun decide(
        nowMillis: Long,
        dayOfWeek: Int,
        minuteOfDay: Int,
        blocks: List<ClassBlock>,
        minutesEarnedToday: Int,
        dailyGoalMinutes: Int,
        lastNotifiedAt: Long,
        remindersEnabled: Boolean,
    ): ReminderDecision {
        if (!remindersEnabled) return ReminderDecision(false, "recordatorios desactivados")
        if (minutesEarnedToday >= dailyGoalMinutes) return ReminderDecision(false, "meta diaria alcanzada")
        if (nowMillis - lastNotifiedAt < MIN_GAP_MS) return ReminderDecision(false, "aviso reciente")

        val today = blocks.filter { it.dayOfWeek == dayOfWeek }
        if (today.any { it.contains(minuteOfDay) }) return ReminderDecision(false, "en clase")
        if (today.any { it.startMinute in (minuteOfDay + 1)..(minuteOfDay + CLASS_SOON_MINUTES) }) {
            return ReminderDecision(false, "clase por comenzar")
        }

        return ReminderDecision(true, "franja libre")
    }

    /**
     * Etiqueta del siguiente bloque de clase de hoy (p. ej. «Cálculo a las 10:00»), o `null` si ya
     * no quedan clases. Permite personalizar el mensaje del aviso.
     */
    fun nextClassLabel(dayOfWeek: Int, minuteOfDay: Int, blocks: List<ClassBlock>): String? =
        blocks.filter { it.dayOfWeek == dayOfWeek && it.startMinute > minuteOfDay }
            .minByOrNull { it.startMinute }
            ?.let { "${it.courseName} a las ${it.startLabel}" }

    /** Minutos libres hasta la próxima clase de hoy, o `null` si no hay más. */
    fun minutesUntilNextClass(dayOfWeek: Int, minuteOfDay: Int, blocks: List<ClassBlock>): Int? =
        blocks.filter { it.dayOfWeek == dayOfWeek && it.startMinute > minuteOfDay }
            .minOfOrNull { it.startMinute }
            ?.minus(minuteOfDay)
}
