package pe.edu.unjbg.studylock.domain

import pe.edu.unjbg.studylock.domain.model.DayHistory

/**
 * Cálculos puros sobre el historial de días, separados del repositorio para poder probarlos en
 * JVM sin base de datos.
 */
object HistoryStats {

    /** Días activos dentro de la ventana analizada. */
    fun activeDays(days: List<DayHistory>): Int = days.count { it.active }

    /** Racha más larga de días consecutivos con al menos una pregunta respondida. */
    fun bestStreak(days: List<DayHistory>): Int {
        var best = 0
        var run = 0
        for (day in days) {
            if (day.questions > 0) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }
}
