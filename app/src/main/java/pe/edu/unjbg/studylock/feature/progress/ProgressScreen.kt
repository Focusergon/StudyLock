package pe.edu.unjbg.studylock.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.unjbg.studylock.core.designsystem.SlBarChart
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlProgressBar
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.Mastery
import pe.edu.unjbg.studylock.ui.masteryBackground
import pe.edu.unjbg.studylock.ui.masteryColor
import pe.edu.unjbg.studylock.ui.masteryLabel

@Composable
fun ProgressScreen(ui: ProgressUi) {
    val weekly = ui.weekly
    SlScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Tu progreso", style = MaterialTheme.typography.headlineSmall, color = SlColors.Ink)
                Text(
                    text = "Cada repaso cuenta, incluso los de 3 minutos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            item {
                SlCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            SlSectionLabel("Esta semana")
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${weekly?.minutes ?: 0}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = SlColors.Ink,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "min estudiados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlColors.Muted,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                        SlPill(
                            text = "${weekly?.sessions ?: 0} sesiones",
                            textColor = SlColors.Primary,
                            background = SlColors.ChipBlue,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SlBarChart(week = weekly?.week.orEmpty())
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressStat(
                        icon = Icons.Filled.Bolt,
                        value = "${weekly?.consecutiveDays ?: 0} días",
                        label = "racha actual",
                        modifier = Modifier.weight(1f),
                    )
                    ProgressStat(
                        icon = Icons.Filled.CheckCircle,
                        value = "${weekly?.accuracy ?: 0}%",
                        label = "aciertos",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressStat(
                        icon = Icons.Filled.EmojiEvents,
                        value = "${weekly?.questions ?: 0}",
                        label = "preguntas",
                        modifier = Modifier.weight(1f),
                    )
                    ProgressStat(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        value = "${weekly?.sessions ?: 0}",
                        label = "sesiones",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Dominio por curso")
            }

            items(ui.courses, key = { it.id }) { course ->
                CourseMasteryCard(course)
            }

            item {
                SlCard(color = SlColors.Background, border = false) {
                    SlSectionLabel("Cómo leer tus estados")
                    Spacer(Modifier.height(10.dp))
                    LegendRow(Mastery.LEARNED, "Respondes con seguridad y el tema queda programado.")
                    Spacer(Modifier.height(8.dp))
                    LegendRow(Mastery.NEEDS_REVIEW, "Fallaste recientemente; volverá antes en tus retos.")
                    Spacer(Modifier.height(8.dp))
                    LegendRow(Mastery.PENDING, "Aún no lo practicas. Aparecerá en tu próximo repaso.")
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun ProgressStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(SlColors.ChipBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SlColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, color = SlColors.Ink)
            Text(label, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
        }
    }
}

@Composable
private fun CourseMasteryCard(course: Course) {
    val accent = Color(course.accent.toInt())
    SlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                Text(
                    "${course.learnedTopics}/${course.topics.size} temas aprendidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
            Text(
                "${course.masteryPercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
        Spacer(Modifier.height(10.dp))
        SlProgressBar(progress = course.masteryPercent / 100f, color = accent)
        Spacer(Modifier.height(12.dp))
        val weakest = course.topics.sortedBy { it.priority }.take(3)
        if (weakest.isEmpty()) {
            Text(
                "Sin temas sincronizados",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
            )
        } else {
            weakest.forEach { topic ->
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
                    Spacer(Modifier.width(8.dp))
                    SlPill(
                        text = masteryLabel(topic.mastery),
                        textColor = masteryColor(topic.mastery),
                        background = masteryBackground(topic.mastery),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(mastery: Mastery, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        SlPill(
            text = masteryLabel(mastery),
            textColor = masteryColor(mastery),
            background = masteryBackground(mastery),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = SlColors.Muted,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}
