package pe.edu.unjbg.studylock.data.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.domain.content.AiGeneration
import pe.edu.unjbg.studylock.domain.content.AiQuestionValidator
import pe.edu.unjbg.studylock.domain.content.QuestionAiProvider
import java.io.File

/**
 * Proveedor de preguntas con Gemma 3 270M ejecutado en el propio teléfono (LiteRT-LM, CPU).
 *
 * No envía el material a ningún servidor. Toda la salida pasa por [AiQuestionValidator] antes de
 * devolverse; si el modelo falla o inventa una cita, el generador híbrido usa el respaldo
 * determinista y la experiencia del estudiante no se interrumpe.
 */
class LocalQuestionAiProvider(
    private val downloader: ModelDownloader,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : QuestionAiProvider, AutoCloseable {

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var enginePath: String = ""

    private val lock = Any()

    override fun isAvailable(): Boolean = downloader.verifiedFile() != null

    override suspend fun generate(
        topic: String,
        excerpt: String,
        count: Int,
    ): AiGeneration = withContext(io) {
        val model = downloader.verifiedFile()
            ?: return@withContext AiGeneration(error = "El modelo todavía no está descargado.")
        if (excerpt.length < MIN_EXCERPT) {
            return@withContext AiGeneration(error = "El material es demasiado corto para la IA.")
        }

        var conversation: Conversation? = null
        try {
            conversation = engineFor(model).createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(SYSTEM_INSTRUCTION),
                    samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.2, seed = 7),
                    maxOutputToken = (count * 110).coerceIn(400, 1_800),
                    enableResponseFormat = true,
                )
            )
            val answer = conversation.sendMessage(
                buildPrompt(topic, excerpt, count),
                responseFormat = ResponseFormat.json(RESPONSE_SCHEMA),
            )
            val json = answer.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("\n") { it.text }
                .trim()
            val parsed = AiQuestionValidator.parse(json, excerpt, topic, count)
            if (parsed.questions.isEmpty()) {
                AiGeneration(error = parsed.errors.firstOrNull() ?: "El modelo no devolvió preguntas válidas.")
            } else {
                AiGeneration(parsed.questions)
            }
        } catch (error: Exception) {
            AiGeneration(error = "El modelo local falló: ${error.message ?: "error desconocido"}")
        } finally {
            runCatching { conversation?.close() }
        }
    }

    private fun engineFor(model: File): Engine {
        val cached = engine
        if (cached != null && enginePath == model.absolutePath && cached.isInitialized()) return cached
        return synchronized(lock) {
            val current = engine
            if (current != null && enginePath == model.absolutePath && current.isInitialized()) {
                current
            } else {
                runCatching { current?.close() }
                val created = Engine(
                    EngineConfig(
                        modelPath = model.absolutePath,
                        backend = Backend.CPU(),
                        cacheDir = File(model.parentFile, "cache").absolutePath,
                        maxNumTokens = 4_096,
                    )
                )
                created.initialize()
                engine = created
                enginePath = model.absolutePath
                created
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            runCatching { engine?.close() }
            engine = null
            enginePath = ""
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
