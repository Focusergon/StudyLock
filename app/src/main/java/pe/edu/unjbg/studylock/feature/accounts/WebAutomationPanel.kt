package pe.edu.unjbg.studylock.feature.accounts

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pe.edu.unjbg.studylock.core.designsystem.SlCard
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession

/**
 * Mini-navegador visible: muestra la web real de la UNJBG mientras la automatización inicia sesión
 * y recorre el contenido. Es transparente y permite al estudiante intervenir si aparece un aviso.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAutomationPanel(
    status: String,
    onSession: (UnjbgWebSession) -> Unit,
    onDestroyed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holder = remember { arrayOfNulls<UnjbgWebSession>(1) }

    SlCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = SlColors.Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = status.ifBlank { "Trabajando…" },
                style = MaterialTheme.typography.bodySmall,
                color = SlColors.Ink,
            )
        }
        Spacer(Modifier.height(10.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            factory = { context ->
                WebView(context).also { web ->
                    val session = UnjbgWebSession(web)
                    holder[0] = session
                    onSession(session)
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            holder[0]?.destroy()
            holder[0] = null
            onDestroyed()
        }
    }
}
