# Design Document

## Overview

This document specifies the technical design for the **offline Bible reading experience**: the data pipeline that gets real scripture into the app, the storage model, and the reader/search/audio UI. It replaces the placeholder `MainScreen` and the stub translation source shipped during `denomination-aware-setup`.

The design is **offline-first and open-source-first**:

- **Bundled default content** — the World English Bible (WEB) and its deuterocanonical edition (WEBBE/“WEB + Deuterocanon”) are packaged into the app and seeded into Room on first launch, so the app reads scripture with no network and no API key, including the Catholic/Orthodox canon.
- **On-demand content** — additional translations are fetched from the **Free Use Bible API** (`https://bible.helloao.org/api`, MIT-licensed, key-less, commercial-OK) and stored in Room for offline reading.
- **Audio** — offline Android TTS now; a `NarratedAudioProvider` seam is defined for future human-narrated audio (out of scope).

It reuses, without redefining, the components shipped by `denomination-aware-setup`: `CanonEngine`, `CanonProfile`, `NumberingScheme`, `PsalmNumberingMapper`, `CanonBookOrdering`, `PsalmDisplay`, `TranslationFilter`, `LectionaryProvider`, `PreferencesStore`, `PendingDownloadRepository`, `ConnectivityChecker`, and the annotation entities/`AnnotationRepository`.

All builds, tests, and releases run on **GitHub Actions** (no local dev machine). The bundled-content asset is produced by a **reproducible CI data-prep step** (below), not hand-authored.

### Goals

- Real scripture available offline on first launch (WEB + deuterocanon), including correct canon, ordering, and Psalm numbering per the active `CanonProfile`.
- A responsive reader (chapter render < 50ms from Room, search < 200ms over ~31k verses, first verse < 1.5s cold start).
- Download/manage additional translations from helloao with offline queuing and integrity (no partial-as-complete).
- Annotations, TTS audio, and search fully functional offline.

### Non-Goals

- Side-by-side translation comparison (deferred).
- Human-narrated audio backend (only the seam + TTS here).
- API.Bible/commercial-licensed translations.

## Content Data Pipeline (how real text gets into the app)

This is a first-class design concern: "bundle WEB" requires ~31k verses of real data, reproducibly, without a local machine.

### Source

The Free Use Bible API exposes per-translation JSON:
- `GET /api/available_translations.json` → catalog (id, name, language, etc.)
- `GET /api/{translation}/books.json` → book list (USFM ids: `GEN`, `PSA`, …)
- `GET /api/{translation}/{book}/{chapter}.json` → chapter content (verses with numbers + text segments)

WEB translation ids on helloao: the English WEB (protestant 66) and the deuterocanon-containing edition. The exact ids are resolved at prep time from `available_translations.json` (do not hard-code blindly; select by language=eng + name match + presence of deuterocanonical books).

### Prep step (CI, reproducible)

A Gradle task `:app:prepareBundledBibles` (registered in `app/build.gradle.kts`, runnable in CI before `assemble`) does:

1. Download `books.json` and every `{book}/{chapter}.json` for the two WEB editions from helloao.
2. Normalize each verse to `{ book(osisId), chapter(canonical int), verse(int), text(plain) }`, flattening helloao's verse content segments to plain text (preserve verse text; drop footnotes/markup for v1).
3. Package into a compact, versioned asset under `app/src/main/assets/bibles/`:
   - `web.bible.json` (66-book) and `web_deuterocanon.bible.json` (with deuterocanon), gzipped, plus a small `manifest.json` (translation id, name, language, canonType hint, version, verse count, checksum).
4. The task is **idempotent and offline-safe at build time**: if the assets already exist with the current `bundledContentVersion`, it is skipped. CI runs it on demand; the produced assets are committed (or produced in the release workflow before `assembleRelease`).

Rationale: committing normalized, gzipped JSON keeps the repo/APK reasonable (WEB plain text ≈ 4–5 MB/edition uncompressed, ~1.5 MB gzipped) and makes seeding fast and deterministic. A prebuilt Room SQLite was considered but rejected for v1 because JSON assets are diff-friendly, smaller, and decouple seeding from Room schema versioning.

### Verse text normalization

`text` is plain UTF-8 (poetry line breaks collapsed to spaces for v1; structure can be enriched later). Verse numbering from helloao is canonical (Masoretic) for Psalms; the app applies Septuagint display numbering at render time via `PsalmDisplay` — **stored numbers are always canonical**, matching the `Verse_Reference` contract.

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│ UI Layer (Compose + ViewModels)                                 │
│  ReaderScreen / ReaderViewModel        (read, navigate, annotate)│
│  TranslationCatalogScreen / VM         (browse, download, switch) │
│  SearchScreen / SearchViewModel                                  │
│  AudioBar (TTS controls) within ReaderScreen                     │
├───────────────────────────────────────────────────────────────┤
│ Domain Layer (pure Kotlin)                                       │
│  GetChapterUseCase, NavigateChapterUseCase                       │
│  SearchScriptureUseCase                                          │
│  DownloadTranslationUseCase, DeleteTranslationUseCase            │
│  SetActiveTranslationUseCase, RestoreReadingPositionUseCase      │
│  reuse: CanonEngine, CanonBookOrdering, PsalmDisplay,            │
│         TranslationFilter, PsalmNumberingMapper                  │
│  TtsReader (interface), NarratedAudioProvider (seam)             │
├───────────────────────────────────────────────────────────────┤
│ Data Layer                                                       │
│  BibleContentRepository (Room) ← BundledBibleSeeder (assets)     │
│                                ← DownloadManager ← HelloAoRemote │
│  Room: BookEntity, ChapterEntity, VerseEntity (+ FTS), Translation│
│  HelloAoRemoteDataSource (Retrofit), HelloAo DTOs                │
│  AndroidTtsReader (Android TextToSpeech)                         │
│  reuse: PreferencesStore, PendingDownloadRepository, Connectivity │
└───────────────────────────────────────────────────────────────┘
```

### Key decisions

1. **Stored numbering is canonical (Masoretic).** Display numbering is computed per render via `PsalmDisplay`. This keeps `Verse_Reference` and annotations stable across denominations/translations (Req 8.7).
2. **One content schema for bundled and downloaded translations.** Both paths write `BookEntity/ChapterEntity/VerseEntity` rows tagged by `translationId`. Seeder and `DownloadManager` share the same insert path → no special-casing in the reader.
3. **Full-text search via Room FTS4/FTS5.** A `VerseFtsEntity` mirrors verse text for fast `MATCH` queries, satisfying the 200ms target without loading all verses into memory.
4. **Repository is the single source of truth.** The reader never calls the network or DAOs directly; it goes through `BibleContentRepository`.
5. **Downloads are transactional per translation.** Content is written under a `translationContentVersion`; a translation is marked `isDownloaded=true` only after all chapters are committed. Cancel/failure deletes partial rows (Req 5.4, 5.7, 15.4).

## Components and Interfaces

### Room entities (data.local)

```kotlin
@Entity(tableName = "books",
        primaryKeys = ["translationId", "osisId"],
        indices = [Index("translationId")])
data class BookEntity(
    val translationId: String,
    val osisId: String,          // GEN, PSA...
    val name: String,            // localized book name from source
    val testament: String,       // OLD/NEW
    val orderIndex: Int,         // source order (canon ordering applied at read time)
    val chapterCount: Int
)

@Entity(tableName = "chapters",
        primaryKeys = ["translationId", "osisId", "chapter"])
data class ChapterEntity(
    val translationId: String,
    val osisId: String,
    val chapter: Int,            // canonical (Masoretic) chapter number
    val verseCount: Int
)

@Entity(tableName = "verses",
        primaryKeys = ["translationId", "osisId", "chapter", "verse"],
        indices = [Index(value = ["translationId", "osisId", "chapter"])])
data class VerseEntity(
    val translationId: String,
    val osisId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(val text: String)
```

`TranslationEntity` (already exists) gains: `isBundled: Boolean`, `contentVersion: Int`, `verseCount: Int` (additive migration).

### DAOs

`BibleContentDao`:
- `observeBooks(translationId): Flow<List<BookEntity>>`
- `getChapter(translationId, osisId, chapter): List<VerseEntity>`
- `getChapterMeta(...)`, `getBook(...)`
- `insertBooks/insertChapters/insertVerses` (transactional `@Insert(OnConflict.REPLACE)`)
- `deleteTranslationContent(translationId)` (books+chapters+verses)
- `search(translationId, query): List<VerseEntity>` via FTS `MATCH`, joined back to verses, limited/paged.

### HelloAo remote (data.remote)

```kotlin
interface HelloAoApi {
    @GET("available_translations.json") suspend fun translations(): AvailableTranslationsDto
    @GET("{id}/books.json") suspend fun books(@Path("id") id: String): BooksDto
    @GET("{id}/{book}/{chapter}.json") suspend fun chapter(
        @Path("id") id: String, @Path("book") book: String, @Path("chapter") chapter: Int
    ): ChapterDto
}
```
- Base URL `https://bible.helloao.org/api/`, Retrofit + kotlinx.serialization converter, OkHttp with timeouts. No auth.
- `HelloAoRemoteDataSource` implements the existing `TranslationRemoteDataSource` seam: `fetchCatalog()` maps `AvailableTranslationsDto` → `List<Translation>` (deriving `canonType`/`hasDeuterocanon` from book presence), `downloadTranslation(id)` streams books+chapters (used by `DownloadManager`).

### BundledBibleSeeder (data)

- Reads `assets/bibles/manifest.json` + gzipped `*.bible.json`, parses, inserts via `BibleContentDao` in a transaction, registers/updates `TranslationEntity(isBundled=true, contentVersion=...)`.
- Idempotent: skips when the bundled `contentVersion` already present (Req 1.3, 15.2). Resumable: seeds per-translation; partial completion re-runs (Req 1.5).
- Runs off main thread (Req 13.4); triggered from app startup (e.g., a `Startup`/`WorkManager` one-shot or in `MannaApplication`/a bootstrap use case) and gated so the reader waits only if no content exists yet.

### BibleContentRepository (data)

```kotlin
interface BibleContentRepository {
    fun books(translationId: String): Flow<List<BookEntity>>
    suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent
    suspend fun hasContent(translationId: String): Boolean
    suspend fun search(translationId: String, query: String): List<VerseMatch>
}
```
`ChapterContent` = ordered verses + book/chapter meta. The repository applies nothing canon-specific; the reader composes it with `CanonBookOrdering`/`PsalmDisplay`.

### DownloadManager (data)

- `download(translationId)`: online → fetch books, then each chapter, insert transactionally, set `isDownloaded` + `contentVersion` at the end; emits progress (`Flow<DownloadProgress>`). Offline → enqueue via `PendingDownloadRepository`.
- `cancel(translationId)` / failure → `deleteTranslationContent` + leave `isDownloaded=false`.
- `delete(translationId)` → `deleteTranslationContent`, clear marker; if active, fall back to a bundled translation (Req 5.5).
- Reuses `ConnectivityChecker` and `retryPendingDownloads` wiring already present.

### Reader (ui.reader)

- `ReaderViewModel(getChapterUseCase, navigateChapterUseCase, canonEngine, preferencesStore, annotationRepository, ttsReader)` exposes `ReaderUiState` (book, displayedChapterNumber, verses with annotation flags, audio state, loading/error).
- Renders verses; verse tap → annotation bottom sheet (highlight/bookmark/note via `AnnotationRepository`). Persists `Reading_Position` on chapter view (Req 7).
- Uses `CanonBookOrdering` for next/prev/picker (canon membership + order) and `PsalmDisplay` for Psalm display numbers.

### TtsReader (domain interface + AndroidTtsReader impl)

```kotlin
interface TtsReader {
    val state: StateFlow<TtsState>      // Idle/Playing(verseIndex)/Paused
    fun play(content: ChapterContent, languageTag: String, speed: Float)
    fun pause(); fun stop(); fun setSpeed(speed: Float)   // speed 0.5..2.0
}
interface NarratedAudioProvider { /* seam; no impl in this feature */ }
```
- `AndroidTtsReader` wraps `android.speech.tts.TextToSpeech`, queues verse utterances with utteranceIds → emits current verse index (Req 9.2); resolves voice by language, falls back gracefully when unavailable (Req 9.6). Continuous-play preference checked only at natural chapter end (Req 9.7).

### SearchScriptureUseCase (domain)

- Delegates to `BibleContentRepository.search` (FTS), restricts to canon books (Req 10.2), maps results to `Verse_Reference` with displayed numbering + snippet.

## Data Models / Persistence

- New tables: `books`, `chapters`, `verses`, `verses_fts`. `TranslationEntity` extended additively.
- Room version bump with a **migration that preserves annotations and preferences** (Req 15.5). Since annotations live in separate tables keyed by verse reference, the migration only adds the new content tables (no data loss).
- `Verse_Reference` string format `OSIS.CHAPTER.VERSE` (canonical numbers) — same contract used by annotations.

## Navigation Flow

```
MannaApp gate (existing): setupCompleted → ReaderScreen (replaces MainScreen placeholder)
ReaderScreen
  ├─ top bar: book+chapter picker, search, translation switcher, overflow (attribution)
  ├─ content: verses (tap → annotate)
  ├─ bottom: audio bar (play/pause/stop/speed), prev/next chapter
  ├─ → TranslationCatalogScreen (browse/download/switch)
  └─ → SearchScreen (query → results → jump to verse)
```
First destination resolves `Reading_Position` (Req 7): persisted position, else first book/chapter of canon.

## Error Handling

| Scenario | Handling |
|---|---|
| Chapter missing for active translation | Explain + offer download/switch (Req 2.6) |
| Remote catalog fetch fails | Show bundled+downloaded immediately + retry (Req 4.6, 11.2) |
| Download fails mid-way | Report, keep pending/resumable, delete partial, not marked downloaded (Req 5.7, 15.4) |
| Download requested offline | Queue via PendingDownloadRepository, retry on reconnect (Req 5.6, 11.5) |
| No TTS voice for language | Inform + offer default voice/guidance, no crash (Req 9.6) |
| Active translation deleted | Switch to bundled translation (Req 5.5) |
| Corrupt bundled asset | Fail seeding safely, log non-PII, retry next launch (Req 1.5) |

## Testing Strategy

All tests run in **GitHub Actions CI** (`testDebugUnitTest`; instrumented where feasible). Logic is pushed into JVM-testable domain/data code.

### Unit tests (JUnit5 + Turbine + MockK)
- Verse normalization/mapping (helloao DTO → VerseEntity), canonical numbering preserved.
- `BibleContentRepository`/DAO with in-memory Room: insert, chapter fetch, delete-translation isolation, FTS search correctness + canon restriction.
- `BundledBibleSeeder`: idempotent seeding (no duplicates), resume after partial.
- `DownloadManager`: online success commits + marks downloaded; cancel/failure deletes partial + not marked; offline enqueues pending.
- `ReaderViewModel`: state emissions for open/next/prev/switch/annotate; reading-position persistence; Psalm display numbering for Septuagint.
- `SearchScriptureUseCase`: matches, canon restriction, no-results.
- `AndroidTtsReader`: verse-advance logic behind a wrapper interface (TextToSpeech faked).

### Property-based tests
- **Numbering stability**: for any stored verse, `Verse_Reference` is invariant under denomination/numbering changes; `PsalmDisplay` round-trips on the principal domain.
- **Seeding idempotence**: seeding N times yields the same row set (no duplicates).
- **Download integrity**: after cancel/failure, no rows for that translation remain and `isDownloaded` is false.
- **Search soundness**: every returned verse contains the query term and belongs to a canon book.

### Instrumented/Compose tests (CI emulator where available)
- Reader happy path renders seeded WEB chapter; next/prev navigation; annotate a verse; a11y (content descriptions, 48dp, text scale).

### CI / data-prep
- `:app:prepareBundledBibles` runs (or assets are present) before `assembleDebug`/`assembleRelease`; unit tests do not require network (use fixture JSON).

## Requirements Coverage

| Requirement | Covered by |
|---|---|
| 1 Bundled offline Bible | BundledBibleSeeder + assets pipeline + idempotent seeding |
| 2 Reading a chapter | ReaderScreen/VM + BibleContentRepository + PsalmDisplay |
| 3 Navigation | NavigateChapterUseCase + CanonBookOrdering + picker |
| 4 Catalog | HelloAoRemoteDataSource + TranslationFilter + offline-first merge |
| 5 Downloads | DownloadManager + PendingDownloadRepository + transactional writes |
| 6 Active translation | SetActiveTranslationUseCase + PreferencesStore |
| 7 Reading position | PreferencesStore lastReadPosition + RestoreReadingPositionUseCase |
| 8 Annotations | AnnotationRepository + reader integration (canonical refs) |
| 9 TTS audio | TtsReader/AndroidTtsReader + NarratedAudioProvider seam |
| 10 Search | VerseFts + SearchScriptureUseCase |
| 11 Offline/failure | Repository-first reads + ConnectivityChecker + pending queue |
| 12 Attribution | Attribution surface (WEB public domain + helloao MIT) |
| 13 Performance | Room indices + FTS + off-main seeding |
| 14 Accessibility | Compose a11y, scalable text, RTL, Simplified audio-first |
| 15 Data integrity/migration | Shared schema, versioned content, additive migration |
