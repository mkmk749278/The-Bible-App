package com.manna.bible.localization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Localization inventory / gap + placeholder check (Requirements 1.3, 3.1, 3.2, 3.3,
 * 4.1, 4.2, 4.3, 4.4, 4.5). A data-driven JUnit 5 test that runs over the real
 * `app/src/main/res` resources and the committed provenance manifest.
 *
 * Scope: Concern 1 prioritizes the PR #80 AI feature strings, the liturgy framing
 * strings, and the Oral-AI/Cultural-Lens `explain_*` strings (Assumption 5). These
 * "in-scope" keys are the localization targets this concern guarantees; the remaining
 * English-only app chrome is the acknowledged BASE-08 baseline and is out of scope for
 * the gap *failure* (it is still surfaced by the per-key presence inventory below).
 */
class LocalizationInventoryTest {

    private val targetLocales = listOf("ta", "te", "hi", "ml")

    private val defaultStrings = LocalizationTestSupport.readStrings("values")
    private val defaultKeys = defaultStrings.keys

    /** Keys defined in the three AI-feature resource files (the glossary "AI Feature Strings"). */
    private val aiFileKeys: Set<String> = buildSet {
        addAll(LocalizationTestSupport.readStringsFromFile(aiFile("strings_crisis.xml")).keys)
        addAll(LocalizationTestSupport.readStringsFromFile(aiFile("strings_sermon.xml")).keys)
        addAll(LocalizationTestSupport.readStringsFromFile(aiFile("strings_card.xml")).keys)
    }

    /** Oral-AI / Cultural-Lens explanation strings (live in the shared strings.xml). */
    private val explainKeys: Set<String> = defaultKeys.filter { it.startsWith("explain_") }.toSet()

    /** Liturgy framing strings (Liturgy_Detail / Liturgy_Library chrome). */
    private val churchKeys: Set<String> =
        LocalizationTestSupport.readStringsFromFile(aiFile("strings_church.xml")).keys

    /** AI feature keys whose presence in every Bible locale is required by Req 1.3. */
    private val aiFeatureKeys: Set<String> = aiFileKeys + explainKeys

    /** Everything Concern 1 commits to translating in this batch. */
    private val inScopeKeys: Set<String> = aiFileKeys + explainKeys + churchKeys

    private fun aiFile(name: String) =
        LocalizationTestSupport.stringFiles("values").first { it.name == name }

    // ---- Provenance manifest (test resource) -------------------------------------------

    private data class LocaleProvenance(
        val provenance: String,
        val reviewed: Set<String>,
        val deferred: Set<String>
    )

    private val provenance: Map<String, LocaleProvenance> by lazy {
        val text = javaClass.getResourceAsStream("/localization/translation-provenance.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("translation-provenance.json not found on the test classpath")
        val root = Json.parseToJsonElement(text).jsonObject
        val locales = root["locales"]!!.jsonObject
        locales.entries.associate { (locale, value) ->
            val obj = value.jsonObject
            locale to LocaleProvenance(
                provenance = obj["provenance"]!!.jsonPrimitive.content,
                reviewed = (obj["reviewed"] as? JsonObject)?.keys
                    ?: obj["reviewed"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
                deferred = obj["deferred"]!!.jsonObject.keys
            )
        }
    }

    // ---- Tests --------------------------------------------------------------------------

    @Test
    fun `provenance is recorded for every Bible locale (Req 4_1, 4_2)`() {
        for (locale in targetLocales) {
            val p = provenance[locale]
            assertTrue(p != null, "no provenance recorded for locale '$locale'")
            assertTrue(p!!.provenance.isNotBlank(), "blank provenance for locale '$locale'")
            // A value that is not human-reviewed is treated as pending review (Req 4.2).
            if (p.provenance != "human-reviewed") {
                assertTrue(
                    p.reviewed.isEmpty() || p.reviewed.all { it in defaultKeys },
                    "reviewed list for '$locale' references unknown keys"
                )
            }
        }
    }

    @Test
    fun `every AI feature key exists in every Bible locale (Req 1_3)`() {
        for (locale in targetLocales) {
            val present = LocalizationTestSupport.readStrings("values-$locale").keys
            val missing = aiFeatureKeys.filter { it !in present }
            assertTrue(
                missing.isEmpty(),
                "locale '$locale' is missing AI feature keys: $missing"
            )
        }
    }

    @Test
    fun `no in-scope key is an undeferred gap in any locale (Req 3_3, Property 17 applied)`() {
        for (locale in targetLocales) {
            val present = LocalizationTestSupport.readStrings("values-$locale").keys
            val deferred = provenance[locale]?.deferred ?: emptySet()
            val gaps = inScopeKeys.filter {
                LocalizationChecks.isGap(it, defaultKeys, present, deferred)
            }
            assertTrue(
                gaps.isEmpty(),
                "locale '$locale' has undeferred in-scope gaps: $gaps"
            )
        }
    }

    @Test
    fun `translated in-scope values preserve English placeholders unless reviewed (Req 4_3-4_5)`() {
        for (locale in targetLocales) {
            val localeStrings = LocalizationTestSupport.readStrings("values-$locale")
            val reviewed = provenance[locale]?.reviewed ?: emptySet()
            val offenders = inScopeKeys.mapNotNull { key ->
                val english = defaultStrings[key] ?: return@mapNotNull null
                val translation = localeStrings[key] ?: return@mapNotNull null
                val accepted = LocalizationChecks.placeholderAccepted(
                    english, translation, key in reviewed
                )
                if (accepted) null else
                    "$key (english=${LocalizationChecks.placeholderTokens(english)}, " +
                        "translation=${LocalizationChecks.placeholderTokens(translation)})"
            }
            assertTrue(
                offenders.isEmpty(),
                "locale '$locale' has placeholder mismatches (not reviewed): $offenders"
            )
        }
    }

    @Test
    fun `the per-key presence inventory is buildable for every locale (Req 3_1)`() {
        // Req 3.1: a documented, machine-buildable inventory of which user-facing keys are
        // present in each locale. We assert it is complete and self-consistent.
        for (locale in targetLocales) {
            val present = LocalizationTestSupport.readStrings("values-$locale").keys
            val inventory: Map<String, Boolean> = defaultKeys.associateWith { it in present }
            assertTrue(inventory.size == defaultKeys.size, "inventory size mismatch for '$locale'")
            // Every in-scope key is recorded as present (true) in the inventory.
            assertTrue(
                inScopeKeys.all { inventory[it] == true },
                "in-scope keys not all present in inventory for '$locale'"
            )
        }
    }
}
