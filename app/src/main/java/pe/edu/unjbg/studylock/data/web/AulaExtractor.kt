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
 */
class AulaExtractor(
    private val session: UnjbgWebSession,
    private val baseUrl: String,
) {

    /** Abre sesión, descubre los cursos y arma el árbol. */
    suspend fun sync(credentials: UnjbgCredentials, status: (String) -> Unit): List<ContentCourse> {
        status("Abriendo el Aula Virtual…")
        session.open(baseUrl)

        val loggedIn = session.loginWith(
            user = credentials.aulaUser,
            password = credentials.aulaPassword,
            status = status,
            loginMessage = "Iniciando sesión en el Aula Virtual…",
            waitingMessage = "Completa el inicio de sesión en la ventana…",
        )
        if (!loggedIn) return emptyList()

        status("Buscando tus cursos…")
        session.settle()

        val courses = session.collectLinks()
            .filter { LinkClassifier.looksLikeCourse(it.text, it.href) }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_COURSES)

        if (courses.isEmpty()) {
            // Sin cursos detectables: al menos rescatamos el material visible del panel.
            val sections = session.crawlSections(baseUrl, maxPages = 6)
            return if (sections.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    ContentCourse(
                        id = LinkClassifier.canonical(baseUrl),
                        name = "Aula Virtual",
                        subtitle = "Material del panel principal",
                        source = ContentSource.AULA,
                        sections = sections,
                    ),
                )
            }
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
        }
        return tree
    }

    private companion object {
        const val MAX_COURSES = 30
        const val MAX_PAGES_PER_COURSE = 8
    }
}
