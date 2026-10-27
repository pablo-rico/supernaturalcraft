"""v0.29 "Guilds": the hunters' guilds' art. A guild has one of the 16 vanilla dye colours and one of 12 emblems.

Everything per guild is drawn hue-free and TINTED by the code with the guild's dye (multiply): near-white where the colour
should show strongly, darker greys for shading, near-black for outlines and ink. The emblems are drawn on top, untinted.

Every per-side Chain of Command texture the Leviathans got a twin of has a `_guild` twin here, same size and same slots
(the Pit's, side 0), built by chain_book_art / chain_gui_art's drawing functions with their side 4 / style "guild":

  textures/gui/book/
    chain_paper_guild.png    64x64    the tree's paper (tiles)
    chain_frame_guild.png    384x248  the tree's vignette frame (clear in the middle)
    chain_widgets_guild.png  256x256  medallions (rank*36, 0) ranks 0..3, the Lord's (144, 62), backdrop 26x26 (144, 36),
                                      the links H 8x6 at (0, 108) and V 6x8 at (8, 108), the faction button 64x16
                                      off (0, 184) / on (64, 184)
    chain_modal_guild.png    256x256  folio (0, 0), page (144, 0), tabs (0|32, 48), buttons (state*32, 80), rule (0, 124);
                                      the guild's marker 7x7 (a hanging banner) at (104, 64)
    chain_portrait_guild.png 256x256  portrait arch (0, 0) and its backdrop (0, 80)
    guild_emblems.png         64x48   the 12 emblems, 16x16, index i at ((i % 4) * 16, (i / 4) * 16): 0 devil's trap,
                                      1 anti-possession star, 2 crossed blades, 3 skull, 4 wings, 5 eye, 6 crown, 7 key,
                                      8 flame, 9 shotgun shell, 10 crescent moon, 11 crossroads. Near-white, light-grey
                                      shading, 1px near-black outline: they read plain and multiplied by any dye.
  textures/gui/chain/
    medallions_guild.png     180x108  row 0 (ranks 0..4); the window is left empty (backdrop only) for the guild's emblem,
                                      16x16 centred at (cell + 10, 10)
    pips_guild.png            36x6    x = lit * 6
    court_guild.png          256x72   as court_pit / court_host
  textures/block/
    guild_standard_pole.png   16x16   y 0..3 blackened iron (caps, finial), y 4..15 dark wood with a vertical grain
    guild_standard_cloth.png  16x16   greyscale cloth with folds (tinted), a hem at the top (y 0..1) and bottom (y 15)
    guild_emblem_<i>.png      16x16   the emblem in bone over clear, 12x12 at (2, 0): an untinted overlay for the cloth
  textures/item/
    guild_standard_cloth.png  16x16   layer0 (tinted): the cloth hanging from the crossbar
    guild_standard_pole.png   16x16   layer1 (plain): pole, crossbar and finial only
"""

import math

import chain_book_art as B
import chain_gui_art as G
from book_art import over, with_alpha
from chain_book_art import FRAME_H, FRAME_W, GREY
from chuck_art import h01
from common import save
from pixelkit import Tex, hexc, mix
from vecdraw import Field

SIDE = 4           # chain_book_art's index
NAME = "guild"     # chain_gui_art's style
INK = hexc("#141414")


def grey(t):
    """Drops any hue left by shared colours (ink, pale gold): each pixel becomes its luminance, alpha kept."""
    for y in range(t.h):
        for x in range(t.w):
            r, g, b, a = t.get(x, y)
            if a:
                v = round(.299 * r + .587 * g + .114 * b)
                t.set(x, y, (v, v, v, a))
    return t


# --- the tree (book) ------------------------------------------------------------------------------------------

def paper():
    """The Host's vellum, without its colour: light stock with soft stains and specks."""
    return grey(B.paper(True))


def frame():
    """A darkened edge with a double rule, a gutter shadow and riveted corner straps round a small devil's trap."""
    t = Tex(FRAME_W, FRAME_H)
    for y in range(FRAME_H):
        for x in range(FRAME_W):
            edge = min(x, y, FRAME_W - 1 - x, FRAME_H - 1 - y)
            gutter = abs(x + .5 - FRAME_W / 2)
            if edge < 14:
                over(t, x, y, GREY[0], .3 * (1 - edge / 14) ** 2)
            if edge == 5:
                t.set(x, y, with_alpha(GREY[1], .8))
            elif edge == 6:
                t.set(x, y, with_alpha(GREY[5], .55))
            elif edge == 9 and (x + y) % 4 == 0:
                t.set(x, y, with_alpha(GREY[1], .45))  # A salt line, dotted.
            if gutter < 11:
                over(t, x, y, GREY[0], .22 * (1 - gutter / 11) ** 2)
    trap = emblem(0, 10, (GREY[5], GREY[3], GREY[0]))
    for fx in (False, True):
        for fy in (False, True):
            corner = Tex(36, 36)
            corner.stamp(["ooooooooooooooooooooooo", "ohhhhhhhhhhhhhhhhhhhhho", "oddddddddddddddddddddo."], {"o": GREY[0], "h": GREY[4], "d": GREY[2]}, 4, 4)
            for i in range(20):
                corner.set(4, 4 + i, GREY[0])
                corner.set(5, 5 + i, GREY[4])
                corner.set(6, 6 + i, GREY[2])
                corner.set(7, 7 + i, GREY[0] if i > 2 else None)
            corner.set(4, 24, GREY[0])
            corner.set(5, 25, GREY[0])
            corner.set(6, 26, GREY[0])
            for rx, ry in ((14, 5), (22, 5), (5, 14), (5, 22)):
                corner.set(rx, ry, GREY[5])
                corner.set(rx + 1, ry, GREY[1])
            corner.paste(trap, 11, 11)
            for y in range(36):
                for x in range(36):
                    c = corner.get(x, y)
                    if c[3]:
                        t.set(FRAME_W - 1 - x if fx else x, FRAME_H - 1 - y if fy else y, c)
    return t


def faction_button(on):
    """The guilds' button on the tree: the Host's plate, without its colour."""
    return grey(B._plate(True, on))


def widgets():
    t = Tex(256, 256)
    for rank in range(4):
        t.paste(B.medallion(SIDE, rank), rank * 36, 0)
    t.paste(B.backdrop(SIDE), 144, 36)
    t.paste(B.medallion(SIDE, 4), 144, 62)
    t.paste(B._connector(False, False), 0, 108)
    t.paste(B._connector(False, True), 8, 108)
    for on in (False, True):
        t.paste(B._plate(True, on), int(on) * 64, 184)
    return grey(t)


def modal():
    t = Tex(256, 256)
    t.paste(B.folio(SIDE), 0, 0)
    t.paste(B.page(SIDE), 144, 0)
    for on in (0, 1):
        t.paste(B.tab(SIDE, on), on * 32, 48)
    for state in range(3):
        t.paste(B.action_button(SIDE, state), state * 32, 80)
    t.paste(B.rule(SIDE), 0, 124)
    t.paste(B._marker(NAME), 104, 64)
    return grey(t)


def portraits():
    t = Tex(256, 256)
    t.paste(G.portrait_frame(NAME), 0, 0)
    t.paste(G.portrait_back(NAME), 0, 80)
    return grey(t)


# --- the HUD --------------------------------------------------------------------------------------------------

def medallions():
    t = Tex(180, 108)
    for rank in range(5):
        cell = Tex(36, 36)
        cell.paste(B.backdrop(SIDE), 5, 5)
        G.emblem(cell, NAME)
        cell.paste(B.medallion(SIDE, rank), 0, 0)
        t.paste(cell, rank * 36, 0)
    return grey(t)


def pips():
    t = Tex(36, 6)
    for lit in (0, 1):
        pal = {"o": GREY[0], "h": GREY[5] if lit else GREY[2], "d": hexc("#ffffff") if lit else GREY[1]}
        t.stamp(["..o...", ".oho..", "ohhdo.", ".odo..", "..o...", "......"], pal, lit * 6, 0)
    return t


# --- the emblems ----------------------------------------------------------------------------------------------

class _Pen:
    """Draws in a 14-unit design square, scaled to {@code inner} px with a 1px margin for the outline. Each shape is
    rasterised alone (coverage >= .5) and painted over what is there: value 1 light, .6 mid, 0 ink; a cut clears."""

    def __init__(self, size, inner, ox=1, oy=1):
        self.size, self.k, self.ox, self.oy = size, inner / 14, ox, oy
        self.v = [[None] * size for _ in range(size)]

    def _layer(self, draw, value, cut):
        f = Field(self.size)
        draw(f)
        for y in range(self.size):
            for x in range(self.size):
                if f.a[y][x] >= .5:
                    self.v[y][x] = None if cut else value

    def p(self, x, y):
        return self.ox + x * self.k, self.oy + y * self.k

    def w(self, width):
        return max(.7, width * self.k)

    def seg(self, x0, y0, x1, y1, width, value=1.0, cut=False):
        a, b = self.p(x0, y0), self.p(x1, y1)
        self._layer(lambda f: f.seg(a[0], a[1], b[0], b[1], self.w(width)), value, cut)

    def poly(self, pts, width, value=1.0, closed=False):
        q = [self.p(*c) for c in pts]
        self._layer(lambda f: f.poly(q, self.w(width), closed=closed), value, False)

    def ring(self, cx, cy, r, width, value=1.0):
        c = self.p(cx, cy)
        self._layer(lambda f: f.ring(c[0], c[1], r * self.k, self.w(width)), value, False)

    def disc(self, cx, cy, r, value=1.0, cut=False):
        c = self.p(cx, cy)
        self._layer(lambda f: f.disc(c[0], c[1], r * self.k), value, cut)

    def fill(self, pts, value=1.0, cut=False):
        q = [self.p(*c) for c in pts]
        self._layer(lambda f: f.fill(q), value, cut)


def _star(cx, cy, r_out, r_in, points=5, rot=-90):
    pts = []
    for i in range(points * 2):
        r = r_out if i % 2 == 0 else r_in
        a = math.radians(rot + i * 180 / points)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def _devils_trap(d):
    """Pixel-exact: a 1px ring over a dark disc and a 1px pentagram, its lines mirrored so it stays symmetric. The star's
    top, upper-left and lower-left vertices per canvas size (16 for the GUI, 12 for the cloth)."""
    n = d.size
    c, r = n / 2, n / 2 - 1
    for y in range(n):
        for x in range(n):
            dist = math.hypot(x + .5 - c, y + .5 - c)
            if dist <= r:
                d.v[y][x] = .6 if dist > r - 1 else 0.0
    top, ul, ll = {16: ((7, 2), (2, 6), (4, 12)), 12: ((5, 2), (2, 4), (3, 9))}[n]

    def line(a, b):
        (x0, y0), (x1, y1) = a, b
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(steps + 1):
            u = i / steps
            x, y = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u
            px = math.floor(x + .5) if x < c else math.ceil(x - .5)
            for xx in (px, n - 1 - px):
                d.v[round(y)][xx] = 1.0
    line(top, ll)
    line(ul, (n - 1 - ul[0], ul[1]))
    line(ul, (n - 1 - ll[0], ll[1]))


def _anti_possession(d):
    big = d.size >= 16
    d.fill(_star(7, 7, 7.0 if big else 7.3, 5.0 if big else 4.4, 10 if big else 8, -90 + (18 if big else 22.5)), value=.6)
    d.disc(7, 7, 5.0 if big else 4.7, value=0)  # The flames round it, then the dark ground.
    d.fill(_star(7, 7.3, 4.4 if big else 4.6, 1.8 if big else 2.0))


def _crossed_blades(d):
    for s in (1, -1):
        def m(x, y):
            return (7 + s * (x - 7), y)
        d.fill([m(3.8, 9.0), m(12.4, 0.4), m(13.6, 1.6), m(5.0, 10.2)])
    for s in (1, -1):
        def m(x, y):
            return (7 + s * (x - 7), y)
        d.seg(*m(2.6, 8.4), *m(5.4, 11.2), 1.2, value=.6)  # Crossguard.
        d.seg(*m(3.8, 10.0), *m(1.4, 12.4), 1.4, value=.6)  # Grip and pommel.


def _skull(d):
    d.disc(7, 6.2, 5.6)
    d.fill([(3.9, 9), (10.1, 9), (10.1, 12.9), (3.9, 12.9)])
    d.fill([(1.8, 7.2), (12.2, 7.2), (10.4, 10.2), (3.6, 10.2)])
    d.disc(4.8, 7.0, 1.6, value=0)
    d.disc(9.2, 7.0, 1.6, value=0)
    d.fill([(7, 8.6), (6.0, 10.4), (8.0, 10.4)], value=0)
    for x in (5.6, 7.0, 8.4):
        d.seg(x, 11.4, x, 12.6, .8, value=0)


def _wings(d):
    right = [(7.8, 5.6), (9.2, 3.4), (11.2, 1.8), (13.6, 1.0), (12.4, 3.2), (13.4, 4.8), (11.8, 6.4), (12.6, 8.0),
             (10.6, 8.8), (11.0, 10.8), (9.2, 10.2), (8.8, 12.4), (7.8, 9.6)]
    for s in (1, -1):
        d.fill([(7 + s * (x - 7), y) for x, y in right])
        d.fill([(7 + s * (x - 7), y) for x, y in right[7:] + [(7.8, 7.4), (12.4, 6.8)]], value=.6)


def _eye(d):
    n = 24
    top = [(0.6 + 12.8 * i / n, 7 - 4.8 * math.sin(math.pi * i / n)) for i in range(n + 1)]
    bottom = [(0.6 + 12.8 * i / n, 7 + 4.2 * math.sin(math.pi * i / n)) for i in range(n, -1, -1)]
    d.fill(top + bottom)
    d.disc(7, 7, 3.4, value=.6)
    d.fill([(7, 3.8), (7.9, 7), (7, 10.2), (6.1, 7)], value=0)  # A slit pupil.
    d.disc(8.6, 5.6, .6)


def _crown(d):
    d.fill([(1.2, 12.6), (1.2, 4.6), (4.2, 8.2), (7, 3.0), (9.8, 8.2), (12.8, 4.6), (12.8, 12.6)])
    d.fill([(1.2, 10.0), (12.8, 10.0), (12.8, 12.6), (1.2, 12.6)], value=.6)
    for x, y in ((1.6, 3.6), (7, 1.9), (12.4, 3.6)):
        d.disc(x, y, 1.2)
    for x in (4.0, 7.0, 10.0):
        d.disc(x, 11.3, .8, value=0)


def _key(d):
    d.ring(4.0, 4.0, 2.6, 1.7)
    d.seg(5.9, 5.9, 12.6, 12.6, 1.8)
    d.fill([(9.0, 9.0), (12.2, 12.2), (13.8, 10.6), (10.6, 7.4)])
    d.seg(10.8, 10.8, 12.6, 9.0, .8, cut=True)


def _flame(d):
    d.fill([(7, 13.6), (3.2, 12.4), (1.6, 9.4), (2.0, 6.0), (3.4, 2.8), (4.6, 6.4), (6.0, 4.0), (7.8, 0.4), (9.2, 4.2),
            (10.4, 5.2), (11.8, 3.0), (12.6, 7.0), (12.4, 10.0), (10.8, 12.6)])
    d.fill([(7, 13.0), (5.2, 12.4), (4.4, 10.4), (5.2, 8.0), (6.2, 9.4), (7.4, 5.4), (8.8, 8.6), (9.6, 10.8), (8.8, 12.6)],
           value=.6)


def _shell(d):
    d.fill([(4.4, 2.4), (5.6, 0.8), (8.4, 0.8), (9.6, 2.4), (9.6, 9.0), (4.4, 9.0)])
    d.seg(7, 0.6, 7, 2.6, .8, value=.6)  # The crimp.
    d.fill([(4.0, 9.2), (10.0, 9.2), (10.0, 11.8), (4.0, 11.8)], value=.6)
    d.fill([(3.2, 12.0), (10.8, 12.0), (10.8, 13.6), (3.2, 13.6)], value=.6)
    d.seg(3.2, 9.1, 10.8, 9.1, 1.0, cut=True)
    d.seg(3.0, 11.9, 11.0, 11.9, 1.0, cut=True)


def _crescent(d):
    d.disc(6.4, 7.4, 6.2)
    d.disc(9.4, 5.4, 5.1, cut=True)
    d.fill(_star(11.2, 3.4, 2.6, .8, 4, -90))


def _crossroads(d):
    for s in (1, -1):
        def m(x, y):
            return (7 + s * (x - 7), y)
        d.seg(*m(1.6, 1.6), *m(12.4, 12.4), 3.8)
    for s in (1, -1):
        def m(x, y):
            return (7 + s * (x - 7), y)
        for t0 in (.06, .24, .70, .88):
            a, b = t0, t0 + .09
            d.seg(*m(1.6 + 10.8 * a, 1.6 + 10.8 * a), *m(1.6 + 10.8 * b, 1.6 + 10.8 * b), .8, value=0)


EMBLEMS = (_devils_trap, _anti_possession, _crossed_blades, _skull, _wings, _eye, _crown, _key, _flame, _shell, _crescent,
           _crossroads)
GUI_PAL = (hexc("#f4f4f4"), hexc("#b6b6b6"), hexc("#181818"))
BONE_PAL = (hexc("#ece4cf"), hexc("#bdb197"), hexc("#2a2219"))


def emblem(i, inner, pal):
    """Emblem i drawn {@code inner} px across, plus a 1px outline all round: light, mid and ink from {@code pal}."""
    size = inner + 2
    d = _Pen(size, inner)
    EMBLEMS[i](d)
    t = Tex(size, size)
    for y in range(size):
        for x in range(size):
            v = d.v[y][x]
            if v is not None:
                t.set(x, y, pal[0] if v > .8 else pal[1] if v > .3 else pal[2])
    B._outline(t, pal[2])
    return t


def emblems():
    t = Tex(64, 48)
    for i in range(12):
        t.paste(emblem(i, 14, GUI_PAL), (i % 4) * 16, (i // 4) * 16)
    return t


# --- the standard (block and item) ----------------------------------------------------------------------------

WOOD = (hexc("#1f1712"), hexc("#2e231b"), hexc("#3d3026"), hexc("#4c3d31"))
IRONS = (hexc("#121214"), hexc("#26262b"), hexc("#3c3c43"), hexc("#5e5e68"))


def pole_block():
    t = Tex(16, 16)
    for y in range(16):
        for x in range(16):
            if y < 4:
                c = IRONS[1] if y in (0, 3) else IRONS[2]
                if y == 1:
                    c = IRONS[3] if x % 5 else IRONS[2]
                if (x + 2) % 8 == 0 and y in (1, 2):
                    c = IRONS[0]  # Rivets' shadow.
            else:
                grain = h01(x, 0, 2901)
                k = 1 if grain < .3 else 2 if grain < .8 else 3
                if h01(x, y // 3, 2902) < .12:
                    k -= 1
                c = WOOD[k]
                if y == 4:
                    c = mix(c, IRONS[0], .6)  # Under the iron cap.
            t.set(x, y, c)
    return t


def _folds(x, y):
    """0 dark .. 1 light: soft vertical folds, swaying a little down the cloth."""
    s = math.sin((x + .6 * math.sin(y * .5)) * 2 * math.pi / 5.3 + .7)
    return .5 + .5 * s


def cloth_block():
    t = Tex(16, 16)
    for y in range(16):
        for x in range(16):
            v = .70 + .22 * _folds(x, y) + (h01(x, y, 2903) - .5) * .05
            if y <= 1:
                v = .58 + .1 * _folds(x, y)   # The hem round the crossbar.
            elif y == 2:
                v = .48 if x % 2 == 0 else v  # Its stitching.
            elif y == 15:
                v = .6 + .1 * _folds(x, y)
            g = round(255 * min(1, v))
            t.set(x, y, (g, g, g, 255))
    return t


def emblem_overlay(i):
    t = Tex(16, 16)
    t.paste(emblem(i, 10, BONE_PAL), 2, 0)
    return t


def _item_cloth_shape(x, y):
    if not (3 <= x <= 12 and 4 <= y <= 13):
        return False
    # A swallowtail: the notch rises to y 10 in the middle.
    return y <= 9 + abs(x - 7.5) * .9


def item_cloth():
    t = Tex(16, 16)
    for y in range(16):
        for x in range(16):
            if _item_cloth_shape(x, y):
                v = .72 + .24 * _folds(x * 1.1, y)
                if y == 4:
                    v = .55
                g = round(255 * v)
                t.set(x, y, (g, g, g, 255))
    B._outline(t, hexc("#262626"))
    # The outline must not cover the crossbar row (y 3): the pole layer draws there.
    for x in range(16):
        t.set(x, 3, (0, 0, 0, 0))
    return t


def item_pole():
    t = Tex(16, 16)
    o = hexc("#141012")
    # The pole, visible above the crossbar and under the cloth.
    for y in list(range(1, 3)) + list(range(13, 16)):
        if y >= 13 and _item_cloth_shape(7, y) or y >= 13 and _item_cloth_shape(8, y):
            continue
        t.set(7, y, WOOD[3])
        t.set(8, y, WOOD[1])
    # The finial: an iron spearhead.
    t.stamp(["..o..", ".ohd.", ".ohd."], {"o": IRONS[0], "h": IRONS[3], "d": IRONS[1]}, 5, -1)
    t.set(7, 0, IRONS[3])
    t.set(8, 0, IRONS[1])
    # The crossbar with iron knobs.
    for x in range(2, 14):
        t.set(x, 3, WOOD[3] if x % 4 else WOOD[2])
    for x in (1, 14):
        t.set(x, 3, IRONS[3])
    B._outline(t, o)
    for x in range(3, 13):
        if t.get(x, 4) == o and _item_cloth_shape(x, 4):
            t.set(x, 4, (0, 0, 0, 0))  # Leave the cloth's top row to layer0.
    return t


def generate():
    save(paper(), "gui/book", "chain_paper_" + NAME)
    save(grey(frame()), "gui/book", "chain_frame_" + NAME)
    save(widgets(), "gui/book", "chain_widgets_" + NAME)
    save(modal(), "gui/book", "chain_modal_" + NAME)
    save(portraits(), "gui/book", "chain_portrait_" + NAME)
    save(emblems(), "gui/book", "guild_emblems")
    save(medallions(), "gui/chain", "medallions_" + NAME)
    save(pips(), "gui/chain", "pips_" + NAME)
    save(grey(G.court(NAME)), "gui/chain", "court_" + NAME)
    save(pole_block(), "block", "guild_standard_pole")
    save(cloth_block(), "block", "guild_standard_cloth")
    for i in range(12):
        save(emblem_overlay(i), "block", "guild_emblem_%d" % i)
    save(item_cloth(), "item", "guild_standard_cloth")
    save(item_pole(), "item", "guild_standard_pole")


if __name__ == "__main__":
    generate()
