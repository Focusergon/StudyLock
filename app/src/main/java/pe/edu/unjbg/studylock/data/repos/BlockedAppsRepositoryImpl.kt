package pe.edu.unjbg.studylock.data.repos

import android.content.ContentValues
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.b
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.model.BlockedApp
import pe.edu.unjbg.studylock.domain.repo.BlockedAppsRepository

class BlockedAppsRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : BlockedAppsRepository {

    override fun observe(): Flow<List<BlockedApp>> = db.changes.map { load() }.flowOn(io)

    override suspend fun saveAll(apps: List<BlockedApp>) = withContext(io) {
        val now = System.currentTimeMillis()
        db.writableDatabase.apply {
            beginTransaction()
            try {
                // La selección es autoritativa: primero se limpia la tabla y luego se inserta
                // todo lo marcado. Así lo nuevo se bloquea al instante y lo desmarcado deja
                // de hacerlo (antes solo se hacía upsert y quedaban filas viejas).
                execSQL("DELETE FROM blocked_apps")
                apps.forEach { app ->
                    insertWithOnConflict(
                        "blocked_apps",
                        null,
                        ContentValues().apply {
                            put("package", app.packageName)
                            put("label", app.label)
                            put("category", app.category)
                            put("enabled", if (app.enabled) 1 else 0)
                            put("created_at", now)
                        },
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        db.notifyChanged()
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) = withContext(io) {
        db.writableDatabase.execSQL(
            "UPDATE blocked_apps SET enabled = ? WHERE package = ?",
            arrayOf<Any?>(if (enabled) 1 else 0, packageName),
        )
        db.notifyChanged()
    }

    override suspend fun remove(packageName: String) = withContext(io) {
        db.writableDatabase.delete("blocked_apps", "package = ?", arrayOf(packageName))
        db.notifyChanged()
    }

    override suspend fun enabledPackages(): Set<String> = withContext(io) {
        load().filter { it.enabled }.map { it.packageName }.toSet()
    }

    private fun load(): List<BlockedApp> = db.readableDatabase.list(
        "SELECT * FROM blocked_apps ORDER BY label",
    ) { c ->
        BlockedApp(
            packageName = c.str("package"),
            label = c.str("label"),
            category = c.str("category"),
            enabled = c.b("enabled"),
        )
    }
}
