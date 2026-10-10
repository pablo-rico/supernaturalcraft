"""v0.18 GUI atlas (HeavenAssets.GUI_ATLAS = textures/gui/heaven.png, 256 x 512): QTE overlay, form HUD, docket, memory toasts,
Ash's menu, the book's Memories tab. 2D art work. Text is always drawn by the code (lang), except the three rubber-stamp
impressions (APPROVED / DENIED / OVERDUE), which are the stamp itself.

Regions (x,y w x h; checked against REGIONS below, which must not overlap):

  ash_panel         0,0 200x140   Ash's menu: a dark-stained roadhouse board, brass corners; header band y 8..24 (title text,
                                  an amber neon line under it at y 25), hints inset x 10..190 y 30..76, list inset x 10..190
                                  y 82..130 (both dark, for text and buttons)
  tab_memories      200,0 16x16   the book's Memories tab icon: a gold-framed snapshot of a sunlit hill
  tab_memories_lit  216,0 16x16   the same, selected / hovered (brighter, a white glint)
  memory_icon       232,0 16x16   a memory: a white-gold orb of light with a four-pointed star (toasts, the book)
  ash_button        200,16 56x18  a button: dark leather in a brass edge (label by code, centred)
  ash_button_hover  200,34 56x18  hovered: amber edge, lighter leather
  ash_button_off    200,52 56x18  disabled: grey, flat
  bullet_pending    200,72 8x8    docket entry still to come: a hollow ink circle
  bullet_next       208,72 8x8    the next entry: a gold dot
  bullet_done       216,72 8x8    done: an ink tick
  bullet_revised    224,72 8x8    revised: a red cross
  ash_check_off     232,72 10x10  a checkbox (visitors welcome), empty
  ash_check_on      242,72 10x10  ticked (amber)
  memory_toast      0,140 160x32  memory toast frame (vanilla toast size): ivory, double gold border; icon slot at 8,8 16x16
                                  (a faint gold ring), text from x 30
  qte_frame         0,172 184x30  the chair QTE: a white clinical bar with cyan corner lights; label band y 2..9 (text by
                                  code), dark track x 4..179 y 13..22 where the fill goes, leather strap marks under it
  qte_key           184,172 32x18 a long blank keycap (the jump key's name drawn by code), up
  qte_key_down      216,172 32x18 the same keycap pressed (1 px lower, darker)
  qte_fill          0,202 176x10  the struggle fill: white-gold light with a sheen (draw clipped from the left)
  qte_fill_danger   0,212 176x10  the same in warning red (time running out)
  form_card         0,224 104x64  the Heavenly Form HUD card: ivory stationery, gold letterhead band y 3..13 (seal at 5,4 8x8,
                                  title text x 16..74), number box x 80..99 y 3..22 (a numeral_n goes at 82,5), ruled lines
                                  y 30/38/46, signature rule y 56 x 50..98
  stamp_approved    104,224 72x22 the green APPROVED stamp impression (draw over the card, may be tilted by the code)
  stamp_denied      104,246 72x22 the red DENIED stamp
  stamp_overdue     176,224 72x22 the orange-red OVERDUE stamp
  numeral_1         176,246 16x16 gold numeral I (the form's number; also the cabinets')
  numeral_2         192,246 16x16 II
  numeral_3         208,246 16x16 III
  numeral_4         224,246 16x16 IV
  docket_panel      0,288 136x112 the docket: a clipboard with a steel clip, a sheet with a title band y 16..28 (text by
                                  code) and five entry rows at y 34, 50, 66, 82, 98 (14 high: bullet at x 12, text from x 24
                                  to 124)
  docket_strike     136,288 112x6 a red ink strike-through stroke (draw over an entry's text, from x 22)
  docket_row_lit    136,296 112x14 the next entry's highlight (a soft gold wash, a marker on its left)
  progress_frame    136,312 104x10 the plot-build progress bar frame (gold, inner x 2..101 y 2..7)
  progress_fill     136,322 100x6 its fill (white-gold), drawn clipped from the left
"""

import math

from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

CLEAR = (0, 0, 0, 0)
W, H = 256, 512

REGIONS = {
    "ash_panel": (0, 0, 200, 140),
    "tab_memories": (200, 0, 16, 16),
    "tab_memories_lit": (216, 0, 16, 16),
    "memory_icon": (232, 0, 16, 16),
    "ash_button": (200, 16, 56, 18),
    "ash_button_hover": (200, 34, 56, 18),
    "ash_button_off": (200, 52, 56, 18),
    "bullet_pending": (200, 72, 8, 8),
    "bullet_next": (208, 72, 8, 8),
    "bullet_done": (216, 72, 8, 8),
    "bullet_revised": (224, 72, 8, 8),
    "ash_check_off": (232, 72, 10, 10),
    "ash_check_on": (242, 72, 10, 10),
    "memory_toast": (0, 140, 160, 32),
    "qte_frame": (0, 172, 184, 30),
    "qte_key": (184, 172, 32, 18),
    "qte_key_down": (216, 172, 32, 18),
    "qte_fill": (0, 202, 176, 10),
    "qte_fill_danger": (0, 212, 176, 10),
    "form_card": (0, 224, 104, 64),
    "stamp_approved": (104, 224, 72, 22),
    "stamp_denied": (104, 246, 72, 22),
    "stamp_overdue": (176, 224, 72, 22),
    "numeral_1": (176, 246, 16, 16),
    "numeral_2": (192, 246, 16, 16),
    "numeral_3": (208, 246, 16, 16),
    "numeral_4": (224, 246, 16, 16),
    "docket_panel": (0, 288, 136, 112),
    "docket_strike": (136, 288, 112, 6),
    "docket_row_lit": (136, 296, 112, 14),
    "progress_frame": (136, 312, 104, 10),
    "progress_fill": (136, 322, 100, 6),
}

GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
LIGHT = Ramp("#b98a2c", "#d9ad4a", "#f0cf72", "#fae6a6", "#fff6d8", "#ffffff")
IVORY = Ramp("#8f897c", "#b4ad9e", "#d2ccbd", "#e6e1d4", "#f3f0e7", "#fffdf8")
WOOD = Ramp("#120b07", "#1e130c", "#2b1c12", "#3a2618", "#4c3220", "#5f412a")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
LEATHER = Ramp("#170e0a", "#24160f", "#331f15", "#43291c", "#563525", "#6b4330")
AMBER = hexc("#ffb347")
CLINIC = Ramp("#5d666c", "#8a949a", "#b3bcc1", "#d3dadd", "#e9eef0", "#ffffff")
CYAN = Ramp("#062027", "#0b3540", "#11535f", "#1a8a96", "#4fd0dc", "#c8fbff")
INK = hexc("#262a33")
RED_INK = hexc("#b3241e")

FONT = {  # 5 x 7, only the letters the stamps need
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "D": ["####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "N": ["#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "U": ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "V": ["#...#", "#...#", "#...#", "#...#", ".#.#.", ".#.#.", "..#.."],
}


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


def _h(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 362437) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFFFF) / 0xFFFFFFFF


class Region:
    """Draws into the atlas in a region's own coordinates; never outside it."""

    def __init__(self, t, name):
        self.t = t
        self.x0, self.y0, self.w, self.h = REGIONS[name]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.t.set(self.x0 + x, self.y0 + y, c)

    def get(self, x, y):
        return self.t.get(self.x0 + x, self.y0 + y)

    def blend(self, x, y, c, a):
        if not (0 <= x < self.w and 0 <= y < self.h) or a <= 0:
            return
        d = self.get(x, y)
        a = min(1.0, a)
        if d[3] == 0:
            self.set(x, y, (c[0], c[1], c[2], int(255 * a)))
            return
        da = d[3] / 255
        oa = a + da * (1 - a)
        rgb = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3))
        self.set(x, y, rgb + (int(255 * oa),))

    def fill(self, fn):
        for y in range(self.h):
            for x in range(self.w):
                self.set(x, y, fn(x, y))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def text(self, s, x, y, c, bold=False):
        for ch in s:
            g = FONT.get(ch)
            if g:
                for gy, row in enumerate(g):
                    for gx, b in enumerate(row):
                        if b == "#":
                            self.set(x + gx, y + gy, c)
                            if bold:
                                self.set(x + gx + 1, y + gy, c)
            x += 7 if bold else 6
        return x


def rounded(x, y, w, h, r):
    """Whether (x, y) is inside a w x h box with corners cut to radius r (pixel-art rounding)."""
    cx = min(x, w - 1 - x)
    cy = min(y, h - 1 - y)
    if cx >= r or cy >= r:
        return True
    return (r - cx - 0.5) ** 2 + (r - cy - 0.5) ** 2 <= r * r


# =====================================================================================================
# Ash's Roadhouse
# =====================================================================================================

def wood_grain(x, y, seed, base=2.8):
    n = fbm(x * 0.25, y * 1.6, seed, 64, 64, 3, 6.0)
    streak = math.sin(y * 0.9 + math.sin(x * 0.05 + seed) * 2.5) * 0.35
    return tone(WOOD, base + (n - 0.5) * 1.6 + streak)


def ash_panel(t):
    r = Region(t, "ash_panel")
    w, h = r.w, r.h

    def f(x, y):
        if not rounded(x, y, w, h, 4):
            return None
        edge = min(x, y, w - 1 - x, h - 1 - y)
        if edge == 0:
            return WOOD[0]
        if edge < 7:
            # The frame: planks with a bevel, lit from the top-left.
            c = wood_grain(x, y, 21001, 3.4)
            if edge == 1:
                c = WOOD[5] if (x < w // 2 and y < h // 2) else WOOD[2]
            if edge == 6:
                c = WOOD[0]
            return c
        # The inside: a darker stained board.
        return wood_grain(x, y, 21002, 1.6)
    r.fill(f)
    # Header band (y 8..24) and its amber neon line (y 25).
    for y in range(8, 25):
        for x in range(8, w - 8):
            r.set(x, y, mix(wood_grain(x, y, 21003, 2.2), hexc("#3a1410"), 0.45))
    for x in range(10, w - 10):
        glow = 1 - abs(x - w / 2) / (w / 2)
        r.set(x, 25, mix(AMBER, hexc("#fff0c0"), 0.3 * glow))
        r.blend(x, 24, AMBER, 0.25)
        r.blend(x, 26, AMBER, 0.2)
    # Two insets for text and buttons.
    for (x0, y0, x1, y1) in ((10, 30, 190, 76), (10, 82, 190, 130)):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                c = mix(hexc("#0f0a08"), hexc("#1a1210"), fbm(x, y, 21004, 64, 64, 2, 8.0))
                if y == y0 or x == x0:
                    c = hexc("#070504")
                elif y == y1 or x == x1:
                    c = WOOD[3]
                r.set(x, y, c)
    # Brass corner caps with a nail each.
    for (cx, cy) in ((0, 0), (w - 12, 0), (0, h - 12), (w - 12, h - 12)):
        for y in range(12):
            for x in range(12):
                lx = x if cx == 0 else 11 - x
                ly = y if cy == 0 else 11 - y
                if lx + ly <= 12 and rounded(cx + x, cy + y, w, h, 4):
                    c = tone(BRASS, 4.2 - (x + y) / 9)
                    if lx + ly in (11, 12):
                        c = BRASS[1]
                    r.set(cx + x, cy + y, c)
        nx, ny = (cx + 4 if cx == 0 else cx + 7), (cy + 4 if cy == 0 else cy + 7)
        r.set(nx, ny, BRASS[5])
        r.set(nx + 1, ny + 1, BRASS[0])
        r.set(nx + 1, ny, BRASS[2])
    # Bottle-cap studs along the header's ends.
    for x in (14, w - 16):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            r.set(x + dx, 15 + dy, hexc("#c0392b") if (dx, dy) != (0, 0) else hexc("#ff8a7a"))


def ash_button(t, name, state):
    r = Region(t, name)
    w, h = r.w, r.h
    edge_c = {"normal": BRASS[3], "hover": AMBER, "off": hexc("#5a5550")}[state]

    def f(x, y):
        if not rounded(x, y, w, h, 3):
            return None
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return hexc("#0b0806")
        if e == 1:
            return edge_c if (y < h // 2 or state == "off") else mix(edge_c, hexc("#000000"), 0.35)
        if state == "off":
            return mix(hexc("#2e2b28"), hexc("#3a3632"), (y - 2) / (h - 4))
        base = 2.6 if state == "normal" else 3.6
        c = tone(LEATHER, base + 1.2 * (1 - y / h) + (fbm(x, y, 21011, 64, 32, 2, 8.0) - 0.5))
        if e == 2 and y == 2:
            c = mix(c, hexc("#ffffff"), 0.12)
        # Stitching one pixel in from the edge.
        if e == 3 and (x + y) % 3 == 0:
            c = mix(c, BRASS[4] if state == "hover" else BRASS[2], 0.6)
        return c
    r.fill(f)


def checkbox(t, name, on):
    r = Region(t, name)

    def f(x, y):
        e = min(x, y, 9 - x, 9 - y)
        if e == 0:
            return BRASS[3] if (x < 5 and y < 5) else BRASS[2]
        if e == 1:
            return hexc("#070504")
        return hexc("#1a1210")
    r.fill(f)
    if on:
        for (x, y) in ((2, 5), (3, 6), (4, 7), (5, 6), (6, 5), (7, 4), (7, 3), (3, 5), (4, 6), (5, 5), (6, 4)):
            r.set(x, y, AMBER)


# =====================================================================================================
# The book's Memories tab and the memory icon
# =====================================================================================================

def snapshot(t, name, lit):
    r = Region(t, name)
    sky = (hexc("#9ec9f0"), hexc("#d8ecff"))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = hexc("#3a2408")
            elif x in (1, 14) or y in (1, 14):
                c = GOLD[5] if (lit and (x + y) % 5 == 0) else (GOLD[4] if x == 1 or y == 1 else GOLD[2])
            else:
                # The picture: a pale sky, a sun, a green hill and a little figure on it.
                c = mix(sky[0], sky[1], (y - 2) / 10)
                if math.hypot(x - 10.5, y - 4.5) < 1.8:
                    c = hexc("#fff6c8")
                hill = 10.5 - 2.2 * math.cos((x - 6) / 5)
                if y >= hill:
                    c = hexc("#6fa85a") if y < hill + 1.2 else hexc("#4f8a44")
                if (x, y) in ((6, 7), (6, 8), (6, 9)):
                    c = hexc("#2a2a33")
                if lit:
                    c = mix(c, hexc("#fff8e0"), 0.18)
            r.set(x, y, c)
    if lit:
        r.set(12, 3, hexc("#ffffff"))
        r.set(3, 12, hexc("#ffffff"))


def memory_icon(t):
    r = Region(t, "memory_icon")
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.6:
                c = tone(LIGHT, 5.2 - d * 0.8 - ((x - 7.5) + (y - 7.5)) / 10)
                r.set(x, y, c)
            elif d < 7.2:
                r.set(x, y, (LIGHT[2][0], LIGHT[2][1], LIGHT[2][2], int(150 * (7.2 - d) / 1.6)))
    for k in range(-4, 5):
        a = 255 if abs(k) < 3 else 180
        r.set(7 + (k if k <= 0 else k), 7, (255, 255, 255, a))
        r.set(8 + (k if k >= 0 else k), 8, (255, 255, 255, a))
        r.set(7, 7 + k, (255, 255, 255, a))
        r.set(8, 8 + k, (255, 255, 255, a))


# =====================================================================================================
# The memory toast
# =====================================================================================================

def memory_toast(t):
    r = Region(t, "memory_toast")
    w, h = r.w, r.h

    def f(x, y):
        if not rounded(x, y, w, h, 4):
            return None
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return GOLD[1]
        if e == 1:
            return GOLD[4] if y < h // 2 else GOLD[3]
        if e == 2:
            return IVORY[5]
        if e == 3:
            return GOLD[3] if (x + y) % 2 == 0 else GOLD[2]
        glow = 1 - x / w
        return mix(IVORY[4], hexc("#fff4d6"), 0.5 * glow + 0.15 * fbm(x, y, 21021, 64, 32, 2, 8.0))
    r.fill(f)
    # The icon slot: a faint gold ring round 8,8 16x16.
    for y in range(5, 27):
        for x in range(5, 27):
            d = math.hypot(x - 15.5, y - 15.5)
            if 9.2 < d < 10.3:
                r.blend(x, y, GOLD[3], 0.55)
            elif d <= 9.2:
                r.blend(x, y, hexc("#fffbea"), 0.5)


# =====================================================================================================
# The chair's QTE
# =====================================================================================================

def qte_frame(t):
    r = Region(t, "qte_frame")
    w, h = r.w, r.h

    def f(x, y):
        if not rounded(x, y, w, h, 4):
            return None
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return CLINIC[0]
        if 13 <= y <= 22 and 4 <= x <= 179:
            if y == 13 or x == 4:
                return hexc("#05080a")
            # The dark track: a leather strap with stitching (hidden as the fill grows).
            c = tone(LEATHER, 1.6 + (fbm(x, y, 21031, 64, 16, 2, 8.0) - 0.5) * 1.2)
            if y in (15, 20) and x % 4 == 0:
                c = LEATHER[4]
            return c
        c = tone(CLINIC, 4.2 - y / h * 1.6)
        if e == 1 and y < h // 2:
            c = CLINIC[5]
        if y == 11 and 6 <= x <= w - 7:
            c = CLINIC[2]
        return c
    r.fill(f)
    # Cyan status lights in the corners of the label band.
    for x in (5, w - 8):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            r.set(x + dx, 4 + dy, CYAN[4] if (dx + dy) else CYAN[5])
        r.blend(x - 1, 4, CYAN[4], 0.3)
        r.blend(x + 2, 5, CYAN[4], 0.3)
    # Buckle plates at both ends of the strap.
    for x0 in (2, w - 6):
        for y in range(12, 24):
            for x in range(x0, x0 + 4):
                if y in (12, 23) or x in (x0, x0 + 3):
                    r.set(x, y, tone(CLINIC, 4.4 - (y - 12) / 6))


def qte_key(t, name, down):
    r = Region(t, name)
    w, h = r.w, r.h
    off = 1 if down else 0

    def f(x, y):
        if y < off:
            return None
        yy = y - off
        hh = h - off
        if not rounded(x, yy, w, hh, 3):
            return None
        e = min(x, yy, w - 1 - x, hh - 1 - yy)
        if e == 0:
            return hexc("#1c1f22")
        if yy >= hh - (2 if not down else 1) - 1:
            return CLINIC[1] if not down else CLINIC[0]         # the key's side
        c = tone(CLINIC, (4.4 if not down else 3.4) - yy / hh * 0.8)
        if e == 1 and yy < 3:
            c = CLINIC[5]
        return c
    r.fill(f)


def qte_fill(t, name, danger):
    r = Region(t, name)
    w, h = r.w, r.h
    base = Ramp("#5a0c0c", "#8f1a16", "#c2302a", "#e85a48", "#ff9a86", "#ffe0d8") if danger else LIGHT

    def f(x, y):
        v = 4.4 - abs(y - 3.0) * 0.7
        c = tone(base, v)
        if (x + y * 2) % 12 < 2:
            c = mix(c, hexc("#ffffff"), 0.35)                    # a moving sheen (scrolled by the code)
        if y == h - 1:
            c = base[1]
        return c
    r.fill(f)


# =====================================================================================================
# Zachariah's paperwork
# =====================================================================================================

def form_card(t):
    r = Region(t, "form_card")
    w, h = r.w, r.h

    def f(x, y):
        if not rounded(x, y, w, h, 2):
            return None
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return IVORY[0]
        c = mix(IVORY[4], IVORY[5], 0.5 + 0.5 * (fbm(x, y, 21041, 128, 64, 2, 16.0) - 0.5))
        if 3 <= y <= 13 and 3 <= x <= 76:
            c = mix(c, GOLD[4], 0.22)
            if y in (3, 13):
                c = GOLD[3]
        if y in (30, 38, 46) and 5 <= x <= w - 6:
            c = hexc("#b9c3d3")
        if y == 56 and 50 <= x <= 98:
            c = INK
        return c
    r.fill(f)
    # The seal at 5,4 (8x8): a gold disc with a star.
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d < 4:
                r.set(5 + x, 4 + y, GOLD[2] if d > 3 else (GOLD[5] if (x in (3, 4) or y in (3, 4)) else GOLD[4]))
    # The number box.
    for y in range(3, 23):
        for x in range(80, 100):
            if y in (3, 22) or x in (80, 99):
                r.set(x, y, GOLD[3])
            elif y in (4, 21) or x in (81, 98):
                r.set(x, y, GOLD[1])
            else:
                r.set(x, y, IVORY[5])
    # A dog-ear in the bottom right.
    for i in range(5):
        for j in range(5 - i):
            r.set(w - 1 - j, h - 1 - i, CLEAR if j < 4 - i else IVORY[1])
    for i in range(5):
        r.set(w - 5 + i, h - 1 - i, IVORY[2])


def stamp(t, name, word, ink):
    r = Region(t, name)
    w, h = r.w, r.h
    seed = sum(map(ord, word))

    def worn(x, y):
        return _h(x, y, seed) < 0.06 or fbm(x, y, seed, 72, 22, 2, 9.0) < 0.2

    def put(x, y):
        if not worn(x, y):
            r.set(x, y, (ink[0], ink[1], ink[2], 225 if _h(x, y, seed + 1) < 0.8 else 170))
    for x in range(1, w - 1):
        for y in (1, 2, h - 3, h - 2):
            put(x, y)
    for y in range(1, h - 1):
        for x in (1, 2, w - 3, w - 2):
            put(x, y)
    for x in range(5, w - 5):
        put(x, 4)
        put(x, h - 5)
    width = len(word) * 7 - 1
    x0 = (w - width) // 2
    for i, ch in enumerate(word):
        g = FONT[ch]
        for gy, row in enumerate(g):
            for gx, b in enumerate(row):
                if b == "#":
                    put(x0 + i * 7 + gx, 7 + gy)
                    put(x0 + i * 7 + gx + 1, 7 + gy)


def numeral(t, n):
    r = Region(t, f"numeral_{n}")
    strokes = {1: [7], 2: [5, 9], 3: [3, 7, 11], 4: [3]}[n]

    def stroke(x0):
        for y in range(3, 13):
            for x in range(x0, x0 + 2):
                r.set(x, y, GOLD[4] if x == x0 else GOLD[2])
        for x in range(x0 - 1, x0 + 3):                         # serifs
            r.set(x, 2, GOLD[5])
            r.set(x, 13, GOLD[2])
    for s in strokes:
        stroke(s)
    if n == 4:
        for y in range(3, 13):
            k = (y - 3) / 9 * 2.5
            r.set(round(7 + k), y, GOLD[4])
            r.set(round(8 + k), y, GOLD[2])
            r.set(round(13 - k), y, GOLD[4])
            r.set(round(12 - k), y, GOLD[2])
        for x in (6, 7, 8, 12, 13, 14):
            r.set(x, 2, GOLD[5])


def docket_panel(t):
    r = Region(t, "docket_panel")
    w, h = r.w, r.h

    def f(x, y):
        if not rounded(x, y, w, h, 4):
            return None
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return hexc("#1e140c")
        if e < 6:
            c = tone(Ramp("#2e1d10", "#4a3019", "#64431f", "#7d5628", "#966a34", "#ae7f42"),
                     3.0 + (fbm(x * 0.3, y * 2, 21051, 64, 64, 2, 6.0) - 0.5) * 1.6 + (0.8 if e == 1 and y < h / 2 else 0))
            return c
        # The sheet.
        c = mix(IVORY[4], IVORY[5], fbm(x, y, 21052, 128, 128, 2, 16.0))
        if 16 <= y <= 28:
            c = mix(c, GOLD[4], 0.18)
            if y == 28:
                c = GOLD[3]
        for ry in (32, 46, 60, 74):
            if y == ry + 13 and 10 <= x <= w - 10:
                c = hexc("#c4ccd8")
        if x == 20 and y >= 30 and y < h - 8:
            c = hexc("#e3a3a0")                                # the margin rule
        return c
    r.fill(f)
    # The steel clip at the top.
    cx = w // 2
    for y in range(0, 13):
        for x in range(cx - 18, cx + 18):
            inside = abs(x - cx) < 18 - max(0, 6 - y)
            if not inside:
                continue
            c = tone(CLINIC, 4.6 - y / 4 - (0.8 if abs(x - cx) > 15 else 0))
            if y == 12 or abs(x - cx) == 17 - max(0, 6 - y):
                c = CLINIC[0]
            r.set(x, y, c)
    for x in range(cx - 6, cx + 6):
        r.set(x, 3, CLINIC[0])
        r.set(x, 4, CLINIC[1])
    for (x, y) in ((cx - 10, 8), (cx + 9, 8)):
        r.set(x, y, CLINIC[5])
        r.set(x + 1, y + 1, CLINIC[0])


def docket_strike(t):
    r = Region(t, "docket_strike")
    for x in range(r.w):
        k = x / (r.w - 1)
        yc = 2.6 + 1.1 * math.sin(k * 7.0 + 0.4) * (0.5 + 0.5 * k)
        thick = 0.9 + 1.1 * math.sin(math.pi * k)
        for y in range(r.h):
            d = abs(y - yc)
            if d < thick:
                a = 0.95 if d < thick - 0.5 else 0.55
                if _h(x, y, 21061) < 0.08:
                    a *= 0.5
                r.blend(x, y, RED_INK, a)


def docket_row_lit(t):
    r = Region(t, "docket_row_lit")
    for y in range(r.h):
        for x in range(r.w):
            a = 0.28 * (1 - x / r.w) + 0.08
            if y in (0, r.h - 1):
                a *= 0.5
            r.set(x, y, (GOLD[4][0], GOLD[4][1], GOLD[4][2], int(255 * a)))
    for y in range(2, r.h - 2):
        r.set(0, y, GOLD[3])
        r.set(1, y, GOLD[4])


def bullets(t):
    pend = Region(t, "bullet_pending")
    nxt = Region(t, "bullet_next")
    done = Region(t, "bullet_done")
    rev = Region(t, "bullet_revised")
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if 2.4 < d < 3.5:
                pend.set(x, y, INK)
            if d < 3.4:
                nxt.set(x, y, GOLD[2] if d > 2.5 else (GOLD[5] if x + y < 6 else GOLD[4]))
    for (x, y) in ((1, 4), (2, 5), (3, 6), (4, 5), (5, 4), (6, 3), (7, 2), (2, 4), (3, 5), (6, 2)):
        done.set(x, y, INK)
    for i in range(1, 7):
        rev.set(i, i, RED_INK)
        rev.set(7 - i, i, RED_INK)
        rev.set(i + 1, i, RED_INK)
        rev.set(8 - i, i, RED_INK)


def progress(t):
    fr = Region(t, "progress_frame")
    for y in range(fr.h):
        for x in range(fr.w):
            if not rounded(x, y, fr.w, fr.h, 2):
                continue
            e = min(x, y, fr.w - 1 - x, fr.h - 1 - y)
            if e == 0:
                fr.set(x, y, GOLD[1])
            elif e == 1:
                fr.set(x, y, GOLD[4] if y < 5 else GOLD[2])
            else:
                fr.set(x, y, hexc("#1d1a26"))
    fl = Region(t, "progress_fill")
    for y in range(fl.h):
        for x in range(fl.w):
            c = tone(LIGHT, 5 - abs(y - 1.5) * 0.9)
            if (x + y) % 9 == 0:
                c = mix(c, hexc("#ffffff"), 0.4)
            fl.set(x, y, c)


# =====================================================================================================

def check_regions():
    names = list(REGIONS)
    for n in names:
        x, y, w, h = REGIONS[n]
        assert x >= 0 and y >= 0 and x + w <= W and y + h <= H, n
        assert f"{n} " in __doc__ and f"{x},{y} {w}x{h}" in __doc__, f"{n} is not documented as {x},{y} {w}x{h}"
    for i, a in enumerate(names):
        ax, ay, aw, ah = REGIONS[a]
        for b in names[i + 1:]:
            bx, by, bw, bh = REGIONS[b]
            assert ax + aw <= bx or bx + bw <= ax or ay + ah <= by or by + bh <= ay, f"{a} overlaps {b}"


def atlas():
    check_regions()
    t = Tex(W, H, 21000)
    ash_panel(t)
    ash_button(t, "ash_button", "normal")
    ash_button(t, "ash_button_hover", "hover")
    ash_button(t, "ash_button_off", "off")
    checkbox(t, "ash_check_off", False)
    checkbox(t, "ash_check_on", True)
    snapshot(t, "tab_memories", False)
    snapshot(t, "tab_memories_lit", True)
    memory_icon(t)
    bullets(t)
    memory_toast(t)
    qte_frame(t)
    qte_key(t, "qte_key", False)
    qte_key(t, "qte_key_down", True)
    qte_fill(t, "qte_fill", False)
    qte_fill(t, "qte_fill_danger", True)
    form_card(t)
    stamp(t, "stamp_approved", "APPROVED", hexc("#2f8f46"))
    stamp(t, "stamp_denied", "DENIED", hexc("#c0281f"))
    stamp(t, "stamp_overdue", "OVERDUE", hexc("#d0561c"))
    for n in range(1, 5):
        numeral(t, n)
    docket_panel(t)
    docket_strike(t)
    docket_row_lit(t)
    progress(t)
    return t


def generate():
    save(atlas(), "gui", "heaven")


if __name__ == "__main__":
    generate()
