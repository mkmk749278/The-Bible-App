package com.manna.bible.domain.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A time-of-day for the daily verse reminder, in 24-hour form.
 *
 * @property hour Hour of day, 0..23.
 * @property minute Minute of hour, 0..59.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class ReminderTime(val hour: Int, val minute: Int) {

    /** Serializes to `HH:mm` (e.g. "07:05"). */
    fun format(): String = "%02d:%02d".format(hour, minute)

    companion object {
        /** Default reminder time (07:00) used until the user picks one. */
        val DEFAULT = ReminderTime(7, 0)

        /**
         * Parses an `HH:mm` string into a [ReminderTime], or returns [DEFAULT] when
         * [value] is null/blank or malformed. Out-of-range values are rejected.
         */
        fun parse(value: String?): ReminderTime {
            if (value.isNullOrBlank()) return DEFAULT
            val parts = value.trim().split(":")
            if (parts.size != 2) return DEFAULT
            val hour = parts[0].toIntOrNull() ?: return DEFAULT
            val minute = parts[1].toIntOrNull() ?: return DEFAULT
            if (hour !in 0..23 || minute !in 0..59) return DEFAULT
            return ReminderTime(hour, minute)
        }
    }
}

/**
 * Computes the next wall-clock instant at which a [ReminderTime] should fire.
 *
 * Pure Kotlin (java.time only) so the scheduling decision is unit-testable without
 * an emulator; the Android `AlarmManager` wrapper consumes the millis it returns.
 */
object NextReminderTime {

    /**
     * Returns the next [ZonedDateTime] at [time] strictly after [now]: today if the
     * time is still ahead, otherwise tomorrow (also tomorrow when exactly equal).
     */
    fun nextTrigger(time: ReminderTime, now: ZonedDateTime): ZonedDateTime {
        val candidate = now
            .withHour(time.hour)
            .withMinute(time.minute)
            .withSecond(0)
            .withNano(0)
        return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
    }

    /** [nextTrigger] expressed as epoch millis, evaluated in [zone] from [nowMillis]. */
    fun nextTriggerMillis(
        time: ReminderTime,
        nowMillis: Long,
        zone: ZoneId
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        return nextTrigger(time, now).toInstant().toEpochMilli()
    }
}
