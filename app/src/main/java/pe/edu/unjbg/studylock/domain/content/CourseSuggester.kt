package pe.edu.unjbg.studylock.domain.content

/**
 * Sugiere el curso al que probablemente pertenece un archivo subido, comparando sus tokens con los
 * nombres de los cursos reales del estudiante. Es solo una **sugerencia**: la app siempre pide
 * confirmación y, si no hay coincidencia, el archivo va a `agregados/`.
 */
object CourseSuggester {

    private val RUIDO = setOf("pdf", "txt", "doc", "docx", "silabo", "syllabus", "tema", "copia", "final")

    fun suggest(fileName: String, courseNames: List<String>, threshold: Double = 0.34): String? {
        val fileTokens = tokens(fileName.substringBeforeLast('.'))
        if (fileTokens.isEmpty()) return null
        return courseNames
            .map { name -> name to score(fileTokens, tokens(name)) }
            .filter { (_, value) -> value >= threshold }
            .maxByOrNull { (_, value) -> value }
            ?.first
    }

    private fun score(a: Set<String>, b: Set<String>): Double {
        if (b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / minOf(a.size, b.size)
    }

    private fun tokens(value: String): Set<String> = value.lowercase()
        .replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 3 && it !in RUIDO }
        .toSet()
}
