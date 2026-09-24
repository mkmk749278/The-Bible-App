package com.manna.bible.data.local

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.manna.bible.di.AppModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.util.concurrent.TimeUnit

/**
 * Verifies that a database written by an older app version is upgraded to the current
 * schema **with every user annotation intact**, using the production database builder
 * ([AppModule.provideMannaDatabase]) and the real [ALL_MIGRATIONS].
 *
 * Room validates every table against the entity definitions after migrating, so this also
 * proves each migration's hand-written DDL matches what Room expects — a mismatch would
 * crash the app on upgrade. Now that the builder no longer falls back to a destructive
 * rebuild, this test is the guard that keeps upgrades both safe and non-destructive.
 *
 * Hang-safety: explicit [GraphicsMode] NATIVE, matching every other Robolectric test class.
 */
@ExtendWith(RobolectricExtension::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class MigrationsTest {

    private val context: Application
        get() = RuntimeEnvironment.getApplication()

    @BeforeEach
    fun setUp() {
        context.deleteDatabase(DB_NAME)
    }

    @AfterEach
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun `a v2 database upgrades to the current schema and keeps every annotation`(): Unit = runBlocking {
        createVersion2Database()

        val database = AppModule.provideMannaDatabase(context)
        try {
            // Opening runs 2 -> 3 -> 4 -> 5 -> 6 and Room's post-migration schema validation.
            database.openHelper.writableDatabase

            val annotations = database.annotationDao()
            assertEquals(listOf("JHN.3.16"), annotations.highlightVerseRefs())
            assertEquals(listOf("PSA.23.1"), annotations.bookmarkVerseRefs())
            assertEquals(listOf("ROM.8.28"), annotations.noteRefs())
            val note = annotations.observeNotes().first().single()
            assertEquals("He works all things for good", note.content)

            // The pre-existing catalog row survives with the v3 columns defaulted.
            val translation = database.translationDao().getById("ENGWEBP")
            assertNotNull(translation)
            assertEquals(false, translation!!.isBundled)
            assertEquals(0, translation.contentVersion)
            assertEquals(0, translation.verseCount)

            // Tables added by later versions are usable, including the FTS mirror.
            val content = database.bibleContentDao()
            content.insertContent(
                books = listOf(BookEntity("ENGWEBP", "JHN", "John", "NEW", 42, 21)),
                chapters = listOf(ChapterEntity("ENGWEBP", "JHN", 3, 1)),
                verses = listOf(
                    VerseEntity("ENGWEBP", "JHN", 3, 16, "For God so loved the world")
                )
            )
            val hits = content.search("ENGWEBP", "\"loved\"*", 10)
            assertEquals(listOf(16), hits.map { it.verse })

            assertTrue(
                database.prayerDao()
                    .insert(PrayerEntryEntity(content = "p", status = "ACTIVE", createdAt = 1, answeredAt = null)) > 0
            )
            assertTrue(
                database.sermonDao()
                    .insert(SermonNoteEntity(title = "t", reference = "r", content = "c", createdAt = 1, updatedAt = 1)) > 0
            )
            database.explanationDao().insert(ExplanationEntity("k", "text", 1))
            assertEquals("text", database.explanationDao().get("k")?.text)
        } finally {
            database.close()
        }
    }

    /**
     * Writes a database exactly as Room created schema v2: the catalog, the offline
     * download queue and the three annotation tables, plus one row of user data in each
     * annotation table.
     */
    private fun createVersion2Database() {
        val file = context.getDatabasePath(DB_NAME)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `translations` (`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `languageCode` TEXT NOT NULL, " +
                    "`canonType` TEXT NOT NULL, `hasDeuterocanon` INTEGER NOT NULL, " +
                    "`isDownloaded` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, " +
                    "`isDefaultForCanon` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_downloads` (`translationId` TEXT NOT NULL, " +
                    "`requestedAt` INTEGER NOT NULL, PRIMARY KEY(`translationId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `highlights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`verseRef` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`verseRef` TEXT NOT NULL, `label` TEXT, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`verseRef` TEXT, `chapterRef` TEXT, `content` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'v2')")

            db.execSQL(
                "INSERT INTO translations VALUES ('ENGWEBP', 'World English Bible', 'en', " +
                    "'protestant_66', 0, 1, 1000, 1)"
            )
            db.execSQL("INSERT INTO highlights (verseRef, colorArgb, createdAt) VALUES ('JHN.3.16', -1, 1)")
            db.execSQL("INSERT INTO bookmarks (verseRef, label, createdAt) VALUES ('PSA.23.1', NULL, 2)")
            db.execSQL(
                "INSERT INTO notes (verseRef, chapterRef, content, createdAt, updatedAt) " +
                    "VALUES ('ROM.8.28', NULL, 'He works all things for good', 3, 3)"
            )
            db.version = 2
        }
    }

    private companion object {
        /** Must match the production file name so [AppModule.provideMannaDatabase] opens it. */
        const val DB_NAME = "manna.db"
    }
}
