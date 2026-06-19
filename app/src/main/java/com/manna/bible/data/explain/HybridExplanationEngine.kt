package com.manna.bible.data.explain

import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import javax.inject.Inject
import javax.inject.Named

/**
 * The [ExplanationEngine] the app actually uses: **on-device first, cloud as
 * fallback**. It prefers Gemini Nano (offline, private, free) when it is available,
 * and falls back to the cloud Gemini Flash engine when Nano can't answer — so the
 * feature degrades gracefully across the whole device range:
 *
 *  - New device + flag on → Nano answers offline; cloud never called.
 *  - Nano unavailable/fails, cloud key present → cloud answers (needs network).
 *  - Neither available → reports the most useful reason (cloud's, e.g. OFFLINE /
 *    NOT_CONFIGURED, falling back to Nano's).
 *
 * Successful results are cached upstream by `DefaultExplanationRepository`, so once a
 * passage is explained by either engine it is available offline forever.
 *
 * Pure orchestration over two injected engines — no Android dependencies — so the
 * selection logic is fully JVM-testable.
 */
class HybridExplanationEngine @Inject constructor(
    @Named("nanoEngine") private val nano: ExplanationEngine,
    @Named("cloudEngine") private val cloud: ExplanationEngine
) : ExplanationEngine {

    override val isConfigured: Boolean
        get() = nano.isConfigured || cloud.isConfigured

    override suspend fun explain(request: ExplanationRequest): ExplanationResult {
        val nanoResult = if (nano.isConfigured) nano.explain(request) else null
        if (nanoResult is ExplanationResult.Success) return nanoResult

        if (cloud.isConfigured) {
            return cloud.explain(request)
        }

        return nanoResult
            ?: ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED)
    }
}
