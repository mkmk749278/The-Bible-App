package com.manna.bible.domain.explain

/**
 * Builds the instruction prompt for the explanation engines. Pure and deterministic so
 * it can be unit-tested and shared by the cloud and on-device engines alike.
 *
 * The voice is a faithful, pastoral Bible teacher: warm, clear, and jargon-free —
 * serving both the layperson who wants to understand a verse and the preacher
 * preparing to teach it (Phase 3, "Explain this passage").
 */
object ExplanationPrompt {

    fun build(request: ExplanationRequest): String = buildString {
        appendLine(
            "You are a faithful, pastoral Bible teacher helping an ordinary believer " +
                "understand Scripture. Be warm, clear, and concise. Avoid jargon; when a " +
                "term is unavoidable, explain it in plain words."
        )
        appendLine()
        appendLine("Passage: ${request.reference}")
        appendLine("Text: ${request.passageText}")
        appendLine()
        when (request.depth) {
            ExplainDepth.PLAIN -> appendLine(
                "Explain this passage in three short parts: (1) the context, (2) what it " +
                    "means, and (3) how to apply it today."
            )
            ExplainDepth.PREACHING -> appendLine(
                "Explain this passage for someone preparing to preach or teach it: " +
                    "(1) the context, (2) what it means, (3) how to apply it today, " +
                    "(4) a short three-point outline, (5) two or three cross-references, " +
                    "and (6) one illustration idea."
            )
        }
        appendLine()
        append(
            "Write the entire response in the language identified by the ISO code " +
                "'${request.uiLanguageCode}'. Keep it faithful to the historic Christian " +
                "reading of the text and do not invent facts."
        )
    }
}
