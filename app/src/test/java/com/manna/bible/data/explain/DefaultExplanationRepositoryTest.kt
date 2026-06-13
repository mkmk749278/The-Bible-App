package com.manna.bible.data.explain

import com.manna.bible.data.local.ExplanationDao
import com.manna.bible.data.local.ExplanationEntity
import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultExplanationRepository] — cache-first behaviour. */
class DefaultExplanationRepositoryTest {

    private fun request() = ExplanationRequest(
        osisRef = "GEN.2.1",
        reference = "Genesis 2:1",
        passageText = "Thus the heavens…",
        uiLanguageCode = "en",
        depth = ExplainDepth.PLAIN
    )

    @Test
    @DisplayName("A cache miss asks the engine once and stores the result")
    fun cacheMiss() = runTest {
        val dao = FakeDao()
        val engine = FakeEngine(ExplanationResult.Success("An explanation."))
        val repo = DefaultExplanationRepository(engine, dao)

        val result = repo.explain(request())

        assertEquals(ExplanationResult.Success("An explanation."), result)
        assertEquals(1, engine.calls)
        assertEquals(1, dao.stored.size)
    }

    @Test
    @DisplayName("A cache hit is served without calling the engine again")
    fun cacheHit() = runTest {
        val dao = FakeDao()
        val engine = FakeEngine(ExplanationResult.Success("Cached me."))
        val repo = DefaultExplanationRepository(engine, dao)

        repo.explain(request())
        val second = repo.explain(request())

        assertEquals(ExplanationResult.Success("Cached me."), second)
        assertEquals(1, engine.calls) // not called the second time
    }

    @Test
    @DisplayName("Failures are returned but not cached, so a retry hits the engine again")
    fun failureNotCached() = runTest {
        val dao = FakeDao()
        val engine = FakeEngine(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.OFFLINE)
        )
        val repo = DefaultExplanationRepository(engine, dao)

        repo.explain(request())
        repo.explain(request())

        assertTrue(dao.stored.isEmpty())
        assertEquals(2, engine.calls)
    }

    private class FakeDao : ExplanationDao {
        val stored = mutableMapOf<String, ExplanationEntity>()
        override suspend fun get(cacheKey: String): ExplanationEntity? = stored[cacheKey]
        override suspend fun insert(entity: ExplanationEntity) {
            stored[entity.cacheKey] = entity
        }
    }

    private class FakeEngine(
        private val result: ExplanationResult,
        override val isConfigured: Boolean = true
    ) : ExplanationEngine {
        var calls = 0
        override suspend fun explain(request: ExplanationRequest): ExplanationResult {
            calls++
            return result
        }
    }
}
