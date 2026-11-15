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
    }
}
