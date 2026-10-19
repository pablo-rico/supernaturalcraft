"""The magic of v0.21 (MagicAssets): particle sheets, cast circle, glyph strip, bolt core, bowl-spell seals, school icons,
mastery badges and the new reagent items.

Everything the client tints at runtime (particles, circle, strip, core, seals) is white/grey on alpha; icons, badges and
items are full colour. The four schools each have a script of their own (RUNES): Enochian angular letters, Infernal
jagged barbed marks, Witchcraft curving moons and knots, Hunter devil's-trap geometry.
"""

import math
import random

import palette as P
from common import art, save
from pixelkit import Ramp, Tex, hexc, item_outline, mix
from vecdraw import Field, draw_glyph
import magic_seals

# --- the four scripts (unit box, x right, y down) --------------------------------------------------------------------------

ENOCHIAN = [
    [("p", [(0.22, 0.18), (0.78, 0.18), (0.78, 0.84)]), ("p", [(0.22, 0.18), (0.22, 0.52), (0.6, 0.52)])],
    [("p", [(0.25, 0.14), (0.25, 0.86), (0.76, 0.86)]), ("p", [(0.25, 0.5), (0.74, 0.26)])],
    [("p", [(0.18, 0.84), (0.5, 0.2), (0.82, 0.84)]), ("p", [(0.34, 0.56), (0.66, 0.56)]), ("d", 0.5, 0.07, 0.06)],
    [("p", [(0.76, 0.16), (0.28, 0.16), (0.28, 0.5), (0.72, 0.5), (0.72, 0.84), (0.24, 0.84)])],
    [("p", [(0.18, 0.18), (0.5, 0.5), (0.82, 0.18)]), ("p", [(0.5, 0.5), (0.5, 0.86)]), ("p", [(0.32, 0.72), (0.68, 0.72)])],
    [("p", [(0.3, 0.86), (0.3, 0.14), (0.74, 0.34), (0.3, 0.54), (0.76, 0.86)])],
    [("p", [(0.18, 0.32), (0.82, 0.32)]), ("p", [(0.22, 0.7), (0.78, 0.7)]), ("p", [(0.5, 0.14), (0.5, 0.86)])],
    [("p", [(0.2, 0.5), (0.5, 0.16), (0.8, 0.5), (0.5, 0.84), (0.2, 0.5)]), ("d", 0.5, 0.5, 0.07)],
]

INFERNAL = [
    [("p", [(0.5, 0.88), (0.5, 0.3)]), ("p", [(0.18, 0.12), (0.22, 0.42), (0.78, 0.42), (0.82, 0.12)]),
     ("p", [(0.5, 0.42), (0.5, 0.12)])],
    [("p", [(0.32, 0.1), (0.62, 0.4), (0.36, 0.56), (0.7, 0.9)]), ("p", [(0.7, 0.9), (0.52, 0.84)])],
    [("p", [(0.5, 0.1), (0.5, 0.9)]), ("p", [(0.2, 0.56), (0.26, 0.66), (0.74, 0.66), (0.8, 0.56)])],
    [("c", 0.5, 0.62, 0.22, 0, 360), ("p", [(0.32, 0.48), (0.18, 0.12), (0.36, 0.28)]),
     ("p", [(0.68, 0.48), (0.82, 0.12), (0.64, 0.28)])],
    [("p", [(0.5, 0.1), (0.5, 0.86)]), ("p", [(0.28, 0.64), (0.5, 0.88), (0.72, 0.64)]),
     ("p", [(0.3, 0.26), (0.5, 0.4), (0.7, 0.26)])],
    [("p", [(0.14, 0.86), (0.3, 0.18), (0.5, 0.62), (0.7, 0.18), (0.86, 0.86)])],
]

WITCH = [
    [("c", 0.5, 0.5, 0.34, 50, 310), ("c", 0.68, 0.5, 0.26, 105, 255)],
    [("c", 0.5, 0.36, 0.2, 0, 360), ("p", [(0.5, 0.56), (0.5, 0.9)]), ("p", [(0.3, 0.76), (0.7, 0.76)])],
    [("c", 0.36, 0.38, 0.17, 120, 420), ("c", 0.64, 0.38, 0.17, 120, 420), ("c", 0.5, 0.64, 0.17, 0, 360)],
    [("p", [(0.5 + (0.04 + 0.34 * k / 40) * math.cos(k / 40 * 3.2 * math.pi), 0.5 + (0.04 + 0.34 * k / 40) * math.sin(
        k / 40 * 3.2 * math.pi)) for k in range(41)])],
    [("c", 0.5, 0.75, 0.42, 205, 335), ("c", 0.5, 0.25, 0.42, 25, 155), ("d", 0.5, 0.5, 0.09)],
    [("p", [(0.5, 0.12), (0.5, 0.88)]), ("c", 0.5, 0.5, 0.3, 200, 340), ("c", 0.5, 0.5, 0.3, 20, 160)],
]

HUNTER = [
    [("c", 0.5, 0.5, 0.38, 0, 360)] + [("p", [
        (0.5 + 0.36 * math.cos(math.radians(-90 + 144 * k)), 0.5 + 0.36 * math.sin(math.radians(-90 + 144 * k)))
        for k in range(6)])],
    [("c", 0.5, 0.5, 0.3, 0, 360), ("p", [(0.5, 0.1), (0.5, 0.9)]), ("p", [(0.1, 0.5), (0.9, 0.5)])],
    [("p", [(0.5, 0.14), (0.86, 0.8), (0.14, 0.8), (0.5, 0.14)]), ("d", 0.5, 0.58, 0.08)],
    [("p", [(0.2, 0.2), (0.8, 0.2), (0.8, 0.8), (0.2, 0.8), (0.2, 0.2)]), ("p", [(0.2, 0.2), (0.8, 0.8)]),
     ("p", [(0.8, 0.2), (0.2, 0.8)])],
    [("p", [(0.18, 0.3), (0.82, 0.3)]), ("p", [(0.18, 0.5), (0.82, 0.5)]), ("p", [(0.18, 0.7), (0.82, 0.7)]),
     ("p", [(0.5, 0.12), (0.5, 0.88)])],
    [("p", [(0.5, 0.12), (0.84, 0.5), (0.5, 0.88), (0.16, 0.5), (0.5, 0.12)]), ("p", [(0.5, 0.3), (0.5, 0.7)])],
]

RUNES = {"enochian": ENOCHIAN, "infernal": INFERNAL, "witchcraft": WITCH, "hunter": HUNTER}
ALL_RUNES = ENOCHIAN + INFERNAL + WITCH + HUNTER


def rune_preview(path):
    """Debug sheet of every rune at 32 px."""
    f = Field(32 * 8, 32 * 4)
    for row, script in enumerate((ENOCHIAN, INFERNAL, WITCH, HUNTER)):
        for i, g in enumerate(script):
            draw_glyph(f, g, 16 + 32 * i, 16 + 32 * row, 26, 1.6)
    save_path = path
    f.bake().save(save_path)


# --- particles (white/grey, tinted by MagicParticleOptions) --------------------------------------------------------------

def _grey(v, a=255):
    return (v, v, v, a)


W_ = _grey(255)
L_ = _grey(225, 235)
M_ = _grey(190, 200)
D_ = _grey(150, 140)
F_ = _grey(120, 80)
GREYS = {"W": W_, "L": L_, "m": M_, "d": D_, "f": F_}


def _px(rows):
    return art(rows, GREYS)


def magic_spark():
    """A four-point twinkle with a white heart that collapses to a point frame by frame."""
    return [_px(r) for r in (
        ["...f....", "...d....", "..fLf...", "fdLWWLdf", ".fdWWLf.", "...Lf...", "...d....", "...f...."],
        ["........", "...f....", "...d....", ".fdWWdf.", "..dWWd..", "...d....", "...f....", "........"],
        ["........", "........", "...f....", "..fWLf..", "..fLWf..", "....f...", "........", "........"],
        ["........", "........", "........", "...dm...", "...mL...", "........", "........", "........"],
    )]


MOTE_GLYPHS = [ENOCHIAN[0], ENOCHIAN[5], INFERNAL[1], INFERNAL[3], WITCH[0], WITCH[3], HUNTER[5], HUNTER[1]]


def magic_glyph():
    """Eight distinct glyphs, two from each school's script (picked at random per particle)."""
    out = []
    for g in MOTE_GLYPHS:
        f = Field(16)
        draw_glyph(f, g, 8, 8, 13.5, 1.15)
        t = f.bake(crisp=True, threshold=0.42)
        # A faint one-pixel halo so the glyph still reads against bright skies.
        halo = Field(16)
        draw_glyph(halo, g, 8, 8, 13.5, 3.2)
        h = halo.bake(crisp=True, threshold=0.4)
        for y in range(16):
            for x in range(16):
                if h.rows[y][x][3] and not t.rows[y][x][3]:
                    t.set(x, y, _grey(160, 70))
        out.append(t)
    return out


def rune_ember():
    """A jagged ember with a hot core, shedding a spark below as it rises and burns down."""
    return [_px(r) for r in (
        ["...f....", "..dLd...", ".dLWLd..", ".mWWWm..", ".dLWLd..", "..dmd...", "...f....", "....f..."],
        ["........", "...f....", "..dLd...", "..LWLf..", "..dLd...", "...d....", "....f...", "........"],
        ["........", "........", "...f....", "..dWd...", "...Ld...", "....f...", "........", "........"],
        ["........", "........", "........", "...m....", "...d....", "........", "........", "........"],
    )]


def witch_mote():
    """A drifting mote that wanes like the moon: full, gibbous, half, crescent."""
    out = []
    for i, cut in enumerate((None, 3.6, 2.4, 1.5)):
        f = Field(8)
        f.disc(4, 4, 2.5, 1.0, 0.92)
        if cut is None:
            f.disc(3.4, 3.4, 0.9, 1.0, 1.0)
        else:
            f.disc(4 + cut, 3.7, 2.6, 1.0, cut=True)
        f.glow(1, 0.55, 0.8)
        out.append(f.bake(min_alpha=0.08))
    return out


def salt_grain():
    """A cubic crystal of salt, then the same grain turned on its corner."""
    return [_px(r) for r in (
        ["........", "........", "...Wm...", "..WLmd..", "..Lmdd..", "...dd...", "........", "........"],
        ["........", "........", "...W....", "..WLm...", "...md...", "........", "........", "........"],
    )]


# --- effect textures --------------------------------------------------------------------------------------------------

def _star(n, k, cx, cy, r, rot=-90):
    pts = [(cx + r * math.cos(math.radians(rot + 360 * i / n)), cy + r * math.sin(math.radians(rot + 360 * i / n)))
           for i in range(n)]
    return [(pts[(i * k) % n], pts[((i + 1) * k) % n]) for i in range(n)]


def cast_circle():
    """The cast circle: a band of Enochian letters between two rims, a heptagram and a small seal at its heart. Radially
    balanced so it can spin; white on alpha with grey secondary lines."""
    S = 128
    c = S / 2
    f = Field(S)
    f.ring(c, c, 61.5, 1.8)
    f.ring(c, c, 58.5, 0.8, 0.7, 0.75)
    f.ring(c, c, 45.5, 1.5)
    # The band of letters: 21 runes, tangential, a dot between each.
    n = 21
    script = ENOCHIAN + WITCH[:2]
    for i in range(n):
        a = 2 * math.pi * i / n
        r = 52.2
        draw_glyph(f, script[(i * 3) % len(script)], c + r * math.cos(a), c + r * math.sin(a), 9.5, 1.15,
                   rot=a + math.pi / 2)
        b = a + math.pi / n
        f.disc(c + 52.2 * math.cos(b), c + 52.2 * math.sin(b), 0.8, 0.8, 0.8)
    # Tick ring just inside the band.
    for i in range(84):
        a = 2 * math.pi * i / 84
        r1 = 43.2 if i % 4 else 41.2
        f.seg(c + 43.6 * math.cos(a), c + 43.6 * math.sin(a), c + r1 * math.cos(a), c + r1 * math.sin(a), 0.9, 0.75, 0.8)
    # Heptagon and heptagram {7/3}, the Sigillum's frame.
    R = 39
    for a_, b_ in _star(7, 1, c, c, R):
        f.seg(*a_, *b_, 1.0, 0.75, 0.8)
    for a_, b_ in _star(7, 3, c, c, R):
        f.seg(*a_, *b_, 1.3)
    # Small ringed nodes at the vertices.
    for i in range(7):
        a = math.radians(-90 + 360 * i / 7)
        x, y = c + R * math.cos(a), c + R * math.sin(a)
        f.disc(x, y, 3.6, 1.0, cut=True)
        f.ring(x, y, 3.0, 1.1)
        f.disc(x, y, 0.9, 1.0)
    # Heart: a ring, a small hexagram, a bright point.
    f.disc(c, c, 13.5, 1.0, cut=True)
    f.ring(c, c, 13.0, 1.3)
    f.ring(c, c, 10.4, 0.7, 0.6, 0.75)
    for a_, b_ in _star(3, 1, c, c, 9.6) + _star(3, 1, c, c, 9.6, rot=90):
        f.seg(*a_, *b_, 0.9, 0.9, 0.9)
    f.disc(c, c, 1.6, 1.0)
    f.glow(2, 0.32)
    return f.bake()


STRIP_CELLS = [ENOCHIAN[0], INFERNAL[0], WITCH[0], HUNTER[1], ENOCHIAN[5], INFERNAL[3], WITCH[2], HUNTER[5]]


def glyph_strip():
    """Eight glyph cells between two rims, 256 x 32; tiles horizontally (the rims and separators wrap)."""
    W, H = 256, 32
    f = Field(W, H)
    f.wrap_x = True
    for y, w, a, v in ((2.5, 1.6, 1.0, 1.0), (29.5, 1.6, 1.0, 1.0), (5.5, 0.7, 0.6, 0.75), (26.5, 0.7, 0.6, 0.75)):
        f.seg(-2, y, W + 2, y, w, a, v)
    cell = W // len(STRIP_CELLS)
    for i, g in enumerate(STRIP_CELLS):
        x = i * cell + cell / 2
        draw_glyph(f, g, x, 16, 17, 1.5)
        sx = i * cell
        f.poly([(sx, 12.5), (sx + 3.5, 16), (sx, 19.5), (sx - 3.5, 16)], 1.0, 0.85, 0.85, closed=True)
        f.disc(sx, 16, 0.7, 0.9)
    f.glow(1, 0.35)
    return f.bake()


def bolt_core():
    """A soft star: a hot white heart, a wide halo and four long rays (plus four short ones)."""
    S = 32
    c = S / 2
    f = Field(S)
    f.radial(c, c, 15.5, lambda d: (1 - d) ** 2.4 * 0.6, 0.78)
    f.radial(c, c, 4.6, lambda d: 1.0 if d < 0.5 else (1 - d) / 0.5, 1.0)
    for ang, length, w in ((0, 15.5, 2.2), (90, 15.5, 2.2), (45, 9, 1.4), (135, 9, 1.4)):
        a = math.radians(ang)
        for sgn in (1, -1):
            steps = 12
            for k in range(steps):
                t0, t1 = k / steps, (k + 1) / steps
                f.seg(c + sgn * math.cos(a) * length * t0, c + sgn * math.sin(a) * length * t0,
                      c + sgn * math.cos(a) * length * t1, c + sgn * math.sin(a) * length * t1,
                      w * (1 - t0 * 0.75), min(1.0, 1.15 * (1 - t0) ** 1.2), 1.0)
    return f.bake(min_alpha=0.02)


# --- school icons and mastery badges (full colour, 16 x 16) ----------------------------------------------------------

def _stamp(t, draw, color, threshold=0.45, size=16):
    """Draws a Field with `draw(f)` and paints its covered pixels onto t; `color` is a colour or a function of (x, y)."""
    f = Field(size)
    draw(f)
    for y in range(size):
        for x in range(size):
            if f.a[y][x] >= threshold:
                t.set(x, y, color(x, y) if callable(color) else color)


def _lit(ramp, lo=1, hi=5):
    """A ramp lit from the top-left: brighter toward (0, 0)."""
    def c(x, y):
        k = 1 - (x + y) / 30.0
        return ramp[int(round(lo + (hi - lo) * max(0.0, min(1.0, k))))]
    return c


ENOCH_RAMP = Ramp("#4b3108", "#8a6418", "#c99a2e", "#f4d67a", "#fbe9ae", "#ffffff")
INFERNAL_RAMP = Ramp("#2e0602", "#6e1204", "#b8250c", "#e8442a", "#ff8c2a", "#ffd25a")
WITCH_RAMP = Ramp("#24091f", "#5a2752", "#86306f", "#b23a8e", "#d86cb6", "#f4b6e2")
HUNTER_RAMP = Ramp("#1f2a38", "#3c5677", "#5a7ea6", "#8ea3b8", "#b7c3cc", "#eef2f5")


def school_icon(school):
    t = Tex(16, 16)
    if school == "enochian":
        # A halo crowned with rays round a white star.
        def rays(f):
            for i in range(8):
                a = math.radians(i * 45 - 90)
                r1 = 7.6 if i % 2 == 0 else 6.6
                f.seg(8 + 5.2 * math.cos(a), 8 + 5.2 * math.sin(a), 8 + r1 * math.cos(a), 8 + r1 * math.sin(a),
                      1.1 if i % 2 == 0 else 1.6)
        _stamp(t, rays, ENOCH_RAMP[2])
        _stamp(t, lambda f: f.ring(8, 8, 4.3, 1.5), _lit(ENOCH_RAMP, 2, 4))
        _stamp(t, lambda f: (f.seg(8, 5.2, 8, 10.8, 1.0), f.seg(5.2, 8, 10.8, 8, 1.0)), ENOCH_RAMP[5])
        _stamp(t, lambda f: f.disc(8, 8, 1.0), ENOCH_RAMP[4])
    elif school == "infernal":
        # Hellfire: a forked flame round a white-hot heart.
        flame = [(7.5, 0.5), (9.6, 4.6), (11.6, 2.4), (12.4, 6.6), (14, 5.4), (14, 10.6), (11.8, 14.6), (8, 15.4),
                 (4.2, 14.6), (2, 10.6), (2.2, 6.2), (3.8, 7.6), (4.6, 3.4), (6.4, 5.4)]
        inner = [(8.2, 5.5), (10, 9), (10.8, 8), (11, 11.4), (9.6, 13.6), (6.4, 13.6), (5, 11.4), (5.4, 8.6), (6.6, 9.6)]

        def fire(x, y):
            return INFERNAL_RAMP[max(1, min(4, int(round(4.6 - (y / 15.0) * 3.4))))]
        _stamp(t, lambda f: f.fill(flame), fire)
        _stamp(t, lambda f: f.fill(inner), lambda x, y: INFERNAL_RAMP[5] if y < 12 else INFERNAL_RAMP[4])
        t.set(8, 11, hexc("#ffffff"))
        t.set(8, 12, hexc("#fff4c8"))
    elif school == "witchcraft":
        # A waning moon cradling a star.
        def moon(f):
            f.disc(7.4, 8, 6.3)
            f.disc(10.2, 6.8, 5.2, cut=True)
        _stamp(t, moon, _lit(WITCH_RAMP, 1, 5), 0.5)
        star = WITCH_RAMP[5]
        for (x, y) in ((11, 9), (10, 10), (11, 10), (12, 10), (11, 11), (11, 8), (11, 12), (9, 10), (13, 10)):
            t.set(x, y, star if (x, y) != (11, 10) else hexc("#ffffff"))
        t.set(13, 4, WITCH_RAMP[4])
        t.set(9, 3, WITCH_RAMP[3])
    elif school == "hunter":
        # A devil's trap: an iron ring round a filled silver pentagram, a salt-white heart.
        _stamp(t, lambda f: f.ring(8, 8, 6.6, 1.6), _lit(HUNTER_RAMP, 1, 3))
        pts = []
        for i in range(10):
            a = math.radians(-90 + 36 * i)
            r = 5.6 if i % 2 == 0 else 2.3
            pts.append((8 + r * math.cos(a), 8.2 + r * math.sin(a)))
        _stamp(t, lambda f: f.fill(pts), _lit(HUNTER_RAMP, 3, 5), 0.5)
        t.set(7, 8, HUNTER_RAMP[5])
        t.set(8, 8, HUNTER_RAMP[5])
    item_outline(t, (18, 12, 10, 230))
    return t


BADGE_METALS = {
    "apprentice": Ramp("#3a1d0c", "#6b3a1a", "#9a5a2c", "#c47f45", "#e2a76c", "#f6d2a6"),
    "adept": Ramp("#2a2f38", "#555e6c", "#848e9c", "#b5bdc7", "#d9dfe6", "#ffffff"),
    "master": Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8"),
}


def mastery_badge(rank):
    """A medallion on a ribbon whose metal and stars mark the rank: one (copper), two (silver, laurel), three (gold,
    laurel, crown)."""
    m = BADGE_METALS[rank]
    n = {"apprentice": 1, "adept": 2, "master": 3}[rank]
    t = Tex(16, 16)
    ribbon = Ramp("#2a0a10", "#5c1420", "#8a2232", "#b13a48")
    for x, y in ((4, 12), (5, 12), (5, 13), (4, 14), (5, 14), (4, 15), (11, 12), (10, 12), (10, 13), (11, 14),
                 (10, 14), (11, 15)):
        t.set(x, y, ribbon[2] if x in (5, 10) else ribbon[1])
    if n >= 2:
        # Two laurel sprays curling up either side.
        _stamp(t, lambda f: (f.arc(8, 8, 6.9, 118, 228, 1.6), f.arc(8, 8, 6.9, -48, 62, 1.6)),
               lambda x, y: hexc("#8cbc52") if (x + y) % 2 == 0 else hexc("#4f7d32"), 0.5)
    _stamp(t, lambda f: f.disc(8, 8, 5.3), _lit(m, 1, 3))
    _stamp(t, lambda f: f.ring(8, 8, 4.8, 1.1), _lit(m, 2, 5))
    spots = {1: [(8, 8)], 2: [(6, 8), (10, 8)], 3: [(5, 9), (8, 7), (11, 9)]}[n]
    for x, y in spots:
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            t.set(x + dx, y + dy, m[4])
        t.set(x, y, m[5])
    if n == 3:
        for x, y, c in ((5, 0, m[4]), (8, 0, m[5]), (11, 0, m[4]), (5, 1, m[3]), (6, 1, m[3]), (7, 1, m[4]), (8, 1, m[4]),
                        (9, 1, m[4]), (10, 1, m[3]), (11, 1, m[3]), (6, 2, m[2]), (7, 2, m[3]), (8, 2, m[3]), (9, 2, m[3]),
                        (10, 2, m[2])):
            t.set(x, y, c)
    item_outline(t, (18, 12, 10, 230))
    return t


# --- reagent items (16 x 16, light from the top-left, outlined like items.py) --------------------------------------------

def _item(rows, pal):
    t = art(rows, pal)
    item_outline(t, P.OUTLINE)
    return t


HAIR = Ramp("#2a170c", "#4a2c17", "#7a4a26", "#a8703c", "#d29c5e")


def taglock():
    """A lock of hair tied off with red thread, the loose ends trailing."""
    pal = {"H": HAIR[4], "h": HAIR[3], "m": HAIR[2], "d": HAIR[1], "R": hexc("#e0473a"), "r": hexc("#9c2320"),
           "t": hexc("#c4352f")}
    return _item([
        "................",
        "..........Hh....",
        ".........Hhm.h..",
        "........hHmhHm..",
        "........HmhHm...",
        ".......RrRrR....",
        "......rRrRrr.r..",
        ".....Hm.hH.m..r.",
        "....Hm..Hm..h...",
        "....Hm...hH.m...",
        ".....hm..Hm..h..",
        "......Hm.Hm..m..",
        ".....Hm.hm..m...",
        "....Hm..m...d...",
        "...hm...........",
        "................",
    ], pal)


def chrism():
    """Holy oil in a small gold-capped ampulla, a cross on its belly."""
    pal = {"Y": P.GOLD[5], "G": P.GOLD[4], "g": P.GOLD[2], "a": P.GLASS[3], "A": P.GLASS[5],
           "O": hexc("#f6d36a"), "o": hexc("#d9a43a"), "q": hexc("#a8721f"), "x": hexc("#fff6cf")}
    return _item([
        "................",
        ".......Y........",
        "......YGg.......",
        "......GGg.......",
        ".......a........",
        "......GYg.......",
        ".....aAAAa......",
        "....aAOOOOa.....",
        "...aAOOxOOoa....",
        "...axOxxxOoa....",
        "...aOOOxOooa....",
        "...aoOOxOoqa....",
        "....aooooqa.....",
        ".....gGGGg......",
        "................",
        "................",
    ], pal)


def hexed_ink():
    """An inkwell of hexed ink with a quill in it, a magenta curl rising off it (the school's tint replaces the magenta)."""
    pal = {"k": hexc("#2a1626"), "K": hexc("#4a2a44"), "c": hexc("#6a3e62"), "F": hexc("#f4f0e8"), "f": hexc("#bdb5a8"),
           "q": hexc("#e8dcc0"), "M": WITCH_RAMP[4], "m": WITCH_RAMP[3],
           "n": WITCH_RAMP[2], "s": WITCH_RAMP[5], "S": WITCH_RAMP[4], "A": P.GLASS[5], "a": P.GLASS[3]}
    return _item([
        "................",
        "...........F....",
        "..........Ff....",
        ".........Ff.s...",
        "........Ff.S.s..",
        ".......Ff...S...",
        ".....kcqcck.....",
        ".....kMMMMk.....",
        "...akKKKKKKka...",
        "..aAnMMmmmmnka..",
        "..aMMsmmmmnnka..",
        "..aMMmmmmnnnka..",
        "..aammnnnnnkka..",
        "...kkkkkkkkkk...",
        "................",
        "................",
    ], pal)


def bound_blood():
    """A vial of blood bound in twine, a knotted charm hanging from it."""
    pal = {"C": P.WOOD[5], "c": P.WOOD[3], "A": P.GLASS[5], "a": P.GLASS[3], "R": P.BLOOD[5], "r": P.BLOOD[4],
           "m": P.BLOOD[3], "D": P.BLOOD[1], "T": hexc("#d8c08a"), "t": hexc("#9c814e"), "k": hexc("#c4352f"),
           "K": hexc("#7e1218"), "w": hexc("#ffffff")}
    return _item([
        "................",
        "......CCc.......",
        "......Ccc.......",
        ".....AaaaAT.....",
        "......Awa..T....",
        "......Aaa...T...",
        "......Aaa...T...",
        ".....TTttT..t...",
        "......RRrm.KkK..",
        "......Rrrm.kKk..",
        ".....TTttT..K...",
        "......rrmD......",
        "......rmDD......",
        ".......DD.......",
        "................",
        "................",
    ], pal)


def generate():
    for name, frames in (("magic_spark", magic_spark()), ("magic_glyph", magic_glyph()), ("rune_ember", rune_ember()),
                         ("witch_mote", witch_mote()), ("salt_grain", salt_grain())):
        for i, t in enumerate(frames):
            save(t, "particle", f"{name}_{i}")
    save(cast_circle(), "effect", "cast_circle")
    save(glyph_strip(), "effect", "glyph_strip")
    save(bolt_core(), "effect", "bolt_core")
    for name, fn in (("taglock", taglock), ("chrism", chrism), ("hexed_ink", hexed_ink), ("bound_blood", bound_blood)):
        save(fn(), "item", name)
    for school in RUNES:
        save(school_icon(school), "gui/school", school)
    for rank in ("apprentice", "adept", "master"):
        save(mastery_badge(rank), "gui/mastery", rank)
    for spell in seal_ids():
        save(magic_seals.seal(spell), "effect/seal", spell)


def seal_ids():
    """MagicAssets.SEALS, read from the Java contract so a new seal there fails here instead of in a test."""
    import os
    import re
    from common import ROOT
    src = open(os.path.join(ROOT, "src/main/java/org/papiricoh/supernaturalcraft/magic/MagicAssets.java")).read()
    body = re.search(r"List<String> SEALS = List\.of\((.*?)\);", src, re.S).group(1)
    ids = re.findall(r'"([a-z_]+)"', re.sub(r"//[^\n]*", "", body))
    missing = [i for i in ids if i not in magic_seals.SEAL_ART]
    if missing:
        raise SystemExit(f"magic_seals.SEAL_ART lacks seals for {missing}")
    return ids
