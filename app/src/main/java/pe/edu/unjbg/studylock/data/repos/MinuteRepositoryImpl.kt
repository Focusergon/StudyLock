package pe.edu.unjbg.studylock.data.repos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.count
import pe.edu.unjbg.studylock.data.local.i
import pe.edu.unjbg.studylock.data.local.l
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.model.MinuteKind
import pe.edu.unjbg.studylock.domain.model.MinuteTx
import pe.edu.unjbg.studylock.domain.repo.MinuteRepository

class MinuteRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MinuteRepository {

    override fun observeBalance(): Flow<Int> =
        db.changes.map { balanceNow() }.flowOn(io)

    override fun observeLedger(limit: Int): Flow<List<MinuteTx>> = db.changes.map {
        db.readableDatabase.list(
            "SELECT * FROM minute_tx ORDER BY created_at DESC LIMIT $limit",
        ) { c ->
            MinuteTx(
                id = c.l("id"),
                minutes = c.i("minutes"),
                kind = runCatching { MinuteKind.valueOf(c.str("kind")) }.getOrDefault(MinuteKind.ADJUSTMENT),
                note = c.str("note"),
                createdAt = c.l("created_at"),
            )
        }
    }.flowOn(io)

    override suspend fun balance(): Int = withContext(io) { balanceNow() }

    override suspend fun earn(minutes: Int, note: String) = record(minutes, "EARNED", note)

    override suspend fun spend(minutes: Int, note: String) = record(-minutes, "SPENT", note)

    override suspend fun grantEmergency(minutes: Int, note: String) = record(minutes, "EMERGENCY", note)

    private suspend fun record(minutes: Int, kind: String, note: String) = withContext(io) {
        val now = System.currentTimeMillis()
        db.writableDatabase.apply {
            insertMinuteTx(this, minutes, kind, note, now)
            if (minutes > 0) bumpDaily(this, earned = minutes) else bumpDaily(this, spent = -minutes)
        }
        db.notifyChanged()
    }

    private fun balanceNow(): Int =
        db.readableDatabase.count("SELECT COALESCE(SUM(minutes), 0) FROM minute_tx")
}
