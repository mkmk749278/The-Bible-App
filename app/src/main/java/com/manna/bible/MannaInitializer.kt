package com.manna.bible

import com.manna.bible.data.bundled.BundledBibleSeeder
import com.manna.bible.domain.audio.AudioForegroundController
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.repository.TranslationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot startup work, invoked from [MannaApplication.onCreate].
 *
 * Runs entirely in the background so it never blocks first frame:
 *  1. Seeds any bundled translations into Room (no-op when no assets ship).
 *  2. Refreshes the translation catalog from the Free Use Bible API when online
 *     (offline this is a no-op and the cached catalog is used).
 *  3. Retries any downloads that were queued while offline.
 *
 * Every step is wrapped so a failure in one (e.g. no connectivity) never prevents
 * the others or crashes startup.
 */
@Singleton
class MannaInitializer @Inject constructor(
    private val seeder: BundledBibleSeeder,
    private val translationRepository: TranslationRepository,
    private val downloadManager: DownloadManager,
    private val audioForegroundController: AudioForegroundController
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Kicks off startup work; safe to call exactly once from Application.onCreate. */
    fun initialize() {
        scope.launch { runCatching { seeder.seed() } }
        scope.launch { runCatching { translationRepository.refreshCatalog() } }
        scope.launch { runCatching { downloadManager.retryPending() } }
        // Watch playback so audio keeps running when the app is backgrounded.
        runCatching { audioForegroundController.start() }
    }
}
