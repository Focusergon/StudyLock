package pe.edu.unjbg.studylock.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.School
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
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlProgressBar
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.Mastery
import pe.edu.unjbg.studylock.ui.masteryBackground
import pe.edu.unjbg.studylock.ui.masteryColor
import pe.edu.unjbg.studylock.ui.masteryLabel

@Composable
fun CoursesScreen(
    ui: CoursesUi,
    onOpenCourse: (String) -> Unit,
    onQuickReview: () -> Unit,
    onImport: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    SlScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Mis cursos", style = MaterialTheme.typography.headlineSmall, color = SlColors.Ink)
                Text(
                    text = "2026-I · ${ui.courses.size} cursos · ${ui.totalTopics} temas",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(SlColors.Purple, Color(0xFF7A5CB8))))
                        .clickable(onClick = onQuickReview)
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Repaso adaptativo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                text = "5 preguntas de los temas que más necesitas reforzar",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.SubtleText,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }

            item {
                SlCard(color = SlColors.Mint, onClick = onOpenGuide) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.School, SlColors.Teal, SlColors.Surface)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Guía rápida", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Aprende la app en 4 pasos · no otorga minutos",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Teal)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SlColors.Surface)
                        .border(1.dp, SlColors.Border, RoundedCornerShape(20.dp))
                        .clickable(onClick = onImport)
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SlColors.ChipBlue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = SlColors.Primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importar contenido", style = MaterialTheme.typography.titleMedium, color = SlColors.Ink)
                            Text(
                                text = "Aula Virtual, texto o archivo .pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Muted)
                    }
                }
            }

            if (ui.courses.isEmpty() && !ui.loading) {
                item {
                    SlCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = SlColors.Muted, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aún no hay cursos sincronizados",
                                style = MaterialTheme.typography.titleSmall,
                                color = SlColors.Ink,
                            )
                            Text(
                                "Conecta tu cuenta UNJBG o importa tu material para empezar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(14.dp))
                            SlPrimaryButton(
                                text = "Importar contenido",
                                onClick = onImport,
                                icon = Icons.Filled.Add,
                            )
                        }
                    }
                }
            } else {
                items(ui.courses, key = { it.id }) { course ->
                    CourseCard(course = course, onClick = { onOpenCourse(course.id) })
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    val accent = Color(course.accent.toInt())
    SlCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = course.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium, color = SlColors.Ink)
                Text(
                    text = "${course.professor} · ${course.topics.size} temas",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Muted)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlSectionLabel("Dominio")
            Spacer(Modifier.weight(1f))
            Text("${course.masteryPercent}%", style = MaterialTheme.typography.labelLarge, color = SlColors.Primary)
        }
        Spacer(Modifier.height(6.dp))
        SlProgressBar(progress = course.masteryPercent / 100f, color = accent)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SlPill("${course.learnedTopics} aprendidos", SlColors.Teal, SlColors.Mint)
            SlPill("${course.accuracy}% aciertos", SlColors.Primary, SlColors.ChipBlue)
            SlPill("${course.questionsAsked} respuestas", SlColors.Muted, SlColors.Background)
        }
    }
}

@Composable
fun CourseDetailScreen(course: Course?, onBack: () -> Unit, onStartReview: (String) -> Unit) {
    if (course == null) {
        Box(Modifier.fillMaxSize().background(SlColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SlColors.Primary)
        }
        return
    }
    val accent = Color(course.accent.toInt())

    SlScreen {
        SlTopBar(onBack = onBack, title = "Detalle del curso")
        Column(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(SlColors.Navy, SlColors.Primary)))
                            .padding(20.dp),
                    ) {
                        Column {
                            Text(course.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${course.professor} · ${course.semester}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.SubtleText,
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${course.masteryPercent}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "de dominio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlColors.SubtleText,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            SlProgressBar(progress = course.masteryPercent / 100f, color = accent)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStat("${course.questionsAsked}", "respuestas", Modifier.weight(1f))
                        DetailStat("${course.accuracy}%", "aciertos", Modifier.weight(1f))
                        DetailStat("${course.learnedTopics}/${course.topics.size}", "aprendidos", Modifier.weight(1f))
                    }
                }

                item {
                    Spacer(Modifier.height(2.dp))
                    SlSectionLabel("Temas del curso")
                }

                items(course.topics, key = { it.id }) { topic ->
                    SlCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topic.name, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (topic.reviewCount == 0) "Sin repasar todavía"
                                    else "${topic.accuracy}% de acierto · ${TimeUtils.relativeDays(topic.lastReviewAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlColors.Muted,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            SlPill(
                                text = masteryLabel(topic.mastery),
                                textColor = masteryColor(topic.mastery),
                                background = masteryBackground(topic.mastery),
                            )
                        }
                        if (topic.mastery != Mastery.PENDING) {
                            Spacer(Modifier.height(10.dp))
                            SlProgressBar(
                                progress = topic.accuracy / 100f,
                                color = masteryColor(topic.mastery),
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            SlPrimaryButton(
                text = "Iniciar repaso · 5 preguntas",
                onClick = { onStartReview(course.id) },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Gana hasta 5 minutos de acceso al terminar.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailStat(value: String, label: String, modifier: Modifier = Modifier) {
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
