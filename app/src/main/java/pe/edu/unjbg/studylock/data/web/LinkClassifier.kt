package pe.edu.unjbg.studylock.data.web

import java.net.URLDecoder

/**
 * Heurísticas puras para clasificar enlaces encontrados al navegar la web de UNJBG.
 *
 * No dependen de Android, así que se pueden probar en la JVM. Son la base de la «automatización
 * que busca lo que queremos»: distinguir cursos, sílabos y archivos descargables.
 */
object LinkClassifier {

    private val IMPORTABLE_EXTENSIONS = listOf(".pdf", ".txt", ".md", ".csv", ".doc", ".docx")
    private val FILE_HINTS = listOf(".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".zip")
    private val SYLLABUS_WORDS = listOf("silabo", "syllabus", "plan curricular", "plan de estudio")

    /** ¿El enlace apunta a un archivo (documento) y no a otra página? */
    fun looksLikeFile(url: String, text: String = ""): Boolean {
        val lowerUrl = url.lowercase()
        val lowerText = text.lowercase()
        if (FILE_HINTS.any { lowerUrl.contains(it) || lowerText.endsWith(it) }) return true
        return lowerUrl.contains("/archivos/") ||
            lowerUrl.contains("/files/") ||
            lowerUrl.contains("pluginfile") ||
            lowerUrl.contains("/download")
    }

    /** ¿Podemos convertir este documento en preguntas (texto o PDF)? */
    fun isImportable(nameOrUrl: String): Boolean {
        val lower = nameOrUrl.lowercase().substringBefore('?')
        return IMPORTABLE_EXTENSIONS.any { lower.endsWith(it) }
    }

    /** ¿El enlace parece un curso del campus? */
    fun looksLikeCourse(text: String, url: String): Boolean {
        val lowerUrl = url.lowercase()
        if (looksLikeFile(url, text)) return false
        if (isSyllabusLink(text, url)) return false
        return lowerUrl.contains("/curso/") ||
            lowerUrl.contains("/course/") ||
            lowerUrl.contains("courseid=") ||
            lowerUrl.contains("/aula/") ||
            lowerUrl.contains("/seccion/")
    }

    /** ¿El enlace lleva a la sección de sílabos? */
    fun isSyllabusLink(text: String, url: String): Boolean {
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains("/silabo") || lowerUrl.contains("/syllabus")) return true
        val normalized = normalize(text)
        return SYLLABUS_WORDS.any { normalized.contains(normalize(it)) }
    }

    /** ¿El texto del enlace parece una sección del curso (unidad, semana, tema, recurso…)? */
    fun looksLikeSection(text: String): Boolean {
        val normalized = normalize(text)
        return normalized.isNotBlank() && SECTION_WORDS.any { normalized.contains(it) }
    }

    /** Nombre de archivo legible a partir de una URL. */
    fun fileNameFromUrl(url: String): String {
        val clean = url.substringBefore('#').substringBefore('?').trimEnd('/')
        val last = clean.substringAfterLast('/').ifBlank { "Documento" }
        return runCatching { URLDecoder.decode(last, "UTF-8") }.getOrDefault(last)
    }

    /** Tipo MIME aproximado según la extensión. */
    fun mimeFor(name: String): String = when (name.lowercase().substringAfterLast('.')) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "md" -> "text/markdown"
        "csv" -> "text/csv"
        else -> ""
    }

    /** Minúsculas sin tildes, para comparar textos de la web. */
    fun normalize(text: String): String = text.lowercase()
        .map { ACCENTS[it] ?: it }
        .joinToString("")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Forma canónica de una URL para comparar: sin fragmento ni barra final. */
    fun canonical(url: String): String = url.substringBefore('#').trimEnd('/')

    /** ¿Dos direcciones pertenecen al mismo sitio? */
    fun sameHost(a: String, b: String): Boolean {
        val hostA = hostOf(a)
        return hostA.isNotEmpty() && hostA == hostOf(b)
    }

    /** ¿[url] cuelga de [base]? Sirve para no salir del curso que estamos recorriendo. */
    fun isWithin(url: String, base: String): Boolean {
        val prefix = canonical(base).substringBefore('?')
        return prefix.isNotEmpty() && canonical(url).startsWith(prefix)
    }

    /**
     * ¿Vale la pena seguir este enlace al recorrer un curso?
     *
     * Se descartan archivos, sílabos, enlaces externos y los del menú de navegación; así la
     * automatización se queda dentro del curso y no recorre todo el campus.
     */
    fun isCrawlableLink(text: String, url: String, base: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("mailto:") || lower.startsWith("tel:")) return false
        if (url.startsWith("#")) return false
        if (looksLikeFile(url, text) || isSyllabusLink(text, url)) return false
        if (!sameHost(url, base)) return false
        val normalized = normalize(text)
        if (normalized.isBlank() || normalized in NAV_WORDS) return false
        return true
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")

    private val ACCENTS = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    )

    private val NAV_WORDS = setOf(
        "inicio", "home", "salir", "cerrar sesion", "cerrar sesión", "perfil", "mi perfil",
        "ayuda", "contacto", "soporte", "notificaciones", "configuracion", "configuración",
        "buscar", "anterior", "siguiente", "volver", "regresar", "menu", "menú",
    )

    private val SECTION_WORDS = listOf(
        "unidad", "semana", "tema", "modulo", "recurso", "material", "contenido",
        "capitulo", "practica", "laboratorio", "sesion", "evaluacion", "tarea",
        "actividad", "documento", "archivo", "clase", "avance",
    )
}
