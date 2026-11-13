package pe.edu.unjbg.studylock.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pe.edu.unjbg.studylock.StudyLockApp

/**
 * Tras un reinicio el acceso temporal no debe sobrevivir (evita "minutos gratis").
 * Solo se limpia el estado efímero y se refresca la lista de apps bloqueadas.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val graph = (context.applicationContext as StudyLockApp).graph
        graph.accessStore.clear()
        graph.gate.refreshBlockedPackages()
        ReminderScheduler.ensureScheduled(context)
    }
}
