package com.manna.bible.data.explain

import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HybridExplanationEngine] — the on-device-first / cloud-fallback
 * orchestration. The two engines are fakes so the selection logic is verified
 * without Android or a network.
 */
class HybridExplanationEngineTest {

    private val request = ExplanationRequest(
        osisRef = "JHN.3.16",
        reference = "John 3:16",
        passageText = "For God so loved the world…",
        uiLanguageCode = "en",
        depth = ExplainDepth.PLAIN
    )

    private class FakeEngine(
        override val isConfigured: Boolean,
        private val result: ExplanationResult,
        var calls: Int = 0
    ) : ExplanationEngine {
        override suspend fun explain(request: ExplanationRequest): ExplanationResult {
            calls++
            return result
        }
    }

    @Test
    @DisplayName("uses Nano when it succeeds and never calls the cloud")
    fun nanoSucceeds() = runTest {
        val nano = FakeEngine(isConfigured = true, result = ExplanationResult.Success("on-device"))
        val cloud = FakeEngine(isConfigured = true, result = ExplanationResult.Success("cloud"))
        val hybrid = HybridExplanationEngine(nano, cloud)

        val result = hybrid.explain(request)

        assertEquals(ExplanationResult.Success("on-device"), result)
        assertEquals(1, nano.calls)
        assertEquals(0, cloud.calls, "cloud must not be called when Nano answers")
    }

    @Test
    @DisplayName("falls back to the cloud when Nano is unavailable")
    fun nanoFailsFallsBackToCloud() = runTest {
        val nano = FakeEngine(
            isConfigured = true,
            result = ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR)
        )
        val cloud = FakeEngine(isConfigured = true, result = ExplanationResult.Success("cloud"))
        val hybrid = HybridExplanationEngine(nano, cloud)

        val result = hybrid.explain(request)

        assertEquals(ExplanationResult.Success("cloud"), result)
        assertEquals(1, nano.calls)
        assertEquals(1, cloud.calls)
    }

    @Test
    @DisplayName("skips Nano entirely when it is not configured")
    fun nanoNotConfigured() = runTest {
        val nano = FakeEngine(isConfigured = false, result = ExplanationResult.Success("on-device"))
        val cloud = FakeEngine(isConfigured = true, result = ExplanationResult.Success("cloud"))
        val hybrid = HybridExplanationEngine(nano, cloud)

        val result = hybrid.explain(request)

        assertEquals(ExplanationResult.Success("cloud"), result)
        assertEquals(0, nano.calls, "an unconfigured Nano engine must not be invoked")
        assertEquals(1, cloud.calls)
    }

    @Test
    @DisplayName("reports NOT_CONFIGURED when neither engine is available")
    fun neitherConfigured() = runTest {
        val nano = FakeEngine(isConfigured = false, result = ExplanationResult.Success("x"))
        val cloud = FakeEngine(isConfigured = false, result = ExplanationResult.Success("y"))
        val hybrid = HybridExplanationEngine(nano, cloud)

        val result = hybrid.explain(request)

        assertEquals(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED),
            result
        )
        assertFalse(hybrid.isConfigured)
    }

    @Test
    @DisplayName("is configured when either engine is configured")
    fun configuredWhenEither() {
        val nano = FakeEngine(isConfigured = true, result = ExplanationResult.Success("x"))
        val cloud = FakeEngine(isConfigured = false, result = ExplanationResult.Success("y"))
        assertTrue(HybridExplanationEngine(nano, cloud).isConfigured)
    }
}
