package com.manna.bible.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SetActiveTranslationUseCase] verifying that setting the active
 * translation persists `bibleTranslationId` via the [PreferencesStore]
 * (Requirement 6.2).
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 *
 * Validates: Requirements 6
 */
class SetActiveTranslationUseCaseTest {

    @Test
    @DisplayName("invoking persists the translation id via the preferences store (Req 6.2)")
    fun persistsActiveTranslation() = runTest {
        val store = FakePreferencesStore()
        SetActiveTranslationUseCase(store).invoke("web_deuterocanon")
        assertEquals("web_deuterocanon", store.activeTranslationId)
    }

    @Test
    @DisplayName("the most recent selection wins when set repeatedly (Req 6.2)")
    fun lastSelectionWins() = runTest {
        val store = FakePreferencesStore()
        val useCase = SetActiveTranslationUseCase(store)
        useCase.invoke("web")
        useCase.invoke("kjv")
        assertEquals("kjv", store.activeTranslationId)
    }
}
