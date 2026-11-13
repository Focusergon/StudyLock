package pe.edu.unjbg.studylock.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import pe.edu.unjbg.studylock.StudyLockApp

/**
 * Detecta qué aplicación pasa a primer plano.
 *
 * Nota de producto/privacidad: no se lee el contenido de la ventana; solo el nombre del paquete.
 */
class StudyLockAccessibilityService : AccessibilityService() {

    private val graph get() = (application as StudyLockApp).graph

    override fun onServiceConnected() {
        super.onServiceConnected()
        graph.gate.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString().orEmpty()
        // TYPE_WINDOW_STATE_CHANGED también se dispara para ventanas auxiliares (popups,
        // animaciones de cierre, etc.) que NO son la app en primer plano. Esas ventanas tienen
        // una clase de framework (android.widget.*, android.view.*); las pantallas reales
        // exponen una Activity. Descartarlas evita falsos bloqueos y un estado "pegado".
        if (className.startsWith("android.")) return
        graph.gate.onForeground(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        graph.gate.onServiceDisconnected()
        return super.onUnbind(intent)
    }
}
