"""Chain of Command book art. Sprite coordinates must match client/book/chain/ChainAtlas.java.

Pure stdlib, fixed seeds, pixel outlines and single-pixel bevels. Frame sheets are 384x248.

v0.27 adds chain_modal.png (256x256), the officer's record and the war panel, sides 0 Pit, 1 Host, 2 Fallen:
  folio      48x48 at (side*48, 0)       nine-slice, corners 12, the edges and middle tile: dark leather and ember / ivory
                                         vellum and gold / ash and tarnished gilt
  page       32x32 at (144 + side*32, 0) nine-slice, corners 6: the parchment insert the tabs open onto
  tab        32x16 at ((side*2 + on)*32, 48), caps 6 (the middle tiles); the open one is the page's colour, open below
  close      12x12 at (192 + on*12, 48)
  icons      10x10 at (i*10, 64): chronicle, bonds, duels, actions, front
  bond icons  9x9  at (60 + i*10, 64): loyal, rival, grudge, pact
  markers     7x7  at (104 + i*8, 64): Pit seat, Host seat, the reader
  buttons    32x14 at (state*32, 80 + side*14), state off / hover / disabled; nine-slice, caps 4
  rule       64x5  at (0, 124 + side*6): a heading's flourish (the middle tiles)
and chain_dial.png (128x128): the front's dial, a ring r 57..62 round (64, 64) with sector notches and a hub of r 6.
In chain_widgets.png: the bonds and front buttons (20x16 at (80 + i*40 + on*20, 200)), the hunter's rim and the reader's
(36x36 at (0, 216) and (36, 216)) and the pact's link (9x7 at (72, 216)).
"""

import math

import palette as P
from book_art import BRASS, INK, IVORY, h01, over, with_alpha
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

IRON = Ramp("#17151b", "#302a30", "#494048", "#70616a", "#a27d68", "#df9c65")
SOOT_GOLD = Ramp("#302b35", "#51464a", "#76674f", "#9c8b62", "#beac7e", "#dbcea5")
EMBER = hexc("#ef8b42")
PALE = hexc("#fff1cf")
# The Leviathans' court (SucroCorp, side/faction 3; its sheets are built by chain_leviathan_art): petrol steel, ochre, goo.
PETROL = Ramp("#040a0a", "#0d2321", "#173734", "#24504a", "#3b7268", "#7fb0a4")
OCHRE = Ramp("#2a1d05", "#5a3f0c", "#8c6414", "#b98a1e", "#ddb03a", "#f5d77a")
GOO = Ramp("#020203", "#060608", "#0c0c10", "#15151b", "#24242d", "#4a4a58")
GOO_GLOSS = hexc("#5f7d78")
FRAME_W, FRAME_H = 384, 248


def _stud(t, x, y, ramp):
    t.rect(x - 1, y - 1, x + 1, y + 1, ramp[0])
    t.set(x - 1, y - 1, ramp[5])
    t.set(x, y, ramp[4])
    t.set(x + 1, y, ramp[2])


def medallion(faction, rank):
    t = Tex(36, 36)
    ramp = (IRON, BRASS, SOOT_GOLD, PETROL)[faction]
    # Side ornaments are behind the rim and never enter the model window.
    if rank >= 2:
        for flip in (False, True):
            wing = ([".....o", "....oh", "...ohh", "..ohhh", ".ohhho", "ohhho.", "ohho..", "oho...", "ohho..", ".oho..", "..oo..", "...o.."]
                    if faction else
                    ["o.....", "oh....", "ohh...", ".ohh..", ".ohhho", "..ohhh", "..ohho", "...oho", "....oo", "......", "......", "......"])
            if faction == 3:
                # Goo running off the rim in two strands.
                wing = ["....oo", "...ohh", "..ohho", "..oho.", ".ohho.", ".oho..", ".oho..", "ohho..", "oho...", "oho...", "ohho..", ".oo..."]
            ornament = Tex(6, 12)
            ornament.stamp(wing, {"o": ramp[0], "h": ramp[4]} if faction != 3 else {"o": GOO[0], "h": GOO[3]})
            if faction == 3:
                ornament.set(4, 1, GOO_GLOSS)
                ornament.set(2, 7, GOO_GLOSS)
            for y in range(12):
                for x in range(6):
                    c = ornament.get(x, y)
                    if c[3]:
                        t.set(35 - x if flip else x, 8 + y, c)
    for y in range(36):
        for x in range(36):
            dx, dy = x + .5 - 18, y + .5 - 18
            d = math.hypot(dx, dy)
            light = (-dx - dy) / max(1, d)
            if d <= 12:
                continue  # The model is drawn between its backdrop and this frame.
            elif d <= 16:
                if d > 15 or d <= 12.8:
                    c = ramp[0]
                elif d > 14:
                    c = ramp[4 if light > .15 else 2]
                else:
                    c = ramp[3 if light > -.3 else 1]
                if faction == 0 and 13 < d < 14.5 and light > .65:
                    c = mix(c, EMBER, .65)
                if faction == 3 and 13 < d < 14.5 and light > .55:
                    c = mix(c, OCHRE[4], .7)  # The company's ochre band catching the light.
                if faction == 2:
                    if (x + 2 * y) % 19 == 0:
                        c = ramp[0]  # Fine fractures in the tarnished metal.
                    elif h01(x, y, 714) < .17:
                        c = mix(c, hexc("#292631"), .4)
                t.set(x, y, c)
    if rank >= 1:
        for x, y in ((7, 7), (28, 7), (7, 28), (28, 28)):
            _stud(t, x, y, ramp)
    if rank == 3:
        # A Commander's chevron: the crown's place, without the crown.
        chevron = ["....o....", "...oho...", "..ohhho..", ".ohhohho.", "ohho.ohho", "oo.....oo"]
        t.stamp(chevron, {"o": ramp[0], "h": ramp[4] if faction != 3 else OCHRE[4]}, 13, 0)
        t.set(17, 2, P.BLOOD[5] if faction == 0 else OCHRE[5] if faction == 3 else PALE)
    if rank >= 4:
        crown = ["o....o....o", "ho..oho..oh", "hhoohhhoohh", "ohhhhhhhho.", ".ooooooooo.", ".hhhhhhhhh."]
        t.stamp(crown, {"o": ramp[0], "h": ramp[2] if faction == 0 else OCHRE[4] if faction == 3 else ramp[4]}, 12, 0)
        t.set(17, 2, P.BLOOD[5] if faction == 0 else OCHRE[5] if faction == 3 else PALE)
        t.set(17, 3, P.BLOOD[3] if faction == 0 else OCHRE[2] if faction == 3 else ramp[5])
        if faction == 0:
            for x in (14, 20):
                t.set(x, 3, IRON[1])
    return t


def backdrop(faction):
    """Opaque model window, placed five pixels inside the 36px frame."""
    t = Tex(26, 26)
    low, high = (("#150e16", "#522521"), ("#111c33", "#334563"), ("#24212e", "#514957"), ("#030707", "#1d4842"))[faction]
    for y in range(26):
        for x in range(26):
            dx, dy = x + .5 - 13, y + .5 - 13
            d = math.hypot(dx, dy)
            if d <= 13:
                c = mix(hexc(low), hexc(high), (dy + 13) / 26 * .8)
                if faction == 1 and d > 10.5:
                    c = mix(c, BRASS[3], .18)
                if faction == 3 and (x + y) % 6 == 0 and dy < 0:
                    c = mix(c, PETROL[4], .12)  # Faint glass-tower glints.
                t.set(x, y, c)
    return t


def _rim(selected=False, vacant=False):
    t = Tex(36, 36)
    for y in range(36):
        for x in range(36):
            d = math.hypot(x + .5 - 18, y + .5 - 18)
            if vacant:
                angle = math.atan2(y + .5 - 18, x + .5 - 18)
                if 14 <= d <= 15 and int((angle + math.pi) * 12) % 3 != 2:
                    t.set(x, y, with_alpha(INK, .38))
            elif selected and 15.5 <= d <= 18:
                t.set(x, y, with_alpha(PALE, .95 if d < 17 else .28))
            elif not selected and 16 <= d <= 17.2:
                t.set(x, y, with_alpha(PALE, .9))
    return t


def _connector(gold=False, vertical=False):
    t = Tex(8, 4 if gold else 6)
    if gold:
        for x in range(8):
            for y, c in enumerate((with_alpha(BRASS[4], .25), BRASS[3], PALE, with_alpha(BRASS[4], .35))):
                t.set(x, y, c)
    else:
        # An oval face link and edge-on half links crossing the tile boundary.
        t.stamp(["..oooo..", ".ohhhho.", "oh....ho", "od....do", ".oddddo.", "..oooo.."],
                {"o": IRON[0], "h": IRON[4], "d": IRON[2]})
        for x in (0, 1, 6, 7):
            t.set(x, 2, IRON[3])
            t.set(x, 3, IRON[1])
    if not vertical:
        return t
    result = Tex(t.h, t.w)
    for y in range(t.h):
        for x in range(t.w):
            result.set(y, x, t.get(x, y))
    return result


GLYPHS = {
    "crown": ["..........", ".o..o..o..", ".ho.ho.oh.", ".hhohhohh.", "..hhhhhh..", "..oddddo..", "..hhhhhh..", "..oooooo..", "..........", ".........."],
    "returning": [".oooooooo.", "..hhhhhh..", "..oho.ho..", "...ohho...", "....oo....", "...ohho...", "..ohh.ho..", "..hhhhhh..", ".oooooooo.", ".........."],
    "kill": ["..oooooo..", ".ohhhhhho.", ".hhhohhho.", ".hoohhooh.", ".hoohhooh.", "..hhddhh..", "..ohhhho..", "...hoho...", "...oooo...", ".........."],
    "ember": [".....o....", "....oh....", "...ohho...", ".o.ohhho..", ".hohhhhh..", ".hhhhhhho.", "..hhhhhho.", "..ohhhho..", "...oddo...", "....oo...."],
    "scroll": [".ooooooo..", "ohhhhhho..", "ohoooho...", ".hhdhho...", ".hhdhho...", ".hhdhho...", ".hhhhhhhho", "..ooooooo.", "..........", ".........."],
    "recenter": ["....o.....", "...oho....", "....h.....", ".o..h..o..", "ohhhohhho.", ".o..h..o..", "....h.....", "...oho....", "....o.....", ".........."],
}


def _glyph(name):
    t = Tex(10, 10)
    pal = {"o": INK, "h": IVORY[5], "d": BRASS[2]}
    if name == "crown":
        pal["h"] = BRASS[4]
    if name == "ember":
        pal = {"o": P.BLOOD[1], "h": EMBER, "d": PALE}
    t.stamp(GLYPHS["kill" if name == "dead" else name], pal)
    if name == "dead":
        t.line(5, 5, 9, 9, P.BLOOD[1])
        t.line(5, 9, 9, 5, P.BLOOD[1])
        for x, y in ((6, 6), (8, 8), (6, 8), (8, 6)):
            t.set(x, y, P.BLOOD[4])
    return t


def _plate(host, on, width=64):
    t = Tex(width, 16)
    ramp = IVORY if host else P.BLOOD
    for y in range(16):
        for x in range(width):
            e = min(x, width - 1 - x, y, 15 - y)
            if e == 0 and x in (0, width - 1) and y in (0, 15):
                continue
            if e == 0:
                c = BRASS[0] if host else IRON[0]
            elif e == 1:
                c = (BRASS[4 if on else 3] if host else IRON[4 if on else 2]) if y < 8 else IRON[1]
            else:
                c = ramp[3 if host else 1]
                if on:
                    c = mix(c, ramp[5 if host else 4], .28 + max(0, 5 - e) * .07)
                if y > 11:
                    c = mix(c, ramp[0], .2)
                if h01(x, y, 744) < .035:
                    c = mix(c, ramp[0], .13)
            t.set(x, y, c)
    for x in (3, width - 4):
        t.set(x, 3, BRASS[5] if host else IRON[4])
        t.set(x, 12, BRASS[2] if host else IRON[2])
    return t


def _card():
    t = Tex(48, 48)
    for y in range(48):
        for x in range(48):
            e = min(x, y, 47 - x, 47 - y)
            c = mix(P.PARCHMENT[4], IVORY[4], .5)
            if e == 0:
                c = IRON[0]
            elif e == 1:
                c = BRASS[3 if x + y < 48 else 1]
            elif e == 2:
                c = IRON[1]
            elif e == 3:
                c = IVORY[1]
            elif e < 7:
                c = mix(c, IVORY[2], (7 - e) * .12)
            else:
                c = mix(c, IVORY[2], h01(x, y, 750) * .055)
            t.set(x, y, c)
    for x, y in ((4, 4), (43, 4), (4, 43), (43, 43)):
        _stud(t, x, y, BRASS)
    return t


def widgets():
    t = Tex(256, 256)
    for faction in range(3):
        t.paste(backdrop(faction), 144 + faction * 26, 36)
        for rank in range(4):
            t.paste(medallion(faction, rank), rank * 36, faction * 36)
        t.paste(medallion(faction, 4), 144 + faction * 36, 62)  # The Lord's, under the backdrops.
    t.paste(_rim(), 144, 0)
    t.paste(_rim(selected=True), 180, 0)
    t.paste(_rim(vacant=True), 216, 0)
    for x, gold, vertical in ((0, False, False), (8, False, True), (16, True, False), (24, True, True)):
        t.paste(_connector(gold, vertical), x, 108)
    broken = Tex(12, 12)
    broken.stamp(["............", "..oooo......", ".ohhho......", ".oh..o......", ".od.........", "..odo.......", ".......oho..", ".........ho.", "......o..do.", "......odddo.", "......oooo..", "............"],
                 {"o": IRON[0], "h": IRON[4], "d": IRON[2]})
    t.paste(broken, 32, 108)
    for x, name in ((0, "crown"), (10, "returning"), (20, "dead"), (30, "kill"), (50, "ember")):
        t.paste(_glyph(name), x, 120)
    pip = Tex(5, 5)
    pip.stamp(["..o..", ".oho.", "ohhdo", ".odo.", "..o.."], {"o": BRASS[0], "h": BRASS[5], "d": BRASS[3]})
    t.paste(pip, 40, 120)
    t.paste(_card(), 0, 136)
    for host in (False, True):
        for on in (False, True):
            t.paste(_plate(host, on), int(host) * 128 + int(on) * 64, 184)
    for i, name in enumerate(("scroll", "recenter")):
        for on in (False, True):
            button = _plate(False, on, 20)
            button.paste(_glyph(name), 5, 3)
            t.paste(button, i * 40 + int(on) * 20, 200)
    for i, name in enumerate(("bonds", "front")):
        for on in (False, True):
            button = _plate(False, on, 20)
            button.paste(_icon(name, {"o": INK, "h": IVORY[5], "d": BRASS[2], "r": P.BLOOD[4]}), 5, 3)
            t.paste(button, 80 + i * 40 + int(on) * 20, 200)
    t.paste(_hunter_rim(False), 0, 216)
    t.paste(_hunter_rim(True), 36, 216)
    t.paste(_pact_link(), 72, 216)
    return t


def paper(host):
    t = Tex(64, 64)
    lo, hi = (hexc("#ddcba2"), hexc("#f1e6c8")) if host else (hexc("#b49774"), hexc("#d1b794"))
    for y in range(64):
        for x in range(64):
            n = fbm(x, y, 761, 64, 64, 3, 4)
            c = mix(lo, hi, round(n * 6) / 6)
            stain = fbm(x, y, 762, 64, 64, 2, 2)
            if stain > .53:
                c = mix(c, BRASS[2] if host else IRON[0], min(.16, (stain - .53) * .5))
            grain = h01(x % 64, y % 64, 763)
            if grain < .018:
                c = mix(c, BRASS[4] if host else EMBER, .26)
            elif grain > .94:
                c = mix(c, IVORY[1], .07)
            t.set(x, y, c)
    return t


def frame(host):
    t = Tex(FRAME_W, FRAME_H)
    for y in range(FRAME_H):
        for x in range(FRAME_W):
            edge = min(x, y, FRAME_W - 1 - x, FRAME_H - 1 - y)
            gutter = abs(x + .5 - FRAME_W / 2)
            if host:
                if edge < 12:
                    over(t, x, y, BRASS[4], .16 * (1 - edge / 12) ** 2)
                if edge == 6:
                    t.set(x, y, with_alpha(BRASS[2], .75))
                elif edge == 7:
                    t.set(x, y, with_alpha(PALE, .65))
            else:
                wave = 2.2 + 1.8 * math.sin(x * .13 + y * .19) + .8 * math.sin(x * .51 - y * .28)
                burn = edge - wave
                if burn < 0:
                    t.set(x, y, hexc("#211a1a") if edge < 1 else hexc("#38261f"))
                elif burn < 1:
                    t.set(x, y, with_alpha(mix(hexc("#6e3925"), EMBER, .45), .87))
                elif burn < 7:
                    over(t, x, y, hexc("#493027"), .48 * (1 - burn / 7) ** 2)
                if 5 < edge < 16 and h01(x, y, 780) < .008:
                    t.set(x, y, with_alpha(EMBER, .65))
            if gutter < 11:
                over(t, x, y, BRASS[1] if host else IRON[0], (.12 if host else .33) * (1 - gutter / 11) ** 2)
    if host:
        for flip_x, flip_y in ((False, False), (True, False), (False, True), (True, True)):
            ornament = Tex(34, 34)
            ornament.line(6, 10, 6, 29, BRASS[3])
            ornament.line(10, 6, 29, 6, BRASS[3])
            for k in range(26):
                angle = k / 25 * math.pi * 1.7
                r = 6 - k / 25 * 3
                ornament.set(round(16 + math.cos(angle) * r), round(16 + math.sin(angle) * r), BRASS[3])
            ornament.line(9, 9, 13, 13, BRASS[4])
            # A small ivory feather with one-pixel gold-grey quill.
            for y in range(12):
                for x in range(7):
                    if ((x - 3) / 3) ** 2 + ((y - 5.5) / 6) ** 2 <= 1:
                        ornament.set(22 + x - y // 3, 17 + y, IVORY[5] if x < 4 else IVORY[3])
            ornament.line(26, 18, 20, 31, IVORY[1])
            for y in range(34):
                for x in range(34):
                    c = ornament.get(x, y)
                    if c[3]:
                        t.set(FRAME_W - 1 - x if flip_x else x, FRAME_H - 1 - y if flip_y else y, c)
    return t


# --- v0.27: the officer's record, the bonds and the front ------------------------------------------------------

SILVER = Ramp("#2a3038", "#4c5866", "#7a8a9c", "#a9b8c8", "#d4dee8", "#f4f8fc")
SKY = hexc("#86b6e3")
ASHEN = Ramp("#141216", "#2b272c", "#433d42", "#625a5c", "#8a8180", "#b8b0a8")
RIMS = (IRON, BRASS, SOOT_GOLD, PETROL)
# The folio's leather (high, low) and the page's parchment (low, high), per side.
LEATHERS = ((hexc("#2e1714"), hexc("#170a0b")), (hexc("#efe4c4"), hexc("#d9c9a0")), (hexc("#4a4446"), hexc("#2c2729")),
            (hexc("#123430"), hexc("#06110f")))
PAGES = ((hexc("#c2a27a"), hexc("#d6bd97")), (hexc("#efe5c9"), hexc("#f8f1dc")), (hexc("#b9b1a6"), hexc("#ccc5ba")),
         (hexc("#bcc4bc"), hexc("#d4dad2")))


def _outline(t, c):
    """Rings every painted pixel with an outline in the transparent pixels around it."""
    rows = t.copy_rows()
    for y in range(t.h):
        for x in range(t.w):
            if rows[y][x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < t.w and 0 <= y + dy < t.h and rows[y + dy][x + dx][3] and rows[y + dy][x + dx] != c:
                    t.set(x, y, c)
                    break


def _ellipse(t, cx, cy, rx, ry, phi, c):
    for k in range(96):
        a = k / 96 * 2 * math.pi
        ex, ey = rx * math.cos(a), ry * math.sin(a)
        t.set(round(cx + ex * math.cos(phi) - ey * math.sin(phi) - .5), round(cy + ex * math.sin(phi) + ey * math.cos(phi) - .5), c)


def _icon(name, pal, size=10):
    """A small outlined glyph drawn from primitives: o outline, h light, d mid, r an accent."""
    t = Tex(size, size)
    h, d, r = pal["h"], pal["d"], pal.get("r", pal["d"])
    if name == "chronicle":
        t.stamp(GLYPHS["scroll"], pal)
        return t
    if name == "bonds":
        _ellipse(t, 3.6, 6.4, 2.6, 1.6, -math.pi / 4, h)
        _ellipse(t, 6.4, 3.6, 2.6, 1.6, -math.pi / 4, d)
        t.set(4, 4, h)
        t.set(5, 5, h)
    elif name == "duels":
        t.line(2, 6, 7, 1, h)
        t.line(7, 6, 2, 1, h)
        t.set(7, 1, d)
        t.set(2, 1, d)
        for x, y in ((1, 6), (3, 8), (8, 6), (6, 8)):
            t.set(x, y, d)
        t.set(1, 8, r)
        t.set(8, 8, r)
        t.set(2, 7, d)
        t.set(7, 7, d)
    elif name == "actions":
        t.rect(1, 2, 8, 7, h)
        t.line(1, 2, 4, 5, d)
        t.line(8, 2, 5, 5, d)
        t.rect(4, 5, 5, 6, r)
    elif name == "front":
        t.vline(2, 1, 8, d)
        t.rect(3, 1, 7, 4, r)
        t.set(7, 3, (0, 0, 0, 0))
        t.set(8, 1, r)
        t.set(8, 4, r)
        t.set(3, 1, h)
    _outline(t, pal["o"])
    return t


def _bond_icon(kind):
    t = Tex(9, 9)
    heart = [".hh.hh.", "hhhhhhh", "hhhhhhh", ".hhhhh.", "..hhh..", "...h..."]
    if kind == "loyal":
        t.stamp(heart, {"h": BRASS[4]}, 1, 1)
        t.set(2, 2, PALE)
        t.set(2, 3, BRASS[5])
        for x in range(3, 7):
            t.set(x, 5, BRASS[2])
    elif kind == "rival":
        t.stamp(heart, {"h": P.BLOOD[5]}, 1, 1)
        t.set(2, 2, hexc("#e8735e"))
        for x, y in ((4, 1), (4, 2), (3, 3), (4, 4), (5, 4), (4, 5)):
            t.set(x, y, P.BLOOD[0])
    elif kind == "grudge":
        drop = ["...h...", "...h...", "..hhh..", ".hhhhh.", ".hhhhh.", ".hhhhh.", "..hhh.."]
        t.stamp(drop, {"h": P.BLOOD[3]}, 1, 0)
        t.set(3, 4, hexc("#e8735e"))
        t.set(3, 5, P.BLOOD[5])
    else:
        _ellipse(t, 3.4, 5.6, 2.4, 1.5, -math.pi / 4, BRASS[4])
        _ellipse(t, 5.6, 3.4, 2.4, 1.5, -math.pi / 4, IRON[4])
    _outline(t, hexc("#1a1010"))
    return t


def _marker(kind):
    t = Tex(7, 7)
    arts = {
        "pit": ["...o...", "..oho..", ".ohhho.", "ohhdhho", ".ohhho.", "..oho..", "...o..."],
        "host": ["...o...", "..oho..", "oohdhoo", "ohdddho", "oohdhoo", "..oho..", "...o..."],
        "player": [".ooooo.", "ohhhhho", "ohhdhho", "ohdddho", "ohhdhho", "ohhhhho", ".ooooo."],
        "leviathan": ["...o...", ".oohoo.", "ohhhhho", "oddhcco", "oddhcco", ".odhco.", "..ooo.."],
    }
    pal = {"pit": {"o": hexc("#1a0b0b"), "h": EMBER, "d": PALE}, "host": {"o": BRASS[0], "h": PALE, "d": BRASS[4]},
           "player": {"o": hexc("#101418"), "h": hexc("#ffffff"), "d": SKY},
           "leviathan": {"o": GOO[0], "h": OCHRE[5], "d": OCHRE[3], "c": OCHRE[1]}}[kind]
    t.stamp(arts[kind], pal)
    return t


def _hunter_rim(mine):
    """A hunter's post: a silver double ring with four notches (the reader's own glows sky-blue)."""
    t = Tex(36, 36)
    for y in range(36):
        for x in range(36):
            dx, dy = x + .5 - 18, y + .5 - 18
            d = math.hypot(dx, dy)
            light = (-dx - dy) / max(1, d)
            if 15.5 <= d <= 17.5:
                t.set(x, y, SILVER[4 if light > .2 else 2] if d < 17 else SILVER[0])
            elif mine and 17.5 < d <= 18:
                t.set(x, y, with_alpha(SKY, .9))
            elif mine and 12.3 <= d < 13.2:
                t.set(x, y, with_alpha(SKY, .55))
    for ang in (0, 90, 180, 270):
        a = math.radians(ang + 45)
        for r in (14, 15, 16, 17):
            t.set(round(18 + math.cos(a) * r - .5), round(18 + math.sin(a) * r - .5), SILVER[5] if r < 17 else SILVER[1])
    return t


def _pact_link():
    t = Tex(9, 7)
    _ellipse(t, 3.2, 3.5, 2.6, 1.6, 0, BRASS[4])
    _ellipse(t, 5.8, 3.5, 2.6, 1.6, 0, BRASS[3])
    t.set(4, 2, PALE)
    _outline(t, BRASS[0])
    return t


def _shape(e, x, y, w, h, chamfer):
    """Whether (x, y) is cut off by a chamfered corner."""
    cx, cy = min(x, w - 1 - x), min(y, h - 1 - y)
    return cx + cy < chamfer


def folio(side):
    """The record's binding, a 48px nine-slice (corners 12; edges and middle tile)."""
    t = Tex(48, 48)
    rim = RIMS[side]
    hi, lo = LEATHERS[side]
    for y in range(48):
        for x in range(48):
            if _shape(0, x, y, 48, 48, 3):
                continue
            e = min(x, y, 47 - x, 47 - y)
            lit = (x <= y and x < 24) or (y < x and y < 24)
            lit = min(x, y) < min(47 - x, 47 - y)
            n = h01(x % 24, y % 24, 900 + side)
            body = mix(hi, lo, .35 + n * .3) if side != 1 else mix(lo, hi, .55 + n * .3)
            if e == 0:
                c = rim[0]
            elif e == 1:
                c = rim[4] if lit else rim[1]
            elif e == 2:
                c = rim[3] if lit else rim[2]
                if side == 0 and lit and h01(x, y, 905) < .1:
                    c = mix(c, EMBER, .55)
                if side == 3 and lit:
                    c = mix(c, OCHRE[3], .45)
            elif e == 3:
                c = rim[0]
            elif e == 4:
                c = mix(body, (0, 0, 0, 255), .35 if side != 1 else .12)
            elif e == 7:
                c = mix(body, (P.BLOOD[4], BRASS[3], SOOT_GOLD[3], OCHRE[4])[side], .55 if side != 2 else .4)
            elif e == 8:
                c = mix(body, (0, 0, 0, 255), .25 if side != 1 else .08)
            else:
                c = body
                if side == 0 and n < .025:
                    c = mix(c, EMBER, .35)
                if side == 1 and n > .985:
                    c = mix(c, BRASS[4], .4)
                if side == 2 and (x * 3 + y * 5) % 29 == 0:
                    c = mix(c, ASHEN[0], .5)
                if side == 3 and (y % 6 == 0) and h01(x // 3, y, 906) < .5:
                    c = mix(c, PETROL[3], .25)  # A faint pinstripe in the leather.
            t.set(x, y, c)
    # Corner pieces in the 12px corners.
    for fx in (False, True):
        for fy in (False, True):
            corner = Tex(12, 12)
            if side == 0:
                corner.stamp(["....oooooo..", "....ohhhhho.", "....oddddo..", "oooo.oooo...", "ohdo........",
                              "ohdo........", "ohdo........", "ohdo........", "oho.........", "oo.........."],
                             {"o": IRON[0], "h": IRON[4], "d": IRON[2]}, 1, 1)
                corner.set(6, 6, EMBER)
                corner.set(7, 7, IRON[0])
            elif side == 1:
                for k in range(14):
                    a = k / 13 * math.pi * 1.5
                    rr = 3.2 - k / 13 * 1.8
                    corner.set(round(7 + math.cos(a) * rr), round(7 + math.sin(a) * rr), BRASS[3])
                corner.hline(5, 11, 5, BRASS[4])
                corner.vline(5, 5, 11, BRASS[4])
                corner.set(5, 5, PALE)
            elif side == 3:
                # A corporate bracket and the sugar cube in its angle.
                corner.stamp(["oooooooo....", "ohhhhhho....", "oho.........", "oho..ccd....", "oho..cdd....",
                              "oho.........", "oho.........", "oo.........."], {"o": GOO[0], "h": OCHRE[4], "c": OCHRE[5],
                                                                               "d": OCHRE[2]}, 1, 1)
            else:
                corner.stamp(["....oooooo..", "....ohhhho..", "...........", "oo..........", "oh..........",
                              "oh..........", "............", "o..........."], {"o": SOOT_GOLD[0], "h": SOOT_GOLD[3]}, 1, 1)
                corner.set(6, 6, ASHEN[4])
            for y in range(12):
                for x in range(12):
                    c = corner.get(x, y)
                    if c[3]:
                        t.set(47 - x if fx else x, 47 - y if fy else y, c)
    return t


def page(side):
    """The parchment insert, a 32px nine-slice (corners 6)."""
    t = Tex(32, 32)
    lo, hi = PAGES[side]
    for y in range(32):
        for x in range(32):
            if _shape(0, x, y, 32, 32, 2):
                continue
            e = min(x, y, 31 - x, 31 - y)
            n = h01(x % 16, y % 16, 910 + side)
            c = mix(lo, hi, .45 + n * .4)
            if e == 0:
                c = mix(c, INK, .7)
            elif e == 1:
                c = mix(c, INK, .3 if (x < 16 and x <= y) or (y < 16 and y < x) else .12)
            elif e == 2 and (x == 2 or y == 2):
                c = mix(c, INK, .1)
            if n < .02:
                c = mix(c, INK, .12)
            t.set(x, y, c)
    return t


def tab(side, on):
    """A tab above the page: 32x16, caps 6. The open one is the page's own colour and has no bottom edge."""
    t = Tex(32, 16)
    lo, hi = PAGES[side]
    rim = RIMS[side]
    for y in range(16):
        for x in range(32):
            cx = min(x, 31 - x)
            if y < 4 and cx + y < 4:
                continue
            e = min(cx, y)
            n = h01(x % 20, y, 920 + side)
            c = mix(lo, hi, .45 + n * .4)
            if not on:
                c = mix(c, LEATHERS[side][1] if side != 1 else BRASS[2], .42)
            if e == 0 or (y < 4 and cx + y == 4):
                c = mix(c, INK, .75)
            elif e == 1 and y < 3:
                c = mix(c, rim[4] if side else EMBER, .5)
            if y == 15:
                c = mix(c, INK, .7) if not on else c
            t.set(x, y, c)
    return t


def close_button(on):
    t = Tex(12, 12)
    t.stamp([".oooooooooo.", "oddddddddddo", "odhoddddohdo", "oddhoddohddo", "odddhoohdddo", "oddddhhddddo",
             "oddddhhddddo", "odddhoohdddo", "oddhoddohddo", "odhoddddohdo", "oddddddddddo", ".oooooooooo."],
            {"o": IRON[0], "d": P.BLOOD[2] if on else IRON[1], "h": PALE if on else IRON[4]})
    return t


def action_button(side, state):
    """A message button on the page: 32x14, caps 4. state 0 off, 1 hover, 2 disabled."""
    t = Tex(32, 14)
    rim = RIMS[side]
    base = (P.BLOOD[2], IVORY[3], ASHEN[3], PETROL[3])[side]
    for y in range(14):
        for x in range(32):
            cx = min(x, 31 - x)
            cy = min(y, 13 - y)
            if cx + cy < 1:
                continue
            e = min(cx, cy)
            c = base
            if state == 1:
                c = mix(c, (EMBER, BRASS[5], ASHEN[5], PETROL[5])[side], .3)
            if y > 8:
                c = mix(c, (0, 0, 0, 255), .15)
            if e == 0:
                c = rim[0]
            elif e == 1:
                c = rim[4 if y < 7 else 2] if state != 1 else rim[5]
            if state == 2:
                g = sum(c[:3]) // 3
                c = mix(c, (g, g, g, 255), .7)
                c = mix(c, PAGES[side][0], .45)
            t.set(x, y, c)
    return t


def rule(side):
    t = Tex(64, 5)
    rim = RIMS[side]
    for x in range(64):
        t.set(x, 2, with_alpha(rim[3], .85))
        if x % 2 == 0:
            t.set(x, 3, with_alpha(rim[1], .5))
    for cx in (32,):
        t.stamp(["..o..", ".oho.", "ohdho", ".oho.", "..o.."], {"o": rim[0], "h": rim[4], "d": (EMBER, PALE, ASHEN[5], OCHRE[4])[side]}, cx - 2, 0)
    return t


def modal():
    t = Tex(256, 256)
    for side in range(3):
        t.paste(folio(side), side * 48, 0)
        t.paste(page(side), 144 + side * 32, 0)
        for on in (0, 1):
            t.paste(tab(side, on), (side * 2 + on) * 32, 48)
        for state in range(3):
            t.paste(action_button(side, state), state * 32, 80 + side * 14)
        t.paste(rule(side), 0, 124 + side * 6)
    for on in (0, 1):
        t.paste(close_button(on), 192 + on * 12, 48)
    pal = {"o": hexc("#1a1010"), "h": IVORY[5], "d": BRASS[3], "r": P.BLOOD[4]}
    for i, name in enumerate(("chronicle", "bonds", "duels", "actions", "front")):
        t.paste(_icon(name, pal), i * 10, 64)
    for i, kind in enumerate(("loyal", "rival", "grudge", "pact")):
        t.paste(_bond_icon(kind), 60 + i * 10, 64)
    for i, kind in enumerate(("pit", "host", "player")):
        t.paste(_marker(kind), 104 + i * 8, 64)
    return t


def dial():
    """The front's dial: a bronze ring r 57..62 round (64, 64), notches at the twelve sector edges, a hub of r 6."""
    t = Tex(128, 128)
    for y in range(128):
        for x in range(128):
            dx, dy = x + .5 - 64, y + .5 - 64
            d = math.hypot(dx, dy)
            light = (-dx - dy) / max(1, d)
            if 57 <= d <= 62.5:
                if d > 61.8 or d < 57.6:
                    c = BRASS[0]
                elif d > 60:
                    c = BRASS[4 if light > .3 else 3 if light > -.3 else 2]
                else:
                    c = BRASS[2 if light > 0 else 1]
                t.set(x, y, c)
            elif 62.5 < d <= 63.5:
                t.set(x, y, with_alpha(INK, .35))
            elif 54 < d < 57:
                over(t, x, y, INK, .18 * (1 - (57 - d) / 3))
            elif d <= 6.5:
                c = IRON[0] if d > 5.6 else IRON[3 if light > .2 else 1]
                if d < 2:
                    c = IRON[0]
                t.set(x, y, c)
    for i in range(12):
        a = i * math.pi / 6
        for r in range(52, 60):
            px, py = round(64 + math.cos(a) * r - .5), round(64 + math.sin(a) * r - .5)
            t.set(px, py, BRASS[4] if r >= 57 else with_alpha(INK, .45))
    # The four quarters: a small diamond on the ring.
    for a in (0, math.pi / 2, math.pi, 3 * math.pi / 2):
        cx, cy = round(64 + math.cos(a - math.pi / 2) * 60 - .5), round(64 + math.sin(a - math.pi / 2) * 60 - .5)
        t.stamp(["..o..", ".oho.", "ohdho", ".oho.", "..o.."], {"o": BRASS[0], "h": BRASS[5], "d": P.BLOOD[4] if a == 0 else BRASS[3]}, cx - 2, cy - 2)
    return t


def generate():
    save(widgets(), "gui/book", "chain_widgets")
    for host, name in ((False, "pit"), (True, "host")):
        save(paper(host), "gui/book", "chain_paper_" + name)
        save(frame(host), "gui/book", "chain_frame_" + name)
    save(modal(), "gui/book", "chain_modal")
    save(dial(), "gui/book", "chain_dial")


if __name__ == "__main__":
    generate()
