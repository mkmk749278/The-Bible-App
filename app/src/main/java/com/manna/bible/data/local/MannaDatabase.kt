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
 *  - v5: adds the [ExplanationEntity] `explanations` cache table.
 *  - v6: adds the [SermonNoteEntity] `sermon_notes` table (Sermon Helper, Phase 3).
 *
 * Every upgrade from v2 is additive and preserves all existing data (see [ALL_MIGRATIONS]).
 * Annotations live in Room and are left untouched; preferences live in DataStore (outside
 * Room) and are unaffected.
 *
 * The schema is exported to `app/schemas/` so each version is reviewable in diffs and
 * future migrations can be verified against the real historical schema. **Any schema
 * change must bump [version] and add a migration** — the database is never wiped to make
 * an upgrade "work", because highlights, notes and prayers exist nowhere else.
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
        ExplanationEntity::class,
        SermonNoteEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class MannaDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun pendingDownloadDao(): PendingDownloadDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun bibleContentDao(): BibleContentDao
    abstract fun prayerDao(): PrayerDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun sermonDao(): SermonDao
}
