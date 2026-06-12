package com.manna.bible.domain.fasting

/**
 * Pure progress maths for an active fast, so the calculation is JVM-testable
 * without a clock or Android. All times are epoch millis; durations in hours.
 */
object FastProgress {

    private const val MILLIS_PER_HOUR = 3_600_000L

    /** Total length of a fast in millis. */
    fun totalMillis(hours: Int): Long = hours.coerceAtLeast(0) * MILLIS_PER_HOUR

    /** Millis elapsed since [startMillis] at [nowMillis] (never negative). */
    fun elapsedMillis(startMillis: Long, nowMillis: Long): Long =
        (nowMillis - startMillis).coerceAtLeast(0L)

    /** Millis remaining until the fast completes (never negative). */
    fun remainingMillis(startMillis: Long, hours: Int, nowMillis: Long): Long =
        (totalMillis(hours) - elapsedMillis(startMillis, nowMillis)).coerceAtLeast(0L)

    /** Progress through the fast in 0f..1f. */
    fun fraction(startMillis: Long, hours: Int, nowMillis: Long): Float {
        val total = totalMillis(hours)
        if (total <= 0L) return 1f
        return (elapsedMillis(startMillis, nowMillis).toFloat() / total).coerceIn(0f, 1f)
    }

    /** True once the fast's duration has elapsed. */
    fun isComplete(startMillis: Long, hours: Int, nowMillis: Long): Boolean =
        elapsedMillis(startMillis, nowMillis) >= totalMillis(hours)
}
