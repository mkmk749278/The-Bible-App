package com.manna.bible.data.canon

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonDefinition
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import kotlinx.serialization.json.Json

/**
 * Pure mapping between the JSON DTOs ([CanonDefinitionDto]) and the domain
 * [CanonDefinition]. Kept free of Android dependencies so the parse + map logic
 * is unit-testable on the JVM from a raw JSON string (no emulator / AssetManager).
 */
internal object CanonDefinitionMapper {

    /**
     * Parses a raw canon-definition JSON [raw] string into a [CanonDefinition].
     *
     * @throws IllegalArgumentException if the canon type or numbering scheme is
     *   unrecognized, or a testament value cannot be parsed.
     * @throws kotlinx.serialization.SerializationException if the JSON is malformed.
     */
    fun parse(json: Json, raw: String): CanonDefinition =
        json.decodeFromString(CanonDefinitionDto.serializer(), raw).toDomain()

    /** Maps a parsed [CanonDefinitionDto] to its domain [CanonDefinition]. */
    fun CanonDefinitionDto.toDomain(): CanonDefinition {
        val type = CanonType.fromId(canonType)
            ?: throw IllegalArgumentException("Unknown canonType: '$canonType'")
        return CanonDefinition(
            canonType = type,
            numberingScheme = numberingSchemeFrom(numberingScheme),
            books = books.map { it.toDomain() }
        )
    }

    private fun CanonBookDto.toDomain(): CanonBook = CanonBook(
        osisId = osisId,
        testament = testamentFrom(testament),
        orderIndex = orderIndex,
        isDeuterocanonical = isDeuterocanonical
    )

    /** Maps the JSON numbering-scheme token to [NumberingScheme]. */
    fun numberingSchemeFrom(value: String): NumberingScheme = when (value.lowercase()) {
        "masoretic" -> NumberingScheme.MASORETIC
        "septuagint" -> NumberingScheme.SEPTUAGINT
        else -> throw IllegalArgumentException("Unknown numberingScheme: '$value'")
    }

    /** Maps the JSON testament token ("OLD"/"NEW") to [Testament]. */
    fun testamentFrom(value: String): Testament =
        try {
            Testament.valueOf(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown testament: '$value'", e)
        }

    /**
     * Builds the `ALL_CANONS` union from the supplied [definitions].
     *
     * Behavior:
     * - Books are de-duplicated by `osisId`; the first occurrence (per the input
     *   order of [definitions]) wins, so callers should pass the most expansive
     *   canon first to obtain a sensible interleaved ordering.
     * - A book is flagged deuterocanonical if any contributing canon marks it so.
     * - Old Testament books precede New Testament books; `orderIndex` is then
     *   reassigned to a contiguous 0-based sequence.
     * - The union uses [NumberingScheme.MASORETIC] (documented choice: the union
     *   spans traditions, so the Protestant baseline numbering is used as the
     *   neutral default; tradition-specific Psalm display numbering is applied at
     *   the presentation layer).
     */
    fun buildUnion(definitions: List<CanonDefinition>): CanonDefinition {
        val firstSeen = LinkedHashMap<String, CanonBook>()
        val deuterocanonical = HashSet<String>()
        for (definition in definitions) {
            for (book in definition.books) {
                if (book.isDeuterocanonical) deuterocanonical.add(book.osisId)
                firstSeen.putIfAbsent(book.osisId, book)
            }
        }

        val combined = firstSeen.values
        val ordered = (combined.filter { it.testament == Testament.OLD } +
            combined.filter { it.testament == Testament.NEW })
            .mapIndexed { index, book ->
                book.copy(
                    orderIndex = index,
                    isDeuterocanonical = book.osisId in deuterocanonical
                )
            }

        return CanonDefinition(
            canonType = CanonType.ALL_CANONS,
            numberingScheme = NumberingScheme.MASORETIC,
            books = ordered
        )
    }
}
