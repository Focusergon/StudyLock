package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.domain.model.ContentCourse

/**
 * Resultado de recorrer uno de los portales UNJBG.
 *
 * Nunca se inventa contenido: si no se detecta nada, [courses] queda vacío y [notice] explica
 * qué pasó. [detectedLinks] sirve para el botón «Ver enlaces detectados» (modo inspección): permite
 * al estudiante ver qué encontró el mini-navegador sin que la app cree cursos falsos.
 */
data class ExtractionResult(
    val courses: List<ContentCourse> = emptyList(),
    val notice: String? = null,
    val detectedLinks: List<WebLink> = emptyList(),
    /** Clasificación del modelo (o heurística) para mostrar y confirmar en modo diagnóstico. */
    val verdicts: List<LinkVerdict> = emptyList(),
    val classificationError: String? = null,
)
