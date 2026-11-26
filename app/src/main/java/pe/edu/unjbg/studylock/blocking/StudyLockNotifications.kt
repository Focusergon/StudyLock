package pe.edu.unjbg.studylock.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pe.edu.unjbg.studylock.R
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.ui.MainActivity

class StudyLockNotifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val access = NotificationChannel(
            CHANNEL_ACCESS,
            "Acceso temporal",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Cuenta regresiva de los minutos ganados" }

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Recordatorios de repaso",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Recordatorios de micro-sesiones de estudio" }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Bloqueos y avisos",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Avisos cuando una aplicación queda limitada" }

        val model = NotificationChannel(
            CHANNEL_MODEL,
            "Modelo local",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Descarga del generador y clasificador de enlaces" }

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(listOf(access, reminders, alerts, model))
    }

    fun accessTimer(label: String, remainingSeconds: Int): Notification {
        val open = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            11,
            Intent(context, AccessTimerService::class.java).setAction(AccessTimerService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ACCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label · ${TimeUtils.formatClock(remainingSeconds)}")
            .setContentText("Tiempo de acceso restante")
            .setContentIntent(open)
            .addAction(0, "Finalizar antes", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun notifyBlocked(label: String) {
        val open = PendingIntent.getActivity(
            context,
            12,
            Intent(context, BlockingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label está limitada")
            .setContentText("Resuelve un repaso rápido para ganar minutos.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { manager.notify(NOTIFY_BLOCKED, notification) }
    }

    fun notifyReminder() {
        val open = PendingIntent.getActivity(
            context,
            13,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¿Un repaso rápido?")
            .setContentText("5 minutos de estudio hoy son 5 minutos de acceso después.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFY_REMINDER, notification) }
    }

    /**
     * Aviso inteligente de franja libre. [nextClass] es el siguiente bloque de clase de hoy
     * (p. ej. «Cálculo a las 10:00») o `null` si ya no quedan clases.
     */
    fun notifySmartReminder(nextClass: String?) {
        val open = PendingIntent.getActivity(
            context,
            14,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (nextClass != null) {
            "Tienes una franja libre. Después: $nextClass."
        } else {
            "Tienes una franja libre. 5 minutos de repaso = 5 minutos de acceso."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Aprovecha el hueco")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFY_REMINDER, notification) }
    }

    /** Notificación foreground del worker: se actualiza con el porcentaje real de la descarga. */
    fun modelForeground(progress: Int, message: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            15,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_MODEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Preparando StudyLock")
            .setContentText(message.ifBlank { "Descargando modelo local…" })
            .setContentIntent(open)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun notifyModelReady() {
        val open = PendingIntent.getActivity(
            context,
            16,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MODEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("StudyLock listo")
            .setContentText("El modelo local ya puede clasificar enlaces y crear preguntas.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFY_MODEL_READY, notification) }
    }

    companion object {
        const val CHANNEL_ACCESS = "access_timer"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_MODEL = "model_download"
        private const val NOTIFY_BLOCKED = 1001
        private const val NOTIFY_REMINDER = 1002
        private const val NOTIFY_MODEL_READY = 1003
    }
}
