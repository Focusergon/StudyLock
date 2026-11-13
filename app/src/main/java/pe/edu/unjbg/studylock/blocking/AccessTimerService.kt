package pe.edu.unjbg.studylock.blocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import pe.edu.unjbg.studylock.StudyLockApp
import pe.edu.unjbg.studylock.core.util.ForegroundApp
import pe.edu.unjbg.studylock.data.prefs.AccessStore

/**
 * Cuenta regresiva del acceso temporal.
 *
 * Consume minutos reales mientras la app limitada está en primer plano. Si el proceso muere,
 * el estado sobrevive en [AccessStore] y el servicio se relanza al volver a la app limitada.
 */
class AccessTimerService : Service() {

    private var remainingSeconds = 0
    private var pendingSeconds = 0
    private var running = false
    private var thread: Thread? = null

    private val graph get() = (application as StudyLockApp).graph
    private val accessStore: AccessStore get() = graph.accessStore

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        graph.notifications.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAccess(showBlock = true)
                return START_NOT_STICKY
            }
            ACTION_FINISH -> {
                // El usuario decide terminar antes: no tiene sentido volver a mostrar el reto.
                stopAccess(showBlock = false)
                return START_NOT_STICKY
            }
            else -> {
                val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
                remainingSeconds = if (seconds > 0) seconds else accessStore.remainingSeconds
                if (remainingSeconds <= 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val label = accessStore.activeLabel.ifBlank { "Acceso temporal" }
                startForegroundCompat(label)
                startTicking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(label: String) {
        val notification = graph.notifications.accessTimer(label, remainingSeconds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTicking() {
        if (running) return
        running = true
        thread = Thread {
            var ticks = 0
            while (running && remainingSeconds > 0) {
                try {
                    Thread.sleep(1_000)
                } catch (_: InterruptedException) {
                    break
                }
                if (!running) break
                ticks++

                // El tiempo corre solo mientras la app limitada está visible.
                if (!isTargetForeground()) {
                    if (ticks % 5 == 0) updateNotification()
                    continue
                }

                remainingSeconds -= 1
                pendingSeconds += 1
                accessStore.remainingSeconds = remainingSeconds

                if (pendingSeconds >= 60) {
                    val charge = pendingSeconds / 60
                    pendingSeconds %= 60
                    chargeMinutes(charge)
                }
                if (ticks % 5 == 0) {
                    updateNotification()
                }
                if (remainingSeconds <= 0) {
                    stopAccess(showBlock = true)
                }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    /**
     * Con accesibilidad activa basta con el último paquete observado (gratis y en tiempo real).
     * Sin ella se consulta UsageStatsManager como respaldo; si tampoco hay permiso, se cuenta
     * siempre para no regalar tiempo por un permiso faltante.
     */
    private fun isTargetForeground(): Boolean {
        val target = accessStore.activePackage ?: return false
        val observed = accessStore.lastForegroundPackage
        if (observed == target) return true
        if (observed.isNotEmpty() && graph.gate.accessibilityConnected) return false
        return ForegroundApp.current(this)?.let { it == target } ?: true
    }

    private fun chargeMinutes(count: Int) {
        if (count <= 0) return
        kotlinx.coroutines.runBlocking {
            runCatching {
                val balance = graph.minutes.balance()
                if (balance > 0) {
                    graph.minutes.spend(minOf(count, balance), accessStore.activeLabel.ifBlank { "App limitada" })
                }
            }.onFailure { Log.w(TAG, "No se pudo registrar el consumo", it) }
        }
    }

    private fun updateNotification() {
        val label = accessStore.activeLabel.ifBlank { "Acceso temporal" }
        runCatching {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, graph.notifications.accessTimer(label, remainingSeconds))
        }
    }

    private fun stopAccess(showBlock: Boolean) {
        running = false
        thread?.interrupt()
        thread = null
        if (pendingSeconds > 0) {
            chargeMinutes(1)
            pendingSeconds = 0
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (showBlock) {
            graph.gate.onAccessExpired()
        }
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        thread?.interrupt()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AccessTimerService"
        private const val NOTIFICATION_ID = 4242
        const val ACTION_START = "pe.edu.unjbg.studylock.action.START_ACCESS"
        const val ACTION_STOP = "pe.edu.unjbg.studylock.action.STOP_ACCESS"
        const val ACTION_FINISH = "pe.edu.unjbg.studylock.action.FINISH_ACCESS"
        private const val EXTRA_SECONDS = "extra_seconds"

        fun ensureRunning(context: Context, packageName: String, label: String) {
            val intent = Intent(context, AccessTimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SECONDS, 0)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "No se pudo iniciar el temporizador", it) }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AccessTimerService::class.java).setAction(ACTION_STOP),
                )
            }
        }

        /** Detiene el acceso por decisión del usuario, sin relanzar la pantalla de bloqueo. */
        fun finish(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AccessTimerService::class.java).setAction(ACTION_FINISH),
                )
            }
        }
    }
}
