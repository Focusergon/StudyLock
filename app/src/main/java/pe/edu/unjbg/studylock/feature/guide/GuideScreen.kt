package pe.edu.unjbg.studylock.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar

private data class GuideQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

private val QUESTIONS = listOf(
    GuideQuestion(
        prompt = "¿Qué ocurre al abrir una app bloqueada sin minutos?",
        options = listOf(
            "Aparece un reto corto; al resolverlo ganas acceso temporal",
            "La app se cierra y no puedes volver a entrar",
            "Se descargan todos los cursos automáticamente",
            "Se borran tus respuestas anteriores",
        ),
        correctIndex = 0,
        explanation = "StudyLock te permite estudiar 2 preguntas para recuperar unos minutos de acceso.",
    ),
    GuideQuestion(
        prompt = "¿Cómo llegan tus cursos a StudyLock?",
        options = listOf(
            "Los escribe el profesor desde la Intranet",
            "Desde Cuentas UNJBG, con el mini-navegador que arma el árbol",
            "Se copian del almacenamiento del celular sin tu permiso",
            "Se inventan automáticamente al instalar la app",
        ),
        correctIndex = 1,
        explanation = "Inicias sesión en el Aula Virtual y la app recorre tus cursos, secciones y archivos.",
    ),
    GuideQuestion(
        prompt = "¿De dónde salen las preguntas?",
        options = listOf(
            "De preguntas fijas que vienen con la app",
            "De internet, sin usar tus archivos",
            "De los archivos que descargaste del Aula Virtual o la Intranet, y de los que agregaste tú",
            "Del historial de apps bloqueadas",
        ),
        correctIndex = 2,
        explanation = "Se generan en tu celular a partir del material del árbol o de los archivos que importes.",
    ),
)

/**
 * Guía de uso con 3 preguntas de comprobación.
 *
 * Es deliberadamente independiente de repositorios: no inserta sesiones, respuestas, minutos ni
 * estadísticas. Por eso no puede otorgar tiempo ni alterar el progreso real del estudiante.
 */
@Composable
fun GuideScreen(onBack: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    SlScreen {
        SlTopBar(onBack = onBack, title = "Guía rápida")

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SlCard(color = SlColors.ChipBlue, border = false) {
                    Text("Aprende a usar StudyLock", style = MaterialTheme.typography.titleMedium, color = SlColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "4 pasos y 3 preguntas de prueba. Esta guía no otorga minutos ni cuenta en tus estadísticas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlColors.Muted,
                    )
                }
            }

            item {
                SlSectionLabel("Cómo empezar")
            }

            item {
                SlCard {
                    STEP_LABELS.forEachIndexed { position, text ->
                        StepRow(number = position + 1, text = text)
                        if (position != STEP_LABELS.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (!finished) {
                item { Spacer(Modifier.height(2.dp)) }
                item { SlSectionLabel("Comprueba lo aprendido · ${index + 1}/${QUESTIONS.size}") }
                item {
                    val question = QUESTIONS[index]
                    SlCard {
                        Text(question.prompt, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                        Spacer(Modifier.height(12.dp))
                        question.options.forEachIndexed { optionIndex, text ->
                            val isChosen = selected == optionIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChosen) SlColors.ChipBlue else SlColors.Background)
                                    .clickable(enabled = selected == null) {
                                        selected = optionIndex
                                        if (optionIndex == question.correctIndex) correctCount++
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isChosen) SlColors.Primary else SlColors.Border),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(text, style = MaterialTheme.typography.bodyMedium, color = SlColors.Ink)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SlPrimaryButton(
                            text = if (index == QUESTIONS.lastIndex) "Ver resultado" else "Siguiente",
                            onClick = {
                                if (index == QUESTIONS.lastIndex) {
                                    finished = true
                                } else {
                                    index++
                                    selected = null
                                }
                            },
                            enabled = selected != null,
                        )
                    }
                }
            } else {
                item {
                    SlCard(color = SlColors.Mint, border = false) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SlColors.Teal)
                            Spacer(Modifier.width(10.dp))
                            Text("Guía terminada", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Respondiste bien $correctCount de ${QUESTIONS.size} preguntas. " +
                                "Recuerda: esto solo era una prueba, no suma minutos ni estadísticas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Ink,
                        )
                        Spacer(Modifier.height(12.dp))
                        SlPrimaryButton(
                            text = "Volver a empezar",
                            onClick = {
                                index = 0
                                selected = null
                                correctCount = 0
                                finished = false
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SlColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SlColors.Surface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SlColors.Ink,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private val STEP_LABELS = listOf(
    "Vincula tus cuentas en Cuentas UNJBG con tu usuario y contraseña institucionales.",
    "Conecta y sincroniza: la app arma el árbol de cursos, secciones y archivos.",
    "Elige los archivos del árbol o sube los tuyos y genera las preguntas.",
    "Cuando se acaben tus minutos, resuelve el reto para ganar acceso temporal.",
)
