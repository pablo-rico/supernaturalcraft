"""The v0.41 items (seasons 12-14), 16x16 sprites, plus the AU Michael greatcoat's armour layer:

  british_pistol      a suppressed black pistol, side view, the long suppressor to the right
  enchanted_round     a brass round standing at a slant, a pale sigil carved into its jacket
  hex_grenade         a black canister, a band of pale teal sigils, a brass cap and lever (also its thrown look)
  british_dossier     a buff card folder, a red CLASSIFIED rule and the Order's blue aquarian star stamped on it
  michaels_orders     a folded grey field order, its flap sealed with gold wax pressed with a wing
  au_michael_greatcoat  the long dark military greatcoat, silver buttons and shoulder boards (sprite + armour layer)
  nephilim_grace      a stoppered vial of gold light with a dark core turning in it
  nephilim_blood      a stoppered vial of dark red blood, gold motes in it
  hess_folio          Dr. Hess's black leather folio, a gilt rule round its cover and the Order's gold aquarian star
  dagons_brand        Dagon's brand: an iron lozenge charm on a black cord, a red-gold hellfire sigil burning in it

(The trophies live in each boss's module; michael_lance_bane and rift_key are not used.)
"""

import math

import palette as P
from common import art, save
from pixelkit import Ramp, Tex, hexc, item_outline, mix, shade

O = P.OUTLINE
GUN = Ramp("#0a0b0c", "#16181b", "#23262a", "#30343a", "#41464e", "#5e646e")
BRASS = Ramp("#3b2a10", "#5e4519", "#836226", "#a98236", "#cba354", "#e9c880")
TEAL = Ramp("#0e3a38", "#16585a", "#26807e", "#4aaaa4", "#7fd0c8", "#c4f4ee")
BUFF = Ramp("#5e4a2c", "#86704a", "#a8926a", "#c6b08a", "#ddcaa4", "#efe2c4")
STAMP = Ramp("#0e1e4a", "#18307a", "#2848a8", "#4068c8", "#6a8ee0", "#a0bcf4")
RED = Ramp("#3a0408", "#5e0810", "#8a1018", "#b01c22", "#d43a36", "#f06a5a")
GREY = Ramp("#2e3034", "#45484e", "#5e6168", "#787b82", "#93969c", "#b2b5ba")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
COAT = Ramp("#0a0c0e", "#121518", "#1a1e22", "#23282d", "#2e343a", "#3c434b")
SILVER = Ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")
GLASS = (hexc("#b8d4e0", 170), hexc("#e8f4fa", 210), hexc("#7a98a8", 200))
GRACE = Ramp("#5a3c08", "#9c7418", "#d4a830", "#f2d260", "#fff0a8", "#ffffff")
BLOOD = Ramp("#1f0204", "#3d0509", "#5c0a10", "#7e1218", "#a01d22", "#c4352f")
BANE = Ramp("#4a5a6e", "#6e84a0", "#96acc6", "#bcd0e6", "#dceaf8", "#f6fbff")
CORK = Ramp("#2a1a0e", "#432b17", "#5c3d22", "#76512e", "#8f673d", "#a97e4f")


def pistol():
    pal = {"o": O, "d": GUN[1], "m": GUN[2], "l": GUN[3], "L": GUN[4], "h": GUN[5], "s": SILVER[3]}
    return art([
        "................",
        "................",
        "................",
        "................",
        "...ooooooooooooo",
        "..ohLLLLLlmmmmmo",
        "..olllllldmlmlmo",
        "..ommmmmmdmmmmmo",
        "..oddommmoooooo.",
        "..oddoodo.......",
        "..olmo.o........",
        "..olmo..........",
        ".olmmo..........",
        ".oldmo..........",
        ".ooooo..........",
        "................",
    ], pal)


def round_():
    t = Tex(16, 16, 4101)
    # A round on a slant: from its base (4, 13) to its point (11, 3).
    ax0, ay0, ax1, ay1 = 4.5, 13.5, 11.5, 2.5
    L = math.hypot(ax1 - ax0, ay1 - ay0)
    ux, uy = (ax1 - ax0) / L, (ay1 - ay0) / L
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - ax0, y + 0.5 - ay0
            a = px * ux + py * uy
            c_ = -px * uy + py * ux
            u = a / L
            if not 0 <= u <= 1:
                continue
            half = 2.2 if u < 0.62 else 2.2 * math.sqrt(max(0.0, 1 - ((u - 0.62) / 0.38) ** 2))
            if abs(c_) > half:
                continue
            lit = c_ < -0.6
            if u < 0.12:
                col = BRASS[2] if not lit else BRASS[3]           # the rim of the case
            elif u < 0.62:
                col = BRASS[4] if lit else (BRASS[3] if c_ < 0.6 else BRASS[2])
            else:
                col = mix(BRASS[4] if lit else BRASS[3], hexc("#b07040"), 0.35)   # the copper head
            t.set(x, y, col)
    # The carved sigil on the case, glowing faintly.
    for x, y in ((6, 10), (7, 9), (7, 10), (8, 9), (6, 8), (8, 11), (7, 8)):
        t.set(x, y, TEAL[4] if (x + y) % 2 else TEAL[3])
    item_outline(t, hexc("#2a1a08"))
    t.set(12, 2, BRASS[5])
    return t


def grenade():
    pal = {"o": O, "d": GUN[1], "m": GUN[2], "l": GUN[3], "h": GUN[5], "t": TEAL[3], "T": TEAL[5], "k": TEAL[1],
           "b": BRASS[2], "B": BRASS[4], "c": BRASS[3]}
    return art([
        "................",
        "......ooooo.....",
        ".....obBBcbo....",
        ".....obbcbbooo..",
        "....oooooooocbo.",
        "...ohlllmmmdo.bo",
        "...ohllmmmmdo.bo",
        "...ohlmmmmddo.o.",
        "...otTktTktko...",
        "...okTttkTttko..",
        "...ohlmmmmddo...",
        "...ohlmmmmddo...",
        "...olmmmmddddo..",
        "....olmmmddo....",
        ".....oooooo.....",
        "................",
    ], pal)


def dossier():
    pal = {"o": O, "P": BUFF[5], "p": BUFF[4], "q": BUFF[3], "r": BUFF[2], "d": BUFF[1], "R": RED[3], "s": STAMP[3], "S": STAMP[2],
           "w": hexc("#f4efe2"), "i": hexc("#3a3024")}
    return art([
        "................",
        "..oooooo........",
        ".oqqqqqqoooooooo",
        ".oppppppppppppqo",
        ".oPpppppppppppqo",
        ".oppRRRRRRRpppqo",
        ".opppppppppppqro",
        ".oppppppsppppqqo",
        ".opppppsSsppppqo",
        ".oppppsSpSspppro",
        ".opppppsSsppppqo",
        ".oppppppsppppqqo",
        ".opiipiiippppqro",
        ".oppppppppppqqro",
        ".oqqrqqrqqrrrrdo",
        ".oooooooooooooo.",
    ], pal)


def orders():
    pal = {"o": O, "P": GREY[5], "p": GREY[4], "q": GREY[3], "r": GREY[2], "d": GREY[1], "i": hexc("#202226"),
           "g": GOLD[2], "G": GOLD[4], "h": GOLD[5], "k": GOLD[1]}
    return art([
        "................",
        "................",
        "..oooooooooooo..",
        "..oPpppppppppqo.",
        "..opiipiiipiqqo.",
        "..oppppppppppqo.",
        "..opiiipiipiqqo.",
        "..oqqqqqqqqqqro.",
        "..orrqqqqqqqrdo.",
        "...orrqqgGGgro..",
        "....orrgGhGgo...",
        ".....orgGkGgo...",
        "......oogggo....",
        "........ooo.....",
        "................",
        "................",
    ], pal)


def greatcoat():
    pal = {"o": O, "L": COAT[5], "l": COAT[4], "t": COAT[3], "m": COAT[2], "d": COAT[1], "D": COAT[0], "s": SILVER[4], "S": SILVER[2],
           "a": hexc("#0b0b0d"), "A": hexc("#1c1c20")}
    return art([
        "...ooooooooo....",
        "..osslo.olsso...",
        ".oLltlaAaltlLo..",
        "oLltttaAattttlo.",
        "olttstaAastttmo.",
        "oltmttaAattmtmo.",
        "otmmstaAastmmdo.",
        "otmmttaAattmmdo.",
        "otmmstaaastmmdo.",
        ".odmtmtaatmtmdo.",
        "..ottmtaatmtto..",
        "..otmmsaasmmto..",
        "..otmmtaatmmdo..",
        "..otmmtoatmddo..",
        "..oddDo.oDdddo..",
        "..ooooo.ooooo...",
    ], pal)


def vial(liquid, core=None, motes=None, seed=0):
    """A stoppered glass vial standing a little slanted: the cork, the neck, the round of the body full of `liquid(x, y)`."""
    t = Tex(16, 16, seed)
    cx, cy, r = 8.0, 10.0, 4.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                c = liquid(x, y, dx / r, dy / r)
                if core and core(dx / r, dy / r):
                    c = core(dx / r, dy / r)
                if d > r - 0.9:
                    c = mix(c, GLASS[2], 0.5)
                if dx < -1.5 and dy < -0.5 and d > r - 2.0:
                    c = mix(c, GLASS[1], 0.65)       # the highlight on the glass
                t.set(x, y, c)
    for y in range(3, 6):
        for x in range(6, 10):
            t.set(x, y, GLASS[0] if x in (6, 9) else GLASS[1])
    for y in range(1, 3):
        for x in range(6, 10):
            t.set(x, y, CORK[3] if x < 8 else CORK[2])
    t.set(7, 1, CORK[4])
    for (x, y, c) in motes or ():
        t.set(x, y, c)
    item_outline(t, hexc("#1a1612"))
    return t


def grace():
    def liquid(x, y, u, v):
        k = 1 - math.hypot(u, v)
        return GRACE[2 + 3 * k + 0.6 * math.sin(x * 1.3 + y)]

    def core(u, v):
        d = math.hypot(u + 0.1, v - 0.1)
        if d < 0.32:
            return hexc("#1a1208") if d < 0.2 else hexc("#3a2a10")   # the dark heart of it
        return None
    return vial(liquid, core, motes=((3, 5, GRACE[5]), (13, 7, GRACE[4]), (12, 3, GRACE[5])), seed=4111)


def blood():
    def liquid(x, y, u, v):
        return BLOOD[2.6 - v * 1.2 + 0.4 * math.sin(x * 1.1 + y * 0.7)]
    motes = [(6, 9, GOLD[5]), (9, 11, GOLD[4]), (8, 8, GOLD[4]), (10, 9, GOLD[5]), (7, 12, GOLD[3])]
    return vial(liquid, motes=motes, seed=4112)


def bane_shard():
    t = Tex(16, 16, 4113)
    # A broken leaf blade along the diagonal, the point at (13, 2), snapped off at the base (3, 13) in a jagged break.
    ax0, ay0, ax1, ay1 = 3.0, 13.5, 13.0, 2.0
    L = math.hypot(ax1 - ax0, ay1 - ay0)
    ux, uy = (ax1 - ax0) / L, (ay1 - ay0) / L
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - ax0, y + 0.5 - ay0
            a = px * ux + py * uy
            c_ = -px * uy + py * ux
            u = a / L
            jag = 0.08 * ((x * 7 + y * 3) % 3)
            if not jag <= u <= 1:
                continue
            half = 2.6 * min(1.0, (1 - u) / 0.55) ** 0.8
            if abs(c_) > half:
                continue
            q = abs(c_) / max(0.3, half)
            if q > 0.75:
                col = BANE[5]                                   # the bright edge
            elif c_ < 0:
                col = BANE[4] if q < 0.3 else BANE[3]
            else:
                col = BANE[2] if q < 0.3 else BANE[1]
            if abs(c_) < 0.5 and u > 0.1:
                col = BANE[4]                                   # the ridge
            t.set(x, y, col)
    item_outline(t, hexc("#1a2638"))
    for x, y in ((14, 1), (2, 10), (12, 8)):
        t.set(x, y, BANE[5])
    return t


def hess_folio():
    """Dr. Hess's folio: black leather, a gilt rule round the cover, the Order's gold aquarian star in the middle, a page edge."""
    lea = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3e3e46")
    pal = {"o": O, "k": lea[1], "m": lea[2], "l": lea[3], "L": lea[4], "g": GOLD[2], "G": GOLD[4], "h": GOLD[5], "p": BUFF[4],
           "q": BUFF[3], "b": STAMP[3]}
    return art([
        "................",
        "..oooooooooooo..",
        "..oLllllllllmoo.",
        "..olggggggggmopo",
        "..olgmmmmmmgmopo",
        "..olgmmmGmmgkopo",
        "..olgmmGhGmgkopo",
        "..olgmGhbhGgkopo",
        "..olgmmGhGmgkopo",
        "..olgmmmGmmgkopo",
        "..olgmmmmmmgkopo",
        "..olggggggggkopo",
        "..omkkkkkkkkkoqo",
        "..ooooooooooooqo",
        "...oqqqqqqqqqqo.",
        "....oooooooooo..",
    ], pal)


def dagons_brand():
    """Dagon's brand: an iron charm on a short black cord, a red-gold hellfire sigil burning in its face."""
    t = Tex(16, 16, 4130)
    fire = Ramp("#3b0602", "#7a1404", "#c02d06", "#ec5a0c", "#ff9a26", "#ffd96a")
    iron = Ramp("#0b0b0c", "#161618", "#222225", "#2f2f33", "#414146", "#5c5c63")
    cx, cy = 8.0, 9.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = abs(dx) + abs(dy) * 0.9                 # a lozenge of iron
            if d <= 6.0:
                c = iron[3.4 - 0.25 * d + (0.8 if dx + dy < -2 else 0)]
                if d > 5.2:
                    c = iron[1]
                t.set(x, y, c)
    # The sigil: a flame over a crossbar and a ring, burning red-gold.
    sig = [(8, 6), (7, 7), (8, 7), (9, 7), (7, 8), (9, 8), (8, 8), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (8, 9), (8, 11),
           (7, 12), (9, 12), (8, 13)]
    for i, (x, y) in enumerate(sig):
        t.set(x, y, fire[5] if (x == 8 and y in (7, 10)) else (fire[4] if i % 2 else fire[3]))
    for x, y in ((6, 9), (10, 9), (6, 11), (10, 11)):
        t.set(x, y, fire[2])
    # The cord, up to a loop.
    for x, y in ((8, 3), (7, 2), (8, 1), (9, 2), (8, 4)):
        t.set(x, y, hexc("#2a1a12"))
    item_outline(t, hexc("#0a0606"))
    t.set(12, 6, fire[5])
    t.set(3, 12, fire[4])
    return t


# --- the greatcoat worn (vanilla humanoid armour layer, 64x32) --------------------------------------------------

def box_faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def fill(t, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                t.set(x0 + x, y0 + y, c)


def wool(x, y, base=3.0):
    v = base + 0.5 * math.sin(x * 1.1 + 0.4) - (0.4 if (x * 3 + y) % 5 == 0 else 0)
    return COAT[max(0, min(5, int(round(v))))]


def greatcoat_layer():
    t = Tex(64, 32, 4120)
    body = box_faces(16, 16, 8, 12, 4)

    def front(x, y, w, h):
        if 3 <= x <= 4 and y < 11:
            return hexc("#0b0b0d") if y % 3 else hexc("#1c1c20")      # the black armour in the opening
        if y <= 3 and x in (2, 5):
            return COAT[5]                                           # the lapels
        if x in (1, 6) and y in (2, 5, 8):
            return SILVER[4]                                         # silver buttons
        if y == 11:
            return COAT[1]
        return wool(x, y, 3.0 if x not in (0, 7) else 2.6)
    fill(t, body["front"], front)
    fill(t, body["back"], lambda x, y, w, h: COAT[1] if (x in (3, 4) and y > 7) else wool(x, y, 2.8))
    for side in ("right", "left"):
        fill(t, body[side], lambda x, y, w, h: wool(x, y, 2.6))
    fill(t, body["top"], lambda x, y, w, h: SILVER[3] if y in (0, 3) and x in (0, 1, 6, 7) else COAT[4])
    fill(t, body["bottom"], lambda x, y, w, h: COAT[1])
    arm = box_faces(40, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        def sleeve(x, y, w, h, face=face):
            if y == 0:
                return SILVER[4] if face in ("right", "left") else COAT[4]   # the shoulder board's edge
            if y in (9, 10):
                return SILVER[2] if y == 9 else COAT[3]
            return wool(x, y, 2.9 if face != "left" else 2.5)
        fill(t, arm[face], sleeve)
    fill(t, arm["top"], lambda x, y, w, h: SILVER[4] if (x + y) % 2 else SILVER[2])
    fill(t, arm["bottom"], lambda x, y, w, h: COAT[1])
    return t


ITEMS = {"british_pistol": pistol, "enchanted_round": round_, "hex_grenade": grenade, "british_dossier": dossier,
         "michaels_orders": orders, "au_michael_greatcoat": greatcoat, "nephilim_grace": grace, "nephilim_blood": blood,
         "hess_folio": hess_folio, "dagons_brand": dagons_brand}


def generate():
    for name, fn in ITEMS.items():
        save(fn(), "item", name)
    save(greatcoat_layer(), "models/armor", "au_michael_greatcoat_layer_1")


if __name__ == "__main__":
    generate()
