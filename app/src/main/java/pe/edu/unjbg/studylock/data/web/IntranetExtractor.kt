package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * «Automatización que busca» dentro de la **Intranet UNJBG**.
 *
 * El objetivo principal son los **sílabos**: se abre la Intranet, se inicia sesión y se buscan los
 * enlaces de sílabos por su texto (tolerante a cambios de ruta). Si no aparecen, se recorre el
 * panel para no devolver un resultado vacío.
 */
class IntranetExtractor(
    private val session: UnjbgWebSession,
    private val baseUrl: String,
) {

    suspend fun sync(credentials: UnjbgCredentials, status: (String) -> Unit): List<ContentCourse> {
        status("Abriendo la Intranet…")
        session.open(baseUrl)

        val loggedIn = session.loginWith(
            user = credentials.effectiveIntranetUser,
            password = credentials.effectiveIntranetPassword,
            status = status,
            loginMessage = "Iniciando sesión en la Intranet…",
            waitingMessage = "Completa el inicio de sesión en la ventana…",
        )
        if (!loggedIn) return emptyList()

        status("Buscando sílabos…")
        session.settle()

        val syllabusLinks = session.collectLinks()
            .filter { LinkClassifier.isSyllabusLink(it.text, it.href) }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_SYLLABUS_PAGES)

        if (syllabusLinks.isNotEmpty()) {
            val tree = ArrayList<ContentCourse>()
            syllabusLinks.forEachIndexed { index, link ->
                val name = link.text.trim().ifBlank { "Sílabos" }
                status("Revisando «${name.take(42)}» (${index + 1}/${syllabusLinks.size})…")
                val sections = session.crawlSections(link.href, maxPages = MAX_PAGES_PER_COURSE)
                tree += ContentCourse(
                    id = LinkClassifier.canonical(link.href),
                    name = name.take(90),
                    subtitle = "Sílabos · Intranet",
                    source = ContentSource.INTRANET,
                    sections = sections,
                )
            }
            return tree
        }

        val sections = session.crawlSections(baseUrl, maxPages = MAX_FALLBACK_PAGES)
        return if (sections.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ContentCourse(
                    id = LinkClassifier.canonical(baseUrl),
                    name = "Intranet",
                    subtitle = "Material encontrado en el panel",
                    source = ContentSource.INTRANET,
                    sections = sections,
                ),
            )
        }
    }

    private companion object {
        const val MAX_SYLLABUS_PAGES = 20
        const val MAX_PAGES_PER_COURSE = 6
        const val MAX_FALLBACK_PAGES = 10
    }
}
