package pe.edu.unjbg.studylock.feature.accounts

import android.annotation.SuppressLint
import android.webkit.WebView
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession

/**
 * Mini-navegador fijo en la raíz de la app.
 *
 * A diferencia del panel anterior, este composable **no se desmonta al cambiar de ruta**: el WebView
 * se crea una sola vez y permanece attached aunque el usuario vaya al Inicio, Cursos o Ajustes.
 * Por eso la sincronización sigue avanzando y el árbol parcial se puede ver desde cualquier sitio.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SyncBrowserHost(controller: AccountsViewModel) {
    val ui by controller.ui.collectAsStateWithLifecycle()

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

        // Se mantiene siempre en la composición (altura 0 cuando no sincroniza) para no destruirlo.
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
                WebView(context).also { web ->
                    controller.attachSession(UnjbgWebSession(web))
                }
            },
            update = { web ->
                if (!ui.syncing) runCatching { web.stopLoading() }
            },
        )
    }
}
