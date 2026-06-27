# Implementation Plan: Mass / Liturgy Guide & Bible-Language Localization

## Overview

This plan converts the approved design into ordered, dependency-aware, incremental coding
tasks for the **Manna** Android app (Kotlin · Jetpack Compose · Hilt · Room; AGP 8.7.3 /
Gradle 8.11.1 / Kotlin 2.1.0 / compileSdk 35; tests JUnit 5 Jupiter + MockK + Turbine,
Robolectric/Compose for rendering; build with JDK 17). Every change is production-grade —
no scaffolds, no stubs.

Two coupled concerns are delivered:

- **Concern 1 — Bible-language localization:** migrate the six PR #80 AI surfaces (Crisis AI,
  Sermon Builder, Verse Cards, Persecution Comfort, Cultural Lens, Oral AI) to resolve static
  user-facing strings through `stringResourceIn(bibleLanguage, …)`; ship `ta/te/hi/ml`
  translations; add a translation provenance manifest plus an automated inventory/gap +
  placeholder-preservation check (with human-review override) and a source-scan guard.
- **Concern 2 — Mass / Liturgy Guide:** extend the domain model with `LocalizedText`, move
  the two hardcoded liturgies into bundled JSON assets (verbatim), add the reader/parser/
  repository/provider data path, build the Library + Detail surfaces, and rewire the
  `FeatureFlags.CHURCH_MODE`-gated entry point.

**Sequencing strategy.** The domain model is migrated first in a way that keeps the existing
`DefaultLiturgyProvider` and `ChurchModeScreen` compiling (its `String` content becomes the
`"en"` entry of each `LocalizedText`), so the build stays green at every step. The bundled
assets + migration fidelity test land and pass **before** `DefaultLiturgyProvider` is removed,
guaranteeing the content policy and provenance survive the move. The two concerns touch
disjoint files and can progress in parallel; intra-file ordering is captured in the Task
Dependency Graph.

**Property-based testing is required.** All 18 Correctness Properties from `design.md` are
implemented as property-based tests (Kotest `io.kotest.property`, ≥100 iterations each), each
tagged with the exact comment `// Feature: mass-liturgy-and-localization, Property {n}: {text}`.
Pure-domain properties run on the JVM; rendering properties run under Robolectric/Compose;
manifest properties run against a generated manifest + in-memory asset map.

> Test sub-tasks marked with `*` (plain unit/example/integration tests) are optional and may be
> skipped for a faster MVP. Property-based test sub-tasks, the automated inventory/placeholder
> check, the source-scan guard, the migration fidelity test, and the feature-flag gating test
> are **required deliverables** of the requirements and are not marked optional.

---

## Tasks

- [ ] 1. Test & build infrastructure for PBT and rendering tests
  - [ ] 1.1 Add Kotest property testing and Robolectric to the unit-test classpath
    - In `gradle/libs.versions.toml` add version + library aliases for `io.kotest:kotest-property` and `io.kotest:kotest-assertions-core` (a stable 5.x release), `org.robolectric:robolectric`, and a JUnit 5 Robolectric extension (`tech.apter.junit5.robolectric:robolectric-extension`) compatible with the existing `useJUnitPlatform()` setup.
    - In `app/build.gradle.kts` add them as `testImplementation`, add the existing `androidx.compose.ui:ui-test-junit4` / `ui-test-manifest` as `testImplementation` so Compose rendering tests can run under Robolectric, and set `testOptions.unitTests.isIncludeAndroidResources = true` so `values-*` resources and `assets/` are visible to JVM tests.
    - Do NOT change `compileSdk`/`jvmTarget`/Kotlin `languageVersion` — keep JDK 17 / `1.9` metadata as configured.
    - _Requirements: supports test strategy for 1–14; Properties 1–18_

- [ ] 2. Extend the liturgy domain model with multilingual content (Concern 2 foundation)
  - [ ] 2.1 Introduce `LocalizedText` and convert liturgy content fields; extend `LiturgyProvider`
    - In `app/src/main/java/com/manna/bible/domain/liturgy/Liturgy.kt` add `data class LocalizedText(private val values: Map<String, String>)` with `english` (required, `init { require(values.containsKey("en")) }`), `resolve(languageTag): String = values[tag] ?: english`, and `languages: Set<String>`.
    - Convert `LiturgyPart.title/text/rubric`, `LiturgySection.title`, and `Liturgy.title/sourceNote` to `LocalizedText` (nullable where currently nullable); add `Liturgy.denominations: List<Denomination> = emptyList()` and `Liturgy.languages: Set<String> = setOf("en")`.
    - Extend the `LiturgyProvider` interface with `forDenomination(d): List<Liturgy>` and `resolvedDefaultFor(d): Liturgy?`, providing interface default implementations (`resolvedDefaultFor = defaultFor(d) ?: all().firstOrNull()`; `forDenomination` default = mapped-first-then-rest using `defaultFor`) so existing implementers keep compiling.
    - Keep `DefaultLiturgyProvider` compiling by wrapping each existing `String` literal in `LocalizedText(mapOf("en" to "…"))` via the small builder helpers (preserve every value, `osisRef`, `needsOfficialText`, and both `sourceNote`s verbatim).
    - Update `ui/church/ChurchModeScreen.kt` and `ui/church/ChurchModeViewModel.kt` references to read the new `LocalizedText` fields (resolve via `rememberBibleLanguage()` / `.english`) so the module still builds; these screens are removed later in task 9.
    - _Requirements: 8.1, 8.2, 12.4_
  - [ ]* 2.2 Property test: `LocalizedText` fallback determinism
    - `// Feature: mass-liturgy-and-localization, Property 7: For any LocalizedText and any requested language tag, resolve(tag) returns the authored value when present and the English value otherwise; English is always present.`
    - JVM Kotest property, ≥100 iterations, `Arb` over maps containing `"en"` plus a random subset of `ta/te/hi/ml`.
    - _Validates: Requirements 1.4, 8.1, 8.2 (Property 7)_
  - [ ]* 2.3 Unit tests for `LocalizedText`
    - Assert the `init` requires `"en"` (throws otherwise), `resolve` returns authored vs English fallback, `languages` reflects keys.
    - _Requirements: 8.1, 8.2_

- [ ] 3. Liturgy serialization DTOs, mapper, and validation (data layer)
  - [ ] 3.1 Create `@Serializable` DTOs in `app/src/main/java/com/manna/bible/data/liturgy/`
    - Add `LiturgyManifestDto`, `LiturgyManifestEntryDto`, `LiturgyDto`, `LiturgySectionDto`, `LiturgyPartDto` exactly per the design schema (`Map<String,String>` for localized fields; `role: String`; optional `osisRef`; `needsOfficialText` default false; `languages` default `["en"]`).
    - _Requirements: 9.4, 10.1_
  - [ ] 3.2 Implement `LiturgyMapper` (parse / serialize / validate)
    - In `data/liturgy/LiturgyMapper.kt` (pure JVM, no `AssetManager`), mirror `CanonDefinitionMapper`: `parse(json, raw): Liturgy` (DTO→domain, `Map`→`LocalizedText`, unknown role / missing `en` / blank id → throw), `serialize(json, liturgy): String` (domain→DTO JSON), and `validate(dto): List<String>` enforcing non-blank `id/title/tradition/sourceNote`, every `LocalizedText` has `"en"`, declared `languages` set equals languages actually present across parts, and roles parse to `LiturgyRole`.
    - _Requirements: 10.1, 10.3, 10.5_
  - [ ] 3.3 Property test: serialization round-trip
    - `// Feature: mass-liturgy-and-localization, Property 1: For any valid Liturgy, serializing to asset JSON and parsing the result produces an equivalent Liturgy (same id, title, tradition, source note, denominations, languages, and same sections/parts — role, localized title/text/rubric, osisRef, needsOfficialText — in the same order).`
    - JVM Kotest property, ≥100 iterations, `Arb<Liturgy>` generator (random sections/parts, roles, `LocalizedText` with `en` + random `ta/te/hi/ml` subset, optional `osisRef`, random `needsOfficialText`).
    - _Validates: Requirements 10.3 (Property 1)_
  - [ ] 3.4 Property test: parse preserves authored order
    - `// Feature: mass-liturgy-and-localization, Property 2: For any valid liturgy asset, parsing yields its sections in authored order and, within each section, its parts in authored order.`
    - JVM Kotest property, ≥100 iterations.
    - _Validates: Requirements 6.1, 6.2, 10.1 (Property 2)_
  - [ ] 3.5 Property test: language/content consistency
    - `// Feature: mass-liturgy-and-localization, Property 6: For any liturgy asset, validation flags it inconsistent if and only if the set of declared languages differs from the set of languages actually present across its parts' localized fields.`
    - JVM Kotest property, ≥100 iterations (generate matching and deliberately mismatched language sets).
    - _Validates: Requirements 10.5 (Property 6)_
  - [ ]* 3.6 Unit tests for `validate` / `parse` error cases
    - Missing `en` key, unknown role token, blank id → descriptive validation error / parse throw.
    - _Requirements: 10.1, 10.5_

- [ ] 4. Bundled liturgy assets + migration fidelity
  - [ ] 4.1 Create the bundled JSON assets under `app/src/main/assets/liturgy/`
    - `manifest.json` indexing both orders (`id`, `title`, `tradition`, `denominations`, `languages`, `assetFile`), `roman_catholic_mass.json`, and `csi_holy_communion.json`.
    - Transcribe `ROMAN_CATHOLIC_MASS` and `CSI_HOLY_COMMUNION` from `DefaultLiturgyProvider.kt` **verbatim** into the `"en"` key of each `LocalizedText`: preserve every section/part order, all role values, the `osisRef = "1CO.11.23"` Words of Institution, every `needsOfficialText` flag, and both `sourceNote`s word-for-word.
    - _Requirements: 9.1, 9.4, 12.1, 12.2, 12.4_
  - [ ] 4.2 Migration fidelity test (must pass BEFORE `DefaultLiturgyProvider` removal)
    - In `app/src/test/.../data/liturgy/` parse `roman_catholic_mass.json` and `csi_holy_communion.json` via `LiturgyMapper` and assert structural + content equality (section/part order, roles, `needsOfficialText`, `osisRef`, English text/titles/rubrics, source notes) against the legacy `DefaultLiturgyProvider` constants (compare resolved `"en"` values).
    - Freeze the expected legacy structure into the test (e.g. a captured fixture) so the assertion remains valid after `DefaultLiturgyProvider` is removed in task 9.
    - _Requirements: 12.1, 12.2, 12.4_
  - [ ]* 4.3 Asset-presence smoke test
    - Assert `assets/liturgy/manifest.json` plus both `{id}.json` files exist and the manifest indexes both ids.
    - _Requirements: 9.1, 9.4_

- [ ] 5. Liturgy asset reader & repository (offline data path)
  - [ ] 5.1 Implement `LiturgyAssetReader` mirroring `AssetCanonDefinitionDataSource`
    - In `data/liturgy/LiturgyAssetReader.kt` (`@Singleton`, `@ApplicationContext` + injected `Json`): `manifest(): LiturgyManifest?` (reads `assets/liturgy/manifest.json`, returns `null` on `FileNotFoundException`/`IOException`) and `readAll(): List<LiturgyParseResult>` that reads ONLY the manifest-listed `{assetFile}` entries, each via `runCatching` yielding `LiturgyParseResult.Success(liturgy)` or `LiturgyParseResult.Failure(id, message)`; all I/O + decode on `Dispatchers.IO`.
    - Add the `LiturgyParseResult` sealed type.
    - _Requirements: 9.2, 9.3, 10.2, 10.4_
  - [ ] 5.2 Implement `LiturgyRepository` (parse-once cache)
    - In `data/liturgy/LiturgyRepository.kt` (`@Singleton`): `suspend fun liturgies(): List<Liturgy>` that loads via the reader once, caches in an `AtomicReference`, and exposes only `Success` entries (drops failures so one bad asset never removes the others).
    - _Requirements: 10.2_
  - [ ] 5.3 Property test: manifest-only loading
    - `// Feature: mass-liturgy-and-localization, Property 4: For any Liturgy_Manifest, the set of asset files the reader opens is a subset of the assetFile values listed in that manifest (no unlisted file is loaded).`
    - Kotest property, ≥100 iterations, against a generated manifest + in-memory asset map (record which keys the reader requests).
    - _Validates: Requirements 9.3, 10.4 (Property 4)_
  - [ ] 5.4 Property test: malformed assets fail safely
    - `// Feature: mass-liturgy-and-localization, Property 3: For any input that is malformed JSON or fails schema validation, the parser/reader returns a descriptive failure result and does not throw, and the resulting Liturgy_Library excludes that entry while retaining all valid entries.`
    - Kotest property, ≥100 iterations mixing valid + malformed assets; assert reader never throws and repository retains exactly the valid set.
    - _Validates: Requirements 10.2 (Property 3)_
  - [ ] 5.5 Property test: manifest completeness
    - `// Feature: mass-liturgy-and-localization, Property 5: For any bundled liturgy referenced by the library, its manifest entry carries a non-blank id, title, tradition, a denominations field, and a non-empty languages list including en.`
    - Kotest property, ≥100 iterations over generated manifests.
    - _Validates: Requirements 9.4 (Property 5)_
  - [ ]* 5.6 Unit tests for `LiturgyAssetReader`
    - Missing manifest → `null` / empty library; a manifest-listed file missing or corrupt → `Failure` with descriptive message, others retained; no network collaborator exists.
    - _Requirements: 9.2, 9.3, 10.2_

- [ ] 6. Asset-backed `LiturgyProvider` and Hilt rebinding
  - [ ] 6.1 Implement `AssetLiturgyProvider`
    - In `domain/liturgy/AssetLiturgyProvider.kt` (`@Singleton`, holds the parsed liturgies from `LiturgyRepository`) with the static `MAPPING: Map<Denomination, List<String>>` table from the design; implement `all()`, `forDenomination()` (mapped-and-available first, then remainder; `SHOW_EVERYTHING` → `all()`), `defaultFor()`, and `resolvedDefaultFor()` (`defaultFor(d) ?: all().firstOrNull()`).
    - _Requirements: 5.1, 5.3, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  - [ ] 6.2 Rebind in `BindingsModule` and remove `DefaultLiturgyProvider`
    - Change `bindLiturgyProvider` in `di/BindingsModule.kt` from `DefaultLiturgyProvider` to `AssetLiturgyProvider`; ensure the injected `Json` is available (reuse the existing provider used by canon). Delete `DefaultLiturgyProvider.kt` only after task 4.2 is green.
    - _Requirements: 11.1, 11.2, 11.4_
  - [ ] 6.3 Property test: denomination mapping ordering & completeness
    - `// Feature: mass-liturgy-and-localization, Property 14: For any Denomination and any bundled library, forDenomination returns, as a prefix, exactly the available liturgies mapped to that denomination, followed by the remaining available liturgies, and the returned set equals the full set of available liturgies (SHOW_EVERYTHING selects the entire library).`
    - JVM Kotest property, ≥100 iterations, `Arb<Denomination>` + `Arb<List<Liturgy>>`.
    - _Validates: Requirements 5.1, 5.3, 11.1, 11.5 (Property 14)_
  - [ ] 6.4 Property test: default resolution is total with fallback
    - `// Feature: mass-liturgy-and-localization, Property 15: For any Denomination, whenever the library has at least one valid liturgy, resolvedDefaultFor returns a non-null selectable liturgy (falling back to an available entry when the mapped default is absent), while the full listing still exposes every available liturgy.`
    - JVM Kotest property, ≥100 iterations.
    - _Validates: Requirements 11.3, 11.6 (Property 15)_
  - [ ]* 6.5 Unit tests for default mapping
    - `CATHOLIC → roman_catholic_mass`; `CSI`/`PROTESTANT_OTHER → csi_holy_communion`; unmapped denomination falls back yet `all()` lists everything.
    - _Requirements: 11.2, 11.3, 11.4, 11.6_

- [ ] 7. Liturgy Library surface (browse)
  - [ ] 7.1 Implement `LiturgyLibraryViewModel`
    - In `ui/church/LiturgyLibraryViewModel.kt`: combine `PreferencesStore.setupState` (denomination) with `provider.forDenomination(...)`; expose `LiturgyLibraryUiState(isLoading, entries: List<LiturgyListItem>, denominationHasMapping)`; populate entirely from bundled assets (no network).
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 9.2, 9.3_
  - [ ] 7.2 Implement `LiturgyLibraryScreen`
    - In `ui/church/LiturgyLibraryScreen.kt`: a `LazyColumn` of cards showing each liturgy `title` + `tradition` (Bible-language framing via `stringResourceIn`), mapped entries first, a calm "an order for your tradition is being prepared" note above the still-complete list when `denominationHasMapping` is false, row tap navigates to detail. Interactive rows ≥48dp (≥56dp when `PreferencesStore.simplifiedMode`), content descriptions, `sp` text, RTL-aware.
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 14.1, 14.2, 14.3, 14.4, 14.5_
  - [ ] 7.3 Property test: library rows contain title and tradition (rendering)
    - `// Feature: mass-liturgy-and-localization, Property 9: For any listed entry in Liturgy_Library, the row contains its title and tradition.`
    - Robolectric/Compose Kotest property, ≥100 iterations over generated entries; assert each row node exposes title + tradition.
    - _Validates: Requirements 5.2 (Property 9, library portion)_
  - [ ]* 7.4 Unit/rendering tests for the library
    - "Being prepared" note + full list for an unmapped denomination; offline (no connectivity) still shows the full list.
    - _Requirements: 5.4, 5.5, 9.2_
  - [ ]* 7.5 `LiturgyLibraryViewModel` tests (Turbine)
    - Denomination-ordered entries; `denominationHasMapping` flag transitions.
    - _Requirements: 5.3, 5.4_

- [ ] 8. Liturgy Detail surface (expanded order of service)
  - [ ] 8.1 Implement `LiturgyDetailViewModel`
    - In `ui/church/LiturgyDetailViewModel.kt`: load a liturgy by id from the provider/repository and observe `setupState.bibleLanguage`; expose `LiturgyDetailUiState(isLoading, liturgy, bibleLanguage)`; no network.
    - _Requirements: 6.1, 8.5, 9.2, 9.3_
  - [ ] 8.2 Implement `LiturgyDetailScreen` (extends the proven `ChurchModeScreen` rendering)
    - In `ui/church/LiturgyDetailScreen.kt`: render sections in authored order as headings, parts in authored order; show part titles when present and spoken text via `text.resolve(bibleLanguage)` with English fallback; role labels for `PRESIDER/PEOPLE/ALL/READER` via `stringResourceIn(bibleLanguage, R.string.church_role_*)`, color-differentiated via the existing `roleColor()`; `RUBRIC` rendered as muted italic instruction with NO speaker label even when its text references roles; `needsOfficialText` parts show `R.string.church_needs_official` and never reproduce fabricated proper-prayer text; parts with `osisRef` expose a "read in context" action calling the existing `onOpenVerse(osisRef)`; source note rendered at the foot via `sourceNote.resolve(bibleLanguage)`; ALL framing strings resolved through `stringResourceIn(bibleLanguage, …)` even while content is on English fallback; accessibility (≥48/56dp, content descriptions, `sp`, RTL mirror via `LocalLayoutDirection`).
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 12.3, 12.5, 14.1, 14.2, 14.3, 14.4, 14.5_
  - [ ] 8.3 Property test: source note present and rendered
    - `// Feature: mass-liturgy-and-localization, Property 8: For any valid Liturgy, its resolved source note is non-blank and appears on the rendered Liturgy_Detail.`
    - Robolectric/Compose Kotest property, ≥100 iterations.
    - _Validates: Requirements 12.4, 12.5 (Property 8)_
  - [ ] 8.4 Property test: rendered surfaces contain resolved content
    - `// Feature: mass-liturgy-and-localization, Property 9: For any Liturgy rendered in Liturgy_Detail, every section heading contains its resolved title, every titled part displays its resolved title, and every part with spoken text displays its resolved text.`
    - Robolectric/Compose Kotest property, ≥100 iterations.
    - _Validates: Requirements 6.3, 6.4, 6.5 (Property 9, detail portion)_
  - [ ] 8.5 Property test: role-label totality
    - `// Feature: mass-liturgy-and-localization, Property 10: For any Liturgy_Part, a speaker role label is assigned exactly when the role is PRESIDER, PEOPLE, ALL, or READER; a RUBRIC part receives no speaker label and is styled as an instruction, regardless of role-referencing phrasing in its text.`
    - Robolectric/Compose Kotest property, ≥100 iterations.
    - _Validates: Requirements 7.1, 7.2 (Property 10)_
  - [ ] 8.6 Property test: speaking roles are visually distinct
    - `// Feature: mass-liturgy-and-localization, Property 11: For any two distinct roles from PRESIDER, PEOPLE, ALL, READER, the role-color mapping is defined for each and differs between them.`
    - JVM Kotest property over the `roleColor()` mapping, ≥100 iterations across role pairs.
    - _Validates: Requirements 7.3 (Property 11)_
  - [ ] 8.7 Property test: official-text parts show the notice and never fabricate
    - `// Feature: mass-liturgy-and-localization, Property 12: For any Liturgy_Part with needsOfficialText = true, Liturgy_Detail displays the official-text notice directing the user to the parish book and does not present a fabricated proper-prayer body as authoritative text.`
    - Robolectric/Compose Kotest property, ≥100 iterations.
    - _Validates: Requirements 7.4, 12.2 (Property 12)_
  - [ ] 8.8 Property test: scripture references are openable
    - `// Feature: mass-liturgy-and-localization, Property 13: For any Liturgy_Part with a non-null osisRef, Liturgy_Detail exposes an action that, when invoked, calls onOpenVerse with exactly that osisRef.`
    - Robolectric/Compose Kotest property, ≥100 iterations; capture the `onOpenVerse` argument.
    - _Validates: Requirements 7.5 (Property 13)_
  - [ ]* 8.9 Unit/rendering tests for detail behavior
    - Detail persists until back; RTL layout direction for an RTL tag; touch targets ≥48dp (≥56dp Simplified Mode); content descriptions present; English fallback while framing stays in Bible language.
    - _Requirements: 6.6, 8.2, 8.3, 14.2, 14.3, 14.4, 14.5_

- [ ] 9. Navigation routes, entry point, and Church Mode replacement
  - [ ] 9.1 Add liturgy routes and rewire the gated entry point
    - In `ui/MannaApp.kt` add `Routes.LITURGY_LIBRARY` and `Routes.LITURGY_DETAIL` (carrying the liturgy `id` as a nav argument); register the library + detail composables wiring `onOpenVerse` to the existing `openInReader(ref, false)` pathway; change the existing `FeatureFlags.CHURCH_MODE`-gated `onOpenChurch` entry (and any `Routes.CHURCH` registration) to navigate to `LITURGY_LIBRARY`; library→detail and back→library navigation; remove the now-superseded `ChurchModeScreen.kt` / `ChurchModeViewModel.kt` (and their import) — keep all gating behavior (hidden when `CHURCH_MODE = false`).
    - _Requirements: 6.6, 6.7, 7.5, 13.1, 13.2, 13.3, 13.4_
  - [ ] 9.2 Feature-flag gating test
    - Assert the liturgy entry point / routes are absent when `CHURCH_MODE = false` and present when `true`, gated through `FeatureFlags`.
    - _Requirements: 13.1, 13.3, 13.4_

- [ ] 10. Checkpoint — Concern 2 builds and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Migrate the six PR #80 AI surfaces to Bible-language resolution (Concern 1)
  - [ ] 11.1 Migrate Crisis AI + Persecution Comfort surface
    - In `ui/crisis/CrisisModeScreen.kt` resolve every static user-facing string in `CrisisAiSection`, `PersecutionSection`, and the screen chrome via `val bibleLanguage = rememberBibleLanguage()` + `stringResourceIn(bibleLanguage, …)` instead of `stringResource(…)`.
    - _Requirements: 1.1, 1.2, 2.4_
  - [ ] 11.2 Migrate Sermon Builder surface
    - In `ui/sermon/SermonHelperScreen.kt` migrate static AI strings to `stringResourceIn(bibleLanguage, …)`.
    - _Requirements: 1.1, 1.2, 2.4_
  - [ ] 11.3 Migrate Verse Cards surfaces
    - In `ui/card/ScriptureCardScreen.kt`, `ui/card/VerseRecommendationScreen.kt`, and `ui/card/VerseCardSheet.kt` migrate static AI strings to `stringResourceIn(bibleLanguage, …)`.
    - _Requirements: 1.1, 1.2, 2.4_
  - [ ] 11.4 Migrate Cultural Lens + Oral AI surface (reader explanation)
    - In `ui/reader/ReaderScreen.kt` migrate the explanation/Oral-AI static strings (`explain_*`, including `explain_speak`, `explain_stop_speaking`, `explain_voice_unavailable`) to `stringResourceIn(bibleLanguage, …)`.
    - _Requirements: 1.1, 1.2, 2.4_
  - [ ] 11.5 Source-scan guard test (required)
    - Add a JVM test that scans the source of the six migrated surfaces (and `LiturgyDetailScreen` framing) and asserts they contain no raw `stringResource(` calls for static user-facing strings (use `stringResourceIn`).
    - _Requirements: 1.2, 8.4_

- [ ] 12. Ship `ta/te/hi/ml` translations for the AI feature strings
  - [ ] 12.1 Add translated resources for every AI Feature String in all four Bible locales
    - Create the corresponding `strings_crisis.xml`, `strings_sermon.xml`, `strings_card.xml`, `strings_church.xml`, and the crisis/sermon/card/explain entries of `strings.xml` under `values-ta/`, `values-te/`, `values-hi/`, and `values-ml/`, preserving every placeholder token (`%1$s`, `%d`, `%%`, `\n`, escaped chars) from the English Fallback. Where an authoritative reviewed translation is not yet available, omit the key and record it as a deferred item in the provenance manifest (task 13.1) rather than inventing text.
    - _Requirements: 1.3, 3.2, 4.3_

- [ ] 13. Localization inventory, provenance, and automated checks (Concern 1 verification)
  - [ ] 13.1 Add the translation provenance manifest (test resource)
    - Create `app/src/test/resources/localization/translation-provenance.json` per the design schema: per-locale `provenance`, `reviewed` key list, and `deferred` map (key → reason) for `ta/te/hi/ml`.
    - _Requirements: 4.1, 4.2, 3.2_
  - [ ] 13.2 Implement the localization inventory / gap + placeholder check (required)
    - Add a JUnit 5 test that parses `values/strings*.xml` to build the user-facing key set (3.1), reports any default key absent from a locale and not listed under `deferred` (3.3), extracts placeholder/format-token multisets from English vs each translation and flags a mismatch as invalid unless the key is in that locale's `reviewed` list (4.3–4.5), records per-locale provenance and treats non-`human-reviewed` values as pending (4.1, 4.2), and asserts the AI feature keys exist in `ta/te/hi/ml` (1.3).
    - _Requirements: 1.3, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5_
  - [ ] 13.3 Property test: placeholder preservation with review override
    - `// Feature: mass-liturgy-and-localization, Property 16: For any translated value of an AI Feature String, the check accepts it without a placeholder flag exactly when either its placeholder/format-token multiset equals the English Fallback's, or it is marked human-reviewed; otherwise it is flagged invalid.`
    - JVM Kotest property, ≥100 iterations, `Arb` over translation strings with random token multisets + reviewed flag.
    - _Validates: Requirements 4.3, 4.4, 4.5 (Property 16)_
  - [ ] 13.4 Property test: gap-check correctness
    - `// Feature: mass-liturgy-and-localization, Property 17: For any user-facing key and any target locale, the check flags the key as a gap if and only if it exists in default values/, has no value in that locale, and is not recorded as a deferred item for that locale.`
    - JVM Kotest property, ≥100 iterations over generated key/locale/deferred combinations.
    - _Validates: Requirements 3.3 (Property 17)_
  - [ ]* 13.5 Build-config smoke test
    - Assert `bundle { language { enableSplit = false } }` and `lint { disable += "MissingTranslation" }` remain in `app/build.gradle.kts`.
    - _Requirements: 2.5, 3.4_

- [ ] 14. Bible-language resolver verification (Concern 1)
  - [ ] 14.1 Property test: blank-tag resolution follows the UI locale
    - `// Feature: mass-liturgy-and-localization, Property 18: For any string resource, resolving it with a blank language tag produces the same value as resolving against the UI locale (the resolver returns the base context unchanged for a blank tag).`
    - Robolectric/Compose Kotest property, ≥100 iterations.
    - _Validates: Requirements 2.3 (Property 18)_
  - [ ]* 14.2 Resolver example tests
    - `stringResourceIn` resolves a `values-ta` key under tag `ta`; English/default fallback when missing; `stringArrayResourceIn` resolves an array; recomposition re-resolves on Bible-language change.
    - _Requirements: 1.1, 1.5, 2.1, 2.2, 2.4_

- [ ] 15. Final integration build and verification
  - [ ] 15.1 Build with JDK 17 and run the unit-test suite green
    - Run `./gradlew compileDebugKotlin testDebugUnitTest` with `JAVA_HOME` on JDK 17 and `ANDROID_HOME=/root/android-sdk`; fix any compilation or test failures so both tasks complete successfully.
    - _Requirements: all (integration gate)_

## Notes

- Sub-tasks marked with `*` are optional plain unit/example tests and can be skipped for a faster MVP.
- Property-based test sub-tasks (Properties 1–18), the migration fidelity test (4.2), the inventory/placeholder check (13.2), the source-scan guard (11.5), and the feature-flag gating test (9.2) are required deliverables of the acceptance criteria and are NOT optional.
- Every property test runs at ≥100 iterations and carries the exact tag `// Feature: mass-liturgy-and-localization, Property {n}: {property_text}`.
- The domain-model migration (2.1) deliberately keeps `DefaultLiturgyProvider` and `ChurchModeScreen` compiling; `DefaultLiturgyProvider` is removed only after the fidelity test (4.2) passes, and `ChurchModeScreen`/`ViewModel` are removed when the new screens are wired (9.1).
- `[PARALLEL]`: tasks 11.x (AI migration), 12.1 (translations), and 13.1 (provenance resource) are independent of Concern 2 and of each other at the file level; the orchestrator may execute them in any order relative to Concern 2.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "4.1", "11.1", "11.2", "11.3", "11.4", "12.1", "13.1"] },
    { "id": 1, "tasks": ["2.2", "2.3", "3.1", "11.5", "13.2", "13.5", "14.1", "14.2"] },
    { "id": 2, "tasks": ["3.2", "13.3", "13.4"] },
    { "id": 3, "tasks": ["3.3", "3.4", "3.5", "3.6", "4.2", "4.3", "5.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.5", "5.6"] },
    { "id": 5, "tasks": ["5.4", "6.1"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4", "6.5"] },
    { "id": 7, "tasks": ["7.1", "8.1"] },
    { "id": 8, "tasks": ["7.2", "8.2"] },
    { "id": 9, "tasks": ["7.3", "7.4", "7.5", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9"] },
    { "id": 10, "tasks": ["9.1"] },
    { "id": 11, "tasks": ["9.2"] },
    { "id": 12, "tasks": ["15.1"] }
  ]
}
```
