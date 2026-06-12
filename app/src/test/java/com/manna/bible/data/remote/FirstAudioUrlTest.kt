package com.manna.bible.data.remote

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [firstAudioUrl] — the tolerant extractor for the helloao
 * `thisChapterAudioLinks` value.
 */
class FirstAudioUrlTest {

    @Test
    @DisplayName("picks the first url from a reader -> url object")
    fun fromObject() {
        val links = buildJsonObject {
            put("Gilbert", "https://audio.example/gen1-gilbert.mp3")
            put("Hays", "https://audio.example/gen1-hays.mp3")
        }
        assertEquals("https://audio.example/gen1-gilbert.mp3", firstAudioUrl(links))
    }

    @Test
    @DisplayName("picks the first url from an array of urls")
    fun fromArrayOfStrings() {
        val links = buildJsonArray {
            add(JsonPrimitive("https://audio.example/a.mp3"))
            add(JsonPrimitive("https://audio.example/b.mp3"))
        }
        assertEquals("https://audio.example/a.mp3", firstAudioUrl(links))
    }

    @Test
    @DisplayName("picks a url from an array of objects")
    fun fromArrayOfObjects() {
        val links = buildJsonArray {
            add(buildJsonObject { put("url", "https://audio.example/c.mp3") })
        }
        assertEquals("https://audio.example/c.mp3", firstAudioUrl(links))
    }

    @Test
    @DisplayName("returns null for missing / empty / non-string payloads")
    fun nullCases() {
        assertNull(firstAudioUrl(null))
        assertNull(firstAudioUrl(JsonNull))
        assertNull(firstAudioUrl(buildJsonObject { }))
        assertNull(firstAudioUrl(buildJsonObject { put("count", 3) }))
        assertNull(firstAudioUrl(buildJsonObject { put("reader", "   ") }))
    }
}
