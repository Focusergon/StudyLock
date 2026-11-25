package pe.edu.unjbg.studylock.domain.content

import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSection
import pe.edu.unjbg.studylock.domain.model.ContentSource

/** Resultado de juntar el Aula Virtual con los sílabos de la Intranet. */
data class MergedContent(
    val courses: List<ContentCourse>,
    /** Sílabos sin curso del Aula al que asociarlos: van a `agregados/` con un aviso. */
    val unmatchedSyllabi: List<ContentCourse>,
)

/**
 * Asocia cada sílabo de la Intranet con el curso del Aula del mismo nombre.
 *
 * No se inventan cursos: un sílabo que no coincide se devuelve en [MergedContent.unmatchedSyllabi]
 * para guardarlo en `agregados/` y avisar al estudiante.
 */
object ContentMerger {

    private val RUIDO = setOf("silabo", "syllabus", "curso", "de", "del", "la", "el", "plan", "curricular")

    fun merge(
        aula: List<ContentCourse>,
        intranet: List<ContentCourse>,
        threshold: Double = 0.6,
    ): MergedContent {
        if (intranet.isEmpty()) return MergedContent(aula, emptyList())
        val result = aula.toMutableList()
        val unmatched = mutableListOf<ContentCourse>()

        for (syllabus in intranet) {
            val key = tokens(syllabus.name)
            val match = result
                .filter { it.source == ContentSource.AULA }
                .map { course -> course to score(key, tokens(course.name)) }
                .filter { (_, value) -> value >= threshold }
                .maxByOrNull { (_, value) -> value }
                ?.first

            if (match == null) {
                unmatched += syllabus
                continue
            }

            val section = ContentSection(
                id = "silabo-${syllabus.id}",
                name = "Sílabo",
                files = syllabus.files.map { file ->
                    file.copy(id = "silabo-${file.id}")
                },
            )
            val index = result.indexOfFirst { it.id == match.id }
            result[index] = match.copy(sections = match.sections + section)
        }
        return MergedContent(result, unmatched)
    }

    private fun score(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val common = a.intersect(b).size
        return common.toDouble() / minOf(a.size, b.size)
    }

    private fun tokens(name: String): Set<String> = name.lowercase()
        .replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 3 && it !in RUIDO }
        .toSet()
}
