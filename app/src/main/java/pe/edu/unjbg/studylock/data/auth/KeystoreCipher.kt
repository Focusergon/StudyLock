package pe.edu.unjbg.studylock.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra y descifra cadenas con una clave AES-256 guardada en el **Android Keystore**.
 *
 * La clave nunca sale del almacén seguro del dispositivo, así que las credenciales institucionales
 * quedan cifradas en reposo. El formato almacenado es `base64(iv):base64(ciphertext)`.
 */
class KeystoreCipher(private val alias: String = DEFAULT_ALIAS) {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /** Cifra [plain] y devuelve el texto listo para guardar. Cadena vacía → cadena vacía. */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return encode(iv) + SEPARATOR + encode(cipherText)
    }

    /** Descifra un texto producido por [encrypt]. Devuelve cadena vacía si está vacío o corrupto. */
    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        return runCatching {
            val (ivPart, dataPart) = stored.split(SEPARATOR, limit = 2)
            if (dataPart.isEmpty()) return ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, decode(ivPart)))
            String(cipher.doFinal(decode(dataPart)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val SEPARATOR = ":"
        const val DEFAULT_ALIAS = "studylock_master_key"
    }
}
