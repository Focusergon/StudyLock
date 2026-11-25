package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * «Automatización que busca» dentro del **Aula Virtual UNJBG**.
 *
 * No se ataca ningún endpoint: se abre la web real en el mini-navegador, se inicia sesión con las
 * credenciales guardadas y se recorre el panel de cursos como lo haría el estudiante. De ese
 * recorrido sale un árbol de cursos → secciones → archivos.
 *
 * Si no se detecta ningún curso, se devuelve solo un aviso: **nunca** se genera un curso inventado
 * para «rellenar» la pantalla.
 */
class AulaExtractor(
    private val session: UnjbgWebSession,
    private val baseUrl: String,
) {

    suspend fun sync(
        credentials: UnjbgCredentials,
        status: (String) -> Unit,
        awaitManual: suspend () -> Unit = {},
        onPartial: (List<ContentCourse>, Int, Int) -> Unit = { _, _, _ -> Unit },
    ): ExtractionResult {
        status("Abriendo el Aula Virtual…")
        session.open(baseUrl)

        val loggedIn = session.loginWith(
            user = credentials.aulaUser,
            password = credentials.aulaPassword,
            status = status,
            loginMessage = "Iniciando sesión en el Aula Virtual…",
            waitingMessage = "Completa el inicio de sesión en la ventana y pulsa «Ya terminé».",
            awaitManual = awaitManual,
        )
        if (!loggedIn) {
            return ExtractionResult(notice = "No se pudo iniciar sesión en el Aula Virtual. Revisa tus credenciales.")
        }

        status("Buscando tus cursos…")
        session.settle()

        val allLinks = session.collectLinks()
        val courses = allLinks
            .filter { LinkClassifier.looksLikeCourse(it.text, it.href) }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_COURSES)

        if (courses.isEmpty()) {
            return ExtractionResult(
                notice = "No se detectó ningún curso en el Aula Virtual.",
                detectedLinks = allLinks,
            )
        }

        val tree = ArrayList<ContentCourse>()
        courses.forEachIndexed { index, link ->
            val name = link.text.trim().ifBlank { LinkClassifier.fileNameFromUrl(link.href) }
            status("Revisando «${name.take(42)}» (${index + 1}/${courses.size})…")
            val sections = session.crawlSections(link.href, maxPages = MAX_PAGES_PER_COURSE)
            tree += ContentCourse(
                id = LinkClassifier.canonical(link.href),
                name = name.take(90),
                source = ContentSource.AULA,
                sections = sections,
            )
            onPartial(tree.toList(), index + 1, courses.size)
        }
        return ExtractionResult(courses = tree)
    }

    private companion object {
        const val MAX_COURSES = 15
        const val MAX_PAGES_PER_COURSE = 4
    }
}
