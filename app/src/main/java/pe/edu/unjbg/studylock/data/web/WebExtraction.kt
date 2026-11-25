package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSection

/**
 * Rutinas compartidas por [AulaExtractor] e [IntranetExtractor].
 *
 * La idea es la misma en ambos: iniciar sesión como lo haría una persona y luego **recorrer** las
 * páginas del curso para descubrir sus archivos, sin depender de endpoints concretos.
 */

/** Tiempo máximo para el login automático antes de pasar a pedir ayuda al usuario. */
private const val LOGIN_TIMEOUT = 12_000L

/** Margen corto tras confirmar el login manual antes de volver a comprobar el formulario. */
private const val AFTER_MANUAL_TIMEOUT = 12_000L

/**
 * Inicia sesión si la página lo pide. Devuelve `false` si, tras esperar, sigue habiendo un
 * formulario de contraseña (credenciales incorrectas o intervención manual necesaria).
 */
internal suspend fun UnjbgWebSession.loginWith(
    user: String,
    password: String,
    status: (String) -> Unit,
    loginMessage: String,
    waitingMessage: String,
    /** La UI confirma «Ya terminé el ingreso»; por defecto se espera un margen corto. */
    awaitManual: suspend () -> Unit = {},
): Boolean {
    if (!hasPasswordField()) return true
    status(loginMessage)
    submitLogin(user, password)
    if (waitUntil(WebScripts.noPasswordField(), LOGIN_TIMEOUT)) return true
    status(waitingMessage)
    awaitManual()
    return waitUntil(WebScripts.noPasswordField(), AFTER_MANUAL_TIMEOUT)
}

/** Espera a que la página termine de pintar contenido dinámico (Angular) antes de leerla. */
internal suspend fun UnjbgWebSession.settle(timeoutMs: Long = 3_000L) {
    waitUntil("document.querySelectorAll('a[href]').length >= 2", timeoutMs, intervalMs = 250)
}

/** Convierte un enlace de la página en un archivo del árbol, o `null` si no es un documento. */
internal fun fileFromLink(link: WebLink): ContentFile? {
    if (!LinkClassifier.looksLikeFile(link.href, link.text)) return null
    val lower = link.href.lowercase()
    if (lower.startsWith("javascript:") || lower.startsWith("mailto:")) return null
    val name = link.text.trim().ifBlank { LinkClassifier.fileNameFromUrl(link.href) }
    return ContentFile(
        id = LinkClassifier.canonical(link.href),
        name = name.take(140),
        url = link.href,
        mimeType = LinkClassifier.mimeFor(name.ifBlank { link.href }),
    )
}

/**
 * Recorre [startUrl] y las páginas relacionadas del mismo sitio, y agrupa los archivos hallados
 * por página: cada página con documentos se convierte en una [ContentSection].
 *
 * Solo se siguen enlaces que parecen secciones del curso (unidad, semana, tema, recurso…) para no
 * recorrer todo el campus.
 */
internal suspend fun UnjbgWebSession.crawlSections(
    startUrl: String,
    maxPages: Int = 10,
): List<ContentSection> {
    val visited = HashSet<String>()
    val queue = ArrayDeque<String>()
    queue += startUrl
    val sections = ArrayList<ContentSection>()
    var pages = 0

    while (queue.isNotEmpty() && pages < maxPages) {
        val url = queue.removeFirst()
        val key = LinkClassifier.canonical(url)
        if (!visited.add(key)) continue
        if (!open(url)) continue
        settle()
        pages++

        val links = collectLinks()
        val files = links.mapNotNull { fileFromLink(it) }.distinctBy { it.url }
        if (files.isNotEmpty()) {
            val title = runJs(WebScripts.pageTitle()).orEmpty().trim().ifBlank { "Material" }
            sections += ContentSection(id = key, name = title.take(90), files = files)
        }

        links.asSequence()
            .filter { LinkClassifier.isCrawlableLink(it.text, it.href, startUrl) }
            .filter { LinkClassifier.isWithin(it.href, startUrl) || LinkClassifier.looksLikeSection(it.text) }
            .map { it.href }
            .forEach { queue += it }
    }
    return sections
}
