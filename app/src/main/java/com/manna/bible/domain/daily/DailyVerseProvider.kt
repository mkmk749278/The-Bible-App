package com.manna.bible.domain.daily

import com.manna.bible.domain.usecase.ReadingRef
import java.time.LocalDate
import javax.inject.Inject

/**
 * Supplies a deterministic "Verse of the Day" (Phase 1 — daily verse).
 *
 * The selection is fully offline and depends only on the calendar date, so every
 * device shows the same verse on a given day and the same date always yields the
 * same verse. References are drawn only from the 66-book Protestant canon, so they
 * are present in every supported canon (including the bundled World English Bible).
 *
 * Pure Kotlin — no Android dependencies.
 */
interface DailyVerseProvider {
    /** Returns the canonical reference for the verse of the day on [date]. */
    fun verseForDate(date: LocalDate): ReadingRef
}

/**
 * Default [DailyVerseProvider]. Rotates through a curated list of well-known,
 * encouraging verses by the date's epoch day, so the rotation is stable and
 * uniform regardless of time zone year boundaries.
 */
class DefaultDailyVerseProvider @Inject constructor() : DailyVerseProvider {

    override fun verseForDate(date: LocalDate): ReadingRef {
        val index = date.toEpochDay().mod(VERSES.size)
        return VERSES[index]
    }

    private companion object {
        /** Curated verses (USFM book ids), all within the 66-book canon. */
        val VERSES: List<ReadingRef> = listOf(
            ReadingRef("JHN", 3, 16),
            ReadingRef("PSA", 23, 1),
            ReadingRef("PRO", 3, 5),
            ReadingRef("ROM", 8, 28),
            ReadingRef("PHP", 4, 13),
            ReadingRef("ISA", 41, 10),
            ReadingRef("JER", 29, 11),
            ReadingRef("MAT", 6, 33),
            ReadingRef("JOS", 1, 9),
            ReadingRef("PSA", 46, 1),
            ReadingRef("ROM", 12, 2),
            ReadingRef("PHP", 4, 6),
            ReadingRef("ISA", 40, 31),
            ReadingRef("PSA", 119, 105),
            ReadingRef("MAT", 11, 28),
            ReadingRef("2CO", 5, 17),
            ReadingRef("GAL", 2, 20),
            ReadingRef("EPH", 2, 8),
            ReadingRef("HEB", 11, 1),
            ReadingRef("1CO", 13, 4),
            ReadingRef("PSA", 27, 1),
            ReadingRef("PRO", 16, 3),
            ReadingRef("ROM", 5, 8),
            ReadingRef("JHN", 14, 6),
            ReadingRef("PSA", 34, 8),
            ReadingRef("ISA", 26, 3),
            ReadingRef("MAT", 5, 16),
            ReadingRef("PSA", 91, 1),
            ReadingRef("1PE", 5, 7),
            ReadingRef("PHP", 1, 6),
            ReadingRef("PSA", 37, 4),
            ReadingRef("PRO", 18, 10),
            ReadingRef("ROM", 15, 13),
            ReadingRef("COL", 3, 23),
            ReadingRef("PSA", 55, 22),
            ReadingRef("2TI", 1, 7),
            ReadingRef("JHN", 16, 33),
            ReadingRef("PSA", 121, 1),
            ReadingRef("HEB", 13, 5),
            ReadingRef("JAS", 1, 2)
        )
    }
}
