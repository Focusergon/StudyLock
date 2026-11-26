package pe.edu.unjbg.studylock.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.data.ai.ModelDownloader
import pe.edu.unjbg.studylock.data.ai.ModelState
import pe.edu.unjbg.studylock.data.ai.ModelStatus
import pe.edu.unjbg.studylock.data.prefs.Prefs
import pe.edu.unjbg.studylock.data.prefs.UserPreferences

data class SettingsUi(
    val prefs: Prefs = Prefs(),
    val loading: Boolean = true,
    val model: ModelState = ModelState(),
) {
    val downloading: Boolean get() = model.status == ModelStatus.DOWNLOADING
    val modelReady: Boolean get() = model.status == ModelStatus.READY
}

class SettingsViewModel(
    private val prefs: UserPreferences,
    private val downloader: ModelDownloader,
    private val requestDownload: () -> Unit,
) : ViewModel() {

    /** El progreso viene del descargador compartido: lo actualiza también el worker en segundo plano. */
    val ui = combine(prefs.flow, downloader.stateFlow) { currentPrefs, modelState ->
        SettingsUi(prefs = currentPrefs, loading = false, model = modelState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUi())

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

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSync(enabled) }
    }

    fun setAutoSyncWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSyncWifiOnly(enabled) }
    }

    fun setAiQuestions(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiQuestions(enabled) }
    }

    fun setModelWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setModelWifiOnly(enabled) }
        // Si cambia la restricción, se reprograma la descarga con la nueva política.
        requestDownload()
    }

    fun setDiagnosticMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDiagnosticMode(enabled) }
    }

    fun setPracticeQuestions(enabled: Boolean) {
        viewModelScope.launch { prefs.setPracticeQuestionsEnabled(enabled) }
    }

    fun setRestrictedAcknowledged(acknowledged: Boolean) {
        viewModelScope.launch { prefs.setRestrictedPermissionAcknowledged(acknowledged) }
    }

    /** Un toque reprograma la descarga con WorkManager (Wi-Fi, reintentos y notificación). */
    fun downloadModel() = requestDownload()

    fun refreshModel() {
        downloader.refresh()
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingDone(false)
            prefs.setPermissionsAcknowledged(false)
            prefs.setRestrictedPermissionAcknowledged(false)
            onDone()
        }
    }
}
