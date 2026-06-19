package com.manna.bible.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Unit tests for [ReminderTime] parsing/formatting and [NextReminderTime]
 * next-trigger computation.
 */
class ReminderTimeTest {

    @Test
    @DisplayName("parses valid HH:mm and rejects malformed/out-of-range to DEFAULT")
    fun parsing() {
        assertEquals(ReminderTime(7, 5), ReminderTime.parse("07:05"))
        assertEquals(ReminderTime(23, 59), ReminderTime.parse("23:59"))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse(null))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse(""))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse("7"))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse("24:00"))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse("07:60"))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse("ab:cd"))
    }

    @Test
    @DisplayName("formats as zero-padded HH:mm")
    fun formatting() {
        assertEquals("07:00", ReminderTime(7, 0).format())
        assertEquals("18:09", ReminderTime(18, 9).format())
    }

    @Test
    @DisplayName("minuteOfDay is a stable id; parseList/formatList sort, dedupe and clean")
    fun listHelpers() {
        assertEquals(0, ReminderTime(0, 0).minuteOfDay)
        assertEquals(740, ReminderTime(12, 20).minuteOfDay)

        // Unsorted, duplicated, and malformed entries -> sorted, distinct, valid only.
        assertEquals(
            listOf(ReminderTime(8, 0), ReminderTime(12, 0), ReminderTime(20, 0)),
            ReminderTime.parseList("20:00, 08:00 ,12:00,08:00,bad,25:00")
        )
        assertEquals(emptyList<ReminderTime>(), ReminderTime.parseList(null))
        assertEquals(emptyList<ReminderTime>(), ReminderTime.parseList(""))

        assertEquals("08:00,12:00,20:00",
            ReminderTime.formatList(listOf(ReminderTime(20, 0), ReminderTime(8, 0), ReminderTime(12, 0))))
    }

    @Test
    @DisplayName("next trigger is today when the time is still ahead")
    fun triggerLaterToday() {
        val zone = ZoneId.of("UTC")
        // Now: 06:00 UTC; reminder at 07:00 → today 07:00.
        val now = LocalDateTime.of(2026, 6, 11, 6, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val expected = LocalDateTime.of(2026, 6, 11, 7, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, NextReminderTime.nextTriggerMillis(ReminderTime(7, 0), now, zone))
    }

    @Test
    @DisplayName("next trigger rolls to tomorrow when the time has passed")
    fun triggerTomorrow() {
        val zone = ZoneId.of("UTC")
        // Now: 08:00 UTC; reminder at 07:00 → tomorrow 07:00.
        val now = LocalDateTime.of(2026, 6, 11, 8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val expected = LocalDateTime.of(2026, 6, 12, 7, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, NextReminderTime.nextTriggerMillis(ReminderTime(7, 0), now, zone))
    }

    @Test
    @DisplayName("exact-equal time rolls to tomorrow (must be strictly after now)")
    fun triggerExactEqualRollsOver() {
        val zone = ZoneId.of("UTC")
        val now = LocalDateTime.of(2026, 6, 11, 7, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val expected = LocalDateTime.of(2026, 6, 12, 7, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, NextReminderTime.nextTriggerMillis(ReminderTime(7, 0), now, zone))
    }
}
