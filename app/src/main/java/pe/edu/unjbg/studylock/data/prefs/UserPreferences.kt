package pe.edu.unjbg.studylock.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "studylock_prefs")

data class Prefs(
    val userName: String = "Alex",
    val academicEmail: String = "",
    val onboardingDone: Boolean = false,
    val permissionsAcknowledged: Boolean = false,
    val dailyGoalMinutes: Int = 30,
    val emergencyEnabled: Boolean = true,
    val lastEmergencyAt: Long = 0L,
    val remindersEnabled: Boolean = true,
    val lastNotifiedAt: Long = 0L,
    val autoSync: Boolean = true,
    val autoSyncWifiOnly: Boolean = true,
    val lastAutoSyncAt: Long = 0L,
    val aiQuestions: Boolean = true,
    val modelWifiOnly: Boolean = true,
    val aiModelAccepted: Boolean = false,
    val practiceQuestionsEnabled: Boolean = true,
    val restrictedPermissionAcknowledged: Boolean = false,
    val diagnosticMode: Boolean = false,
) {
    val displayName: String get() = userName.ifBlank { "Estudiante" }
}

class UserPreferences(private val context: Context) {

    val flow: Flow<Prefs> = context.userDataStore.data.map { p ->
        Prefs(
            userName = p[Keys.NAME] ?: "Alex",
            academicEmail = p[Keys.EMAIL] ?: "",
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            permissionsAcknowledged = p[Keys.PERMISSIONS] ?: false,
            dailyGoalMinutes = p[Keys.GOAL] ?: 30,
            emergencyEnabled = p[Keys.EMERGENCY] ?: true,
            lastEmergencyAt = p[Keys.LAST_EMERGENCY] ?: 0L,
            remindersEnabled = p[Keys.REMINDERS] ?: true,
            lastNotifiedAt = p[Keys.LAST_NOTIFIED] ?: 0L,
            autoSync = p[Keys.AUTO_SYNC] ?: true,
            autoSyncWifiOnly = p[Keys.AUTO_SYNC_WIFI] ?: true,
            lastAutoSyncAt = p[Keys.LAST_AUTO_SYNC] ?: 0L,
            aiQuestions = p[Keys.AI_QUESTIONS] ?: true,
            modelWifiOnly = p[Keys.MODEL_WIFI] ?: true,
            aiModelAccepted = p[Keys.AI_MODEL_ACCEPTED] ?: false,
            practiceQuestionsEnabled = p[Keys.PRACTICE_QUESTIONS] ?: true,
            restrictedPermissionAcknowledged = p[Keys.RESTRICTED_PERMISSION] ?: false,
            diagnosticMode = p[Keys.DIAGNOSTIC_MODE] ?: false,
        )
    }

    /** Lectura puntual del estado actual (p. ej. desde un worker en segundo plano). */
    suspend fun current(): Prefs = flow.first()

    suspend fun setOnboardingDone(done: Boolean) = put { it[Keys.ONBOARDING] = done }
    suspend fun setPermissionsAcknowledged(done: Boolean) = put { it[Keys.PERMISSIONS] = done }
    suspend fun setProfile(name: String, email: String) = put {
        it[Keys.NAME] = name
        it[Keys.EMAIL] = email
    }
    suspend fun setDailyGoal(minutes: Int) = put { it[Keys.GOAL] = minutes }
    suspend fun setEmergencyEnabled(enabled: Boolean) = put { it[Keys.EMERGENCY] = enabled }
    suspend fun markEmergencyUsed(at: Long) = put { it[Keys.LAST_EMERGENCY] = at }
    suspend fun setRemindersEnabled(enabled: Boolean) = put { it[Keys.REMINDERS] = enabled }
    suspend fun markNotified(at: Long) = put { it[Keys.LAST_NOTIFIED] = at }
    suspend fun setAutoSync(enabled: Boolean) = put { it[Keys.AUTO_SYNC] = enabled }
    suspend fun setAutoSyncWifiOnly(enabled: Boolean) = put { it[Keys.AUTO_SYNC_WIFI] = enabled }
    suspend fun markAutoSyncAttempt(at: Long) = put { it[Keys.LAST_AUTO_SYNC] = at }
    suspend fun setAiQuestions(enabled: Boolean) = put { it[Keys.AI_QUESTIONS] = enabled }
    suspend fun setModelWifiOnly(enabled: Boolean) = put { it[Keys.MODEL_WIFI] = enabled }
    suspend fun setAiModelAccepted(accepted: Boolean) = put { it[Keys.AI_MODEL_ACCEPTED] = accepted }

    /** Solo desactiva las 5 preguntas de prueba; los repasos y retos siguen igual. */
    suspend fun setPracticeQuestionsEnabled(enabled: Boolean) = put { it[Keys.PRACTICE_QUESTIONS] = enabled }

    /** Confirmación del paso "Permisos restringidos" (no detectable por API pública). */
    suspend fun setRestrictedPermissionAcknowledged(acknowledged: Boolean) =
        put { it[Keys.RESTRICTED_PERMISSION] = acknowledged }

    /** Muestra cada paso de la vinculación y pide confirmar los enlaces clasificados. */
    suspend fun setDiagnosticMode(enabled: Boolean) = put { it[Keys.DIAGNOSTIC_MODE] = enabled }

    private suspend fun put(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.userDataStore.edit { prefs -> block(prefs) }
    }

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("academic_email")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PERMISSIONS = booleanPreferencesKey("permissions_ack")
        val GOAL = intPreferencesKey("daily_goal_minutes")
        val EMERGENCY = booleanPreferencesKey("emergency_enabled")
        val LAST_EMERGENCY = longPreferencesKey("last_emergency_at")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val LAST_NOTIFIED = longPreferencesKey("last_notified_at")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val AUTO_SYNC_WIFI = booleanPreferencesKey("auto_sync_wifi_only")
        val LAST_AUTO_SYNC = longPreferencesKey("last_auto_sync_at")
        val AI_QUESTIONS = booleanPreferencesKey("ai_questions")
        val MODEL_WIFI = booleanPreferencesKey("model_wifi_only")
        val AI_MODEL_ACCEPTED = booleanPreferencesKey("ai_model_accepted")
        val PRACTICE_QUESTIONS = booleanPreferencesKey("practice_questions_enabled")
        val RESTRICTED_PERMISSION = booleanPreferencesKey("restricted_permission_ack")
        val DIAGNOSTIC_MODE = booleanPreferencesKey("diagnostic_mode")
    }
}
