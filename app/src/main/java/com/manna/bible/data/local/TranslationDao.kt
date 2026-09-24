package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `translations` catalog table.
 */
@Dao
interface TranslationDao {

    /** Observes the full translation catalog, emitting on every change. */
    @Query("SELECT * FROM translations")
    fun observeAll(): Flow<List<TranslationEntity>>

    /** Inserts or updates the given catalog rows (replacing every column). */
    @Upsert
    suspend fun upsertAll(translations: List<TranslationEntity>)

    /** Returns the whole catalog as a one-shot snapshot. */
    @Query("SELECT * FROM translations")
    suspend fun getAll(): List<TranslationEntity>

    /**
     * Merges a freshly fetched remote catalog without losing what is on the device.
     *
     * New editions are inserted as-is (not downloaded). For editions already known, only
     * the descriptive fields from the network are refreshed; the on-device state — whether
     * it is downloaded, its size, content version and verse count, bundled and default
     * flags — is kept. Bundled editions are left untouched entirely: the in-app manifest is
     * their source of truth.
     *
     * A plain [upsertAll] of the remote rows would reset every known edition to "not
     * downloaded, 0 verses" — which is what used to happen on every online launch, making
     * the downloaded Bibles look missing and forcing all five bundled Bibles to be
     * re-installed on the next start.
     */
    @Transaction
    suspend fun mergeRemoteCatalog(remote: List<TranslationEntity>) {
        val local = getAll().associateBy { it.id }
        val merged = remote.mapNotNull { incoming ->
            val existing = local[incoming.id] ?: return@mapNotNull incoming
            if (existing.isBundled) {
                null
            } else {
                incoming.copy(
                    isDownloaded = existing.isDownloaded,
                    sizeBytes = existing.sizeBytes,
                    isDefaultForCanon = existing.isDefaultForCanon,
                    isBundled = existing.isBundled,
                    contentVersion = existing.contentVersion,
                    verseCount = existing.verseCount
                )
            }
        }
        if (merged.isNotEmpty()) upsertAll(merged)
    }

    /** Marks a translation as downloaded and records its on-disk size. */
    @Query("UPDATE translations SET isDownloaded = 1, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun setDownloaded(id: String, sizeBytes: Long)

    /**
     * Marks a translation as downloaded and records its size, content version, and
     * verse count in a single update.
     *
     * Called by the `DownloadManager` only once every chapter is committed, so a
     * translation is never observable as downloaded with partial content
     * (Req 5.3, 15.4).
     */
    @Query(
        "UPDATE translations SET isDownloaded = 1, sizeBytes = :sizeBytes, " +
            "contentVersion = :contentVersion, verseCount = :verseCount WHERE id = :id"
    )
    suspend fun setDownloadedContent(
        id: String,
        sizeBytes: Long,
        contentVersion: Int,
        verseCount: Int
    )

    /**
     * Clears a translation's downloaded marker and content metadata after its stored
     * content has been removed (Req 5.5). Leaves the catalog row itself in place so
     * the translation can be re-downloaded later.
     */
    @Query(
        "UPDATE translations SET isDownloaded = 0, contentVersion = 0, verseCount = 0, " +
            "sizeBytes = 0 WHERE id = :id"
    )
    suspend fun clearDownloaded(id: String)

    /** Returns a single translation by id, or null if absent. */
    @Query("SELECT * FROM translations WHERE id = :id")
    suspend fun getById(id: String): TranslationEntity?
}
