package com.manna.bible.data.repository

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultAnnotationRepository].
 *
 * Runs on the JVM (JUnit 5) without an emulator, backed by an in-memory fake
 * [AnnotationLocalDataSource]. Exercises book-scoped queries and the "hide, never
 * delete" visibility rule across a mix of canonical and deuterocanonical books.
 *
 * Validates: Requirements 11, 12
 */
class DefaultAnnotationRepositoryTest {

    /**
     * In-memory [AnnotationLocalDataSource] holding annotations across several
     * books. Mirrors the contract of the Room-backed source: it retains every row
     * regardless of the active canon.
     */
    private class FakeAnnotationLocalDataSource(
        highlights: List<Highlight> = emptyList(),
        bookmarks: List<Bookmark> = emptyList(),
        notes: List<Note> = emptyList()
    ) : AnnotationLocalDataSource {

        val highlights = MutableStateFlow(highlights)
        val bookmarks = MutableStateFlow(bookmarks)
        val notes = MutableStateFlow(notes)

        override fun observeHighlights(): Flow<List<Highlight>> = highlights
        override fun observeBookmarks(): Flow<List<Bookmark>> = bookmarks
        override fun observeNotes(): Flow<List<Note>> = notes

        override suspend fun insertHighlight(highlight: Highlight): Long {
            highlights.value = highlights.value + highlight
            return highlight.id
        }

        override suspend fun insertBookmark(bookmark: Bookmark): Long {
            bookmarks.value = bookmarks.value + bookmark
            return bookmark.id
        }

        override suspend fun insertNote(note: Note): Long {
            notes.value = notes.value + note
            return note.id
        }

        override suspend fun deleteHighlight(id: Long) {
            highlights.value = highlights.value.filterNot { it.id == id }
        }

        override suspend fun deleteBookmark(id: Long) {
            bookmarks.value = bookmarks.value.filterNot { it.id == id }
        }

        override suspend fun deleteNote(id: Long) {
            notes.value = notes.value.filterNot { it.id == id }
        }

        override suspend fun allVerseRefs(): List<String> =
            highlights.value.map { it.verseRef } +
                bookmarks.value.map { it.verseRef } +
                notes.value.mapNotNull { it.verseRef ?: it.chapterRef }
    }

    // Annotations spanning Protestant (GEN, PSA, JHN) and deuterocanonical (TOB) books.
    private fun populatedSource() = FakeAnnotationLocalDataSource(
        highlights = listOf(
            Highlight(id = 1, verseRef = "GEN.1.1", colorArgb = 0xFFFF0000.toInt(), createdAt = 1),
            Highlight(id = 2, verseRef = "PSA.23.1", colorArgb = 0xFF00FF00.toInt(), createdAt = 2)
        ),
        bookmarks = listOf(
            Bookmark(id = 1, verseRef = "JHN.3.16", label = "salvation", createdAt = 3)
        ),
        notes = listOf(
            Note(id = 1, verseRef = "TOB.3.2", chapterRef = null, content = "Tobit prayer", createdAt = 4, updatedAt = 4),
            Note(id = 2, verseRef = null, chapterRef = "PSA.23", content = "chapter note", createdAt = 5, updatedAt = 5)
        )
    )

    // --- allAnnotatedBookIds (Req 12) ----------------------------------------

    @Test
    fun `allAnnotatedBookIds returns the distinct set across all annotation types`() = runTest {
        val repo = DefaultAnnotationRepository(populatedSource())

        assertEquals(setOf("GEN", "PSA", "JHN", "TOB"), repo.allAnnotatedBookIds())
    }

    @Test
    fun `allAnnotatedBookIds is empty when no annotations exist`() = runTest {
        val repo = DefaultAnnotationRepository(FakeAnnotationLocalDataSource())

        assertTrue(repo.allAnnotatedBookIds().isEmpty())
    }

    // --- annotatedBookIdsOutside (Req 12.1) ----------------------------------

    @Test
    fun `annotatedBookIdsOutside returns annotated books excluded by a protestant canon`() = runTest {
        val repo = DefaultAnnotationRepository(populatedSource())

        // A Protestant canon excludes the deuterocanonical Tobit (TOB).
        val visible = setOf("GEN", "PSA", "JHN")

        assertEquals(setOf("TOB"), repo.annotatedBookIdsOutside(visible))
    }

    @Test
    fun `annotatedBookIdsOutside is empty when the canon covers every annotated book`() = runTest {
        val repo = DefaultAnnotationRepository(populatedSource())

        val visible = setOf("GEN", "PSA", "JHN", "TOB", "REV")

        assertTrue(repo.annotatedBookIdsOutside(visible).isEmpty())
    }

    // --- visible* filtering hides but never deletes (Req 12.4) ---------------

    @Test
    fun `visibleHighlights hides highlights outside the canon and keeps the rest`() = runTest {
        val source = populatedSource()
        val repo = DefaultAnnotationRepository(source)

        // Canon excludes PSA — its highlight must be hidden.
        val visible = repo.visibleHighlights(setOf("GEN", "JHN")).first()

        assertEquals(listOf("GEN.1.1"), visible.map { it.verseRef })
        // Nothing deleted: the source still holds both highlights.
        assertEquals(2, source.highlights.value.size)
    }

    @Test
    fun `visibleBookmarks hides bookmarks outside the canon and keeps the rest`() = runTest {
        val source = populatedSource()
        val repo = DefaultAnnotationRepository(source)

        // Canon excludes JHN — its bookmark must be hidden.
        val visible = repo.visibleBookmarks(setOf("GEN", "PSA")).first()

        assertTrue(visible.isEmpty())
        assertEquals(1, source.bookmarks.value.size)
    }

    @Test
    fun `visibleNotes hides deuterocanonical notes when the canon excludes them`() = runTest {
        val source = populatedSource()
        val repo = DefaultAnnotationRepository(source)

        // Protestant canon (no TOB) keeps the PSA chapter note, hides the TOB note.
        val visible = repo.visibleNotes(setOf("GEN", "PSA", "JHN")).first()

        assertEquals(listOf<Long>(2), visible.map { it.id })
        // Nothing deleted: the source still holds both notes.
        assertEquals(2, source.notes.value.size)
    }

    @Test
    fun `visibleNotes shows deuterocanonical notes again when the canon includes them`() = runTest {
        val source = populatedSource()
        val repo = DefaultAnnotationRepository(source)

        // Catholic canon includes TOB — the previously hidden note reappears.
        val visible = repo.visibleNotes(setOf("GEN", "PSA", "JHN", "TOB")).first()

        assertEquals(setOf<Long>(1, 2), visible.map { it.id }.toSet())
    }

    @Test
    fun `switching to a narrower canon hides annotations without deleting any rows`() = runTest {
        val source = populatedSource()
        val repo = DefaultAnnotationRepository(source)

        // Narrow canon: only Genesis visible.
        val highlights = repo.visibleHighlights(setOf("GEN")).first()
        val notes = repo.visibleNotes(setOf("GEN")).first()

        assertEquals(listOf("GEN.1.1"), highlights.map { it.verseRef })
        assertTrue(notes.isEmpty())

        // Underlying storage is untouched (Req 12.2): all rows survive.
        assertEquals(2, source.highlights.value.size)
        assertEquals(1, source.bookmarks.value.size)
        assertEquals(2, source.notes.value.size)
        assertEquals(setOf("GEN", "PSA", "JHN", "TOB"), repo.allAnnotatedBookIds())
    }
}
