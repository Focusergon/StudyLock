package pe.edu.unjbg.studylock.core.text

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTextExtractorTest {

    private val content = "BT /F1 12 Tf (Hola mundo de prueba academica) Tj ET"

    @Test
    fun `detects pdf magic header`() {
        assertTrue(PdfTextExtractor.looksLikePdf("%PDF-1.7 contenido".toByteArray()))
        assertFalse(PdfTextExtractor.looksLikePdf("no soy un pdf".toByteArray()))
    }

    @Test
    fun `extracts text from an uncompressed stream`() {
        val pdf = buildPdf(content.toByteArray())
        val text = PdfTextExtractor.extract(pdf)
        assertTrue("esperaba el texto del stream, obtuve: $text", text.contains("Hola mundo de prueba academica"))
    }

    @Test
    fun `extracts text from a flate compressed stream`() {
        val pdf = buildPdf(deflate(content.toByteArray()))
        val text = PdfTextExtractor.extract(pdf)
        assertTrue("esperaba el texto comprimido, obtuve: $text", text.contains("Hola mundo de prueba academica"))
    }

    @Test
    fun `decodes escaped parentheses`() {
        val pdf = buildPdf("BT (Total \\(IVA\\) incluido) Tj ET".toByteArray())
        val text = PdfTextExtractor.extract(pdf)
        assertTrue("esperaba los paréntesis, obtuve: $text", text.contains("Total (IVA) incluido"))
    }

    @Test
    fun `returns empty for empty input`() {
        assertTrue(PdfTextExtractor.extract(ByteArray(0)).isEmpty())
    }

    private fun buildPdf(stream: ByteArray): ByteArray {
        val header = "%PDF-1.4\n1 0 obj<</Length ${stream.size}>>\nstream\n".toByteArray(Charsets.ISO_8859_1)
        val footer = "\nendstream\nendobj\n%%EOF".toByteArray(Charsets.ISO_8859_1)
        return ByteArrayOutputStream().apply {
            write(header)
            write(stream)
            write(footer)
        }.toByteArray()
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(256)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            out.write(buffer, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }
}
