package pe.edu.unjbg.studylock.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseSuggesterTest {

    private val courses = listOf("Base de Datos I", "Programación Web", "Física Cuántica")

    @Test
    fun `suggests the course matching the file name`() {
        assertEquals("Base de Datos I", CourseSuggester.suggest("silabo base de datos i.pdf", courses))
    }

    @Test
    fun `ignores the extension`() {
        assertEquals("Programación Web", CourseSuggester.suggest("programacion web tema 3.docx", courses))
    }

    @Test
    fun `no match returns null so the file goes to agregados`() {
        assertNull(CourseSuggester.suggest("receta de cocina.pdf", courses))
    }

    @Test
    fun `empty course list returns null`() {
        assertNull(CourseSuggester.suggest("base de datos.pdf", emptyList()))
    }
}
