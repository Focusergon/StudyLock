package pe.edu.unjbg.studylock.di

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.blocking.BlockingGate
import pe.edu.unjbg.studylock.blocking.StudyLockNotifications
import pe.edu.unjbg.studylock.data.ai.LocalAiEngine
import pe.edu.unjbg.studylock.data.ai.LocalQuestionAiProvider
import pe.edu.unjbg.studylock.data.ai.ModelDownloadWorker
import pe.edu.unjbg.studylock.data.ai.ModelDownloader
import pe.edu.unjbg.studylock.data.auth.CredentialStore
import pe.edu.unjbg.studylock.data.content.ContentStore
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.network.NetworkStatus
import pe.edu.unjbg.studylock.data.prefs.AccessStore
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.data.repos.BlockedAppsRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.ContentRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.CourseRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.MinuteRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.ScheduleRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.StatsRepositoryImpl
import pe.edu.unjbg.studylock.data.repos.StudyRepositoryImpl
import pe.edu.unjbg.studylock.data.web.AiLinkClassifier
import pe.edu.unjbg.studylock.data.web.FileDownloader
import pe.edu.unjbg.studylock.domain.content.HybridQuestionGenerator
import pe.edu.unjbg.studylock.feature.accounts.AccountsViewModel
import java.util.concurrent.TimeUnit

/**
 * Inyección de dependencias manual.
 *
 * Un único contenedor por proceso. Si el equipo crece, este archivo es el punto de entrada
 * para migrar a Hilt sin tocar features.
 */
class AppGraph(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: StudyLockDatabase by lazy { StudyLockDatabase(appContext) }
    val prefs: UserPreferences by lazy { UserPreferences(appContext) }
    val accessStore: AccessStore by lazy { AccessStore(appContext) }
    val credentials: CredentialStore by lazy { CredentialStore(appContext) }
    val contentStore: ContentStore by lazy { ContentStore(appContext) }
    val network: NetworkStatus by lazy { NetworkStatus(appContext) }
    val modelDownloader: ModelDownloader by lazy { ModelDownloader(appContext, network) }

    /** Un solo motor para preguntas y clasificación de enlaces; se inicializa bajo demanda. */
    val localAiEngine: LocalAiEngine by lazy { LocalAiEngine(modelDownloader) }
    val linkClassifier: AiLinkClassifier by lazy { AiLinkClassifier(localAiEngine) }
    val aiProvider: LocalQuestionAiProvider by lazy { LocalQuestionAiProvider(localAiEngine) }

    /** IA local opcional + respaldo determinista; nunca depende de una clave ni de un servidor. */
    val questionGenerator: HybridQuestionGenerator by lazy {
        HybridQuestionGenerator(aiProvider, enabled = { prefs.current().aiQuestions })
    }

    val courses by lazy { CourseRepositoryImpl(db) }
    val study by lazy { StudyRepositoryImpl(db) }
    val minutes by lazy { MinuteRepositoryImpl(db) }
    val blockedApps by lazy { BlockedAppsRepositoryImpl(db) }
    val stats by lazy { StatsRepositoryImpl(db, prefs, courses) }
    val content by lazy { ContentRepositoryImpl(db) }
    val schedule by lazy { ScheduleRepositoryImpl(db) }

    val notifications by lazy { StudyLockNotifications(appContext) }
    val fileDownloader: FileDownloader by lazy { FileDownloader() }

    /**
     * Controlador único de la sincronización UNJBG. Vive aquí y no en una ruta para que salir de
     * «Cuentas» no destruya el WebView ni cancele la búsqueda.
     */
    val accounts: AccountsViewModel by lazy {
        AccountsViewModel(
            credentials = credentials,
            content = content,
            store = contentStore,
            generator = questionGenerator,
            classifier = linkClassifier,
            model = modelDownloader,
            preferences = prefs,
            downloader = fileDownloader,
        )
    }

    val gate: BlockingGate by lazy {
        BlockingGate(appContext, minutes, blockedApps, accessStore, prefs, notifications)
    }

    /**
     * Programa la descarga del modelo al abrir la app. WorkManager aplica la restricción Wi-Fi
     * guardada y reintenta con backoff; `force` se usa cuando el estudiante pulsa «Reintentar».
     */
    fun startModelDownload(force: Boolean = false) {
        appScope.launch {
            if (modelDownloader.verifiedFile() != null) {
                modelDownloader.refresh()
                return@launch
            }
            val current = prefs.current()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (current.modelWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                )
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(ModelDownloadWorker.input(current.modelWifiOnly))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30_000L, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                ModelDownloadWorker.UNIQUE_NAME,
                if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
