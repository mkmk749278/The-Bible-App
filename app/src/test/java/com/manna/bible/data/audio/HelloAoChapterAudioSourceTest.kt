package com.manna.bible.data.audio

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.data.remote.RemoteBook
import com.manna.bible.data.remote.RemoteChapter
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HelloAoChapterAudioSource] — online resolution, offline/short-circuit,
 * and error/blank handling.
 */
class HelloAoChapterAudioSourceTest {

    @Test
    @DisplayName("returns the remote audio url when online")
    fun onlineReturnsUrl() = runTest {
        val remote = FakeRemote(url = "https://audio.example/jhn3.mp3")
        val source = HelloAoChapterAudioSource(remote, FakeConnectivity(online = true))

        assertEquals("https://audio.example/jhn3.mp3", source.audioUrl("web", "JHN", 3))
    }

    @Test
    @DisplayName("returns null when offline and does not hit the network")
    fun offlineShortCircuits() = runTest {
        val remote = FakeRemote(url = "https://audio.example/jhn3.mp3")
        val source = HelloAoChapterAudioSource(remote, FakeConnectivity(online = false))

        assertNull(source.audioUrl("web", "JHN", 3))
        assertFalse(remote.audioRequested, "offline must not call the remote source")
    }

    @Test
    @DisplayName("returns null when the remote resolution fails")
    fun remoteErrorIsNull() = runTest {
        val source = HelloAoChapterAudioSource(FakeRemote(throwOnAudio = true), FakeConnectivity(true))
        assertNull(source.audioUrl("web", "JHN", 3))
    }

    @Test
    @DisplayName("treats a blank url as no audio")
    fun blankUrlIsNull() = runTest {
        val source = HelloAoChapterAudioSource(FakeRemote(url = "  "), FakeConnectivity(true))
        assertNull(source.audioUrl("web", "JHN", 3))
    }

    private class FakeConnectivity(private val online: Boolean) : ConnectivityChecker {
        override fun isOnline(): Boolean = online
    }

    private class FakeRemote(
        private val url: String? = null,
        private val throwOnAudio: Boolean = false
    ) : HelloAoRemoteDataSource {
        var audioRequested = false

        override suspend fun chapterAudioUrl(id: String, osisId: String, chapter: Int): String? {
            audioRequested = true
            if (throwOnAudio) throw RuntimeException("boom")
            return url
        }

        override suspend fun books(id: String): List<RemoteBook> = emptyList()
        override suspend fun chapter(id: String, osisId: String, chapter: Int): RemoteChapter =
            RemoteChapter(osisId, chapter, emptyList())

        override suspend fun fetchCatalog(): List<Translation> = emptyList()
        override suspend fun downloadTranslation(id: String): Long = 0L
    }
}
