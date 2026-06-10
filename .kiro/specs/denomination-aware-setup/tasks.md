# Implementation Plan

- [x] 1. Domain models and canon definition assets
- [x] 1.1 Create core domain enums and data classes
  - Add `Denomination`, `CanonType`, `NumberingScheme`, `Testament`, `CanonBook`, `CanonProfile`, and `SetupState` in `domain/model/`.
  - Pure Kotlin, no Android imports.
  - _Requirements: 3, 6, 7, 10_
- [x] 1.2 Author bundled canon definition JSON assets
  - Create `assets/canon/protestant_66.json`, `catholic_73.json`, `orthodox_expanded.json` with osisId, testament, orderIndex, deuterocanonical flag.
  - Catholic file interleaves the 7 deuterocanonical books + Esther/Daniel additions in correct OT order.
  - _Requirements: 3, 6_
- [x] 1.3 Implement CanonDefinitionDataSource to parse assets
  - Kotlinx Serialization parsing; expose `definitionFor(canonType)`.
  - Fallback to `protestant_66` on missing/corrupt asset.
  - _Requirements: 6_

- [x] 2. Canon engine and mappers (pure domain)
- [x] 2.1 Implement CanonEngine
  - `canonTypeFor(denomination)` and `profileFor(denomination, bibleLanguage)`.
  - Assign deterministic, contiguous orderIndex per testament; build `ALL_CANONS` union for SHOW_EVERYTHING.
  - Set numberingScheme: SEPTUAGINT for Catholic/Orthodox, MASORETIC otherwise.
  - _Requirements: 3, 6, 7, 16_
- [x] 2.2 Implement PsalmNumberingMapper
  - `toDisplay` / `toCanonical` for Masoretic⇄Septuagint over Psalms 9–147 (LXX joins 9–10, 114–115; splits 116, 147).
  - _Requirements: 7, 17_
- [x] 2.3* Property-based tests for CanonEngine and PsalmNumberingMapper
  - Canon size invariants (66 / 73), ordering determinism, Psalm mapping invertibility.
  - _Requirements: 3, 6, 7_

- [x] 3. Preferences persistence
- [x] 3.1 Extend PreferencesStore with setup keys
  - Add denomination, canon, numberingScheme, namingConvention, bibleTranslationId, lectionary, setupCompleted, showDeuterocanonical.
  - Implement `setupState: Flow<SetupState>`, `saveSetup`, `setSetupCompleted`, `updateDenomination`, `setShowDeuterocanonical`.
  - _Requirements: 1, 10, 15_
- [x] 3.2* Unit tests for PreferencesStore round-trip
  - In-memory DataStore; persistence idempotence property.
  - _Requirements: 10_

- [x] 4. Translation filtering and offline downloads
- [x] 4.1 Implement TranslationFilter
  - Filter catalog by language + canon compatibility; rank Catholic editions for CATHOLIC_73; closest-in-language fallback.
  - _Requirements: 5_
- [x] 4.2 Add PendingDownloadRepository and extend TranslationRepository
  - `markPendingDownload`, retry-when-online, `download` with progress; set `isDownloaded` on success.
  - _Requirements: 13_
- [x] 4.3* Unit tests for TranslationFilter
  - Filtering, ranking, and fallback cases.
  - _Requirements: 5_

- [x] 5. Lectionary association
- [x] 5.1 Implement LectionaryProvider
  - Map CSI→csi_almanac, Catholic→rc_calendar, else general/null.
  - _Requirements: 9, 18_

- [x] 6. Annotation preservation and canon-switch impact
- [x] 6.1 Extend AnnotationRepository with book-scoped queries
  - `annotationsInBooks(osisIds)`; ensure no deletion on canon change; hide annotations for books outside active canon at query/view layer.
  - _Requirements: 11, 12_
- [x] 6.2 Implement ResolveCanonSwitchImpactUseCase
  - Compute annotated books excluded by a candidate canon for the warning dialog.
  - _Requirements: 12_
- [x] 6.3* Tests for annotation preservation across canon switches
  - Property: stored annotation set unchanged across switch sequences.
  - _Requirements: 11, 12_

- [x] 7. Use cases
- [x] 7.1 Implement CompleteSetupUseCase and ApplyDenominationChangeUseCase
  - Compose CanonEngine + PreferencesStore; recompute profile on denomination change.
  - _Requirements: 2, 10, 11_

- [x] 8. Setup UI (Compose + ViewModel)
- [x] 8.1 Implement SetupViewModel with StateFlow<SetupUiState>
  - Handle selection/skip for each step; apply defaults on skip; expose completion action with error retention.
  - _Requirements: 1, 2, 4, 5, 7, 9, 10, 15_
- [x] 8.2 Build SetupHost NavHost and step screens
  - Welcome, Denomination (with descriptions + "Not sure"), UiLanguage, BibleLanguage, Translation, Lectionary (conditional), Summary.
  - Apply selected UI language immediately to subsequent steps.
  - _Requirements: 1, 2, 3, 4, 5, 9, 16_
- [x] 8.3 First-launch gate in app navigation
  - Route to SetupHost when `setupCompleted` is false, else main reader.
  - _Requirements: 1_
- [x] 8.4 Accessibility pass on setup screens
  - 48dp/56dp targets, content descriptions, 200% text scaling, RTL layout, audio-first prompts in Simplified Mode.
  - _Requirements: 14_

- [x] 9. Settings integration
- [x] 9.1 Add Denomination & Languages section to Settings
  - Re-run setup in edit mode; deuterocanonical toggle for Protestant; canon-switch warning dialog wiring.
  - _Requirements: 11, 12, 13, 15, 16_
- [x] 9.2* Compose UI tests for setup happy path, skip path, and accessibility
  - Reaches reader and sets setupCompleted; a11y assertions.
  - _Requirements: 1, 2, 14_

- [x] 10. Reader/share integration of canon profile
- [x] 10.1 Apply CanonProfile ordering and Psalm display numbering in reader
  - Reader uses CanonProfile.books ordering; display Psalm numbers via PsalmNumberingMapper.
  - _Requirements: 6, 7_
- [x] 10.2 Apply numbering scheme and naming convention to verse sharing
  - Format shared references per active scheme + naming.
  - _Requirements: 17_

- [x] 11. Wiring and CI
- [x] 11.1 Add Hilt modules/bindings for new components
  - Bind CanonEngine, TranslationFilter, LectionaryProvider, repositories, PreferencesStore.
  - _Requirements: all_
- [x] 11.2 Ensure unit tests run in GitHub Actions PR pipeline
  - Confirm `testDebugUnitTest` (incl. new domain/property tests) executes in `android-build.yml`; no local-run assumptions; secrets via GitHub Secrets.
  - _Requirements: all_
