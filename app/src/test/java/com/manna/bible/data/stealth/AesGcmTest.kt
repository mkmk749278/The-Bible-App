package com.manna.bible.data.stealth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [AesGcm] — AES-256-GCM at-rest encryption for sensitive data. */
class AesGcmTest {

    private val salt = ByteArray(16) { it.toByte() }

    @Test
    @DisplayName("ciphertext decrypts back to the original plaintext")
    fun roundTrip() {
        val key = AesGcm.deriveKey("unlock-pin-4821", salt)
        val plaintext = "Father, into your hands I commit my spirit."
        val encrypted = AesGcm.encrypt(plaintext, key)
        assertEquals(plaintext, AesGcm.decrypt(encrypted, key))
    }

    @Test
    @DisplayName("encryption is non-deterministic (fresh IV each time)")
    fun nonDeterministic() {
        val key = AesGcm.deriveKey("pass", salt)
        assertNotEquals(AesGcm.encrypt("same", key), AesGcm.encrypt("same", key))
    }

    @Test
    @DisplayName("plaintext does not appear in the ciphertext")
    fun plaintextHidden() {
        val key = AesGcm.deriveKey("pass", salt)
        val encrypted = AesGcm.encrypt("SECRET-VERSE", key)
        assertFalse(encrypted.contains("SECRET-VERSE"))
    }

    @Test
    @DisplayName("decrypting with the wrong key fails authentication and returns null")
    fun wrongKeyReturnsNull() {
        val encrypted = AesGcm.encrypt("hidden", AesGcm.deriveKey("right", salt))
        assertNull(AesGcm.decrypt(encrypted, AesGcm.deriveKey("wrong", salt)))
    }

    @Test
    @DisplayName("tampered ciphertext returns null rather than throwing")
    fun tamperedReturnsNull() {
        val key = AesGcm.deriveKey("pass", salt)
        assertNull(AesGcm.decrypt("not-valid-base64!!", key))
        assertNull(AesGcm.decrypt("", key))
    }
}
