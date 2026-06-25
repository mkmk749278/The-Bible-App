# Gemini AI Features — Delivery Roadmap

> **Status**: Active  
> **Prerequisite reading**: [GEMINI_FEATURES.md](GEMINI_FEATURES.md), [IMPLEMENTATION.md](IMPLEMENTATION.md)  
> **Last updated**: 2026-06-25

This roadmap defines the phased delivery of all six Gemini AI features, sequenced by
dependency order, risk, and user impact. Each phase has a hard definition of done, a list
of blocking risks, and a rollback plan.

---

## Sequencing Rationale

Features were sequenced on three axes:

**Dependency order** — F-01 (cultural lens) is a prompt change that every subsequent
feature benefits from. It must ship first. F-02 (oral AI) depends on the `SpeechEngine`
interface extension from F-01's test pass. F-03 (crisis AI) and F-04 (sermon builder)
are independent of each other but both depend on the `GeminiApi` patterns established in
F-01. F-05 (verse cards AI) depends on the verse-resolution pattern proven in F-03.
F-06 (persecution comfort) is fully offline and has no Gemini dependency — it can ship
any time but is co-located with F-03 since both live in the crisis domain.

**Risk** — F-01 has zero network risk (it is a prompt string change). F-06 has zero
network risk (curated data). These two ship first with no feature flag gate needed.
F-02 through F-05 all require active API calls and are gated by feature flags.

**User impact** — F-01 immediately benefits every Telugu, Tamil, Malayalam, and Hindi
reader who uses Explain this passage. The change is invisible to users who do not care
about it and life-changing for those who do. It is the highest-ROI change in the set.

---

## Phase Timeline

```
Phase A │ F-01 + F-06       │ Weeks 1–2  │ No flag gate │ Zero network risk
Phase B │ F-03 + F-02       │ Weeks 3–5  │ Flag-gated   │ Crisis AI + Oral AI
Phase C │ F-04              │ Weeks 6–8  │ Flag-gated   │ Sermon Builder
Phase D │ F-05              │ Weeks 9–10 │ Flag-gated   │ Verse Recommendation
Phase E │ Flag promotion    │ Week 11    │ QA gate      │ All flags → true
```

---

## Phase A — Foundation (Weeks 1–2)

**Features**: F-01 (Indian Cultural Lens), F-06 (Persecution-Aware Comfort)  
**Flag changes**: None — both ship enabled by default.  
**Database changes**: None.

### Deliverables

| # | Deliverable | Acceptance test |
|---|-------------|-----------------|
| A-1 | `ExplanationRequest.denomination` field added | Builds; all existing tests pass |
| A-2 | `ExplanationPrompt.build()` includes cultural grounding for all requests | `ExplanationPromptTest` — cultural note present without denomination |
| A-3 | Denomination-specific instruction in prompt for Catholic, Orthodox, CSI, Mar Thoma | `ExplanationPromptTest` — each denomination class produces correct block |
| A-4 | Cache key includes denomination suffix | `DefaultExplanationRepositoryTest` — Catholic and Protestant requests produce different keys for the same osisRef |
| A-5 | `ReaderViewModel` passes `setup.denomination` to `ExplanationRequest` | Manual: explanation for a CSI user on John 15 references South Indian tradition |
| A-6 | `PersecutionCategory` enum + `DefaultPersecutionCompanion` curated verse sets | `PersecutionCompanionTest` — all five categories non-empty, all refs within 66-book canon |
| A-7 | `FeatureFlags.PERSECUTION_COMFORT = true` | — |
| A-8 | Crisis Mode screen shows category selector under `PERSECUTION_COMFORT` flag | Manual: five chips visible; tapping `FAMILY_REJECTION` shows Matt 10:34 text |

### Definition of Done

- [ ] All existing unit tests pass (`./gradlew testDebugUnitTest` — zero failures).
- [ ] New unit tests for A-2, A-3, A-4, A-6 pass.
- [ ] Manual QA: explanation for Genesis 1:1 in Telugu for a Catholic user contains
      the word "sacramental" or a Malayalam/Telugu equivalent AND references Indian
      cultural context.
- [ ] Manual QA: persecution categories visible and functional in Crisis Mode.
- [ ] APK size delta: < 5 KB (prompt strings only; no new library).
- [ ] Lint clean.

### Blocking Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Gemini ignores the cultural instruction | Low — Gemini 2.5-flash follows system instructions reliably | Adjust prompt wording; test against 5 passages manually before merge |
| Cache key change causes stale explanations for existing users | Low — old keys simply miss cache; explanation is regenerated on next tap | Acceptable — no data loss, just one extra API call |
| `setup.denomination` null for pre-existing users who completed setup before the field was tracked | Medium — all existing SetupState writes include denomination | Field defaults to null in ExplanationRequest; prompt omits denomination block gracefully |

### Rollback Plan

A-1 through A-5: revert `ExplanationPrompt.kt`, `Explanation.kt` (remove denomination
field with default), and `ReaderViewModel.kt`. The cache key change reverts; old cached
entries remain valid (they just lack denomination suffix — safe to serve to any user).

A-6 through A-8: toggle `FeatureFlags.PERSECUTION_COMFORT = false`. No data to roll back.

---

## Phase B — Crisis AI + Oral AI (Weeks 3–5)

**Features**: F-03 (Crisis AI Companion), F-02 (Oral Bible AI)  
**Flag changes**: `CRISIS_AI_COMPANION = false` (ships off), `ORAL_AI_EXPLANATION = false` (ships off)  
**Database changes**: None.

### Deliverables

| # | Deliverable | Acceptance test |
|---|-------------|-----------------|
| B-1 | `CrisisAiEngine` interface + `GeminiCrisisEngine` implementation | `GeminiCrisisEngineTest` — Success/Offline/Unavailable paths |
| B-2 | Night window prompt softening | `GeminiCrisisEngineTest` — night=true prompt contains "middle of the night" |
| B-3 | Denomination-aware crisis prompt | Manual or unit test: CATHOLIC request prompt contains "Catholic" |
| B-4 | Situation text never persisted | Code review: no Room insert, no DataStore write, no log.d of situation text anywhere in the call path |
| B-5 | `CrisisModeViewModel.submitSituation()` + state changes | `CrisisModeViewModelTest` — state sequence: loading → success; situationText cleared from state |
| B-6 | Crisis Mode screen UI changes (text field, AI response card, offline hint) | Manual under `CRISIS_AI_COMPANION = true` |
| B-7 | `SpeechEngine.isLanguageAvailable()` added to interface and impl | `SpeechEngineTest` or manual with Telugu voice absent |
| B-8 | `ReaderViewModel.speakExplanation()` / `stopExplanation()` | `ReaderViewModelTest` — explanationSpeaking state toggles |
| B-9 | Speak button on explanation sheet under `ORAL_AI_EXPLANATION` flag | Manual under flag=true: speaker icon starts TTS |
| B-10 | Narrated audio pauses when explanation TTS starts | Manual: start chapter audio, then tap speak — narration pauses |

### Definition of Done

- [ ] All existing unit tests pass.
- [ ] New tests for B-1, B-2, B-5, B-8 pass.
- [ ] `CRISIS_AI_COMPANION = true` manual QA with a live API key:
  - Malayalam input "My son is sick" → Malayalam response referencing a specific verse
    within 10 seconds on a 4G connection.
  - Situation text not present in any SQLite table or DataStore after response.
- [ ] `ORAL_AI_EXPLANATION = true` manual QA:
  - Explanation in Telugu spoken within 500ms of speaker tap.
  - Tapping again stops speech; icon returns to play state.
- [ ] Both flags ship `false` in the release build.
- [ ] Lint clean. APK size delta: < 50 KB (no new library; new Kotlin files only).

### Blocking Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Gemini response format not parseable (missing REF: / MESSAGE: lines) | Medium — LLM output is probabilistic | Add a `parseResponse` fallback: if parsing fails, return `Unavailable("Malformed response")` and log the raw text to a non-crash diagnostic channel (not production logs) |
| `AndroidSpeechEngine.speak()` is not suspending — blocks the coroutine | Medium — depends on current implementation | Wrap with `suspendCancellableCoroutine` if needed; do not call from main thread |
| Crisis AI response reveals personal situation to Crashlytics if an exception fires | Low — the situation string is a local variable, not a tag or breadcrumb | Review `GeminiCrisisEngine` exception handlers: catch blocks must not include `situation` in their logged detail string |

### Rollback Plan

Toggle `CRISIS_AI_COMPANION = false` and `ORAL_AI_EXPLANATION = false`. All new code is
behind these flags. No data to roll back. `SpeechEngine.isLanguageAvailable()` is
additive and safe to leave in place.

---

## Phase C — AI Sermon Builder (Weeks 6–8)

**Features**: F-04 (AI Sermon Builder)  
**Flag changes**: `SERMON_AI_BUILDER = false` (ships off)  
**Database changes**: None.

### Deliverables

| # | Deliverable | Acceptance test |
|---|-------------|-----------------|
| C-1 | `CongregationType` enum, `SermonOutlineRequest`, `SermonOutlineResult`, `SermonAiEngine` interface | Compiles; domain layer has no Android dependency |
| C-2 | `GeminiSermonEngine` implementation | `GeminiSermonEngineTest` — Success/Offline/Unavailable; denomination block in prompt for Catholic; congregation type in prompt for GRIEF |
| C-3 | `SermonHelperViewModel` refactored to MutableStateFlow pattern (prerequisite for `update`) | `SermonHelperViewModelTest` — all existing tests still pass |
| C-4 | `generateOutline()` added to ViewModel; outline inserted into draft | `SermonHelperViewModelTest` — draft.content populated with outline text on success |
| C-5 | Congregation type selector in sermon editor UI | Manual under `SERMON_AI_BUILDER = true` |
| C-6 | "Build outline" button visible when reference non-blank + engine configured | Manual |
| C-7 | Offline: button absent; manual edit fully functional | Manual: airplane mode + enter reference — no button, editor works |

### Definition of Done

- [ ] All existing unit tests pass.
- [ ] New tests for C-2, C-3 (regression), C-4 pass.
- [ ] Manual QA with live API key:
  - Reference "John 4:1-26", denomination CSI, language Tamil, congregation GENERAL →
    Tamil outline with three named points and a South Indian illustration within 30s.
  - Same reference, denomination CATHOLIC → outline references Baptism or Eucharist.
  - Reference "2 Corinthians 5:1", congregation GRIEF → no triumphalist language;
    tone is comforting.
- [ ] Flag ships `false`.
- [ ] Lint clean.

### Blocking Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `SermonHelperViewModel` state refactor breaks existing sermon list or save | High — stateIn → MutableStateFlow is a significant change | Write regression tests for all existing ViewModel functions before refactoring; run against both patterns to confirm parity |
| Gemini produces prose instead of structured outline | Medium | Add explicit "DO NOT write prose" line to the prompt; if the format is wrong, return as-is (the user edits it anyway) |
| 30s timeout on slow Indian mobile connections | Medium | Gemini 2.5-flash is typically < 10s for 1024 tokens; set OkHttp read timeout to 45s for the Gemini client specifically |

### Rollback Plan

Toggle `SERMON_AI_BUILDER = false`. The `SermonHelperViewModel` refactor (C-3) is the
highest-risk change; it must be reverted if it causes regressions. All new domain and
data-layer files can remain safely dead code.

---

## Phase D — Context-Aware Verse Cards (Weeks 9–10)

**Features**: F-05 (Context-Aware Verse Cards)  
**Flag changes**: `VERSE_RECOMMENDATION_AI = false` (ships off)  
**Database changes**: None.

### Deliverables

| # | Deliverable | Acceptance test |
|---|-------------|-----------------|
| D-1 | `VerseRecommendationEngine` interface + `GeminiVerseRecommendationEngine` | `VerseRecommendationEngineTest` — Success parses REF; verse text resolved from BibleContentRepository fake |
| D-2 | `VerseRecommendationViewModel` | `VerseRecommendationViewModelTest` — loading state; result state; clear state |
| D-3 | `VerseRecommendationScreen` with situation field + result card | Manual under flag=true |
| D-4 | Navigation entry point from `ScriptureCardScreen` or `VerseCardSheet` | Manual: tap "Find a verse" → opens recommendation screen |
| D-5 | Result feeds into existing `ScriptureCardRenderer` for share | Manual: recommended verse card visually identical to manually selected card |
| D-6 | Offline: text field disabled; share flow unaffected | Manual: airplane mode → field greyed, share flow via existing browse still works |

### Definition of Done

- [ ] All existing tests pass.
- [ ] New tests for D-1, D-2 pass.
- [ ] Manual QA: "wedding today" in Telugu → Ephesians 5 or Ruth passage resolved from
      active Telugu translation → card generated → WhatsApp share intent opens with
      personal message pre-populated.
- [ ] Flag ships `false`.
- [ ] Lint clean.

### Blocking Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| OSIS ref returned by Gemini is malformed or references a book not in the active canon | Medium | Validate: attempt `bibleContentRepository.chapter()` for the returned ref; if null, return `Unavailable("Verse not found in active translation")` and prompt user to try a different description |
| `GeminiVerseRecommendationEngine` has two injected repositories — testing is complex | Low | Use simple fakes per the established test pattern |

---

## Phase E — Feature Flag Promotion (Week 11)

**Purpose**: Promote all stable flags from `false` to `true` after QA sign-off.

### Promotion Criteria (per feature)

A flag is promoted when:
1. No P0 or P1 bug filed against the feature in the last 5 business days.
2. The feature passes the manual QA checklist in its phase definition of done.
3. At least one non-developer user has tested it on a physical device with an Indian
   language (Telugu, Tamil, Malayalam, or Hindi).
4. The Gemini API key is confirmed provisioned and scoped for production traffic.

### Promotion Order

```
PERSECUTION_COMFORT    already true — no change
ORAL_AI_EXPLANATION    promote after B passes criteria
CRISIS_AI_COMPANION    promote after B passes criteria
SERMON_AI_BUILDER      promote after C passes criteria
VERSE_RECOMMENDATION_AI promote after D passes criteria
```

F-01 (cultural lens) shipped without a flag and cannot be reverted via flag. If a
regression is found post-production, a hotfix that reverts `ExplanationPrompt.kt` is
the rollback path.

---

## Release Alignment

| Phase | Target release version | Branch |
|-------|----------------------|--------|
| A | v1.3.0 | `feature/ai-cultural-lens` |
| B | v1.4.0 | `feature/ai-crisis-oral` |
| C | v1.5.0 | `feature/ai-sermon-builder` |
| D | v1.6.0 | `feature/ai-verse-cards` |
| E | v1.7.0 | `feature/ai-flag-promotion` |

All feature branches target `develop` as their merge destination. `develop` → `main` via
the standard release process defined in CLAUDE.md.

---

## API Cost Projection

All cloud calls use `gemini-2.5-flash` at the time of writing. Token estimates are
conservative (prompt + response).

| Feature | Avg. tokens/call | Calls/DAU estimate | Monthly cost @ 10K DAU |
|---------|-----------------|-------------------|----------------------|
| F-01 (cultural lens) | ~1,100 | 0.3 | Already budgeted in Explain |
| F-03 (crisis AI) | ~600 | 0.05 | ~$9 |
| F-04 (sermon builder) | ~1,400 | 0.02 | ~$6 |
| F-05 (verse cards) | ~700 | 0.04 | ~$6 |

Gemini Nano (on-device, F-01 via the hybrid engine) incurs zero token cost. The
`DefaultExplanationRepository` cache means F-01 cost decreases over time as popular
passages get cached. Crisis and sermon calls are never cached.

These projections assume Gemini 2.5-flash pricing of ~$0.15/1M input tokens,
~$0.60/1M output tokens. Monitor actual usage via GCP Console and set a budget alert
at 150% of projection.
