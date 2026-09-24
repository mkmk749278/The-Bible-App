# Privacy Policy — Manna (Bible App)

**Last updated:** 2026-09-24
**App:** Manna (`com.manna.bible`)
**Contact:** mulakapati446@gmail.com

Manna is built privacy-first. We designed it so the app is fully useful **without an
account, without tracking, and without ads** — ever.

## The short version

- **No account is required** to use any core feature.
- **We do not collect, store, or sell your personal data.**
- **No analytics, no advertising, and no third‑party tracking SDKs** are in the app.
- **Your spiritual data stays on your device** — your notes, highlights, bookmarks,
  prayers, reading position and settings are stored locally and are **not** uploaded to
  us or to any cloud service. They are also excluded from Android's automatic Google
  cloud backup by design. (On Android 12 and later, Android's own phone-to-phone transfer
  can copy them directly to your new phone when you set it up; nothing passes through us.)
- **Optional AI features send what you ask to Google's Gemini service** — see below.

## What data the app handles

### Stored only on your device
The following never leaves your phone:
- Highlights, bookmarks, and notes
- Prayer journal entries and reflections
- Reading progress and your last reading position
- App settings (languages, theme, audio preferences, reminders)
- Downloaded Bible translations

You can erase all of it at any time by clearing the app's data or uninstalling the app.

### Network requests the app makes
Manna connects to the internet only for these purposes, and only to these hosts:

1. **`bible.helloao.org`** — the key‑less, MIT‑licensed *Free Use Bible API*, used to
   download Bible translations and to fetch streamed narrated‑audio links. Only generic
   content references (translation id, book, chapter) are sent. **No personal data is
   sent.**
2. **`generativelanguage.googleapis.com` (Google Gemini)** — *optional.* Used only when
   you use one of the AI features below and a Gemini API key has been configured for the
   build (otherwise these features are hidden or disabled and no request is made):
   - **Explain this passage** — the passage text and reference, your Bible language, and
     the church tradition you chose during setup (if any).
   - **Crisis companion** ("Tell me what's happening") — **the words you type** about your
     situation, your Bible language, whether it is night-time, and your tradition.
   - **Verse cards** ("find a verse for…") — the occasion you describe, your Bible
     language and your tradition.
   - **Sermon builder** — the passage reference, your tradition, language and the kind of
     congregation you chose.

   Manna does not send your name, account, contacts, location or any device identifier,
   and Manna does not store what you type. Requests identify the *app* (its package name
   and signing-certificate fingerprint) so the API key cannot be misused by other apps.
   Google processes these requests under the Gemini API terms. Because what you type in
   the crisis companion or verse cards may be personal, **please avoid including names or
   other identifying details.**

All network traffic uses encrypted HTTPS. Offline‑first features (reading bundled
Bibles, on‑device text‑to‑speech, notes, prayers, daily verse) work with no network at
all.

### On‑device AI
On supported devices, "Explain this passage" can run **on‑device** via Gemini Nano, in
which case nothing is sent off the device. The other AI features always use the online
service described above.

### Stealth (Persecution) Mode
When Stealth Mode is on, your PIN is never stored — only a salted PBKDF2 hash. The app
opens behind a calculator disguise, re-locks whenever you leave it, hides its content
from the Recents screen and screenshots, blanks the home-screen widget and does not show
reminder notifications.

## Permissions

- **Internet / Network state** — to download Bibles and stream narrated audio.
- **Notifications** — to show the optional daily‑verse reminder you choose.
- **Run after restart** — to re‑arm your daily reminder after the phone reboots.

We request no location, contacts, microphone, camera, or storage permissions.

## Children's privacy

Manna does not knowingly collect any personal information from anyone, including
children. The app contains no ads and no data collection. The optional AI features
described above are processed by Google and are not directed at children.

## Changes to this policy

If this policy changes, the updated version will be published at this URL with a new
"Last updated" date.

## Contact

Questions about privacy? Email **mulakapati446@gmail.com**.
