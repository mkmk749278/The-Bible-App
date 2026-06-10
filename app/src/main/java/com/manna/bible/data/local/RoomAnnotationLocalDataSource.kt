package com.manna.bible.data.local

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [AnnotationLocalDataSource] that maps between the annotation entities
 * and their pure-domain models.
 *
 * Exposes the full stored annotation set; visibility filtering by the active
 * canon's books happens in the repository so nothing is ever deleted (Req 12).
 */
class RoomAnnotationLocalDataSource @Inject constructor(
    private val dao: AnnotationDao
) : AnnotationLocalDataSource {

    override fun observeHighlights(): Flow<List<Highlight>> =
        dao.observeHighlights().map { rows -> rows.map { it.toDomain() } }

    override fun observeBookmarks(): Flow<List<Bookmark>> =
        dao.observeBookmarks().map { rows -> rows.map { it.toDomain() } }

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeNotes().map { rows -> rows.map { it.toDomain() } }

    override suspend fun insertHighlight(highlight: Highlight): Long =
        dao.insertHighlight(highlight.toEntity())

    override suspend fun insertBookmark(bookmark: Bookmark): Long =
        dao.insertBookmark(bookmark.toEntity())

    override suspend fun insertNote(note: Note): Long =
        dao.insertNote(note.toEntity())

    override suspend fun allVerseRefs(): List<String> =
        dao.highlightVerseRefs() +
            dao.bookmarkVerseRefs() +
            dao.noteRefs().filterNotNull()
}
