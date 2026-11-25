package pe.edu.unjbg.studylock.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.QuestionTier

class AiQuestionValidatorTest {

    private val source = "La membrana semipermeable permite el paso selectivo de sustancias " +
        "según su tamaño y carga. Este mecanismo mantiene el equilibrio interno de la célula " +
        "sin gastar energía adicional."

    private fun json(
        prompt: String = "¿Qué permite la membrana semipermeable?",
        options: String = "[\"opción uno\",\"opción dos\",\"opción tres\",\"opción cuatro\"]",
        index: Int = 1,
        explanation: String = "El material indica que el paso es selectivo.",
        quote: String = "permite el paso selectivo de sustancias",
        tier: String = "CONCEPTO",
    ) = """
        {"questions":[{"prompt":"$prompt","options":$options,"correct":{"index":$index},
        "explanation":"$explanation","sourceQuote":"$quote","tier":"$tier"}]}
    """.trimIndent().replace("\n", " ")

    @Test
    fun `acepta pregunta valida y ancla la cita`() {
        val result = AiQuestionValidator.parse(json(), source, "Membranas", 4)

        assertEquals(1, result.questions.size)
        assertTrue(result.errors.isEmpty())
        val question = result.questions.single()
        assertEquals(1, question.correctIndex)
        assertEquals(QuestionTier.CONCEPTO, question.tier)
        assertEquals("permite el paso selectivo de sustancias", question.sourceQuote)
        assertEquals("membranas", question.conceptKey)
    }

    @Test
    fun `rechaza json no estricto`() {
        val result = AiQuestionValidator.parse("{questions: []}", source, "Membranas", 4)
        assertTrue(result.questions.isEmpty())
        assertTrue(result.errors.first().contains("JSON válido"))
    }

    @Test
    fun `descarta cita inventada`() {
        val result = AiQuestionValidator.parse(
            json(quote = "el modelo se inventa esta frase literal"),
            source,
            "Membranas",
            4,
        )
        assertTrue(result.questions.isEmpty())
        assertTrue(result.errors.first().contains("no aparece"))
    }

    @Test
    fun `exige cuatro opciones unicas e indice valido`() {
        val repeated = AiQuestionValidator.parse(
            json(options = "[\"igual\",\"igual\",\"otra\",\"otra\"]"),
            source,
            "Membranas",
            4,
        )
        assertTrue(repeated.questions.isEmpty())
        assertTrue(repeated.errors.first().contains("repite"))

        val badIndex = AiQuestionValidator.parse(json(index = 9), source, "Membranas", 4)
        assertTrue(badIndex.questions.isEmpty())
        assertTrue(badIndex.errors.first().contains("índice correcto"))
    }

    @Test
    fun `respeta el limite de preguntas`() {
        val two = """
            {"questions":[
              {"prompt":"Pregunta uno","options":["a","b","c","d"],"correct":{"index":0},
               "explanation":"Por el material.","sourceQuote":"permite el paso selectivo de sustancias","tier":"PROCESO"},
              {"prompt":"Pregunta dos","options":["a","b","c","d"],"correct":{"index":1},
               "explanation":"Por el material.","sourceQuote":"mantiene el equilibrio interno de la célula","tier":"GENERAL"}
            ]}
        """.trimIndent().replace("\n", " ")
        val result = AiQuestionValidator.parse(two, source, "Membranas", maxQuestions = 1)

        assertEquals(1, result.questions.size)
        assertEquals(QuestionTier.PROCESO, result.questions.single().tier)
        assertEquals(3, result.questions.single().difficulty)
    }
}
