package com.manna.bible.data.canon

import com.manna.bible.data.canon.CanonDefinitionMapper.toDomain
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Unit tests for the canon-definition parse + map logic.
 *
 * These run on the JVM (JUnit 5) without an emulator: the bundled JSON assets are
 * read directly from the module's `src/main/assets` directory, exercising the same
 * parsing the [AssetCanonDefinitionDataSource] performs at runtime.
 *
 * Validates: Requirements 6
 */
class CanonDefinitionMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- Real bundled-asset parsing -----------------------------------------

    @Test
    fun `protestant_66 parses to 66 books, masoretic, no deuterocanonical`() {
        val definition = CanonDefinitionMapper.parse(json, readAsset("protestant_66.json"))

        assertEquals(CanonType.PROTESTANT_66, definition.canonType)
        assertEquals(NumberingScheme.MASORETIC, definition.numberingScheme)
        assertEquals(66, definition.books.size)
        assertEquals(39, definition.books.count { it.testament == Testament.OLD })
        assertEquals(27, definition.books.count { it.testament == Testament.NEW })
        assertEquals(0, definition.books.count { it.isDeuterocanonical })
    }

    @Test
    fun `catholic_73 parses to 73 books, septuagint, with 7 deuterocanonical`() {
        val definition = CanonDefinitionMapper.parse(json, readAsset("catholic_73.json"))

        assertEquals(CanonType.CATHOLIC_73, definition.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, definition.numberingScheme)
        assertEquals(73, definition.books.size)
        assertEquals(7, definition.books.count { it.isDeuterocanonical })

        val deuteroIds = definition.books.filter { it.isDeuterocanonical }.map { it.osisId }.toSet()
        assertEquals(setOf("TOB", "JDT", "WIS", "SIR", "BAR", "1MA", "2MA"), deuteroIds)
    }

    @Test
    fun `orthodox_expanded parses with septuagint scheme and is a superset of protestant`() {
        val orthodox = CanonDefinitionMapper.parse(json, readAsset("orthodox_expanded.json"))
        val protestant = CanonDefinitionMapper.parse(json, readAsset("protestant_66.json"))

        assertEquals(CanonType.ORTHODOX_EXPANDED, orthodox.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, orthodox.numberingScheme)

        val orthodoxIds = orthodox.books.map { it.osisId }.toSet()
        val protestantIds = protestant.books.map { it.osisId }.toSet()
        assertTrue(orthodoxIds.containsAll(protestantIds))
    }

    // --- DTO -> domain mapping ----------------------------------------------

    @Test
    fun `maps testament, canonType, numberingScheme, and deuterocanonical key`() {
        val raw = """
            {
              "canonType": "catholic_73",
              "numberingScheme": "septuagint",
              "books": [
                { "osisId": "GEN", "testament": "OLD", "orderIndex": 0, "deuterocanonical": false },
                { "osisId": "TOB", "testament": "OLD", "orderIndex": 1, "deuterocanonical": true },
                { "osisId": "MAT", "testament": "NEW", "orderIndex": 2, "deuterocanonical": false }
              ]
            }
        """.trimIndent()

        val definition = CanonDefinitionMapper.parse(json, raw)

        assertEquals(CanonType.CATHOLIC_73, definition.canonType)
        assertEquals(NumberingScheme.SEPTUAGINT, definition.numberingScheme)

        val gen = definition.books[0]
        assertEquals("GEN", gen.osisId)
        assertEquals(Testament.OLD, gen.testament)
        assertEquals(0, gen.orderIndex)
        assertFalse(gen.isDeuterocanonical)

        // The JSON key "deuterocanonical" maps onto isDeuterocanonical.
        assertTrue(definition.books[1].isDeuterocanonical)
        assertEquals(Testament.NEW, definition.books[2].testament)
    }

    @Test
    fun `numberingSchemeFrom is case-insensitive`() {
        assertEquals(NumberingScheme.MASORETIC, CanonDefinitionMapper.numberingSchemeFrom("MASORETIC"))
        assertEquals(NumberingScheme.SEPTUAGINT, CanonDefinitionMapper.numberingSchemeFrom("Septuagint"))
    }

    @Test
    fun `unknown canonType throws`() {
        val dto = CanonDefinitionDto(
            canonType = "bogus_canon",
            numberingScheme = "masoretic",
            books = emptyList()
        )
        assertThrows(IllegalArgumentException::class.java) { dto.toDomain() }
    }

    @Test
    fun `unknown numberingScheme throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonDefinitionMapper.numberingSchemeFrom("vulgate")
        }
    }

    @Test
    fun `unknown testament throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonDefinitionMapper.testamentFrom("APOCRYPHA")
        }
    }

    // --- Union (ALL_CANONS) --------------------------------------------------

    @Test
    fun `buildUnion de-duplicates by osisId and reassigns contiguous order`() {
        val orthodox = CanonDefinitionMapper.parse(json, readAsset("orthodox_expanded.json"))
        val catholic = CanonDefinitionMapper.parse(json, readAsset("catholic_73.json"))
        val protestant = CanonDefinitionMapper.parse(json, readAsset("protestant_66.json"))

        val union = CanonDefinitionMapper.buildUnion(listOf(orthodox, catholic, protestant))

        assertEquals(CanonType.ALL_CANONS, union.canonType)
        assertEquals(NumberingScheme.MASORETIC, union.numberingScheme)

        // No duplicate osisIds.
        val ids = union.books.map { it.osisId }
        assertEquals(ids.size, ids.toSet().size)

        // Union contains every book from every source canon.
        val expectedIds = (orthodox.books + catholic.books + protestant.books).map { it.osisId }.toSet()
        assertEquals(expectedIds, ids.toSet())

        // orderIndex is contiguous and 0-based.
        assertEquals(union.books.indices.toList(), union.books.map { it.orderIndex })

        // Old Testament books all precede New Testament books.
        val firstNewIndex = union.books.indexOfFirst { it.testament == Testament.NEW }
        val lastOldIndex = union.books.indexOfLast { it.testament == Testament.OLD }
        assertTrue(lastOldIndex < firstNewIndex)
    }

    @Test
    fun `buildUnion marks a book deuterocanonical if any canon flags it`() {
        val orthodox = CanonDefinitionMapper.parse(json, readAsset("orthodox_expanded.json"))
        val protestant = CanonDefinitionMapper.parse(json, readAsset("protestant_66.json"))

        // Protestant lists no deuterocanonical books; orthodox flags TOB.
        val union = CanonDefinitionMapper.buildUnion(listOf(protestant, orthodox))

        val tob = union.books.first { it.osisId == "TOB" }
        assertTrue(tob.isDeuterocanonical)
    }

    private fun readAsset(name: String): String {
        val candidates = listOf(
            File("src/main/assets/canon/$name"),
            File("app/src/main/assets/canon/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Canon asset '$name' not found (cwd=${File(".").absolutePath})")
        return file.readText()
    }
}
