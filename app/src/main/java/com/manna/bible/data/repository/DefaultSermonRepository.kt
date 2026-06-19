package com.manna.bible.data.repository

import com.manna.bible.data.local.SermonDao
import com.manna.bible.data.local.SermonNoteEntity
import com.manna.bible.data.local.toDomain
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.domain.sermon.SermonNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [SermonRepository]. Maps entities to/from the pure-domain [SermonNote]
 * and stamps timestamps with the system clock. A note with nothing in it (no title,
 * reference, or content) is ignored on [save].
 */
@Singleton
class DefaultSermonRepository @Inject constructor(
    private val sermonDao: SermonDao
) : SermonRepository {

    override fun observeSermons(): Flow<List<SermonNote>> =
        sermonDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): SermonNote? = sermonDao.getById(id)?.toDomain()

    override suspend fun save(id: Long, title: String, reference: String, content: String): Long {
        val cleanTitle = title.trim()
        val cleanReference = reference.trim()
        val cleanContent = content.trim()
        if (cleanTitle.isEmpty() && cleanReference.isEmpty() && cleanContent.isEmpty()) return -1

        val now = System.currentTimeMillis()
        // Fall back to the reference (then a default) when the preacher leaves the
        // title blank, so a saved sermon always has something to show in the list.
        val resolvedTitle = cleanTitle.ifEmpty { cleanReference.ifEmpty { DEFAULT_TITLE } }

        return if (id <= 0L) {
            sermonDao.insert(
                SermonNoteEntity(
                    title = resolvedTitle,
                    reference = cleanReference,
                    content = cleanContent,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val existing = sermonDao.getById(id) ?: return -1
            sermonDao.update(
                existing.copy(
                    title = resolvedTitle,
                    reference = cleanReference,
                    content = cleanContent,
                    updatedAt = now
                )
            )
            id
        }
    }

    override suspend fun delete(id: Long) {
        sermonDao.deleteById(id)
    }

    private companion object {
        const val DEFAULT_TITLE = "Untitled sermon"
    }
}
