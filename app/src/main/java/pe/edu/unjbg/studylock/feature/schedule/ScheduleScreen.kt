package pe.edu.unjbg.studylock.feature.schedule

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import pe.edu.unjbg.studylock.core.designsystem.SlTextField
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.domain.model.ClassBlock

/**
 * Horario de clases del estudiante.
 *
 * Con estos bloques, StudyLock sabe cuándo hay franjas libres y solo entonces envía recordatorios
 * de repaso (ver `SmartReminder`).
 */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SlScreen {
        SlTopBar(onBack = onBack, title = "Horario de clases")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Registra tus clases para recibir recordatorios solo en tus ratos libres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            item { DaySelector(selected = ui.dayOfWeek, onSelect = viewModel::selectDay) }

            item {
                SlCard(color = SlColors.Mint, border = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(Icons.Filled.EventAvailable, SlColors.Teal, SlColors.Surface, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Franjas libres", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                text = if (ui.freeWindows.isEmpty()) "Sin huecos disponibles este día"
                                else ui.freeWindows.joinToString(" · ") { "${ClassBlock.formatMinute(it.first)}–${ClassBlock.formatMinute(it.second)}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Ink,
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Agregar bloque · ${ClassBlock.DAY_LABELS[ui.dayOfWeek - 1]}")
            }

            item {
                SlCard {
                    SlTextField(
                        label = "Curso o actividad",
                        value = ui.courseName,
                        onValueChange = viewModel::setCourseName,
                        placeholder = "Ej. Cálculo II",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SlTextField(
                            label = "Inicio",
                            value = ui.startText,
                            onValueChange = viewModel::setStartText,
                            placeholder = "08:00",
                            modifier = Modifier.weight(1f),
                        )
                        SlTextField(
                            label = "Fin",
                            value = ui.endText,
                            onValueChange = viewModel::setEndText,
                            placeholder = "10:00",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SlTextField(
                        label = "Aula (opcional)",
                        value = ui.room,
                        onValueChange = viewModel::setRoom,
                        placeholder = "Ej. Pabellón B · 204",
                    )
                    Spacer(Modifier.height(14.dp))
                    SlPrimaryButton(text = "Agregar al horario", onClick = viewModel::addBlock, icon = Icons.Filled.Add)
                    ui.error?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Danger,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Bloques del día")
            }

            if (ui.todayBlocks.isEmpty()) {
                item {
                    SlCard(color = SlColors.Background, border = false) {
                        Text(
                            text = "Aún no registras clases para este día.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Muted,
                        )
                    }
                }
            } else {
                items(ui.todayBlocks, key = { it.id }) { block ->
                    BlockRow(block = block, onDelete = { viewModel.removeBlock(block.id) })
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DaySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ClassBlock.DAY_SHORT.forEachIndexed { index, label ->
            val day = index + 1
            val active = day == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) SlColors.Primary else SlColors.Surface)
                    .border(1.dp, if (active) SlColors.Primary else SlColors.Border, RoundedCornerShape(12.dp))
                    .clickable { onSelect(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) SlColors.Surface else SlColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun BlockRow(block: ClassBlock, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .border(1.dp, SlColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(block.startLabel, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            Text(block.endLabel, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(block.courseName, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            if (block.room.isNotBlank()) {
                Text(block.room, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
            }
        }
        SlPill("${block.endMinute - block.startMinute} min", SlColors.Primary, SlColors.ChipBlue)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = SlColors.Danger, modifier = Modifier.size(18.dp))
        }
    }
}
