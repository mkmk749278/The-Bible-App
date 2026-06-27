package com.manna.bible.domain.liturgy

import com.manna.bible.domain.model.Denomination
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Property + unit tests for [AssetLiturgyProvider]'s pure denomination mapping, ordering, and
 * default-resolution logic (Req 5.1, 5.3, 11.1–11.6). Exercised directly through the companion
 * functions over an explicit `available` list — no repository or blocking required.
 */
class AssetLiturgyProviderTest {

    private fun liturgy(id: String): Liturgy = Liturgy(
        id = id,
        title = LocalizedText(mapOf("en" to id)),
        tradition = "Tradition $id",
        sections = emptyList(),
        sourceNote = LocalizedText(mapOf("en" to "Source"))
    )

    /** Ids drawn from the mapped orders (present & future) plus unmapped extras. */
    private val idPool = listOf(
        "roman_catholic_mass", "csi_holy_communion", "orthodox_holy_qurbana",
        "mar_thoma_holy_qurbana", "extra_a", "extra_b", "extra_c"
    )

    /** Libraries with distinct ids, varying which mapped/unmapped orders are present. */
    private val availableArb: Arb<List<Liturgy>> =
        Arb.set(Arb.of(idPool), 0..idPool.size).map { ids -> ids.map(::liturgy) }

    @Test
    fun `forDenomination surfaces mapped orders first, then the rest, covering the whole library`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 14: For any Denomination and any bundled library, forDenomination returns, as a prefix, exactly the available liturgies mapped to that denomination, followed by the remaining available liturgies, and the returned set equals the full set of available liturgies (SHOW_EVERYTHING selects the entire library).
        checkAll(250, Arb.enum<Denomination>(), availableArb) { denomination, available ->
            val result = AssetLiturgyProvider.orderedFor(denomination, available)

            // The returned set equals the full available set, with no duplicates or drops.
            assertEquals(available.map { it.id }.toSet(), result.map { it.id }.toSet())
            assertEquals(available.size, result.size)

            if (denomination == Denomination.SHOW_EVERYTHING) {
                assertEquals(available, result)
            } else {
                val mappedIds = AssetLiturgyProvider.MAPPING[denomination].orEmpty()
                    .filter { id -> available.any { it.id == id } }
                val expectedPrefix = mappedIds.map { id -> available.first { it.id == id } }
                assertEquals(expectedPrefix, result.take(expectedPrefix.size))

                val expectedRest = available.filter { it.id !in mappedIds.toSet() }
                assertEquals(expectedRest, result.drop(expectedPrefix.size))
            }
        }
    }

    @Test
    fun `resolvedDefaultFor is total with fallback while the full listing exposes everything`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 15: For any Denomination, whenever the library has at least one valid liturgy, resolvedDefaultFor returns a non-null selectable liturgy (falling back to an available entry when the mapped default is absent), while the full listing still exposes every available liturgy.
        checkAll(250, Arb.enum<Denomination>(), availableArb) { denomination, available ->
            val resolved = AssetLiturgyProvider.resolvedDefaultFor(denomination, available)

            if (available.isEmpty()) {
                assertNull(resolved)
            } else {
                assertNotNull(resolved)
                assertTrue(available.any { it.id == resolved!!.id }, "default must be an available order")
            }

            // The full listing still exposes every available liturgy.
            assertEquals(
                available.map { it.id }.toSet(),
                AssetLiturgyProvider.orderedFor(denomination, available).map { it.id }.toSet()
            )
        }
    }

    // --- default-mapping unit tests (task 6.5) ------------------------------

    private val bundled = listOf(liturgy("roman_catholic_mass"), liturgy("csi_holy_communion"))

    @Test
    fun `catholic maps to the Roman Mass and CSI or other Protestant to Holy Communion`() {
        assertEquals("roman_catholic_mass", AssetLiturgyProvider.mappedDefaultFor(Denomination.CATHOLIC, bundled)?.id)
        assertEquals("csi_holy_communion", AssetLiturgyProvider.mappedDefaultFor(Denomination.CSI, bundled)?.id)
        assertEquals("csi_holy_communion", AssetLiturgyProvider.mappedDefaultFor(Denomination.PROTESTANT_OTHER, bundled)?.id)
    }

    @Test
    fun `an unmapped denomination falls back yet all entries remain listed`() {
        // Orthodox's mapped asset is not bundled yet → no explicit default…
        assertNull(AssetLiturgyProvider.mappedDefaultFor(Denomination.ORTHODOX, bundled))
        // …but resolvedDefaultFor still yields a selectable order (fallback to the first available).
        assertEquals("roman_catholic_mass", AssetLiturgyProvider.resolvedDefaultFor(Denomination.ORTHODOX, bundled)?.id)
        // …and the full listing still exposes everything.
        assertEquals(
            setOf("roman_catholic_mass", "csi_holy_communion"),
            AssetLiturgyProvider.orderedFor(Denomination.ORTHODOX, bundled).map { it.id }.toSet()
        )
    }

    @Test
    fun `show everything selects the entire library in order`() {
        assertEquals(bundled, AssetLiturgyProvider.orderedFor(Denomination.SHOW_EVERYTHING, bundled))
    }
}
