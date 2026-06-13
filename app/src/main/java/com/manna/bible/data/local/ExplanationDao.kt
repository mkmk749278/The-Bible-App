package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Room DAO for the `explanations` cache table. */
@Dao
interface ExplanationDao {

    /** Returns the cached explanation for [cacheKey], or null. */
    @Query("SELECT * FROM explanations WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): ExplanationEntity?

    /** Stores an explanation, replacing any existing one for the same key. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExplanationEntity)
}
