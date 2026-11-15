package pe.edu.unjbg.studylock.navigation

object Routes {
    const val WELCOME = "onboarding/welcome"
    const val ACCOUNT = "onboarding/account"
    const val PERMISSIONS = "onboarding/permissions"
    const val APPS = "onboarding/apps"

    const val HOME = "home"
    const val COURSES = "courses"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    const val IMPORT = "import"

    const val COURSE_DETAIL = "course/{courseId}"
    const val START_REVIEW = "start_review/{courseId}"
    const val SESSION = "session/{courseId}"
    const val ACCESS_ACTIVE = "access_active"

    /** Identificador especial: repaso adaptativo entre todos los cursos. */
    const val WEAK = "weak"

    fun courseDetail(courseId: String) = "course/$courseId"
    fun startReview(courseId: String) = "start_review/$courseId"
    fun session(courseId: String) = "session/$courseId"

    val tabRoutes = setOf(HOME, COURSES, PROGRESS, SETTINGS)
}
