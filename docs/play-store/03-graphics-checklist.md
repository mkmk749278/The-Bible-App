# Store graphics checklist

Everything Play requires for the **Main store listing**, with exact specs and a
content brief drawn from the Manna design system (warm white, deep navy ink,
muted gold accent, sage — "morning sunlight entering a church"). Keep text on
graphics minimal; Play rejects feature graphics that are mostly text.

> **Where these live in the repo.** None of the raster store graphics exist yet —
> only the in-app adaptive launcher icon (vector, in `app/src/main/res/`). Once
> produced, store them under `docs/play-store/assets/` for reference and, if you
> later automate listing uploads, mirror them into the publisher's listing dir.

---

## Required

### 1. App icon (Play listing)
- **Size:** 512 × 512 px
- **Format:** 32-bit PNG (with alpha) — but design as if opaque; Play applies the
  rounded mask.
- **Content:** the Manna mark on the warm cream/gold background. Derive it from
  the adaptive icon (`res/drawable/ic_launcher_foreground.xml` +
  `res/values/ic_launcher_background.xml`) so the store icon matches the installed
  icon exactly.
- **Note:** this is separate from the in-APK launcher icon; both must agree
  visually.

### 2. Feature graphic
- **Size:** 1024 × 500 px
- **Format:** PNG or JPEG, no alpha
- **Content:** calm hero banner — soft cream→gold light, the wordmark
  "Manna" with the tagline "Daily Bread from Heaven" and a small subtitle
  ("Bible · Prayers · Daily Verse · Offline"). Leave generous whitespace.
  Avoid neon, gradients-gone-wild, or busy collage. This shows at the
  top of the listing and in promos.

### 3. Phone screenshots — **2 to 8** (at least 2 required)
- **Aspect ratio:** 16:9 or 9:16
- **Min dimension:** 320 px; **max:** 3840 px; each side between 320–3840 px.
- **Recommended:** portrait 1080 × 1920 (or 1080 × 2400).
- **Format:** PNG or JPEG.
- **Suggested set (capture from the running app):**
  1. **Reader** — a chapter in clean serif scripture (the hero surface).
  2. **Listen** — mini player / read-aloud in progress.
  3. **Language picker** — Tamil UI + English Bible (show the dual-language idea).
  4. **Simplified / Elder Mode** — big buttons, large text.
  5. **Prayers hub** — Rosary / Stations in an Indian language.
  6. **Daily verse / home** — Continue Reading + Today's Verse.
- Optional: add a one-line caption band per shot in brand colors (keep it short
  and high-contrast for accessibility).

---

## Recommended (improves placement / required for some device listings)

### 4. 7-inch tablet screenshots — up to 8
- **Recommended:** 1200 × 1920 (portrait) or landscape equivalent.
- Needed if you want the listing to look right on small tablets.

### 5. 10-inch tablet screenshots — up to 8
- **Recommended:** 1600 × 2560 (portrait) or landscape equivalent.

### 6. (Optional) Promo / intro video
- A YouTube URL. Skip for launch; add later.

---

## Production tips

- **Capture clean frames:** run on a device/emulator at the recommended
  resolution, use a real chapter (e.g. John 1 / யோவான் 1), hide debug overlays.
- **Localized listings (later):** if you add `ta-IN`, `hi-IN`, `te-IN`, `ml-IN`
  store listings, each locale can have its own screenshots — show that locale's
  UI for authenticity.
- **Accessibility carries over:** the design system's 4.5:1 contrast requirement
  applies to any text you bake into graphics, too.
- **Don't imply features you don't ship** (Play policy on misleading assets) —
  only screenshot what's actually in versionCode 5.

## Checklist

- [x] 512×512 app icon (PNG, matches launcher icon) → `assets/icon-512.png`
- [x] 1024×500 feature graphic → `assets/feature-graphic-1024x500.png`
- [ ] ≥2 phone screenshots (recommend 4–6, portrait 1080×1920+) — **real device captures needed**
- [ ] (optional) 7" tablet screenshots
- [ ] (optional) 10" tablet screenshots
- [x] Assets directory created → `docs/play-store/assets/` (regenerate with `generate_graphics.py`)
