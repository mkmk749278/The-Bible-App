package com.manna.bible.domain.sermon

import com.manna.bible.domain.model.Denomination

/**
 * The kind of congregation a sermon outline is being prepared for (F-04). The chosen
 * type shapes the tone and register of the generated outline — a mixed Sunday gathering,
 * a youth service, or a funeral / memorial where grief must be honoured.
 *
 * @property id stable, persistence-safe identifier (lower-case, never localised).
 */
enum class CongregationType(val id: String) {
    /** Mixed Sunday congregation of all ages. */
    GENERAL("general"),

    /** Teens and young adults — modern references welcome. */
    YOUTH("youth"),

    /** A funeral or memorial service — gentle, comforting, never triumphalist. */
    GRIEF("grief")
}

/**
 * A request to generate a sermon outline for a passage (F-04).
 *
 * @property reference free-text scripture reference, e.g. "John 4:1-26".
 * @property denomination the preacher's tradition (from setup), or null to leave the
 *   homiletic framing unconstrained.
 * @property languageCode ISO code of the Bible language the outline must be written in.
 * @property congregationType the audience the outline is being shaped for.
 */
data class SermonOutlineRequest(
    val reference: String,
    val denomination: Denomination?,
    val languageCode: String,
    val congregationType: CongregationType
)

/**
 * The outcome of a single sermon-outline generation (F-04).
 *
 * Outlines are never cached: each generation is fresh, and the saved sermon is the
 * user's own edited content, not an AI artefact. [Offline] carries no message — the UI
 * sources its wording from string resources so the engine stays free of presentation
 * concerns (and, like the rest of this contract, pure Kotlin with no Android dependency).
 */
sealed interface SermonOutlineResult {

    /**
     * A successfully generated outline.
     *
     * @property outlineText the full, editable outline text in the Bible language.
     */
    data class Success(val outlineText: String) : SermonOutlineResult

    /** The request could not reach the network; the manual editor remains usable. */
    data object Offline : SermonOutlineResult

    /** The builder is unavailable (not configured, empty output, HTTP error). */
    data class Unavailable(val reason: String) : SermonOutlineResult
}

/**
 * AI sermon-outline builder for the Village Pastor Sermon Helper (F-04). Given a passage
 * reference, the preacher's tradition, language, and congregation type, returns a complete,
 * editable sermon outline — an outline, not finished prose — rooted in Indian daily life.
 *
 * The notepad remains the primary surface; this output is an editable starting point.
 *
 * Pure Kotlin — no Android dependency — so it is fully JVM-testable.
 */
interface SermonAiEngine {

    /** True when the engine has the credentials it needs to make a request. */
    val isConfigured: Boolean

    /** Generates an editable sermon outline for [request]. */
    suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult
}
