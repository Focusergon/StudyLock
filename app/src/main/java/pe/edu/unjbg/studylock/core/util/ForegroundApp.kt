package pe.edu.unjbg.studylock.core.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * Respaldo para saber qué app está en primer plano cuando el servicio de accesibilidad
 * no está disponible. Se usa solo para pausar el temporizador, nunca para leer contenido.
 */
object ForegroundApp {

    fun current(context: Context): String? {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        if (!PermissionUtils.hasUsageAccess(context)) return null

        val now = System.currentTimeMillis()
        val events = runCatching { manager.queryEvents(now - WINDOW_MS, now) }.getOrNull() ?: return null
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (isForegroundEvent(event.eventType)) {
                last = event.packageName
            }
        }
        return last
    }

    private fun isForegroundEvent(type: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            type == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
    }

    private const val WINDOW_MS = 15_000L
}
