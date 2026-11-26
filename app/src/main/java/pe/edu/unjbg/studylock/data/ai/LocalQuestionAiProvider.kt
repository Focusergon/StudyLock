package pe.edu.unjbg.studylock.data.ai

import pe.edu.unjbg.studylock.domain.content.AiGeneration
import pe.edu.unjbg.studylock.domain.content.AiQuestionValidator
import pe.edu.unjbg.studylock.domain.content.QuestionAiProvider

/**
 * Proveedor de preguntas con Gemma 3 270M ejecutado en el propio teléfono (LiteRT-LM, CPU).
 *
 * No envía el material a ningún servidor. Usa el [LocalAiEngine] compartido con el clasificador de
 * enlaces y toda la salida pasa por [AiQuestionValidator]; si el modelo falla o inventa una cita, el
 * generador híbrido usa el respaldo determinista y la experiencia del estudiante no se interrumpe.
 */
class LocalQuestionAiProvider(
    private val engine: LocalAiEngine,
) : QuestionAiProvider {

    override fun isAvailable(): Boolean = engine.isAvailable()

    override suspend fun generate(
        topic: String,
        excerpt: String,
        count: Int,
    ): AiGeneration {
        if (!isAvailable()) {
            return AiGeneration(error = "El modelo todavía no está descargado.")
        }
        if (excerpt.length < MIN_EXCERPT) {
            return AiGeneration(error = "El material es demasiado corto para la IA.")
        }

        val json = engine.generateJson(
            LocalAiEngine.Request(
                systemInstruction = SYSTEM_INSTRUCTION,
                prompt = buildPrompt(topic, excerpt, count),
                schema = RESPONSE_SCHEMA,
                maxOutputTokens = (count * 110).coerceIn(400, 1_800),
            )
        )
        val parsed = AiQuestionValidator.parse(json, excerpt, topic, count)
        return if (parsed.questions.isEmpty()) {
            AiGeneration(error = parsed.errors.firstOrNull() ?: "El modelo no devolvió preguntas válidas.")
        } else {
            AiGeneration(parsed.questions)
        }
    }

    private fun buildPrompt(topic: String, excerpt: String, count: Int): String = """
        Genera hasta $count preguntas de opción múltiple sobre el tema «$topic».
        Devuelve SOLO un objeto JSON con esta forma exacta:
        {"questions":[{"prompt":"...","options":["a","b","c","d"],"correct":{"index":0},"explanation":"...","sourceQuote":"...","tier":"CONCEPTO"}]}

        Reglas obligatorias:
        - 4 opciones distintas; "correct.index" entre 0 y 3.
        - "sourceQuote" DEBE ser una copia literal de un fragmento del MATERIAL. No lo inventes ni lo parafrasees.
        - "explanation" no vacía y basada en esa cita.
        - "tier" es CONCEPTO (definiciones), PROCESO (pasos/flujos) o GENERAL (relaciones entre temas).
        - Empieza por CONCEPTO. Si pides varias, incluye al menos un PROCESO cuando el MATERIAL lo permita.
        - No inventes datos fuera del MATERIAL. Si no hay suficiente información, devuelve menos preguntas.
        - Sin markdown, sin comentarios y sin texto antes o después del JSON.

        MATERIAL:
        ${excerpt.take(MAX_EXCERPT)}
    """.trimIndent()

    private companion object {
        const val MIN_EXCERPT = 80
        const val MAX_EXCERPT = 6_000
        const val SYSTEM_INSTRUCTION =
            "Eres un profesor universitario que crea micro-preguntas de repaso. Respondes únicamente con JSON válido."
        const val RESPONSE_SCHEMA = """
            {
              "type": "OBJECT",
              "properties": {
                "questions": {
                  "type": "ARRAY",
                  "items": {
                    "type": "OBJECT",
                    "properties": {
                      "prompt": { "type": "STRING" },
                      "options": { "type": "ARRAY", "items": { "type": "STRING" }, "minItems": 4, "maxItems": 4 },
                      "correct": {
                        "type": "OBJECT",
                        "properties": { "index": { "type": "INTEGER", "minimum": 0, "maximum": 3 } },
                        "required": ["index"]
                      },
                      "explanation": { "type": "STRING" },
                      "sourceQuote": { "type": "STRING" },
                      "tier": { "type": "STRING", "enum": ["CONCEPTO", "PROCESO", "GENERAL"] }
                    },
                    "required": ["prompt", "options", "correct", "explanation", "sourceQuote", "tier"]
                  }
                }
              },
              "required": ["questions"]
            }
        """
    }
}
