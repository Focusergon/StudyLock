package pe.edu.unjbg.studylock.core.text

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Extractor de texto de PDF sin dependencias externas.
 *
 * Es **best-effort**: recorre los flujos (`stream…endstream`), los descomprime con `Inflater`
 * (zlib o deflate crudo) y recoge los literales de texto `(…)` de los operadores `Tj`/`TJ`.
 * Funciona bien con PDFs de texto exportados desde Word/LaTeX (que es el caso típico de un
 * sílabo). Los PDFs con fuentes CID o escaneados no se pueden recuperar así: para esos casos
 * hace falta OCR o una librería completa.
 *
 * Se mantiene en Kotlin puro (sin Android) para poder probarlo en JVM.
 */
object PdfTextExtractor {

    fun looksLikePdf(bytes: ByteArray): Boolean =
        bytes.size >= 5 && String(bytes, 0, 5, Charsets.ISO_8859_1) == "%PDF-"

    fun extract(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val raw = String(bytes, Charsets.ISO_8859_1)
        val text = StringBuilder()
        var index = 0
        while (true) {
            val start = raw.indexOf(STREAM, index)
            if (start < 0) break
            var contentStart = start + STREAM.length
            if (contentStart < raw.length && raw[contentStart] == '\r') contentStart++
            if (contentStart < raw.length && raw[contentStart] == '\n') contentStart++
            val end = raw.indexOf(ENDSTREAM, contentStart)
            if (end < 0) break
            var contentEnd = end
            while (contentEnd > contentStart && (raw[contentEnd - 1] == '\n' || raw[contentEnd - 1] == '\r')) {
                contentEnd--
            }
            if (contentEnd > contentStart) {
                val block = raw.substring(contentStart, contentEnd).toByteArray(Charsets.ISO_8859_1)
                val extracted = extractText(decode(block))
                if (extracted.isNotBlank()) text.append(extracted).append(' ')
            }
            index = end + ENDSTREAM.length
        }
        return text.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun decode(data: ByteArray): String =
        inflate(data)?.let { String(it, Charsets.ISO_8859_1) } ?: String(data, Charsets.ISO_8859_1)

    private fun inflate(data: ByteArray): ByteArray? =
        inflateWith(data, raw = false) ?: inflateWith(data, raw = true)

    private fun inflateWith(data: ByteArray, raw: Boolean): ByteArray? = try {
        val inflater = Inflater(raw)
        inflater.setInput(data)
        val out = ByteArrayOutputStream(data.size * 4)
        val buffer = ByteArray(8192)
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        inflater.end()
        out.toByteArray().takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }

    /** Recoge los literales `(…)` preservando el orden; `]` separa celdas de un arreglo `TJ`. */
    private fun extractText(content: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < content.length) {
            when (content[i]) {
                '(' -> {
                    val sb = StringBuilder()
                    i++
                    var depth = 1
                    while (i < content.length && depth > 0) {
                        val ch = content[i]
                        when {
                            ch == '\\' -> {
                                if (i + 1 >= content.length) {
                                    i++
                                    continue
                                }
                                val next = content[i + 1]
                                when {
                                    next == 'n' -> { sb.append('\n'); i += 2 }
                                    next == 'r' -> { sb.append('\r'); i += 2 }
                                    next == 't' -> { sb.append('\t'); i += 2 }
                                    next == 'b' || next == 'f' -> { sb.append(' '); i += 2 }
                                    next == '(' || next == ')' || next == '\\' -> { sb.append(next); i += 2 }
                                    next in '0'..'7' -> {
                                        var oct = 0
                                        var k = i + 1
                                        var count = 0
                                        while (k < content.length && count < 3 && content[k] in '0'..'7') {
                                            oct = oct * 8 + (content[k] - '0')
                                            k++
                                            count++
                                        }
                                        sb.append(oct.toChar())
                                        i = k
                                    }
                                    else -> { sb.append(next); i += 2 }
                                }
                            }
                            ch == '(' -> { depth++; sb.append(ch); i++ }
                            ch == ')' -> { depth--; if (depth > 0) sb.append(ch); i++ }
                            else -> { sb.append(ch); i++ }
                        }
                    }
                    out.append(sb).append(' ')
                }
                ']' -> { out.append(' '); i++ }
                else -> i++
            }
        }
        return out.toString()
    }

    private const val STREAM = "stream"
    private const val ENDSTREAM = "endstream"
}
