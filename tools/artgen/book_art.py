"""The Hunter's Book: the open spread, the widget atlas, the roadmap's map paper and the spell
preview sprites. Sprite positions in book_widgets.png must match client/book/BookAtlas.java."""

import math
import random

import palette as P
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

INK = hexc("#3b2a1a")
FADED = hexc("#7a6448")
BRASS = Ramp("#2e1d07", "#5a3c10", "#8a6420", "#b48a34", "#d8b452", "#f4dc8a")
GREEN = Ramp("#0d1f11", "#1a3a20", "#27532d", "#386d3a", "#52894c", "#77a868")
VIOLET = Ramp("#120d2e", "#211852", "#312675", "#463a99", "#6458bb", "#9188da")
OCHRE = Ramp("#3a2608", "#664510", "#956619", "#bf8c28", "#dcb048", "#f2d785")
MANA = Ramp("#0f1c3f", "#1d3570", "#2c4f98", "#3d65b8", "#6189d4", "#a3c0ef")
SANITY = Ramp("#0f2614", "#1c4424", "#2b6334", "#3f8247", "#62a65f", "#9fd08c")
DANGER = Ramp("#2a0306", "#550a0e", "#7e1218", "#a01d22", "#c4352f", "#e8735e")
# v0.17: the Men of Letters' Archive -- dark walnut leather edged in brass.
WALNUT = Ramp("#140b06", "#24140a", "#3b2414", "#5a3a1e", "#8a6824", "#c9a54e")
TAB_RAMPS = (P.BLOOD, GREEN, VIOLET, OCHRE, WALNUT)


def over(t, x, y, c, a=1.0):
    """Alpha-composites c (scaled by a) over the pixel, transparent pixels included."""
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h):
        return
    sa = c[3] / 255 * max(0.0, min(1.0, a))
    if sa <= 0:
        return
    d = t.rows[y][x]
    da = d[3] / 255
    oa = sa + da * (1 - sa)
    rgb = tuple(int(round((c[i] * sa + d[i] * da * (1 - sa)) / oa)) for i in range(3))
    t.rows[y][x] = rgb + (int(round(oa * 255)),)


def tint(t, x, y, c, a):
    """Mixes an opaque pixel toward c, keeping its alpha."""
    if 0 <= x < t.w and 0 <= y < t.h and t.rows[y][x][3]:
        d = t.rows[y][x]
        t.rows[y][x] = mix(d, c[:3] + (d[3],), max(0.0, min(1.0, a)))


def with_alpha(c, a):
    return c[:3] + (int(round(max(0.0, min(1.0, a)) * 255)),)


def h01(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 362437) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFFFF) / 0xFFFFFFFF


# ---------------------------------------------------------------------------------------------
# the open book
# ---------------------------------------------------------------------------------------------

BW, BH = 416, 272
SPINE = 207.5          # between the pages' last columns (207 | 208)
COVER_R = 6


def _in_cover(x, y):
    """Inside the rounded cover rectangle? Returns the distance to its edge (or -1)."""
    r = COVER_R
    cx = min(max(x, r), BW - 1 - r)
    cy = min(max(y, r), BH - 1 - r)
    if (x, y) != (cx, cy):
        d = math.hypot(x - cx, y - cy)
        if d > r + 0.35:
            return -1
        return max(0, int(r + 0.35 - d))
    return min(x, y, BW - 1 - x, BH - 1 - y)


def _cover(t):
    for y in range(BH):
        for x in range(BW):
            e = _in_cover(x, y)
            if e < 0:
                continue
            n = fbm(x, y, 91, 512, 512, 4, 32.0)
            wear = fbm(x, y, 92, 512, 512, 3, 8.0)
            v = 2.0 + (n - 0.5) * 1.8
            if wear > 0.62:
                v += (wear - 0.62) * 4.0
            if h01(x, y, 93) < 0.04:
                v -= 0.6
            if e == 0:
                c = P.OUTLINE
            elif e == 1:
                c = P.LEATHER[3] if h01(x, y, 94) < 0.7 else P.LEATHER[4]
            elif e == 2:
                c = P.LEATHER[int(round(v + 0.4))]
            else:
                c = P.LEATHER[max(1, min(4, int(round(v))))]
            t.set(x, y, c)
    # Blind-tooled groove and a worn gold rule.
    for y in range(BH):
        for x in range(BW):
            e = _in_cover(x, y)
            if e == 4 and h01(x, y, 95) < 0.86:
                t.set(x, y, P.GOLD[2] if h01(x, y, 96) < 0.75 else P.GOLD[1])
            elif e == 5:
                tint(t, x, y, P.LEATHER[0], 0.45)
    # The spine's fold in the head and tail of the cover.
    for x in range(200, 216):
        d = abs(x + 0.5 - (SPINE + 0.5))
        a = max(0.0, 0.55 - d * 0.08)
        for y in range(BH):
            if _in_cover(x, y) >= 1:
                tint(t, x, y, P.LEATHER[0], a)


def _bow(x):
    """How far the page edge dips near the spine."""
    d = min(abs(x + 0.5 - (SPINE + 0.5)), 99)
    return int(round(max(0.0, (12 - d) / 12) ** 2 * 2.4))


def _page_box(side, sheet):
    """Columns and rows of a page sheet: sheet 0 is the top one, 1-2 peek out under it."""
    if side == 0:
        x0, x1 = 14 - 2 * sheet, 207
    else:
        x0, x1 = 208, 401 + 2 * sheet
    return x0, x1, 10, 259 + 2 * sheet


def _pages(t):
    P_ = P.PARCHMENT
    # Shadow the pages cast on the cover.
    for side in (0, 1):
        x0, x1, y0, y1 = _page_box(side, 2)
        for y in range(y0, y1 + 3):
            for x in range(x0 - 2, x1 + 3):
                if _in_cover(x, y) < 1:
                    continue
                inside = x0 <= x <= x1 and y0 <= y <= y1
                if not inside:
                    dx = max(x0 - x, x - x1, 0)
                    dy = max(y - y1, 0)
                    d = max(dx, dy)
                    if (side == 0 and x <= x1) or (side == 1 and x >= x0):
                        tint(t, x, y, P.LEATHER[0], 0.55 if d == 1 else 0.25)
    # Stacked sheets underneath, then the top sheet.
    for sheet in (2, 1):
        for side in (0, 1):
            x0, x1, y0, y1 = _page_box(side, sheet)
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    if y > y1 - _bow(x):
                        continue
                    outer = x == x0 if side == 0 else x == x1
                    c = P_[2] if (outer or y == y1 - _bow(x)) else (P_[3] if sheet == 1 else mix(P_[3], P_[2], 0.4))
                    t.set(x, y, c)
    rng = random.Random(97)
    for side in (0, 1):
        x0, x1, y0, y1 = _page_box(side, 0)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                b = _bow(x)
                if y < y0 + b or y > y1 - b:
                    continue
                n = fbm(x, y, 98, 512, 512, 4, 16.0)
                q = max(0.0, min(1.0, 0.2 + (n - 0.5) * 1.5))
                q = round(q * 4) / 4
                c = mix(P_[5], P_[4], q * 0.85)
                # Darker toward the edges of the sheet.
                e = min(x - x0 if side == 0 else x1 - x, y - (y0 + b), (y1 - b) - y)
                if e == 0:
                    c = mix(P_[3], P_[2], 0.35)
                elif e < 4:
                    c = mix(c, P_[3], (4 - e) * 0.16)
                # The gutter: a soft shadow toward the spine.
                d = abs(x + 0.5 - (SPINE + 0.5))
                s = 0.55 * math.exp(-d / 2.3) + 0.10 * max(0.0, 1 - d / 18)
                c = mix(c, P_[1], s)
                t.set(x, y, c)
    # Foxing: denser toward the margins, never more than a whisper over the text.
    for side in (0, 1):
        x0, x1, y0, y1 = _page_box(side, 0)
        for y in range(y0 + 2, y1 - 2):
            for x in range(x0 + 1, x1):
                writable = (22 <= x <= 202 or 214 <= x <= 394) and 18 <= y <= 250
                chance = 0.0035 if writable else 0.03
                if rng.random() < chance:
                    tint(t, x, y, P_[2], 0.18 if writable else 0.4)
    # A printed rule framing the outer margins.
    rule = mix(P_[4], FADED, 0.45)
    for x in list(range(18, 196)) + list(range(220, 398)):
        for y in (14, 255):
            if h01(x, y, 99) < 0.93:
                t.set(x, y, mix(t.get(x, y), rule, 0.7))
    for y in range(14, 256):
        for x in (18, 397):
            if h01(x, y, 100) < 0.93:
                t.set(x, y, mix(t.get(x, y), rule, 0.7))
    for (cx, cy) in ((18, 14), (397, 14), (18, 255), (397, 255)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            t.set(cx + dx, cy + dy, mix(P_[4], INK, 0.55))
        t.set(cx, cy, P.BLOOD[3])
    # Stains: coffee rings and a few drops of blood in the margins.
    for (cx, cy, r, a) in ((168, 226, 15, 0.12), (300, 44, 11, 0.09), (60, 70, 22, 0.05)):
        for y in range(cy - r - 2, cy + r + 3):
            for x in range(cx - r - 2, cx + r + 3):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                wob = 0.8 * math.sin(math.atan2(y - cy, x - cx) * 3 + r)
                k = abs(d - (r + wob))
                if k < 1.2:
                    tint(t, x, y, P_[1], a * (1.2 - k) * 1.4)
                elif d < r:
                    tint(t, x, y, P_[2], a * 0.25)
    for (bx, by, size) in ((382, 256, 2), (387, 258, 1), (30, 257, 1), (395, 12, 1)):
        for y in range(by - size, by + size + 1):
            for x in range(bx - size, bx + size + 1):
                d = math.hypot(x - bx, y - by)
                if d <= size + 0.2:
                    t.set(x, y, P.BLOOD[2] if d > size - 0.8 else P.BLOOD[3])
    # The spine: a crease and the binding thread.
    for y in range(8, 264):
        for x in (207, 208):
            c = t.get(x, y)
            if c[3]:
                t.set(x, y, mix(c, P.LEATHER[1], 0.35))
        if y % 9 in (0, 1, 2) and 20 < y < 252:
            t.set(207 if y % 9 != 1 else 208, y, mix(P.BLOOD[2], P_[1], 0.4))


def _brass_corners(t):
    L = 17
    for (fx, fy) in ((False, False), (True, False), (False, True), (True, True)):
        for ly in range(L + 1):
            for lx in range(L + 1):
                s = lx + ly
                if s > L:
                    continue
                x = BW - 1 - lx if fx else lx
                y = BH - 1 - ly if fy else ly
                e = _in_cover(x, y)
                if e < 0:
                    continue
                if e == 0 or s == L:
                    c = BRASS[0]
                elif s == L - 1:
                    c = BRASS[4]                       # the bevelled edge catches the light
                elif s == L - 2:
                    c = BRASS[2]
                elif e == 1:
                    c = BRASS[5] if (ly == 1) != fy else BRASS[4]
                elif s == L - 7:
                    c = BRASS[1]                       # engraved line
                elif s == L - 8:
                    c = BRASS[4]
                else:
                    n = h01(x, y, 101)
                    c = BRASS[3] if n < 0.7 else (BRASS[2] if n < 0.9 else BRASS[4])
                t.set(x, y, c)
        # A domed rivet.
        for dx, dy, c in ((0, 0, BRASS[1]), (1, 0, BRASS[1]), (2, 0, BRASS[1]), (0, 1, BRASS[1]), (0, 2, BRASS[1]),
                          (1, 1, BRASS[5]), (2, 1, BRASS[4]), (1, 2, BRASS[4]), (2, 2, BRASS[2]),
                          (3, 1, BRASS[0]), (3, 2, BRASS[0]), (1, 3, BRASS[0]), (2, 3, BRASS[0]), (3, 3, BRASS[0])):
            rx, ry = (3 + (3 - dx) if fx else 3 + dx), (3 + (3 - dy) if fy else 3 + dy)
            x = BW - 1 - rx if fx else rx
            y = BH - 1 - ry if fy else ry
            t.set(x, y, c)


def spread():
    t = Tex(512, 512, 90)
    _cover(t)
    _pages(t)
    _brass_corners(t)
    return t


# ---------------------------------------------------------------------------------------------
# widgets
# ---------------------------------------------------------------------------------------------

def _mask_depth(mask, w, h, open_left=False):
    """Chamfer distance of every inside pixel to the outside (0 = the outline row)."""
    INF = 99
    d = [[INF if mask[y][x] else -1 for x in range(w)] for y in range(h)]
    changed = True
    while changed:
        changed = False
        for y in range(h):
            for x in range(w):
                if d[y][x] < 0:
                    continue
                best = d[y][x]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if nx < 0 and open_left:
                        continue
                    out = not (0 <= nx < w and 0 <= ny < h) or d[ny][nx] < 0
                    best = min(best, 0 if out else d[ny][nx] + 1)
                if best != d[y][x]:
                    d[y][x] = best
                    changed = True
    return d


TAB_ICON = (10, 3, 25, 18)


def _tab(ramp, active):
    """A leather index tab; its left end runs under the cover, the rounded end sticks out."""
    W, H, R = 30, 22, 6
    t = Tex(W, H)
    base = ramp if active else Ramp(*[mix(c, P.LEATHER[2], 0.4) for c in ramp.c])

    def inside(x, y):
        if not 1 <= y <= 20:
            return False
        if x > W - 1 - R:
            cy = min(max(y + 0.5, 1 + R), 21 - R)
            return math.hypot(x + 0.5 - (W - R), y + 0.5 - cy) <= R
        return True

    mask = [[inside(x, y) for x in range(W)] for y in range(H)]
    depth = _mask_depth(mask, W, H, open_left=True)
    ix0, iy0, ix1, iy1 = TAB_ICON
    for y in range(H):
        for x in range(W):
            e = depth[y][x]
            if e < 0:
                continue
            if e == 0:
                c = base[0]
            elif e == 1:
                up = y < 11
                c = base[4] if up and y <= 2 else (base[1] if y >= 19 else (base[2] if x > 24 and not up else base[3]))
            else:
                c = base[3] if y < 15 else base[2]
                if h01(x, y, 7) < 0.06 and not (ix0 <= x <= ix1 and iy0 <= y <= iy1):
                    c = mix(c, base[2], 0.7)            # a scuff or two in the leather
            if not active:
                c = mix(c, base[1], 0.18)
            if x < 4:
                c = mix(c, base[0], (4 - x) * 0.16)      # in the shadow of the cover
            t.set(x, y, c)
    stitch = P.GOLD[3] if active else mix(base[1], P.LEATHER[0], 0.35)
    for y in range(H):
        for x in range(5, W):
            along = depth[y][x] == 1 and y in (2, 19) and x <= W - R
            around = depth[y][x] == 2 and x > ix1
            if (along and x % 2 == 1) or (around and (x + y) % 2 == 1):
                t.set(x, y, stitch)
    return t


ARROW_ART = [
    ".....#............",
    "....##............",
    "...##.............",
    "..##..........#..#",
    ".##..........#..#.",
    "##############.#..",
    "##############.#..",
    ".##..........#..#.",
    "..##..........#..#",
    "...##.............",
    "....##............",
    ".....#............",
]


def _arrow(left, hover):
    t = Tex(18, 12)
    ink = P.BLOOD[3] if hover else INK
    tip = P.BLOOD[4] if hover else mix(INK, FADED, 0.3)
    for y, row in enumerate(ARROW_ART):
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            X = x if left else 17 - x
            t.set(X, y, tip if (x < 2 and y in (5, 6)) or (hover and y == 5 and x < 14) else ink)
    return t


def _ribbon(on):
    t = Tex(10, 24)

    def inside(x, y):
        if not 1 <= x <= 8:
            return False
        notch = y - 18
        return notch < 0 or abs(x - 4.5) > notch * 0.9 + 0.2

    if not on:
        k = 0
        for y in range(24):
            for x in range(10):
                if inside(x, y) and not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    if y == 0:
                        continue
                    if (x + y) % 3 != 2:
                        t.set(x, y, with_alpha(FADED, 0.85))
        return t
    for y in range(24):
        for x in range(10):
            if not inside(x, y):
                continue
            edge = not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1)))
            if edge:
                c = P.BLOOD[1]
            elif x == 3:
                c = P.BLOOD[5]
            elif x in (2, 4):
                c = P.BLOOD[4]
            elif x == 7:
                c = P.BLOOD[2]
            else:
                c = P.BLOOD[3]
            if y < 2:
                c = mix(c, P.BLOOD[0], 0.5)
            if y % 7 == 5 and not edge:
                c = mix(c, P.BLOOD[2], 0.4)
            t.set(x, y, c)
    return t


def _dot(ramp):
    t = Tex(6, 6)
    for y in range(6):
        for x in range(6):
            d = math.hypot(x + 0.5 - 3, y + 0.5 - 3)
            if d > 3.0:
                continue
            if d > 2.2:
                c = ramp[0]
            elif (x, y) in ((2, 1), (1, 2)):
                c = ramp[5]
            elif x + y <= 4:
                c = ramp[4]
            else:
                c = ramp[3]
            t.set(x, y, c)
    return t


def _node(size, state, boss):
    """state 0 done (gilded), 1 available (inked, glowing), 2 locked (dark, smudged)."""
    t = Tex(size, size, 200 + size + state)
    c0 = size / 2
    r_in = 11.0 if boss else 8.6
    r_ring = 13.2 if boss else 10.2
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c0, y + 0.5 - c0
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            # Points of the star (boss: eight; milestone: four small ticks).
            if boss:
                spike = 2.6 * max(0.0, math.cos(4 * a)) ** 6 + 1.3 * max(0.0, math.cos(4 * a + math.pi)) ** 10
            else:
                spike = 1.7 * max(0.0, math.cos(2 * a) ** 2) ** 8
            r_out = r_ring + spike
            if state == 2:
                wob = (fbm(x, y, 300 + size, size, size, 2, 4.0) - 0.5) * 1.4
                r_out += wob
            light = (-dx - dy) / (d + 0.001)              # top-left light
            if d <= r_in:
                if state == 0:
                    c = mix(P.PARCHMENT[5], P.GOLD[5], 0.18)
                    if d > r_in - 1.2:
                        c = P.GOLD[1]
                elif state == 1:
                    c = P.PARCHMENT[5]
                    if d > r_in - 1.1:
                        c = mix(P.PARCHMENT[3], INK, 0.2)
                else:
                    n = fbm(x, y, 310 + size, size, size, 2, 4.0)
                    c = mix(P.PARCHMENT[2], P.PARCHMENT[1], n * 0.8)
                    if d > r_in - 1.1:
                        c = mix(P.PARCHMENT[1], P.ASH[1], 0.5)
                t.set(x, y, c)
            elif d <= r_out:
                if state == 0:
                    if d > r_out - 0.9:
                        c = P.GOLD[0]
                    else:
                        c = P.GOLD[3 + (1 if light > 0.3 else 0) - (1 if light < -0.4 else 0)]
                        if d > r_ring - 0.2:
                            c = P.GOLD[2 + (1 if light > 0 else 0)]
                elif state == 1:
                    c = INK if (d > r_ring - 1.6 or d > r_out - 1.0) else mix(P.PARCHMENT[4], INK, 0.15)
                    if r_ring - 1.6 < d <= r_ring and light > 0.5:
                        c = mix(INK, P.GOLD[2], 0.35)
                else:
                    n = h01(x, y, 320 + size)
                    c = mix(P.ASH[1], P.LEATHER[1], 0.5) if n < 0.8 else P.ASH[2]
                t.set(x, y, c)
            elif state == 1 and d <= r_out + 3.2:
                k = 1 - (d - r_out) / 3.2
                over(t, x, y, P.GOLD[4], 0.55 * k * k)
            elif state == 2 and d <= r_out + 1.6 and h01(x, y, 330 + size) < 0.35:
                over(t, x, y, P.ASH[1], 0.35)
    if state == 0:
        # Gems at the cardinal points of the boss ring; gilt beads on the milestone.
        b = int(c0 - r_ring + 0.5)
        m = int(c0) - 1
        gem = (P.BLOOD[5], P.BLOOD[3], P.BLOOD[3], P.BLOOD[2]) if boss else (P.GOLD[5], P.GOLD[4], P.GOLD[4], P.GOLD[2])
        for (gx, gy) in ((m, b), (m, size - 2 - b), (b, m), (size - 2 - b, m)):
            for i, (dx, dy) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
                t.set(gx + dx, gy + dy, gem[i])
    return t


def _seal():
    t = Tex(12, 12, 401)
    for y in range(12):
        for x in range(12):
            dx, dy = x + 0.5 - 6, y + 0.5 - 6
            d = math.hypot(dx, dy)
            r = 5.3 + 0.55 * math.sin(math.atan2(dy, dx) * 5 + 0.7)
            if d > r:
                continue
            light = (-dx - dy) / (d + 0.001)
            if d > r - 0.9:
                c = P.GOLD[0] if light < 0.2 else P.GOLD[1]
            elif d > 3.2:
                c = P.GOLD[3] if light > -0.3 else P.GOLD[2]
                if light > 0.6:
                    c = P.GOLD[4]
            elif d > 2.5:
                c = P.GOLD[1]
            else:
                c = P.GOLD[3]
            t.set(x, y, c)
    for (x, y, c) in ((6, 4, P.GOLD[1]), (5, 5, P.GOLD[1]), (6, 5, P.GOLD[5]), (7, 5, P.GOLD[1]),
                      (6, 6, P.GOLD[1]), (6, 7, P.GOLD[1]), (5, 4, P.GOLD[4])):
        t.set(x, y, c)
    return t


def _lock():
    rows = [
        "...oooo...",
        "..oSSSSo..",
        ".oSo..oSo.",
        ".oSo..oSo.",
        ".oSo..oSo.",
        "oooooooooo",
        "oHHHHHHHMo",
        "oHMMooMMDo",
        "oHMMooMMDo",
        "oMMMMoMMDo",
        "oMDDDDDDDo",
        "oooooooooo",
    ]
    pal = {"o": P.OUTLINE, "S": P.STEEL[2], "H": BRASS[4], "M": BRASS[3], "D": BRASS[1]}
    t = Tex(10, 12)
    t.stamp(rows, pal)
    t.set(3, 1, P.STEEL[4])
    t.set(2, 2, P.STEEL[3])
    return t


def _slot(w, h, hover):
    t = Tex(w, h)
    border = mix(INK, P.GOLD[2], 0.55) if hover else INK
    fill = mix(mix(P.PARCHMENT[4], P.PARCHMENT[3], 0.55), P.GOLD[4], 0.22) if hover else mix(P.PARCHMENT[4], P.PARCHMENT[3], 0.55)
    for y in range(h):
        for x in range(w):
            e = min(x, y, w - 1 - x, h - 1 - y)
            corner = (x in (0, w - 1)) and (y in (0, h - 1))
            if corner:
                continue
            if e == 0:
                c = border
            elif e == 1 and (x == 1 or y == 1):
                c = mix(fill, P.PARCHMENT[1], 0.35)  # recessed: shade inside top/left
            else:
                c = fill
            t.set(x, y, c)
    # Inked corner ticks.
    tick = P.GOLD[3] if hover else mix(INK, P.PARCHMENT[3], 0.3)
    for (cx, cy, sx, sy) in ((1, 1, 1, 1), (w - 2, 1, -1, 1), (1, h - 2, 1, -1), (w - 2, h - 2, -1, -1)):
        t.set(cx, cy, tick)
        t.set(cx + sx, cy, tick if hover else t.get(cx + sx, cy))
        t.set(cx, cy + sy, tick if hover else t.get(cx, cy + sy))
    return t


def _card():
    t = Tex(32, 32)
    fill = mix(P.PARCHMENT[4], P.PARCHMENT[3], 0.5)
    for y in range(32):
        for x in range(32):
            e = min(x, y, 31 - x, 31 - y)
            if (x in (0, 31)) and (y in (0, 31)):
                continue
            if e == 0:
                c = mix(INK, P.PARCHMENT[3], 0.1)
            elif e == 1:
                c = mix(fill, P.PARCHMENT[5], 0.35)
            elif e == 2:
                c = mix(fill, INK, 0.12)
            else:
                c = fill
            t.set(x, y, c)
    for (x, y) in ((2, 2), (29, 2), (2, 29), (29, 29)):
        t.set(x, y, P.GOLD[2])
    return t


def _button(state):
    """0 normal, 1 hover, 2 disabled."""
    t = Tex(32, 16)
    if state == 0:
        fill, border, hi, lo = P.PARCHMENT[4], INK, P.PARCHMENT[5], P.PARCHMENT[2]
    elif state == 1:
        fill, border, hi, lo = mix(P.PARCHMENT[4], P.GOLD[4], 0.3), mix(INK, P.GOLD[1], 0.4), P.GOLD[5], mix(P.PARCHMENT[2], P.GOLD[2], 0.4)
    else:
        grey = mix(P.PARCHMENT[3], P.ASH[4], 0.3)
        fill, border, hi, lo = grey, FADED, mix(grey, P.PARCHMENT[4], 0.4), mix(grey, P.ASH[3], 0.4)
    for y in range(16):
        for x in range(32):
            if (x in (0, 31)) and (y in (0, 15)):
                continue
            e = min(x, y, 31 - x, 15 - y)
            if e == 0:
                c = border
            elif y == 1:
                c = hi
            elif y >= 13 or x == 30:
                c = lo
            else:
                c = fill
            t.set(x, y, c)
    if state == 1:
        for (x, y) in ((2, 2), (29, 2), (2, 12), (29, 12)):
            t.set(x, y, P.GOLD[3])
    return t


def _bar_frame():
    t = Tex(64, 8)
    for y in range(8):
        for x in range(64):
            if (x in (0, 63)) and (y in (0, 7)):
                continue
            e = min(x, y, 63 - x, 7 - y)
            if e == 0:
                t.set(x, y, INK)
            else:
                t.set(x, y, with_alpha(P.LEATHER[1], 0.45 if y > 1 else 0.6))
    for x in (16, 32, 48):                  # quarter ticks
        t.set(x, 0, P.GOLD[2])
        t.set(x, 7, P.GOLD[2])
    return t


def _bar_fill(ramp):
    t = Tex(64, 8)
    shades = {1: ramp[5], 2: ramp[4], 3: ramp[3], 4: ramp[3], 5: ramp[2], 6: ramp[1]}
    for y in range(1, 7):
        for x in range(64):
            t.set(x, y, INK if x in (0, 63) else shades[y])
    return t


CORNER_ART = [
    "................",
    ".############...",
    ".#ggggggggg#....",
    ".#g###..........",
    ".#g#..##........",
    ".#g#...#........",
    ".#g.#r.#........",
    ".#g..##.........",
    ".#g.............",
    ".#g.............",
    ".#g.............",
    ".##.............",
    ".#..............",
    ".#..............",
    "................",
    "................",
]


def _corner():
    t = Tex(16, 16)
    t.stamp(CORNER_ART, {"#": INK, "g": P.GOLD[2], "r": P.BLOOD[3]})
    for x, a in ((13, 0.7), (14, 0.35)):
        t.set(x, 1, with_alpha(INK, a))
    for y, a in ((14, 0.7), (15, 0.35)):
        t.set(1, y, with_alpha(INK, a))
    return t


def _flourish():
    t = Tex(120, 8)
    mid = 60
    for x in range(2, 118):
        d = abs(x + 0.5 - mid) / 58
        a = 1.0 if d < 0.7 else max(0.25, 1.0 - (d - 0.7) / 0.3 * 0.75)
        t.set(x, 4, with_alpha(INK, a))
        if 0.12 < d < 0.5:
            t.set(x, 3, with_alpha(INK, 0.9 if d < 0.35 else 0.9 * (0.5 - d) / 0.15))
    for x in (1, 118):
        t.set(x, 4, with_alpha(INK, 0.5))
    # Curls either side of the centre.
    for sx in (-1, 1):
        cx = mid + sx * 10
        for k in range(16):
            a = k / 16 * math.pi * 1.6 + math.pi * 0.2
            x = int(round(cx - 0.5 + sx * math.cos(a) * 2.6))
            y = int(round(3.5 - math.sin(a) * 2.6))
            if 0 <= y < 8:
                t.set(x, y, INK)
        for k in (26, 38):                       # dots along the rule
            t.set(mid - 1 + sx * k if sx > 0 else mid + sx * k, 2, with_alpha(INK, 0.85))
            t.set(mid - 1 + sx * k if sx > 0 else mid + sx * k, 6, with_alpha(INK, 0.85))
    # The lozenge, with a drop of blood.
    for y in range(8):
        for x in range(52, 68):
            k = abs(x + 0.5 - mid) / 6.0 + abs(y + 0.5 - 4) / 3.9
            if k <= 1.0:
                t.set(x, y, INK if k > 0.68 else (P.BLOOD[4] if (x - mid + y) < 2 else P.BLOOD[3]))
    t.set(mid - 1, 3, P.BLOOD[5])
    return t


def widgets():
    t = Tex(256, 256, 120)
    for s, ramp in enumerate(TAB_RAMPS):
        t.paste(_tab(ramp, False), s * 30, 0)
        t.paste(_tab(ramp, True), s * 30, 22)
    t.paste(_arrow(True, False), 0, 44)
    t.paste(_arrow(True, True), 18, 44)
    t.paste(_arrow(False, False), 36, 44)
    t.paste(_arrow(False, True), 54, 44)
    t.paste(_ribbon(False), 72, 44)
    t.paste(_ribbon(True), 82, 44)
    t.paste(_dot(P.GOLD), 92, 44)
    t.paste(_dot(P.BLOOD), 98, 44)
    for state in range(3):
        t.paste(_node(32, state, True), state * 32, 72)
        t.paste(_node(24, state, False), 96 + state * 24, 72)
    t.paste(_seal(), 168, 72)
    t.paste(_lock(), 180, 72)
    t.paste(_slot(20, 20, False), 0, 104)
    t.paste(_slot(20, 20, True), 20, 104)
    t.paste(_slot(56, 20, False), 40, 104)
    t.paste(_slot(56, 20, True), 96, 104)
    t.paste(_card(), 0, 128)
    for state in range(3):
        t.paste(_button(state), 32, 128 + state * 16)
    t.paste(_bar_frame(), 64, 128)
    t.paste(_bar_fill(MANA), 64, 136)
    t.paste(_bar_fill(SANITY), 64, 144)
    t.paste(_bar_fill(DANGER), 64, 152)
    t.paste(_corner(), 128, 128)
    t.paste(_flourish(), 0, 192)
    return t


# ---------------------------------------------------------------------------------------------
# roadmap paper and spell sprites
# ---------------------------------------------------------------------------------------------

def roadmap_paper():
    S = 256
    t = Tex(S, S, 500)
    lo = mix(P.PARCHMENT[3], hexc("#d9b870"), 0.25)
    hi = mix(P.PARCHMENT[4], hexc("#ead19a"), 0.3)
    grid = mix(P.PARCHMENT[2], FADED, 0.4)
    rings = ((70, 180, 18), (200, 40, 26), (250, 250, 12))
    for y in range(S):
        for x in range(S):
            n = fbm(x, y, 501, S, S, 4, 4.0)
            m = fbm(x, y, 502, S, S, 3, 8.0)
            q = max(0.0, min(1.0, 0.45 + (n - 0.5) * 1.6))
            c = mix(hi, lo, round(q * 5) / 5)
            if m > 0.66:
                c = mix(c, P.PARCHMENT[2], min(0.22, (m - 0.66) * 1.6))
            if x % 32 == 0 or y % 32 == 0:
                c = mix(c, grid, 0.22 if (x + y) % 4 else 0.12)
            elif x % 8 == 0 and y % 8 == 0:
                c = mix(c, grid, 0.2)
            for (cx, cy, r) in rings:
                dx = (x - cx + S // 2) % S - S // 2
                dy = (y - cy + S // 2) % S - S // 2
                k = abs(math.hypot(dx, dy) - r)
                if k < 1.3:
                    c = mix(c, P.PARCHMENT[1], 0.14 * (1.3 - k))
            if h01(x, y, 503) < 0.004:
                c = mix(c, P.PARCHMENT[1], 0.35)
            t.set(x, y, c)
    return t


def spell_fx():
    t = Tex(64, 64)

    def put(x, y, a):
        if a > 0.01:
            t.set(x, y, (255, 255, 255, int(round(min(1.0, a) * 255))))

    for y in range(8):                     # spark: a four-pointed star
        for x in range(8):
            dx, dy = abs(x + 0.5 - 4), abs(y + 0.5 - 4)
            core = max(0.0, 1 - math.hypot(dx, dy) / 2.6) ** 0.7

            def arm(along, across):
                return max(0.0, 1 - along / 5.0) * (1.0 if across <= 0.5 else 0.15 if across <= 1.5 else 0.0)
            arms = max(arm(dx, dy), arm(dy, dx))
            put(x, y, max(core, arms))
    for y in range(16):                    # glow: soft radial
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8
            put(32 + x, y, max(0.0, 1 - d) ** 2)
    for y in range(32):                    # ring
        for x in range(32):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            a = math.exp(-((d - 13.0) / 1.25) ** 2)
            if d < 13:
                a = max(a, 0.12 * (d / 13) ** 3)
            put(x, 16 + y, a)
    for y in range(8):                     # streak: bright head on the right
        for x in range(32):
            body = (x / 31) ** 2.2 * math.exp(-((y + 0.5 - 4) / (0.9 + x / 31 * 0.9)) ** 2)
            head = max(0.0, 1 - math.hypot(x + 0.5 - 28, (y + 0.5 - 4) * 1.3) / 3.6)
            put(32 + x, 16 + y, max(body, head))
    return t


def generate():
    save(spread(), "gui/book", "book_spread")
    save(widgets(), "gui/book", "book_widgets")
    save(roadmap_paper(), "gui/book", "roadmap_paper")
    save(spell_fx(), "gui/book", "spell_fx")
