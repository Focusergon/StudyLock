package pe.edu.unjbg.studylock.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.OptionState
import pe.edu.unjbg.studylock.core.designsystem.SlAnswerOption
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlProgressBar
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar

@Composable
fun SessionScreen(viewModel: StudyViewModel, onDone: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SlScreen {
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SlColors.Primary)
            }

            ui.finished -> ResultContent(ui, onDone)

            ui.empty -> EmptyContent(onDone)

            else -> QuestionContent(
                ui = ui,
                onChoose = viewModel::choose,
                onNext = viewModel::next,
                onClose = { viewModel.finishNow() },
            )
        }
    }
}

@Composable
private fun ColumnScope.QuestionContent(
    ui: StudyUi,
    onChoose: (String) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val question = ui.current ?: return

    SlTopBar(
        onBack = onClose,
        title = ui.courseName,
        trailing = {
            SlPill(
                text = "Reto ${ui.index + 1}/${ui.total}",
                textColor = SlColors.Primary,
                background = SlColors.ChipBlue,
            )
        },
    )
    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        SlProgressBar(
            progress = (ui.index + 1).toFloat() / ui.total.coerceAtLeast(1),
            color = SlColors.Purple,
        )
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.headlineSmall,
            color = SlColors.Ink,
        )
        Spacer(Modifier.height(18.dp))

        question.options.forEachIndexed { index, option ->
            val state = when {
                !ui.answered -> if (ui.selectedOptionId == option.id) OptionState.SELECTED else OptionState.NORMAL
                option.isCorrect -> OptionState.CORRECT
                ui.selectedOptionId == option.id -> OptionState.WRONG
                else -> OptionState.NORMAL
            }
            SlAnswerOption(
                letter = ('A' + index).toString(),
                text = option.text,
                state = state,
                onClick = { onChoose(option.id) },
            )
            Spacer(Modifier.height(10.dp))
        }

        if (ui.answered) {
            Spacer(Modifier.height(8.dp))
            SlCard(
                color = if (ui.wasCorrect) SlColors.Mint else SlColors.DangerSoft,
                border = false,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ui.wasCorrect) Icons.Filled.Check else Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = if (ui.wasCorrect) SlColors.Teal else SlColors.Danger,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ui.wasCorrect) "¡Correcto!" else "La respuesta correcta era otra",
                        style = MaterialTheme.typography.titleSmall,
                        color = SlColors.Ink,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlColors.Ink,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        SlPrimaryButton(
            text = when {
                ui.finishing -> "Guardando…"
                !ui.answered -> "Elige una respuesta"
                ui.isLast -> "Ver resultado"
                else -> "Siguiente pregunta"
            },
            onClick = onNext,
            enabled = ui.answered && !ui.finishing,
        )
    }
}

@Composable
private fun ResultContent(ui: StudyUi, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (ui.accuracy >= 50) SlColors.Teal else SlColors.Warning,
                            if (ui.accuracy >= 50) Color(0xFF2A9C8C) else Color(0xFFD99A48),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (ui.accuracy >= 50) "¡Buen repaso!" else "Sigue practicando",
            style = MaterialTheme.typography.headlineMedium,
            color = SlColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Acertaste ${ui.correctCount} de ${ui.total} preguntas.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlColors.Muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(SlColors.Navy, SlColors.Primary)))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MINUTOS OBTENIDOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlColors.SubtleText,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+${ui.minutesAwarded} min",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Úsalos cuando quieras en tus apps limitadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.SubtleText,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultStat("${ui.accuracy}%", "aciertos", Modifier.weight(1f))
            ResultStat("${ui.total}", "preguntas", Modifier.weight(1f))
            ResultStat("${ui.correctCount}", "correctas", Modifier.weight(1f))
        }

        Spacer(Modifier.height(28.dp))
        SlPrimaryButton(text = "Volver al inicio", onClick = onDone)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Los temas fallados volverán a aparecer antes en tu próximo repaso.",
            style = MaterialTheme.typography.bodySmall,
            color = SlColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = SlColors.Ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
    }
}

@Composable
private fun EmptyContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = SlColors.Muted, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No encontramos preguntas para este repaso.",
            style = MaterialTheme.typography.titleMedium,
            color = SlColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Vuelve a intentarlo cuando tu Aula Virtual tenga material sincronizado.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        SlPrimaryButton(text = "Volver al inicio", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
