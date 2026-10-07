"""Ink echoes (v0.10): the Author writes the hunter's old enemies back in living ink for one attack.

An echo is drawn with the boss's OWN GeckoLib model and a recoloured copy of its texture, so every texture here is
derived by code from the boss's shipped texture: same size, same UV layout. The recolour turns the painting into wet
ink (black to indigo, the original shading kept as the value of the ink), runs a broken paper-white rim around every
cube face (read from the boss's geo.json, so the edges of the model read as pen lines) and lets a few gold drips run
down the vertical faces. Whatever glowed in the original (eyes, cracks of light) burns gold. `ink/<boss>_glowmask.png`
carries the rim, the drips and the gold, for an emissive layer.

Which texture is echoed (the boss's main look; renderer: `textures/entity/ink/<boss>.png` on `geo/entity/<geo>.geo.json`):
    lucifer          <- lucifer_p1.png          (the vessel in his coat; wings painted in ash, hide them as you like)
    azazel           <- azazel_p1.png
    lilith           <- lilith_p1.png
    metatron         <- metatron_p1.png
    lucifer_uncaged  <- lucifer_uncaged_p1.png  (model lucifer_uncaged.geo.json)
    amara            <- amara.png
    broken_chorus    <- broken_chorus.png
This module must run after the boss modules (it reads their PNG and geo.json): it is last in generate.MODULES.
"""

import json
import os
import random

from common import ASSETS, TEXTURES, save
from pixelkit import Tex, fbm, hexc, mix
from pngio import read_png

ECHOES = {
    "lucifer": ("lucifer_p1", "lucifer"),
    "azazel": ("azazel_p1", "azazel"),
    "lilith": ("lilith_p1", "lilith"),
    "metatron": ("metatron_p1", "metatron"),
    "lucifer_uncaged": ("lucifer_uncaged_p1", "lucifer_uncaged"),
    "amara": ("amara", "amara"),
    "broken_chorus": ("broken_chorus", "broken_chorus"),
}

# Ink, darkest to the glossiest blue-violet sheen.
INK = [hexc("#040307"), hexc("#0b0914"), hexc("#141127"), hexc("#1e1a3b"), hexc("#2b2654"), hexc("#3d3772")]
SHEEN = hexc("#5a54a8")
PAPER = hexc("#efe8d6")
PAPER_DIM = hexc("#c9c0aa")
GOLD = [hexc("#9a6b1c"), hexc("#d9a93a"), hexc("#f3d27a")]
CLEAR = (0, 0, 0, 0)


def face_rects(geo_name):
    """Every cube face of a model as (face, u, v, w, h) in texels, read from its geo.json."""
    with open(os.path.join(ASSETS, "geo", "entity", geo_name + ".geo.json")) as f:
        geo = json.load(f)
    rects = []
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            for face, r in cube.get("uv", {}).items():
                (u, v), (w, h) = r["uv"], r["uv_size"]
                rects.append((face, int(u), int(v), int(abs(w)), int(abs(h))))
    return rects


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


def ink_of(c, x, y, seed, w, h):
    """The body colour: the source's value becomes the ink's depth, with a faint wet sheen."""
    v = lum(c) ** 0.7
    n = fbm(x, y, seed, w, h, 2, 24.0)
    k = v * 4.2 + (n - 0.5) * 0.9
    i = max(0, min(len(INK) - 1, int(k)))
    col = mix(INK[i], INK[min(len(INK) - 1, i + 1)], max(0.0, min(1.0, k - i)))
    if n > 0.78:
        col = mix(col, SHEEN, (n - 0.78) * 1.6)
    return (col[0], col[1], col[2], c[3])


def echo(boss):
    src_name, geo_name = ECHOES[boss]
    w, h, rows = read_png(os.path.join(TEXTURES, "entity", src_name + ".png"))
    seed = sum(map(ord, boss)) * 13
    rng = random.Random(seed)
    t, g = Tex(w, h, seed), Tex(w, h, seed + 1)
    for y in range(h):
        for x in range(w):
            c = rows[y][x]
            if c[3]:
                t.set(x, y, ink_of(c, x, y, seed, w, h))
    # What glowed in the original (eyes, cracks of light) burns gold in the ink; faint whole-body glows are ignored.
    glow_path = os.path.join(TEXTURES, "entity", src_name + "_glowmask.png")
    if os.path.exists(glow_path):
        _, _, glow_rows = read_png(glow_path)
        for y in range(h):
            for x in range(w):
                c = glow_rows[y][x]
                if c[3] and rows[y][x][3] and max(c[:3]) > 200:
                    gold = GOLD[2] if lum(c) > 0.6 else GOLD[1]
                    t.set(x, y, (gold[0], gold[1], gold[2], max(rows[y][x][3], 200)))
                    g.set(x, y, gold)

    def opaque(x, y):
        return 0 <= x < w and 0 <= y < h and rows[y][x][3] > 0

    def paint(x, y, c, glow=True):
        if opaque(x, y):
            a = rows[y][x][3]
            t.set(x, y, (c[0], c[1], c[2], max(a, 200)))
            if glow:
                g.set(x, y, (c[0], c[1], c[2], 255))

    # Pen lines: a broken pale rim, paper-white along the top edge of every face (light catching wet ink) and a
    # fainter grey-violet down the long sides of the bigger faces; slivers stay ink (a rim would turn them white).
    for face, u, v, fw, fh in face_rects(geo_name):
        if fw < 3 or fh < 3:
            continue
        top = [(u + i, v) for i in range(fw)]
        sides = []
        if fw >= 6:
            sides = [(u, v + j) for j in range(1, fh)] + [(u + fw - 1, v + j) for j in range(1, fh)]
        for (x, y) in top:
            n = fbm(x, y, seed + 7, w, h, 2, 32.0)
            if n > 0.42:
                paint(x, y, PAPER if n > 0.55 else PAPER_DIM)
        for (x, y) in sides:
            n = fbm(x, y, seed + 9, w, h, 2, 32.0)
            if n > 0.5:
                paint(x, y, mix(t.get(x, y), PAPER_DIM, 0.45), glow=False)
        # Gold drips running down from the top edge of the vertical faces: a bead at the top, a thinning trail.
        if face in ("north", "south", "east", "west") and fh >= 6 and fw >= 3:
            for _ in range(max(1, fw // 10)):
                if rng.random() > 0.45:
                    continue
                dx = u + rng.randrange(1, fw - 1)
                length = rng.randint(2, max(3, int(fh * 0.55)))
                for j in range(length):
                    paint(dx, v + j, GOLD[2] if j == 0 else GOLD[1] if j < length - 1 else GOLD[0])
                paint(dx, v + length, GOLD[1])
    return t, g


def generate():
    for boss in ECHOES:
        t, g = echo(boss)
        save(t, "entity/ink", boss)
        save(g, "entity/ink", boss + "_glowmask")
