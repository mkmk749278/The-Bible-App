package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `pending_downloads` table.
 */
@Dao
interface PendingDownloadDao {

    /** Records a pending download; re-requesting the same translation replaces the row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingDownloadEntity)

    /** Removes a pending download by translation id (e.g. after it completes). */
    @Query("DELETE FROM pending_downloads WHERE translationId = :id")
    suspend fun deleteById(id: String)

    /** Observes the set of pending downloads, emitting on every change. */
    @Query("SELECT * FROM pending_downloads")
    fun observeAll(): Flow<List<PendingDownloadEntity>>

    /** Returns the current pending downloads as a one-shot snapshot. */
    @Query("SELECT * FROM pending_downloads")
    suspend fun getAll(): List<PendingDownloadEntity>
}
