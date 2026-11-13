package pe.edu.unjbg.studylock.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.data.prefs.Prefs
import pe.edu.unjbg.studylock.data.prefs.UserPreferences

data class SettingsUi(
    val prefs: Prefs = Prefs(),
    val loading: Boolean = true,
)

class SettingsViewModel(
    private val prefs: UserPreferences,
) : ViewModel() {

    val ui = prefs.flow
        .map { SettingsUi(prefs = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUi())

    fun increaseGoal() = setGoal(ui.value.prefs.dailyGoalMinutes + 5)

    fun decreaseGoal() = setGoal(ui.value.prefs.dailyGoalMinutes - 5)

    private fun setGoal(minutes: Int) {
        viewModelScope.launch { prefs.setDailyGoal(minutes.coerceIn(5, 180)) }
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch { prefs.setRemindersEnabled(enabled) }
    }

    fun setEmergency(enabled: Boolean) {
        viewModelScope.launch { prefs.setEmergencyEnabled(enabled) }
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingDone(false)
            prefs.setPermissionsAcknowledged(false)
            onDone()
        }
    }
}
