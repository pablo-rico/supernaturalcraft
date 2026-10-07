"""The Author's particles (v0.10): typed letters, scraps of manuscript, golden motes.

  ink_letter_0..7  -- typewriter capitals (slab serifs, uneven ink like a worn ribbon) T H E N D A S ?, each with a
                      faint paper-white rim so they read over ink as well as over paper.
  page_scrap_0..3  -- torn scraps of a typed page, jagged edges, a ruled line or a few typed dashes.
  golden_mote_0..3 -- soft gold motes, growing, the biggest with a four-pointed glint.
"""

import math
import random

from common import save
from pixelkit import Ramp, Tex, hexc, mix

INK = hexc("1a1622")
INK_FADED = hexc("3a3446")
RIM = hexc("efe8d6", 110)
PAPER = Ramp("#9c917a", "#c4baa2", "#dcd3bd", "#ece5d3", "#f6f1e4", "#fffcf4")
TYPED = hexc("4a4250")
RULE = hexc("9fb3cf")
GOLD = Ramp("#8a5a12", "#c08a22", "#e2b443", "#f4d47a", "#fde9ad", "#ffffff")

# 5x7 slab-serif capitals.
GLYPHS = {
    "T": ["#####", "#.#.#", "..#..", "..#..", "..#..", "..#..", ".###."],
    "H": ["##.##", ".#.#.", ".#.#.", ".###.", ".#.#.", ".#.#.", "##.##"],
    "E": ["#####", ".#..#", ".#...", ".###.", ".#...", ".#..#", "#####"],
    "N": ["##.##", ".##.#", ".##.#", ".#.##", ".#.##", ".#..#", "##.##"],
    "D": ["####.", ".#..#", ".#..#", ".#..#", ".#..#", ".#..#", "####."],
    "A": ["..#..", ".#.#.", ".#.#.", "#...#", "#####", "#...#", "##.##"],
    "S": [".####", "#...#", "#....", ".###.", "....#", "#...#", "####."],
    "?": [".###.", "#...#", "....#", "..##.", "..#..", ".....", "..#.."],
}
LETTERS = "THENDAS?"


def letter(ch, seed):
    t = Tex(8, 8, seed)
    rng = random.Random(seed)
    ink = set()
    for y, row in enumerate(GLYPHS[ch]):
        for x, c in enumerate(row):
            if c == "#":
                ink.add((x + 1, y))
    # The rim first (only where the letter is not), then the letter in uneven ribbon ink.
    for (x, y) in ink:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in ink:
                t.set(x + dx, y + dy, RIM)
    for (x, y) in ink:
        t.set(x, y, INK_FADED if rng.random() < 0.18 else INK)
    return t


SCRAPS = [
    ["..######", ".#######", "########", "#######.", "######..", ".####...", "..##....", "........"],
    ["........", "#####...", "#######.", "########", ".#######", "..######", "...####.", "....##.."],
    [".###....", "######..", "#######.", "#######.", "########", ".######.", "..#####.", "....##.."],
    ["....###.", "..######", ".#######", "########", "#######.", "#####...", "##......", "........"],
]


def scrap(i):
    t = Tex(8, 8, 40 + i)
    rng = random.Random(40 + i)
    shape = {(x, y) for y, row in enumerate(SCRAPS[i]) for x, c in enumerate(row) if c == "#"}
    for (x, y) in shape:
        edge = any((x + dx, y + dy) not in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        c = PAPER[2] if edge else PAPER[4 if (x + y) % 3 else 3]
        if edge and rng.random() < 0.3:
            c = PAPER[1]  # torn fibres
        t.set(x, y, c)
    # One ruled line (blue) or a row of typed dashes, clipped to the scrap.
    y = 3 + i % 2
    for x in range(8):
        if (x, y) in shape and not any((x + dx, y) not in shape for dx in (1, -1)):
            t.set(x, y, mix(t.get(x, y), RULE if i % 2 == 0 else TYPED, 0.55 if i % 2 == 0 else 0.8))
    return t


def mote(i):
    t = Tex(8, 8, 60 + i)
    r = 1.0 + i * 0.55
    for y in range(8):
        for x in range(8):
            d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
            if d <= r + 0.6:
                k = max(0.0, 1 - d / (r + 0.6))
                a = int(255 * min(1.0, 0.35 + k * 1.1))
                c = GOLD.t(min(1.0, 0.35 + k * 0.8))
                t.set(x, y, (c[0], c[1], c[2], a))
    if i >= 2:
        # A four-pointed glint through the centre.
        for s in range(0, 4):
            a = max(60, 230 - s * 55)
            for (x, y) in ((3 - s, 3), (4 + s, 4), (3, 3 - s), (4, 4 + s), (3 - s, 4), (4 + s, 3), (4, 3 - s), (3, 4 + s)):
                cur = t.get(x, y)
                c = GOLD[5] if s == 0 else GOLD[4]
                t.set(x, y, (c[0], c[1], c[2], max(a, cur[3] if cur[3] else 0)))
    return t


def generate():
    for i, ch in enumerate(LETTERS):
        save(letter(ch, 70 + i), "particle", f"ink_letter_{i}")
    for i in range(4):
        save(scrap(i), "particle", f"page_scrap_{i}")
        save(mote(i), "particle", f"golden_mote_{i}")
