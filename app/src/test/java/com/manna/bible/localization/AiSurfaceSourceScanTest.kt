package com.manna.bible.localization

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Source-scan guard (Requirements 1.2, 8.4). Reads the real Kotlin sources of the six
 * PR #80 AI surfaces plus the Liturgy_Detail framing and asserts their static, user-facing
 * strings are resolved through the Bible-language resolver (`stringResourceIn`) rather than
 * a raw `stringResource(` call against the UI locale. This keeps the migration enforceable
 * over time: a regression that reintroduces `stringResource(` for these strings fails here.
 *
 * Note: the literal substring "stringResource(" does NOT match "stringResourceIn(" (the
 * latter has "In" before the paren), so the raw-call check is precise.
 */
class AiSurfaceSourceScanTest {

    /** Surfaces migrated in full — every user-facing string goes through stringResourceIn. */
    private val fullyMigrated = listOf(
        "ui/crisis/CrisisModeScreen.kt",
        "ui/sermon/SermonHelperScreen.kt",
        "ui/card/ScriptureCardScreen.kt",
        "ui/card/VerseRecommendationScreen.kt",
        "ui/card/VerseCardSheet.kt"
    )

    @Test
    fun `fully-migrated AI surfaces contain no raw stringResource calls and use stringResourceIn`() {
        for (path in fullyMigrated) {
            val src = LocalizationTestSupport.readMainSource(path)
            assertFalse(
                src.contains("stringResource("),
                "$path still contains a raw stringResource( call (should use stringResourceIn)"
            )
            assertTrue(
                src.contains("stringResourceIn("),
                "$path does not use stringResourceIn at all"
            )
        }
    }

    @Test
    fun `ReaderScreen resolves explain_ strings via stringResourceIn, never raw`() {
        val src = LocalizationTestSupport.readMainSource("ui/reader/ReaderScreen.kt")
        assertFalse(
            src.contains("stringResource(R.string.explain_"),
            "ReaderScreen still resolves an explain_ string with a raw stringResource( call"
        )
        assertTrue(
            src.contains("stringResourceIn(") &&
                Regex("""stringResourceIn\([^)]*R\.string\.explain_""").containsMatchIn(src),
            "ReaderScreen does not resolve explain_ strings via stringResourceIn"
        )
    }

    @Test
    fun `LiturgyDetailScreen framing strings use stringResourceIn, never raw church_ strings`() {
        val src = LocalizationTestSupport.readMainSource("ui/church/LiturgyDetailScreen.kt")
        assertFalse(
            src.contains("stringResource(R.string.church_"),
            "LiturgyDetailScreen resolves a church_ framing string with a raw stringResource( call"
        )
        assertTrue(
            src.contains("stringResourceIn("),
            "LiturgyDetailScreen does not use stringResourceIn for framing"
        )
    }
}
