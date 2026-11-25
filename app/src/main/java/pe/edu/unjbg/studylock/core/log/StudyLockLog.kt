package pe.edu.unjbg.studylock.core.log

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registro local de fallos que el estudiante puede ver y copiar desde la propia app.
 *
 * Conserva las últimas líneas en memoria y en `filesDir/logs/studylock.log`, para que un crash
 * ocurrido antes de que aparezca la interfaz pueda leerse al volver a abrir. Nunca guarda
 * credenciales: solo mensajes explícitos y stack traces.
 */
object StudyLockLog {

    private val lines = CopyOnWriteArrayList<String>()
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var file: File? = null

    fun init(context: Context) {
        if (file != null) return
        val dir = File(context.applicationContext.filesDir, "logs")
        runCatching { dir.mkdirs() }
        val target = File(dir, "studylock.log")
        file = target
        if (target.exists()) {
            runCatching {
                target.readLines().takeLast(MAX_LINES).forEach { lines.add(it) }
            }
        }
        log("App", "Inicio de StudyLock")
    }

    fun log(tag: String, message: String) = add("I", tag, message)

    fun warning(tag: String, message: String) = add("W", tag, message)

    fun error(tag: String, error: Throwable, extra: String = "") {
        val detail = listOfNotNull(extra.takeIf { it.isNotBlank() }, error.message, error.stackTraceToString())
            .joinToString(" | ")
        add("E", tag, detail)
    }

    fun allText(): String = lines.joinToString("\n")

    fun clear() {
        lines.clear()
        runCatching { file?.writeText("") }
    }

    private fun add(level: String, tag: String, message: String) {
        val line = "${format.format(Date())} $level/$tag: ${message.replace('\n', ' ')}"
        lines.add(line)
        while (lines.size > MAX_LINES) lines.removeAt(0)
        runCatching { file?.appendText(line + "\n") }
        trimFile()
    }

    private fun trimFile() {
        val target = file ?: return
        runCatching {
            if (target.exists() && target.length() > MAX_BYTES) {
                val kept = target.readLines().takeLast(MAX_LINES / 2)
                target.writeText(kept.joinToString("\n", postfix = "\n"))
            }
        }
    }

    private const val MAX_LINES = 200
    private const val MAX_BYTES = 200_000
}
