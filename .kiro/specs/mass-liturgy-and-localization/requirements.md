# Requirements Document

## Introduction

This spec covers two related, production-grade improvements to **Manna** (the offline-first, India-focused Bible app, package `com.manna.bible`):

1. **Bible-language localization (gap fill).** Manna's interface chrome is currently English-only in practice. A recent change (`docs/ai/TASKS.md` → BASE-08, "UI language selection removed; English hardcoded") removed the in-app UI-language switch, while Bible content and recited prayer text follow the user's chosen **Bible language** (`SetupState.bibleLanguage`, one of `ta`, `te`, `hi`, `ml`, `en`). PR #80 added six Gemini AI features (Crisis AI, Sermon Builder, Verse Cards, Persecution Comfort, Cultural Lens, Oral AI). Their user-facing strings were added only to the default (English) `values/` resources — `values-ta`, `values-te`, `values-hi`, `values-ml` are missing those translations (those locales currently contain only four navigation strings plus prayer content). This concern makes the new features' (and other gap) UI strings render in the user's chosen Bible language, using the app's existing `stringResourceIn(languageTag, …)` / `rememberLocalizedContext` mechanism (`ui/util/LocalizedString.kt`), with a defined English fallback.

2. **Mass / Liturgy Guide (new feature).** Manna already has a hidden-by-default "Church Mode" (`FeatureFlags.CHURCH_MODE`, `ui/church/ChurchModeScreen.kt`) that shows a single denomination-appropriate order of worship, chosen from two hardcoded English-only liturgies in `domain/liturgy/DefaultLiturgyProvider.kt` (Roman Catholic Mass, CSI Holy Communion), with a switcher between them. This concern evolves that into a production-grade **Liturgy Library**: a browsable list of Masses / liturgical services mapped to denominations, where selecting one expands its full order of service (prayers, congregational responses, rubrics, and section headings) in the exact sequence celebrated in church. The liturgical content must be **fully offline** (bundled assets, consistent with how `assets/bibles/` and `assets/canon/` ship), **multilingual** (displayed in the Bible language, tying into Concern 1), role-distinguished (who speaks / responds), accessible to elderly and semi-literate users (Elder / Simplified Mode), and faithful to the project's existing **content policy of never inventing liturgical text** — structure, public-domain ordinary texts, and authentic short responses are shipped, while presidential prayers proper to the day are flagged to follow the parish's official book.

The two concerns are coupled: liturgical text rendering depends on the localization mechanism defined in Concern 1.

This document specifies **what** the system must do. Implementation detail (class design, asset schema specifics) belongs in the design phase, except where a concrete existing convention must be named to make a requirement testable.

> **Note on ambiguity:** Liturgical scope (which traditions/Masses to ship first), authoritative text sourcing, and copyright of vernacular liturgical texts are genuinely open. They are captured in **§ Assumptions** and **§ Open Questions** rather than silently guessed, and several acceptance criteria are written to hold regardless of how those questions resolve.

## Glossary

- **Manna**: The Android Bible application (`com.manna.bible`) as a whole.
- **Bible Language**: The language code the user selected for scripture content, persisted as `SetupState.bibleLanguage` (one of `en`, `ta`, `te`, `hi`, `ml`). Distinct from the device/system UI locale.
- **UI Locale**: The Android system/device locale that a plain `stringResource(...)` call resolves against. Per BASE-08 there is no in-app switch that changes the UI Locale; it follows the device.
- **String_Resolver**: The component that resolves a string (or string-array) resource in an explicitly supplied language tag rather than the UI Locale. Realized today by `stringResourceIn(languageTag, id)` and `rememberLocalizedContext(languageTag)` in `ui/util/LocalizedString.kt`.
- **Localized Surface**: A screen or component whose user-facing static strings are resolved through the **String_Resolver** using the **Bible Language**.
- **English Fallback**: The default `values/strings*.xml` resource value, used when a string has no translation in the requested **Bible Language**.
- **AI Feature Strings**: The static UI strings introduced by PR #80 for the six Gemini features, currently present only in `values/` (e.g. `strings_crisis.xml`, `strings_sermon.xml`, `strings_card.xml`, and crisis/sermon/card entries in `strings.xml`).
- **Denomination**: The user's selected tradition, `domain/model/Denomination` — one of `CATHOLIC`, `CSI`, `PROTESTANT_OTHER`, `ORTHODOX`, `MAR_THOMA`, `SHOW_EVERYTHING`.
- **Liturgy**: A complete order of worship for one tradition/occasion (e.g. "The Holy Mass"). Domain type `domain/liturgy/Liturgy`.
- **Liturgy_Library**: The browsable list surface that lists available **Liturgy** entries and lets the user select one.
- **Liturgy_Detail**: The surface that displays a selected **Liturgy** expanded into its ordered sections and parts.
- **Liturgy_Section**: A major division of a **Liturgy** (e.g. "Introductory Rites"), `domain/liturgy/LiturgySection`.
- **Liturgy_Part**: One step of a service, `domain/liturgy/LiturgyPart`, carrying a **Liturgy_Role**, optional title, spoken text, rubric, scripture reference (`osisRef`), and a `needsOfficialText` flag.
- **Liturgy_Role**: Who speaks or acts a part, `domain/liturgy/LiturgyRole` — one of `PRESIDER`, `PEOPLE`, `ALL`, `READER`, `RUBRIC`.
- **Rubric**: An instruction (stage direction) rather than spoken text, e.g. "All stand".
- **Official-Text Part**: A **Liturgy_Part** with `needsOfficialText = true` — a presidential prayer proper to the day whose exact words are intentionally not reproduced; the surface directs the user to the parish book / Missal.
- **Liturgy_Provider**: The component supplying available **Liturgy** entries and the default for a **Denomination** (`domain/liturgy/LiturgyProvider`).
- **Liturgy_Asset**: A bundled, offline data file under `app/src/main/assets/` describing one or more **Liturgy** entries, parsed at runtime.
- **Liturgy_Asset_Parser**: The component that reads and validates a **Liturgy_Asset** into domain **Liturgy** objects.
- **Liturgy_Manifest**: The bundled index file listing available **Liturgy_Asset** files and their metadata (id, title key, tradition, denomination mapping, languages), analogous to `assets/bibles/manifest.json`.
- **Feature_Flags**: The compile-time gating object `domain/FeatureFlags`. The relevant flag is `CHURCH_MODE`.
- **Simplified Mode**: The combined Elder / Oral accessibility mode (DataStore `simplifiedMode`), which uses larger touch targets (56dp+), 150% text scale, and audio assistance, per CLAUDE.md.
- **Content Policy (Never Invent Liturgy)**: The existing project rule that Manna ships only liturgical **structure**, **rubrics**, authentic short **congregational responses**, and **public-domain / traditional ordinary texts**; presidential prayers proper to the day are shipped as **Official-Text Parts**, never fabricated.

---

## Requirements

### Concern 1 — Bible-Language Localization

### Requirement 1: AI feature UI strings render in the Bible language

**User Story:** As a Tamil/Telugu/Hindi/Malayalam-reading user, I want the new AI features' interface text to appear in my chosen Bible language, so that I can use Crisis AI, Sermon Builder, Verse Cards, Persecution Comfort, Cultural Lens, and Oral AI without reading English.

#### Acceptance Criteria

1. WHERE the **Bible Language** has a translated value for an **AI Feature String**, THE **String_Resolver** SHALL return the value in the **Bible Language**.
2. WHEN a Localized Surface for any of the six PR #80 features is displayed, THE Manna SHALL resolve every static user-facing **AI Feature String** on that surface through the **String_Resolver** using the **Bible Language**.
3. THE Manna SHALL provide translated values for every **AI Feature String** in `values-ta`, `values-te`, `values-hi`, and `values-ml`.
4. IF a translated value is absent for a requested **AI Feature String** in the **Bible Language**, THEN THE **String_Resolver** SHALL return the **English Fallback** value.
5. WHEN the **Bible Language** is `en`, THE **String_Resolver** SHALL return the default `values/` value.

### Requirement 2: Bible-language string resolution mechanism

**User Story:** As a developer, I want a single, consistent way to resolve interface strings in the Bible language, so that localization behaves identically across every screen and is testable.

#### Acceptance Criteria

1. THE **String_Resolver** SHALL resolve a string resource in a supplied BCP-47 language tag independent of the **UI Locale**.
2. THE **String_Resolver** SHALL resolve a string-array resource in a supplied BCP-47 language tag independent of the **UI Locale**.
3. WHEN the supplied language tag is blank, THE **String_Resolver** SHALL resolve the resource against the **UI Locale**.
4. WHEN the **Bible Language** value changes in preferences, THE Manna SHALL render newly displayed Localized Surfaces in the updated **Bible Language** without requiring an app reinstall.
5. THE Manna SHALL retain the build configuration that includes all locale string resources in every distributed artifact (the existing `bundle { language { enableSplit = false } }` setting), so that locale resources are available regardless of the device system language.

### Requirement 3: Localization gap coverage and inventory

**User Story:** As a maintainer, I want a complete, verifiable inventory of which interface strings are localized, so that "everything shows in the Bible language" is auditable rather than aspirational.

#### Acceptance Criteria

1. THE Manna SHALL present a documented inventory that lists, for each user-facing string resource in `values/`, whether a translation exists in each of `values-ta`, `values-te`, `values-hi`, and `values-ml`.
2. WHERE a string resource is identified as user-facing and feature-active, THE Manna SHALL provide a translation for that resource in each of `values-ta`, `values-te`, `values-hi`, and `values-ml`, or record it in the inventory as an explicit deferred item with a reason.
3. WHEN an automated check runs over the resource inventory, THE check SHALL report any user-facing resource that is present in `values/` but absent from a target locale and is not recorded as a deferred item.
4. THE Manna SHALL keep the `MissingTranslation` lint accommodation consistent with incremental localization (translations may land in batches) while the inventory provides the completeness guarantee.

### Requirement 4: Translation quality and review provenance

**User Story:** As a user who depends on Manna in my own language, I want translations to be trustworthy, so that the interface does not mislead me with machine-mangled or unreviewed text.

#### Acceptance Criteria

1. THE Manna SHALL record, for each delivered translation locale, the provenance of its translations (for example human-reviewed, community-contributed, or machine-translated-pending-review).
2. WHERE a translated value has not been reviewed, THE Manna SHALL record that value as pending review in the inventory.
3. THE translated value for any **AI Feature String** SHALL preserve placeholder tokens and formatting markers present in the **English Fallback** value (for example positional format arguments and escaped characters).
4. IF a translated value would omit or corrupt a placeholder token present in the **English Fallback** AND that value is not marked human-reviewed, THEN an automated check SHALL flag that value as invalid.
5. WHERE a translated value is marked human-reviewed, THE automated check SHALL accept that value without flagging it for placeholder mismatch.

### Concern 2 — Mass / Liturgy Guide

### Requirement 5: Browse the list of available liturgies

**User Story:** As a churchgoer, I want to see a list of the Masses and liturgical services available for my tradition, so that I can pick the one I want to follow.

#### Acceptance Criteria

1. WHEN the user opens the **Liturgy_Library**, THE Manna SHALL display a list of every available **Liturgy** entry.
2. THE **Liturgy_Library** SHALL display, for each listed **Liturgy**, its title and its tradition.
3. WHERE the user's **Denomination** has at least one mapped **Liturgy**, THE **Liturgy_Library** SHALL present the mapped entries for that **Denomination** ahead of the others.
4. IF the user's **Denomination** has no mapped **Liturgy**, THEN THE **Liturgy_Library** SHALL display a message that an order for the chosen tradition is being prepared and SHALL still list the available entries.
5. WHEN no Internet connection is available, THE **Liturgy_Library** SHALL display the full list of available **Liturgy** entries.

### Requirement 6: Select and expand a liturgy's order of service

**User Story:** As a worshipper, I want to open a selected liturgy and see its full order of service in sequence, so that I can follow along exactly as it goes in church.

#### Acceptance Criteria

1. WHEN the user selects a **Liturgy** from the **Liturgy_Library**, THE **Liturgy_Detail** SHALL display that **Liturgy**'s **Liturgy_Section** entries in their defined order.
2. WHEN a **Liturgy_Section** is displayed, THE **Liturgy_Detail** SHALL display that section's **Liturgy_Part** entries in their defined order.
3. THE **Liturgy_Detail** SHALL display each **Liturgy_Section** title as a section heading.
4. WHERE a **Liturgy_Part** has a title, THE **Liturgy_Detail** SHALL display that title with the part.
5. WHERE a **Liturgy_Part** carries spoken text, THE **Liturgy_Detail** SHALL display that text.
6. WHILE the user has not navigated back, THE **Liturgy_Detail** SHALL remain displayed for the selected **Liturgy**.
7. WHEN the user explicitly navigates back from a **Liturgy_Detail**, THE Manna SHALL display the list of available **Liturgy** entries.

### Requirement 7: Distinguish spoken parts, responses, and rubrics

**User Story:** As someone following the service, I want to clearly see who says what — priest, people, everyone, reader — versus instructions, so that I know when to speak and when to act.

#### Acceptance Criteria

1. WHERE a **Liturgy_Part** has a **Liturgy_Role** of `PRESIDER`, `PEOPLE`, `ALL`, or `READER`, THE **Liturgy_Detail** SHALL display a role label identifying who speaks that part.
2. WHERE a **Liturgy_Part** has a **Liturgy_Role** of `RUBRIC`, THE **Liturgy_Detail** SHALL display the part as an instruction visually distinct from spoken text and SHALL NOT attach a formal speaker role label, even where the rubric text itself contains role-referencing phrasing (for example "The priest says" or "All respond").
3. THE **Liturgy_Detail** SHALL visually differentiate `PRESIDER`, `PEOPLE`, `ALL`, and `READER` roles from one another.
4. WHERE a **Liturgy_Part** is an **Official-Text Part**, THE **Liturgy_Detail** SHALL display a notice that the prayer is proper to the day and SHALL direct the user to the official parish book / Missal in place of reproduced text.
5. WHERE a **Liturgy_Part** carries a scripture reference, THE **Liturgy_Detail** SHALL provide an action that opens that reference in the reader.

### Requirement 8: Liturgical content displays in the Bible language

**User Story:** As a Tamil/Telugu/Hindi/Malayalam-speaking worshipper, I want the order of service in my Bible language, so that I can read and pray it in the language I understand.

#### Acceptance Criteria

1. WHERE a **Liturgy** has content available in the **Bible Language**, THE **Liturgy_Detail** SHALL display that **Liturgy**'s section titles, part titles, spoken text, responses, and rubrics in the **Bible Language**.
2. IF a **Liturgy**'s content is unavailable in the **Bible Language**, THEN THE **Liturgy_Detail** SHALL display the **English Fallback** content for the missing parts.
3. WHILE a **Liturgy**'s content is displayed using the **English Fallback**, THE **Liturgy_Detail** SHALL continue to resolve its static framing strings (role labels, the official-text notice, the "read in context" action, the source note label) in the **Bible Language**.
4. THE **Liturgy_Detail** SHALL resolve every static framing string through the **String_Resolver** using the **Bible Language**.
5. WHEN the **Bible Language** is changed in preferences, THE **Liturgy_Detail** SHALL display subsequently opened liturgies in the updated **Bible Language**.

### Requirement 9: Offline bundled liturgical content

**User Story:** As a rural user with no reliable Internet, I want the liturgies to work completely offline, so that I can follow Mass in church where there is no signal.

#### Acceptance Criteria

1. THE Manna SHALL bundle all **Liturgy** content as **Liturgy_Asset** files within the application package under `app/src/main/assets/`.
2. WHEN no Internet connection is available, THE **Liturgy_Detail** SHALL display the complete selected **Liturgy** content.
3. THE Manna SHALL load **Liturgy** content without performing any network request.
4. THE **Liturgy_Manifest** SHALL index every bundled **Liturgy_Asset** with its id, title, tradition, mapped denomination(s), and available languages, consistent with the bundled-asset manifest convention used by `assets/bibles/manifest.json`.

### Requirement 10: Liturgy asset parsing and integrity

**User Story:** As a developer, I want bundled liturgy data parsed safely and verifiably, so that malformed or incomplete content never crashes the app or misrepresents a liturgy.

#### Acceptance Criteria

1. WHEN the **Liturgy_Asset_Parser** reads a valid **Liturgy_Asset**, THE **Liturgy_Asset_Parser** SHALL produce the corresponding **Liturgy** domain objects with sections and parts in their authored order.
2. IF a **Liturgy_Asset** is malformed or fails schema validation, THEN THE **Liturgy_Asset_Parser** SHALL report a descriptive error and THE Manna SHALL exclude that entry from the **Liturgy_Library** without crashing.
3. FOR ALL valid **Liturgy** objects, serializing a **Liturgy** to the **Liturgy_Asset** format and parsing the result SHALL produce an equivalent **Liturgy** object (round-trip property).
4. WHEN the **Liturgy_Asset_Parser** loads the **Liturgy_Manifest**, THE **Liturgy_Asset_Parser** SHALL load only the **Liturgy_Asset** files listed in the manifest.
5. IF a **Liturgy_Asset** declares a content language that is not present in its parts, THEN an automated check SHALL flag that asset as inconsistent.

### Requirement 11: Denomination-to-liturgy mapping

**User Story:** As a user who picked a tradition during setup, I want the liturgies relevant to my tradition surfaced, so that a Catholic sees the Mass and a CSI member sees Holy Communion.

#### Acceptance Criteria

1. THE **Liturgy_Provider** SHALL return the set of **Liturgy** entries mapped to a given **Denomination**.
2. WHERE the **Denomination** is `CATHOLIC`, THE **Liturgy_Provider** SHALL map the Roman Catholic Order of Mass as a default entry.
3. IF the mapped default **Liturgy** for a **Denomination** is unavailable, THEN THE **Liturgy_Provider** SHALL fall back to an available **Liturgy** entry rather than returning no selectable order.
4. WHERE the **Denomination** is `CSI` or `PROTESTANT_OTHER`, THE **Liturgy_Provider** SHALL map the Church of South India Holy Communion as a default entry.
5. WHERE the **Denomination** is `SHOW_EVERYTHING`, THE **Liturgy_Provider** SHALL make every available **Liturgy** entry selectable.
6. IF a **Denomination** has no mapped **Liturgy**, THEN THE **Liturgy_Provider** SHALL supply a fallback **Liturgy** from the available entries while still exposing all available entries through its full listing.

### Requirement 12: Faithful content policy (never invent liturgy)

**User Story:** As a member of a church tradition, I want the app to never put fabricated prayers in my mouth, so that I can trust it is faithful to the actual liturgy.

#### Acceptance Criteria

1. THE Manna SHALL include in each **Liturgy** only liturgical structure, rubrics, authentic congregational responses, and public-domain or traditional ordinary texts.
2. WHERE a presidential prayer is proper to the day, THE Manna SHALL represent it as an **Official-Text Part** rather than reproducing fabricated wording.
3. WHERE the official wording of a presidential prayer is available from an authorized source, THE Manna MAY display that official text alongside the **Official-Text Part** designation.
4. THE Manna SHALL record, for each **Liturgy**, a source note identifying the order of service and the provenance of its texts.
5. THE **Liturgy_Detail** SHALL display each **Liturgy**'s source note.

### Requirement 13: Feature-flag gating

**User Story:** As a release manager, I want the Mass / Liturgy Guide gated by the existing feature-flag convention, so that incomplete traditions can land on the default branch without being exposed to users.

#### Acceptance Criteria

1. WHERE `FeatureFlags.CHURCH_MODE` is `false`, THE Manna SHALL NOT expose the main entry point to the **Liturgy_Library**.
2. WHERE `FeatureFlags.CHURCH_MODE` is `false`, THE Manna MAY retain previously loaded or cached liturgy data reachable through other internal paths, provided the main entry point is hidden.
3. WHERE `FeatureFlags.CHURCH_MODE` is `true`, THE Manna SHALL expose an entry point to the **Liturgy_Library**.
4. THE Manna SHALL gate the **Liturgy_Library** through the `FeatureFlags` object consistent with the existing feature-flag convention described in CLAUDE.md.

### Requirement 14: Accessibility for elderly and semi-literate users

**User Story:** As an elderly or semi-literate worshipper, I want the liturgy guide to be easy to read and navigable by screen reader, so that I can follow the service comfortably.

#### Acceptance Criteria

1. THE **Liturgy_Library** and **Liturgy_Detail** SHALL render all text using scalable `sp` units so that text scales with the system font size.
2. THE interactive elements in the **Liturgy_Library** and **Liturgy_Detail** SHALL present a touch target of at least 48dp by 48dp.
3. WHERE **Simplified Mode** is active, THE Manna SHALL present interactive elements in the **Liturgy_Library** and **Liturgy_Detail** with a touch target of at least 56dp by 56dp.
4. THE interactive controls and meaningful icons in the **Liturgy_Library** and **Liturgy_Detail** SHALL expose a content description for screen-reader navigation.
5. WHERE the **Bible Language** uses a right-to-left script, THE **Liturgy_Detail** SHALL mirror the entire interface layout to the right-to-left reading direction, regardless of the language of individual liturgy content.

---

## Assumptions

These are working assumptions adopted to let implementation proceed; each can be overridden by answers to the Open Questions.

1. **Initial tradition scope.** The first production release of the Liturgy Library ships the two orders already present (Roman Catholic Order of Mass; CSI Holy Communion), migrated from hardcoded Kotlin into bundled **Liturgy_Assets**, plus a defined plan for adding Orthodox (Holy Qurbana) and Mar Thoma liturgies under the same content policy.
2. **"Many Masses" interpretation.** "Many Masses" is interpreted as multiple distinct liturgical orders across traditions (and, where authoritative, multiple occasions within a tradition such as Sunday Mass vs. a Requiem/Funeral order), surfaced as a browsable list rather than a single switcher.
3. **Text sourcing.** Ordinary texts use public-domain / traditional or ecumenical (ICET) English wording, matching the provenance already recorded in `DefaultLiturgyProvider` source notes. Presidential prayers proper to the day remain **Official-Text Parts**.
4. **Vernacular liturgical text.** Where authoritative, published vernacular liturgical texts (Tamil/Telugu/Hindi/Malayalam) exist for an order, those are used; the app does not auto-translate sacred liturgical text with the AI engine. Where no authoritative vernacular text is available, the part falls back to the English ordinary text or is shown as an Official-Text Part (see Open Questions).
5. **Localization scope priority.** Concern 1 prioritizes the PR #80 AI feature strings first, then extends coverage to remaining user-facing surfaces tracked by the inventory (Requirement 3).
6. **Reader integration.** Scripture references in liturgy parts open in the existing reader using OSIS references, reusing the current `onOpenVerse` pathway.

## Open Questions

These require user/stakeholder input or authoritative sourcing before or during design. They do not block initial requirements approval.

1. **Which traditions/Masses ship first, and in what order after launch?** Confirm whether Orthodox (Holy Qurbana) and Mar Thoma liturgies are in scope for the first release or deferred, and whether multiple occasions per tradition (e.g. Sunday Mass, Funeral/Requiem, Nuptial Mass) are required initially.
2. **Authoritative liturgical text sources and copyright.** What published service books are the authoritative sources (e.g. USCCB Order of Mass, CSI Book of Common Worship), and are their texts public-domain or licensed for inclusion? Reproducing copyrighted vernacular liturgical texts may require permission.
3. **Vernacular liturgical translations.** For Tamil/Telugu/Hindi/Malayalam, are authoritative published liturgical translations available and cleared for use, or should non-public-domain portions remain Official-Text Parts pointing to the parish book?
4. **Entry point and information architecture.** Should the Liturgy Library remain reached via the current Church Mode entry, or be promoted to a more prominent location (e.g. Prayers hub / More tab)?
5. **Audio.** Is read-aloud (TTS in the Bible language) of liturgical parts required for the semi-literate audience in this release, or deferred to a later phase?
