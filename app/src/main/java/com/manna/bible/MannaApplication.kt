package com.manna.bible

import android.app.Application
import com.manna.bible.data.reminder.ReminderCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Hilt generates the dependency graph from here.
 *
 * On creation it kicks off [MannaInitializer] to seed bundled content and refresh
 * the translation catalog in the background (never blocking first frame), and
 * starts [ReminderCoordinator] so the daily-verse alarm tracks the saved
 * preferences and is re-armed on every cold start.
 */
@HiltAndroidApp
class MannaApplication : Application() {

    @Inject
    lateinit var initializer: MannaInitializer

    @Inject
    lateinit var reminderCoordinator: ReminderCoordinator

    override fun onCreate() {
        super.onCreate()
        initializer.initialize()
        reminderCoordinator.start()
    }
}
