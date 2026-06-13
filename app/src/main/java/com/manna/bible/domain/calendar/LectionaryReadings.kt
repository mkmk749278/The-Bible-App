package com.manna.bible.domain.calendar

import com.manna.bible.domain.model.Denomination
import java.time.LocalDate
import javax.inject.Inject

/** The role a reading plays in the liturgy of the day. */
enum class ReadingKind { FIRST, PSALM, SECOND, GOSPEL }

/**
 * One appointed reading: its [kind] and the passage. [verse]..[endVerse] is the
 * displayed range (same chapter); [endVerse] is null for a single verse or a passage
 * whose extent we render only by its opening verse. The opening verse is what the
 * reader navigates to.
 */
data class LectionaryReading(
    val kind: ReadingKind,
    val osisId: String,
    val chapter: Int,
    val verse: Int,
    val endVerse: Int? = null
)

/**
 * Supplies the appointed readings for a date in the chosen tradition.
 *
 * Scope: the **principal feasts and holy days** of the liturgical year carry their
 * proper readings (Old Testament · Psalm · Epistle · Gospel) drawn from the widely
 * shared Western (Revised Common / Roman) selections. The full weekday/Sunday cycle
 * (the three-year lectionary) is curated data that is added over time; until then,
 * ordinary days return no specific readings and the day detail shows the season alone.
 *
 * Pure Kotlin — no Android dependencies, fully unit-testable.
 */
interface LectionaryReadingsProvider {
    /** Appointed readings for [date], or empty when the day has no proper readings. */
    fun readingsFor(date: LocalDate, denomination: Denomination): List<LectionaryReading>
}

/**
 * Default [LectionaryReadingsProvider]. Resolves the feast on [date] via
 * [JesusEventsProvider] and returns that feast's proper readings. The principal feasts
 * share the same readings across the Western traditions, so [Denomination] does not yet
 * vary the result.
 */
class DefaultLectionaryReadingsProvider @Inject constructor(
    private val jesusEvents: JesusEventsProvider
) : LectionaryReadingsProvider {

    override fun readingsFor(date: LocalDate, denomination: Denomination): List<LectionaryReading> {
        val feastId = jesusEvents.entriesFor(date.year)
            .firstOrNull { it.date == date }
            ?.id ?: return emptyList()
        return READINGS[feastId].orEmpty()
    }

    private companion object {
        private fun first(osis: String, ch: Int, v: Int, end: Int? = null) =
            LectionaryReading(ReadingKind.FIRST, osis, ch, v, end)

        private fun psalm(ch: Int, v: Int, end: Int? = null) =
            LectionaryReading(ReadingKind.PSALM, "PSA", ch, v, end)

        private fun second(osis: String, ch: Int, v: Int, end: Int? = null) =
            LectionaryReading(ReadingKind.SECOND, osis, ch, v, end)

        private fun gospel(osis: String, ch: Int, v: Int, end: Int? = null) =
            LectionaryReading(ReadingKind.GOSPEL, osis, ch, v, end)

        /**
         * Proper readings for the principal feasts, keyed by the ids from
         * [JesusEventsProvider]. Selections follow the widely shared Western lectionary
         * for these days. Passages that span chapters are rendered by their opening
         * verse (no [LectionaryReading.endVerse]).
         */
        val READINGS: Map<String, List<LectionaryReading>> = mapOf(
            "holy_name" to listOf(
                first("NUM", 6, 22, 27), psalm(8, 1), second("GAL", 4, 4, 7), gospel("LUK", 2, 15, 21)
            ),
            "nativity" to listOf(
                first("ISA", 9, 2, 7), psalm(96, 1), second("TIT", 2, 11, 14), gospel("LUK", 2, 1, 14)
            ),
            "holy_innocents" to listOf(
                first("JER", 31, 15, 17), psalm(124, 1), gospel("MAT", 2, 13, 18)
            ),
            "epiphany" to listOf(
                first("ISA", 60, 1, 6), psalm(72, 1, 7), second("EPH", 3, 1, 12), gospel("MAT", 2, 1, 12)
            ),
            "presentation" to listOf(
                first("MAL", 3, 1, 4), psalm(24, 7, 10), second("HEB", 2, 14, 18), gospel("LUK", 2, 22, 40)
            ),
            "annunciation" to listOf(
                first("ISA", 7, 10, 14), psalm(40, 5, 10), second("HEB", 10, 4, 10), gospel("LUK", 1, 26, 38)
            ),
            "transfiguration" to listOf(
                first("EXO", 34, 29, 35), psalm(99, 1), second("2PE", 1, 16, 19), gospel("LUK", 9, 28, 36)
            ),
            "ash_wednesday" to listOf(
                first("JOL", 2, 1, 2), psalm(51, 1, 17), second("2CO", 5, 20), gospel("MAT", 6, 1, 21)
            ),
            "palm_sunday" to listOf(
                first("ISA", 50, 4, 9), psalm(31, 9, 16), second("PHP", 2, 5, 11), gospel("MAT", 21, 1, 11)
            ),
            "maundy_thursday" to listOf(
                first("EXO", 12, 1, 14), psalm(116, 1), second("1CO", 11, 23, 26), gospel("JHN", 13, 1)
            ),
            "good_friday" to listOf(
                first("ISA", 52, 13), psalm(22, 1), second("HEB", 10, 16, 25), gospel("JHN", 18, 1)
            ),
            "easter" to listOf(
                first("ACT", 10, 34, 43), psalm(118, 14, 24), second("COL", 3, 1, 4), gospel("JHN", 20, 1, 18)
            ),
            "ascension" to listOf(
                first("ACT", 1, 1, 11), psalm(47, 1), second("EPH", 1, 15, 23), gospel("LUK", 24, 44, 53)
            ),
            "pentecost" to listOf(
                first("ACT", 2, 1, 21), psalm(104, 24, 34), second("1CO", 12, 3, 13), gospel("JHN", 20, 19, 23)
            )
        )
    }
}
