package com.manna.bible.data.remote

/**
 * Normalized, domain-ish content models produced by [HelloAoRemoteDataSource] and
 * consumed by the `DownloadManager` (Task 6) when persisting a translation into Room.
 *
 * These intentionally decouple the rest of the app from the raw helloao DTO shapes:
 * verse text is already flattened to plain UTF-8, and chapter numbers are canonical
 * (Masoretic), matching the `Verse_Reference` contract.
 */

/**
 * A book within a translation.
 *
 * @property osisId USFM/OSIS-like book id (e.g. "GEN").
 * @property name display book name.
 * @property testament "OLD"/"NEW" when known; null when the source doesn't provide it.
 * @property orderIndex source ordering index (canon ordering is applied at read time).
 * @property chapterCount number of chapters in the book.
 */
data class RemoteBook(
    val osisId: String,
    val name: String,
    val testament: String?,
    val orderIndex: Int,
    val chapterCount: Int
)

/**
 * A chapter's verses for a book.
 *
 * @property osisId the book id this chapter belongs to.
 * @property chapter canonical (Masoretic) chapter number.
 * @property verses ordered verses with plain-text content.
 */
data class RemoteChapter(
    val osisId: String,
    val chapter: Int,
    val verses: List<RemoteVerse>
)

/**
 * A single verse.
 *
 * @property verse verse number within the chapter.
 * @property text plain-text verse content (segments flattened, footnotes/markup dropped).
 */
data class RemoteVerse(
    val verse: Int,
    val text: String
)
