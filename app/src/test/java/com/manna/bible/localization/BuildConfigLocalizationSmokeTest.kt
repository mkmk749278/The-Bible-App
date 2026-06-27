package com.manna.bible.localization

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Build-config smoke test (Requirements 2.5, 3.4): the bundle config must keep all locale
 * resources in every artifact (`enableSplit = false`) and the `MissingTranslation` lint
 * must stay disabled so incremental localization batches don't fail the build.
 */
class BuildConfigLocalizationSmokeTest {

    private val buildGradle: String by lazy {
        File(LocalizationTestSupport.moduleDir, "build.gradle.kts").readText()
    }

    @Test
    fun `language split is disabled so all locale resources ship to every device`() {
        val normalized = buildGradle.replace(Regex("""\s+"""), " ")
        assertTrue(
            normalized.contains("language { enableSplit = false }") ||
                normalized.contains("enableSplit = false"),
            "expected bundle { language { enableSplit = false } } to remain in build.gradle.kts"
        )
    }

    @Test
    fun `MissingTranslation lint check remains disabled for incremental localization`() {
        assertTrue(
            buildGradle.contains("\"MissingTranslation\""),
            "expected lint { disable += \"MissingTranslation\" } to remain in build.gradle.kts"
        )
    }
}
