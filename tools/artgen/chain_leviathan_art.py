"""Chain of Command art for the third court, the Leviathans (SucroCorp): corporate petrol steel, ochre and black goo.

The Pit/Host/Fallen sheets stay as they are. Every per-side texture gets a Leviathan twin named with `_leviathan`:

  textures/gui/book/
    chain_paper_leviathan.png   64x64    the tree's paper (tiles)
    chain_frame_leviathan.png   384x248  the tree's vignette frame
    chain_widgets_leviathan.png 256x256  same layout as chain_widgets.png, the Leviathan sprites in the Pit's slots:
                                         medallions (rank*36, 0) ranks 0..3, the Lord's (144, 62), backdrop (144, 36),
                                         the links of the tree (goo strands) H 8x6 at (0, 108) and V 6x8 at (8, 108),
                                         the faction button 64x16 off (0, 184) / on (64, 184)
    chain_modal_leviathan.png   256x256  same layout as chain_modal.png, side 0's slots: folio (0, 0), page (144, 0),
                                         tabs (0|32, 48), buttons (state*32, 80), rule (0, 124); the Leviathan seat's
                                         marker 7x7 at (104, 64)
    chain_portrait_leviathan.png 256x256 portrait arch (0, 0) and its backdrop (0, 80)
  textures/gui/chain/
    card_leviathan, bar_leviathan, lord_leviathan, court_leviathan   as their pit/host twins
    medallions_leviathan.png    180x108  row 0 (ranks 0..4)
    pips_leviathan.png          36x6     x = lit * 6
    talk_leviathan.png          256x192  row 0
    duel_leviathan.png          256x128  row 0 (the half plate and its mirror)

So in code a Leviathan sprite is the Pit's (side 0) sprite read from the `_leviathan` texture.
"""

import math

import chain_book_art as B
import chain_gui_art as G
from book_art import over, with_alpha
from chain_book_art import GOO, GOO_GLOSS, OCHRE, PETROL, FRAME_W, FRAME_H
from common import save
from pixelkit import Tex, fbm, hexc, mix
from chuck_art import h01

SIDE = 3              # chain_book_art's index
NAME = "leviathan"    # chain_gui_art's style


def paper():
    """Cool grey-green office stock, a few specks of goo."""
    t = Tex(64, 64)
    lo, hi = hexc("#bcc3ba"), hexc("#d6dbd2")
    for y in range(64):
        for x in range(64):
            n = fbm(x, y, 1761, 64, 64, 3, 4)
            c = mix(lo, hi, round(n * 6) / 6)
            stain = fbm(x, y, 1762, 64, 64, 2, 2)
            if stain > .55:
                c = mix(c, PETROL[2], min(.14, (stain - .55) * .5))
            grain = h01(x, y, 1763)
            if grain < .005:
                c = mix(c, GOO[1], .35)
            elif grain < .02:
                c = mix(c, OCHRE[3], .22)
            elif grain > .95:
                c = mix(c, hexc("#ffffff"), .08)
            t.set(x, y, c)
    return t


def frame():
    """Goo creeping in from the edges, dripping from the top; an ochre letterhead rule and corner brackets inside."""
    t = Tex(FRAME_W, FRAME_H)
    for y in range(FRAME_H):
        for x in range(FRAME_W):
            edge = min(x, y, FRAME_W - 1 - x, FRAME_H - 1 - y)
            gutter = abs(x + .5 - FRAME_W / 2)
            wave = 2.6 + 1.6 * math.sin(x * .11 + y * .17) + .9 * math.sin(x * .43 - y * .31)
            # Runs of goo hanging from the top edge, 2px wide with a bulb at the end.
            col = h01(x // 3, 1780)
            length = 5 + col * 160 if col < .1 else 0
            in_run = y < length and x % 3 != 2
            bulb = length and length - 2 <= y <= length + (1 if x % 3 != 2 else 0)
            goo = edge - wave
            if goo < 0 or in_run or bulb:
                gl = (x + y) % 9 == 0 or ((in_run or bulb) and x % 3 == 0 and goo >= 0)
                t.set(x, y, mix(GOO[1], GOO_GLOSS, .4) if gl else GOO[1] if edge > 1 else GOO[0])
            elif goo < 1:
                t.set(x, y, with_alpha(mix(GOO[3], PETROL[3], .3), .9))
            elif goo < 7:
                over(t, x, y, PETROL[1], .45 * (1 - goo / 7) ** 2)
            if edge == 11 and t.get(x, y)[3] < 200:
                over(t, x, y, OCHRE[4], .45)
            if gutter < 11:
                over(t, x, y, GOO[1], .3 * (1 - gutter / 11) ** 2)
    for fx in (False, True):
        for fy in (False, True):
            corner = Tex(30, 30)
            for i in range(14, 24):
                corner.set(i, 14, OCHRE[4])
                corner.set(14, i, OCHRE[4])
                corner.set(i, 15, GOO[0])
                corner.set(15, i, GOO[0])
            G.sugar_cube(corner, 20, 17, 3, 2)
            for y in range(30):
                for x in range(30):
                    c = corner.get(x, y)
                    if c[3]:
                        t.set(FRAME_W - 1 - x if fx else x, FRAME_H - 1 - y if fy else y, c)
    return t


def faction_button(on, width=64):
    """The court's button on the tree: a petrol plate, its rim ochre when chosen."""
    t = Tex(width, 16)
    for y in range(16):
        for x in range(width):
            e = min(x, width - 1 - x, y, 15 - y)
            if e == 0 and x in (0, width - 1) and y in (0, 15):
                continue
            if e == 0:
                c = GOO[0]
            elif e == 1:
                c = (OCHRE[4] if on else PETROL[4]) if y < 8 else (OCHRE[2] if on else PETROL[1])
            else:
                c = PETROL[2]
                if on:
                    c = mix(c, PETROL[4], .28 + max(0, 5 - e) * .07)
                if y > 11:
                    c = mix(c, GOO[1], .25)
                if h01(x, y, 1744) < .035:
                    c = mix(c, GOO[1], .2)
            t.set(x, y, c)
    for x in (3, width - 4):
        t.set(x, 3, OCHRE[5] if on else OCHRE[3])
        t.set(x, 12, OCHRE[2])
    return t


def links():
    """The tree's links: a strand of goo (tiles along its length), H 8x6 and V 6x8."""
    h = Tex(8, 6)
    for x in range(8):
        wob = round(.6 * math.sin(x / 8 * 2 * math.pi))
        h.set(x, 1 + wob, GOO[0])
        h.set(x, 2 + wob, GOO_GLOSS if x % 4 == 1 else GOO[3])
        h.set(x, 3 + wob, GOO[1])
        h.set(x, 4 + wob, GOO[0])
    v = Tex(6, 8)
    for y in range(8):
        for x in range(6):
            c = h.get(y, x)
            if c[3]:
                v.set(x, y, c)
    return h, v


def widgets():
    t = Tex(256, 256)
    for rank in range(4):
        t.paste(B.medallion(SIDE, rank), rank * 36, 0)
    t.paste(B.backdrop(SIDE), 144, 36)
    t.paste(B.medallion(SIDE, 4), 144, 62)
    h, v = links()
    t.paste(h, 0, 108)
    t.paste(v, 8, 108)
    for on in (False, True):
        t.paste(faction_button(on), int(on) * 64, 184)
    return t


def modal():
    t = Tex(256, 256)
    t.paste(B.folio(SIDE), 0, 0)
    t.paste(B.page(SIDE), 144, 0)
    for on in (0, 1):
        t.paste(B.tab(SIDE, on), on * 32, 48)
    for state in range(3):
        t.paste(B.action_button(SIDE, state), state * 32, 80)
    t.paste(B.rule(SIDE), 0, 124)
    t.paste(B._marker("leviathan"), 104, 64)
    return t


def portraits():
    t = Tex(256, 256)
    t.paste(G.portrait_frame(NAME), 0, 0)
    t.paste(G.portrait_back(NAME), 0, 80)
    return t


def medallions():
    t = Tex(180, 108)
    for rank in range(5):
        cell = Tex(36, 36)
        cell.paste(B.backdrop(SIDE), 5, 5)
        G.emblem(cell, NAME)
        cell.paste(B.medallion(SIDE, rank), 0, 0)
        t.paste(cell, rank * 36, 0)
    return t


def pips():
    t = Tex(36, 6)
    s = G.STYLES[NAME]
    for lit in (0, 1):
        pal = {"o": s["rim"][0], "h": s["accent"] if lit else s["rim"][2], "d": s["glow"] if lit else s["rim"][1]}
        t.stamp(["..o...", ".oho..", "ohhdo.", ".odo..", "..o...", "......"], pal, lit * 6, 0)
    return t


def talk():
    t = Tex(256, 192)
    t.paste(G._frame9(NAME), 0, 0)
    t.paste(G._ring(NAME), 64, 0)
    t.paste(G._well(NAME), 128, 0)
    for hover in (0, 1):
        t.paste(G._answer(NAME, hover), 192, hover * 14)
        t.paste(G._badge(NAME, hover), 224, hover * 12)
    return t


def duel():
    t = Tex(256, 128)
    half = G._half(NAME)
    t.paste(half, 0, 0)
    for y in range(24):
        for x in range(112):
            c = half.get(x, y)
            if c[3]:
                t.set(112 + 111 - x, y, c)
    return t


def generate():
    save(paper(), "gui/book", "chain_paper_" + NAME)
    save(frame(), "gui/book", "chain_frame_" + NAME)
    save(widgets(), "gui/book", "chain_widgets_" + NAME)
    save(modal(), "gui/book", "chain_modal_" + NAME)
    save(portraits(), "gui/book", "chain_portrait_" + NAME)
    save(medallions(), "gui/chain", "medallions_" + NAME)
    save(pips(), "gui/chain", "pips_" + NAME)
    save(G.card(NAME), "gui/chain", "card_" + NAME)
    save(G.bar(NAME), "gui/chain", "bar_" + NAME)
    save(G.lord_bar(NAME), "gui/chain", "lord_" + NAME)
    save(G.court(NAME), "gui/chain", "court_" + NAME)
    save(talk(), "gui/chain", "talk_" + NAME)
    save(duel(), "gui/chain", "duel_" + NAME)


if __name__ == "__main__":
    generate()
