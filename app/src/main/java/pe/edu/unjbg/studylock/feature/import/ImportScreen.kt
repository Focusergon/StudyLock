package pe.edu.unjbg.studylock.feature.import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.designsystem.SlTextField
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.data.aula.AulaCourse
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind

/**
 * Pantalla de importación de contenido académico.
 *
 * Es la funcionalidad diferenciadora frente a Anki/Quizlet: el estudiante no crea tarjetas a
 * mano. Puede (1) pegar texto del sílabo, (2) elegir un archivo .txt/.md/.pdf o (3) conectar su
 * Aula Virtual y traer un curso completo; la app genera las preguntas automáticamente.
 */
@Composable
fun ImportScreen(viewModel: ImportViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }

    SlScreen {
        SlTopBar(onBack = onBack, title = "Importar contenido")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Trae tu material y StudyLock crea las preguntas por ti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            ui.error?.let { message ->
                item { BannerCard(message = message, onClose = viewModel::dismissError) }
            }

            ui.result?.let { result ->
                item {
                    ResultCard(
                        title = "Curso listo",
                        message = "«${result.courseName}» con ${result.topicCount} temas y ${result.questionCount} preguntas.",
                        onClose = viewModel::dismissResult,
                        onOpen = onBack,
                    )
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Aula Virtual UNJBG")
            }

            item {
                AulaConnectCard(ui = ui, viewModel = viewModel)
            }

            if (ui.connected) {
                if (ui.courses.isEmpty()) {
                    item {
                        SlCard(color = SlColors.Background, border = false) {
                            Text(
                                text = "Tu cuenta no tiene cursos con matrícula activa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                    }
                } else {
                    items(ui.courses, key = { it.id }) { course ->
                        AulaCourseRow(
                            course = course,
                            importing = ui.importingCourseId == course.id,
                            enabled = ui.importingCourseId == null,
                            onImport = { viewModel.importAulaCourse(course) },
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Pegar texto o subir archivo")
            }

            item {
                SlCard {
                    SlTextField(
                        label = "Nombre del curso",
                        value = ui.courseName,
                        onValueChange = viewModel::setCourseName,
                        placeholder = "Ej. Base de Datos",
                    )
                    Spacer(Modifier.height(12.dp))
                    SlTextField(
                        label = "Docente (opcional)",
                        value = ui.professor,
                        onValueChange = viewModel::setProfessor,
                        placeholder = "Ej. Ing. Quispe",
                    )
                    Spacer(Modifier.height(12.dp))
                    SlTextArea(
                        label = "Material del curso",
                        value = ui.text,
                        onValueChange = viewModel::setText,
                        placeholder = "Pega aquí el sílabo, tus apuntes o el resumen del tema…",
                    )
                    Spacer(Modifier.height(14.dp))
                    SlPrimaryButton(
                        text = if (ui.generating) "Generando preguntas…" else "Generar preguntas",
                        onClick = viewModel::generateFromText,
                        enabled = !ui.generating,
                        icon = Icons.Filled.AutoAwesome,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlColors.Background)
                            .clickable(enabled = !ui.generating) { pickFile.launch(FILE_TYPES) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlIconBadge(Icons.Filled.UploadFile, SlColors.Primary, SlColors.ChipBlue, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Subir archivo", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                text = "Formatos .txt, .md y .pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = SlColors.Muted)
                    }
                }
            }

            if (ui.generating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = SlColors.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Analizando el material…", style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
                    }
                }
            }

            if (ui.topics.isNotEmpty()) {
                item {
                    SlCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlIconBadge(Icons.Filled.CheckCircle, SlColors.Teal, SlColors.Mint, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vista previa", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                                Text(
                                    text = "${ui.topics.size} temas · ${ui.questionCount} preguntas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlColors.Muted,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ui.topics.forEach { topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = topic.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlColors.Ink,
                                    modifier = Modifier.weight(1f),
                                )
                                SlPill("${topic.questions.size}", SlColors.Primary, SlColors.ChipBlue)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SlPrimaryButton(
                            text = if (ui.saving) "Guardando…" else "Guardar curso",
                            onClick = viewModel::save,
                            enabled = ui.canSave,
                            icon = Icons.Filled.CheckCircle,
                        )
                        if (ui.courseName.isBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Escribe un nombre de curso para guardarlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Danger,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (ui.sources.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(2.dp))
                    SlSectionLabel("Importaciones recientes")
                }
                items(ui.sources, key = { it.id }) { source ->
                    SourceRow(
                        title = source.title,
                        subtitle = "${kindLabel(source.kind)} · ${source.questionCount} preguntas · ${TimeUtils.relativeDays(source.createdAt)}",
                        onDelete = { viewModel.deleteSource(source.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AulaConnectCard(ui: ImportUi, viewModel: ImportViewModel) {
    SlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlIconBadge(Icons.Filled.Cloud, SlColors.Primary, SlColors.ChipBlue, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (ui.connected) "Aula Virtual conectada" else "Conectar Aula Virtual",
                    style = MaterialTheme.typography.titleSmall,
                    color = SlColors.Ink,
                )
                Text(
                    text = ui.site?.let { "${it.userName} · ${it.siteName}" }
                        ?: "Moodle UNJBG · importa tus cursos automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
            if (ui.connected) {
                SlPill("Activa", SlColors.Teal, SlColors.Mint)
            }
        }

        if (!ui.connected) {
            Spacer(Modifier.height(12.dp))
            SlTextField(
                label = "Dirección del Aula Virtual",
                value = ui.aulaUrl,
                onValueChange = viewModel::setAulaUrl,
                placeholder = ImportUi.DEFAULT_AULA_URL,
            )
            Spacer(Modifier.height(12.dp))
            SlTextField(
                label = "Clave (token) de servicio web",
                value = ui.aulaToken,
                onValueChange = viewModel::setAulaToken,
                placeholder = "Genera tu token en Moodle → Seguridad → Claves",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tu contraseña institucional nunca se guarda: solo se usa el token que generas en tu perfil de Moodle.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(12.dp))
            SlPrimaryButton(
                text = if (ui.connecting) "Conectando…" else "Conectar",
                onClick = viewModel::connectAula,
                enabled = !ui.connecting,
                icon = Icons.Filled.Link,
            )
        } else {
            Spacer(Modifier.height(6.dp))
            SlTextButton(text = "Desconectar", onClick = viewModel::disconnectAula, color = SlColors.Danger)
        }
    }
}

@Composable
private fun AulaCourseRow(
    course: AulaCourse,
    importing: Boolean,
    enabled: Boolean,
    onImport: () -> Unit,
) {
    SlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlColors.ChipBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.School, contentDescription = null, tint = SlColors.Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.fullName, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                Text(course.shortName, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
            }
            Spacer(Modifier.width(8.dp))
            if (importing) {
                CircularProgressIndicator(color = SlColors.Primary, modifier = Modifier.size(20.dp))
            } else {
                SlTextButton(text = "Importar", onClick = onImport, color = SlColors.Primary)
            }
        }
        if (!enabled && !importing) {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SourceRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlIconBadge(Icons.Filled.Description, SlColors.Muted, SlColors.Background, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = SlColors.Danger, modifier = Modifier.size(18.dp))
        }
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
private fun ResultCard(title: String, message: String, onClose: () -> Unit, onOpen: () -> Unit) {
    SlCard(color = SlColors.Mint, border = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SlColors.Teal)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
        }
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = SlColors.Ink)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SlTextButton(text = "Ver cursos", onClick = onOpen, color = SlColors.Primary)
            SlTextButton(text = "Cerrar", onClick = onClose, color = SlColors.Muted)
        }
    }
}

@Composable
private fun SlTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SlColors.Muted)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = SlColors.Muted) },
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlColors.Surface,
                unfocusedContainerColor = SlColors.Surface,
                focusedBorderColor = SlColors.Primary,
                unfocusedBorderColor = SlColors.Border,
            ),
        )
    }
}

private fun kindLabel(kind: ImportSourceKind): String = when (kind) {
    ImportSourceKind.TEXT -> "Texto pegado"
    ImportSourceKind.FILE -> "Archivo"
    ImportSourceKind.MOODLE -> "Aula Virtual"
}

private val FILE_TYPES = arrayOf(
    "text/plain",
    "text/markdown",
    "application/pdf",
    "application/octet-stream",
)
