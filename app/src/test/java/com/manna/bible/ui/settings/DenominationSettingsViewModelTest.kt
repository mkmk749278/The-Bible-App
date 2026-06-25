package com.manna.bible.ui.settings

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Note
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.AnnotationRepository
import com.manna.bible.domain.usecase.ApplyDenominationChangeUseCase
import com.manna.bible.domain.usecase.ResolveCanonSwitchImpactUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DenominationSettingsViewModel] verifying the canon-switch warning gate,
 * confirm/apply flow, immediate language changes, and the deuterocanonical toggle
 * (Requirements 11, 12, 13, 15).
 */
class DenominationSettingsViewModelTest {

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
        store: FakePreferencesStore = FakePreferencesStore(),
        annotationRepository: AnnotationRepository = FakeAnnotationRepository()
    ): DenominationSettingsViewModel {
        val canonEngine = FakeCanonEngine()
        return DenominationSettingsViewModel(
            preferencesStore = store,
            applyDenominationChangeUseCase = ApplyDenominationChangeUseCase(
                canonEngine,
                FakeLectionaryProvider(),
                store
            ),
            resolveCanonSwitchImpactUseCase = ResolveCanonSwitchImpactUseCase(
                canonEngine,
                annotationRepository
            )
        )
    }

    @Test
    @DisplayName("requestDenominationChange with impact sets pending and does not apply (Req 12.1)")
    fun requestWithImpactShowsDialog() = runTest {
        val store = FakePreferencesStore()
        // Annotated book that is excluded by every candidate canon in the fake engine.
        val vm = viewModel(store, FakeAnnotationRepository(annotatedOutside = setOf("TOB")))

        vm.uiState.test {
            awaitItem() // initial
            vm.requestDenominationChange(Denomination.PROTESTANT_OTHER)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(Denomination.PROTESTANT_OTHER, state.pendingDenomination)
            assertTrue(state.showCanonSwitchDialog)
            assertEquals(setOf("TOB"), state.pendingImpact?.excludedBookIds)
            // Not yet applied: store denomination unchanged.
            assertEquals(Denomination.CATHOLIC, store.currentState.denomination)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("confirmDenominationChange applies the change and clears pending (Req 11, 12)")
    fun confirmAppliesChange() = runTest {
        val store = FakePreferencesStore()
        val vm = viewModel(store, FakeAnnotationRepository(annotatedOutside = setOf("TOB")))

        vm.uiState.test {
            awaitItem()
            vm.requestDenominationChange(Denomination.PROTESTANT_OTHER)
            advanceUntilIdle()
            assertEquals(Denomination.PROTESTANT_OTHER, expectMostRecentItem().pendingDenomination)

            vm.confirmDenominationChange()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.pendingDenomination)
            assertNull(state.pendingImpact)
            assertEquals(Denomination.PROTESTANT_OTHER, store.currentState.denomination)
            assertEquals(CanonType.PROTESTANT_66, store.currentState.canonType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("requestDenominationChange with no impact applies directly, no pending (Req 11)")
    fun requestWithoutImpactAppliesDirectly() = runTest {
        val store = FakePreferencesStore()
        val vm = viewModel(store, FakeAnnotationRepository(annotatedOutside = emptySet()))

        vm.uiState.test {
            awaitItem()
            vm.requestDenominationChange(Denomination.PROTESTANT_OTHER)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.pendingDenomination)
            assertFalse(state.showCanonSwitchDialog)
            assertEquals(Denomination.PROTESTANT_OTHER, store.currentState.denomination)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("setShowDeuterocanonical calls preferencesStore.setShowDeuterocanonical (Req 15)")
    fun setShowDeuterocanonicalPersists() = runTest {
        val store = FakePreferencesStore()
        val vm = viewModel(store)
        advanceUntilIdle()

        vm.setShowDeuterocanonical(true)
        advanceUntilIdle()

        assertTrue(store.currentState.showDeuterocanonical)
        assertEquals(true, store.lastShowDeuterocanonical)
    }

    private companion object {
        val INITIAL_STATE = SetupState(
            denomination = Denomination.CATHOLIC,
            canonType = CanonType.CATHOLIC_73,
            uiLanguage = "en",
            bibleLanguage = "ml",
            numberingScheme = NumberingScheme.SEPTUAGINT,
            namingConventionId = null,
            bibleTranslationId = null,
            lectionaryId = "rc_calendar",
            showDeuterocanonical = false,
            setupCompleted = true
        )
    }

    /** In-memory [PreferencesStore] backed by a [MutableStateFlow]. */
    private class FakePreferencesStore : PreferencesStore {
        private val state = MutableStateFlow(INITIAL_STATE)
        var lastShowDeuterocanonical: Boolean? = null
            private set

        val currentState: SetupState get() = state.value

        override val setupState: Flow<SetupState> = state

        override suspend fun saveSetup(state: SetupState) {
            this.state.value = state
        }

        override suspend fun setSetupCompleted(value: Boolean) {
            state.value = state.value.copy(setupCompleted = value)
        }

        override suspend fun updateDenomination(profile: CanonProfile) {
            state.value = state.value.copy(
                denomination = profile.denomination,
                canonType = profile.canonType,
                numberingScheme = profile.numberingScheme,
                namingConventionId = profile.namingConventionId,
                bibleTranslationId = profile.suggestedTranslationId,
                lectionaryId = profile.lectionaryId
            )
        }

        override suspend fun setShowDeuterocanonical(value: Boolean) {
            lastShowDeuterocanonical = value
            state.value = state.value.copy(showDeuterocanonical = value)
        }

        private val lastReadPositionFlow = MutableStateFlow<String?>(null)
        override val lastReadPosition: Flow<String?> = lastReadPositionFlow

        override suspend fun setActiveTranslation(translationId: String) {
            state.value = state.value.copy(bibleTranslationId = translationId)
        }

        override suspend fun setLastReadPosition(ref: String) {
            lastReadPositionFlow.value = ref
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

        override suspend fun profileFor(
            denomination: Denomination,
            bibleLanguage: String
        ): CanonProfile {
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

    /** Fake [LectionaryProvider]. */
    private class FakeLectionaryProvider : LectionaryProvider {
        override fun lectionaryIdFor(denomination: Denomination): String? = when (denomination) {
            Denomination.CSI -> "csi_almanac"
            Denomination.CATHOLIC -> "rc_calendar"
            else -> null
        }
    }

    /** Fake [AnnotationRepository] returning a fixed excluded-book set. */
    private class FakeAnnotationRepository(
        private val annotatedOutside: Set<String> = emptySet()
    ) : AnnotationRepository {
        override suspend fun allAnnotatedBookIds(): Set<String> = annotatedOutside
        override suspend fun annotatedBookIdsOutside(visibleBookIds: Set<String>): Set<String> =
            annotatedOutside
        override fun visibleHighlights(visibleBookIds: Set<String>): Flow<List<Highlight>> =
            flowOf(emptyList())
        override fun visibleBookmarks(visibleBookIds: Set<String>): Flow<List<Bookmark>> =
            flowOf(emptyList())
        override fun visibleNotes(visibleBookIds: Set<String>): Flow<List<Note>> =
            flowOf(emptyList())

        override suspend fun addHighlight(verseRef: String, colorArgb: Int): Long = 0
        override suspend fun addBookmark(verseRef: String, label: String?): Long = 0
        override suspend fun addNote(verseRef: String, content: String): Long = 0
        override suspend fun deleteHighlight(id: Long) {}
        override suspend fun deleteBookmark(id: Long) {}
        override suspend fun deleteNote(id: Long) {}
    }
}
