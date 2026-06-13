package com.manna.bible.ui.calendar

import androidx.annotation.StringRes
import com.manna.bible.R
import com.manna.bible.domain.calendar.LiturgicalSeason
import com.manna.bible.domain.calendar.ReadingKind

/** Localized name for a feast [id] from `JesusEventsProvider`. */
@StringRes
internal fun eventNameRes(id: String): Int = when (id) {
    "holy_name" -> R.string.calendar_event_holy_name_name
    "epiphany" -> R.string.calendar_event_epiphany_name
    "presentation" -> R.string.calendar_event_presentation_name
    "annunciation" -> R.string.calendar_event_annunciation_name
    "transfiguration" -> R.string.calendar_event_transfiguration_name
    "nativity" -> R.string.calendar_event_nativity_name
    "holy_innocents" -> R.string.calendar_event_holy_innocents_name
    "ash_wednesday" -> R.string.calendar_event_ash_wednesday_name
    "palm_sunday" -> R.string.calendar_event_palm_sunday_name
    "maundy_thursday" -> R.string.calendar_event_maundy_thursday_name
    "good_friday" -> R.string.calendar_event_good_friday_name
    "easter" -> R.string.calendar_event_easter_name
    "ascension" -> R.string.calendar_event_ascension_name
    "pentecost" -> R.string.calendar_event_pentecost_name
    else -> R.string.calendar_event_easter_name
}

/** Localized description for a feast [id] from `JesusEventsProvider`. */
@StringRes
internal fun eventDescriptionRes(id: String): Int = when (id) {
    "holy_name" -> R.string.calendar_event_holy_name_desc
    "epiphany" -> R.string.calendar_event_epiphany_desc
    "presentation" -> R.string.calendar_event_presentation_desc
    "annunciation" -> R.string.calendar_event_annunciation_desc
    "transfiguration" -> R.string.calendar_event_transfiguration_desc
    "nativity" -> R.string.calendar_event_nativity_desc
    "holy_innocents" -> R.string.calendar_event_holy_innocents_desc
    "ash_wednesday" -> R.string.calendar_event_ash_wednesday_desc
    "palm_sunday" -> R.string.calendar_event_palm_sunday_desc
    "maundy_thursday" -> R.string.calendar_event_maundy_thursday_desc
    "good_friday" -> R.string.calendar_event_good_friday_desc
    "easter" -> R.string.calendar_event_easter_desc
    "ascension" -> R.string.calendar_event_ascension_desc
    "pentecost" -> R.string.calendar_event_pentecost_desc
    else -> R.string.calendar_event_easter_desc
}

/** Localized name for a [LiturgicalSeason]. */
@StringRes
internal fun seasonNameRes(season: LiturgicalSeason): Int = when (season) {
    LiturgicalSeason.ADVENT -> R.string.season_advent
    LiturgicalSeason.CHRISTMAS -> R.string.season_christmas
    LiturgicalSeason.EPIPHANY -> R.string.season_epiphany
    LiturgicalSeason.LENT -> R.string.season_lent
    LiturgicalSeason.HOLY_WEEK -> R.string.season_holy_week
    LiturgicalSeason.EASTER -> R.string.season_easter
    LiturgicalSeason.ORDINARY -> R.string.season_ordinary
}

/** Localized label for a reading's role in the liturgy. */
@StringRes
internal fun readingKindRes(kind: ReadingKind): Int = when (kind) {
    ReadingKind.FIRST -> R.string.reading_first
    ReadingKind.PSALM -> R.string.reading_psalm
    ReadingKind.SECOND -> R.string.reading_second
    ReadingKind.GOSPEL -> R.string.reading_gospel
}
