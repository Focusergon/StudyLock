package pe.edu.unjbg.studylock.navigation

object Routes {
    const val WELCOME = "onboarding/welcome"
    /** Retorno rápido: solo confirma el correo; no repite permisos, apps ni credenciales. */
    const val RETURNING = "onboarding/returning"
    const val ACCOUNT = "onboarding/account"
    const val PERMISSIONS = "onboarding/permissions"
    const val APPS = "onboarding/apps"

    const val HOME = "home"
    const val COURSES = "courses"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    const val IMPORT = "import"
    const val GUIDE = "guide"
    const val SCHEDULE = "schedule"
    const val BLOCKED_APPS = "blocked_apps"
    const val ACCOUNTS = "accounts"
    /** Patrón con argumento para el auto-sync diario (Navigation lo parsea como `autoSync`). */
    const val ACCOUNTS_AUTO = "accounts?autoSync={autoSync}"

    fun accounts(autoSync: Boolean = false): String =
        if (autoSync) "accounts?autoSync=true" else ACCOUNTS

    const val COURSE_DETAIL = "course/{courseId}"
    const val START_REVIEW = "start_review/{courseId}"
    const val SESSION = "session/{courseId}"
    /** 5 preguntas de prueba desde el inicio: minutos extra, sin estadísticas. */
    const val PRACTICE = "session/practice"
    const val ACCESS_ACTIVE = "access_active"
    /** Visor de errores dentro de la app, para copiarlos sin cable. */
    const val LOGS = "logs"

    /** Identificador especial: repaso adaptativo entre todos los cursos. */
    const val WEAK = "weak"

    fun courseDetail(courseId: String) = "course/$courseId"
    fun startReview(courseId: String) = "start_review/$courseId"
    fun session(courseId: String) = "session/$courseId"

    val tabRoutes = setOf(HOME, COURSES, PROGRESS, SETTINGS)
}
