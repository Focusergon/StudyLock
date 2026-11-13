package pe.edu.unjbg.studylock.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.util.InstalledApp
import pe.edu.unjbg.studylock.core.util.InstalledAppsLoader
import pe.edu.unjbg.studylock.core.util.PermissionStatus
import pe.edu.unjbg.studylock.core.util.PermissionUtils
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.domain.model.BlockedApp
import pe.edu.unjbg.studylock.domain.repo.BlockedAppsRepository

data class OnboardingUi(
    val name: String = "",
    val email: String = "",
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val error: String? = null,
    val permissions: PermissionStatus = PermissionStatus(),
    val apps: List<InstalledApp> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val loadingApps: Boolean = false,
    val saving: Boolean = false,
) {
    val canConnect: Boolean get() = name.isNotBlank() && email.contains("@") && email.contains(".")
}

class OnboardingViewModel(
    private val prefs: UserPreferences,
    private val blockedApps: BlockedAppsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUi())
    val ui: StateFlow<OnboardingUi> = _ui.asStateFlow()

    fun refreshPermissions(context: Context) {
        _ui.update {
            it.copy(
                permissions = PermissionStatus(
                    usage = PermissionUtils.hasUsageAccess(context),
                    overlay = PermissionUtils.hasOverlay(context),
                    accessibility = PermissionUtils.isAccessibilityEnabled(context),
                ),
            )
        }
    }

    fun setName(value: String) = _ui.update { it.copy(name = value, error = null) }

    fun setEmail(value: String) = _ui.update { it.copy(email = value, error = null) }

    fun connect() {
        val current = _ui.value
        if (!current.canConnect) {
            _ui.update { it.copy(error = "Usa tu correo institucional para vincular tus cursos.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(connecting = true, error = null) }
            prefs.setProfile(current.name.trim(), current.email.trim())
            _ui.update { it.copy(connecting = false, connected = true) }
        }
    }

    fun loadApps(context: Context) {
        if (_ui.value.apps.isNotEmpty() || _ui.value.loadingApps) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingApps = true) }
            val installed = withContext(Dispatchers.IO) { InstalledAppsLoader.load(context) }
            val existing = blockedApps.enabledPackages()
            val preselected = if (existing.isNotEmpty()) {
                existing
            } else {
                InstalledAppsLoader.defaultsFor(context, installed).map { it.packageName }.toSet()
            }
            _ui.update { it.copy(apps = installed, selected = preselected, loadingApps = false) }
        }
    }

    fun setQuery(value: String) = _ui.update { it.copy(query = value) }

    fun toggleApp(packageName: String) = _ui.update { state ->
        val selected = if (packageName in state.selected) {
            state.selected - packageName
        } else {
            state.selected + packageName
        }
        state.copy(selected = selected)
    }

    fun saveSelection(onDone: () -> Unit) {
        val state = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            val apps = state.apps
                .filter { it.packageName in state.selected }
                .map { BlockedApp(it.packageName, it.label, it.category, true) }
            blockedApps.saveAll(apps)
            prefs.setPermissionsAcknowledged(true)
            prefs.setOnboardingDone(true)
            _ui.update { it.copy(saving = false) }
            onDone()
        }
    }

    fun filteredApps(): List<InstalledApp> {
        val state = _ui.value
        val query = state.query.trim()
        return if (query.isEmpty()) state.apps
        else state.apps.filter { it.label.contains(query, ignoreCase = true) }
    }
}
