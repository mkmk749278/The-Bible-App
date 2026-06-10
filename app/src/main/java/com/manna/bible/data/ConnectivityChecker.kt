package com.manna.bible.data

/**
 * Reports whether the device currently has an internet connection.
 *
 * Interface only — the Android `ConnectivityManager`-backed implementation is
 * deferred to a later task. Abstracting it keeps the repository JVM-testable.
 */
interface ConnectivityChecker {
    /** True if a network connection is currently available. */
    fun isOnline(): Boolean
}
