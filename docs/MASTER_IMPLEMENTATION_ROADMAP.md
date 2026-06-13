# MASTER IMPLEMENTATION ROADMAP: Manna Prayer Ecosystem + UI/UX Redesign

> **Status**: Comprehensive Plan Complete, Ready for Development  
> **Scope**: 4 major prayer features + complete UI overhaul + multi-language support  
> **Timeline**: 15 weeks (3.5 months)  
> **Last Updated**: June 13, 2026

---

## EXECUTIVE OVERVIEW

This document consolidates the complete implementation plan for Manna v1.0.0, bringing together:

✅ **Reader Screen Redesign** (search prominence, audio reorganization, chapter nav)  
✅ **Calendar Improvements** (event visibility, feast/fast tracking)  
✅ **4-Category Prayer Ecosystem**:
   - 14 Stations of the Cross (passion meditation)
   - The Rosary/Japamala (5 mystery sets, interactive beads)
   - Jesus Prayer (3 Orthodox levels, breathing guide)
   - Paraloka Teachings (afterlife verses + 40-day Sramanikal tracker)

✅ **Beautiful, Meditation-Focused UI** (calming colors, large readable text, tactile interactions)  
✅ **Multi-Language Support** (Tamil, Telugu, Hindi, Malayalam, English)  
✅ **Full Accessibility** (WCAG 2.1 AA, TalkBack, high contrast)  
✅ **100% Offline** (all prayers, no internet required)

---

## PHASES AT A GLANCE

| Phase | What | Duration | Completion Target |
|-------|------|----------|-------------------|
| **0** | Discovery, design system, DB schema | 1 week | Week 1 |
| **1** | Database foundation, entities, DAOs | 2 weeks | Week 3 |
| **2** | Theme extensions, UI components | 1.5 weeks | Week 4.5 |
| **3** | Stations of the Cross (all 14) | 2 weeks | Week 6.5 |
| **4** | Rosary with bead counter | 2.5 weeks | Week 9 |
| **5** | Jesus Prayer (3 levels) | 1.5 weeks | Week 10.5 |
| **6** | Paraloka + Sramanikal tracker | 2 weeks | Week 12.5 |
| **7** | Integration & search | 1.5 weeks | Week 14 |
| **8** | Multi-language (Te/Hi/Ma) | 1.5 weeks | Week 15.5 |
| **9** | Polish & accessibility | 1.5 weeks | Week 17 |
| **10** | Documentation | 1 week | Week 18 |
| **11** | Comprehensive testing | 1 week | Week 19 |
| **12** | Final polish & release | 1 week | **Week 20 / 15 weeks** |

---

## KEY DELIVERABLES (by phase)

### Week 1-3: Foundation Ready ✓
- [ ] Room database v6 with 5 new entities
- [ ] All DAOs and repositories
- [ ] Prayer content seed data (English + Tamil)
- [ ] Hilt injection wiring

### Week 4-6: Stations Complete ✓
- [ ] All 14 Stations with full prayers
- [ ] Station detail screen with meditation
- [ ] Progress tracking (user can mark as complete)
- [ ] Verse linking to reader with highlighting
- [ ] More hub integration
- [ ] Device tested (Tamil fonts, dark mode)

### Week 7-10: Rosary + Jesus Prayer Complete ✓
- [ ] 5 mystery sets with 15 individual mysteries
- [ ] Interactive bead counter (53-bead japamala)
- [ ] Jesus Prayer 3-level guide with breathing animation
- [ ] Session tracking (rosary history)
- [ ] All integrated into Prayer hub

### Week 11-12: Full Ecosystem Complete ✓
- [ ] 15 Paraloka verses + 5 funeral prayers
- [ ] 40-day Sramanikal tracker (calendar)
- [ ] Prayer search across all categories
- [ ] Verse-to-prayer suggestions (explain screen)
- [ ] Mini-player persistent during prayer viewing
- [ ] Calendar integration (prayer suggestions on feasts)

### Week 13-15: Polish + Release Ready ✓
- [ ] Telugu, Hindi, Malayalam seed data
- [ ] WCAG 2.1 AA accessibility verified
- [ ] Dark mode perfected
- [ ] All tests passing (unit + UI + integration)
- [ ] APK size optimized
- [ ] Documentation complete
- [ ] Play Store release notes + screenshots

---

## CRITICAL SUCCESS FACTORS

### 1. Content Sourcing (Phase 0 - MUST START IMMEDIATELY)
- Contact CSI Central Office (Chennai) for official prayers
- Reach out to Catholic Diocese for Requiem Mass texts
- Malankara Orthodox Syrian Church for liturgical materials
- Tamil, Telugu, Hindi, Malayalam speakers for translations
- **Do NOT invent prayers; source from official liturgies only**

### 2. Multi-Language Quality (Phase 8 - ONGOING)
- All 5 languages (Tamil, Telugu, Hindi, Malayalam, English)
- Native speaker validation for each language
- Church liaison review before seeding
- Proper font rendering (Noto Serif fonts for each script)

### 3. Beautiful UI/UX (All phases)
- Every prayer screen is meditation-focused (large text, whitespace)
- Interactive elements feel satisfying (beads tap with subtle feedback)
- Dark mode optimized for evening prayer
- No animations that distract (calm, smooth transitions only)
- Accessibility-first (not an afterthought)

### 4. Verse Linking (Phase 7 - CRITICAL)
- Every prayer verse reference must link to reader
- Reader must highlight the linked verse
- Must work offline (all verses pre-downloaded)
- Must work across all 4 prayer categories

### 5. Performance (Phase 11 - MANDATORY)
- Prayer list load < 100ms
- Bead counter animation 60 FPS (smooth, no jank)
- Breathing guide animation 60 FPS
- Database queries < 50ms
- APK size increase < 5MB

---

## DATABASE SCHEMA (Phase 1)

**5 New Entities**:

1. **prayer_resources** — Unified table for all prayers
   - id (PK): "station_01", "mystery_joyful_1", "jesus_prayer_vocal", etc.
   - category: "stations", "rosary", "jesus_prayer", "paraloka"
   - titleEn, titleTa, titleTe, titleHi, titleMl
   - textEn, textTa, textTe, textHi, textMl
   - linkedVerseIds: JSON array ["MATT.27.1", ...]
   - meditationPoints: JSON array for reflection prompts
   - sequenceNumber: 1-14 (stations), 1-5 (mysteries), 1-3 (Jesus levels)
   - parentResourceId: for grouping (e.g., "rosary" parent)

2. **prayer_verse_links** — Fast verse lookups
   - prayerId, verseOsisRef, sequenceInPrayer

3. **prayer_categories** — 4 categories
   - id: "stations", "rosary", "jesus_prayer", "paraloka"
   - nameEn/Ta, descriptionEn/Ta, iconEmoji, colorToken, sequenceNumber

4. **station_progress** — Track user progress
   - stationId (PK), isCompleted, completedAt, reflectionNotes

5. **sramanikal_entries** — 40-day tracker
   - id (PK), personName, dateOfDeath, dayNumber (1-40), prayerOfferings

---

## KEY UI COMPONENTS (Phase 2)

**Must Build**:
- `PrayerCard.kt` — Reusable card for browsing
- `StationCard.kt` — Station-specific card with progress
- `RosaryBeadCounter.kt` — Interactive 53-bead grid
- `BreathingGuide.kt` — Animated circle for Jesus Prayer
- `VerseLink.kt` — Inline, clickable verse references
- `ProgressTracker.kt` — "Station X of 14" indicators
- `PrayerHubBrowser.kt` — Category tabs + content

---

## PHASE 3-6 SCREENS (THE HEART OF THE APP)

### Phase 3: Stations of the Cross
- **StationsListScreen** — Grid of 14 station cards
- **StationDetailScreen** — Full prayer + meditation + verses (70% of screen is prayer text)

### Phase 4: Rosary
- **RosaryHubScreen** — 4 mystery set cards (Joyful/Sorrowful/Glorious/Luminous)
- **RosaryMysteryScreen** — Mystery title + meditation prompt + linked verses
- **RosaryBeadCounterScreen** — Interactive bead counter (main prayer experience)

### Phase 5: Jesus Prayer
- **JesusPrayerLevelScreen** — 3 tabs (Vocal/Mental/Heart)
- **JesusPrayerGuideScreen** — Breathing guide + prayer counter (during session)

### Phase 6: Paraloka + Sramanikal
- **ParalokaHubScreen** — 15 verses + 5 funeral prayers browser
- **SramanikalTrackerScreen** — 40-day calendar with daily entry fields
- **GriefSupportScreen** — Comfort verses + Sramanikal explanation

---

## CRITICAL DESIGN DECISIONS

1. **Color Palette for Prayers**: New tokens for calm, sacred spaces
   - `meditation_bg` (ultra-soft, off-white)
   - `prayer_card` (softer than standard)
   - `progress_accent` (for beads/station counters)
   - Keep existing gold as primary prayer accent

2. **Bead Counter**: Interactive, satisfying
   - Visual grid showing all 53 beads
   - Current bead highlighted + larger
   - Completed beads dimmed/crossed out
   - Tap any bead to jump (or tap current to confirm)
   - Haptic feedback on tap (subtle pulse)

3. **Prayer Text Sizing**: Optimized for long reads
   - Default prayer text: 18sp (larger than body text)
   - Line height: 1.8x (generous spacing)
   - Verse references: 14sp, blue/gold underline (indicating link)
   - Meditation points: 16sp italic, secondary color

4. **Navigation**: "Prayers" as 4th primary tab
   - Read · Calendar · **Prayers** · More
   - Prayer hub shows 4 category cards (Stations, Rosary, Jesus Prayer, Paraloka)
   - Tap category → browse that prayer type
   - Or use search to find any prayer across all categories

5. **Verse Linking**: Seamless reader integration
   - Tap verse in prayer → reader opens at that ref
   - Verse highlighted in gold (3-second fade)
   - Can return to prayer without interruption
   - Mini-player stays visible during prayer viewing

---

## MULTI-LANGUAGE ROADMAP

### Phase 1-7: English + Tamil (Minimum Viable)
- All 4 prayer categories fully working in English + Tamil
- Launch internally with this

### Phase 8: Full Expansion
- Add Telugu, Hindi, Malayalam seed data
- Ensure proper font rendering for each script
- Language-aware UI (show prayers in user's UI language)
- Regional tradition appropriateness (CSI-Tamil, Catholic-regional, etc.)

---

## TESTING STRATEGY

### Unit Tests (Phase 11)
- GetStationsUseCase, GetRosaryMysteryUseCase, etc.
- Repository logic with in-memory Room DB
- Database query performance

### UI Tests (Phase 11)
- Navigate through all prayer screens
- Tap verses → verify reader opens correctly
- Mark stations complete → verify UI updates
- Bead counter increment → verify visual feedback
- Back navigation → verify history preserved

### Device Testing (Phase 11)
- Pixel 5 (6.0"), Galaxy A12 (6.5"), Redmi (5.5"), Tablet (10")
- Light + dark modes
- Tamil, Telugu, Hindi text rendering
- TalkBack navigation (accessibility)

### Performance Testing (Phase 11)
- Prayer list load < 100ms
- Bead counter animation 60 FPS
- Breathing guide 60 FPS
- Verse search < 200ms
- APK size increase < 5MB

---

## RISK MITIGATION

| Risk | Severity | Plan |
|------|----------|------|
| Content inaccuracy | 🔴 High | Contact official sources in Week 1; validate everything against liturgies |
| Language quality | 🔴 High | Hire native speakers Phase 8; church liaison review |
| Performance issues | 🟡 Medium | Profile continuously Phase 11; optimize database indexes |
| Scope creep | 🟡 Medium | Freeze feature list after Phase 0; defer audio, AR, AI to Year 2 |
| Multi-device bugs | 🟡 Medium | Use Google Play Pre-launch reports; test 5+ screen sizes |

---

## SUCCESS METRICS

✅ **Phase 3**: Stations fully prayable, tested, merged  
✅ **Phase 6**: All 4 prayer categories live, integrated, tested  
✅ **Phase 9**: Accessibility verified (WCAG 2.1 AA)  
✅ **Phase 12**: Play Store ready (English + Tamil, fully offline, 60 FPS, < 20MB)

**Launch Criteria**:
- [ ] All 14 Stations prayable + tracked
- [ ] All 5 Rosary mysteries + bead counter
- [ ] Jesus Prayer 3 levels + breathing guide
- [ ] 15 Paraloka verses + 5 funeral prayers + 40-day Sramanikal
- [ ] Verse linking works from every prayer
- [ ] Multi-language (English, Tamil, Telugu, Hindi, Malayalam)
- [ ] WCAG 2.1 AA accessibility
- [ ] Dark mode optimized
- [ ] 100% offline
- [ ] APK < 20MB, cold start < 1.5s, verse search < 200ms
- [ ] Zero crashes on real devices

---

## GET STARTED

**Week 1 Checklist (Phase 0)**:
- [ ] Contact CSI HQ for official prayer texts
- [ ] Reach out to Catholic Bishop's office
- [ ] Connect with Malankara Orthodox Church
- [ ] Finalize prayer content list (English + Tamil versions)
- [ ] Finalize design system (colors, fonts, spacing)
- [ ] Review database schema with team
- [ ] Set up seed data structure (JSON/CSV format)

**Then proceed to Phase 1 (database implementation) immediately.**

---

## NEXT STEPS

1. **Approve this roadmap** — confirm all phases and scope
2. **Start Phase 0 immediately** — contact prayer sources (don't wait)
3. **Allocate resources** — developer(s), designer(s), translator(s)
4. **Set up tracking** — Jira/GitHub issues for each phase
5. **Begin Phase 1** — database + seeder by Week 3

---

**This is the complete blueprint. Let's build the best prayer app for Indian Christians.** 🙏

