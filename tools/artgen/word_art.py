"""v0.36 "The Word and the Ward": wall sigils, the buried Enochian crypt, the Word of God tablets. 2D art only.

textures/block/
  banishing_sigil          the angel-banishing sigil of the series painted in fresh blood: a hand-drawn circle, the rod
                           and the square of Enochian marks inside, runs of blood dripping from the strokes (wall decal).
  enochian_ward            a white chalk-paint warding ring: two rings, glyph ticks between them, a triangle and eye-dot in
                           the middle (decal).
  alarm_sigil(_lit)        pale ochre paint: an eye in a ring with three rays; _lit the same burning bright, a soft halo.
  holy_oil_trail_dot/_side unlit holy oil on the ground (same shapes as salt_line_dot/_side): dark glossy amber, a glint.
  engraved_devils_trap_0..8 the devil's trap of devils_trap_0..8, chiselled into stone: dark grooves lit on their lower
                           edge, a faint rust-red inlay (decal, same 3x3 split: part = row * 3 + column).
  enochian_stone(_bricks), chiseled_enochian_stone
                           pale weathered limestone, faint gold veins; dressed square ashlar; a carved Word glyph inlaid
                           with gold. (The crypt's columns reuse the Cage's enochian_pillar, drawn in cage_art.py.)
  sigil_plate_<s>(_solved) a framed crypt-stone plate with the sigil's faint scratched outline; _solved painted in and
                           glowing gold.
  sealed_crypt_door        a heavy stone slab, a band of gold-inlaid glyphs across it and an Enochian seal on it.
  word_mural_0..3          crypt-stone panels inscribed with four Word glyphs (the atlas's shapes), one pair gilded.
  tablet_pedestal_side/_top/_top_full   a stone pedestal; _top_full with the black demon tablet lying on it.
textures/item/  banishing_sigil, enochian_ward, alarm_sigil, engraved_devils_trap, demon_tablet, hellhound_blood,
                purgatory_jar, purgatory_jar_full, innocent_soul.
textures/gui/   tablet_screen (256x256), word_glyphs (208x32: 26 glyphs, 13 x 2), interrogation_bar (128x32).

The 26 Word glyphs are defined once (GLYPHS, on a 5x5 lattice) and drawn at 16 px for the atlas and at 5 px in the murals.
"""

import math
import random

import blocks
import palette as P
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix

CLEAR = (0, 0, 0, 0)
O = P.OUTLINE
LIME = Ramp("#6f685a", "#958d7c", "#b3ab98", "#c9c1ad", "#dbd4c1", "#ece6d6")
GOLD = P.GOLD
BLOOD = Ramp("#2a0306", "#4f070c", "#760d13", "#9c161b", "#c22424", "#e04a3c")
CHALK = Ramp("#9b968c", "#c2bdb2", "#dad6cc", "#ebe8e0", "#f6f4ee", "#ffffff")
OCHRE = Ramp("#4f3510", "#7f5a1d", "#a97a2c", "#cb9b44", "#e4bf6c", "#f8e2a2")
OIL = Ramp("#140b03", "#2c1906", "#46290b", "#634010", "#86591a", "#c99a45")
BASALT = Ramp("#0c0b0d", "#17151a", "#221f26", "#2e2a33", "#3b3641", "#4b4552")
EMBER = Ramp("#3a0505", "#6e0b0b", "#a51612", "#d8321c", "#ff6a3a", "#ffc29a")
SOUL = Ramp("#2a2c2e", "#45484b", "#676b6e", "#8e9295", "#b9bcbe", "#e6e8e9")


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


def _h(x, y, seed):
    return random.Random((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)).random()


# =====================================================================================================================
# The Word of God: 26 glyphs on a 5x5 lattice (x right, y down, 0..4). ("l", x0, y0, x1, y1) a line between lattice
# points (straight or 45 degrees), ("p", x, y) a dot. Index i is the cipher letter chr(65 + i).
# =====================================================================================================================

GLYPHS = [
    [("l", 2, 0, 2, 4), ("l", 0, 0, 2, 0), ("l", 0, 0, 0, 2), ("l", 3, 4, 4, 4)],                      # 0  A
    [("l", 0, 0, 0, 4), ("l", 0, 4, 4, 4), ("l", 4, 4, 4, 2), ("p", 2, 2)],                             # 1  B
    [("l", 0, 0, 4, 0), ("l", 4, 0, 0, 4), ("l", 0, 4, 2, 4)],                                          # 2  C
    [("l", 0, 0, 4, 0), ("l", 4, 0, 4, 4), ("l", 1, 2, 3, 2)],                                          # 3  D
    [("l", 0, 0, 0, 4), ("l", 0, 0, 3, 0), ("l", 0, 2, 2, 2), ("l", 4, 1, 4, 3)],                      # 4  E
    [("l", 2, 0, 2, 2), ("l", 0, 2, 4, 2), ("l", 0, 2, 0, 4), ("l", 4, 2, 4, 4)],                      # 5  F
    [("l", 0, 0, 0, 4), ("l", 0, 0, 4, 4), ("l", 4, 0, 4, 2)],                                          # 6  G
    [("l", 0, 1, 4, 1), ("l", 1, 1, 1, 4), ("l", 3, 1, 3, 4), ("p", 2, 0)],                            # 7  H
    [("l", 1, 0, 3, 0), ("l", 2, 0, 2, 4), ("l", 1, 4, 3, 4), ("p", 4, 2)],                            # 8  I
    [("l", 0, 0, 0, 4), ("l", 0, 4, 4, 4), ("l", 0, 0, 4, 4)],                                          # 9  J
    [("l", 0, 0, 4, 0), ("l", 1, 0, 1, 4), ("l", 3, 0, 3, 2), ("l", 3, 2, 4, 3)],                      # 10 K
    [("l", 0, 0, 4, 0), ("l", 2, 0, 2, 2), ("l", 2, 2, 0, 4), ("l", 2, 2, 4, 4)],                      # 11 L
    [("l", 1, 0, 3, 0), ("l", 3, 0, 3, 2), ("l", 3, 2, 1, 2), ("l", 1, 2, 1, 0), ("l", 2, 2, 2, 4)],   # 12 M
    [("l", 4, 0, 4, 4), ("l", 4, 2, 1, 2), ("l", 1, 2, 1, 0), ("l", 1, 0, 4, 0), ("p", 0, 4)],         # 13 N
    [("l", 0, 2, 4, 2), ("l", 2, 0, 2, 4), ("l", 0, 0, 1, 1), ("p", 4, 0)],                            # 14 O
    [("l", 4, 0, 4, 4), ("l", 4, 2, 2, 0), ("l", 4, 2, 2, 4), ("p", 0, 2)],                            # 15 P
    [("l", 0, 0, 2, 2), ("l", 2, 2, 0, 4), ("l", 2, 2, 4, 2), ("l", 4, 2, 4, 4)],                      # 16 Q
    [("l", 0, 0, 4, 0), ("l", 2, 0, 2, 4), ("l", 2, 4, 4, 4), ("p", 0, 2)],                            # 17 R
    [("l", 0, 0, 2, 2), ("l", 4, 0, 2, 2), ("l", 2, 2, 2, 4), ("l", 1, 4, 3, 4)],                      # 18 S
    [("l", 0, 0, 0, 4), ("l", 4, 0, 4, 4), ("l", 0, 0, 4, 4)],                                          # 19 T
    [("l", 2, 0, 4, 2), ("l", 4, 2, 2, 4), ("l", 2, 4, 0, 2), ("l", 0, 2, 2, 0)],                      # 20 U
    [("l", 0, 2, 2, 0), ("l", 2, 0, 4, 2), ("l", 0, 4, 4, 4)],                                          # 21 V
    [("l", 0, 2, 4, 2), ("l", 2, 0, 4, 2), ("l", 2, 4, 4, 2)],                                          # 22 W
    [("l", 0, 0, 4, 0), ("l", 0, 0, 4, 4), ("l", 4, 0, 0, 4), ("l", 0, 4, 4, 4)],                      # 23 X
    [("l", 0, 0, 0, 2), ("l", 0, 2, 4, 2), ("l", 4, 2, 4, 4), ("p", 2, 0), ("p", 2, 4)],               # 24 Y
    [("l", 0, 4, 4, 4), ("l", 0, 2, 0, 4), ("l", 2, 1, 2, 4), ("l", 4, 0, 4, 4)],                      # 25 Z
]
# T and J share a shape by accident of the lattice: T is the mirror (the diagonal runs the other way).
GLYPHS[19] = [("l", 0, 0, 0, 4), ("l", 4, 0, 4, 4), ("l", 4, 0, 0, 4)]


def glyph_cells(index):
    """The lattice points (0..4, 0..4) a glyph covers."""
    cells = set()
    for s in GLYPHS[index]:
        if s[0] == "p":
            cells.add((s[1], s[2]))
            continue
        _, x0, y0, x1, y1 = s
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            cells.add((x0 + (x1 - x0) * i // n, y0 + (y1 - y0) * i // n))
    return cells


def glyph16(index, ink, shadow):
    """A Word glyph at 16 px: lattice point g at 2 + 3g, 1 px strokes, a 1 px shadow down-right."""
    f = Tex(16, 16)
    pts = set()
    for s in GLYPHS[index]:
        if s[0] == "p":
            gx, gy = 2 + 3 * s[1], 2 + 3 * s[2]
            for dx in (0, 1):
                for dy in (0, 1):
                    pts.add((gx + dx - (1 if s[1] == 4 else 0), gy + dy - (1 if s[2] == 4 else 0)))
            continue
        _, x0, y0, x1, y1 = s
        ax, ay, bx, by = 2 + 3 * x0, 2 + 3 * y0, 2 + 3 * x1, 2 + 3 * y1
        n = max(abs(bx - ax), abs(by - ay))
        for i in range(n + 1):
            pts.add((ax + (bx - ax) * i // n, ay + (by - ay) * i // n))
    for (x, y) in pts:
        if (x + 1, y + 1) not in pts:
            f.set(x + 1, y + 1, shadow)
    for (x, y) in pts:
        f.set(x, y, ink)
    return f


# =====================================================================================================================
# Stone
# =====================================================================================================================

def _veins(seed, count, length):
    rng = random.Random(seed)
    out = {}
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        dx = rng.choice((-1, 1))
        for i in range(length):
            out[(x % 16, y % 16)] = 1.0 - i / (length * 1.5)
            if rng.random() < 0.55:
                x += dx
            else:
                y += 1
            if rng.random() < 0.15:
                dx = -dx
    return out


def lime(x, y, seed, base=3.0, amp=1.8):
    """Weathered limestone: soft mottling, fine pits."""
    n = 0.65 * fbm(x % 16, y % 16, seed, 16, 16, 3, 4.0) + 0.35 * fbm((x + 7) % 16, (y + 3) % 16, seed + 1, 16, 16, 2, 8.0)
    v = base + (n - 0.5) * amp * 2
    if _h(x % 16, y % 16, seed + 2) < 0.06:
        v -= 1.2
    elif _h(x % 16, y % 16, seed + 3) < 0.05:
        v += 0.8
    return tone(LIME, v)


def enochian_stone():
    t = Tex(16, 16, 3601)
    veins = _veins(3602, 2, 10)
    for y in range(16):
        for x in range(16):
            c = lime(x, y, 3600)
            if (x, y) in veins:
                c = mix(c, GOLD[3], 0.22 + 0.25 * veins[(x, y)])
            t.set(x, y, c)
    return t


def _ashlar(seed, gold_seams=True):
    """Square dressed stones 8x8, every other course offset by 4: worn arrises, a dark joint, gold in some joints."""
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            course = y // 8
            bx = (x + course * 4) % 16
            lx, ly = bx % 8, y % 8
            stone = (bx // 8) + course * 2
            c = lime(x, y, seed + stone * 7, base=3.1 + (_h(stone, 0, seed) - 0.5) * 0.8, amp=1.3)
            if ly == 7 or lx == 7:
                c = LIME[1] if not (gold_seams and (stone + ly) % 5 == 0 and _h(x, y, seed + 9) < 0.6) else GOLD[2]
            elif ly == 0 or lx == 0:
                c = mix(c, LIME[5], 0.45)
            elif ly == 6 or lx == 6:
                c = mix(c, LIME[1], 0.25)
            t.set(x, y, c)
    return t


def enochian_stone_bricks():
    t = _ashlar(3610)
    # A tiny worn glyph on one stone.
    for (x, y) in glyph_cells(9):
        t.set(9 + x, 1 + y, mix(t.get(9 + x, 1 + y), LIME[0], 0.45))
    return t


def chiseled_enochian_stone():
    """A framed stone with a Word glyph cut deep and inlaid with gold."""
    t = Tex(16, 16, 3620)
    for y in range(16):
        for x in range(16):
            c = lime(x, y, 3620, base=3.2, amp=1.1)
            e = min(x, y, 15 - x, 15 - y)
            if e == 0:
                c = LIME[1]
            elif e == 1:
                c = mix(c, LIME[5], 0.4) if (x < 8 and y < 8) or x < 2 or y < 2 else mix(c, LIME[1], 0.45)
            t.set(x, y, c)
    g = glyph16(16, GOLD[3], LIME[0])
    for y in range(16):
        for x in range(16):
            c = g.get(x, y)
            if c[3]:
                if c == GOLD[3]:
                    t.set(x, y, GOLD[3] if (x + y) % 3 else GOLD[4])
                elif 2 <= x <= 13 and 2 <= y <= 13:
                    t.set(x, y, mix(t.get(x, y), LIME[0], 0.6))
    return t


# =====================================================================================================================
# The three wall sigils (shared by the decals, the plates and the item icons)
# =====================================================================================================================

def _bres(pts, x0, y0, x1, y1):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.add((x0, y0))
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def _ring_px(pts, c, r, w=0.5):
    for y in range(2 * c + 1):
        for x in range(2 * c + 1):
            if abs(math.hypot(x - c, y - c) - r) < w:
                pts.add((x, y))


def sigil_pixels(name, n=13):
    """The sigil as crisp pixels on an n x n grid (n odd, centre n // 2): the shape the decals, plates and icons share."""
    c = n // 2
    pts = set()
    if name == "banishing":
        # The circle, the rod through it, a diamond on the rod, two bars and two marks.
        _ring_px(pts, c, c - 0.1)
        _bres(pts, c, 1, c, n - 2)
        d = max(2, c // 2)
        for (a, b) in (((c, c - d), (c + d, c)), ((c + d, c), (c, c + d)), ((c, c + d), (c - d, c)), ((c - d, c), (c, c - d))):
            _bres(pts, *a, *b)
        if n >= 13:
            _bres(pts, c - d - 1, c - d, c - d, c - d)
            _bres(pts, c + d, c + d, c + d + 1, c + d)
        pts.add((c + d, c - d))
        pts.add((c - d, c + d))
    elif name == "ward":
        _ring_px(pts, c, c - 0.1)
        _ring_px(pts, c, c - 2.3, 0.45)
        for i in range(8):
            a = math.radians(i * 45 + 22.5)
            pts.add((int(round(c + (c - 1.2) * math.cos(a))), int(round(c + (c - 1.2) * math.sin(a)))))
        if n >= 13:
            _bres(pts, c, c - 3, c + 3, c + 1)
            _bres(pts, c + 3, c + 1, c - 3, c + 1)
            _bres(pts, c - 3, c + 1, c, c - 3)
        pts.add((c, c))
    elif name == "alarm":
        _ring_px(pts, c, c - 0.1)
        e = c - 2
        for (a, b) in (((c - e, c), (c - e + 2, c - 2)), ((c - e + 2, c - 2), (c + e - 2, c - 2)), ((c + e - 2, c - 2), (c + e, c)),
                       ((c - e, c), (c - e + 2, c + 2)), ((c - e + 2, c + 2), (c + e - 2, c + 2)), ((c + e - 2, c + 2), (c + e, c))):
            _bres(pts, *a, *b)
        for (x, y) in ((c, c), (c - 1, c), (c + 1, c), (c, c - 1), (c, c + 1)):
            pts.add((x, y))
        for dx in (-2, 0, 2):
            pts.add((c + dx, c - 4))
    return pts


def _paint(pts, ramp, seed, base, ox=0, oy=0, size=16):
    t = Tex(size, size, seed)
    for (x, y) in pts:
        n = fbm(x, y, seed, 16, 16, 2, 6.0)
        v = base + (n - 0.5) * 2.0
        if _h(x, y, seed + 1) < 0.08:
            v -= 1.0
        t.set(ox + x, oy + y, tone(ramp, v))
    return t


def _thicken(pts, n, seed, share):
    """Paint is laid with a brush: thicken some strokes by a pixel down or right."""
    out = set(pts)
    for (x, y) in pts:
        if _h(x, y, seed) < share:
            out.add((x + 1, y) if _h(y, x, seed + 1) < 0.5 else (x, y + 1))
    return {(x, y) for (x, y) in out if 0 <= x < n and 0 <= y < n}


def banishing_sigil():
    pts = _thicken(sigil_pixels("banishing", 15), 15, 3650, 0.12)
    t = _paint(pts, BLOOD, 3651, 3.2, 0, 1)
    # Runs of blood from the lowest points of the strokes.
    rng = random.Random(3652)
    rows = t.copy_rows()
    for x in range(16):
        for y in range(15, -1, -1):
            if rows[y][x][3] and y < 15 and not rows[y + 1][x][3]:
                if rng.random() < 0.2:
                    n = rng.randint(1, 3)
                    for k in range(1, n + 1):
                        if y + k < 16 and not rows[y + k][x][3]:
                            t.set(x, y + k, BLOOD[2] if k < n else BLOOD[1])
                break
    for (x, y) in ((7, 2), (2, 6), (12, 9), (7, 13)):
        if t.get(x, y)[3]:
            t.set(x, y, BLOOD[5])
    return t


def enochian_ward():
    pts = sigil_pixels("ward", 15)
    t = _paint(pts, CHALK, 3661, 3.8, 0, 1)
    rng = random.Random(3662)
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        if not t.get(x, y)[3] and any(t.get(x + dx, y + dy)[3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            t.set(x, y, CHALK[3][:3] + (100,))
    return t


def alarm_sigil(lit=False):
    pts = sigil_pixels("alarm", 15)
    if not lit:
        return _paint(pts, OCHRE, 3672, 2.9, 0, 1)
    t = Tex(16, 16)
    for (x, y) in pts:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in pts:
                    t.set(x + dx, y + 1 + dy, OCHRE[5][:3] + (90,))
    for (x, y) in pts:
        t.set(x, y + 1, tone(OCHRE, 4.6 + (_h(x, y, 3671) - 0.5)))
    for (x, y) in ((7, 8), (6, 8), (8, 8), (7, 7), (7, 9)):
        t.set(x, y, hexc("#fff4cc"))
    t.set(7, 8, hexc("#ffffff"))
    return t


# =====================================================================================================================
# Holy oil, engraved trap
# =====================================================================================================================

def _glossy(t, seed):
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            c = t.get(x, y)
            if c[3] and rng.random() < 0.07 and t.get(x, y - 1)[3]:
                t.set(x, y, OIL[5])
    return t


def holy_oil_dot():
    return _glossy(blocks.line_dot(OIL, 3680), 3681)


def holy_oil_side():
    return _glossy(blocks.line_side(OIL, 3682), 3683)


def engraved_trap_parts():
    full = blocks.devils_trap_full()
    S = full.w
    on = [[full.rows[y][x][3] > 0 for x in range(S)] for y in range(S)]
    t = Tex(S, S, 3690)
    groove = Ramp("#2a2622", "#3a342e", "#4a433b", "#5a5248", "#6b6255", "#7d7363")
    for y in range(S):
        for x in range(S):
            if on[y][x]:
                up = y > 0 and not on[y - 1][x]
                left = x > 0 and not on[y][x - 1]
                down = y < S - 1 and not on[y + 1][x]
                right = x < S - 1 and not on[y][x + 1]
                if up or left:
                    c = groove[0]
                elif down or right:
                    c = groove[3]
                else:
                    c = groove[1]
                if not (up or left) and _h(x, y, 3691) < 0.55:
                    c = mix(c, hexc("#7a2a1a"), 0.55)
                t.set(x, y, c)
            else:
                # The rim: the cut's lower-right lip catches the light.
                if (x > 0 and on[y][x - 1]) or (y > 0 and on[y - 1][x]):
                    t.set(x, y, (236, 230, 214, 96))
                elif (x < S - 1 and on[y][x + 1]) or (y < S - 1 and on[y + 1][x]):
                    t.set(x, y, (20, 16, 12, 70))
    parts = []
    for part in range(9):
        ox, oy = (part % 3) * 16, (part // 3) * 16
        p = Tex(16, 16)
        for y in range(16):
            for x in range(16):
                p.set(x, y, t.rows[oy + y][ox + x])
        parts.append(p)
    return t, parts


# =====================================================================================================================
# Plates, door, murals, pedestal
# =====================================================================================================================

def _plate_base(seed):
    """A framed crypt-stone plate: a raised bevelled border two pixels wide, a smooth field inside, gold studs."""
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            e = min(x, y, 15 - x, 15 - y)
            if e == 0:
                c = LIME[1]
            elif e == 1:
                lit = (x == 1 and y < 15) or (y == 1 and x < 15)
                c = mix(lime(x, y, seed, 3.2, 0.5), LIME[5] if lit else LIME[1], 0.45)
            else:
                c = lime(x, y, seed, base=3.5, amp=0.35)
                if x == 2 or y == 2:
                    c = mix(c, LIME[1], 0.35)
            t.set(x, y, c)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        t.set(x, y, GOLD[3])
    return t


def sigil_plate(name, solved):
    t = _plate_base(3700 + len(name))
    pts = sigil_pixels(name, 11)
    ox, oy = 3, 3
    for (x, y) in pts:
        X, Y = ox + x, oy + y
        if solved:
            t.set(X, Y, GOLD[4] if _h(X, Y, 3710) < 0.7 else GOLD[5] if _h(X, Y, 3714) < 0.6 else GOLD[3])
        else:
            # Scratched faintly into the stone: a dark scratch, a pale nick here and there, some of it worn away.
            if _h(X, Y, 3712) < 0.1:
                continue
            c = t.get(X, Y)
            t.set(X, Y, mix(c, LIME[0], 0.5) if _h(X, Y, 3711) < 0.85 else mix(c, LIME[1], 0.5))
            if (x + 1, y + 1) not in pts and _h(X, Y, 3713) < 0.5:
                t.set(X + 1, Y + 1, mix(t.get(X + 1, Y + 1), LIME[5], 0.45))
    if solved:
        rows = t.copy_rows()
        lit = {(ox + x, oy + y) for (x, y) in pts}
        for y in range(2, 14):
            for x in range(2, 14):
                if (x, y) in lit:
                    continue
                near = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (x + dx, y + dy) in lit)
                if near:
                    t.set(x, y, mix(rows[y][x], GOLD[4], 0.12 + 0.08 * near))
    return t


def sealed_crypt_door():
    """A heavy slab, quarry marks; a band of gold-inlaid glyphs across the middle (tiles sideways), the seal over it."""
    t = Tex(16, 16, 3720)
    for y in range(16):
        for x in range(16):
            c = lime(x, y, 3720, base=2.7, amp=1.2)
            if y in (0, 15):
                c = LIME[1] if y == 15 else mix(c, LIME[4], 0.4)
            if (x * 3 + y * 5) % 11 == 0 and _h(x, y, 3721) < 0.5:
                c = mix(c, LIME[1], 0.4)
            t.set(x, y, c)
    # The band: dark recess with gold glyph ticks.
    for x in range(16):
        t.set(x, 6, LIME[0])
        t.set(x, 10, LIME[4])
        for y in (7, 8, 9):
            t.set(x, y, mix(LIME[1], BASALT[3], 0.4))
    for x0, gi in ((1, 3), (11, 17)):
        for (gx, gy) in glyph_cells(gi):
            if gy in (1, 2, 3):
                t.set(x0 + gx, 6 + gy, GOLD[3])
    # The seal: a gold-rimmed disc with a cross of the Word.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 4.4:
                c = mix(lime(x, y, 3722, 3.4, 0.6), GOLD[2], 0.2)
                if abs(d - 3.9) < 0.5:
                    c = GOLD[2] if x + y < 16 else GOLD[1]
                elif d < 3.3 and (abs(x + 0.5 - 8) < 0.6 or abs(y + 0.5 - 8) < 0.6):
                    c = GOLD[4] if x + y < 16 else GOLD[3]
                elif d < 1.3:
                    c = GOLD[4]
                t.set(x, y, c)
    return t


# Each mural: four glyph indices (top-left, top-right, bottom-left, bottom-right) and which row is gilded (0 top, 1 bottom).
# Gilded pairs over the four variants: W O | R D | O F | G O ("the Word of God").
MURALS = [((22, 14, 4, 11), 0), ((7, 1, 17, 3), 1), ((14, 5, 20, 24), 0), ((12, 2, 6, 14), 1)]


def word_mural(v):
    glyphs, gilded = MURALS[v]
    t = Tex(16, 16, 3730 + v)
    for y in range(16):
        for x in range(16):
            c = lime(x, y, 3730 + v, base=3.0, amp=1.0)
            e = min(x, y, 15 - x, 15 - y)
            if e == 0:
                c = mix(c, LIME[1], 0.6)
            if y in (7, 8) and 1 <= x <= 14:
                c = LIME[1] if y == 7 else mix(c, LIME[5], 0.35)
            t.set(x, y, c)
    for i, gi in enumerate(glyphs):
        ox, oy = (2 if i % 2 == 0 else 9), (1 if i < 2 else 9)
        gold = (i // 2) == gilded
        cells = glyph_cells(gi)
        for (gx, gy) in cells:
            x, y = ox + gx, oy + gy
            if gold:
                t.set(x, y, GOLD[4] if (x + y) % 3 else GOLD[3])
                if (gx + 1, gy + 1) not in cells:
                    t.set(x + 1, y + 1, mix(t.get(x + 1, y + 1), LIME[0], 0.5))
            else:
                t.set(x, y, mix(LIME[0], LIME[1], 0.4))
                if (gx + 1, gy + 1) not in cells and (gx, gy + 1) not in cells:
                    t.set(x, y + 1, mix(t.get(x, y + 1), LIME[5], 0.4))
    return t


def pedestal_side():
    t = Tex(16, 16, 3740)
    for y in range(16):
        for x in range(16):
            # A narrow shaft between a moulded cap and base.
            if y <= 2 or y >= 13:
                c = lime(x, y, 3740, base=3.2, amp=0.8)
                if y in (0, 13):
                    c = mix(c, LIME[5], 0.4)
                if y in (2, 15):
                    c = LIME[1]
                t.set(x, y, c)
            elif 3 <= x <= 12:
                c = lime(x, y, 3741, base=2.9 if 4 <= x <= 11 else 2.1, amp=0.8)
                if x == 4:
                    c = mix(c, LIME[5], 0.35)
                t.set(x, y, c)
    for x in range(3, 13):
        t.set(x, 4, GOLD[2] if x % 3 else GOLD[4])
        t.set(x, 11, GOLD[1])
    for (gx, gy) in glyph_cells(18):
        t.set(6 + gx, 6 + gy, mix(LIME[0], GOLD[1], 0.4))
    return t


def script(t, x0, y0, x1, lines, seed, ink, bright, gap=2, skip=None, bright_share=0.25):
    """Lines of tiny glyph marks (2-3 px wide, 2 px tall, one-pixel spaces): text in a script too small to read."""
    rng = random.Random(seed)
    marks = [((0, 0), (1, 0), (0, 1)), ((0, 0), (0, 1), (1, 1)), ((1, 0), (0, 1), (1, 1)), ((0, 0), (1, 1)), ((0, 0), (0, 1)),
             ((0, 0), (1, 0), (2, 0), (1, 1)), ((0, 1), (1, 0), (2, 1)), ((0, 0), (2, 0), (1, 1))]
    for k in range(lines):
        y = y0 + k * (2 + gap)
        x = x0 + rng.randint(0, 1)
        while x < x1:
            m = rng.choice(marks)
            w = max(dx for dx, _ in m) + 1
            if x + w > x1:
                break
            for (dx, dy) in m:
                if skip is None or not skip(x + dx, y + dy):
                    t.set(x + dx, y + dy, bright if rng.random() < bright_share else ink)
            x += w + 1
    return t


TABLET = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...oTbbbbbbbo...",
    "...oTbbbbbbbbo..",
    "...oTbbbbbbbbo..",
    "...oTbbbbbbbbo..",
    "...oTbbbbbbbbo..",
    "...oTbbbbbbbbo..",
    "...oTbbbbbbbo...",
    "...oTbbbbbboo...",
    "....oooooo......",
    "................",
    "................",
    "................",
]


def pedestal_top(full=False):
    t = Tex(16, 16, 3750)
    for y in range(16):
        for x in range(16):
            e = min(x, y, 15 - x, 15 - y)
            c = lime(x, y, 3750, base=3.3, amp=0.8)
            if e == 0:
                c = LIME[1]
            elif e == 1:
                c = mix(c, LIME[5], 0.3)
            elif e == 2:
                c = mix(c, GOLD[2], 0.35)
            t.set(x, y, c)
    if not full:
        # The worn bed where the tablet lay.
        for y in range(4, 12):
            for x in range(4, 13):
                if TABLET[y][x] in "Tb":
                    t.set(x, y, mix(t.get(x, y), LIME[1], 0.35))
        return t
    # The demon tablet: a broken slab of black stone, lines of red glyphs faintly glowing.
    for y, row in enumerate(TABLET):
        for x, ch in enumerate(row):
            if ch == "o":
                t.set(x, y, BASALT[0])
            elif ch == "T":
                t.set(x, y, BASALT[4])
            elif ch == "b":
                t.set(x, y, tone(BASALT, 2.4 + (fbm(x, y, 3751, 16, 16, 2, 6.0) - 0.5) * 1.6))
    script(t, 5, 5, 12, 3, 3752, EMBER[2], EMBER[4], gap=0, skip=lambda x, y: TABLET[y][x] != "b")
    return t


# =====================================================================================================================
# Items
# =====================================================================================================================

def _pot(t, paint, seed):
    """A small open tin of paint seen a little from above: the paint's surface, a drip down the front, a brush in it."""
    tin = P.STEEL
    for y in range(7, 14):
        for x in range(3, 13):
            k = (x - 3) / 9
            t.set(x, y, tin[4] if k < 0.2 else tin[3] if k < 0.6 else tin[2] if k < 0.9 else tin[1])
    for x in range(3, 13):
        t.set(x, 14, tin[1])
    # The rim and the paint inside it (an ellipse two rows deep).
    for x in range(4, 12):
        t.set(x, 5, tin[4])
    for x in range(3, 13):
        t.set(x, 6, paint[4] if 4 <= x <= 11 else tin[3])
    for x in range(4, 12):
        t.set(x, 5, paint[5] if x < 7 else paint[4])
    t.set(3, 6, tin[4])
    t.set(12, 6, tin[2])
    for (x, y) in ((5, 7), (5, 8), (5, 9), (6, 7)):
        t.set(x, y, paint[3] if y < 9 else paint[2])
    # The brush handle leaning in it.
    for i in range(6):
        t.set(9 + (i + 1) // 2, 5 - i, P.WOOD[4] if i % 2 else P.WOOD[3])
    t.set(9, 5, P.STEEL[4])
    return t


def item_banishing_sigil():
    """A brush loaded with blood over the sigil it painted."""
    t = Tex(16, 16)
    for (x, y) in sigil_pixels("banishing", 11):
        t.set(x, y + 5, BLOOD[3] if _h(x, y, 3761) < 0.7 else BLOOD[4])
    for i in range(6):
        t.set(15 - i, i, P.WOOD[4] if i % 2 else P.WOOD[3])
        t.set(15 - i, i + 1, P.WOOD[1])
    for (x, y, c) in ((9, 6, P.STEEL[4]), (8, 7, P.STEEL[3]), (7, 8, BLOOD[4]), (8, 8, BLOOD[2]), (6, 9, BLOOD[5]), (7, 9, BLOOD[3])):
        t.set(x, y, c)
    item_outline(t, O)
    return t


def item_enochian_ward():
    t = _pot(Tex(16, 16), CHALK, 3762)
    for (gx, gy) in glyph_cells(20):
        t.set(7 + gx, 8 + gy, GOLD[3] if gy < 3 else GOLD[2])
    item_outline(t, O)
    return t


def item_alarm_sigil():
    t = _pot(Tex(16, 16), OCHRE, 3763)
    for (x, y) in ((7, 10), (8, 9), (9, 9), (10, 9), (11, 10), (8, 11), (9, 11), (10, 11)):
        t.set(x, y, OCHRE[4])
    t.set(9, 10, O)
    for (x, y) in ((9, 7), (7, 8), (11, 8)):
        t.set(x, y, OCHRE[3])
    item_outline(t, O)
    return t


def item_engraved_trap():
    """A stone tile with the trap's ring cut in it, a chisel over it."""
    t = Tex(16, 16)
    for y in range(6, 15):
        for x in range(1, 12):
            t.set(x, y, lime(x, y, 3770, base=3.2, amp=0.8) if not (y == 14 or x == 11) else LIME[1])
    groove = mix(LIME[0], hexc("#7a2a1a"), 0.35)
    for y in range(6, 14):
        for x in range(1, 11):
            d = math.hypot(x + 0.5 - 6, y + 0.5 - 10)
            if abs(d - 3.4) < 0.55:
                t.set(x, y, groove)
    for (a, b) in (((6, 8), (5, 11)), ((5, 11), (8, 9)), ((8, 9), (4, 9)), ((4, 9), (7, 11)), ((7, 11), (6, 8))):
        pts = set()
        _bres(pts, *a, *b)
        for p in pts:
            t.set(*p, groove)
    for i in range(4):
        t.set(8 + i, 7 - i, P.STEEL[4] if i % 2 else P.STEEL[3])
    for i in range(4, 9):
        t.set(8 + i, 7 - i, P.WOOD[4] if i % 2 else P.WOOD[3])
        t.set(8 + i - 1, 7 - i, P.WOOD[2])
    t.set(7, 8, P.STEEL[5])
    item_outline(t, O)
    return t


def item_demon_tablet():
    rows = [
        "....oooooooo....",
        "...oTbbbbbbbo...",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbo..",
        "..oTbbbbbbbbbbo.",
        "..oTbbbbbbbbbbo.",
        "..oTbbbbbbbbboo.",
        "..oTbbbbbbbbo...",
        "..oTbbbbbbbo....",
        "...ooooooooo....",
        "................",
    ]
    t = Tex(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                t.set(x, y, O)
            elif ch == "T":
                t.set(x, y, BASALT[4])
            elif ch == "b":
                v = 2.3 + (fbm(x, y, 3780, 16, 16, 2, 6.0) - 0.5) * 1.6 + (0.9 if y == 1 else 0) - (0.7 if x >= 12 else 0)
                t.set(x, y, tone(BASALT, v))
    script(t, 4, 3, 12, 4, 3781, EMBER[2], EMBER[4], gap=1, skip=lambda x, y: rows[y][x] != "b", bright_share=0.15)
    # The broken corner shows a fresh, paler face.
    for (x, y) in ((11, 12), (12, 11), (13, 10)):
        t.set(x, y, BASALT[5])
    return t


def item_hellhound_blood():
    t = Tex(16, 16)
    body = [
        "................",
        "................",
        "................",
        "......oooo......",
        "......occo......",
        "......oCco......",
        ".....ogGago.....",
        "......oGao......",
        ".....oGaaao.....",
        "....oGkkkkko....",
        "...oGkrkkkkko...",
        "...okkkkrkkko...",
        "...okkkkkkkro...",
        "...okrkkkkkko...",
        "....okkkkkko....",
        ".....oooooo.....",
    ]
    pal = {"o": O, "c": P.WOOD[3], "C": P.WOOD[5], "g": P.GLASS[1], "G": P.GLASS[5], "a": P.GLASS[3],
           "k": hexc("#160406"), "r": BLOOD[3]}
    for y, row in enumerate(body):
        for x, ch in enumerate(row):
            if ch in pal:
                t.set(x, y, pal[ch])
    t.set(5, 10, BLOOD[4])
    t.set(5, 11, hexc("#3a0a0c"))
    for (x, y, a) in ((10, 3, 190), (11, 2, 150), (10, 1, 120), (11, 0, 80), (5, 2, 140), (4, 1, 100)):
        t.set(x, y, (60, 52, 56, a))
    return t


def item_purgatory_jar(full=False):
    rows = [
        "................",
        "....oooooooo....",
        "....owwwwwwo....",
        "....oWWWWWWo....",
        "...oggggggggo...",
        "..oHaaaaaaaago..",
        "..oHaaaaaaaago..",
        "..obbbbbbbbbbo..",
        "..oHaaaaaaaago..",
        "..oHaaaaaaaago..",
        "..oaaaaaaaaago..",
        "..obbbbbbbbbbo..",
        "..oaaaaaaaaago..",
        "...oggggggggo...",
        "....oooooooo....",
        "................",
    ]
    glass = hexc("#9fb6c0", 90)
    pal = {"o": O, "w": P.STEEL[4], "W": P.STEEL[2], "g": hexc("#6f8792", 200), "H": hexc("#f2fbff", 210), "a": glass, "b": GOLD[3]}
    t = Tex(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                t.set(x, y, pal[ch])
    for x in range(3, 13):
        if x % 3 == 1:
            t.set(x, 7, GOLD[1])
            t.set(x, 11, GOLD[5])
    t.set(7, 2, BLOOD[4])
    t.set(8, 2, BLOOD[3])
    t.set(7, 3, BLOOD[2])
    if full:
        # A grey soul turning in the glass: a pale wisp spiralling, brighter at its heart.
        for y in range(5, 13):
            for x in range(4, 12):
                if rows[y][x] == "a":
                    t.set(x, y, hexc("#8d969c", 170))
        swirl = {6: "..sSs...", 8: ".s.WWSs.", 9: "s.SWWs.s", 10: ".Ss..sS.", 12: "..sSSs.."}
        for y, row in swirl.items():
            for k, ch in enumerate(row):
                if ch != ".":
                    t.set(4 + k, y, {"s": SOUL[3], "S": SOUL[4], "W": hexc("#ffffff")}[ch])
        for x in (5, 9):
            t.set(x, 7, SOUL[2])
            t.set(x + 1, 11, SOUL[2])
    return t


WISP = Ramp("#1d3c6e", "#2f63a8", "#5a95d8", "#94c4f0", "#cfe7ff", "#ffffff")


def item_innocent_soul():
    """An innocent soul (the Trials): a small wisp of white-blue light, its tail curling, a soft halo round it."""
    rows = [
        "................",
        "........h.......",
        ".......h2h......",
        "......h232h.....",
        ".....h23432h....",
        "....h2345432h...",
        "....h3455543h...",
        "...h245555542h..",
        "...h345555543h..",
        "...h234555432h..",
        "....h2344432h...",
        ".....h23332h....",
        "......h2221h....",
        ".......h11h.....",
        "......h1hh......",
        ".....hh.........",
    ]
    t = Tex(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "h":
                t.set(x, y, WISP[3][:3] + (90,))
            elif ch.isdigit():
                t.set(x, y, WISP[int(ch)])
    # Two motes drifting off it.
    t.set(12, 3, WISP[4][:3] + (200,))
    t.set(3, 4, WISP[3][:3] + (150,))
    return t


# =====================================================================================================================
# GUI
# =====================================================================================================================

def word_glyph_atlas():
    t = Tex(208, 32)
    ink, shadow = hexc("#f4ecd6"), (24, 18, 14, 150)
    for i in range(26):
        g = glyph16(i, ink, shadow)
        t.paste(g, (i % 13) * 16, (i // 13) * 16)
    return t


def tablet_screen():
    """The cipher table: a slab of dark tablet stone in a carved frame. 256x256 texture, the panel at (0, 0) 248x200:
       main field (recessed, plain) x 10..183, y 10..189; key strip (recessed) x 192..237, y 10..189. Sprites under the
       panel at y 204: an empty glyph cell 20x20 at (0, 204), the selected cell at (20, 204), a wrong cell at (40, 204),
       a solved cell at (60, 204)."""
    W, H = 248, 200
    t = Tex(256, 256, 3800)
    stone = Ramp("#15131a", "#1f1c24", "#2a2630", "#36313d", "#443e4c", "#57505f")
    for y in range(H):
        for x in range(W):
            n = fbm(x, y, 3800, 64, 64, 3, 6.0)
            v = 2.6 + (n - 0.5) * 2.2
            e = min(x, y, W - 1 - x, H - 1 - y)
            if e == 0:
                c = stone[0]
            elif e <= 2:
                c = stone[5] if (x <= 2 or y <= 2) else stone[1]
            elif e <= 7:
                c = tone(stone, v + 0.6)
            else:
                c = tone(stone, v)
            if _h(x, y, 3801) < 0.01:
                c = stone[0]
            t.set(x, y, c)
    # A frieze of faint red glyphs round the frame.
    for i in range(0, W - 16, 12):
        gi = (i // 12 * 7) % 26
        for (gx, gy) in glyph_cells(gi):
            for (ox, oy) in ((i + 6, 1), (i + 6, H - 7)):
                if 3 <= ox + gx < W - 3:
                    t.set(ox + gx, oy + gy, EMBER[1] if (gx + gy) % 2 else EMBER[2])

    def recess(x0, y0, x1, y1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                e = min(x - x0, y - y0, x1 - x, y1 - y)
                n = fbm(x, y, 3802, 64, 64, 2, 10.0)
                c = tone(stone, 1.7 + (n - 0.5) * 0.5)
                if e == 0:
                    c = stone[0] if (x == x0 or y == y0) else stone[4]
                elif e == 1 and (x == x0 + 1 or y == y0 + 1):
                    c = stone[0]
                t.set(x, y, c)

    recess(10, 10, 183, 189)
    recess(192, 10, 237, 189)
    # Cracks running in from the edges.
    rng = random.Random(3803)
    for _ in range(6):
        x, y = rng.choice([(rng.randrange(W), 3), (rng.randrange(W), H - 4), (3, rng.randrange(H)), (W - 4, rng.randrange(H))])
        for _ in range(rng.randint(5, 12)):
            if 10 <= x <= 183 and 10 <= y <= 189 or 192 <= x <= 237 and 10 <= y <= 189:
                break
            t.set(x, y, stone[0])
            x += rng.choice((-1, 0, 1))
            y += rng.choice((-1, 0, 1))

    def cell(x0, y0, rim, fill):
        for y in range(20):
            for x in range(20):
                e = min(x, y, 19 - x, 19 - y)
                if e == 0:
                    c = stone[0]
                elif e == 1:
                    c = rim[0] if (x == 1 or y == 1) else rim[1]
                else:
                    c = fill
                t.set(x0 + x, y0 + y, c)

    cell(0, 204, (stone[1], stone[4]), stone[2])
    cell(20, 204, (GOLD[2], GOLD[4]), stone[3])
    cell(40, 204, (BLOOD[2], BLOOD[4]), hexc("#2e1416"))
    cell(60, 204, (GOLD[3], GOLD[5]), hexc("#3a3020"))
    return t


def interrogation_bar():
    t = Tex(128, 32)
    for y in range(12):
        for x in range(128):
            e = min(x, y, 127 - x, 11 - y)
            if e == 0:
                c = O
            elif e == 1:
                c = GOLD[3] if y == 1 or x == 1 else GOLD[1]
            elif e == 2:
                c = BASALT[0]
            else:
                c = (18, 10, 12, 210)
            t.set(x, y, c)
    # Rivets and glyph ticks along the frame; end caps.
    for x in range(8, 120, 10):
        t.set(x, 1, GOLD[5])
        t.set(x, 10, GOLD[2])
    for x0 in (0, 124):
        for y in range(12):
            for x in range(x0, x0 + 4):
                if 0 < y < 11:
                    t.set(x, y, GOLD[2] if (x + y) % 2 else GOLD[3])
    # The fill: deep red, a lighter top line, a dark bottom, a faint pulse pattern.
    for y in range(12):
        for x in range(128):
            v = 3.0 if 2 <= y <= 3 else 2.4 if y <= 8 else 1.6
            if y in (0, 11):
                v = 1.0
            if (x // 4 + y) % 7 == 0:
                v += 0.5
            t.set(x, 12 + y, tone(BLOOD, v))
    return t


# =====================================================================================================================

def generate():
    save(banishing_sigil(), "block", "banishing_sigil")
    save(enochian_ward(), "block", "enochian_ward")
    save(alarm_sigil(False), "block", "alarm_sigil")
    save(alarm_sigil(True), "block", "alarm_sigil_lit")
    save(holy_oil_dot(), "block", "holy_oil_trail_dot")
    save(holy_oil_side(), "block", "holy_oil_trail_side")
    _, parts = engraved_trap_parts()
    for i, p in enumerate(parts):
        save(p, "block", f"engraved_devils_trap_{i}")
    save(enochian_stone(), "block", "enochian_stone")
    save(enochian_stone_bricks(), "block", "enochian_stone_bricks")
    save(chiseled_enochian_stone(), "block", "chiseled_enochian_stone")
    for name in ("banishing", "ward", "alarm"):
        save(sigil_plate(name, False), "block", f"sigil_plate_{name}")
        save(sigil_plate(name, True), "block", f"sigil_plate_{name}_solved")
    save(sealed_crypt_door(), "block", "sealed_crypt_door")
    for v in range(4):
        save(word_mural(v), "block", f"word_mural_{v}")
    save(pedestal_side(), "block", "tablet_pedestal_side")
    save(pedestal_top(False), "block", "tablet_pedestal_top")
    save(pedestal_top(True), "block", "tablet_pedestal_top_full")
    save(item_banishing_sigil(), "item", "banishing_sigil")
    save(item_enochian_ward(), "item", "enochian_ward")
    save(item_alarm_sigil(), "item", "alarm_sigil")
    save(item_engraved_trap(), "item", "engraved_devils_trap")
    save(item_demon_tablet(), "item", "demon_tablet")
    save(item_hellhound_blood(), "item", "hellhound_blood")
    save(item_purgatory_jar(False), "item", "purgatory_jar")
    save(item_purgatory_jar(True), "item", "purgatory_jar_full")
    save(item_innocent_soul(), "item", "innocent_soul")
    save(tablet_screen(), "gui", "tablet_screen")
    save(word_glyph_atlas(), "gui", "word_glyphs")
    save(interrogation_bar(), "gui", "interrogation_bar")
