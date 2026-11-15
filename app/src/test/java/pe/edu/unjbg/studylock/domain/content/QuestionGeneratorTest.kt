package pe.edu.unjbg.studylock.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorTest {

    private val syllabus = """
        Modelo relacional
        Una clave primaria identifica cada registro de forma única dentro de una tabla.
        La clave foránea referencia la clave primaria de otra tabla y protege la integridad referencial.
        Una relación en el modelo relacional representa una tabla con filas y columnas.

        Normalización
        La primera forma normal exige que todos los atributos contengan valores atómicos simples.
        La segunda forma normal elimina las dependencias parciales de una clave compuesta.
        La tercera forma normal elimina las dependencias transitivas entre atributos no clave.
        La normalización reduce la redundancia y evita anomalías de actualización en la base de datos.
    """.trimIndent()

    @Test
    fun `generates multiple choice questions with four options`() {
        val questions = QuestionGenerator.generate(syllabus, "Modelo relacional", maxQuestions = 10)
        assertTrue("debe generar preguntas", questions.isNotEmpty())
        questions.forEach { q ->
            assertEquals("cada pregunta tiene 4 opciones", 4, q.options.size)
            assertTrue("el índice correcto es válido", q.correctIndex in q.options.indices)
            assertEquals("las opciones son únicas", 4, q.options.map { it.lowercase() }.toSet().size)
            assertTrue("tiene explicación", q.explanation.isNotBlank())
        }
    }

    @Test
    fun `generation is deterministic`() {
        val first = QuestionGenerator.generate(syllabus, "Tema", maxQuestions = 10).map { it.prompt }
        val second = QuestionGenerator.generate(syllabus, "Tema", maxQuestions = 10).map { it.prompt }
        assertEquals(first, second)
    }

    @Test
    fun `respects the maximum number of questions`() {
        val questions = QuestionGenerator.generate(syllabus, maxQuestions = 3)
        assertTrue(questions.size <= 3)
    }

    @Test
    fun `does not repeat prompts`() {
        val prompts = QuestionGenerator.generate(syllabus, maxQuestions = 20).map { it.prompt.lowercase() }
        assertEquals(prompts.size, prompts.toSet().size)
    }

    @Test
    fun `returns nothing for empty or tiny text`() {
        assertTrue(QuestionGenerator.generate("").isEmpty())
        assertTrue(QuestionGenerator.generate("Hola.").isEmpty())
    }

    @Test
    fun `topicsFrom detects headings`() {
        val topics = QuestionGenerator.topicsFrom(syllabus, maxTopics = 6, questionsPerTopic = 6)
        assertTrue("debe detectar al menos un tema", topics.isNotEmpty())
        topics.forEach { topic ->
            assertTrue(topic.name.isNotBlank())
            assertTrue(topic.questions.size >= 2)
        }
    }

    @Test
    fun `cloze questions hide the key word`() {
        val questions = QuestionGenerator.generate(syllabus, maxQuestions = 10)
        val cloze = questions.firstOrNull { it.prompt.contains("_____") }
        assertNotNull("debe haber preguntas de completar", cloze)
    }
}
