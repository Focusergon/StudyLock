package pe.edu.unjbg.studylock.di

import android.content.Context
import pe.edu.unjbg.studylock.blocking.BlockingGate
import pe.edu.unjbg.studylock.blocking.StudyLockNotifications
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.prefs.AccessStore
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.data.repos.BlockedAppsRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.CourseRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.MinuteRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.StatsRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.StudyRepositoryImpl

/**
 * Inyección de dependencias manual.
 *
 * Un único contenedor por proceso. Si el equipo crece, este archivo es el punto de entrada
 * para migrar a Hilt sin tocar features.
 */
class AppGraph(context: Context) {

    private val appContext = context.applicationContext

    val db: StudyLockDatabase by lazy { StudyLockDatabase(appContext) }
    val prefs: UserPreferences by lazy { UserPreferences(appContext) }
    val accessStore: AccessStore by lazy { AccessStore(appContext) }

    val courses by lazy { CourseRepositoryImpl(db) }
    val study by lazy { StudyRepositoryImpl(db) }
    val minutes by lazy { MinuteRepositoryImpl(db) }
    val blockedApps by lazy { BlockedAppsRepositoryImpl(db) }
    val stats by lazy { StatsRepositoryImpl(db, prefs, courses) }

    val notifications by lazy { StudyLockNotifications(appContext) }

    val gate: BlockingGate by lazy {
        BlockingGate(appContext, minutes, blockedApps, accessStore, prefs, notifications)
    }
}
