package pe.edu.unjbg.studylock.data.prefs

import android.content.Context

/**
 * Estado del acceso temporal y del temporizador.
 *
 * Vive en SharedPreferences (no DataStore) porque lo leen y escriben servicios en segundo plano
 * de forma síncrona. Solo guarda estado efímero de sesión, nunca datos académicos.
 */
class AccessStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("access_state", Context.MODE_PRIVATE)

    var activePackage: String?
        get() = prefs.getString(KEY_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_PACKAGE, value).apply()

    var activeLabel: String
        get() = prefs.getString(KEY_LABEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LABEL, value).apply()

    var remainingSeconds: Int
        get() = prefs.getInt(KEY_REMAINING, 0)
        set(value) = prefs.edit().putInt(KEY_REMAINING, value.coerceAtLeast(0)).apply()

    var endsAt: Long
        get() = prefs.getLong(KEY_ENDS_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_ENDS_AT, value).apply()

    var lastBlockedAt: Long
        get() = prefs.getLong(KEY_LAST_BLOCKED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BLOCKED, value).apply()

    var lastForegroundPackage: String
        get() = prefs.getString(KEY_LAST_FOREGROUND, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_FOREGROUND, value).apply()

    val isActive: Boolean get() = remainingSeconds > 0

    fun start(packageName: String, label: String, seconds: Int) {
        prefs.edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_LABEL, label)
            .putInt(KEY_REMAINING, seconds)
            .putLong(KEY_ENDS_AT, System.currentTimeMillis() + seconds * 1000L)
            .apply()
    }

    fun pause() {
        prefs.edit().putInt(KEY_REMAINING, 0).putLong(KEY_ENDS_AT, 0L).apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_PACKAGE)
            .remove(KEY_LABEL)
            .putInt(KEY_REMAINING, 0)
            .putLong(KEY_ENDS_AT, 0L)
            .apply()
    }

    private companion object {
        const val KEY_PACKAGE = "active_package"
        const val KEY_LABEL = "active_label"
        const val KEY_REMAINING = "remaining_seconds"
        const val KEY_ENDS_AT = "ends_at"
        const val KEY_LAST_BLOCKED = "last_blocked_at"
        const val KEY_LAST_FOREGROUND = "last_foreground_package"
    }
}
