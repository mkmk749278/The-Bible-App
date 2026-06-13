# Manna — Reader & Calendar UX Redesign Roadmap

> **Status**: Proposed (June 2026). Addresses UX pain points from live app + adds Paraloka spiritual reference hub.
> This document outlines 4 phases of incremental improvements to the Reader, Audio Player, Calendar, and Prayer/Spirituality features.

---

## 1. Current Pain Points (from screenshot review)

| # | Issue | Impact | Severity |
|---|-------|--------|----------|
| 1 | **Audio player cramped** — play, stop, speed, continuous toggle + chapter nav (prev/next) all squeezed into bottom bar | User confusion; hard to tap targets | 🔴 High |
| 2 | **Chapter navigation weak** — small prev/next buttons at the end of a long bar | Requires scrolling or extra taps to find | 🟡 Medium |
| 3 | **Search bar not prominent** — buried in top actions; users don't realize they can search | Low discoverability | 🟡 Medium |
| 4 | **No verse linking from reference materials** — prayers exist but don't jump to verses; no highlighting context | Broken workflows (read prayer → can't easily jump to verse) | 🔴 High |
| 5 | **Calendar shows one event at a time** — day detail is modal; can't scan all month's feasts/fasts at once | Poor event visibility; users miss liturgical context | 🟡 Medium |
| 6 | **No Paraloka/prayer resource** — gap in Indian Christian theology (afterlife, funeral rites, comfort) | Misses users in grief, facing mortality | 🔴 High |
| 7 | **Mini-player not persistent** — audio stops when navigating tabs | Poor listening UX for long readings | 🟡 Medium |

---

## 2. Target Architecture

### Audio Player Redesign
```
┌─ Reader Screen ──────────────────┐
│ [Book Name] [Ch 2]     [🔍] [...] │  ← Top bar (unchanged for now)
├──────────────────────────────────┤
│ Verse 1: "In the beginning..."   │
│ Verse 2: "And the earth was..."  │  ← Main content (unchanged)
│ Verse 3: "And God saw that it... │
├──────────────────────────────────┤
│ ▶ 1x ◼ Continuous    Genesis 1:1 │  ← Compact audio player (NEW)
│ < 1   2 >                        │  ← Chapter navigation (SEPARATED)
└──────────────────────────────────┘
```

**Before**: Audio bar + chapter nav in one row = cramped
**After**: Audio controls in one row, chapter nav below = clearer

### Reader with Search Prominence
```
┌ Top Bar ─────────────────────────┐
│ [Book Ch]  [Search] [Menu]       │  ← Search icon (already here)
├ Search Results (optional) ────────┤
│ "Search the Bible" — [_______]    │  ← Enhanced prominence (NEW)
├ Verses ──────────────────────────┤
│ 1. In the beginning...            │
│ 2. And the earth was...           │
└──────────────────────────────────┘
```

### Calendar with All Events Visible
```
┌─ Calendar Screen ────────────────┐
│ June 2026      [Today]           │
├─────┬─────┬─────┬─────┬─────┐────┤
│ Sun │ Mon │ Tue │ Wed │ Thu │... │
├─────┼─────┼─────┼─────┼─────┤────┤
│  2  │  3  │  4  │  5  │  6  │... │  ← Day cells with color + icons
│ 🟢  │     │     │🟡   │🔴   │... │
├─────┴─────┴─────┴─────┴─────┴────┤
│ EVENTS THIS MONTH:                │  ← (NEW) Scrollable list
│ 🟢 Pentecost (Jun 5) — Readings  │
│ 🔴 Feast of Sacred Heart (Jun 12)│
│ 🟡 Ember Wednesday (Jun 19)       │
│ ⚪ Nativity of John Baptist(Jun 24)│
├──────────────────────────────────┤
│ Tap a day for detail              │
└──────────────────────────────────┘
```

### Prayer/Spirituality Hub
```
Library / More
  ├─ Prayer & Spirituality (NEW)
  │   ├─ Paraloka & Afterlife (Topical Hub)
  │   │   ├─ "What is Paraloka?" explainer
  │   │   ├─ 15 curated verses (John 14:2-3, 2 Cor 5:1-8, etc.)
  │   │   ├─ 5 CSI/Catholic/Orthodox prayers
  │   │   ├─ Multi-language glossary (Ta/Te/Hi/Ma/En)
  │   │   └─ [Read verses] [Read prayers] [Filters]
  │   │
  │   ├─ Funeral & Memorial Prayers
  │   │   ├─ CSI funeral service (with verses)
  │   │   ├─ Catholic requiem
  │   │   ├─ Orthodox commemoration
  │   │   └─ 40-day family prayer (Sramanikal)
  │   │
  │   └─ Prayer Search
  │       └─ [Search "paraloka" / "heaven" / "grief"]
  │          Results: verses + prayers + commentary
```

---

## 3. Phased Implementation

### Phase R1: Audio Player Reorganization (1 week)
**Goal**: Decouple audio controls from chapter navigation for clarity.

**Changes**:
1. **ReaderBottomBar.kt**: Split into two sections
   - `AudioControlsRow` (play, stop, speed, continuous) — full width, cleaner spacing
   - `ChapterNavigationRow` (prev/next) — below, centered, larger buttons
2. **Layout**: Add 8dp vertical gap between sections
3. **Button sizing**: Increase chapter nav buttons to 56dp (vs 48dp now)
4. **Styling**: Subtle elevation/border between sections for visual separation

**Files to modify**:
- `app/src/main/java/com/manna/bible/ui/reader/ReaderScreen.kt` (ReaderBottomBar, AudioBar)

**Testing**:
- [ ] Audio controls visible and tappable on all screen sizes
- [ ] Chapter navigation below audio (no horizontal wrapping)
- [ ] Touch targets meet 56dp minimum in audio row

**Verification**: Device test (Pixel 6, Galaxy A12) — audio/chapter nav feel separated and clear.

---

### Phase R2: Reader Search Prominence (1 week)
**Goal**: Make "Search the Bible" more discoverable in the reader.

**Changes**:
1. **ReaderTopBar.kt**: Add a search hint/card above verses (only when verses are visible, not in loading state)
2. **ReaderContent.kt**: Add `ReaderSearchCard()` composable
   - Shows: "Search the Bible" text + magnifying glass + hint
   - Tappable → opens search screen
   - Appears between top bar and first verse
3. **Verse numbering adjustment**: Add SEARCH_CARD_ITEMS = 1 offset (similar to SEARCH_BAR_ITEMS)

**Files to modify**:
- `app/src/main/java/com/manna/bible/ui/reader/ReaderScreen.kt`
- `app/src/main/res/values/strings.xml` (add search hints)

**Testing**:
- [ ] Search card appears only when verses loaded
- [ ] Card dismissible or always visible (decide UX)
- [ ] Tapping card opens search screen
- [ ] Scroll offsets are correct

**Verification**: Compose preview + device test.

---

### Phase C1: Calendar Event List (1.5 weeks)
**Goal**: Show all month's feasts/fasts at a glance.

**Changes**:
1. **LiturgicalCalendarScreen.kt**: Add an "Events this month" section below the month grid
   - Scrollable list of all feasts/fasts for the month
   - Color-coded by LiturgicalColor (white, green, violet, red)
   - Tap an event → highlights that day in grid + shows detail card
2. **LiturgicalCalendarViewModel.kt**: Add `allMonthEvents` StateFlow
   - Compute all feast/fast days in the displayed month
   - Sort by date
3. **Styling**: 
   - Event cards: 56dp tall, icon + name + date
   - Color bar on left (matching grid cell color)
   - Selectable/clickable

**Files to modify**:
- `app/src/main/java/com/manna/bible/ui/calendar/LiturgicalCalendarScreen.kt`
- `app/src/main/java/com/manna/bible/ui/calendar/LiturgicalCalendarViewModel.kt`

**Testing**:
- [ ] Event list shows all feasts/fasts for displayed month
- [ ] Events are sorted by date
- [ ] Tapping event highlights day + shows detail
- [ ] Colors match season colors

**Verification**: Device test with June 2026 (Pentecost, Ember days, etc.).

---

### Phase P1: Paraloka Topical Hub (2 weeks)
**Goal**: Add Paraloka (afterlife/heaven concept) as a searchable prayer/study resource with verse linking.

**Data Model**:
```kotlin
// New Room entities
@Entity("spiritual_topics")
data class SpiritualTopic(
    @PrimaryKey val id: String,                    // "paraloka"
    val titleEn: String,                           // "Paraloka & Heaven"
    val titleTa: String,                           // "பரலோகம்"
    val titleTe: String,                           // "పరలోకం"
    val titleHi: String,                           // "परलोक"
    val descriptionEn: String,
    val descriptionMl: String? = null,
    val category: String,                          // PRAYER, STUDY, COMFORT
    val linkedVerseIds: List<String>,              // JSON list of OSIS refs
    val linkedPrayerIds: List<String>,             // FK to prayers table
    val isFeatured: Boolean = true
)

@Entity("prayers")
data class Prayer(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleTa: String,
    val titleTe: String,
    val titleHi: String,
    val textEn: String,
    val textTa: String,
    val textTe: String? = null,
    val textHi: String? = null,
    val source: String,                            // "CSI Funeral Liturgy", "Catholic Requiem", etc.
    val tradition: String,                         // "CSI", "CATHOLIC", "ORTHODOX"
    val linkedVerseIds: List<String>,              // JSON list of OSIS refs
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "PRAYER"
)

@Entity("prayer_verses")
data class PrayerVerseCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayerId: String,
    val osisRef: String,
    val context: String?                           // "related" or "quoted"
)
```

**UI Components**:
1. **SpiritualTopicScreen.kt** — Detail screen for Paraloka
   - Header: "Paraloka & Heaven" + icon
   - Tabs or sections:
     - "What is it?" — multi-language explainer
     - "Scripture" — 15 verses, each tappable → opens reader
     - "Prayers" — 5+ CSI/Catholic/Orthodox prayers
     - "Glossary" — Ta/Te/Hi/Ma/En terms for paraloka/swargam/heaven
   - Search within topic: "Find prayer about..." or "Find verses about..."

2. **PrayerCard.kt** — Reusable prayer display
   - Title (multi-lang), text (Tamil/English toggle)
   - Tradition badge (CSI, Catholic, Orthodox)
   - "Linked verses" section with verse taps
   - Bookmark button (optional)

3. **More Hub Integration** — Add "Prayer & Spirituality" section
   - Card: "Paraloka & Afterlife" with image/icon
   - Subtitle: "CSI prayers, Catholic requiem, Orthodox rites"
   - Tap → SpiritualTopicScreen

**Data seeding** (hardcoded in repository, or from JSON asset):
- 1 topic (Paraloka)
- 15 verses (John 14:2-3, 2 Cor 5:1-8, Rev 21:1-4, Luke 23:43, Phil 3:20, 1 Thess 4:16-17, 1 Cor 15:42-58, John 3:16, John 10:28, Heb 11:10, 1 Pet 1:3-4, Rom 6:9-10, 1 Tim 4:7-8, Titus 1:1-2, 2 Tim 4:7-8)
- 5 prayers (CSI funeral, Catholic requiem, Orthodox commemoration, Sramanikal, intercession)
- All in Tamil, Telugu, Hindi, English

**Verse Linking Mechanism**:
- Prayer has `linkedVerseIds: List<String>` (e.g., ["JHN.14.2", "2CO.5.1"])
- UI renders prayer text with **clickable verse refs** (blue/gold underline)
- Tapping verse → reader opens at that ref with highlight
- Reader receives `pendingScrollRef` + `pendingHighlightVerse` callbacks

**Files to create**:
- `app/src/main/java/com/manna/bible/domain/prayer/Prayer.kt` (domain model)
- `app/src/main/java/com/manna/bible/domain/prayer/SpiritualTopic.kt`
- `app/src/main/java/com/manna/bible/data/local/prayer/PrayerEntity.kt` (Room)
- `app/src/main/java/com/manna/bible/data/local/prayer/PrayerDao.kt`
- `app/src/main/java/com/manna/bible/data/local/prayer/PrayerRepository.kt`
- `app/src/main/java/com/manna/bible/ui/prayer/SpiritualTopicScreen.kt`
- `app/src/main/java/com/manna/bible/ui/prayer/PrayerCard.kt`
- `app/src/main/java/com/manna/bible/ui/prayer/SpiritualTopicViewModel.kt`
- `app/src/main/java/com/manna/bible/ui/more/PrayerAndSpiritualitySection.kt`

**Testing**:
- [ ] Prayer data loads from Room
- [ ] Verse links in prayer text are clickable
- [ ] Tapping verse link opens reader at correct ref
- [ ] Multi-language prayers display correctly
- [ ] Tradition badges show correctly
- [ ] Search within topic works

**Verification**: 
- Device test: Tamil UI + Tamil prayer text displays correctly
- Tap "JHN.14.2" in prayer → reader opens Genesis 1, Reader, or John 14?
- Highlight verse when opened from prayer

---

### Phase P2: Funeral & Memorial Prayer Expansion (1 week)
**Goal**: Add full suite of funeral/memorial prayers for CSI, Catholic, Orthodox.

**Changes**:
1. Add 8-10 more prayer entries (CSI funeral full text, Catholic requiem variations, Orthodox commemoration rite)
2. New "Funeral & Memorial" topical hub
3. Context: "For those facing loss" or "Comfort in grief"
4. Link to Prayer Journal and Faith Timeline (if available)

**Files**:
- Expand `PrayerRepository` with seeded funeral prayer data
- Add "Funeral & Memorial" topical screen
- Update MoreScreen to show "Funeral Prayers" as separate card (optional)

**Testing**:
- [ ] All 8-10 prayers load
- [ ] Prayers display correctly in each language
- [ ] Verses link correctly

---

### Phase A1: Persistent Mini-Player (2 weeks) — *Deferred*
**Goal**: Keep audio playing when navigating between tabs.

**Notes**: 
- Requires architectural change (move audio state to app-level ViewModel)
- Mini-player UI (Spotify-style floating bar at bottom)
- May conflict with bottom tab navigation
- *Proposed for Phase 2 after R1-C1-P1 stabilize*

---

## 4. Data Requirements

### Paraloka Content (Hardcoded Seed Data)

**15 Key Verses**:
```
JHN.14.2-3       — "Many mansions prepared"
2CO.5.1-8        — "Eternal house in heaven"
REV.21.1-4       — "New heavens and new earth"
PHP.3.20         — "Citizenship in heaven"
LUK.23.43        — "Today you will be with me in paradise"
1TH.4.16-17      — "Caught up to meet the Lord"
1CO.15.42-58     — "Spiritual body, imperishable"
JHN.3.16         — Eternal life
JHN.10.28        — "No one snatches them from my hand"
HEB.11.10        — "City whose architect is God"
1PE.1.3-4        — "Incorruptible inheritance"
ROM.6.9-10       — "Death defeated"
1TI.4.7-8        — "Godliness and eternal life"
TIT.1.1-2        — "Promise of eternal life"
2TI.4.7-8        — "Crown of righteousness"
```

**5 Core Prayers** (with multi-language variants):
1. **CSI Funeral Service Prayer**
   - Source: Church of South India Liturgy
   - Languages: Tamil, Telugu, English
   - Linked verses: JHN.14.2, REV.21.1, 1TH.4.16

2. **Catholic Requiem (Rest Eternal)**
   - Source: Roman Missal
   - Languages: Tamil, English
   - Linked verses: REV.21.4, LUK.23.43

3. **Orthodox Commemoration Prayer**
   - Source: Malankara Orthodox Liturgy
   - Languages: Malayalam (primary), English
   - Linked verses: JHN.14.2, 1CO.15.42

4. **CSI 40-Day Prayer (Sramanikal)**
   - Source: CSI Family Tradition
   - Languages: Tamil, Telugu, English
   - Linked verses: 1TH.4.16, HEB.11.10

5. **Daily Intercession for the Departed**
   - Source: Indian Christian Devotional
   - Languages: Tamil, Hindi, English
   - Linked verses: ROM.6.9, 1PE.1.3

---

## 5. Success Metrics

| Phase | Metric | Target |
|-------|--------|--------|
| R1 | Audio player usability (via Compose preview) | 0 errors; clean layout |
| R2 | Search discoverability (analytics after release) | +20% search usage in reader tab |
| C1 | Calendar event visibility | All month events visible without modal |
| P1 | Paraloka reach (uptake) | 500+ views in first month; 10% bookmark rate |
| Overall | User satisfaction | Reddit/Play Store: "UI much clearer" feedback |

---

## 6. Implementation Order

**Recommended sequence** (dependencies + risk):

1. **R1** (1 week) — Audio reorganization (low risk; visual change only)
2. **R2** (1 week) — Search prominence (low risk; additive UI)
3. **C1** (1.5 weeks) — Calendar event list (medium risk; new ViewModel logic)
4. **P1** (2 weeks) — Paraloka hub (medium risk; new entities, verse linking)
5. **P2** (1 week) — Funeral prayers expansion (low risk; data addition)
6. **A1** (deferred to Phase 2) — Persistent mini-player (high risk; architectural)

**Total: 6.5 weeks** (parallelizable: R1 + C1 could run simultaneously)

---

## 7. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Prayer/verse data seeding incomplete | Partial Paraloka launch | Audit 15 verses + 5 prayers before PR |
| Verse linking broken (reader doesn't receive callback) | Users can't jump to verses from prayers | Wire `pendingScrollRef` + `pendingHighlightVerse` in MannaApp |
| Multi-language rendering issues (Tamil/Telugu fonts) | Text displays incorrectly | Test on device with system font sizes 150%+ |
| Calendar event list too long (30+ events in a month) | Scrolling becomes tedious | Limit to "principal" feasts (8-12 per month); rest in detail modal |
| Audio player reorganization affects touch targets | Accessibility regression | Verify all buttons meet 48dp+ minimum |

---

## 8. Feature Flags

Add to `domain/FeatureFlags.kt`:

```kotlin
object FeatureFlags {
    // ... existing flags ...

    /** Reader bottom bar redesign (R1) */
    const val READER_AUDIO_REORGANIZATION: Boolean = true

    /** Calendar event list sidebar (C1) */
    const val CALENDAR_EVENT_LIST: Boolean = true

    /** Paraloka & Prayer resource hub (P1) */
    const val PRAYER_HUB: Boolean = true
    const val PARALOKA_TOPIC: Boolean = true

    /** Funeral & Memorial prayers (P2) */
    const val FUNERAL_PRAYERS: Boolean = true

    /** Persistent mini-player across tabs (A1) — deferred */
    const val PERSISTENT_MINI_PLAYER: Boolean = false
}
```

---

## 9. Next Steps

1. **Review this roadmap** with team/users — confirm priorities and scope
2. **Start R1** (audio reorganization) — quick win, improves user clarity immediately
3. **In parallel, design P1 data model** — finalize prayer data seeding
4. **QA checklist** — create device testing matrix (Tamil, Telugu, Hindi, Malayalam + English on Pixel 6, Galaxy A12, A10)
5. **Translation** — coordinate with CSI/Catholic liaisons for accurate prayer texts

---

*Last updated: 2026-06-13*
*Maintained by the Manna development team*
