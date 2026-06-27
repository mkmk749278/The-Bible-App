package com.manna.bible.domain.liturgy

import com.manna.bible.data.liturgy.LiturgyRepository
import com.manna.bible.domain.model.Denomination
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asset-backed [LiturgyProvider] that replaces the hardcoded `DefaultLiturgyProvider`. It
 * exposes the parsed, validated bundled liturgies from [LiturgyRepository] and applies the
 * static denomination→liturgy mapping (Req 5.1, 5.3, 11.1–11.6).
 *
 * The [LiturgyProvider] contract is synchronous while the repository loads asynchronously
 * from bundled assets; because the content is two small offline JSON files parsed exactly
 * once (then cached in the repository), the first access blocks briefly via [runBlocking].
 *
 * The pure mapping/ordering/default logic is exposed as companion functions over an explicit
 * `available` list so it is unit/property-testable without the repository or any blocking.
 */
@Singleton
class AssetLiturgyProvider @Inject constructor(
    private val repository: LiturgyRepository
) : LiturgyProvider {

    private val loaded: List<Liturgy> by lazy { runBlocking { repository.liturgies() } }

    override fun all(): List<Liturgy> = loaded

    override fun forDenomination(denomination: Denomination): List<Liturgy> =
        orderedFor(denomination, all())

    override fun defaultFor(denomination: Denomination): Liturgy? =
        mappedDefaultFor(denomination, all())

    override fun resolvedDefaultFor(denomination: Denomination): Liturgy? =
        resolvedDefaultFor(denomination, all())

    companion object {

        /**
         * Denomination → ordered liturgy ids. `SHOW_EVERYTHING` maps to the empty list and is
         * handled as "the whole library" by [orderedFor]. The Orthodox / Mar Thoma entries name
         * planned future assets; until they ship, the mapped id is simply absent from the
         * library and fallback applies (Req 11.3, 11.6).
         */
        val MAPPING: Map<Denomination, List<String>> = mapOf(
            Denomination.CATHOLIC to listOf("roman_catholic_mass"),
            Denomination.CSI to listOf("csi_holy_communion"),
            Denomination.PROTESTANT_OTHER to listOf("csi_holy_communion"),
            Denomination.ORTHODOX to listOf("orthodox_holy_qurbana"),
            Denomination.MAR_THOMA to listOf("mar_thoma_holy_qurbana"),
            Denomination.SHOW_EVERYTHING to emptyList()
        )

        /**
         * The full [available] library with the liturgies mapped to [denomination] (that are
         * actually present) surfaced first, in mapping order, followed by the remaining
         * available liturgies in their original order. `SHOW_EVERYTHING` yields the whole
         * library (Req 5.3, 11.1, 11.5). The returned set always equals the full available set.
         */
        fun orderedFor(denomination: Denomination, available: List<Liturgy>): List<Liturgy> {
            if (denomination == Denomination.SHOW_EVERYTHING) return available
            val mappedIds = MAPPING[denomination].orEmpty()
            val mapped = mappedIds.mapNotNull { id -> available.firstOrNull { it.id == id } }
            val mappedIdSet = mapped.map { it.id }.toSet()
            val rest = available.filter { it.id !in mappedIdSet }
            return mapped + rest
        }

        /** The mapped default for [denomination] that is present in [available], or null. */
        fun mappedDefaultFor(denomination: Denomination, available: List<Liturgy>): Liturgy? =
            MAPPING[denomination].orEmpty()
                .firstNotNullOfOrNull { id -> available.firstOrNull { it.id == id } }

        /**
         * A non-null selectable order whenever [available] is non-empty: the mapped default, or
         * the first available liturgy as a fallback (Req 11.3, 11.6).
         */
        fun resolvedDefaultFor(denomination: Denomination, available: List<Liturgy>): Liturgy? =
            mappedDefaultFor(denomination, available) ?: available.firstOrNull()
    }
}
