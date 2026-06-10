package com.manna.bible.domain.canon

import com.manna.bible.domain.model.NumberingScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit and property tests for [PsalmNumberingMapper].
 *
 * Runs on the JVM (JUnit 5) without an emulator. These verify the Masoretic ⇄
 * Septuagint Psalm number correspondence used at the presentation layer.
 *
 * Validates: Requirements 7, 17
 */
class PsalmNumberingMapperTest {

    private val MASORETIC = NumberingScheme.MASORETIC
    private val SEPTUAGINT = NumberingScheme.SEPTUAGINT

    // --- Masoretic identity, both directions (Req 7.2, 7.4) ------------------

    @Test
    fun `masoretic toDisplay is identity for all psalms`() {
        for (n in 1..150) {
            assertEquals(n, PsalmNumberingMapper.toDisplay(MASORETIC, n), "toDisplay($n)")
        }
    }

    @Test
    fun `masoretic toCanonical is identity for all psalms`() {
        for (n in 1..150) {
            assertEquals(n, PsalmNumberingMapper.toCanonical(MASORETIC, n), "toCanonical($n)")
        }
    }

    // --- Septuagint known values (Req 7.3) -----------------------------------

    @Test
    fun `septuagint toDisplay matches known reference values`() {
        // Masoretic -> Septuagint display
        assertEquals(8, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 8))    // identical region
        assertEquals(9, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 9))    // join member (principal)
        assertEquals(9, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 10))   // join member (collapsed)
        assertEquals(10, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 11))  // offset by one
        assertEquals(112, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 113))
        assertEquals(113, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 114)) // join
        assertEquals(113, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 115)) // join
        assertEquals(114, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 116)) // split, first part
        assertEquals(146, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 147)) // split, first part
        assertEquals(150, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 150)) // identical region
    }

    @Test
    fun `septuagint toCanonical matches known reference values`() {
        // Septuagint display -> canonical Masoretic
        assertEquals(8, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 8))
        assertEquals(9, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 9))
        assertEquals(11, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 10))
        assertEquals(113, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 112))
        assertEquals(114, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 113))
        assertEquals(116, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 114))
        assertEquals(147, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 146))
        assertEquals(150, PsalmNumberingMapper.toCanonical(SEPTUAGINT, 150))
    }

    // --- Round-trip invertibility property (Req 7) ---------------------------

    /**
     * The Septuagint round-trip
     * `toCanonical(SEPTUAGINT, toDisplay(SEPTUAGINT, m)) == m`
     * holds for every Masoretic psalm in the PRINCIPAL domain: 1..150 minus the
     * second members of the two LXX joins. Masoretic 10 joins into LXX 9 and
     * Masoretic 115 joins into LXX 113; both collapse onto a shared LXX number
     * whose canonical inverse is, by convention, the join's first member (9 -> 9,
     * 113 -> 114). Those two values are therefore not recoverable and are
     * deliberately excluded here.
     */
    @Test
    fun `septuagint round-trip is identity over the principal domain`() {
        val excluded = PsalmNumberingMapper.SEPTUAGINT_NON_INVERTIBLE_MASORETIC
        assertEquals(setOf(10, 115), excluded, "documented non-invertible second-members")

        for (m in 1..150) {
            if (m in excluded) continue
            val display = PsalmNumberingMapper.toDisplay(SEPTUAGINT, m)
            val roundTripped = PsalmNumberingMapper.toCanonical(SEPTUAGINT, display)
            assertEquals(m, roundTripped, "round-trip failed for Masoretic $m (display=$display)")
        }
    }

    @Test
    fun `septuagint excluded join members collapse onto the principal member`() {
        // Masoretic 10 -> LXX 9 -> canonical 9 (the principal join member, not 10).
        assertEquals(
            9,
            PsalmNumberingMapper.toCanonical(SEPTUAGINT, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 10))
        )
        // Masoretic 115 -> LXX 113 -> canonical 114 (the principal join member, not 115).
        assertEquals(
            114,
            PsalmNumberingMapper.toCanonical(SEPTUAGINT, PsalmNumberingMapper.toDisplay(SEPTUAGINT, 115))
        )
    }

    // --- Range validation (input guard) --------------------------------------

    @Test
    fun `toDisplay rejects out-of-range psalms`() {
        assertThrows(IllegalArgumentException::class.java) {
            PsalmNumberingMapper.toDisplay(SEPTUAGINT, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PsalmNumberingMapper.toDisplay(SEPTUAGINT, 151)
        }
    }

    @Test
    fun `toCanonical rejects out-of-range psalms`() {
        assertThrows(IllegalArgumentException::class.java) {
            PsalmNumberingMapper.toCanonical(MASORETIC, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PsalmNumberingMapper.toCanonical(MASORETIC, 151)
        }
    }
}
