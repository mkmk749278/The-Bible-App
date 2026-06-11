package com.manna.bible.domain.reminder

/**
 * Schedules (or cancels) the recurring daily verse reminder.
 *
 * Implemented in the data layer by an `AlarmManager`-backed wrapper; kept as a
 * domain interface so the coordinator and ViewModels stay free of Android types
 * and can be tested with fakes.
 */
interface ReminderScheduler {

    /** Schedules a daily reminder at [time], replacing any existing schedule. */
    fun schedule(time: ReminderTime)

    /** Cancels any scheduled daily reminder. */
    fun cancel()
}
