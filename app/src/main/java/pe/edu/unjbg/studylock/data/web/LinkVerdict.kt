package pe.edu.unjbg.studylock.data.web

import com.google.gson.JsonObject
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

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
    val label: String get() = link.text.ifBlank { LinkClassifier.fileNameFromUrl(link.href) }
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
 * Valida la respuesta del modelo antes de que pueda crear cursos.
 *
 * Se exige JSON estricto, una entrada por enlace, índices únicos y en rango, categoría conocida y
 * motivo no vacío. Si una sola entrada es inválida se descarta todo el lote y se usan heurísticas:
 * nunca se crea un curso a partir de una respuesta dudosa del modelo.
 */
object LinkVerdictParser {

    data class Result(
        val verdicts: List<LinkVerdict> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    @Suppress("DEPRECATION")
    fun parse(json: String, links: List<WebLink>): Result {
        if (json.isBlank() || links.isEmpty()) return Result()
        val root = runCatching {
            val reader = JsonReader(StringReader(json)).apply { isLenient = false }
            val element = Streams.parse(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) error("Sobra texto después del JSON.")
            element.asJsonObject
        }.getOrElse { return Result(errors = listOf("El modelo no devolvió un JSON válido.")) }

        val array = runCatching { root.getAsJsonArray("links") }
            .getOrElse { return Result(errors = listOf("Falta la lista «links».")) }
            ?: return Result(errors = listOf("Falta la lista «links»."))

        if (array.size() != links.size) {
            return Result(errors = listOf("El modelo clasificó ${array.size()} enlaces; se esperaban ${links.size}."))
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
            val index = runCatching { item.get("i").asInt }.getOrElse { -1 }
            val role = runCatching { LinkRole.valueOf(item.stringOrNull("k").orEmpty().uppercase()) }
                .getOrDefault(LinkRole.OTRO)
            val why = item.stringOrNull("why")?.trim().orEmpty()
            val knownRole = item.stringOrNull("k")?.trim()?.uppercase().orEmpty() in LinkRole.entries.map { it.name }

            val error = when {
                index !in links.indices -> "Enlace $position: índice $index fuera de rango."
                !seen.add(index) -> "Enlace $position: índice repetido."
                !knownRole -> "Enlace $position: categoría desconocida."
                why.length < 4 -> "Enlace $position: motivo vacío."
                else -> null
            }
            if (error != null) {
                errors += error
                continue
            }
            verdicts += LinkVerdict(
                link = links[index],
                role = role,
                reason = why.take(220),
                source = VerdictSource.IA,
                confidence = runCatching { item.get("c").asInt }.getOrDefault(80).coerceIn(0, 100),
            )
        }
        return if (errors.isEmpty()) Result(verdicts) else Result(errors = errors)
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        runCatching { get(name)?.takeIf { it.isJsonPrimitive }?.asString }.getOrNull()
}
