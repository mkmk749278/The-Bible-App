package com.manna.bible.domain.usecase

import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.AnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ResolveCanonSwitchImpactUseCase].
 *
 * Runs on the JVM (JUnit 5) with hand-rolled fakes for [CanonEngine] and
 * [AnnotationRepository]; no emulator or DI container required. Verifies that the
 * use case reports the annotated books a candidate canon would hide (Req 12).
 *
 * Validates: Requirements 12
 */
class ResolveCanonSwitchImpactUseCaseTest {

    /** [CanonEngine] fake whose [profileFor] returns a profile over [visibleBookIds]. */
    private class FakeCanonEngine(private val visibleBookIds: List<String>) : CanonEngine {
        override fun canonTypeFor(denomination: Denomination): CanonType = CanonType.PROTESTANT_66

        override suspend fun profileFor(
            denomination: Denomination,
            bibleLanguage: String
        ): CanonProfile = CanonProfile(
            denomination = denomination,
            canonType = CanonType.PROTESTANT_66,
            books = visibleBookIds.mapIndexed { index, osisId ->
                CanonBook(
                    osisId = osisId,
                    testament = Testament.OLD,
                    orderIndex = index,
                    isDeuterocanonical = false
                )
            },
            numberingScheme = NumberingScheme.MASORETIC,
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )
    }

    /** [AnnotationRepository] fake backed by a fixed set of annotated book ids. */
    private class FakeAnnotationRepository(private val annotatedBookIds: Set<String>) : AnnotationRepository {
        override suspend fun allAnnotatedBookIds(): Set<String> = annotatedBookIds

        override suspend fun annotatedBookIdsOutside(visibleBookIds: Set<String>): Set<String> =
            annotatedBookIds - visibleBookIds

        override fun visibleHighlights(visibleBookIds: Set<String>): Flow<List<Highlight>> = flowOf(emptyList())
        override fun visibleBookmarks(visibleBookIds: Set<String>): Flow<List<Bookmark>> = flowOf(emptyList())
        override fun visibleNotes(visibleBookIds: Set<String>): Flow<List<Note>> = flowOf(emptyList())
    }

    @Test
    fun `switching to a canon without TOB reports the hidden annotated book`() = runTest {
        // Target canon (Protestant 66) excludes the deuterocanonical Tobit (TOB).
        val canonEngine = FakeCanonEngine(visibleBookIds = listOf("GEN", "PSA", "JHN"))
        val annotationRepository = FakeAnnotationRepository(setOf("GEN", "TOB"))
        val useCase = ResolveCanonSwitchImpactUseCase(canonEngine, annotationRepository)

        val impact = useCase(Denomination.PROTESTANT_OTHER, "en")

        assertEquals(setOf("TOB"), impact.excludedBookIds)
        assertTrue(impact.hasImpact)
    }

    @Test
    fun `switching to a canon covering every annotated book reports no impact`() = runTest {
        val canonEngine = FakeCanonEngine(visibleBookIds = listOf("GEN", "PSA", "JHN", "TOB"))
        val annotationRepository = FakeAnnotationRepository(setOf("GEN", "TOB"))
        val useCase = ResolveCanonSwitchImpactUseCase(canonEngine, annotationRepository)

        val impact = useCase(Denomination.CATHOLIC, "en")

        assertTrue(impact.excludedBookIds.isEmpty())
        assertFalse(impact.hasImpact)
    }

    @Test
    fun `no annotations reports no impact`() = runTest {
        val canonEngine = FakeCanonEngine(visibleBookIds = listOf("GEN", "PSA", "JHN"))
        val annotationRepository = FakeAnnotationRepository(emptySet())
        val useCase = ResolveCanonSwitchImpactUseCase(canonEngine, annotationRepository)

        val impact = useCase(Denomination.PROTESTANT_OTHER, "en")

        assertTrue(impact.excludedBookIds.isEmpty())
        assertFalse(impact.hasImpact)
    }
}
