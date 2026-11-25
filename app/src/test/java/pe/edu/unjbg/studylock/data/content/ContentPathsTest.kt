package pe.edu.unjbg.studylock.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSection
import pe.edu.unjbg.studylock.domain.model.ContentSource

class ContentPathsTest {

    @Test
    fun `sanitize produces safe folder and file names`() {
        assertEquals("Base de Datos I", ContentPaths.sanitize("  Base de Datos I  "))
        assertFalse(ContentPaths.sanitize("a/b\\c:d").contains('/'))
        assertFalse(ContentPaths.sanitize("a/b\\c:d").contains('\\'))
        assertEquals("sin-nombre", ContentPaths.sanitize("   "))
    }

    @Test
    fun `sha256 is stable and different for different bytes`() {
        val a = ContentPaths.sha256("hola".toByteArray())
        val b = ContentPaths.sha256("hola".toByteArray())
        val c = ContentPaths.sha256("chau".toByteArray())
        assertEquals(64, a.length)
        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun `incremental download skips unchanged files`() {
        val file = ContentFile("1", "tema.pdf", "https://unjbg/tema.pdf", sizeBytes = 100)
        assertTrue(ContentPaths.needsDownload(file, null, null))
        assertFalse(ContentPaths.needsDownload(file, "abc", 100))
        assertTrue(ContentPaths.needsDownload(file, "abc", 120))
    }

    @Test
    fun `fingerprint changes when the tree changes`() {
        val file = ContentFile("1", "tema.pdf", "https://unjbg/tema.pdf", sizeBytes = 100)
        val course = ContentCourse(
            id = "bd",
            name = "Base de Datos",
            source = ContentSource.AULA,
            sections = listOf(ContentSection(id = "s1", name = "Semana 1", files = listOf(file))),
        )
        val first = ContentPaths.fingerprint(listOf(course))
        assertEquals(first, ContentPaths.fingerprint(listOf(course)))
        val changed = course.copy(
            sections = listOf(ContentSection(id = "s1", name = "Semana 1", files = listOf(file.copy(sizeBytes = 200)))),
        )
        assertTrue(first != ContentPaths.fingerprint(listOf(changed)))
    }

    @Test
    fun `auto sync respects the daily window`() {
        val now = 1_000_000L
        assertTrue(ContentPaths.shouldAutoSync(0L, "", "fp", now))
        assertFalse(ContentPaths.shouldAutoSync(now - 1_000L, "fp", "fp", now))
        assertTrue(ContentPaths.shouldAutoSync(now - 25 * 3_600_000L, "fp", "fp", now))
        assertTrue(ContentPaths.shouldAutoSync(now - 1_000L, "old", "fp", now))
    }
}
