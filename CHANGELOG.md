# Changelog

All notable changes to Manna are documented here. Versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed
- **Your notes, highlights and prayers can no longer be wiped by an app update.** The
  database used to be silently rebuilt from scratch if an upgrade ever lacked a schema
  migration; it now always migrates in place (a migration test guards every upgrade).
- **A damaged settings file no longer makes the app crash on every launch.** Settings
  reset to defaults instead, so nobody has to "Clear data" and lose their notes.
- **AI answers no longer come back empty or cut off.** Gemini 2.5 Flash spends part of
  its answer budget on hidden "thinking"; the Crisis companion, verse cards, sermon
  builder and passage explanations now switch thinking off so the budget goes to the answer.
- **Stealth Mode re-locks when you leave the app.** Previously the disguised calculator
  only came back after the app was fully closed. While Stealth Mode is on, Recents shows a
  blank card instead of scripture, screenshots are blocked, the home-screen widget shows no
  verse, and daily reminders are not shown.
- Entering the Stealth PIN no longer freezes the screen on slower phones.
- Read-aloud keeps going in the background from one chapter to the next with continuous play.
- Tapping play on a slow connection starts on-device reading within a few seconds instead of
  waiting up to half a minute for narrated audio.
- The reader opens as soon as the built-in Bibles finish installing on first launch, instead of
  showing a "download" prompt.
- Notification icons show correctly (monochrome) in the status bar.
- **Verses merged into a neighbour now resolve.** The Telugu IRV prints 103 verses inside
  the verse before them (e.g. 1 Corinthians 4:15–16 is one verse numbered 15, 2 Timothy
  3:16–17 is verse 16). Links to those verses — the daily verse and widget, devotions,
  Crisis/Grief/Fasting companions, the calendar, cards, AI answers and search — now open
  or show the verse that carries them, and highlights, bookmarks and notes made on them
  (e.g. in English) appear on that verse in Telugu.

### Added
- **Crisis Mode now shows how to reach a person right away:** Tele-MANAS (14416, free, 24x7,
  20+ languages) and the 112 emergency number, with one-tap dialling.
- The Crisis AI companion says plainly that what you type is sent to Google Gemini, and it
  shows a gentle message instead of nothing if an answer can't be found.

### Changed
- **Targets Android 16 (API 36)** with Android Gradle Plugin 8.10.1 — Google Play has
  required this for app updates since 31 August 2026.

### Security
- The Gemini API key is sent in a request header instead of the URL, and requests carry
  the app's package/certificate identity so the key can be restricted to Manna in Google
  Cloud.

## [0.2.9] — 2026-06-28

### Added
- **Church Mode liturgies from bundled data** — the Holy Mass and the CSI Holy Communion
  now come from bundled, validated liturgy files, with the framing text localized.
- **AI features speak your Bible language** — Crisis companion, sermon builder, verse cards
  and "Explain this passage" labels are localized for Tamil, Telugu, Hindi and Malayalam.

### Changed
- Unit tests run in parallel with hard timeouts so CI can no longer hang.

## [0.2.8] — 2026-06-26

### Added
- **Indian Cultural Lens** — "Explain this passage" frames its answer for your tradition.
- **Persecution-Aware Comfort** — offline Scripture in Crisis Mode for family pressure,
  livelihood threats, physical danger, social exclusion and doubt.
- **Crisis AI companion** — describe what is happening and receive one relevant passage.
- **Oral Bible AI** — hear an explanation read aloud.
- **AI Sermon Builder** — a full, editable outline from a passage reference.
- **Context-Aware Verse Cards** — describe an occasion and get a passage to share.

### Changed
- The in-app interface-language picker was removed: the interface follows the phone's
  language, while scripture, prayers and AI answers follow your chosen Bible language.
- "Explain this passage" answers in your Bible language.
- All translations of the interface ship to every device (no Play Store language split).

## [0.2.7] — 2026-06-25

### Changed
- The theme defaults to dark (still changeable in Settings → Appearance).
- The verse of the day lives in the Calendar tab, shareable as a card.

### Fixed
- "Explain this passage" moved to a supported Gemini model after the old one was retired.

## [0.2.6] — 2026-06-23

### Added
- **Stealth (Persecution) Mode** — a disguised calculator lock with a PBKDF2-hashed PIN.
- Settings and Library screens; appearance preferences are remembered.

### Changed
- Releases can be cut with one click and upload to the Play Console internal track.

## [0.2.5] — 2026-06-22

### Added
- Play Store listing assets, localized store descriptions and a hosted privacy policy.

## [0.2.4] — 2026-06-22

### Added
- Elder / Simplified Mode text scaling across the whole app, including the book picker.
- TalkBack labels on interactive elements.

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
