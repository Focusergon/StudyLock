package pe.edu.unjbg.studylock.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
) : ViewModel() {

    private val model = MutableStateFlow(downloader.state())

    val ui = combine(prefs.flow, model) { currentPrefs, modelState ->
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
    }

    fun setPracticeQuestions(enabled: Boolean) {
        viewModelScope.launch { prefs.setPracticeQuestionsEnabled(enabled) }
    }

    fun setRestrictedAcknowledged(acknowledged: Boolean) {
        viewModelScope.launch { prefs.setRestrictedPermissionAcknowledged(acknowledged) }
    }

    /**
     * Un solo toque acepta el modelo y lo descarga. Sin clave de API: si no hay Wi-Fi o falla la
     * verificación, el generador determinista sigue funcionando igual.
     */
    fun downloadModel() {
        if (ui.value.downloading) return
        viewModelScope.launch {
            prefs.setAiModelAccepted(true)
            prefs.setAiQuestions(true)
            model.update { ModelState(ModelStatus.DOWNLOADING, 0, "Preparando la descarga…") }
            val wifiOnly = prefs.current().modelWifiOnly
            val result = downloader.download(wifiOnly) { percent ->
                model.update { ModelState(ModelStatus.DOWNLOADING, percent, "Descargando modelo… $percent%") }
            }
            model.value = result
        }
    }

    fun refreshModel() {
        model.value = downloader.state()
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
