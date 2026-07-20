package com.applimit.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure storage for the child PIN and the parent PIN/password (Prompt Punkt 1
 * and 6).
 *
 * Security model:
 *  - We never store the PIN in clear text. We store a random per-PIN salt plus
 *    a PBKDF2-SHA256 hash of the PIN.
 *  - Salt + hash themselves live in EncryptedSharedPreferences, whose backing
 *    key is held in the Android Keystore (hardware-backed where available).
 *
 * So even reading the raw prefs file off a rooted device yields only an
 * encrypted blob, and even that only contains a salted hash — not the PIN.
 */
class SecurePinStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "applimit_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isChildPinSet(): Boolean = prefs.contains("$CHILD.hash")
    fun isParentPinSet(): Boolean = prefs.contains("$PARENT.hash")

    fun setChildPin(pin: String) = store(CHILD, pin)
    fun setParentPin(pin: String) = store(PARENT, pin)

    fun verifyChildPin(pin: String): Boolean = verify(CHILD, pin)
    fun verifyParentPin(pin: String): Boolean = verify(PARENT, pin)

    /** Change the parent PIN after verifying the old one. Returns false if wrong. */
    fun changeParentPin(oldPin: String, newPin: String): Boolean {
        if (!verify(PARENT, oldPin)) return false
        store(PARENT, newPin)
        return true
    }

    /** Change the child PIN after verifying the old one. */
    fun changeChildPin(oldPin: String, newPin: String): Boolean {
        if (!verify(CHILD, oldPin)) return false
        store(CHILD, newPin)
        return true
    }

    private fun store(prefix: String, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString("$prefix.salt", salt.toBase64())
            .putString("$prefix.hash", hash.toBase64())
            .apply()
    }

    private fun verify(prefix: String, pin: String): Boolean {
        val salt = prefs.getString("$prefix.salt", null)?.fromBase64() ?: return false
        val expected = prefs.getString("$prefix.hash", null)?.fromBase64() ?: return false
        val actual = pbkdf2(pin, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun ByteArray.toBase64() = android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
    private fun String.fromBase64() = android.util.Base64.decode(this, android.util.Base64.NO_WRAP)

    companion object {
        private const val CHILD = "child_pin"
        private const val PARENT = "parent_pin"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
    }
}
