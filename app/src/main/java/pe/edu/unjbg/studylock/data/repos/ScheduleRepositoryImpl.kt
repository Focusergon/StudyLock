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
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.model.ClassBlock
import pe.edu.unjbg.studylock.domain.repo.ScheduleRepository

/** Horario de clases del estudiante. Alimenta las notificaciones inteligentes en periodos libres. */
class ScheduleRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ScheduleRepository {

    override fun observe(): Flow<List<ClassBlock>> =
        db.changes.map { query(null) }.flowOn(io)

    override suspend fun add(block: ClassBlock) = withContext(io) {
        db.writableDatabase.insertWithOnConflict(
            "class_blocks",
            null,
            ContentValues().apply {
                put("id", block.id)
                put("course_id", block.courseId)
                put("course_name", block.courseName)
                put("day_of_week", block.dayOfWeek)
                put("start_minute", block.startMinute)
                put("end_minute", block.endMinute)
                put("room", block.room)
                put("created_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        db.notifyChanged()
    }

    override suspend fun remove(id: String) = withContext(io) {
        db.writableDatabase.delete("class_blocks", "id = ?", arrayOf(id))
        db.notifyChanged()
    }

    override suspend fun blocksForDay(dayOfWeek: Int): List<ClassBlock> = withContext(io) { query(dayOfWeek) }

    override suspend fun all(): List<ClassBlock> = withContext(io) { query(null) }

    private fun query(dayOfWeek: Int?): List<ClassBlock> {
        val sql = if (dayOfWeek == null) {
            "SELECT * FROM class_blocks ORDER BY day_of_week, start_minute"
        } else {
            "SELECT * FROM class_blocks WHERE day_of_week = ? ORDER BY start_minute"
        }
        val args = if (dayOfWeek == null) emptyArray<String>() else arrayOf(dayOfWeek.toString())
        return db.readableDatabase.list(sql, args) { c ->
            ClassBlock(
                id = c.str("id"),
                courseId = c.str("course_id"),
                courseName = c.str("course_name"),
                dayOfWeek = c.i("day_of_week"),
                startMinute = c.i("start_minute"),
                endMinute = c.i("end_minute"),
                room = c.str("room"),
            )
        }
    }
}
