package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.translation.Translation

/**
 * Room persistence model for a Bible [Translation] in the catalog.
 *
 * Stored in the `translations` table. Unlike the pure-domain [Translation], the
 * entity additionally retains [sizeBytes] (download size metadata) which the domain
 * model does not carry. The [canonType] is persisted by its stable string id so the
 * schema is decoupled from the enum's ordinal.
 *
 * Use [toDomain] / [Translation.toEntity] to convert between layers.
 */
@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val languageCode: String,
    val canonType: String,
    val hasDeuterocanon: Boolean,
    val isDownloaded: Boolean,
    val sizeBytes: Long,
    val isDefaultForCanon: Boolean,
    /**
     * True when this translation's content ships inside the app (seeded from
     * assets) rather than being downloaded. Defaults to `false` so existing rows
     * and the additive migration remain safe.
     */
    val isBundled: Boolean = false,
    /**
     * Version of the content currently stored for this translation. Bundled
     * content is seeded under its asset version; downloads commit a version only
     * once all chapters are written. Defaults to `0` (no content committed yet).
     */
    val contentVersion: Int = 0,
    /**
     * Number of verses stored for this translation. Defaults to `0` until content
     * is seeded or downloaded.
     */
    val verseCount: Int = 0
)

/**
 * Maps this entity to its pure-domain [Translation].
 *
 * The domain model has no `sizeBytes` field, so that value stays on the entity.
 * An unrecognized [canonType] id falls back to [CanonType.PROTESTANT_66] to keep
 * reads resilient to stale rows.
 */
fun TranslationEntity.toDomain(): Translation = Translation(
    id = id,
    name = name,
    languageCode = languageCode,
    canonType = CanonType.fromId(canonType) ?: CanonType.PROTESTANT_66,
    hasDeuterocanon = hasDeuterocanon,
    isDownloaded = isDownloaded,
    isDefaultForCanon = isDefaultForCanon,
    isBundled = isBundled
)

/**
 * Maps a pure-domain [Translation] to a persistable [TranslationEntity].
 *
 * @param sizeBytes download size metadata to retain on the entity; defaults to 0
 *   since the domain model does not track it.
 * @param isBundled whether the translation's content ships in the app; defaults
 *   to the domain model's own [Translation.isBundled].
 * @param contentVersion stored content version; defaults to `0`.
 * @param verseCount number of stored verses; defaults to `0`.
 */
fun Translation.toEntity(
    sizeBytes: Long = 0L,
    isBundled: Boolean = this.isBundled,
    contentVersion: Int = 0,
    verseCount: Int = 0
): TranslationEntity = TranslationEntity(
    id = id,
    name = name,
    languageCode = languageCode,
    canonType = canonType.id,
    hasDeuterocanon = hasDeuterocanon,
    isDownloaded = isDownloaded,
    sizeBytes = sizeBytes,
    isDefaultForCanon = isDefaultForCanon,
    isBundled = isBundled,
    contentVersion = contentVersion,
    verseCount = verseCount
)
