"""The v0.41 boss bars: one 256x128 sheet per boss (`textures/gui/<rig>_bar.png`), all on the ApocalypseBar layout that
client/s1214/S1214Hud reads (the constants in ApocalypseBar mirror these):

  band     (0, 0)     256x40   the boss's plate; a recessed trough (x 34..221, y 22..31), an empty band above it for the name
                                (drawn by the code at y 5..15)
  fill     (0, 40)    188x10   the health (drawn cropped from the left)
  drained  (0, 50)    188x10   the trough where it has run out
  emblems  (0, 64)    24x24 each: whole (x 0), the current phase's (x 24), lost (x 48) -- one hangs per phase
  edge     (96, 64)   6 frames of 8x16 (x = 96 + 8 * frame): the live edge of the bar (a drip hangs from y 0, a flame or
                                sparks stand 6 px over the trough)

  ketch           gunmetal, riveted; a belt of brass rounds runs out; emblems a round whole / smoking / a spent case; muzzle smoke
  hess            dark tweed bound in black leather, gilt tooling; ink with gold flecks; the Order's aquarian star whole / lit /
                  cracked; ink dripping
  dagon           black lacquer split by fire; hellfire; a burning eye open / flaring / burnt shut; a flame
  asmodeus        ivory and gold filigree; liquid grace; a vial of grace full / glowing / shattered; gold sparks
  au_michael      blackened steel, silver insignia, scorched; dark gold light; a silver star whole / burning / broken; sparks
  michael_vessel  charcoal, a fine ice-blue rule; blue-white grace; a feather of light whole / glowing / snapped; frost motes
  soulless_jack   black, cracked with gold; gold light; a gold eye open / blazing / closed; sparks
  shadow          wet black tar with a violet sheen; tar; a tendril whole / writhing / severed; tar dripping
"""

import math

from chuck_art import h01
from common import save
from pixelkit import Tex, fbm, hexc, mix

W, H = 256, 128
TROUGH = (34, 22, 188, 10)


def ramp(*cs):
    return [hexc(c) for c in cs]


def R(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]


def put(t, x, y, c, a=1.0):
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0 or c is None:
        return
    d = t.rows[y][x]
    a = min(1.0, a)
    if d[3] == 0:
        t.rows[y][x] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    t.rows[y][x] = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3)) + (int(255 * oa),)


# --- shared drawing -----------------------------------------------------------------------------------------------

def band(t, base, rim_hi, rim_lo, seed, accent=None, ends=None):
    """The 256x40 plate: `base(x, y)`, a bevelled rim, clipped corners; `accent(x, y, c)` may repaint; `ends(t)` decorates."""
    for y in range(40):
        for x in range(256):
            if min(x, 255 - x) + min(y, 39 - y) < 4:
                continue
            e = min(x, 255 - x, y, 39 - y)
            c = base(x, y)
            if e == 0:
                c = rim_lo
            elif e == 1:
                c = mix(c, rim_hi if y < 20 else rim_lo, 0.55)
            if accent:
                c = accent(x, y, c) or c
            put(t, x, y, c)
    tx, ty, tw, th = TROUGH
    for x in range(tx - 1, tx + tw + 1):
        put(t, x, ty - 1, rim_lo)
        put(t, x, ty + th, rim_hi)
    for y in range(ty - 1, ty + th + 1):
        put(t, tx - 1, y, rim_lo)
        put(t, tx + tw, y, rim_hi)
    for y in range(ty, ty + th):
        for x in range(tx, tx + tw):
            put(t, x, y, hexc("#050506"))
    if ends:
        ends(t)


def strip(t, v0, fn):
    for y in range(10):
        for x in range(188):
            c = fn(x, y)
            if c is not None:
                put(t, x, v0 + y, c)


def emblems(t, fn, h=24):
    for s in range(3):
        for y in range(h):
            for x in range(24):
                c = fn(s, x, y)
                if c is not None:
                    if len(c) == 4:
                        put(t, s * 24 + x, 64 + y, c[:3], c[3] / 255)
                    else:
                        put(t, s * 24 + x, 64 + y, c)


def edges(t, fn):
    for fr in range(6):
        for y in range(16):
            for x in range(8):
                c = fn(fr, x, y)
                if c is not None:
                    if len(c) == 4:
                        put(t, 96 + fr * 8 + x, 64 + y, c[:3], c[3] / 255)
                    else:
                        put(t, 96 + fr * 8 + x, 64 + y, c)


def drip_edge(core, hi):
    """A drop hanging from the bar's cut end, swelling and falling (frame = time)."""
    def f(fr, x, y):
        d = abs(x - 3.5)
        if y < 10 and d < 1.6 - y * 0.05:
            return hi if d < 0.6 and y < 4 else core
        drop_y = 10 + fr
        if fr < 5 and math.hypot(x - 3.5, y - drop_y) < 1.4:
            return core
        return None
    return f


def flame_edge(cols):
    """A flame standing over the bar's end, flickering (6 px above the trough's top)."""
    def f(fr, x, y):
        d = abs(x - 3.5 + 0.6 * math.sin(fr * 1.7 + y * 0.5))
        top = 2 + (fr % 3)
        if y < top:
            return None
        k = (y - top) / (16 - top)
        if d > 0.6 + 2.6 * k:
            return None
        return cols[min(len(cols) - 1, int((1 - k) * (len(cols) - 1) + (0 if d < 1 else 1)))]
    return f


def spark_edge(core, spark):
    def f(fr, x, y):
        d = abs(x - 3.5)
        if y >= 6 and d < 1.6:
            return core if d < 0.8 else spark
        if y < 6 and h01("sp", fr, x, y) < 0.08:
            return spark + (220,)
        return None
    return f


def disc(cx, cy, r):
    return lambda x, y: math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r


# --- the eight bosses ---------------------------------------------------------------------------------------------

def ketch():
    t = Tex(W, H, 58001)
    gm = ramp("#0b0c0e", "#16181b", "#23262a", "#30343a", "#41464e", "#5e646e")
    brass = ramp("#3b2a10", "#5e4519", "#836226", "#a98236", "#cba354", "#e9c880")

    def base(x, y):
        n = fbm(x, y, 58002, 256, 64, 2, 10.0)
        c = R(gm, 2.4 + (n - 0.5) * 1.2 + (0.6 if (x + y * 3) % 23 == 0 else 0))
        if 3 <= y <= 4 or 35 <= y <= 36:
            c = gm[4]
        return c

    def ends(t):
        for cx in list(range(10, 30, 9)) + list(range(232, 252, 9)):
            for cy in (8, 31):
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        put(t, cx + dx, cy + dy, gm[5] if dx + dy < 0 else gm[1])
    band(t, base, gm[5], gm[0], 58003, ends=ends)
    # Fill: a belt of brass rounds, nose to tail.
    strip(t, 40, lambda x, y: (R(brass, 4.4 - abs(y - 3) * 0.5) if x % 6 < 4 else (hexc("#a06a40") if x % 6 == 4 and 2 <= y <= 7 else gm[1]))
          if 1 <= y <= 8 else gm[0])
    strip(t, 50, lambda x, y: R(gm, 1.0 + 0.6 * ((x // 6 + y) % 2)) if 1 <= y <= 8 else gm[0])

    def em(s, x, y):
        cx = 12
        if s == 2:
            if 8 <= y <= 18 and abs(x - cx) <= 2:
                return R(brass, 2.6 - (y - 8) * 0.05) if not (y == 8) else brass[1]   # a spent, open case
            return None
        if 2 <= y <= 6 and abs(x - cx) <= 2 - max(0, 4 - y) * 0.5:
            return hexc("#b07040") if s == 0 else hexc("#d8904a")
        if 6 < y <= 18 and abs(x - cx) <= 2:
            return R(brass, 4.2 - abs(x - cx) * 0.7)
        if s == 1 and y < 3 and h01("sm", x, y) < 0.3:
            return (180, 180, 180, 150)
        return None
    emblems(t, em)
    edges(t, lambda fr, x, y: ((200, 200, 200, 130) if y < 8 and h01("smk", fr, x // 2, (y + fr) // 2) < 0.35 else None))
    return t


def hess():
    t = Tex(W, H, 58101)
    tweed = ramp("#1c1410", "#2a1e18", "#3a2a20", "#4a372a", "#5c4636", "#705642")
    gilt = ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
    ink = ramp("#04060e", "#0a1020", "#121c36", "#1c2a50", "#2a3c6c", "#3c548a")

    def base(x, y):
        if y < 6 or y > 33 or x < 8 or x > 247:
            return R(ramp("#050505", "#0b0a0a", "#141212", "#1e1b1a", "#2a2624", "#3a3532"), 2.4 + 0.6 * math.sin(x * 0.3 + y))
        c = R(tweed, 2.6 + (h01("tw", x, y) - 0.5) * 1.6 + (0.5 if (x + 2 * y) % 4 == 0 else 0))
        if y in (6, 33) or x in (8, 247):
            c = gilt[3]
        return c

    def ends(t):
        star = ["...s...", "..sSs..", ".sSGSs.", "sSGGGSs", ".sSGSs.", "..sSs..", "...s..."]
        for cx in (20, 235):
            for j, row in enumerate(star):
                for i, ch in enumerate(row):
                    col = {"s": gilt[2], "S": gilt[4], "G": hexc("#5a8ab8")}.get(ch)
                    put(t, cx - 3 + i, 17 - 3 + j, col)
    band(t, base, gilt[4], hexc("#050505"), 58103, ends=ends)
    strip(t, 40, lambda x, y: (gilt[4] if h01("fl", x, y) < 0.05 else R(ink, 3.4 - abs(y - 3) * 0.4 + 0.4 * math.sin(x * 0.2))))
    strip(t, 50, lambda x, y: R(ramp("#3a3024", "#4e4232", "#625440", "#76684e", "#8a7c5e", "#a09070"), 2.2 + 0.6 * math.sin(x * 0.4 + y)))

    def em(s, x, y):
        d = abs(x - 11.5) + abs(y - 11.5)
        if d > 9:
            return None
        if s == 2 and (x - y) in (0, 1) and d < 9:
            return hexc("#050505")                    # cracked through
        if d > 7:
            return gilt[2] if s != 2 else gilt[1]
        if d < 2.5:
            return hexc("#9ad0ff") if s == 1 else hexc("#5a8ab8")
        return gilt[5] if s == 1 else (gilt[4] if s == 0 else gilt[2])
    emblems(t, em)
    edges(t, drip_edge(ink[2], ink[5]))
    return t


def dagon():
    t = Tex(W, H, 58201)
    lac = ramp("#030203", "#080506", "#0f0a0b", "#181012", "#22171a", "#2e2024")
    fire = ramp("#3b0602", "#7a1404", "#c02d06", "#ec5a0c", "#ff9a26", "#ffd96a")

    def accent(x, y, c):
        crack = abs(math.sin(x * 0.13 + 3 * math.sin(y * 0.3 + x * 0.02))) < 0.05 and 4 < y < 36
        return mix(c, fire[3], 0.8) if crack else None
    band(t, lambda x, y: R(lac, 2.4 + 0.8 * fbm(x, y, 58202, 256, 64, 2, 8.0) + (1.0 if y < 4 else 0)), fire[2], lac[0], 58203,
         accent=accent)
    strip(t, 40, lambda x, y: R(fire, 3.6 - abs(y - 3) * 0.45 + (fbm(x, y, 58204, 188, 10, 2, 6.0) - 0.5) * 1.6))
    strip(t, 50, lambda x, y: R(lac, 1.6 + fbm(x * 2, y * 2, 58205, 188, 10, 2, 5.0)) if h01("emb", x, y) > 0.03 else fire[1])

    def em(s, x, y):
        dx, dy = (x - 11.5) / 10, (y - 11.5) / 5
        inside = dx * dx + dy * dy <= 1
        if not inside:
            return (fire[3][0], fire[3][1], fire[3][2], 140) if s == 1 and dx * dx + dy * dy <= 1.5 and h01("fl", x, y) < 0.4 else None
        if s == 2:
            return lac[3] if abs(y - 11.5) > 0.6 else lac[0]          # burnt shut
        r = math.hypot(x - 11.5, y - 11.5)
        if r < 2.2:
            return hexc("#1a0402") if r < 1.2 else fire[5]
        if r < 4.5:
            return fire[4] if s == 1 else fire[3]
        return fire[2] if s == 0 else fire[3]
    emblems(t, em)
    edges(t, flame_edge([fire[5], fire[4], fire[3], fire[2], fire[1]]))
    return t


def asmodeus():
    t = Tex(W, H, 58301)
    ivory = ramp("#8e8a80", "#b0aca2", "#ccc8be", "#dfdcd3", "#ecead4", "#faf9f2")
    gold = ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")

    def accent(x, y, c):
        if y in (4, 35) or ((x + y) % 12 == 0 and (y < 8 or y > 32)):
            return gold[3]
        if (abs(math.sin(x * 0.2) * 3 - (y - 36)) < 0.6 or abs(math.sin(x * 0.2) * 3 - (y - 3)) < 0.6):
            return gold[4]
        return None
    band(t, lambda x, y: R(ivory, 3.2 + 0.4 * math.sin(x * 0.11 + y * 0.2) - 0.3 * y / 40), gold[5], gold[0], 58303, accent=accent)
    strip(t, 40, lambda x, y: R(gold, 4.0 - abs(y - 3) * 0.4 + 0.5 * math.sin(x * 0.3 - y)) if not (y <= 1) else gold[5])
    strip(t, 50, lambda x, y: R(ivory, 1.2 + 0.6 * math.sin(x * 0.5 + y)))

    def em(s, x, y):
        cx = 11.5
        if 2 <= y <= 4 and abs(x - cx) <= 1.5:
            return gold[4] if s != 2 else gold[1]
        if 5 <= y <= 7 and abs(x - cx) <= 1:
            return (200, 220, 230, 170)
        if 8 <= y <= 19 and abs(x - cx) <= 4 - max(0, 10 - y) * 0.6:
            if s == 2:
                if h01("sh", x, y) < 0.5 or y < 12:
                    return None
                return (200, 220, 230, 150)
            lvl = 10 if s == 0 else 9
            if y >= lvl:
                return gold[5] if (x + y) % 3 == 0 else gold[4]
            return (200, 220, 230, 160)
        if s == 1 and h01("gs", x, y) < 0.04:
            return gold[5]
        return None
    emblems(t, em)
    edges(t, spark_edge(gold[5], gold[4]))
    return t


def au_michael():
    t = Tex(W, H, 58401)
    steel = ramp("#050506", "#0b0b0d", "#131316", "#1c1c20", "#28282e", "#3a3a44")
    silver = ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")
    gold = ramp("#2a1c06", "#4a320c", "#7a5414", "#a8781e", "#d0a032", "#f0d070")

    def accent(x, y, c):
        if 3 <= y <= 4 or 35 <= y <= 36:
            return silver[3]
        scorch = fbm(x, y, 58402, 256, 64, 2, 10.0)
        if scorch > 0.68:
            return mix(c, hexc("#2a1408"), 0.5)
        return None

    def ends(t):
        for cx in (18, 237):
            for k in range(5):
                a = math.pi / 2 + k * 2 * math.pi / 5
                for rr in range(1, 7):
                    put(t, cx + rr * math.cos(a), 19 - rr * math.sin(a), silver[4] if rr < 4 else silver[2])
    band(t, lambda x, y: R(steel, 2.6 + 0.6 * fbm(x, y, 58403, 256, 64, 2, 6.0) + (1 if y < 3 else 0)), silver[4], steel[0], 58404,
         accent=accent, ends=ends)
    strip(t, 40, lambda x, y: R(gold, 3.8 - abs(y - 3) * 0.45 + (fbm(x, y, 58405, 188, 10, 2, 6.0) - 0.5) * 1.2))
    strip(t, 50, lambda x, y: R(steel, 1.4 + fbm(x * 2, y * 2, 58406, 188, 10, 2, 5.0)))

    def em(s, x, y):
        a = math.atan2(-(y - 11.5), x - 11.5)
        r = math.hypot(x - 11.5, y - 11.5)
        lim = 4 + 5 * abs(math.cos(2.5 * (a - math.pi / 2)))
        if r > lim:
            return None
        if s == 2 and (x > 12 or (x + y) % 5 == 0):
            return None                                   # broken
        if s == 1:
            return gold[5] if r < 3 else gold[4]
        return silver[5] if r < 2.5 else (silver[3] if x < 12 else silver[2])
    emblems(t, em)
    edges(t, spark_edge(gold[5], gold[4]))
    return t


def michael_vessel():
    t = Tex(W, H, 58501)
    char = ramp("#0a0b0d", "#121316", "#1a1b1f", "#232428", "#2d2e33", "#38393f")
    ice = ramp("#20304a", "#3a5478", "#5a7ca6", "#86a8cc", "#b4cce6", "#e2eefa")

    def accent(x, y, c):
        if y in (4, 35):
            return ice[3]
        return None
    band(t, lambda x, y: R(char, 2.6 + 0.5 * math.sin(x * 0.07 + y * 0.3)), ice[4], char[0], 58503, accent=accent)
    strip(t, 40, lambda x, y: R(ice, 4.0 - abs(y - 3) * 0.45 + 0.4 * math.sin(x * 0.25 + y)) if y > 1 else ice[5])
    strip(t, 50, lambda x, y: R(char, 1.2 + 0.6 * ((x // 3 + y) % 2)) if h01("fr", x, y) > 0.04 else ice[1])

    def em(s, x, y):
        u = (y - 2) / 20
        if not 0 <= u <= 1:
            return None
        if s == 2 and u > 0.55:
            return None                                   # snapped
        half = 4.5 * math.sin(math.pi * min(1.0, u * 1.2)) if u < 0.8 else 4.5 * (1 - u) * 3
        d = x - 11.5 - (u - 0.5) * 3
        if abs(d) > half:
            return None
        c = ice[5] if abs(d) < 0.7 else (ice[4] if s == 1 else ice[3])
        return c
    emblems(t, em)
    edges(t, spark_edge(ice[5], ice[4]))
    return t


def soulless_jack():
    t = Tex(W, H, 58601)
    blk = ramp("#020203", "#060608", "#0c0c10", "#141418", "#1c1c22", "#28282e")
    gold = ramp("#5a3c08", "#9c7418", "#d4a830", "#f2d260", "#fff0a8", "#ffffff")

    def accent(x, y, c):
        crack = abs(math.sin(x * 0.09 + 2.4 * math.sin(y * 0.4 + x * 0.05))) < 0.04 and 3 < y < 37
        return gold[3] if crack else (gold[2] if y in (3, 36) else None)
    band(t, lambda x, y: R(blk, 2.4 + 0.6 * fbm(x, y, 58602, 256, 64, 2, 8.0)), gold[3], blk[0], 58603, accent=accent)
    strip(t, 40, lambda x, y: R(gold, 3.8 - abs(y - 3) * 0.45 + (fbm(x, y, 58604, 188, 10, 2, 6.0) - 0.5) * 1.4))
    strip(t, 50, lambda x, y: R(blk, 1.6 + fbm(x * 2, y * 2, 58605, 188, 10, 2, 5.0)))

    def em(s, x, y):
        dx, dy = (x - 11.5) / 10, (y - 11.5) / 5
        k = dx * dx + dy * dy
        if k > 1:
            return (gold[4][0], gold[4][1], gold[4][2], 120) if s == 1 and k < 1.6 and h01("bl", x, y) < 0.35 else None
        if s == 2:
            return blk[4] if abs(y - 11.5) > 0.6 else gold[1]
        r = math.hypot(x - 11.5, y - 11.5)
        if r < 1.4:
            return blk[0]
        if r < 4.2:
            return gold[5] if s == 1 else gold[4]
        return hexc("#e8e2d8")
    emblems(t, em)
    edges(t, spark_edge(gold[5], gold[4]))
    return t


def shadow():
    t = Tex(W, H, 58701)
    tar = ramp("#020203", "#060509", "#0b0910", "#120f19", "#1b1624", "#282034")
    sheen = ramp("#2a1f3e", "#3e2c5c", "#56407a", "#705898", "#8c74b4", "#b4a0d8")

    def base(x, y):
        c = R(tar, 2.2 + 0.8 * fbm(x, y, 58702, 256, 64, 2, 8.0) - 0.5 * y / 40)
        s = math.sin(x * 0.21 + 2.0 * math.sin(y * 0.2))
        if s > 0.93 and h01("col", x) < 0.5:
            c = mix(c, sheen[2], 0.5)
        return c

    def accent(x, y, c):
        if y > 34 and h01("drp", x // 3) < 0.25:
            return tar[1]
        return None
    band(t, base, sheen[3], tar[0], 58703, accent=accent)
    strip(t, 40, lambda x, y: (sheen[4] if (y == 1 and (x // 5) % 3 == 0) else
                               mix(R(tar, 4.4 - abs(y - 4) * 0.5), R(sheen, 2.6 - abs(y - 3) * 0.5 + 0.6 * math.sin(x * 0.3 + y)), 0.6)))
    strip(t, 50, lambda x, y: hexc("#000000") if h01("v", x, y) > 0.03 else sheen[0])

    def em(s, x, y):
        u = y / 23
        cx = 11.5 + 4 * math.sin(u * 5 + (s * 1.3))
        w = 2.6 * (1 - u) + 0.4
        if s == 2 and u > 0.45:
            return None                                   # severed
        if abs(x - cx) > w:
            return None
        return sheen[3] if abs(x - cx) < 0.6 and s == 1 else R(tar, 4.2 - u * 1.5)
    emblems(t, em)
    edges(t, drip_edge(tar[3], sheen[3]))
    return t


SHEETS = {"ketch": ketch, "hess": hess, "dagon": dagon, "asmodeus": asmodeus, "au_michael": au_michael,
          "michael_vessel": michael_vessel, "soulless_jack": soulless_jack, "shadow": shadow}


def generate():
    for name, fn in SHEETS.items():
        save(fn(), "gui", f"{name}_bar")


if __name__ == "__main__":
    generate()
