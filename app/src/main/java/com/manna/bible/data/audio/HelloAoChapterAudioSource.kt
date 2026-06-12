package com.manna.bible.data.audio

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.domain.audio.ChapterAudioSource
import javax.inject.Inject

/**
 * [ChapterAudioSource] backed by the Free Use Bible API (helloao), the same provider
 * already used for text — so narrated audio needs no extra API key.
 *
 * Audio is streamed, so it requires connectivity: offline (or any resolution error)
 * yields null and the reader falls back to on-device TTS. Returns null for blank URLs.
 */
class HelloAoChapterAudioSource @Inject constructor(
    private val remote: HelloAoRemoteDataSource,
    private val connectivity: ConnectivityChecker
) : ChapterAudioSource {

    override suspend fun audioUrl(translationId: String, osisId: String, chapter: Int): String? {
        if (!connectivity.isOnline()) return null
        return runCatching { remote.chapterAudioUrl(translationId, osisId, chapter) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
