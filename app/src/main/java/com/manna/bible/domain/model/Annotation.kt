package com.manna.bible.domain.model

/**
 * The kind of user-created annotation that references Bible text (Req 11).
 */
enum class AnnotationType { HIGHLIGHT, BOOKMARK, NOTE }

/**
 * A colored highlight applied to a single verse.
 *
 * Pure-domain mirror of the persisted highlight row. [verseRef] is a canonical
 * verse reference such as `"GEN.1.1"` (OSIS book id `.` chapter `.` verse); the
 * book id is the part before the first `.`.
 */
data class Highlight(
    val id: Long,
    val verseRef: String,
    val colorArgb: Int,
    val createdAt: Long
)

/**
 * A bookmark on a single verse, optionally labelled by the user.
 *
 * [verseRef] is a canonical verse reference such as `"TOB.3.2"`.
 */
data class Bookmark(
    val id: Long,
    val verseRef: String,
    val label: String?,
    val createdAt: Long
)

/**
 * A free-text note attached to a verse or a whole chapter.
 *
 * Exactly one of [verseRef] (e.g. `"PSA.23.1"`) or [chapterRef] (e.g. `"PSA.23"`)
 * is expected to be set; both embed the OSIS book id before the first `.`.
 */
data class Note(
    val id: Long,
    val verseRef: String?,
    val chapterRef: String?,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
