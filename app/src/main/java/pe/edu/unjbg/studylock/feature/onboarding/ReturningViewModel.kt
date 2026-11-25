package pe.edu.unjbg.studylock.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.data.auth.CredentialStore
import pe.edu.unjbg.studylock.data.prefs.UserPreferences

data class ReturningUi(
    val loaded: Boolean = false,
    val name: String = "",
    val email: String = "",
    /** Las credenciales UNJBG ya están cifradas en este dispositivo. */
    val hasSavedAccount: Boolean = false,
    val error: String? = null,
) {
    val canContinue: Boolean get() = email.contains("@") && email.contains(".")
}

/**
 * Ruta corta para quien ya configuró la app: solo confirma el correo institucional.
 *
 * El nombre, los permisos, las apps limitadas y las credenciales UNJBG no se vuelven a pedir;
 * si no hay credenciales guardadas (p. ej. tras desinstalar), la UI ofrece el onboarding completo.
 */
class ReturningViewModel(
    private val prefs: UserPreferences,
    credentials: CredentialStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(ReturningUi())
    val ui: StateFlow<ReturningUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val current = prefs.current()
            val saved = credentials.current()
            _ui.update {
                it.copy(
                    loaded = true,
                    name = current.userName,
                    email = current.academicEmail,
                    hasSavedAccount = saved.hasAula,
                )
            }
        }
    }

    fun setEmail(value: String) = _ui.update { it.copy(email = value.trim(), error = null) }

    fun continueToApp(onDone: () -> Unit) {
        val state = _ui.value
        if (!state.canContinue) {
            _ui.update { it.copy(error = "Escribe tu correo institucional para continuar.") }
            return
        }
        viewModelScope.launch {
            prefs.setProfile(state.name.ifBlank { "Estudiante" }, state.email)
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}
