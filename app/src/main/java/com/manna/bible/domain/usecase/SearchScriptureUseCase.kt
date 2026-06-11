package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.reader.PsalmDisplay
import com.manna.bible.domain.repository.BibleContentRepository
import javax.inject.Inject

/**
 * A single full-text search hit, ready for display and for jumping to the verse.
 *
 * @property osisId Canonical OSIS book id of the match (for navigation).
 * @property chapter Canonical (Masoretic) chapter number (for navigation).
 * @property verse Canonical verse number (for navigation).
 * @property reference Display reference in the active numbering, e.g. "Psalms 23:1".
 * @property snippet Plain-text verse content.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class SearchResult(
    val osisId: String,
    val chapter: Int,
    val verse: Int,
    val reference: String,
    val snippet: String
)

/**
 * Searches the active translation's stored text and returns canon-visible results
 * with a display reference and snippet (Requirement 10).
 *
 * Delegates the FTS `MATCH` to [BibleContentRepository.search] (offline, Req 10.1,
 * 10.5), restricts results to books in the active [CanonProfile] (Req 10.2), and
 * formats each reference in the active displayed numbering — Psalms via
 * [PsalmDisplay] (Req 10.3). Book display names are supplied by the caller (from the
 * translation's stored book list) so this stays pure and JVM-testable.
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class SearchScriptureUseCase @Inject constructor(
    private val repository: BibleContentRepository
) {

    /**
     * Returns the canon-visible matches for [query] in [translationId], or an empty
     * list for a blank query.
     *
     * @param bookNames OSIS id → display name for the translation's books; an
     *   unknown id falls back to the OSIS id.
     */
    suspend operator fun invoke(
        translationId: String,
        query: String,
        profile: CanonProfile,
        bookNames: Map<String, String>
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val visible = profile.books.mapTo(HashSet()) { it.osisId }
        return repository.search(translationId, query)
            .filter { it.osisId in visible }
            .map { match ->
                val displayChapter = if (match.osisId == PSALMS_OSIS_ID) {
                    PsalmDisplay.displayPsalmNumber(profile, match.chapter)
                } else {
                    match.chapter
                }
                val name = bookNames[match.osisId] ?: match.osisId
                SearchResult(
                    osisId = match.osisId,
                    chapter = match.chapter,
                    verse = match.verse,
                    reference = "$name $displayChapter:${match.verse}",
                    snippet = match.text
                )
            }
    }

    private companion object {
        const val PSALMS_OSIS_ID = "PSA"
    }
}
