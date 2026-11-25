package pe.edu.unjbg.studylock.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun `restricted permission is the first prerequisite`() {
        val status = PermissionStatus(
            restricted = false,
            usage = true,
            overlay = true,
            accessibility = true,
        )
        assertFalse(status.allGranted)
        assertEquals(3, status.grantedCount)
        assertEquals(4, status.total)
    }

    @Test
    fun `all four steps are required to continue`() {
        val status = PermissionStatus(
            restricted = true,
            usage = true,
            overlay = true,
            accessibility = true,
        )
        assertTrue(status.allGranted)
        assertEquals(4, status.grantedCount)
    }
}
