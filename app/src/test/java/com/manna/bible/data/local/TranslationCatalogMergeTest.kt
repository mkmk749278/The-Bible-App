package com.manna.bible.data.local

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
 * Regression test for the catalog refresh against a real Room database.
 *
 * The remote catalog includes the very editions Manna bundles (ENGWEBP, tam_irv, …).
 * Refreshing it used to upsert them as "not downloaded, 0 verses", which made downloaded
 * Bibles look missing and forced every bundled Bible to be re-installed on the next launch.
 *
 * Hang-safety: explicit [GraphicsMode] NATIVE, matching every other Robolectric test class.
 */
@ExtendWith(RobolectricExtension::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class TranslationCatalogMergeTest {

    private lateinit var database: MannaDatabase
    private lateinit var dao: TranslationDao

    @BeforeEach
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication() as Application,
            MannaDatabase::class.java
        ).build()
        dao = database.translationDao()
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun `refreshing the remote catalog keeps bundled and downloaded editions installed`(): Unit = runBlocking {
        dao.upsertAll(
            listOf(
                row("ENGWEBP", "World English Bible (bundled)").copy(
                    isDownloaded = true, isBundled = true, contentVersion = 1, verseCount = 31103, sizeBytes = 4_000_000
                ),
                row("KJV", "King James").copy(
                    isDownloaded = true, contentVersion = 2, verseCount = 31102, sizeBytes = 4_100_000
                ),
                row("OLD", "Not downloaded")
            )
        )

        // What the network returns: every edition "not downloaded", fresh names.
        dao.mergeRemoteCatalog(
            listOf(
                row("ENGWEBP", "World English Bible"),
                row("KJV", "King James Version"),
                row("OLD", "Renamed upstream"),
                row("NEW", "Brand new edition")
            )
        )

        val bundled = dao.getById("ENGWEBP")!!
        assertTrue(bundled.isDownloaded)
        assertTrue(bundled.isBundled)
        assertEquals(31103, bundled.verseCount)
        assertEquals(1, bundled.contentVersion)
        assertEquals("World English Bible (bundled)", bundled.name, "the bundled manifest is authoritative")

        val downloaded = dao.getById("KJV")!!
        assertTrue(downloaded.isDownloaded, "a downloaded edition must stay downloaded")
        assertEquals(2, downloaded.contentVersion)
        assertEquals(31102, downloaded.verseCount)
        assertEquals(4_100_000, downloaded.sizeBytes)
        assertEquals("King James Version", downloaded.name, "descriptive fields refresh")

        assertEquals("Renamed upstream", dao.getById("OLD")!!.name)
        val added = dao.getById("NEW")
        assertNotNull(added)
        assertFalse(added!!.isDownloaded)
    }

    private fun row(id: String, name: String) = TranslationEntity(
        id = id,
        name = name,
        languageCode = "en",
        canonType = "protestant_66",
        hasDeuterocanon = false,
        isDownloaded = false,
        sizeBytes = 0,
        isDefaultForCanon = false
    )
}
