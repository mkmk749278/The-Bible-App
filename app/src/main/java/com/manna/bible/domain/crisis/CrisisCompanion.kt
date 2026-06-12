package com.manna.bible.domain.crisis

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * Curated content for 3AM / Crisis Mode (Phase 2, blueprint P2-1): a calm,
 * compassionate companion for the moments people open their Bible most urgently —
 * grief, panic, a sleepless 3am. No streaks, no plans, no guilt; just warmth.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
interface CrisisCompanion {

    /** Comforting passages for the "Comfort me with scripture" path. */
    fun comfortVerses(): List<ReadingRef>

    /** A single gentle passage for the "Just listen" path (read aloud). */
    fun listenPassage(): ReadingRef
}

/** Night window helper for softening the experience between late night and dawn. */
object NightWindow {
    /** True for the 11pm–5am window the blueprint warms the UI for. */
    fun isNight(hourOfDay: Int): Boolean = hourOfDay >= 23 || hourOfDay < 5
}

/**
 * Default [CrisisCompanion] with a hand-curated set of comforting passages, drawn
 * only from the 66-book canon so they resolve in any translation. Centred on the
 * passages the blueprint calls out — Psalm 23, Isaiah 43, John 14 — plus verses for
 * fear, grief, and sleeplessness.
 */
class DefaultCrisisCompanion @Inject constructor() : CrisisCompanion {

    override fun comfortVerses(): List<ReadingRef> = COMFORT

    override fun listenPassage(): ReadingRef = ReadingRef("PSA", 23, 1)

    private companion object {
        val COMFORT: List<ReadingRef> = listOf(
            ReadingRef("PSA", 23, 4),   // though I walk through the valley
            ReadingRef("ISA", 43, 2),   // when you pass through the waters
            ReadingRef("JHN", 14, 27),  // peace I leave with you
            ReadingRef("PSA", 34, 18),  // close to the broken-hearted
            ReadingRef("MAT", 11, 28),  // come to me, all who are weary
            ReadingRef("ISA", 41, 10),  // fear not, for I am with you
            ReadingRef("PSA", 4, 8),    // in peace I will lie down and sleep
            ReadingRef("PHP", 4, 6),    // do not be anxious about anything
            ReadingRef("ROM", 8, 38),   // nothing can separate us from his love
            ReadingRef("PSA", 121, 1),  // I lift up my eyes to the hills
            ReadingRef("2CO", 1, 3),    // the God of all comfort
            ReadingRef("PSA", 91, 1),   // shelter of the Most High
            ReadingRef("JHN", 14, 1),   // let not your hearts be troubled
            ReadingRef("PSA", 34, 4)    // he delivered me from all my fears
        )
    }
}
