# Design Document

## Overview

This document describes the technical design for the **denomination-aware first-launch setup** feature of Manna. The feature guides a first-time user through selecting their denomination, App UI language, and Bible text language, then derives a complete `CanonProfile` (book set, book ordering, Psalm/verse numbering scheme, proper-noun naming convention, suggested translation, and lectionary) and persists it locally. All selections are changeable later from Settings without losing user annotations.

The design follows the existing Manna conventions in `CLAUDE.md`: Clean Architecture (UI → Domain → Data), Jetpack Compose + Material 3, Room for content/user data, DataStore for preferences, Hilt for DI, type-safe Navigation Compose, and offline-first behavior. It is built to be developed, tested, and released entirely through **GitHub Actions** — there is no local developer machine; CI is the source of truth for builds and verification.

### Goals

- Map a chosen `Denomination` to a deterministic, testable `CanonProfile`.
- Keep App UI language and Bible text language fully independent.
- Complete setup with zero network; defer all downloads.
- Persist all choices to DataStore; re-runnable from Settings.
- Preserve all user annotations (highlights, bookmarks, notes) across canon/denomination/language changes, keyed by canonical verse reference.
- Meet accessibility requirements (48dp/56dp targets, 200% text scale, TalkBack, RTL, audio-first).

### Non-Goals

- Authoring or shipping full Bible text for every canon (handled by the existing translation/download pipeline). This feature ships **canon definition metadata** only (book lists, ordering, numbering rules), not scripture text.
- Full lectionary reading content authoring. This feature associates a `lectionaryId` and renders readings if lectionary data is present; bundling complete lectionary datasets is a separate content task.

## Architecture

### Layer view

```
┌──────────────────────────────────────────────────────────────┐
│ UI Layer (Compose + ViewModels)                                │
│  SetupHostScreen (NavHost for setup steps)                     │
│   ├─ WelcomeStep                                               │
│   ├─ DenominationStep                                          │
│   ├─ UiLanguageStep                                            │
│   ├─ BibleLanguageStep                                         │
│   ├─ TranslationStep                                           │
│   ├─ LectionaryStep (conditional)                              │
│   └─ SummaryStep                                               │
│  SetupViewModel  (StateFlow<SetupUiState>)                     │
│  Settings: DenominationSettingsViewModel                       │
├──────────────────────────────────────────────────────────────┤
│ Domain Layer (pure Kotlin, no Android)                         │
│  CanonEngine            : Denomination → CanonProfile          │
│  PsalmNumberingMapper   : Masoretic ⇄ Septuagint (Ps 9–147)    │
│  TranslationFilter      : catalog → compatible list + default  │
│  LectionaryProvider     : Denomination → lectionaryId          │
│  Use cases:                                                    │
│   CompleteSetupUseCase, ApplyDenominationChangeUseCase,        │
│   ResolveCanonSwitchImpactUseCase                              │
│  Models: Denomination, CanonType, CanonProfile, NumberingScheme│
├──────────────────────────────────────────────────────────────┤
│ Data Layer                                                     │
│  PreferencesStore (DataStore)                                  │
│  CanonDefinitionDataSource (bundled JSON in assets)            │
│  TranslationRepository (Room + Bible Brain via Retrofit)       │
│  AnnotationRepository (Room: Highlight/Bookmark/Note)          │
│  PendingDownloadRepository (Room/DataStore)                    │
└──────────────────────────────────────────────────────────────┘
```

### Key design decisions

1. **Canon definitions are bundled static metadata, not code.** Book lists, testament grouping, `orderIndex`, and numbering scheme per `CanonType` are stored as JSON assets (`assets/canon/*.json`) parsed by `CanonDefinitionDataSource`. This keeps `CanonEngine` pure and the data auditable/testable without recompiling logic.
2. **`CanonProfile` is the single derived object.** Every downstream consumer (reader, search, share, lectionary) reads from the persisted `CanonProfile` rather than re-deriving from `Denomination`.
3. **Annotations are verse-reference-based.** Highlights/bookmarks/notes already reference verses via stable OSIS-style IDs (e.g., `GEN.1.1`). Canon switches only change which books are *visible*; annotation rows are never deleted on canon change, satisfying Requirements 11 & 12.
4. **Psalm numbering is a presentation-layer mapping.** Underlying verse storage uses one canonical numbering (Masoretic); `PsalmNumberingMapper` converts to display numbers when the scheme is `septuagint`. This avoids duplicating Psalm data per tradition.
5. **CI-only verification.** All tests run in GitHub Actions (`testDebugUnitTest`). The design favors JVM-testable pure domain logic (CanonEngine, mappers, filters) so the bulk of correctness is verified without a device.

## Components and Interfaces

### Domain models

```kotlin
enum class Denomination(val id: String) {
    CATHOLIC("catholic"),
    CSI("csi"),
    PROTESTANT_OTHER("protestant_other"),
    ORTHODOX("orthodox"),
    MAR_THOMA("mar_thoma"),
    SHOW_EVERYTHING("show_everything")
}

enum class CanonType(val id: String) {
    PROTESTANT_66("protestant_66"),
    CATHOLIC_73("catholic_73"),
    ORTHODOX_EXPANDED("orthodox_expanded"),
    ALL_CANONS("all_canons")
}

enum class NumberingScheme { MASORETIC, SEPTUAGINT }

data class CanonBook(
    val osisId: String,        // e.g. "GEN", "TOB"
    val testament: Testament,  // OLD, NEW
    val orderIndex: Int,
    val isDeuterocanonical: Boolean
)

data class CanonProfile(
    val denomination: Denomination,
    val canonType: CanonType,
    val books: List<CanonBook>,         // ordered
    val numberingScheme: NumberingScheme,
    val namingConventionId: String?,    // null → translation default
    val suggestedTranslationId: String?,
    val lectionaryId: String?
)
```

### CanonEngine (Domain)

```kotlin
interface CanonEngine {
    fun canonTypeFor(denomination: Denomination): CanonType
    suspend fun profileFor(
        denomination: Denomination,
        bibleLanguage: String
    ): CanonProfile
}
```

- Reads bundled canon definitions via `CanonDefinitionDataSource`.
- Determines `numberingScheme`: `SEPTUAGINT` for Catholic/Orthodox, `MASORETIC` otherwise (Req 7).
- Assigns deterministic `orderIndex`, interleaving deuterocanonical books for `CATHOLIC_73` (Req 6).
- `SHOW_EVERYTHING` → `ALL_CANONS` = union of all book sets, labeled by tradition (Req 16).

### PsalmNumberingMapper (Domain)

```kotlin
object PsalmNumberingMapper {
    // Masoretic → Septuagint display number (and inverse)
    fun toDisplay(scheme: NumberingScheme, masoreticPsalm: Int): Int
    fun toCanonical(scheme: NumberingScheme, displayPsalm: Int): Int
}
```

- For `SEPTUAGINT`: Psalms 1–8 and 148–150 unchanged; 9–147 region uses the documented offset (LXX joins Masoretic 9–10 and 114–115, splits 116 and 147). The function is the inverse of `toCanonical` over the valid domain — a property to be tested.

### TranslationFilter (Domain)

```kotlin
interface TranslationFilter {
    fun filter(catalog: List<Translation>, profile: CanonProfile, bibleLanguage: String): List<Translation>
    fun suggestedDefault(catalog: List<Translation>, profile: CanonProfile, bibleLanguage: String): Translation?
}
```

- Filters by language + canon compatibility; ranks Catholic editions above non-deuterocanonical ones for `CATHOLIC_73` (Req 5.4).
- Fallback to closest in-language translation when none match exactly (Req 5.6).

### LectionaryProvider (Domain)

```kotlin
interface LectionaryProvider {
    fun lectionaryIdFor(denomination: Denomination): String?  // CSI→"csi_almanac", Catholic→"rc_calendar", else general/null
}
```

### PreferencesStore (Data)

DataStore Preferences keys (extends existing `uiLanguage`, `bibleLanguage`):

| Key | Type | Notes |
|-----|------|-------|
| `denomination` | String | Denomination.id |
| `canon` | String | CanonType.id |
| `numberingScheme` | String | masoretic / septuagint |
| `namingConvention` | String? | nullable |
| `bibleTranslationId` | String? | selected translation |
| `lectionary` | String? | lectionary id |
| `setupCompleted` | Boolean | gates first-launch (Req 1) |
| `showDeuterocanonical` | Boolean | Protestant toggle (Req 15) |

```kotlin
interface PreferencesStore {
    val setupState: Flow<SetupState>
    suspend fun saveSetup(state: SetupState)
    suspend fun setSetupCompleted(value: Boolean)
    suspend fun updateDenomination(profile: CanonProfile)
    suspend fun setShowDeuterocanonical(value: Boolean)
}
```

### Repositories (Data)

- `TranslationRepository`: exposes catalog (cached Room table, refreshed from Bible Brain when online), `download(translationId)`, and `markPendingDownload(translationId)` for offline (Req 13).
- `AnnotationRepository`: queries Highlight/Bookmark/Note by verse reference; `annotationsInBooks(osisIds)` used to compute canon-switch impact (Req 12). Never deletes on canon change.

## Data Models / Persistence

### Bundled canon definitions

`assets/canon/protestant_66.json`, `catholic_73.json`, `orthodox_expanded.json` — each:

```json
{
  "canonType": "catholic_73",
  "numberingScheme": "septuagint",
  "books": [
    { "osisId": "GEN", "testament": "OLD", "orderIndex": 0, "deuterocanonical": false },
    { "osisId": "TOB", "testament": "OLD", "orderIndex": 17, "deuterocanonical": true }
  ]
}
```

`ALL_CANONS` is computed at runtime as the union (no separate file), preserving source-canon labels.

### Room considerations

- No destructive schema change required for annotations; they remain keyed by verse reference.
- A small `pending_downloads` table (or DataStore set) records translations requested while offline.
- If `Book.orderIndex` is currently translation-scoped, reading-layer ordering should prefer `CanonProfile.books[].orderIndex` so book order follows the active canon rather than the downloaded translation. Migration is additive only.

## Navigation Flow

```
App start
  └─ setupCompleted? ── true ──▶ Main reading interface
          │ false
          ▼
  SetupHost: Welcome → Denomination → UiLanguage → BibleLanguage
            → Translation → [Lectionary if available] → Summary
          │ (Skip available on each step → applies defaults, Req 2)
          ▼
  CompleteSetupUseCase → PreferencesStore.saveSetup + setSetupCompleted(true)
          │ success ──▶ Main reading interface
          │ failure ──▶ stay on Summary, show error, retain selections (Req 15.4 / general error handling)
```

Settings re-entry: `Settings → "Setup / Denomination & Languages"` launches the same SetupHost in "edit" mode; `ApplyDenominationChangeUseCase` recomputes the profile and, if the new canon hides annotated books, surfaces a confirmation dialog (Req 12.1) before persisting.

## Error Handling

| Scenario | Handling |
|----------|----------|
| Persistence failure on completion | Retain on-screen selections; show error; allow retry (Req 15.4) |
| No translation matches language+canon | Offer closest in-language translation (Req 5.6) |
| Download requested offline | Mark pending; complete setup; retry when online (Req 13.1) |
| Download fails | Inform user; offer retry (Req 13.4) |
| Canon switch hides annotated books | Warn before applying; retain annotations in storage (Req 12) |
| Corrupt/missing canon asset | Fall back to `protestant_66` safe default; log non-PII diagnostic |

## Testing Strategy

All tests execute in **GitHub Actions CI** via `./gradlew testDebugUnitTest` (unit/domain) and `connectedDebugAndroidTest` where an emulator is available in CI. No local runs are assumed. The design pushes correctness into pure JVM-testable domain code.

### Unit tests (JUnit 5 + Turbine)

- `CanonEngine`: each Denomination → expected CanonType and book counts.
- `SetupViewModel`: StateFlow emissions for every selection/skip path.
- `TranslationFilter`: language+canon filtering, Catholic ranking, fallback.
- `PreferencesStore`: round-trip persistence (in-memory DataStore).
- `AnnotationRepository`: annotations preserved across simulated canon switches.

### Property-based tests (correctness properties)

1. **Canon size invariant**: `PROTESTANT_66` → 66 books (39 OT + 27 NT); `CATHOLIC_73` → 73 including the 7 deuterocanonical + Esther/Daniel additions. (Req 3, 6)
2. **Psalm mapping invertibility**: for all valid Psalm n, `toCanonical(scheme, toDisplay(scheme, n)) == n`. (Req 7)
3. **Ordering determinism**: `profileFor(d, lang).books.map{orderIndex}` is strictly increasing and contiguous per testament. (Req 6)
4. **Annotation preservation**: for any sequence of canon switches, the set of stored annotation rows is unchanged (only visibility changes). (Req 11, 12)
5. **Persistence idempotence**: `saveSetup(s)` then read yields a `SetupState` equal to `s`. (Req 10)

### UI tests (Compose Test)

- Full setup happy path + skip path reach the reader and set `setupCompleted`.
- Accessibility: every interactive control asserts a content description; touch targets ≥ 48dp (≥ 56dp in Simplified Mode); RTL layout for RTL UI language.

### CI integration

- Add the above to the existing `android-build.yml` PR pipeline (lint + unit tests + assembleDebug).
- Release/AAB builds remain in `android-release.yml`; Bible Brain API key and keystore injected from GitHub Secrets at CI time (no secrets in source).

## Requirements Coverage

| Requirement | Covered by |
|-------------|-----------|
| 1 First-launch detection | `setupCompleted` key, NavHost gate |
| 2 Skippable defaults | Skip control per step, `SHOW_EVERYTHING`/`ALL_CANONS` defaults |
| 3 Denomination selection | DenominationStep, CanonEngine.canonTypeFor |
| 4 UI language | UiLanguageStep, `uiLanguage` key |
| 5 Bible language & translation | BibleLanguageStep, TranslationStep, TranslationFilter |
| 6 Canon & ordering | CanonEngine + bundled JSON, orderIndex |
| 7 Numbering scheme | CanonEngine, PsalmNumberingMapper |
| 8 Naming convention | `namingConvention` key, CanonProfile |
| 9 Lectionary selection | LectionaryProvider, LectionaryStep |
| 10 Persistence | PreferencesStore |
| 11 Re-run without data loss | Settings edit mode, AnnotationRepository |
| 12 Canon switch warning | ResolveCanonSwitchImpactUseCase, dialog |
| 13 Offline download | PendingDownloadRepository, TranslationRepository |
| 14 Accessible/dual-language screens | Compose a11y, RTL, audio-first |
| 15 Deuterocanonical toggle | `showDeuterocanonical` key |
| 16 Show-everything mode | `ALL_CANONS` union |
| 17 Share references | PsalmNumberingMapper + naming on share |
| 18 Daily lectionary readings | LectionaryProvider + lectionary data |
