package pe.edu.unjbg.studylock.core.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * Respaldo para saber qué app está en primer plano cuando el servicio de accesibilidad
 * no está disponible. Se usa solo para pausar el temporizador, nunca para leer contenido.
 *
 * Estrategia: se reconstruye el primer plano a partir de los eventos RESUMED/PAUSED de
 * UsageStatsManager. Es más fiable que mirar solo el último evento de una ventana corta,
 * porque una app puede permanecer abierta mucho tiempo sin emitir eventos nuevos.
 */
object ForegroundApp {

    @Volatile
    private var cachedAt = 0L

    @Volatile
    private var cachedPackage: String? = null

    fun current(context: Context): String? {
        val now = System.currentTimeMillis()
        if (now - cachedAt < CACHE_MS) return cachedPackage
        val detected = detect(context, now)
        cachedPackage = detected
        cachedAt = now
        return detected
    }

    private fun detect(context: Context, now: Long): String? {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        if (!PermissionUtils.hasUsageAccess(context)) return null
        foregroundFromEvents(manager, now)?.let { return it }
        return mostRecentlyUsed(manager, now)
    }

    /** Reconstruye el conjunto de actividades activas y devuelve el paquete de la más reciente. */
    private fun foregroundFromEvents(manager: UsageStatsManager, now: Long): String? {
        val events = runCatching { manager.queryEvents(now - WINDOW_MS, now) }.getOrNull() ?: return null
        val event = UsageEvents.Event()
        val active = LinkedHashSet<String>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val key = "$pkg/${event.className.orEmpty()}"
            when (event.eventType) {
                RESUMED -> {
                    active.remove(key)
                    active.add(key)
                }
                PAUSED, STOPPED -> active.remove(key)
            }
        }
        return active.lastOrNull()?.substringBefore('/')
    }

    /**
     * Si no hubo eventos (p. ej. UsageStats no los expone), se elige el paquete con el último
     * uso más reciente como última red de seguridad.
     */
    private fun mostRecentlyUsed(manager: UsageStatsManager, now: Long): String? {
        val stats = runCatching {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - DAY_MS, now)
        }.getOrNull() ?: return null
        return stats
            .filter { it.lastTimeUsed > 0 }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private val RESUMED: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }

    private val PAUSED: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_PAUSED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_BACKGROUND
        }

    private val STOPPED: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_STOPPED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_BACKGROUND
        }

    private const val CACHE_MS = 2_000L
    private const val WINDOW_MS = 6 * 60 * 60 * 1000L
    private const val DAY_MS = 24 * 60 * 60 * 1000L
}
