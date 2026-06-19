# Manna (மன்னா) — The Bible App

An **offline-first, India-focused Bible app** for the most underserved Christians —
the illiterate, the elderly, the persecuted, and the rural.

- **Dual-language** — the app UI language and the Bible text language are independent
  (e.g. Tamil UI + English Bible).
- **Offline-first** — every core feature works without internet. Five Bibles ship in
  the app (English WEB + Tamil/Hindi/Telugu/Malayalam IRV); more download on demand.
- **Audio-first** — on-device text-to-speech and (where available) human-narrated audio.
- **Privacy-first** — no account, no tracking, no ads.

> Full project instructions live in [`CLAUDE.md`](CLAUDE.md). The product blueprint is
> [`bible-app-future.jsx.txt`](bible-app-future.jsx.txt), and design/feature roadmaps are
> under [`docs/`](docs/).

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · Hilt · DataStore · Retrofit/OkHttp ·
Media3/ExoPlayer · Glance widgets. Bible content comes from the key-less, MIT-licensed
[Free Use Bible API](https://bible.helloao.org/). Min SDK 26, target/compile SDK 35.

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests (JUnit 5 + Turbine + MockK)
./gradlew lint                   # Android lint
./gradlew :app:prepareBundledBibles   # regenerate the committed offline Bibles
```

The Android toolchain (lint + unit tests + APK) runs in CI on every pull request to
`main` via [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml).
Signed release APK + AAB are built by
[`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) on a
`v*.*.*` tag.
