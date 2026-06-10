package com.manna.bible.ui.setup

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import com.manna.bible.domain.translation.TranslationFilter
import com.manna.bible.domain.usecase.CompleteSetupUseCase
import com.manna.bible.domain.usecase.SetupSelections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SetupViewModel] verifying state emissions for selection, navigation, the
 * translation step population, and completion success/failure (Requirements 1, 2, 4, 5, 7, 9, 10, 15).
 */
class SetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        useCase: CompleteSetupUseCase = CompleteSetupUseCase(
            FakeCanonEngine(),
            FakeLectionaryProvider(),
            FakePreferencesStore()
        ),
        catalog: List<Translation> = defaultCatalog
    ) = SetupViewModel(
        completeSetupUseCase = useCase,
        canonEngine = FakeCanonEngine(),
        lectionaryProvider = FakeLectionaryProvider(),
        translationFilter = FakeTranslationFilter(),
        translationRepository = FakeTranslationRepository(catalog)
    )

    @Test
    @DisplayName("selectDenomination sets denomination and derived lectionary (Req 3, 9)")
    fun selectDenominationSetsLectionary() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(SetupUiState(), awaitItem())
            vm.selectDenomination(Denomination.CATHOLIC)
            val state = awaitItem()
            assertEquals(Denomination.CATHOLIC, state.denomination)
            assertEquals("rc_calendar", state.lectionaryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("next advances through the setup steps in order (Req 1)")
    fun nextAdvancesStep() = runTest {
        val vm = viewModel()

        assertEquals(SetupStep.WELCOME, vm.uiState.value.step)
        vm.next()
        assertEquals(SetupStep.DENOMINATION, vm.uiState.value.step)
        vm.next()
        assertEquals(SetupStep.UI_LANGUAGE, vm.uiState.value.step)
        vm.back()
        assertEquals(SetupStep.DENOMINATION, vm.uiState.value.step)
    }

    @Test
    @DisplayName("entering translation step populates available translations from the filter (Req 5)")
    fun translationStepPopulatesAvailableTranslations() = runTest {
        val vm = viewModel()
        vm.selectDenomination(Denomination.CATHOLIC)
        vm.selectUiLanguage("ml")
        vm.selectBibleLanguage("ml")

        // Advance WELCOME -> DENOMINATION -> UI_LANGUAGE -> BIBLE_LANGUAGE -> TRANSLATION
        repeat(4) { vm.next() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(SetupStep.TRANSLATION, state.step)
        assertEquals(listOf("ml_cath", "ml_prot"), state.availableTranslations.map { it.id })
        assertEquals("ml_cath", state.bibleTranslationId)
    }

    @Test
    @DisplayName("complete success marks completed and persists selections (Req 10)")
    fun completeSuccess() = runTest {
        val store = FakePreferencesStore()
        val vm = viewModel(
            useCase = CompleteSetupUseCase(FakeCanonEngine(), FakeLectionaryProvider(), store)
        )
        vm.selectDenomination(Denomination.CSI)
        vm.selectUiLanguage("ta")
        vm.selectBibleLanguage("ta")

        vm.complete()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.completed)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
        assertNotNull(store.savedState)
        assertEquals(Denomination.CSI, store.savedState!!.denomination)
    }

    @Test
    @DisplayName("complete failure sets error, retains selections, stays incomplete (Req 15.4)")
    fun completeFailureRetainsSelections() = runTest {
        val store = FakePreferencesStore(failOnSave = true)
        val vm = viewModel(
            useCase = CompleteSetupUseCase(FakeCanonEngine(), FakeLectionaryProvider(), store)
        )
        vm.selectDenomination(Denomination.CATHOLIC)
        vm.selectUiLanguage("ml")
        vm.selectBibleLanguage("ml")

        vm.complete()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.completed)
        assertFalse(state.isSaving)
        assertNotNull(state.errorMessage)
        // Selections retained for retry.
        assertEquals(Denomination.CATHOLIC, state.denomination)
        assertEquals("ml", state.uiLanguage)
        assertEquals("ml", state.bibleLanguage)
    }

    private companion object {
        val defaultCatalog = listOf(
            Translation("ml_cath", "Malayalam Catholic", "ml", CanonType.CATHOLIC_73, hasDeuterocanon = true, isDefaultForCanon = true),
            Translation("ml_prot", "Malayalam Protestant", "ml", CanonType.PROTESTANT_66, hasDeuterocanon = false),
            Translation("en_kjv", "King James", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false)
        )
    }
}

/** Fake [CanonEngine] mirroring the real denomination -> canon/numbering mapping. */
private class FakeCanonEngine : CanonEngine {
    override fun canonTypeFor(denomination: Denomination): CanonType = when (denomination) {
        Denomination.CATHOLIC -> CanonType.CATHOLIC_73
        Denomination.ORTHODOX -> CanonType.ORTHODOX_EXPANDED
        Denomination.SHOW_EVERYTHING -> CanonType.ALL_CANONS
        else -> CanonType.PROTESTANT_66
    }

    override suspend fun profileFor(denomination: Denomination, bibleLanguage: String): CanonProfile {
        val canonType = canonTypeFor(denomination)
        val numbering = when (canonType) {
            CanonType.CATHOLIC_73, CanonType.ORTHODOX_EXPANDED -> NumberingScheme.SEPTUAGINT
            else -> NumberingScheme.MASORETIC
        }
        return CanonProfile(
            denomination = denomination,
            canonType = canonType,
            books = emptyList(),
            numberingScheme = numbering,
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )
    }
}

/** Fake [LectionaryProvider] mirroring the real denomination -> lectionary mapping. */
private class FakeLectionaryProvider : LectionaryProvider {
    override fun lectionaryIdFor(denomination: Denomination): String? = when (denomination) {
        Denomination.CSI -> "csi_almanac"
        Denomination.CATHOLIC -> "rc_calendar"
        Denomination.ORTHODOX -> "orthodox_calendar"
        Denomination.MAR_THOMA, Denomination.PROTESTANT_OTHER -> "general_lectionary"
        Denomination.SHOW_EVERYTHING -> null
    }
}

/** Fake [TranslationFilter] returning catalog entries matching the language, default first. */
private class FakeTranslationFilter : TranslationFilter {
    override fun filter(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): List<Translation> = catalog
        .filter { it.languageCode == bibleLanguage }
        .sortedByDescending { it.isDefaultForCanon }

    override fun suggestedDefault(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): Translation? = filter(catalog, profile, bibleLanguage).firstOrNull()
}

/** Fake [TranslationRepository] exposing a fixed catalog. */
private class FakeTranslationRepository(
    private val catalog: List<Translation>
) : TranslationRepository {
    override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
    override suspend fun refreshCatalog() = Unit
    override suspend fun download(id: String): DownloadResult = error("not used")
    override suspend fun markPendingDownload(id: String) = Unit
    override suspend fun retryPendingDownloads() = Unit
}

/** In-memory [PreferencesStore] recording the last saved state. */
private class FakePreferencesStore(private val failOnSave: Boolean = false) : PreferencesStore {
    var savedState: SetupState? = null

    override val setupState: Flow<SetupState>
        get() = flowOf(savedState ?: error("no state"))

    override suspend fun saveSetup(state: SetupState) {
        if (failOnSave) throw RuntimeException("disk full")
        savedState = state
    }

    override suspend fun setSetupCompleted(value: Boolean) = Unit
    override suspend fun updateDenomination(profile: CanonProfile) = Unit
    override suspend fun setShowDeuterocanonical(value: Boolean) = Unit
}
