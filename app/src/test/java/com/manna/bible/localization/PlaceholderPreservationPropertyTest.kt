package com.manna.bible.localization

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Property test for the placeholder-preservation check with the human-review override
 * (Requirements 4.3, 4.4, 4.5). Pure JVM — runs at >=100 iterations.
 */
class PlaceholderPreservationPropertyTest {

    /** Format / escape tokens the check is designed to track. */
    private val tokenPool = listOf("%1\$s", "%2\$s", "%d", "%s", "%%", "\\n", "\\t")

    /** Alphabetic filler that never itself introduces a format/escape token. */
    private val filler: Arb<String> = Arb.string(0, 6, Codepoint.alphanumeric())

    /** Build a value by interleaving filler words with the given tokens (order varied). */
    private fun compose(tokens: List<String>, fillers: List<String>): String {
        val sb = StringBuilder()
        val maxLen = maxOf(tokens.size, fillers.size)
        for (i in 0 until maxLen) {
            fillers.getOrNull(i)?.let { sb.append(it).append(' ') }
            tokens.getOrNull(i)?.let { sb.append(it).append(' ') }
        }
        return sb.toString().trim()
    }

    @Test
    fun `accepted exactly when tokens match or value is human-reviewed`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 16: For any translated value of an AI Feature String, the check accepts it without a placeholder flag exactly when either its placeholder/format-token multiset equals the English Fallback's, or it is marked human-reviewed; otherwise it is flagged invalid.
        val tokensArb = Arb.list(Arb.of(tokenPool), 0..5)
        val fillerArb = Arb.list(filler, 0..5)
        // sameTokens drives whether the translation keeps the exact English token multiset.
        checkAll(
            300,
            tokensArb, fillerArb, fillerArb,
            Arb.of(tokenPool), Arb.boolean(), Arb.boolean()
        ) { englishTokens, engFiller, transFiller, extraToken, sameTokens, reviewed ->
            val english = compose(englishTokens, engFiller)
            val translationTokens = if (sameTokens) {
                englishTokens.reversed() // same multiset, different order
            } else {
                englishTokens + extraToken // strictly different multiset (one extra token)
            }
            val translation = compose(translationTokens, transFiller)

            val englishMs = LocalizationChecks.placeholderTokens(english)
            val translationMs = LocalizationChecks.placeholderTokens(translation)
            val multisetsEqual = englishMs == translationMs
            // By construction sameTokens => equal multisets; otherwise a strict superset.
            assertEquals(sameTokens, multisetsEqual, "multiset equality should match construction")

            val accepted = LocalizationChecks.placeholderAccepted(english, translation, reviewed)
            if (reviewed) {
                assertTrue(accepted, "human-reviewed values are always accepted")
            } else if (multisetsEqual) {
                assertTrue(accepted, "equal token multisets must be accepted")
            } else {
                assertFalse(accepted, "differing tokens without review must be flagged invalid")
            }
        }
    }
}
