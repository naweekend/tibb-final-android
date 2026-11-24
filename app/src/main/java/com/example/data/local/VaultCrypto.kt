package com.example.data.local

import android.util.Base64
import com.example.data.model.EncryptedEnvelope
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private val random = SecureRandom()

    fun generateSaltBase64(bytes: Int = 16): String {
        val salt = ByteArray(bytes)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun deriveAesKey(secret: String, saltBase64: String, iterations: Int = PBKDF2_ITERATIONS): SecretKey {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
        val raw = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    fun deriveVerifierBase64(pin: String, saltBase64: String): String {
        val key = deriveAesKey("TIBB_PIN_CHECK::$pin", saltBase64)
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun encrypt(plainText: String, key: SecretKey): EncryptedEnvelope {
        val iv = ByteArray(IV_BYTES)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedEnvelope(
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            cipherBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
        )
    }

    fun decrypt(envelope: EncryptedEnvelope, key: SecretKey): String {
        val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(envelope.cipherBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = cipher.doFinal(cipherBytes)
        return String(plain, Charsets.UTF_8)
    }

    fun validatePinFormat(pin: String): Boolean = pin.matches(Regex("^\\d{4,8}$"))
}
