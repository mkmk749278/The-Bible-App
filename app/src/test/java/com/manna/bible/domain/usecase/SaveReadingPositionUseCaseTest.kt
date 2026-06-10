package com.manna.bible.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SaveReadingPositionUseCase] verifying the reading position is
 * persisted in canonical `OSIS.CHAPTER.VERSE` form (Requirement 7.1).
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 *
 * Validates: Requirements 7
 */
class SaveReadingPositionUseCaseTest {

    @Test
    @DisplayName("persists the position in canonical OSIS.CHAPTER.VERSE form (Req 7.1)")
    fun persistsCanonicalPosition() = runTest {
        val store = FakePreferencesStore()
        SaveReadingPositionUseCase(store).invoke(ReadingRef("PSA", 23, 4))
        assertEquals("PSA.23.4", store.lastReadPosition.first())
    }

    @Test
    @DisplayName("a later save overwrites the previously persisted position (Req 7.1)")
    fun overwritesPreviousPosition() = runTest {
        val store = FakePreferencesStore()
        val useCase = SaveReadingPositionUseCase(store)
        useCase.invoke(ReadingRef("GEN", 1, 1))
        useCase.invoke(ReadingRef("EXO", 2, 3))
        assertEquals("EXO.2.3", store.lastReadPosition.first())
    }
}
