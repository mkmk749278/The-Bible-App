package com.manna.bible.domain.explain

import com.manna.bible.domain.model.Denomination

/** How deep an explanation should go. */
enum class ExplainDepth {
    /** A plain, warm explanation for an ordinary reader: context, meaning, application. */
    PLAIN,

    /** The above plus a short outline, cross-references, and an illustration idea. */
    PREACHING
}

/**
 * A request to explain a passage.
 *
 * @property osisRef canonical opening reference (`OSIS.CHAPTER.VERSE`) — the cache key
 *   and the target for "read in context".
 * @property reference the human reference for the prompt (e.g. "Genesis 2:1").
 * @property passageText the verse text(s) to explain, in the Bible language.
 * @property uiLanguageCode the language the explanation should be written in.
 * @property depth [ExplainDepth].
 * @property denomination the reader's tradition, used to add a denomination-aware
 *   cultural lens to the explanation. Null applies no denominational constraint.
 */
data class ExplanationRequest(
    val osisRef: String,
    val reference: String,
    val passageText: String,
    val uiLanguageCode: String,
    val depth: ExplainDepth,
    val denomination: Denomination? = null
)

/** Why an explanation could not be produced. */
enum class ExplanationUnavailableReason {
    /** No API key configured and no on-device engine available. */
    NOT_CONFIGURED,

    /** The device is offline and the passage isn't cached. */
    OFFLINE,

    /** The engine returned an error or empty response. */
    ERROR
}

/** The outcome of an explanation request. */
sealed interface ExplanationResult {
    data class Success(val text: String) : ExplanationResult

    /**
     * No explanation was produced.
     *
     * @property reason the coarse cause shown to the user.
     * @property detail an optional engine-level diagnostic (e.g. "Cloud HTTP 403:
     *   API key not authorized") surfaced under the message to help configure the
     *   feature. Null for the ordinary, expected cases.
     */
    data class Unavailable(
        val reason: ExplanationUnavailableReason,
        val detail: String? = null
    ) : ExplanationResult
}

/**
 * Produces an explanation for a passage. Implementations may be cloud (Gemini) or
 * on-device (Nano); the repository picks the best available one.
 *
 * Pure domain contract — no Android dependencies.
 */
interface ExplanationEngine {
    /** True when this engine can run (key present / model available). */
    val isConfigured: Boolean

    /** Explains [request], or reports why it could not. */
    suspend fun explain(request: ExplanationRequest): ExplanationResult
}

/**
 * Explains passages, caching successful results so each passage is fetched once and is
 * then available offline forever (offline-first).
 */
interface ExplanationRepository {
    /** True when an explanation engine is configured (e.g. an API key is present). */
    fun isConfigured(): Boolean

    /** Returns a cached explanation if present, otherwise asks the engine and caches it. */
    suspend fun explain(request: ExplanationRequest): ExplanationResult
}
