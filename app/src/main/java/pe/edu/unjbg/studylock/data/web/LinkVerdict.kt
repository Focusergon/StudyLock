package pe.edu.unjbg.studylock.data.web

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.util.ArrayDeque
import kotlin.math.roundToInt

/** Tipo de enlace que el modelo debe distinguir en los portales UNJBG. */
enum class LinkRole {
    CURSO,
    SILABO,
    SECCION,
    OTRO,
}

enum class VerdictSource { IA, HEURISTICA }

enum class LinkPortal { AULA, INTRANET }

/** Clasificación de un enlace detectado, con la razón que se muestra al estudiante. */
data class LinkVerdict(
    val link: WebLink,
    val role: LinkRole,
    val reason: String,
    val source: VerdictSource = VerdictSource.HEURISTICA,
    val confidence: Int = 60,
) {
    val href: String get() = link.href
    /** Nombre visible del curso/material: usa el contexto cuando el enlace no trae texto. */
    val label: String get() = LinkClassifier.displayLabel(link.text, link.href, link.context)
}

data class ClassificationOutcome(
    val verdicts: List<LinkVerdict> = emptyList(),
    val usedAi: Boolean = false,
    val error: String? = null,
)

/** Devuelve las URLs que el recorrido debe abrir. En modo diagnóstico la UI decide. */
typealias LinkConfirmation = suspend (List<LinkVerdict>) -> Set<String>

/** Selección automática cuando el estudiante no pidió confirmar enlace por enlace. */
fun List<LinkVerdict>.acceptedByDefault(): Set<String> = asSequence()
    .filter { it.role == LinkRole.CURSO || it.role == LinkRole.SILABO }
    .filter { it.confidence >= 60 }
    .map { it.href }
    .toSet()

/**
 * Completa los enlaces que el modelo omitió con el respaldo determinista.
 *
 * El modelo local pequeño a veces devuelve menos entradas de las pedidas. Las entradas que sí
 * devuelve ya fueron validadas; las faltantes se clasifican con heurísticas sin inventar cursos.
 */
fun List<LinkVerdict>.mergeWithFallback(fallback: List<LinkVerdict>): List<LinkVerdict> {
    val byLink = associateBy { it.link }
    return fallback.map { byLink[it.link] ?: it }
}

/**
 * Descarta las decisiones de IA que contradicen indicios objetivos del enlace.
 *
 * El modelo pequeño a veces devuelve el ejemplo del prompt o etiqueta como CURSO un enlace del
 * menú. Un CURSO sin ruta de curso, o un SILABO sin texto/ruta de sílabo, se reemplaza por la
 * heurística antes de aceptarse automáticamente.
 */
fun List<LinkVerdict>.sanitizedBy(fallback: List<LinkVerdict>): List<LinkVerdict> {
    val heuristicByLink = fallback.associateBy { it.link }
    return map { verdict ->
        if (verdict.source != VerdictSource.IA) return@map verdict
        val heuristic = heuristicByLink[verdict.link] ?: return@map verdict
        val link = verdict.link
        val contradictory = when (verdict.role) {
            LinkRole.CURSO -> !LinkClassifier.looksLikeCourse(link.text, link.href)
            LinkRole.SILABO -> !LinkClassifier.hasSyllabusUrl(link.href) &&
                !LinkClassifier.isSyllabusLink(link.text, link.href)
            else -> false
        }
        if (contradictory) heuristic else verdict
    }
}

/**
 * Valida la respuesta del modelo antes de que pueda crear cursos.
 *
 * Acepta un array raíz, un objeto con `links` y envoltorios habituales (`data`, `result`,
 * markdown) porque el modelo local a veces cambia la forma del JSON. La validación sigue siendo
 * estricta: JSON bien formado, índices únicos y en rango, categoría conocida y motivo no vacío.
 * Si una sola entrada devuelta es inválida se descarta todo el lote; las entradas omitidas se
 * completan fuera del parser con [mergeWithFallback].
 */
object LinkVerdictParser {

    data class Result(
        val verdicts: List<LinkVerdict> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    @Suppress("DEPRECATION")
    fun parse(json: String, links: List<WebLink>): Result {
        if (json.isBlank() || links.isEmpty()) return Result()
        val cleaned = unwrapJson(json)

        val root = runCatching {
            val reader = JsonReader(StringReader(cleaned)).apply { isLenient = false }
            val element = Streams.parse(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) error("Sobra texto después del JSON.")
            element
        }.getOrElse {
            return Result(
                errors = listOf("El modelo no devolvió un JSON válido. ${it.message ?: ""}".trim()),
            )
        }

        if (root.isJsonObject) {
            val reported = root.asJsonObject.stringOrNull(listOf("error")).orEmpty().trim()
            if (reported.isNotBlank()) {
                val message = if (reported.startsWith("El modelo local falló")) {
                    reported
                } else {
                    "El modelo local falló: $reported"
                }
                return Result(errors = listOf(message))
            }
        }

        val array = findVerdictArray(root)
            ?: return Result(errors = listOf("Falta la lista «links»."))

        if (array.size() > links.size) {
            return Result(
                errors = listOf(
                    "El modelo clasificó ${array.size()} enlaces; se esperaban como máximo ${links.size}.",
                ),
            )
        }

        val errors = ArrayList<String>()
        val verdicts = ArrayList<LinkVerdict>()
        val seen = HashSet<Int>()
        for ((position, element) in array.withIndex()) {
            val item = runCatching { element.asJsonObject }
                .getOrElse {
                    errors += "Enlace $position: no es un objeto."
                    continue
                }
            val index = item.intOrNull(INDEX_KEYS) ?: -1
            val role = roleFrom(item.stringOrNull(ROLE_KEYS))
            val why = item.stringOrNull(REASON_KEYS)?.trim().orEmpty()

            val error = when {
                index !in links.indices -> "Enlace $position: índice $index fuera de rango."
                !seen.add(index) -> "Enlace $position: índice repetido."
                role == null -> "Enlace $position: categoría desconocida."
                why.length < 4 -> "Enlace $position: motivo vacío."
                else -> null
            }
            if (error != null) {
                errors += error
                continue
            }
            verdicts += LinkVerdict(
                link = links[index],
                role = checkNotNull(role),
                reason = why.take(220),
                source = VerdictSource.IA,
                confidence = item.confidenceOrNull() ?: 80,
            )
        }
        return if (errors.isEmpty()) Result(verdicts) else Result(errors = errors)
    }

    /** Quita cercos de markdown y texto previo, pero deja un JSON que se parsea sin leniencia. */
    private fun unwrapJson(raw: String): String {
        var text = raw.trim().trimStart('\uFEFF')
        if (text.startsWith("```")) {
            val newline = text.indexOf('\n')
            if (newline >= 0) text = text.substring(newline + 1)
            text = text.substringBeforeLast("```").trim()
        }
        return extractFirstContainer(text) ?: text
    }

    private fun extractFirstContainer(text: String): String? {
        val start = text.indexOfFirst { it == '{' || it == '[' }
        if (start < 0) return null
        val stack = ArrayDeque<Char>()
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val char = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{', '[' -> stack.addLast(char)
                '}', ']' -> {
                    if (stack.isEmpty()) return null
                    val open = stack.removeLast()
                    if ((open == '{' && char != '}') || (open == '[' && char != ']')) return null
                    if (stack.isEmpty()) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun findVerdictArray(root: JsonElement): JsonArray? {
        if (root.isJsonArray) return root.asJsonArray
        if (!root.isJsonObject) return null
        val objectRoot = root.asJsonObject
        ARRAY_KEYS.firstNotNullOfOrNull { key ->
            objectRoot.get(key)?.takeIf { it.isJsonArray }?.asJsonArray
        }?.let { return it }
        NESTED_KEYS.firstNotNullOfOrNull { key ->
            objectRoot.get(key)?.takeIf { it.isJsonObject }?.let { findVerdictArray(it) }
        }?.let { return it }
        return null
    }

    private fun roleFrom(value: String?): LinkRole? {
        val normalized = LinkClassifier.normalize(value.orEmpty())
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return when (normalized) {
            "curso", "course", "courses", "materia", "asignatura" -> LinkRole.CURSO
            "silabo", "syllabus", "programa analitico", "sumilla" -> LinkRole.SILABO
            "seccion", "section", "unidad", "tema", "modulo" -> LinkRole.SECCION
            "otro", "other", "ninguno", "menu", "nav" -> LinkRole.OTRO
            else -> null
        }
    }

    private fun JsonObject.primitiveOrNull(names: Collection<String>): JsonPrimitive? =
        names.firstNotNullOfOrNull { name ->
            get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
        }

    private fun JsonObject.intOrNull(names: Collection<String>): Int? =
        primitiveOrNull(names)?.runCatching { asInt }?.getOrNull()

    private fun JsonObject.doubleOrNull(names: Collection<String>): Double? =
        primitiveOrNull(names)?.runCatching { asDouble }?.getOrNull()

    private fun JsonObject.stringOrNull(names: Collection<String>): String? =
        primitiveOrNull(names)?.runCatching { asString }?.getOrNull()

    private fun JsonObject.confidenceOrNull(): Int? {
        val value = doubleOrNull(CONFIDENCE_KEYS) ?: return null
        if (value.isNaN()) return null
        return when {
            value > 0.0 && value < 1.0 -> (value * 100).roundToInt()
            else -> value.roundToInt().coerceIn(0, 100)
        }
    }

    private val INDEX_KEYS = listOf("i", "index", "idx", "indice", "n")
    private val ROLE_KEYS = listOf("k", "role", "kind", "categoria", "category", "tipo", "type")
    private val REASON_KEYS = listOf("why", "reason", "razon", "motivo", "justificacion", "explanation")
    private val CONFIDENCE_KEYS = listOf("c", "confidence", "confianza", "certeza", "score")
    private val ARRAY_KEYS = listOf(
        "links", "items", "result", "results", "verdicts",
        "clasificaciones", "clasificacion", "classification", "output", "salida",
    )
    private val NESTED_KEYS = listOf("data", "response", "result", "output")
}
