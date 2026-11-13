package pe.edu.unjbg.studylock.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.StudyLockApp
import pe.edu.unjbg.studylock.core.designsystem.StudyLockTheme
import pe.edu.unjbg.studylock.core.util.InstalledAppsLoader
import pe.edu.unjbg.studylock.feature.blocking.BlockingScreen
import pe.edu.unjbg.studylock.feature.blocking.ChallengeViewModel
import pe.edu.unjbg.studylock.ui.viewModelFactory

/**
 * Pantalla que intercepta la app limitada.
 *
 * Se abre desde el AccessibilityService (o desde la notificación si el sistema bloquea el
 * lanzamiento directo). Vive en una tarea propia para no apilarse sobre la app interceptada.
 */
class BlockingActivity : ComponentActivity() {

    private val graph get() = (application as StudyLockApp).graph

    private var blockedPackage by mutableStateOf("")
    private var appLabel by mutableStateOf("")

    private val viewModel: ChallengeViewModel by viewModels {
        viewModelFactory { ChallengeViewModel(graph) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readIntent(intent)

        setContent {
            StudyLockTheme {
                val ui by viewModel.ui.collectAsStateWithLifecycle()
                BlockingScreen(
                    ui = ui,
                    appLabel = appLabel,
                    onStart = viewModel::startChallenge,
                    onChoose = viewModel::choose,
                    onNext = viewModel::next,
                    onEmergency = viewModel::emergencyUnlock,
                    onOpen = {
                        if (blockedPackage.isNotBlank()) {
                            graph.gate.onChallengeCompleted(blockedPackage, appLabel)
                            openApp(blockedPackage)
                        } else {
                            finish()
                        }
                    },
                    onDismiss = { goHome() },
                )
            }
        }
    }

    /** "Volver" debe sacar al usuario de la app limitada, no dejarlo dentro para re-bloquear. */
    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(home) }
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val previous = blockedPackage
        readIntent(intent)
        // Si llega otra app interceptada reutilizando esta instancia, el reto debe empezar de cero.
        if (previous.isNotEmpty() && previous != blockedPackage) {
            viewModel.reset()
        }
    }

    private fun readIntent(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE).orEmpty()
        blockedPackage = pkg
        appLabel = intent?.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }
            ?: if (pkg.isNotBlank()) InstalledAppsLoader.labelFor(this, pkg) else ""
    }

    private fun openApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launch) }
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }
}
