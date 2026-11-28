package pe.edu.unjbg.studylock.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkVerdictParserTest {

    private val links = listOf(
        WebLink("Cálculo I", "https://aula.unjbg.edu.pe/curso/1"),
        WebLink("Inicio", "https://aula.unjbg.edu.pe/home"),
    )

    @Test
    fun `acepta una clasificacion completa y valida`() {
        val json = """{"links":[{"i":0,"k":"CURSO","why":"Entrada a una materia","c":90},""" +
            """{"i":1,"k":"OTRO","why":"Enlace del menú principal","c":95}]}"""
        val result = LinkVerdictParser.parse(json, links)

        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.verdicts.size)
        assertEquals(LinkRole.CURSO, result.verdicts[0].role)
        assertEquals(VerdictSource.IA, result.verdicts[0].source)
        assertEquals("Cálculo I", result.verdicts[0].label)
        assertEquals(LinkRole.OTRO, result.verdicts[1].role)
    }

    @Test
    fun `rechaza JSON lenient`() {
        val result = LinkVerdictParser.parse("{links: []}", links)
        assertTrue(result.verdicts.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `acepta menos entradas y las faltantes se completan con heuristica`() {
        val result = LinkVerdictParser.parse("""{"links":[{"i":0,"k":"CURSO","why":"Solo uno"}]}""", links)

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.verdicts.size)
        val fallback = links.map { LinkClassifier.heuristicVerdict(it, LinkPortal.AULA) }
        val merged = result.verdicts.mergeWithFallback(fallback)
        assertEquals(2, merged.size)
        assertEquals(VerdictSource.IA, merged[0].source)
        assertEquals(VerdictSource.HEURISTICA, merged[1].source)
    }

    @Test
    fun `rechaza mas entradas que enlaces`() {
        val result = LinkVerdictParser.parse(
            """{"links":[{"i":0,"k":"CURSO","why":"Uno"},""" +
                """{"i":1,"k":"OTRO","why":"Dos"},{"i":2,"k":"OTRO","why":"Tres"}]}""",
            links,
        )
        assertTrue(result.verdicts.isEmpty())
        assertTrue(result.errors.first().contains("como máximo 2"))
    }

    @Test
    fun `rechaza indice fuera de rango o repetido`() {
        val repeated = LinkVerdictParser.parse(
            """{"links":[{"i":0,"k":"CURSO","why":"Primero"},{"i":0,"k":"OTRO","why":"Repetido"}]}""",
            links,
        )
        assertTrue(repeated.verdicts.isEmpty())

        val outOfRange = LinkVerdictParser.parse(
            """{"links":[{"i":0,"k":"CURSO","why":"Primero"},{"i":9,"k":"OTRO","why":"Fuera"}]}""",
            links,
        )
        assertTrue(outOfRange.verdicts.isEmpty())
    }

    @Test
    fun `rechaza categoria desconocida y motivo vacio`() {
        val badRole = LinkVerdictParser.parse(
            """{"links":[{"i":0,"k":"CURSO","why":"Válido"},{"i":1,"k":"NOSE","why":"Otro"}]}""",
            links,
        )
        assertTrue(badRole.verdicts.isEmpty())

        val emptyReason = LinkVerdictParser.parse(
            """{"links":[{"i":0,"k":"CURSO","why":"   "},{"i":1,"k":"OTRO","why":"Válido"}]}""",
            links,
        )
        assertTrue(emptyReason.verdicts.isEmpty())
    }

    @Test
    fun `respaldo heuristico distingue curso y silabo`() {
        val course = LinkClassifier.heuristicVerdict(links[0], LinkPortal.AULA)
        val syllabus = LinkClassifier.heuristicVerdict(
            WebLink("Sílabo de Redes", "https://intranet.unjbg.edu.pe/silabo/3"),
            LinkPortal.INTRANET,
        )
        assertEquals(LinkRole.CURSO, course.role)
        assertEquals(VerdictSource.HEURISTICA, course.source)
        assertEquals(LinkRole.SILABO, syllabus.role)
        assertEquals(setOf(links[0].href, syllabus.href), listOf(course, syllabus).acceptedByDefault())
    }

    @Test
    fun `acepta array raiz con alias de campos`() {
        val json = """[{"index":0,"role":"course","reason":"Entrada a la materia","confidence":88},""" +
            """{"index":1,"role":"otro","reason":"Enlace del menú principal","confidence":91}]"""
        val result = LinkVerdictParser.parse(json, links)

        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.verdicts.size)
        assertEquals(LinkRole.CURSO, result.verdicts[0].role)
        assertEquals(88, result.verdicts[0].confidence)
        assertEquals(LinkRole.OTRO, result.verdicts[1].role)
    }

    @Test
    fun `acepta markdown con objeto envuelto y rol con tilde`() {
        val json = """
            ```json
            {"data":{"items":[
              {"i":0,"k":"SÍLABO","why":"Documento de silabo","c":90},
              {"i":1,"k":"OTRO","why":"Enlace del menú principal","c":95}
            ]}}
            ```
        """.trimIndent()
        val result = LinkVerdictParser.parse(json, links)

        assertTrue(result.errors.isEmpty())
        assertEquals(LinkRole.SILABO, result.verdicts[0].role)
        assertEquals(LinkRole.OTRO, result.verdicts[1].role)
    }

    @Test
    fun `informa el error reportado por el modelo`() {
        val result = LinkVerdictParser.parse("""{"error":"El modelo local falló: sin memoria"}""", links)

        assertTrue(result.verdicts.isEmpty())
        assertTrue(result.errors.first().contains("sin memoria"))
    }

    @Test
    fun `rechaza alias con indice repetido`() {
        val result = LinkVerdictParser.parse(
            """[{"index":0,"role":"CURSO","why":"Primero"},""" +
                """{"index":0,"role":"OTRO","why":"Repetido"}]""",
            links,
        )

        assertTrue(result.verdicts.isEmpty())
        assertTrue(result.errors.first().contains("repetido"))
    }

    @Test
    fun `descarta curso inventado por IA en enlace de menu`() {
        val invented = LinkVerdict(
            link = links[1],
            role = LinkRole.CURSO,
            reason = "Inventado por el modelo",
            source = VerdictSource.IA,
            confidence = 95,
        )
        val fallback = links.map { LinkClassifier.heuristicVerdict(it, LinkPortal.AULA) }

        val sanitized = listOf(invented).sanitizedBy(fallback)

        assertEquals(LinkRole.OTRO, sanitized.single().role)
        assertEquals(VerdictSource.HEURISTICA, sanitized.single().source)
        assertTrue(sanitized.acceptedByDefault().isEmpty())
    }
}
