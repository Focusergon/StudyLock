package pe.edu.unjbg.studylock.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.log.StudyLockLog
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
 * Descarga el modelo en streaming, reanuda el archivo `.part` si la descarga se cortó y solo lo
 * acepta si coincide la longitud y el SHA-256.
 *
 * El estado se expone como [StateFlow]: la pantalla de Ajustes, la sincronización automática y el
 * worker ven el mismo progreso aunque se navegue a otra ruta. El binario no se versiona: se publica
 * en un GitHub Release y se guarda en el almacenamiento privado de la app.
 */
class ModelDownloader(
    context: Context,
    private val network: NetworkStatus = NetworkStatus(context),
    private val baseDir: File = File(context.filesDir, "ai"),
) {
    private val modelFile = File(baseDir, AiModel.FILE_NAME)
    private val partFile = File(baseDir, "${AiModel.FILE_NAME}.part")
    private val sidecar = File(baseDir, "${AiModel.FILE_NAME}.sha256")

    private val _state = MutableStateFlow(ModelState())
    val stateFlow: StateFlow<ModelState> = _state.asStateFlow()

    init {
        baseDir.mkdirs()
        _state.value = diskState()
    }

    fun state(): ModelState = _state.value

    /** Recalcula el estado leyendo el disco, por ejemplo al volver de segundo plano. */
    fun refresh() {
        _state.value = diskState()
    }

    private fun diskState(): ModelState {
        val file = verifiedFile() ?: return ModelState(ModelStatus.MISSING)
        return ModelState(
            ModelStatus.READY,
            100,
            "${file.length() / 1_048_576L} MB verificados",
        )
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
        onProgress: suspend (Int) -> Unit = {},
    ): ModelState = withContext(Dispatchers.IO) {
        verifiedFile()?.let {
            val ready = ModelState(ModelStatus.READY, 100, "El modelo ya está listo.")
            _state.value = ready
            onProgress(100)
            return@withContext ready
        }
        if (!network.isConnected()) {
            return@withContext fail("No hay conexión a internet. Se reintentará automáticamente.")
        }
        if (wifiOnly && !network.isWifi()) {
            return@withContext ModelState(
                ModelStatus.WAITING_WIFI,
                0,
                "Conéctate a Wi-Fi para descargar el modelo (~290 MB).",
            ).also { _state.value = it }
        }

        _state.value = ModelState(ModelStatus.DOWNLOADING, 0, "Preparando la descarga del modelo…")
        var connection: HttpURLConnection? = null
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var offset = partFile.length().takeIf { it in 1 until AiModel.EXPECTED_BYTES } ?: 0L
            if (offset > 0) {
                StudyLockLog.log("Modelo", "Reanudando descarga desde ${offset / 1_048_576L} MB")
                hashPart(digest, partFile, offset)
            } else {
                truncatePart()
            }

            connection = URL(AiModel.DOWNLOAD_URL).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext fail("No se pudo descargar (HTTP $code). Se reintentará automáticamente.")
            }
            // Si el servidor ignoró Range, empezamos de cero; si respondió 206, continuamos.
            if (offset > 0 && code != HttpURLConnection.HTTP_PARTIAL) {
                StudyLockLog.log("Modelo", "El servidor ignoró la reanudación; se reinicia la descarga")
                offset = 0
                digest.reset()
                truncatePart()
            }

            var bytes = offset
            var lastPercent = -1
            connection.inputStream.use { input ->
                RandomAccessFile(partFile, "rw").use { output ->
                    output.seek(bytes)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        bytes += read
                        val percent = ((bytes * 100) / AiModel.EXPECTED_BYTES)
                            .toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            publish(percent, "Descargando modelo… $percent%")
                            onProgress(percent)
                        }
                    }
                }
            }

            if (bytes != AiModel.EXPECTED_BYTES) {
                keepForRetry("La descarga está incompleta (${bytes / 1_048_576L} MB).")
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(AiModel.SHA256, ignoreCase = true)) {
                partFile.delete()
                return@withContext fail("El archivo no pasó la verificación SHA-256. No se guardó.")
            }

            if (!partFile.renameTo(modelFile)) {
                modelFile.delete()
                if (!partFile.renameTo(modelFile)) return@withContext fail("No se pudo guardar el modelo.")
            }
            writeSidecar(hash)
            publish(100, "Modelo listo para clasificar enlaces y generar preguntas sin internet.")
            onProgress(100)
            StudyLockLog.log("Modelo", "Modelo verificado y guardado (${AiModel.EXPECTED_BYTES} bytes)")
            _state.value
        } catch (error: Exception) {
            ensureActive()
            fail("No se pudo descargar: ${error.message ?: "error desconocido"}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun publish(progress: Int, message: String) {
        _state.value = ModelState(ModelStatus.DOWNLOADING, progress, message)
    }

    private fun fail(message: String): ModelState {
        StudyLockLog.warning("Modelo", message)
        return ModelState(ModelStatus.ERROR, 0, message).also { _state.value = it }
    }

    /** Conserva el `.part` para que el siguiente intento no vuelva a empezar de cero. */
    private fun keepForRetry(message: String): Nothing {
        StudyLockLog.warning("Modelo", message)
        throw IllegalStateException(message)
    }

    private fun hashPart(digest: MessageDigest, file: File, length: Long) {
        var remaining = length
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size, remaining.toInt()))
                if (read < 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }
        if (remaining != 0L) {
            digest.reset()
            runCatching { RandomAccessFile(file, "rw").use { it.setLength(0) } }
        }
    }

    private fun truncatePart() {
        runCatching { RandomAccessFile(partFile, "rw").use { it.setLength(0) } }
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
