"""The Author's rewards and his map marker (v0.10).

  the_end_manuscript -- a thick typed manuscript held by a black binder clip, "THE / END" typed on its top page.
  authors_pen        -- an old black-lacquer fountain pen: gold nib, gold bands and clip, drawn on the diagonal.
  sams_amulet        -- the Samulet: a small bronze horned head on a black cord.
  author_cabin       -- 8x8 map marker in the style of hymnal_spire: a little cabin with a lit window and smoke.
"""

import math

import palette as P
from common import art, save
from pixelkit import Ramp, Tex, hexc, item_outline, mix

O = P.OUTLINE
INK = hexc("1a1410")
PAPER = Ramp("#8f8468", "#b8ad90", "#d6ccb2", "#e9e1cb", "#f5efdf", "#fffaf0")
LACQUER = Ramp("#07070a", "#121218", "#1d1d26", "#2b2b37", "#40404f", "#62627a")
BRONZE = Ramp("#3a2410", "#5e3c1a", "#875a27", "#a97733", "#c99a4a", "#e8c070")

# 3x3 typewriter capitals, enough for "THE END" at item scale.
GLYPHS3 = {
    "T": ["###", ".#.", ".#."],
    "H": ["#.#", "###", "#.#"],
    "E": ["###", "##.", "###"],
    "N": ["##.", "#.#", "#.#"],
    "D": ["##.", "#.#", "##."],
}


def _type(t, x, y, word, c):
    for i, ch in enumerate(word):
        for gy, row in enumerate(GLYPHS3[ch]):
            for gx, px in enumerate(row):
                if px == "#":
                    t.set(x + i * 4 + gx, y + gy, c)


def manuscript():
    t = Tex(16, 16, 1)
    # The stack: page edges peeking out under the top sheet (right and bottom), then the top sheet.
    t.rect(3, 4, 14, 15, PAPER[1])
    t.rect(2, 3, 14, 14, PAPER[2])
    t.rect(1, 2, 13, 13, PAPER[4])
    t.hline(1, 13, 2, PAPER[5])
    # A typed line above the title, uneven like a real draft.
    for x in range(3, 12):
        if (x * 7) % 5:
            t.set(x, 4, PAPER[2])
    _type(t, 2, 6, "THE", INK)
    _type(t, 2, 10, "END", INK)
    # The binder clip at the top, centred, with its silver handles.
    t.rect(5, 1, 9, 3, LACQUER[2])
    t.hline(5, 9, 1, LACQUER[4])
    t.set(5, 0, P.SILVER[3])
    t.set(9, 0, P.SILVER[3])
    t.hline(6, 8, 0, P.SILVER[2])
    item_outline(t, O)
    return t


def _segment_sprite(seed, p0, p1, profile, colour):
    """Draws a round body along a segment, supersampled: profile(s) -> radius, colour(s, d) -> rgba (d = side offset)."""
    t = Tex(16, 16, seed)
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - p0[0], y + 0.5 - p0[1]
            s = (px * ux + py * uy) / length
            d = px * -uy + py * ux
            if -0.02 <= s <= 1.02 and abs(d) <= profile(max(0.0, min(1.0, s))):
                t.set(x, y, colour(s, d))
    return t


def pen():
    nib_end, grip_end, band = 0.24, 0.36, 0.40

    def profile(s):
        if s < nib_end:
            return 0.35 + s / nib_end * 0.9
        if s < grip_end:
            return 1.05
        return 1.45 if s < 0.97 else 1.1

    def colour(s, d):
        lit = d < -0.35  # the upper-left side catches the light
        dark = d > 0.6
        if s < nib_end:
            if abs(d) < 0.3 and s > 0.05:
                return P.GOLD[1]          # the slit
            return P.GOLD[5] if lit else P.GOLD[2] if dark else P.GOLD[4]
        if s < grip_end:
            return LACQUER[3] if lit else LACQUER[1]
        if s < band or 0.66 < s < 0.70 or s > 0.95:
            return P.GOLD[4] if lit else P.GOLD[2] if dark else P.GOLD[3]
        return LACQUER[5] if lit else LACQUER[0] if dark else LACQUER[2]
    t = _segment_sprite(2, (1.6, 14.4), (14.2, 1.8), profile, colour)
    # The clip: a gold line along the cap's lit side.
    for i in range(4):
        t.set(10 + i, 6 - i, P.GOLD[4] if i else P.GOLD[5])
    t.set(13, 2, P.GOLD[5])
    item_outline(t, O)
    return t


def amulet():
    cord = hexc("#2a1d14")
    pal = {"o": O, "c": cord, "h": BRONZE[5], "H": BRONZE[4], "b": BRONZE[3], "m": BRONZE[2], "d": BRONZE[1], "k": BRONZE[0]}
    t = art([
        ".c............c.",
        "..c..........c..",
        "...c........c...",
        "....c......c....",
        ".oo..c....c..oo.",
        "ohbo..cooc..obmo",
        ".ohbo.oHHo.obmo.",
        "..ohboHbbbobmo..",
        "...ohHbbbbbmo...",
        "...oHkkbbkkmo...",
        "...ohbbHbbbmo...",
        "....ohbHbbmo....",
        "....obkkkkdo....",
        ".....obbbmo.....",
        "......oddo......",
        ".......oo.......",
    ], pal)
    return t


def cabin_marker():
    """8x8 map marker: a log cabin with a lit window, smoke from the chimney."""
    pal = {"o": O, "r": hexc("#3e2216"), "R": hexc("#6b3a22"), "w": P.WOOD[4], "W": P.WOOD[5], "d": P.WOOD[2],
           "y": hexc("#ffd25a"), "s": hexc("#d8d4cc"), "c": P.STONE[3]}
    return art([
        "......s.",
        "...oo.c.",
        "..oRRoc.",
        ".oRRRRo.",
        "oRrrrrRo",
        ".oWwyWo.",
        ".odwdwo.",
        ".oooooo.",
    ], pal)


def generate():
    save(manuscript(), "item", "the_end_manuscript")
    save(pen(), "item", "authors_pen")
    save(amulet(), "item", "sams_amulet")
    save(cabin_marker(), "map/decorations", "author_cabin")
