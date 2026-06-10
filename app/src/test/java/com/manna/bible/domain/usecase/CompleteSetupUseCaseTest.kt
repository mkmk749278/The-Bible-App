package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CompleteSetupUseCase] covering default resolution, language fallback, derived
 * persistence, and error handling (Requirements 2, 7, 10, 15).
 */
class CompleteSetupUseCaseTest {

    private val lectionary = FakeLectionaryProvider()

    private fun useCase(store: PreferencesStore, engine: CanonEngine = FakeCanonEngine()) =
        CompleteSetupUseCase(engine, lectionary, store)

    @Test
    @DisplayName("Explicit Catholic selection persists derived canon config (Req 2, 10)")
    fun catholicSelectionPersistsDerivedConfig() = runTest {
        val store = FakePreferencesStore()
        val result = useCase(store).invoke(
            SetupSelections(
                denomination = Denomination.CATHOLIC,
                uiLanguage = "ta",
                bibleLanguage = "ml",
                bibleTranslationId = null
            )
        )

        assertTrue(result.isSuccess)
        val state = store.savedState!!
        assertEquals(Denomination.CATHOLIC, state.denomination)
        assertEquals(CanonType.CATHOLIC_73, state.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, state.numberingScheme)
        assertEquals("rc_calendar", state.lectionaryId)
        assertEquals("ml", state.bibleLanguage)
        assertEquals("ta", state.uiLanguage)
        assertTrue(state.setupCompleted)
    }

    @Test
    @DisplayName("Skipped denomination maps to show-everything / all-canons (Req 2.3)")
    fun skippedDenominationMapsToShowEverything() = runTest {
        val store = FakePreferencesStore()
        val result = useCase(store).invoke(
            SetupSelections(
                denomination = null,
                uiLanguage = "en",
                bibleLanguage = "en",
                bibleTranslationId = null
            )
        )

        assertTrue(result.isSuccess)
        val state = store.savedState!!
        assertEquals(Denomination.SHOW_EVERYTHING, state.denomination)
        assertEquals(CanonType.ALL_CANONS, state.canonType)
        assertNull(state.lectionaryId)
        assertTrue(state.setupCompleted)
    }

    @Test
    @DisplayName("Bible language falls back to UI language when unset (Req 7.4)")
    fun bibleLanguageFallsBackToUiLanguage() = runTest {
        val store = FakePreferencesStore()
        val result = useCase(store).invoke(
            SetupSelections(
                denomination = Denomination.CSI,
                uiLanguage = "ta",
                bibleLanguage = null,
                bibleTranslationId = null
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("ta", store.savedState!!.bibleLanguage)
    }

    @Test
    @DisplayName("Persistence failure surfaces as Result.failure (Req 15.4)")
    fun persistenceFailureSurfacesAsFailure() = runTest {
        val store = FakePreferencesStore(failOnSave = true)
        val result = useCase(store).invoke(
            SetupSelections(
                denomination = Denomination.CATHOLIC,
                uiLanguage = "en",
                bibleLanguage = "en",
                bibleTranslationId = null
            )
        )

        assertTrue(result.isFailure)
        assertNull(store.savedState)
    }
}
