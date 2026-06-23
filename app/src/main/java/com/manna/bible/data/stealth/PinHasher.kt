package com.manna.bible.data.stealth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * One-way PIN protection for Stealth (Persecution) Mode.
 *
 * The user's PIN is never stored. Instead we derive a key with PBKDF2-HMAC-SHA256
 * over a per-credential random salt and persist only `Base64(salt):Base64(hash)`.
 * Verifying re-derives the hash from the candidate PIN and the stored salt and
 * compares in constant time, so a leaked preference store cannot reveal the PIN.
 *
 * Pure JVM crypto (`javax.crypto`) with no Android dependencies, so it runs in unit
 * tests without an emulator. PBKDF2WithHmacSHA256 is available from API 26 (minSdk).
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val SEPARATOR = ":"

    private val secureRandom = SecureRandom()

    /**
     * Derives a fresh credential string for [pin]: `Base64(salt):Base64(hash)`. A new
     * random salt is generated on every call, so the same PIN yields a different
     * credential each time.
     */
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val hash = derive(pin, salt)
        return encode(salt) + SEPARATOR + encode(hash)
    }

    /**
     * Returns true when [pin] reproduces the hash embedded in [credential] (a value
     * previously produced by [hash]). A malformed or empty credential returns false.
     */
    fun verify(pin: String, credential: String): Boolean {
        val parts = credential.split(SEPARATOR)
        if (parts.size != 2) return false
        val salt = runCatching { decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { decode(parts[1]) }.getOrNull() ?: return false
        val actual = derive(pin, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Length-independent, constant-time array comparison to avoid timing leaks. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        Base64.getDecoder().decode(value)
}
