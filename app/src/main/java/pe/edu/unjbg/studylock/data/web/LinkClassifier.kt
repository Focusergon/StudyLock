package pe.edu.unjbg.studylock.data.web

import java.net.URLDecoder

/**
 * Heurísticas puras para clasificar enlaces encontrados al navegar la web de UNJBG.
 *
 * No dependen de Android, así que se pueden probar en la JVM. Son la base de la «automatización
 * que busca lo que queremos»: distinguir cursos, sílabos y archivos descargables.
 */
object LinkClassifier {

    private val IMPORTABLE_EXTENSIONS = listOf(".pdf", ".txt", ".md", ".csv", ".doc", ".docx")
    private val FILE_HINTS = listOf(".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".zip")
    private val SYLLABUS_WORDS = listOf("silabo", "syllabus", "programa analitico", "sumilla")
    private val NON_SYLLABUS_WORDS = listOf(
        "plan de estudio", "planes de estudio", "plan de estudios", "malla curricular",
        "malla academica", "horario de clase", "horarios de clase", "horario de clases",
        "horarios de clases", "evaluacion docente", "manuales y guias", "manuales y guías",
    )
    private val COURSE_PATH_HINTS = listOf(
        "/curso/", "/cursos/", "/course/", "/courses/",
        "courseid=", "course_id=", "cursoid=", "curso_id=",
        "/materia/", "/asignatura/",
    )
    private val GENERIC_COURSE_WORDS = setOf(
        "mis cursos", "cursos", "mis materias", "materias", "asignaturas",
        "lista de cursos", "ver cursos", "todos los cursos", "panel de cursos",
        "panel", "dashboard", "mi panel", "inicio", "home", "portada", "bienvenida",
        "aula virtual unjbg", "tablero",
    )
    private val CONTEXT_ACTION_WORDS = listOf(
        "ver curso", "ir al curso", "continuar", "ingresar", "abrir", "acceso", "ingresar",
        "omitir mis cursos", "omitir", "saltar al contenido", "saltar", "ver mas informacion",
        "ver más información", "mas informacion", "más información", "clic aqui", "click aqui",
    )
    /** «ESIS INGENIERÍA WEB…»: tras el código de carrera viene el nombre del curso. */
    private val COURSE_CODE = Regex("(?:^|\\s)(?:ESIS|[A-Z]{3,6})\\s+(?=[A-ZÁÉÍÓÚÑ])")
    private val NON_COURSE_PATHS = listOf(
        "/course/index.php", "/course/category.php", "/my/", "/user/",
        "/message/", "/grade/", "/login/",
    )
    private val ID_IN_PATH = Regex("/(?:curso|cursos|course|courses|materia|asignatura)/\\d+")
    private val ID_IN_QUERY = Regex(
        "(?:^|[?&/])(?:id|cid|courseid|cursoid|course_id|curso_id)\\s*=\\s*\\d+",
    )

    /** ¿El enlace apunta a un archivo (documento) y no a otra página? */
    fun looksLikeFile(url: String, text: String = ""): Boolean {
        val lowerUrl = url.lowercase()
        val lowerText = text.lowercase()
        if (FILE_HINTS.any { lowerUrl.contains(it) || lowerText.endsWith(it) }) return true
        return lowerUrl.contains("/archivos/") ||
            lowerUrl.contains("/files/") ||
            lowerUrl.contains("pluginfile") ||
            lowerUrl.contains("/download")
    }

    /** ¿Podemos convertir este documento en preguntas (texto o PDF)? */
    fun isImportable(nameOrUrl: String): Boolean {
        val lower = nameOrUrl.lowercase().substringBefore('?')
        return IMPORTABLE_EXTENSIONS.any { lower.endsWith(it) }
    }

    /** ¿El enlace lleva explícitamente a un sílabo, programa analítico o sumilla? */
    fun hasSyllabusUrl(url: String): Boolean {
        val lower = url.lowercase()
        return listOf("/silabo", "syllabus", "programa-analitico", "programa_analitico", "sumilla")
            .any { lower.contains(it) }
    }

    /** ¿El enlace es malla/plan de estudios, horario o evaluación docente (no un sílabo)? */
    fun isPlanOrScheduleLink(text: String, url: String): Boolean {
        if (hasSyllabusUrl(url)) return false
        return NON_SYLLABUS_WORDS.any {
            normalizeLoose(text).contains(it) || normalizeLoose(url).contains(it)
        }
    }

    /** ¿El enlace parece una entrada de curso del campus? */
    fun looksLikeCourse(text: String, url: String): Boolean {
        val lowerUrl = url.lowercase()
        val looseText = normalizeLoose(text)
        if (looksLikeFile(url, text)) return false
        if (isSyllabusLink(text, url)) return false
        if (looseText in GENERIC_COURSE_WORDS) return false
        val path = pathOf(url).lowercase()
        if (NON_COURSE_PATHS.any { path.startsWith(it) }) return false
        if (!COURSE_PATH_HINTS.any { lowerUrl.contains(it) }) return false
        return ID_IN_PATH.containsMatchIn(lowerUrl) ||
            ID_IN_QUERY.containsMatchIn(lowerUrl) ||
            looseText.isNotBlank()
    }

    /** ¿El enlace lleva a la sección de sílabos? «Plan de estudios» NO cuenta como sílabo. */
    fun isSyllabusLink(text: String, url: String): Boolean {
        if (hasSyllabusUrl(url)) return true
        if (isPlanOrScheduleLink(text, url)) return false
        val looseText = normalizeLoose(text)
        return SYLLABUS_WORDS.any { looseText.contains(it) }
    }

    /** ¿El texto del enlace parece una sección del curso (unidad, semana, tema, recurso…)? */
    fun looksLikeSection(text: String): Boolean {
        val normalized = normalizeLoose(text)
        if (normalized.isBlank() || normalized in GENERIC_COURSE_WORDS) return false
        if (NON_SYLLABUS_WORDS.any { normalized.contains(it) }) return false
        return SECTION_WORDS.any { normalized.contains(it) }
    }

    /** Nombre de archivo legible a partir de una URL. */
    fun fileNameFromUrl(url: String): String {
        val clean = url.substringBefore('#').substringBefore('?').trimEnd('/')
        val last = clean.substringAfterLast('/').ifBlank { "Documento" }
        return runCatching { URLDecoder.decode(last, "UTF-8") }.getOrDefault(last)
    }

    /**
     * Etiqueta legible para un enlace. El Aula Virtual pinta algunas tarjetas de curso con un
     * `<a>` sin texto; en ese caso el nombre está en el contenedor cercano ([context]).
     */
    fun displayLabel(text: String, url: String, context: String = ""): String {
        val direct = text.replace(Regex("\\s+"), " ").trim()
        if (direct.isNotBlank() && !direct.equals(fileNameFromUrl(url), ignoreCase = true)) {
            return direct.take(140)
        }
        return labelFromContext(context) ?: fileNameFromUrl(url)
    }

    /**
     * Nombre específico de un curso del Aula. En el panel, cada tarjeta repite el nombre de la
     * carrera («Ingeniería en Informática y Sistemas 26-II») y añade el curso tras el código
     * (`ESIS`), el grupo (`F2`) y la acción («Acceso»). Sin esta limpieza los seis cursos saldrían
     * con el mismo nombre.
     */
    fun courseLabel(text: String, url: String, context: String = ""): String {
        for (source in listOf(text, context)) {
            val clean = source.replace(Regex("\\s+"), " ").trim()
            if (clean.isBlank()) continue
            val marker = COURSE_CODE.find(clean) ?: continue
            var label = clean.substring(marker.range.last + 1).trim()
            CONTEXT_ACTION_WORDS.forEach { word ->
                label = label.replace(Regex("(?i)\\b${Regex.escape(word)}\\b"), " ")
            }
            label = label.replace(Regex("\\s+"), " ").trim()
            label = label.replace(Regex("\\s+[A-Z]\\d{1,2}$"), "").trim()
            if (label.length >= 4) return label.take(140)
        }
        return displayLabel(text, url, context)
    }

    /** Extrae la primera frase útil del contenedor de un enlace sin texto. */
    private fun labelFromContext(context: String): String? {
        if (context.isBlank()) return null
        var cleaned = context.replace(Regex("https?://\\S+"), " ")
        CONTEXT_ACTION_WORDS.forEach { word ->
            cleaned = cleaned.replace(Regex("(?i)\\b${Regex.escape(word)}\\b"), "\n")
        }
        val candidate = cleaned.split('\n')
            .asSequence()
            .map { it.replace(Regex("[|•·]+"), " ").replace(Regex("\\s+"), " ").trim(' ', '-', '.', ':') }
            .firstOrNull { it.length >= 4 && !isGenericLabel(it) }
            ?: return null
        return trimCourseCode(candidate).take(140)
    }

    /** «… 26-II ESIS INGENIERÍA W» se queda en «… 26-II»: el código no es parte del nombre. */
    private fun trimCourseCode(label: String): String {
        val words = label.split(' ')
        if (words.size < 5) return label
        val cut = words.indices.firstOrNull { index ->
            index >= 4 && words[index].length >= 3 && words[index].none { it.isDigit() } &&
                words[index].any { it.isLetter() } &&
                words[index].all { !it.isLetter() || it.isUpperCase() } &&
                index + 1 < words.size &&
                words[index + 1].all { !it.isLetter() || it.isUpperCase() }
        } ?: -1
        return if (cut > 0) words.take(cut).joinToString(" ").trim() else label
    }

    private fun isGenericLabel(value: String): Boolean {
        val normalized = normalizeLoose(value)
        return normalized in GENERIC_COURSE_WORDS || normalized in NAV_WORDS || looksLikeFile(value, "")
    }

    /** Tipo MIME aproximado según la extensión. */
    fun mimeFor(name: String): String = when (name.lowercase().substringAfterLast('.')) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "md" -> "text/markdown"
        "csv" -> "text/csv"
        else -> ""
    }

    /** Minúsculas sin tildes, para comparar textos de la web. */
    fun normalize(text: String): String = text.lowercase()
        .map { ACCENTS[it] ?: it }
        .joinToString("")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Como [normalize], pero tratando guiones y guiones bajos como espacios. */
    fun normalizeLoose(text: String): String = normalize(text)
        .replace('-', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Forma canónica de una URL para comparar: sin fragmento ni barra final. */
    fun canonical(url: String): String = url.substringBefore('#').trimEnd('/')

    /** ¿Dos direcciones pertenecen al mismo sitio? */
    fun sameHost(a: String, b: String): Boolean {
        val hostA = hostOf(a)
        return hostA.isNotEmpty() && hostA == hostOf(b)
    }

    /**
     * ¿[url] pertenece a la misma sección/curso que [base]?
     *
     * En Moodle el `id` de `course/view.php` identifica al curso. Un enlace a otro `id`, al panel
     * (`/my/`), a una categoría (`course/index.php`) o a la portada no debe colarse en el árbol de
     * ese curso. Las actividades `/mod/…/view.php?id=…` sí se aceptan: son las secciones del curso.
     */
    fun isWithin(url: String, base: String): Boolean {
        if (!sameHost(url, base)) return false
        val baseId = courseIdOf(base)
        if (baseId != null && isCourseEntry(base)) {
            val targetId = courseIdOf(url)
            if (targetId != null) return targetId == baseId
            val path = pathOf(url)
            return path.startsWith("/mod/") || path == "/course/view.php"
        }
        val basePath = canonical(base).substringBefore('?').trimEnd('/')
        val targetPath = canonical(url).substringBefore('?').trimEnd('/')
        return basePath.isNotEmpty() && (targetPath == basePath || targetPath.startsWith("$basePath/"))
    }

    /**
     * `id` de curso en la ruta o la consulta, solo cuando la URL es una entrada de curso.
     * En Moodle los `id` de `/mod/…/view.php` son de actividad, no de curso, y no deben compararse.
     */
    fun courseIdOf(url: String): String? {
        if (!isCourseEntry(url)) return null
        val lower = url.lowercase()
        ID_IN_QUERY.find(lower)?.let { match ->
            return match.value.substringAfterLast('=')
        }
        ID_IN_PATH.find(lower)?.let { match ->
            return match.value.substringAfterLast('/')
        }
        return null
    }

    /**
     * ¿Vale la pena seguir este enlace al recorrer un curso?
     *
     * Se descartan archivos, sílabos, planes/horarios, enlaces externos, páginas genéricas del
     * campus y los del menú de navegación; así la automatización se queda dentro del curso.
     */
    fun isCrawlableLink(text: String, url: String, base: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("mailto:") || lower.startsWith("tel:")) return false
        if (url.startsWith("#")) return false
        if (looksLikeFile(url, text) || isSyllabusLink(text, url)) return false
        if (isPlanOrScheduleLink(text, url)) return false
        if (!sameHost(url, base)) return false
        if (isGenericCrawlPage(url)) return false
        val normalized = normalize(text)
        if (normalized.isBlank() || normalized in NAV_WORDS || normalized in GENERIC_COURSE_WORDS) return false
        return true
    }

    /** Portadas, perfiles y listados que aparecen en todas las páginas del Aula. */
    private fun isGenericCrawlPage(url: String): Boolean {
        val lower = url.lowercase()
        val path = pathOf(url).lowercase().trimEnd('/')
        if (lower.contains("sesskey=")) return true
        if (path.startsWith("/mod/forum/")) return true
        return path.isEmpty() || path == "/" || path == "/index.php" ||
            path == "/course/index.php" || path.startsWith("/course/index.php/") ||
            path.startsWith("/my/") || path.startsWith("/user/") ||
            path.startsWith("/message/") || path.startsWith("/grade/") ||
            path.startsWith("/login/")
    }

    private fun isCourseEntry(url: String): Boolean {
        val path = pathOf(url).lowercase()
        return path == "/course/view.php" || ID_IN_PATH.containsMatchIn(path)
    }

    private fun pathOf(url: String): String =
        runCatching { java.net.URI(url).path.orEmpty() }.getOrDefault("")

    /** Respaldo determinista cuando el modelo no está listo o devuelve un JSON inválido. */
    fun heuristicVerdict(link: WebLink, portal: LinkPortal): LinkVerdict {
        val planLike = isPlanOrScheduleLink(link.text, link.href)
        val role = when {
            hasSyllabusUrl(link.href) -> LinkRole.SILABO
            !planLike && isSyllabusLink(link.text, link.href) -> LinkRole.SILABO
            planLike -> LinkRole.OTRO
            looksLikeCourse(link.text, link.href) -> LinkRole.CURSO
            looksLikeSection(link.text) -> LinkRole.SECCION
            else -> LinkRole.OTRO
        }
        val detail = when (role) {
            LinkRole.CURSO -> "La ruta y el texto parecen una entrada de curso."
            LinkRole.SILABO -> "El texto o la ruta mencionan un sílabo."
            LinkRole.SECCION -> "Parece una unidad, semana o tema dentro de un curso."
            LinkRole.OTRO -> if (planLike) {
                "Parece malla, plan de estudios u horario; no es un sílabo."
            } else {
                "No hay indicios suficientes en texto o ruta."
            }
        }
        val confidence = when (role) {
            LinkRole.CURSO -> 75
            LinkRole.SILABO -> 76
            LinkRole.SECCION -> 55
            LinkRole.OTRO -> if (planLike) 35 else 40
        }
        return LinkVerdict(
            link = link,
            role = role,
            reason = "Heurística · $detail${if (portal == LinkPortal.AULA) " (Aula)" else " (Intranet)"}",
            source = VerdictSource.HEURISTICA,
            confidence = confidence,
        )
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")

    private val ACCENTS = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    )

    private val NAV_WORDS = setOf(
        "inicio", "home", "salir", "cerrar sesion", "cerrar sesión", "perfil", "mi perfil",
        "ayuda", "contacto", "soporte", "notificaciones", "configuracion", "configuración",
        "buscar", "anterior", "siguiente", "volver", "regresar", "menu", "menú",
        "foro", "foros", "suscribirse", "discusion", "discusión",
    )

    private val SECTION_WORDS = listOf(
        "unidad", "semana", "tema", "modulo", "recurso", "material", "contenido",
        "capitulo", "practica", "laboratorio", "sesion", "evaluacion", "tarea",
        "actividad", "documento", "archivo", "clase", "avance",
    )
}
