"""Chain of Command HUD (textures/gui/chain/), laid out exactly as client/chain/ChainOverlay (and OfficerCard, OfficerBar,
DungeonTitle) read it.

  medallions   180x108  opaque rank medallions, 36x36: column = rank (Soldier .. Lord), row = Pit, Host, Fallen. The book's
                        medallion (chain_book_art) over its backdrop, with the side's emblem in the window
  card_<side>  256x88   the officer's title card: a plate x 16..239, y 16..87 (chamfered rim, inner rule), a socket for the
                        medallion round (128, 18) and the side's ornaments in the 16px margins
  bar_<side>   176x24   an officer's bar: a socket round (12, 12) for an 18px medallion, the plate x 10..175, y 3..20, and an
                        empty dark track x 26..170, y 15..18 that the code fills
  lord_<side>  248x44   the Lord's bar: a socket round (20, 24) for the 36px medallion, the plate x 18..231, y 10..39, a crest
                        over its middle (y 1..10), an end ornament x 232..247, and the track x 44..225, y 30..35
  pips          36x6    level pips, 6x6 cells: x = (side * 2 + lit) * 6
  court_<side> 256x72   the stronghold's title band: fades out at both ends, body y 20..59 between rules at y 18 and 61, a
                        crest (y 0..21) over the middle and a small diamond under it

  talk         256x192  the talk screen (v0.27), a row of 64 per side: a nine-slice frame 64x64 at x 0 (corners 16, the edges
                        and middle tile), the portrait's ring 64x64 at x 64 (open inside r 26), its well at x 128 (an opaque
                        disc r 26.5), an answer's plate 32x14 at x 192 (y +0 off, +14 hover; caps 4) and its number's badge
                        12x12 at x 224 (y +0 off, +14 hover)
  duel         256x128  the duel banner: per side (row side*24) a half plate 112x24 facing left at x 0 and its mirror at x 112
                        (a socket round (12, 12) / (99, 12), the name at y 5, a track x 26..104 / 7..85, y 15..18), and the
                        crests 32x32 at (0, 72) to the death and (32, 72) until one yields
  duel_ring     32x32   the ring round a duel in the world: runes on a band that brightens downwards (tinted in code)
and, for the book (textures/gui/book/chain_portrait.png, 256x256): an officer's portrait arch per side, 64x80 at (side*64, 0),
open in its window (x 10..53 under a round top of r 22 about (32, 36), down to y 71), and the window's backdrop at
(side*64, 80).

Sides: pit (iron and blood), host (gold and white), fallen (ash and tarnished gold). Courts: pit and host only.
"""

import math

import palette as P
from book_art import BRASS, h01, over, with_alpha
from chain_book_art import IRON, SOOT_GOLD, EMBER, PALE, backdrop, medallion
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

CLEAR = (0, 0, 0, 0)
WHITE = hexc("#ffffff")
ASH = Ramp("#141216", "#2b272c", "#433d42", "#625a5c", "#8a8180", "#b8b0a8")

STYLES = {
    "pit": dict(rim=IRON, accent=P.BLOOD[5], deep=P.BLOOD[2], glow=EMBER, lo=hexc("#0d0709"), hi=hexc("#2b1114"), seed=811),
    "host": dict(rim=BRASS, accent=hexc("#fffaf0"), deep=BRASS[2], glow=P.GOLD[5], lo=hexc("#121628"), hi=hexc("#2c3554"), seed=812),
    "fallen": dict(rim=SOOT_GOLD, accent=ASH[5], deep=ASH[2], glow=ASH[4], lo=hexc("#0f0e11"), hi=hexc("#2a262a"), seed=813),
}
SIDES = ("pit", "host", "fallen")


# --- plates ---------------------------------------------------------------------------------------------------

def _edge(x, y, x0, y0, x1, y1, chamfer):
    """Distance in from the plate's chamfered rim (negative outside), and whether the nearest side is lit (top/left)."""
    dx, dy = min(x - x0, x1 - x), min(y - y0, y1 - y)
    e = min(dx, dy, dx + dy - chamfer)
    lit = (x - x0 < x1 - x and dx <= dy) or (y - y0 <= y1 - y and dy < dx)
    return e, lit


def plate(t, x0, y0, x1, y1, side, chamfer=4, rule=True, rim_w=2):
    s = STYLES[side]
    rim = s["rim"]
    height = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            e, lit = _edge(x, y, x0, y0, x1, y1, chamfer)
            if e < 0:
                continue
            if e == 0:
                c = rim[0]
            elif e <= rim_w:
                c = rim[(4 if e == 1 else 3) if lit else (1 if e == 1 else 2)]
                if side == "pit" and lit and e == 1 and h01(x, y, s["seed"]) < .08:
                    c = mix(c, EMBER, .5)  # Embers caught in the iron.
                if side == "fallen" and (x * 3 + y * 7) % 23 == 0:
                    c = ASH[1]  # Fine cracks in the tarnished gilt.
            elif e == rim_w + 1:
                c = rim[0]
            else:
                k = (y - y0) / height
                c = mix(s["hi"], s["lo"], .25 + .75 * k)
                n = fbm(x, y, s["seed"], 64, 64, 3, 4)
                c = mix(c, s["lo"], max(0, .5 - n) * .5)
                c = mix(c, s["lo"], max(0, 6 - e) * .07)  # A soft vignette under the rim.
                if rule and e == rim_w + 3:
                    c = mix(c, s["accent"], .45 if side != "fallen" else .3)
                if side == "fallen" and h01(x, y, 820) < .02:
                    c = mix(c, ASH[4], .35)  # Ash settled on it.
                if side == "host" and h01(x, y, 821) < .012:
                    c = mix(c, WHITE, .3)  # Motes of light.
            t.set(x, y, c)
    # Studs along the top and bottom rails.
    step = 24
    for x in range(x0 + chamfer + 6, x1 - chamfer - 4, step):
        for y in (y0 + 1, y1 - 1):
            _stud(t, x, y, rim, side)


def _stud(t, x, y, rim, side):
    if side == "host":
        t.set(x, y, PALE)
        t.set(x - 1, y, rim[4])
        t.set(x + 1, y, rim[2])
    else:
        t.set(x, y, rim[5] if side == "pit" else rim[4])
        t.set(x + 1, y, rim[1])


def socket(t, cx, cy, r, side):
    """A round seat for a medallion: a bevelled ring and a dark well (the medallion covers the middle)."""
    s = STYLES[side]
    rim = s["rim"]
    for y in range(t.h):
        for x in range(t.w):
            dx, dy = x + .5 - cx, y + .5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            light = (-dx - dy) / max(1, d)
            if d > r - 1:
                c = rim[0]
            elif d > r - 3:
                c = rim[4 if light > .2 else 3 if light > -.3 else 1]
            elif d > r - 4:
                c = rim[0]
            else:
                c = mix(s["lo"], hexc("#000000"), .4)
            t.set(x, y, c)


def track(t, x0, y0, x1, y1, side):
    rim = STYLES[side]["rim"]
    t.rect(x0 - 1, y0 - 1, x1 + 1, y1 + 1, rim[0])
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t.set(x, y, mix(hexc("#060405"), STYLES[side]["deep"], .18 if y == y0 else .08))
    t.hline(x0 - 1, x1 + 1, y1 + 2, with_alpha(rim[3], .6))


# --- ornaments ------------------------------------------------------------------------------------------------

def _spike(t, bx, by, length, width, out, side):
    """A tapering iron spike from (bx, by) pointing out (-1 left, +1 right), its tip dipped in blood."""
    rim = STYLES[side]["rim"]
    for i in range(length):
        half = width * (1 - i / length) / 2
        x = bx + out * i
        for y in range(round(by - half), round(by + half) + 1):
            c = rim[3] if y < by else rim[1]
            if i > length * .7:
                c = mix(c, P.BLOOD[4], .7)
            t.set(x, y, c)
        t.set(x, round(by - half) - 1, rim[0])
        t.set(x, round(by + half) + 1, rim[0])


def _feather(t, bx, by, angle, length, width, colours, ragged=0.0, seed=0):
    """A feather from its quill at (bx, by), lying at {@code angle} (radians, 0 = right)."""
    ca, sa = math.cos(angle), math.sin(angle)
    for i in range(int(length * 3)):
        u = i / 3
        w = width * math.sin(math.pi * min(1, (u / length) * 1.15)) / 2
        for j in range(-int(w * 3), int(w * 3) + 1):
            v = j / 3
            if ragged and h01(int(u * 2), j, seed) < ragged * (u / length):
                continue
            x, y = bx + ca * u - sa * v, by + sa * u + ca * v
            c = colours[1] if v < 0 else colours[0]
            if abs(v) < .5:
                c = colours[2]
            t.set(round(x), round(y), c)


def wing(t, bx, by, out, side, size=15):
    """Four feathers fanned outward from (bx, by): white and gold for the Host, grey and torn for the Fallen."""
    fallen = side == "fallen"
    colours = (ASH[3], ASH[4], ASH[1]) if fallen else (P.SALT[3], WHITE, BRASS[3])
    for k, (a, L) in enumerate(((-.85, size), (-.45, size + 1), (-.05, size), (.35, size - 2), (.7, size - 4))):
        if fallen:
            a += .45  # Drooping.
        ang = a if out > 0 else math.pi - a
        _feather(t, bx - out * 2, by + k * 2 - 4, ang, L, 4.5, colours, ragged=.35 if fallen else 0, seed=830 + k)


def crest(t, cx, top, side):
    """The emblem over a court band or a Lord's bar."""
    s = STYLES[side]
    rim = s["rim"]
    if side == "pit":
        # Three iron spikes, the middle one tallest, on a bar.
        for dx, h in ((-6, 5), (0, 8), (6, 5)):
            for i in range(h):
                half = (h - i) / h * 1.6
                for x in range(round(cx + dx - half), round(cx + dx + half) + 1):
                    t.set(x, top + 8 - i, rim[3] if x <= cx + dx else rim[1])
            t.set(cx + dx, top + 8 - h, P.BLOOD[5])
        t.hline(cx - 9, cx + 9, top + 9, rim[0])
        t.hline(cx - 8, cx + 8, top + 8, rim[4])
    elif side == "host":
        # A halo with short rays.
        for k in range(12):
            a = k / 12 * math.pi * 2
            for r in range(6, 9 if k % 2 == 0 else 8):
                over(t, round(cx + math.cos(a) * r), round(top + 5 + math.sin(a) * r * .5), PALE, .7)
        for y in range(t.h):
            for x in range(t.w):
                d = math.hypot((x + .5 - cx) / 5, (y + .5 - top - 5) / 2)
                if .75 < d <= 1.15:
                    t.set(x, y, BRASS[4] if y < top + 5 else BRASS[2])
    else:
        # A broken crown: the middle tine snapped.
        for dx, h in ((-6, 5), (-2, 3), (2, 4), (6, 5)):
            for i in range(h):
                t.set(cx + dx, top + 8 - i, rim[3])
                t.set(cx + dx + 1, top + 8 - i, rim[1])
        t.hline(cx - 8, cx + 9, top + 9, rim[0])
        t.hline(cx - 7, cx + 8, top + 8, rim[4])
        t.set(cx - 1, top + 9, CLEAR)


# --- emblems --------------------------------------------------------------------------------------------------

def emblem(t, side):
    """The side's mark in a medallion's 24px window (centre 18, 18)."""
    if side == "pit":
        # Horns over a face with two black eyes.
        for flip in (-1, 1):
            for i in range(20):
                u = i / 19
                x = 18 + flip * (3.5 + 4.5 * u + 1.5 * u * u)
                y = 16 - 9 * u + 3 * u * u
                c = mix(IRON[4], P.BLOOD[5], u ** 2)
                t.set(round(x), round(y), c)
                if u < .55:
                    t.set(round(x) + flip, round(y), IRON[2])
        for y in range(36):
            for x in range(36):
                d = math.hypot((x + .5 - 18) / 5.5, (y + .5 - 20) / 6)
                if d <= 1:
                    t.set(x, y, mix(IRON[3], IRON[1], (y - 14) / 12))
        for ex in (15, 20):
            t.rect(ex, 19, ex + 1, 20, hexc("#050304"))
            t.set(ex + 1, 19, P.BLOOD[5])
        t.hline(16, 20, 24, IRON[0])
    elif side == "host":
        # A halo over a four-pointed star of light.
        for y in range(36):
            for x in range(36):
                d = math.hypot((x + .5 - 18) / 6.5, (y + .5 - 11) / 2.6)
                if .62 < d <= 1.1:
                    t.set(x, y, BRASS[4] if y < 11 else BRASS[2])
        for i in range(-6, 7):
            k = 1 - abs(i) / 7
            t.set(18, 21 + i, mix(BRASS[3], WHITE, k))
            if abs(i) <= 5:
                t.set(18 + i, 21, mix(BRASS[3], WHITE, 1 - abs(i) / 6))
        for d in range(2, 4):
            for sx in (-1, 1):
                for sy in (-1, 1):
                    over(t, 18 + sx * d, 21 + sy * d, PALE, .55 if d == 2 else .3)
        for d in (-1, 1):
            t.set(18 + d, 21, WHITE)
            t.set(18, 21 + d, WHITE)
            t.set(18 + d, 21 + d, mix(BRASS[3], WHITE, .4))
            t.set(18 - d, 21 + d, mix(BRASS[3], WHITE, .4))
        t.set(18, 21, WHITE)
    else:
        # A broken halo over a falling feather.
        for y in range(36):
            for x in range(36):
                dx, dy = (x + .5 - 18) / 6.5, (y + .5 - 10) / 2.6
                d = math.hypot(dx, dy)
                a = math.atan2(dy, dx)
                if .62 < d <= 1.1 and not (-1.2 < a < -.3) and not (1.9 < a < 2.4):
                    t.set(x, y, SOOT_GOLD[3] if y < 11 else SOOT_GOLD[1])
        _feather(t, 12, 15, .95, 15, 6, (ASH[3], ASH[5], ASH[1]), ragged=.25, seed=840)


def medallions():
    t = Tex(180, 108)
    for row, side in enumerate(SIDES):
        for rank in range(5):
            cell = Tex(36, 36)
            cell.paste(backdrop(row), 5, 5)
            emblem(cell, side)
            # The window is a disc of 12: anything of the emblem past it is the rim's.
            cell.paste(medallion(row, rank), 0, 0)
            t.paste(cell, rank * 36, row * 36)
    return t


# --- sheets ---------------------------------------------------------------------------------------------------

def card(side):
    t = Tex(256, 88)
    s = STYLES[side]
    plate(t, 16, 16, 239, 87, side, chamfer=6, rim_w=3)
    # A second, finer inner rule.
    for x in range(30, 226):
        over(t, x, 81, s["accent"], .25)
    socket(t, 128, 18, 21, side)
    # Ornaments in the margins, at mid-height of the plate.
    for out, bx in ((-1, 16), (1, 239)):
        if side == "pit":
            _spike(t, bx + out, 44, 13, 6, out, side)
            _spike(t, bx + out, 60, 9, 4, out, side)
        else:
            wing(t, bx + out, 50, out, side)
    if side == "pit":
        # Chain links hanging from the bottom corners.
        for x0, y0 in ((22, 22), (222, 22), (22, 70), (222, 70)):
            _bracket(t, x0, y0, x0 < 128, y0 < 50, side)
    else:
        for x0, y0 in ((22, 22), (222, 22), (22, 70), (222, 70)):
            _bracket(t, x0, y0, x0 < 128, y0 < 50, side)
    return t


def _bracket(t, x0, y0, left, top, side):
    """An L-shaped corner ornament inside a card, 12px a side."""
    s = STYLES[side]
    rim = s["rim"]
    sx, sy = (1 if left else -1), (1 if top else -1)
    ox, oy = (x0 if left else x0 + 11), (y0 if top else y0 + 11)
    for i in range(12):
        t.set(ox + sx * i, oy, rim[3])
        t.set(ox, oy + sy * i, rim[3])
        t.set(ox + sx * i, oy + sy, rim[0])
        t.set(ox + sx, oy + sy * i, rim[0])
    t.set(ox + sx * 2, oy + sy * 2, s["accent"])
    t.set(ox + sx * 12, oy, s["glow"])
    t.set(ox, oy + sy * 12, s["glow"])


def bar(side):
    t = Tex(176, 24)
    plate(t, 10, 3, 175, 20, side, chamfer=3, rule=False, rim_w=1)
    track(t, 26, 15, 170, 18, side)
    socket(t, 12, 12, 11.5, side)
    return t


def lord_bar(side):
    t = Tex(248, 44)
    plate(t, 18, 10, 231, 39, side, chamfer=5, rule=False, rim_w=2)
    for x in range(44, 226):
        over(t, x, 14, STYLES[side]["accent"], .35)
    track(t, 44, 30, 225, 35, side)
    crest(t, 132, 1, side)
    out = 1
    if side == "pit":
        _spike(t, 232, 19, 14, 6, out, side)
        _spike(t, 232, 31, 10, 4, out, side)
    else:
        wing(t, 232, 25, out, side, size=13)
    socket(t, 20, 24, 20, side)
    return t


def pips():
    t = Tex(36, 6)
    for i, side in enumerate(SIDES):
        s = STYLES[side]
        for lit in (0, 1):
            ox = (i * 2 + lit) * 6
            art = ["..o...", ".oho..", "ohhdo.", ".odo..", "..o...", "......"]
            pal = {"o": s["rim"][0], "h": s["accent"] if lit else s["rim"][2], "d": (s["glow"] if lit else s["rim"][1])}
            if side == "pit" and lit:
                pal["h"] = P.BLOOD[5]
                pal["d"] = P.BLOOD[3]
            t.stamp(art, pal, ox, 0)
    return t


def court(side):
    t = Tex(256, 72)
    s = STYLES[side]
    rim = s["rim"]
    for y in range(72):
        for x in range(256):
            # Alpha falls away toward both ends.
            ends = min(1, min(x, 255 - x) / 70)
            ends = ends * ends * (3 - 2 * ends)
            if 20 <= y <= 59:
                k = 1 - abs(y - 39.5) / 20
                c = mix(s["lo"], s["hi"], .3 + .5 * k)
                over(t, x, y, c, .82 * ends)
            if y in (18, 61):
                over(t, x, y, rim[4], ends)
            if y in (19, 62):
                over(t, x, y, rim[1], ends)
            if y in (21, 58):
                over(t, x, y, s["accent"], .35 * ends)
    if side == "pit":
        # Battlements along the top rule, and embers.
        for x in range(40, 216):
            if (x // 7) % 2 == 0:
                for y in range(13, 18):
                    over(t, x, y, rim[2] if y > 14 else rim[4], min(1, min(x - 40, 215 - x) / 40))
        for x in range(256):
            if h01(x, 0, 850) < .05:
                y = 22 + int(h01(x, 1, 851) * 34)
                over(t, x, y, EMBER, .5 * min(1, min(x, 255 - x) / 70))
    else:
        # Soft rays fanning up from behind the title.
        for y in range(20, 60):
            for x in range(256):
                a = math.atan2(x + .5 - 128, 68 - y)
                if int((a + 2) * 18) % 3 == 0:
                    ends = min(1, min(x, 255 - x) / 90)
                    over(t, x, y, PALE, .07 * ends * (1 - (y - 20) / 40))
    # The crest, at twice the pixel size, sitting on the top rule.
    small = Tex(24, 11)
    crest(small, 12, 0, side)
    for y in range(small.h):
        for x in range(small.w):
            c = small.get(x, y)
            if c[3]:
                t.rect(128 - 24 + x * 2, y * 2, 128 - 24 + x * 2 + 1, y * 2 + 1, c)
    # A small diamond under the middle of the bottom rule.
    for i in range(4):
        t.hline(128 - (3 - i), 128 + (3 - i), 63 + i, rim[3] if i < 2 else rim[1])
    t.set(128, 63, s["accent"])
    return t


# --- v0.27: portraits, the talk screen, duels ---------------------------------------------------------------

def _arch_window(x, y):
    return (y + .5 >= 36 and 10 <= x <= 53 and y <= 71) or math.hypot(x + .5 - 32, y + .5 - 36) <= 22


def _arch_outer(x, y):
    return (y + .5 >= 36 and 5 <= x <= 58 and y <= 76) or math.hypot(x + .5 - 32, y + .5 - 36) <= 27


def portrait_frame(side):
    t = Tex(64, 80)
    s = STYLES[side]
    rim = s["rim"]
    for y in range(80):
        for x in range(64):
            if not _arch_outer(x, y) or _arch_window(x, y):
                continue
            if y + .5 < 36:
                d = math.hypot(x + .5 - 32, y + .5 - 36)
                e_out, e_in = 27 - d, d - 22
                nx, ny = (x + .5 - 32) / max(1, d), (y + .5 - 36) / max(1, d)
            else:
                e_out = min(x - 5, 58 - x, 76 - y) + .5
                e_in = max(10 - x, x - 53, y - 71) - .5
                nx, ny = (-1 if x < 32 else 1), (1 if y > 71 else 0)
            lit = -nx - ny > 0
            if e_out < 1 or e_in < .9:
                c = rim[0]
            elif e_out < 2:
                c = rim[4] if lit else rim[2]
            elif e_in < 2:
                c = rim[1] if lit else rim[3]
            else:
                c = rim[3] if lit else rim[2]
                if side == "fallen" and (x * 3 + y * 7) % 23 == 0:
                    c = ASH[1]
            if side == "pit" and lit and e_out < 2.5 and h01(x, y, 860) < .1:
                c = mix(c, EMBER, .55)
            t.set(x, y, c)
    # The plinth.
    for y in range(72, 80):
        for x in range(2, 62):
            e = min(x - 2, 61 - x, y - 72, 79 - y)
            c = rim[0] if e == 0 else (rim[4] if y == 73 else rim[2] if y < 78 else rim[1])
            t.set(x, y, c)
    for x in range(10, 54, 6):
        _stud(t, x, 75, rim, side)
    if side == "pit":
        for by, length in ((44, 9), (58, 6)):
            _spike(t, 4, by, length, 4, -1, side)
            _spike(t, 59, by, length, 4, 1, side)
    else:
        wing(t, 6, 46, -1, side, size=11)
        wing(t, 57, 46, 1, side, size=11)
    crest(t, 32, 0, side)
    return t


def portrait_back(side):
    t = Tex(64, 80)
    s = STYLES[side]
    for y in range(80):
        for x in range(64):
            if not _arch_window(x, y):
                continue
            k = (y - 14) / 58
            if side == "pit":
                c = mix(hexc("#0c0607"), hexc("#5a1e16"), k ** 1.6)
                if h01(x, y, 870) < .02 * k:
                    c = mix(c, EMBER, .6)
            elif side == "host":
                c = mix(hexc("#3b4f78"), hexc("#141a30"), k)
                a = math.atan2(x + .5 - 32, y + .5 - 4)
                if int((a + 2) * 16) % 3 == 0:
                    c = mix(c, PALE, .08 * (1 - k))
            else:
                c = mix(hexc("#3a3538"), hexc("#151316"), k)
                if h01(x, y, 871) < .015:
                    c = mix(c, ASH[4], .4)
            d = min(x - 10, 53 - x)
            if d < 4:
                c = mix(c, hexc("#000000"), .3 * (1 - d / 4))
            t.set(x, y, c)
    return t


def portraits():
    t = Tex(256, 256)
    for i, side in enumerate(SIDES):
        t.paste(portrait_frame(side), i * 64, 0)
        t.paste(portrait_back(side), i * 64, 80)
    return t


def _frame9(side):
    """A 64px nine-slice plate (corners 16): flat inside, so its middle tiles."""
    t = Tex(64, 64)
    s = STYLES[side]
    rim = s["rim"]
    for y in range(64):
        for x in range(64):
            e, lit = _edge(x, y, 0, 0, 63, 63, 6)
            if e < 0:
                continue
            if e == 0:
                c = rim[0]
            elif e <= 3:
                c = rim[(4 if e == 1 else 3) if lit else (1 if e == 1 else 2)]
                if side == "pit" and lit and e == 1 and h01(x, y, 880) < .08:
                    c = mix(c, EMBER, .5)
            elif e == 4:
                c = rim[0]
            else:
                c = mix(s["hi"], s["lo"], .55 + h01(x % 32, y % 32, 881) * .2)
                c = mix(c, s["lo"], max(0, 9 - e) * .06)
                if e == 7:
                    c = mix(c, s["accent"], .4 if side != "fallen" else .28)
                if side == "host" and h01(x % 32, y % 32, 882) < .015:
                    c = mix(c, WHITE, .3)
            t.set(x, y, c)
    for x in (12, 36, 60):
        for y in (1, 62):
            _stud(t, x, y, rim, side)
    return t


def _ring(side):
    t = Tex(64, 64)
    rim = STYLES[side]["rim"]
    for y in range(64):
        for x in range(64):
            dx, dy = x + .5 - 32, y + .5 - 34
            d = math.hypot(dx, dy)
            light = (-dx - dy) / max(1, d)
            if 26 <= d <= 30.5:
                if d > 29.6 or d < 26.8:
                    c = rim[0]
                elif d > 28.2:
                    c = rim[4 if light > .2 else 3 if light > -.3 else 1]
                else:
                    c = rim[2 if light > 0 else 1]
                if side == "pit" and light > .4 and h01(x, y, 884) < .12:
                    c = mix(c, EMBER, .5)
                t.set(x, y, c)
    crest(t, 32, 0, side)
    return t


def _well(side):
    t = Tex(64, 64)
    s = STYLES[side]
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + .5 - 32, y + .5 - 34)
            if d <= 26.8:
                k = (y + .5 - 7) / 54
                c = mix(s["hi"], s["lo"], k) if side != "pit" else mix(hexc("#120708"), hexc("#4a1a14"), k ** 1.5)
                c = mix(c, hexc("#000000"), max(0, d - 20) / 7 * .5)
                t.set(x, y, c)
    return t


def _answer(side, hover):
    t = Tex(32, 14)
    s = STYLES[side]
    rim = s["rim"]
    for y in range(14):
        for x in range(32):
            cx, cy = min(x, 31 - x), min(y, 13 - y)
            if cx + cy < 1:
                continue
            e = min(cx, cy)
            c = with_alpha(mix(s["lo"], s["hi"], .5 if hover else .2), .82)
            if hover:
                c = with_alpha(mix(c, s["glow"], .22), .9)
            if e == 0:
                c = rim[4] if hover else with_alpha(rim[2], .9)
            t.set(x, y, c)
    return t


def _badge(side, hover):
    t = Tex(12, 12)
    s = STYLES[side]
    rim = s["rim"]
    for y in range(12):
        for x in range(12):
            d = abs(x + .5 - 6) + abs(y + .5 - 6)
            if d <= 6:
                c = rim[0] if d > 5.2 else (rim[4] if hover else rim[3]) if d > 4.2 else mix(s["lo"], s["glow"] if hover else s["deep"], .5)
                t.set(x, y, c)
    return t


def talk():
    t = Tex(256, 192)
    for i, side in enumerate(SIDES):
        row = i * 64
        t.paste(_frame9(side), 0, row)
        t.paste(_ring(side), 64, row)
        t.paste(_well(side), 128, row)
        for hover in (0, 1):
            t.paste(_answer(side, hover), 192, row + hover * 14)
            t.paste(_badge(side, hover), 224, row + hover * 12)
    return t


def _half(side):
    t = Tex(112, 24)
    plate(t, 10, 3, 111, 20, side, chamfer=3, rule=False, rim_w=1)
    track(t, 26, 15, 104, 18, side)
    socket(t, 12, 12, 11.5, side)
    return t


def _crest_badge(lethal):
    t = Tex(32, 32)
    rim = IRON if lethal else BRASS
    for y in range(32):
        for x in range(32):
            d = abs(x + .5 - 16) + abs(y + .5 - 16)
            if d <= 15:
                light = (16 - x) + (16 - y) > 0
                if d > 14:
                    c = rim[0]
                elif d > 12:
                    c = rim[4 if light else 2]
                elif d > 11:
                    c = rim[0]
                else:
                    c = mix(hexc("#2a0a0c") if lethal else hexc("#1c2238"), hexc("#090405") if lethal else hexc("#0c0f1c"), (y - 4) / 24)
                t.set(x, y, c)
    blade, edge = ((hexc("#e8e0dc"), P.BLOOD[5]) if lethal else (PALE, BRASS[4]))
    dark = hexc("#0a0606")
    swords = Tex(32, 32)
    for flip in (False, True):
        for k in range(15):
            x, y = 9 + k, 23 - k  # Blade from the hilt (bottom) to the tip (top).
            if flip:
                x = 31 - x
            swords.set(x, y, blade)
            swords.set(x + (1 if not flip else -1), y, mix(blade, dark, .35) if k < 14 else blade)
        # Crossguard, grip and pommel.
        gx, gy = (11, 21) if not flip else (20, 21)
        for i in range(-2, 3):
            swords.set(gx + i, gy + i, rim[4])
        px, py = (8, 24) if not flip else (23, 24)
        swords.set(px, py, rim[3])
        swords.set(px + (-1 if not flip else 1), py + 1, edge)
    _outline_px(swords, dark)
    for y in range(32):
        for x in range(32):
            c = swords.get(x, y)
            if c[3] and abs(x + .5 - 16) + abs(y + .5 - 16) <= 13:
                t.set(x, y, c)
    over(t, 16, 12, edge, .8)
    return t


def _outline_px(t, c):
    rows = t.copy_rows()
    for y in range(t.h):
        for x in range(t.w):
            if rows[y][x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < t.w and 0 <= y + dy < t.h and rows[y + dy][x + dx][3]:
                    t.set(x, y, c)
                    break


def duel():
    t = Tex(256, 128)
    for i, side in enumerate(SIDES):
        half = _half(side)
        t.paste(half, 0, i * 24)
        for y in range(24):
            for x in range(112):
                c = half.get(x, y)
                if c[3]:
                    t.set(112 + 111 - x, i * 24 + y, c)
    t.paste(_crest_badge(True), 0, 72)
    t.paste(_crest_badge(False), 32, 72)
    return t


def duel_ring():
    """White runes on a band, alpha rising downwards: the wall's texture (v 0 top, 1 the ground), tinted in code."""
    t = Tex(32, 32)
    runes = ["o.o..ooo..o...oo..o.o.ooo..o..o.", "o.o...o..ooo.o..o.ooo..o..ooo.o.", "ooo...o...o..o..o..o...o...o..oo",
             "o.o..ooo..o...oo...o..ooo..o..o."]
    for y in range(32):
        for x in range(32):
            k = y / 31
            a = .1 + .5 * k ** 2
            if 20 <= y <= 23 and runes[y - 20][x] == "o":
                a = .95
            if y in (18, 25):
                a = .7
            t.set(x, y, with_alpha(WHITE, a))
    return t


def generate():
    save(medallions(), "gui/chain", "medallions")
    save(pips(), "gui/chain", "pips")
    for side in SIDES:
        save(card(side), "gui/chain", "card_" + side)
        save(bar(side), "gui/chain", "bar_" + side)
        save(lord_bar(side), "gui/chain", "lord_" + side)
    for side in ("pit", "host"):
        save(court(side), "gui/chain", "court_" + side)
    save(talk(), "gui/chain", "talk")
    save(duel(), "gui/chain", "duel")
    save(duel_ring(), "gui/chain", "duel_ring")
    save(portraits(), "gui/book", "chain_portrait")


if __name__ == "__main__":
    generate()
