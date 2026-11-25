package pe.edu.unjbg.studylock.data.content

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pe.edu.unjbg.studylock.core.text.PdfTextExtractor
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import java.io.File
import java.security.MessageDigest

/**
 * Almacén físico del material académico.
 *
 * Guarda las copias reales de los PDFs (no solo URIs `content://`) en:
 * ```
 * filesDir/unjbg/cursos/<curso>/<sección>/archivo.pdf
 * filesDir/unjbg/agregados/<categoría>/archivo.pdf
 * filesDir/unjbg/manifest.json
 * ```
 * Cada archivo lleva su SHA-256 en el manifiesto, lo que permite sincronización incremental: si el
 * nombre, la URL y el tamaño no cambiaron, no se vuelve a descargar.
 */
class ContentStore(
    private val root: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    constructor(context: Context) : this(File(context.filesDir, "unjbg"))

    data class StoredFile(
        val name: String,
        val relativePath: String,
        val sha256: String,
        val sizeBytes: Long,
        val sourceUrl: String,
        val category: String = "",
        val suggestedCourse: String = "",
    )

    data class StoredSection(val name: String, val files: List<StoredFile>)

    data class StoredCourse(
        val id: String,
        val name: String,
        val source: String,
        val sections: List<StoredSection>,
    )

    data class Manifest(
        val version: Int = 1,
        val savedAt: Long = 0L,
        val lastSyncAt: Long = 0L,
        val fingerprint: String = "",
        val courses: List<StoredCourse> = emptyList(),
        val added: List<StoredFile> = emptyList(),
    )

    val manifestFile: File get() = File(root, "manifest.json")
    val cursosDir: File get() = File(root, "cursos")
    val agregadosDir: File get() = File(root, "agregados")

    init {
        cursosDir.mkdirs()
        agregadosDir.mkdirs()
    }

    suspend fun manifest(): Manifest = withContext(io) {
        if (!manifestFile.exists()) return@withContext Manifest()
        runCatching {
            val json = JSONObject(manifestFile.readText())
            Manifest(
                version = json.optInt("version", 1),
                savedAt = json.optLong("savedAt"),
                lastSyncAt = json.optLong("lastSyncAt"),
                fingerprint = json.optString("fingerprint"),
                courses = json.optJSONArray("courses").objects().map(::courseFrom),
                added = json.optJSONArray("added").objects().map(::fileFrom),
            )
        }.getOrDefault(Manifest())
    }

    suspend fun saveManifest(manifest: Manifest) = withContext(io) {
        val json = JSONObject().apply {
            put("version", manifest.version)
            put("savedAt", System.currentTimeMillis())
            put("lastSyncAt", manifest.lastSyncAt)
            put("fingerprint", manifest.fingerprint)
            put("courses", JSONArray(manifest.courses.map(::courseToJson)))
            put("added", JSONArray(manifest.added.map(::fileToJson)))
        }
        val temp = File(root, "manifest.json.tmp")
        temp.writeText(json.toString())
        if (manifestFile.exists()) manifestFile.delete()
        temp.renameTo(manifestFile)
    }

    /** Guarda un archivo descargado del Aula/Intranet en su carpeta de curso y sección. */
    suspend fun storeDownloaded(
        course: ContentCourse,
        sectionName: String,
        file: ContentFile,
        bytes: ByteArray,
    ): StoredFile? = withContext(io) {
        val relative = "${ContentPaths.DIR_CURSOS}/${ContentPaths.sanitize(course.name)}/" +
            "${ContentPaths.sanitize(sectionName)}/${ContentPaths.sanitize(file.name)}"
        write(relative, bytes, file.url, category = "", suggestedCourse = "")
    }

    /** Guarda un archivo subido dentro de la carpeta del curso confirmado por el estudiante. */
    suspend fun storeForCourse(
        courseName: String,
        name: String,
        bytes: ByteArray,
    ): StoredFile? = withContext(io) {
        val relative = "${ContentPaths.DIR_CURSOS}/${ContentPaths.sanitize(courseName)}/" +
            "Subidos/${ContentPaths.sanitize(name)}"
        write(relative, bytes, "", category = "Subido", suggestedCourse = courseName)
    }

    /** Guarda un archivo que el estudiante subió o un sílabo sin curso en `agregados/`. */
    suspend fun storeAdded(
        name: String,
        bytes: ByteArray,
        category: String = "General",
        suggestedCourse: String = "",
        sourceUrl: String = "",
    ): StoredFile? = withContext(io) {
        val safeCategory = ContentPaths.sanitize(category.ifBlank { "General" })
        val relative = "${ContentPaths.DIR_AGREGADOS}/$safeCategory/${ContentPaths.sanitize(name)}"
        write(relative, bytes, sourceUrl, category, suggestedCourse)
    }

    /** Archivo físico ya guardado (para reutilizarlo sin volver a descargar). */
    fun physicalFile(relativePath: String): File = File(root, relativePath)

    /** Lee el texto de una copia física (PDF o texto plano). */
    suspend fun readText(file: StoredFile): String? = withContext(io) {
        val path = File(root, file.relativePath)
        if (!path.exists()) return@withContext null
        val bytes = path.readBytes()
        if (bytes.isEmpty()) return@withContext null
        if (PdfTextExtractor.looksLikePdf(bytes)) PdfTextExtractor.extract(bytes)
        else String(bytes, Charsets.UTF_8)
    }

    /** ¿Hay que volver a descargar este archivo? (sincronización incremental). */
    suspend fun needsDownload(file: ContentFile, courseId: String): Boolean = withContext(io) {
        val stored = manifest().courses.firstOrNull { it.id == courseId }
            ?.sections?.flatMap { it.files }
            ?.firstOrNull { it.sourceUrl == file.url }
        ContentPaths.needsDownload(file, stored?.sha256, stored?.sizeBytes)
    }

    private fun write(
        relativePath: String,
        bytes: ByteArray,
        sourceUrl: String,
        category: String,
        suggestedCourse: String,
    ): StoredFile? {
        if (bytes.isEmpty()) return null
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeBytes(bytes)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) return null
        return StoredFile(
            name = target.name,
            relativePath = relativePath.replace('\\', '/'),
            sha256 = ContentPaths.sha256(bytes),
            sizeBytes = bytes.size.toLong(),
            sourceUrl = sourceUrl,
            category = category,
            suggestedCourse = suggestedCourse,
        )
    }

    private fun courseFrom(json: JSONObject): StoredCourse = StoredCourse(
        id = json.optString("id"),
        name = json.optString("name"),
        source = json.optString("source"),
        sections = json.optJSONArray("sections").objects().map { obj ->
            StoredSection(
                name = obj.optString("name"),
                files = obj.optJSONArray("files").objects().map(::fileFrom),
            )
        },
    )

    private fun courseToJson(course: StoredCourse) = JSONObject().apply {
        put("id", course.id)
        put("name", course.name)
        put("source", course.source)
        put("sections", JSONArray(course.sections.map { section ->
            JSONObject().apply {
                put("name", section.name)
                put("files", JSONArray(section.files.map(::fileToJson)))
            }
        }))
    }

    private fun fileFrom(json: JSONObject): StoredFile {
        return StoredFile(
            name = json.optString("name"),
            relativePath = json.optString("relativePath"),
            sha256 = json.optString("sha256"),
            sizeBytes = json.optLong("sizeBytes"),
            sourceUrl = json.optString("sourceUrl"),
            category = json.optString("category"),
            suggestedCourse = json.optString("suggestedCourse"),
        )
    }

    private fun fileToJson(file: StoredFile) = JSONObject().apply {
        put("name", file.name)
        put("relativePath", file.relativePath)
        put("sha256", file.sha256)
        put("sizeBytes", file.sizeBytes)
        put("sourceUrl", file.sourceUrl)
        put("category", file.category)
        put("suggestedCourse", file.suggestedCourse)
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
}

/** Funciones puras del almacén: se prueban en JVM sin Android. */
object ContentPaths {
    const val DIR_CURSOS = "cursos"
    const val DIR_AGREGADOS = "agregados"

    /** Nombre de archivo/carpeta seguro, sin caracteres que rompan rutas en Android/Windows. */
    fun sanitize(value: String): String = value.trim()
        .replace('\\', '_')
        .replace('/', '_')
        .replace(Regex("[<>:\"|?*\\u0000-\\u001F]"), "_")
        .replace(Regex("\\s+"), " ")
        .take(96)
        .trim('.')
        .ifBlank { "sin-nombre" }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Huella del árbol remoto: sirve para detectar cambios sin volver a descargar todo. */
    fun fingerprint(courses: List<ContentCourse>): String {
        val raw = courses.sortedBy { it.id }.joinToString("\n") { course ->
            "${course.id}|${course.name}|" + course.sections.sortedBy { it.id }.joinToString(";") { section ->
                "${section.id}|${section.name}|" + section.files.sortedBy { it.url }.joinToString(",") { file ->
                    "${file.url}:${file.sizeBytes}"
                }
            }
        }
        return sha256(raw.toByteArray(Charsets.UTF_8))
    }

    /**
     * Incremental: si ya guardamos el archivo con la misma URL y tamaño, no se descarga otra vez.
     * El SHA-256 se recalcula al guardar para detectar cambios de contenido con el mismo tamaño.
     */
    fun needsDownload(file: ContentFile, storedSha: String?, storedSize: Long?): Boolean {
        if (storedSha.isNullOrBlank() || storedSize == null) return true
        return storedSize != file.sizeBytes || file.url.isBlank()
    }

    /** Una sincronización automática al día como máximo. */
    fun shouldAutoSync(lastSyncAt: Long, fingerprint: String, currentFingerprint: String, now: Long): Boolean {
        if (lastSyncAt <= 0L) return true
        if (fingerprint != currentFingerprint) return true
        return now - lastSyncAt >= 24 * 60 * 60 * 1000L
    }
}
