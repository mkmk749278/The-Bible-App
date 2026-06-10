package com.manna.bible.domain.usecase

/**
 * A canonical reading position: an OSIS book id, a canonical (Masoretic) chapter,
 * and a verse within that chapter.
 *
 * The string form is `OSIS.CHAPTER.VERSE` (e.g. `"GEN.1.1"`), matching the
 * `Verse_Reference`/`Reading_Position` contract used for persistence and
 * annotations. Numbering is always canonical; display numbering is applied at the
 * presentation layer.
 *
 * @property osisId OSIS book id (e.g. "GEN", "PSA"). Must be non-blank.
 * @property chapter Canonical chapter number (1-based).
 * @property verse Canonical verse number (1-based); defaults to the first verse.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class ReadingRef(
    val osisId: String,
    val chapter: Int,
    val verse: Int = 1
) {

    /** Serializes to the canonical `OSIS.CHAPTER.VERSE` string form. */
    fun format(): String = "$osisId.$chapter.$verse"

    companion object {

        /**
         * Parses a `OSIS.CHAPTER.VERSE` (or `OSIS.CHAPTER`, verse defaulting to 1)
         * reference into a [ReadingRef].
         *
         * Returns null when [ref] is null/blank, has the wrong number of segments,
         * has a blank book id, or has a non-positive / non-numeric chapter or verse.
         * This lets callers treat an unparseable stored position as "no position".
         */
        fun parse(ref: String?): ReadingRef? {
            if (ref.isNullOrBlank()) return null
            val parts = ref.trim().split('.')
            if (parts.size < 2 || parts.size > 3) return null

            val osisId = parts[0].trim()
            if (osisId.isBlank()) return null

            val chapter = parts[1].trim().toIntOrNull() ?: return null
            if (chapter < 1) return null

            val verse = if (parts.size == 3) {
                parts[2].trim().toIntOrNull() ?: return null
            } else {
                1
            }
            if (verse < 1) return null

            return ReadingRef(osisId, chapter, verse)
        }
    }
}
