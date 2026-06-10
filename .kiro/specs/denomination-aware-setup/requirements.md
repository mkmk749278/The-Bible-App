# Requirements Document

## Introduction

This feature defines the **first-launch onboarding and setup flow** for Manna, the offline-first, India-focused Bible app. The flow captures the user's Christian tradition (denomination), App UI language, and Bible text language, and then configures the app to render the correct Bible content for that tradition.

The core insight driving this feature is that a user's **denomination determines the Bible canon** (which books appear), the **book ordering**, the **Psalm and verse numbering scheme**, the **proper-noun naming convention**, and the **liturgical lectionary**. No mainstream Bible app performs denomination-aware onboarding that sets canon, ordering, numbering, naming, and lectionary together; this is Manna's differentiator.

Research basis incorporated into this document:

- **Protestant** traditions (including CSI, CNI, Baptist, Pentecostal, Mar Thoma) use a **66-book** canon (39 OT + 27 NT).
- **Roman Catholic** traditions (Latin, Syro-Malabar, Syro-Malankara) use a **73-book** canon, adding 7 deuterocanonical books (Tobit, Judith, Wisdom, Sirach, Baruch, 1-2 Maccabees) plus additions to Esther and Daniel, with deuterocanonical books interleaved in the Old Testament ordering.
- **Orthodox** traditions (Malankara Orthodox / Jacobite Syrian) use an expanded Septuagint-based Old Testament (up to ~49 OT books).
- **CSI** (Church of South India) is a united Protestant church with a 66-book canon and its own revised lectionary (CSI Almanac) and seasonal collects.
- Catholic/Orthodox traditions use **Septuagint/Vulgate Psalm numbering**, which differs from the Protestant Masoretic numbering by one for most Psalms 9 to 147.
- Denomination-specific proper-noun/name translations exist within the same language (documented in Telugu and Malayalam).

This feature must honor existing project constraints: the dual-language architecture (independent `uiLanguage` and `bibleLanguage` in DataStore), offline-first operation, audio-first and accessibility requirements (Simplified/Elder Mode, 48dp/56dp touch targets, TalkBack, RTL, text scaling), no-account/privacy-first design, and preservation of existing user data (highlights, bookmarks, notes) that reference verses.

## Glossary

- **Setup_Flow**: The guided, multi-screen first-launch experience that collects denomination, UI language, Bible language, translation, and lectionary choices.
- **Denomination**: The user's selected Christian tradition. Supported values: `catholic`, `csi`, `protestant_other`, `orthodox`, `mar_thoma`, `show_everything`.
- **Canon**: The set of biblical books associated with a denomination. Supported values: `protestant_66`, `catholic_73`, `orthodox_expanded`, `all_canons`.
- **UI_Language**: The language used for the application interface, stored as the `uiLanguage` preference.
- **Bible_Language**: The language used for Bible text, stored as the `bibleLanguage` preference, independent of UI_Language.
- **Translation**: A downloadable Bible version, represented by the existing Room `Translation` entity (id, name, languageCode, scriptDirection, isDownloaded, sizeBytes).
- **Translation_Catalog**: The list of available translations sourced from the Bible Brain API, used to recommend and download translations.
- **Lectionary**: A denomination-specific liturgical reading calendar (for example, the CSI Almanac or the Roman Catholic liturgical calendar).
- **Numbering_Scheme**: The Psalm and verse numbering convention. Supported values: `masoretic` (Protestant) and `septuagint` (Catholic/Orthodox).
- **Naming_Convention**: The denomination-specific proper-noun naming set applied within a Bible_Language.
- **Deuterocanonical_Books**: The 7 additional Old Testament books and the additions to Esther and Daniel present in the Catholic canon.
- **Setup_Store**: The DataStore preferences that persist the user's setup choices, including new keys: `denomination`, `canon`, `lectionary`, `numberingScheme`, `namingConvention`, `bibleTranslationId`, and `setupCompleted`.
- **User_Annotation**: Any user-created data that references a verse, including highlights, bookmarks, and notes.
- **Settings_Screen**: The existing settings area from which the Setup_Flow can be re-run.
- **System**: The Manna application as a whole.

## Requirements

### Requirement 1: First-Launch Detection

**User Story:** As a first-time user, I want the app to recognize my first launch and guide me through setup, so that the app is configured correctly before I begin reading.

#### Acceptance Criteria

1. WHEN the App is launched AND the `setupCompleted` preference is absent or false, THE System SHALL start the Setup_Flow.
2. WHEN the App is launched AND the `setupCompleted` preference is true, THE System SHALL skip the Setup_Flow and open the main reading experience.
3. WHEN the Setup_Flow completes successfully, THE System SHALL set the `setupCompleted` preference to true.
4. IF the App is closed before the Setup_Flow completes, THEN THE System SHALL restart the Setup_Flow on the next launch.
5. THE Setup_Flow SHALL operate fully offline without requiring an internet connection to complete.

### Requirement 2: Skippable Setup With Defaults

**User Story:** As a user who wants to start quickly, I want to skip detailed setup and use sensible defaults, so that I can begin reading immediately.

#### Acceptance Criteria

1. WHERE the Setup_Flow is displayed, THE Setup_Flow SHALL provide a skip control on each setup screen.
2. WHEN the user activates the skip control, THE System SHALL apply India-first default values for Denomination, Canon, UI_Language, Bible_Language, and Numbering_Scheme.
3. WHEN defaults are applied through skipping, THE System SHALL set the Denomination default to `show_everything` and the Canon default to `all_canons`.
4. WHEN the user completes the Setup_Flow by skipping, THE System SHALL set the `setupCompleted` preference to true.
5. THE System SHALL allow the user to re-run the Setup_Flow later from the Settings_Screen.

### Requirement 3: Denomination Selection

**User Story:** As a Christian from a specific tradition, I want to select my denomination during setup, so that the app shows the Bible content correct for my tradition.

#### Acceptance Criteria

1. THE Setup_Flow SHALL present the following Denomination options: Catholic, CSI, Other Protestant, Orthodox, Mar Thoma, and "Not sure / Show me everything".
2. WHEN the user selects Catholic, THE System SHALL set the Canon to `catholic_73`.
3. WHEN the user selects CSI, THE System SHALL set the Canon to `protestant_66`.
4. WHEN the user selects Other Protestant, THE System SHALL set the Canon to `protestant_66`.
5. WHEN the user selects Mar Thoma, THE System SHALL set the Canon to `protestant_66`.
6. WHEN the user selects Orthodox, THE System SHALL set the Canon to `orthodox_expanded`.
7. WHEN the user selects "Not sure / Show me everything", THE System SHALL set the Canon to `all_canons`.
8. THE Setup_Flow SHALL display a short description of each Denomination option so that the user can identify the correct tradition.

### Requirement 4: App UI Language Selection

**User Story:** As a user, I want to choose the language of the app interface independently of the Bible text language, so that I can navigate the app in my preferred language.

#### Acceptance Criteria

1. THE Setup_Flow SHALL present a list of supported UI_Language options.
2. WHEN the user selects a UI_Language, THE System SHALL store the selection in the `uiLanguage` preference.
3. WHEN a UI_Language is selected, THE System SHALL render all subsequent Setup_Flow screens in the selected UI_Language.
4. THE System SHALL store the UI_Language independently of the Bible_Language so that the two preferences can differ.
5. WHERE the user has not selected a UI_Language, THE System SHALL default the `uiLanguage` preference to the device system language when that language is supported, and to English otherwise.

### Requirement 5: Bible Language and Translation Selection

**User Story:** As a user, I want to choose my Bible text language and a specific translation recommended for my denomination, so that I read an edition appropriate to my tradition.

#### Acceptance Criteria

1. THE Setup_Flow SHALL present a list of Bible_Language options independent of the selected UI_Language.
2. WHEN the user selects a Bible_Language, THE System SHALL store the selection in the `bibleLanguage` preference.
3. WHEN a Bible_Language and Denomination are selected, THE System SHALL display Translation options from the Translation_Catalog filtered to the selected Bible_Language and compatible with the selected Canon.
4. WHEN Translation options are displayed for a Denomination with Canon `catholic_73`, THE System SHALL rank Catholic editions above editions lacking Deuterocanonical_Books.
5. WHEN the user selects a Translation, THE System SHALL store the Translation identifier in the `bibleTranslationId` preference.
6. IF no Translation in the Translation_Catalog matches both the selected Bible_Language and the selected Canon, THEN THE System SHALL inform the user and offer the closest available Translation in the selected Bible_Language.

### Requirement 6: Applying Canon and Book Ordering

**User Story:** As a user, I want the app to display exactly the books of my tradition in the correct order, so that the Bible matches what I use in my church.

#### Acceptance Criteria

1. WHEN the Canon is `protestant_66`, THE System SHALL display 39 Old Testament books and 27 New Testament books and SHALL exclude Deuterocanonical_Books.
2. WHEN the Canon is `catholic_73`, THE System SHALL display the 66 Protestant books and the 7 Deuterocanonical_Books with the deuterocanonical additions to Esther and Daniel.
3. WHEN the Canon is `catholic_73`, THE System SHALL order the Deuterocanonical_Books interleaved within the Old Testament according to the Catholic ordering.
4. WHEN the Canon is `orthodox_expanded`, THE System SHALL display the Septuagint-based Old Testament book set associated with the Orthodox tradition.
5. WHEN the Canon is `all_canons`, THE System SHALL display the union of all supported canons' books.
6. THE System SHALL apply the selected Canon's ordering to the `orderIndex` used when listing Books.

### Requirement 7: Psalm and Verse Numbering Scheme

**User Story:** As a Catholic or Orthodox user, I want Psalms numbered the way my tradition numbers them, so that references I look up match my church's references.

#### Acceptance Criteria

1. WHEN the Denomination is Catholic or Orthodox, THE System SHALL set the Numbering_Scheme to `septuagint`.
2. WHEN the Denomination is CSI, Other Protestant, or Mar Thoma, THE System SHALL set the Numbering_Scheme to `masoretic`.
3. WHILE the Numbering_Scheme is `septuagint`, THE System SHALL display Psalm numbers using the Septuagint/Vulgate numbering for Psalms 9 through 147.
4. WHILE the Numbering_Scheme is `masoretic`, THE System SHALL display Psalm numbers using the Masoretic numbering.
5. THE System SHALL store the selected Numbering_Scheme in the `numberingScheme` preference.

### Requirement 8: Proper-Noun Naming Convention

**User Story:** As a user in a tradition with distinct name spellings, I want proper nouns rendered in my tradition's convention, so that names read as I expect.

#### Acceptance Criteria

1. WHEN a Denomination and Bible_Language are selected, THE System SHALL set the Naming_Convention to the convention associated with that Denomination and Bible_Language.
2. WHERE a denomination-specific Naming_Convention is available for the selected Bible_Language, THE System SHALL apply that Naming_Convention to displayed proper nouns.
3. WHERE no denomination-specific Naming_Convention is available for the selected Bible_Language, THE System SHALL apply the Translation's default naming.
4. THE System SHALL store the selected Naming_Convention in the `namingConvention` preference.

### Requirement 9: Lectionary Selection

**User Story:** As a user who follows a liturgical calendar, I want to select my tradition's lectionary, so that the app can show the correct daily and seasonal readings.

#### Acceptance Criteria

1. WHEN the Denomination is selected, THE Setup_Flow SHALL offer the Lectionary options available for that Denomination.
2. WHEN the Denomination is CSI, THE System SHALL offer the CSI Almanac as a Lectionary option.
3. WHEN the Denomination is Catholic, THE System SHALL offer the Roman Catholic liturgical calendar as a Lectionary option.
4. WHEN the user selects a Lectionary, THE System SHALL store the selection in the `lectionary` preference.
5. WHERE no Lectionary is available for the selected Denomination, THE System SHALL allow the user to proceed without selecting a Lectionary.

### Requirement 10: Persisting Setup Choices

**User Story:** As a user, I want my setup choices saved, so that the app remembers my configuration across launches.

#### Acceptance Criteria

1. WHEN the user confirms a setup choice, THE System SHALL persist the choice to the Setup_Store.
2. THE System SHALL persist the following preferences: `denomination`, `canon`, `numberingScheme`, `namingConvention`, `uiLanguage`, `bibleLanguage`, `bibleTranslationId`, `lectionary`, and `setupCompleted`.
3. WHEN the App is relaunched after setup completion, THE System SHALL load all persisted setup preferences from the Setup_Store.
4. THE System SHALL persist setup choices using local storage without requiring a user account.

### Requirement 11: Re-running Setup Without Data Loss

**User Story:** As a user whose tradition or preferences change, I want to change my denomination and languages later without losing my notes, highlights, and bookmarks, so that my personal study is preserved.

#### Acceptance Criteria

1. THE Settings_Screen SHALL provide a control to re-run the Setup_Flow.
2. WHEN the user changes the Denomination through the Setup_Flow, THE System SHALL update the Canon, Numbering_Scheme, and Naming_Convention preferences to match the new Denomination.
3. WHEN the user changes any setup preference, THE System SHALL preserve all existing User_Annotation records.
4. WHEN the user changes the Bible_Language or Translation, THE System SHALL retain User_Annotation records by their verse references.

### Requirement 12: Canon Switch Affecting Annotated Verses

**User Story:** As a user who switches to a narrower canon, I want to be warned about annotations on books that will no longer be visible, so that I do not silently lose access to my study notes.

#### Acceptance Criteria

1. IF the user changes the Canon to a canon that excludes books containing existing User_Annotation records, THEN THE System SHALL warn the user before applying the change.
2. WHEN the Canon change is applied and User_Annotation records reference excluded books, THE System SHALL retain those User_Annotation records in storage.
3. WHEN the Canon is later changed to include the previously excluded books, THE System SHALL display the retained User_Annotation records on their original verses.
4. WHILE the Canon excludes a book, THE System SHALL hide User_Annotation records that reference verses in that book from the reading view.

### Requirement 13: Offline Translation Availability and Download

**User Story:** As a user setting up offline or on a slow connection, I want clear handling when my recommended translation is not yet downloaded, so that I can still proceed and download later.

#### Acceptance Criteria

1. IF the selected Translation is not downloaded AND no internet connection is available, THEN THE System SHALL allow the user to complete setup and SHALL mark the Translation for download when a connection becomes available.
2. WHEN the selected Translation is not downloaded AND an internet connection is available, THE System SHALL offer to download the Translation and SHALL display download progress.
3. WHEN a Translation download completes, THE System SHALL set the Translation's `isDownloaded` flag to true.
4. IF a Translation download fails, THEN THE System SHALL inform the user and SHALL allow the user to retry the download.
5. WHILE no Translation is downloaded for the selected Bible_Language, THE System SHALL present available offline content and SHALL indicate that the chosen Translation is pending download.

### Requirement 14: Accessible and Dual-Language Setup Screens

**User Story:** As an elderly, low-vision, or screen-reader user, I want the setup screens themselves to be accessible, so that I can configure the app independently.

#### Acceptance Criteria

1. THE Setup_Flow SHALL provide interactive elements with a minimum touch target of 48dp by 48dp.
2. WHILE Simplified Mode is active, THE Setup_Flow SHALL provide a minimum touch target of 56dp by 56dp.
3. THE Setup_Flow SHALL provide a content description for every interactive control and image so that TalkBack can announce each control.
4. WHEN the selected UI_Language uses a right-to-left script, THE Setup_Flow SHALL render its layout right-to-left.
5. THE Setup_Flow SHALL scale text with the system font size setting up to the supported text scale maximum.
6. WHERE the user prefers audio, THE Setup_Flow SHALL provide spoken prompts for each setup screen.

### Requirement 15: Deuterocanonical Visibility Toggle (Optional)

**User Story:** As a Protestant user curious about the deuterocanonical books, I want an optional toggle to show them, so that I can read them without switching my whole tradition.

#### Acceptance Criteria

1. WHERE the Canon is `protestant_66`, THE Settings_Screen SHALL provide a toggle to show Deuterocanonical_Books.
2. WHEN the user enables the Deuterocanonical_Books toggle, THE System SHALL display the Deuterocanonical_Books while retaining the Protestant ordering and Numbering_Scheme.
3. WHEN the user disables the Deuterocanonical_Books toggle, THE System SHALL hide the Deuterocanonical_Books from the reading view.

### Requirement 16: Show-Everything Multi-Canon Mode (Optional)

**User Story:** As a user exploring multiple traditions, I want a mode that shows all canons together, so that I can compare books across traditions.

#### Acceptance Criteria

1. WHEN the Canon is `all_canons`, THE System SHALL display books from all supported canons in a unified list.
2. WHEN the Canon is `all_canons`, THE System SHALL label each book group with its associated tradition so that the user can distinguish canon membership.
3. WHILE the Canon is `all_canons`, THE System SHALL allow the user to switch to a single-tradition Canon from the Settings_Screen.

### Requirement 17: Denomination-Correct Verse References When Sharing (Optional)

**User Story:** As a user sharing a verse, I want the reference formatted in my tradition's convention, so that recipients in my tradition recognize it.

#### Acceptance Criteria

1. WHEN the user shares a verse AND the Numbering_Scheme is `septuagint`, THE System SHALL format the shared reference using the Septuagint Psalm numbering.
2. WHEN the user shares a verse AND the Numbering_Scheme is `masoretic`, THE System SHALL format the shared reference using the Masoretic Psalm numbering.
3. WHEN the user shares a verse, THE System SHALL include the book name in the active Naming_Convention.

### Requirement 18: Denomination-Aware Daily Lectionary Readings (Optional / Future)

**User Story:** As a user following my tradition's calendar, I want daily readings from my lectionary, so that I read alongside my church.

#### Acceptance Criteria

1. WHERE a Lectionary is selected, THE System SHALL present the readings assigned to the current date by that Lectionary.
2. WHEN the current date has no assigned readings in the selected Lectionary, THE System SHALL present the next dated readings from that Lectionary.
3. WHILE no Lectionary is selected, THE System SHALL present the app's default daily verse instead of lectionary readings.
