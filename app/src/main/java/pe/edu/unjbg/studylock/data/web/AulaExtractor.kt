package pe.edu.unjbg.studylock.data.web

import pe.edu.unjbg.studylock.core.log.StudyLockLog
import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentSource

/**
 * «Automatización que busca» dentro del **Aula Virtual UNJBG**.
 *
 * No se ataca ningún endpoint: se abre la web real en el mini-navegador, se inicia sesión con las
 * credenciales guardadas y se recorre el panel de cursos como lo haría el estudiante. El modelo local
 * clasifica los enlaces detectados; en modo diagnóstico el estudiante confirma cuáles son cursos.
 * Si no se detecta ningún curso, se devuelve solo un aviso: **nunca** se genera un curso inventado.
 */
class AulaExtractor(
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
        status("Abriendo el Aula Virtual…")
        StudyLockLog.log("Diagnóstico", "Aula · paso 1: abrir portal")
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
        StudyLockLog.log("Diagnóstico", "Aula · paso 2: ${allLinks.size} enlace(s) detectado(s)")

        status("Clasificando enlaces con el modelo local…")
        val outcome = classifier.classify(allLinks, LinkPortal.AULA)
        val verdicts = outcome.verdicts.ifEmpty { allLinks.map { LinkClassifier.heuristicVerdict(it, LinkPortal.AULA) } }
        StudyLockLog.log(
            "Diagnóstico",
            "Aula · paso 3: clasificación ${if (outcome.usedAi) "IA" else "heurística"}, " +
                "${verdicts.count { it.role == LinkRole.CURSO }} curso(s) candidato(s)",
        )

        val accepted = confirmLinks(verdicts)
        val courses = verdicts
            .filter { it.href in accepted && it.role == LinkRole.CURSO }
            .distinctBy { LinkClassifier.canonical(it.href) }
            .take(MAX_COURSES)
        StudyLockLog.log("Aula", "Enlaces detectados: ${allLinks.size} · cursos confirmados: ${courses.size}")

        if (courses.isEmpty()) {
            return ExtractionResult(
                notice = "No se detectó ningún curso en el Aula Virtual.",
                detectedLinks = allLinks,
                verdicts = verdicts,
                classificationError = outcome.error,
            )
        }

        val tree = ArrayList<ContentCourse>()
        courses.forEachIndexed { index, verdict ->
            val name = verdict.label
            status("Revisando «${name.take(42)}» (${index + 1}/${courses.size})…")
            StudyLockLog.log("Diagnóstico", "Aula · paso 4.${index + 1}: revisando ${name.take(60)}")
            val sections = session.crawlSections(verdict.href, maxPages = MAX_PAGES_PER_COURSE)
            StudyLockLog.log("Aula", "Curso ${index + 1}: ${sections.size} sección(es)")
            tree += ContentCourse(
                id = LinkClassifier.canonical(verdict.href),
                name = name.take(90),
                source = ContentSource.AULA,
                sections = sections,
            )
            onPartial(tree.toList(), index + 1, courses.size)
        }
        return ExtractionResult(
            courses = tree,
            detectedLinks = allLinks,
            verdicts = verdicts,
            classificationError = outcome.error,
        )
    }

    private companion object {
        const val MAX_COURSES = 15
        const val MAX_PAGES_PER_COURSE = 4
    }
}
