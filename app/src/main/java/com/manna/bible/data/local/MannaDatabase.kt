package com.manna.bible.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Manna Room database.
 *
 * Holds the translation catalog, the offline pending-download queue, and the user
 * annotation tables (highlights, bookmarks, notes).
 *
 * Version 2 adds the annotation tables. Since no schema has shipped yet, the Hilt
 * builder (task 11.1) may use `fallbackToDestructiveMigration()` for now rather
 * than supplying a migration. The Hilt provisioning module is intentionally
 * deferred; this class only declares the schema and exposes DAOs.
 */
@Database(
    entities = [
        TranslationEntity::class,
        PendingDownloadEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        NoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MannaDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun pendingDownloadDao(): PendingDownloadDao
    abstract fun annotationDao(): AnnotationDao
}
