package pe.edu.unjbg.studylock.data.web

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Acceso a las cookies de sesión que el mini-navegador dejó guardadas.
 *
 * `CookieManager` es un singleton del proceso: sobrevive a la destrucción del `WebView`, así que
 * podemos reutilizar la sesión para descargar archivos sin volver a mostrar el navegador.
 */
object WebCookies {

    /** Cookies de sesión aplicables a [url] (cadena lista para la cabecera `Cookie`). */
    suspend fun cookieFor(url: String): String = withContext(Dispatchers.Main) {
        runCatching { CookieManager.getInstance().getCookie(url).orEmpty() }.getOrDefault("")
    }

    /** Borra todas las cookies: cierra la sesión en el Aula y la Intranet. */
    suspend fun clear() = withContext(Dispatchers.Main) {
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
    }
}
