package pe.edu.unjbg.studylock.domain.content

import kotlin.random.Random

/** Pregunta generada automáticamente a partir de material académico. */
data class GeneratedQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: Int = 1,
)

/** Tema detectado en un documento, con sus preguntas. */
data class GeneratedTopic(
    val name: String,
    val questions: List<GeneratedQuestion>,
)

/**
 * Generador de preguntas de opción múltiple a partir de texto (sílabo, apuntes, PDF).
 *
 * Es determinista y no depende de Android ni de red, así que se puede probar en JVM. La
 * estrategia es pedagógicamente simple pero efectiva para micro-repasos:
 *
 *  1. **Completar la palabra clave** (cloze): se toma una oración y se oculta su término más
 *     relevante; los distractores salen del propio documento.
 *  2. **Definición**: se detectan oraciones del tipo «X es Y» y se pregunta qué se afirma de X.
 *
 * En producción esto se puede sustituir por un generador con LLM sin tocar la UI: la interfaz
 * sigue siendo `List<GeneratedQuestion>`.
 */
object QuestionGenerator {

    private val STOPWORDS = setOf(
        "sobre", "entre", "desde", "hasta", "hacia", "según", "mientras", "porque", "cuando",
        "donde", "también", "puede", "pueden", "debe", "deben", "tiene", "tienen", "hace",
        "hacen", "sido", "estar", "este", "esta", "estos", "estas", "esos", "esas", "aquel",
        "aquella", "para", "como", "pero", "sino", "cada", "otro", "otra", "otros", "otras",
        "mismo", "misma", "todo", "toda", "todos", "todas", "muy", "más", "menos", "además",
        "aunque", "entonces", "través", "dentro", "fuera", "parte", "forma", "caso", "ejemplo",
        "manera", "hacer", "tanto", "según", "segundo", "primero", "durante", "según", "the",
        "and", "with", "from", "that", "this", "have", "will", "your", "their", "about",
        "which", "when", "there", "these", "those", "such", "into", "than", "then",
    )

    private val SENTENCE_SPLIT = Regex("(?<=[.!?…])\\s+|\\n+")
    private val WORD = Regex("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]{4,}")
    private val NUMBERED = Regex("^\\d+[.)]?\\s+")
    private val DEF = Regex(
        "^(.{4,70}?)\\s+(?:es|son|se define como|se refiere a|consiste en|significa)\\s+(.{12,160})$",
        RegexOption.IGNORE_CASE,
    )

    private data class Def(val subject: String, val predicate: String)

    /**
     * Genera hasta [maxQuestions] preguntas a partir de [text].
     *
     * @param topicName nombre del tema (se usa solo como contexto de la explicación).
     */
    fun generate(text: String, topicName: String = "", maxQuestions: Int = 12): List<GeneratedQuestion> {
        if (maxQuestions <= 0) return emptyList()
        val sentences = sentences(text)
        if (sentences.isEmpty()) return emptyList()
        val vocab = vocabulary(sentences)
        val defs = definitions(sentences)

        val questions = mutableListOf<GeneratedQuestion>()
        questions += definitionQuestions(defs)
        questions += sentences.mapNotNull { cloze(it, vocab, topicName) }

        return questions
            .distinctBy { it.prompt.lowercase() }
            .take(maxQuestions)
    }

    /**
     * Divide el documento en temas y genera preguntas por tema.
     *
     * Detecta encabezados (líneas cortas en Título, numeradas, EN MAYÚSCULAS o que terminan en
     * «:»). Si no encuentra ninguno, trata todo el documento como un único tema.
     */
    fun topicsFrom(
        text: String,
        maxTopics: Int = 6,
        questionsPerTopic: Int = 8,
    ): List<GeneratedTopic> {
        val sections = sections(text).take(maxTopics)
        val topics = sections.mapNotNull { (title, body) ->
            val questions = generate(body, title, questionsPerTopic)
            if (questions.size < 2) null else GeneratedTopic(title.ifBlank { DEFAULT_TOPIC }, questions)
        }
        if (topics.isNotEmpty()) return topics
        val fallback = generate(text, DEFAULT_TOPIC, questionsPerTopic * 2)
        return if (fallback.isEmpty()) emptyList() else listOf(GeneratedTopic(DEFAULT_TOPIC, fallback))
    }

    // --- Extracción ---------------------------------------------------------

    private fun sentences(text: String): List<String> =
        text.replace('\u00A0', ' ')
            .split(SENTENCE_SPLIT)
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length in 30..400 && it.count(Char::isLetter) >= 20 }

    private fun vocabulary(sentences: List<String>): List<String> {
        val freq = HashMap<String, Int>()
        val canonical = HashMap<String, String>()
        for (sentence in sentences) {
            for (match in WORD.findAll(sentence)) {
                val word = match.value
                val key = word.lowercase()
                if (word.length < 6 || key in STOPWORDS) continue
                freq[key] = (freq[key] ?: 0) + 1
                canonical.putIfAbsent(key, word)
            }
        }
        return freq.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .mapNotNull { canonical[it.key] }
    }

    private fun definitions(sentences: List<String>): List<Def> =
        sentences.mapNotNull { sentence ->
            val match = DEF.find(sentence) ?: return@mapNotNull null
            val subject = match.groupValues[1].trim().trim('«', '»', '"', '\'', '.', ',', ';', ':')
            val predicate = match.groupValues[2].trim().trimEnd('.', ';', ',')
            if (subject.length < 4 || predicate.length !in 12..90) return@mapNotNull null
            Def(subject, predicate)
        }.distinctBy { it.subject.lowercase() }

    // --- Generadores --------------------------------------------------------

    private fun cloze(sentence: String, vocab: List<String>, topicName: String): GeneratedQuestion? {
        val words = WORD.findAll(sentence).map { it.value }.toList()
        val key = words
            .filter { it.length >= 6 && it.lowercase() !in STOPWORDS }
            .maxByOrNull { it.length }
            ?: return null
        if (key.length < 6) return null

        val prompt = replaceFirstWord(sentence, key) { "_____" }
        if (!prompt.contains("_____")) return null

        val lowerSentence = sentence.lowercase()
        val nearby = vocab.filter {
            it.lowercase() != key.lowercase() &&
                it.lowercase() !in lowerSentence &&
                it.length in (key.length - 3).coerceAtLeast(4)..(key.length + 6)
        }
        val distractors = (if (nearby.size >= 3) nearby else vocab.filter {
            it.lowercase() != key.lowercase() && it.lowercase() !in lowerSentence
        }).distinctBy { it.lowercase() }.take(3)

        if (distractors.size < 3) return null
        return build(prompt, key, distractors, topicName, "La palabra que completa la idea es «$key».")
    }

    private fun definitionQuestions(defs: List<Def>): List<GeneratedQuestion> {
        if (defs.size < 4) return emptyList()
        return defs.mapNotNull { def ->
            val distractors = defs.asSequence()
                .filter { it.subject.lowercase() != def.subject.lowercase() }
                .map { it.predicate }
                .filter { !it.equals(def.predicate, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(3)
                .toList()
            if (distractors.size < 3) return@mapNotNull null
            val prompt = "Según el material, ¿qué se afirma sobre «${def.subject}»?"
            build(prompt, def.predicate, distractors, "", "«${def.subject}» ${def.predicate}.")
        }
    }

    private fun build(
        prompt: String,
        correct: String,
        distractors: List<String>,
        topicName: String,
        explanation: String,
    ): GeneratedQuestion? {
        val options = (listOf(correct) + distractors).distinctBy { it.lowercase() }
        if (options.size < 4) return null
        val shuffled = options.shuffled(Random(prompt.hashCode()))
        val correctIndex = shuffled.indexOfFirst { it.equals(correct, ignoreCase = true) }
        if (correctIndex < 0) return null
        val full = if (topicName.isBlank()) explanation else "$explanation (${topicName})"
        return GeneratedQuestion(
            prompt = prompt,
            options = shuffled,
            correctIndex = correctIndex,
            explanation = full,
            difficulty = if (prompt.contains("_____")) 1 else 2,
        )
    }

    private fun replaceFirstWord(text: String, word: String, replacement: () -> String): String {
        val pattern = Regex("(?<![\\p{L}])${Regex.escape(word)}(?![\\p{L}])", RegexOption.IGNORE_CASE)
        return pattern.replaceFirst(text, replacement())
    }

    // --- Secciones ----------------------------------------------------------

    private fun sections(text: String): List<Pair<String, String>> {
        val lines = text.replace('\u00A0', ' ').split('\n').map { it.trim() }
        val result = mutableListOf<Pair<String, String>>()
        var title: String? = null
        val body = StringBuilder()

        fun flush() {
            val current = title
            if (current != null && body.toString().trim().length >= 120) {
                result += current to body.toString().trim()
            }
            body.clear()
        }

        for (line in lines) {
            when {
                line.isEmpty() -> body.append('\n')
                isHeading(line) -> {
                    flush()
                    title = line.trimStart('•', '-', '*', '#', ' ').trim().trimEnd(':', '.')
                }
                else -> {
                    if (title == null) title = ""
                    body.append(line).append('\n')
                }
            }
        }
        flush()
        return result
    }

    private fun isHeading(line: String): Boolean {
        val trimmed = line.trim().trimStart('•', '-', '*', '#', ' ').trim()
        if (trimmed.length !in 3..70) return false
        if (trimmed.endsWith('.') || trimmed.endsWith(',') || trimmed.endsWith(';')) return false
        if (trimmed.count(Char::isLetter) < 3) return false
        if (NUMBERED.containsMatchIn(trimmed)) return true
        val letters = trimmed.filter(Char::isLetter)
        if (letters.isNotEmpty() && letters.all(Char::isUpperCase)) return true
        if (line.trimEnd().endsWith(":")) return true
        val words = trimmed.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 8) return false
        val capitalized = words.count { it.firstOrNull()?.isUpperCase() == true }
        return capitalized >= (words.size + 1) / 2
    }

    const val DEFAULT_TOPIC = "Contenido importado"
}
