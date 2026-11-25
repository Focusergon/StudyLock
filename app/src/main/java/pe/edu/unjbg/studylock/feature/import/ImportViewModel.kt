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
import pe.edu.unjbg.studylock.data.content.ContentStore
import pe.edu.unjbg.studylock.domain.content.CourseSuggester
import pe.edu.unjbg.studylock.domain.content.GeneratedTopic
import pe.edu.unjbg.studylock.domain.content.HybridQuestionGenerator
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.ImportResult
import pe.edu.unjbg.studylock.domain.model.ImportSource
import pe.edu.unjbg.studylock.domain.model.ImportSourceKind
import pe.edu.unjbg.studylock.domain.repo.ContentRepository
import pe.edu.unjbg.studylock.domain.repo.CourseRepository

/** Estado de la pantalla de importación. */
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
    val courses: List<Course> = emptyList(),
    /** Archivo elegido, esperando confirmación de categoría y curso. */
    val pendingFile: String? = null,
    val suggestedCourse: String? = null,
    val selectedCourseId: String? = null,
    val categories: List<String> = DEFAULT_CATEGORIES,
    val selectedCategory: String = DEFAULT_CATEGORIES.first(),
    val customCategory: String = "",
    /** Diagnóstico: alguna pregunta vino del modelo validado (el respaldo sigue disponible). */
    val usedAi: Boolean = false,
) {
    val questionCount: Int get() = topics.sumOf { it.questions.size }
    val canSave: Boolean get() = topics.isNotEmpty() && courseName.isNotBlank() && !saving
    val needsUploadConfirmation: Boolean get() = pendingFile != null
}

private val DEFAULT_CATEGORIES = listOf("Apuntes", "Sílabo", "Práctica", "Examen", "Tarea")

/**
 * Orquesta la importación manual de material académico.
 *
 * Los archivos subidos se **copian físicamente** al almacén de la app (nunca se conserva solo la URI
 * `content://`), se etiquetan con chips/categorías (se puede crear una nueva) y se les sugiere un
 * curso por similitud, siempre con confirmación. «Sin curso» los deja en `agregados/`.
 */
class ImportViewModel(
    private val context: Context,
    private val content: ContentRepository,
    private val coursesRepo: CourseRepository,
    private val store: ContentStore,
    private val generator: HybridQuestionGenerator,
) : ViewModel() {

    private val _ui = MutableStateFlow(ImportUi())
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    /** Bytes del archivo pendiente: se conservan aquí, no en el estado Compose. */
    private var pendingBytes: ByteArray? = null
    private var pendingName: String = ""

    init {
        viewModelScope.launch {
            content.observeSources().collect { list -> _ui.update { it.copy(sources = list) } }
        }
        viewModelScope.launch {
            coursesRepo.observeCourses().collect { list -> _ui.update { it.copy(courses = list) } }
        }
    }

    fun setCourseName(value: String) = _ui.update { it.copy(courseName = value, error = null) }
    fun setProfessor(value: String) = _ui.update { it.copy(professor = value) }
    fun setText(value: String) = _ui.update { it.copy(text = value) }
    fun dismissError() = _ui.update { it.copy(error = null) }
    fun dismissResult() = _ui.update { it.copy(result = null) }

    fun setCategory(value: String) = _ui.update { it.copy(selectedCategory = value, customCategory = "") }
    fun setCustomCategory(value: String) = _ui.update { it.copy(customCategory = value, selectedCategory = "") }
    fun setCourse(id: String?) = _ui.update { it.copy(selectedCourseId = id) }

    /** Añade una categoría nueva escrita por el estudiante y la deja seleccionada. */
    fun createCategory() {
        val value = _ui.value.customCategory.trim()
        if (value.isBlank()) return
        _ui.update {
            val exists = it.categories.any { name -> name.equals(value, ignoreCase = true) }
            it.copy(
                categories = if (exists) it.categories else it.categories + value,
                selectedCategory = value,
                customCategory = "",
            )
        }
    }

    fun cancelUpload() {
        pendingBytes = null
        pendingName = ""
        _ui.update {
            it.copy(
                pendingFile = null,
                suggestedCourse = null,
                selectedCourseId = null,
                error = null,
            )
        }
    }

    /** Genera preguntas a partir del texto pegado y las deja listas para previsualizar. */
    fun generateFromText() {
        val text = _ui.value.text
        if (text.trim().length < MIN_TEXT) {
            _ui.update { it.copy(error = "Pega al menos $MIN_TEXT caracteres de tu sílabo o apuntes.") }
            return
        }
        _ui.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val topics = generator.topicsFrom(text)
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
                        usedAi = topics.any { topic -> topic.questions.any { it.sourceQuote.isNotBlank() } },
                    )
                }
            }
        }
    }

    /**
     * Lee el archivo elegido y prepara la confirmación: categoría + curso sugerido.
     * Todavía no se guarda nada ni se generan preguntas.
     */
    fun importFile(uri: Uri) {
        _ui.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) { readFile(uri) }
            if (parsed == null) {
                _ui.update { it.copy(generating = false, error = "No se pudo leer el archivo seleccionado.") }
                return@launch
            }
            val (name, bytes, _) = parsed
            pendingBytes = bytes
            pendingName = name
            val names = _ui.value.courses.map { it.name }
            val suggestion = CourseSuggester.suggest(name, names)
            _ui.update {
                it.copy(
                    generating = false,
                    pendingFile = name,
                    suggestedCourse = suggestion,
                    selectedCourseId = suggestion?.let { suggested -> names.indexOf(suggested) }
                        ?.let { index -> _ui.value.courses.getOrNull(index)?.id },
                    courseName = it.courseName.ifBlank { name.substringBeforeLast('.').take(48) },
                )
            }
        }
    }

    /** Confirma la categoría/curso, guarda la copia física y genera las preguntas. */
    fun confirmUpload() {
        val bytes = pendingBytes
        if (bytes == null || _ui.value.pendingFile == null) {
            _ui.update { it.copy(error = "Selecciona un archivo primero.") }
            return
        }
        _ui.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val state = _ui.value
            val category = state.selectedCategory.ifBlank { "General" }
            val name = pendingName
            val course = state.courses.firstOrNull { it.id == state.selectedCourseId }

            val stored = withContext(Dispatchers.IO) {
                if (course != null) {
                    store.storeForCourse(course.name, name, bytes)
                } else {
                    store.storeAdded(name, bytes, category = category, suggestedCourse = state.suggestedCourse.orEmpty())
                }
            }
            if (stored == null) {
                _ui.update { it.copy(generating = false, error = "No se pudo guardar una copia del archivo.") }
                return@launch
            }

            val body = withContext(Dispatchers.Default) {
                if (PdfTextExtractor.looksLikePdf(bytes)) PdfTextExtractor.extract(bytes)
                else String(bytes, Charsets.UTF_8)
            }
            val topics = generator.topicsFrom(body)
            if (topics.isEmpty()) {
                _ui.update {
                    it.copy(
                        generating = false,
                        error = "El archivo no contiene suficiente texto para generar preguntas. " +
                            "La copia física sí se guardó en ${stored.relativePath}.",
                    )
                }
                return@launch
            }

            pendingBytes = null
            pendingName = ""
            _ui.update {
                it.copy(
                    generating = false,
                    topics = topics,
                    sourceTitle = name,
                    kind = ImportSourceKind.FILE,
                    courseName = course?.name ?: it.courseName,
                    usedAi = topics.any { topic -> topic.questions.any { question -> question.sourceQuote.isNotBlank() } },
                    pendingFile = null,
                    suggestedCourse = null,
                    selectedCourseId = null,
                )
            }
        }
    }

    /** Guarda los temas generados, en el curso elegido o como nuevo. */
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

    fun deleteSource(id: String) {
        viewModelScope.launch { content.deleteSource(id) }
    }

    private fun readFile(uri: Uri): Triple<String, ByteArray, String>? {
        val resolver = context.contentResolver
        val name = queryDisplayName(uri) ?: "documento"
        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        val text = if (PdfTextExtractor.looksLikePdf(bytes)) {
            PdfTextExtractor.extract(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
        return Triple(name, bytes, text)
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
