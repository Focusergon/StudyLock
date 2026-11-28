package pe.edu.unjbg.studylock.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.log.StudyLockLog
import pe.edu.unjbg.studylock.core.text.PdfTextExtractor
import pe.edu.unjbg.studylock.data.ai.ModelDownloader
import pe.edu.unjbg.studylock.data.ai.ModelStatus
import pe.edu.unjbg.studylock.data.auth.CredentialStore
import pe.edu.unjbg.studylock.data.auth.DEFAULT_AULA_URL
import pe.edu.unjbg.studylock.data.auth.DEFAULT_INTRANET_URL
import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.data.content.ContentPaths
import pe.edu.unjbg.studylock.data.content.ContentStore
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.data.web.AiLinkClassifier
import pe.edu.unjbg.studylock.data.web.AulaExtractor
import pe.edu.unjbg.studylock.data.web.ExtractionResult
import pe.edu.unjbg.studylock.data.web.FileDownloader
import pe.edu.unjbg.studylock.data.web.IntranetExtractor
import pe.edu.unjbg.studylock.data.web.LinkVerdict
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession
import pe.edu.unjbg.studylock.data.web.WebCookies
import pe.edu.unjbg.studylock.data.web.WebLink
import pe.edu.unjbg.studylock.data.web.acceptedByDefault
import pe.edu.unjbg.studylock.domain.content.ContentMerger
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.content.HybridQuestionGenerator
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSource
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind
import pe.edu.unjbg.studylock.domain.repo.ContentRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Estado de la pantalla «Cuentas UNJBG». */
data class AccountsUi(
    val loaded: Boolean = false,
    val aulaUser: String = "",
    val aulaPassword: String = "",
    val intranetUser: String = "",
    val intranetPassword: String = "",
    val samePassword: Boolean = true,
    val revealPasswords: Boolean = false,
    val aulaUrl: String = AccountsDefaults.AULA_URL,
    val intranetUrl: String = AccountsDefaults.INTRANET_URL,
    val saving: Boolean = false,
    val syncing: Boolean = false,
    val status: String = "",
    val browserVisible: Boolean = false,
    val error: String? = null,
    val tree: List<ContentCourse> = emptyList(),
    val selected: Set<String> = emptySet(),
    val importing: Boolean = false,
    val resultMessage: String? = null,
    /** Avisos de los portales cuando no se detectó contenido (nunca se inventa contenido). */
    val notices: List<String> = emptyList(),
    /** Enlaces vistos en el mini-navegador, para el modo inspección. */
    val detectedLinks: List<WebLink> = emptyList(),
    val showLinks: Boolean = false,
    /** Progreso en vivo del recorrido, para poder verlo desde cualquier pantalla. */
    val syncPhase: String = "",
    val syncDone: Int = 0,
    val syncTotal: Int = 0,
    /** El portal pide intervención: la UI muestra «Ya terminé el ingreso». */
    val manualLoginNeeded: Boolean = false,
    /** El WebView no pudo crearse: se reintentará en la próxima sincronización. */
    val browserFailed: Boolean = false,
    /** Enlaces clasificados por el modelo (o heurística) para revisar y confirmar. */
    val classifiedLinks: List<LinkVerdict> = emptyList(),
    /** Modo diagnóstico: la sincronización se detiene hasta que el estudiante confirme. */
    val diagnosticMode: Boolean = false,
    val confirmationNeeded: Boolean = false,
    val confirmedHrefs: Set<String> = emptySet(),
    val rejectedHrefs: Set<String> = emptySet(),
    /** Historial corto de pasos, visible en el informe que se puede copiar. */
    val diagnosticSteps: List<String> = emptyList(),
) {
    /** Se exige el Aula Virtual; la Intranet se deriva de ella si se marca «misma contraseña». */
    val canSave: Boolean
        get() = aulaUser.isNotBlank() && aulaPassword.isNotBlank() &&
            (samePassword || intranetPassword.isNotBlank())

    val canSync: Boolean get() = canSave && !syncing && !saving

    /** Un curso sin archivos también es árbol: debe verse con «Sin archivos detectados». */
    val hasTree: Boolean get() = tree.isNotEmpty()

    val selectedCount: Int get() = selected.size

    val importableSelected: Int
        get() = tree.flatMap { it.files }.count { it.url in selected && it.isImportable }

    val canGenerate: Boolean get() = importableSelected > 0 && !importing && !syncing
}

/** Valores por defecto de los portales de la UNJBG (editables desde la pantalla). */
object AccountsDefaults {
    const val AULA_URL = DEFAULT_AULA_URL
    const val INTRANET_URL = DEFAULT_INTRANET_URL
}

/**
 * Orquesta la vinculación con la UNJBG. Vive en [AppGraph], no en una ruta: así salir de la pantalla
 * de Cuentas no destruye el WebView ni cancela la búsqueda, y el árbol parcial se puede ver desde
 * cualquier pantalla mientras sigue recorriendo los portales.
 */
class AccountsViewModel(
    private val credentials: CredentialStore,
    private val content: ContentRepository,
    private val store: ContentStore,
    private val generator: HybridQuestionGenerator,
    private val classifier: AiLinkClassifier,
    private val model: ModelDownloader,
    private val preferences: UserPreferences,
    private val downloader: FileDownloader = FileDownloader(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    /** Sílabos sin curso del Aula: se archivan en `agregados/` al generar. */
    private var unmatchedSyllabi: List<ContentCourse> = emptyList()

    private val _ui = MutableStateFlow(AccountsUi())
    val ui: StateFlow<AccountsUi> = _ui.asStateFlow()

    private var session: UnjbgWebSession? = null
    private var sessionReady: CompletableDeferred<UnjbgWebSession> = CompletableDeferred()
    private var syncJob: Job? = null
    /** Vigilante que corta la búsqueda si un portal se cuelga y deja la UI usable. */
    private var syncWatchdog: Job? = null
    private var manualLogin: CompletableDeferred<Unit>? = null
    private var linkConfirmation: CompletableDeferred<Set<String>>? = null
    private var aulaPartial: List<ContentCourse> = emptyList()
    private var intranetPartial: List<ContentCourse> = emptyList()

    init {
        viewModelScope.launch {
            preferences.flow.collect { current ->
                _ui.update { it.copy(diagnosticMode = current.diagnosticMode) }
            }
        }

        viewModelScope.launch {
            runCatching { credentials.current() }
                .onSuccess { stored ->
                    _ui.update {
                        it.copy(
                            loaded = true,
                            aulaUser = stored.aulaUser,
                            aulaPassword = stored.aulaPassword,
                            intranetUser = stored.intranetUser,
                            intranetPassword = stored.intranetPassword,
                            samePassword = stored.samePassword,
                            aulaUrl = stored.aulaUrl.ifBlank { AccountsDefaults.AULA_URL },
                            intranetUrl = stored.intranetUrl.ifBlank { AccountsDefaults.INTRANET_URL },
                        )
                    }
                }
                .onFailure { error ->
                    StudyLockLog.error("Cuentas", error, "No se pudieron leer las credenciales guardadas")
                    _ui.update { it.copy(loaded = true) }
                }
        }
    }

    // --- WebView -------------------------------------------------------------

    /** La UI (panel fijo en la raíz) entrega el WebView ya creado. */
    fun attachSession(webSession: UnjbgWebSession) {
        session = webSession
        StudyLockLog.log("Navegador", "Sesión WebView lista")
        if (!sessionReady.isCompleted) sessionReady.complete(webSession)
    }

    /** La UI ha soltado la referencia; no destruimos el WebView al navegar. */
    fun detachSession() {
        session = null
    }

    /**
     * Prepara el canal de sesión para una nueva corrida. Si el WebView global ya está creado (lo
     * está desde que se compone la raíz), lo reutiliza en lugar de esperar uno nuevo.
     */
    private fun prepareSession() {
        sessionReady = CompletableDeferred()
        session?.let { sessionReady.complete(it) }
    }

    /** El WebView no pudo crearse: se registra, se cancela y se permite reintentar después. */
    fun reportBrowserFailure(message: String) {
        StudyLockLog.error("Navegador", IllegalStateException(message))
        cancelSync()
        _ui.update {
            it.copy(
                error = message,
                syncing = false,
                browserVisible = false,
                browserFailed = true,
                status = "",
            )
        }
    }

    // --- Edición de credenciales -------------------------------------------

    fun setAulaUser(value: String) = _ui.update { it.copy(aulaUser = value, error = null) }
    fun setAulaPassword(value: String) = _ui.update { it.copy(aulaPassword = value, error = null) }
    fun setIntranetUser(value: String) = _ui.update { it.copy(intranetUser = value, error = null) }
    fun setIntranetPassword(value: String) = _ui.update { it.copy(intranetPassword = value, error = null) }
    fun setSamePassword(value: Boolean) = _ui.update { it.copy(samePassword = value, error = null) }
    fun toggleReveal() = _ui.update { it.copy(revealPasswords = !it.revealPasswords) }
    fun setAulaUrl(value: String) = _ui.update { it.copy(aulaUrl = value.trim(), error = null) }
    fun setIntranetUrl(value: String) = _ui.update { it.copy(intranetUrl = value.trim(), error = null) }
    fun dismissError() = _ui.update { it.copy(error = null) }
    fun dismissResult() = _ui.update { it.copy(resultMessage = null) }
    fun toggleLinks() = _ui.update { it.copy(showLinks = !it.showLinks) }

    /** Guarda las credenciales cifradas sin conectar. */
    fun save() {
        if (!_ui.value.canSave) {
            _ui.update { it.copy(error = "Escribe tu usuario y contraseña del Aula Virtual.") }
            return
        }
        _ui.update { it.copy(saving = true, error = null, resultMessage = null) }
        viewModelScope.launch {
            runCatching { credentials.save(currentCredentials()) }
                .onSuccess { _ui.update { it.copy(saving = false, resultMessage = "Credenciales guardadas.") } }
                .onFailure { error ->
                    _ui.update { it.copy(saving = false, error = "No se pudieron guardar: ${error.message}") }
                }
        }
    }

    // --- Automatización con el mini-navegador -------------------------------

    /** Auto-sync diario: se llama al entrar a la ruta `accounts?autoSync=true`. */
    fun syncFromAuto() {
        viewModelScope.launch {
            if (!credentials.current().hasAula || _ui.value.syncing) return@launch
            sync()
        }
    }

    /** Inicia sesión en Aula e Intranet, recorre el contenido y arma el árbol. */
    fun sync() {
        val state = _ui.value
        if (!state.canSave) {
            _ui.update { it.copy(error = "Escribe tu usuario y contraseña del Aula Virtual.") }
            return
        }
        if (state.syncing) return

        syncJob?.cancel()
        manualLogin?.cancel()
        manualLogin = null
        prepareSession()
        aulaPartial = emptyList()
        intranetPartial = emptyList()

        _ui.update {
            it.copy(
                syncing = true,
                error = null,
                resultMessage = null,
                tree = emptyList(),
                selected = emptySet(),
                detectedLinks = emptyList(),
                classifiedLinks = emptyList(),
                confirmationNeeded = false,
                confirmedHrefs = emptySet(),
                rejectedHrefs = emptySet(),
                diagnosticSteps = emptyList(),
                notices = emptyList(),
                showLinks = false,
                manualLoginNeeded = false,
                browserFailed = false,
                syncPhase = "Preparando",
                syncDone = 0,
                syncTotal = 0,
                status = "Preparando el navegador…",
                browserVisible = true,
            )
        }

        StudyLockLog.log("Cuentas", "Sincronización iniciada")
        syncJob = viewModelScope.launch {
            val creds = currentCredentials()
            runCatching { credentials.save(creds) }
            val status: (String) -> Unit = { message ->
                StudyLockLog.log("Diagnóstico", message)
                _ui.update {
                    it.copy(
                        status = message,
                        diagnosticSteps = (it.diagnosticSteps + message).takeLast(40),
                    )
                }
            }

            val awaitManual: suspend () -> Unit = {
                val signal = CompletableDeferred<Unit>()
                manualLogin = signal
                _ui.update {
                    it.copy(
                        manualLoginNeeded = true,
                        status = "Completa el ingreso en la ventanita y pulsa «Ya terminé».",
                    )
                }
                signal.await()
                _ui.update { it.copy(manualLoginNeeded = false) }
            }

            try {
                val webSession = sessionReady.await()
                val aula = runCatching {
                    AulaExtractor(webSession, state.aulaUrl, classifier, ::requestLinkConfirmation).sync(
                        credentials = creds,
                        status = status,
                        awaitManual = awaitManual,
                        onPartial = { partial, done, total ->
                            aulaPartial = partial
                            publishPartial("Aula", done, total)
                        },
                    )
                }.getOrDefault(ExtractionResult(notice = "No se pudo leer el Aula Virtual."))

                val intranet = if (creds.hasIntranet) {
                    runCatching {
                        IntranetExtractor(webSession, state.intranetUrl, classifier, ::requestLinkConfirmation).sync(
                            credentials = creds,
                            status = status,
                            awaitManual = awaitManual,
                            onPartial = { partial, done, total ->
                                intranetPartial = partial
                                publishPartial("Intranet", done, total)
                            },
                        )
                    }.getOrDefault(ExtractionResult(notice = "No se pudo leer la Intranet."))
                } else {
                    ExtractionResult()
                }

                val merged = ContentMerger.merge(aula.courses, intranet.courses)
                unmatchedSyllabi = merged.unmatchedSyllabi
                val tree = merged.courses
                val notices = listOfNotNull(aula.notice, intranet.notice) + if (unmatchedSyllabi.isEmpty()) {
                    emptyList()
                } else {
                    listOf(
                        "${unmatchedSyllabi.size} sílabo(s) no coinciden con ningún curso del Aula. " +
                            "Se guardarán en «Agregados» con una aviso; no se creó ningún curso falso."
                    )
                }
                val links = (aula.detectedLinks + intranet.detectedLinks)
                    .distinctBy { it.href + it.text }
                    .take(200)
                val verdicts = (aula.verdicts + intranet.verdicts)
                    .distinctBy { it.href + "|" + it.link.text }
                    .take(200)
                val classNotices = listOfNotNull(aula.classificationError, intranet.classificationError)
                    .filter { it.isNotBlank() }
                    .map { "Clasificación de enlaces: $it" }
                _ui.update {
                    it.copy(
                        syncing = false,
                        browserVisible = false,
                        status = "",
                        syncPhase = "",
                        syncDone = 0,
                        syncTotal = 0,
                        manualLoginNeeded = false,
                        confirmationNeeded = false,
                        tree = tree,
                        selected = autoSelected(tree),
                        notices = notices + classNotices,
                        detectedLinks = links,
                        classifiedLinks = verdicts,
                        error = null,
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelación del usuario: el estado ya lo dejó cancelSync().
                throw e
            } catch (e: Exception) {
                StudyLockLog.error("Cuentas", e, "Fallo durante la sincronización")
                _ui.update {
                    it.copy(
                        syncing = false,
                        browserVisible = false,
                        manualLoginNeeded = false,
                        confirmationNeeded = false,
                        syncPhase = "",
                        status = "",
                        error = "No se pudo completar la sincronización: ${e.message}",
                    )
                }
            }
        }

        // Vigilante: si el WebView se queda sin responder, cortamos y mostramos lo ya encontrado.
        syncWatchdog?.cancel()
        syncWatchdog = viewModelScope.launch {
            // El reloj no cuenta el tiempo en que la app espera una decisión del estudiante.
            var gracePeriods = 0
            while (isActive) {
                delay(SYNC_TIMEOUT)
                if (syncJob?.isActive != true) return@launch
                val waiting = _ui.value.manualLoginNeeded || _ui.value.confirmationNeeded
                if (waiting && gracePeriods < MAX_WATCHDOG_GRACE) {
                    gracePeriods++
                    continue
                }
                StudyLockLog.error(
                    "Cuentas",
                    IllegalStateException("Sincronización excedió ${SYNC_TIMEOUT / 1000} s"),
                )
                cancelSync()
                _ui.update {
                    it.copy(
                        error = "La sincronización tardó demasiado y se detuvo. " +
                            "Se conserva el árbol encontrado hasta ahora.",
                    )
                }
                return@launch
            }
        }
    }

    /** El usuario terminó captcha/SSO en el mini-navegador. */
    fun confirmManualLogin() {
        manualLogin?.complete(Unit)
        _ui.update { it.copy(manualLoginNeeded = false) }
    }

    // --- Confirmación de enlaces (modo diagnóstico) --------------------------

    /**
     * Suspende el recorrido para que el estudiante revise la clasificación del modelo.
     * Sin modo diagnóstico se aceptan cursos/sílabos con confianza suficiente y se sigue.
     */
    private suspend fun requestLinkConfirmation(verdicts: List<LinkVerdict>): Set<String> {
        if (!_ui.value.diagnosticMode) return verdicts.acceptedByDefault()
        val signal = CompletableDeferred<Set<String>>()
        linkConfirmation = signal
        val initial = verdicts.acceptedByDefault()
        _ui.update {
            it.copy(
                confirmationNeeded = true,
                classifiedLinks = verdicts,
                confirmedHrefs = initial,
                rejectedHrefs = emptySet(),
                status = "Revisa los enlaces clasificados y pulsa «Continuar».",
            )
        }
        StudyLockLog.log("Diagnóstico", "Esperando confirmación de ${verdicts.size} enlace(s)")
        return signal.await()
    }

    /** Fija la decisión de un enlace: «Esto es un curso/sílabo» o «Esto no». */
    fun setLinkDecision(href: String, accepted: Boolean) = _ui.update { state ->
        val confirmed = if (accepted) state.confirmedHrefs + href else state.confirmedHrefs - href
        val rejected = if (accepted) state.rejectedHrefs - href else state.rejectedHrefs + href
        state.copy(confirmedHrefs = confirmed, rejectedHrefs = rejected)
    }

    fun toggleLink(href: String) = setLinkDecision(href, href !in _ui.value.confirmedHrefs)

    fun setAllLinks(accepted: Boolean) = _ui.update { state ->
        if (accepted) {
            state.copy(
                confirmedHrefs = state.classifiedLinks.map { it.href }.toSet(),
                rejectedHrefs = emptySet(),
            )
        } else {
            state.copy(confirmedHrefs = emptySet(), rejectedHrefs = state.classifiedLinks.map { it.href }.toSet())
        }
    }

    /** «Continuar»: solo se recorren los enlaces que el estudiante dejó marcados. */
    fun confirmClassifiedLinks() {
        val signal = linkConfirmation ?: return
        val accepted = _ui.value.confirmedHrefs
        StudyLockLog.log("Diagnóstico", "Enlaces confirmados: ${accepted.size}")
        _ui.update { it.copy(confirmationNeeded = false) }
        signal.complete(accepted)
    }

    /** «Saltar»: se continúa sin cursos/sílabos de este portal. */
    fun skipClassifiedLinks() {
        val signal = linkConfirmation ?: return
        StudyLockLog.log("Diagnóstico", "Clasificación saltada por el estudiante")
        _ui.update { it.copy(confirmationNeeded = false, confirmedHrefs = emptySet()) }
        signal.complete(emptySet())
    }

    /** Texto para copiar desde la app: pasos, clasificación y avisos, sin credenciales. */
    fun diagnosticReport(): String {
        val ui = _ui.value
        val model = model.state()
        return buildString {
            append("StudyLock · informe de vinculación UNJBG\n")
            append("Fecha: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("Modelo: ${model.status} ${model.progress}% · ${model.message}\n")
            append("Modo diagnóstico: ${if (ui.diagnosticMode) "sí" else "no"}\n")
            append("Fase: ${ui.syncPhase.ifBlank { "—" }} · ${ui.syncDone}/${ui.syncTotal}\n\n")
            append("Pasos:\n")
            if (ui.diagnosticSteps.isEmpty()) append("(sin pasos registrados)\n")
            else ui.diagnosticSteps.forEach { append("- $it\n") }
            append("\nEnlaces clasificados (${ui.classifiedLinks.size}):\n")
            ui.classifiedLinks.forEach { verdict ->
                val mark = when (verdict.href) {
                    in ui.confirmedHrefs -> "SI"
                    in ui.rejectedHrefs -> "NO"
                    else -> "?"
                }
                append("[$mark] ${verdict.role} · ${verdict.source} (${verdict.confidence}%) · ${verdict.label}\n")
                append("     ${verdict.href.take(180)}\n")
                append("     ${verdict.reason.take(180)}\n")
            }
            if (ui.notices.isNotEmpty()) {
                append("\nAvisos:\n")
                ui.notices.forEach { append("- $it\n") }
            }
            ui.error?.let { append("\nError: $it\n") }
            append("\nRegistro:\n")
            append(StudyLockLog.allText().takeLast(6_000))
        }
    }

    /** Corta la búsqueda sin destruir el WebView global. */
    fun cancelSync() {
        syncWatchdog?.cancel()
        syncWatchdog = null
        syncJob?.cancel()
        syncJob = null
        manualLogin?.cancel()
        manualLogin = null
        linkConfirmation?.cancel()
        linkConfirmation = null
        session?.stopLoading()
        _ui.update {
            it.copy(
                syncing = false,
                browserVisible = false,
                manualLoginNeeded = false,
                confirmationNeeded = false,
                status = "",
                syncPhase = "",
                syncDone = 0,
                syncTotal = 0,
                notices = it.notices + "Sincronización cancelada.",
            )
        }
    }

    private fun publishPartial(phase: String, done: Int, total: Int) {
        val merged = ContentMerger.merge(aulaPartial, intranetPartial)
        val tree = merged.courses
        unmatchedSyllabi = merged.unmatchedSyllabi
        StudyLockLog.log("Cuentas", "Árbol parcial · $phase $done/$total · ${tree.size} curso(s)")
        _ui.update { state ->
            state.copy(
                tree = tree,
                selected = state.selected + autoSelected(tree),
                syncPhase = phase,
                syncDone = done,
                syncTotal = total,
            )
        }
    }

    private fun autoSelected(tree: List<ContentCourse>): Set<String> =
        tree.flatMap { course -> course.files.filter { it.isImportable }.map { it.url } }.toSet()

    // --- Selección del árbol ------------------------------------------------

    fun toggleFile(url: String) = _ui.update {
        val next = if (url in it.selected) it.selected - url else it.selected + url
        it.copy(selected = next)
    }

    fun selectAll() = _ui.update { state ->
        state.copy(selected = state.tree.flatMap { it.files }.map { it.url }.toSet())
    }

    fun clearSelection() = _ui.update { state ->
        state.copy(selected = emptySet())
    }

    // --- Generación de preguntas -------------------------------------------

    /** Descarga los archivos marcados y los convierte en cursos con preguntas. */
    fun generate() {
        val state = _ui.value
        if (state.importableSelected == 0) {
            _ui.update { it.copy(error = "Marca al menos un archivo PDF o de texto.") }
            return
        }
        if (state.importing) return

        _ui.update { it.copy(importing = true, error = null, resultMessage = null) }
        viewModelScope.launch {
            var importedCourses = 0
            var totalQuestions = 0
            var usedAi = false
            val storedCourses = ArrayList<ContentStore.StoredCourse>()
            val previous = store.manifest()

            for (course in state.tree) {
                val storedSections = ArrayList<ContentStore.StoredSection>()
                val outcome = withContext(Dispatchers.Default) {
                    generateFor(course, state.selected, storedSections)
                }
                val topics = outcome.topics
                if (outcome.usedAi) usedAi = true
                if (storedSections.isNotEmpty()) {
                    storedCourses += ContentStore.StoredCourse(
                        id = course.id,
                        name = course.name,
                        source = course.source.name,
                        sections = storedSections,
                    )
                }
                if (topics.isEmpty()) continue
                val result = content.importCourse(
                    courseName = course.name,
                    professor = "",
                    topics = topics,
                    kind = if (course.source == ContentSource.INTRANET) ImportSourceKind.INTRANET else ImportSourceKind.AULA,
                    sourceTitle = "${course.source.label} · ${course.name}",
                )
                importedCourses++
                totalQuestions += result.questionCount
            }

            // Sílabos sin coincidencia: copia física en agregados/, nunca un curso inventado.
            val added = ArrayList(previous.added)
            for (syllabus in unmatchedSyllabi) {
                for (file in syllabus.files.filter { it.isImportable }) {
                    val bytes = downloadBytes(file) ?: continue
                    val stored = store.storeAdded(
                        name = file.name,
                        bytes = bytes,
                        category = "Sílabos sin curso",
                        suggestedCourse = syllabus.name,
                        sourceUrl = file.url,
                    ) ?: continue
                    added += stored
                }
            }

            store.saveManifest(
                previous.copy(
                    lastSyncAt = System.currentTimeMillis(),
                    fingerprint = ContentPaths.fingerprint(state.tree),
                    courses = storedCourses.ifEmpty { previous.courses },
                    added = added,
                )
            )

            _ui.update {
                if (importedCourses == 0 && added.isEmpty()) {
                    it.copy(
                        importing = false,
                        error = "No se pudieron extraer preguntas del material seleccionado. Prueba con otro archivo.",
                    )
                } else {
                    it.copy(
                        importing = false,
                        resultMessage = "Listo: $importedCourses curso(s), $totalQuestions preguntas " +
                            "y ${added.size} archivo(s) agregado(s)." +
                            if (usedAi) " Algunas preguntas usaron el modelo local." else "",
                        selected = emptySet(),
                    )
                }
            }
        }
    }

    /** Cierra la sesión: borra cookies y credenciales guardadas. */
    fun logout() {
        cancelSync()
        viewModelScope.launch {
            WebCookies.clear()
            runCatching { credentials.clear() }
            _ui.update {
                it.copy(
                    aulaPassword = "",
                    intranetPassword = "",
                    tree = emptyList(),
                    selected = emptySet(),
                    detectedLinks = emptyList(),
                    classifiedLinks = emptyList(),
                    confirmationNeeded = false,
                    confirmedHrefs = emptySet(),
                    rejectedHrefs = emptySet(),
                    notices = emptyList(),
                    resultMessage = "Sesión cerrada y credenciales eliminadas.",
                    error = null,
                )
            }
        }
    }

    // --- Interno ------------------------------------------------------------

    private fun currentCredentials() = UnjbgCredentials(
        aulaUser = _ui.value.aulaUser.trim(),
        aulaPassword = _ui.value.aulaPassword,
        intranetUser = _ui.value.intranetUser.trim(),
        intranetPassword = _ui.value.intranetPassword,
        samePassword = _ui.value.samePassword,
        aulaUrl = _ui.value.aulaUrl,
        intranetUrl = _ui.value.intranetUrl,
    )

    private suspend fun generateFor(
        course: ContentCourse,
        selected: Set<String>,
        storedSections: MutableList<ContentStore.StoredSection>,
    ): CourseGeneration {
        val topics = ArrayList<GeneratedTopic>()
        var usedAi = false
        for (section in course.sections) {
            val body = StringBuilder()
            val storedFiles = ArrayList<ContentStore.StoredFile>()
            for (file in section.files) {
                if (file.url !in selected || !file.isImportable) continue
                val stored = storeAndRead(course, section.name, file) ?: continue
                storedFiles += stored.first
                val text = stored.second
                if (text.isNotBlank()) body.append(text).append("\n\n")
            }
            if (storedFiles.isNotEmpty()) {
                storedSections += ContentStore.StoredSection(section.name, storedFiles)
            }
            if (body.isBlank()) continue
            val outcome = generator.generate(body.toString(), section.name, maxQuestions = 8)
            if (outcome.usedAi) usedAi = true
            if (outcome.questions.size >= 2) topics += GeneratedTopic(section.name, outcome.questions)
        }
        if (topics.isEmpty()) {
            val whole = StringBuilder()
            for (file in course.files) {
                if (file.url !in selected || !file.isImportable) continue
                val stored = storeAndRead(course, course.name, file) ?: continue
                val text = stored.second
                if (text.isNotBlank()) whole.append(text).append("\n\n")
            }
            if (whole.isNotBlank()) {
                val outcome = generator.generate(whole.toString(), course.name, maxQuestions = 12)
                if (outcome.usedAi) usedAi = true
                if (outcome.questions.size >= 2) topics += GeneratedTopic(course.name, outcome.questions)
            }
        }
        return CourseGeneration(topics, usedAi)
    }

    private data class CourseGeneration(
        val topics: List<GeneratedTopic>,
        val usedAi: Boolean,
    )

    /** Descarga (solo si cambió), guarda la copia física y devuelve el texto extraído. */
    private suspend fun storeAndRead(
        course: ContentCourse,
        sectionName: String,
        file: ContentFile,
    ): Pair<ContentStore.StoredFile, String>? {
        val existing = store.manifest().courses.firstOrNull { it.id == course.id }
            ?.sections?.flatMap { it.files }
            ?.firstOrNull { it.sourceUrl == file.url }
        val bytes = if (existing != null && !ContentPaths.needsDownload(file, existing.sha256, existing.sizeBytes)) {
            val physical = store.physicalFile(existing.relativePath)
            if (physical.exists()) physical.readBytes() else downloadBytes(file)
        } else {
            downloadBytes(file)
        } ?: return null
        if (bytes.isEmpty()) return null
        val stored = store.storeDownloaded(course, sectionName, file, bytes) ?: return null
        val text = if (PdfTextExtractor.looksLikePdf(bytes)) {
            PdfTextExtractor.extract(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
        return stored to text
    }

    private suspend fun downloadBytes(file: ContentFile): ByteArray? = withContext(io) {
        val cookie = WebCookies.cookieFor(file.url)
        downloader.download(file.url, cookie)
    }

    private companion object {
        /** Límite global: si un portal se cuelga, se corta y se conserva el árbol parcial. */
        const val SYNC_TIMEOUT = 6 * 60_000L
        /** Margen para esperar al estudiante (login manual o confirmación de enlaces). */
        const val MAX_WATCHDOG_GRACE = 8
    }
}
