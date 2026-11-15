package pe.edu.unjbg.studylock.feature.import

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.text.PdfTextExtractor
import pe.edu.unjbg.studylock.data.aula.AulaCourse
import pe.edu.unjbg.studylock.data.aula.AulaSite
import pe.edu.unjbg.studylock.data.aula.AulaVirtualClient
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.content.QuestionGenerator
import pe.edu.unjbg.studylock.domain.model.ImportResult
import pe.edu.unjbg.studylock.domain.model.ImportSource
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind
import pe.edu.unjbg.studylock.domain.repo.ContentRepository

/**
 * Estado de la pantalla de importación.
 *
 * Cubre las tres vías de entrada de contenido: pegar texto, elegir un archivo (.txt/.md/.pdf) y
 * conectar el Aula Virtual (Moodle). El resultado siempre es una lista de [GeneratedTopic] lista
 * para previsualizar y guardar como curso.
 */
data class ImportUi(
    val courseName: String = "",
    val professor: String = "",
    val text: String = "",
    val generating: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val topics: List<GeneratedTopic> = emptyList(),
    val sourceTitle: String = "",
    val kind: ImportSourceKind = ImportSourceKind.TEXT,
    val result: ImportResult? = null,
    val sources: List<ImportSource> = emptyList(),
    // Aula Virtual
    val aulaUrl: String = DEFAULT_AULA_URL,
    val aulaToken: String = "",
    val connecting: Boolean = false,
    val site: AulaSite? = null,
    val courses: List<AulaCourse> = emptyList(),
    val importingCourseId: Int? = null,
) {
    val questionCount: Int get() = topics.sumOf { it.questions.size }
    val canSave: Boolean get() = topics.isNotEmpty() && courseName.isNotBlank() && !saving
    val connected: Boolean get() = site != null

    companion object {
        const val DEFAULT_AULA_URL = "https://aulavirtual.unjbg.edu.pe"
    }
}

/**
 * Orquesta la importación de material académico.
 *
 * Todo el trabajo pesado (extracción de PDF y generación de preguntas) corre fuera del hilo
 * principal; el Aula Virtual se consulta en [Dispatchers.IO].
 */
class ImportViewModel(
    private val context: Context,
    private val content: ContentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ImportUi())
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            content.observeSources().collect { list -> _ui.update { it.copy(sources = list) } }
        }
    }

    fun setCourseName(value: String) = _ui.update { it.copy(courseName = value, error = null) }
    fun setProfessor(value: String) = _ui.update { it.copy(professor = value) }
    fun setText(value: String) = _ui.update { it.copy(text = value) }
    fun setAulaUrl(value: String) = _ui.update { it.copy(aulaUrl = value) }
    fun setAulaToken(value: String) = _ui.update { it.copy(aulaToken = value) }
    fun dismissError() = _ui.update { it.copy(error = null) }
    fun dismissResult() = _ui.update { it.copy(result = null) }

    /** Genera preguntas a partir del texto pegado y las deja listas para previsualizar. */
    fun generateFromText() {
        val text = _ui.value.text
        if (text.trim().length < MIN_TEXT) {
            _ui.update { it.copy(error = "Pega al menos $MIN_TEXT caracteres de tu sílabo o apuntes.") }
            return
        }
        _ui.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val topics = withContext(Dispatchers.Default) { QuestionGenerator.topicsFrom(text) }
            if (topics.isEmpty()) {
                _ui.update {
                    it.copy(
                        generating = false,
                        error = "No se pudieron extraer preguntas. Prueba con un texto más largo y con oraciones completas.",
                    )
                }
            } else {
                _ui.update {
                    it.copy(
                        generating = false,
                        topics = topics,
                        sourceTitle = "Texto pegado",
                        kind = ImportSourceKind.TEXT,
                    )
                }
            }
        }
    }

    /** Procesa un archivo (.txt, .md o .pdf) elegido por el estudiante. */
    fun importFile(uri: Uri) {
        _ui.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) { readFile(uri) }
            if (parsed == null) {
                _ui.update { it.copy(generating = false, error = "No se pudo leer el archivo seleccionado.") }
                return@launch
            }
            val (name, body) = parsed
            val topics = withContext(Dispatchers.Default) { QuestionGenerator.topicsFrom(body) }
            if (topics.isEmpty()) {
                _ui.update {
                    it.copy(generating = false, error = "El archivo no contiene suficiente texto para generar preguntas.")
                }
            } else {
                _ui.update {
                    it.copy(
                        generating = false,
                        topics = topics,
                        sourceTitle = name,
                        kind = ImportSourceKind.FILE,
                        courseName = it.courseName.ifBlank { name.substringBeforeLast('.').take(48) },
                    )
                }
            }
        }
    }

    /** Guarda los temas generados como un curso nuevo. */
    fun save() {
        val state = _ui.value
        if (state.topics.isEmpty()) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = content.importCourse(
                courseName = state.courseName.ifBlank { "Curso importado" },
                professor = state.professor,
                topics = state.topics,
                kind = state.kind,
                sourceTitle = state.sourceTitle,
            )
            _ui.update {
                it.copy(
                    saving = false,
                    result = result,
                    topics = emptyList(),
                    text = "",
                    courseName = "",
                    professor = "",
                )
            }
        }
    }

    // --- Aula Virtual -------------------------------------------------------

    fun connectAula() {
        val state = _ui.value
        if (state.aulaToken.isBlank()) {
            _ui.update { it.copy(error = "Pega la clave (token) del Aula Virtual.") }
            return
        }
        _ui.update { it.copy(connecting = true, error = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val client = AulaVirtualClient(state.aulaUrl, state.aulaToken.trim())
                    val site = client.validate()
                    site to client.courses()
                }
            }
            outcome.fold(
                onSuccess = { (site, courses) ->
                    _ui.update { it.copy(connecting = false, site = site, courses = courses, error = null) }
                },
                onFailure = { error ->
                    _ui.update {
                        it.copy(
                            connecting = false,
                            error = error.message ?: "No se pudo conectar con el Aula Virtual.",
                        )
                    }
                },
            )
        }
    }

    fun disconnectAula() = _ui.update { it.copy(site = null, courses = emptyList(), aulaToken = "") }

    /** Descarga los documentos de un curso del Aula Virtual y genera sus preguntas. */
    fun importAulaCourse(course: AulaCourse) {
        val state = _ui.value
        _ui.update { it.copy(importingCourseId = course.id, error = null, result = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { buildAulaTopics(state.aulaUrl, state.aulaToken.trim(), course) }
            }
            outcome.fold(
                onSuccess = { topics ->
                    if (topics.isEmpty()) {
                        _ui.update {
                            it.copy(
                                importingCourseId = null,
                                error = "El curso «${course.fullName}» no tiene documentos de texto o PDF para procesar.",
                            )
                        }
                    } else {
                        val result = content.importCourse(
                            courseName = course.fullName,
                            professor = state.site?.siteName.orEmpty(),
                            topics = topics,
                            kind = ImportSourceKind.MOODLE,
                            sourceTitle = "Aula Virtual · ${course.shortName.ifBlank { course.fullName }}",
                        )
                        _ui.update { it.copy(importingCourseId = null, result = result) }
                    }
                },
                onFailure = { error ->
                    _ui.update {
                        it.copy(importingCourseId = null, error = error.message ?: "No se pudo importar el curso.")
                    }
                },
            )
        }
    }

    fun deleteSource(id: String) {
        viewModelScope.launch { content.deleteSource(id) }
    }

    private fun buildAulaTopics(url: String, token: String, course: AulaCourse): List<GeneratedTopic> {
        val client = AulaVirtualClient(url, token)
        val sections = client.sections(course.id)
        val topics = ArrayList<GeneratedTopic>()
        for (section in sections) {
            val body = StringBuilder()
            for (file in section.files) {
                val text = downloadText(client, file.url) ?: continue
                if (text.isNotBlank()) body.append(text).append("\n\n")
            }
            if (body.isBlank()) continue
            val generated = QuestionGenerator.generate(body.toString(), section.name, maxQuestions = 8)
            if (generated.size >= 2) topics += GeneratedTopic(section.name, generated)
        }
        if (topics.isEmpty()) {
            // Sin secciones útiles: se trata todo el curso como un único tema.
            val whole = sections.flatMap { it.files }
                .joinToString("\n\n") { downloadText(client, it.url).orEmpty() }
            val generated = QuestionGenerator.generate(whole, course.fullName, maxQuestions = 12)
            if (generated.size >= 2) topics += GeneratedTopic(course.fullName, generated)
        }
        return topics
    }

    private fun downloadText(client: AulaVirtualClient, url: String): String? {
        val bytes = runCatching { client.download(url) }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        return if (PdfTextExtractor.looksLikePdf(bytes)) {
            PdfTextExtractor.extract(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun readFile(uri: Uri): Pair<String, String>? {
        val resolver = context.contentResolver
        val name = queryDisplayName(uri) ?: "documento"
        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        val text = if (PdfTextExtractor.looksLikePdf(bytes)) {
            PdfTextExtractor.extract(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
        return name to text
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    private companion object {
        const val MIN_TEXT = 240
    }
}
