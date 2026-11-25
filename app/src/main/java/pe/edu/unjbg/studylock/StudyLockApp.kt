package pe.edu.unjbg.studylock

import android.app.Application
import pe.edu.unjbg.studylock.blocking.ReminderScheduler
import pe.edu.unjbg.studylock.core.log.StudyLockLog
import pe.edu.unjbg.studylock.di.AppGraph

class StudyLockApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        StudyLockLog.init(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { StudyLockLog.error("Crash", throwable, "Hilo: ${thread.name}") }
            previous?.uncaughtException(thread, throwable)
        }

        graph = AppGraph(this)
        runCatching { graph.notifications.ensureChannels() }
            .onFailure { StudyLockLog.error("Notificaciones", it) }
        runCatching { ReminderScheduler.ensureScheduled(this) }
            .onFailure { StudyLockLog.error("Recordatorios", it) }
    }
}
