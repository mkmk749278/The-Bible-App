# Requirements Document

## Introduction

This feature delivers the **core Bible reading experience** for Manna: the screens, content pipeline, and data model that let a user actually read, listen to, search, and annotate scripture after completing the denomination-aware setup. It replaces the current placeholder `MainScreen` and the stubbed translation source with a real, production-grade content system.

The feature is **offline-first and open-source-first**. Manna ships a bundled, public-domain translation (the World English Bible, including its deuterocanonical edition) seeded into the local database on first launch, so the app reads scripture with zero network and zero API keys — including the Catholic/Orthodox canon. Additional translations are sourced on demand from the **Free Use Bible API (bible.helloao.org)**, which is MIT-licensed, key-less, rate-limit-free, and cleared for commercial use. Human-narrated audio (Faith Comes By Hearing / Bible Brain) is intentionally **out of scope**; this feature provides offline Android TTS read-aloud and a clean integration seam for narrated audio later.

This feature builds directly on the shipped `denomination-aware-setup` capability and reuses its components: `CanonEngine`, `CanonProfile`, `NumberingScheme`, `PsalmNumberingMapper`, `CanonBookOrdering`, `PsalmDisplay`, `TranslationFilter`, `LectionaryProvider`, `PreferencesStore`, `TranslationRepository`, `PendingDownloadRepository`, `ConnectivityChecker`, and the annotation entities/repository. It does not redefine them.

All work is built, tested, and released exclusively through GitHub Actions (no local developer machine). Any future secret (e.g., a narrated-audio API key) is provided via GitHub Secrets.

## Glossary

- **System**: The Manna application as a whole.
- **Reader_Screen**: The Compose screen that renders a chapter of scripture and hosts reading interactions (navigation, audio, annotation, search entry).
- **Reader_ViewModel**: The ViewModel exposing `StateFlow` reading state to the `Reader_Screen`.
- **Bible_Content_Repository**: The single source of truth for scripture text (books, chapters, verses), backed by Room and populated by the `Bundled_Bible_Seeder` and `Download_Manager`.
- **HelloAo_Remote_DataSource**: The Retrofit-backed data source for the Free Use Bible API at `https://bible.helloao.org/api` (translations catalog, books, chapter content). No API key.
- **Bundled_Bible_Seeder**: The component that seeds the bundled World English Bible (and its deuterocanonical edition) from app assets into Room on first launch.
- **Bundled_Translation**: A translation shipped inside the APK (WEB and WEB-deuterocanon), always available offline.
- **Download_Manager**: The component that downloads, tracks progress for, cancels, and deletes on-demand translations, reusing `PendingDownloadRepository` for offline queuing.
- **Translation_Catalog**: The list of available translations, sourced from `HelloAo_Remote_DataSource` and merged with bundled and downloaded translations.
- **Active_Translation**: The translation currently selected for reading, persisted via `PreferencesStore` (`bibleTranslationId`).
- **Chapter_Content**: The ordered set of verses (with verse numbers and text) for one book+chapter of a translation.
- **Verse_Reference**: A stable canonical identifier of the form `BOOK.CHAPTER.VERSE` using OSIS book ids and canonical (Masoretic) chapter numbers, e.g. `GEN.1.1`.
- **Reading_Position**: The persisted last-read `Verse_Reference` (existing `lastReadPosition` preference).
- **Tts_Reader**: The Android TextToSpeech-backed component that reads `Chapter_Content` aloud, exposing playback state and per-verse progress.
- **Narrated_Audio_Provider**: An interface seam for future human-narrated audio (Faith Comes By Hearing / Bible Brain); not implemented in this feature.
- **Search_UseCase**: The component that performs full-text search across the `Active_Translation`'s verses.
- **Annotation_Repository**: The existing repository for highlights, bookmarks, and notes keyed by `Verse_Reference`.
- **Canon_Profile**: The active canon configuration (book set, ordering, numbering scheme) produced by the existing `CanonEngine`.
- **Attribution_Notice**: The copyright/licensing text shown for a translation (e.g., WEB public-domain statement; Free Use Bible API MIT acknowledgement).

## Requirements

### Requirement 1: Bundled Offline Bible Available on First Launch

**User Story:** As a new user with no internet, I want a complete Bible available immediately after setup, so that I can read scripture without downloading anything.

#### Acceptance Criteria

1. WHEN the `System` is launched for the first time AND the bundled content has not yet been seeded, THE `Bundled_Bible_Seeder` SHALL seed the `Bundled_Translation` content (World English Bible and its deuterocanonical edition) into the `Bible_Content_Repository`.
2. THE `Bundled_Bible_Seeder` SHALL complete seeding without requiring a network connection.
3. WHEN seeding has completed once, THE `Bundled_Bible_Seeder` SHALL NOT re-seed on subsequent launches (idempotent seeding).
4. WHERE the active `Canon_Profile` is `CATHOLIC_73` or `ORTHODOX_EXPANDED`, THE `Bible_Content_Repository` SHALL expose the deuterocanonical books from the bundled deuterocanonical edition.
5. IF seeding is interrupted before completion, THEN THE `Bundled_Bible_Seeder` SHALL resume or restart seeding on the next launch so that the bundled content becomes complete.
6. WHEN seeding completes, THE `Bible_Content_Repository` SHALL contain every book of the `Bundled_Translation` consistent with the book set the `Canon_Engine` defines.

### Requirement 2: Reading a Chapter

**User Story:** As a reader, I want to open a book and chapter and read its verses, so that I can engage with scripture.

#### Acceptance Criteria

1. WHEN the user opens a `Verse_Reference`'s book and chapter, THE `Reader_Screen` SHALL display the `Chapter_Content` for the `Active_Translation` with each verse's number and text.
2. THE `Reader_Screen` SHALL render verses in ascending verse order within the chapter.
3. WHERE the active `Canon_Profile` uses the `SEPTUAGINT` `NumberingScheme` AND the book is Psalms, THE `Reader_Screen` SHALL display Psalm chapter numbers using `PsalmDisplay`/`PsalmNumberingMapper`.
4. WHILE a chapter is displayed, THE `Reader_Screen` SHALL present the book name and displayed chapter number as a heading.
5. WHEN the requested `Chapter_Content` is present in the `Bible_Content_Repository`, THE `Reader_Screen` SHALL render it without a network connection.
6. IF the requested `Chapter_Content` is not available for the `Active_Translation`, THEN THE `Reader_Screen` SHALL display an explanatory state and offer to download or switch translation.

### Requirement 3: Book and Chapter Navigation

**User Story:** As a reader, I want to move between chapters and books and jump to a reference, so that I can navigate scripture efficiently.

#### Acceptance Criteria

1. WHEN the user requests the next chapter AND a next chapter exists in the active `Canon_Profile`'s ordering, THE `Reader_Screen` SHALL display the next chapter, advancing to the next book when at a book's last chapter.
2. WHEN the user requests the previous chapter AND a previous chapter exists, THE `Reader_Screen` SHALL display the previous chapter, moving to the prior book's last chapter when at a book's first chapter.
3. WHILE displaying the first chapter of the first book in the canon, THE `Reader_Screen` SHALL NOT offer a previous-chapter action.
4. WHILE displaying the last chapter of the last book in the canon, THE `Reader_Screen` SHALL NOT offer a next-chapter action.
5. THE `Reader_Screen` SHALL provide a book-and-chapter picker listing only books within the active `Canon_Profile` in its ordering.
6. WHEN the user selects a book and chapter from the picker, THE `Reader_Screen` SHALL display that `Chapter_Content`.
7. WHERE a book or chapter selected for navigation is outside the active `Canon_Profile`, THE `Reader_Screen` SHALL NOT offer it.

### Requirement 4: Translation Catalog from the Free Use Bible API

**User Story:** As a reader, I want to browse Bibles that match my tradition and language, so that I can choose an appropriate translation.

#### Acceptance Criteria

1. WHEN the user opens the translation catalog AND a network connection is available, THE `HelloAo_Remote_DataSource` SHALL retrieve the list of available translations from `available_translations.json`.
2. WHEN the catalog is retrieved, THE `Translation_Catalog` SHALL be filtered by the active `Canon_Profile` and the user's Bible language using the existing `TranslationFilter`.
3. WHEN the user opens the translation catalog, THE `Translation_Catalog` SHALL immediately include the `Bundled_Translation` entries and any already-downloaded translations marked as available offline, independent of and prior to any remote catalog retrieval.
4. THE `Translation_Catalog` SHALL display, for each translation, its name, language, and whether it is downloaded.
5. WHILE no network connection is available, THE `Translation_Catalog` SHALL present the bundled and downloaded translations and indicate that more are available when online.
6. IF retrieving the remote catalog fails, THEN THE `System` SHALL present the bundled and downloaded translations and a retry affordance, without crashing.

### Requirement 5: Downloading Translations for Offline Use

**User Story:** As a reader, I want to download a translation for offline use, so that I can read it without internet later.

#### Acceptance Criteria

1. WHEN the user starts a download for a translation AND a network connection is available, THE `Download_Manager` SHALL retrieve the translation's books and chapter content from `HelloAo_Remote_DataSource` and store them in the `Bible_Content_Repository`.
2. WHILE a download is in progress, THE `Download_Manager` SHALL report progress to the UI.
3. WHEN a translation download completes, THE `Download_Manager` SHALL mark the translation as downloaded so it is available offline.
4. WHEN the user cancels an in-progress download, THE `Download_Manager` SHALL stop the download AND SHALL remove partially stored content for that translation so no partial translation is presented as complete.
5. WHEN the user deletes a downloaded translation, THE `Download_Manager` SHALL remove its stored content AND SHALL clear its downloaded marker; IF the deleted translation was the `Active_Translation`, THEN THE `System` SHALL switch the `Active_Translation` to a `Bundled_Translation`.
6. IF a download is requested WHILE no network connection is available, THEN THE `Download_Manager` SHALL record the request as pending via `PendingDownloadRepository` and SHALL attempt it when a connection becomes available.
7. IF a download fails mid-way, THEN THE `Download_Manager` SHALL report the failure, retain a resumable/pending state, AND SHALL NOT mark the translation as downloaded.
8. THE `Download_Manager` SHALL present the approximate storage size of a translation before and after download where that information is available.

### Requirement 6: Selecting and Managing the Active Translation

**User Story:** As a reader, I want to switch which translation I am reading, so that I can read the version I prefer.

#### Acceptance Criteria

1. THE `System` SHALL allow the user to set any bundled or downloaded translation as the `Active_Translation`.
2. WHEN the user sets an `Active_Translation`, THE `PreferencesStore` SHALL persist the selection (`bibleTranslationId`).
3. WHEN the `Active_Translation` changes, THE `Reader_Screen` SHALL display the current `Reading_Position` rendered from the newly active translation.
4. WHERE the current `Reading_Position`'s book or chapter does not exist in the newly active translation, THE `Reader_Screen` SHALL display the nearest valid position within that translation.
5. THE `System` SHALL retain multiple downloaded translations simultaneously.
6. Parallel side-by-side comparison of two translations is OUT OF SCOPE for this feature and SHALL be deferred to a future feature.

### Requirement 7: Persisting and Restoring Reading Position

**User Story:** As a reader, I want the app to remember where I stopped reading, so that I can resume where I left off.

#### Acceptance Criteria

1. WHEN the user views a chapter, THE `System` SHALL persist the `Reading_Position` to the `PreferencesStore` (`lastReadPosition`).
2. WHEN the app is relaunched after content is available, THE `Reader_Screen` SHALL open at the persisted `Reading_Position`.
3. WHERE no `Reading_Position` has been persisted, THE `Reader_Screen` SHALL open at the first chapter of the first book in the active `Canon_Profile`.
4. WHERE the persisted `Reading_Position` is outside the active `Canon_Profile`, THE `Reader_Screen` SHALL open at the nearest valid position.

### Requirement 8: Verse Annotations in the Reader

**User Story:** As a reader, I want to highlight, bookmark, and note verses while reading, so that I can mark and reflect on scripture.

#### Acceptance Criteria

1. WHEN the user selects a verse, THE `Reader_Screen` SHALL offer to highlight, bookmark, or add a note to that `Verse_Reference`.
2. WHEN the user creates a highlight, bookmark, or note, THE `Annotation_Repository` SHALL persist it keyed by the canonical `Verse_Reference`.
3. WHILE a chapter is displayed, THE `Reader_Screen` SHALL visually indicate verses that have an existing highlight, bookmark, or note.
4. WHEN the user edits or deletes an existing annotation from the reader, THE `Annotation_Repository` SHALL update or remove it accordingly.
5. WHERE a verse's book is outside the active `Canon_Profile`, THE `Reader_Screen` SHALL NOT display that verse's annotations, consistent with the existing canon-visibility behavior, AND SHALL NOT delete the annotation.
6. THE `System` SHALL allow creating an annotation on any verse the user can view, and SHALL preserve such annotations when the active `Canon_Profile` later changes.
7. THE `Verse_Reference` used for annotations SHALL be canonical (Masoretic) and independent of the displayed numbering scheme, so annotations remain stable across denominations and translations.

### Requirement 9: Offline Audio Read-Aloud (Android TTS)

**User Story:** As a listener, especially one who prefers audio, I want the app to read scripture aloud offline, so that I can listen without reading.

#### Acceptance Criteria

1. WHEN the user starts audio playback for a chapter, THE `Tts_Reader` SHALL read the `Chapter_Content`'s verses aloud in order using Android TextToSpeech.
2. WHILE audio is playing, THE `Reader_Screen` SHALL indicate the verse currently being read AND SHALL advance the indication as playback progresses.
3. THE `Tts_Reader` SHALL provide play, pause, and stop controls.
4. THE `Tts_Reader` SHALL provide a playback speed control within a defined range of 0.5x to 2.0x, with a default of 1.0x.
5. THE `Tts_Reader` SHALL select a TTS voice matching the `Active_Translation`'s language where such a voice is available on the device.
6. IF no TTS voice is available for the `Active_Translation`'s language, THEN THE `Tts_Reader` SHALL inform the user AND SHALL offer the device default voice or guidance to install a voice, without crashing.
7. WHEN audio playback reaches the end of the chapter, THE `Tts_Reader` SHALL stop or continue to the next chapter according to the user's continuous-play preference.
8. THE `System` SHALL define a `Narrated_Audio_Provider` interface seam for future human-narrated audio; implementing a narrated-audio backend is OUT OF SCOPE for this feature.

### Requirement 10: Search Within the Active Translation

**User Story:** As a reader, I want to search for words or phrases, so that I can find verses quickly.

#### Acceptance Criteria

1. WHEN the user submits a non-empty search query, THE `Search_UseCase` SHALL return the verses in the `Active_Translation` whose text matches the query.
2. THE `Search_UseCase` SHALL restrict results to books within the active `Canon_Profile`.
3. WHEN search results are returned, THE `System` SHALL display each result's `Verse_Reference` (in the active displayed numbering) and a snippet of the verse text.
4. WHEN the user selects a search result, THE `Reader_Screen` SHALL open that result's chapter at the matching verse.
5. THE `Search_UseCase` SHALL operate on locally stored content without requiring a network connection.
6. WHERE a query yields no matches, THE `System` SHALL present an explicit no-results state.

### Requirement 11: Offline and Network-Failure Behavior

**User Story:** As a user with unreliable connectivity, I want the reader to work offline and fail gracefully online, so that my reading is never blocked.

#### Acceptance Criteria

1. WHILE no network connection is available, THE `Reader_Screen` SHALL fully support reading, navigation, annotation, audio, and search for bundled and downloaded content.
2. IF a network operation (catalog retrieval or download) fails, THEN THE `System` SHALL surface a clear, non-blocking error state with a retry affordance.
3. THE `System` SHALL NOT block reading of available content on any network operation.
4. WHILE a network operation has failed AND no retry option is available, THE `Reader_Screen` SHALL continue to allow reading, navigation, annotation, audio, and search for available content.
5. WHEN connectivity is restored, THE `Download_Manager` SHALL resume pending downloads recorded via `PendingDownloadRepository`.

### Requirement 12: Licensing and Attribution

**User Story:** As a user and as a responsible publisher, I want translation licensing and attribution shown, so that the app honors content licenses.

#### Acceptance Criteria

1. THE `Reader_Screen` SHALL provide access to the `Attribution_Notice` of the `Active_Translation`.
2. WHERE a translation is public domain (such as the World English Bible), THE `Attribution_Notice` SHALL state its public-domain status.
3. THE `System` SHALL include an acknowledgement of the Free Use Bible API (MIT) in an about/attribution surface at all times, regardless of which translations are currently in use.
4. WHERE a translation provided by the catalog carries a specific license or required attribution, THE `System` SHALL display that license/attribution with the translation.

### Requirement 13: Performance

**User Story:** As a user on a mid-range device, I want fast reading and search, so that the app feels responsive.

#### Acceptance Criteria

1. WHEN a chapter is requested from locally stored content, THE `Bible_Content_Repository` SHALL return the `Chapter_Content` within 50 milliseconds on a mid-range device under normal conditions.
2. WHEN the user submits a search query over a translation of approximately 31,000 verses, THE `Search_UseCase` SHALL return results within 200 milliseconds on a mid-range device under normal conditions.
3. WHEN the app cold-starts with bundled content already seeded, THE `Reader_Screen` SHALL display the first verse within 1.5 seconds on a mid-range device.
4. THE `Bundled_Bible_Seeder` SHALL perform seeding off the main thread so the UI remains responsive.

### Requirement 14: Accessibility

**User Story:** As a user who relies on assistive features, I want the reader to be fully accessible, so that I can read independently.

#### Acceptance Criteria

1. THE `Reader_Screen` SHALL render scripture text using scalable units that respond to the system font scale up to 200 percent.
2. THE `Reader_Screen` SHALL provide content descriptions for all interactive controls for screen-reader navigation.
3. THE `Reader_Screen` SHALL render all interactive elements with a touch target of at least 48 density-independent pixels per side.
4. WHERE the active Bible language uses a right-to-left script, THE `Reader_Screen` SHALL render its text and layout right-to-left.
5. WHERE the Simplified Mode preference is enabled, THE `Reader_Screen` SHALL present an audio-first presentation with enlarged controls.

### Requirement 15: Data Model, Seeding Integrity, and Migration

**User Story:** As the system owner, I want scripture stored with integrity and safe migrations, so that content is correct and never duplicated.

#### Acceptance Criteria

1. THE `Bible_Content_Repository` SHALL store scripture as Book, Chapter, and Verse records associated with a translation, keyed so that a verse is uniquely identified by translation, book, chapter, and verse number.
2. THE `Bundled_Bible_Seeder` SHALL seed content idempotently so that repeated seeding does not create duplicate books, chapters, or verses.
3. WHEN a translation is deleted, THE `Bible_Content_Repository` SHALL remove only that translation's content and SHALL leave other translations and all annotations intact.
4. THE `System` SHALL version stored translation content so that a translation can be re-downloaded or updated without leaving orphaned or duplicated records.
5. WHEN the Room schema changes, THE `System` SHALL provide a migration path that preserves user annotations and preferences.
