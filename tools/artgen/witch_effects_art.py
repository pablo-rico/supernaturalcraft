"""18x18 status-effect icons for the witchcraft effects (v0.21, textures/mob_effect/<id>.png), in the style of
effect_icons.py: a dark ink outline, three or four tones per material, lit from the top left.

  silenced     lips stitched shut, a magenta thread
  agony        a crimson heart-knot with veins spreading out of it
  rot          a rotting apple: green-brown, mould spots, a fly
  name_warded  a silver shield, a magenta border, a line of a written name across it
  mirrored     a jagged shard of mirror glass, a reflection streak, a silver edge
  silver_edge  a silver blade on the diagonal with a star glint
  compelled    an eye whose iris is a honey-gold spiral
"""

import math

from common import save
from pixelkit import Tex, hexc, item_outline, mix

K = hexc("#1E1410")


def _from(rows, pal):
    t = Tex(18, 18)
    for y, row in enumerate(rows):
        assert len(row) == 18, (y, len(row), row)
        for x, ch in enumerate(row):
            if ch in pal:
                t.set(x, y, pal[ch])
    return t


def silenced():
    pal = {"k": K, "l": hexc("#8a2a4a"), "L": hexc("#b5476a"), "h": hexc("#d77a96"), "d": hexc("#5a1530"),
           "t": hexc("#e05bc8"), "T": hexc("#ffb0ea")}
    return _from([
        "..................",
        "..................",
        "..................",
        "....kkk....kkk....",
        "...kLLLk..kLLLk...",
        "..kLhLLLkkLLLhLk..",
        ".kLhLTLLLLLLTLLLk.",
        ".klLLtLLTLLtLLTlk.",
        "kllllTlllTllltlllk",
        "kdkkktkkktkkkTkkdk",
        ".kdllTllltlllTldk.",
        ".kdllllTlllTllldk.",
        "..kdlltlllTllldk..",
        "...kddlllllllddk..",
        "....kkddddddkk....",
        "......kkkkkk......",
        "..................",
        "..................",
    ], pal)


def agony():
    """A crimson heart wrung tight, dark veins branching over it and out past its edges, pulse ticks either side."""
    pal = {"k": K, "d": hexc("#4a0610"), "r": hexc("#7a0c1a"), "R": hexc("#a8182a"), "h": hexc("#d0303e"), "w": hexc("#f0808a"),
           "v": hexc("#2a0208"), "p": hexc("#d0303e")}
    return _from([
        "..................",
        "..................",
        "...kkkk....kkkk...",
        "..khhhRk..kRRRRk..",
        ".khwhRRRkkRRvRRrk.",
        ".khwhRvRRRRRvRRrk.",
        "kpkhRRRvRRRvRRrkpk",
        "kpkhRRRRvRvRRRrkpk",
        ".kRRRvRRRvRRRvrk..",
        ".kRRRRvRRvRRvRrk..",
        "..kRRRRvvvvRRrk...",
        "...kRRRRvvRRrk....",
        "....kRRRvRRrk.....",
        ".....krRvRrk......",
        "......krvrk.......",
        ".......kdk........",
        "........k.........",
        "..................",
    ], pal)


def rot():
    pal = {"k": K, "g": hexc("#4a5a24"), "G": hexc("#6d7a32"), "h": hexc("#94a048"), "b": hexc("#5a3a1c"), "B": hexc("#3a2410"),
           "m": hexc("#c8d27a"), "s": hexc("#4a2e14"), "f": hexc("#20242a"), "w": hexc("#b8c8d8")}
    return _from([
        "..............kk..",
        "......k......kwwk.",
        ".....ksk...kkfkwk.",
        "......ks...kffk...",
        "....kkksk..kk.....",
        "...kGGGksGkk......",
        "..kGhGGGGGGGk.....",
        ".kGhhGmGGGbGGk....",
        ".kGhGGGGGbBbGk....",
        ".kGGGbGGGGbGGgk...",
        ".kGGbBbGGmGGGgk...",
        ".kgGGbGGGGGGggk...",
        ".kgGGGGmGGbggk....",
        "..kgggGGGbBbgk....",
        "...kggggggbgk.....",
        "....kkgggggk......",
        "......kkkkk.......",
        "..................",
    ], pal)


def name_warded():
    pal = {"k": K, "m": hexc("#a3187e"), "M": hexc("#d22aa6"), "p": hexc("#ffb0ea"), "s": hexc("#8c95a1"), "S": hexc("#b5bdc7"),
           "w": hexc("#e0e5ea"), "d": hexc("#626a76"), "i": hexc("#3a1030")}
    return _from([
        "..................",
        "..kkkkkkkkkkkkkk..",
        ".kMMMMMMMMMMMMMMk.",
        ".kMwwwSSSSSSSSsMk.",
        ".kMwSSSSSSSSSssMk.",
        ".kMSSSSSSSSSSssMk.",
        ".kMSiSiiSiSiiSsMk.",
        ".kMSiiSiiiSSiisMk.",
        ".kmSSSSSSSSSsssmk.",
        ".kmSSSSSSSSsssdmk.",
        "..kmSSSSSSsssdmk..",
        "..kmsSSSSsssddmk..",
        "...kmsSSsssddmk...",
        "....kmssssddmk....",
        ".....kmsdddmk.....",
        "......kmmmmk......",
        ".......kkkk.......",
        "..................",
    ], pal)


def mirrored():
    pal = {"k": K, "g": hexc("#5d7a8a"), "G": hexc("#8fb0c0"), "h": hexc("#c8e0ea"), "w": hexc("#f4fbff"), "s": hexc("#a6adb8"),
           "S": hexc("#d9dfe6"), "d": hexc("#3d4f5a")}
    return _from([
        "..................",
        "...........kk.....",
        "..........kSsk....",
        ".........kShGk....",
        "........kShGGgk...",
        ".......kShwGGgk...",
        "......kShwGGGgk...",
        ".....kShwGGGGgdk..",
        "....kShwGGGGggdk..",
        "...kShwGGGGGgdk...",
        "..kShwGGGhGGgdk...",
        "..kshGGGhwGggdk...",
        "...ksGGGGGggdk....",
        "....ksGGGggddk....",
        ".....ksGgdddk.....",
        "......ksddkk......",
        ".......kkk........",
        "..................",
    ], pal)


def silver_edge():
    pal = {"k": K, "s": hexc("#7a828e"), "S": hexc("#a6adb8"), "h": hexc("#d9dfe6"), "w": hexc("#ffffff"), "d": hexc("#4f5661"),
           "b": hexc("#5a3a20"), "B": hexc("#7a5230"), "g": hexc("#b08b36")}
    return _from([
        "..............kkk.",
        ".............khwk.",
        "............khwSk.",
        "...........khwSdk.",
        "..........khwSdk..",
        "....k....khwSdk...",
        "...kwk..khwSdk....",
        "..kwwwkkhwSdk.....",
        "...kwkkhwSdk......",
        "....kkhwSdk.......",
        ".....kgSdk........",
        "....kgkgk.........",
        "...kBkkgk.........",
        "..kBbk.k..........",
        ".kBbk.............",
        "kgbk..............",
        "kkk...............",
        "..................",
    ], pal)


def compelled():
    """An eye, its iris a honey-gold spiral pulling inward."""
    t = Tex(18, 18)
    white = hexc("#f0e6c8")
    lid = hexc("#6a4a1c")
    gold = [hexc("#5a3a08"), hexc("#8a5c10"), hexc("#c08a1c"), hexc("#e8b43a"), hexc("#ffe08a")]
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 8.5
            # The almond of the eye.
            if abs(dy) < 5.2 * (1 - (dx / 8.6) ** 2):
                t.set(x, y, white)
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 8.5
            r = math.hypot(dx, dy)
            if r < 4.4 and t.get(x, y)[3]:
                a = math.atan2(dy, dx)
                s = (a / (2 * math.pi) + r / 2.2) % 1.0
                t.set(x, y, gold[1] if s < 0.45 else gold[3] if s < 0.8 else gold[4])
                if r < 1.1:
                    t.set(x, y, gold[0])
    # Lids: a dark line along the upper edge, lashes.
    for x in range(18):
        for y in range(18):
            if t.get(x, y)[3] and not t.get(x, y - 1)[3]:
                t.set(x, y, lid)
    for x, y in ((4, 3), (8, 2), (12, 3), (6, 2), (10, 2)):
        t.set(x, y, K)
    t.set(6, 7, gold[4])
    item_outline(t, K)
    return t


ICONS = {"silenced": silenced, "agony": agony, "rot": rot, "name_warded": name_warded, "mirrored": mirrored,
         "silver_edge": silver_edge, "compelled": compelled}


def generate():
    for name, fn in ICONS.items():
        save(fn(), "mob_effect", name)


if __name__ == "__main__":
    generate()
