package com.manna.bible.data.explain

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationPrompt
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Cloud [ExplanationEngine] backed by Gemini Flash. Builds the prompt with
 * [ExplanationPrompt], calls the API with the injected key, and maps the first
 * candidate's text back. Network failures surface as [ExplanationUnavailableReason.OFFLINE];
 * a blank key as [ExplanationUnavailableReason.NOT_CONFIGURED]; anything else as ERROR.
 *
 * The key is injected (from `BuildConfig.GEMINI_API_KEY`) rather than read here, so the
 * engine stays unit-testable without Android.
 */
class GeminiExplanationEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String
) : ExplanationEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun explain(request: ExplanationRequest): ExplanationResult {
        if (apiKey.isBlank()) {
            return ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED)
        }
        return try {
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(ExplanationPrompt.build(request))))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.4f,
                        maxOutputTokens = 1024
                    )
                )
            )
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (text.isNullOrBlank()) {
                ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR)
            } else {
                ExplanationResult.Success(text)
            }
        } catch (e: IOException) {
            ExplanationResult.Unavailable(ExplanationUnavailableReason.OFFLINE)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR)
        }
    }

    private companion object {
        /** Fast, low-cost model suited to on-the-fly explanations. */
        const val MODEL = "gemini-2.0-flash"
    }
}
