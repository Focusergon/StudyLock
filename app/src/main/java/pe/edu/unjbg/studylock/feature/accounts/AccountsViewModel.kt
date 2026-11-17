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
import pe.edu.unjbg.studylock.data.auth.UnjbgCredentials
import pe.edu.unjbg.studylock.data.web.AulaExtractor
import pe.edu.unjbg.studylock.data.web.FileDownloader
import pe.edu.unjbg.studylock.data.web.IntranetExtractor
import pe.edu.unjbg.studylock.data.web.UnjbgWebSession
import pe.edu.unjbg.studylock.data.web.WebCookies
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.content.QuestionGenerator
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
    private val downloader: FileDownloader = FileDownloader(),
) : ViewModel() {

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
                )
            }
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
                status = "Preparando el navegador…",
            )
        }

        viewModelScope.launch {
            val creds = currentCredentials()
            runCatching { credentials.save(creds) }
            val status: (String) -> Unit = { message -> _ui.update { it.copy(status = message) } }

            val webSession = ready.await()
            val aula = runCatching { AulaExtractor(webSession, state.aulaUrl).sync(creds, status) }
                .getOrDefault(emptyList())
            val intranet = if (creds.hasIntranet) {
                runCatching { IntranetExtractor(webSession, state.intranetUrl).sync(creds, status) }
                    .getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val tree = aula + intranet
            val autoSelected = tree.flatMap { course -> course.files.filter { it.isImportable }.map { it.url } }.toSet()
            _ui.update {
                it.copy(
                    syncing = false,
                    browserVisible = false,
                    status = "",
                    tree = tree,
                    selected = autoSelected,
                    error = if (tree.none { course -> course.files.isNotEmpty() }) {
                        "No encontramos archivos. Verifica tus credenciales e inténtalo de nuevo."
                    } else {
                        null
                    },
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

            for (course in state.tree) {
                val topics = withContext(Dispatchers.Default) { generateFor(course, state.selected) }
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

            _ui.update {
                if (importedCourses == 0) {
                    it.copy(
                        importing = false,
                        error = "No se pudieron extraer preguntas del material seleccionado. Prueba con otro archivo.",
                    )
                } else {
                    it.copy(
                        importing = false,
                        resultMessage = "Listo: $importedCourses curso(s) y $totalQuestions preguntas.",
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
    )

    private suspend fun generateFor(course: ContentCourse, selected: Set<String>): List<GeneratedTopic> {
        val topics = ArrayList<GeneratedTopic>()
        for (section in course.sections) {
            val body = StringBuilder()
            for (file in section.files) {
                if (file.url !in selected) continue
                val text = downloadText(file) ?: continue
                if (text.isNotBlank()) body.append(text).append("\n\n")
            }
            if (body.isBlank()) continue
            val generated = QuestionGenerator.generate(body.toString(), section.name, maxQuestions = 8)
            if (generated.size >= 2) topics += GeneratedTopic(section.name, generated)
        }
        if (topics.isEmpty()) {
            val whole = StringBuilder()
            for (file in course.files) {
                if (file.url !in selected) continue
                val text = downloadText(file) ?: continue
                if (text.isNotBlank()) whole.append(text).append("\n\n")
            }
            if (whole.isNotBlank()) {
                val generated = QuestionGenerator.generate(whole.toString(), course.name, maxQuestions = 12)
                if (generated.size >= 2) topics += GeneratedTopic(course.name, generated)
            }
        }
        return topics
    }

    private suspend fun downloadText(file: ContentFile): String? {
        val cookie = WebCookies.cookieFor(file.url)
        val bytes = downloader.download(file.url, cookie) ?: return null
        if (bytes.isEmpty()) return null
        return if (PdfTextExtractor.looksLikePdf(bytes)) {
            PdfTextExtractor.extract(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }
}
