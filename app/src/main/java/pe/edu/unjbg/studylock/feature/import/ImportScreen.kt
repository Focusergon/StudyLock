package pe.edu.unjbg.studylock.feature.import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind

/**
 * Pantalla de importación de contenido académico.
 *
 * Es la funcionalidad diferenciadora frente a Anki/Quizlet: el estudiante no crea tarjetas a
 * mano. Puede (1) pegar texto del sílabo, (2) elegir un archivo .txt/.md/.pdf o (3) conectar su
 * Aula Virtual y traer un curso completo; la app genera las preguntas automáticamente.
 */
@Composable
fun ImportScreen(viewModel: ImportViewModel, onBack: () -> Unit, onAccounts: () -> Unit) {
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
                SlCard(onClick = onAccounts) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.School, SlColors.Primary, SlColors.ChipBlue, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cuentas UNJBG", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                text = "Inicia sesión en el Aula Virtual e Intranet y arma tu árbol de cursos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.Filled.Link, contentDescription = null, tint = SlColors.Muted)
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

            if (ui.needsUploadConfirmation) {
                item { UploadConfirmationCard(ui = ui, viewModel = viewModel) }
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
                                    text = "${ui.topics.size} temas · ${ui.questionCount} preguntas" +
                                        if (ui.usedAi) " · IA local" else "",
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
private fun UploadConfirmationCard(ui: ImportUi, viewModel: ImportViewModel) {
    SlCard(color = SlColors.ChipBlue, border = false) {
        Text("Confirma cómo guardar el archivo", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            text = ui.pendingFile.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = SlColors.Muted,
        )
        Spacer(Modifier.height(12.dp))

        Text("Categoría", style = MaterialTheme.typography.labelMedium, color = SlColors.Muted)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ui.categories.forEach { category ->
                val selected = ui.selectedCategory.equals(category, ignoreCase = true)
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) SlColors.Surface else SlColors.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) SlColors.Primary else SlColors.Surface)
                        .clickable { viewModel.setCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlTextField(
                label = "Crear nueva categoría",
                value = ui.customCategory,
                onValueChange = viewModel::setCustomCategory,
                placeholder = "Ej. Laboratorio",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SlTextButton(text = "Crear", onClick = viewModel::createCategory)
        }
        Spacer(Modifier.height(12.dp))

        Text("Curso sugerido", style = MaterialTheme.typography.labelMedium, color = SlColors.Muted)
        Spacer(Modifier.height(6.dp))
        if (ui.suggestedCourse != null) {
            val suggested = ui.courses.firstOrNull { it.name == ui.suggestedCourse }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlColors.Surface)
                    .clickable { viewModel.setCourse(suggested?.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlIconBadge(Icons.Filled.School, SlColors.Primary, SlColors.ChipBlue, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = ui.suggestedCourse.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (ui.selectedCourseId == suggested?.id) "Elegido" else "Usar",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlColors.Primary,
                )
            }
        } else {
            Text(
                text = "No encontramos un curso parecido. Se guardará en «Agregados».",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        SlTextButton(
            text = "Sin curso (guardar en Agregados)",
            onClick = { viewModel.setCourse(null) },
            color = if (ui.selectedCourseId == null) SlColors.Primary else SlColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
        SlPrimaryButton(
            text = if (ui.generating) "Guardando y analizando…" else "Guardar copia y generar preguntas",
            onClick = viewModel::confirmUpload,
            enabled = !ui.generating,
            icon = Icons.Filled.UploadFile,
        )
        Spacer(Modifier.height(6.dp))
        SlTextButton(text = "Cancelar", onClick = viewModel::cancelUpload, color = SlColors.Muted)
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
    ImportSourceKind.AULA -> "Aula Virtual"
    ImportSourceKind.INTRANET -> "Intranet"
}

private val FILE_TYPES = arrayOf(
    "text/plain",
    "text/markdown",
    "application/pdf",
    "application/octet-stream",
)
