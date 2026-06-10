package com.manna.bible.data.preferences

import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Unit + property-style tests for [SetupPreferencesMapper].
 *
 * Validates the persistence idempotence property (Req 10): for any [SetupState] `s`,
 * `fromMap(toMap(s)) == s`, including null preservation and the boolean flags.
 */
class SetupPreferencesMapperTest {

    // --- Example-based tests --------------------------------------------------

    @Test
    fun `fully populated state round-trips unchanged`() {
        val state = SetupState(
            denomination = Denomination.CATHOLIC,
            canonType = CanonType.CATHOLIC_73,
            uiLanguage = "en",
            bibleLanguage = "ml",
            numberingScheme = NumberingScheme.SEPTUAGINT,
            namingConventionId = "catholic_naming",
            bibleTranslationId = "nrsv_ce",
            lectionaryId = "rc_calendar",
            showDeuterocanonical = true,
            setupCompleted = true
        )

        assertEquals(state, SetupPreferencesMapper.fromMap(SetupPreferencesMapper.toMap(state)))
    }

    @Test
    fun `all-null selection state round-trips and preserves nulls`() {
        val state = SetupState(
            denomination = null,
            canonType = null,
            uiLanguage = null,
            bibleLanguage = null,
            numberingScheme = null,
            namingConventionId = null,
            bibleTranslationId = null,
            lectionaryId = null,
            showDeuterocanonical = false,
            setupCompleted = false
        )

        val map = SetupPreferencesMapper.toMap(state)

        // Null selection fields must be absent from the serialized map.
        assertFalse(map.containsKey(SetupPreferencesMapper.Keys.DENOMINATION))
        assertFalse(map.containsKey(SetupPreferencesMapper.Keys.NUMBERING_SCHEME))
        assertFalse(map.containsKey(SetupPreferencesMapper.Keys.LECTIONARY))

        assertEquals(state, SetupPreferencesMapper.fromMap(map))
    }

    @Test
    fun `missing booleans default to false on read`() {
        val state = SetupPreferencesMapper.fromMap(emptyMap())

        assertFalse(state.setupCompleted)
        assertFalse(state.showDeuterocanonical)
        assertNull(state.denomination)
        assertNull(state.canonType)
        assertNull(state.numberingScheme)
    }

    @Test
    fun `unresolvable enum ids deserialize to null`() {
        val map = mapOf<String, Any>(
            SetupPreferencesMapper.Keys.DENOMINATION to "not_a_denomination",
            SetupPreferencesMapper.Keys.CANON to "not_a_canon",
            SetupPreferencesMapper.Keys.NUMBERING_SCHEME to "NOT_A_SCHEME"
        )

        val state = SetupPreferencesMapper.fromMap(map)

        assertNull(state.denomination)
        assertNull(state.canonType)
        assertNull(state.numberingScheme)
    }

    // --- Property-based test (Req 10: persistence idempotence) ----------------

    @Test
    fun `property - toMap then fromMap is identity for arbitrary states`() {
        val random = Random(seed = 20240611)
        repeat(500) {
            val state = randomSetupState(random)
            val roundTripped = SetupPreferencesMapper.fromMap(SetupPreferencesMapper.toMap(state))
            assertEquals(state, roundTripped, "Round-trip failed for: $state")
        }
    }

    private fun randomSetupState(random: Random): SetupState =
        SetupState(
            denomination = random.nullableOf { Denomination.entries.random(random) },
            canonType = random.nullableOf { CanonType.entries.random(random) },
            uiLanguage = random.nullableOf { random.languageCode() },
            bibleLanguage = random.nullableOf { random.languageCode() },
            numberingScheme = random.nullableOf { NumberingScheme.entries.random(random) },
            namingConventionId = random.nullableOf { "naming_" + random.nextInt(100) },
            bibleTranslationId = random.nullableOf { "trans_" + random.nextInt(100) },
            lectionaryId = random.nullableOf { "lect_" + random.nextInt(100) },
            showDeuterocanonical = random.nextBoolean(),
            setupCompleted = random.nextBoolean()
        )

    private fun <T> Random.nullableOf(producer: () -> T): T? =
        if (nextBoolean()) producer() else null

    private fun Random.languageCode(): String =
        listOf("en", "ml", "ta", "hi", "es", "fr", "de").random(this)
}
