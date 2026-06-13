package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * A single Station of the Cross — one stop on the *Via Crucis*, the meditation on
 * Christ's passion from His condemnation to His burial.
 *
 * @property id Stable, machine-readable id (e.g. "station_01"). The presentation
 *   layer maps this to a localized title, reflection, and prayer, so the domain
 *   stays free of UI strings (matching [com.manna.bible.domain.calendar.JesusEventsProvider]).
 * @property number The station's place in the sequence (1..14).
 * @property scripture The Gospel passage the station is drawn from, within the
 *   66-book canon so it resolves in any downloaded translation. Stations that rest
 *   on tradition rather than an explicit verse (e.g. Veronica, the falls) borrow the
 *   nearest passion passage so the reader always has Scripture to open.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
data class Station(
    val id: String,
    val number: Int,
    val scripture: ReadingRef
)

/**
 * Supplies the fourteen Stations of the Cross (the Prayers hub). The order, count,
 * and Gospel anchoring are the traditional Western form fixed by the Church; the
 * devotional prose (the "We adore you, O Christ…" versicle, each reflection, and
 * each prayer) lives in string resources indexed by [Station.id].
 *
 * Fully offline and deterministic. Pure Kotlin — no Android dependencies.
 */
interface StationsProvider {

    /** Every station, in devotional order (1..14). */
    fun stations(): List<Station>

    /** The station with [number] (1..14), or null when out of range. */
    fun station(number: Int): Station?
}

/**
 * Default [StationsProvider] with the traditional fourteen stations, each anchored
 * to a passion passage in the shared 66-book canon.
 */
class DefaultStationsProvider @Inject constructor() : StationsProvider {

    override fun stations(): List<Station> = STATIONS

    override fun station(number: Int): Station? = STATIONS.getOrNull(number - 1)

    private companion object {
        val STATIONS: List<Station> = listOf(
            Station("station_01", 1, ReadingRef("MAT", 27, 26)),   // condemned to death
            Station("station_02", 2, ReadingRef("JHN", 19, 17)),   // takes up His cross
            Station("station_03", 3, ReadingRef("ISA", 53, 4)),    // falls the first time
            Station("station_04", 4, ReadingRef("LUK", 2, 34)),    // meets His mother
            Station("station_05", 5, ReadingRef("MRK", 15, 21)),   // Simon helps carry it
            Station("station_06", 6, ReadingRef("ISA", 53, 2)),    // Veronica wipes His face
            Station("station_07", 7, ReadingRef("ISA", 53, 5)),    // falls the second time
            Station("station_08", 8, ReadingRef("LUK", 23, 27)),   // the women of Jerusalem
            Station("station_09", 9, ReadingRef("LAM", 3, 19)),    // falls the third time
            Station("station_10", 10, ReadingRef("JHN", 19, 23)),  // stripped of His garments
            Station("station_11", 11, ReadingRef("LUK", 23, 33)),  // nailed to the cross
            Station("station_12", 12, ReadingRef("LUK", 23, 46)),  // dies on the cross
            Station("station_13", 13, ReadingRef("JHN", 19, 38)),  // taken down from the cross
            Station("station_14", 14, ReadingRef("MAT", 27, 59))   // laid in the tomb
        )
    }
}
