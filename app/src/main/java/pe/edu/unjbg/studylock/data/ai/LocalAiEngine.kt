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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Único motor LiteRT-LM del proceso.
 *
 * Lo comparten el generador de preguntas y el clasificador de enlaces: inicializar el modelo es
 * caro, así que se reutiliza el mismo [Engine] (CPU, sin GPU: el 270M emite `<pad>` en GPU).
 * Todas las inferencias se serializan con un mutex porque una conversación no es reentrante.
 */
class LocalAiEngine(
    private val downloader: ModelDownloader,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {

    data class Request(
        val systemInstruction: String,
        val prompt: String,
        val schema: String,
        val maxOutputTokens: Int,
        val temperature: Double = 0.2,
        val seed: Int = 7,
    )

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var enginePath: String = ""

    private val engineLock = Any()
    private val inferenceLock = Mutex()

    fun isAvailable(): Boolean = downloader.verifiedFile() != null

    /** Devuelve la respuesta del modelo como texto; el llamador la valida con JSON estricto. */
    suspend fun generateJson(request: Request): String = withContext(io) {
        val model = downloader.verifiedFile()
            ?: return@withContext error("El modelo todavía no está descargado.")
        inferenceLock.withLock {
            var conversation: Conversation? = null
            try {
                conversation = engineFor(model).createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(request.systemInstruction),
                        samplerConfig = SamplerConfig(
                            topK = 20,
                            topP = 0.9,
                            temperature = request.temperature,
                            seed = request.seed,
                        ),
                        maxOutputToken = request.maxOutputTokens.coerceIn(200, 2_000),
                        enableResponseFormat = true,
                    )
                )
                val answer = conversation.sendMessage(
                    request.prompt,
                    responseFormat = ResponseFormat.json(request.schema),
                )
                answer.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("\n") { it.text }
                    .trim()
            } catch (error: Exception) {
                error("El modelo local falló: ${error.message ?: "error desconocido"}")
            } finally {
                runCatching { conversation?.close() }
            }
        }
    }

    private fun engineFor(model: File): Engine {
        val cached = engine
        if (cached != null && enginePath == model.absolutePath && cached.isInitialized()) return cached
        return synchronized(engineLock) {
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
        synchronized(engineLock) {
            runCatching { engine?.close() }
            engine = null
            enginePath = ""
        }
    }

    private fun error(message: String): String = """{"error":"${message.replace('"', '\'')}"}"""
}
