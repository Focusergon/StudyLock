package pe.edu.unjbg.studylock.blocking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pe.edu.unjbg.studylock.StudyLockApp
import pe.edu.unjbg.studylock.domain.SmartReminder
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Recordatorio inteligente: corre cada media hora y solo avisa cuando el estudiante está en una
 * franja libre, no ha llegado a su meta diaria y no recibió un aviso reciente. La decisión vive en
 * [SmartReminder], que es lógica pura y testeable.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as StudyLockApp).graph
        val prefs = graph.prefs.current()
        val now = System.currentTimeMillis()
        val zoned = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        val dayOfWeek = zoned.dayOfWeek.value
        val minuteOfDay = zoned.hour * 60 + zoned.minute
        val blocks = graph.schedule.all()

        val decision = SmartReminder.decide(
            nowMillis = now,
            dayOfWeek = dayOfWeek,
            minuteOfDay = minuteOfDay,
            blocks = blocks,
            minutesEarnedToday = graph.stats.todayEarned(),
            dailyGoalMinutes = prefs.dailyGoalMinutes,
            lastNotifiedAt = prefs.lastNotifiedAt,
            remindersEnabled = prefs.remindersEnabled,
        )

        if (decision.notify) {
            val nextClass = SmartReminder.nextClassLabel(dayOfWeek, minuteOfDay, blocks)
            graph.notifications.notifySmartReminder(nextClass)
            graph.prefs.markNotified(now)
        }
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "studylock_smart_reminder"
    private const val LEGACY_WORK_NAME = "studylock_daily_reminder"

    fun ensureScheduled(context: Context) {
        // Limpia el trabajo diario de versiones anteriores.
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
