package pe.edu.unjbg.studylock

import android.app.Application
import pe.edu.unjbg.studylock.blocking.ReminderScheduler
import pe.edu.unjbg.studylock.di.AppGraph

class StudyLockApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.notifications.ensureChannels()
        ReminderScheduler.ensureScheduled(this)
    }
}
