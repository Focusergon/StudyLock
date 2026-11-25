package pe.edu.unjbg.studylock.data.web

import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/** Un enlace encontrado en la página que el mini-navegador está mostrando. */
data class WebLink(
    val text: String,
    val href: String,
    /** Origen del enlace: `a`, `routerLink`, `data-url`, `onclick` o `button`. */
    val kind: String = "a",
)

/** Respuesta de una petición `fetch` hecha dentro de la sesión del WebView. */
data class FetchResult(val ok: Boolean, val status: Int, val body: String)

/**
 * Conduce un [WebView] como lo haría una persona: abre páginas, rellena el login, espera y lee el
 * contenido. Mantiene la sesión (cookies) entre pasos, que es justo lo que hace robusta la
 * automatización frente a cambios de endpoints.
 *
 * Todos los métodos suspenden y garantizan que la llamada al WebView ocurra en el hilo principal.
 */
class UnjbgWebSession(private val webView: WebView) {

    private val pending = ConcurrentHashMap<String, kotlinx.coroutines.CancellableContinuation<String>>()
    private val requestCounter = AtomicInteger(0)

    init {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(Bridge(), BRIDGE)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        }
    }

    /** Carga [url] y espera a que el documento esté listo. */
    suspend fun open(url: String, timeoutMs: Long = NAV_TIMEOUT): Boolean {
        withContext(Dispatchers.Main) { webView.loadUrl(url) }
        return waitUntil("document.readyState === 'complete' && !!document.body", timeoutMs)
    }

    /** Ejecuta [script] y devuelve el resultado ya deserializado (o `null`). */
    suspend fun runJs(script: String): String? = decodeJsValue(evaluate(script))

    /** Espera a que [predicateJs] devuelva `true`, sondeando cada [intervalMs]. */
    suspend fun waitUntil(predicateJs: String, timeoutMs: Long = NAV_TIMEOUT, intervalMs: Long = 400): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (runJs(predicateJs) == "true") return true
            delay(intervalMs)
        }
        return false
    }

    /** `true` si la página todavía muestra el formulario de contraseña. */
    suspend fun hasPasswordField(): Boolean = runJs(WebScripts.hasPasswordField()) == "true"

    /** URL actual del WebView. */
    suspend fun currentUrl(): String = runJs(WebScripts.location()).orEmpty()

    /** Texto visible de la página actual. */
    suspend fun pageText(): String = runJs(WebScripts.pageText()).orEmpty()

    /** Rellena el login y pulsa «Ingresar». Devuelve el estado reportado por el script. */
    suspend fun submitLogin(user: String, password: String): String =
        runJs(WebScripts.login(user, password)).orEmpty()

    /** Enlaces de la página actual. */
    suspend fun collectLinks(): List<WebLink> {
        val raw = runJs(WebScripts.collectLinks()) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val href = obj.optString("h")
                if (href.isBlank()) null else WebLink(obj.optString("t"), href, obj.optString("k", "a"))
            }
        }.getOrDefault(emptyList())
    }

    /** Descarga [url] con `fetch` dentro de la sesión (misma cookies que el navegador). */
    suspend fun fetchText(url: String, timeoutMs: Long = FETCH_TIMEOUT): FetchResult {
        val id = "req-" + requestCounter.incrementAndGet()
        val payload = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    pending[id] = cont
                    cont.invokeOnCancellation { pending.remove(id) }
                    webView.evaluateJavascript(WebScripts.fetchText(url, id, BRIDGE)) { }
                }
            }
        }
        pending.remove(id)
        if (payload == null) return FetchResult(false, 0, "timeout")
        return runCatching {
            val json = JSONObject(payload)
            FetchResult(json.optBoolean("ok"), json.optInt("status"), json.optString("body"))
        }.getOrElse { FetchResult(false, 0, payload) }
    }

    /** Cookies de sesión para [url], para reutilizarlas al descargar archivos grandes. */
    suspend fun cookiesFor(url: String): String = withContext(Dispatchers.Main) {
        CookieManager.getInstance().getCookie(url).orEmpty()
    }

    /** Detiene la carga actual sin destruir el WebView (por ejemplo al cancelar una sincronización). */
    fun stopLoading() {
        runCatching { webView.stopLoading() }
    }

    /** Libera el WebView. */
    fun destroy() {
        pending.values.forEach { it.cancel() }
        pending.clear()
        runCatching {
            webView.removeJavascriptInterface(BRIDGE)
            webView.stopLoading()
            webView.destroy()
        }
    }

    // --- Interno ------------------------------------------------------------

    private suspend fun evaluate(script: String): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            runCatching {
                webView.evaluateJavascript(script) { value -> if (cont.isActive) cont.resume(value) }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    /**
     * `evaluateJavascript` devuelve un literal JSON; aquí se decodifica a texto plano.
     *
     * Se normalizan booleanos y números a texto porque `waitUntil` compara contra `"true"`.
     */
    private fun decodeJsValue(raw: String?): String? {
        if (raw == null || raw == "null") return null
        val value = runCatching { JSONTokener(raw).nextValue() }.getOrNull() ?: return null
        return when (value) {
            is String -> value
            is Boolean -> value.toString()
            is Number -> value.toString()
            JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onResult(id: String, payload: String) {
            val cont = pending.remove(id) ?: return
            if (cont.isActive) cont.resume(payload)
        }
    }

    companion object {
        const val BRIDGE = "StudyLockBridge"
        const val NAV_TIMEOUT = 10_000L
        const val FETCH_TIMEOUT = 30_000L
    }
}
