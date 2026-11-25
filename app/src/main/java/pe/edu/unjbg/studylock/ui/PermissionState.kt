package pe.edu.unjbg.studylock.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import pe.edu.unjbg.studylock.core.util.PermissionStatus
import pe.edu.unjbg.studylock.core.util.PermissionUtils

fun readPermissionStatus(context: Context, restricted: Boolean = false): PermissionStatus = PermissionStatus(
    restricted = restricted,
    usage = PermissionUtils.hasUsageAccess(context),
    overlay = PermissionUtils.hasOverlay(context),
    accessibility = PermissionUtils.isAccessibilityEnabled(context),
)

/**
 * Relee los permisos cada vez que la pantalla vuelve a primer plano.
 * Evita que el usuario tenga que "refrescar" manualmente tras volver de Ajustes.
 */
@Composable
fun rememberPermissionStatus(restricted: Boolean = false): PermissionStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf(readPermissionStatus(context, restricted)) }

    // Si cambia la confirmación (o se reanuda la pantalla tras Ajustes), relectura inmediata.
    LaunchedEffect(restricted) { status = readPermissionStatus(context, restricted) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = readPermissionStatus(context, restricted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}
