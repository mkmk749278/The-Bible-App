package com.manna.bible.ui.catalog

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.download.DownloadProgress
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.DefaultTranslationFilter
import com.manna.bible.domain.translation.Translation
import com.manna.bible.domain.usecase.SetActiveTranslationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [TranslationCatalogViewModel]: in-language filtering with
 * downloaded-first ordering, active-on-first-download, and the offline flag.
 * Pure JVM with hand-rolled fakes.
 */
class TranslationCatalogViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun translation(id: String, lang: String, downloaded: Boolean) = Translation(
        id = id, name = id, languageCode = lang, canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false, isDownloaded = downloaded
    )

    private fun viewModel(
        catalog: List<Translation>,
        repo: FakeTranslationRepository = FakeTranslationRepository(catalog),
        prefs: FakePreferencesStore = FakePreferencesStore(),
        downloads: FakeDownloadManager = FakeDownloadManager(),
        online: Boolean = true
    ) = TranslationCatalogViewModel(
        translationRepository = repo,
        downloadManager = downloads,
        setActiveTranslationUseCase = SetActiveTranslationUseCase(prefs),
        preferencesStore = prefs,
        translationFilter = DefaultTranslationFilter(),
        canonEngine = FakeCanonEngine(),
        connectivity = object : ConnectivityChecker {
            override fun isOnline(): Boolean = online
        }
    )

    @Test
    @DisplayName("items are filtered to the Bible language, downloaded first, active tagged")
    fun buildsItems() = runTest {
        val prefs = FakePreferencesStore(bibleLanguage = "te", activeId = "te_b")
        val vm = viewModel(
            catalog = listOf(
                translation("te_a", "te", downloaded = false),
                translation("te_b", "te", downloaded = true),
                translation("en_x", "en", downloaded = false)
            ),
            prefs = prefs
        )

        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf("te_b", "te_a"), items.map { it.id }) // downloaded first; en filtered out
        assertTrue(items.first { it.id == "te_b" }.isActive)
        assertTrue(items.first { it.id == "te_b" }.isDownloaded)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    @DisplayName("download success makes the edition active when none is active yet")
    fun downloadSetsActiveWhenNone() = runTest {
        val prefs = FakePreferencesStore(bibleLanguage = "te", activeId = null)
        val vm = viewModel(
            catalog = listOf(translation("te_a", "te", downloaded = false)),
            prefs = prefs
        )
        advanceUntilIdle()

        vm.download("te_a")
        advanceUntilIdle()

        assertEquals("te_a", prefs.state.value.bibleTranslationId)
    }

    @Test
    @DisplayName("offline connectivity raises the offline flag")
    fun offlineFlag() = runTest {
        val vm = viewModel(
            catalog = listOf(translation("te_a", "te", downloaded = false)),
            online = false
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isOffline)
    }

    // --- fakes ---------------------------------------------------------------

    private class FakeTranslationRepository(
        catalog: List<Translation>
    ) : TranslationRepository {
        private val catalogFlow = MutableStateFlow(catalog)
        override fun catalog(): Flow<List<Translation>> = catalogFlow
        override suspend fun refreshCatalog() {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun retryPendingDownloads() {}
    }

    private class FakeDownloadManager(
        private val outcome: DownloadOutcome = DownloadOutcome.Success
    ) : DownloadManager {
        private val progressFlow = MutableStateFlow<DownloadProgress?>(null)
        override fun progress(): Flow<DownloadProgress?> = progressFlow
        override suspend fun download(translationId: String): DownloadOutcome = outcome
        override suspend fun cancel(translationId: String) {}
        override suspend fun delete(translationId: String) {}
        override suspend fun retryPending() {}
    }

    private class FakeCanonEngine : CanonEngine {
        override fun canonTypeFor(denomination: Denomination): CanonType = CanonType.ALL_CANONS
        override suspend fun profileFor(denomination: Denomination, bibleLanguage: String) =
            CanonProfile(
                denomination = denomination,
                canonType = CanonType.ALL_CANONS,
                books = emptyList(),
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                suggestedTranslationId = null,
                lectionaryId = null
            )
    }

    private class FakePreferencesStore(
        bibleLanguage: String = "te",
        activeId: String? = null
    ) : PreferencesStore {
        val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.SHOW_EVERYTHING,
                canonType = CanonType.ALL_CANONS,
                uiLanguage = "en",
                bibleLanguage = bibleLanguage,
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = activeId,
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true
            )
        )

        override val setupState: Flow<SetupState> get() = state
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)

        override suspend fun saveSetup(state: SetupState) { this.state.value = state }
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {
            state.value = state.value.copy(bibleTranslationId = translationId)
        }
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
