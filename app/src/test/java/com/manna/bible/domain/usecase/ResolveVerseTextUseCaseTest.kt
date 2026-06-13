package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.repository.VerseLine
import com.manna.bible.domain.repository.VerseMatch
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ResolveVerseTextUseCase] — the shared "ref → verse text" resolver
 * used across the Prayers hub. Covers the happy path, book-name formatting, dropping
 * verses absent from the active translation, chapter de-duplication, and the
 * no-translation fallback.
 */
class ResolveVerseTextUseCaseTest {

    @Test
    @DisplayName("resolves refs to text with book-name references, in order")
    fun resolvesInOrder() = runTest {
        val useCase = ResolveVerseTextUseCase(
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository()
        )
        val result = useCase(listOf(ReadingRef("PSA", 23, 4), ReadingRef("JHN", 14, 2)))
        assertEquals(2, result.size)
        assertEquals("Psalms 23:4", result[0].reference)
        assertEquals("PSA 23:4", result[0].text)
        assertEquals("PSA.23.4", result[0].osisRef)
        assertEquals("John 14:2", result[1].reference)
    }

    @Test
    @DisplayName("drops a ref whose verse is not present in the translation")
    fun dropsMissingVerse() = runTest {
        val useCase = ResolveVerseTextUseCase(
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            // Only 5 verses per chapter, so verse 99 cannot resolve.
            bibleContentRepository = FakeContentRepository(versesPerChapter = 5)
        )
        val result = useCase(listOf(ReadingRef("PSA", 23, 99), ReadingRef("PSA", 23, 1)))
        assertEquals(1, result.size)
        assertEquals("PSA.23.1", result.single().osisRef)
    }

    @Test
    @DisplayName("loads each chapter only once across a batch")
    fun deduplicatesChapterLoads() = runTest {
        val repo = FakeContentRepository()
        val useCase = ResolveVerseTextUseCase(
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = repo
        )
        useCase(listOf(ReadingRef("PSA", 23, 1), ReadingRef("PSA", 23, 4), ReadingRef("PSA", 23, 6)))
        assertEquals(1, repo.chapterLoads, "three verses from one chapter should load it once")
    }

    @Test
    @DisplayName("returns empty when no translation is available")
    fun noTranslation() = runTest {
        val useCase = ResolveVerseTextUseCase(
            preferencesStore = FakePreferencesStore(activeId = null),
            translationRepository = FakeTranslationRepository(emptyList()),
            bibleContentRepository = FakeContentRepository()
        )
        assertTrue(useCase(listOf(ReadingRef("PSA", 23, 1))).isEmpty())
    }

    @Test
    @DisplayName("single-ref overload returns null when unavailable")
    fun singleNull() = runTest {
        val useCase = ResolveVerseTextUseCase(
            preferencesStore = FakePreferencesStore(activeId = null),
            translationRepository = FakeTranslationRepository(emptyList()),
            bibleContentRepository = FakeContentRepository()
        )
        assertNull(useCase(ReadingRef("PSA", 23, 1)))
    }

    private fun translation(id: String) = Translation(
        id = id, name = "World English Bible", languageCode = "en",
        canonType = CanonType.PROTESTANT_66, hasDeuterocanon = false,
        isDownloaded = true, isBundled = true
    )

    private class FakeContentRepository(
        private val versesPerChapter: Int = 200
    ) : BibleContentRepository {
        var chapterLoads = 0
            private set
        private val names = mapOf("PSA" to "Psalms", "JHN" to "John")

        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(names.entries.mapIndexed { i, e ->
                BookSummary(e.key, e.value, Testament.OLD, i, 150)
            })

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent {
            chapterLoads++
            return ChapterContent(
                translationId, osisId, chapter,
                (1..versesPerChapter).map { VerseLine(it, "$osisId $chapter:$it") }
            )
        }

        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore(private val activeId: String?) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en", bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null, bibleTranslationId = activeId,
                lectionaryId = null, showDeuterocanonical = false, setupCompleted = true
            )
        )
        override val setupState: Flow<SetupState> = state
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private class FakeTranslationRepository(private val catalog: List<Translation>) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun retryPendingDownloads() {}
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
    }
}
