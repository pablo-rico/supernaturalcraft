"""The Leviathan officer skins (Chain of Command, the SucroCorp court): an executive, a security guard and a chemist.

Only textures: each is painted on the plain Leviathan's rig (`leviathan_art.rig(look)` / `head_rig(look)`), so it shares
its geo, its UV layout and its animations (the split maw included). Every look has its own face (skin tone, hair, eyes)
and its own clothes; the maw's gums, teeth and tongue stay the base's.

Outputs textures/entity/leviathan_<skin>.png and leviathan_<skin>_head.png for skin in executive, guard, chemist.
"""

import horsemen_art as H
import leviathan_art as L
from chuck_art import h01, none_on
from common import save
from pixelkit import Ramp, fbm, hexc, mix, shade

GOO, TOOTH, tone, cloth, veined = L.GOO, L.TOOTH, L.tone, L.cloth, L.veined

# SucroCorp's colours: ochre-yellow on deep petrol teal.
OCHRE = Ramp("#2a1d05", "#5a3f0c", "#8c6414", "#b98a1e", "#ddb03a", "#f5d77a")
PETROL = Ramp("#061210", "#0d2522", "#163834", "#214d47", "#30665e", "#4a8479")
WHITE_SHIRT = Ramp("#8e959e", "#aab0b8", "#c3c8ce", "#d7dbe0", "#e8ebee", "#f7f8fa")
BLACK = Ramp("#050506", "#0c0c0f", "#151519", "#202026", "#2e2e36", "#4a4a56")
STEEL = Ramp("#2c3036", "#4a5058", "#6c737c", "#8f969e", "#b4bac0", "#dde1e4")


def badge(c_bg, x, y, x0, y0):
    """The SucroCorp mark, 3x3 at (x0, y0): an ochre sugar cube with a petrol heart (or None outside it)."""
    dx, dy = x - x0, y - y0
    if not (0 <= dx < 3 and 0 <= dy < 3):
        return None
    return {"h": OCHRE[5], "o": OCHRE[4], "P": PETROL[2], "d": OCHRE[2]}[("hoo", "oPo", "ood")[dy][dx]]


class Look:
    def __init__(self, name, face, skin, hair, eyes, extra, mats_fn, seed, hair_mat=None, wrap=None):
        self.name, self.face, self.skin, self.hair, self.eyes, self.extra = name, face, skin, hair, eyes, extra
        self.seed, self.hair_mat, self.wrap = seed, hair_mat, wrap
        self.mats = mats_fn(self)

    def sk(self, seed, amount=0.012):
        return veined(H.skin_mat(self.skin, seed, 3.0, 0.7), seed + 1, amount)

    def legend(self):
        sk = self.sk(self.seed)
        hair = self.hair_mat(self) if self.hair_mat else H.skin_mat(self.hair, self.seed + 2, 2.6, 1.2)
        white, iris = self.eyes
        leg = {"s": sk, "h": hair, "H": self.hair[4], "b": self.hair[1], "w": white, "E": iris, "u": self.skin[2],
               "n": self.skin[4], "N": self.skin[1], "m": hexc("#2e2526"), "v": GOO[2],
               "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.85)}
        leg.update({k: (v(self, sk) if callable(v) and getattr(v, "wants_look", False) else v) for k, v in self.extra.items()})
        if self.wrap:
            sk, hair = self.wrap(sk), self.wrap(hair)
        return sk, hair, leg


def needs_look(f):
    f.wants_look = True
    return f


def face_ok(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return rows


def skin_parts(look):
    """Neck, hands and fingers in the look's own grey."""
    sk = look.sk(look.seed + 100, 0.015)
    return {"neck": lambda s: sk, "hand": lambda s: sk,
            "fingers": lambda s: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9 if x % 2 else 1.0))}


def stained(base, seed, amount=0.3):
    """Black goo splashed on a material, dribbling down in runs."""
    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        if c is None:
            return None
        n = fbm(x * 2 + len(face) * 13, y, seed, 48, 48, 3, 6.0)
        run = h01(seed, face, x) < 0.22 and fbm(x * 2 + len(face) * 13, max(0, y - 3), seed, 48, 48, 3, 6.0) > 1 - amount
        if n > 1 - amount or (run and face not in ("up", "down")):
            return GOO[1] if n > 1 - amount * 0.6 else GOO[3]
        if n > 1 - amount * 1.25:
            return mix(c, GOO[3], 0.45)
        return c
    return m


# =====================================================================================================
# The executive: charcoal-navy suit, white shirt, SucroCorp ochre tie and lapel pin, polished shoes
# =====================================================================================================

SUIT = Ramp("#0c0f16", "#141a24", "#1d2431", "#262f3f", "#323d51", "#445168")
EXEC_FACE = face_ok([
    "hhhhhhhhhhhhhhhh",
    "hhHHHhhhhhhhhhhh",
    "GhhhhHHHhhhhhhhG",
    "Gsssshhhhhhhhhss",
    "ssssssssssssssss",
    "sbbbbbsvsssbbbbs",
    "ssssssssvsssssss",
    "swEEwwssvsswwEEs",
    "ssuuussvsssuuuss",
    "sssssssnnsssssss",
    "sssssssnnsssvsss",
    "ssssssNnnNssssss",
    "ssssssssssssssss",
    "sssssmmmmmmMssss",
    "jssssssssssssssj",
    "jjssssssssssssjj",
])


@needs_look
def exec_hair(look, sk=None):
    """Slicked back: combed streaks, grey at the temples."""
    def m(face, x, y, w, h):
        n = fbm(x, y * 3 + len(face), look.seed + 5, 48, 48, 2, 6.0)
        c = tone(look.hair, 2.2 + (n - 0.5) * 1.4 + (0.9 if (x + (y // 3)) % 3 == 0 else 0))
        if face in ("east", "west") and 4 <= y <= 11 and h01(look.seed, face, x, y) < 0.45:
            c = mix(c, hexc("#8d9198"), 0.6)        # salt and pepper at the temples
        return c
    return m


def exec_mats(look):
    suit = cloth(SUIT, 41110, 2.7)
    shirt = cloth(WHITE_SHIRT, 41111, 3.6)
    sk = look.sk(look.seed + 100, 0.015)

    def torso(face, x, y, w, h):
        c = suit(face, x, y, w, h)
        if face == "south":
            if abs(x - (w - 1) / 2) < 0.6 and y >= h - 6:
                return SUIT[0]                     # the vent
            return c
        if face != "north":
            return c
        mid = (w - 1) / 2
        dx = abs(x - mid)
        open_w = 3.6 - y * 0.24                    # the jacket's V, closing at the button
        if y < 1 and dx <= 1.5:
            return sk(face, x, y, w, h)
        if y <= 13 and dx <= open_w:
            if dx < 1:                             # the tie: knot, then the blade with a thin stripe
                if y <= 2:
                    return OCHRE[4] if y == 1 else OCHRE[3]
                return OCHRE[2] if (y + x) % 4 == 0 else OCHRE[3 if x < mid else 4]
            return shirt(face, x, y, w, h)
        if y <= 13 and dx <= open_w + 1.1:
            return SUIT[4] if y > 3 else SUIT[5]   # the lapel's edge
        if y == 4 and dx <= open_w + 2.2:
            return SUIT[0]                         # the notch
        if (x, y) in ((11, 7), (12, 7), (11, 8), (12, 8)):
            return (OCHRE[5], OCHRE[3], OCHRE[3], OCHRE[1])[(x - 11) + (y - 7) * 2]   # the SucroCorp lapel pin
        if dx < 0.6 and y in (14, 17):
            return SUIT[0]                         # buttons
        if dx < 0.6 and y > 14:
            return SUIT[1]                         # the closure
        if y == 16 and 2 <= abs(x - mid) <= 5.5:
            return SUIT[1]                         # flap pockets
        if x in (2, 3, 4) and y == 6:
            return SUIT[1] if x != 2 else WHITE_SHIRT[5]   # the breast pocket and its square
        if y == h - 1:
            return SUIT[0]
        return c

    def forearm(face, x, y, w, h):
        if y >= h - 2:
            if face in ("east", "west") and y == h - 2 and x == w // 2:
                return OCHRE[4]                    # a cufflink
            return WHITE_SHIRT[4] if y == h - 2 else WHITE_SHIRT[2]
        return suit(face, x, y, w, h)

    def trousers(s):
        tr = cloth(SUIT, 41140 + s, 2.6)

        def m(face, x, y, w, h):
            c = tr(face, x, y, w, h)
            if face == "north" and abs(x - (w - 1) / 2) < 0.6:
                return SUIT[3]                     # the crease
            return c
        return m

    def shoe(face, x, y, w, h):
        if face != "up" and y >= h - 1:
            return BLACK[0]
        c = BLACK[2]
        if face == "up" or (face == "north" and y == 1) or (face in ("east", "west") and y == 1 and x > w // 2):
            c = BLACK[5] if (x + y) % 3 == 0 else BLACK[4]     # the polish
        return c

    mats = skin_parts(look)
    mats.update({
        "torso": lambda s: torso,
        "buckle": lambda s: lambda f, x, y, w, h: SUIT[0] if (f == "north" and x == 2) else SUIT[2],
        "collar": lambda s: lambda f, x, y, w, h: WHITE_SHIRT[5] if y == 0 else WHITE_SHIRT[4],
        "sleeve": lambda s: cloth(SUIT, 41120 + s, 2.7),
        "roll": lambda s: none_on(("up",), cloth(SUIT, 41125 + s, 2.6)),
        "forearm": lambda s: forearm,
        "thigh": trousers, "shin": trousers,
        "boot": lambda s: shoe,
    })
    return mats


# =====================================================================================================
# The guard: dark uniform, tactical vest with the SucroCorp badge, duty belt with radio and baton, cargo trousers
# =====================================================================================================

UNIFORM = Ramp("#0a0d12", "#11161e", "#1a212b", "#232c38", "#2f3a48", "#425062")
VEST = Ramp("#0b0c0a", "#141612", "#1e211b", "#292d25", "#363b31", "#4a5043")
CARGO = Ramp("#121314", "#1c1e20", "#27292c", "#33363a", "#41454a", "#555a60")
GUARD_FACE = face_ok([
    "kkkkkkkkkkkkkkkk",
    "kkkkkkkkkkkkkkkk",
    "kkkkkkkkkkkkkkkk",
    "sskkssssssssskss",
    "ssssssssssssssss",
    "bbbbbbsvssbbbbbb",
    "ssssssssvsssssss",
    "swEEwsssvsswEEws",
    "suuuusssvsssuuus",
    "sssssssnnsssssss",
    "sssCsssnnsssssss",
    "ssssCNNnnNNsssss",
    "ssssssssssssssss",
    "sssmmmmmmmmmmsss",
    "jkkkkkkkkkkkkkkj",
    "jjkkkkkkkkkkkkjj",
])


@needs_look
def stubble(look, sk):
    """A buzz cut / a day's stubble: the skin speckled with dark hair."""
    def m(face, x, y, w, h):
        c = sk(face, x, y, w, h)
        return mix(c, look.hair[1], 0.5 + 0.2 * h01(look.seed, face, x, y))
    return m


def guard_mats(look):
    shirt = cloth(UNIFORM, 42110, 2.8)
    vest = cloth(VEST, 42111, 2.8)
    belt = lambda y: BLACK[2] if y % 2 else BLACK[1]

    def torso(face, x, y, w, h):
        c = shirt(face, x, y, w, h)
        mid = (w - 1) / 2
        if face == "up":
            return vest(face, x, y, w, h) if 2 <= x <= 4 or w - 5 <= x <= w - 3 else c   # the shoulder straps
        if face == "down":
            return c
        if 19 <= y <= 21:                          # the duty belt
            if face == "north" and abs(x - mid) < 2:
                return STEEL[3]
            if face == "west" and 1 <= x <= 4:     # the radio on the belt, its antenna up past the vest
                return OCHRE[4] if (x, y) == (3, 19) else BLACK[3]
            return belt(y) if y != 19 else BLACK[3]
        if face == "west" and 1 <= x <= 4 and 15 <= y <= 18:
            return BLACK[3] if x != 1 else BLACK[1]
        if face == "west" and x == 2 and 11 <= y <= 14:
            return BLACK[1]
        if face == "east" and 3 <= x <= 4 and y >= 15:
            return BLACK[3] if x == 3 else BLACK[1]  # the baton, hanging from the belt
        if y >= 22:
            return tone(CARGO, 2.6)
        if face == "north":
            if y < 2 and abs(x - mid) <= 1.5 - y:
                return look.sk(look.seed + 100)(face, x, y, w, h)
            if 2 <= y <= 18 and 1 <= x <= w - 2:
                v = vest(face, x, y, w, h)
                if x in (1, w - 2) or y == 18:
                    return VEST[0]
                b = badge(v, x, y, 10, 4)          # the SucroCorp badge over the heart
                if b is not None:
                    return b
                if y == 4 and 2 <= x <= 6:
                    return STEEL[4] if x % 2 == 0 else STEEL[2]     # "SECURITY" on the name tape
                if y in (9, 11):
                    return VEST[1]                 # webbing
                if 13 <= y <= 17 and (2 <= x <= 4 or 6 <= x <= 9 or 11 <= x <= 13):
                    if x in (5, 10):
                        return VEST[0]
                    return VEST[4] if y == 13 else VEST[3] if y == 14 else v   # pouches and their flaps
                return v
            if y < 2 and (2 <= x <= 4 or w - 5 <= x <= w - 3):
                return vest(face, x, y, w, h)
            return c
        if face == "south":
            if 2 <= y <= 18 and 1 <= x <= w - 2:
                v = vest(face, x, y, w, h)
                return VEST[0] if x in (1, w - 2) or y == 18 else (VEST[1] if y in (6, 9, 12) else v)
            if y < 2 and (2 <= x <= 4 or w - 5 <= x <= w - 3):
                return vest(face, x, y, w, h)
            return c
        if 4 <= y <= 18:                           # the cummerbund on the sides
            return VEST[2] if y not in (8, 12) else VEST[0]
        return c

    def sleeve(s):
        sl = cloth(UNIFORM, 42120 + s, 2.8)

        def m(face, x, y, w, h):
            if face in ("east", "west"):
                b = badge(sl(face, x, y, w, h), x, y, 2, 3)
                if b is not None:
                    return b
                if 2 <= x <= 6 and 2 <= y <= 6 and (x in (2, 6) or y in (2, 6)):
                    return OCHRE[2]                # the shoulder patch's border
                if 3 <= x <= 5 and 3 <= y <= 5:
                    return PETROL[3]
            return sl(face, x, y, w, h)
        return m

    def cargo(s):
        tr = cloth(CARGO, 42140 + s, 2.6)

        def m(face, x, y, w, h):
            c = tr(face, x, y, w, h)
            if face in ("east", "west") and 1 <= x <= 6 and 3 <= y <= 9:   # the cargo pocket
                if y == 3:
                    return CARGO[4]
                if x in (1, 6) or y == 9:
                    return CARGO[1]
                return shade(c, 1.08)
            return c
        return m

    def shin(s):
        tr = cloth(CARGO, 42150 + s, 2.6)

        def m(face, x, y, w, h):
            if y >= 4:                             # the boots come up the shin
                if face == "north" and y % 2 == 0 and 2 <= x <= 5:
                    return BLACK[4]                # laces
                return BLACK[2] if y > 4 else BLACK[3]
            return tr(face, x, y, w, h)
        return m

    def boot(face, x, y, w, h):
        if face != "up" and y >= h - 2:
            return BLACK[0] if y == h - 1 else BLACK[1]    # the thick sole
        if face == "north" and y % 2 == 0 and 2 <= x <= 5:
            return BLACK[4]
        return BLACK[2] if (x + y) % 5 else BLACK[3]

    mats = skin_parts(look)
    mats.update({
        "torso": lambda s: torso,
        "buckle": lambda s: lambda f, x, y, w, h: STEEL[4] if f == "north" and y == 0 else STEEL[2],
        "collar": lambda s: lambda f, x, y, w, h: UNIFORM[4] if y == 0 else UNIFORM[3],
        "sleeve": sleeve,
        "roll": lambda s: none_on(("up",), lambda f, x, y, w, h: UNIFORM[4] if y == 0 else UNIFORM[2]),
        "forearm": lambda s: look.sk(look.seed + 130 + s, 0.014),
        "thigh": cargo, "shin": shin,
        "boot": lambda s: boot,
    })
    return mats


# =====================================================================================================
# The chemist: goo-stained white lab coat, safety goggles pushed up on the forehead, nitrile gloves, a name tag
# =====================================================================================================

COAT = Ramp("#878e93", "#a6adb1", "#bfc5c8", "#d3d8da", "#e5e9ea", "#f5f7f6")
BLUE_SHIRT = Ramp("#4f6170", "#677b8b", "#8094a3", "#99abb8", "#b2c2cd", "#cad7df")
GLOVE = Ramp("#14303d", "#1d4455", "#285a6e", "#357287", "#4a8ba0", "#6aa7ba")
SLACKS = Ramp("#1e1b18", "#2c2824", "#3b3631", "#4a443e", "#5b544c", "#6e665d")
GOGGLE_FRAME, LENS, LENS_HI, STRAP = hexc("#24383a"), hexc("#5e9a9c"), hexc("#bfe6e4"), hexc("#141618")
CHEM_FACE = face_ok([
    "hhHhhhhhHhhhhhHh",
    "hhhhhHhhhhhHhhhh",
    "HhhhhhhhhhhhhhhH",
    "tggggggvvggggggt",
    "tgllLlgvvglLllgt",
    "tggggggvvggggggt",
    "shbbbsssvssbbbhs",
    "swEEwsssvsswEEws",
    "sdddssssvssssdds",
    "sssssssnnsssssss",
    "ssvssssnnsssssss",
    "sssssNNnnNNsssvs",
    "ssssssssssssssss",
    "ssssssmmmmssssss",
    "jssssssssssssssj",
    "jjsssssssssssjjj",
])


@needs_look
def chem_hair(look, sk=None):
    """Messy ginger hair, tufts sticking up."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face) * 5, y * 2, look.seed + 7, 48, 48, 3, 7.0)
        c = tone(look.hair, 2.5 + (n - 0.5) * 2.4)
        if h01(look.seed, face, x, y) < 0.05:
            c = look.hair[5]
        return c
    return m


def goggle_strap(mat):
    """The goggles' strap round the sides and back of the head, level with the lenses."""
    def m(face, x, y, w, h):
        if face in ("east", "west", "south") and 3 <= y <= 5:
            return STRAP if y != 4 else shade(STRAP, 1.6)
        return mat(face, x, y, w, h)
    return m


def chem_mats(look):
    coat = stained(cloth(COAT, 43110, 3.4), 43112, 0.24)
    shirt = cloth(BLUE_SHIRT, 43111, 3.0)

    def torso(face, x, y, w, h):
        c = coat(face, x, y, w, h)
        if face != "north":
            return c
        mid = (w - 1) / 2
        dx = abs(x - mid)
        open_w = 2.8 - y * 0.28
        if y < 1 and dx <= 1.5:
            return look.sk(look.seed + 100)(face, x, y, w, h)
        if y <= 9 and dx <= open_w:
            return shirt(face, x, y, w, h)
        if y <= 9 and dx <= open_w + 1.1:
            return COAT[5] if c[0] > 60 else c     # the coat's lapel
        if dx < 0.6 and y > 9:
            return COAT[1] if y % 5 else STEEL[2]  # the front edge and its buttons
        if 2 <= x <= 5 and 7 <= y <= 9:            # the name tag: an ochre strip over a white card
            if y == 7:
                return OCHRE[4]
            return WHITE_SHIRT[5] if (x + y) % 2 or x in (2, 5) else BLACK[3]
        if 10 <= x <= 13 and 7 <= y <= 10:         # the breast pocket and its pens
            if y == 7 and x in (11, 12):
                return (OCHRE[4], PETROL[4])[x - 11]
            return COAT[2] if y == 7 or x in (10, 13) else c
        if 15 <= y <= 18 and (2 <= x <= 5 or 10 <= x <= 13):
            return COAT[2] if y == 15 else c       # the side pockets
        return c

    def sleeve(s):
        return stained(cloth(COAT, 43120 + s, 3.4), 43122 + s, 0.22)

    def forearm(s):
        sl = stained(cloth(COAT, 43125 + s, 3.3), 43127 + s, 0.28)

        def m(face, x, y, w, h):
            if y == h - 1 and face not in ("up", "down"):
                return COAT[2]                     # the cuff
            return sl(face, x, y, w, h)
        return m

    def glove(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, 43130, 32, 32, 2, 5.0)
        c = tone(GLOVE, 3.0 + (n - 0.5) * 1.4)
        if face == "up" and y == 0:
            return GLOVE[4]
        return c

    def fingers(face, x, y, w, h):
        c = shade(glove(face, x, y, w, h), 0.9 if x % 2 else 1.0)
        if (face == "down" or y == h - 1) and h01(43131, face, x, y) < 0.45:
            return GOO[2]                          # goo on the fingertips
        return c

    def thigh(s):
        # The coat's skirt hangs to the knee, open down the front between the legs; slacks show inside it.
        sk_ = stained(cloth(COAT, 43140 + s, 3.3), 43142 + s, 0.24)
        sl = cloth(SLACKS, 43144 + s, 2.8)
        inner = "east" if s < 0 else "west"

        def m(face, x, y, w, h):
            if face in ("up", "down"):
                return sl(face, x, y, w, h)
            if face == inner:
                return sl(face, x, y, w, h)
            if face == "north":
                # x = 0 is the viewer's left: the right leg's outer half is there, the left leg's on the other side.
                inside = x >= w - 3 if s < 0 else x <= 2
                if inside:
                    return sl(face, x, y, w, h)
                if (x == w - 4 if s < 0 else x == 3):
                    return COAT[1]                 # the front edge
            c = sk_(face, x, y, w, h)
            return shade(c, 0.85) if y == h - 1 else c   # the hem
        return m

    def shoe(face, x, y, w, h):
        if face != "up" and y >= h - 1:
            return hexc("#120c08")
        if y == 0 and face != "up":
            return tone(SLACKS, 1.8)               # the trouser hem over the shoe
        c = tone(L.BOOT, 2.4 + (0.6 if (x + y) % 4 == 0 else 0))
        if face == "north" and y == 2 and x % 2:
            return L.BOOT[0]
        return c

    mats = {
        "neck": lambda s: look.sk(look.seed + 100, 0.015),
        "hand": lambda s: glove, "fingers": lambda s: fingers,
        "torso": lambda s: torso,
        "buckle": lambda s: lambda f, x, y, w, h: COAT[3] if f != "north" or x != 2 else STEEL[2],
        "collar": lambda s: lambda f, x, y, w, h: BLUE_SHIRT[5] if y == 0 else BLUE_SHIRT[4],
        "sleeve": sleeve,
        "roll": lambda s: none_on(("up",), stained(cloth(COAT, 43128 + s, 3.3), 43129 + s, 0.2)),
        "forearm": forearm,
        "thigh": thigh,
        "shin": lambda s: cloth(SLACKS, 43150 + s, 2.8),
        "boot": lambda s: shoe,
    }
    return mats


# =====================================================================================================

LOOKS = [
    Look("executive", EXEC_FACE,
         skin=Ramp("#25272d", "#3d4149", "#575c65", "#70757e", "#898e96", "#a3a8ae"),
         hair=Ramp("#08090c", "#121419", "#1c1f26", "#282c35", "#383d48", "#505764"),
         eyes=(hexc("#cfd2d4"), hexc("#3f5a70")),
         extra={"G": hexc("#8d9198"), "M": hexc("#4a3a3a")}, mats_fn=exec_mats, seed=41001, hair_mat=exec_hair),
    Look("guard", GUARD_FACE,
         skin=Ramp("#221d1b", "#38302c", "#514540", "#695b53", "#82736a", "#9a8b80"),
         hair=Ramp("#0d0b0a", "#1a1512", "#28201b", "#372c25", "#463830", "#57463b"),
         eyes=(hexc("#c4bdb2"), hexc("#2c221c")),
         extra={"k": stubble, "C": hexc("#8f7e74")}, mats_fn=guard_mats, seed=42001,
         hair_mat=lambda look: stubble(look, look.sk(look.seed))),
    Look("chemist", CHEM_FACE,
         skin=Ramp("#262923", "#40453c", "#5b6156", "#767c6e", "#909684", "#a9ae9a"),
         hair=Ramp("#2b1206", "#4a1f0b", "#6b2f12", "#8c421a", "#ab5a25", "#c97a3a"),
         eyes=(hexc("#c8c9bd"), hexc("#4d5e36")),
         extra={"g": GOGGLE_FRAME, "l": LENS, "L": LENS_HI, "t": STRAP, "d": hexc("#4a4a44")}, mats_fn=chem_mats,
         seed=43001, hair_mat=chem_hair, wrap=goggle_strap),
]


def check_same_layout(base, variant):
    """The skins share the base's geo file: the packed UVs must be identical."""
    a, b = base.rig, variant.rig
    assert (a.tex_w, a.tex_h) == (b.tex_w, b.tex_h)
    for ca, cb in zip((c for c, *_ in base.jobs), (c for c, *_ in variant.jobs)):
        assert ca.rects == cb.rects and ca.tag == cb.tag
    assert len(base.jobs) == len(variant.jobs)


def generate():
    base, hbase = L.rig(), L.head_rig()
    base.rig.pack(gutter=1)
    hbase.rig.pack(gutter=1)
    for look in LOOKS:
        m = L.rig(look)
        t, _ = m.build(gutter=1, seed=25000)
        check_same_layout(base, m)
        save(t, "entity", "leviathan_" + look.name)
        hm = L.head_rig(look)
        t, _ = hm.build(gutter=1, seed=25300)
        check_same_layout(hbase, hm)
        save(t, "entity", "leviathan_" + look.name + "_head")


if __name__ == "__main__":
    generate()
