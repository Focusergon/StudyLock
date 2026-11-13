package pe.edu.unjbg.studylock.feature.access

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pe.edu.unjbg.studylock.blocking.AccessTimerService
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.core.designsystem.SlPrimaryButton
import pe.edu.unjbg.studylock.core.designsystem.SlTextButton
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.ui.LocalAppGraph

@Composable
fun AccessActiveScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = LocalAppGraph.current.accessStore
    var remaining by remember { mutableIntStateOf(store.remainingSeconds) }
    var label by remember { mutableStateOf(store.activeLabel) }
    val packageName = store.activePackage
    val totalSeconds = remember { store.remainingSeconds.coerceAtLeast(1) }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = store.remainingSeconds
            label = store.activeLabel
            delay(500)
        }
    }

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
                Text(
                    text = "ACCESO TEMPORAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlColors.SubtleText,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label.ifBlank { "Aplicación desbloqueada" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(230.dp)) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color.White.copy(alpha = 0.14f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    val fraction = (remaining.toFloat() / totalSeconds).coerceIn(0f, 1f)
                    drawArc(
                        color = SlColors.Teal,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TimeUtils.formatClock(remaining),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "TIEMPO RESTANTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlColors.SubtleText,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Cada minuto se descuenta solo mientras usas esta app. " +
                    "Al llegar a cero volverás a ver el reto.",
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.SubtleText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            SlPrimaryButton(
                text = "Volver a la app",
                onClick = {
                    val launch = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
                    if (launch != null) {
                        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(launch) }
                    } else {
                        onClose()
                    }
                },
                containerColor = Color.White,
                contentColor = SlColors.Navy,
                icon = null,
            )
            Spacer(Modifier.height(6.dp))
            SlTextButton(
                text = "Finalizar antes",
                onClick = {
                    AccessTimerService.finish(context)
                    store.clear()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
                color = SlColors.SubtleText,
            )
        }
    }
}
