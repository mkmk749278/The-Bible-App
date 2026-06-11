package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.VerseMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SearchScriptureUseCase]: canon restriction, displayed numbering
 * in references, blank-query short-circuit. Pure JVM with a fake repository.
 */
class SearchScriptureUseCaseTest {

    private fun profile(numbering: NumberingScheme) = CanonProfile(
        denomination = Denomination.CATHOLIC,
        canonType = CanonType.CATHOLIC_73,
        books = listOf(
            CanonBook("GEN", Testament.OLD, orderIndex = 0, isDeuterocanonical = false),
            CanonBook("PSA", Testament.OLD, orderIndex = 18, isDeuterocanonical = false)
        ),
        numberingScheme = numbering,
        namingConventionId = null,
        suggestedTranslationId = null,
        lectionaryId = null
    )

    private val bookNames = mapOf("GEN" to "Genesis", "PSA" to "Psalms")

    @Test
    @DisplayName("restricts results to canon books and formats references with display numbering")
    fun restrictsAndFormats() = runTest {
        val repo = FakeRepo(
            listOf(
                VerseMatch("GEN", 1, 1, "In the beginning, God created"),
                VerseMatch("PSA", 23, 1, "The Lord is my shepherd"),
                VerseMatch("TOB", 1, 1, "outside the Protestant canon")
            )
        )
        val useCase = SearchScriptureUseCase(repo)

        val results = useCase("web", "lord", profile(NumberingScheme.SEPTUAGINT), bookNames)

        // TOB is not in the profile's books, so it is excluded (Req 10.2).
        assertEquals(listOf("GEN", "PSA"), results.map { it.osisId })
        assertEquals("Genesis 1:1", results.first { it.osisId == "GEN" }.reference)
        // Psalm 23 (Masoretic) displays as 22 under the Septuagint scheme (Req 10.3).
        assertEquals("Psalms 22:1", results.first { it.osisId == "PSA" }.reference)
        // Navigation fields stay canonical.
        assertEquals(23, results.first { it.osisId == "PSA" }.chapter)
    }

    @Test
    @DisplayName("blank query returns no results without querying")
    fun blankQuery() = runTest {
        val repo = FakeRepo(listOf(VerseMatch("GEN", 1, 1, "x")))
        val useCase = SearchScriptureUseCase(repo)

        val results = useCase("web", "   ", profile(NumberingScheme.MASORETIC), bookNames)

        assertTrue(results.isEmpty())
        assertEquals(0, repo.searchCalls)
    }

    private class FakeRepo(private val matches: List<VerseMatch>) : BibleContentRepository {
        var searchCalls = 0
            private set

        override fun books(translationId: String): Flow<List<BookSummary>> = flowOf(emptyList())
        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent? = null
        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> {
            searchCalls++
            return matches
        }
    }
}
