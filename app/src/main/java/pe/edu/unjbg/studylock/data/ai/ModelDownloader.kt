package pe.edu.unjbg.studylock.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.data.network.NetworkStatus
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Modelo pequeño, fijo y verificado: la app no genera ni pide claves de API. */
object AiModel {
    const val FILE_NAME = "gemma3-270m-it-q8.litertlm"
    const val DOWNLOAD_URL =
        "https://github.com/Focusergon/StudyLock/releases/download/model-gemma3-270m/$FILE_NAME"
    const val SHA256 = "757e9119fa5bd667a2774fb470ac4afcd3190a21c677f8e69a5d6bc908abdd63"
    const val EXPECTED_BYTES = 304_005_120L
}

enum class ModelStatus { MISSING, WAITING_WIFI, DOWNLOADING, READY, ERROR }

data class ModelState(
    val status: ModelStatus = ModelStatus.MISSING,
    val progress: Int = 0,
    val message: String = "",
)

/**
 * Descarga el modelo en streaming y solo lo acepta si coincide la longitud y el SHA-256.
 *
 * El binario no se versiona: se publica en un GitHub Release y se guarda en el almacenamiento
 * privado de la app. El archivo `.sha256` es un sidecar local que evita recalcular el hash en cada
 * arranque; la verificación real siempre se hace sobre los bytes descargados.
 */
class ModelDownloader(
    context: Context,
    private val network: NetworkStatus = NetworkStatus(context),
    private val baseDir: File = File(context.filesDir, "ai"),
) {
    private val modelFile = File(baseDir, AiModel.FILE_NAME)
    private val partFile = File(baseDir, "${AiModel.FILE_NAME}.part")
    private val sidecar = File(baseDir, "${AiModel.FILE_NAME}.sha256")

    init {
        baseDir.mkdirs()
    }

    fun state(): ModelState {
        val file = verifiedFile() ?: return ModelState(ModelStatus.MISSING)
        return ModelState(ModelStatus.READY, 100, "${file.length() / 1_048_576L} MB verificados")
    }

    /** Solo devuelve la ruta cuando el modelo pasó la verificación. */
    fun verifiedFile(): File? {
        if (!modelFile.exists() || modelFile.length() != AiModel.EXPECTED_BYTES) return null
        if (!sidecar.exists()) {
            // Descarga heredada de una versión anterior: se comprueba de verdad antes de fiarse.
            if (!hashMatches(modelFile)) return null
            writeSidecar()
        }
        return modelFile.takeIf { sidecarHasExpectedHash() }
    }

    suspend fun download(
        wifiOnly: Boolean = true,
        onProgress: (Int) -> Unit = {},
    ): ModelState = withContext(Dispatchers.IO) {
        verifiedFile()?.let {
            return@withContext ModelState(ModelStatus.READY, 100, "El modelo ya está listo.")
        }
        if (wifiOnly && !network.isWifi()) {
            return@withContext ModelState(
                ModelStatus.WAITING_WIFI,
                0,
                "Conéctate a Wi-Fi para descargar el modelo (~290 MB).",
            )
        }
        if (!network.isConnected()) {
            return@withContext ModelState(ModelStatus.ERROR, 0, "No hay conexión a internet.")
        }

        var connection: HttpURLConnection? = null
        try {
            connection = URL(AiModel.DOWNLOAD_URL).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            if (connection.responseCode !in 200..299) {
                return@withContext ModelState(
                    ModelStatus.ERROR,
                    0,
                    "No se pudo descargar (HTTP ${connection.responseCode}). " +
                        "Comprueba que el Release del modelo esté publicado.",
                )
            }

            val total = connection.contentLength.takeIf { it > 0 }?.toLong() ?: AiModel.EXPECTED_BYTES
            val digest = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                RandomAccessFile(partFile, "rw").use { output ->
                    output.setLength(0)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        bytes += read
                        val percent = ((bytes * 100) / total).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }

            if (bytes != AiModel.EXPECTED_BYTES) {
                partFile.delete()
                return@withContext ModelState(
                    ModelStatus.ERROR,
                    0,
                    "La descarga está incompleta (${bytes / 1_048_576L} MB). Inténtalo de nuevo.",
                )
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(AiModel.SHA256, ignoreCase = true)) {
                partFile.delete()
                return@withContext ModelState(
                    ModelStatus.ERROR,
                    0,
                    "El archivo no pasó la verificación SHA-256. No se guardó.",
                )
            }

            if (!partFile.renameTo(modelFile)) {
                modelFile.delete()
                if (!partFile.renameTo(modelFile)) {
                    partFile.delete()
                    return@withContext ModelState(ModelStatus.ERROR, 0, "No se pudo guardar el modelo.")
                }
            }
            writeSidecar(hash)
            onProgress(100)
            ModelState(ModelStatus.READY, 100, "Modelo listo para generar preguntas sin internet.")
        } catch (error: Exception) {
            ensureActive()
            ModelState(ModelStatus.ERROR, 0, "No se pudo descargar: ${error.message ?: "error desconocido"}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun hashMatches(file: File): Boolean = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
            .equals(AiModel.SHA256, ignoreCase = true)
    }.getOrDefault(false)

    private fun writeSidecar(hash: String = AiModel.SHA256) {
        sidecar.writeText("${AiModel.FILE_NAME}  ${hash.lowercase()}\n")
    }

    private fun sidecarHasExpectedHash(): Boolean =
        runCatching { sidecar.readText().contains(AiModel.SHA256, ignoreCase = true) }
            .getOrDefault(false)
}
