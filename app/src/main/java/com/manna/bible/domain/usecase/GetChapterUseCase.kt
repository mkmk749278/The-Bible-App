package com.manna.bible.domain.usecase

import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fetches the [ChapterContent] for a book+chapter of a translation from the
 * `Bible_Content_Repository` (Requirement 2).
 *
 * A thin pass-through over [BibleContentRepository]; it applies no canon logic so
 * the reader composes the result with `CanonBookOrdering`/`PsalmDisplay`. Returns
 * null when the chapter is not available locally, letting the reader surface the
 * download/switch state (Req 2.6).
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class GetChapterUseCase @Inject constructor(
    private val repository: BibleContentRepository
) {

    /** Returns the chapter content, or null when it is not stored for [translationId]. */
    suspend operator fun invoke(
        translationId: String,
        osisId: String,
        chapter: Int
    ): ChapterContent? = repository.chapter(translationId, osisId, chapter)

    /** Emits the books available for [translationId] (for navigation/picker building). */
    fun books(translationId: String): Flow<List<BookSummary>> = repository.books(translationId)
}
