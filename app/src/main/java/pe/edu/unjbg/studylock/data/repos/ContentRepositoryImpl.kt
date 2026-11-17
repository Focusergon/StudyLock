package pe.edu.unjbg.studylock.data.repos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.i
import pe.edu.unjbg.studylock.data.local.l
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.one
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.model.ImportResult
import pe.edu.unjbg.studylock.domain.model.ImportSource
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind
import pe.edu.unjbg.studylock.domain.repo.ContentRepository
import java.util.UUID

/**
 * Persiste el contenido importado (texto, archivo o Aula Virtual) como cursos, temas, preguntas
 * y opciones, reutilizando exactamente el mismo esquema que la semilla. Reimportar un curso con
 * el mismo nombre lo reemplaza en lugar de duplicarlo.
 */
class ContentRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ContentRepository {

    override fun observeSources(): Flow<List<ImportSource>> =
        db.changes.map { loadSources() }.flowOn(io)

    override suspend fun importCourse(
        courseName: String,
        professor: String,
        topics: List<GeneratedTopic>,
        kind: ImportSourceKind,
        sourceTitle: String,
    ): ImportResult = withContext(io) {
        val name = courseName.trim().ifBlank { "Curso importado" }
        val courseId = stableCourseId(name)
        val now = System.currentTimeMillis()
        var questionCount = 0

        db.writableDatabase.apply {
            beginTransaction()
            try {
                removeCourseContent(this, courseId)

                val sort = nextCourseSort()
                insertWithOnConflict(
                    "courses",
                    null,
                    ContentValues().apply {
                        put("id", courseId)
                        put("name", name)
                        put("professor", professor.trim())
                        put("semester", "Importado")
                        put("accent", accentFor(name))
                        put("sort", sort)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )

                topics.forEachIndexed { topicIndex, topic ->
                    val topicId = "$courseId-t${topicIndex + 1}"
                    insertWithOnConflict(
                        "topics",
                        null,
                        ContentValues().apply {
                            put("id", topicId)
                            put("course_id", courseId)
                            put("name", topic.name)
                            put("sort", topicIndex)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                    topic.questions.forEachIndexed { questionIndex, question ->
                        val questionId = "$topicId-q${questionIndex + 1}"
                        insertWithOnConflict(
                            "questions",
                            null,
                            ContentValues().apply {
                                put("id", questionId)
                                put("topic_id", topicId)
                                put("prompt", question.prompt)
                                put("explanation", question.explanation)
                                put("difficulty", question.difficulty)
                                put("sort", questionIndex)
                            },
                            SQLiteDatabase.CONFLICT_REPLACE,
                        )
                        question.options.forEachIndexed { optionIndex, text ->
                            insertWithOnConflict(
                                "options",
                                null,
                                ContentValues().apply {
                                    put("id", "$questionId-o${optionIndex + 1}")
                                    put("question_id", questionId)
                                    put("text", text)
                                    put("is_correct", if (optionIndex == question.correctIndex) 1 else 0)
                                    put("sort", optionIndex)
                                },
                                SQLiteDatabase.CONFLICT_REPLACE,
                            )
                        }
                        questionCount++
                    }
                }

                insertWithOnConflict(
                    "import_sources",
                    null,
                    ContentValues().apply {
                        put("id", UUID.randomUUID().toString())
                        put("kind", kind.name)
                        put("title", sourceTitle.trim().ifBlank { name })
                        put("course_id", courseId)
                        put("question_count", questionCount)
                        put("created_at", now)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )

                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        db.notifyChanged()
        ImportResult(courseId = courseId, courseName = name, topicCount = topics.size, questionCount = questionCount)
    }

    override suspend fun deleteSource(sourceId: String) = withContext(io) {
        db.writableDatabase.apply {
            val courseId = one("SELECT course_id FROM import_sources WHERE id = ?", arrayOf(sourceId)) { it.str("course_id") }
            beginTransaction()
            try {
                delete("import_sources", "id = ?", arrayOf(sourceId))
                if (!courseId.isNullOrBlank()) {
                    removeCourseContent(this, courseId)
                    delete("courses", "id = ?", arrayOf(courseId))
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        db.notifyChanged()
    }

    private fun removeCourseContent(db: SQLiteDatabase, courseId: String) {
        db.execSQL(
            "DELETE FROM options WHERE question_id IN (SELECT q.id FROM questions q JOIN topics t ON t.id = q.topic_id WHERE t.course_id = ?)",
            arrayOf<Any?>(courseId),
        )
        db.execSQL(
            "DELETE FROM questions WHERE topic_id IN (SELECT id FROM topics WHERE course_id = ?)",
            arrayOf<Any?>(courseId),
        )
        db.execSQL("DELETE FROM topics WHERE course_id = ?", arrayOf<Any?>(courseId))
    }

    private fun nextCourseSort(): Int =
        db.readableDatabase.one("SELECT COALESCE(MAX(sort), -1) + 1 FROM courses") { it.getInt(0) } ?: 0

    private fun loadSources(): List<ImportSource> =
        db.readableDatabase.list("SELECT * FROM import_sources ORDER BY created_at DESC") { c ->
            ImportSource(
                id = c.str("id"),
                kind = parseKind(c.str("kind")),
                title = c.str("title"),
                courseId = c.str("course_id"),
                questionCount = c.i("question_count"),
                createdAt = c.l("created_at"),
            )
        }

    /** El valor `MOODLE` se usó en versiones anteriores; ahora el Aula Virtual es `AULA`. */
    private fun parseKind(raw: String): ImportSourceKind = when (raw) {
        "MOODLE" -> ImportSourceKind.AULA
        else -> runCatching { ImportSourceKind.valueOf(raw) }.getOrDefault(ImportSourceKind.TEXT)
    }

    private fun stableCourseId(name: String): String {
        val slug = name.lowercase()
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("")
            .trim('-')
            .replace(Regex("-+"), "-")
            .take(28)
            .ifBlank { "curso" }
        return "imp-$slug-${Integer.toHexString(name.lowercase().hashCode())}"
    }

    private fun accentFor(name: String): Long =
        ACCENTS[(name.hashCode() and 0x7FFFFFFF) % ACCENTS.size]

    private companion object {
        val ACCENTS = listOf(
            0xFF9073C9, 0xFF35B6A4, 0xFFEBAE5F,
            0xFF5B8DEF, 0xFFD87076, 0xFF6FBF73,
        )
    }
}
