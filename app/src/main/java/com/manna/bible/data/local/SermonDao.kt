package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Room DAO for the `sermon_notes` table (Village Pastor Sermon Helper). */
@Dao
interface SermonDao {

    /** Observes all sermon notes, most recently updated first. */
    @Query("SELECT * FROM sermon_notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SermonNoteEntity>>

    /** Returns a single sermon note by id, or null. */
    @Query("SELECT * FROM sermon_notes WHERE id = :id")
    suspend fun getById(id: Long): SermonNoteEntity?

    /** Inserts a sermon note, returning its generated row id. */
    @Insert
    suspend fun insert(entity: SermonNoteEntity): Long

    /** Updates an existing sermon note. */
    @Update
    suspend fun update(entity: SermonNoteEntity)

    /** Deletes the sermon note with [id]. */
    @Query("DELETE FROM sermon_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
