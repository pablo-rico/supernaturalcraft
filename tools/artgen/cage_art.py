"""The Cage's fittings: textures and block models (elements) for every unbreakable piece of the Cage and its Pit.

Called from hell_art.generate(). Blockstates are written by datagen (SNBlockStateProvider.cageFittings), which only
points at the models written here.

  cage_bars        multipart like vanilla iron bars: an engraved post (cage_bars_post) + one side per joined side
                   (cage_bars_side: two rails and an intermediate bar); cage_bars_inventory for the item.
  cage_post        a thick Enochian post the bars run into (same side model); cage_post_inventory for the item.
  cage_chain       heavy links in alternating planes, tiling every 16 px along its axis.
  enochian_pillar  a carved shaft: core, four corner ribs and a joint band; enochian_capital its flared end.
  hellfire_brazier a hollow iron bowl on four clawed legs, a fire cage over it and animated hellfire.
  seal_lantern     an iron lantern caged round a burning seal (standing and hanging).
  chain_anchor     a wall plate with a ring and a link of chain (faces out of the wall).
  fallen_watcher   a bound angel kneeling on a plinth, its wings folded.
  full cubes       cage_frame, cage_seal, abyssal_bedrock, abyssal_flagstone (+ stairs/slab), abyssal_bricks,
                   cage_ritual_stone (+ side).
"""

import json
import math
import os
import random

from blockmodel import cube, model, write
from common import TEXTURES, save
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

IRON = Ramp("#08080a", "#121216", "#1d1d23", "#2a2a32", "#3a3a45", "#55555f")
LINK = Ramp("#0c0c0f", "#1c1c22", "#2e2e36", "#43434d", "#5c5c68", "#7a7a86")
RUST = Ramp("#1e0e08", "#3a1a0d", "#5a2a14", "#7a3d1c", "#985428", "#b46d3a")
ABYSS = Ramp("#050407", "#0c0910", "#140f1a", "#1d1625", "#281e32", "#352842")
BRICK = Ramp("#07060a", "#100c15", "#1a1421", "#251d2f", "#32283f", "#43364f")
STONE = Ramp("#0e0c12", "#1a1720", "#28232f", "#38313f", "#4a4252", "#5e5566")
STATUE = Ramp("#16131b", "#26212c", "#383140", "#4c4455", "#625a6c", "#7c7386")
RACK = Ramp("#1e0408", "#350911", "#4f111a", "#6b1b24", "#88292f", "#a63d3c")
VIOLET = Ramp("#2a0a3a", "#4a1466", "#7020a0", "#9a3ad0", "#c070f0", "#ecc0ff")
RED = [hexc("#4a0606"), hexc("#7a0a0a"), hexc("#b3121a"), hexc("#ff3b1f"), hexc("#ff7a3a"), hexc("#ffc080")]
EMBER = [hexc("#7a1a04"), hexc("#c42a06"), hexc("#ff5a0a"), hexc("#ff9a24"), hexc("#ffd060"), hexc("#fff0b0")]
CLEAR = (0, 0, 0, 0)

ENOCHIAN = [
    ["x.x", "xxx", "..x"], ["xx.", ".x.", ".xx"], ["x..", "xxx", "x.x"], [".x.", "x.x", "xxx"],
    ["xxx", "x..", "xx."], ["xx.", "x.x", ".xx"], [".xx", "xx.", "..x"], ["x.x", "x.x", ".x."],
]


def at(ramp, v):
    """A continuous sample of a ramp, so shading reads as a gradient."""
    v = max(0.0, min(len(ramp) - 1.0, v))
    i = int(v)
    return ramp[i] if i >= len(ramp) - 1 else mix(ramp[i], ramp[i + 1], v - i)


def glyph(t, ox, oy, idx, color):
    for y, row in enumerate(ENOCHIAN[idx % len(ENOCHIAN)]):
        for x, ch in enumerate(row):
            if ch == "x":
                t.set(ox + x, oy + y, color)


def metal(t, x0, y0, x1, y1, seed, base=2.4, amp=1.0, ramp=IRON, streak=True):
    """Wrought iron: low noise, faint vertical streaks of wear."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            n = fbm(x, y, seed, 16, 16, 3, 5.0)
            v = base + (n - 0.5) * amp + (0.18 * math.sin(x * 2.3 + seed) if streak else 0)
            t.set(x, y, at(ramp, v))


def rust_drip(t, x, y, seed, length=3):
    rng = random.Random(seed)
    for i in range(length):
        if 0 <= y + i < t.h:
            t.set(x, y + i, mix(t.get(x, y + i), RUST[3 - min(2, i)], 0.55 - i * 0.12))
        if rng.random() < 0.3:
            x += rng.choice((-1, 1))


def rivet(t, x, y):
    t.set(x, y, IRON[5])
    t.set(x + 1, y, IRON[4])
    t.set(x, y + 1, IRON[3])
    t.set(x + 1, y + 1, IRON[0])


def stone(t, ramp, seed, base=2.4, contrast=1.0, scale=4.0):
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, scale)
            t.set(x, y, at(ramp, base + (n - 0.5) * 2.4 * contrast))


# --- full cubes ------------------------------------------------------------------------------------------

def cage_frame():
    """The Cage's girders: a raised border of riveted iron round a recessed panel with an Enochian glyph cut in."""
    t = Tex(16, 16, 301)
    metal(t, 0, 0, 16, 16, 301, 2.5)
    for i in range(16):
        t.set(i, 0, IRON[5]), t.set(0, i, IRON[4])
        t.set(i, 15, IRON[0]), t.set(15, i, IRON[0])
        if 0 < i < 15:
            t.set(i, 1, mix(t.get(i, 1), IRON[4], 0.5)), t.set(1, i, mix(t.get(1, i), IRON[4], 0.4))
            t.set(i, 14, mix(t.get(i, 14), IRON[1], 0.6)), t.set(14, i, mix(t.get(14, i), IRON[1], 0.6))
    for y in range(3, 13):  # recessed panel, lit from the top left
        for x in range(3, 13):
            t.set(x, y, shade(t.get(x, y), 0.78))
    for i in range(3, 13):
        t.set(i, 3, IRON[0]), t.set(3, i, IRON[0])
        t.set(i, 12, IRON[3]), t.set(12, i, IRON[3])
    for (x, y) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y)
    glyph(t, 6, 6, 3, RED[1])
    t.set(7, 9, RED[0])
    for (x, y, s) in ((2, 3, 1), (14, 3, 2), (8, 13, 3)):
        rust_drip(t, x, y, s)
    return t


def cage_seal():
    """One of the sixty-six seals, burning in an iron plate (the block is emissive)."""
    t = Tex(16, 16, 311)
    metal(t, 0, 0, 16, 16, 311, 1.7, 0.8)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            if 5.6 <= d <= 6.8:
                t.set(x, y, RED[3] if math.sin(a * 6) > -0.3 else RED[2])
            elif 6.8 < d <= 7.4:
                t.set(x, y, mix(t.get(x, y), RED[0], 0.6))
            elif d < 5.6:
                t.set(x, y, mix(t.get(x, y), RED[0], 0.25 + 0.15 * (5.6 - d) / 5.6))
    # The seal itself: a burning star of strokes, its heart white-hot.
    for (x0, y0, x1, y1) in ((8, 3, 4, 11), (8, 3, 12, 11), (4, 6, 12, 6), (4, 11, 12, 11), (8, 3, 8, 12)):
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            if 0 <= x < 16 and 0 <= y < 16 and math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 5.6:
                t.set(x, y, RED[4] if (x + y) % 2 else RED[3])
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        t.set(x, y, RED[5])
    for (x, y) in ((0, 0), (14, 0), (0, 14), (14, 14)):
        rivet(t, x, y)
    return t


def abyssal_bedrock():
    """The Pit's own rock: strata of violet-black stone, deep fissures and a few cold glints."""
    t = Tex(16, 16, 341)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 341, 16, 16, 3, 4.0)
            strata = 0.5 * math.sin((y + 2.0 * math.sin(x * 0.6)) * 1.1)
            v = 2.2 + (n - 0.5) * 2.6 + strata
            t.set(x, y, at(ABYSS, v))
    rng = random.Random(342)
    for _ in range(4):  # fissures
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(4, 8)):
            t.set(x % 16, y % 16, ABYSS[0])
            t.set((x + 1) % 16, y % 16, mix(t.get((x + 1) % 16, y % 16), ABYSS[5], 0.35))
            x += rng.choice((-1, 0, 1))
            y += 1
    for _ in range(3):
        t.set(rng.randrange(16), rng.randrange(16), VIOLET[1])
    return t


def abyssal_flagstone():
    """The island's floor: worn flagstones of three sizes (they tile), chipped corners, hairline cracks."""
    t = Tex(16, 16, 351)
    # Joints: a horizontal course at y=9; verticals at x=5 above it and x=12 below it; edges at 15.
    def slab_id(x, y):
        if y < 9:
            return 0 if x < 5 else 1
        return 2 if x < 12 else 3
    tones = {0: 0.15, 1: -0.1, 2: 0.25, 3: -0.2}
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 351, 16, 16, 3, 4.0)
            v = 2.5 + (n - 0.5) * 1.6 + tones[slab_id(x, y)]
            t.set(x, y, at(ABYSS, v))
    joints = set()
    for x in range(16):
        joints.add((x, 8)), joints.add((x, 15))
    for y in range(9):
        joints.add((4, y)), joints.add((15, y))
    for y in range(9, 16):
        joints.add((11, y)), joints.add((15, y))
    for (x, y) in joints:
        t.set(x, y, ABYSS[0])
    for (x, y) in joints:  # each slab's lit top-left edge just below/right of a joint
        for dx, dy in ((1, 0), (0, 1)):
            nx, ny = (x + dx) % 16, (y + dy) % 16
            if (nx, ny) not in joints:
                t.set(nx, ny, mix(t.get(nx, ny), ABYSS[5], 0.35))
    for (x, y) in ((5, 0), (3, 7), (12, 9), (0, 10), (10, 14)):  # chipped corners
        t.set(x, y, ABYSS[1])
    rng = random.Random(352)
    x, y = 8, 2
    for _ in range(5):  # a hairline crack across the big slab
        t.set(x, y, ABYSS[1])
        x += 1
        y += rng.choice((0, 1))
    return t


def abyssal_bricks():
    """Dark masonry of the Pit: long bricks in running bond, violet-black, mortar sunk deep."""
    t = Tex(16, 16, 361)
    for y in range(16):
        course = y // 4
        off = 4 * (course % 2)
        for x in range(16):
            bx = (x + off) % 8
            brick = ((x + off) // 8 + course * 3) % 5
            n = fbm(x, y, 361, 16, 16, 3, 4.0)
            v = 2.4 + (n - 0.5) * 1.4 + (brick - 2) * 0.12
            ly = y % 4
            if ly == 0:
                v += 0.7
            elif ly == 2:
                v -= 0.25
            if bx == 0:
                v += 0.45
            c = at(BRICK, v)
            if ly == 3 or bx == 7:
                c = BRICK[0]
            t.set(x, y, c)
    for (x, y) in ((2, 1), (13, 6), (6, 10), (9, 13)):
        t.set(x, y, BRICK[1])
    return t


def cage_ritual_stone():
    """The dais: blood-dark stone cut with a burning ring and a glyph (emissive)."""
    t = Tex(16, 16, 401)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 401, 16, 16, 3, 4.0)
            t.set(x, y, at(RACK, 1.3 + (n - 0.5) * 1.6))
    for i in range(16):
        t.set(i, 0, RACK[3]), t.set(0, i, RACK[3]), t.set(i, 15, RACK[0]), t.set(15, i, RACK[0])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 5.9 <= d <= 6.9:
                t.set(x, y, RED[3] if (x + y) % 3 else RED[4])
            elif 6.9 < d <= 7.4 or 5.4 <= d < 5.9:
                t.set(x, y, mix(t.get(x, y), RED[1], 0.5))
    glyph(t, 6, 6, 5, RED[4])
    t.set(7, 7, RED[5])
    for (x, y) in ((8, 1), (7, 14), (1, 7), (14, 8)):  # the ring's four ticks
        t.set(x, y, RED[4])
    return t


def cage_ritual_stone_side():
    t = Tex(16, 16, 411)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 411, 16, 16, 3, 4.0)
            t.set(x, y, at(RACK, 1.2 + (n - 0.5) * 1.4 - y * 0.04))
    for x in range(16):
        t.set(x, 0, RED[3] if x % 4 else RED[4])
        t.set(x, 1, mix(t.get(x, 1), RED[1], 0.6))
        t.set(x, 15, RACK[0])
    for i, ox in enumerate((2, 7, 12)):
        glyph(t, ox, 7, i * 2 + 1, RED[2])
    return t


# --- bars and posts ------------------------------------------------------------------------------------

def cage_bars():
    """Atlas for the bars' elements.
      x 0-3   the post's face (engraved, a collar at each end)     x 4-5  a thin bar's face
      x 6-15, y 0-1   a rail's side          x 6-15, y 2-4  a collar's side
      x 6-9,  y 6-9   the post's end         x 10-15, y 6-15 plain iron (particles)
    """
    t = Tex(16, 16, 321)
    metal(t, 0, 0, 16, 16, 321, 2.3, 0.8)
    for y in range(16):
        for x, v in ((0, 4.3), (1, 3.2), (2, 2.4), (3, 1.3)):
            n = fbm(x, y, 322, 16, 16, 2, 4.0)
            t.set(x, y, at(IRON, v + (n - 0.5) * 0.6))
        for x, v in ((4, 4.0), (5, 1.6)):
            t.set(x, y, at(IRON, v + (fbm(x, y, 323, 16, 16, 2, 4.0) - 0.5) * 0.6))
    for gy in (3, 7, 11):  # Enochian notches down the post, faintly red-hot in their depths
        for (x, y) in ((1, gy), (2, gy + 1), (1, gy + 2), (2, gy + 2)):
            t.set(x, y, RED[1] if (x + y) % 2 else RED[0])
    for y in (0, 15):
        for x in range(6):
            t.set(x, y, IRON[5] if y == 0 else IRON[0])
    for x in range(6, 16):
        t.set(x, 0, IRON[4] if x % 4 else IRON[5])
        t.set(x, 1, IRON[1])
        t.set(x, 2, IRON[5])
        t.set(x, 3, IRON[3] if x % 3 else RED[1])
        t.set(x, 4, IRON[0])
    for y in range(6, 10):
        for x in range(6, 10):
            d = max(abs(x - 7.5), abs(y - 7.5))
            t.set(x, y, IRON[4] if d > 1.4 else IRON[1])
    return t


def cage_post_tex():
    """The heavy post: x 0-7 its face (bevelled, a lit Enochian strip), x 8-15 y 0-7 its end, x 8-15 y 8-10 a band."""
    t = Tex(16, 16, 331)
    metal(t, 0, 0, 16, 16, 331, 2.4, 0.9)
    for y in range(16):
        for x in range(8):
            v = {0: 4.4, 1: 3.4, 6: 1.6, 7: 0.9}.get(x, 2.5)
            t.set(x, y, at(IRON, v + (fbm(x, y, 332, 16, 16, 2, 4.0) - 0.5) * 0.7))
    for y in range(2, 14):  # a sunken channel with glyphs glowing faintly in it
        t.set(3, y, IRON[0]), t.set(4, y, IRON[0])
    for i, gy in enumerate((3, 8)):
        glyph(t, 2, gy, i * 3 + 2, RED[2])
    for y in (0, 1, 14, 15):
        for x in range(8):
            t.set(x, y, IRON[5] if y in (0, 14) else IRON[1])
    for y in range(8):
        for x in range(8, 16):
            d = max(abs(x - 11.5), abs(y - 3.5))
            t.set(x, y, IRON[4] if d > 3 else (IRON[2] if d > 1.5 else RED[1]))
    for x in range(8, 16):
        t.set(x, 8, IRON[5]), t.set(x, 9, IRON[3] if x % 2 else RED[1]), t.set(x, 10, IRON[0])
    return t


def bars_post_elements():
    tex = {"*": "bars"}
    return [
        cube("post", (6, 0, 6), (10, 16, 10), tex, uv={f: [0, 0, 4, 16] for f in ("north", "south", "east", "west")} |
             {"up": [6, 6, 10, 10], "down": [6, 6, 10, 10]}),
        cube("collar_low", (5.5, 0, 5.5), (10.5, 1.5, 10.5), tex, uv=_band_uv()),
        cube("collar_high", (5.5, 14.5, 5.5), (10.5, 16, 10.5), tex, uv=_band_uv()),
    ]


def _band_uv():
    return {f: [6, 2, 11, 4] for f in ("north", "south", "east", "west")} | {"up": [6, 6, 10, 10], "down": [6, 6, 10, 10]}


def bars_side_elements(mirror=False):
    """One joined side, reaching north (z 0..6) -- mirror=True gives the south one (for the inventory model)."""
    def z(a, b):
        return (16 - b, 16 - a) if mirror else (a, b)
    tex = {"*": "bars"}
    side = {f: [6, 0, 12, 2] for f in ("east", "west")} | {"up": [4, 0, 6, 6], "down": [4, 0, 6, 6], "north": [4, 0, 6, 2], "south": [4, 0, 6, 2]}
    bar = {f: [4, 0, 6, 16] for f in ("north", "south", "east", "west")} | {"up": [6, 6, 8, 8], "down": [6, 6, 8, 8]}
    z0, z1 = z(0, 6)
    b0, b1 = z(2, 4)
    return [
        cube("rail_low", (7, 1.5, z0), (9, 3.5, z1), tex, uv=side),
        cube("rail_high", (7, 12.5, z0), (9, 14.5, z1), tex, uv=side),
        cube("bar", (7, 0, b0), (9, 16, b1), tex, uv=bar),
    ]


def post_elements():
    tex = {"*": "post"}
    face = {f: [0, 0, 8, 16] for f in ("north", "south", "east", "west")} | {"up": [8, 0, 16, 8], "down": [8, 0, 16, 8]}
    band = {f: [8, 8, 16, 11] for f in ("north", "south", "east", "west")} | {"up": [8, 0, 16, 8], "down": [8, 0, 16, 8]}
    return [
        cube("post", (4, 0, 4), (12, 16, 12), tex, uv=face),
        cube("band_low", (3, 0, 3), (13, 3, 13), tex, uv=band),
        cube("band_high", (3, 13, 3), (13, 16, 13), tex, uv=band),
    ]


# --- chain ---------------------------------------------------------------------------------------------

def cage_chain():
    """Iron for the heavy links: a lit middle, darker toward the edges, a few rust flecks."""
    t = Tex(16, 16, 341)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 343, 16, 16, 2, 4.0)
            v = 4.6 - abs(x - 7.5) * 0.3 + (n - 0.5) * 0.8 + (0.5 if y % 8 in (0, 1) else 0) - (0.6 if y % 8 == 7 else 0)
            t.set(x, y, at(LINK, v))
    rng = random.Random(344)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, mix(t.get(x, y), RUST[3], 0.6))
    return t


def chain_link(y0, plane, name, x=8, z=8, tex="chain", height=10, width=6, bar=2):
    """One link, `height` tall from y0, in the xy plane ("x") or the zy plane ("z")."""
    h, w, b = height, width, bar
    els = []
    if plane == "x":
        x0 = x - w / 2
        zz = (z - b / 2, z + b / 2)
        parts = (((x0, y0 + 1, zz[0]), (x0 + b, y0 + h - 1, zz[1])), ((x0 + w - b, y0 + 1, zz[0]), (x0 + w, y0 + h - 1, zz[1])),
                 ((x0 + 1, y0 + h - b, zz[0]), (x0 + w - 1, y0 + h, zz[1])), ((x0 + 1, y0, zz[0]), (x0 + w - 1, y0 + b, zz[1])))
    else:
        z0 = z - w / 2
        xx = (x - b / 2, x + b / 2)
        parts = (((xx[0], y0 + 1, z0), (xx[1], y0 + h - 1, z0 + b)), ((xx[0], y0 + 1, z0 + w - b), (xx[1], y0 + h - 1, z0 + w)),
                 ((xx[0], y0 + h - b, z0 + 1), (xx[1], y0 + h, z0 + w - 1)), ((xx[0], y0, z0 + 1), (xx[1], y0 + b, z0 + w - 1)))
    for i, (a, c) in enumerate(parts):
        els.append(cube(f"{name}_{i}", a, c, {"*": tex}))
    return els


def chain_elements():
    # Two links per block: one in each plane, interlocking, the second reaching 2 px into the block above (so it tiles).
    return chain_link(0, "x", "a", width=7, bar=1.5) + chain_link(8, "z", "b", width=7, bar=1.5)


# --- pillar and capital ------------------------------------------------------------------------------------

def enochian_pillar():
    """The shaft's side: x 2-13 the face between the ribs (fluting, a glyph column lit red), x 0-1/14-15 the ribs."""
    t = Tex(16, 16, 361)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 362, 16, 16, 3, 5.0)
            if x < 2 or x > 13:
                v = (4.2 if x in (0, 14) else 3.0) + (n - 0.5) * 0.8
            else:
                flute = {3: -0.9, 4: 0.5, 11: -0.9, 12: 0.5}.get(x, 0.0)
                v = 2.6 + (n - 0.5) * 1.2 + flute
            t.set(x, y, at(STONE, v))
    for y in range(16):  # the glyph column, cut deep
        for x in range(6, 10):
            t.set(x, y, mix(t.get(x, y), STONE[0], 0.55))
    for i, oy in enumerate((1, 6, 11)):
        glyph(t, 6, oy, i * 3 + 1, RED[2])
        t.set(9, oy + 1, RED[1])
    for x in range(16):
        t.set(x, 0, mix(t.get(x, 0), STONE[5], 0.4))
        t.set(x, 15, mix(t.get(x, 15), STONE[0], 0.5))
    return t


def enochian_pillar_top():
    t = Tex(16, 16, 371)
    for y in range(16):
        for x in range(16):
            d = max(abs(x + 0.5 - 8), abs(y + 0.5 - 8))
            v = 3.0 - d * 0.12 + (fbm(x, y, 372, 16, 16, 2, 4.0) - 0.5) * 0.9
            if abs(d - 4.5) < 0.6:
                v = 1.2
            t.set(x, y, at(STONE, v))
    for (x, y) in ((0, 0), (14, 0), (0, 14), (14, 14)):
        for dx in (0, 1):
            for dy in (0, 1):
                t.set(x + dx, y + dy, STONE[4])
    glyph(t, 7, 7, 3, RED[2])
    return t


def pillar_elements():
    side = {"*": "side"}
    els = [cube("core", (2, 0, 2), (14, 16, 14), {"*": "side", "up": "end", "down": "end"},
                uv={f: [2, 0, 14, 16] for f in ("north", "south", "east", "west")} | {"up": [2, 2, 14, 14], "down": [2, 2, 14, 14]})]
    for i, (x, z) in enumerate(((1, 1), (13, 1), (1, 13), (13, 13))):
        els.append(cube(f"rib{i}", (x, 0, z), (x + 2, 16, z + 2), {"*": "side", "up": "end", "down": "end"},
                        uv={f: [0, 0, 2, 16] for f in ("north", "south", "east", "west")} | {"up": [0, 0, 2, 2], "down": [0, 0, 2, 2]}))
    return els


def enochian_capital():
    """The flared end, by rows: 0-2 the abacus (a lit glyph band), 3-5 the ovolo, 6-7 the necking, 8-15 the shaft."""
    t = Tex(16, 16, 381)
    shaft = enochian_pillar()
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 382, 16, 16, 3, 5.0)
            if y <= 2:
                v = (4.4 if y == 0 else 2.6) + (n - 0.5) * 0.8
            elif y <= 5:
                v = 3.6 - (y - 3) * 0.8 + (n - 0.5) * 0.6  # the ovolo rounds away from the light
            elif y <= 7:
                v = (1.0 if y == 6 else 3.2) + (n - 0.5) * 0.5
            else:
                t.set(x, y, shaft.get(x, y))
                continue
            t.set(x, y, at(STONE, v))
    for i, ox in enumerate((1, 6, 11)):  # glyphs round the abacus
        for gy, row in enumerate(ENOCHIAN[(i * 3 + 2) % len(ENOCHIAN)][:2]):
            for gx, ch in enumerate(row):
                if ch == "x":
                    t.set(ox + gx + 1, gy + 1, RED[3] if gy == 0 else RED[2])
    for x in range(16):  # egg-and-dart on the ovolo
        if x % 3 == 1:
            t.set(x, 4, STONE[1])
    return t


def enochian_capital_top():
    t = Tex(16, 16, 391)
    for y in range(16):
        for x in range(16):
            d = max(abs(x + 0.5 - 8), abs(y + 0.5 - 8))
            v = 3.0 + (fbm(x, y, 392, 16, 16, 2, 4.0) - 0.5) * 0.9 + (0.8 if d > 6.9 else 0) - (0.9 if abs(d - 5.5) < 0.6 else 0)
            t.set(x, y, at(STONE, v))
    for (x, y) in ((7, 3), (8, 3), (3, 7), (3, 8), (12, 7), (12, 8), (7, 12), (8, 12)):
        t.set(x, y, RED[2])
    return t


def capital_elements():
    """Drawn as a capital (flare on top); the blockstate turns it over for a base."""
    sides = ("north", "south", "east", "west")
    tex = {"*": "side", "up": "top", "down": "top"}
    els = [cube("shaft", (2, 0, 2), (14, 8, 14), {"*": "side", "up": "top", "down": "top"},
                uv={f: [2, 8, 14, 16] for f in sides} | {"up": [2, 2, 14, 14], "down": [2, 2, 14, 14]})]
    for i, (x, z) in enumerate(((1, 1), (13, 1), (1, 13), (13, 13))):
        els.append(cube(f"rib{i}", (x, 0, z), (x + 2, 8, z + 2), tex,
                        uv={f: [0, 8, 2, 16] for f in sides} | {"up": [0, 0, 2, 2], "down": [0, 0, 2, 2]}))
    els.append(cube("necking", (1.5, 8, 1.5), (14.5, 10, 14.5), tex, uv={f: [1, 6, 15, 8] for f in sides} | {"up": [1, 1, 15, 15], "down": [1, 1, 15, 15]}))
    els.append(cube("ovolo", (0.75, 10, 0.75), (15.25, 13, 15.25), tex, uv={f: [1, 3, 15, 6] for f in sides} | {"up": [1, 1, 15, 15], "down": [1, 1, 15, 15]}))
    els.append(cube("abacus", (0, 13, 0), (16, 16, 16), tex, uv={f: [0, 0, 16, 3] for f in sides} | {"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}))
    return els


# --- brazier ---------------------------------------------------------------------------------------------

FLAME_FRAMES = 12


def brazier_side():
    """x 0-11 y 0-4 the bowl's wall (a lit glyph band, ember lip), y 5-7 its lower curve, y 8-9 the rim."""
    t = Tex(16, 16, 381)
    metal(t, 0, 0, 16, 16, 383, 2.3, 0.9)
    for x in range(16):
        t.set(x, 0, EMBER[2] if x % 3 else EMBER[3])
        t.set(x, 1, IRON[4])
        t.set(x, 4, IRON[1])
        t.set(x, 8, IRON[5])
        t.set(x, 9, IRON[2])
    for i, ox in enumerate((1, 5, 9, 13)):
        for gy, row in enumerate(ENOCHIAN[(i * 2 + 1) % len(ENOCHIAN)][:2]):
            for gx, ch in enumerate(row):
                if ch == "x" and ox + gx < 16:
                    t.set(ox + gx, 2 + gy, RED[2])
    for y in (5, 6, 7):
        for x in range(16):
            t.set(x, y, at(IRON, 2.6 - (y - 5) * 0.7 + (fbm(x, y, 384, 16, 16, 2, 4.0) - 0.5) * 0.6))
    return t


def brazier_top():
    """The coal bed: black lumps split by white-hot seams."""
    t = Tex(16, 16, 391)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 391, 16, 16, 3, 5.0)
            cell = fbm(x * 2, y * 2, 393, 32, 32, 2, 6.0)
            if abs(cell - 0.5) < 0.06:
                c = EMBER[min(5, 3 + int(n * 3))]
            elif n > 0.62:
                c = EMBER[2]
            else:
                c = at(IRON, 0.6 + n * 2.0)
            t.set(x, y, c)
    return t


def brazier_flame():
    """Hellfire, a strip of frames: tongues of flame rising, red at the root, gold at the heart."""
    t = Tex(16, 16 * FLAME_FRAMES, 395)
    for f in range(FLAME_FRAMES):
        ph = f / FLAME_FRAMES
        for y in range(16):
            for x in range(16):
                h = 1 - y / 15.0  # 0 at the bottom .. 1 at the top
                sway = math.sin((h * 3.0 - ph) * math.tau) * 1.6 * h
                cx = x + 0.5 - 8 - sway
                n = fbm(x, (y + f * 3) % 16, 396, 16, 16, 2, 4.0)
                width = (6.8 - h * 6.0) * (0.75 + n * 0.5) + 1.2 * math.sin((x * 0.9 + ph * 6.28) + h * 4)
                d = abs(cx) / max(0.4, width)
                if d >= 1.0 or h > 0.95:
                    continue
                heat = (1 - d) * (1.1 - h)
                if heat > 0.75:
                    c = EMBER[5]
                elif heat > 0.5:
                    c = EMBER[4]
                elif heat > 0.32:
                    c = EMBER[3]
                elif heat > 0.18:
                    c = EMBER[2]
                else:
                    c = RED[2]
                t.set(x, y + f * 16, c)
    return t


def brazier_elements():
    iron = {"*": "iron"}
    sides = ("north", "south", "east", "west")
    bowl = {"*": "side", "up": "iron", "down": "iron"}
    els = []
    # Four clawed legs and a ring that braces them.
    for i, (x, z) in enumerate(((2.5, 2.5), (11.5, 2.5), (2.5, 11.5), (11.5, 11.5))):
        els.append(cube(f"leg{i}", (x, 1, z), (x + 2, 9, z + 2), iron, uv={f: [4, 0, 6, 8] for f in sides} | {"up": [6, 6, 8, 8], "down": [6, 6, 8, 8]}))
        fx, fz = (x - 1 if x < 8 else x), (z - 1 if z < 8 else z)
        els.append(cube(f"foot{i}", (fx, 0, fz), (fx + 3, 1, fz + 3), iron, uv={f: [6, 2, 9, 3] for f in sides} | {"up": [6, 6, 9, 9], "down": [6, 6, 9, 9]}))
    els.append(cube("brace_n", (3, 4, 3), (13, 5, 4), iron, uv={f: [6, 2, 16, 3] for f in sides} | {"up": [6, 0, 16, 1], "down": [6, 0, 16, 1]}))
    els.append(cube("brace_s", (3, 4, 12), (13, 5, 13), iron, uv={f: [6, 2, 16, 3] for f in sides} | {"up": [6, 0, 16, 1], "down": [6, 0, 16, 1]}))
    # The bowl: a lower curve, four walls and the coals; an ember lip on its rim.
    els.append(cube("bowl_low", (3, 8, 3), (13, 10, 13), bowl, uv={f: [2, 5, 12, 7] for f in sides} | {"up": [3, 3, 13, 13], "down": [3, 3, 13, 13]}))
    for name, frm, to in (("wall_n", (1.5, 10, 1.5), (14.5, 14, 2.5)), ("wall_s", (1.5, 10, 13.5), (14.5, 14, 14.5)),
                          ("wall_w", (1.5, 10, 2.5), (2.5, 14, 13.5)), ("wall_e", (13.5, 10, 2.5), (14.5, 14, 13.5))):
        w = (to[0] - frm[0]) if to[0] - frm[0] > 1.5 else (to[2] - frm[2])
        els.append(cube(name, frm, to, bowl, uv={f: [0, 0, min(16, w), 4] for f in sides} | {"up": [0, 8, 13, 9], "down": [0, 8, 13, 9]}))
    els.append(cube("coals", (2.5, 10, 2.5), (13.5, 12.5, 13.5), {"*": "iron", "up": "top"}, uv={"up": [2.5, 2.5, 13.5, 13.5]} | {f: [6, 6, 10, 8] for f in sides}))
    # A cage of bars over the fire, its top ring three pixels above the block.
    for i, (x, z) in enumerate(((2, 2), (13, 2), (2, 13), (13, 13), (7.5, 2), (7.5, 13), (2, 7.5), (13, 7.5))):
        els.append(cube(f"cage{i}", (x, 14, z), (x + 1, 19, z + 1), iron, uv={f: [4, 0, 5, 5] for f in sides} | {"up": [4, 0, 5, 1], "down": [4, 0, 5, 1]}))
    for name, frm, to in (("ring_n", (2, 18, 2), (14, 19, 3)), ("ring_s", (2, 18, 13), (14, 19, 14)),
                          ("ring_w", (2, 18, 3), (3, 19, 13)), ("ring_e", (13, 18, 3), (14, 19, 13))):
        els.append(cube(name, frm, to, iron, uv={f: [6, 0, 16, 1] for f in sides} | {"up": [6, 0, 16, 1], "down": [6, 1, 16, 2]}))
    # Hellfire: two crossed planes, unshaded.
    for i, ang in enumerate((45, -45)):
        e = cube(f"flame{i}", (1, 11, 8), (15, 23, 8), {"north": "fire", "south": "fire"},
                 uv={"north": [1, 4, 15, 16], "south": [1, 4, 15, 16]}, rot=("y", ang, (8, 8, 8)), shade=False)
        e["rotation"]["rescale"] = True
        els.append(e)
    return els


# --- lantern -------------------------------------------------------------------------------------------

def seal_lantern_tex():
    """x 0-5 y 0-7 the body's side (iron frame round a burning seal pane), x 0-5 y 8-13 its top and bottom,
    x 6-9 y 0-1 the cap's side, x 6-9 y 2-5 its top, x 11-13 y 0-7 the chain."""
    t = Tex(16, 16, 401)
    metal(t, 0, 0, 16, 16, 402, 2.2, 0.8)
    for y in range(8):
        for x in range(6):
            frame = x in (0, 5) or y in (0, 7)
            if frame:
                t.set(x, y, IRON[4] if (x == 0 or y == 0) else IRON[1])
            else:
                d = math.hypot(x - 2.5, y - 3.5)
                t.set(x, y, RED[5] if d < 0.8 else RED[4] if d < 1.6 else RED[3])
    for (x, y) in ((2, 2), (3, 3), (2, 4), (3, 5)):  # the seal's strokes on the pane
        t.set(x, y, RED[2])
    for y in range(8, 14):
        for x in range(6):
            t.set(x, y, IRON[4] if (x in (0, 5) or y in (8, 13)) else (IRON[2] if (x + y) % 2 else RED[1]))
    for x in range(6, 10):
        t.set(x, 0, IRON[5]), t.set(x, 1, IRON[2])
        for y in range(2, 6):
            t.set(x, y, IRON[4] if y in (2, 5) or x in (6, 9) else IRON[1])
    for y in range(16):
        for x in range(11, 14):
            t.set(x, y, CLEAR)
    for y in range(8):
        if y % 4 in (0, 3):
            t.set(11, y, IRON[4]), t.set(12, y, IRON[3]), t.set(13, y, IRON[1])
        else:
            t.set(11, y, IRON[4]), t.set(13, y, IRON[1])
    return t


def lantern_elements(hanging):
    o = 1 if hanging else 0
    sides = ("north", "south", "east", "west")
    tex = {"*": "lantern"}
    els = [
        cube("body", (5, o, 5), (11, o + 8, 11), tex, uv={f: [0, 0, 6, 8] for f in sides} | {"up": [0, 8, 6, 14], "down": [0, 8, 6, 14]}),
        cube("cap", (6, o + 8, 6), (10, o + 10, 10), tex, uv={f: [6, 0, 10, 2] for f in sides} | {"up": [6, 2, 10, 6], "down": [6, 2, 10, 6]}),
    ]
    top = 16 if hanging else o + 13
    for i, ang in enumerate((45, -45)):
        e = cube(f"chain{i}", (6.5, o + 10, 8), (9.5, top, 8), {"north": "lantern", "south": "lantern"},
                 uv={"north": [11, 0, 14, top - o - 10], "south": [11, 0, 14, top - o - 10]}, rot=("y", ang, (8, 8, 8)))
        els.append(e)
    return els


# --- anchor --------------------------------------------------------------------------------------------

def chain_anchor_tex():
    """x 3-12 y 1-12 the wall plate's face (riveted, a glyph lit faintly), the rest plain iron."""
    t = Tex(16, 16, 411)
    metal(t, 0, 0, 16, 16, 412, 2.3, 0.9)
    for y in range(1, 13):
        for x in range(3, 13):
            edge_hi = x == 3 or y == 1
            edge_lo = x == 12 or y == 12
            if edge_hi:
                t.set(x, y, IRON[5])
            elif edge_lo:
                t.set(x, y, IRON[0])
    for (x, y) in ((4, 2), (10, 2), (4, 10), (10, 10)):
        rivet(t, x, y)
    glyph(t, 6, 3, 6, RED[2])
    rust_drip(t, 5, 4, 7)
    rust_drip(t, 11, 4, 8, 4)
    return t


def anchor_elements():
    sides = ("north", "south", "east", "west")
    iron = {"*": "iron"}
    els = [
        cube("plate", (3, 3, 14), (13, 15, 16), {"*": "iron", "north": "plate"}, uv={"north": [3, 1, 13, 13]}),
        cube("boss", (6, 10, 12), (10, 14, 14), iron, uv={f: [6, 2, 10, 4] for f in sides} | {"up": [6, 6, 10, 10], "down": [6, 6, 10, 10]}),
    ]
    # The ring, hanging from the boss in the plane of the wall.
    ring = (("ring_l", (5, 5, 12.5), (7, 11, 13.5)), ("ring_r", (9, 5, 12.5), (11, 11, 13.5)),
            ("ring_b", (6, 4, 12.5), (10, 6, 13.5)), ("ring_t", (6, 10, 12.5), (10, 12, 13.5)))
    for name, a, b in ring:
        els.append(cube(name, a, b, {"*": "chain"}))
    # One link through the ring, at right angles to it.
    els += chain_link(0, "z", "link", x=8, z=12.5, height=6, width=4, bar=1.5)
    return els


# --- the fallen watcher ------------------------------------------------------------------------------------

def watcher_tex():
    """Carved abyssal stone: chisel marks, feathers cut into the wings' region (x 0-7), the bowed face at x 12-15 y 12-15."""
    t = Tex(16, 16, 421)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 422, 16, 16, 3, 5.0)
            v = 2.7 + (n - 0.5) * 1.3 + (0.25 if (x * 3 + y * 5) % 7 == 0 else 0)
            t.set(x, y, at(STATUE, v))
    for y in range(16):  # feathers: rows of scallops on the left half
        for x in range(8):
            ly, lx = y % 4, (x + (y // 4) % 2 * 2) % 4
            if ly == 3 or (ly == 2 and lx in (0, 3)):
                t.set(x, y, STATUE[1])
            elif ly == 0 and lx in (1, 2):
                t.set(x, y, STATUE[4])
    face = ["....", ".rr.", "....", ".--."]  # eyes closed but burning through, the mouth a line
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            c = t.get(12 + x, 12 + y)
            if ch == "r":
                c = RED[1]
            elif ch == "-":
                c = STATUE[1]
            t.set(12 + x, 12 + y, c)
    for x in range(12, 16):
        t.set(x, 12, STATUE[4])  # the brow catches the light
    return t


def watcher_elements():
    sides = ("north", "south", "east", "west")
    stone = {"*": "stone"}
    plinth = {"*": "plinth", "up": "plinth_top", "down": "plinth_top"}
    els = [
        cube("plinth", (1, 0, 1), (15, 3, 15), plinth),
        cube("plinth_lip", (0.5, 2.5, 0.5), (15.5, 3.5, 15.5), plinth, uv={f: [0, 0, 15, 1] for f in sides}),
        cube("shins", (4, 3.5, 8), (12, 5.5, 14), stone, uv={f: [8, 0, 16, 2] for f in sides} | {"up": [8, 2, 16, 8], "down": [8, 2, 16, 8]}),
        cube("thighs", (4.5, 3.5, 5), (11.5, 7, 10), stone, uv={f: [8, 4, 15, 8] for f in sides} | {"up": [8, 4, 15, 9]}),
        cube("torso", (5, 7, 6), (11, 12.5, 10), stone, uv={f: [9, 6, 15, 12] for f in sides} | {"up": [9, 6, 15, 10]}),
        cube("arm_l", (11, 7.5, 6.5), (12.5, 12, 9.5), stone, uv={f: [9, 0, 11, 5] for f in sides}),
        cube("arm_r", (3.5, 7.5, 6.5), (5, 12, 9.5), stone, uv={f: [9, 0, 11, 5] for f in sides}),
        cube("forearms", (5, 7.5, 4.5), (11, 9, 6.5), stone, uv={f: [9, 10, 15, 12] for f in sides} | {"up": [9, 10, 15, 12]}),
        cube("shackle", (7, 7, 4), (9, 9.5, 7), {"*": "iron"}, uv={f: [6, 2, 8, 4] for f in sides} | {"up": [6, 6, 8, 8], "down": [6, 6, 8, 8]}),
        cube("tether", (7.5, 3, 4.5), (8.5, 7, 5.5), {"*": "chain"}),
        cube("head", (6, 12, 5.5), (10, 16, 9.5), stone, uv={"north": [12, 12, 16, 16]} | {f: [12, 8, 16, 12] for f in ("south", "east", "west")}
             | {"up": [12, 8, 16, 12], "down": [12, 8, 16, 12]}, rot=("x", -22.5, (8, 12, 8))),
    ]
    # Folded wings behind him, rising past his bowed head.
    for name, frm, to, ang, piv in (("wing_l", (9.5, 4, 10), (13.5, 16, 12), -22.5, (10, 10, 11)),
                                    ("wing_r", (2.5, 4, 10), (6.5, 16, 12), 22.5, (6, 10, 11))):
        els.append(cube(name, frm, to, stone, uv={f: [0, 0, 4, 12] for f in ("north", "south")} | {f: [0, 0, 2, 12] for f in ("east", "west")}
                        | {"up": [0, 0, 4, 2], "down": [0, 0, 4, 2]}, rot=("y", ang, piv)))
    return els


# --- item sprites ---------------------------------------------------------------------------------------------

def outline(t, color=hexc("#050406")):
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


def item_chain():
    t = Tex(16, 16, 431)
    for i in range(4):  # four links down a diagonal, alternating face-on and edge-on
        cx, cy = 4 + i * 2.6, 2 + i * 3.6
        if i % 2 == 0:
            for y in range(int(cy) - 2, int(cy) + 3):
                for x in range(int(cx) - 2, int(cx) + 2):
                    ring = x in (int(cx) - 2, int(cx) + 1) or y in (int(cy) - 2, int(cy) + 2)
                    if ring and 0 <= x < 16 and 0 <= y < 16:
                        t.set(x, y, IRON[5] if (x == int(cx) - 2 or y == int(cy) - 2) else IRON[3])
        else:
            for y in range(int(cy) - 2, int(cy) + 3):
                t.set(int(cx), y, IRON[4])
                t.set(int(cx) - 1, y, IRON[2])
    outline(t)
    return t


def item_lantern():
    t = Tex(16, 16, 441)
    for y in range(5, 15):
        for x in range(4, 12):
            frame = x in (4, 11) or y in (5, 14)
            if frame:
                t.set(x, y, IRON[4] if (x == 4 or y == 5) else IRON[1])
            else:
                d = math.hypot(x - 7.5, y - 9.5)
                t.set(x, y, RED[5] if d < 1.2 else RED[4] if d < 2.4 else RED[3])
    for x in range(5, 11):
        t.set(x, 4, IRON[3])
    for x in range(6, 10):
        t.set(x, 3, IRON[4])
    for y in range(0, 3):
        t.set(7, y, IRON[4] if y % 2 == 0 else IRON[2])
        if y % 2:
            t.set(6, y, IRON[3]), t.set(8, y, IRON[3])
    for (x, y) in ((7, 8), (8, 9), (7, 10), (8, 11)):
        t.set(x, y, RED[2])
    outline(t)
    return t


# --- writing -------------------------------------------------------------------------------------------------

BLOCK_TEX = {
    "cage_frame": cage_frame, "cage_seal": cage_seal, "cage_bars": cage_bars, "cage_post": cage_post_tex, "cage_chain": cage_chain,
    "abyssal_bedrock": abyssal_bedrock, "abyssal_flagstone": abyssal_flagstone, "abyssal_bricks": abyssal_bricks,
    "enochian_pillar": enochian_pillar, "enochian_pillar_top": enochian_pillar_top, "enochian_capital": enochian_capital,
    "enochian_capital_top": enochian_capital_top, "hellfire_brazier_side": brazier_side, "hellfire_brazier_top": brazier_top,
    "cage_ritual_stone": cage_ritual_stone, "cage_ritual_stone_side": cage_ritual_stone_side, "seal_lantern": seal_lantern_tex,
    "chain_anchor": chain_anchor_tex, "fallen_watcher": watcher_tex,
}
ITEM_TEX = {"cage_chain": item_chain, "seal_lantern": item_lantern}


def models():
    """Every block model, by name."""
    bars = {"bars": "cage_bars"}
    post = {"post": "cage_post"}
    out = {
        "cage_bars_post": model(bars, bars_post_elements(), "cage_bars"),
        "cage_bars_side": model(bars, bars_side_elements(), "cage_bars"),
        "cage_bars_inventory": model(bars, bars_post_elements() + bars_side_elements() + bars_side_elements(True), "cage_bars"),
        "cage_post": model(post | bars, post_elements(), "cage_post"),
        "cage_post_inventory": model(post | bars, post_elements() + bars_side_elements() + bars_side_elements(True), "cage_post"),
        "cage_chain": model({"chain": "cage_chain"}, chain_elements(), "cage_chain"),
        "enochian_pillar": model({"side": "enochian_pillar", "end": "enochian_pillar_top"}, pillar_elements(), "enochian_pillar", cutout=False),
        "enochian_capital": model({"side": "enochian_capital", "top": "enochian_capital_top"}, capital_elements(), "enochian_capital", cutout=False),
        "hellfire_brazier": model({"side": "hellfire_brazier_side", "top": "hellfire_brazier_top", "iron": "cage_bars",
                                   "fire": "hellfire_brazier_flame"}, brazier_elements(), "hellfire_brazier_side"),
        "seal_lantern": model({"lantern": "seal_lantern"}, lantern_elements(False), "seal_lantern"),
        "seal_lantern_hanging": model({"lantern": "seal_lantern"}, lantern_elements(True), "seal_lantern"),
        "chain_anchor": model({"plate": "chain_anchor", "iron": "cage_bars", "chain": "cage_chain"}, anchor_elements(), "chain_anchor"),
        "fallen_watcher": model({"stone": "fallen_watcher", "plinth": "abyssal_bricks", "plinth_top": "abyssal_flagstone",
                                 "iron": "cage_bars", "chain": "cage_chain"}, watcher_elements(), "fallen_watcher", cutout=False),
    }
    return out


def generate():
    for name, fn in BLOCK_TEX.items():
        save(fn(), "block", name)
    for name, fn in ITEM_TEX.items():
        save(fn(), "item", name)
    save(brazier_flame(), "block", "hellfire_brazier_flame")
    with open(os.path.join(TEXTURES, "block", "hellfire_brazier_flame.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh, indent=1)
        fh.write("\n")
    for name, m in models().items():
        write(name, m)
