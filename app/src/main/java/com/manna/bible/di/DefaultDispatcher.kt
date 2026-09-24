package com.manna.bible.di

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineDispatcher] for CPU-bound work that must stay
 * off the main thread — e.g. PBKDF2 PIN hashing, which takes hundreds of milliseconds on
 * the low-end phones Manna targets. Injected (rather than hard-coding
 * `Dispatchers.Default`) so ViewModel tests can run it on the test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
