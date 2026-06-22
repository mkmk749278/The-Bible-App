#!/usr/bin/env python3
"""Generate Manna's Play Store brand graphics (app icon + feature graphic).

Outputs PNGs sized exactly to Play's requirements, plus editable SVG sources.
The mark and colours match the in-app adaptive launcher icon
(app/src/main/res/.../ic_launcher_*) so the store icon and installed icon agree.

Run:  python3 generate_graphics.py   (needs Pillow)
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

# --- Brand colours (from CLAUDE.md design tokens / launcher icon) -------------
NAVY = (8, 12, 20)          # #080C14  launcher background / dark ink
GOLD = (201, 149, 42)       # #C9952A  sacred accent (dark-palette gold)
GOLD_DEEP = (138, 103, 28)  # #8A671C  muted gold (light-palette)
CREAM = (250, 247, 239)     # #FAF7EF  warm white bg
CREAM_WARM = (243, 232, 205) # soft warm gold-cream for gradient end
INK = (31, 45, 61)          # #1F2D3D  deep navy ink
SAGE = (63, 122, 92)        # #3F7A5C  sage accent

# The "M" mark, as the same polygon used by ic_launcher_foreground.xml
# (108x108 viewport). Centre of the glyph is ~(54, 56).
M_PATH = [(36,42),(46,42),(54,58),(62,42),(72,42),(72,70),
          (64,70),(64,54),(56,68),(52,68),(44,54),(44,70),(36,70)]
M_CX, M_CY = 54, 56

SS = 4  # supersampling factor for crisp polygon edges


def map_pts(scale, ox, oy):
    return [(ox + (x - M_CX) * scale, oy + (y - M_CY) * scale) for x, y in M_PATH]


def font(path, size):
    return ImageFont.truetype(path, size)


SERIF_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def make_icon(size=512, out="icon-512.png"):
    img = Image.new("RGBA", (size * SS, size * SS), NAVY + (255,))
    d = ImageDraw.Draw(img)
    # M sized to ~56% of the canvas width, centred.
    target_w = size * SS * 0.56
    scale = target_w / 36.0  # glyph is 36 viewport units wide
    cx = cy = size * SS / 2
    d.polygon(map_pts(scale, cx, cy), fill=GOLD + (255,))
    img = img.resize((size, size), Image.LANCZOS)
    img.save(os.path.join(HERE, out))
    print("wrote", out)


def vgradient(w, h, top, bottom):
    base = Image.new("RGB", (w, h), top)
    top_arr = top
    for y in range(h):
        t = y / (h - 1)
        # ease so most of the canvas stays light, warming toward the bottom
        t = t * t
        col = tuple(round(top_arr[i] + (bottom[i] - top_arr[i]) * t) for i in range(3))
        ImageDraw.Draw(base).line([(0, y), (w, y)], fill=col)
    return base


def make_feature(w=1024, h=500, out="feature-graphic-1024x500.png"):
    img = vgradient(w, h, CREAM, CREAM_WARM)
    # soft sunlight glow, upper-right
    glow = Image.new("L", (w, h), 0)
    gd = ImageDraw.Draw(glow)
    gd.ellipse([w * 0.55, -h * 0.6, w * 1.5, h * 0.8], fill=70)
    img = Image.composite(Image.new("RGB", (w, h), (255, 250, 235)), img, glow)
    d = ImageDraw.Draw(img)

    # Icon chip on the left: a rounded navy square with the gold M (echoes install icon)
    chip = h - 200
    cx0, cy0 = 96, (h - chip) // 2
    chip_img = Image.new("RGBA", (chip * SS, chip * SS), (0, 0, 0, 0))
    cd = ImageDraw.Draw(chip_img)
    r = int(chip * SS * 0.22)
    cd.rounded_rectangle([0, 0, chip * SS, chip * SS], radius=r, fill=NAVY + (255,))
    scale = (chip * SS * 0.56) / 36.0
    cd.polygon(map_pts(scale, chip * SS / 2, chip * SS / 2), fill=GOLD + (255,))
    chip_img = chip_img.resize((chip, chip), Image.LANCZOS)
    img.paste(chip_img, (cx0, cy0), chip_img)

    tx = cx0 + chip + 56
    safe_right = w - 48  # keep text clear of the edge (Play crops/overlays edges)

    def fit(text, path, size, max_w):
        """Shrink the font until the text fits within max_w pixels."""
        while size > 8:
            f = font(path, size)
            if d.textlength(text, font=f) <= max_w:
                return f
            size -= 2
        return font(path, size)

    max_w = safe_right - tx
    # Wordmark
    f_title = fit("Manna", SERIF_BOLD, 132, max_w)
    d.text((tx, 150), "Manna", font=f_title, fill=INK)
    bbox = d.textbbox((tx, 150), "Manna", font=f_title)
    # gold rule under the wordmark
    d.rectangle([tx + 4, bbox[3] + 18, bbox[2], bbox[3] + 24], fill=GOLD_DEEP)
    # tagline — "Manna" means daily bread from heaven (Exodus 16)
    tag = "Daily Bread from Heaven"
    f_tag = fit(tag, SERIF, 42, max_w)
    d.text((tx + 4, bbox[3] + 40), tag, font=f_tag, fill=GOLD_DEEP)
    # quiet subtitle
    sub = "Bible · Prayers · Daily Verse · Offline"
    f_sub = fit(sub, SANS, 26, max_w)
    d.text((tx + 6, bbox[3] + 104), sub, font=f_sub, fill=(106, 120, 132))

    img.save(os.path.join(HERE, out))
    print("wrote", out)


if __name__ == "__main__":
    make_icon()
    make_feature()
