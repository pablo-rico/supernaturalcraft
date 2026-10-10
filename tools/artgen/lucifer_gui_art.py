"""The Cage's HUD (Lucifer and Lucifer Uncaged): textures/gui/lucifer/, laid out exactly as LuciferGui draws them.

  bar_frame    256x32    black iron round the bar, rivets, runes graven along the top rail, spearheads at the ends, the
                         inverted star hanging under the middle; the fill's rect x 24..232, y 12..20 is left clear
  bar_runes    256x32    only the graven runes and the star's heart, white (drawn additive, tinted by the phase)
  fill_<name>  208x64    8 frames of 208x8: fire or light flowing along (frame = time); one sheet per palette
  bar_bars     96x32     8 states of a 12x32 cage bar, over the frame: straight, red-hot, rimed, bent left, bent right,
                         snapped (its upper half, thrown off), stump (its lower half, left), cracked with gold
  debris       32x8      four 8x8 sprites: a chain link, a rivet, an ember, a shard of ice
  glyphs_large 160x120   8x5 cells of 20x24: gothic capitals (white, shaded; tinted by the code), baseline at y 19
  glyphs_small 80x60     8x5 cells of 10x12: the same capitals, small, baseline at y 9
  glyphs.json            the character set and each glyph's advance, for both atlases
  title_seal   256x256   a seal of two rings of runes and a star, white on clear (drawn additive, tinted)
  title_motifs 512x384   4x3 cells of 128: the motif at the heart of each title (index below), white on clear
  title_chain  256x24    a chain, tiling horizontally (iron, lit from above)

Motifs: 0 Lucifer's sigil, 1 a falling wing, 2 rimed bars, 3 a winged sun, 4 a shackle, 5 flames, 6 a crystal of ice,
7 hounds' eyes, 8 a falling star, 9 six wings, 10 a padlock.
"""

import json
import math
import os

from common import TEXTURES, WRITTEN, save
from pixelkit import Tex, fbm, hexc, mix

CLEAR = (0, 0, 0, 0)
WHITE = (255, 255, 255, 255)

# --- gothic capitals ---------------------------------------------------------------------------------------
#
# Each letter is drawn with a broad pen: polylines in design units (cap height 10, y down, baseline at 10) swept by a
# nib held at 30 degrees, so verticals and falling diagonals come out thick and rising ones and hairlines thin. The same
# strokes give both sizes.

NIB_ANGLE = math.radians(30)
NIB_DIR = (math.cos(NIB_ANGLE), -math.sin(NIB_ANGLE))

# char -> (advance in units, [polyline, ...]); a one-point polyline is a diamond (a dot).
LETTERS = {
    "A": (8, [[(0.4, 10), (1.4, 9), (1.4, 3.2), (4, 0.2), (6.6, 3.2), (6.6, 9), (7.6, 10)], [(1.4, 6), (6.6, 6)]]),
    "B": (7.5, [[(0.3, 0.6), (1.2, 0.3), (5, 0.3), (6.2, 1.5), (6.2, 3.8), (4.8, 5)], [(1.2, 0.3), (1.2, 9.7)],
                [(1.2, 5), (5.2, 5), (6.7, 6.5), (6.7, 8.5), (5.5, 9.7), (0.3, 9.7)]]),
    "C": (7.3, [[(6.6, 1.6), (5.4, 0.3), (2.6, 0.3), (1, 2), (1, 8), (2.6, 9.7), (5.4, 9.7), (6.8, 8.4)],
                ("h", [(3.4, 1.4), (3.4, 8.6)])]),
    "D": (7.8, [[(1.2, 0.3), (1.2, 9.7)], [(0.2, 0.3), (4.6, 0.3), (6.9, 2.6), (6.9, 7.4), (4.6, 9.7), (0.2, 9.7)],
                ("h", [(3.8, 1.4), (3.8, 8.6)])]),
    "E": (7.0, [[(6.2, 1.2), (5.2, 0.3), (2.6, 0.3), (1, 2), (1, 8), (2.6, 9.7), (5.2, 9.7), (6.4, 8.6)],
                [(1, 5), (4.2, 5)], [(4.6, 5)]]),
    "F": (6.8, [[(6.4, 1.4), (5.6, 0.3), (0.6, 0.3)], [(2, 0.3), (2, 9), (1, 10.4)], [(2, 5), (5, 5)]]),
    "G": (7.6, [[(6.6, 1.6), (5.4, 0.3), (2.6, 0.3), (1, 2), (1, 8), (2.6, 9.7), (5.4, 9.7), (6.8, 8.4), (6.8, 5.6),
                 (4.2, 5.6)], ("h", [(3.4, 1.4), (3.4, 5)])]),
    "H": (7.8, [[(1.2, 0.3), (1.2, 9.7)], [(6.6, 0.3), (6.6, 9.4), (5.8, 11.2)], [(1.2, 5), (6.6, 5)],
                [(0.2, 0.3), (1.2, 0.3)]]),
    "I": (4.2, [[(2.1, 0.3), (2.1, 9.7)], [(0.6, 0.3), (3.6, 0.3)], [(0.6, 9.7), (3.6, 9.7)]]),
    "J": (6.2, [[(4.6, 0.3), (4.6, 8.6), (3.2, 10.8), (1.2, 10.8), (0.6, 10)], [(3, 0.3), (6, 0.3)]]),
    "K": (7.4, [[(1.2, 0.3), (1.2, 9.7)], [(6.4, 0.3), (1.6, 5.2), (6.8, 9.7)], [(0.2, 0.3), (1.2, 0.3)]]),
    "L": (7.0, [[(0.4, 0.3), (2, 0.3)], [(1.4, 0.3), (1.4, 9.7), (6.4, 9.7), (6.8, 8.6)]]),
    "M": (9.4, [[(0.4, 10), (1.2, 9), (1.2, 1.4), (2.4, 0.3), (4.6, 2.6), (6.8, 0.3), (8, 1.4), (8, 9), (8.8, 10)],
                [(4.6, 2.6), (4.6, 7.6)]]),
    "N": (7.8, [[(0.4, 10), (1.2, 9), (1.2, 0.3)], [(1.2, 0.3), (6.6, 9.7)], [(6.6, 9.7), (6.6, 0.3), (7.4, -0.4)]]),
    "O": (7.6, [[(3, 0.3), (1, 2.2), (1, 7.8), (3, 9.7), (4.6, 9.7), (6.6, 7.8), (6.6, 2.2), (4.6, 0.3), (3, 0.3)],
                ("h", [(3.8, 1.4), (3.8, 8.6)])]),
    "P": (7.2, [[(1.2, 0.3), (1.2, 10.8)], [(0.2, 0.3), (5, 0.3), (6.5, 1.8), (6.5, 4), (5, 5.5), (1.2, 5.5)]]),
    "Q": (7.6, [[(3, 0.3), (1, 2.2), (1, 7.8), (3, 9.7), (4.6, 9.7), (6.6, 7.8), (6.6, 2.2), (4.6, 0.3), (3, 0.3)],
                [(3.6, 7.6), (6.8, 11.2)], ("h", [(3.8, 1.4), (3.8, 6.6)])]),
    "R": (7.6, [[(1.2, 0.3), (1.2, 9.7)], [(0.2, 0.3), (5, 0.3), (6.5, 1.8), (6.5, 4), (5, 5.5), (1.2, 5.5)],
                [(3.4, 5.5), (6.6, 9.7), (7.4, 9.7)]]),
    "S": (7.0, [[(6.2, 1.2), (5, 0.3), (2.4, 0.3), (1, 1.6), (1, 3.6), (6.2, 6.2), (6.2, 8.4), (4.8, 9.7), (2, 9.7),
                 (0.6, 8.6)]]),
    "T": (7.6, [[(0.4, 1.2), (1.2, 0.3), (7.2, 0.3)], [(3.8, 0.3), (3.8, 9.7)], [(2.6, 9.7), (5, 9.7)]]),
    "U": (7.8, [[(0.2, 0.3), (1.2, 0.3), (1.2, 8), (2.8, 9.7), (4.8, 9.7), (6.6, 8)], [(6.6, 0.3), (6.6, 9.7), (7.4, 10.2)]]),
    "V": (7.6, [[(0.4, 0.3), (3.8, 9.7), (7.2, 0.3)]]),
    "W": (10.2, [[(0.4, 0.3), (2.6, 9.7), (5.1, 2.6), (7.6, 9.7), (9.8, 0.3)]]),
    "X": (7.4, [[(0.8, 0.3), (6.6, 9.7)], [(6.6, 0.3), (0.8, 9.7)]]),
    "Y": (7.4, [[(0.6, 0.3), (3.7, 5)], [(6.8, 0.3), (3.7, 5), (3.7, 9.7)], [(2.6, 9.7), (4.8, 9.7)]]),
    "Z": (7.2, [[(0.8, 1.2), (1.4, 0.3), (6.4, 0.3), (0.8, 9.7), (6.4, 9.7), (6.8, 8.8)]]),
    ",": (2.6, [[(1.2, 8.8), (1.2, 9.8), (0.4, 11.2)]]),
    ".": (2.6, [[(1.2, 9.2)]]),
    "-": (4.6, [[(0.6, 5.2), (4, 5.2)]]),
    "'": (2.6, [[(1.4, 0), (1.4, 1.6), (0.6, 2.8)]]),
    "!": (3.0, [[(1.4, 0.3), (1.4, 6.8)], [(1.4, 9.2)]]),
    "?": (6.6, [[(0.8, 1.6), (2, 0.3), (4.6, 0.3), (5.8, 1.6), (5.8, 3.2), (3.2, 5.2), (3.2, 6.8)], [(3.2, 9.2)]]),
    " ": (3.2, []),
}
CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ,.-'!? "


def _seg_cover(px, py, a, b, nib, hair):
    """Whether (px, py) is under the pen swept from a to b: the nib's parallelogram, or a hairline round the path."""
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    nx, ny = NIB_DIR
    # Solve p = a + s*(b-a) + t*nib for s in [0,1], t in [-nib/2, nib/2].
    det = dx * ny - dy * nx
    rx, ry = px - ax, py - ay
    if abs(det) > 1e-6:
        s = (rx * ny - ry * nx) / det
        t = (dx * ry - dy * rx) / det
        if 0 <= s <= 1 and abs(t) <= nib / 2:
            return True
    else:
        # Moving along the nib itself: the stroke is the nib's own line, lengthened.
        L = dx * dx + dy * dy
        s = 0 if L == 0 else max(0.0, min(1.0, (rx * dx + ry * dy) / L))
        cx, cy = rx - dx * s, ry - dy * s
        t = cx * nx + cy * ny
        if abs(t) <= nib / 2 and abs(cx * ny - cy * nx) <= hair:
            return True
    # Hairline: distance to the path itself.
    L = dx * dx + dy * dy
    s = 0 if L == 0 else max(0.0, min(1.0, (rx * dx + ry * dy) / L))
    return math.hypot(rx - dx * s, ry - dy * s) <= hair


def _diamond(p, size):
    """A dot: a short downward stroke, which the nib turns into a lozenge."""
    x, y = p
    return [(x, y - size / 2), (x, y + size / 2)]


def glyph_mask(ch, unit, nib, hair, ss=4, hairlines=True):
    """Coverage of a glyph at `unit` px per design unit, its cap top at y 0: {(x, y): 0..1}. Without `hairlines` the
    split-bowl hairlines are left out (too small to read)."""
    adv, strokes = LETTERS[ch]
    segs = []
    for line in strokes:
        thin = isinstance(line, tuple) and line[0] == "h"
        if thin:
            if not hairlines:
                continue
            line = line[1]
        if len(line) == 1:
            line = _diamond(line[0], 1.2)
        for i in range(len(line) - 1):
            segs.append((line[i], line[i + 1], thin))
    cover = {}
    if not segs:
        return cover
    xs = [p[0] for s in segs for p in s[:2]]
    ys = [p[1] for s in segs for p in s[:2]]
    x0, x1 = int(math.floor((min(xs) - nib) * unit)) - 1, int(math.ceil((max(xs) + nib) * unit)) + 1
    y0, y1 = int(math.floor((min(ys) - nib) * unit)) - 1, int(math.ceil((max(ys) + nib) * unit)) + 1
    for y in range(y0, y1):
        for x in range(x0, x1):
            hits = 0
            for sy in range(ss):
                for sx in range(ss):
                    u = (x + (sx + 0.5) / ss) / unit
                    v = (y + (sy + 0.5) / ss) / unit
                    if any(_seg_cover(u, v, a, b, 0 if thin else nib, hair) for a, b, thin in segs):
                        hits += 1
            if hits:
                cover[(x, y)] = hits / (ss * ss)
    return cover


ATLASES = {
    # name: cell w, h, cols, px per unit, nib, hairline (units), cap top y, coverage threshold, letter gap (px)
    "large": dict(cw=20, ch=24, cols=8, unit=1.6, nib=2.5, hair=0.34, top=3, cut=0.42, gap=1, hairlines=True),
    "small": dict(cw=10, ch=12, cols=8, unit=0.8, nib=2.4, hair=0.55, top=1, cut=0.4, gap=1, hairlines=False),
}


def glyph_atlas(name):
    a = ATLASES[name]
    rows = (len(CHARSET) + a["cols"] - 1) // a["cols"]
    t = Tex(a["cw"] * a["cols"], a["ch"] * rows, 9901 if name == "large" else 9902)
    widths = []
    for i, ch in enumerate(CHARSET):
        ox, oy = (i % a["cols"]) * a["cw"], (i // a["cols"]) * a["ch"] + a["top"]
        cover = glyph_mask(ch, a["unit"], a["nib"], a["hair"], hairlines=a["hairlines"])
        cap = 10 * a["unit"]
        for (x, y), c in cover.items():
            if c < a["cut"] or not (0 <= x < a["cw"] and -a["top"] <= y < a["ch"] - a["top"]):
                continue
            # White, lit from above: the top of each stroke brightest, the foot dimmer (the code tints it).
            v = 255 - int(70 * max(0.0, min(1.0, y / cap)))
            t.set(ox + x, oy + y, (v, v, v, 255))
        adv = LETTERS[ch][0]
        widths.append(min(a["cw"], int(round(adv * a["unit"])) + a["gap"]))
    return t, widths


# --- light: white shapes with soft edges (drawn additive and tinted by the code) -----------------------------

class Lum:
    """A canvas of light: coverage 0..1 per pixel, saved white on clear. Shapes take the brighter of what is there."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.a = [[0.0] * w for _ in range(h)]

    def put(self, x, y, v):
        if 0 <= x < self.w and 0 <= y < self.h and v > self.a[y][x]:
            self.a[y][x] = min(1.0, v)

    def carve(self, x, y, v=0.0):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[y][x] = min(self.a[y][x], v)

    def _box(self, x0, y0, x1, y1, pad):
        return (max(0, int(min(x0, x1) - pad)), max(0, int(min(y0, y1) - pad)),
                min(self.w, int(max(x0, x1) + pad) + 2), min(self.h, int(max(y0, y1) + pad) + 2))

    def line(self, x0, y0, x1, y1, width=2.0, v=1.0, soft=1.2, carve=False):
        bx0, by0, bx1, by1 = self._box(x0, y0, x1, y1, width / 2 + soft)
        dx, dy = x1 - x0, y1 - y0
        L = dx * dx + dy * dy
        for y in range(by0, by1):
            for x in range(bx0, bx1):
                px, py = x + 0.5 - x0, y + 0.5 - y0
                s = 0 if L == 0 else max(0.0, min(1.0, (px * dx + py * dy) / L))
                d = math.hypot(px - dx * s, py - dy * s) - width / 2
                k = 1.0 if d <= 0 else max(0.0, 1 - d / soft) if soft > 0 else 0.0
                if k > 0:
                    # Carving darkens down to v (0 cuts right through).
                    self.carve(x, y, 1 - k * (1 - v)) if carve else self.put(x, y, v * k)

    def path(self, pts, width=2.0, v=1.0, soft=1.2, closed=False):
        seq = list(pts) + ([pts[0]] if closed else [])
        for i in range(len(seq) - 1):
            self.line(seq[i][0], seq[i][1], seq[i + 1][0], seq[i + 1][1], width, v, soft)

    def ring(self, cx, cy, r, width=2.0, v=1.0, soft=1.2):
        bx0, by0, bx1, by1 = self._box(cx - r, cy - r, cx + r, cy + r, width / 2 + soft)
        for y in range(by0, by1):
            for x in range(bx0, bx1):
                d = abs(math.hypot(x + 0.5 - cx, y + 0.5 - cy) - r) - width / 2
                k = 1.0 if d <= 0 else max(0.0, 1 - d / soft) if soft > 0 else 0.0
                if k > 0:
                    self.put(x, y, v * k)

    def disc(self, cx, cy, r, v=1.0, soft=1.2, carve=False, rx=None, ry=None):
        rx, ry = rx or r, ry or r
        bx0, by0, bx1, by1 = self._box(cx - rx, cy - ry, cx + rx, cy + ry, soft)
        for y in range(by0, by1):
            for x in range(bx0, bx1):
                q = math.hypot((x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry)
                d = (q - 1) * min(rx, ry)
                k = 1.0 if d <= 0 else max(0.0, 1 - d / soft) if soft > 0 else 0.0
                if k > 0:
                    self.carve(x, y, 1 - k) if carve else self.put(x, y, v * k)

    def poly(self, pts, v=1.0, carve=False):
        """Fills a polygon (even-odd), 2x2 supersampled for its edge."""
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        for y in range(max(0, int(min(ys)) - 1), min(self.h, int(max(ys)) + 2)):
            for x in range(max(0, int(min(xs)) - 1), min(self.w, int(max(xs)) + 2)):
                hits = 0
                for sy in (0.25, 0.75):
                    for sx in (0.25, 0.75):
                        if _inside(x + sx, y + sy, pts):
                            hits += 1
                if hits:
                    self.carve(x, y, 1 - hits / 4) if carve else self.put(x, y, v * hits / 4)

    def tex(self, seed=0):
        t = Tex(self.w, self.h, seed)
        for y in range(self.h):
            for x in range(self.w):
                a = self.a[y][x]
                if a > 0.02:
                    t.set(x, y, (255, 255, 255, int(255 * a)))
        return t


def _inside(x, y, pts):
    n, c = len(pts), False
    j = n - 1
    for i in range(n):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            c = not c
        j = i
    return c


# --- the boss bar ---------------------------------------------------------------------------------------------

IRON = [hexc(c) for c in ("#050506", "#0d0d10", "#17171b", "#222228", "#303038", "#44444e", "#5c5c68")]
RUST = hexc("#2a1210")
FX0, FX1, FY0, FY1 = 24, 232, 12, 20          # the fill's rect (exclusive ends)
RAIL_TOP, RAIL_BOT = (5, 11), (20, 25)        # rows of the rails
BAR_W, BAR_H, BARS = 12, 32, 8                # one bar of the cage; how many cross the bar
# The runes graven along the top rail: Lucifer's own script (the same table as Michael's Enochian, another seed).
RUNE_SEED = 6661


def bar_x(i):
    """Centre x (in the frame) of cage bar i: eight, evenly spaced, the medallion's column left free between 3 and 4."""
    return FX0 + (FX1 - FX0) * (i + 0.5) / BARS


def iron(v):
    return IRON[max(0, min(len(IRON) - 1, int(round(v))))]


def _rune_bits():
    """The pixels of the graven runes on the top rail: (x, y)."""
    from michael_art import glyph_bit
    bits = []
    x, gx = FX0 + 4, 0
    while x + 3 <= FX1 - 4:
        if abs(x + 1 - 128) > 9:
            for cy in range(5):
                for cx in range(3):
                    if glyph_bit(RUNE_SEED, gx, 0, cx, cy):
                        bits.append((x + cx, RAIL_TOP[0] + 1 + cy))
        x += 5
        gx += 1
    return bits


def _star_points(cx, cy, r, inverted=True, n=5):
    pts = []
    for k in range(n):
        a = math.radians((90 if inverted else -90) + k * 360 / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return [pts[(k * 2) % n] for k in range(n)]


def bar_frame():
    t = Tex(256, 32, 9911)
    n = lambda x, y: fbm(x, y, 9912, 256, 32, 3, 32.0)
    # The rails: a lit upper edge, a body of pitted iron, a dark groove next to the fill.
    for x in range(FX0 - 3, FX1 + 3):
        top0, top1 = RAIL_TOP
        t.set(x, top0, iron(5))
        for y in range(top0 + 1, top1):
            t.set(x, y, iron(2.6 + 1.2 * (n(x, y) - 0.5) - 0.3 * (y - top0)))
        t.set(x, top1, IRON[0])
        b0, b1 = RAIL_BOT
        t.set(x, b0, IRON[0])
        t.set(x, b0 + 1, iron(4.4))
        for y in range(b0 + 2, b1):
            t.set(x, y, iron(2.4 + 1.2 * (n(x, y) - 0.5)))
        t.set(x, b1, IRON[0])
    # Rust bleeding down from the rivets, here and there.
    for x in range(FX0, FX1, 13):
        if n(x, 3) > 0.55:
            for y in range(RAIL_BOT[0] + 2, RAIL_BOT[1]):
                t.set(x, y, mix(t.get(x, y), RUST, 0.5))
    # The graven runes: cut into the iron (dark), their light is bar_runes.
    for x, y in _rune_bits():
        t.set(x, y, IRON[0])
        # A lit lip under each cut.
        t.set(x, y + 1, mix(t.get(x, y + 1), IRON[4], 0.5))
    # Sockets where each cage bar passes through the rails.
    for i in range(BARS):
        bx = round(bar_x(i))
        for y in (RAIL_TOP[1] - 1, RAIL_BOT[0] + 1):
            for dx in range(-3, 4):
                t.set(bx + dx, y, IRON[0] if abs(dx) == 3 else iron(1.5))
    # The posts joining the rails at each end, and the end blocks with their rivets.
    for side in (-1, 1):
        px = FX0 - 3 if side < 0 else FX1
        for y in range(RAIL_TOP[0], RAIL_BOT[1] + 1):
            for dx in range(3):
                t.set(px + dx, y, iron(4 - dx) if side < 0 else iron(2 + dx * 0.5))
        bx0 = FX0 - 10 if side < 0 else FX1 + 3
        for y in range(2, 30):
            for dx in range(7):
                x = bx0 + dx
                e = min(dx, 6 - dx, y - 2, 29 - y)
                c = IRON[0] if e == 0 else iron(4.6 - 0.07 * y) if e == 1 and (dx == 1 or y == 3) else iron(2.8 + 1.2 * (n(x, y) - 0.5))
                t.set(x, y, c)
        for ry in (6, 15, 24):
            cx = bx0 + 3
            t.set(cx, ry, IRON[6])
            t.set(cx - 1, ry, IRON[4])
            t.set(cx, ry + 1, IRON[1])
            t.set(cx + 1, ry + 1, IRON[0])
        # A spearhead pointing outward: the cage's bars end like this.
        tip = 1 if side < 0 else 254
        base = bx0 - 1 if side < 0 else bx0 + 7
        L = abs(base - tip)
        for k in range(L + 1):
            x = base + (-k if side < 0 else k)
            f = k / L
            # Widening from the collar to its barbs, then tapering to the point.
            half = 4.5 * (1 - f) if f > 0.35 else 1 + 3.5 * f / 0.35
            for dy in range(-5, 6):
                if abs(dy) <= half:
                    edge = abs(dy) > half - 1
                    t.set(x, 15 + dy, IRON[0] if edge else iron(4.5 - 0.4 * abs(dy) - (1 if dy > 0 else 0)))
        # A collar where the spear meets the block.
        cxl = base + (-1 if side < 0 else 1)
        for dy in range(-3, 5):
            t.set(cxl, 15 + dy, iron(5) if dy < 0 else iron(3))
    # The medallion hanging under the middle: the inverted star in an iron ring.
    cx, cy = 128, 26.5
    for y in range(18, 32):
        for x in range(119, 138):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r <= 5.6:
                t.set(x, y, iron(1.2))
            elif r <= 7.0:
                t.set(x, y, iron(4.8 - (y - 19) * 0.25))
            elif r <= 7.8:
                t.set(x, y, IRON[0])
    star = _star_points(cx, cy, 5.0)
    for y in range(20, 32):
        for x in range(121, 136):
            if _star_pixel(x, y, star):
                t.set(x, y, IRON[0])
    return t


def _star_pixel(x, y, star, width=0.75):
    for i in range(5):
        (ax, ay), (bx, by) = star[i], star[(i + 1) % 5]
        dx, dy = bx - ax, by - ay
        L = dx * dx + dy * dy
        px, py = x + 0.5 - ax, y + 0.5 - ay
        s = max(0.0, min(1.0, (px * dx + py * dy) / L))
        if math.hypot(px - dx * s, py - dy * s) <= width:
            return True
    return False


def bar_runes():
    t = Tex(256, 32, 9913)
    for x, y in _rune_bits():
        t.set(x, y, WHITE)
    star = _star_points(128, 26.5, 5.0)
    for y in range(20, 32):
        for x in range(121, 136):
            if _star_pixel(x, y, star):
                t.set(x, y, WHITE)
    return t


# Fill palettes, darkest to brightest, and what moves in them.
FILLS = {
    "hellfire": ("#1a0302", "#4a0904", "#8c1607", "#cf3a0c", "#f2701a", "#ffb03a", "#ffe08a"),
    "embers": ("#0c0605", "#24100a", "#4a1c0e", "#7a2e12", "#c24a16", "#ff7a24", "#ffc060"),
    "chains": ("#120203", "#2e0507", "#55090d", "#8a0e14", "#b81f1c", "#e8452a", "#ff8a5a"),
    "frost": ("#0a1420", "#1c3248", "#345a7a", "#5a8cb0", "#8fd8ff", "#c8eeff", "#ffffff"),
    "legion": ("#0a0103", "#22030a", "#430713", "#6a0f2a", "#94163a", "#c42a4a", "#ff5a6a"),
    "star": ("#1e1204", "#4a2e08", "#8a5a14", "#c89024", "#f0c04a", "#ffe08a", "#fff8e0"),
    "light": ("#6a6050", "#a89a7a", "#dccaa2", "#f0e4c8", "#fbf5e6", "#fffcf4", "#ffffff"),
}


def fill(name):
    ramp = [hexc(c) for c in FILLS[name]]
    seed = 9920 + sorted(FILLS).index(name)
    t = Tex(208, 64, seed)
    for f in range(8):
        for y in range(8):
            core = 1 - abs(y - 3.5) / 4.0
            for x in range(208):
                xs = x - f * 26                      # the flow moves right, a whole period over the eight frames
                n = fbm(xs, y * 6, seed, 208, 48, 3, 16.0)
                if name in ("hellfire", "embers", "chains"):
                    # Flames licking upward: bright low, ragged high.
                    lick = fbm(xs * 2, y * 10 - f * 20, seed + 1, 416, 80, 2, 16.0)
                    v = 1.2 + 3.6 * core * (0.55 + 0.45 * n) + 1.6 * (lick - 0.5) + 0.5 * (y / 7)
                    if name == "embers":
                        v -= 1.0
                        if _h(x, y, f, seed) > 0.97:
                            v = 6
                elif name == "frost":
                    # Ice: still, crystalline streaks that glint as the light runs over them.
                    streak = abs(((x * 0.6 + y * 1.7) % 9) - 4.5) < 0.6
                    v = 2.4 + 2.4 * core + 1.0 * (n - 0.5) + (1.4 if streak else 0)
                    v += 1.2 * max(0.0, math.cos((x - f * 26) * 2 * math.pi / 104)) ** 8
                elif name == "legion":
                    # Blood with veins that pulse.
                    vein = abs(fbm(x, y * 6, seed + 2, 208, 48, 2, 8.0) - 0.5) < 0.05
                    v = 1.4 + 2.6 * core + 1.4 * (n - 0.5) + (2.2 * (0.5 + 0.5 * math.sin(f * math.pi / 4)) if vein else 0)
                else:
                    # Gold and white: light running along, sparkles.
                    wave = 0.5 + 0.5 * math.sin(xs * 2 * math.pi / 52)
                    v = 2.6 + 2.4 * core + 0.9 * wave * core + 0.6 * (n - 0.5)
                    if _h(x, y, f, seed) > 0.985 and core > 0.4:
                        v = 6.5
                c = ramp[max(0, min(len(ramp) - 1, int(round(v))))]
                t.set(x, f * 8 + y, c)
    return t


def _h(x, y, f, seed):
    n = (x * 73856093 ^ y * 19349663 ^ f * 83492791 ^ seed * 2654435761) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 0xFFFF


BAR_STATES = ("straight", "hot", "rimed", "bent_left", "bent_right", "snapped", "stump", "cracked")
HOT = [hexc(c) for c in ("#3a0a06", "#7a1608", "#c4320c", "#f26a14", "#ffb040", "#ffe8a0")]
RIME = [hexc(c) for c in ("#5a7894", "#8fb4d0", "#cfe8f8", "#ffffff")]
GOLD_CRACK = hexc("#ffe08a")


def _bar(t, ox, state):
    """One cage bar into its 12x32 cell at x ox: a spear tip, the shaft, a foot."""
    cx = ox + 5
    torn_lo, torn_hi = 13, 18          # where a bar tears
    for y in range(BAR_H):
        if state == "snapped" and y > torn_hi:
            continue
        if state == "stump" and y < torn_lo + 4:
            continue
        # The shaft bows for a bent bar: most in the middle.
        bow = 0
        if state in ("bent_left", "bent_right"):
            bow = round(3.2 * math.sin(math.pi * max(0, min(1, (y - 3) / 26))) ** 1.4) * (-1 if state == "bent_left" else 1)
        x = cx + bow
        if y <= 4:
            # The spear tip.
            half = min(2, y // 2)
            for dx in range(-half, half + 1):
                t.set(x + dx + 1, y, IRON[5] if dx < 0 else IRON[3] if dx == 0 else IRON[1])
            t.set(x - half, y, IRON[0]) if half else None
            t.set(x + half + 2, y, IRON[0]) if half else None
            continue
        if y >= 28:
            # The foot: a collar.
            for dx in range(-2, 5):
                t.set(x + dx, y, IRON[0] if dx in (-2, 4) or y == 31 else iron(4.4 - 0.5 * (y - 28) - 0.3 * max(0, dx)))
            continue
        shade = (IRON[0], IRON[5], IRON[3], IRON[2], IRON[0])
        for dx in range(-1, 4):
            t.set(x + dx, y, shade[dx + 1])
        if state == "hot":
            glow = math.sin(math.pi * (y - 4) / 24)
            for dx in range(0, 3):
                t.set(x + dx, y, HOT[max(0, min(5, round(1 + 4 * glow - (1 if dx == 2 else 0))))])
        elif state == "rimed":
            if _h(ox, y, 0, 77) > 0.45:
                t.set(x + (y % 3), y, RIME[1 + (y % 3 == 1)])
            if y % 7 == 3:
                t.set(x - 1, y, RIME[2])
        elif state == "cracked":
            k = (y * 5) % 9
            if 3 < k < 7 and 6 < y < 26:
                t.set(x + 1 + (y // 3) % 2, y, GOLD_CRACK)
        elif state in ("bent_left", "bent_right") and abs(bow) >= 2:
            # Scuffed where it took the blow.
            t.set(x + 1, y, IRON[6])
    if state == "rimed":
        # Icicles under the collars of the rails.
        for k, yy in enumerate((12, 26)):
            for d in range(2 + k):
                t.set(cx + 1 - k, yy + d, RIME[3 - d])
            t.set(cx + 3, yy, RIME[2])
    if state in ("snapped", "stump"):
        # The torn end, still glowing.
        y = torn_hi if state == "snapped" else torn_lo + 4
        for dx in range(-1, 4):
            jag = (dx * 7 + 3) % 3
            t.set(cx + dx, y + (jag if state == "snapped" else -jag), HOT[4 - abs(dx - 1)])
    return t


def bar_bars():
    t = Tex(BAR_W * len(BAR_STATES), BAR_H, 9930)
    for i, s in enumerate(BAR_STATES):
        _bar(t, i * BAR_W, s)
    return t


def debris():
    t = Tex(32, 8, 9931)
    # A chain link: an iron oval, its hole clear.
    for y in range(8):
        for x in range(8):
            q = math.hypot((x + 0.5 - 4) / 3.6, (y + 0.5 - 4) / 2.6)
            hole = math.hypot((x + 0.5 - 4) / 1.8, (y + 0.5 - 4) / 0.9)
            if q <= 1 and hole > 1:
                t.set(x, y, IRON[5] if y < 3 else IRON[3] if y < 5 else IRON[1])
    # A rivet.
    for y in range(8):
        for x in range(8):
            r = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
            if r <= 2.6:
                t.set(8 + x, y, IRON[6] if x + y < 6 else IRON[4] if r < 1.8 else IRON[1])
    # An ember (white-hot heart, orange rim; the code tints it).
    for y in range(8):
        for x in range(8):
            r = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
            if r <= 3:
                t.set(16 + x, y, (255, 255, 255, 255) if r < 1.4 else (255, 200, 120, int(255 * (1 - (r - 1.4) / 1.8))))
    # A shard of ice.
    for y in range(8):
        for x in range(8):
            if abs(x + 0.5 - 4) * 2.2 + abs(y + 0.5 - 4) <= 3.6:
                t.set(24 + x, y, RIME[3] if x < 4 else RIME[1])
    return t


# --- the title card ----------------------------------------------------------------------------------------------

def title_seal():
    from michael_art import glyph_bit
    L = Lum(256, 256)
    c = 128.0
    L.ring(c, c, 122, 2.2, 1.0, 1.4)
    L.ring(c, c, 116, 1.0, 0.8, 1.0)
    # The band of runes, read round the ring.
    r_out, r_in = 113.0, 101.0
    for y in range(256):
        for x in range(256):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy)
            if not (r_in <= r < r_out):
                continue
            u = (math.atan2(dy, dx) % (2 * math.pi)) * 107
            v = r_out - r
            g, gu = divmod(u, 9.0)
            cx, cy = int(gu / 2.2), int(v / 2.2)
            if cx < 3 and cy < 5 and glyph_bit(RUNE_SEED, int(g), 1, cx, cy):
                L.put(x, y, 0.95)
    L.ring(c, c, 96, 2.0, 1.0, 1.4)
    L.ring(c, c, 62, 1.2, 0.7, 1.2)
    # Ticks between the inner rings.
    for k in range(72):
        a = math.radians(k * 5)
        r0 = 64 if k % 3 else 62
        L.line(c + r0 * math.cos(a), c + r0 * math.sin(a), c + 70 * math.cos(a), c + 70 * math.sin(a), 1.0 if k % 3 else 1.8, 0.75, 1.0)
    # The inverted star, its points on the ring, small rings at its points.
    star = _star_points(c, c, 96)
    L.path(star, 2.2, 1.0, 1.6, closed=True)
    for k in range(5):
        a = math.radians(90 + k * 72)
        L.disc(c + 96 * math.cos(a), c + 96 * math.sin(a), 4.5, 1.0, 1.2)
        L.disc(c + 96 * math.cos(a), c + 96 * math.sin(a), 2.0, 0.0, 0.5, carve=True)
    # The heart left clear for the motif, in a ring of its own.
    L.disc(c, c, 50, 0.0, 1.5, carve=True)
    L.ring(c, c, 50, 1.6, 0.9, 1.2)
    return L.tex(9940)


def _feather(L, x0, y0, angle, length, width, v=1.0):
    """A feather: a leaf from its quill at (x0, y0) along `angle`, its widest a third of the way, pointed at the tip;
    the shaft down its middle a little darker so overlapping feathers still read apart."""
    ca, sa = math.cos(angle), math.sin(angle)
    left, right = [], []
    for k in range(13):
        f = k / 12
        half = width / 2 * (math.sin(math.pi * min(1.0, f * 1.5)) ** 0.5 if f < 0.34 else (1 - (f - 0.34) / 0.66) ** 0.7)
        px, py = x0 + ca * length * f, y0 + sa * length * f
        left.append((px - sa * half, py + ca * half))
        right.append((px + sa * half, py - ca * half))
    L.poly(left + right[::-1], v)
    L.line(x0, y0, x0 + ca * length * 0.8, y0 + sa * length * 0.8, 0.6, v * 0.6, 0.0, carve=True)


def _feather_fan(L, rx, ry, angles, length, width=6.0, v=1.0):
    """Feathers fanning from a root, the outer ones longest, alternate ones a shade dimmer."""
    n = len(angles)
    for i, a in enumerate(angles):
        ln = length * (0.62 + 0.38 * i / max(1, n - 1))
        _feather(L, rx, ry, a, ln, width, v * (1.0 if i % 2 else 0.8))


def motif_sigil(L, cx, cy):
    # After the grimoire's seal of Lucifer: a cup over a cross, horns above, crosses at its lips.
    L.path([(cx - 30, cy - 18), (cx + 30, cy - 18), (cx, cy + 16)], 3.0, 1.0, 1.4, closed=True)
    L.line(cx, cy + 16, cx, cy + 44, 3.0)
    L.line(cx - 12, cy + 30, cx + 12, cy + 30, 3.0)
    L.line(cx - 7, cy + 38, cx + 7, cy + 38, 2.0)
    L.path([(cx - 20, cy - 44), (cx, cy - 26), (cx + 20, cy - 44)], 3.0)
    L.path([(cx - 16, cy - 40), (cx - 26, cy - 50)], 2.0)
    L.path([(cx + 16, cy - 40), (cx + 26, cy - 50)], 2.0)
    for sx in (-1, 1):
        x0, y0 = cx + sx * 30, cy - 18
        L.line(x0, y0 - 8, x0, y0 + 8, 2.0)
        L.line(x0 - 6, y0, x0 + 6, y0, 2.0)
        L.disc(x0 + sx * 10, y0 + 16, 3.0)
    L.line(cx - 8, cy - 8, cx + 8, cy + 4, 2.0)
    L.line(cx + 8, cy - 8, cx - 8, cy + 4, 2.0)


def motif_falling_wing(L, cx, cy):
    # One wing tumbling down and to the left, its primaries trailing, embers rising off it.
    arm = [(cx + 34, cy - 30), (cx + 8, cy - 18), (cx - 22, cy + 6)]
    for i in range(9):
        f = i / 8
        seg = 0 if f < 0.5 else 1
        g = f * 2 - seg
        px = arm[seg][0] + (arm[seg + 1][0] - arm[seg][0]) * g
        py = arm[seg][1] + (arm[seg + 1][1] - arm[seg][1]) * g
        _feather(L, px, py, math.radians(105 - 50 * f), 22 + 30 * f, 8 - 2 * f, 0.8 if i % 2 else 1.0)
    for i in range(5):
        _feather(L, arm[1][0] + 10 - i * 7, arm[1][1] - 6 + i * 5, math.radians(110 - 10 * i), 16, 7, 0.95 if i % 2 else 0.75)
    L.path(arm, 3.6, 1.0, 1.2)
    for k in range(8):
        L.disc(cx + 38 + (k % 3) * 3 - k, cy - 36 - k * 4.6, 2.4 - k * 0.22, 1.0 - k * 0.09, 1.4)


def motif_rimed_bars(L, cx, cy):
    L.line(cx - 44, cy - 44, cx + 44, cy - 44, 4.0)
    L.line(cx - 44, cy + 46, cx + 44, cy + 46, 4.0)
    for k in range(5):
        x = cx - 36 + k * 18
        L.line(x, cy - 44, x, cy + 46, 5.0)
        # Icicles under the top rail and frost along the bar.
        L.poly([(x + 5, cy - 42), (x + 12, cy - 42), (x + 8.5, cy - 42 + 10 + (k * 7) % 9)], 0.85)
        for j in range(5):
            fy = cy - 30 + j * 16 + (k * 5) % 7
            L.line(x - 4, fy, x - 1, fy - 3, 1.2, 0.8, 0.8)
            L.line(x + 1, fy - 3, x + 4, fy, 1.2, 0.8, 0.8)


def motif_winged_sun(L, cx, cy):
    L.disc(cx, cy - 6, 14, 1.0, 1.5)
    L.ring(cx, cy - 6, 20, 2.0)
    for k in range(16):
        a = math.radians(k * 22.5)
        r1 = 34 if k % 2 == 0 else 28
        L.line(cx + 23 * math.cos(a), cy - 6 + 23 * math.sin(a), cx + r1 * math.cos(a), cy - 6 + r1 * math.sin(a), 2.0)
    for side in (-1, 1):
        rx = cx + side * 22
        _feather_fan(L, rx, cy + 2, [math.pi / 2 - side * (math.pi / 2 + 0.24 * j) for j in range(-1, 5)], 36, 8.0, 0.9)


def motif_shackle(L, cx, cy):
    L.ring(cx, cy - 16, 22, 8.0, 1.0, 1.4)
    L.disc(cx, cy + 6, 6, 1.0, 1.2)
    # The chain hanging from it, its last link broken open.
    for k in range(3):
        y = cy + 18 + k * 13
        if k % 2 == 0:
            L.disc(cx, y, 0, rx=5.5, ry=8.0)
            L.disc(cx, y, 0, 0.0, 0.0, carve=True, rx=2.2, ry=4.6)
        else:
            L.line(cx, y - 7, cx, y + 7, 4.0)
    L.path([(cx + 2, cy + 55), (cx + 6, cy + 60), (cx + 3, cy + 64)], 3.0)
    L.path([(cx - 3, cy + 57), (cx - 7, cy + 62)], 3.0)


def _tongue(cx, base_y, w, h, lean):
    """A tongue of flame: wide at its base, leaning as it rises to a point."""
    outline = []
    for k in range(0, 21):
        f = k / 20
        y = base_y - h * f
        half = w / 2 * (math.sin(math.pi * (0.15 + 0.85 * (1 - f))) if f > 0.15 else 1) * (1 - f) ** 0.4
        outline.append((cx - half + lean * f * f, y))
    for k in range(20, -1, -1):
        f = k / 20
        y = base_y - h * f
        half = w / 2 * (math.sin(math.pi * (0.15 + 0.85 * (1 - f))) if f > 0.15 else 1) * (1 - f) ** 0.4
        outline.append((cx + half + lean * f * f, y))
    return outline


def motif_flames(L, cx, cy):
    for dx, w, h, lean, v in ((-22, 26, 58, -10, 0.75), (22, 26, 62, 12, 0.75), (0, 34, 92, 6, 0.9)):
        L.poly(_tongue(cx + dx, cy + 44, w, h, lean), v)
        L.poly(_tongue(cx + dx + lean * 0.1, cy + 44, w * 0.5, h * 0.62, lean * 0.6), 1.0)


def motif_crystal(L, cx, cy):
    for k in range(6):
        a = math.radians(90 + k * 60)
        ex, ey = cx + 50 * math.cos(a), cy + 50 * math.sin(a)
        L.line(cx, cy, ex, ey, 4.0)
        for f, ln in ((0.45, 14), (0.72, 10)):
            px, py = cx + (ex - cx) * f, cy + (ey - cy) * f
            for s in (-1, 1):
                b = a + s * math.radians(55)
                L.line(px, py, px + ln * math.cos(b), py + ln * math.sin(b), 2.4)
        L.disc(ex, ey, 3.0)
    hexagon = [(cx + 13 * math.cos(math.radians(30 + 60 * k)), cy + 13 * math.sin(math.radians(30 + 60 * k))) for k in range(6)]
    L.path(hexagon, 2.4, closed=True)


def _eye(L, cx, cy, w, h, v=1.0):
    """An almond eye with a slit pupil (the pupil carved out)."""
    pts = []
    for k in range(24):
        a = math.pi * 2 * k / 24
        pts.append((cx + w * math.cos(a), cy + h * math.sin(a) * (1 - 0.35 * abs(math.cos(a)))))
    L.disc(cx, cy, 0, v * 0.25, 4.0, rx=w * 1.5, ry=h * 2.4)
    L.poly(pts, v)
    L.disc(cx, cy, 0, 0.0, 0.0, carve=True, rx=max(1.0, w * 0.14), ry=h * 0.9)


def motif_hounds(L, cx, cy):
    for ex, ey, s in ((-22, -28, 1.3), (26, -14, 1.0), (-4, 16, 1.7), (32, 38, 0.8), (-34, 32, 0.75)):
        for side in (-1, 1):
            _eye(L, cx + ex + side * 11 * s, cy + ey, 7 * s, 3.6 * s, 0.55 + 0.45 * min(1.0, s / 1.5))


def motif_falling_star(L, cx, cy):
    sx, sy = cx + 22, cy - 24
    tx, ty = cx - 50, cy + 50
    for k in range(24):
        f0, f1 = k / 24, (k + 1) / 24
        w = 14 * (1 - f0) + 2
        L.line(sx + (tx - sx) * f0, sy + (ty - sy) * f0, sx + (tx - sx) * f1, sy + (ty - sy) * f1, w, 0.55 * (1 - f0) ** 1.5, 3.0)
        L.line(sx + (tx - sx) * f0, sy + (ty - sy) * f0, sx + (tx - sx) * f1, sy + (ty - sy) * f1, w * 0.3, (1 - f0) ** 1.2, 1.5)
    pts = []
    for k in range(10):
        a = math.radians(-90 + 12 + k * 36)
        r = 19 if k % 2 == 0 else 7.5
        pts.append((sx + r * math.cos(a), sy + r * math.sin(a)))
    L.disc(sx, sy, 14, 0.35, 8.0)
    L.poly(pts, 1.0)
    L.line(sx - 30, sy, sx + 30, sy, 1.4, 0.7, 2.0)
    L.line(sx, sy - 30, sx, sy + 30, 1.4, 0.7, 2.0)
    for k in range(7):
        f = 0.2 + k * 0.1
        off = (8 if k % 2 else -8) * (1 - f)
        L.disc(sx + (tx - sx) * f + off, sy + (ty - sy) * f + off * 0.6, 1.8 - k * 0.15, 0.9 - k * 0.08, 1.0)


def motif_six_wings(L, cx, cy):
    for lift, length in ((0.9, 46), (0.05, 52), (-0.8, 42)):
        for side in (-1, 1):
            base = math.pi / 2 - side * math.pi / 2  # outward: 0 for right, pi for left
            direction = base - side * lift
            angles = [direction + side * (j - 2) * 0.17 for j in range(5)]
            _feather_fan(L, cx + side * 8, cy - lift * 6, angles, length, 9.0, 0.9)
    _eye(L, cx, cy, 10, 6, 1.0)
    L.ring(cx, cy - 2, 16, 1.6, 0.8)


def motif_padlock(L, cx, cy):
    L.ring(cx, cy - 12, 20, 7.0, 1.0, 1.4)
    L.disc(cx, cy - 12, 0, 0.0, 0.0, carve=True, rx=16.5, ry=16.5)
    L.poly([(cx - 30, cy - 8), (cx + 30, cy - 8), (cx + 30, cy + 40), (cx - 30, cy + 40)], 0.85)
    L.path([(cx - 30, cy - 8), (cx + 30, cy - 8), (cx + 30, cy + 40), (cx - 30, cy + 40)], 2.4, 1.0, 1.2, closed=True)
    L.disc(cx, cy + 10, 6, 0.0, 0.6, carve=True)
    L.poly([(cx - 3.5, cy + 12), (cx + 3.5, cy + 12), (cx + 6, cy + 28), (cx - 6, cy + 28)], carve=True)
    for x in (cx - 24, cx + 24):
        for y in (cy - 2, cy + 34):
            L.disc(x, y, 2.0, 0.0, 0.6, carve=True)


MOTIFS = [motif_sigil, motif_falling_wing, motif_rimed_bars, motif_winged_sun, motif_shackle, motif_flames, motif_crystal,
          motif_hounds, motif_falling_star, motif_six_wings, motif_padlock]


def title_motifs():
    L = Lum(512, 384)
    for i, draw in enumerate(MOTIFS):
        draw(L, (i % 4) * 128 + 64, (i // 4) * 128 + 64)
    return L.tex(9950)


def title_chain():
    t = Tex(256, 24, 9960)
    cy = 11.5
    # Edge-on links first (behind), then the face-on links over them.
    for k in range(9):
        x0 = k * 32 - 11
        for x in range(x0, x0 + 22):
            for y in range(int(cy) - 2, int(cy) + 3):
                if 0 <= x < 256:
                    e = y - (int(cy) - 2)
                    t.set(x, y, IRON[0] if e in (0, 4) or x in (x0, x0 + 21) else iron(5 - e))
    for k in range(8):
        lx = 16 + k * 32
        for y in range(24):
            for x in range(lx - 13, lx + 14):
                q = math.hypot((x + 0.5 - lx) / 12.5, (y + 0.5 - cy) / 9.5)
                hole = math.hypot((x + 0.5 - lx) / 7.0, (y + 0.5 - cy) / 3.6)
                if q <= 1 and hole > 1:
                    edge = q > 0.9 or hole < 1.18
                    lit = (cy - (y + 0.5)) / 9.5
                    t.set(x, y, IRON[0] if edge else iron(3.2 + 2.2 * lit + (fbm(x, y, 9961, 256, 24, 2, 16.0) - 0.5)))
    return t


# --- generate ------------------------------------------------------------------------------------------------

def glyph_json(widths):
    data = {"chars": CHARSET}
    for name, a in ATLASES.items():
        data[name] = {"cell": [a["cw"], a["ch"]], "cols": a["cols"], "baseline": a["top"] + int(round(10 * a["unit"])),
                      "advance": widths[name]}
    return data


TEXTURES_OUT = {"bar_frame": bar_frame, "bar_runes": bar_runes, "bar_bars": bar_bars, "debris": debris,
                "title_seal": title_seal, "title_motifs": title_motifs, "title_chain": title_chain}


def generate():
    for name, fn in TEXTURES_OUT.items():
        save(fn(), "gui/lucifer", name)
    for name in FILLS:
        save(fill(name), "gui/lucifer", "fill_" + name)
    widths = {}
    for name in ATLASES:
        t, w = glyph_atlas(name)
        save(t, "gui/lucifer", "glyphs_" + name)
        widths[name] = w
    path = os.path.join(TEXTURES, "gui", "lucifer", "glyphs.json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(glyph_json(widths), f, separators=(", ", ": "))
        f.write("\n")
    WRITTEN.append(path)


if __name__ == "__main__":
    generate()
