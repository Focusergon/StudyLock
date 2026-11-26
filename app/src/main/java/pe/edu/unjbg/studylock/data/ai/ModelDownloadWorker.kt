package pe.edu.unjbg.studylock.data.ai

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import pe.edu.unjbg.studylock.StudyLockApp
import pe.edu.unjbg.studylock.blocking.StudyLockNotifications
import pe.edu.unjbg.studylock.core.log.StudyLockLog

/**
 * Descarga del modelo como una «actualización de juego»: WorkManager la mantiene viva aunque el
 * usuario cambie de pantalla y reanuda el `.part` si se corta la conexión.
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val graph = (applicationContext as StudyLockApp).graph
        val notifications = graph.notifications
        val wifiOnly = inputData.getBoolean(KEY_WIFI_ONLY, true)
        StudyLockLog.log("Modelo", "Descarga en segundo plano · intento ${runAttemptCount + 1}")

        runCatching { setForeground(foregroundInfo(0, "Preparando la descarga…")) }
            .onFailure { StudyLockLog.warning("Modelo", "No se pudo mostrar la notificación: ${it.message}") }

        val result = graph.modelDownloader.download(wifiOnly) { percent ->
            runCatching {
                setProgress(workDataOf(KEY_PROGRESS to percent))
                setForeground(foregroundInfo(percent, "Descargando modelo… $percent%"))
            }
        }
        return when (result.status) {
            ModelStatus.READY -> {
                notifications.notifyModelReady()
                Result.success()
            }
            ModelStatus.WAITING_WIFI -> Result.success()
            else -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(0, "Preparando la descarga del modelo…")

    private fun foregroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = (applicationContext as StudyLockApp).graph.notifications
            .modelForeground(progress, message)
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val UNIQUE_NAME = "model_download"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_PROGRESS = "progress"
        private const val NOTIFICATION_ID = 1401
        private const val MAX_ATTEMPTS = 10

        fun input(wifiOnly: Boolean): Data = workDataOf(KEY_WIFI_ONLY to wifiOnly)
    }
}
