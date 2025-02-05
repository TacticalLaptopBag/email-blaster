package com.github.tacticallaptopbag.mail_blaster

import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


private const val ALGORITHM: String = "AES/GCM/NoPadding"
private const val KEY_LENGTH: Int = 256
private const val IV_LENGTH: Int = 12
private const val TAG_LENGTH: Int = 128

class Crypt(private val _masterKey: String) {
    private fun deriveKey(salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(_masterKey.toCharArray(), salt, 65536, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(plaintext: String): String {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val key = deriveKey(salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))

        val ciphertext = cipher.doFinal(plaintext.toByteArray())

        // Combine salt, IV, and ciphertext
        val combined = salt + iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(crypttext: String): String {
        val decoded = Base64.getDecoder().decode(crypttext)

        // Split combined data
        val salt = decoded.slice(0..15).toByteArray()
        val iv = decoded.slice(16..27).toByteArray()
        val ciphertext = decoded.slice(28..decoded.lastIndex).toByteArray()

        val key = deriveKey(salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))

        return String(cipher.doFinal(ciphertext))
    }
}