package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.model.Denomination
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Migration fidelity guard (Req 12.1, 12.2, 12.4).
 *
 * Parses the shipped bundled assets (`src/main/assets/liturgy/{id}.json`) via
 * [LiturgyMapper] and asserts deep structural + content equality against a **frozen
 * fixture** captured from the legacy `DefaultLiturgyProvider` at migration time
 * (`src/test/resources/liturgy/legacy/{id}.json`). The fixture is self-contained, so this
 * assertion remains valid after `DefaultLiturgyProvider` is removed.
 *
 * On top of the structural freeze it pins the content-policy invariants explicitly: the
 * verbatim source notes, the Words-of-Institution `osisRef`, every `needsOfficialText`
 * flag, and authored section/part ordering.
 */
class LiturgyMigrationFidelityTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun moduleFile(relative: String): File =
        listOf(File(relative), File("app/$relative")).firstOrNull { it.exists() }
            ?: error("Missing $relative (cwd=${File(".").absolutePath})")

    private fun liveAsset(id: String): Liturgy =
        LiturgyMapper.parse(json, moduleFile("src/main/assets/liturgy/$id.json").readText())

    private fun frozenFixture(id: String): Liturgy =
        LiturgyMapper.parse(json, moduleFile("src/test/resources/liturgy/legacy/$id.json").readText())

    @Test
    fun `roman_catholic_mass asset matches the frozen legacy structure verbatim`() {
        val live = liveAsset("roman_catholic_mass")
        val frozen = frozenFixture("roman_catholic_mass")

        // Deep structural + content freeze (order, roles, localized text, flags, osisRefs).
        assertEquals(frozen, live)

        assertEquals("roman_catholic_mass", live.id)
        assertEquals("The Holy Mass", live.title.english)
        assertEquals("Roman Catholic", live.tradition)
        assertEquals(listOf(Denomination.CATHOLIC), live.denominations)
        assertEquals(
            listOf("Introductory Rites", "Liturgy of the Word", "Liturgy of the Eucharist", "Concluding Rites"),
            live.sections.map { it.title.english }
        )
        assertEquals(
            "Order of Mass, Roman Rite — structure per the USCCB Order of Mass. Ordinary texts in " +
                "traditional / ecumenical (ICET) English. The prayers proper to the day (Collect, " +
                "Preface, Eucharistic Prayer, Prayer after Communion) follow the parish Missal.",
            live.sourceNote.english
        )
        // The five presidential prayers proper to the day stay flagged, never fabricated.
        assertEquals(5, live.officialTextParts())
        assertTrue(live.allSpokenPartsHaveText(), "every non-rubric part carries spoken text")
    }

    @Test
    fun `csi_holy_communion asset matches the frozen legacy structure verbatim`() {
        val live = liveAsset("csi_holy_communion")
        val frozen = frozenFixture("csi_holy_communion")

        assertEquals(frozen, live)

        assertEquals("csi_holy_communion", live.id)
        assertEquals("The Holy Communion", live.title.english)
        assertEquals("Church of South India", live.tradition)
        assertEquals(listOf(Denomination.CSI, Denomination.PROTESTANT_OTHER), live.denominations)
        assertEquals(
            "The Lord's Supper or the Holy Eucharist — structure per the Church of South India Book " +
                "of Common Worship (1950/1954). Texts in traditional English; the Collect of the day " +
                "and the Thanksgiving (Eucharistic) prayer follow the CSI service book.",
            live.sourceNote.english
        )
        assertEquals(3, live.officialTextParts())

        // The Words of Institution link to 1 Corinthians 11:23.
        val institution = live.sections.flatMap { it.parts }.firstOrNull { it.osisRef != null }
        assertEquals("1CO.11.23", institution?.osisRef)
        assertEquals("The Words of Institution", institution?.title?.english)
    }

    private fun Liturgy.officialTextParts(): Int =
        sections.flatMap { it.parts }.count { it.needsOfficialText }

    private fun Liturgy.allSpokenPartsHaveText(): Boolean =
        sections.flatMap { it.parts }.all { it.role == LiturgyRole.RUBRIC || !it.text?.english.isNullOrBlank() }
}
