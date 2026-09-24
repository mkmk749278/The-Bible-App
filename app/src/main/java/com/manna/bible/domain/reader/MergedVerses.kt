package com.manna.bible.domain.reader

import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.VerseLine

/**
 * Finds the verse that actually carries a given verse number in a translation.
 *
 * Some editions merge neighbouring verses into one numbered verse. The Telugu IRV, for
 * example, prints 1 Corinthians 4:15–16 as a single verse numbered 15, so verse 16 has no
 * row of its own (103 verses are merged this way, e.g. 2 Timothy 3:16–17). A reference to
 * such a verse — the daily verse, an AI answer, a curated devotion, or a highlight made in
 * another translation — resolves to the verse that carries its text instead of failing.
 *
 * Rules:
 *  - A verse that exists is its own carrier.
 *  - A verse missing *inside* a chapter is carried by the nearest verse before it.
 *  - A verse missing *after* the chapter's last verse is carried by the last verse only
 *    when it is at most [MAX_MERGED_AFTER_LAST] past it (the widest merge in the bundled
 *    editions), so a verse number that simply doesn't exist still resolves to nothing.
 *  - Romans 14:24–26 never falls back: many editions, the Indian Revised Versions among
 *    them, print that doxology as Romans 16:25–27, so 14:23 does not carry it.
 *
 * Pure Kotlin — no Android dependencies.
 */
object MergedVerses {

    /** The widest run of merged verses found in the bundled editions. */
    const val MAX_MERGED_AFTER_LAST = 3

    /**
     * The present verse number that carries [verse] in chapter [chapter] of [osisId], or
     * null when no row carries it. [present] is the chapter's verse numbers, in any order.
     */
    fun carrierOf(present: Collection<Int>, osisId: String, chapter: Int, verse: Int): Int? {
        if (verse in present) return verse
        val before = present.filter { it < verse }.maxOrNull() ?: return null
        val last = present.max()
        if (before < last) return before // a gap inside the chapter
        val pastLast = verse - last
        return if (pastLast <= MAX_MERGED_AFTER_LAST && !isRelocatedDoxology(osisId, chapter)) last else null
    }

    /**
     * For each present verse, the verse numbers it carries: itself plus any missing verses
     * merged into it (see [carrierOf]). Used to show, and to toggle, annotations that were
     * made on a verse number this edition merges into a neighbour.
     */
    fun spans(present: Collection<Int>, osisId: String, chapter: Int): Map<Int, IntRange> {
        val sorted = present.distinct().sorted()
        return sorted.withIndex().associate { (index, verse) ->
            val end = sorted.getOrNull(index + 1)?.let { it - 1 }
                ?: if (isRelocatedDoxology(osisId, chapter)) verse else verse + MAX_MERGED_AFTER_LAST
            verse to (verse..end)
        }
    }

    private fun isRelocatedDoxology(osisId: String, chapter: Int): Boolean =
        osisId == "ROM" && chapter == 14
}

/**
 * The line carrying [verse] in this chapter — the verse itself, or the neighbour it is
 * merged into (see [MergedVerses]) — or null when the chapter has no such verse.
 */
fun ChapterContent.lineCarrying(verse: Int): VerseLine? {
    val carrier = MergedVerses.carrierOf(verses.map { it.verse }, osisId, chapter, verse)
        ?: return null
    return verses.firstOrNull { it.verse == carrier }
}
