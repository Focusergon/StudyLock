package pe.edu.unjbg.studylock.blocking

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.core.util.InstalledAppsLoader
import pe.edu.unjbg.studylock.data.prefs.AccessStore
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.domain.repo.BlockedAppsRepository
import pe.edu.unjbg.studylock.domain.repo.MinuteRepository

/**
 * Cerebro del bloqueo.
 *
 * Recibe el paquete que pasa a primer plano y decide: dejar pasar, iniciar temporizador de
 * acceso o mostrar el reto. Se mantiene fuera de la UI y sin dependencias de Android UI para
 * poder probarlo y, en el futuro, moverlo a un módulo propio.
 */
class BlockingGate(
    private val context: Context,
    private val minutes: MinuteRepository,
    private val blockedApps: BlockedAppsRepository,
    private val accessStore: AccessStore,
    private val prefs: UserPreferences,
    private val notifications: StudyLockNotifications,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var blockedPackages: Set<String> = emptySet()

    @Volatile
    private var lastHandledAt: Long = 0L

    init {
        // Mantiene la lista de apps limitadas sincronizada con la base de datos:
        // si el usuario cambia su selección en Ajustes/onboarding, el bloqueo se actualiza solo.
        scope.launch {
            blockedApps.observe().collect { apps ->
                blockedPackages = apps.filter { it.enabled }.map { it.packageName }.toSet()
            }
        }
    }

    @Volatile
    var accessibilityConnected: Boolean = false
        private set

    fun refreshBlockedPackages() {
        scope.launch { blockedPackages = blockedApps.enabledPackages() }
    }

    fun onServiceConnected() {
        accessibilityConnected = true
        refreshBlockedPackages()
    }

    fun onServiceDisconnected() {
        accessibilityConnected = false
    }

    /** Llamado por el AccessibilityService en cada cambio de ventana. */
    fun onForeground(packageName: String) {
        if (packageName.isBlank()) return
        // Registramos SIEMPRE el paquete real en primer plano, incluso si es de sistema o es
        // nuestra propia app: así el temporizador sabe cuándo la app limitada dejó de estar
        // visible y pausa la cuenta (en vez de quedarse pegado al último paquete bloqueado).
        accessStore.lastForegroundPackage = packageName
        // El bloqueo solo aplica a apps de usuario; los paquetes de sistema y el nuestro se
        // registran arriba pero no se evalúan.
        if (packageName in SYSTEM_PACKAGES) return
        if (packageName == context.packageName) return
        if (packageName !in blockedPackages) return
        scope.launch { evaluate(packageName) }
    }

    fun onChallengeCompleted(packageName: String, label: String) {
        scope.launch { startAccess(packageName, label) }
    }

    fun onAccessExpired() {
        val packageName = accessStore.activePackage ?: return
        val label = accessStore.activeLabel.ifBlank {
            InstalledAppsLoader.labelFor(context, packageName)
        }
        accessStore.clear()
        launchBlocking(packageName, label)
    }

    private suspend fun evaluate(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastHandledAt < 1_200) return
        lastHandledAt = now

        val label = InstalledAppsLoader.labelFor(context, packageName)
        if (accessStore.activePackage == packageName && accessStore.remainingSeconds > 0) {
            AccessTimerService.ensureRunning(context, packageName, label)
            return
        }

        val balance = minutes.balance()
        if (balance <= 0) {
            launchBlocking(packageName, label)
        } else {
            startAccess(packageName, label)
        }
    }

    private suspend fun startAccess(packageName: String, label: String) {
        val balance = minutes.balance()
        if (balance <= 0) {
            launchBlocking(packageName, label)
            return
        }
        accessStore.start(packageName, label, balance * 60)
        AccessTimerService.ensureRunning(context, packageName, label)
    }

    private fun launchBlocking(packageName: String, label: String) {
        accessStore.lastBlockedAt = System.currentTimeMillis()
        val intent = Intent(context, BlockingActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .putExtra(BlockingActivity.EXTRA_PACKAGE, packageName)
            .putExtra(BlockingActivity.EXTRA_LABEL, label)
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo abrir la pantalla de bloqueo; se notifica", t)
            notifications.notifyBlocked(label)
        }
    }

    companion object {
        private const val TAG = "BlockingGate"
        private val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.android.settings",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
        )
    }
}
