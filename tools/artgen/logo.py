"""Project logo (Modrinth/CurseForge icon): the Colt over a devil's trap.

Not in generate.py's MODULES. Drawn as 64x64 pixel art and also written nearest-scaled to
512x512 (what the mod pages show), plus the in-jar copy for the mod list. Run: python logo.py
"""

import math
import os

import colt_art
import preview3d
from common import ASSETS, ROOT
from pixelkit import Tex, fbm, hexc, mix
from pngio import write_png

SIZE = 64
SCALE = 8
OUT = os.path.join(ROOT, "branding")
# logoFile in neoforge.mods.toml: the mod list in game.
IN_JAR = os.path.join(os.path.dirname(os.path.dirname(ASSETS)), "supernaturalcraft_logo.png")

VOID = hexc("#050206")
EMBER = hexc("#3a0808")
BLOOD = hexc("#b3141a")
BLOOD_DARK = hexc("#5e0a0e")
BLOOD_HOT = hexc("#ff4a2e")
SPARK = hexc("#ffb43a")

C = SIZE / 2
R_OUT, R_IN = 29.5, 24.5

# 3x3 marks stamped between the two rings, one per "spoke" of the trap.
GLYPHS = [
    ["x.x", ".x.", "x.x"],
    ["xxx", ".x.", ".x."],
    ["x..", "xxx", "..x"],
    [".x.", "xxx", ".x."],
    ["x.x", "xxx", "x.x"],
    ["xx.", ".x.", ".xx"],
    ["x.x", "x.x", "xxx"],
    [".xx", "x..", ".xx"],
    ["xxx", "x.x", "x.."],
    ["x..", "x..", "xxx"],
]


def background(t):
    """Scorched dark disc lit from the centre, so the round icon crop still reads."""
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x + 0.5 - C, y + 0.5 - C) / (SIZE * 0.72)
            n = fbm(x, y, 7, SIZE, SIZE, octaves=3, scale=6.0)
            k = max(0.0, 1.0 - d) ** 1.6 * (0.75 + 0.5 * n)
            t.set(x, y, mix(VOID, EMBER, min(1.0, k)))


def halo(t, mask, color, radius, strength):
    """Soft glow around every pixel of a mask (a set of (x, y)), painted under the lines."""
    acc = [[0.0] * SIZE for _ in range(SIZE)]
    r = int(math.ceil(radius))
    for (mx, my) in mask:
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                d = math.hypot(dx, dy)
                if d <= radius:
                    x, y = mx + dx, my + dy
                    if 0 <= x < SIZE and 0 <= y < SIZE:
                        acc[y][x] = max(acc[y][x], 1.0 - d / (radius + 0.5))
    for y in range(SIZE):
        for x in range(SIZE):
            if acc[y][x] > 0:
                t.blend(x, y, color, acc[y][x] ** 2 * strength)


def trap_mask():
    """Pixels of the trap: two rings, a pentagram on the inner one, glyphs in the band."""
    lines, glyphs = set(), set()
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x + 0.5 - C, y + 0.5 - C)
            if R_OUT - 1.4 <= d <= R_OUT or R_IN - 1.0 <= d <= R_IN:
                lines.add((x, y))
    pts = [(C - 0.5 + (R_IN - 1.2) * math.sin(math.radians(a)),
            C - 0.5 - (R_IN - 1.2) * math.cos(math.radians(a))) for a in range(0, 360, 72)]
    for i in range(5):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 2) % 5]
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for s in range(steps + 1):
            f = s / steps
            lines.add((round(x0 + (x1 - x0) * f), round(y0 + (y1 - y0) * f)))
    mid = (R_OUT - 0.7 + R_IN) / 2 - 0.5
    for i, g in enumerate(GLYPHS):
        a = math.radians(18 + i * 36)
        gx = round(C - 0.5 + mid * math.sin(a)) - 1
        gy = round(C - 0.5 - mid * math.cos(a)) - 1
        for yy, row in enumerate(g):
            for xx, ch in enumerate(row):
                if ch == "x":
                    glyphs.add((gx + xx, gy + yy))
    return lines, glyphs


def trap(t):
    lines, glyphs = trap_mask()
    halo(t, lines | glyphs, BLOOD_HOT, 2.5, 0.38)
    for (x, y) in lines | glyphs:
        # Brighter towards the top-left, like fresh paint catching the fire light.
        k = 0.5 + 0.5 * fbm(x, y, 3, SIZE, SIZE, octaves=2, scale=5.0)
        t.set(x, y, mix(BLOOD_DARK, BLOOD, min(1.0, 0.45 + k * 0.7)))
    for (x, y) in glyphs:
        t.set(x, y, mix(BLOOD, BLOOD_HOT, 0.35))


def colt():
    rig, tex, _, _ = colt_art.build()
    hidden = ("muzzle_flash", "loose_round", "hand_l", "hand_r")
    return preview3d.icon(rig, tex, size=46, angle_deg=-22, view="side", hidden=hidden)


def shadow(t, sprite, ox, oy):
    for y in range(sprite.h):
        for x in range(sprite.w):
            if sprite.rows[y][x][3]:
                for dx, dy in ((2, 2), (1, 2), (2, 1)):
                    t.blend(ox + x + dx, oy + y + dy, VOID, 0.55)


def sparks(t):
    """A few embers rising off the circle."""
    for (x, y, c) in ((9, 14, SPARK), (54, 10, BLOOD_HOT), (57, 41, SPARK), (6, 47, BLOOD_HOT),
                      (49, 56, SPARK), (16, 57, SPARK), (31, 3, BLOOD_HOT)):
        t.set(x, y, c)


def build():
    t = Tex(SIZE, SIZE, seed=66)
    background(t)
    trap(t)
    sparks(t)
    gun = colt()
    # Centre the silhouette, not the sprite box: the grip hangs low and to the left.
    opaque = [(x, y) for y in range(gun.h) for x in range(gun.w) if gun.rows[y][x][3]]
    cx = (min(p[0] for p in opaque) + max(p[0] for p in opaque)) / 2
    cy = (min(p[1] for p in opaque) + max(p[1] for p in opaque)) / 2
    ox, oy = round(C - 0.5 - cx), round(C - 0.5 - cy)
    shadow(t, gun, ox, oy)
    t.paste(gun, ox, oy)
    return t


def upscale(t, k):
    return [[t.rows[y // k][x // k] for x in range(t.w * k)] for y in range(t.h * k)]


def generate():
    t = build()
    os.makedirs(OUT, exist_ok=True)
    t.save(os.path.join(OUT, "logo_64.png"))
    write_png(os.path.join(OUT, "logo.png"), upscale(t, SCALE))
    t.save(IN_JAR)


if __name__ == "__main__":
    generate()
