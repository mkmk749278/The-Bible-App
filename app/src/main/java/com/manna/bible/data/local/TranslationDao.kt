package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Query
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

    /** Inserts or updates the given catalog rows. */
    @Upsert
    suspend fun upsertAll(translations: List<TranslationEntity>)

    /** Marks a translation as downloaded and records its on-disk size. */
    @Query("UPDATE translations SET isDownloaded = 1, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun setDownloaded(id: String, sizeBytes: Long)

    /** Returns a single translation by id, or null if absent. */
    @Query("SELECT * FROM translations WHERE id = :id")
    suspend fun getById(id: String): TranslationEntity?
}
