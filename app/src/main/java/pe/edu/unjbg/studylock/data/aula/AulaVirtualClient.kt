package pe.edu.unjbg.studylock.data.aula

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Error legible del Aula Virtual, listo para mostrarse al estudiante. */
class AulaVirtualException(message: String) : IOException(message)

/** Información de la cuenta vinculada en el Aula Virtual. */
data class AulaSite(
    val siteName: String,
    val userName: String,
    val userId: Int,
)

/** Curso matriculado en el Aula Virtual. */
data class AulaCourse(
    val id: Int,
    val fullName: String,
    val shortName: String,
)

/** Archivo descargable dentro de una sección del curso. */
data class AulaFile(
    val name: String,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long,
)

/** Sección del curso (semana o unidad) con sus archivos importables. */
data class AulaSection(
    val name: String,
    val files: List<AulaFile>,
)

/**
 * Conector del Aula Virtual UNJBG (Moodle Web Services).
 *
 * Usa el **token de servicio web móvil** que el propio estudiante genera en su perfil de Moodle
 * (Preferencias → Seguridad → Claves → «Moodle mobile web service»). De esta forma la app nunca
 * ve ni guarda la contraseña institucional, cumpliendo el requisito de privacidad del proyecto.
 *
 * Solo se implementan las funciones de lectura necesarias:
 *  - `core_webservice_get_site_info` (validar el token)
 *  - `core_enrol_get_users_courses` (cursos matriculados)
 *  - `core_course_get_contents` (secciones y archivos)
 */
class AulaVirtualClient(baseUrl: String, private val token: String) {

    val baseUrl: String = normalize(baseUrl)
    private val endpoint: String get() = "$baseUrl/webservices/rest/server.php"

    /** Valida el token y devuelve la cuenta vinculada. Lanza [AulaVirtualException] si falla. */
    fun validate(): AulaSite {
        val json = callObject(FUNCTION_SITE_INFO)
        return AulaSite(
            siteName = json.optString("sitename"),
            userName = json.optString("fullname").ifBlank { json.optString("username") },
            userId = json.optInt("userid"),
        )
    }

    /** Cursos en los que el estudiante está matriculado. */
    fun courses(): List<AulaCourse> {
        val site = validate()
        val array = callArray(FUNCTION_USER_COURSES, listOf("userid" to site.userId.toString()))
        return array.mapObjects { obj ->
            AulaCourse(
                id = obj.optInt("id"),
                fullName = obj.optString("fullname"),
                shortName = obj.optString("shortname"),
            )
        }.filter { it.id > 0 && it.fullName.isNotBlank() }
    }

    /** Secciones del curso con los archivos que se pueden convertir en preguntas. */
    fun sections(courseId: Int): List<AulaSection> {
        val array = callArray(FUNCTION_COURSE_CONTENTS, listOf("courseid" to courseId.toString()))
        return array.mapObjects { section ->
            val name = section.optString("name").ifBlank { section.optString("summary").stripHtml() }
            val files = ArrayList<AulaFile>()
            val modules = section.optJSONArray("modules") ?: JSONArray()
            for (module in modules.objects()) {
                val contents = module.optJSONArray("contents") ?: continue
                for (content in contents.objects()) {
                    val url = content.optString("fileurl")
                    val fileName = content.optString("filename")
                    val mime = content.optString("mimetype")
                    if (url.isBlank() || !isImportable(mime, fileName)) continue
                    files += AulaFile(
                        name = fileName.ifBlank { "Documento" },
                        url = url,
                        mimeType = mime,
                        sizeBytes = content.optLong("filesize"),
                    )
                }
            }
            AulaSection(name = name.ifBlank { "Sección" }, files = files)
        }.filter { it.files.isNotEmpty() }
    }

    /** Descarga un archivo del curso. La URL de Moodle ya puede traer el token; si no, se añade. */
    fun download(url: String): ByteArray {
        val connection = open(ensureToken(url))
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw AulaVirtualException("No se pudo descargar el archivo (HTTP $code).")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    // --- Transporte ---------------------------------------------------------

    private fun callObject(function: String, params: List<Pair<String, String>> = emptyList()): JSONObject {
        val body = rawCall(function, params)
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) return JSONObject()
        val json = JSONObject(trimmed)
        failIfError(json)
        return json
    }

    private fun callArray(function: String, params: List<Pair<String, String>> = emptyList()): JSONArray {
        val trimmed = rawCall(function, params).trim()
        if (trimmed.startsWith("{")) {
            failIfError(JSONObject(trimmed))
            return JSONArray()
        }
        return JSONArray(trimmed)
    }

    private fun failIfError(json: JSONObject) {
        if (json.has("exception")) {
            val message = json.optString("message").ifBlank { json.optString("error") }
            throw AulaVirtualException(message.ifBlank { "El Aula Virtual devolvió un error." })
        }
    }

    private fun rawCall(function: String, params: List<Pair<String, String>>): String {
        val payload = buildString {
            append("wstoken=").append(encode(token))
            append("&wsfunction=").append(encode(function))
            append("&moodlewsrestformat=json")
            params.forEach { (key, value) ->
                append('&').append(encode(key)).append('=').append(encode(value))
            }
        }.toByteArray(Charsets.UTF_8)

        val connection = open(endpoint).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) {
                throw AulaVirtualException("El Aula Virtual respondió HTTP $code. Verifica la dirección.")
            }
            if (text.isBlank()) throw AulaVirtualException("Respuesta vacía del Aula Virtual.")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }

    private fun ensureToken(url: String): String =
        if (url.contains("token=")) url else url + (if (url.contains('?')) "&" else "?") + "token=$token"

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val FUNCTION_SITE_INFO = "core_webservice_get_site_info"
        const val FUNCTION_USER_COURSES = "core_enrol_get_users_courses"
        const val FUNCTION_COURSE_CONTENTS = "core_course_get_contents"

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

        fun normalize(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) throw AulaVirtualException("Escribe la dirección del Aula Virtual.")
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
            url = url.substringBefore("/webservices/").trimEnd('/')
            return url
        }

        fun isImportable(mime: String, name: String): Boolean {
            val lower = name.lowercase()
            return mime.startsWith("text/") ||
                mime.equals("application/pdf", ignoreCase = true) ||
                lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".md")
        }
    }
}

// --- Utilidades JSON --------------------------------------------------------

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        out += transform(item)
    }
    return out
}

private fun JSONArray.objects(): List<JSONObject> = mapObjects { it }

/** Elimina etiquetas HTML básicas del resumen de un curso o sección. */
internal fun String.stripHtml(): String =
    replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
