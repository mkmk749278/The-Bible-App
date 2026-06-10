package com.manna.bible.data.repository

import com.manna.bible.data.local.BibleContentDao
import com.manna.bible.data.local.BookEntity
import com.manna.bible.data.local.VerseEntity
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.VerseLine
import com.manna.bible.domain.repository.VerseMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [BibleContentRepository] (Task 4.1).
 *
 * Pure delegation over [BibleContentDao] plus entity → domain mapping; it applies
 * no canon logic (book ordering, Psalm display numbering, and canon-book
 * restriction live in the reader/search use cases above this layer).
 */
class DefaultBibleContentRepository @Inject constructor(
    private val dao: BibleContentDao
) : BibleContentRepository {

    override fun books(translationId: String): Flow<List<BookSummary>> =
        dao.observeBooks(translationId).map { rows -> rows.map { it.toSummary() } }

    override suspend fun chapter(
        translationId: String,
        osisId: String,
        chapter: Int
    ): ChapterContent? {
        val verses = dao.getChapter(translationId, osisId, chapter)
        if (verses.isEmpty()) return null
        return ChapterContent(
            translationId = translationId,
            osisId = osisId,
            chapter = chapter,
            verses = verses.map { VerseLine(it.verse, it.text) }
        )
    }

    override suspend fun hasContent(translationId: String): Boolean =
        dao.hasAnyContent(translationId)

    override suspend fun search(translationId: String, query: String): List<VerseMatch> {
        val matchQuery = query.toFtsMatchQuery() ?: return emptyList()
        return dao.search(translationId, matchQuery, SEARCH_LIMIT)
            .map { it.toMatch() }
    }

    private companion object {
        const val SEARCH_LIMIT = 100
    }
}

/** Maps the persisted testament marker to the domain [Testament] (defaults to OLD). */
private fun String.toTestament(): Testament =
    if (equals("NEW", ignoreCase = true)) Testament.NEW else Testament.OLD

private fun BookEntity.toSummary(): BookSummary = BookSummary(
    osisId = osisId,
    name = name,
    testament = testament.toTestament(),
    orderIndex = orderIndex,
    chapterCount = chapterCount
)

private fun VerseEntity.toMatch(): VerseMatch = VerseMatch(
    osisId = osisId,
    chapter = chapter,
    verse = verse,
    text = text
)

/**
 * Turns free-form user input into a safe FTS4 `MATCH` query.
 *
 * Each whitespace-delimited token is quoted so punctuation (apostrophes, commas)
 * never produces an FTS syntax error, and a trailing `*` is appended for prefix
 * matching. Returns null when the query has no usable tokens.
 */
private fun String.toFtsMatchQuery(): String? {
    val tokens = trim()
        .split(Regex("\\s+"))
        .map { it.replace("\"", "") }
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" ") { "\"$it\"*" }
}
