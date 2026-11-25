package pe.edu.unjbg.studylock.domain.content

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unjbg.studylock.domain.model.QuestionTier

class HybridQuestionGeneratorTest {

    private val material = (
        "La fotosíntesis transforma la energía luminosa en energía química y almacena glucosa " +
            "en los tejidos vegetales. El proceso ocurre en los cloroplastos y requiere agua y dióxido de carbono. " +
            "La respiración celular libera la energía almacenada cuando la célula la necesita. " +
            "Este processo ocurre en las mitocondrias y produce agua y dióxido de carbono como residuos."
        )

    private fun aiQuestion(prompt: String) = GeneratedQuestion(
        prompt = prompt,
        options = listOf("uno", "dos", "tres", "cuatro"),
        correctIndex = 0,
        explanation = "Basado en el material.",
        tier = QuestionTier.CONCEPTO,
        sourceQuote = "fragmento literal del material",
    )

    private class FakeProvider(
        private val questions: List<GeneratedQuestion>,
        private val available: Boolean = true,
        private val fail: Boolean = false,
    ) : QuestionAiProvider {
        var calls = 0
        val excerpts = mutableListOf<String>()

        override fun isAvailable(): Boolean = available

        override suspend fun generate(topic: String, excerpt: String, count: Int): AiGeneration {
            calls++
            excerpts += excerpt
            if (fail) throw IllegalStateException("modelo no cargado")
            return AiGeneration(questions.take(count))
        }
    }

    @Test
    fun `mezcla preguntas validadas con el respaldo`() = runBlocking {
        val ai = FakeProvider(listOf(aiQuestion("¿Dónde ocurre la fotosíntesis?"), aiQuestion("¿Qué libera la respiración?")))
        val outcome = HybridQuestionGenerator(ai, enabled = { true }).generate(material, "Biología", maxQuestions = 4)

        assertTrue(outcome.usedAi)
        assertEquals(1, ai.calls)
        assertEquals(4, outcome.questions.size)
        assertTrue(outcome.questions.take(2).all { it.sourceQuote.isNotBlank() })
    }

    @Test
    fun `si la IA esta desactivada no se llama al modelo`() = runBlocking {
        val ai = FakeProvider(listOf(aiQuestion("Pregunta de IA")))
        val outcome = HybridQuestionGenerator(ai, enabled = { false }).generate(material, "Biología", 4)

        assertFalse(outcome.usedAi)
        assertEquals(0, ai.calls)
        assertTrue(outcome.questions.isNotEmpty())
    }

    @Test
    fun `si el modelo falla se usa solo el generador determinista`() = runBlocking {
        val ai = FakeProvider(emptyList(), fail = true)
        val outcome = HybridQuestionGenerator(ai, enabled = { true }).generate(material, "Biología", 4)

        assertFalse(outcome.usedAi)
        assertTrue(outcome.questions.isNotEmpty())
        assertTrue(outcome.questions.none { it.sourceQuote.isNotBlank() })
    }

    @Test
    fun `topicsFrom envia a la IA el texto de cada seccion`() = runBlocking {
        val sectionOne = "La fotosíntesis transforma la energía luminosa en energía química y " +
            "almacena glucosa en los tejidos vegetales durante el ciclo diurno de la planta. " +
            "El proceso requiere agua y dióxido de carbono en los cloroplastos de las hojas."
        val sectionTwo = "La respiración celular libera la energía química almacenada en la glucosa " +
            "cuando la célula la necesita para realizar sus funciones vitales. " +
            "Este proceso ocurre en las mitocondrias y produce agua como residuo."
        val text = "FOTOSINTESIS\n$sectionOne\n\nRESPIRACION\n$sectionTwo"
        val ai = FakeProvider(listOf(aiQuestion("Pregunta uno"), aiQuestion("Pregunta dos")))

        val topics = HybridQuestionGenerator(ai, enabled = { true }).topicsFrom(text, maxTopics = 3)

        assertEquals(2, ai.calls)
        assertEquals(2, topics.size)
        assertTrue(ai.excerpts[0].contains("fotosíntesis", ignoreCase = true))
        assertTrue(ai.excerpts[1].contains("respiración", ignoreCase = true))
        assertFalse(ai.excerpts[0].contains("mitocondrias"))
    }
}
