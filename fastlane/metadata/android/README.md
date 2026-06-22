# Play Store listing metadata

Localized store-listing text in the [fastlane supply][supply] layout. Each locale
directory holds:

- `title.txt` — app name (**max 30 characters**)
- `short_description.txt` — tagline (**max 80 characters**)
- `full_description.txt` — long description (**max 4000 characters**)
- `changelogs/<versionCode>.txt` — release notes for that build (**max 500 chars**)

## Locales

| Dir | Language | Source |
|-----|----------|--------|
| `en-US` | English (default) | original copy |
| `ta-IN` | Tamil | translated from `en-US` |
| `hi-IN` | Hindi | translated from `en-US` |
| `te-IN` | Telugu | translated from `en-US` |
| `ml-IN` | Malayalam | translated from `en-US` |

## ⚠️ Before publishing the localized listings

1. **Native-speaker review.** The `ta/hi/te/ml` text was machine-translated from
   the English source and should be proofread by a native speaker for tone and
   accuracy before going live — consistent with the project's incremental
   localization approach (see the `MissingTranslation` lint note in
   `app/build.gradle.kts`).
2. **Verify character limits in the console.** Play counts **Unicode characters**,
   and Indic scripts count each combining mark separately, so a title that looks
   short can still exceed 30. Paste each `title.txt` / `short_description.txt`
   into the Play Console field (or count with a Unicode-aware tool) and trim if it
   flags over-limit. Shorten the title first if needed (e.g. drop "ഓഡിയോ/ఆడియో/
   ஒலி/ऑडियो").

## Adding the listings in Play Console

Per locale: **Store presence → Main store listing → Manage translations → Add
translations** → pick the locale → paste `title`, `short_description`,
`full_description`. Add release notes per locale under the release's notes editor.
(Or let fastlane `supply` / Gradle Play Publisher upload them once a listing
strategy is configured — currently the CD pipeline leaves listings untouched.)

[supply]: https://docs.fastlane.tools/actions/supply/
