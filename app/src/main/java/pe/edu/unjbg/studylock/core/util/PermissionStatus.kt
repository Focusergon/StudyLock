package pe.edu.unjbg.studylock.core.util

/** Estado de los permisos críticos del "Filtro de estudio". */
data class PermissionStatus(
    val usage: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
) {
    val allGranted: Boolean get() = usage && overlay && accessibility
    val grantedCount: Int get() = listOf(usage, overlay, accessibility).count { it }
    val total: Int get() = 3
}
