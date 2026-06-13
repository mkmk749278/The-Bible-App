package com.manna.bible.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Google Gemini generative-language REST API
 * (`https://generativelanguage.googleapis.com/`). Used by the cloud "Explain this
 * passage" engine. The API key is passed as a query parameter (never logged).
 */
interface GeminiApi {

    /** `POST v1beta/models/{model}:generateContent?key=…` — single-turn generation. */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") key: String,
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
    val maxOutputTokens: Int? = null
)

@Serializable
data class GeminiResponseDto(
    val candidates: List<GeminiCandidateDto> = emptyList()
)

@Serializable
data class GeminiCandidateDto(
    val content: GeminiContentDto? = null
)
