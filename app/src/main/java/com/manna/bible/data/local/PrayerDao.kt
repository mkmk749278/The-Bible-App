package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Room DAO for the `prayers` table (prayer journal / Faith Timeline). */
@Dao
interface PrayerDao {

    /** Observes all prayers, newest first. */
    @Query("SELECT * FROM prayers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrayerEntryEntity>>

    /** Returns a single prayer by id, or null. */
    @Query("SELECT * FROM prayers WHERE id = :id")
    suspend fun getById(id: Long): PrayerEntryEntity?

    /** Inserts a prayer, returning its generated row id. */
    @Insert
    suspend fun insert(entity: PrayerEntryEntity): Long

    /** Updates an existing prayer (status / answered timestamp). */
    @Update
    suspend fun update(entity: PrayerEntryEntity)

    /** Deletes the prayer with [id]. */
    @Query("DELETE FROM prayers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
