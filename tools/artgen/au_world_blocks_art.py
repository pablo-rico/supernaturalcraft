"""The Apocalypse World's blocks (v0.41), 16x16, the list agent B posted in apocworld/AuBlocksArt.md:

  scorched_stone        dark charcoal-brown rock, coarse, faint rust-red streaks
  scorched_soil_top     charred earth from above: black-brown, grey ash flecks, thin orange ember cracks
  scorched_soil         its side (and bottom): dry dark-brown dirt, a charred band at the top
  au_ash_block          soft pale grey ash, darker grey and black flecks (ash_layer reuses it). NOT `ash_block`: that texture
                        is the Hell's ash block (hell_art, AllBlocks.ASH_BLOCK), and the id `ash_block` is taken
  smouldering_ash       dark grey ash, buried embers: glowing orange-red specks, a crack or two
  burned_log            charred bark: black, alligator-cracked, a deep orange glow only in the deepest cracks
  burned_log_top        its rings: charcoal grey, a black rim, a dull ember at the heart
  burned_planks         blackened scorched planks, the grain still showing, a few burnt-through holes
  rubble                broken grey concrete chunks, soot, bits of rusted rebar
  ruined_bricks         soot-stained red brick, cracked, mortar missing, black scorch smears
  wrecked_metal         a burned car's body steel: faded paint (dull red, dull blue) under rust and soot, a dent line
  citadel_stone         Michael's fortress: black polished basalt bricks, thin dark-gold seams between them
  camp_lock             a riveted steel plate, two chains crossed over it, a heavy padlock in the middle
  au_rift               a tear into the burned world (animated, 16 frames): black at the edges, ember light, swirling ash
"""

import json
import math
import os
import random

from chuck_art import h01
from common import TEXTURES, save
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

STONE = Ramp("#120e0c", "#1e1814", "#2a221c", "#372d25", "#463a30", "#56483c")
SOIL = Ramp("#120c08", "#1e140e", "#2c1e14", "#3a281a", "#4a3422", "#5c422c")
ASH = Ramp("#4a4846", "#646260", "#7e7c79", "#979592", "#b0aeaa", "#c8c6c2")
ASH_D = Ramp("#18171a", "#242326", "#323134", "#424044", "#545256", "#686669")
EMBER = Ramp("#3a0604", "#6e0d06", "#a8200a", "#e0430f", "#ff7a26", "#ffc46a")
CHAR = Ramp("#050404", "#0c0a09", "#151211", "#1f1b19", "#2b2623", "#3a3430")
WOOD = Ramp("#120c08", "#1e150e", "#2a1d14", "#38271a", "#463222", "#563e2a")
CONCRETE = Ramp("#2e2d2c", "#454341", "#5c5a57", "#74716d", "#8c8984", "#a6a29c")
RUST = Ramp("#2a1208", "#45200e", "#643016", "#86421e", "#a65a2a", "#c47a40")
BRICK = Ramp("#2a0e0a", "#441812", "#5e221a", "#782e22", "#90402e", "#a8543c")
MORTAR = Ramp("#2a2826", "#3e3a36", "#545048", "#6a645c", "#807a70", "#968e84")
BASALT = Ramp("#040405", "#08080a", "#0e0e11", "#151519", "#1e1e24", "#2c2c34")
GOLD = Ramp("#3a2606", "#5a3c0c", "#7e5614", "#a4721e", "#c89030", "#e8b450")
STEEL = Ramp("#1e2024", "#33363c", "#4a4e56", "#636871", "#7e848e", "#9ca2ac")
PAINT_R = Ramp("#2a0c0a", "#401410", "#581c16", "#6e261e", "#843428", "#984436")
PAINT_B = Ramp("#0c1420", "#141e30", "#1c2a40", "#263652", "#304462", "#3c5272")


def tex(fn, seed, w=16, h=16):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def n2(x, y, seed, scale=5.0, octaves=3):
    return fbm(x, y, seed, 16, 16, octaves, scale)


def scorched_stone():
    def f(x, y):
        n = n2(x, y, 56001, 4.0)
        c = STONE[1.2 + n * 3.2]
        if n2(x * 2, y, 56002, 3.0, 2) > 0.7 and (x + y) % 3 != 0:
            c = mix(c, RUST[2], 0.35)
        if h01("pit", x, y) < 0.05:
            c = STONE[0]
        return c
    return tex(f, 56001)


def soil_top():
    def f(x, y):
        n = n2(x, y, 56010, 4.0)
        c = SOIL[0.8 + n * 2.4]
        r = h01("ash", x, y)
        if r < 0.1:
            c = ASH[1 + 2 * r * 10 % 3]
        crack = abs(math.sin(x * 0.9 + 2.2 * math.sin(y * 0.5))) < 0.12 and n > 0.45
        if crack:
            c = EMBER[3] if n > 0.6 else EMBER[2]
        return c
    return tex(f, 56010)


def soil_side():
    def f(x, y):
        n = n2(x, y, 56011, 4.0)
        c = SOIL[1.4 + n * 2.4 - y * 0.03]
        if y < 3 + (1 if h01("e", x) < 0.4 else 0):
            c = mix(CHAR[1 + n * 2], ASH[1], 0.2 if h01("a", x, y) < 0.2 else 0)
        if h01("st", x, y) < 0.04:
            c = STONE[3]
        return c
    return tex(f, 56011)


def ash_block():
    def f(x, y):
        n = n2(x, y, 56020, 5.0)
        c = ASH[2.0 + n * 2.4]
        r = h01("fl", x, y)
        if r < 0.05:
            c = ASH_D[1]
        elif r < 0.11:
            c = ASH[1]
        return c
    return tex(f, 56020)


def smouldering():
    def f(x, y):
        n = n2(x, y, 56030, 5.0)
        c = ASH_D[1.6 + n * 2.4]
        r = h01("em", x, y)
        if r < 0.05:
            c = EMBER[4] if r < 0.02 else EMBER[3]
        crack = abs(math.sin(x * 0.7 + 1.8 * math.sin(y * 0.6) + 1.0)) < 0.1 and n > 0.5
        if crack:
            c = EMBER[4] if n > 0.65 else EMBER[2]
        return c
    return tex(f, 56030)


def burned_log():
    cols = [0, 3, 5, 9, 12, 14]                 # the bark's deep vertical fissures

    def f(x, y):
        n = n2(x, y, 56040, 4.0)
        wob = int(round(1.2 * math.sin(y * 0.5 + x)))
        fissure = any((x + wob) % 16 == cx for cx in cols)
        # Alligator charring: each plate between two fissures breaks across now and then.
        plate = sum(1 for cx in cols if cx <= x)
        brk = (y + int(h01("brk", plate) * 16)) % 7 == 0
        if fissure:
            return EMBER[2] if (n > 0.66 and h01("gl", x, y // 3) < 0.5) else CHAR[0]
        if brk:
            return CHAR[0] if h01("bk", x, y) < 0.7 else CHAR[1]
        return CHAR[1.6 + n * 2.4 + (0.7 if (x + wob) % 16 in [c + 1 for c in cols] else 0)]
    return tex(f, 56040)


def burned_log_top():
    def f(x, y):
        d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
        if d > 7.0:
            return CHAR[0]
        if d < 1.6:
            return EMBER[2] if d < 0.9 else CHAR[2]
        ring = int(d) % 2
        n = n2(x, y, 56041, 4.0)
        c = CHAR[2.4 + ring * 0.8 + n]
        if h01("ck", x, y) < 0.05:
            c = CHAR[0]
        return c
    return tex(f, 56041)


def burned_planks():
    def f(x, y):
        row = y // 4
        if y % 4 == 3:
            return CHAR[0]
        seam = (x + row * 5) % 16 == 0
        if seam:
            return CHAR[0]
        n = n2(x * 0.5, y * 2, 56050 + row, 4.0)
        c = mix(WOOD[1.2 + n * 2.4], CHAR[2], 0.5 + 0.3 * math.sin(x * 0.4 + row))
        if (x * 3 + row) % 7 == 0:
            c = shade(c, 1.2)                     # the grain
        if h01("hole", row, x // 3) < 0.07 and 1 <= y % 4 <= 2:
            return CHAR[0]                        # burnt through
        return c
    return tex(f, 56050)


def rubble():
    rng = random.Random(56060)
    chunks = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(2.0, 4.2), rng.random()) for _ in range(9)]

    def f(x, y):
        best = None
        for cx, cy, r, k in chunks:
            d = math.hypot(((x - cx + 8) % 16) - 8, ((y - cy + 8) % 16) - 8) / r
            if d < 1 and (best is None or d < best[0]):
                best = (d, k, cx, cy)
        if best is None:
            return mix(CONCRETE[0], CHAR[1], 0.5)
        d, k, cx, cy = best
        lit = (((x - cx + 8) % 16) - 8) + (((y - cy + 8) % 16) - 8) < 0
        c = CONCRETE[2.2 + 1.4 * k + (0.8 if lit else -0.4) - d]
        if h01("soot", x, y) < 0.15:
            c = mix(c, CHAR[1], 0.4)
        return c
    t = tex(f, 56060)
    for x, y in ((3, 12), (4, 11), (5, 10), (6, 9), (11, 3), (12, 3), (13, 3)):
        t.set(x, y, RUST[3] if (x + y) % 2 else RUST[2])          # rebar
    return t


def ruined_bricks():
    def f(x, y):
        row = y // 4
        off = 4 if row % 2 else 0
        mortar = y % 4 == 3 or (x + off) % 8 == 7
        n = n2(x, y, 56070, 4.0)
        if mortar:
            return MORTAR[1 + n * 2] if h01("mo", x, y) > 0.3 else CHAR[1]   # mortar, missing in places
        c = BRICK[1.8 + n * 2.2 + (0.4 if y % 4 == 0 else 0)]
        if n2(x, y, 56071, 6.0, 2) > 0.62:
            c = mix(c, CHAR[1], 0.6)                                         # scorch smears
        if h01("cr", x, y) < 0.04:
            c = BRICK[0]
        return c
    return tex(f, 56070)


def wrecked_metal():
    def f(x, y):
        n = n2(x, y, 56080, 5.0)
        paint = PAINT_R if x < 9 else PAINT_B
        c = paint[2 + n * 2]
        rust = n2(x, y, 56081, 4.0)
        if rust > 0.55:
            c = mix(c, RUST[2 + (rust - 0.55) * 8], 0.8)
        if n2(x, y, 56082, 6.0, 2) > 0.65:
            c = mix(c, CHAR[1], 0.6)
        if y == 9 or (y == 10 and x % 3 == 0):
            c = shade(c, 0.6 if y == 9 else 1.3)                             # the dent line
        if x in (8, 9) and y % 5 == 2:
            c = STEEL[2]
        return c
    return tex(f, 56080)


def citadel_stone():
    def f(x, y):
        row = y // 8
        off = 4 if row % 2 else 0
        seam_h = y % 8 == 7
        seam_v = (x + off) % 8 == 7
        if seam_h or seam_v:
            return GOLD[2] if (x + y) % 3 else GOLD[3]
        n = n2(x, y, 56090, 5.0)
        c = BASALT[1.4 + n * 2.0]
        u, v = (x + off) % 8, y % 8
        if u + v < 3:
            c = BASALT[4]                                                    # the polish catches the light
        if u + v > 11:
            c = BASALT[0]
        return c
    return tex(f, 56090)


def camp_lock():
    def f(x, y):
        n = n2(x, y, 56100, 5.0)
        c = STEEL[1.6 + n * 2.0]
        if x in (0, 15) or y in (0, 15):
            c = STEEL[0]
        if (x in (2, 13) and y in (2, 13)):
            c = STEEL[5]                                                      # rivets
        elif (x in (2, 13) and y in (1, 3)) or (x in (1, 3, 12, 14) and y in (2, 13)) or (x in (2, 13) and y in (12, 14)):
            c = STEEL[0]
        if n2(x, y, 56101, 4.0) > 0.66:
            c = mix(c, RUST[2], 0.5)
        return c
    t = tex(f, 56100)
    # Two chains crossed over the plate.
    for i in range(16):
        for (x, y, dx) in ((i, i, 1), (15 - i, i, -1)):
            link = i % 3
            t.set(x, y, STEEL[5] if link == 0 else (STEEL[4] if link == 1 else STEEL[0]))
            if 0 <= x + dx < 16:
                t.set(x + dx, y, STEEL[3] if link != 2 else STEEL[0])
    # The padlock: a shackle and a heavy body.
    for y in range(4, 8):
        for x in range(6, 10):
            if x in (6, 9) or y == 4:
                t.set(x, y, STEEL[4] if x == 6 else STEEL[3])
    for y in range(7, 13):
        for x in range(5, 11):
            c = hexc("#5e4519") if (x + y) % 5 else hexc("#836226")
            if x == 5 or y == 7:
                c = hexc("#a98236")
            if x == 10 or y == 12:
                c = hexc("#3b2a10")
            t.set(x, y, c)
    t.set(7, 9, hexc("#0a0806"))
    t.set(8, 9, hexc("#0a0806"))
    t.set(8, 10, hexc("#0a0806"))
    return t


RIFT_FRAMES = 16


def au_rift():
    t = Tex(16, 16 * RIFT_FRAMES, 56110)
    rng = random.Random(56111)
    flakes = [(rng.randrange(16), rng.randrange(16), rng.random() * math.tau) for _ in range(7)]
    for f_ in range(RIFT_FRAMES):
        ph = f_ / RIFT_FRAMES * math.tau
        for y in range(16):
            for x in range(16):
                cx, cy = x + 0.5 - 8, y + 0.5 - 8
                ang = math.atan2(cy, cx)
                d = math.hypot(cx, cy)
                swirl = math.sin(ang * 2 - d * 0.7 + ph) * 0.5 + 0.5
                n = fbm(int(x + math.cos(ph) * 3) % 16, int(y + f_) % 16, 56112, 16, 16, 2, 4.0)
                v = swirl * 0.5 + n * 0.7 - max(0.0, (d - 5.5)) * 0.18      # darker at the edges
                if v > 0.92:
                    c, a = EMBER[5], 235
                elif v > 0.76:
                    c, a = EMBER[4], 225
                elif v > 0.58:
                    c, a = EMBER[2], 210
                elif v > 0.4:
                    c, a = mix(EMBER[0], CHAR[2], 0.4), 200
                else:
                    c, a = CHAR[0], 210
                t.set(x, y + f_ * 16, (c[0], c[1], c[2], a))
        for fx, fy, p in flakes:
            k = math.sin(ph + p)
            if k > 0.0:
                c = ASH[4] if k > 0.6 else ASH[2]
                t.set(fx, (fy + f_) % 16 + f_ * 16, (c[0], c[1], c[2], 230))
    return t


BLOCKS = {"scorched_stone": scorched_stone, "scorched_soil_top": soil_top, "scorched_soil": soil_side, "au_ash_block": ash_block,
          "smouldering_ash": smouldering, "burned_log": burned_log, "burned_log_top": burned_log_top, "burned_planks": burned_planks,
          "rubble": rubble, "ruined_bricks": ruined_bricks, "wrecked_metal": wrecked_metal, "citadel_stone": citadel_stone,
          "camp_lock": camp_lock}


def generate():
    for name, fn in BLOCKS.items():
        save(fn(), "block", name)
    save(au_rift(), "block", "au_rift")
    path = os.path.join(TEXTURES, "block", "au_rift.png.mcmeta")
    with open(path, "w") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh, indent=1)
        fh.write("\n")


if __name__ == "__main__":
    generate()
