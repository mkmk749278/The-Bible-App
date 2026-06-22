# Store graphics — assets

Generated brand graphics for the Play listing. Sized exactly to Play's specs and
colour-matched to the in-app launcher icon.

| File | Spec | Status |
|------|------|--------|
| `icon-512.png` | 512×512 app icon | ✅ final — gold "M" (#C9952A) on navy (#080C14), matches the installed launcher icon |
| `feature-graphic-1024x500.png` | 1024×500 feature graphic | ✅ final — warm cream→gold "morning sunlight" background, navy wordmark, gold accent |
| `generate_graphics.py` | source generator (Pillow) | regenerate with `python3 generate_graphics.py` |

## Phone screenshots — still to capture (2–8 required)

These are **not** generated here on purpose: Play policy requires screenshots to
honestly reflect the running app, so they must be **real device/emulator
captures** — a fabricated mockup risks a listing rejection. Capture them at
1080×1920 (or taller) from a debug build. Suggested set (see
[`../03-graphics-checklist.md`](../03-graphics-checklist.md)):

1. Reader — a chapter in clean serif scripture
2. Listen / mini player
3. Language picker (Tamil UI + English Bible)
4. Simplified / Elder Mode
5. Prayers hub
6. Daily verse / Home

## Regenerating / editing

The mark is the same polygon as `ic_launcher_foreground.xml` (108×108 viewport),
so the store icon and launcher stay in sync. Edit colours/text in
`generate_graphics.py` and re-run. Requires `pip install Pillow`. Fonts used:
DejaVu Serif/Sans (Latin only — there are no Indic fonts in this toolchain, so
the feature graphic is intentionally English; localized feature graphics, if
desired, need a Noto Serif/Sans Indic font added).
