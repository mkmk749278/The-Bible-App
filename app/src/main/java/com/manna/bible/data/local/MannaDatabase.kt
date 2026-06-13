package com.manna.bible.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Manna Room database.
 *
 * Holds the translation catalog, the offline pending-download queue, the user
 * annotation tables (highlights, bookmarks, notes), and the offline Bible content
 * tables (books, chapters, verses) plus the `verses_fts` full-text mirror.
 *
 * Version history:
 *  - v2: adds the annotation tables.
 *  - v3: adds offline Bible content ([BookEntity], [ChapterEntity], [VerseEntity],
 *        [VerseFtsEntity]) and the additive content-tracking columns on
 *        [TranslationEntity] (`isBundled`, `contentVersion`, `verseCount`).
 *  - v4: adds the [PrayerEntryEntity] `prayers` table (prayer journal, Phase 2).
 *
 * The v2 -> v3 upgrade is fully additive and preserves all existing data: see
 * [MIGRATION_2_3]. Annotations live in Room and are left untouched; preferences
 * live in DataStore (outside Room) and are unaffected. The Hilt builder
 * (task 14.1) wires this migration via `.addMigrations(MIGRATION_2_3)`.
 */
@Database(
    entities = [
        TranslationEntity::class,
        PendingDownloadEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        BookEntity::class,
        ChapterEntity::class,
        VerseEntity::class,
        VerseFtsEntity::class,
        PrayerEntryEntity::class,
        ExplanationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MannaDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun pendingDownloadDao(): PendingDownloadDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun bibleContentDao(): BibleContentDao
    abstract fun prayerDao(): PrayerDao
    abstract fun explanationDao(): ExplanationDao
}
