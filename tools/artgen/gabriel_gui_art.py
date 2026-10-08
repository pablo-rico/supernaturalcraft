"""Gabriel's HUD textures (v0.14), textures/gui/gabriel/, laid out as GabrielAssets and the client's HUD read them:

  sign_laugh / sign_applause / sign_on_air  128x32  lit studio signs: a bulb-framed box (red / amber / red) with the word
  osd        128x16   green on-screen-display type on clear: "CH" at x 0..15, digit n at x 16 + n*11 (11 wide)
  static     128x128  four 64x64 frames (2x2) of grey TV noise, opaque (tinted and scrolled by the HUD)
  quiz_panel 256x96   a game-show panel: the question band x 8..248, y 8..40; three answer slots RED / BLUE / YELLOW at
                      x 4..84, 88..168, 172..252, y 52..88 (text by code)
  bar        256x32   the programme banner: a dark band for the name (y 6..18), an unlit lamp at x 6..20, y 6..18, and an
                      empty dark track x 8..247, y 24..30 that the code fills
"""

import math
import random

from common import save
from pixelkit import Tex, hexc, mix, shade

CLEAR = (0, 0, 0, 0)

FONT = {  # 5 x 7
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "C": [".####", "#....", "#....", "#....", "#....", "#....", ".####"],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "G": [".####", "#....", "#....", "#.###", "#...#", "#...#", ".###."],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "L": ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    "N": ["#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "U": ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    " ": ["....."] * 7,
    "0": [".###.", "#..##", "#.#.#", "#.#.#", "#.#.#", "##..#", ".###."],
    "1": ["..#..", ".##..", "#.#..", "..#..", "..#..", "..#..", "#####"],
    "2": [".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"],
    "3": ["####.", "....#", "....#", ".###.", "....#", "....#", "####."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": [".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."],
}


def blend(t, x, y, c, a=1.0):
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[int(y)][int(x)]
    a = min(1.0, a) * (c[3] / 255 if len(c) > 3 else 1)
    if d[3] == 0:
        t.rows[int(y)][int(x)] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    rgb = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3))
    t.rows[int(y)][int(x)] = rgb + (int(255 * oa),)


def text_mask(text, sx, sy, gap):
    """The pixels of `text` in the 5x7 font scaled by (sx, sy), letters `gap` apart: (set of (x, y), width, height)."""
    pts, x0 = set(), 0
    for ch in text:
        g = FONT[ch]
        for gy, row in enumerate(g):
            for gx, b in enumerate(row):
                if b == "#":
                    for dy in range(sy):
                        for dx in range(sx):
                            pts.add((x0 + gx * sx + dx, gy * sy + dy))
        x0 += 5 * sx + gap
    return pts, x0 - gap, 7 * sy


def rounded_box(t, x0, y0, x1, y1, r, fn):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dx = max(x0 + r - x, 0, x - (x1 - r))
            dy = max(y0 + r - y, 0, y - (y1 - r))
            if dx * dx + dy * dy <= r * r:
                t.set(x, y, fn(x, y))


# --- the studio signs ----------------------------------------------------------------------------------------------------

def sign(word, hue, sx, sy, gap):
    t = Tex(128, 32, 1)
    deep, mid, hot = hue
    rounded_box(t, 0, 0, 127, 31, 4, lambda x, y: shade(deep, 0.55))
    rounded_box(t, 2, 2, 125, 29, 3, lambda x, y: mix(deep, shade(deep, 0.6), y / 31))
    # The lit word: a hot core, a coloured glow round it.
    pts, w, h = text_mask(word, sx, sy, gap)
    ox, oy = (128 - w) // 2, (32 - h) // 2
    for (x, y) in pts:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.hypot(dx, dy)
                if 0 < d <= 2.3:
                    blend(t, ox + x + dx, oy + y + dy, mid, 0.42 * (1 - d / 2.6))
    for (x, y) in pts:
        edge = any((x + dx, y + dy) not in pts for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        t.set(ox + x, oy + y, hot if not edge else mix(hot, mid, 0.35))
    # Bulbs all round the frame.
    bulbs = [(x, 2) for x in range(5, 124, 7)] + [(x, 29) for x in range(5, 124, 7)] + [(2, y) for y in (9, 16, 23)] + \
            [(125, y) for y in (9, 16, 23)]
    for (bx, by) in bulbs:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.hypot(dx, dy)
                if d <= 1.6:
                    t.set(bx + dx, by + dy, mix(hexc("#fffbe6"), hexc("#ffd36a"), d / 1.6))
                elif d <= 2.6:
                    blend(t, bx + dx, by + dy, hexc("#ffcf6a"), 0.35)
    return t


# --- the on-screen display -----------------------------------------------------------------------------------------------

C7 = [".######", "##.....", "##.....", "##.....", "##.....", "##.....", ".######"]
H7 = ["##...##", "##...##", "##...##", "#######", "##...##", "##...##", "##...##"]
OSD_GREEN = hexc("#5cff6a")
OSD_DARK = hexc("#0b3a12", 200)


def osd():
    t = Tex(128, 16, 2)
    cells = [(0, None)] + [(16 + n * 11, str(n)) for n in range(10)]
    for x0, ch in cells:
        pts = set()
        if ch is None:
            for gx0, g in ((0, C7), (8, H7)):
                for gy, row in enumerate(g):
                    for gx, b in enumerate(row):
                        if b == "#":
                            pts |= {(gx0 + gx, 1 + gy * 2), (gx0 + gx, 2 + gy * 2)}
        else:
            g = FONT[ch]
            for gy, row in enumerate(g):
                for gx, b in enumerate(row):
                    if b == "#":
                        pts |= {(gx * 2 + dx, 1 + gy * 2 + dy) for dx in (0, 1) for dy in (0, 1)}
        for (x, y) in pts:   # a dark drop shadow, one down and right
            if (x + 1, y + 1) not in pts:
                t.set(x0 + x + 1, y + 1, OSD_DARK)
        for (x, y) in pts:
            t.set(x0 + x, y, OSD_GREEN if y % 2 else mix(OSD_GREEN, hexc("#e6ffe8"), 0.35))
    return t


# --- static --------------------------------------------------------------------------------------------------------------

def static():
    t = Tex(128, 128, 3)
    for fi in range(4):
        rng = random.Random(7300 + fi)
        ox, oy = (fi % 2) * 64, (fi // 2) * 64
        streaks = {rng.randrange(64): rng.uniform(0.15, 0.45) for _ in range(5)}
        for y in range(64):
            row_bias = streaks.get(y, 0.0) + (0.08 if y % 2 else 0.0)
            for x in range(64):
                v = rng.random() ** 1.3
                v = min(1.0, v + row_bias)
                if y % 2:
                    v *= 0.82
                g = int(30 + 210 * v)
                t.set(ox + x, oy + y, (g, g, g, 255))
    return t


# --- the quiz panel ------------------------------------------------------------------------------------------------------

SLOTS = ((4, 84, hexc("#d8262e")), (88, 168, hexc("#2468e0")), (172, 252, hexc("#f2c418")))


def quiz_panel():
    t = Tex(256, 96, 4)
    rounded_box(t, 0, 0, 255, 95, 6, lambda x, y: hexc("#8a6418"))
    rounded_box(t, 1, 1, 254, 94, 5, lambda x, y: mix(hexc("#ffe08a"), hexc("#b8861e"), y / 95))
    rounded_box(t, 3, 3, 252, 92, 4, lambda x, y: mix(hexc("#1a1440"), hexc("#0a0820"), y / 95))
    # The question band: a deep blue screen with a soft top light, a gold rule round it.
    rounded_box(t, 6, 6, 249, 42, 3, lambda x, y: hexc("#e0b84c"))
    rounded_box(t, 8, 8, 247, 40, 2, lambda x, y: mix(hexc("#2b3c9a"), hexc("#101850"), (y - 8) / 32) if (y % 2 == 0)
                else shade(mix(hexc("#2b3c9a"), hexc("#101850"), (y - 8) / 32), 0.92))
    # Three answer slots, a bevelled tile each with a light along the top.
    for (x0, x1, col) in SLOTS:
        rounded_box(t, x0, 52, x1, 88, 4, lambda x, y: shade(col, 0.45))
        rounded_box(t, x0 + 2, 54, x1 - 2, 86, 3, lambda x, y, col=col: mix(shade(col, 1.18), shade(col, 0.72), (y - 54) / 32))
        for x in range(x0 + 6, x1 - 5):
            blend(t, x, 56, hexc("#ffffff"), 0.45)
    # Marquee bulbs along the bottom edge and between the slots.
    for x in range(10, 250, 10):
        for (dx, dy, a) in ((0, 0, 1.0), (1, 0, 0.6), (0, 1, 0.6)):
            blend(t, x + dx, 46 + dy, hexc("#fff3c0"), a)
    return t


# --- the programme banner ------------------------------------------------------------------------------------------------

def bar():
    t = Tex(256, 32, 5)
    rounded_box(t, 0, 0, 255, 31, 4, lambda x, y: hexc("#6e5014"))
    rounded_box(t, 1, 1, 254, 30, 3, lambda x, y: mix(hexc("#ffe08a"), hexc("#a8781a"), y / 31))
    # The banner slab: dark violet, a chrome line along its top.
    rounded_box(t, 3, 3, 252, 21, 2, lambda x, y: mix(hexc("#2a1f44"), hexc("#120c22"), (y - 3) / 18))
    for x in range(5, 251):
        blend(t, x, 4, hexc("#c9c2e8"), 0.5)
    # The name band y 6..18 stays plain; an unlit ON AIR lamp at x 6..20.
    rounded_box(t, 6, 6, 20, 18, 3, lambda x, y: hexc("#3a0c0c"))
    rounded_box(t, 8, 8, 18, 16, 2, lambda x, y: mix(hexc("#6a1414"), hexc("#2a0606"), math.hypot(x - 11, y - 10) / 7))
    # The track: x 8..247, y 24..30, dark and empty, in a gold rim.
    for y in range(22, 32):
        for x in range(6, 250):
            inner = 8 <= x <= 247 and 24 <= y <= 30
            if inner:
                t.set(x, y, hexc("#0c0814"))
            elif y < 31:
                t.set(x, y, hexc("#3a2a0c") if (x in (6, 7, 248, 249) or y in (22, 23)) else t.rows[y][x])
    return t


def generate():
    red = (hexc("#5a0a0e"), hexc("#ff3b30"), hexc("#fff0e0"))
    amber = (hexc("#5a3006"), hexc("#ffa020"), hexc("#fff6e0"))
    save(sign("LAUGH", red, 3, 3, 3), "gui/gabriel", "sign_laugh")
    save(sign("APPLAUSE", amber, 2, 3, 2), "gui/gabriel", "sign_applause")
    save(sign("ON AIR", red, 3, 3, 2), "gui/gabriel", "sign_on_air")
    save(osd(), "gui/gabriel", "osd")
    save(static(), "gui/gabriel", "static")
    save(quiz_panel(), "gui/gabriel", "quiz_panel")
    save(bar(), "gui/gabriel", "bar")


if __name__ == "__main__":
    generate()
