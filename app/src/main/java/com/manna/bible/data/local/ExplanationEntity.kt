package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached passage explanation, in the `explanations` table. The [cacheKey] combines
 * the passage reference, the explanation language, and the depth, so each distinct
 * request is stored once and then served offline forever.
 */
@Entity(tableName = "explanations")
data class ExplanationEntity(
    @PrimaryKey val cacheKey: String,
    val text: String,
    val createdAt: Long
)
