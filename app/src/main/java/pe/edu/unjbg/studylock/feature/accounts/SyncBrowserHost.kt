package pe.edu.unjbg.studylock.feature.accounts

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPill
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
    val context = LocalContext.current
    var browserCreated by remember { mutableStateOf(false) }

    LaunchedEffect(ui.syncing, ui.browserFailed) {
        when {
            ui.browserFailed -> browserCreated = false
            ui.syncing -> browserCreated = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (ui.confirmationNeeded) {
            SlCard(modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
                Text(
                    text = "Revisa los enlaces clasificados",
                    style = MaterialTheme.typography.titleSmall,
                    color = SlColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "El modelo local propuso ${ui.classifiedLinks.size} enlace(s). " +
                        "Confirma cuáles son cursos o sílabos; los demás se ignoran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
                Spacer(Modifier.height(10.dp))
                ui.classifiedLinks.take(MAX_VISIBLE_LINKS).forEach { verdict ->
                    val accepted = verdict.href in ui.confirmedHrefs
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = verdict.label.take(90),
                                style = MaterialTheme.typography.labelMedium,
                                color = SlColors.Ink,
                            )
                            Text(
                                text = "${verdict.role.name} · ${verdict.reason.take(90)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlColors.Muted,
                                maxLines = 2,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        SlPill(
                            text = if (accepted) "SÍ" else "NO",
                            textColor = if (accepted) SlColors.Teal else SlColors.Danger,
                            background = if (accepted) SlColors.Mint else SlColors.DangerSoft,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlTextButton(
                            text = "Esto es un curso / sílabo",
                            onClick = { controller.setLinkDecision(verdict.href, true) },
                            color = if (accepted) SlColors.Teal else SlColors.Primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        SlTextButton(
                            text = "Esto no",
                            onClick = { controller.setLinkDecision(verdict.href, false) },
                            color = if (accepted) SlColors.Muted else SlColors.Danger,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SlTextButton(text = "Todos sí", onClick = { controller.setAllLinks(true) }, color = SlColors.Teal)
                    Spacer(Modifier.width(6.dp))
                    SlTextButton(text = "Ninguno", onClick = { controller.setAllLinks(false) }, color = SlColors.Muted)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SlTextButton(
                        text = "Continuar",
                        onClick = controller::confirmClassifiedLinks,
                        color = SlColors.Teal,
                    )
                    Spacer(Modifier.width(8.dp))
                    SlTextButton(text = "Saltar", onClick = controller::skipClassifiedLinks, color = SlColors.Danger)
                    Spacer(Modifier.width(8.dp))
                    SlTextButton(
                        text = "Copiar informe",
                        onClick = { copyReport(context, controller) },
                        color = SlColors.Primary,
                    )
                }
            }
        }

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
                    if (ui.diagnosticMode) {
                        Spacer(Modifier.width(8.dp))
                        SlTextButton(
                            text = "Copiar informe",
                            onClick = { copyReport(context, controller) },
                            color = SlColors.Primary,
                        )
                    }
                }
            }
        }

        if (browserCreated) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            ui.manualLoginNeeded || ui.confirmationNeeded -> 260.dp
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

private fun copyReport(context: Context, controller: AccountsViewModel) {
    val report = controller.diagnosticReport()
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("StudyLock", report))
        Toast.makeText(context, "Informe copiado", Toast.LENGTH_SHORT).show()
    }.onFailure {
        StudyLockLog.error("Diagnóstico", it, "No se pudo copiar el informe")
    }
}

private const val MAX_VISIBLE_LINKS = 12
