# CLAUDE.md — Bible App (Manna) Project Instructions

> **This file is the single source of truth for all AI assistants and developers working on this codebase.**
> Read this fully before making any changes.

---

## Project Overview

**Manna** is an offline-first, India-focused Bible app designed to serve the most underserved Christians in the world — the illiterate, the elderly, the persecuted, and the rural. It prioritizes:

- **Dual language architecture** — App UI language and Bible text language are independent (e.g., Tamil UI + English Bible, or Hindi UI + Telugu Bible)
- **Offline-first** — Every core feature works without internet. Internet is for sync, updates, and premium AI only.
- **Audio-first** — 60%+ of target users prefer listening over reading
- **On-device AI** — Gemini Nano (Android 16+) for offline verse explanations, topical search, and sermon tools
- **Privacy-first** — No account required. No tracking. No ads. Ever.

### Naming

- **App name**: Manna (மன்னா) — "Daily bread from heaven" (Exodus 16)
- **India launch alternate**: Vaan (வான்) — Tamil for "sky/heaven"
- **Package name**: `com.manna.bible` (Android)
- **Internal codename**: `manna`

---

## Repository Structure

```
The Bible App/
├── CLAUDE.md                          # This file — project instructions
├── bible-app-future.jsx.txt           # Product blueprint & requirements (interactive React doc)
├── app/                               # Android app (Kotlin + Jetpack Compose)
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/manna/bible/
│   │   │   │   ├── data/              # Room database, repositories, data sources
│   │   │   │   ├── domain/            # Use cases, domain models
│   │   │   │   ├── ui/                # Compose screens, components, themes
│   │   │   │   ├── ai/                # Gemini Nano integration (ML Kit GenAI)
│   │   │   │   ├── audio/             # ExoPlayer, TTS, Bible Brain audio
│   │   │   │   ├── widget/            # Glance home screen widgets
│   │   │   │   └── di/                # Hilt modules
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                      # Unit tests
│   │   └── androidTest/               # Instrumented tests
├── web/                               # PWA (Next.js / Vite + React) — future
├── .github/
│   └── workflows/
│       ├── android-build.yml          # CI: build debug APK on every PR
│       ├── android-release.yml        # CD: build signed release APK + AAB
│       └── lint.yml                   # Lint and static analysis
├── gradle/
├── build.gradle.kts                   # Root build file
├── settings.gradle.kts
├── gradle.properties
└── local.properties                   # Local SDK paths (NOT committed)
```

---

## Tech Stack

### Android (Primary Platform)

| Layer          | Technology                                        |
|----------------|---------------------------------------------------|
| Language       | **Kotlin** (latest stable, currently 2.1+)        |
| UI             | **Jetpack Compose** with Material 3               |
| Navigation     | Jetpack Navigation Compose (type-safe routes)     |
| Database       | **Room** (SQLite) for Bible text + user data       |
| DI             | **Hilt** (Dagger)                                 |
| State          | ViewModel + StateFlow + DataStore (preferences)   |
| AI             | **ML Kit GenAI Prompt API** (Gemini Nano)          |
| Audio          | **ExoPlayer** (Media3) + Android TTS               |
| Bible data     | **Bible Brain API** (Digital Bible Platform)        |
| Widgets        | **Glance** (Jetpack Compose for widgets)           |
| Image loading  | **Coil** (Compose-native)                          |
| Networking     | **Retrofit** + OkHttp (for Bible Brain API)        |
| Serialization  | **Kotlinx Serialization** (JSON)                   |
| Testing        | JUnit 5, Turbine (Flow testing), Compose Test      |
| Min SDK        | **26** (Android 8.0) — covers 95%+ of India        |
| Target SDK     | **36** (Android 16) — required for Gemini Nano     |
| Compile SDK    | **36**                                             |

### PWA / Web (Future)

| Layer     | Technology                                          |
|-----------|-----------------------------------------------------|
| Framework | Next.js 15+ (App Router) or Vite + React            |
| Offline   | Service Workers + IndexedDB (Dexie.js)               |
| AI        | Gemini Flash API with IndexedDB response caching     |
| Styling   | Vanilla CSS with design tokens from blueprint        |
| Audio     | Web Audio API + HTML5 `<audio>`                      |

### Backend / Shared Services

| Service        | Technology                                       |
|----------------|--------------------------------------------------|
| Bible content  | **Bible Brain API** (bible.cloud) — 2000+ translations |
| Auth (opt-in)  | Firebase Auth — Google Sign-In + Phone Auth       |
| Sync (opt-in)  | Firebase Firestore (encrypted user data sync)     |
| Analytics      | Firebase Analytics (privacy-first, no PII)        |
| Crash reports  | Firebase Crashlytics                              |
| Push (future)  | Firebase Cloud Messaging                          |

---

## Architecture

### Clean Architecture (3-layer)

```
┌─────────────────────────────────────────────┐
│  UI Layer (Compose screens + ViewModels)     │
│  - Screens observe StateFlow from ViewModels │
│  - ViewModels call Use Cases                 │
├─────────────────────────────────────────────┤
│  Domain Layer (Use Cases + Models)           │
│  - Pure Kotlin, no Android dependencies      │
│  - Business logic lives here                 │
├─────────────────────────────────────────────┤
│  Data Layer (Repositories + Data Sources)    │
│  - Room DAOs for local Bible + user data     │
│  - Retrofit services for Bible Brain API     │
│  - DataStore for preferences                 │
│  - ML Kit wrapper for Gemini Nano            │
└─────────────────────────────────────────────┘
```

### Key Architecture Rules

1. **ViewModels never import Android framework classes** (except `ViewModel` itself)
2. **Repositories are the single source of truth** — screens never access DAOs directly
3. **Use Cases are optional** — only create them when business logic is complex enough to warrant it
4. **All data operations are suspend functions or Flows** — no blocking calls on main thread
5. **Navigation uses type-safe route objects** — no string-based navigation
6. **All user data is local-first** — cloud sync is a background operation that never blocks the UI

---

## Data Model

### Core Entities (Room)

```kotlin
// Bible content (pre-populated from Bible Brain API downloads)
@Entity Translation(id, name, languageCode, scriptDirection, isDownloaded, sizeBytes)
@Entity Book(id, translationId, name, abbreviation, testament, orderIndex)
@Entity Chapter(id, bookId, number)
@Entity Verse(id, chapterId, number, text, audioUrl?)

// User data (local, optionally synced)
@Entity Highlight(id, verseId, color, createdAt)
@Entity Bookmark(id, verseId, label?, createdAt)
@Entity Note(id, verseId?, chapterId?, content, createdAt, updatedAt)
@Entity ReadingProgress(id, planId, dayIndex, completedAt)

// Features
@Entity PrayerEntry(id, content, status, linkedVerseIds, createdAt, answeredAt?)
@Entity FaithMoment(id, date, event, verseId, reflection, createdAt)
@Entity AlarmConfig(id, hour, minute, daysOfWeek, verseSource, isEnabled)
@Entity CalendarEvent(id, date, name, type, verseIds, description)
@Entity VoiceLegacy(id, verseId, audioPath, recipientName, unlockCondition, createdAt)
@Entity SermonNote(id, date, verseIds, content, tags, createdAt)
```

### DataStore Preferences

```kotlin
// User preferences (DataStore Proto or Preferences)
uiLanguage: String           // App UI language code (e.g., "ta", "hi", "en")
bibleLanguage: String        // Bible text language code
preferredMode: String        // "read", "listen", "both"
faithJourney: String         // "new", "growing", "mature", "pastor"
simplifiedMode: Boolean      // Elder Mode / Oral Bible combined
darkMode: String             // "system", "dark", "light"
textScale: Float             // 1.0 default, up to 2.0 for accessibility
ttsSpeed: Float              // 0.5 to 2.0
dailyVerseEnabled: Boolean
dailyVerseTime: String       // "07:00"
stealthModeEnabled: Boolean
stealthPin: String           // encrypted
lastReadPosition: String     // "GEN.1.1" format
```

---

## Build System & CI/CD

### GitHub Actions — Android Builds

We use **GitHub Actions** for all CI/CD. All workflows live in `.github/workflows/`.

#### CI: Debug Build on Every PR

**File**: `.github/workflows/android-build.yml`

Triggers on every PR to `main` and `develop`:
- Runs lint (`./gradlew lint`)
- Runs unit tests (`./gradlew testDebugUnitTest`)
- Builds debug APK (`./gradlew assembleDebug`)
- Uploads debug APK as workflow artifact

#### CD: Signed Release APK + AAB

**File**: `.github/workflows/android-release.yml`

Triggers on:
- Push of a version tag (`v*.*.*`, e.g., `v1.0.0`)
- Manual dispatch (workflow_dispatch) for ad-hoc releases

Steps:
1. Checkout code
2. Set up JDK 17 (temurin)
3. Cache Gradle dependencies
4. Decode keystore from GitHub Secret (base64-encoded)
5. Build signed release APK: `./gradlew assembleRelease`
6. Build signed release AAB: `./gradlew bundleRelease`
7. Upload APK + AAB as workflow artifacts
8. Create GitHub Release with APK + AAB attached
9. *(Future)* Upload AAB to Play Store via Gradle Play Publisher plugin

### GitHub Secrets

All secrets are stored in **GitHub Secrets** (Settings → Secrets and Variables → Actions). **NEVER** commit secrets, keys, API keys, or keystores to the repository.

| Secret Name                   | Description                                         | Format            |
|-------------------------------|-----------------------------------------------------|--------------------|
| `KEYSTORE_BASE64`             | Release keystore file, base64-encoded                | Base64 string      |
| `KEYSTORE_PASSWORD`           | Keystore password                                    | Plain text         |
| `KEY_ALIAS`                   | Key alias within the keystore                        | Plain text         |
| `KEY_PASSWORD`                | Key password (often same as keystore password)       | Plain text         |
| `BIBLE_BRAIN_API_KEY`         | API key for Bible Brain / Digital Bible Platform     | Plain text         |
| `FIREBASE_GOOGLE_SERVICES`    | `google-services.json` contents, base64-encoded      | Base64 string      |
| `PLAY_STORE_SERVICE_ACCOUNT`  | Google Play service account JSON for AAB upload       | Base64 JSON        |

#### How secrets are used in workflows:

```yaml
# Decode keystore from base64 secret
- name: Decode Keystore
  run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/release-keystore.jks

# Pass signing config via environment variables
- name: Build Release APK
  env:
    KEYSTORE_PATH: app/release-keystore.jks
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew assembleRelease

# Decode google-services.json
- name: Decode Google Services
  run: echo "${{ secrets.FIREBASE_GOOGLE_SERVICES }}" | base64 -d > app/google-services.json
```

#### Signing config in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### API keys in the app:

Bible Brain API key and other runtime secrets are injected via `BuildConfig`:

```kotlin
// In app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "BIBLE_BRAIN_API_KEY",
            "\"${System.getenv("BIBLE_BRAIN_API_KEY") ?: ""}\"")
    }
}
```

For local development, set these in `local.properties` (which is `.gitignore`d):

```properties
BIBLE_BRAIN_API_KEY=your_key_here
```

---

## Branching Strategy

```
main          — Production-ready code. Protected branch. Release tags here.
develop       — Integration branch. PRs merge here first.
feature/*     — Feature branches (e.g., feature/pastor-mode, feature/3am-crisis)
bugfix/*      — Bug fix branches
release/*     — Release preparation branches (version bumps, changelog)
hotfix/*      — Emergency production fixes (branch from main, merge to both main + develop)
```

### Commit Message Convention

Use **Conventional Commits**:

```
feat: add Pastor Mode 5-step flow
fix: audio player not resuming after phone call
docs: update CLAUDE.md with widget guidelines
refactor: extract verse formatting to shared component
test: add unit tests for reading plan progress
chore: bump Compose BOM to 2026.06.00
perf: optimize Room query for full-text search
a11y: increase touch target size for Elder Mode buttons
```

### PR Requirements

- Must pass CI (lint + tests + build)
- Must have a descriptive title and body
- Must reference the related feature/issue
- Must not increase APK size by more than 500KB without justification

---

## Design System

### Color Tokens

All colors are defined as a role-based palette with light and dark instances. Do not use hardcoded hex values in screens — read the active palette via `MannaTheme.colors.<token>` (see `ui/theme/Color.kt` and `ui/theme/Theme.kt`).

```kotlin
// ui/theme/Color.kt — role-based tokens, two palettes
data class MannaPalette(
    bg, surface, card, border,        // structure
    gold, goldDim,                    // sacred accent
    ink, muted, soft,                 // text (primary / tertiary / secondary)
    lavender, sage, red, cyan, orange // semantic accents
)

// Default: LightMannaPalette — "morning sunlight entering a church"
//   warm white bg (#FAF7EF), soft cream surfaces, deep navy ink (#1F2D3D),
//   muted gold (#8A671C), sage (#3F7A5C)
// Dark-mode variant: DarkMannaPalette — near-black navy bg (#080C14),
//   gold (#C9952A), cream ink (#EDE3C8)
```

Text-role tokens (`gold`, `soft`, `ink`) must meet a 4.5:1 contrast ratio against `bg` and `card` in both palettes.

### Typography

- **Primary font**: Inter (Google Fonts) — clean, modern, excellent Unicode support
- **Bible text font**: Noto Serif (Google Fonts) — supports all Indian scripts
- **Monospace**: JetBrains Mono (for Strong's numbers, interlinear references)

### Design Principles

Per the **UX Master Design Directive**, Manna is a spiritual companion, not a productivity tool. When choosing between more features and more peace, choose more peace.

1. **Light by default** — the app should feel like *morning sunlight entering a church*: warm white, soft cream, deep navy, muted gold, sage. The dark palette is the dark-mode variant (follows system setting). Avoid pure black, neon colors, and aggressive gradients.
2. **Gold as the sacred accent** — muted gold is the primary accent. It conveys reverence without being ostentatious.
3. **Generous whitespace** — Bible text needs room to breathe. Never crowd the reading view. Silence is part of the experience.
4. **Large touch targets** — minimum 48dp for all interactive elements. 56dp+ in Simplified Mode.
5. **No clutter** — the reading screen shows the text and nothing else. All tools are behind gestures or bottom sheets. The interface must disappear.
6. **Calm motion** — 250–400ms fade / soft slide / scale only. No bounce, elastic, or flashy transitions.
7. **Never feel** busy, corporate, commercial, social-media-like, gamified, or noisy. No feeds, no ads, no promotional content.

### App Structure (UX directive)

- **Primary destinations**: Home (Continue Reading · Today's Verse · Continue Listening), Listen, Search, Library — via a calm bottom navigation bar.
- **Reading screen** is the most important surface (~60% of design effort); it opens full-screen above the navigation bar so the text dominates.
- **Audio** feels integrated: a minimal mini player, available wherever relevant.
- **Settings contain complexity** — the main interface stays simple; advanced options stay hidden until needed.

---

## Feature Flags

Use a `FeatureFlags` object to gate in-progress features:

```kotlin
// domain/FeatureFlags.kt
object FeatureFlags {
    const val PASTOR_MODE = true
    const val GEMINI_NANO_AI = true         // Requires Android 16+
    const val STEALTH_MODE = false           // Phase 2
    const val GRIEF_COMPANION = false        // Phase 2
    const val SCRIPTURE_INHERITANCE = false   // Phase 2
    const val CHURCH_MODE = false            // Phase 3
    const val PRAYER_WALL = false            // Phase 3 — needs moderation system
    const val FAMILY_DEVOTION = false        // Year 2
    const val WEAR_OS = false                // Year 2
    const val ANDROID_AUTO = false           // Year 2
}
```

Gate features in navigation and UI:

```kotlin
if (FeatureFlags.PASTOR_MODE) {
    composable<PastorModeRoute> { PastorModeScreen() }
}
```

---

## Accessibility Requirements

These are **non-negotiable**:

1. **All text scales with system font size** — use `sp` units, never fixed `dp` for text
2. **Content descriptions on all icons and images** — no decorative-only icons without description
3. **Minimum contrast ratio of 4.5:1** for all text against backgrounds
4. **Minimum touch target of 48dp × 48dp** for all interactive elements
5. **Screen reader support** — all screens must be navigable with TalkBack
6. **RTL layout support** — for Urdu, Arabic Bible translations
7. **Simplified Mode** — 4 giant buttons, 150% scale, auto-play audio, slower TTS

---

## Performance Targets

| Metric                         | Target                    |
|--------------------------------|---------------------------|
| Cold start to first verse      | < 1.5 seconds             |
| Chapter load (Room query)      | < 50ms                    |
| Verse search (full-text)       | < 200ms for 31,000 verses |
| Gemini Nano response           | < 100ms (on-device)       |
| APK size (core)                | < 15MB                    |
| APK size (with 1 translation)  | < 20MB                    |
| Memory usage (reading)         | < 80MB RAM                |
| Offline capability             | 100% of core features     |

---

## Security & Privacy

1. **No analytics without consent** — analytics is opt-in during onboarding
2. **No personal data leaves the device** by default — sync is explicit opt-in
3. **Stealth Mode data** — all Bible data encrypted with AES-256 when stealth mode is active
4. **No third-party SDKs that phone home** — review every dependency
5. **Prayer journal, notes, highlights** — stored in encrypted Room database when stealth mode is on
6. **Firebase** — configured with minimal data collection (`analyticsCollectionEnabled = false` by default)
7. **API keys** — stored in GitHub Secrets, injected via BuildConfig, never in source code
8. **Keystore** — never committed. Base64-encoded in GitHub Secrets, decoded during CI/CD only.

---

## Testing Strategy

### Unit Tests (JUnit 5 + Turbine)
- All ViewModels: test state emissions for every user action
- All Use Cases: test business logic with fake repositories
- All Repositories: test with in-memory Room database
- Run with: `./gradlew testDebugUnitTest`

### UI Tests (Compose Test)
- Critical user flows: onboarding, Bible reading, search, bookmarking
- Accessibility checks via `ComposeTestRule.onNode().assertHasContentDescription()`
- Run with: `./gradlew connectedDebugAndroidTest`

### Manual Testing Checklist (before release)
- [ ] App works fully offline (airplane mode)
- [ ] Dual language: Tamil UI + English Bible text renders correctly
- [ ] Audio plays and pauses correctly with interruptions (phone call, notification)
- [ ] Daily alarm fires at correct time and shows verse
- [ ] Home screen widget updates daily
- [ ] Text scales correctly at 200% system font size
- [ ] TalkBack can navigate all screens
- [ ] APK size under 20MB
- [ ] App starts under 1.5s on a mid-range device

---

## Release Process

### Version Naming

Use **Semantic Versioning**: `MAJOR.MINOR.PATCH`

- `1.0.0` — Phase 1 launch (Bible reader + audio + daily verse + Pastor Mode)
- `1.1.0` — New feature (e.g., add search)
- `1.0.1` — Bug fix release

### Steps to Release

1. Create a `release/v1.x.x` branch from `develop`
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`
3. Update `CHANGELOG.md`
4. PR to `main`, get approval, merge
5. Tag the merge commit: `git tag v1.x.x && git push --tags`
6. GitHub Actions automatically:
   - Builds signed release APK + AAB
   - Creates a GitHub Release with artifacts
   - *(Future)* Uploads AAB to Google Play via service account
7. Merge `main` back into `develop`

### Play Store Release (AAB)

The AAB (Android App Bundle) is uploaded to Google Play Console. Google Play generates optimized APKs for each device configuration (screen density, ABI, language).

For automated upload, we use the **Gradle Play Publisher** plugin (`com.github.triplet.play`):

```kotlin
// app/build.gradle.kts
plugins {
    id("com.github.triplet.play") version "3.x.x"
}

play {
    serviceAccountCredentials.set(file("play-service-account.json"))
    track.set("internal")  // internal → alpha → beta → production
    defaultToAppBundles.set(true)
}
```

The service account JSON is decoded from `PLAY_STORE_SERVICE_ACCOUNT` GitHub Secret during CI.

---

## Important Files — Quick Reference

| File | Purpose |
|------|---------|
| `CLAUDE.md` | This file — project instructions |
| `bible-app-future.jsx.txt` | Product blueprint with all 22 feature ideas + analysis |
| `app/build.gradle.kts` | Android app build config, dependencies, signing |
| `build.gradle.kts` | Root build file (Gradle plugins, repositories) |
| `.github/workflows/android-release.yml` | Release build + signing + artifact upload |
| `app/src/main/java/com/manna/bible/ui/theme/` | Design system (colors, typography, shapes) |
| `app/src/main/java/com/manna/bible/data/` | Room database, DAOs, repositories |
| `app/src/main/java/com/manna/bible/ai/` | Gemini Nano wrapper |
| `gradle.properties` | Gradle JVM args, Android config |
| `local.properties` | Local SDK path + API keys (NEVER commit) |

---

## .gitignore Essentials

These must ALWAYS be in `.gitignore`:

```
local.properties
*.jks
*.keystore
google-services.json
play-service-account.json
/build/
/app/build/
.gradle/
.idea/
*.apk
*.aab
```

---

## Common Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config env vars)
./gradlew assembleRelease

# Build release AAB for Play Store
./gradlew bundleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lint

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean

# Check dependency updates
./gradlew dependencyUpdates

# Generate signed APK locally (set env vars first)
export KEYSTORE_PATH=path/to/keystore.jks
export KEYSTORE_PASSWORD=yourpassword
export KEY_ALIAS=youralias
export KEY_PASSWORD=yourkeypassword
./gradlew assembleRelease
```

---

## Phase Roadmap (from blueprint)

### Phase 1 — Foundation (Months 1–3) ← **WE ARE HERE**
- [x] Project setup, CI/CD, signing
- [ ] Core offline Bible reader + Room database
- [ ] Dual language setup (UI lang ≠ Bible lang)
- [ ] Bible text rendering (Compose, Noto Serif)
- [ ] Highlights, bookmarks, notes
- [ ] Audio player (ExoPlayer + Bible Brain)
- [ ] Daily alarm + verse of the day
- [ ] Jesus Events Calendar
- [ ] Pastor Mode (5-step flow)
- [ ] Home screen widget (Glance)
- [ ] Onboarding flow (4 screens)
- [ ] Search (text + topical)
- [ ] Offline download manager

### Phase 2 — Soul Depth (Months 4–6)
- [ ] 3AM Crisis Mode
- [ ] Grief Companion (30-day journey)
- [ ] Prayer journal + Faith Timeline
- [ ] Scripture Card Generator (WhatsApp-ready)
- [ ] Fasting Companion
- [ ] Scripture Inheritance (Voice Legacy)
- [ ] Stealth / Persecution Mode ← moved up from Phase 3

### Phase 3 — AI + Ministry (Months 7–10)
- [ ] Gemini Nano offline AI theologian
- [ ] Simplified Mode (Oral Bible + Elder Mode combined)
- [ ] Village Pastor Sermon Helper
- [ ] Church Mode (Sermon Companion)
- [ ] Anonymous Prayer Wall (with moderation)

### Phase 4 — Futuristic (Year 2)
- [ ] Scripture Memory Palace
- [ ] WearOS companion
- [ ] Android Auto devotionals
- [ ] Family Devotion Mode (room codes)
- [ ] AR Biblical World Map
- [ ] Bible Dramatization (multi-voice audio)

---

*Last updated: 2026-06-09*
*Maintained by the Manna development team*
