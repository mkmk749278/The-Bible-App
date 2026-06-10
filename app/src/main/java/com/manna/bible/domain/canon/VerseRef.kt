package com.manna.bible.domain.canon

/**
 * Helpers for canonical verse/chapter references.
 *
 * A reference embeds the OSIS book id as the segment before the first `.`:
 * `"GEN.1.1"` → book `GEN`, `"TOB.3.2"` → book `TOB`, `"PSA.23"` → book `PSA`.
 *
 * Pure Kotlin with no Android dependencies so it is JVM-unit-testable.
 */
object VerseRef {

    /**
     * Extracts the OSIS book id from a verse or chapter [ref].
     *
     * Returns the text before the first `.`, trimmed. Returns `null` when [ref]
     * is null, blank, or has no non-blank book segment, so callers can treat an
     * unparseable reference as "not associated with any book".
     */
    fun bookId(ref: String?): String? {
        if (ref.isNullOrBlank()) return null
        val book = ref.substringBefore('.').trim()
        return book.ifBlank { null }
    }
}
