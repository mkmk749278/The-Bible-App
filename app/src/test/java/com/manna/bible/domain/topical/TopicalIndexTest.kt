package com.manna.bible.domain.topical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultTopicalIndex] — the curated, offline topical search index.
 */
class TopicalIndexTest {

    private val index = DefaultTopicalIndex()

    @Test
    @DisplayName("topics() is non-empty and curates at least 20 topics")
    fun topicsAreCurated() {
        val topics = index.topics()
        assertFalse(topics.isEmpty(), "topics() must not be empty")
        assertTrue(topics.size >= 20, "expected at least 20 topics, was ${topics.size}")
    }

    @Test
    @DisplayName("all topic ids are unique")
    fun topicIdsAreUnique() {
        val ids = index.topics().map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate topic ids found")
    }

    @Test
    @DisplayName("every topic has a non-blank label")
    fun topicLabelsAreNonBlank() {
        for (topic in index.topics()) {
            assertTrue(topic.label.isNotBlank(), "blank label for topic '${topic.id}'")
        }
    }

    @Test
    @DisplayName("every topic resolves to a non-empty list of structurally valid verses")
    fun versesForEveryTopicAreValid() {
        for (topic in index.topics()) {
            val verses = index.versesFor(topic.id)
            assertFalse(verses.isEmpty(), "no verses for topic '${topic.id}'")
            for (ref in verses) {
                assertTrue(ref.osisId.isNotBlank(), "blank book id in topic '${topic.id}'")
                assertTrue(ref.chapter >= 1, "non-positive chapter in topic '${topic.id}'")
                assertTrue(ref.verse >= 1, "non-positive verse in topic '${topic.id}'")
            }
        }
    }

    @Test
    @DisplayName("versesFor() returns an empty list for an unknown topic id")
    fun unknownTopicReturnsEmpty() {
        assertEquals(emptyList<Any>(), index.versesFor("nonexistent"))
    }
}
