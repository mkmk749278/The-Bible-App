# Manna — UX Redesign Roadmap

> Status: proposed (June 2026). Driven by a review of the live app screenshots, a
> competitive scan (YouVersion, Universalis/Laudate, Scripturly/Bibley/Got Questions),
> and the CSI Almanac / Roman Catholic lectionary model. This document is the single
> source of truth for the redesign and supersedes ad-hoc feature placement.

---

## 1. Why we're redesigning

The app shipped a lot of features quickly, but they're **siloed, under-utilised, and
the information architecture doesn't match how the target user (CSI / Catholic / Indian
Christian) actually lives their faith day-to-day.** Concretely:

### Gap analysis (from the live screenshots)

| # | Gap | Evidence |
|---|-----|----------|
| 1 | **Bottom nav wastes itself.** Home, Listen and Search overlap and each screen is ~70% empty. | Home = "Start reading" + "Listen" cards; **Listen tab = the same Listen card again**; Search = just a search bar + topic chips. |
| 2 | **The "calendar" is not a calendar.** It's a flat scrolling list of feasts. | "Jesus Calendar · 2026" — no month grid, no highlighted days, no fasting days, no daily readings, no reminders, no link to the daily verse or cards. |
| 3 | **The chosen tradition/lectionary is ignored.** Onboarding collects Catholic/CSI/Orthodox + a lectionary (and even leaks the raw id `rc_calendar`), then nothing uses it. | This is the **single biggest missed opportunity** — that choice should drive a real liturgical calendar with daily readings. |
| 4 | **Library is a dumping ground.** Translations + a flat list of 9 unrelated tools. | "I'm not okay", "Walking through grief", "Prayer Journal", "Fasting", "Share a card", "Daily reminder", "Jesus Calendar", "Pastor Mode", "Attribution" — no grouping or hierarchy. |
| 5 | **Pastor Mode is the wrong feature.** A 5-step empty notebook (Choose Passage → Observe → Outline → Illustrate → Apply) that explains nothing. | The real need: a CSI pastor / Catholic father reads e.g. **ఆదికాండం 2:1 (Genesis 2:1)** and **explains** it so people understand. That is a *verse-explanation* feature, not a sermon-prep form. |
| 6 | **Poor screen utilisation & onboarding polish.** Large empty areas; raw ids shown. | e.g. `rc_calendar` rendered verbatim on the "Liturgical calendar" and "All set" steps. |

### What comparable apps do (research)

- **YouVersion** — Home · Bible · **Plans** · Discover · **More**. The daily reading plan is a first-class destination; the long tail lives in a **More** menu. They recently *un-buried* Notes because hiding features hurt usage. → Surface the daily hub; group the rest; don't bury.
- **Universalis / Laudate** (the bar for the Catholic/CSI audience) — a full **liturgical calendar (1970–2300)** with **local calendars**, and a **"Today / Mass Today"** page that shows the day's **readings + feast/saint + reflection** in one place, fully offline. → This is the spine Manna is missing.
- **CSI Almanac** — themes / lessons / **lectionary readings** for Sundays + feasts (3-year cycle), weekday readings, a Psalms table, and collects. → CSI users expect *daily readings tied to the church calendar*.
- **Scripturly / Bibley / Got Questions** — "**tap any verse → a clear, contextual explanation**" (context, meaning, application, jargon-free). → This is the real "Pastor Mode."

Sources: youversion.com/blog · universalis.com/mass.htm · laudate (App Store) · csisynod.com · scripturly.app · faith.tools/bible-commentary

---

## 2. Target architecture

### Navigation: 3 tabs + a persistent mini-player

Collapse Home / Listen / Search into a single **Read** tab.

| Tab | Contents |
|-----|----------|
| **Read** | Search bar **on top** → continue-reading / the Bible reader → a persistent **mini audio player ("listen") docked at the bottom**. Verse actions: highlight · **Explain** · share-card · listen. Listen is no longer a tab. |
| **Calendar** | A real **month grid** — the new spiritual spine (see §3). |
| **More** | A clean, grouped hub of tools + settings (see §5). |

A **persistent mini-player** is available across tabs (Spotify-style), per the UX Master
Design Directive.

---

## 3. The Calendar (centerpiece)

A real month-grid liturgical calendar driven by the **tradition already chosen at
onboarding** (CSI Almanac / Roman Catholic lectionary / general / Orthodox).

- **Regular days + highlighted feasts** — liturgical-season colours; Christmas, Easter,
  Ash Wednesday, Pentecost, etc. marked.
- **Fasting days** marked (Lent, Fridays, Ember days, etc., per tradition) — pulls the
  Fasting Companion into the calendar.
- **Tap a day → day detail:** that day's **lectionary readings** (OT / Psalm / Epistle /
  Gospel per the cycle), the feast + collect, the day's verse, and one-tap
  **"Remind me"** and **"Share as card."**
- **Reminders live inside the calendar** (not a separate buried screen).
- A **"Today"** shortcut → the current day's readings (the Universalis "Mass Today" model).

This single surface ties together: calendar + lectionary + daily verse + reminders +
fasting + share-cards.

---

## 4. "Explain this passage" (replaces Pastor Mode)

In the reader, tap a verse (e.g. ఆదికాండం 2:1) → **"Explain"** → a plain-language
explanation (context · meaning · application) in the **UI language**.

- A **"For preaching"** depth toggle adds outline + cross-references + illustration prompts
  — so it serves *both* the layperson understanding 2:1 *and* the pastor/father preparing
  to teach it. The empty 5-step notebook is retired (or demoted to optional notes).

### Engine decision

There is **no keyless high-quality cloud LLM**, so the real trade-off is:

| Option | Reach | Offline | Cost / key | Quality |
|--------|-------|---------|------------|---------|
| **Cloud (Gemini Flash / Claude)** | All devices w/ network | No (cache responses) | **Needs an API key** | Highest |
| **On-device Gemini Nano (ML Kit GenAI)** | Only Pixel 8+/Galaxy S24-class on Android 14+/16 | Yes | None | Good, device-bound |

**Recommendation: cloud-primary hybrid.** Use **Gemini Flash** as the primary engine
(key via GitHub Secrets + `BuildConfig`, exactly like the existing `BIBLE_BRAIN_API_KEY`),
**cache explanations in Room** so each verse is fetched once then offline forever, and use
**on-device Nano automatically when present** as a free, fully-offline upgrade. Nano-only
would reach too few of the India-focused target devices to be the primary path.

*(Confirm the key/engine before Phase C build.)*

---

## 5. The "More" tab (replaces the Library dump)

Grouped cards instead of a flat list:

- **Library** — Translations (download / switch / delete).
- **Pray** — Prayer Journal · 3AM / Crisis · Grief Companion.
- **Practice** — Fasting Companion · Daily reminder.
- **Create** — Verse cards.
- **Settings & About** — language, theme, accessibility, attribution.

---

## 6. Phased delivery

Each phase notes what is verifiable here (unit-tested / CI) vs. device-only.

### Phase A — IA restructure (foundation)
3-tab nav (Read · Calendar · More); merge Home + Listen + Search into Read; persistent
mini-player; regroup Library → More into sections. *Mostly Compose + nav — verifiable.*

### Phase B — Liturgical calendar + lectionary (highest value)
Lectionary data model + providers (CSI Almanac / RC / general / Orthodox, driven by
onboarding) → month-grid calendar UI → day-detail with readings → fold in fasting days,
reminders, and share-cards. *Lectionary/feast/computus logic is pure-domain and
unit-testable; the grid UI is Compose.*

### Phase C — "Explain this passage" (reframe Pastor Mode)
Reader verse-action → explanation; cloud-primary hybrid engine with Room caching; Nano
upgrade; "for preaching" depth. *Caching/mapping + UI testable; live AI calls need the
key + network (and Nano needs a device).*

### Phase D — Polish
Fill empty screens (Today hub density), fix onboarding (`rc_calendar` → real names),
tighten spacing, mini-player behaviour, accessibility pass on the new surfaces.

---

## 7. Notes / decisions

- The lectionary is the **linchpin**: the app already asks tradition + calendar at
  onboarding but doesn't use it. Implementing lectionary daily readings is the
  highest-leverage addition and unifies Calendar + Today + readings.
- Retire **Pastor Mode** (sermon-prep notebook) in favour of **Explain**; keep optional
  sermon notes as a secondary affordance inside Explain if pastors want it.
- Keep everything **offline-first**: cache lectionary/feast data and Explain responses;
  the network is for the first fetch and premium AI only.
