package pe.edu.unjbg.studylock.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlIconBadge
import pe.edu.unjbg.studylock.core.designsystem.SlPill
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlScreen
import pe.edu.unjbg.studylock.core.designsystem.SlSearchField
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.designsystem.SlTextField
import pe.edu.unjbg.studylock.core.designsystem.SlTopBar
import pe.edu.unjbg.studylock.core.util.PermissionUtils
import pe.edu.unjbg.studylock.R
import pe.edu.unjbg.studylock.ui.rememberPermissionStatus

@Composable
fun WelcomeScreen(onStart: () -> Unit, onConnect: () -> Unit, onLogs: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SlColors.Navy, SlColors.Primary))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 26.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.studylock_logo),
                    contentDescription = "Logo de StudyLock",
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "StudyLock",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }

            Spacer(Modifier.weight(1f))

            SlPill(
                text = "UNJBG · FILTRO DE ESTUDIO",
                textColor = SlColors.Navy,
                background = SlColors.Teal,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Convierte tu tiempo de ocio en repaso.",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Antes de abrir TikTok, Instagram o YouTube, resuelve 2 preguntas de tus cursos. " +
                    "Lo que estudias te devuelve minutos de acceso.",
                style = MaterialTheme.typography.bodyLarge,
                color = SlColors.SubtleText,
            )

            Spacer(Modifier.height(26.dp))
            BenefitRow(Icons.Filled.School, "Cursos importados de tu Aula Virtual")
            BenefitRow(Icons.Filled.QueryStats, "Preguntas generadas de tus sílabos")
            BenefitRow(Icons.Filled.Timer, "Sesiones de 3 a 5 minutos")

            Spacer(Modifier.weight(1f))

            SlPrimaryButton(
                text = "Comenzar",
                onClick = onStart,
                containerColor = Color.White,
                contentColor = SlColors.Navy,
            )
            Spacer(Modifier.height(6.dp))
            SlTextButton(
                text = "Ya tengo cuenta",
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                color = SlColors.SubtleText,
            )
            SlTextButton(
                text = "Ver registro de errores",
                onClick = onLogs,
                modifier = Modifier.fillMaxWidth(),
                color = SlColors.SubtleText,
            )
        }
    }
}

@Composable
fun ReturningScreen(
    viewModel: ReturningViewModel,
    onContinue: () -> Unit,
    onFullSetup: () -> Unit,
    onSync: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SlScreen {
        SlTopBar(onBack = onFullSetup, title = "Ya tengo cuenta")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Solo confirma tu correo institucional",
                style = MaterialTheme.typography.headlineSmall,
                color = SlColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El resto ya está configurado en este dispositivo: permisos, apps limitadas y " +
                    "credenciales UNJBG. No vuelvas a escribirlos.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.Muted,
            )

            Spacer(Modifier.height(22.dp))
            SlTextField(
                label = "Correo institucional o Gmail",
                value = ui.email,
                onValueChange = viewModel::setEmail,
                placeholder = "nombre@unjbg.edu.pe",
                keyboardType = KeyboardType.Email,
            )
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error!!, style = MaterialTheme.typography.bodySmall, color = SlColors.Danger)
            }

            Spacer(Modifier.height(18.dp))
            SlCard(color = if (ui.hasSavedAccount) SlColors.Mint else SlColors.DangerSoft, border = false) {
                Text(
                    text = if (ui.hasSavedAccount) {
                        "Cuentas UNJBG ya vinculadas"
                    } else {
                        "No hay credenciales guardadas en este dispositivo"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = SlColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (ui.hasSavedAccount) {
                        "Puedes sincronizar tus cursos cuando quieras desde Cuentas UNJBG."
                    } else {
                        "Si desinstalaste la app, necesitas vincularlas una sola vez."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
                if (!ui.hasSavedAccount && ui.loaded) {
                    Spacer(Modifier.height(12.dp))
                    SlTextButton(text = "Vincular ahora", onClick = onFullSetup, color = SlColors.Primary)
                }
            }

            Spacer(Modifier.height(22.dp))
            SlPrimaryButton(
                text = "Continuar al Inicio",
                onClick = { viewModel.continueToApp(onContinue) },
                enabled = ui.canContinue,
                icon = null,
            )
            Spacer(Modifier.height(8.dp))
            SlTextButton(text = "Sincronizar mis cursos", onClick = onSync, color = SlColors.Primary)
            Spacer(Modifier.height(6.dp))
            SlTextButton(text = "Empezar configuración completa", onClick = onFullSetup, color = SlColors.Muted)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SlColors.Teal, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = SlColors.SubtleText)
    }
}

@Composable
fun ConnectAccountScreen(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onContinue: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SlScreen {
        SlTopBar(onBack = onBack, title = "Conectar UNJBG")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Vincula tu cuenta institucional",
                style = MaterialTheme.typography.headlineSmall,
                color = SlColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "StudyLock inicia sesión en el Aula Virtual y la Intranet con tu usuario y contraseña, " +
                    "y arma el árbol de cursos y archivos para generar las preguntas. No tendrás que crear tarjetas a mano.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(20.dp))

            SlCard(onClick = onOpenAccounts) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SlIconBadge(Icons.Filled.School, SlColors.Primary, SlColors.ChipBlue, size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vincular cuentas UNJBG", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                        Text(
                            text = "Aula Virtual e Intranet · guarda tus credenciales cifradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Muted,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SlColors.Muted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Tu perfil en la app",
                style = MaterialTheme.typography.labelMedium,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(8.dp))

            SlTextField(
                label = "Nombre",
                value = ui.name,
                onValueChange = viewModel::setName,
                placeholder = "Alex Ramírez",
                enabled = !ui.connected,
            )
            Spacer(Modifier.height(16.dp))
            SlTextField(
                label = "Correo institucional",
                value = ui.email,
                onValueChange = viewModel::setEmail,
                placeholder = "alex.ramirez@unjbg.edu.pe",
                keyboardType = KeyboardType.Email,
                enabled = !ui.connected,
            )
            if (ui.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(ui.error!!, style = MaterialTheme.typography.bodySmall, color = SlColors.Danger)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlColors.ChipBlue)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SlColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Tus credenciales se cifran con el almacén seguro de Android y solo se usan para " +
                        "iniciar sesión en las páginas oficiales de la UNJBG. Nunca se envían a otros servidores.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Primary,
                )
            }

            Spacer(Modifier.height(24.dp))
            if (!ui.connected) {
                SlPrimaryButton(
                    text = if (ui.connecting) "Guardando…" else "Guardar mi perfil",
                    onClick = viewModel::connect,
                    enabled = ui.canConnect && !ui.connecting,
                    icon = null,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlColors.Mint)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SlColors.Teal)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Perfil guardado", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                        Text(
                            "Puedes vincular tus cuentas UNJBG ahora o más tarde desde Ajustes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlColors.Muted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SlPrimaryButton(text = "Continuar", onClick = onContinue)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionsScreen(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val status = rememberPermissionStatus(ui.restrictedAcknowledged)

    SlScreen {
        SlTopBar(onBack = onBack, title = "Permisos")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Activa el filtro de estudio",
                style = MaterialTheme.typography.headlineSmall,
                color = SlColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Primero activa Permisos restringidos y luego los tres permisos del filtro: " +
                    "sin ese paso previo, algunos dispositivos no dejan activar los demás.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(20.dp))

            RestrictedPermissionCard(
                acknowledged = ui.restrictedAcknowledged,
                onOpen = { runCatching { context.startActivity(PermissionUtils.appDetailsIntent(context)) } },
                onAcknowledge = viewModel::setRestrictedAcknowledged,
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                icon = Icons.Filled.QueryStats,
                title = "Acceso al uso de apps",
                description = "Detecta qué aplicación pasa a primer plano. No lee su contenido.",
                granted = status.usage,
                onOpen = { runCatching { context.startActivity(PermissionUtils.usageAccessIntent()) } },
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                icon = Icons.Filled.Visibility,
                title = "Superposición",
                description = "Permite mostrar la pantalla del reto encima de la app limitada.",
                granted = status.overlay,
                onOpen = { runCatching { context.startActivity(PermissionUtils.overlayIntent(context)) } },
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                icon = Icons.Filled.School,
                title = "Servicio de accesibilidad",
                description = "Refuerza la detección en tiempo real. Solo usamos el nombre del paquete.",
                granted = status.accessibility,
                onOpen = { runCatching { context.startActivity(PermissionUtils.accessibilityIntent()) } },
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlColors.Lavender)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = SlColors.Purple, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "El acceso se lee únicamente para aplicar el bloqueo. No guardamos historial de uso " +
                        "ni compartimos datos con terceros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Ink,
                )
            }

            Spacer(Modifier.height(20.dp))
            SlPrimaryButton(
                text = when {
                    status.allGranted -> "Continuar"
                    !status.restricted -> "Activa primero Permisos restringidos"
                    else -> "Faltan ${status.total - status.grantedCount} permisos"
                },
                onClick = onContinue,
                enabled = status.allGranted,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RestrictedPermissionCard(
    acknowledged: Boolean,
    onOpen: () -> Unit,
    onAcknowledge: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (acknowledged) SlColors.Mint else SlColors.DangerSoft)
            .border(1.dp, if (acknowledged) SlColors.Teal else SlColors.Danger, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlIconBadge(
                icon = Icons.Filled.Lock,
                tint = if (acknowledged) SlColors.Teal else SlColors.Danger,
                background = SlColors.Surface,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("1. Permisos restringidos", style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                Text(
                    text = "Android 13+ con APK: Información de la app → menú ⋮ → Permitir permisos restringidos. " +
                        "Sin esto, el sistema puede impedir activar los otros permisos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Abrir ajustes de la app",
            style = MaterialTheme.typography.labelLarge,
            color = SlColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SlColors.Surface)
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ya lo activé",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = acknowledged,
                onCheckedChange = onAcknowledge,
                colors = SwitchDefaults.colors(checkedTrackColor = SlColors.Teal),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SlColors.Surface)
            .border(1.dp, if (granted) SlColors.Teal else SlColors.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlIconBadge(
            icon = icon,
            tint = if (granted) SlColors.Teal else SlColors.Primary,
            background = if (granted) SlColors.Mint else SlColors.ChipBlue,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
            Spacer(Modifier.height(8.dp))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = SlColors.Teal, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Concedido", style = MaterialTheme.typography.labelSmall, color = SlColors.Teal)
                }
            } else {
                Text(
                    text = "Activar",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SlColors.ChipBlue)
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun AppSelectionScreen(viewModel: OnboardingViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadApps(context) }

    SlScreen {
        SlTopBar(onBack = onBack, title = "Apps a limitar")
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                text = "¿Qué apps quieres limitar?",
                style = MaterialTheme.typography.headlineSmall,
                color = SlColors.Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Cuando se te acaben los minutos, estas apps mostrarán un reto rápido.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlColors.Muted,
            )
            Spacer(Modifier.height(14.dp))
            SlSearchField(
                value = ui.query,
                onValueChange = viewModel::setQuery,
                placeholder = "Buscar aplicación",
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${ui.selected.size} seleccionadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = SlColors.Primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Recomendadas primero",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlColors.Muted,
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        if (ui.loadingApps) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SlColors.Primary)
            }
        } else {
            val filtered = viewModel.filteredApps()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val selected = app.packageName in ui.selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlColors.Surface)
                            .border(1.dp, if (selected) SlColors.Teal else SlColors.Border, RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleApp(app.packageName) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlColors.Background),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(Icons.Filled.School, contentDescription = null, tint = SlColors.Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleSmall, color = SlColors.Ink)
                            Text(app.category, style = MaterialTheme.typography.bodySmall, color = SlColors.Muted)
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (selected) SlColors.Teal else SlColors.ChipBlue)
                                .border(1.dp, if (selected) SlColors.Teal else SlColors.Border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(SlColors.Background)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            SlPrimaryButton(
                text = if (ui.saving) "Guardando…" else "Guardar y continuar",
                onClick = { viewModel.saveSelection(onDone) },
                enabled = ui.selected.isNotEmpty() && !ui.saving,
            )
        }
    }
}
