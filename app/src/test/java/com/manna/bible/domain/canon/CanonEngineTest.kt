package com.manna.bible.domain.canon

import com.manna.bible.data.canon.CanonDefinitionDataSource
import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonDefinition
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultCanonEngine].
 *
 * Runs on the JVM (JUnit 5) without an emulator. A fake [CanonDefinitionDataSource]
 * returns known definitions so the engine's mapping and profile assembly can be
 * verified deterministically.
 *
 * Validates: Requirements 3, 6, 7, 16
 */
class CanonEngineTest {

    // --- Fake data source ----------------------------------------------------

    /** Builds a [CanonDefinition] with [otCount] OT then [ntCount] NT books. */
    private fun definition(
        canonType: CanonType,
        scheme: NumberingScheme,
        otCount: Int,
        ntCount: Int,
        deuteroIndices: Set<Int> = emptySet()
    ): CanonDefinition {
        val books = buildList {
            var order = 0
            repeat(otCount) {
                add(
                    CanonBook(
                        osisId = "OT$order",
                        testament = Testament.OLD,
                        orderIndex = order,
                        isDeuterocanonical = order in deuteroIndices
                    )
                )
                order++
            }
            repeat(ntCount) {
                add(
                    CanonBook(
                        osisId = "NT$order",
                        testament = Testament.NEW,
                        orderIndex = order,
                        isDeuterocanonical = false
                    )
                )
                order++
            }
        }
        return CanonDefinition(canonType, scheme, books)
    }

    /** A fake that hands back definitions keyed by canon type, optionally shuffled. */
    private inner class FakeDataSource(
        private val shuffleBooks: Boolean = false
    ) : CanonDefinitionDataSource {
        override suspend fun definitionFor(canonType: CanonType): CanonDefinition {
            val def = when (canonType) {
                CanonType.PROTESTANT_66 ->
                    definition(canonType, NumberingScheme.MASORETIC, otCount = 39, ntCount = 27)
                CanonType.CATHOLIC_73 ->
                    definition(
                        canonType,
                        NumberingScheme.SEPTUAGINT,
                        otCount = 46,
                        ntCount = 27,
                        deuteroIndices = setOf(40, 41, 42, 43, 44, 45, 46).map { it - 1 }.toSet()
                    )
                CanonType.ORTHODOX_EXPANDED ->
                    definition(canonType, NumberingScheme.SEPTUAGINT, otCount = 49, ntCount = 27)
                CanonType.ALL_CANONS ->
                    definition(canonType, NumberingScheme.MASORETIC, otCount = 49, ntCount = 27)
            }
            return if (shuffleBooks) def.copy(books = def.books.shuffled()) else def
        }
    }

    private val engine = DefaultCanonEngine(FakeDataSource())

    // --- canonTypeFor: every Denomination value (Req 3, 16) ------------------

    @Test
    fun `canonTypeFor maps every denomination to its canon`() {
        assertEquals(CanonType.CATHOLIC_73, engine.canonTypeFor(Denomination.CATHOLIC))
        assertEquals(CanonType.PROTESTANT_66, engine.canonTypeFor(Denomination.CSI))
        assertEquals(CanonType.PROTESTANT_66, engine.canonTypeFor(Denomination.PROTESTANT_OTHER))
        assertEquals(CanonType.PROTESTANT_66, engine.canonTypeFor(Denomination.MAR_THOMA))
        assertEquals(CanonType.ORTHODOX_EXPANDED, engine.canonTypeFor(Denomination.ORTHODOX))
        assertEquals(CanonType.ALL_CANONS, engine.canonTypeFor(Denomination.SHOW_EVERYTHING))
    }

    // --- profileFor: catholic & csi (Req 3, 6, 7) ----------------------------

    @Test
    fun `profileFor catholic yields catholic_73 septuagint 73 books`() = runBlocking {
        val profile = engine.profileFor(Denomination.CATHOLIC, bibleLanguage = "en")

        assertEquals(Denomination.CATHOLIC, profile.denomination)
        assertEquals(CanonType.CATHOLIC_73, profile.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, profile.numberingScheme)
        assertEquals(73, profile.books.size)
        assertEquals(7, profile.books.count { it.isDeuterocanonical })
    }

    @Test
    fun `profileFor csi yields protestant_66 masoretic 66 books`() = runBlocking {
        val profile = engine.profileFor(Denomination.CSI, bibleLanguage = "en")

        assertEquals(Denomination.CSI, profile.denomination)
        assertEquals(CanonType.PROTESTANT_66, profile.canonType)
        assertEquals(NumberingScheme.MASORETIC, profile.numberingScheme)
        assertEquals(66, profile.books.size)
        assertEquals(39, profile.books.count { it.testament == Testament.OLD })
        assertEquals(27, profile.books.count { it.testament == Testament.NEW })
        assertEquals(0, profile.books.count { it.isDeuterocanonical })
    }

    // --- ordering determinism (Req 6) ----------------------------------------

    @Test
    fun `profileFor returns books sorted strictly increasing by orderIndex`() = runBlocking {
        val shuffledEngine = DefaultCanonEngine(FakeDataSource(shuffleBooks = true))
        val profile = shuffledEngine.profileFor(Denomination.CATHOLIC, bibleLanguage = "en")

        val orders = profile.books.map { it.orderIndex }
        assertEquals(orders.sorted(), orders)
        orders.zipWithNext { a, b -> assertTrue(b > a, "orderIndex must strictly increase: $a then $b") }
    }

    // --- later-task fields are null for now (Req 7 scope) --------------------

    @Test
    fun `profileFor leaves naming, translation, and lectionary unset`() = runBlocking {
        val profile: CanonProfile = engine.profileFor(Denomination.ORTHODOX, bibleLanguage = "ml")

        assertNull(profile.namingConventionId)
        assertNull(profile.suggestedTranslationId)
        assertNull(profile.lectionaryId)
    }
}
