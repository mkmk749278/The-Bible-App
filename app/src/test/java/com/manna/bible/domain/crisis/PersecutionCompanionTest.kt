package com.manna.bible.domain.crisis

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultPersecutionCompanion] — curated, canon-safe verse sets (F-06). */
class PersecutionCompanionTest {

    private val companion = DefaultPersecutionCompanion()
    private val sixtySixBookCanon = setOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI",
        "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER",
        "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAH", "HAB", "ZEP",
        "HAG", "ZEC", "MAL", "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL",
        "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE",
        "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
    )

    @Test
    @DisplayName("all categories return non-empty lists")
    fun allCategoriesNonEmpty() {
        PersecutionCategory.entries.forEach { cat ->
            assertTrue(companion.versesFor(cat).isNotEmpty(), "Empty list for $cat")
        }
    }

    @Test
    @DisplayName("every category offers a generous set (8-12 passages)")
    fun everyCategoryGenerous() {
        PersecutionCategory.entries.forEach { cat ->
            val size = companion.versesFor(cat).size
            assertTrue(size in 8..12, "Expected 8-12 passages for $cat but found $size")
        }
    }

    @Test
    @DisplayName("all refs are within the 66-book canon")
    fun allRefsWithinCanon() {
        PersecutionCategory.entries.forEach { cat ->
            companion.versesFor(cat).forEach { ref ->
                assertTrue(
                    ref.osisId in sixtySixBookCanon,
                    "${ref.osisId} in $cat is not in the 66-book canon"
                )
            }
        }
    }

    @Test
    @DisplayName("all refs have valid chapter and verse numbers")
    fun allRefsStructurallyValid() {
        PersecutionCategory.entries.forEach { cat ->
            companion.versesFor(cat).forEach { ref ->
                assertTrue(ref.chapter >= 1, "bad chapter in $cat: $ref")
                assertTrue(ref.verse >= 1, "bad verse in $cat: $ref")
            }
        }
    }

    @Test
    @DisplayName("family rejection contains Matthew 10:34")
    fun familyRejectionContainsMatthew10v34() {
        assertTrue(
            companion.versesFor(PersecutionCategory.FAMILY_REJECTION)
                .any { it.osisId == "MAT" && it.chapter == 10 && it.verse == 34 }
        )
    }

    @Test
    @DisplayName("physical danger contains Isaiah 43:2")
    fun physicalDangerContainsIsaiah43v2() {
        assertTrue(
            companion.versesFor(PersecutionCategory.PHYSICAL_DANGER)
                .any { it.osisId == "ISA" && it.chapter == 43 && it.verse == 2 }
        )
    }

    @Test
    @DisplayName("social exclusion contains Galatians 3:28")
    fun socialExclusionContainsGalatians3v28() {
        assertTrue(
            companion.versesFor(PersecutionCategory.SOCIAL_EXCLUSION)
                .any { it.osisId == "GAL" && it.chapter == 3 && it.verse == 28 }
        )
    }

    @Test
    @DisplayName("faith crisis contains Mark 9:24")
    fun faithCrisisContainsMark9v24() {
        assertTrue(
            companion.versesFor(PersecutionCategory.FAITH_CRISIS)
                .any { it.osisId == "MRK" && it.chapter == 9 && it.verse == 24 }
        )
    }

    @Test
    @DisplayName("all five categories are offered for any denomination")
    fun categoriesForAnyDenomination() {
        assertTrue(companion.categoriesForDenomination(null).size == 5)
    }
}
