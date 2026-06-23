package com.manna.bible.data.stealth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM authenticated encryption for sensitive user data at rest (notes,
 * prayers, journal entries) when Stealth (Persecution) Mode is active — the
 * "AES-256" requirement from the security spec.
 *
 * A 256-bit key is derived from a passphrase (e.g. the unlocked stealth PIN) via
 * PBKDF2-HMAC-SHA256 over a caller-provided salt, then used with GCM, which provides
 * both confidentiality and tamper detection. Each [encrypt] generates a fresh random
 * 12-byte IV, prepended to the ciphertext and Base64-encoded for storage.
 *
 * Pure JVM crypto (`javax.crypto`) — no Android dependencies, so it is exercised in
 * unit tests without an emulator. AES/GCM and PBKDF2WithHmacSHA256 are available from
 * API 26 (the app's minSdk).
 */
object AesGcm {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val DERIVE_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /** Derives a reusable 256-bit AES key from [passphrase] and [salt]. */
    fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(DERIVE_ALGORITHM).generateSecret(spec).encoded
            SecretKeySpec(bytes, KEY_ALGORITHM)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Encrypts [plaintext] under [key], returning Base64(`iv || ciphertext||tag`). A
     * fresh random IV is used for every call so identical plaintext encrypts differently.
     */
    fun encrypt(plaintext: String, key: SecretKey): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Reverses [encrypt]. Returns null when [encoded] is malformed or its GCM tag fails
     * to authenticate (wrong key or tampered data) rather than throwing.
     */
    fun decrypt(encoded: String, key: SecretKey): String? = runCatching {
        val combined = Base64.getDecoder().decode(encoded)
        require(combined.size > IV_LENGTH_BYTES)
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()
}
