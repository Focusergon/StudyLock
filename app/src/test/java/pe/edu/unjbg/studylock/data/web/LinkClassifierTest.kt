package pe.edu.unjbg.studylock.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkClassifierTest {

    @Test
    fun `detecta enlaces de archivo`() {
        assertTrue(LinkClassifier.looksLikeFile("https://aula.unjbg.edu.pe/a/silabo.pdf"))
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

    @Test
    fun `plan de estudios no cuenta como silabo`() {
        val link = WebLink(
            "Plan de estudios",
            "https://intranet.unjbg.edu.pe/alumno/malla-curricular",
        )

        assertFalse(LinkClassifier.isSyllabusLink(link.text, link.href))
        val verdict = LinkClassifier.heuristicVerdict(link, LinkPortal.INTRANET)
        assertEquals(LinkRole.OTRO, verdict.role)
        assertTrue(listOf(verdict).acceptedByDefault().isEmpty())
    }

    @Test
    fun `reconoce cursos reales del aula pero no el menu de cursos`() {
        assertTrue(
            LinkClassifier.looksLikeCourse(
                "Cálculo I",
                "https://aula.unjbg.edu.pe/course/view.php?id=123",
            ),
        )
        assertTrue(
            LinkClassifier.looksLikeCourse(
                "Redes",
                "https://aula.unjbg.edu.pe/cursos/2026-I/redes",
            ),
        )
        assertFalse(
            LinkClassifier.looksLikeCourse(
                "Mis cursos",
                "https://aula.unjbg.edu.pe/mis-cursos",
            ),
        )
        assertFalse(
            LinkClassifier.looksLikeCourse(
                "Mis cursos",
                "https://aula.unjbg.edu.pe/cursos",
            ),
        )
    }

    @Test
    fun `usa el contexto para nombrar un curso sin texto`() {
        val url = "https://aula.unjbg.edu.pe/course/view.php?id=25474"
        assertEquals(
            "Ingeniería en Informática y Sistemas 2026-II",
            LinkClassifier.displayLabel(
                "",
                url,
                "Ingeniería en Informática y Sistemas 2026-II Ver curso",
            ),
        )
        assertEquals("view.php", LinkClassifier.displayLabel("", url, ""))
        assertEquals(
            "Ingeniería en Informática y Sistemas 26-II",
            LinkClassifier.displayLabel(
                "",
                url,
                "Ingeniería en Informática y Sistemas 26-II ESIS INGENIERÍA W Ver curso",
            ),
        )
    }

    @Test
    fun `el nombre del curso no es el de la carrera`() {
        val url = "https://aula.unjbg.edu.pe/course/view.php?id=25474"
        val full = "Ingeniería en Informática y Sistemas 26-II ESIS " +
            "INGENIERÍA WEB Y APLICACIONES MÓVILES-A F2 Acceso"
        assertEquals(
            "INGENIERÍA WEB Y APLICACIONES MÓVILES-A",
            LinkClassifier.courseLabel("", url, full),
        )
        assertEquals(
            "SEGURIDAD INFORMÁTICA-B",
            LinkClassifier.courseLabel(
                "26-II ESIS SEGURIDAD INFORMÁTICA-B F2",
                url,
                "",
            ),
        )
        assertEquals(
            "INGENIERÍA WEB Y APLICACIONES MÓVILES-A",
            LinkClassifier.courseLabel("Acceso", url, full),
        )
    }

    @Test
    fun `no salta a otro curso ni a paginas genericas`() {
        val base = "https://aula.unjbg.edu.pe/course/view.php?id=25474"
        assertFalse(
            LinkClassifier.isWithin(
                "https://aula.unjbg.edu.pe/course/view.php?id=25448",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isWithin(
                "https://aula.unjbg.edu.pe/course/index.php?categoryid=894",
                base,
            ),
        )
        assertFalse(LinkClassifier.isWithin("https://aula.unjbg.edu.pe/", base))
        assertTrue(
            LinkClassifier.isWithin(
                "https://aula.unjbg.edu.pe/mod/resource/view.php?id=12345",
                base,
            ),
        )
    }

    @Test
    fun `no recorre horarios ni listados del campus`() {
        val base = "https://aula.unjbg.edu.pe/course/view.php?id=25474"
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "HORARIOS DE CLASE 2026-II",
                "https://aula.unjbg.edu.pe/mod/page/view.php?id=359254",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "Ingeniería en Informática y Sistemas",
                "https://aula.unjbg.edu.pe/course/index.php?categoryid=894",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "AULA VIRTUAL UNJBG",
                "https://aula.unjbg.edu.pe/",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "Foro",
                "https://aula.unjbg.edu.pe/mod/forum/view.php?id=2&lang=en",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "Foro",
                "https://aula.unjbg.edu.pe/mod/forum/view.php?id=2",
                base,
            ),
        )
        assertFalse(
            LinkClassifier.isCrawlableLink(
                "Suscribirse",
                "https://aula.unjbg.edu.pe/mod/forum/subscribe.php?id=2&sesskey=abc",
                base,
            ),
        )
    }

    @Test
    fun `una categoria del aula no es un curso`() {
        assertFalse(
            LinkClassifier.looksLikeCourse(
                "Ingeniería en Informática y Sistemas",
                "https://aula.unjbg.edu.pe/course/index.php?categoryid=894",
            ),
        )
    }
}
