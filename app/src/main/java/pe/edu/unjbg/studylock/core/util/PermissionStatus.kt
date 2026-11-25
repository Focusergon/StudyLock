package pe.edu.unjbg.studylock.core.util

/**
 * Estado de los permisos del "Filtro de estudio".
 *
 * [restricted] no es detectable por API pública: se guarda como confirmación del usuario tras
 * abrir los ajustes de la app (Android 13+ con instalación por APK). Va primero porque, si el
 * sistema aplica permisos restringidos, puede impedir activar los otros tres.
 */
data class PermissionStatus(
    val restricted: Boolean = false,
    val usage: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
) {
    val allGranted: Boolean get() = restricted && usage && overlay && accessibility
    val grantedCount: Int
        get() = listOf(restricted, usage, overlay, accessibility).count { it }
    val total: Int get() = 4
}
