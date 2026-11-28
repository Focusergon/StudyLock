package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.core.log.StudyLockLog
import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * «Automatización que busca» dentro de la **Intranet UNJBG**.
 *
 * El objetivo principal son los **sílabos**: se abre la Intranet, se inicia sesión y el modelo local
 * clasifica los enlaces detectados. En modo diagnóstico el estudiante confirma cuáles son sílabos.
 * Si no aparecen, solo se avisa: no se crea un curso «Intranet» artificial con lo que haya en el panel.
 */
class IntranetExtractor(
    private val session: UnjbgWebSession,
    private val baseUrl: String,
    private val classifier: AiLinkClassifier,
    private val confirmLinks: LinkConfirmation = { verdicts -> verdicts.acceptedByDefault() },
) {

    suspend fun sync(
        credentials: UnjbgCredentials,
        status: (String) -> Unit,
        awaitManual: suspend () -> Unit = {},
        onPartial: (List<ContentCourse>, Int, Int) -> Unit = { _, _, _ -> Unit },
    ): ExtractionResult {
        status("Abriendo la Intranet…")
        StudyLockLog.log("Diagnóstico", "Intranet · URL: $baseUrl")
        StudyLockLog.log("Diagnóstico", "Intranet · paso 1: abrir portal")
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
        session.settle(timeoutMs = 10_000L, minAnchors = 8)
        StudyLockLog.log(
            "Diagnóstico",
            "Intranet · página: ${session.currentUrl()} · " +
                session.runJs(WebScripts.pageTitle()).orEmpty().take(80) +
                " · texto: " + session.pageText().replace('\n', ' ').take(180),
        )
        val allLinks = session.collectLinks()
        StudyLockLog.log("Diagnóstico", "Intranet · paso 2: ${allLinks.size} enlace(s) detectado(s)")

        status("Clasificando enlaces con el modelo local…")
        val outcome = classifier.classify(allLinks, LinkPortal.INTRANET)
        val verdicts = outcome.verdicts.ifEmpty { allLinks.map { LinkClassifier.heuristicVerdict(it, LinkPortal.INTRANET) } }
        StudyLockLog.log(
            "Diagnóstico",
            "Intranet · paso 3: clasificación ${if (outcome.usedAi) "IA" else "heurística"}, " +
                "${verdicts.count { it.role == LinkRole.SILABO }} sílabo(s) candidato(s)",
        )

        val accepted = confirmLinks(verdicts)
        val syllabusLinks = verdicts
            .filter { it.href in accepted && it.role == LinkRole.SILABO }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_SYLLABUS_PAGES)

        if (syllabusLinks.isEmpty()) {
            return ExtractionResult(
                notice = "No se detectaron sílabos en la Intranet.",
                detectedLinks = allLinks,
                verdicts = verdicts,
                classificationError = outcome.error,
            )
        }

        val tree = ArrayList<ContentCourse>()
        syllabusLinks.forEachIndexed { index, verdict ->
            val name = verdict.label.ifBlank { "Sílabos" }
            status("Revisando «${name.take(42)}» (${index + 1}/${syllabusLinks.size})…")
            StudyLockLog.log("Diagnóstico", "Intranet · paso 4.${index + 1}: revisando ${name.take(60)}")
            val sections = session.crawlSections(verdict.href, maxPages = MAX_PAGES_PER_COURSE)
            StudyLockLog.log("Intranet", "Sílabo ${index + 1}: ${sections.size} sección(es)")
            tree += ContentCourse(
                id = LinkClassifier.canonical(verdict.href),
                name = name.take(90),
                subtitle = "Sílabos · Intranet",
                source = ContentSource.INTRANET,
                sections = sections,
            )
            onPartial(tree.toList(), index + 1, syllabusLinks.size)
        }
        return ExtractionResult(
            courses = tree,
            detectedLinks = allLinks,
            verdicts = verdicts,
            classificationError = outcome.error,
        )
    }

    private companion object {
        const val MAX_SYLLABUS_PAGES = 8
        const val MAX_PAGES_PER_COURSE = 3
    }
}
