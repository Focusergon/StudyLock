package pe.edu.unjbg.studylock.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.secureDataStore by preferencesDataStore(name = "studylock_secure")

/**
 * Credenciales de UNJBG que el estudiante decide guardar.
 *
 * La contraseña de la Intranet puede ser la misma que la del Aula Virtual (casilla «misma
 * contraseña»); en ese caso no se pide por separado.
 */
data class UnjbgCredentials(
    val aulaUser: String = "",
    val aulaPassword: String = "",
    val intranetUser: String = "",
    val intranetPassword: String = "",
    val samePassword: Boolean = true,
) {
    /** Usuario de la Intranet; si no se indicó, se reutiliza el del Aula Virtual. */
    val effectiveIntranetUser: String get() = intranetUser.ifBlank { aulaUser }

    /** Contraseña de la Intranet; si «misma contraseña», se reutiliza la del Aula Virtual. */
    val effectiveIntranetPassword: String get() = if (samePassword) aulaPassword else intranetPassword

    val hasAula: Boolean get() = aulaUser.isNotBlank() && aulaPassword.isNotBlank()

    val hasIntranet: Boolean
        get() = effectiveIntranetUser.isNotBlank() && effectiveIntranetPassword.isNotBlank()

    val hasAny: Boolean get() = hasAula || hasIntranet
}

/**
 * Guarda las credenciales cifradas con [KeystoreCipher] dentro de un DataStore aparte.
 *
 * Nunca se escribe nada en claro ni en logs; si el Keystore falla, se conserva lo anterior y se
 * propaga el error para que la UI lo muestre.
 */
class CredentialStore(
    private val context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) {

    val flow: Flow<UnjbgCredentials> = context.secureDataStore.data.map { prefs ->
        UnjbgCredentials(
            aulaUser = cipher.decrypt(prefs[Keys.AULA_USER].orEmpty()),
            aulaPassword = cipher.decrypt(prefs[Keys.AULA_PASSWORD].orEmpty()),
            intranetUser = cipher.decrypt(prefs[Keys.INTRANET_USER].orEmpty()),
            intranetPassword = cipher.decrypt(prefs[Keys.INTRANET_PASSWORD].orEmpty()),
            samePassword = prefs[Keys.SAME_PASSWORD] ?: true,
        )
    }

    suspend fun current(): UnjbgCredentials = flow.first()

    suspend fun save(credentials: UnjbgCredentials) {
        context.secureDataStore.edit { prefs ->
            prefs[Keys.AULA_USER] = cipher.encrypt(credentials.aulaUser.trim())
            prefs[Keys.AULA_PASSWORD] = cipher.encrypt(credentials.aulaPassword)
            prefs[Keys.INTRANET_USER] = cipher.encrypt(credentials.intranetUser.trim())
            prefs[Keys.INTRANET_PASSWORD] = cipher.encrypt(
                if (credentials.samePassword) "" else credentials.intranetPassword,
            )
            prefs[Keys.SAME_PASSWORD] = credentials.samePassword
        }
    }

    suspend fun clear() {
        context.secureDataStore.edit { it.clear() }
    }

    private object Keys {
        val AULA_USER = stringPreferencesKey("aula_user")
        val AULA_PASSWORD = stringPreferencesKey("aula_password")
        val INTRANET_USER = stringPreferencesKey("intranet_user")
        val INTRANET_PASSWORD = stringPreferencesKey("intranet_password")
        val SAME_PASSWORD = booleanPreferencesKey("same_password")
    }
}
