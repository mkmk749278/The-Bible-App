package com.manna.bible.data.explain

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiCandidateDto
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.data.remote.GeminiResponseDto
import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

/** Unit tests for [GeminiExplanationEngine] — response mapping and failure modes. */
class GeminiExplanationEngineTest {

    private fun request() = ExplanationRequest(
        osisRef = "GEN.2.1",
        reference = "Genesis 2:1",
        passageText = "Thus the heavens…",
        uiLanguageCode = "en",
        depth = ExplainDepth.PLAIN
    )

    @Test
    @DisplayName("A blank key is not configured and returns NOT_CONFIGURED")
    fun blankKey() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { GeminiResponseDto() }, apiKey = "")
        assertFalse(engine.isConfigured)
        assertEquals(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED),
            engine.explain(request())
        )
    }

    @Test
    @DisplayName("A candidate's text is mapped to Success")
    fun success() = runTest {
        val engine = GeminiExplanationEngine(
            FakeApi {
                GeminiResponseDto(
                    candidates = listOf(
                        GeminiCandidateDto(GeminiContentDto(listOf(GeminiPartDto("  Here is what it means.  "))))
                    )
                )
            },
            apiKey = "key"
        )
        assertEquals(
            ExplanationResult.Success("Here is what it means."),
            engine.explain(request())
        )
    }

    @Test
    @DisplayName("A network failure maps to OFFLINE")
    fun offline() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { throw IOException("no net") }, apiKey = "key")
        assertEquals(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.OFFLINE),
            engine.explain(request())
        )
    }

    @Test
    @DisplayName("An empty response maps to ERROR")
    fun empty() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { GeminiResponseDto() }, apiKey = "key")
        assertEquals(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR),
            engine.explain(request())
        )
    }

    private class FakeApi(
        private val behavior: suspend () -> GeminiResponseDto
    ) : GeminiApi {
        override suspend fun generate(
            model: String,
            key: String,
            body: GeminiRequestDto
        ): GeminiResponseDto = behavior()
    }
}
