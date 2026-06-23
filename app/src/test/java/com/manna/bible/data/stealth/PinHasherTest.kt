package com.manna.bible.data.stealth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [PinHasher] — the PBKDF2 PIN protection behind Stealth Mode. */
class PinHasherTest {

    @Test
    @DisplayName("a hashed PIN verifies against its own credential")
    fun verifyRoundTrip() {
        val credential = PinHasher.hash("4821")
        assertTrue(PinHasher.verify("4821", credential))
    }

    @Test
    @DisplayName("a wrong PIN does not verify")
    fun wrongPinFails() {
        val credential = PinHasher.hash("4821")
        assertFalse(PinHasher.verify("0000", credential))
        assertFalse(PinHasher.verify("482", credential))
        assertFalse(PinHasher.verify("48210", credential))
    }

    @Test
    @DisplayName("the same PIN hashes to different credentials (random salt)")
    fun saltedHashesDiffer() {
        val a = PinHasher.hash("1234")
        val b = PinHasher.hash("1234")
        assertNotEquals(a, b)
        // …yet both still verify the original PIN.
        assertTrue(PinHasher.verify("1234", a))
        assertTrue(PinHasher.verify("1234", b))
    }

    @Test
    @DisplayName("the plaintext PIN never appears in the stored credential")
    fun pinNotLeaked() {
        val credential = PinHasher.hash("9753")
        assertFalse(credential.contains("9753"))
    }

    @Test
    @DisplayName("a malformed credential returns false rather than throwing")
    fun malformedCredential() {
        assertFalse(PinHasher.verify("1234", ""))
        assertFalse(PinHasher.verify("1234", "not-a-credential"))
        assertFalse(PinHasher.verify("1234", "only-one-part"))
    }
}
