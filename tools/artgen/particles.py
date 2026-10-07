"""8x8 particle sprites, a few animation frames each."""

import math
import random

import palette as P
from common import save
from pixelkit import Tex


def _blob(ramp, radius, seed, core=True):
    t = Tex(8, 8, seed)
    rng = random.Random(seed)
    for y in range(8):
        for x in range(8):
            d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
            if d <= radius + rng.uniform(-0.4, 0.3):
                k = 1 - d / max(0.01, radius + 0.5)
                t.set(x, y, ramp.t(min(1.0, 0.25 + k * (0.9 if core else 0.6))))
    return t


def flame(ramp, name, frames=4):
    for i in range(frames):
        save(_blob(ramp, 3.4 - i * 0.7, 100 + i), "particle", f"{name}_{i}")


def spark(ramp, name, frames=4):
    """A four-pointed twinkle that shrinks frame by frame."""
    for i in range(frames):
        t = Tex(8, 8)
        arm = max(1, 3 - i)
        for s in range(1, arm + 1):
            c = ramp[5 - s] if s < 3 else ramp[2]
            for dx, dy in ((s, 0), (-s, 0), (0, s), (0, -s)):
                t.set(3 + dx, 3 + dy, c)
                t.set(4 + dx if dx > 0 else 4 + dx - 1 if dx < 0 else 4, 4 + dy if dy > 0 else 4 + dy - 1 if dy < 0 else 4, c)
        for x, y in ((3, 3), (4, 3), (3, 4), (4, 4)):
            t.set(x, y, ramp[5])
        save(t, "particle", f"{name}_{i}")


GLYPHS = [
    ["..##....", ".#..#...", "....#...", "...#....", "..####..", "....#...", "...#.#..", "........"],
    ["#.....#.", ".#...#..", "..###...", "...#....", "...#....", "..#.#...", ".#...#..", "........"],
    ["..###...", ".#...#..", ".#......", "..###...", ".....#..", ".#...#..", "..###...", "........"],
    ["...#....", "..###...", ".#.#.#..", "...#....", ".#####..", "...#....", "..#.#...", "........"],
]


def glyphs(ramp, name):
    for i, g in enumerate(GLYPHS):
        t = Tex(8, 8)
        for y, row in enumerate(g):
            for x, ch in enumerate(row):
                if ch == "#":
                    t.set(x, y, ramp[5] if (x + y) % 3 else ramp[4])
        save(t, "particle", f"{name}_{i}")


def smoke(name):
    for i in range(4):
        t = _blob(P.ASH, 3.6 - i * 0.5, 200 + i, core=False)
        # Demon smoke is black with a faint oily sheen.
        for y in range(8):
            for x in range(8):
                c = t.rows[y][x]
                if c[3]:
                    t.rows[y][x] = (c[0] // 2, c[1] // 2, c[2] // 2, 230)
        save(t, "particle", f"{name}_{i}")


def generate():
    flame(P.HELLFIRE, "hellfire")
    spark(P.GRACE, "grace")
    glyphs(P.GRACE, "sigil")
    smoke("demon_smoke")
    spark(P.FROST, "frost", 2)
    flame(P.ASH, "ash", 2)
