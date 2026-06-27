package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.domain.liturgy.LocalizedText
import com.manna.bible.domain.model.Denomination
import kotlinx.serialization.json.Json

/**
 * Pure mapping between the JSON DTOs ([LiturgyDto]) and the domain [Liturgy], plus the
 * reverse for the serialization round-trip. Kept free of Android dependencies (no
 * `AssetManager`) so the parse + map + validate logic is unit-testable on the JVM from a
 * raw JSON string, mirroring [com.manna.bible.data.canon.CanonDefinitionMapper].
 */
internal object LiturgyMapper {

    /**
     * Parses a raw liturgy-asset JSON [raw] into a [Liturgy].
     *
     * @throws IllegalArgumentException if the asset fails schema validation (blank id /
     *   title / tradition / source note, a `LocalizedText` missing its `en` value, a role
     *   that is not a known [LiturgyRole], or a declared/present language mismatch).
     * @throws kotlinx.serialization.SerializationException if the JSON is malformed.
     */
    fun parse(json: Json, raw: String): Liturgy {
        val dto = json.decodeFromString(LiturgyDto.serializer(), raw)
        val errors = validate(dto)
        require(errors.isEmpty()) { "Invalid liturgy asset '${dto.id}': ${errors.joinToString("; ")}" }
        return dto.toDomain()
    }

    /** Serializes a [Liturgy] back to the asset JSON format (the round-trip inverse of [parse]). */
    fun serialize(json: Json, liturgy: Liturgy): String =
        json.encodeToString(LiturgyDto.serializer(), liturgy.toDto())

    /**
     * Returns the list of schema/consistency problems with [dto] — empty when the asset is
     * valid. Enforces (Req 10.1, 10.5):
     *  - non-blank `id`, `tradition`, English `title`, and English `sourceNote`;
     *  - every localized field carries an `en` value;
     *  - every part role parses to a known [LiturgyRole];
     *  - the declared `languages` set equals the set of languages actually present across
     *    the parts' localized fields (the language/content consistency flag, Property 6).
     */
    fun validate(dto: LiturgyDto): List<String> {
        val errors = mutableListOf<String>()

        if (dto.id.isBlank()) errors += "id is blank"
        if (dto.tradition.isBlank()) errors += "tradition is blank"
        requireEnglish(dto.title, "title", errors)
        requireEnglish(dto.sourceNote, "sourceNote", errors)

        dto.sections.forEachIndexed { sIndex, section ->
            requireEnglish(section.title, "sections[$sIndex].title", errors)
            section.parts.forEachIndexed { pIndex, part ->
                val where = "sections[$sIndex].parts[$pIndex]"
                if (roleOrNull(part.role) == null) errors += "$where has unknown role '${part.role}'"
                part.title?.let { requireEnglish(it, "$where.title", errors) }
                part.text?.let { requireEnglish(it, "$where.text", errors) }
                part.rubric?.let { requireEnglish(it, "$where.rubric", errors) }
            }
        }

        val declared = dto.languages.toSet()
        val present = presentLanguages(dto)
        if (declared != present) {
            errors += "declared languages $declared differ from languages present in parts $present"
        }

        return errors
    }

    /** The set of language tags actually authored across the parts' localized fields. */
    private fun presentLanguages(dto: LiturgyDto): Set<String> =
        dto.sections
            .flatMap { it.parts }
            .flatMap { part -> listOfNotNull(part.title, part.text, part.rubric) }
            .flatMap { it.keys }
            .toSet()

    private fun requireEnglish(values: Map<String, String>, field: String, errors: MutableList<String>) {
        val en = values["en"]
        if (en == null) {
            errors += "$field is missing an 'en' value"
        } else if (en.isBlank()) {
            errors += "$field has a blank 'en' value"
        }
    }

    private fun roleOrNull(role: String): LiturgyRole? =
        LiturgyRole.entries.firstOrNull { it.name == role }

    /**
     * Completeness errors for a manifest entry (Req 9.4) — empty when the entry is complete:
     * non-blank `id`, `title`, `tradition`, and `assetFile`, plus a non-empty `languages` list
     * that includes `en`.
     */
    fun validateManifestEntry(entry: LiturgyManifestEntryDto): List<String> {
        val errors = mutableListOf<String>()
        if (entry.id.isBlank()) errors += "id is blank"
        if (entry.title.isBlank()) errors += "title is blank"
        if (entry.tradition.isBlank()) errors += "tradition is blank"
        if (entry.assetFile.isBlank()) errors += "assetFile is blank"
        if (entry.languages.isEmpty()) {
            errors += "languages is empty"
        } else if ("en" !in entry.languages) {
            errors += "languages does not include 'en'"
        }
        return errors
    }

    // --- DTO -> domain -------------------------------------------------------

    private fun LiturgyDto.toDomain(): Liturgy = Liturgy(
        id = id,
        title = title.toLocalizedText(),
        tradition = tradition,
        sections = sections.map { it.toDomain() },
        sourceNote = sourceNote.toLocalizedText(),
        denominations = denominations.mapNotNull(Denomination::fromId),
        languages = languages.toSet()
    )

    private fun LiturgySectionDto.toDomain(): LiturgySection = LiturgySection(
        title = title.toLocalizedText(),
        parts = parts.map { it.toDomain() }
    )

    private fun LiturgyPartDto.toDomain(): LiturgyPart = LiturgyPart(
        role = roleOrNull(role) ?: throw IllegalArgumentException("Unknown liturgy role: '$role'"),
        title = title?.toLocalizedText(),
        text = text?.toLocalizedText(),
        rubric = rubric?.toLocalizedText(),
        osisRef = osisRef,
        needsOfficialText = needsOfficialText
    )

    private fun Map<String, String>.toLocalizedText(): LocalizedText = LocalizedText(this)

    // --- domain -> DTO -------------------------------------------------------

    private fun Liturgy.toDto(): LiturgyDto = LiturgyDto(
        id = id,
        title = title.toMap(),
        tradition = tradition,
        denominations = denominations.map { it.id },
        languages = languages.toList(),
        sourceNote = sourceNote.toMap(),
        sections = sections.map { it.toDto() }
    )

    private fun LiturgySection.toDto(): LiturgySectionDto = LiturgySectionDto(
        title = title.toMap(),
        parts = parts.map { it.toDto() }
    )

    private fun LiturgyPart.toDto(): LiturgyPartDto = LiturgyPartDto(
        role = role.name,
        title = title?.toMap(),
        text = text?.toMap(),
        rubric = rubric?.toMap(),
        osisRef = osisRef,
        needsOfficialText = needsOfficialText
    )

    /** Reconstructs the full language→value map from a [LocalizedText] (inverse of [toLocalizedText]). */
    private fun LocalizedText.toMap(): Map<String, String> = languages.associateWith(::resolve)
}
