package pe.edu.unjbg.studylock.blocking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pe.edu.unjbg.studylock.StudyLockApp
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as StudyLockApp).graph
        val reminderNotifications = graph.notifications
        reminderNotifications.notifyReminder()
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "studylock_daily_reminder"

    fun ensureScheduled(context: Context) {
        val now = ZonedDateTime.now()
        val target = now.toLocalDate().atTime(LocalTime.of(18, 30))
        val next = if (now.toLocalTime().isAfter(LocalTime.of(18, 30))) {
            target.plusDays(1).atZone(now.zone)
        } else {
            target.atZone(now.zone)
        }
        val delay = Duration.between(now, next).toMinutes().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
