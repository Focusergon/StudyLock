package pe.edu.unjbg.studylock.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pe.edu.unjbg.studylock.core.designsystem.SlBarChart
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlProgressBar
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.R
import pe.edu.unjbg.studylock.domain.model.Mastery
import pe.edu.unjbg.studylock.navigation.Routes
import pe.edu.unjbg.studylock.ui.LocalAppGraph
import pe.edu.unjbg.studylock.ui.masteryBackground
import pe.edu.unjbg.studylock.ui.masteryColor
import pe.edu.unjbg.studylock.ui.masteryLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    ui: HomeUi,
    onStartReview: (String) -> Unit,
    onStartPractice: () -> Unit,
    onImportContent: () -> Unit,
    onOpenAccess: () -> Unit,
    onBrowseCourses: () -> Unit,
    onBlockedApps: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    val dashboard = ui.dashboard
    val accessStore = LocalAppGraph.current.accessStore
    val remaining by produceState(initialValue = if (accessStore.isActive) accessStore.remainingSeconds else null) {
        while (true) {
            value = if (accessStore.isActive) accessStore.remainingSeconds else null
            delay(1_000)
        }
    }

    SlScreen {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.studylock_logo),
                        contentDescription = "Logo de StudyLock",
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hola, ${ui.dashboard?.userName?.substringBefore(' ') ?: "Alex"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SlColors.Ink,
                        )
                        Text(
                            text = formatToday(),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Muted,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SlColors.ChipBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (ui.dashboard?.userName?.firstOrNull() ?: 'A').uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = SlColors.Primary,
                        )
                    }
                }
            }

            item { BalanceCard(dashboard?.availableMinutes ?: 0, goal = ui.goalMinutes, onStart = { onStartReview(dashboard?.nextCourse?.id ?: Routes.WEAK) }) }

            if (ui.practiceEnabled) {
                item {
                    SlCard(
                        color = SlColors.Lavender,
                        onClick = if (ui.hasPracticeQuestions) onStartPractice else onImportContent,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlIconBadge(Icons.Filled.Bolt, SlColors.Purple, SlColors.Surface)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "5 preguntas de prueba",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SlColors.Ink,
                                )
                                Text(
                                    text = if (ui.hasPracticeQuestions) {
                                        "Minutos extra al instante · no cuentan en tu progreso"
                                    } else {
                                        "Importa material o sincroniza tus cursos para usarlas"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlColors.Muted,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = SlColors.Purple,
                            )
                        }
                    }
                }
            }

            if (remaining != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SlColors.Mint)
                            .clickable(onClick = onOpenAccess)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlIconBadge(Icons.Filled.Timer, SlColors.Teal, Color.White)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acceso activo", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "${accessStore.activeLabel} · ${TimeUtils.formatClock(remaining ?: 0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Teal)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TodayStat(
                        value = "${dashboard?.minutesEarnedToday ?: 0}",
                        label = "min ganados",
                        modifier = Modifier.weight(1f),
                    )
                    TodayStat(
                        value = "${dashboard?.questionsToday ?: 0}",
                        label = "preguntas",
                        modifier = Modifier.weight(1f),
                    )
                    TodayStat(
                        value = accuracyLabel(dashboard?.questionsToday ?: 0, dashboard?.correctToday ?: 0),
                        label = "aciertos",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SlCard(
                    color = SlColors.Mint,
                    onClick = onOpenGuide,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.School, SlColors.Teal, SlColors.Surface)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Guía rápida · no otorga minutos", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "4 pasos y 3 preguntas para aprender a usar la app",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Teal)
                    }
                }
            }

            item {
                SlCard(
                    color = SlColors.Surface,
                    onClick = onBrowseCourses,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.School, SlColors.Purple, SlColors.Lavender)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SlSectionLabel("Continuar donde quedaste")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = dashboard?.nextCourse?.name ?: "Sin cursos todavía",
                                style = MaterialTheme.typography.titleMedium,
                                color = SlColors.Ink,
                            )
                            Text(
                                text = dashboard?.nextTopic?.name ?: "Importa tus cursos para empezar",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                    }
                    if (dashboard?.nextCourse != null) {
                        Spacer(Modifier.height(12.dp))
                        SlProgressBar((dashboard.nextCourse!!.masteryPercent / 100f).coerceIn(0f, 1f))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlPill(
                                text = masteryLabel(dashboard.nextTopic?.mastery),
                                textColor = masteryColor(dashboard.nextTopic?.mastery),
                                background = masteryBackground(dashboard.nextTopic?.mastery),
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Repasar 5 preguntas",
                                style = MaterialTheme.typography.labelLarge,
                                color = SlColors.Primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SlColors.ChipBlue)
                                    .clickable {
                                        onStartReview(dashboard.nextCourse!!.id)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Toca para explorar tus cursos",
                            style = MaterialTheme.typography.labelLarge,
                            color = SlColors.Primary,
                        )
                    }
                }
            }

            item {
                SlCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlSectionLabel("Tu semana")
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${dashboard?.week?.sumOf { it.minutes } ?: 0} min de estudio",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlColors.Teal,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SlBarChart(week = dashboard?.week.orEmpty())
                }
            }

            item {
                SlCard(onClick = onBlockedApps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.Lock, SlColors.Danger, SlColors.DangerSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Apps limitadas", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                text = blockedSummary(dashboard?.blockedApps?.map { it.label }.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Muted)
                    }
                }
            }

            if (ui.emergencyEnabled) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlColors.Lavender)
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = SlColors.Purple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Si necesitas entrar con urgencia, la pantalla de bloqueo ofrece un " +
                                "desbloqueo de emergencia de ${pe.edu.unjbg.studylock.domain.RewardPolicy.EMERGENCY_MINUTES} min.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Ink,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun BalanceCard(minutes: Int, goal: Int, onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(SlColors.Navy, SlColors.Primary)))
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MINUTOS DISPONIBLES",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlColors.SubtleText,
                )
                Spacer(Modifier.weight(1f))
                SlPill(
                    text = "Meta $goal min",
                    textColor = Color.White,
                    background = Color.White.copy(alpha = 0.14f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$minutes",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "min",
                    style = MaterialTheme.typography.titleMedium,
                    color = SlColors.SubtleText,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (minutes == 0) "Resuelve un reto rápido para desbloquear tus apps."
                else "Se consumen solo mientras usas una app limitada.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.SubtleText,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(onClick = onStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = SlColors.Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hacer un repaso",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlColors.Navy,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "3–5 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun TodayStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = SlColors.Ink)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
    }
}

private fun formatToday(): String {
    val format = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"))
    return format.format(Date()).replaceFirstChar { it.uppercase() }
}

private fun accuracyLabel(questions: Int, correct: Int): String =
    if (questions == 0) "—" else "${correct * 100 / questions}%"

private fun blockedSummary(labels: List<String>): String = when {
    labels.isEmpty() -> "Aún no has elegido aplicaciones"
    labels.size <= 2 -> labels.joinToString(", ")
    else -> "${labels.take(2).joinToString(", ")} y ${labels.size - 2} más"
}
