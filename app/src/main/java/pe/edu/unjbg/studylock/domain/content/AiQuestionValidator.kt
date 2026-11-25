package pe.edu.unjbg.studylock.domain.content

import com.google.gson.JsonObject
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import pe.edu.unjbg.studylock.domain.model.QuestionTier
import java.io.StringReader

/**
 * Valida la salida del modelo local antes de que entre en la base de datos.
 *
 * No se confía en que el modelo cumpla el formato: se exige JSON estricto, exactamente 4 opciones
 * únicas, un índice correcto válido, explicación no vacía y una cita que aparezca literalmente en el
 * material del estudiante. Si algo falla, la pregunta se descarta y el generador híbrido usa el
 * respaldo determinista.
 */
object AiQuestionValidator {

    data class Result(
        val questions: List<GeneratedQuestion> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    @Suppress("DEPRECATION") // JsonReader.isLenient sigue siendo la única forma de exigir JSON estricto.
    fun parse(json: String, sourceText: String, topicName: String, maxQuestions: Int): Result {
        if (json.isBlank() || maxQuestions <= 0) return Result()
        // JsonParser fuerza modo leniente; Streams.parse respeta isLenient=false y por tanto
        // rechaza claves sin comillas, comillas simples, comentarios y comas finales.
        val root = runCatching {
            val reader = JsonReader(StringReader(json)).apply { isLenient = false }
            val element = Streams.parse(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) error("Sobra texto después del JSON.")
            element.asJsonObject
        }.getOrElse { return Result(errors = listOf("El modelo no devolvió un JSON válido.")) }

        val array = runCatching { root.getAsJsonArray("questions") }
            .getOrElse { return Result(errors = listOf("Falta la lista «questions».")) }
            ?: return Result(errors = listOf("Falta la lista «questions»."))

        val source = normalize(sourceText)
        if (source.length < 12) return Result(errors = listOf("El material es demasiado corto para citarlo."))

        val errors = ArrayList<String>()
        val questions = ArrayList<GeneratedQuestion>()
        val seenPrompts = HashSet<String>()
        val key = conceptKey(topicName)

        for ((index, element) in array.withIndex()) {
            val item = runCatching { element.asJsonObject }
                .getOrElse {
                    errors += "Pregunta ${index + 1}: no es un objeto."
                    continue
                }
            val prompt = item.stringOrNull("prompt")?.trim()
            val options = runCatching {
                item.getAsJsonArray("options").map { it.takeIf { value -> value.isJsonPrimitive }?.asString?.trim().orEmpty() }
            }.getOrElse { emptyList() }
            val correctIndex = runCatching { item.getAsJsonObject("correct").get("index").asInt }
                .getOrElse { -1 }
            val explanation = item.stringOrNull("explanation")?.trim().orEmpty()
            val quote = item.stringOrNull("sourceQuote")?.trim().orEmpty()
            val tier = runCatching { QuestionTier.valueOf(item.stringOrNull("tier").orEmpty().uppercase()) }
                .getOrDefault(QuestionTier.CONCEPTO)

            val error = when {
                prompt.isNullOrBlank() -> "Pregunta ${index + 1}: enunciado vacío."
                options.size != 4 -> "Pregunta ${index + 1}: debe tener 4 opciones."
                options.any { it.isBlank() } -> "Pregunta ${index + 1}: hay opciones vacías."
                options.distinctBy { it.lowercase() }.size != 4 -> "Pregunta ${index + 1}: las opciones se repiten."
                correctIndex !in 0..3 -> "Pregunta ${index + 1}: índice correcto inválido."
                explanation.isBlank() -> "Pregunta ${index + 1}: explicación vacía."
                normalize(quote).length < 12 -> "Pregunta ${index + 1}: falta una cita suficiente."
                !source.contains(normalize(quote), ignoreCase = true) ->
                    "Pregunta ${index + 1}: la cita no aparece en el material original."
                !seenPrompts.add(prompt.lowercase()) -> "Pregunta ${index + 1}: enunciado repetido."
                else -> null
            }
            if (error != null) {
                errors += error
                continue
            }
            questions += GeneratedQuestion(
                prompt = prompt!!,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                difficulty = when (tier) {
                    QuestionTier.CONCEPTO -> 1
                    QuestionTier.PROCESO -> 3
                    QuestionTier.GENERAL -> 4
                },
                tier = tier,
                conceptKey = key,
                sourceQuote = quote,
            )
            if (questions.size >= maxQuestions) break
        }
        return Result(questions, errors)
    }

    /** Misma normalización para la cita y el material: solo se ignoran saltos/espacios. */
    private fun normalize(value: String): String =
        value.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()

    private fun JsonObject.stringOrNull(name: String): String? =
        runCatching { get(name)?.takeIf { it.isJsonPrimitive }?.asString }.getOrNull()

    /** Clave estable por tema: permite contar aciertos por concepto entre preguntas de la IA. */
    fun conceptKey(topicName: String): String = topicName.lowercase()
        .replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
        .trim()
        .replace(Regex("\\s+"), "-")
        .take(48)
        .ifBlank { "concepto" }
}
