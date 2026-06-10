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
