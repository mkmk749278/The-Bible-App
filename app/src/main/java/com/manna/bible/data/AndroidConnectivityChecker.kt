package com.manna.bible.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Android [ConnectivityChecker] backed by [ConnectivityManager].
 *
 * Reports the device as online when the active network advertises the
 * [NetworkCapabilities.NET_CAPABILITY_INTERNET] capability. Lives in the data
 * layer because it depends on Android APIs; the repository stays JVM-testable by
 * depending on the [ConnectivityChecker] interface.
 */
class AndroidConnectivityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectivityChecker {

    override fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
