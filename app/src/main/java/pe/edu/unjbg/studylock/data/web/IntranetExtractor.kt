package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * «Automatización que busca» dentro de la **Intranet UNJBG**.
 *
 * El objetivo principal son los **sílabos**: se abre la Intranet, se inicia sesión y se buscan los
 * enlaces de sílabos por su texto (tolerante a cambios de ruta). Si no aparecen, solo se avisa:
 * no se crea un curso «Intranet» artificial con lo que haya en el panel.
 */
class IntranetExtractor(
    private val session: UnjbgWebSession,
    private val baseUrl: String,
) {

    suspend fun sync(
        credentials: UnjbgCredentials,
        status: (String) -> Unit,
        awaitManual: suspend () -> Unit = {},
        onPartial: (List<ContentCourse>, Int, Int) -> Unit = { _, _, _ -> Unit },
    ): ExtractionResult {
        status("Abriendo la Intranet…")
        session.open(baseUrl)

        val loggedIn = session.loginWith(
            user = credentials.effectiveIntranetUser,
            password = credentials.effectiveIntranetPassword,
            status = status,
            loginMessage = "Iniciando sesión en la Intranet…",
            waitingMessage = "Completa el inicio de sesión en la ventana y pulsa «Ya terminé».",
            awaitManual = awaitManual,
        )
        if (!loggedIn) {
            return ExtractionResult(notice = "No se pudo iniciar sesión en la Intranet. Revisa tus credenciales.")
        }

        status("Buscando sílabos…")
        session.settle()

        val allLinks = session.collectLinks()
        val syllabusLinks = allLinks
            .filter { LinkClassifier.isSyllabusLink(it.text, it.href) }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_SYLLABUS_PAGES)

        if (syllabusLinks.isEmpty()) {
            return ExtractionResult(
                notice = "No se detectaron sílabos en la Intranet.",
                detectedLinks = allLinks,
            )
        }

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
            onPartial(tree.toList(), index + 1, syllabusLinks.size)
        }
        return ExtractionResult(courses = tree)
    }

    private companion object {
        const val MAX_SYLLABUS_PAGES = 8
        const val MAX_PAGES_PER_COURSE = 3
    }
}
