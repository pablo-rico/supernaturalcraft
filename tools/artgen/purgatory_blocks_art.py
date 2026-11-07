"""Purgatory's blocks (v0.39): wood, leaves, stone, soul amber and the rift (names in purgatory/PurgatoryAssets.java).

Purgatory is an endless grey forest at permanent dusk: ash-pale bark gone black in its furrows, leaves of a dark grey-green
with blue in it (drawn cutout, never biome-tinted: these are the final colours), blue-grey slate, warm amber with a soul
caught in it. Every texture is 16x16; the rift is a strip of animation frames with its .mcmeta, like Hell's.
"""

import json
import math
import os
import random

from blocks import stone_base
from common import TEXTURES, save
from hell_art import bricks
from pixelkit import Ramp, Tex, fbm, hexc, mix

BARK = Ramp("#14161a", "#262a30", "#3c4148", "#565c63", "#737a80", "#959b9f")
HEART = Ramp("#2a2d31", "#454a50", "#62686e", "#80878c", "#a1a7aa", "#c3c7c6")
PLANK = Ramp("#22262c", "#363c44", "#4b525b", "#626a72", "#7b838a", "#959ca2")
LEAF = Ramp("#101716", "#1d2826", "#2a3836", "#3a4a48", "#4e605d", "#687a78")
SLATE = Ramp("#14171d", "#21252d", "#2f343e", "#3e4450", "#4f5663", "#646b78")
AMBER = Ramp("#4a2205", "#7a3d08", "#b0640f", "#d98f1e", "#f2b844", "#ffe39a")
SOUL = hexc("#e8f4ff")
MIST = [hexc("#1a2230"), hexc("#3a4a5e"), hexc("#6a7e94"), hexc("#a8bccc"), hexc("#dfeaf2")]


def _v(ramp, v):
    return ramp[max(0, min(5, int(round(v))))]


def purgatory_log():
    """Bark: long vertical plates split by deep black furrows that wander, a knot here and there."""
    t = Tex(16, 16, 39001)
    rng = random.Random(39002)
    # Furrow lines: wandering columns.
    furrow = [[False] * 16 for _ in range(16)]
    for x0 in (1, 5, 9, 13):
        x = x0 + rng.choice((-1, 0, 0, 1))
        for y in range(16):
            furrow[y][x % 16] = True
            if rng.random() < 0.35:
                x += rng.choice((-1, 1))
    for y in range(16):
        for x in range(16):
            n = fbm(x * 2, y, 39003, 32, 16, 2, 4.0)
            if furrow[y][x]:
                c = BARK[0] if rng.random() < 0.7 else BARK[1]
            else:
                left = furrow[y][(x - 1) % 16]
                right = furrow[y][(x + 1) % 16]
                v = 3.2 + (n - 0.5) * 2.2 + (0.7 if left else 0) - (0.9 if right else 0)
                c = _v(BARK, v)
            t.set(x, y, c)
    # A knot: a dark ring with a pale centre.
    for (dx, dy, c) in ((0, 0, BARK[1]), (1, 0, BARK[0]), (-1, 0, BARK[0]), (0, 1, BARK[0]), (0, -1, BARK[1]), (0, 0, BARK[2])):
        t.set(11 + dx, 6 + dy, c)
    t.set(11, 6, HEART[1])
    return t


def purgatory_log_top():
    """The cut end: pale heartwood rings inside a ring of black bark."""
    t = Tex(16, 16, 39011)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) + (fbm(x, y, 39012, 16, 16, 2, 4.0) - 0.5) * 1.2
            if d > 7.0:
                c = _v(BARK, 1.5 + fbm(x, y, 39013, 16, 16, 2, 6.0) * 2)
            elif d > 6.2:
                c = BARK[0]
            else:
                wob = (fbm(x, y, 39014, 16, 16, 2, 8.0) - 0.5) * 1.6
                ring = (math.sin((d + wob) * 2.2) + 1) * 0.5
                c = _v(HEART, 1.8 + ring * 2.2 + (6 - d) * 0.15)
            t.set(x, y, c)
    t.set(8, 8, HEART[1])
    t.set(7, 7, HEART[2])
    # A radial crack.
    for (x, y) in ((9, 7), (10, 6), (10, 5), (11, 4), (12, 4), (12, 3)):
        t.set(x, y, HEART[0])
    return t


def purgatory_planks():
    """Four boards of weathered ash-grey wood, grain running along them, the odd nail and a split."""
    t = Tex(16, 16, 39021)
    rng = random.Random(39022)
    for y in range(16):
        board = y // 4
        off = (board * 5) % 16
        for x in range(16):
            n = fbm(x * 3 + board * 11, y, 39023 + board, 48, 16, 2, 4.0)
            v = 2.9 + (n - 0.5) * 1.8 + (0.6 if y % 4 == 0 else 0)
            if y % 4 == 3:
                c = PLANK[0]
            elif (x + off) % 16 == 0:
                c = PLANK[1]          # a board's butt joint
            else:
                c = _v(PLANK, v)
                if (x * 7 + y * 3 + board) % 11 == 0:
                    c = mix(c, PLANK[1], 0.6)   # grain streak
            t.set(x, y, c)
    for board in range(4):
        x = rng.randrange(2, 14)
        t.set(x, board * 4 + 1, PLANK[5])
    t.set(6, 9, PLANK[0])
    t.set(7, 9, PLANK[0])
    return t


def purgatory_leaves():
    """Clusters of dark grey-green leaves with blue in them, holes between (cutout)."""
    t = Tex(16, 16, 39031)
    rng = random.Random(39032)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 39033, 16, 16, 3, 4.0)
            m = fbm(x + 7, y + 3, 39034, 16, 16, 2, 8.0)
            if n < 0.43 or (m < 0.3 and rng.random() < 0.6) or rng.random() < 0.07:
                continue                       # a gap
            v = 1.4 + (n - 0.43) * 6.0
            c = _v(LEAF, v)
            if rng.random() < 0.06:
                c = mix(c, hexc("#6d7f88"), 0.35)   # a leaf gone grey
            t.set(x, y, c)
    # Small leaf highlights on the top-left of each cluster.
    for y in range(1, 16):
        for x in range(1, 16):
            here = t.get(x, y)
            if here[3] and not t.get(x - 1, y - 1)[3] and rng.random() < 0.5:
                t.set(x, y, LEAF[5])
    return t


def purgatory_stone():
    t = stone_base(SLATE, 39041, 1.0)
    # Slate: faint horizontal cleavage lines.
    for y in (4, 11):
        for x in range(16):
            if (x * 5 + y) % 7 < 4:
                t.set(x, y, mix(t.get(x, y), SLATE[1], 0.6))
    return t


def purgatory_cobblestone():
    """Rounded broken stones packed in dark grit."""
    t = Tex(16, 16, 39051)
    rng = random.Random(39052)
    centres = [(2, 2), (8, 1), (13, 3), (4, 7), (11, 8), (1, 12), (7, 12), (14, 13), (8, 6)]
    for y in range(16):
        for x in range(16):
            best, second, bc = 99.0, 99.0, None
            for cx, cy in centres:
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        d = math.hypot(x - cx - ox, y - cy - oy)
                        if d < best:
                            best, second, bc = d, best, (cx, cy)
                        elif d < second:
                            second = d
            if second - best < 1.1:
                c = SLATE[0]
            else:
                n = fbm(x, y, 39053, 16, 16, 2, 6.0)
                v = 2.2 + (n - 0.5) * 1.6 + (0.9 if y < bc[1] else 0) - best * 0.12
                c = _v(SLATE, v)
            t.set(x, y, c)
    for _ in range(6):
        t.set(rng.randrange(16), rng.randrange(16), SLATE[5])
    return t


def purgatory_stone_bricks():
    t = bricks(SLATE, 39061, rows=4, mortar=0, course=8)
    # Moss of the endless wood creeping into the mortar.
    rng = random.Random(39062)
    for _ in range(9):
        x, y = rng.randrange(16), rng.choice((3, 7, 11, 15))
        t.set(x, y, LEAF[2])
    return t


def soul_amber_ore():
    """Slate with nuggets of amber in it, each lit from above, a pale soul-wisp in the biggest."""
    t = stone_base(SLATE, 39071, 1.0)
    nuggets = [(4, 4, 2.9), (12, 3, 2.1), (10, 10, 3.2), (3, 12, 2.0)]
    for cx, cy, r in nuggets:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.15) + (fbm(x, y, 39072, 16, 16, 2, 8.0) - 0.5) * 1.4
                if d > r:
                    continue
                if d > r - 0.7:
                    c = AMBER[0]
                else:
                    v = 3.6 - (y + 0.5 - cy) * 0.9 - (x + 0.5 - cx) * 0.4
                    c = AMBER[max(1, min(5, int(round(v))))]
                t.set(x, y, c)
    t.set(10, 10, SOUL)
    t.set(9, 10, mix(SOUL, AMBER[4], 0.4))
    t.set(10, 9, mix(SOUL, AMBER[5], 0.5))
    return t


# --- the rift ---------------------------------------------------------------------------------------

RIFT_FRAMES = 16


def rift():
    """A strip of frames: grey-blue mist swirling in a tear, pale soul-light threading it, amber flecks."""
    t = Tex(16, 16 * RIFT_FRAMES, 39081)
    rng = random.Random(39082)
    flecks = [(rng.randrange(16), rng.randrange(16), rng.random() * math.tau) for _ in range(5)]
    for f in range(RIFT_FRAMES):
        ph = f / RIFT_FRAMES * math.tau
        for y in range(16):
            for x in range(16):
                cx, cy = x + 0.5 - 8, y + 0.5 - 8
                ang = math.atan2(cy, cx)
                d = math.hypot(cx, cy)
                swirl = math.sin(ang * 2 - d * 0.8 + ph) * 0.5 + 0.5
                n = fbm(int(x + math.cos(ph) * 3) % 16, int(y - f) % 16, 39083, 16, 16, 2, 4.0)
                v = swirl * 0.55 + n * 0.65
                if v > 0.9:
                    c, a = MIST[4], 230
                elif v > 0.72:
                    c, a = MIST[3], 215
                elif v > 0.52:
                    c, a = MIST[2], 200
                elif v > 0.34:
                    c, a = MIST[1], 190
                else:
                    c, a = MIST[0], 180
                t.set(x, y + f * 16, (c[0], c[1], c[2], a))
        for fx, fy, p in flecks:
            k = math.sin(ph + p)
            if k > 0.2:
                c = AMBER[4] if k > 0.7 else AMBER[3]
                t.set(fx, (fy - f) % 16 + f * 16, (c[0], c[1], c[2], 240))
    return t


def write_rift():
    save(rift(), "block", "purgatory_rift")
    path = os.path.join(TEXTURES, "block", "purgatory_rift.png.mcmeta")
    with open(path, "w") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh, indent=1)
        fh.write("\n")


def generate():
    save(purgatory_log(), "block", "purgatory_log")
    save(purgatory_log_top(), "block", "purgatory_log_top")
    save(purgatory_planks(), "block", "purgatory_planks")
    save(purgatory_leaves(), "block", "purgatory_leaves")
    save(purgatory_stone(), "block", "purgatory_stone")
    save(purgatory_cobblestone(), "block", "purgatory_cobblestone")
    save(purgatory_stone_bricks(), "block", "purgatory_stone_bricks")
    save(soul_amber_ore(), "block", "soul_amber_ore")
    write_rift()


if __name__ == "__main__":
    generate()
