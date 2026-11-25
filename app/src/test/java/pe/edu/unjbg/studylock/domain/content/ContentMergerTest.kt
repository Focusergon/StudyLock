package pe.edu.unjbg.studylock.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSection
import pe.edu.unjbg.studylock.domain.model.ContentSource

class ContentMergerTest {

    private fun course(id: String, name: String, source: ContentSource) = ContentCourse(
        id = id,
        name = name,
        source = source,
        sections = listOf(
            ContentSection(
                id = "semana1",
                name = "Semana 1",
                files = listOf(ContentFile("$id-f", "material.pdf", "https://unjbg/$id.pdf")),
            ),
        ),
    )

    @Test
    fun `syllabus with matching name becomes a section of the aula course`() {
        val aula = course("bd", "Base de Datos I", ContentSource.AULA)
        val silabo = course("sil", "Sílabo de Base de Datos I", ContentSource.INTRANET)

        val result = ContentMerger.merge(listOf(aula), listOf(silabo))

        assertEquals(1, result.courses.size)
        assertTrue(result.unmatchedSyllabi.isEmpty())
        val sections = result.courses.first().sections
        assertEquals(2, sections.size)
        assertEquals("Sílabo", sections.last().name)
        assertEquals("silabo-sil-f", sections.last().files.first().id)
    }

    @Test
    fun `syllabus without match is returned for agregados`() {
        val aula = course("bd", "Base de Datos I", ContentSource.AULA)
        val silabo = course("sil", "Sílabo de Física Cuántica", ContentSource.INTRANET)

        val result = ContentMerger.merge(listOf(aula), listOf(silabo))

        assertEquals(1, result.courses.size)
        assertEquals(1, result.unmatchedSyllabi.size)
        assertEquals("Sílabo de Física Cuántica", result.unmatchedSyllabi.first().name)
        assertTrue(result.courses.first().sections.none { it.name == "Sílabo" })
    }

    @Test
    fun `no syllabi leaves the aula tree untouched`() {
        val aula = course("bd", "Base de Datos I", ContentSource.AULA)
        val result = ContentMerger.merge(listOf(aula), emptyList())
        assertEquals(listOf(aula), result.courses)
        assertTrue(result.unmatchedSyllabi.isEmpty())
    }
}
