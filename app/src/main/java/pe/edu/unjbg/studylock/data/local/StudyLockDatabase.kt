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

    init {
        // Sembramos el replay con un valor inicial: así cualquier colector nuevo (p. ej. el
        // dashboard tras abrir la app) recibe datos de inmediato, incluso si aún no ha habido
        // ninguna escritura en esta sesión de proceso.
        _changes.tryEmit(Unit)
    }

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
        // Migración aditiva: el esquema usa CREATE … IF NOT EXISTS, así que aplicarlo sobre una
        // base existente solo crea lo que falte (por ejemplo, las tablas de horario e importación)
        // sin borrar el progreso del estudiante.
        SCHEMA.forEach(db::execSQL)
    }

    companion object {
        private const val NAME = "studylock.db"
        private const val VERSION = 2

        private val SCHEMA = listOf(
            """
            CREATE TABLE IF NOT EXISTS courses(
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                professor TEXT NOT NULL DEFAULT '',
                semester TEXT NOT NULL DEFAULT '',
                accent INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS topics(
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
            CREATE TABLE IF NOT EXISTS questions(
                id TEXT PRIMARY KEY,
                topic_id TEXT NOT NULL,
                prompt TEXT NOT NULL,
                explanation TEXT NOT NULL DEFAULT '',
                difficulty INTEGER NOT NULL DEFAULT 1,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS options(
                id TEXT PRIMARY KEY,
                question_id TEXT NOT NULL,
                text TEXT NOT NULL,
                is_correct INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS answers(
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
            CREATE TABLE IF NOT EXISTS sessions(
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
            CREATE TABLE IF NOT EXISTS minute_tx(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                minutes INTEGER NOT NULL,
                kind TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS blocked_apps(
                package TEXT PRIMARY KEY,
                label TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT 'Otras',
                enabled INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS daily_stats(
                day TEXT PRIMARY KEY,
                minutes INTEGER NOT NULL DEFAULT 0,
                questions INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                earned INTEGER NOT NULL DEFAULT 0,
                spent INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS class_blocks(
                id TEXT PRIMARY KEY,
                course_id TEXT NOT NULL DEFAULT '',
                course_name TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                start_minute INTEGER NOT NULL,
                end_minute INTEGER NOT NULL,
                room TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS import_sources(
                id TEXT PRIMARY KEY,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                course_id TEXT NOT NULL DEFAULT '',
                question_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS idx_topics_course ON topics(course_id)",
            "CREATE INDEX IF NOT EXISTS idx_questions_topic ON questions(topic_id)",
            "CREATE INDEX IF NOT EXISTS idx_answers_created ON answers(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_tx_created ON minute_tx(created_at)",
        )
    }
}
