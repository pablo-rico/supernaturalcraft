"""Hell: its stone and ores, the Cage's fittings, the rift (animated) and the items found there.

Every texture is 16x16 (the rift is a strip of animation frames with its .mcmeta).
"""

import json
import math
import os
import random

import palette as P
from blocks import stone_base, ore, CRYSTAL_A, CRYSTAL_B, CRYSTAL_C
from common import TEXTURES, save
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

HELLSTONE = Ramp("#1a0907", "#2c100c", "#401812", "#552219", "#6b2d21", "#84402d")
RACK = Ramp("#1e0408", "#350911", "#4f111a", "#6b1b24", "#88292f", "#a63d3c")
CLOT = Ramp("#120103", "#2a0207", "#45050d", "#650a15", "#8a1420", "#b8303a")
ASHEN = Ramp("#2a2626", "#3d3837", "#524b49", "#6a615e", "#847a75", "#a19691")
CORRIDOR = Ramp("#171515", "#262222", "#363130", "#47403e", "#5a514e", "#6f6460")
ABYSS = Ramp("#050407", "#0c0910", "#140f1a", "#1d1625", "#281e32", "#352842")
IRON = Ramp("#070708", "#111114", "#1b1b20", "#26262d", "#33333b", "#43434d")
RUST = Ramp("#1e0e08", "#3a1a0d", "#5a2a14", "#7a3d1c", "#985428", "#b46d3a")
VIOLET_GEM = Ramp("#2a0a3a", "#4a1466", "#7020a0", "#9a3ad0", "#c070f0", "#ecc0ff")
GLOW_RED = [hexc("#7a0a0a"), hexc("#b3121a"), hexc("#ff3b1f"), hexc("#ff7a3a"), hexc("#ffc080")]
EMBER = [hexc("#ff4a0a"), hexc("#ff8a1e"), hexc("#ffc04a")]
CLEAR = (0, 0, 0, 0)


def bricks(ramp, seed, rows=4, mortar=0, course=8):
    """Running-bond bricks: rows of `course`-wide bricks offset every other row."""
    t = Tex(16, 16, seed)
    h = 16 // rows
    for y in range(16):
        r = y // h
        off = (course // 2) * (r % 2)
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 2, 4.0)
            v = 2.6 + (n - 0.5) * 2.4
            edge_y = y % h == h - 1
            edge_x = (x + off) % course == course - 1
            if edge_y or edge_x:
                t.set(x, y, ramp[mortar])
            else:
                if y % h == 0:
                    v += 0.7
                t.set(x, y, ramp[max(0, min(5, int(round(v))))])
    return t


def veins(t, ramp, seed, count=4, dark=True):
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(5, 10)):
            t.set(x % 16, y % 16, ramp[0 if dark else 5])
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1, 1))


# --- caverns ---------------------------------------------------------------------------------------

def hellstone():
    t = stone_base(HELLSTONE, 101, 1.1)
    veins(t, HELLSTONE, 102, 3)
    return t


def rack_stone():
    t = stone_base(RACK, 111, 1.2)
    veins(t, RACK, 112, 5)
    # Sinew: pale streaks like stretched tissue.
    rng = random.Random(113)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        for i in range(4):
            t.set((x + i) % 16, y, RACK[5])
    return t


def congealed_blood():
    t = Tex(16, 16, 121)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 121, 16, 16, 3, 3.0)
            t.set(x, y, CLOT[int(round(1.5 + n * 3.2))])
    rng = random.Random(122)
    for _ in range(5):
        x, y = rng.randrange(15), rng.randrange(15)
        t.set(x, y, CLOT[5])
        t.set(x + 1, y, mix(CLOT[5], CLOT[4], 0.5))
    return t


def ash_block():
    t = stone_base(ASHEN, 131, 0.7)
    rng = random.Random(132)
    for _ in range(14):
        t.set(rng.randrange(16), rng.randrange(16), ASHEN[5] if rng.random() < 0.6 else ASHEN[0])
    return t


def brimstone_ore():
    return ore(HELLSTONE, P.SULFUR, 141, [(1, 2, CRYSTAL_C), (9, 1, CRYSTAL_B), (8, 9, CRYSTAL_A), (2, 11, CRYSTAL_B)])


def abyssal_shard_ore():
    return ore(ABYSS, VIOLET_GEM, 151, [(2, 1, CRYSTAL_B), (9, 3, CRYSTAL_C), (3, 9, CRYSTAL_A), (11, 11, CRYSTAL_B)])


def corridor_stone():
    t = stone_base(CORRIDOR, 161, 0.6)
    return t


def corridor_bricks():
    # Large cell-block masonry: two courses of long blocks.
    return bricks(CORRIDOR, 171, rows=2, mortar=0, course=16)


def abyssal_stone():
    t = stone_base(ABYSS, 181, 1.0)
    rng = random.Random(182)
    for _ in range(3):
        t.set(rng.randrange(16), rng.randrange(16), VIOLET_GEM[1])
    return t


def hellstone_bricks():
    return bricks(HELLSTONE, 191, rows=4, mortar=0)


def hellfire_vent_top():
    t = stone_base(P.ASH, 201, 0.9)
    # A ragged crack through the middle, glowing.
    rng = random.Random(202)
    x = 8
    for y in range(16):
        for dx in (-1, 0, 1):
            if dx == 0 or rng.random() < 0.5:
                d = abs(dx)
                t.set((x + dx) % 16, y, EMBER[2 - d * 2 if d < 2 else 0] if d == 0 else EMBER[0])
        x += rng.choice((-1, 0, 0, 1))
    for _ in range(4):
        cx, cy = rng.randrange(16), rng.randrange(16)
        t.set(cx, cy, EMBER[0])
    return t


def hellfire_vent_side():
    t = stone_base(HELLSTONE, 211, 1.0)
    for x in range(16):
        if (x * 7) % 5 < 3:
            t.set(x, 0, EMBER[1])
            if (x * 3) % 4 == 0:
                t.set(x, 1, EMBER[0])
    return t


# --- the Cage --------------------------------------------------------------------------------------

ENOCHIAN = [
    ["x.x", "xxx", "..x"], ["xx.", ".x.", ".xx"], ["x..", "xxx", "x.x"], [".x.", "x.x", "xxx"],
    ["xxx", "x..", "xx."], ["x.x", ".x.", "x.x"], ["xx.", "x.x", ".xx"], [".xx", "xx.", "..x"],
]


def glyph(t, ox, oy, idx, color):
    for y, row in enumerate(ENOCHIAN[idx % len(ENOCHIAN)]):
        for x, ch in enumerate(row):
            if ch == "x":
                t.set(ox + x, oy + y, color)


def iron_plate(seed):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 2, 5.0)
            t.set(x, y, IRON[int(round(1.8 + (n - 0.5) * 1.6))])
    for i in range(16):
        t.set(i, 0, IRON[4])
        t.set(0, i, IRON[4])
        t.set(i, 15, IRON[0])
        t.set(15, i, IRON[0])
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.set(x, y, IRON[5])
        t.set(x + 1, y + 1, IRON[0])
    return t


def cage_frame():
    t = iron_plate(301)
    glyph(t, 4, 6, 1, shade(GLOW_RED[0], 0.9))
    glyph(t, 9, 6, 4, shade(GLOW_RED[0], 0.9))
    return t


def cage_seal():
    t = iron_plate(311)
    # A ring of fire with a sigil at its heart: one of the sixty-six seals.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 5.2 <= d <= 6.4:
                t.set(x, y, GLOW_RED[2] if (x + y) % 3 else GLOW_RED[3])
    glyph(t, 5, 5, 6, GLOW_RED[3])
    glyph(t, 8, 8, 2, GLOW_RED[2])
    t.set(8, 7, GLOW_RED[4])
    return t


def cage_bars():
    t = Tex(16, 16, 321)
    for x in (1, 2, 7, 8, 13, 14):
        for y in range(16):
            n = fbm(x, y, 321, 16, 16, 2, 4.0)
            c = IRON[3 if x in (1, 7, 13) else 2] if n > 0.3 else IRON[1]
            t.set(x, y, c)
    for y in (0, 15):
        for x in range(16):
            t.set(x, y, IRON[2])
    for (x, y) in ((1, 5), (7, 9), (13, 4), (2, 11), (8, 3), (14, 12)):
        t.set(x, y, GLOW_RED[1])
    return t


def cage_chain():
    """Two planes of links, as the vanilla chain model expects (columns 0-2 and 3-5)."""
    t = Tex(16, 16, 331)
    for col0 in (0, 3):
        for y in range(16):
            phase = (y + (2 if col0 else 0)) % 4
            if phase in (0, 3):
                t.set(col0, y, IRON[3])
                t.set(col0 + 2, y, IRON[2])
            else:
                t.set(col0, y, IRON[3])
                t.set(col0 + 2, y, IRON[1])
                if phase == 1:
                    t.set(col0 + 1, y, IRON[4])
    return t


def abyssal_bedrock():
    t = stone_base(ABYSS, 341, 1.6)
    rng = random.Random(342)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, ABYSS[0])
        t.set((x + 1) % 16, y, ABYSS[0])
    return t


def abyssal_flagstone():
    t = Tex(16, 16, 351)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 351, 16, 16, 2, 4.0)
            v = 2.4 + (n - 0.5) * 1.8 + (0.4 if (x // 8 + y // 8) % 2 else 0)
            t.set(x, y, ABYSS[max(0, min(5, int(round(v))))])
    for i in range(16):
        t.set(i, 0, ABYSS[0])
        t.set(0, i, ABYSS[0])
        t.set(i, 8, ABYSS[0])
        t.set(8, i, ABYSS[0])
    return t


def enochian_pillar():
    t = Tex(16, 16, 361)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 361, 16, 16, 2, 5.0)
            fluting = 0.6 if x % 4 == 1 else (-0.7 if x % 4 == 3 else 0)
            t.set(x, y, ABYSS[max(0, min(5, int(round(2.6 + (n - 0.5) * 1.4 + fluting))))])
    for i, oy in enumerate((1, 6, 11)):
        glyph(t, 6, oy, i * 3 + 1, GLOW_RED[1])
    return t


def enochian_pillar_top():
    t = Tex(16, 16, 371)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            v = 3.2 - d * 0.18
            if abs(d - 5.5) < 0.6:
                v = 1
            t.set(x, y, ABYSS[max(0, min(5, int(round(v))))])
    glyph(t, 7, 7, 3, GLOW_RED[1])
    return t


def brazier_side():
    t = iron_plate(381)
    for x in range(16):
        t.set(x, 1, EMBER[0] if x % 3 else EMBER[1])
    return t


def brazier_top():
    t = Tex(16, 16, 391)
    rng = random.Random(391)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                t.set(x, y, IRON[3])
            else:
                n = fbm(x, y, 391, 16, 16, 3, 3.0)
                t.set(x, y, EMBER[min(2, int(n * 3.2))] if n > 0.3 else GLOW_RED[1])
    for _ in range(6):
        t.set(rng.randrange(1, 15), rng.randrange(1, 15), GLOW_RED[4])
    return t


def cage_ritual_stone():
    t = Tex(16, 16, 401)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 401, 16, 16, 2, 4.0)
            t.set(x, y, RACK[max(0, min(5, int(round(1.4 + (n - 0.5) * 1.4))))])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 6.0 <= d <= 6.9:
                t.set(x, y, GLOW_RED[2])
    glyph(t, 6, 6, 5, GLOW_RED[3])
    for (x, y) in ((8, 1), (8, 14), (1, 8), (14, 8)):
        t.set(x, y, GLOW_RED[4])
    return t


def cage_ritual_stone_side():
    t = Tex(16, 16, 411)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 411, 16, 16, 2, 4.0)
            t.set(x, y, RACK[max(0, min(5, int(round(1.2 + (n - 0.5) * 1.2))))])
    for x in range(16):
        t.set(x, 0, GLOW_RED[1])
    glyph(t, 6, 6, 7, GLOW_RED[1])
    return t


def meat_hook():
    t = Tex(16, 16, 421)
    # A short chain at the top, then the hook's shank and its curve.
    for y in range(0, 6):
        t.set(8, y, IRON[3] if y % 2 else IRON[1])
        if y % 2 == 0:
            t.set(7, y, IRON[2])
            t.set(9, y, IRON[2])
    for y in range(6, 12):
        t.set(8, y, RUST[3])
        t.set(9, y, RUST[1])
    curve = [(8, 12), (8, 13), (7, 14), (6, 14), (5, 13), (5, 12), (5, 11)]
    for (x, y) in curve:
        t.set(x, y, RUST[4])
    t.set(5, 10, RUST[5])
    for (x, y) in ((8, 9), (6, 13), (9, 11)):
        t.set(x, y, P.BLOOD[3])
    return t


# --- the rift -----------------------------------------------------------------------------------------

RIFT_FRAMES = 16


def rift():
    """A strip of frames: a red tear swirling over black, translucent at its edges."""
    t = Tex(16, 16 * RIFT_FRAMES, 501)
    for f in range(RIFT_FRAMES):
        ph = f / RIFT_FRAMES * math.tau
        for y in range(16):
            for x in range(16):
                cx, cy = x + 0.5 - 8, y + 0.5 - 8
                ang = math.atan2(cy, cx)
                d = math.hypot(cx, cy)
                swirl = math.sin(ang * 3 + d * 0.9 - ph * 2) * 0.5 + 0.5
                n = fbm(int(x + math.cos(ph) * 4) % 16, int(y + math.sin(ph) * 4) % 16, 502, 16, 16, 2, 4.0)
                v = swirl * 0.6 + n * 0.6
                if v > 0.85:
                    c, a = GLOW_RED[4], 235
                elif v > 0.68:
                    c, a = GLOW_RED[3], 225
                elif v > 0.5:
                    c, a = GLOW_RED[2], 210
                elif v > 0.32:
                    c, a = GLOW_RED[1], 200
                else:
                    c, a = hexc("#120204"), 190
                t.set(x, y + f * 16, (c[0], c[1], c[2], a))
    return t


def write_rift():
    save(rift(), "block", "hell_rift")
    path = os.path.join(TEXTURES, "block", "hell_rift.png.mcmeta")
    with open(path, "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh, indent=1)
        fh.write("\n")


# --- items --------------------------------------------------------------------------------------------

def item_outline(t, color=P.OUTLINE):
    rows = t.copy_rows()
    for y in range(16):
        for x in range(16):
            if rows[y][x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and rows[ny][nx][3]:
                    t.set(x, y, color)
                    break


def brimstone():
    t = Tex(16, 16, 601)
    for y in range(16):
        for x in range(16):
            d = math.hypot((x + 0.5 - 8) * 1.1, (y + 0.5 - 9))
            if d < 5.2 - fbm(x, y, 601, 16, 16, 2, 3.0) * 1.6:
                n = fbm(x, y, 602, 16, 16, 2, 3.0)
                t.set(x, y, P.SULFUR[int(round(1.5 + n * 3.5))])
    for (x, y) in ((7, 7), (9, 9), (6, 10)):
        t.set(x, y, EMBER[1])
    item_outline(t)
    return t


def rack_hook():
    t = Tex(16, 16, 611)
    for y in range(2, 10):
        t.set(9, y, RUST[3])
        t.set(10, y, RUST[1])
    for (x, y) in ((9, 10), (9, 11), (8, 12), (7, 12), (6, 11), (6, 10), (6, 9)):
        t.set(x, y, RUST[4])
    t.set(6, 8, RUST[5])
    t.set(9, 1, IRON[4])
    t.set(10, 1, IRON[3])
    t.set(9, 0, IRON[3])
    for (x, y) in ((9, 6), (8, 12)):
        t.set(x, y, P.BLOOD[3])
    item_outline(t)
    return t


def damned_contract():
    t = Tex(16, 16, 621)
    for y in range(2, 14):
        for x in range(3, 13):
            n = fbm(x, y, 621, 16, 16, 2, 4.0)
            t.set(x, y, P.PARCHMENT[int(round(2.6 + (n - 0.5) * 1.6))])
    for y in range(4, 11, 2):
        for x in range(5, 11):
            if (x * 3 + y) % 5:
                t.set(x, y, P.PARCHMENT[0])
    for (x, y) in ((6, 12), (7, 11), (8, 12), (9, 11), (10, 12)):
        t.set(x, y, P.BLOOD[3])
    t.set(11, 3, P.BLOOD[4])
    t.set(12, 4, P.BLOOD[2])
    item_outline(t)
    return t


def abyssal_shard():
    t = Tex(16, 16, 631)
    pts = [(8, 1), (11, 6), (10, 13), (7, 14), (5, 8)]
    for y in range(16):
        for x in range(16):
            inside = all(((pts[(i + 1) % 5][0] - pts[i][0]) * (y - pts[i][1]) - (pts[(i + 1) % 5][1] - pts[i][1]) * (x - pts[i][0])) >= 0
                         for i in range(5))
            if inside:
                v = 2 + (x - 5) * 0.35 - (y - 7) * 0.08
                t.set(x, y, VIOLET_GEM[max(0, min(5, int(round(v))))])
    t.set(8, 4, VIOLET_GEM[5])
    t.set(9, 6, VIOLET_GEM[5])
    item_outline(t)
    return t


def hellhound_fang():
    t = Tex(16, 16, 641)
    bone = Ramp("#5a4a38", "#8a7658", "#b8a382", "#d8c6a6", "#efe2c8", "#fffaf0")
    for i in range(10):
        x = 5 + int(round(i * 0.55 + math.sin(i * 0.35) * 1.5))
        y = 3 + i
        width = max(1, 3 - i // 4)
        for w in range(width):
            t.set(x + w, y, bone[4 - w])
    t.set(10, 13, P.BLOOD[4])
    t.set(10, 12, P.BLOOD[3])
    item_outline(t)
    return t


def fallen_star():
    t = Tex(16, 16, 651)
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5 - 8, y + 0.5 - 8
            ang, d = math.atan2(cy, cx), math.hypot(cx, cy)
            r = 3.2 + 3.6 * max(0, math.cos(ang * 4)) ** 4
            if d < r:
                k = d / r
                c = mix(hexc("#ffffff"), P.GOLD[4], k) if k < 0.7 else P.GOLD[3]
                t.set(x, y, c)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        t.set(x, y, GLOW_RED[2])
    item_outline(t, hexc("#3a1a04"))
    return t


def ring(band, gem, seed):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            d = math.hypot((x + 0.5 - 8) * 1.0, (y + 0.5 - 9.5) * 1.25)
            if 3.4 <= d <= 5.0:
                k = (x + y) / 30
                t.set(x, y, band[max(1, min(5, int(round(4 - k * 3 + (1 if y < 9 else 0)))))])
    for y in range(2, 7):
        for x in range(6, 10):
            if (x in (6, 9)) and y in (2, 6):
                continue
            v = 3 + (1 if (x, y) == (7, 3) else 0) - (1 if y >= 5 else 0)
            t.set(x, y, gem[max(0, min(5, v))])
    t.set(7, 3, gem[5])
    item_outline(t)
    return t


WAR_GEM = Ramp("#3a0204", "#6a0508", "#a00c12", "#d01a1e", "#f05050", "#ffc0c0")
FAMINE_GEM = Ramp("#050505", "#101012", "#1c1c22", "#2c2c36", "#44444f", "#8a8a96")
PESTILENCE_GEM = Ramp("#0a2a08", "#145010", "#2a7a1a", "#4aa02a", "#7cd04a", "#d0ffa0")
DEATH_GEM = Ramp("#4a4a50", "#76767e", "#a4a4ac", "#cfcfd6", "#eeeef2", "#ffffff")
DARK_SILVER = Ramp("#1a1a1e", "#34343a", "#55555e", "#7a7a84", "#a4a4ae", "#d0d0d8")


ITEMS = {
    "brimstone": brimstone, "rack_hook": rack_hook, "damned_contract": damned_contract, "abyssal_shard": abyssal_shard,
    "hellhound_fang": hellhound_fang, "fallen_star": fallen_star,
    "ring_of_war": lambda: ring(P.GOLD, WAR_GEM, 661),
    "ring_of_famine": lambda: ring(P.GOLD, FAMINE_GEM, 662),
    "ring_of_pestilence": lambda: ring(DARK_SILVER, PESTILENCE_GEM, 663),
    "ring_of_death": lambda: ring(DARK_SILVER, DEATH_GEM, 664),
}

BLOCKS = {
    "hellstone": hellstone, "hellstone_bricks": hellstone_bricks, "rack_stone": rack_stone, "congealed_blood": congealed_blood,
    "ash_block": ash_block, "brimstone_ore": brimstone_ore, "corridor_stone": corridor_stone, "corridor_bricks": corridor_bricks,
    "abyssal_stone": abyssal_stone, "abyssal_shard_ore": abyssal_shard_ore, "hellfire_vent_top": hellfire_vent_top,
    "hellfire_vent_side": hellfire_vent_side, "cage_frame": cage_frame, "cage_seal": cage_seal, "cage_bars": cage_bars,
    "cage_chain": cage_chain, "abyssal_bedrock": abyssal_bedrock, "abyssal_flagstone": abyssal_flagstone,
    "enochian_pillar": enochian_pillar, "enochian_pillar_top": enochian_pillar_top, "hellfire_brazier_side": brazier_side,
    "hellfire_brazier_top": brazier_top, "cage_ritual_stone": cage_ritual_stone, "cage_ritual_stone_side": cage_ritual_stone_side,
    "meat_hook": meat_hook,
}


def generate():
    for name, fn in BLOCKS.items():
        save(fn(), "block", name)
    write_rift()
    for name, fn in ITEMS.items():
        save(fn(), "item", name)
