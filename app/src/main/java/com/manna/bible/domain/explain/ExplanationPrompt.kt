package com.manna.bible.domain.explain

import com.manna.bible.domain.model.Denomination

/**
 * Builds the instruction prompt for the explanation engines. Pure and deterministic so
 * it can be unit-tested and shared by the cloud and on-device engines alike.
 *
 * The voice is a faithful, pastoral Bible teacher: warm, clear, and jargon-free —
 * serving both the layperson who wants to understand a verse and the preacher
 * preparing to teach it (Phase 3, "Explain this passage").
 *
 * The prompt also carries an Indian Cultural Lens (F-01): every explanation is grounded
 * in Indian lived experience, and — when the reader's [Denomination] is known — framed
 * for that tradition. The cultural lens shapes the *application* of the text; it never
 * overrides the historic Christian reading.
 */
object ExplanationPrompt {

    fun build(request: ExplanationRequest): String = buildString {
        appendLine(
            "You are a faithful, pastoral Bible teacher helping an ordinary believer " +
                "understand Scripture. Be warm, clear, and concise. Avoid jargon; when a " +
                "term is unavoidable, explain it in plain words."
        )
        appendLine()
        // Cultural and denominational grounding (F-01 Indian Cultural Lens).
        val culturalInstruction = buildCulturalInstruction(request)
        if (culturalInstruction.isNotBlank()) {
            appendLine(culturalInstruction)
            appendLine()
        }
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

    /**
     * Builds the cultural-grounding block for [request]: an optional denomination-specific
     * framing followed by the Indian cultural lens that applies to every reader. Returns
     * an empty string only in the impossible case where nothing applies (the Indian lens
     * is always present).
     */
    private fun buildCulturalInstruction(request: ExplanationRequest): String {
        val parts = mutableListOf<String>()

        // Denominational framing.
        val denominationNote = when (request.denomination) {
            Denomination.CATHOLIC ->
                "The reader belongs to the Catholic tradition. Where relevant, acknowledge " +
                    "the sacramental, Marian, or magisterial dimensions of the text as a " +
                    "Catholic would understand them."
            Denomination.ORTHODOX ->
                "The reader belongs to the Orthodox tradition. Where relevant, draw on the " +
                    "patristic and liturgical reading of the text as practised in the Orthodox Church."
            Denomination.CSI ->
                "The reader belongs to the Church of South India (CSI), a South Indian " +
                    "Protestant-Anglican tradition. Where relevant, reference the liturgical " +
                    "calendar and social-justice heritage of the CSI."
            Denomination.MAR_THOMA ->
                "The reader belongs to the Mar Thoma Church, a reformed Oriental tradition " +
                    "rooted in the Kerala church of St Thomas. Honour both its reformed " +
                    "theology and its ancient Syrian heritage in how you apply the text."
            Denomination.SHOW_EVERYTHING, Denomination.PROTESTANT_OTHER, null -> ""
        }
        if (denominationNote.isNotBlank()) parts.add(denominationNote)

        // Cultural grounding — applied to all Indian language readers.
        parts.add(
            "The reader lives in India. In your application section, use imagery, analogies, " +
                "and illustrations that are natural to Indian daily life — rice fields, monsoons, " +
                "family honour, joint families, village community, chai — rather than Western " +
                "equivalents. Where the original text uses agrarian or social imagery (harvest, " +
                "debt, servants, bread), name its Indian equivalent. Where the text speaks to " +
                "social equality, acknowledge the caste dimension directly and honestly, then " +
                "explain what the Gospel says about it. Never override the historic Christian " +
                "reading — add the Indian lens to the application, do not replace the meaning."
        )

        return parts.joinToString("\n\n")
    }
}
