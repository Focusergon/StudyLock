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
import androidx.compose.material3.LinearProgressIndicator
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
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSectionLabel
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.util.PermissionUtils
import pe.edu.unjbg.studylock.data.ai.ModelStatus
import pe.edu.unjbg.studylock.ui.rememberPermissionStatus

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onImport: () -> Unit, onAccounts: () -> Unit, onSchedule: () -> Unit, onBlockedApps: () -> Unit, onResetOnboarding: () -> Unit) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus(ui.prefs.restrictedPermissionAcknowledged)

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
                    icon = Icons.Filled.Lock,
                    title = "Permisos restringidos",
                    subtitle = "Paso previo en Android 13+ con APK",
                    granted = ui.prefs.restrictedPermissionAcknowledged,
                    onOpen = { runCatching { context.startActivity(PermissionUtils.appDetailsIntent(context)) } },
                )
                SettingSwitch(
                    title = "Ya activé Permisos restringidos",
                    subtitle = "Confirma cuando el sistema te lo permita",
                    checked = ui.prefs.restrictedPermissionAcknowledged,
                    onCheckedChange = viewModel::setRestrictedAcknowledged,
                )
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
                    Spacer(Modifier.height(10.dp))
                    SettingSwitch(
                        title = "Preguntas de prueba",
                        subtitle = "5 preguntas al inicio con minutos extra; solo se desactivan estas",
                        checked = ui.prefs.practiceQuestionsEnabled,
                        onCheckedChange = viewModel::setPracticeQuestions,
                    )
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
                SlSectionLabel("Bloqueo")
            }

            item {
                SlCard(onClick = onBlockedApps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(
                            icon = Icons.Filled.Lock,
                            tint = SlColors.Danger,
                            background = SlColors.DangerSoft,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Apps limitadas", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Elige qué apps muestran el reto al agotar tus minutos",
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
                SlCard(onClick = onAccounts) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlIconBadge(
                            icon = Icons.Filled.School,
                            tint = SlColors.Primary,
                            background = SlColors.ChipBlue,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cuentas UNJBG", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(
                                "Aula Virtual e Intranet: inicia sesión y arma tu árbol de cursos",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlColors.Muted,
                            )
                        }
                        SlPill("Nuevo", SlColors.Teal, SlColors.Mint)
                    }
                }
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
                                "Texto pegado o archivo .pdf",
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
                SlSectionLabel("Sincronización")
            }

            item {
                SlCard {
                    SettingSwitch(
                        title = "Sincronización automática",
                        subtitle = "Al abrir la app, una vez al día, sin tocar nada",
                        checked = ui.prefs.autoSync,
                        onCheckedChange = viewModel::setAutoSync,
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingSwitch(
                        title = "Sincronizar solo por Wi-Fi",
                        subtitle = "Evita consumir datos móviles",
                        checked = ui.prefs.autoSyncWifiOnly,
                        enabled = ui.prefs.autoSync,
                        onCheckedChange = viewModel::setAutoSyncWifiOnly,
                    )
                }
            }

            item {
                Spacer(Modifier.height(2.dp))
                SlSectionLabel("Generador local")
            }

            item {
                SlCard {
                    Text(
                        "Usar generador de preguntas local",
                        style = MaterialTheme.typography.titleSmall,
                        color = SlColors.Ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingSwitch(
                        title = "Usar el generador al crear preguntas",
                        subtitle = "Mezcla el modelo con el generador de respaldo",
                        checked = ui.prefs.aiQuestions,
                        onCheckedChange = viewModel::setAiQuestions,
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingSwitch(
                        title = "Descargar modelo solo por Wi-Fi",
                        subtitle = "El binario ocupa unos 290 MB",
                        checked = ui.prefs.modelWifiOnly,
                        enabled = ui.prefs.aiQuestions,
                        onCheckedChange = viewModel::setModelWifiOnly,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Gemma 3 270M se ejecuta en tu teléfono: sin clave de API, sin subir tu material " +
                            "y con verificación SHA-256 antes de usarlo. Si no está listo, se usa el respaldo determinista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlColors.Muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    when (ui.model.status) {
                        ModelStatus.READY -> SlPill("Modelo verificado", SlColors.Teal, SlColors.Mint)
                        ModelStatus.DOWNLOADING -> {
                            LinearProgressIndicator(
                                progress = { ui.model.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = SlColors.Primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(ui.model.message, style = MaterialTheme.typography.labelSmall, color = SlColors.Muted)
                        }
                        ModelStatus.WAITING_WIFI, ModelStatus.ERROR -> {
                            Text(ui.model.message, style = MaterialTheme.typography.labelSmall, color = SlColors.Danger)
                            Spacer(Modifier.height(8.dp))
                        }
                        ModelStatus.MISSING -> Unit
                    }
                    SlPrimaryButton(
                        text = when (ui.model.status) {
                            ModelStatus.READY -> "Modelo listo"
                            ModelStatus.DOWNLOADING -> "Descargando ${ui.model.progress}%"
                            else -> "Aceptar y descargar modelo"
                        },
                        onClick = viewModel::downloadModel,
                        enabled = !ui.modelReady && !ui.downloading,
                        icon = null,
                    )
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
                    Text("StudyLock 0.3.0-beta.2", style = MaterialTheme.typography.labelMedium, color = SlColors.Muted)
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
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SlColors.Teal),
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
