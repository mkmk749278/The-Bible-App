# Changelog

All notable changes to Manna are documented here. Versions follow
[Semantic Versioning](https://semver.org/).

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
