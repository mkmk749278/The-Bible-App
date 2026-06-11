package com.manna.bible.ui.attribution

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.attribution.DefaultAttributionProvider
import com.manna.bible.domain.attribution.TranslationLicense
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AttributionViewModel] (Requirement 12.1, 12.2). Runs on the JVM
 * with hand-rolled fakes and the real [DefaultAttributionProvider].
 */
class AttributionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun web() = Translation(
        id = "web",
        name = "World English Bible",
        languageCode = "en",
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false,
        isDownloaded = true,
        isBundled = true
    )

    private fun kjv() = Translation(
        id = "kjv",
        name = "King James Version",
        languageCode = "en",
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false,
        isDownloaded = true
    )

    private fun viewModel(
        activeId: String?,
        catalog: List<Translation>
    ) = AttributionViewModel(
        preferencesStore = FakePreferencesStore(activeId),
        translationRepository = FakeTranslationRepository(catalog),
        attributionProvider = DefaultAttributionProvider()
    )

    @Test
    @DisplayName("resolves the persisted public-domain translation (Req 12.1, 12.2)")
    fun resolvesPersistedPublicDomain() = runTest {
        val vm = viewModel(activeId = "web", catalog = listOf(web(), kjv()))
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("World English Bible", state.attribution.translationName)
            assertEquals(TranslationLicense.PUBLIC_DOMAIN, state.attribution.license)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("falls back to the first downloaded edition when none is persisted (Req 12.4)")
    fun fallsBackToDownloaded() = runTest {
        val vm = viewModel(activeId = null, catalog = listOf(kjv(), web()))
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("King James Version", state.attribution.translationName)
            assertEquals(TranslationLicense.SOURCE_PROVIDED, state.attribution.license)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("empty catalog yields no active translation (Req 12.1)")
    fun emptyCatalog() = runTest {
        val vm = viewModel(activeId = null, catalog = emptyList())
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(null, state.attribution.translationName)
            assertEquals(null, state.attribution.license)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore(activeId: String?) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en",
                bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = activeId,
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true
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

    private class FakeTranslationRepository(
        private val catalog: List<Translation>
    ) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = MutableStateFlow(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun retryPendingDownloads() {}
    }
}
