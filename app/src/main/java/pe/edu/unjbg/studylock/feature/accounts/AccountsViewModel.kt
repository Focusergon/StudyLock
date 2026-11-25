package pe.edu.unjbg.studylock.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.text.PdfTextExtractor
import pe.edu.unjbg.studylock.data.auth.CredentialStore
import pe.edu.unjbg.studylock.data.content.ContentStore
import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.data.web.AulaExtractor
import pe.edu.unjbg.studylock.data.web.ExtractionResult
import pe.edu.unjbg.studylock.data.web.FileDownloader
import pe.edu.unjbg.studylock.data.web.IntranetExtractor
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession
import pe.edu.unjbg.studylock.data.web.WebCookies
import pe.edu.unjbg.studylock.data.web.WebLink
import pe.edu.unjbg.studylock.data.content.ContentPaths
import pe.edu.unjbg.studylock.domain.content.ContentMerger
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.content.HybridQuestionGenerator
import pe.edu.unjbg.studylock.domain.model.ContentCourse
import pe.edu.unjbg.studylock.domain.model.ContentFile
import pe.edu.unjbg.studylock.domain.model.ContentSource
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind
import pe.edu.unjbg.studylock.domain.repo.ContentRepository

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
) {
    /** Se exige el Aula Virtual; la Intranet se deriva de ella si se marca «misma contraseña». */
    val canSave: Boolean
        get() = aulaUser.isNotBlank() && aulaPassword.isNotBlank() &&
            (samePassword || intranetPassword.isNotBlank())

    val canSync: Boolean get() = canSave && !syncing && !saving

    val hasTree: Boolean get() = tree.any { it.files.isNotEmpty() }

    val selectedCount: Int get() = selected.size

    val importableSelected: Int
        get() = tree.flatMap { it.files }.count { it.url in selected && it.isImportable }

    val canGenerate: Boolean get() = importableSelected > 0 && !importing && !syncing
}

/** Valores por defecto de los portales de la UNJBG (editables desde la pantalla). */
object AccountsDefaults {
    const val AULA_URL = "https://aulavirtual.unjbg.edu.pe"
    const val INTRANET_URL = "https://intranet.unjbg.edu.pe"
}

/**
 * Orquesta la vinculación con la UNJBG: guarda las credenciales cifradas, conduce el mini-navegador
 * para descubrir el contenido y convierte lo elegido en preguntas.
 *
 * El `WebView` lo aporta la UI (Compose) a través de [attachSession]; el ViewModel nunca lo crea ni
 * lo retiene más de lo necesario.
 */
class AccountsViewModel(
    private val credentials: CredentialStore,
    private val content: ContentRepository,
    private val store: ContentStore,
    private val generator: HybridQuestionGenerator,
    private val downloader: FileDownloader = FileDownloader(),
    /** Auto-sync diario: la navegación lo activa después de comprobar pref, Wi-Fi y fecha. */
    private val autoSync: Boolean = false,
) : ViewModel() {

    /** Sílabos sin curso del Aula: se archivan en `agregados/` al generar. */
    private var unmatchedSyllabi: List<ContentCourse> = emptyList()

    private val _ui = MutableStateFlow(AccountsUi())
    val ui: StateFlow<AccountsUi> = _ui.asStateFlow()

    private var sessionReady: CompletableDeferred<UnjbgWebSession>? = null
    private var session: UnjbgWebSession? = null

    init {
        viewModelScope.launch {
            val stored = credentials.current()
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
            // Sin WebView todavía no se puede sincronizar: la UI crea el panel en cuanto
            // `browserVisible` pasa a true y entonces entrega la sesión a `attachSession`.
            if (autoSync && stored.hasAula) sync()
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

    /** La UI entrega el WebView listo para usarse. */
    fun attachSession(webSession: UnjbgWebSession) {
        session = webSession
        sessionReady?.complete(webSession)
    }

    /** La UI destruyó el WebView. */
    fun detachSession() {
        session = null
    }

    /** Inicia sesión en Aula e Intranet, recorre el contenido y arma el árbol. */
    fun sync() {
        val state = _ui.value
        if (!state.canSave) {
            _ui.update { it.copy(error = "Escribe tu usuario y contraseña del Aula Virtual.") }
            return
        }
        if (state.syncing) return

        val ready = CompletableDeferred<UnjbgWebSession>()
        sessionReady = ready
        _ui.update {
            it.copy(
                syncing = true,
                error = null,
                resultMessage = null,
                tree = emptyList(),
                selected = emptySet(),
                browserVisible = true,
                notices = emptyList(),
                detectedLinks = emptyList(),
                showLinks = false,
                status = "Preparando el navegador…",
            )
        }

        viewModelScope.launch {
            val creds = currentCredentials()
            runCatching { credentials.save(creds) }
            val status: (String) -> Unit = { message -> _ui.update { it.copy(status = message) } }

            val webSession = ready.await()
            val aula = runCatching { AulaExtractor(webSession, state.aulaUrl).sync(creds, status) }
                .getOrDefault(ExtractionResult(notice = "No se pudo leer el Aula Virtual."))
            val intranet = if (creds.hasIntranet) {
                runCatching { IntranetExtractor(webSession, state.intranetUrl).sync(creds, status) }
                    .getOrDefault(ExtractionResult(notice = "No se pudo leer la Intranet."))
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
            val autoSelected = tree.flatMap { course -> course.files.filter { it.isImportable }.map { it.url } }.toSet()
            _ui.update {
                it.copy(
                    syncing = false,
                    browserVisible = false,
                    status = "",
                    tree = tree,
                    selected = autoSelected,
                    notices = notices,
                    detectedLinks = links,
                    // Sin contenido no mostramos un error genérico: el aviso ya dice qué pasó.
                    error = null,
                )
            }
        }
    }

    // --- Selección del árbol ------------------------------------------------

    fun toggleFile(url: String) = _ui.update {
        val next = if (url in it.selected) it.selected - url else it.selected + url
        it.copy(selected = next)
    }

    fun selectAll() = _ui.update { state ->
        state.copy(selected = state.tree.flatMap { it.files }.map { it.url }.toSet())
    }

    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }

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
        viewModelScope.launch {
            WebCookies.clear()
            runCatching { credentials.clear() }
            _ui.update {
                it.copy(
                    aulaPassword = "",
                    intranetPassword = "",
                    tree = emptyList(),
                    selected = emptySet(),
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

    private suspend fun downloadBytes(file: ContentFile): ByteArray? {
        val cookie = WebCookies.cookieFor(file.url)
        return downloader.download(file.url, cookie)
    }
}
