package com.manna.bible.data.share

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiCandidateDto
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.data.remote.GeminiResponseDto
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
import com.manna.bible.domain.share.VerseRecommendation
import com.manna.bible.domain.share.VerseRecommendationRequest
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit tests for [GeminiVerseRecommendationEngine] — response parsing, verse-text
 * resolution from the active translation, failure mapping, and the verse-not-found case.
 * Uses hand-rolled fakes (no Mockito), mirroring [com.manna.bible.data.crisis.GeminiCrisisEngineTest].
 */
class GeminiVerseRecommendationEngineTest {

    private fun successDto(text: String) = GeminiResponseDto(
        candidates = listOf(GeminiCandidateDto(GeminiContentDto(listOf(GeminiPartDto(text)))))
    )

    private fun engine(
        api: GeminiApi,
        apiKey: String = "key",
        content: BibleContentRepository = FakeContentRepository(),
        translations: List<Translation> = listOf(translation("web")),
        activeId: String? = "web"
    ) = GeminiVerseRecommendationEngine(
        api = api,
        apiKey = apiKey,
        bibleContentRepository = content,
        translationRepository = FakeTranslationRepository(translations),
        preferencesStore = FakePreferencesStore(activeId)
    )

    private fun request(situation: String = "failed exam") = VerseRecommendationRequest(
        situationText = situation,
        languageCode = "en",
        denomination = null
    )

    @Test
    @DisplayName("A blank key is not configured and returns Unavailable")
    fun unconfigured() = runTest {
        val e = engine(FakeApi { successDto("REF: EPH.5.25") }, apiKey = "")
        assertFalse(e.isConfigured)
        assertInstanceOf(VerseRecommendation.Unavailable::class.java, e.recommend(request()))
    }

    @Test
    @DisplayName("A valid reply resolves the verse text from the active translation")
    fun validResolvesText() = runTest {
        val e = engine(
            FakeApi {
                successDto(
                    """
                    REF: EPH.5.25
                    DISPLAY: Ephesians 5:25
                    MESSAGE: Love each other deeply. May your home be full of grace.
                    """.trimIndent()
                )
            }
        )
        val result = e.recommend(request("wedding today"))
        assertEquals(
            VerseRecommendation.Success(
                osisRef = "EPH.5.25",
                reference = "Ephesians 5:25",
                verseText = "EPH 5:25",
                personalMessage = "Love each other deeply. May your home be full of grace."
            ),
            result
        )
    }

    @Test
    @DisplayName("A verse absent from the active translation returns Unavailable")
    fun verseNotFound() = runTest {
        // The fake chapter only has 30 verses, so verse 99 cannot be resolved.
        val e = engine(
            FakeApi {
                successDto(
                    """
                    REF: EPH.5.99
                    DISPLAY: Ephesians 5:99
                    MESSAGE: A note.
                    """.trimIndent()
                )
            }
        )
        val result = e.recommend(request())
        assertInstanceOf(VerseRecommendation.Unavailable::class.java, result)
        result as VerseRecommendation.Unavailable
        assertTrue(result.reason.contains("not found", ignoreCase = true))
    }

    @Test
    @DisplayName("A network failure maps to Offline")
    fun offline() = runTest {
        val e = engine(FakeApi { throw IOException("no net") })
        assertEquals(VerseRecommendation.Offline, e.recommend(request()))
    }

    @Test
    @DisplayName("An HTTP error maps to Unavailable carrying only the status code")
    fun httpError() = runTest {
        val e = engine(
            FakeApi {
                val body = "blocked".toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<GeminiResponseDto>(429, body))
            }
        )
        val result = e.recommend(request())
        assertInstanceOf(VerseRecommendation.Unavailable::class.java, result)
        result as VerseRecommendation.Unavailable
        assertTrue(result.reason.contains("429"))
    }

    @Test
    @DisplayName("A malformed reply (missing REF) returns Unavailable")
    fun malformed() = runTest {
        val e = engine(FakeApi { successDto("this is not the expected format at all") })
        val result = e.recommend(request())
        assertInstanceOf(VerseRecommendation.Unavailable::class.java, result)
        result as VerseRecommendation.Unavailable
        assertTrue(result.reason.contains("REF"))
    }

    @Test
    @DisplayName("With no active translation available the recommendation is Unavailable")
    fun noActiveTranslation() = runTest {
        val e = engine(
            FakeApi {
                successDto("REF: EPH.5.25\nDISPLAY: Ephesians 5:25\nMESSAGE: A note.")
            },
            translations = emptyList(),
            activeId = null
        )
        val result = e.recommend(request())
        assertInstanceOf(VerseRecommendation.Unavailable::class.java, result)
    }

    private fun translation(id: String) = Translation(
        id = id, name = "World English Bible", languageCode = "en",
        canonType = CanonType.PROTESTANT_66, hasDeuterocanon = false,
        isDownloaded = true, isBundled = true
    )

    private class FakeApi(
        private val behavior: suspend () -> GeminiResponseDto
    ) : GeminiApi {
        override suspend fun generate(
            model: String,
            key: String,
            body: GeminiRequestDto
        ): GeminiResponseDto = behavior()
    }

    private class FakeContentRepository : BibleContentRepository {
        private val names = mapOf("EPH" to "Ephesians", "PSA" to "Psalms", "ROM" to "Romans")
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(names.entries.mapIndexed { i, e ->
                BookSummary(e.key, e.value, Testament.NEW, i, 16)
            })

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent =
            ChapterContent(
                translationId, osisId, chapter,
                (1..30).map { VerseLine(it, "$osisId $chapter:$it") }
            )

        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakeTranslationRepository(private val catalog: List<Translation>) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun retryPendingDownloads() {}
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
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
}
