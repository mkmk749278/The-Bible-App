package com.manna.bible.di

import javax.inject.Qualifier

/**
 * Qualifies the application-lifetime [kotlinx.coroutines.CoroutineScope] used for
 * work that must outlive any single screen — notably Bible downloads, which keep
 * running when the user navigates away or backgrounds the app.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadScope
