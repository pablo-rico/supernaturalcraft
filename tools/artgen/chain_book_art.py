"""Chain of Command book art. Sprite coordinates must match client/book/chain/ChainAtlas.java.

Pure stdlib, fixed seeds, pixel outlines and single-pixel bevels. Frame sheets are 384x248.
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
FRAME_W, FRAME_H = 384, 248


def _stud(t, x, y, ramp):
    t.rect(x - 1, y - 1, x + 1, y + 1, ramp[0])
    t.set(x - 1, y - 1, ramp[5])
    t.set(x, y, ramp[4])
    t.set(x + 1, y, ramp[2])


def medallion(faction, rank):
    t = Tex(36, 36)
    ramp = (IRON, BRASS, SOOT_GOLD)[faction]
    # Side ornaments are behind the rim and never enter the model window.
    if rank >= 2:
        for flip in (False, True):
            wing = ([".....o", "....oh", "...ohh", "..ohhh", ".ohhho", "ohhho.", "ohho..", "oho...", "ohho..", ".oho..", "..oo..", "...o.."]
                    if faction else
                    ["o.....", "oh....", "ohh...", ".ohh..", ".ohhho", "..ohhh", "..ohho", "...oho", "....oo", "......", "......", "......"])
            ornament = Tex(6, 12)
            ornament.stamp(wing, {"o": ramp[0], "h": ramp[4]})
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
        t.stamp(chevron, {"o": ramp[0], "h": ramp[4]}, 13, 0)
        t.set(17, 2, P.BLOOD[5] if faction == 0 else PALE)
    if rank >= 4:
        crown = ["o....o....o", "ho..oho..oh", "hhoohhhoohh", "ohhhhhhhho.", ".ooooooooo.", ".hhhhhhhhh."]
        t.stamp(crown, {"o": ramp[0], "h": ramp[2] if faction == 0 else ramp[4]}, 12, 0)
        t.set(17, 2, P.BLOOD[5] if faction == 0 else PALE)
        t.set(17, 3, P.BLOOD[3] if faction == 0 else ramp[5])
        if faction == 0:
            for x in (14, 20):
                t.set(x, 3, IRON[1])
    return t


def backdrop(faction):
    """Opaque model window, placed five pixels inside the 36px frame."""
    t = Tex(26, 26)
    low, high = (("#150e16", "#522521"), ("#111c33", "#334563"), ("#24212e", "#514957"))[faction]
    for y in range(26):
        for x in range(26):
            dx, dy = x + .5 - 13, y + .5 - 13
            d = math.hypot(dx, dy)
            if d <= 13:
                c = mix(hexc(low), hexc(high), (dy + 13) / 26 * .8)
                if faction == 1 and d > 10.5:
                    c = mix(c, BRASS[3], .18)
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


def generate():
    save(widgets(), "gui/book", "chain_widgets")
    for host, name in ((False, "pit"), (True, "host")):
        save(paper(host), "gui/book", "chain_paper_" + name)
        save(frame(host), "gui/book", "chain_frame_" + name)
