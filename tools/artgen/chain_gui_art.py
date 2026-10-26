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


def generate():
    save(medallions(), "gui/chain", "medallions")
    save(pips(), "gui/chain", "pips")
    for side in SIDES:
        save(card(side), "gui/chain", "card_" + side)
        save(bar(side), "gui/chain", "bar_" + side)
        save(lord_bar(side), "gui/chain", "lord_" + side)
    for side in ("pit", "host"):
        save(court(side), "gui/chain", "court_" + side)


if __name__ == "__main__":
    generate()
