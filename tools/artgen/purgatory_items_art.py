"""Purgatory's items (v0.39): soul amber and Eve's spoils, the Mother's Heartwood (names in purgatory/PurgatoryAssets.java).

16x16 sprites, outlined like Hell's items (hell_art.item_outline).
"""

import math

from common import save
from hell_art import item_outline
from pixelkit import Ramp, Tex, fbm, hexc, mix

AMBER = Ramp("#4a2205", "#7a3d08", "#b0640f", "#d98f1e", "#f2b844", "#ffe39a")
SOUL = hexc("#eef8ff")
SOUL_DIM = hexc("#b9d4e6")
WOOD = Ramp("#120c09", "#22160f", "#352218", "#4a3022", "#61412e", "#7a553d")


def _inside(pts, x, y):
    n = len(pts)
    return all(((pts[(i + 1) % n][0] - pts[i][0]) * (y - pts[i][1]) - (pts[(i + 1) % n][1] - pts[i][1]) * (x - pts[i][0])) >= 0
               for i in range(n))


def soul_amber():
    """A rough lump of amber, lit from the top left, with a pale soul-wisp curled inside it."""
    t = Tex(16, 16, 39101)
    pts = [(6, 2), (11, 3), (14, 8), (12, 13), (6, 14), (2, 10), (3, 5)]
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            if not _inside(pts, px, py):
                continue
            n = fbm(x, y, 39102, 16, 16, 2, 4.0)
            v = 3.6 - (px - 4) * 0.12 - (py - 4) * 0.18 + (n - 0.5) * 1.0
            t.set(x, y, AMBER[max(1, min(5, int(round(v))))])
    # Facets: a bright edge on the top left, a dark rim at the bottom right.
    for (x, y) in ((6, 3), (7, 3), (5, 4), (4, 5), (4, 6)):
        t.set(x, y, AMBER[5])
    for (x, y) in ((12, 11), (11, 12), (10, 13), (9, 13), (13, 9)):
        t.set(x, y, AMBER[1])
    # The wisp: a little spiral of pale light.
    for i in range(9):
        a = i * 0.75
        r = 0.6 + i * 0.32
        x, y = int(round(8.5 + math.cos(a) * r)), int(round(8.5 + math.sin(a) * r))
        t.set(x, y, mix(SOUL, AMBER[4], i / 14))
    t.set(8, 8, SOUL)
    t.set(9, 8, SOUL_DIM)
    item_outline(t)
    return t


def mothers_heartwood():
    """A gnarled knot of near-black heartwood, roots curling off it, veins of glowing amber through it."""
    t = Tex(16, 16, 39111)
    cx, cy = 8.0, 8.0
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            ang = math.atan2(py - cy, px - cx)
            r = 5.6 + 0.9 * math.sin(ang * 3 + 0.6) + 0.5 * math.sin(ang * 5 + 1.3)
            d = math.hypot(px - cx, (py - cy) * 1.05)
            if d < r:
                n = fbm(x, y, 39112, 16, 16, 2, 5.0)
                v = 3.0 - (px - 5) * 0.13 - (py - 4) * 0.16 + (n - 0.5) * 1.4
                t.set(x, y, WOOD[max(1, min(5, int(round(v))))])
    # Short thick roots curling off its underside.
    for line in ([(4, 13), (3, 14), (2, 14)], [(11, 13), (12, 14), (13, 14)], [(7, 14), (7, 15)]):
        for i, (x, y) in enumerate(line):
            t.set(x, y, WOOD[2] if i else WOOD[3])
    # Gnarled grain: dark whorls round two knots.
    for (kx, ky) in ((5, 6), (11, 9)):
        for a in range(8):
            ang = a * math.pi / 4
            t.set(int(round(kx + math.cos(ang) * 1.4)), int(round(ky + math.sin(ang) * 1.4)), WOOD[1])
        t.set(kx, ky, WOOD[0])
    # Amber veins: a branching crack of light through it.
    vein = [(4, 10), (5, 10), (6, 9), (7, 8), (8, 8), (9, 7), (10, 6), (11, 5), (8, 9), (8, 10), (9, 11), (7, 7), (7, 6)]
    for i, (x, y) in enumerate(vein):
        t.set(x, y, AMBER[5] if i in (4, 5) else AMBER[4] if i % 2 else AMBER[3])
    t.set(8, 8, hexc("#fff4c4"))
    item_outline(t)
    return t


def generate():
    save(soul_amber(), "item", "soul_amber")
    save(mothers_heartwood(), "item", "mothers_heartwood")


if __name__ == "__main__":
    generate()
