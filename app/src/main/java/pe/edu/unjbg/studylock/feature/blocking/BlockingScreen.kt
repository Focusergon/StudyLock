package pe.edu.unjbg.studylock.feature.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.unjbg.studylock.core.designsystem.OptionState
import pe.edu.unjbg.studylock.core.designsystem.SlAnswerOption
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlProgressBar
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.domain.RewardPolicy

@Composable
fun BlockingScreen(
    ui: ChallengeUi,
    appLabel: String,
    onStart: () -> Unit,
    onChoose: (String) -> Unit,
    onNext: () -> Unit,
    onEmergency: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (ui.phase) {
        ChallengePhase.LOCKED -> LockedContent(ui, appLabel, onStart, onEmergency, onDismiss)
        ChallengePhase.LOADING -> LoadingContent(appLabel)
        ChallengePhase.QUESTION -> ChallengeContent(ui, appLabel, onChoose, onNext, onDismiss)
        ChallengePhase.RESULT -> ResultContent(ui, appLabel, onOpen, onDismiss)
    }
}

@Composable
private fun LockedContent(
    ui: ChallengeUi,
    appLabel: String,
    onStart: () -> Unit,
    onEmergency: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SlColors.Navy, SlColors.Primary))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SlColors.Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("FILTRO DE ESTUDIO", style = MaterialTheme.typography.labelSmall, color = SlColors.SubtleText)
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "$appLabel está limitada",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            SlPill(
                text = "0 min disponibles",
                textColor = SlColors.Navy,
                background = SlColors.Teal,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Resuelve ${RewardPolicy.CHALLENGE_QUESTIONS} preguntas de tus cursos para ganar minutos de acceso. " +
                    "Te tomará menos de 1 minuto.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.SubtleText,
                textAlign = TextAlign.Center,
            )

            if (ui.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = ui.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Warning,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))

            SlPrimaryButton(
                text = "Resolver reto rápido",
                onClick = onStart,
                containerColor = Color.White,
                contentColor = SlColors.Navy,
            )
            if (ui.emergencyEnabled) {
                Spacer(Modifier.height(6.dp))
                SlTextButton(
                    text = "Desbloqueo de emergencia · ${RewardPolicy.EMERGENCY_MINUTES} min",
                    onClick = onEmergency,
                    modifier = Modifier.fillMaxWidth(),
                    color = SlColors.SubtleText,
                )
            }
            SlTextButton(
                text = "Volver",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                color = SlColors.SubtleText,
            )
        }
    }
}

@Composable
private fun LoadingContent(appLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SlColors.Navy, SlColors.Primary))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SlColors.Teal)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Preparando tu reto de $appLabel…",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.SubtleText,
            )
        }
    }
}

@Composable
private fun ChallengeContent(
    ui: ChallengeUi,
    appLabel: String,
    onChoose: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val question = ui.current ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlColors.Background)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlPill("Reto para $appLabel", SlColors.Primary, SlColors.ChipBlue)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${ui.index + 1}/${ui.total}",
                style = MaterialTheme.typography.labelLarge,
                color = SlColors.Muted,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Salir",
                style = MaterialTheme.typography.labelMedium,
                color = SlColors.Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SlColors.Border)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            SlProgressBar(
                progress = (ui.index + 1).toFloat() / ui.total.coerceAtLeast(1),
                color = SlColors.Teal,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Text(question.prompt, style = MaterialTheme.typography.headlineSmall, color = SlColors.Ink)
            Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(6.dp))
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
                            text = if (ui.wasCorrect) "¡Correcto!" else "Casi. Revisa la explicación",
                            style = MaterialTheme.typography.titleSmall,
                            color = SlColors.Ink,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = SlColors.Ink)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            SlPrimaryButton(
                text = when {
                    !ui.answered -> "Elige una respuesta"
                    ui.isLast -> "Ver mis minutos"
                    else -> "Siguiente"
                },
                onClick = onNext,
                enabled = ui.answered,
            )
            if (!ui.answered) {
                Spacer(Modifier.height(6.dp))
                SlTextButton(
                    text = "Abandonar reto",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    color = SlColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    ui: ChallengeUi,
    appLabel: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlColors.Background)
            .systemBarsPadding()
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(if (ui.emergency) SlColors.Lavender else SlColors.Mint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (ui.emergency) Icons.Filled.Timer else Icons.Filled.Check,
                contentDescription = null,
                tint = if (ui.emergency) SlColors.Purple else SlColors.Teal,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (ui.emergency) "Acceso de emergencia" else "¡Minutos obtenidos!",
            style = MaterialTheme.typography.headlineMedium,
            color = SlColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                ui.emergency -> "Úsalos con calma. El siguiente desbloqueo de emergencia tendrá espera."
                ui.minutesAwarded > 0 -> "Acertaste ${ui.correctCount} de ${ui.total}. Ya puedes entrar a $appLabel."
                else -> "No encontramos preguntas para tu reto. Intenta de nuevo en un momento."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SlColors.Muted,
            textAlign = TextAlign.Center,
        )
        if (ui.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(ui.error, style = MaterialTheme.typography.bodySmall, color = SlColors.Danger, textAlign = TextAlign.Center)
        }

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
                Text("TIEMPO DE ACCESO", style = MaterialTheme.typography.labelSmall, color = SlColors.SubtleText)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+${ui.minutesAwarded} min",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        SlPrimaryButton(text = "Abrir $appLabel", onClick = onOpen)
        Spacer(Modifier.height(6.dp))
        SlTextButton(
            text = "Volver",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            color = SlColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
    }
}
