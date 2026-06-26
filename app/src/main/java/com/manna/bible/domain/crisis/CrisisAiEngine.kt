package com.manna.bible.domain.crisis

import com.manna.bible.domain.model.Denomination

/**
 * The outcome of a single-turn Crisis AI Companion request (F-03).
 *
 * The companion takes an open-text description of a person's situation and returns
 * the single most relevant passage, explained compassionately in the Bible language.
 * The curated offline comfort list ([CrisisCompanion]) remains the primary, no-network
 * fallback; this enrichment layer is the only part that needs connectivity.
 *
 * [Offline] carries no message — the UI sources its wording from string resources so
 * the engine stays free of presentation concerns (and, like the rest of this contract,
 * pure Kotlin with no Android dependency).
 */
sealed interface CrisisAiResult {

    /**
     * A successfully chosen passage and its compassionate explanation.
     *
     * @property passageRef Display reference, e.g. "Psalm 34:18".
     * @property osisRef Canonical `OSIS.CHAPTER.VERSE`, e.g. "PSA.34.18".
     * @property explanation 2–3 sentences in the Bible language, leading with comfort.
     */
    data class Success(
        val passageRef: String,
        val osisRef: String,
        val explanation: String
    ) : CrisisAiResult

    /** The request could not reach the network; fall through to the curated list. */
    data object Offline : CrisisAiResult

    /** The companion is unavailable (not configured, malformed output, HTTP error). */
    data class Unavailable(val reason: String) : CrisisAiResult
}

/**
 * Single-turn AI companion for Crisis Mode (F-03). Given a private description of a
 * person's situation, returns the most directly relevant passage with a short, pastoral
 * explanation in their Bible language.
 *
 * Privacy contract: implementations must never log, persist, or echo the [situation]
 * text — not in any log line, exception message, or [CrisisAiResult.Unavailable.reason].
 *
 * Pure Kotlin — no Android dependency — so it is fully JVM-testable.
 */
interface CrisisAiEngine {

    /** True when the engine has the credentials it needs to make a request. */
    val isConfigured: Boolean

    /**
     * Chooses and explains the passage most relevant to [situation].
     *
     * @param situation Free-text, private description of what the person is facing.
     * @param languageCode ISO code of the Bible language the response must be written in.
     * @param isNight When true, the response is prefaced to be quieter and more present
     *   (the person is alone in the middle of the night).
     * @param denomination The person's tradition, used to frame the passage, or null.
     */
    suspend fun respond(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): CrisisAiResult
}
