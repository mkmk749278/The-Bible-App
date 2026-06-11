package com.manna.bible.domain.translation

import com.manna.bible.domain.model.CanonType

/**
 * A Bible edition as seen by the domain layer for canon-aware filtering.
 *
 * This is the pure-domain view of a translation. It deliberately carries only the
 * information needed to decide canon compatibility and ranking — the canon the
 * edition follows, whether it includes the deuterocanonical books, and whether it
 * is the recommended/default edition for its canon+language — plus availability.
 * The data layer maps its Room `Translation` entity (id, name, languageCode,
 * scriptDirection, isDownloaded, sizeBytes) onto this model, supplying
 * [canonType], [hasDeuterocanon], and [isDefaultForCanon] from catalog metadata.
 *
 * @property id Stable translation identifier (matches the catalog / Room entity id).
 * @property name Human-readable edition name.
 * @property languageCode Bible text language code (e.g. "en", "ml", "te").
 * @property canonType The canon this edition follows.
 * @property hasDeuterocanon True if the edition includes the deuterocanonical books.
 * @property isDownloaded Whether the edition is available offline.
 * @property isDefaultForCanon True if this is a recommended/default edition for its
 *   canon and language.
 * @property isBundled True if this edition ships inside the app (the bundled,
 *   public-domain World English Bible). Used for attribution (Req 12.2).
 *
 * Pure Kotlin — no Android dependencies.
 */
data class Translation(
    val id: String,
    val name: String,
    val languageCode: String,
    val canonType: CanonType,
    val hasDeuterocanon: Boolean,
    val isDownloaded: Boolean = false,
    val isDefaultForCanon: Boolean = false,
    val isBundled: Boolean = false
)
