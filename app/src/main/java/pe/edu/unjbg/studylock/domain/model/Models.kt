package pe.edu.unjbg.studylock.domain.model

/** Estado de dominio de un tema, alineado con los estados del diseño (aprendido / necesita repaso / pendiente). */
enum class Mastery { LEARNED, NEEDS_REVIEW, PENDING }

data class Topic(
    val id: String,
    val courseId: String,
    val name: String,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val repStreak: Int = 0,
    val lastReviewAt: Long = 0L,
) {
    val accuracy: Int
        get() = if (reviewCount == 0) 0 else (correctCount * 100) / reviewCount

    val mastery: Mastery
        get() = when {
            reviewCount == 0 -> Mastery.PENDING
            accuracy >= 80 && repStreak >= 1 -> Mastery.LEARNED
            else -> Mastery.NEEDS_REVIEW
        }

    /** Prioridad de repaso: menor = más urgente. */
    val priority: Double
        get() {
            val overdue = if (lastReviewAt == 0L) 1.0 else 0.0
            return accuracy / 100.0 + intervalDays * 0.05 - overdue
        }
}

data class Course(
    val id: String,
    val name: String,
    val professor: String,
    val semester: String,
    val accent: Long,
    val topics: List<Topic> = emptyList(),
) {
    val questionsAsked: Int get() = topics.sumOf { it.reviewCount }
    val correct: Int get() = topics.sumOf { it.correctCount }
    val accuracy: Int get() = if (questionsAsked == 0) 0 else (correct * 100) / questionsAsked
    val learnedTopics: Int get() = topics.count { it.mastery == Mastery.LEARNED }
    val masteryPercent: Int
        get() = if (topics.isEmpty()) 0 else (topics.sumOf {
            when (it.mastery) {
                Mastery.LEARNED -> 100
                Mastery.NEEDS_REVIEW -> 55
                Mastery.PENDING -> 0
            }
        } / topics.size)
}

data class AnswerOption(
    val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
)

data class Question(
    val id: String,
    val topicId: String,
    val courseId: String,
    val prompt: String,
    val explanation: String,
    val difficulty: Int,
    val options: List<AnswerOption>,
) {
    val correctOption: AnswerOption get() = options.first { it.isCorrect }
}

enum class MinuteKind { EARNED, SPENT, EMERGENCY, ADJUSTMENT }

data class MinuteTx(
    val id: Long,
    val minutes: Int,
    val kind: MinuteKind,
    val note: String,
    val createdAt: Long,
)

enum class SessionKind { REVIEW, CHALLENGE, EMERGENCY }

data class StudySession(
    val id: String,
    val courseId: String,
    val kind: SessionKind,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val questionCount: Int = 0,
    val correctCount: Int = 0,
    val minutesAwarded: Int = 0,
)

data class BlockedApp(
    val packageName: String,
    val label: String,
    val category: String,
    val enabled: Boolean,
)

/** Respuesta ya evaluada, usada por la UI para el feedback inmediato. */
data class AnsweredQuestion(
    val question: Question,
    val chosen: AnswerOption,
    val isCorrect: Boolean,
)

data class DayActivity(val dayLabel: String, val minutes: Int)

data class DailyStatsRow(
    val questions: Int = 0,
    val correct: Int = 0,
    val earned: Int = 0,
    val spent: Int = 0,
    val minutes: Int = 0,
)

data class Dashboard(
    val availableMinutes: Int,
    val minutesEarnedToday: Int,
    val minutesSpentToday: Int,
    val questionsToday: Int,
    val correctToday: Int,
    val streakDays: Int,
    val week: List<DayActivity> = emptyList(),
    val blockedApps: List<BlockedApp> = emptyList(),
    val nextCourse: Course? = null,
    val nextTopic: Topic? = null,
    val userName: String = "",
)

data class WeeklyProgress(
    val minutes: Int,
    val sessions: Int,
    val questions: Int,
    val correct: Int,
    val consecutiveDays: Int,
    val week: List<DayActivity> = emptyList(),
) {
    val accuracy: Int get() = if (questions == 0) 0 else (correct * 100) / questions
}

data class AccessPlan(
    val packageName: String,
    val label: String,
    val remainingSeconds: Int,
)

/** Un día del historial de estudio. */
data class DayHistory(
    val dayKey: String,
    val dayLabel: String,
    val minutes: Int,
    val earned: Int,
    val spent: Int,
    val questions: Int,
    val correct: Int,
) {
    val accuracy: Int get() = if (questions == 0) 0 else (correct * 100) / questions
    val active: Boolean get() = questions > 0 || minutes > 0
}

/** Historial agregado de los últimos días, con rachas y totales. */
data class History(
    val days: List<DayHistory> = emptyList(),
    val totalMinutes: Int = 0,
    val totalQuestions: Int = 0,
    val activeDays: Int = 0,
    val bestStreak: Int = 0,
    val currentStreak: Int = 0,
)

/** Origen del contenido importado. */
enum class ImportSourceKind { TEXT, FILE, MOODLE }

/** Registro de una importación de contenido (texto pegado, archivo o Aula Virtual). */
data class ImportSource(
    val id: String,
    val kind: ImportSourceKind,
    val title: String,
    val courseId: String,
    val questionCount: Int,
    val createdAt: Long,
)

/** Resultado de importar un curso con sus temas y preguntas. */
data class ImportResult(
    val courseId: String,
    val courseName: String,
    val topicCount: Int,
    val questionCount: Int,
)

/**
 * Bloque del horario de clases del estudiante.
 *
 * [dayOfWeek] usa la numeración ISO (1 = lunes … 7 = domingo) y las horas son minutos desde las
 * 00:00, para que la lógica de «periodos libres» no dependa de zonas horarias.
 */
data class ClassBlock(
    val id: String,
    val courseId: String,
    val courseName: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String = "",
) {
    val startLabel: String get() = formatMinute(startMinute)
    val endLabel: String get() = formatMinute(endMinute)
    val dayLabel: String get() = DAY_LABELS[(dayOfWeek - 1).coerceIn(0, 6)]

    fun contains(minuteOfDay: Int): Boolean = minuteOfDay in startMinute until endMinute

    companion object {
        val DAY_LABELS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val DAY_SHORT = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        fun formatMinute(minute: Int): String =
            "%02d:%02d".format(minute / 60, minute % 60)
    }
}
