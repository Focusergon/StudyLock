package pe.edu.unjbg.studylock.data.repos

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.i
import pe.edu.unjbg.studylock.data.local.l
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.RewardPolicy
import pe.edu.unjbg.studylock.domain.ReviewScheduler
import pe.edu.unjbg.studylock.domain.model.AnswerOption
import pe.edu.unjbg.studylock.domain.model.Question
import pe.edu.unjbg.studylock.domain.model.SessionKind
import pe.edu.unjbg.studylock.domain.model.Topic
import pe.edu.unjbg.studylock.domain.repo.SessionDraft
import pe.edu.unjbg.studylock.domain.repo.StudyRepository
import java.util.UUID

private data class QuestionRow(
    val id: String,
    val topicId: String,
    val courseId: String,
    val prompt: String,
    val explanation: String,
    val difficulty: Int,
)

class StudyRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : StudyRepository {

    override suspend fun startSession(courseId: String, kind: SessionKind, count: Int): SessionDraft =
        withContext(io) {
            val questions = pickQuestions(ReviewScheduler.sortByPriority(loadTopics(courseId)), count)
            val sessionId = UUID.randomUUID().toString()
            insertSession(sessionId, courseId, kind, questions.size)
            db.notifyChanged()
            SessionDraft(sessionId, courseId, questions)
        }

    override suspend fun startWeakSession(kind: SessionKind, count: Int): SessionDraft = withContext(io) {
        val questions = pickQuestions(ReviewScheduler.sortByPriority(loadAllTopics()), count)
        val sessionId = UUID.randomUUID().toString()
        insertSession(sessionId, questions.firstOrNull()?.courseId.orEmpty(), kind, questions.size)
        db.notifyChanged()
        SessionDraft(sessionId, questions.firstOrNull()?.courseId.orEmpty(), questions)
    }

    override suspend fun answer(
        sessionId: String,
        question: Question,
        chosenOptionId: String,
    ): Boolean = withContext(io) {
        val correct = question.options.firstOrNull { it.id == chosenOptionId }?.isCorrect == true
        val now = System.currentTimeMillis()
        db.writableDatabase.apply {
            execSQL(
                "INSERT INTO answers(session_id, question_id, topic_id, chosen_option_id, is_correct, created_at) VALUES(?,?,?,?,?,?)",
                arrayOf<Any?>(sessionId, question.id, question.topicId, chosenOptionId, if (correct) 1 else 0, now),
            )
            loadTopic(question.topicId)?.let { topic ->
                val updated = ReviewScheduler.apply(topic, correct, now)
                execSQL(
                    "UPDATE topics SET ease=?, interval_days=?, rep_streak=?, last_review_at=?, review_count=?, correct_count=? WHERE id=?",
                    arrayOf<Any?>(
                        updated.ease, updated.intervalDays, updated.repStreak, updated.lastReviewAt,
                        updated.reviewCount, updated.correctCount, updated.id,
                    ),
                )
            }
            bumpDaily(this, questions = 1, correct = if (correct) 1 else 0)
        }
        db.notifyChanged()
        correct
    }

    override suspend fun finishSession(
        sessionId: String,
        questionCount: Int,
        correctCount: Int,
        minutesAwarded: Int,
    ) = withContext(io) {
        val now = System.currentTimeMillis()
        db.writableDatabase.apply {
            execSQL(
                "UPDATE sessions SET finished_at=?, question_count=?, correct_count=?, minutes_awarded=? WHERE id=?",
                arrayOf<Any?>(now, questionCount, correctCount, minutesAwarded, sessionId),
            )
            if (minutesAwarded > 0) {
                insertMinuteTx(this, minutesAwarded, "EARNED", "Recompensa por repaso", now)
            }
            bumpDaily(
                this,
                earned = minutesAwarded,
                minutes = if (minutesAwarded > 0) RewardPolicy.studyMinutes(questionCount) else 0,
            )
        }
        db.notifyChanged()
    }

    private fun insertSession(sessionId: String, courseId: String, kind: SessionKind, questionCount: Int) {
        db.writableDatabase.execSQL(
            "INSERT INTO sessions(id, course_id, kind, started_at, question_count) VALUES(?,?,?,?,?)",
            arrayOf<Any?>(sessionId, courseId, kind.name, System.currentTimeMillis(), questionCount),
        )
    }

    private fun pickQuestions(orderedTopics: List<Topic>, count: Int): List<Question> {
        if (orderedTopics.isEmpty() || count <= 0) return emptyList()
        val ids = orderedTopics.map { it.id }
        val placeholders = ids.joinToString(",") { "?" }
        val rows = db.readableDatabase.list(
            """
            SELECT q.id AS id, q.topic_id AS topic_id, t.course_id AS course_id,
                   q.prompt AS prompt, q.explanation AS explanation, q.difficulty AS difficulty
            FROM questions q
            JOIN topics t ON t.id = q.topic_id
            WHERE q.topic_id IN ($placeholders)
            """.trimIndent(),
            ids.toTypedArray(),
        ) { c ->
            QuestionRow(
                id = c.str("id"),
                topicId = c.str("topic_id"),
                courseId = c.str("course_id"),
                prompt = c.str("prompt"),
                explanation = c.str("explanation"),
                difficulty = c.i("difficulty"),
            )
        }
        if (rows.isEmpty()) return emptyList()

        val byTopic = rows.groupBy { it.topicId }.mapValues { (_, value) -> value.shuffled() }
        val selected = mutableListOf<QuestionRow>()
        var round = 0
        while (selected.size < count && round < 8) {
            var added = false
            for (topic in orderedTopics) {
                val row = byTopic[topic.id]?.getOrNull(round) ?: continue
                selected += row
                added = true
                if (selected.size == count) break
            }
            if (!added) break
            round++
        }
        return hydrate(selected)
    }

    private fun hydrate(rows: List<QuestionRow>): List<Question> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.id }
        val placeholders = ids.joinToString(",") { "?" }
        val options = db.readableDatabase.list(
            "SELECT * FROM options WHERE question_id IN ($placeholders) ORDER BY sort",
            ids.toTypedArray(),
        ) { c ->
            AnswerOption(
                id = c.str("id"),
                questionId = c.str("question_id"),
                text = c.str("text"),
                isCorrect = c.i("is_correct") == 1,
            )
        }.groupBy { it.questionId }

        return rows.mapNotNull { row ->
            val opts = options[row.id].orEmpty()
            if (opts.size < 2 || opts.none { it.isCorrect }) return@mapNotNull null
            Question(
                id = row.id,
                topicId = row.topicId,
                courseId = row.courseId,
                prompt = row.prompt,
                explanation = row.explanation,
                difficulty = row.difficulty,
                options = opts,
            )
        }
    }

    private fun loadTopics(courseId: String): List<Topic> = db.readableDatabase.list(
        "SELECT * FROM topics WHERE course_id = ? ORDER BY sort",
        arrayOf(courseId),
    ) { it.toTopic() }

    private fun loadAllTopics(): List<Topic> =
        db.readableDatabase.list("SELECT * FROM topics ORDER BY sort") { it.toTopic() }

    private fun loadTopic(topicId: String): Topic? = db.readableDatabase.list(
        "SELECT * FROM topics WHERE id = ?",
        arrayOf(topicId),
    ) { it.toTopic() }.firstOrNull()

    private fun android.database.Cursor.toTopic(): Topic = Topic(
        id = str("id"),
        courseId = str("course_id"),
        name = str("name"),
        reviewCount = i("review_count"),
        correctCount = i("correct_count"),
        ease = getDouble(getColumnIndexOrThrow("ease")),
        intervalDays = i("interval_days"),
        repStreak = i("rep_streak"),
        lastReviewAt = l("last_review_at"),
    )
}

internal fun insertMinuteTx(db: SQLiteDatabase, minutes: Int, kind: String, note: String, now: Long) {
    db.execSQL(
        "INSERT INTO minute_tx(minutes, kind, note, created_at) VALUES(?,?,?,?)",
        arrayOf<Any?>(minutes, kind, note, now),
    )
}

internal fun bumpDaily(
    db: SQLiteDatabase,
    questions: Int = 0,
    correct: Int = 0,
    earned: Int = 0,
    spent: Int = 0,
    minutes: Int = 0,
) {
    val day = TimeUtils.todayKey()
    db.execSQL("INSERT OR IGNORE INTO daily_stats(day) VALUES(?)", arrayOf<Any?>(day))
    db.execSQL(
        "UPDATE daily_stats SET questions=questions+?, correct=correct+?, earned=earned+?, spent=spent+?, minutes=minutes+? WHERE day=?",
        arrayOf<Any?>(questions, correct, earned, spent, minutes, day),
    )
}
