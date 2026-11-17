package pe.edu.unjbg.studylock.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlCheckMark
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.designsystem.SlTextField
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * Pantalla «Cuentas UNJBG».
 *
 * Une todo el flujo: guardar credenciales cifradas, conectar con el mini-navegador (Aula Virtual e
 * Intranet), ver el árbol de cursos/secciones/archivos y generar las preguntas del material elegido.
 */
@Composable
fun AccountsScreen(viewModel: AccountsViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SlScreen {
        SlTopBar(onBack = onBack, title = "Cuentas UNJBG")

        if (ui.browserVisible) {
            WebAutomationPanel(
                status = ui.status,
                onSession = viewModel::attachSession,
                onDestroyed = viewModel::detachSession,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Vincula tus cuentas una sola vez. StudyLock inicia sesión por ti y arma el árbol " +
                        "de cursos y archivos para generar las preguntas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            ui.error?.let { message ->
                item { BannerCard(message = message, onClose = viewModel::dismissError) }
            }
            ui.resultMessage?.let { message ->
                item { ResultBanner(message = message, onClose = viewModel::dismissResult) }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Aula Virtual UNJBG")
            }
            item { CredentialsCard(ui = ui, viewModel = viewModel) }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Árbol de contenido")
            }

            if (!ui.hasTree) {
                item { EmptyTreeCard(ui) }
            } else {
                item { TreeToolbar(ui = ui, viewModel = viewModel) }
                items(ui.tree, key = { it.id }) { course ->
                    CourseTreeCard(
                        course = course,
                        selected = ui.selected,
                        onToggle = viewModel::toggleFile,
                    )
                }
                item {
                    SlPrimaryButton(
                        text = if (ui.importing) {
                            "Generando preguntas…"
                        } else {
                            "Generar preguntas (${ui.importableSelected})"
                        },
                        onClick = viewModel::generate,
                        enabled = ui.canGenerate,
                        icon = Icons.Filled.AutoAwesome,
                    )
                }
            }

            item { PrivacyCard() }
            item { LogoutCard(onLogout = viewModel::logout) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CredentialsCard(ui: AccountsUi, viewModel: AccountsViewModel) {
    var advanced by remember { mutableStateOf(false) }

    SlCard {
        SlTextField(
            label = "Usuario (Aula Virtual)",
            value = ui.aulaUser,
            onValueChange = viewModel::setAulaUser,
            placeholder = "2026-171190",
        )
        Spacer(Modifier.height(12.dp))
        SlTextField(
            label = "Contraseña",
            value = ui.aulaPassword,
            onValueChange = viewModel::setAulaPassword,
            placeholder = "Tu contraseña institucional",
            isPassword = !ui.revealPasswords,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setSamePassword(!ui.samePassword) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlCheckMark(checked = ui.samePassword)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Usar la misma contraseña en la Intranet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlColors.Ink,
                )
                Text(
                    text = "Se reutiliza la contraseña del Aula Virtual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SlTextField(
            label = "Usuario (Intranet, opcional)",
            value = ui.intranetUser,
            onValueChange = viewModel::setIntranetUser,
            placeholder = ui.aulaUser.ifBlank { "Igual que el Aula Virtual" },
        )

        if (!ui.samePassword) {
            Spacer(Modifier.height(12.dp))
            SlTextField(
                label = "Contraseña (Intranet)",
                value = ui.intranetPassword,
                onValueChange = viewModel::setIntranetPassword,
                placeholder = "Contraseña de la Intranet",
                isPassword = !ui.revealPasswords,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlTextButton(
                text = if (ui.revealPasswords) "Ocultar contraseñas" else "Mostrar contraseñas",
                onClick = viewModel::toggleReveal,
                color = SlColors.Muted,
            )
            Spacer(Modifier.weight(1f))
            SlTextButton(
                text = if (advanced) "Ocultar direcciones" else "Direcciones",
                onClick = { advanced = !advanced },
                color = SlColors.Muted,
            )
        }

        if (advanced) {
            SlTextField(
                label = "Aula Virtual",
                value = ui.aulaUrl,
                onValueChange = viewModel::setAulaUrl,
                placeholder = AccountsDefaults.AULA_URL,
            )
            Spacer(Modifier.height(12.dp))
            SlTextField(
                label = "Intranet",
                value = ui.intranetUrl,
                onValueChange = viewModel::setIntranetUrl,
                placeholder = AccountsDefaults.INTRANET_URL,
            )
        }

        Spacer(Modifier.height(14.dp))
        SlPrimaryButton(
            text = if (ui.syncing) "Sincronizando…" else "Conectar y sincronizar",
            onClick = viewModel::sync,
            enabled = ui.canSync,
            icon = Icons.Filled.Cloud,
        )
        Spacer(Modifier.height(8.dp))
        SlTextButton(
            text = if (ui.saving) "Guardando…" else "Guardar credenciales",
            onClick = viewModel::save,
            color = SlColors.Primary,
        )
    }
}

@Composable
private fun TreeToolbar(ui: AccountsUi, viewModel: AccountsViewModel) {
    val total = ui.tree.sumOf { it.files.size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${ui.selectedCount} de $total archivos",
            style = MaterialTheme.typography.labelMedium,
            color = SlColors.Muted,
        )
        Spacer(Modifier.weight(1f))
        SlTextButton(text = "Todos", onClick = viewModel::selectAll, color = SlColors.Primary)
        SlTextButton(text = "Ninguno", onClick = viewModel::clearSelection, color = SlColors.Muted)
    }
}

@Composable
private fun CourseTreeCard(
    course: ContentCourse,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    SlCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlIconBadge(
                icon = if (course.source == ContentSource.INTRANET) Icons.Filled.Description else Icons.Filled.School,
                tint = SlColors.Primary,
                background = SlColors.ChipBlue,
                size = 38.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                Text(
                    text = "${course.source.label} · ${course.sections.size} sección(es) · ${course.importableCount} archivo(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = SlColors.Muted,
            )
        }

        if (expanded) {
            if (course.sections.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sin archivos detectados en este curso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            } else {
                course.sections.forEach { section ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = SlColors.Muted,
                    )
                    section.files.forEach { file ->
                        FileRow(
                            file = file,
                            checked = file.url in selected,
                            onToggle = { onToggle(file.url) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: ContentFile, checked: Boolean, onToggle: () -> Unit) {
    val enabled = file.isImportable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SlColors.Background)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlCheckMark(checked = checked && enabled)
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = if (file.mimeType.equals("application/pdf", ignoreCase = true)) {
                Icons.Filled.PictureAsPdf
            } else {
                Icons.Filled.Description
            },
            contentDescription = null,
            tint = if (enabled) SlColors.Primary else SlColors.Muted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) SlColors.Ink else SlColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (enabled) fileKind(file) else "No compatible",
                style = MaterialTheme.typography.labelSmall,
                color = SlColors.Muted,
            )
        }
    }
}

@Composable
private fun EmptyTreeCard(ui: AccountsUi) {
    SlCard(color = SlColors.Background, border = false) {
        if (ui.syncing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = SlColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = ui.status.ifBlank { "Sincronizando…" },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
        } else {
            Text(
                text = "Aún no hay contenido. Guarda tus credenciales y pulsa «Conectar y sincronizar».",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    SlCard(color = SlColors.ChipBlue, border = false) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = SlColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Tus credenciales se cifran con el almacén seguro de Android y solo se escriben en " +
                    "las páginas oficiales de la UNJBG. Nunca se envían a otros servidores ni se guardan en logs.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Primary,
            )
        }
    }
}

@Composable
private fun LogoutCard(onLogout: () -> Unit) {
    SlCard(color = SlColors.DangerSoft, border = false) {
        Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
        Text(
            text = "Borra las cookies de la UNJBG y las credenciales guardadas en este dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            color = SlColors.Muted,
        )
        Spacer(Modifier.height(6.dp))
        SlTextButton(text = "Cerrar sesión y borrar credenciales", onClick = onLogout, color = SlColors.Danger)
    }
}

@Composable
private fun BannerCard(message: String, onClose: () -> Unit) {
    SlCard(color = SlColors.DangerSoft, border = false) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = SlColors.Danger)
        Spacer(Modifier.height(6.dp))
        SlTextButton(text = "Cerrar", onClick = onClose, color = SlColors.Danger)
    }
}

@Composable
private fun ResultBanner(message: String, onClose: () -> Unit) {
    SlCard(color = SlColors.Mint, border = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SlColors.Teal)
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = SlColors.Ink)
        }
        Spacer(Modifier.height(6.dp))
        SlTextButton(text = "Cerrar", onClick = onClose, color = SlColors.Muted)
    }
}

private fun fileKind(file: ContentFile): String = when {
    file.mimeType.equals("application/pdf", ignoreCase = true) -> "PDF"
    file.mimeType.startsWith("text/") -> "Texto"
    file.name.lowercase().endsWith(".pdf") -> "PDF"
    else -> "Documento"
}
