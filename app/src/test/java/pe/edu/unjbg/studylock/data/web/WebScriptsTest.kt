package pe.edu.unjbg.studylock.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebScriptsTest {

    @Test
    fun `escapa comillas y saltos de linea`() {
        assertEquals("\"a\\\"b\"", WebScripts.jsString("a\"b"))
        assertEquals("\"line\\nbreak\"", WebScripts.jsString("line\nbreak"))
        assertEquals("\"back\\\\slash\"", WebScripts.jsString("back\\slash"))
    }

    @Test
    fun `el script de login incrusta las credenciales`() {
        val script = WebScripts.login("2026-171190", "p@ss\"x")
        assertTrue(script.contains("\"2026-171190\""))
        assertTrue(script.contains("\"p@ss\\\"x\""))
        assertTrue(script.contains("input[type=\"password\"]"))
    }
}
