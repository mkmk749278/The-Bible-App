package com.manna.bible.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the Google Gemini generative-language REST API
 * (`https://generativelanguage.googleapis.com/`). Used by the cloud AI engines.
 *
 * The API key travels in the `x-goog-api-key` header rather than the `?key=` query
 * string, so it never appears in request URLs (which end up in proxy logs, crash
 * reports and exception messages). It is never logged.
 */
interface GeminiApi {

    /** `POST v1beta/models/{model}:generateContent` — single-turn generation. */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Header("x-goog-api-key") key: String,
        @Body body: GeminiRequestDto
    ): GeminiResponseDto
}

@Serializable
data class GeminiRequestDto(
    val contents: List<GeminiContentDto>,
    val generationConfig: GeminiGenerationConfigDto? = null
)

@Serializable
data class GeminiContentDto(
    val parts: List<GeminiPartDto>,
    val role: String? = null
)

@Serializable
data class GeminiPartDto(val text: String)

@Serializable
data class GeminiGenerationConfigDto(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val thinkingConfig: GeminiThinkingConfigDto? = null
)

/**
 * Controls Gemini 2.5 "thinking". Thinking is on by default for `gemini-2.5-flash`, and
 * thought tokens are billed against `maxOutputTokens` — with the small budgets these
 * engines use (256–1024), the model can spend the whole budget thinking and return an
 * empty or truncated answer. Every engine therefore sends [OFF] explicitly (the shared
 * [kotlinx.serialization.json.Json] does not encode default values, so it must be passed).
 */
@Serializable
data class GeminiThinkingConfigDto(val thinkingBudget: Int) {
    companion object {
        /** `thinkingBudget = 0` disables thinking on Gemini 2.5 Flash. */
        val OFF = GeminiThinkingConfigDto(thinkingBudget = 0)
    }
}

@Serializable
data class GeminiResponseDto(
    val candidates: List<GeminiCandidateDto> = emptyList()
)

@Serializable
data class GeminiCandidateDto(
    val content: GeminiContentDto? = null
)
