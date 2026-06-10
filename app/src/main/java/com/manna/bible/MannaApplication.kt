package com.manna.bible

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Hilt generates the dependency graph from here.
 *
 * On creation it kicks off [MannaInitializer] to seed bundled content and refresh
 * the translation catalog in the background (never blocking first frame).
 */
@HiltAndroidApp
class MannaApplication : Application() {

    @Inject
    lateinit var initializer: MannaInitializer

    override fun onCreate() {
        super.onCreate()
        initializer.initialize()
    }
}
