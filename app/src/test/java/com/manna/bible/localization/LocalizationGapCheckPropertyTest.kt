package com.manna.bible.localization

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Property test for the localization gap-check (Requirement 3.3). Pure JVM — >=100 iters.
 */
class LocalizationGapCheckPropertyTest {

    private val keyArb: Arb<String> = Arb.string(1, 12, Codepoint.alphanumeric())

    @Test
    fun `a key is a gap iff present in defaults, absent in locale, and not deferred`(): Unit =
        runBlocking {
            // Feature: mass-liturgy-and-localization, Property 17: For any user-facing key and any target locale, the check flags the key as a gap if and only if it exists in the default values/, has no value in that locale, and is not recorded as a deferred item for that locale.
            val setArb = Arb.set(keyArb, 0..12)
            val probeArb = Arb.list(keyArb, 1..8)
            checkAll(200, setArb, setArb, setArb, probeArb) { defaults, locale, deferred, probes ->
                // Probe both the random universe and keys drawn from the three sets, so we
                // exercise every membership combination.
                val universe = probes + defaults + locale + deferred
                for (key in universe) {
                    val expected = key in defaults && key !in locale && key !in deferred
                    val actual = LocalizationChecks.isGap(key, defaults, locale, deferred)
                    assertEquals(
                        expected, actual,
                        "isGap('$key') mismatch (default=${key in defaults}, " +
                            "locale=${key in locale}, deferred=${key in deferred})"
                    )
                }
            }
        }
}
