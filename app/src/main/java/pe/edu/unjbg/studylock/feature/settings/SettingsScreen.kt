package pe.edu.unjbg.studylock.feature.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.util.PermissionUtils
import pe.edu.unjbg.studylock.ui.rememberPermissionStatus

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onImport: () -> Unit, onSchedule: () -> Unit, onResetOnboarding: () -> Unit) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus()

    SlScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, color = SlColors.Ink)
                Text(
                    text = "Controla permisos, metas y privacidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }

            item {
                SlCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SlColors.ChipBlue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (ui.prefs.displayName.firstOrNull() ?: 'A').uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = SlColors.Primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ui.prefs.displayName, style = MaterialTheme.typography.titleMedium, color = SlColors.Ink)
                            Text(
                                text = ui.prefs.academicEmail.ifBlank { "Cuenta UNJBG no vinculada" },
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        SlPill("UNJBG", SlColors.Teal, SlColors.Mint)
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Permisos del sistema")
            }

            item {
                PermissionRow(
                    icon = Icons.Filled.QueryStats,
                    title = "Acceso al uso de apps",
                    subtitle = "Detecta la app en primer plano",
                    granted = permissions.usage,
                    onOpen = { runCatching { context.startActivity(PermissionUtils.usageAccessIntent()) } },
                )
            }
            item {
                PermissionRow(
                    icon = Icons.Filled.Visibility,
                    title = "Superposición",
                    subtitle = "Muestra el reto sobre la app",
                    granted = permissions.overlay,
                    onOpen = { runCatching { context.startActivity(PermissionUtils.overlayIntent(context)) } },
                )
            }
            item {
                PermissionRow(
                    icon = Icons.Filled.School,
                    title = "Servicio de accesibilidad",
                    subtitle = "Refuerza la detección en tiempo real",
                    granted = permissions.accessibility,
                    onOpen = { runCatching { context.startActivity(PermissionUtils.accessibilityIntent()) } },
                )
            }
            item {
                PermissionRow(
                    icon = Icons.Filled.Lock,
                    title = "Optimización de batería",
                    subtitle = "Evita que el sistema detenga el temporizador",
                    granted = PermissionUtils.isIgnoringBatteryOptimizations(context),
                    onOpen = { runCatching { context.startActivity(PermissionUtils.batteryIntent(context)) } },
                )
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Estudio")
            }

            item {
                SlCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Meta diaria de estudio", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Repasos cortos para llegar sin presión",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        StepperButton("−") { viewModel.decreaseGoal() }
                        Text(
                            text = "${ui.prefs.dailyGoalMinutes} min",
                            style = MaterialTheme.typography.labelLarge,
                            color = SlColors.Ink,
                            modifier = Modifier
                                .width(64.dp)
                                .padding(horizontal = 6.dp),
                            textAlign = TextAlign.Center,
                        )
                        StepperButton("+") { viewModel.increaseGoal() }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorios", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Avisos en franjas libres entre clases",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Switch(
                            checked = ui.prefs.remindersEnabled,
                            onCheckedChange = viewModel::setReminders,
                            colors = SwitchDefaults.colors(checkedTrackColor = SlColors.Teal),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desbloqueo de emergencia", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "2 minutos para no frustrarte y desinstalar",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Switch(
                            checked = ui.prefs.emergencyEnabled,
                            onCheckedChange = viewModel::setEmergency,
                            colors = SwitchDefaults.colors(checkedTrackColor = SlColors.Teal),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlColors.ChipBlue)
                            .clickable(onClick = onSchedule)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlIconBadge(
                            icon = Icons.Filled.CalendarMonth,
                            tint = SlColors.Primary,
                            background = SlColors.Surface,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Horario de clases", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Define tus clases para avisarte solo en ratos libres",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlColors.Muted)
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Contenido")
            }

            item {
                SlCard(onClick = onImport) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(
                            icon = Icons.Filled.CloudDownload,
                            tint = SlColors.Primary,
                            background = SlColors.ChipBlue,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importar contenido", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Aula Virtual UNJBG, texto pegado o archivo .pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        SlPill("Nuevo", SlColors.Teal, SlColors.Mint)
                    }
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Privacidad")
            }

            item {
                SlCard(color = SlColors.Background, border = false) {
                    Text(
                        text = "StudyLock guarda tus respuestas, cursos y minutos solo en este dispositivo. " +
                            "El servicio de accesibilidad lee únicamente el nombre del paquete de la app en pantalla; " +
                            "no accede a mensajes, fotos ni contenido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlColors.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Abrir ajustes de la app en Android",
                        style = MaterialTheme.typography.labelLarge,
                        color = SlColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SlColors.ChipBlue)
                            .clickable {
                                runCatching { context.startActivity(PermissionUtils.appDetailsIntent(context)) }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Cuenta y datos")
            }

            item {
                SlCard {
                    Text(
                        text = "Volver a ver el onboarding",
                        style = MaterialTheme.typography.titleSmall,
                        color = SlColors.Ink,
                    )
                    Text(
                        text = "Reinicia la vinculación y la selección de apps. Tus cursos y minutos se conservan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlColors.Muted,
                    )
                    Spacer(Modifier.height(6.dp))
                    SlTextButton(
                        text = "Reiniciar onboarding",
                        onClick = { viewModel.resetOnboarding(onResetOnboarding) },
                        color = SlColors.Danger,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("StudyLock 0.1.0", style = MaterialTheme.typography.labelMedium, color = SlColors.Muted)
                    Text(
                        "Hecho para estudiantes de la UNJBG",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlColors.Surface)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlIconBadge(
            icon = icon,
            tint = if (granted) SlColors.Teal else SlColors.Muted,
            background = if (granted) SlColors.Mint else SlColors.ChipBlue,
            size = 38.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
        }
        SlPill(
            text = if (granted) "Activo" else "Pendiente",
            textColor = if (granted) SlColors.Teal else SlColors.Danger,
            background = if (granted) SlColors.Mint else SlColors.DangerSoft,
        )
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SlColors.ChipBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = SlColors.Primary)
    }
}
