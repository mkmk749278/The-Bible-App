package com.manna.bible.domain.repository

import com.manna.bible.domain.model.Testament
import kotlinx.coroutines.flow.Flow

/**
 * Summary metadata for a single book of a translation, as stored in the
 * `Bible_Content_Repository`.
 *
 * @property osisId Stable OSIS book id (e.g. "GEN", "PSA").
 * @property name Localized book name as supplied by the translation source.
 * @property testament Whether the book belongs to the Old or New Testament.
 * @property orderIndex Source order of the book within the translation.
 * @property chapterCount Number of chapters the book contains.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class BookSummary(
    val osisId: String,
    val name: String,
    val testament: Testament,
    val orderIndex: Int,
    val chapterCount: Int
)

/**
 * A single rendered verse line within a chapter.
 *
 * @property verse Canonical (Masoretic) verse number.
 * @property text Plain-text verse content.
 */
data class VerseLine(
    val verse: Int,
    val text: String
)

/**
 * The ordered content of one book+chapter of a translation.
 *
 * Verse numbers are canonical (Masoretic); display numbering (e.g. Septuagint
 * Psalms) is applied at the presentation layer via `PsalmDisplay`.
 *
 * @property translationId Translation the content belongs to.
 * @property osisId OSIS book id.
 * @property chapter Canonical chapter number.
 * @property verses Verses ordered ascending by verse number.
 */
data class ChapterContent(
    val translationId: String,
    val osisId: String,
    val chapter: Int,
    val verses: List<VerseLine>
)

/**
 * A full-text search hit within a translation.
 *
 * @property osisId OSIS book id of the matching verse.
 * @property chapter Canonical chapter number.
 * @property verse Canonical verse number.
 * @property text Plain-text verse content (snippet source).
 */
data class VerseMatch(
    val osisId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

/**
 * Single source of truth for scripture text (books, chapters, verses), backed by
 * local storage and populated by the bundled seeder and download manager.
 *
 * The repository applies no canon-specific logic; reader/search use cases compose
 * its output with the active `CanonProfile`, `CanonBookOrdering`, and
 * `PsalmDisplay`.
 *
 * Pure Kotlin contract — no Android dependencies.
 */
interface BibleContentRepository {

    /** Emits the books available for [translationId], updating as content changes. */
    fun books(translationId: String): Flow<List<BookSummary>>

    /**
     * Returns the [ChapterContent] for [translationId]/[osisId]/[chapter], or null
     * when that chapter is not present locally for the translation.
     */
    suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent?

    /** Returns true when any content is stored for [translationId]. */
    suspend fun hasContent(translationId: String): Boolean

    /** Returns verses in [translationId] whose text matches [query]. */
    suspend fun search(translationId: String, query: String): List<VerseMatch>
}
