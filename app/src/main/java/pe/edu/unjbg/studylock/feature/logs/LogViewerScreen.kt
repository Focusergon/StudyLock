package pe.edu.unjbg.studylock.feature.logs

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.core.log.StudyLockLog

/** Pantalla para leer, copiar y compartir los fallos registrados en el propio dispositivo. */
@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf(StudyLockLog.allText()) }
    var copied by remember { mutableStateOf(false) }
    val shareText = text.ifBlank { "Sin errores registrados." }

    SlScreen {
        SlTopBar(onBack = onBack, title = "Registro de errores")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Text(
                text = "Si la app se queda en blanco o algo falla, aquí aparece el motivo. " +
                    "Copia todo y envíame el texto.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(10.dp))

            Row {
                SlTextButton(
                    text = "Actualizar",
                    onClick = { text = StudyLockLog.allText(); copied = false },
                    color = SlColors.Primary,
                )
                SlTextButton(
                    text = "Limpiar",
                    onClick = { StudyLockLog.clear(); text = ""; copied = false },
                    color = SlColors.Danger,
                )
            }

            SlPrimaryButton(
                text = if (copied) "Copiado ✓" else "Copiar todo",
                onClick = {
                    clipboard.setText(AnnotatedString(shareText))
                    copied = true
                    StudyLockLog.log("Diagnóstico", "Registro copiado al portapapeles")
                },
                icon = null,
            )
            Spacer(Modifier.height(8.dp))
            SlPrimaryButton(
                text = "Compartir / enviar",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "StudyLock - registro de errores")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Enviar registro")) }
                        .onFailure { StudyLockLog.error("Diagnóstico", it, "No se pudo compartir") }
                },
                containerColor = SlColors.Teal,
                icon = null,
            )
            Spacer(Modifier.height(12.dp))

            SlCard(modifier = Modifier.weight(1f)) {
                val vertical = rememberScrollState()
                val horizontal = rememberScrollState()
                SelectionContainer {
                    Text(
                        text = shareText,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlColors.Ink,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vertical)
                            .horizontalScroll(horizontal),
                    )
                }
            }
        }
    }
}
