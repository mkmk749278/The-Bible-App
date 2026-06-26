package com.manna.bible.data.liturgy

import android.app.Application
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

/**
 * Robolectric-backed integration test for the real bundled liturgy assets, read through the
 * production [AndroidLiturgyAssetSource] + [LiturgyAssetReader] via the Android `AssetManager`.
 *
 * Doubles as the JUnit 5 + Robolectric integration check (task 1.1): it proves the
 * `tech.apter` extension runs under `useJUnitPlatform()` and that
 * `testOptions.unitTests.isIncludeAndroidResources = true` makes the bundled
 * `assets/liturgy` JSON files visible to JVM tests.
 */
@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34])
class LiturgyAssetReaderRobolectricTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun reader(): LiturgyAssetReader {
        val context = RuntimeEnvironment.getApplication() as Application
        return LiturgyAssetReader(AndroidLiturgyAssetSource(context), json)
    }

    @Test
    fun `reads the bundled manifest and both orders through the Android AssetManager`() = runBlocking {
        val reader = reader()

        val manifest = reader.manifest()
        assertNotNull(manifest)
        assertEquals(
            setOf("roman_catholic_mass", "csi_holy_communion"),
            manifest!!.liturgies.map { it.id }.toSet()
        )

        val results = reader.readAll()
        assertEquals(2, results.size)
        assertTrue(results.all { it is LiturgyParseResult.Success }, "both bundled orders parse")

        val liturgies = LiturgyRepository(reader).liturgies()
        assertEquals(setOf("roman_catholic_mass", "csi_holy_communion"), liturgies.map { it.id }.toSet())
    }
}
