package pe.edu.unjbg.studylock.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import pe.edu.unjbg.studylock.data.seed.SeedContent

/**
 * Persistencia local del MVP.
 *
 * Se usa SQLite directo (sin procesadores de anotaciones) para mantener el build simple y
 * reproducible. Toda la app consume repositorios, así que migrar a Room más adelante es un
 * cambio contenido a la capa data, sin tocar features.
 */
class StudyLockDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    NAME,
    null,
    VERSION,
) {
    private val _changes = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emite cada vez que cambia cualquier tabla. Base de la reactividad de los repositorios. */
    val changes: Flow<Unit> = _changes

    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }

    override fun onCreate(db: SQLiteDatabase) {
        SCHEMA.forEach(db::execSQL)
        SeedContent.populate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TABLES.forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    companion object {
        private const val NAME = "studylock.db"
        private const val VERSION = 1

        private val TABLES = listOf(
            "courses", "topics", "questions", "options", "answers",
            "sessions", "minute_tx", "blocked_apps", "daily_stats",
        )

        private val SCHEMA = listOf(
            """
            CREATE TABLE courses(
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                professor TEXT NOT NULL DEFAULT '',
                semester TEXT NOT NULL DEFAULT '',
                accent INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE topics(
                id TEXT PRIMARY KEY,
                course_id TEXT NOT NULL,
                name TEXT NOT NULL,
                sort INTEGER NOT NULL DEFAULT 0,
                ease REAL NOT NULL DEFAULT 2.5,
                interval_days INTEGER NOT NULL DEFAULT 0,
                rep_streak INTEGER NOT NULL DEFAULT 0,
                last_review_at INTEGER NOT NULL DEFAULT 0,
                review_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE questions(
                id TEXT PRIMARY KEY,
                topic_id TEXT NOT NULL,
                prompt TEXT NOT NULL,
                explanation TEXT NOT NULL DEFAULT '',
                difficulty INTEGER NOT NULL DEFAULT 1,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE options(
                id TEXT PRIMARY KEY,
                question_id TEXT NOT NULL,
                text TEXT NOT NULL,
                is_correct INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE answers(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT,
                question_id TEXT NOT NULL,
                topic_id TEXT NOT NULL,
                chosen_option_id TEXT,
                is_correct INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE sessions(
                id TEXT PRIMARY KEY,
                course_id TEXT,
                kind TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                finished_at INTEGER,
                question_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                minutes_awarded INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE minute_tx(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                minutes INTEGER NOT NULL,
                kind TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE blocked_apps(
                package TEXT PRIMARY KEY,
                label TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT 'Otras',
                enabled INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE daily_stats(
                day TEXT PRIMARY KEY,
                minutes INTEGER NOT NULL DEFAULT 0,
                questions INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                earned INTEGER NOT NULL DEFAULT 0,
                spent INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            "CREATE INDEX idx_topics_course ON topics(course_id)",
            "CREATE INDEX idx_questions_topic ON questions(topic_id)",
            "CREATE INDEX idx_answers_created ON answers(created_at)",
            "CREATE INDEX idx_tx_created ON minute_tx(created_at)",
        )
    }
}
