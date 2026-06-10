package com.manna.bible.domain.model

/**
 * Snapshot of the user's setup choices, persisted to and loaded from the preferences store.
 *
 * All selection fields are nullable to represent an in-progress or skipped setup; defaults are
 * applied by the setup flow when a value is absent.
 *
 * @property denomination Selected tradition, or null if not yet chosen.
 * @property canonType Derived canon, or null if not yet chosen.
 * @property uiLanguage App UI language code, independent of [bibleLanguage].
 * @property bibleLanguage Bible text language code, independent of [uiLanguage].
 * @property numberingScheme Selected numbering scheme, or null if not yet chosen.
 * @property namingConventionId Selected naming convention id, or null for translation default.
 * @property bibleTranslationId Selected translation id, or null if none chosen.
 * @property lectionaryId Selected lectionary id, or null if none chosen.
 * @property showDeuterocanonical Protestant toggle to show deuterocanonical books.
 * @property setupCompleted True once the setup flow has completed, gating first-launch.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class SetupState(
    val denomination: Denomination?,
    val canonType: CanonType?,
    val uiLanguage: String?,
    val bibleLanguage: String?,
    val numberingScheme: NumberingScheme?,
    val namingConventionId: String?,
    val bibleTranslationId: String?,
    val lectionaryId: String?,
    val showDeuterocanonical: Boolean = false,
    val setupCompleted: Boolean = false
)
