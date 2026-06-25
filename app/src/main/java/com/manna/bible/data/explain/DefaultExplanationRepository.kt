package com.manna.bible.data.explain

import com.manna.bible.data.local.ExplanationDao
import com.manna.bible.data.local.ExplanationEntity
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationRepository
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import javax.inject.Inject

/**
 * Default [ExplanationRepository]: cache-first. A successful explanation is stored in
 * the Room [ExplanationDao] keyed by passage + language + depth, so the same request is
 * fetched once and then served offline. Failures are returned but never cached, so a
 * later online retry can succeed.
 */
class DefaultExplanationRepository @Inject constructor(
    private val engine: ExplanationEngine,
    private val dao: ExplanationDao
) : ExplanationRepository {

    override fun isConfigured(): Boolean = engine.isConfigured

    override suspend fun explain(request: ExplanationRequest): ExplanationResult {
        val key = cacheKey(request)
        dao.get(key)?.let { return ExplanationResult.Success(it.text) }

        val result = engine.explain(request)
        if (result is ExplanationResult.Success) {
            dao.insert(ExplanationEntity(key, result.text, System.currentTimeMillis()))
        }
        return result
    }

    private fun cacheKey(request: ExplanationRequest): String =
        "${request.osisRef}|${request.uiLanguageCode}|${request.depth.name}|${request.denomination?.id ?: "any"}"
}
