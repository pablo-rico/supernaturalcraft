"""GUI and effect textures: sigil glyphs, the journal page, the mana vial, the ward circle."""

import math
import random

import palette as P
from common import save
from pixelkit import Tex, fbm, hexc, item_outline, mix

GOLD_INK = P.GOLD[4]


def _line(t, x0, y0, x1, y1, c):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(steps + 1):
        s = i / steps
        t.set(int(round(x0 + (x1 - x0) * s)), int(round(y0 + (y1 - y0) * s)), c)


def _ring(t, cx, cy, r, c, a0=0, a1=360):
    n = int(r * 8) + 8
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        t.set(int(round(cx + math.cos(a) * r)), int(round(cy + math.sin(a) * r)), c)


# Each glyph: list of strokes in a 16x16 box. ("l", x0,y0,x1,y1) line, ("c", cx,cy,r[,a0,a1]) arc.
GLYPHS = {
    "touch": [("l", 8, 12, 8, 6), ("l", 6, 7, 6, 4), ("l", 8, 6, 8, 3), ("l", 10, 7, 10, 4), ("l", 5, 10, 6, 7), ("l", 6, 12, 10, 12)],
    "bolt": [("l", 4, 12, 11, 5), ("l", 11, 5, 7, 5), ("l", 11, 5, 11, 9), ("l", 5, 9, 7, 11)],
    "burst": [("l", 8, 3, 8, 13), ("l", 3, 8, 13, 8), ("l", 5, 5, 11, 11), ("l", 11, 5, 5, 11), ("c", 8, 8, 2)],
    "ward": [("c", 8, 8, 3.2), ("l", 8, 3, 8, 13), ("l", 3, 8, 13, 8)],
    "smite": [("l", 9, 3, 6, 8), ("l", 6, 8, 10, 8), ("l", 10, 8, 7, 13), ("l", 5, 3, 11, 3)],
    "hellfire": [("l", 8, 3, 5, 9), ("l", 8, 3, 11, 9), ("c", 8, 10, 3, 0, 180), ("l", 8, 7, 8, 11)],
    "frost": [("l", 8, 3, 8, 13), ("l", 4, 5, 12, 11), ("l", 12, 5, 4, 11), ("l", 7, 4, 9, 4), ("l", 7, 12, 9, 12)],
    "exorcise": [("l", 8, 3, 8, 13), ("l", 5, 6, 11, 6), ("c", 8, 8, 4.5, 200, 340), ("l", 6, 13, 10, 13)],
    "bind": [("c", 6, 8, 2.5), ("c", 10, 8, 2.5), ("l", 3, 13, 13, 3)],
    "mend": [("l", 8, 4, 8, 12), ("l", 4, 8, 12, 8), ("l", 6, 4, 10, 4), ("l", 6, 12, 10, 12)],
    "repel": [("l", 3, 8, 13, 8), ("l", 3, 8, 5, 6), ("l", 3, 8, 5, 10), ("l", 13, 8, 11, 6), ("l", 13, 8, 11, 10), ("l", 8, 5, 8, 11)],
    "reveal": [("c", 8, 11, 5, 200, 340), ("c", 8, 5, 5, 20, 160), ("l", 8, 7, 8, 9), ("l", 7, 8, 9, 8)],
    "empower": [("l", 4, 9, 8, 5), ("l", 8, 5, 12, 9), ("l", 4, 13, 8, 9), ("l", 8, 9, 12, 13)],
    "extend": [("l", 3, 8, 13, 8), ("l", 3, 5, 3, 11), ("l", 13, 5, 13, 11), ("l", 6, 6, 6, 10), ("l", 10, 6, 10, 10)],
    "widen": [("c", 8, 8, 2), ("c", 8, 8, 5, 300, 420), ("c", 8, 8, 5, 120, 240), ("l", 8, 3, 8, 5), ("l", 8, 11, 8, 13)],
    "echo": [("l", 4, 4, 8, 8), ("l", 8, 8, 4, 12), ("l", 8, 4, 12, 8), ("l", 12, 8, 8, 12)],
}
KIND = {"touch": "form", "bolt": "form", "burst": "form", "ward": "form",
        "empower": "modifier", "extend": "modifier", "widen": "modifier", "echo": "modifier"}
COLORS = {"smite": "#F2E6B0", "hellfire": "#FF6A1F", "frost": "#9FD8F0", "exorcise": "#C9C2FF", "bind": "#D23A2A",
          "mend": "#7FE07A", "repel": "#D8E4F0", "reveal": "#FFF6B0"}


def glyph(name):
    t = Tex(16, 16, 7)
    kind = KIND.get(name, "effect")
    ink = hexc(COLORS[name]) if kind == "effect" else (P.SILVER[5] if kind == "form" else P.GOLD[4])
    frame = mix(ink, (20, 16, 14, 255), 0.45)
    if kind == "form":
        for i in range(1, 15):
            for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
                t.set(x, y, frame)
        for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
            t.set(x, y, frame)
    elif kind == "effect":
        _ring(t, 7.5, 7.5, 7.0, frame)
    else:
        for i in range(8):
            for (x, y) in ((7 - i, i), (8 + i, i), (7 - i, 15 - i), (8 + i, 15 - i)):
                t.set(x, y, frame)
    for s in GLYPHS[name]:
        if s[0] == "l":
            _line(t, s[1] - 0.5, s[2] - 0.5, s[3] - 0.5, s[4] - 0.5, ink)
        else:
            a0, a1 = (s[4], s[5]) if len(s) > 4 else (0, 360)
            _ring(t, s[1] - 0.5, s[2] - 0.5, s[3], ink, a0, a1)
    item_outline(t, (18, 12, 10, 200))
    return t


def journal_page():
    W, H = 256, 256
    t = Tex(W, H, 3)
    rng = random.Random(4)
    for y in range(196):
        for x in range(W):
            n = fbm(x, y, 3, 256, 256, 3, 10.0)
            v = 3.6 + (n - 0.5) * 0.9
            edge = min(x, y, W - 1 - x, 195 - y)
            if edge < 10:
                v -= (10 - edge) * 0.09
            t.set(x, y, P.PARCHMENT[int(round(v))])
            if rng.random() < 0.004:
                t.set(x, y, P.PARCHMENT[1])
    # Leather border, gold inner rule.
    for y in range(196):
        for x in range(W):
            edge = min(x, y, W - 1 - x, 195 - y)
            if edge < 3:
                t.set(x, y, P.LEATHER[1 + edge])
            elif edge == 4:
                t.set(x, y, P.GOLD[2])
    # Corner flourishes.
    for cx, cy in ((10, 10), (W - 11, 10), (10, 185), (W - 11, 185)):
        _ring(t, cx, cy, 3, P.GOLD[2])
        t.set(cx, cy, P.BLOOD[3])
    return t


def mana_bar():
    t = Tex(32, 32)
    # Frame: 9x22 vial.
    for y in range(22):
        for x in range(9):
            edge = min(x, y, 8 - x, 21 - y)
            if edge == 0:
                t.set(x, y, P.OUTLINE)
            elif edge == 1:
                t.set(x, y, P.GOLD[2] if (y < 3 or y > 18) else P.STEEL[1])
            else:
                t.set(x, y, (20, 14, 30, 200))
    violet = [hexc("#2a0f4a"), hexc("#43177a"), hexc("#6327b3"), hexc("#8a4be0"), hexc("#b685ff"), hexc("#e2ccff")]
    grey = [hexc("#2b2830"), hexc("#3d3944"), hexc("#55505e"), hexc("#6e6878"), hexc("#8a8494"), hexc("#a7a1b0")]
    for k, ramp in ((1, violet), (2, grey)):
        for y in range(22):
            for x in range(9):
                v = 2 + (1 if x == 3 else 0) + (2 if x == 2 else 0) - (1 if x == 6 else 0)
                if y % 5 == 0 and x == 4:
                    v += 1
                t.set(k * 9 + x, y, ramp[max(0, min(5, v))])
    return t


def ward_circle():
    S = 64
    t = Tex(S, S, 9)
    c = S / 2
    white = (255, 255, 255, 235)
    soft = (255, 255, 255, 140)
    _ring(t, c - 0.5, c - 0.5, 30.5, white)
    _ring(t, c - 0.5, c - 0.5, 29.5, soft)
    _ring(t, c - 0.5, c - 0.5, 24, white)
    # Hexagram: two triangles.
    for rot in (-90, 90):
        pts = [(c - 0.5 + 23 * math.cos(math.radians(rot + k * 120)), c - 0.5 + 23 * math.sin(math.radians(rot + k * 120))) for k in range(3)]
        for k in range(3):
            _line(t, *pts[k], *pts[(k + 1) % 3], white)
    _ring(t, c - 0.5, c - 0.5, 6, white)
    # Runes in the outer band.
    rng = random.Random(3)
    for k in range(24):
        a = math.radians(k * 15 + 7)
        r = 27
        x, y = c - 0.5 + r * math.cos(a), c - 0.5 + r * math.sin(a)
        ang = rng.uniform(0, math.pi)
        _line(t, x - math.cos(ang) * 1.3, y - math.sin(ang) * 1.3, x + math.cos(ang) * 1.3, y + math.sin(ang) * 1.3, soft)
    return t


def hellforge_gui():
    """A dark iron panel, slot frames and an ember-lit inventory, 176x166."""
    W, H = 176, 166
    t = Tex(256, 256, 5)
    for y in range(H):
        for x in range(W):
            n = fbm(x, y, 5, 64, 64, 2, 8.0)
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if edge < 2:
                c = P.STEEL[1] if edge == 0 else P.STEEL[3]
            else:
                c = P.DEEPSLATE[1 + int(round(n * 2))]
            t.set(x, y, c)

    def slot(x, y, accent=None):
        for i in range(18):
            t.set(x + i, y, P.OUTLINE)
            t.set(x, y + i, P.OUTLINE)
            t.set(x + i, y + 17, P.STEEL[3])
            t.set(x + 17, y + i, P.STEEL[3])
        for yy in range(y + 1, y + 17):
            for xx in range(x + 1, x + 17):
                t.set(xx, yy, accent or P.DEEPSLATE[0])
    slot(25, 34, P.HELLFIRE[1])
    for i in range(4):
        slot(61 + i * 20, 34)
    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(7 + col * 18, 141)
    # Ember glow under the weapon slot.
    for x in range(20, 50):
        t.set(x, 54, P.HELLFIRE[3] if x % 3 else P.HELLFIRE[4])
    return t


def generate():
    save(hellforge_gui(), "gui", "hellforge")
    for name in GLYPHS:
        save(glyph(name), "gui/sigil", name)
    save(journal_page(), "gui", "journal")
    save(mana_bar(), "gui", "mana_bar")
    save(ward_circle(), "effect", "ward_circle")
