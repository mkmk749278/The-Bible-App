# Changelog

All notable changes to Manna are documented here. Versions follow
[Semantic Versioning](https://semver.org/).

## [0.2.3] — 2026-06-21

### Added
- **Audio keeps playing in the background** — read-aloud and narrated audio now
  continue with the app backgrounded or the screen off, via a media-playback
  foreground service with a notification (Play / Pause / Stop).
- **Audio focus** — playback pauses for a phone call or another app's audio and
  resumes when it ends, so Manna never talks over a call.
- **Bible downloads run in the background** — a download keeps going when the app is
  backgrounded, with an "x of y chapters" progress notification.

### Changed
- **All Prayers-hub prayers now appear in your Bible language** — the Rosary
  (Japamala), Stations of the Cross, the Jesus Prayer, Paraloka and the Sramanikal
  40-day observance show their recited prayers and meditations in the Bible language
  you chose, not the app's interface language (falling back to English where a
  translation isn't available yet).

## [0.2.2] — 2026-06-20

### Changed
- Privacy hardening for the first Google Play release: user notes, prayers and
  highlights are excluded from Android cloud backup (`allowBackup=false`), and the app
  declares an explicit HTTPS‑only network policy.

## [0.2.1] — 2026-06-20

### Added
- **Narrated chapter audio** — streams human‑narrated audio through ExoPlayer when
  available, with automatic fallback to on‑device text‑to‑speech (offline or on any
  error), so a listener always hears something.
- **Simplified / Elder Mode in the reader** — opening a chapter by navigation now
  reads it aloud automatically, and the reader's controls and verse rows grow to large
  56dp touch targets for elderly and non‑literate users.
- **The Rosary (Japamala) prayers** now display in full, with authentic prayer texts in
  Telugu, Hindi, Tamil and Malayalam (Sign of the Cross, Apostles' Creed, Our Father,
  Hail Mary, Glory Be, Fatima prayer, Hail Holy Queen).

### Changed
- The daily reminder now supports multiple custom prayer‑bell times.
- The calendar can share the day's verse as a styled image card.

### Fixed
- "Explain this passage" now shows the real reason when an explanation can't load.

## [0.2.0] — 2026-06-19

- First keyed/signed release: offline Bible reader with five bundled translations
  (English WEB + Tamil, Hindi, Telugu, Malayalam IRV), dual‑language setup, highlights /
  bookmarks / notes, text‑to‑speech read‑aloud, search, daily verse + reminder,
  liturgical calendar, the Prayers hub (Stations of the Cross, Rosary, Jesus Prayer,
  and more), Church Mode, Sermon Helper, and "Explain this passage" (cloud + on‑device
  Gemini Nano).
