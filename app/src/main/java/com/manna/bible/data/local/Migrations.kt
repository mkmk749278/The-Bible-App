package com.manna.bible.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive migration from schema v2 to v3 for the Manna database.
 *
 * v3 introduces the offline Bible content tables ([BookEntity], [ChapterEntity],
 * [VerseEntity]) plus the `verses_fts` full-text mirror, and three additive
 * columns on `translations` (`isBundled`, `contentVersion`, `verseCount`).
 *
 * The migration is strictly additive — it only `ALTER`s in new columns with safe
 * defaults and `CREATE`s new tables/indices. No existing table is dropped or
 * rewritten, so all user annotations (highlights, bookmarks, notes) are preserved
 * verbatim. Preferences are stored in DataStore (outside Room) and are unaffected.
 *
 * The DDL below mirrors exactly what Room generates for the corresponding entities
 * (column order, types, `NOT NULL`, composite primary keys, index names, and the
 * external-content FTS4 statement) so Room's runtime schema validation succeeds.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- additive columns on translations (Task 1.2) ----------------------
        db.execSQL("ALTER TABLE `translations` ADD COLUMN `isBundled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `translations` ADD COLUMN `contentVersion` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `translations` ADD COLUMN `verseCount` INTEGER NOT NULL DEFAULT 0")

        // --- books ------------------------------------------------------------
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `books` (" +
                "`translationId` TEXT NOT NULL, " +
                "`osisId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`testament` TEXT NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "`chapterCount` INTEGER NOT NULL, " +
                "PRIMARY KEY(`translationId`, `osisId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_books_translationId` ON `books` (`translationId`)"
        )

        // --- chapters ---------------------------------------------------------
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chapters` (" +
                "`translationId` TEXT NOT NULL, " +
                "`osisId` TEXT NOT NULL, " +
                "`chapter` INTEGER NOT NULL, " +
                "`verseCount` INTEGER NOT NULL, " +
                "PRIMARY KEY(`translationId`, `osisId`, `chapter`))"
        )

        // --- verses -----------------------------------------------------------
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `verses` (" +
                "`translationId` TEXT NOT NULL, " +
                "`osisId` TEXT NOT NULL, " +
                "`chapter` INTEGER NOT NULL, " +
                "`verse` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "PRIMARY KEY(`translationId`, `osisId`, `chapter`, `verse`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_verses_translationId_osisId_chapter` " +
                "ON `verses` (`translationId`, `osisId`, `chapter`)"
        )

        // --- verses_fts (external-content FTS4 mirror of verses.text) ---------
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `verses_fts` USING FTS4(" +
                "`text` TEXT NOT NULL, content=`verses`)"
        )
    }
}

/**
 * Additive migration from schema v3 to v4: adds the `prayers` table (prayer journal
 * / Faith Timeline, Phase 2).
 *
 * Strictly additive — it only `CREATE`s the new table, so all existing content and
 * user annotations are preserved verbatim. The DDL mirrors exactly what Room
 * generates for [PrayerEntryEntity] (an autoGenerate `Long` primary key, nullable
 * `answeredAt`) so Room's runtime schema validation succeeds.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `prayers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`content` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`answeredAt` INTEGER)"
        )
    }
}

/**
 * v4 -> v5: adds the [ExplanationEntity] `explanations` cache table for the "Explain
 * this passage" feature. Purely additive — existing data is untouched.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `explanations` (" +
                "`cacheKey` TEXT PRIMARY KEY NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

/**
 * v5 -> v6: adds the [SermonNoteEntity] `sermon_notes` table (Village Pastor Sermon
 * Helper, Phase 3). Purely additive — existing data is untouched. The DDL mirrors
 * exactly what Room generates for the entity (an autoGenerate `Long` primary key and
 * the non-null text/timestamp columns) so Room's runtime schema validation succeeds.
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sermon_notes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`reference` TEXT NOT NULL, " +
                "`content` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
    }
}
