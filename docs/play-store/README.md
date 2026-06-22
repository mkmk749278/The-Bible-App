# Publishing Manna to Google Play

This folder contains everything needed to create and operate the **Manna**
(`com.manna.bible`) app on the Google Play Console.

| Document | What it covers |
|----------|----------------|
| [`01-create-app-guide.md`](01-create-app-guide.md) | Step-by-step: creating the app in Play Console, App Access, store listing, releases. |
| [`02-data-safety-and-rating.md`](02-data-safety-and-rating.md) | Ready-to-paste answers for the **Data Safety** form and the **IARC content-rating** questionnaire. |
| [`03-graphics-checklist.md`](03-graphics-checklist.md) | Exact sizes + content briefs for every required store graphic. |
| [`04-automated-publishing.md`](04-automated-publishing.md) | How the Gradle Play Publisher CD pipeline works and how to enable it. |

## Current release facts (keep in sync with `app/build.gradle.kts`)

| Field | Value |
|-------|-------|
| Package / Application ID | `com.manna.bible` |
| versionName | `0.2.3` |
| versionCode | `5` |
| minSdk / targetSdk | 26 / 35 |
| Privacy policy | [`/PRIVACY_POLICY.md`](../../PRIVACY_POLICY.md) (must be hosted at a public URL) |
| Store listing text | [`/fastlane/metadata/android/en-US/`](../../fastlane/metadata/android/en-US/) |

## The one thing automation cannot do

The Google Play Developer API **cannot create a brand-new app**. You must create
the app once through the Play Console website and upload at least one AAB
manually. After that, the CD pipeline ([`04`](04-automated-publishing.md)) can
take over uploads. Start with [`01-create-app-guide.md`](01-create-app-guide.md).
