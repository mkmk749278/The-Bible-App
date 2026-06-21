# Manna — Deep UX/UI Audit

**Date:** 2026-06-21 · **Build:** v0.2.3 · **Scope:** all of `app/src/main/java/com/manna/bible/ui` + theme + manifest
**Method:** six parallel code-level audits (onboarding/IA, reading screen, audio/search/library, design-system/motion, accessibility, secondary screens & states), measured against the UX Master Design Directive, the non-negotiable accessibility requirements, and the roadmap.

---

## 0. Executive summary

Manna has an **excellent foundation**: the design system is close to flawless (role-based color, zero hardcoded hex, system-driven dark mode, calm 300ms screen fades, consistent 48dp targets), the reading typography is reverent and breathes, and the tone of the copy is warm and India-aware. The work to fix is **not** "make it pretty" — it's about **finishing the promises the app already makes**: audio-first, elder/illiterate accessibility, and a true "home".

Grades by area:

| Area | Grade | One-line |
|---|---|---|
| Design system (color/type/shape/dark) | **A** | Near-perfect; 2 borderline contrast pairs to nudge |
| Motion | **A−** | Screen fades compliant; in-screen micro-motion mostly absent |
| Reading screen | **B+** | Beautiful text; missing swipe, max-width, tap affordance |
| Accessibility (TalkBack) | **C** | ~10 interactive elements unlabelled — blocks the core audience |
| Audio-first experience | **C** | No scrubbing, no persistent player, no resume — undersells the #1 promise |
| Simplified / Elder Mode | **C** | Reader-only; not in prayers/search/catalog; not in onboarding |
| Onboarding | **B−** | Solid but misses mode, simplified opt-in, notif priming, download expectation |
| Home / IA | **B−** | "Home" cards not implemented; player not cross-tab |
| Search | **B** | Fast & calm but 2-tap, no history, no match highlight |
| Library / Downloads | **B** | Works; no size, no active-language label, no queued state |
| Prayers / Calendar / Settings / states | **B+** | Reverent & well-built; offline-in-prayers + simplified gaps |

**The single most important finding:** the app is built for the **illiterate, elderly, and low-vision** — yet the **Rosary bead counter and ~9 other core interactive elements are invisible to TalkBack**, and **Simplified/Elder Mode only exists in the reader**. Fixing accessibility is both the highest-mission-impact and the lowest-risk work available.

---

## 1. The punch list (top priorities)

### P0 — Critical (mission-blocking, do first)
1. **Add `semantics { contentDescription }` to ~10 unlabelled interactive elements** — TalkBack users can't operate ~30% of the interactive surface. The **Rosary bead** is the worst (core elderly interaction, announces only "button"). *(§3.1)*
2. **Make Simplified/Elder Mode real & app-wide** — today it only enlarges the reader. Prayers, search, catalog, calendar, settings ignore it. The audience this app exists for is left behind outside the reader. *(§3.2)*

### P1 — High (directive promises not yet met)
3. **Audio: progress/scrubbing + persistent cross-tab mini-player + "Continue Listening" resume + buffering state.** The audio-first promise (60% of users) is currently a play/pause button with no position. *(§4)*
4. **Reader: swipe between chapters + verse-tap affordance + tablet max-width.** *(§5)*
5. **Onboarding: capture mode (read/listen/both), offer Simplified Mode, prime POST_NOTIFICATIONS, set the "your Bible downloads now" expectation.** *(§6)*
6. **Nudge the two borderline contrast pairs** and the **muted-on-light** token (2.87:1) used near low-vision users. *(§3.3)*

### P2 — Medium (UX debt / friction)
7. Centralize touch-target + simplified-state (24 files redefine `MinTouchTarget`); standardize the back affordance. *(§8)*
8. Search: 1-tap-to-query, recent searches, highlight the matched term, topical fallback on no-results. *(§7)*
9. Library: show download size, label the active Bible language, distinguish bundled vs downloaded, show "queued" when offline. *(§7)*
10. Communicate offline-unavailable scripture inside prayer screens; specific (not generic) error copy + a retry on the Explain sheet. *(§9)*

### P3 — Polish
11. In-screen calm motion (selection/step/highlight transitions); bead anim 200→250ms. Calendar season legend. Widget card styling. Bundle Inter/Noto Serif. *(§10)*

---

## 2. What's strong (preserve — do not regress)

- **Color system:** role-based `MannaTheme.colors.*`, **zero hardcoded hex anywhere in `ui/`**, alpha derived from tokens. Dark mode is system-driven, no pure black (`#080C14`). *(Color.kt, Theme.kt)*
- **Contrast (core roles):** ink/soft/gold all pass 4.5:1 on bg in both palettes (ink 13:1, soft 5.12:1, gold 4.86:1 light).
- **Reading typography:** scripture serif applied; 18→24sp text with 28→36sp line height; gold verse numbers; generous spacing. Verses carry full TalkBack descriptions incl. highlight/bookmark/note state — *exemplary*.
- **Touch targets:** consistent 48dp / 56dp simplified across most screens.
- **Audio mini-player controls** (where present): labelled, haptics, play/pause/speed/continuous/stop.
- **Calendar:** month grid + "feasts this month" list + per-day card with readings & daily verse; day cells have rich semantics.
- **Tone:** "Daily bread from heaven", "Reflecting on the passage…", "Nothing leaves your device.", "The Rosary · Japamala", "Sramanikal · 40 Days" — warm, reverent, India-aware. No gamification, streaks, ads, or corporate voice anywhere.
- **Settings:** canon-switch warning is transparent and reassuring; "Re-run setup" preserves annotations.
- **Empty/error states** in reader, daily verse, prayer journal, catalog are human and reassuring.

---

## 3. Accessibility (the mission-critical area)

### 3.1 Missing content descriptions / TalkBack semantics — **P0**
Interactive elements whose `.clickable{}` carries **no** `semantics{}`, so TalkBack announces only child text (or just "button"):

| Element | File | Note |
|---|---|---|
| **Rosary Bead** | `ui/prayers/rosary/RosaryScreen.kt` (~440) | **CRITICAL** — announces "button"; no index/state (filled/next/empty). Core elderly interaction. |
| Rosary `PrayerItem` (accordion) | RosaryScreen.kt (~289) | No expanded/collapsed state or toggle role. |
| Rosary `ScriptureCard` | RosaryScreen.kt (~449) | No "opens in reader" intent. |
| Search `ResultRow` | `ui/search/SearchScreen.kt` (~178) | Snippet truncated at 2 lines → full verse not announced. |
| `SermonCard` | `ui/sermon/SermonHelperScreen.kt` (~145) | No "edit sermon" intent. |
| Stations `StationCard` | `ui/prayers/stations/StationsScreen.kt` | No intent. |
| Paraloka cards (verse + prayer) | `ui/prayers/paraloka/ParalokaScreen.kt` | No intent. |
| PrayersHub `CategoryCard` | `ui/prayers/PrayersHubScreen.kt` (~152) | Glyph decorative; card intent implicit. |
| CrisisMode `ComfortCard` | `ui/crisis/CrisisModeScreen.kt` (~166) | No "read verse" intent. |
| More `EntryCard` | `ui/more/MoreScreen.kt` (~204) | Label readable but no explicit intent. |
| Calendar event row | `ui/calendar/LiturgicalCalendarScreen.kt` (~230) | Feast/fast dot colour not described. |

**Fix:** add `.semantics { contentDescription = … }` (and for the accordion, `stateDescription`/role). The bead should announce e.g. *"Bead 3: tap to mark prayed"*. Low-risk, high-impact; do this first.

### 3.2 Simplified / Elder Mode is reader-only — **P0/P1**
`simplifiedMode` is read in the reader (enlarges text to 24sp, targets to 56dp, auto-reads on navigation, defaults TTS to 0.75×). But it is **not plumbed** to: prayers (Prev/Next stay Material default), search, catalog, calendar, settings, or the book/chapter picker (picker text hardcoded 16sp, fixed 420dp height). So an elder who enables it still hits small text and 48dp controls everywhere except the reading pane. Onboarding never offers it.
**Fix:** expose simplified-state app-wide (see §8 centralization), apply enlarged text + 56dp + (where relevant) auto-read across the prayer/search/catalog screens; add a Simplified opt-in to onboarding; scale picker text.

### 3.3 Contrast — **P1 (nudge), reconciled**
Authoritative values from reading `Color.kt`:

| Pair (light) | Ratio | Verdict |
|---|---|---|
| ink on bg / card | 13.1 / 10.1 | ✅ |
| soft on bg | 5.12 | ✅ |
| **soft on card** | **4.49** | ⚠️ 0.01 under — nudge |
| gold on bg | 4.86 | ✅ |
| **gold on card** | **4.26** | ⚠️ under 4.5 — OK only as bold/large (verse #s/refs); nudge for body |
| **muted on bg** | **2.87** | ⚠️ "exempt" per directive (tertiary) but **risky for low-vision** target users |

Dark palette: all core pairs pass comfortably (gold 7.28, soft 6.60, ink 15.3). **Fix:** darken light `gold`/`soft` ~one step or lighten `card` slightly so gold/soft clear 4.5:1 on card; reconsider `muted` lightness given the audience. (Earlier onboarding-agent claim of "3.2:1 gold-on-card" used a wrong card hex — disregard.)

### 3.4 Touch targets & RTL — **P2**
- Targets generally meet 48/56dp, **but** Calendar month-nav `IconButton`s are unconstrained (may be <48dp); `MinTouchTarget` is redefined in ~24 files (no single source). *(§8)*
- RTL: `supportsRtl=true`, no `left/right` hardcoding, reader flips `LayoutDirection`, Calendar uses `AutoMirrored` arrows ✅. **But** the custom back glyph `"‹"` (a text char) does **not** mirror for Urdu/Arabic — should be `"›"` in RTL. *(also §8)*

### 3.5 200% font safety — **P2**
sp is used everywhere (good), but fixed-height containers (book/chapter picker `height(420.dp)`; some cards) risk clipping at 200%. Needs a pass at max font scale.

---

## 4. Audio-first experience — **P1** (the headline product promise)

| # | Gap | File |
|---|---|---|
| 4.1 | **No progress bar / position / scrubbing** in the mini-player — user can't see how far through a chapter or seek. | `ui/reader/ReaderScreen.kt` AudioMiniPlayer (~451–548) |
| 4.2 | **Mini-player is Read-tab only** — leaving for Calendar/Prayers abandons the listening context. Roadmap wants a persistent player. | `ui/MannaApp.kt` |
| 4.3 | **No "Continue Listening" resume** affordance at launch/home (state is saved but you must navigate + tap play). | ReaderViewModel + MannaApp |
| 4.4 | **No buffering/preparing state** for narrated audio — `TtsStatus` has only IDLE/PLAYING/PAUSED; slow streams give no feedback. | `domain/audio/TtsModels.kt` |
| 4.5 | **Narrated audio doesn't highlight the current verse** (`currentVerse` stays null) — no follow-along for narrated mode. | `domain/audio/NarratedAudioPlayer.kt`, ReaderViewModel (~252) |
| 4.6 | **No narrated-vs-TTS indication** — user can't tell human narration from synthetic. | ReaderScreen (~502) |
| 4.7 | Auto-advance/auto-play only in Simplified Mode; non-simplified audio users re-tap play each chapter. | ReaderViewModel (~781) |
| 4.8 | Aggressive auto-scroll: `animateScrollToItem` fires even when the verse is already visible. | ReaderScreen (~613–629) |

*(Note: background playback, audio focus, and notification Play/Pause/Stop shipped in v0.2.3 — lock-screen scrubbing / MediaSession metadata remains the open piece.)*

---

## 5. Reading screen — **P1/P2**

- **No swipe between chapters** — Android convention absent; every chapter change needs the nav bar. **P1.** *(ReaderScreen ~643)*
- **No verse-tap affordance** — verses are tappable but give no visual cue/ripple/long-press; new users won't discover annotation. **P1.** *(~696)*
- **No max-width on large screens** — verse lines exceed ~75 chars on tablets. Wrap list in `widthIn(max≈900.dp)`. **P1.** *(~643)*
- **Book/chapter picker:** fixed `height(420.dp)` (clips/oversizes across devices) and **picker text hardcoded 16sp** (ignores Simplified Mode). **P2.** *(~864, 869, 888, 904)*
- **No "Jump to verse"** entry for power users. **P2.**
- Verse highlight is a hard **2500ms delay**, not a fade. **P3.** *(ReaderViewModel ~141)*

---

## 6. Onboarding & Home / IA — **P1/P2**

**Onboarding** (`ui/setup/SetupHost.kt`, 7 steps: Welcome→Denomination→UI lang→Bible lang→Translation→Lectionary→Summary):
- **Missing "mode" (read/listen/both)** — `preferredMode` pref is never captured; audio-first users get a read-first app. **P1.**
- **No Simplified-Mode opt-in** — elders must later find it buried in Settings. **P1.**
- **No POST_NOTIFICATIONS priming** — users enable the daily reminder, then nothing fires. **P1.**
- **No download expectation/initiation** — setup ends with no scripture on device and no "your Bible is downloading" message; offline-first promise feels broken on first read. **P1.**
- Dual-language independence isn't taught (UI lang ≠ Bible lang). **P2.**
- "Skip for now" actually *completes* setup with defaults (not resumable) and silently clears denomination → SHOW_EVERYTHING canon. **P2.**
- Setup step changes are instant (no calm transition); Simplified flag not passed to all steps. **P3.**

**Home / IA:**
- The directive's **Home (Continue Reading · Today's Verse · Continue Listening)** is **not** a destination — the Read tab opens straight into the reader; Daily Verse is a separate pushed screen. Either build a light home, or surface these as cards atop the reader. **P2 (decision).**
- Nav is **4 tabs** (Read · Calendar · Prayers · More) vs the roadmap's 3 (Prayers feature-flagged into a tab). Confirm intent. **P2 (decision).**
- Search reachable only after entering the reader (then 1 tap). Acceptable, but not a primary destination. **P2.**

---

## 7. Search & Library — **P2**

**Search** (`ui/search/SearchScreen.kt`): 2 taps before typing (no 1-tap-to-query); **no recent searches/history**; **matched term not highlighted** in snippets; **no-results** doesn't offer the topical browse that already exists; no "offline/local" reassurance chip.

**Library / Catalog** (`ui/catalog/…`): **download size not shown** (size exists in the entity); **active Bible language not labelled** (user can't tell why English translations are hidden when Bible lang = Malayalam); **bundled vs downloaded** look identical (deleting a bundled one surprises); **no "queued" state** when you tap download offline; **offline banner only on catalog** (not reader/search); download progress lacks ETA/speed.

---

## 8. Cross-cutting consistency — **P2**

- **`MinTouchTarget = 48.dp` is redefined in ~24 files**, and Simplified state is threaded ad-hoc. Create `ui/theme/Dimens.kt` (`touchTarget(simplified)`, spacing scale) and an app-wide simplified-state (DataStore-backed `CompositionLocal` or a small shared VM).
- **Back affordance inconsistent:** custom `"‹"` glyph (most screens) vs text "Back" (setup) vs implicit; not RTL-aware. Make one `MannaBackButton` (real `Icons.AutoMirrored…Back`, content description, mirrors for RTL).
- **Padding varies** 16/18/20/24dp across screens — pick a scale.
- `SetupHost` uses `MaterialTheme.colorScheme.error` rather than `MannaTheme.colors.red` (one off-palette spot).

---

## 9. Prayers / Calendar / Settings / Explain / states — **P2/P3**

- **Prayers don't communicate offline-unavailable scripture** — if a verse isn't downloaded the card silently omits it; add a quiet "Download a translation to see this verse." **P2.**
- **Prayers ignore Simplified Mode** (see §3.2). **P1.**
- **Explain sheet:** no **Retry** in the Unavailable state (must dismiss & re-tap). Loading/offline/key states are otherwise excellent. **P2.**
- **Generic error copy:** the reader's download `else` branch and the catalog "Something went wrong" don't distinguish transient (retry) vs permanent (404/removed). **P2.**
- **Calendar:** season colours have **no legend/key**; "feasts this month" vs fasts not always labelled. **P3.**
- **Settings:** flat section hierarchy (all `titleMedium`); "Deuterocanonical" is jargon → "Additional scriptures". **P3.**
- **Widget:** functional & offline but visually plain (no card, weak hierarchy). **P3.**
- **Prayer text mappings** (`mysteryTitleRes`, `StationTitles`, …) are duplicated across files — consider a shared provider. **P3 (maintainability).**

---

## 10. Design system & motion — **P3 (mostly strong)**

- **Color/type/shape/dark = A** (see §2). Fonts are **system serif/sans** — Inter & Noto Serif aren't bundled (acceptable & offline-safe; a brand-polish opportunity). **JetBrains Mono / Strong's numbers** not implemented yet.
- **Motion:** screen-to-screen fades are a compliant 300ms; the breathing guide (4s) is intentional. **But in-screen micro-motion is largely absent** — selections, setup steps, and the verse highlight don't use the directive's calm 250–400ms transitions. Bead tap anim is 200ms (just under 250). **P3.**
- Corner radii cluster on 14dp (good); spacing roughly on an 8dp grid. Fine.

---

## 11. Gap analysis vs Directive & Roadmap

| Directive / roadmap intent | Status |
|---|---|
| Light-first warm palette, gold accent | ✅ Fully |
| Role-based tokens, no hardcoded hex | ✅ Fully |
| Generous whitespace, serif scripture | ✅ Fully |
| Calm motion 250–400ms | ⚠️ Screen fades ✅; in-screen ❌ |
| 48dp / 56dp targets | ⚠️ Reader ✅; not app-wide for simplified |
| Text scales with system font (sp) | ✅ (verify at 200%) |
| Contrast 4.5:1 (gold/soft/ink) | ⚠️ ✅ on bg; gold/soft borderline on card |
| TalkBack on all screens | ❌ ~10 elements unlabelled |
| RTL (Urdu/Arabic) | ⚠️ Layout ✅; back glyph not mirrored |
| Simplified/Elder Mode (4 buttons, 150%, auto-play, slow TTS) | ⚠️ Reader-only; not in onboarding/other screens |
| Audio-first, integrated minimal player | ⚠️ No scrubbing/resume/persistence |
| Home (Continue Reading · Today's Verse · Continue Listening) | ❌ Not a destination |
| Search / Library as primary destinations | ⚠️ Folded into reader/More (roadmap-intended) |
| The interface "disappears" in the reader | ⚠️ Good, but audio+search bars always on; no swipe |
| No feeds/ads/gamification, reverent tone | ✅ Fully |

---

## 12. Recommended remediation sequence

**Sprint 1 — Accessibility & the audience (P0):** add the ~10 `semantics`/contentDescription fixes (start with the bead); plumb Simplified Mode app-wide (shared dimens + simplified `CompositionLocal`), apply to prayers/search/catalog/calendar; scale the picker. *Low risk, highest mission impact, all CI-testable.*

**Sprint 2 — Finish audio-first (P1):** progress/scrub + position in the mini-player; persistent cross-tab player; "Continue Listening"; buffering state; narrated verse-highlight; engine label.

**Sprint 3 — Reader & onboarding (P1):** swipe between chapters; verse-tap affordance; tablet max-width; onboarding mode + simplified opt-in + notif priming + download expectation; nudge the borderline contrast tokens.

**Sprint 4 — Friction & polish (P2/P3):** centralize touch-target/back-button; search history + 1-tap + match highlight; catalog size/active-language/queued; offline-in-prayers; specific errors + Explain retry; calm in-screen motion; calendar legend; widget styling.

---

## 13. Open decisions for you

1. **Home:** build a light Home destination with the three cards, or surface them atop the Read tab? (Directive wants a Home; the roadmap leaned toward folding it into Read.)
2. **Nav tabs:** keep 4 (Read · Calendar · Prayers · More) or fold Prayers into More for the calmer 3-tab roadmap shape?
3. **Simplified Mode ambition:** enlarge-everything (fast) vs the full "4 giant buttons" oral-Bible interface (Phase-3 scope)?
4. **Fonts:** bundle Inter + Noto Serif now (brand fidelity, ~APK cost) or stay on system serif/sans?
