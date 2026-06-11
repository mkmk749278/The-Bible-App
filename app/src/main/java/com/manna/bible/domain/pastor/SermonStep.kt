package com.manna.bible.domain.pastor

/**
 * The five ordered stages of Pastor Mode's guided sermon-preparation flow.
 *
 * Steps are presented to the pastor strictly in declaration order. The enum
 * carries no Android references — it is pure domain logic, safe to use from
 * ViewModels and tests.
 *
 * The flow walks from choosing a passage through to a concrete application:
 *
 * 1. [PASSAGE] — pick the text.
 * 2. [OBSERVE] — note what the text says.
 * 3. [OUTLINE] — structure the main points.
 * 4. [ILLUSTRATE] — find stories/images that land the truth.
 * 5. [APPLY] — call the congregation to a response.
 */
enum class SermonStep {
    /** Choose the passage God is leading the pastor to preach. */
    PASSAGE,

    /** Observe what the passage actually says — context, key words, themes. */
    OBSERVE,

    /** Outline the sermon's main points and their flow. */
    OUTLINE,

    /** Gather illustrations, stories, and images that make the truth vivid. */
    ILLUSTRATE,

    /** Land the application — how should the congregation respond this week. */
    APPLY;

    /** Zero-based position of this step within the ordered flow. */
    val index: Int get() = ordinal

    /** One-based position for display (e.g. "Step 2 of 5"). */
    val displayNumber: Int get() = ordinal + 1

    companion object {
        /** The steps in presentation order. */
        val ordered: List<SermonStep> get() = entries.toList()

        /** Total number of steps in the flow. */
        val count: Int get() = entries.size

        /** The first step in the flow. */
        val first: SermonStep get() = entries.first()

        /** The last step in the flow. */
        val last: SermonStep get() = entries.last()
    }
}
