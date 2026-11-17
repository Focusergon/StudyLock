package pe.edu.unjbg.studylock.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnjbgCredentialsTest {

    @Test
    fun `con la misma contrasena reutiliza la del aula`() {
        val credentials = UnjbgCredentials(
            aulaUser = "2026-171190",
            aulaPassword = "secreta",
            samePassword = true,
        )
        assertEquals("2026-171190", credentials.effectiveIntranetUser)
        assertEquals("secreta", credentials.effectiveIntranetPassword)
        assertTrue(credentials.hasAula)
        assertTrue(credentials.hasIntranet)
    }

    @Test
    fun `con contrasena distinta usa la de la intranet`() {
        val credentials = UnjbgCredentials(
            aulaUser = "aula-user",
            aulaPassword = "aula-pass",
            intranetUser = "intra-user",
            intranetPassword = "intra-pass",
            samePassword = false,
        )
        assertEquals("intra-user", credentials.effectiveIntranetUser)
        assertEquals("intra-pass", credentials.effectiveIntranetPassword)
    }

    @Test
    fun `el usuario de la intranet cae al del aula si esta vacio`() {
        val credentials = UnjbgCredentials(
            aulaUser = "2026-171190",
            aulaPassword = "aula-pass",
            intranetUser = "",
            intranetPassword = "intra-pass",
            samePassword = false,
        )
        assertEquals("2026-171190", credentials.effectiveIntranetUser)
        assertEquals("intra-pass", credentials.effectiveIntranetPassword)
    }

    @Test
    fun `sin contrasena no hay cuentas disponibles`() {
        val credentials = UnjbgCredentials(aulaUser = "2026-171190", aulaPassword = "")
        assertFalse(credentials.hasAula)
        assertFalse(credentials.hasIntranet)
        assertFalse(credentials.hasAny)
    }
}
