package pe.edu.unjbg.studylock.domain.model

/**
 * Árbol de contenido académico descubierto en la web de UNJBG.
 *
 * El «árbol» es la vista que pide el estudiante: qué cursos tiene, qué secciones y qué archivos
 * hay en cada uno. De aquí se eligen los documentos y se generan las preguntas.
 */

/** Sistema universitario del que proviene el contenido. */
enum class ContentSource(val label: String) {
    AULA("Aula Virtual"),
    INTRANET("Intranet"),
}

/** Un archivo descargable descubierto en el campus virtual. */
data class ContentFile(
    val id: String,
    val name: String,
    val url: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
) {
    /** Solo procesamos texto y PDF: es lo que el generador de preguntas puede leer. */
    val isImportable: Boolean
        get() = mimeType.startsWith("text/") ||
            mimeType.equals("application/pdf", ignoreCase = true) ||
            looksImportable(name)
}

/** Agrupación de archivos dentro de un curso (semana, unidad, sección o carpeta). */
data class ContentSection(
    val id: String,
    val name: String,
    val files: List<ContentFile>,
)

/** Curso descubierto con su árbol de secciones y archivos. */
data class ContentCourse(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val source: ContentSource,
    val sections: List<ContentSection> = emptyList(),
) {
    val files: List<ContentFile> get() = sections.flatMap { it.files }
    val importableCount: Int get() = files.count { it.isImportable }
}

private fun looksImportable(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".pdf") ||
        lower.endsWith(".txt") ||
        lower.endsWith(".md") ||
        lower.endsWith(".csv") ||
        lower.endsWith(".doc") ||
        lower.endsWith(".docx")
}
