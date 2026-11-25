package pe.edu.unjbg.studylock.feature.accounts

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.log.StudyLockLog
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession

/**
 * Mini-navegador fijo en la raíz de la app.
 *
 * El WebView **no se crea al abrir la app**: se construye la primera vez que se sincroniza y luego
 * permanece montado (a 1 dp cuando no se usa), para que navegar a otras pantallas no lo destruya.
 * Si el dispositivo no puede crear un WebView, se registra el fallo y se muestra en el visor de
 * errores en vez de dejar la pantalla en blanco.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SyncBrowserHost(controller: AccountsViewModel) {
    val ui by controller.ui.collectAsStateWithLifecycle()
    var browserCreated by remember { mutableStateOf(false) }

    LaunchedEffect(ui.syncing, ui.browserFailed) {
        when {
            ui.browserFailed -> browserCreated = false
            ui.syncing -> browserCreated = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (ui.syncing) {
            SlCard(modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
                val progress = if (ui.syncTotal > 0) ui.syncDone.toFloat() / ui.syncTotal else 0f
                if (ui.syncTotal > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = SlColors.Primary,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = buildString {
                        if (ui.syncPhase.isNotBlank()) append("${ui.syncPhase} ")
                        if (ui.syncTotal > 0) append("${ui.syncDone}/${ui.syncTotal} · ")
                        append(ui.status.ifBlank { "Sincronizando…" })
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ui.manualLoginNeeded) {
                        SlTextButton(
                            text = "Ya terminé el ingreso",
                            onClick = controller::confirmManualLogin,
                            color = SlColors.Teal,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    SlTextButton(
                        text = "Cancelar",
                        onClick = controller::cancelSync,
                        color = SlColors.Danger,
                    )
                }
            }
        }

        if (browserCreated) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            ui.manualLoginNeeded -> 260.dp
                            ui.syncing -> 150.dp
                            else -> 1.dp
                        },
                    ),
                factory = { context ->
                    runCatching<View> {
                        WebView(context).also { web ->
                            controller.attachSession(UnjbgWebSession(web))
                            StudyLockLog.log("Navegador", "WebView creado")
                        }
                    }.getOrElse { error ->
                        controller.reportBrowserFailure(
                            "No se pudo crear el mini-navegador en este dispositivo: ${error.message}",
                        )
                        FrameLayout(context)
                    }
                },
                update = { view ->
                    if (view is WebView && !ui.syncing) runCatching { view.stopLoading() }
                },
            )
        }
    }
}
