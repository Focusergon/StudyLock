package pe.edu.unjbg.studylock.domain.content

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generador que intenta usar el modelo local y, si no está listo o incumple las reglas, cae al
 * generador determinista offline. Nunca se queda sin preguntas por un fallo de la IA.
 */
class HybridQuestionGenerator(
    private val ai: QuestionAiProvider,
    /** El estudiante puede desactivar la IA en Ajustes; sin modelo siempre es determinista. */
    private val enabled: suspend () -> Boolean,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    data class Outcome(
        val questions: List<GeneratedQuestion>,
        /** True cuando al menos una pregunta viene del modelo validado. */
        val usedAi: Boolean,
    )

    suspend fun generate(
        text: String,
        topicName: String = "",
        maxQuestions: Int = 12,
    ): Outcome = withContext(io) {
        val deterministic = QuestionGenerator.generate(text, topicName, maxQuestions)
        if (maxQuestions <= 0) return@withContext Outcome(emptyList(), false)
        if (!enabled() || !ai.isAvailable()) return@withContext Outcome(deterministic, false)

        val result = runCatching {
            ai.generate(topicName.ifBlank { QuestionGenerator.DEFAULT_TOPIC }, text.take(MAX_EXCERPT), maxQuestions)
        }.getOrNull()
        val valid = result?.questions
            ?.filter { it.options.size == 4 && it.correctIndex in 0..3 && it.explanation.isNotBlank() }
            .orEmpty()

        if (valid.size < MIN_AI_QUESTIONS) return@withContext Outcome(deterministic, false)

        val prompts = valid.map { it.prompt.lowercase() }.toHashSet()
        val combined = valid + deterministic.filter { it.prompt.lowercase() !in prompts }
        Outcome(combined.take(maxQuestions), true)
    }

    /** Divide el material en temas y aplica el híbrido al texto de cada tema. */
    suspend fun topicsFrom(
        text: String,
        maxTopics: Int = 6,
        questionsPerTopic: Int = 8,
    ): List<GeneratedTopic> = withContext(io) {
        val sources = QuestionGenerator.splitTopics(text, maxTopics).ifEmpty {
            listOf(QuestionGenerator.DEFAULT_TOPIC to text)
        }
        sources.mapNotNull { (title, body) ->
            val outcome = generate(body, title, questionsPerTopic)
            if (outcome.questions.size < 2) null
            else GeneratedTopic(title.ifBlank { QuestionGenerator.DEFAULT_TOPIC }, outcome.questions)
        }
    }

    private companion object {
        const val MAX_EXCERPT = 6_000
        const val MIN_AI_QUESTIONS = 2
    }
}
