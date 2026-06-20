package com.manna.bible.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.manna.bible.domain.download.DownloadForegroundController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts [DownloadForegroundService]. Downloads are user-initiated while the app is in
 * the foreground, so the foreground-service start is permitted; if it is ever refused
 * (a background start, e.g. from startup retry), the failure is swallowed and the
 * download continues on the app scope without a notification.
 */
@Singleton
class AndroidDownloadForegroundController @Inject constructor(
    @ApplicationContext private val context: Context
) : DownloadForegroundController {

    override fun ensureRunning() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadForegroundService::class.java)
            )
        }
    }
}
