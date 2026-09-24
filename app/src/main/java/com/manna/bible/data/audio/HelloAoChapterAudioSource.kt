package com.manna.bible.data.audio

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.domain.audio.ChapterAudioSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * [ChapterAudioSource] backed by the Free Use Bible API (helloao), the same provider
 * already used for text — so narrated audio needs no extra API key.
 *
 * Audio is streamed, so it requires connectivity: offline (or any resolution error)
 * yields null and the reader falls back to on-device TTS. Returns null for blank URLs.
 *
 * The lookup is bounded by [lookupTimeoutMs]: it runs every time play is pressed, and on
 * the slow rural connections Manna targets an unbounded request (30s OkHttp timeouts)
 * would leave the listener in silence long before on-device reading could have started.
 */
class HelloAoChapterAudioSource(
    private val remote: HelloAoRemoteDataSource,
    private val connectivity: ConnectivityChecker,
    private val lookupTimeoutMs: Long
) : ChapterAudioSource {

    @Inject
    constructor(
        remote: HelloAoRemoteDataSource,
        connectivity: ConnectivityChecker
    ) : this(remote, connectivity, DEFAULT_LOOKUP_TIMEOUT_MS)

    override suspend fun audioUrl(translationId: String, osisId: String, chapter: Int): String? {
        if (!connectivity.isOnline()) return null
        val url = try {
            withTimeoutOrNull(lookupTimeoutMs) {
                remote.chapterAudioUrl(translationId, osisId, chapter)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return url?.takeIf { it.isNotBlank() }
    }

    companion object {
        /** Long enough for a normal 3G lookup, short enough that TTS still starts promptly. */
        const val DEFAULT_LOOKUP_TIMEOUT_MS = 4_000L
    }
}
