package pe.edu.unjbg.studylock.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkClassifierTest {

    @Test
    fun `detecta enlaces de archivo`() {
        assertTrue(LinkClassifier.looksLikeFile("https://aulavirtual.unjbg.edu.pe/a/silabo.pdf"))
        assertTrue(LinkClassifier.looksLikeFile("https://x.edu.pe/archivos/123"))
        assertTrue(LinkClassifier.looksLikeFile("https://x.edu.pe/files/guia.docx"))
        assertFalse(LinkClassifier.looksLikeFile("https://x.edu.pe/curso/12"))
    }

    @Test
    fun `solo texto y pdf son importables`() {
        assertTrue(LinkClassifier.isImportable("silabo.pdf"))
        assertTrue(LinkClassifier.isImportable("apuntes.txt?download=1"))
        assertTrue(LinkClassifier.isImportable("guia.docx"))
        assertFalse(LinkClassifier.isImportable("video.mp4"))
        assertFalse(LinkClassifier.isImportable("presentacion.pptx"))
    }

    @Test
    fun `distingue cursos de silabos y archivos`() {
        assertTrue(LinkClassifier.looksLikeCourse("Base de Datos", "https://x.edu.pe/curso/12"))
        assertTrue(LinkClassifier.looksLikeCourse("Curso", "https://x.edu.pe/aula/courseid=7"))
        assertFalse(LinkClassifier.looksLikeCourse("Sílabo de Redes", "https://x.edu.pe/silabo/3"))
        assertFalse(LinkClassifier.looksLikeCourse("Guía", "https://x.edu.pe/archivos/9.pdf"))
    }

    @Test
    fun `reconoce enlaces de silabo por texto o url`() {
        assertTrue(LinkClassifier.isSyllabusLink("Sílabos 2026", "https://x.edu.pe/panel"))
        assertTrue(LinkClassifier.isSyllabusLink("Ver", "https://x.edu.pe/silabo/2026-I"))
        assertFalse(LinkClassifier.isSyllabusLink("Mis cursos", "https://x.edu.pe/curso/1"))
    }

    @Test
    fun `reconoce secciones de curso`() {
        assertTrue(LinkClassifier.looksLikeSection("Unidad 1"))
        assertTrue(LinkClassifier.looksLikeSection("Semana 3 - Práctica"))
        assertTrue(LinkClassifier.looksLikeSection("Recursos del tema"))
        assertFalse(LinkClassifier.looksLikeSection("Inicio"))
    }

    @Test
    fun `extrae el nombre del archivo de la url`() {
        assertEquals("Mi Silabo.pdf", LinkClassifier.fileNameFromUrl("https://x.edu.pe/a/Mi%20Silabo.pdf?v=2"))
        assertEquals("guia.pdf", LinkClassifier.fileNameFromUrl("https://x.edu.pe/a/guia.pdf#page=2"))
        assertEquals("Documento", LinkClassifier.fileNameFromUrl(""))
    }

    @Test
    fun `asigna el mime segun la extension`() {
        assertEquals("application/pdf", LinkClassifier.mimeFor("silabo.pdf"))
        assertEquals("text/plain", LinkClassifier.mimeFor("notas.txt"))
        assertEquals("", LinkClassifier.mimeFor("video.mp4"))
    }

    @Test
    fun `normaliza texto sin tildes ni espacios extra`() {
        assertEquals("silabo de redes", LinkClassifier.normalize("Sílabo   de   Redes"))
    }

    @Test
    fun `compara hosts y prefijos de url`() {
        assertTrue(LinkClassifier.sameHost("https://x.edu.pe/a", "https://x.edu.pe/b"))
        assertFalse(LinkClassifier.sameHost("https://x.edu.pe/a", "https://y.edu.pe/b"))
        assertTrue(LinkClassifier.isWithin("https://x.edu.pe/curso/1/unidad/2", "https://x.edu.pe/curso/1"))
        assertFalse(LinkClassifier.isWithin("https://x.edu.pe/curso/9", "https://x.edu.pe/curso/1"))
    }

    @Test
    fun `no sigue enlaces de menu ni externos`() {
        val base = "https://x.edu.pe/curso/1"
        assertTrue(LinkClassifier.isCrawlableLink("Unidad 1", "https://x.edu.pe/curso/1/unidad/1", base))
        assertFalse(LinkClassifier.isCrawlableLink("Cerrar sesión", "https://x.edu.pe/logout", base))
        assertFalse(LinkClassifier.isCrawlableLink("Guía", "https://x.edu.pe/curso/1/guia.pdf", base))
        assertFalse(LinkClassifier.isCrawlableLink("Otro sitio", "https://otro.com/curso/1", base))
    }
}
