package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ApplyDenominationChangeUseCase] covering recomputation and canon-only persistence
 * on a post-setup denomination change (Requirement 11).
 */
class ApplyDenominationChangeUseCaseTest {

    private val engine = FakeCanonEngine()
    private val lectionary = FakeLectionaryProvider()

    private fun useCase(store: FakePreferencesStore) =
        ApplyDenominationChangeUseCase(engine, lectionary, store)

    @Test
    @DisplayName("Recomputes profile, persists canon config, and returns the profile (Req 11)")
    fun recomputesAndPersistsProfile() = runTest {
        val store = FakePreferencesStore()

        val result = useCase(store).invoke(Denomination.CATHOLIC, "ml")

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(Denomination.CATHOLIC, profile.denomination)
        assertEquals(CanonType.CATHOLIC_73, profile.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, profile.numberingScheme)
        assertEquals("rc_calendar", profile.lectionaryId)

        // The exact recomputed profile (including the enriched lectionary id) is what gets persisted.
        assertSame(profile, store.updatedProfile)
        assertEquals("rc_calendar", store.updatedProfile!!.lectionaryId)
    }

    @Test
    @DisplayName("Persistence failure surfaces as Result.failure")
    fun persistenceFailureSurfacesAsFailure() = runTest {
        val store = FakePreferencesStore(failOnSave = true)

        val result = useCase(store).invoke(Denomination.CSI, "ta")

        assertTrue(result.isFailure)
    }
}
