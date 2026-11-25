package pe.edu.unjbg.studylock.domain.content

/**
 * Generación de preguntas con un modelo local, sin clave de API ni servicios externos.
 *
 * La implementación real vive en `data/ai` (LiteRT-LM). Esta interfaz es pura para que el dominio y
 * sus tests no dependan de Android ni del binario del modelo.
 */
interface QuestionAiProvider {

    /** ¿Está el modelo descargado y verificado en este dispositivo? */
    fun isAvailable(): Boolean

    /** Devuelve preguntas validadas; vacío si el modelo falló (el híbrido aplica el respaldo). */
    suspend fun generate(topic: String, excerpt: String, count: Int): AiGeneration
}

data class AiGeneration(
    val questions: List<GeneratedQuestion> = emptyList(),
    val error: String? = null,
)
