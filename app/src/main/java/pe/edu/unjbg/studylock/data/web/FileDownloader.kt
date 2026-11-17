package pe.edu.unjbg.studylock.data.web

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga archivos del campus usando la sesión ya abierta por el mini-navegador.
 *
 * Se reutilizan las cookies de `CookieManager` (misma sesión que el navegador) para que el servidor
 * entregue el documento sin pedir credenciales otra vez.
 */
class FileDownloader(private val io: CoroutineDispatcher = Dispatchers.IO) {

    suspend fun download(url: String, cookie: String): ByteArray? = withContext(io) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/pdf,text/plain,*/*")
                if (cookie.isNotBlank()) setRequestProperty("Cookie", cookie)
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val CONNECT_TIMEOUT = 20_000
        const val READ_TIMEOUT = 40_000
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) StudyLock/0.3"
    }
}
