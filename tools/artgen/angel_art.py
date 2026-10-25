"""Wild angels and the fallen (v0.22): vessels in street clothes, as in the show. One rig, seven textures.

  angel_0          a beige trench coat open over a dark suit, a loose blue tie; brown hair, fair skin
  angel_1          an office worker: white shirt, navy tie, a lanyard, grey slacks; dark skin, close-cut black hair
  angel_2          casual: a brown leather jacket over a grey tee, jeans, sneakers; ginger hair, freckles
  angel_3          a woman in a charcoal blazer over a cream blouse, dark trousers; long black hair, olive skin
  fallen_angel_0   a torn olive parka over a grey hoodie, filthy jeans, long matted hair and a beard; pale
  fallen_angel_1   a stained shirt under a ragged brown cardigan, torn corduroys, grey stubble; dark skin gone ashy
  fallen_angel_2   a woman in a torn denim jacket over a faded flannel, ripped leggings, matted blonde hair; pale
Each with its `_glowmask`: the angels' eyes burn pale blue (and their wings' rims of light); the fallen's are dimmed to a
grey-blue ember, and the singed tips of their wings smoulder.

The wings (allegiance_art.build_wings) are the same cubes on every texture: the angels' paint them `shadow` (smoke, see
through), the fallen's `burnt` (charred, holed, missing feathers, ember tips).

Skeleton (michael_art.skeleton):
  root -- body -- head -- eye_l, eye_r, mouth          locators, no cubes: on the face's front surface
               -- right_arm -- right_forearm -- right_hand -- right_item   the held item (no rest rotation)
               -- left_arm  -- left_forearm  -- left_hand
               -- coat_tail_r, coat_tail_l, wings -- wing_r, wing_l -- ...
       -- right_leg -- right_shin;  left_leg -- left_shin
"""

import math
import os
import sys

import allegiance_art as AL
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on
from common import ASSETS, save
from michael_art import ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, fabric, skeleton, tone
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

A = "animation.angel."
CLEAR = (0, 0, 0, 0)

# --- palettes -------------------------------------------------------------------------------------------------
SK_FAIR = Ramp("#6e4b3d", "#9c7062", "#c0927e", "#d6ab95", "#e5c1aa", "#f1d7c4")
SK_DARK = Ramp("#24160f", "#38231a", "#4e3224", "#634130", "#79523d", "#90654c")
SK_OLIVE = Ramp("#57402c", "#7d5b42", "#9b7556", "#b18b69", "#c39f7e", "#d5b594")
SK_FRECKLE = Ramp("#74503f", "#a47664", "#c79a85", "#dcb39d", "#e9c8b2", "#f4dccb")
SK_PALE = Ramp("#5f5452", "#887a76", "#a69791", "#bbada6", "#cbbfb9", "#dad0cb")
SK_ASHY = Ramp("#221a17", "#342823", "#473831", "#58473f", "#69574d", "#7c695e")

HR_BROWN = Ramp("#140d09", "#22160e", "#332216", "#45301f", "#58402a", "#6c5136")
HR_BLACK = Ramp("#060505", "#0c0a0a", "#141111", "#1d1818", "#272121", "#332b2b")
HR_GINGER = Ramp("#3a1407", "#5c220c", "#7f3312", "#a0461a", "#bb5d27", "#d27a3c")
HR_RAVEN = Ramp("#07070a", "#0e0e12", "#16161c", "#202028", "#2c2c36", "#3a3a47")
HR_MATTED = Ramp("#2a2014", "#3e3020", "#54432c", "#695638", "#7c6745", "#8e7853")
HR_GREY = Ramp("#1a1818", "#2e2b2a", "#45413f", "#5f5a57", "#7b7672", "#98938f")
HR_DIRTY_BLONDE = Ramp("#4a3a20", "#6a5530", "#887044", "#a38a58", "#b9a06c", "#cbb480")

TRENCH = Ramp("#4f4433", "#6e604a", "#8c7d63", "#a6977b", "#bcae92", "#cfc3aa")
SUIT = Ramp("#07080b", "#0e1015", "#161920", "#1f232b", "#2a2f38", "#363c47")
SHIRT = Ramp("#9aa1aa", "#b5bbc3", "#cbd0d6", "#dde1e6", "#ebeef1", "#f8f9fb")
BLUE_TIE = Ramp("#0d1a3a", "#152a5a", "#1f3c7c", "#2b509c", "#3c66b6", "#5482cc")
NAVY = Ramp("#080d1c", "#0e162c", "#16213e", "#202d52", "#2c3b67", "#3a4b7e")
SLACKS = Ramp("#25272b", "#33363b", "#43464c", "#54585f", "#686c74", "#7e838b")
LEATHER = Ramp("#1e110a", "#2e1a10", "#3f2516", "#52311d", "#663e25", "#7b4d2f")
TEE = Ramp("#3a3d42", "#4d5056", "#61656b", "#767a80", "#8b8f95", "#a1a5ab")
DENIM = Ramp("#141c2c", "#1d2940", "#283855", "#344869", "#44597b", "#5a6f8e")
SNEAKER = Ramp("#5d5f63", "#8a8c90", "#b0b2b5", "#cfd1d3", "#e3e4e6", "#f4f4f5")
CHARCOAL = Ramp("#0f1013", "#181a1e", "#222429", "#2d3036", "#3a3d44", "#4a4e56")
CREAM = Ramp("#8f8574", "#ada290", "#c7bda9", "#dad1be", "#e8e0cf", "#f4eee1")
BLACK_SHOE = Ramp("#050404", "#0b0909", "#131011", "#1c1819", "#2a2526", "#4a4446")
OLIVE = Ramp("#1f2414", "#2d331d", "#3c4427", "#4c5532", "#5c663d", "#6e784a")
HOODIE = Ramp("#26272a", "#35363a", "#46474b", "#57585d", "#6a6b70", "#7f8085")
CARDIGAN = Ramp("#24180f", "#352417", "#47321f", "#5a4029", "#6c4f33", "#7f5f3e")
CORD = Ramp("#2a1d12", "#3b2a1a", "#4e3823", "#61472d", "#745637", "#886643")
STAINED = Ramp("#6e6656", "#8a826f", "#a39b86", "#b8b19c", "#c9c3af", "#d7d2c0")
FLANNEL = Ramp("#3a0f0e", "#561817", "#712221", "#8a2d2a", "#a03b35", "#b54d44")
LEGGING = Ramp("#0f0f12", "#18181c", "#222227", "#2d2d33", "#393940", "#46464e")
BOOT = Ramp("#0e0a07", "#18110c", "#231912", "#2e2218", "#3a2c20", "#483729")
GRIME = hexc("#2e261c")
LANYARD = Ramp("#0d2a5c", "#14397a", "#1d4b98", "#2a5fb4", "#3c74c8", "#5590dc")
EMBER = Ramp("#3a0604", "#6e0d06", "#a8200a", "#e0430f", "#ff7a26", "#ffc46a")
CHAR = Ramp("#050404", "#0b0908", "#13100e", "#1c1815", "#27211d", "#352d27")


# --- vessels --------------------------------------------------------------------------------------------------
# style: hair (short, crop, tousled, long, matted, cropgrey, long_blonde); beard: None, stubble, full, grey
VESSELS = {
    "angel_0": dict(skin=SK_FAIR, hair=HR_BROWN, style="tousled", fallen=False),
    "angel_1": dict(skin=SK_DARK, hair=HR_BLACK, style="crop", fallen=False),
    "angel_2": dict(skin=SK_FRECKLE, hair=HR_GINGER, style="short", fallen=False, freckles=True),
    "angel_3": dict(skin=SK_OLIVE, hair=HR_RAVEN, style="long", fallen=False, woman=True),
    "fallen_angel_0": dict(skin=SK_PALE, hair=HR_MATTED, style="matted", fallen=True, beard="full"),
    "fallen_angel_1": dict(skin=SK_ASHY, hair=HR_GREY, style="cropgrey", fallen=True, beard="grey"),
    "fallen_angel_2": dict(skin=SK_PALE, hair=HR_DIRTY_BLONDE, style="long", fallen=True, woman=True),
}
ANGELS = ("angel_0", "angel_1", "angel_2", "angel_3")
FALLEN = ("fallen_angel_0", "fallen_angel_1", "fallen_angel_2")


def seed_of(name):
    return 7100 + sum(ord(c) for c in name) * 7


# --- the face (north face of the head, 16x16 texels, x = 0 at the vessel's RIGHT) -----------------------------
# Eyes: texels 2..5 (right) and 10..13 (left), rows 7..8. Mouth: texels 5..10, row 12. The locators sit on those.

def face_char(v, x, y):
    style, woman, beard, fallen = v["style"], v.get("woman"), v.get("beard"), v["fallen"]
    eye_r, eye_l = 2 <= x <= 5, 10 <= x <= 13
    # hairline
    if style in ("long",):
        if y < 3 or (y < 5 and x < 9 - y) or (y < 4 and x > 11):
            return "h"
        if (x <= 0 or x >= 15) and y < 11:
            return "h"
    elif style == "matted":
        if y < 3 or (y == 3 and x % 3 != 1) or (y == 4 and x in (1, 2, 6, 12, 13)):
            return "h"
        if (x == 0 or x == 15) and y < 12:
            return "h"
    elif style in ("crop", "cropgrey"):
        if y < 2 or (y == 2 and 2 < x < 13):
            return "h"
    elif style == "tousled":
        if y < 3 or (y == 3 and x in (2, 3, 4, 8, 9, 13)):
            return "h"
    else:  # short
        if y < 3 or (y == 3 and 3 < x < 12):
            return "h"
    if y == 6 and (eye_r or eye_l):
        if woman and (x in (2, 13)):
            return "s"
        return "b"
    if y in (7, 8) and (eye_r or eye_l):
        if woman and y == 7 and x in (2, 13):
            return "l"
        return "E" if x in (3, 4, 11, 12) else "w"
    if y == 9 and (eye_r or eye_l):
        return "U" if fallen else "u"
    if y == 9 and x in (7, 8):
        return "n"
    if y == 10 and x in (6, 9):
        return "N"
    if y == 10 and x in (7, 8):
        return "n"
    if y == 12 and 5 <= x <= 10:
        return "L" if woman and x in (5, 10) else "m"
    if woman and y == 13 and 6 <= x <= 9:
        return "L"
    if beard == "full" and ((y >= 11 and (x <= 3 or x >= 12)) or y >= 13 or (y == 11 and 5 <= x <= 10)):
        return "B"
    if beard == "grey" and ((y >= 11 and (x <= 2 or x >= 13)) or y >= 13 or (y == 11 and 5 <= x <= 10)):
        return "k"
    if v.get("freckles") and y in (9, 10) and x in (1, 3, 5, 10, 12, 14) and (x + y) % 2:
        return "f"
    if fallen and ((y == 10 and x in (12, 13)) or (y == 11 and x == 13)):
        return "d"
    if fallen and v["style"] == "matted" and y == 5 and x in (11, 12):
        return "c"
    if y >= 14 and (x <= 1 or x >= 14):
        return "j"
    return "s"


def face_rows(v):
    return ["".join(face_char(v, x, y) for x in range(16)) for y in range(16)]


def eye_colours(v):
    if v["fallen"]:
        return hexc("#7d8896"), hexc("#c3bcae")
    return hexc("#9fd6ff"), hexc("#e4eff8")


def head_parts(m, v):
    sk, hr = v["skin"], v["hair"]
    seed = v["seed"]
    skin = H.skin_mat(sk, seed + 1, 3.0, 0.4)
    if v["fallen"]:
        base = skin
        skin = lambda f, x, y, w, h: mix(base(f, x, y, w, h), GRIME, 0.25) if fbm(x * 2 + len(f), y * 2, seed + 5, 64, 64, 2, 7) > 0.66 \
            else base(f, x, y, w, h)
    hair = lambda f, x, y, w, h: tone(hr, 2.7 + 1.1 * (fbm(x * 2 + len(f) * 7, y, seed + 9, 64, 64, 2, 8.0) - 0.5) * 2
                                      + 0.45 * math.sin(x * 1.9 + seed) - 0.6 * (y / max(1, h)))
    eye, white = eye_colours(v)
    beard_r = hr if v.get("beard") == "full" else HR_GREY
    beard = lambda f, x, y, w, h: tone(beard_r, 2.2 + 1.2 * math.sin(x * 2.1 + y * 1.3))
    stub = lambda f, x, y, w, h: mix(skin(f, x, y, w, h), HR_GREY[3] if (x * 3 + y) % 3 else HR_GREY[1], 0.55)
    legend = {"h": hair, "s": skin, "b": hr[1], "w": white, "E": eye, "l": hr[0], "u": sk[2], "U": mix(sk[1], hexc("#4a3a4a"), 0.3),
              "n": sk[4], "N": sk[1], "m": mix(sk[1], hexc("#7a3b38"), 0.4), "L": mix(sk[2], hexc("#a0444a"), 0.55),
              "B": beard, "k": stub, "f": mix(sk[2], hexc("#8a4a2a"), 0.5), "d": mix(sk[2], GRIME, 0.5), "c": hexc("#6a1f1c"),
              "j": sk[2]}

    def eye_glow(f, x, y, w, h, c):
        if f != "north":
            return None
        if c == eye:
            return hexc("#4d6680") if v["fallen"] else hexc("#d6f0ff")
        if c == white and not v["fallen"]:
            return hexc("#5f97d8")
        return None
    side_beard = beard if v.get("beard") == "full" else (stub if v.get("beard") == "grey" else None)
    H.head_cube(m, face_rows(v), legend, H.head_sides(skin, hair, top=4, back=12, temple=7, sideburn=8, beard=side_beard,
                                                      beard_from=11), glow=eye_glow)
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: sk[4] if f == "north" else sk[2], density=2, tag="nose")
    # The hair shell over the crown: its front face is the fringe.
    style = v["style"]

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "up" or f == "south":
            return hair(f, x, y, w, h)
        if f == "north":
            if style == "long":
                return hair(f, x, y, w, h) if y < 2 + (8 - x) // 2 or y < 2 else None
            if style == "matted":
                return hair(f, x, y, w, h) if y < 2 + (h01("strand", seed, x) * 6) else None
            if style in ("crop", "cropgrey"):
                return hair(f, x, y, w, h) if y < 1 else None
            if style == "tousled":
                return hair(f, x, y, w, h) if y < 2 + (x % 5 == 2) + (x % 7 == 4) * 2 else None
            return hair(f, x, y, w, h) if y < 2 + (x > 9) else None
        fx = x if f == "west" else w - 1 - x  # 0 at the front
        lim = {"long": 8, "matted": 8, "crop": 3, "cropgrey": 3, "tousled": 5, "short": 4}[style]
        if y < 2 or (fx > 6 and y < lim + 2) or (fx > 3 and y < lim):
            return hair(f, x, y, w, h)
        return None
    crop = style in ("crop", "cropgrey")
    m.cube("head", (-4, 28, -4), (8, 4, 8), shell, density=2, inflate=0.12 if crop else 0.3, tag="hair")
    # Long hair down the back and over the ears (clear on the short-haired).
    long_ = style in ("long", "matted")

    def mane(f, x, y, w, h):
        if not long_ or f in ("north", "up"):
            return None
        if f == "down":
            return hair(f, x, y, w, h)
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            if y > h - 3 and h01("tip", seed, f, x) < 0.5:
                return None
            return hair(f, x, y, w, h) if fx >= 1 or y < 5 else None
        if y > h - 3 and h01("tip", seed, f, x) < 0.45:
            return None
        return hair(f, x, y, w, h)
    m.cube("head", (-4, 21, -2), (8, 9, 6), mane, density=2, inflate=0.45, tag="mane")


# --- cloth ------------------------------------------------------------------------------------------------------

def worn(mat, seed, dirt=0.4, holes=0.0, under=None, fray=True):
    """Street clothes after the Fall: grime patches, a faded cast, holes (showing `under`, or clear) and frayed hems."""
    if mat is None:
        return None

    def m(f, x, y, w, h):
        c = mat(f, x, y, w, h)
        if c is None or c[3] == 0:
            return c
        n = fbm(x * 2 + len(f) * 9, y * 2, seed, 64, 64, 3, 6.0)
        if holes and f not in ("up", "down"):
            hn = fbm(x * 1.5 + len(f) * 17, y * 1.5, seed + 11, 64, 64, 2, 5.0)
            if hn > 1 - holes:
                return under(f, x, y, w, h) if under else None
            if hn > 1 - holes - 0.04:
                c = shade(c, 0.7)                     # the frayed rim of a hole
        if fray and f in ("north", "south", "east", "west") and y == h - 1 and h01("fray", seed, f, x) < 0.45:
            return under(f, x, y, w, h) if under else None
        c = mix(c, (128, 122, 112, 255), 0.18)      # faded
        if n > 1 - dirt:
            c = mix(c, GRIME, 0.25 + 0.8 * (n - (1 - dirt)))
        return c
    return m


def shirt_front(shirt, tie=None, tie_dx=0, loose=False, lanyard=False, buttons=True):
    """A shirt's body: a collar, a button placket, an optional tie (loose: knot low and askew) and a lanyard."""
    def m(f, x, y, w, h):
        if f != "north":
            return shirt(f, x, y, w, h)
        mid = (w - 1) / 2
        if tie is not None:
            k0 = 2 if loose else 0
            tx = mid + tie_dx * min(1.0, y / 10)
            if k0 <= y <= k0 + 1 and abs(x - (mid + tie_dx * 0.2)) <= 1.0:
                return tie[4] if y == k0 else tie[3]
            if k0 + 1 < y < 17 and abs(x - tx) <= 1.0 + (y > 13) * 0.4:
                return tie[4] if x < tx else tie[2]
            if y == 17 and abs(x - tx) <= 0.6:
                return tie[2]
        if y < 2 and abs(x - mid) > 1.5 and abs(x - mid) < 4.5 - y:
            return shade(shirt(f, x, y, w, h), 1.08)  # the collar points
        if lanyard:
            if y < 9 and (abs(x - (mid - 3 + y * 0.28)) < 0.6 or abs(x - (mid + 3 - y * 0.28)) < 0.6):
                return LANYARD[3 + (y % 3 == 0)]
            if 9 <= y <= 13 and abs(x - mid) <= 1.6:
                return hexc("#f1f3f5") if not (y == 11 and abs(x - mid) < 1) else hexc("#3a6aa8")
        if buttons and abs(x - mid) < 0.6 and y > 2:
            return shade(shirt(f, x, y, w, h), 0.86) if y % 4 else hexc("#e8e8e4")
        return shirt(f, x, y, w, h)
    return m


def tee_front(tee, seed, print_=False):
    def m(f, x, y, w, h):
        if f == "north" and y < 1 and abs(x - (w - 1) / 2) < 3:
            return shade(tee(f, x, y, w, h), 0.75)   # the crew neck
        return tee(f, x, y, w, h)
    return m


def flannel(seed):
    def m(f, x, y, w, h):
        v = 2.6 + 0.4 * math.sin(x * 0.8 + y * 0.3 + seed)
        c = FLANNEL[v]
        if x % 6 in (0, 1):
            c = mix(c, hexc("#1a1414"), 0.55)
        if y % 6 in (0, 1):
            c = mix(c, hexc("#1a1414"), 0.45)
        if x % 6 == 3 or y % 6 == 3:
            c = mix(c, hexc("#c99a6a"), 0.25)
        return c
    return m


def denim(seed, ramp=DENIM):
    def m(f, x, y, w, h):
        n = fbm(x + len(f) * 3, y, seed, 64, 64, 3, 8.0)
        v = 2.6 + (n - 0.5) * 1.6 + (0.5 if (x - y) % 3 == 0 else 0)
        c = ramp[v]
        if f == "north" and h > 8 and 0.25 < y / h < 0.6 and 2 <= x <= w - 3:
            c = mix(c, ramp[5], 0.22)
        if f in ("east", "west") and x == w // 2:
            c = shade(c, 1.15) if y % 2 else shade(c, 0.85)
        return c
    return m


def corduroy(seed):
    def m(f, x, y, w, h):
        v = 2.8 + (0.6 if x % 2 else -0.2) - 0.6 * y / max(1, h)
        return tone(CORD, v)
    return m


def open_front(mat, half=2.5, collar=4, zipper=None, lapel=None, v_depth=0, buttons=None):
    """An open jacket or coat: a vertical opening down the middle (clear: the layer beneath shows), its edges lit, a
    collar or lapels at the top; `v_depth` > 0 turns it into a buttoned suit jacket's V."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        if f != "north":
            return mat(f, x, y, w, h)
        mid = (w - 1) / 2
        d = abs(x - mid)
        if v_depth:
            hw = 3.5 * max(0.0, 1 - y / v_depth)
            if y < v_depth and d <= hw + 0.5:
                return None
            if y < v_depth + 1 and d <= hw + 2.0:
                return lapel or shade(mat(f, x, y, w, h), 1.15)
            if buttons is not None and d < 0.6 and y in (v_depth + 2, v_depth + 6):
                return buttons
            if d < 0.5:
                return shade(mat(f, x, y, w, h), 0.8)
            return mat(f, x, y, w, h)
        hw = half + (2.0 if y < collar else 0.0) * (1 - y / max(1, collar))
        if d <= hw:
            return None
        if d <= hw + 1.0:
            if zipper is not None and d <= hw + 0.6:
                return zipper if y % 2 else shade(zipper, 0.7)
            return lapel or shade(mat(f, x, y, w, h), 1.18 if y < collar + 2 else 1.08)
        return mat(f, x, y, w, h)
    return m


def blouse_front(blouse):
    def m(f, x, y, w, h):
        if f == "north":
            mid = (w - 1) / 2
            if y < 4 and abs(x - mid) < 2.5 - y * 0.5:
                return None  # (the neck shows on the torso below: the blouse is the torso itself)
        return blouse(f, x, y, w, h)
    return m


def outfit(name, v):
    """Every layer of a vessel (None = clear): torso, jacket (0.3), coat (0.6), tails, sleeve, trousers, shoe, belt, hood."""
    s = v["seed"]
    skin = H.skin_mat(v["skin"], s + 3, 3.0, 0.4)
    o = dict(jacket=None, coat=None, tails=None, belt=None, hood=None)
    if name == "angel_0":
        coat = fabric(TRENCH, s + 10, 3.1, 0.25)
        suit = fabric(SUIT, s + 11, 3.0, 0.3)
        o.update(torso=shirt_front(fabric(SHIRT, s + 12, 3.6, 0.15), BLUE_TIE, tie_dx=1.2, loose=True),
                 jacket=open_front(suit, v_depth=13, buttons=SUIT[4]), coat=open_front(coat, half=3.2, collar=6),
                 tails=coat, sleeve=coat, trousers=fabric(SUIT, s + 13, 2.7, 0.15, crease=True), shoe=BLACK_SHOE,
                 belt=BLACK_SHOE)
    elif name == "angel_1":
        shirt = fabric(SHIRT, s + 20, 3.9, 0.25)
        o.update(torso=shirt_front(shirt, NAVY, lanyard=True), sleeve=shirt, trousers=fabric(SLACKS, s + 21, 2.9, 0.15, crease=True),
                 shoe=BLACK_SHOE, belt=BLACK_SHOE)
    elif name == "angel_2":
        lea = H.skin_mat(LEATHER, s + 30, 3.0, 1.0)
        o.update(torso=tee_front(fabric(TEE, s + 31, 3.0, 0.3), s), jacket=open_front(lea, half=2.6, collar=4, zipper=hexc("#8c8c86")),
                 sleeve=lea, trousers=denim(s + 32), shoe=SNEAKER, belt=LEATHER)
    elif name == "angel_3":
        blazer = fabric(CHARCOAL, s + 40, 3.1, 0.3)
        o.update(torso=blouse_front(fabric(CREAM, s + 41, 3.4, 0.25)), jacket=open_front(blazer, v_depth=15, buttons=CHARCOAL[5]),
                 sleeve=blazer, trousers=fabric(CHARCOAL, s + 42, 2.6, 0.15, crease=True), shoe=BLACK_SHOE)
    elif name == "fallen_angel_0":
        hood = worn(fabric(HOODIE, s + 50, 3.0, 0.35), s + 51, 0.5, 0.18, skin)
        parka = fabric(OLIVE, s + 52, 3.0, 0.5)
        o.update(torso=hood, coat=worn(open_front(parka, half=3.0, collar=5, zipper=hexc("#3a3a36")), s + 53, 0.55, 0.22),
                 tails=worn(parka, s + 54, 0.6, 0.2, fray=True), sleeve=worn(parka, s + 55, 0.55, 0.16, hood),
                 trousers=worn(denim(s + 56), s + 57, 0.6, 0.2, skin), shoe=BOOT, hood=hood)
    elif name == "fallen_angel_1":
        shirt = worn(shirt_front(fabric(STAINED, s + 60, 3.3, 0.3), buttons=True), s + 61, 0.6, 0.15, skin)
        cardi = fabric(CARDIGAN, s + 62, 3.0, 0.5)
        o.update(torso=shirt, jacket=worn(open_front(cardi, half=2.2, collar=3), s + 63, 0.5, 0.25),
                 sleeve=worn(cardi, s + 64, 0.5, 0.2, skin), trousers=worn(corduroy(s + 65), s + 66, 0.55, 0.2, skin),
                 shoe=BOOT, belt=Ramp("#3a3020", "#544630", "#6e5e42", "#867556", "#9c8b6a", "#b2a17e"))
    else:  # fallen_angel_2
        jean = denim(s + 70, Ramp("#1b2230", "#26303f", "#334050", "#405063", "#506176", "#64768c"))
        o.update(torso=worn(flannel(s + 71), s + 72, 0.5, 0.12, skin), jacket=worn(open_front(jean, half=2.4, collar=4), s + 73, 0.5, 0.25),
                 sleeve=worn(jean, s + 74, 0.5, 0.2, skin), trousers=worn(fabric(LEGGING, s + 75, 2.8, 0.2), s + 76, 0.4, 0.14, lambda f, x, y, w, h: shade(skin(f, x, y, w, h), 0.8)),
                 shoe=BOOT)
    return o


# --- the rig ----------------------------------------------------------------------------------------------------

EYE_Y, MOUTH_Y, FACE_Z = 28.0, 25.75, -4.0
EYE_X = 2.0
ITEM_PIVOT = (-6.0, 12.0, -2.0)


def clear(f, x, y, w, h):
    return CLEAR


def rig(name="angel_0"):
    v = dict(VESSELS[name])
    v["seed"] = seed_of(name)
    s = v["seed"]
    m = Model("angel", 256, 512)
    skeleton(m)
    head_parts(m, v)
    o = outfit(name, v)
    skin = H.skin_mat(v["skin"], s + 3, 3.0, 0.4)
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, density=2, tag="neck")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o["torso"], density=2, tag="torso")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o["jacket"] or clear, density=2, inflate=0.3, tag="jacket")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o["coat"] or clear, density=2, inflate=0.6, tag="coat")
    belt = o["belt"]
    m.cube("body", (-4, 11.6, -2), (8, 1.2, 4), (lambda f, x, y, w, h: hexc("#b9b4a6") if f == "north" and 6 <= x <= 9
           else belt[2 + (x % 7 == 0)]) if belt else clear, inflate=0.38, density=2, tag="belt")
    # A hood fallen down the back (the hoodie's).
    m.cube("body", (-3.5, 20.5, 2.0), (7, 3, 2), none_on(("north",), o["hood"]) if o["hood"] else clear, density=2, inflate=0.2,
           tag="hood")
    hand = H.skin_mat(v["skin"], s + 4, 3.1, 0.35)
    shoe = o["shoe"]
    for side in ("right", "left"):
        H.arm_cubes(m, side, o["sleeve"], hand, sleeve_inflate=0.25)
        H.leg_cubes(m, side, o["trousers"], lambda f, x, y, w, h, sh=shoe: sh[4] if f == "up" and y < 3 else
                    (sh[4] if shoe is SNEAKER and y == h - 1 else sh[2]), shoe_h=2.0)
    for side, sg in (("r", -1), ("l", 1)):
        b = m.bone(f"coat_tail_{side}", (2 * sg, 12, 0), "body")
        tails = o["tails"]

        def tail(f, x, y, w, h, sg=sg, tails=tails):
            if tails is None or f in ("up", "down"):
                return None
            if f == "north" and ((sg < 0 and x >= w - 2) or (sg > 0 and x <= 1)):
                return None                       # the coat hangs open in front
            return tails(f, x, y, w, h)
        m.cube(b, (2 * sg - 2.2, 3, -2.4), (4.4, 9, 4.8), tail, density=2, tag="coat_tail")
    # Locators: where the light pours out (on the face's front surface) and where the blade is held.
    m.bone("eye_l", (EYE_X, EYE_Y, FACE_Z), "head")
    m.bone("eye_r", (-EYE_X, EYE_Y, FACE_Z), "head")
    m.bone("mouth", (0.0, MOUTH_Y, FACE_Z), "head")
    m.bone("right_item", ITEM_PIVOT, "right_hand")
    wb = m.bone("wings", (0, 20, 3.0), "body")
    AL.build_wings(m, wb.name, "burnt" if v["fallen"] else "shadow", off=(0.0, 19.8, 0.8), glow_rims=not v["fallen"])
    return m


# --- animations -------------------------------------------------------------------------------------------------

def wings(pr, pl=None):
    """Both wings: `pr` for the right one and `pl` (default the same) for the left."""
    pl = pl or pr
    out = {k: v for k, v in AL.wing_pose(**pr).items() if "wing_r_" in k}
    out.update({k: v for k, v in AL.wing_pose(**pl).items() if "wing_l_" in k})
    return out


FOLD = AL.FOLD
OPEN = AL.OPEN
# The rest of a fallen angel: the left wing folded but hanging low, the right one broken -- out from the back and down.
SAG_L = dict(AL.FOLD, arm=(18, 10, 70))
BROKEN_R = dict(AL.FOLD, arm=(24, 14, 46), hand=(0, 6, -160))
DROOP = P_(wings(BROKEN_R, SAG_L), {"wing_r": [4, 0, 16]})
SPREAD = wings(OPEN)
WIDE = wings(dict(OPEN, arm=(0, 10, 42), spread=2.0))
HIGH = wings(dict(arm=(0, 8, 70), fore=(0, -4, -30), hand=(0, -4, -36), fan={"primary": 12, "secondary": 6}))
DOWN = wings(AL.FLAP_DOWN)
UNFURL = wings(dict(arm=(0, 12, 74), fore=(0, 2, -72), hand=(0, 0, -40), fan={"primary": 30, "secondary": 22, "tertial": 10,
                                                                               "greater": 16, "median": 10}))
FOLDED = wings(FOLD)


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    # idle: the stillness of a borrowed body -- a slow breath, the head tilting to study you; the blade held low.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        c.key(t, P_(DROOP, {"@body": [0, -0.15 * abs(k), 0], "head": [-2 * abs(k), [0, 6, 0, -8, 0][i], [0, 7, 0, -3, 0][i]],
                            "right_arm": [-4, 0, 3], "right_forearm": [-14 - 3 * abs(k), 0, 0], "left_arm": [0, 0, -3],
                            "left_forearm": [-6 - 3 * abs(k), 0, 0], "coat_tail_r": [1.5 * abs(k), 0, 1], "coat_tail_l": [1.5 * abs(k), 0, -1],
                            "wing_r_hand": [0, 6, -160 + 4 * abs(k)]}), E_IO if i else None)

    # walk: an unhurried, purposeful stride.
    c = clip("walk", 1.0, loop=True)
    for i in range(5):
        t = i / 4
        sn = math.sin(2 * math.pi * t)
        c.key(t, P_(DROOP, {"right_leg": [-28 * sn, 0, 0], "left_leg": [28 * sn, 0, 0],
                            "right_shin": [34 * max(0, math.sin(2 * math.pi * t + 1.2)), 0, 0],
                            "left_shin": [34 * max(0, -math.sin(2 * math.pi * t + 1.2)), 0, 0],
                            "right_arm": [18 * sn - 4, 0, 3], "right_forearm": [-16, 0, 0], "left_arm": [-18 * sn, 0, -3],
                            "left_forearm": [-12, 0, 0], "body": [2, 4 * sn, 0], "head": [-1, -4 * sn, 0],
                            "@root": [0, 0.5 * abs(math.cos(2 * math.pi * t)), 0], "coat_tail_r": [12 * max(0, -sn) + 3, 0, 2],
                            "coat_tail_l": [12 * max(0, sn) + 3, 0, -2]}), E_IO if i else None)

    # stab (hit at 0.3 s = 6 ticks): the wings' shadow thrown open, the blade raised overhand, driven down and in.
    c = clip("stab", 0.6)
    up = {"right_arm": [-165, -10, 18], "right_forearm": [-38, 0, 0], "body": [-6, 16, 0], "head": [-6, -10, 0],
          "left_arm": [-30, 0, -14], "left_forearm": [-30, 0, 0], "@root": [0, 0.3, 0.5], "right_leg": [10, 0, 0], "left_leg": [-8, 0, 0]}
    hit = {"right_arm": [-62, 8, 0], "right_forearm": [-34, 0, 0], "body": [16, -10, 0], "head": [6, 8, 0], "left_arm": [-40, 0, -8],
           "left_forearm": [-50, 0, 0], "@root": [0, -0.8, -2.0], "right_leg": [14, 0, 0], "left_leg": [-26, 0, 0], "left_shin": [20, 0, 0]}
    c.key(0, FOLDED)
    c.key(0.18, P_(UNFURL, up), E_OUT)
    c.key(0.3, P_(WIDE, hit), E_SNAP)
    c.key(0.42, P_(SPREAD, hit, {"body": [14, -10, 0], "@root": [0, -0.7, -1.8]}), E_OUT)
    c.key(0.6, FOLDED, E_IO)

    # teleport (the beat lands at 0.4 s = 8 ticks): a crouch, the wings raised high, one beat down -- gone.
    c = clip("teleport", 0.5)
    crouch = {"@root": [0, -1.6, 0], "body": [14, 0, 0], "head": [-10, 0, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0],
              "right_shin": [46, 0, 0], "left_shin": [46, 0, 0], "right_arm": [10, 0, 20], "left_arm": [10, 0, -20]}
    c.key(0, FOLDED)
    c.key(0.15, P_(UNFURL, crouch), E_OUT)
    c.key(0.28, P_(HIGH, crouch, {"@root": [0, -2.0, 0], "body": [18, 0, 0]}), E_IO)
    c.key(0.4, P_(DOWN, {"@root": [0, 0.6, 0], "body": [-4, 0, 0], "head": [-6, 0, 0], "right_arm": [0, 0, 26], "left_arm": [0, 0, -26]}),
          E_SNAP)
    c.key(0.5, P_(DOWN, {"@root": [0, 1.2, 0], "body": [-6, 0, 0], "head": [-8, 0, 0], "right_arm": [0, 0, 30], "left_arm": [0, 0, -30]}),
          E_OUT)

    # smitten (30 ticks, held): struck by an angel blade -- snapped rigid in 4 ticks, head thrown back, arms flung out,
    # the back arched, the wings' shadow burnt open; it holds, trembling, while the light pours out.
    c = clip("smitten", 1.5, hold=True)
    rigid = {"head": [-38, 0, 0], "body": [-10, 0, 0], "right_arm": [-12, 0, 82], "left_arm": [-12, 0, -82],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [0, 0, 10], "left_hand": [0, 0, -10],
             "right_leg": [4, 0, 5], "left_leg": [4, 0, -5], "@root": [0, 0.6, 0.6], "coat_tail_r": [8, 0, 4], "coat_tail_l": [8, 0, -4]}
    c.key(0, FOLDED)
    c.key(0.06, P_(UNFURL, {"head": [-10, 0, 0], "body": [-3, 0, 0], "right_arm": [-6, 0, 30], "left_arm": [-6, 0, -30]}), E_OUT)
    c.key(0.2, P_(WIDE, rigid), E_OUT)
    c.key(0.85, P_(WIDE, rigid, {"head": [-42, 0, 0], "body": [-12, 0, 0], "right_arm": [-14, 0, 86], "left_arm": [-14, 0, -86],
                                 "@root": [0, 0.9, 0.7]}), E_IO)
    c.key(1.5, P_(WIDE, rigid, {"head": [-44, 0, 0], "body": [-13, 0, 0], "right_arm": [-15, 0, 88], "left_arm": [-15, 0, -88],
                                "@root": [0, 1.0, 0.8]}), E_IO)
    trem = lambda amp, fx, fy: (lambda t: [0.0, 0.0, 0.0] if t < 0.2 else
                                [amp * math.sin(t * fx), amp * 0.6 * math.sin(t * fy), amp * 0.5 * math.sin(t * (fx + fy) * 0.5)])
    c.layer("head", trem(1.6, 97, 71))
    c.layer("body", trem(0.7, 83, 59))
    c.layer("right_arm", trem(1.2, 91, 67))
    c.layer("left_arm", trem(1.2, 79, 101))

    # leap (the lunge lands at 0.25 s = 5 ticks): a fallen angel's spring, the broken wings flaring, hands first.
    c = clip("leap", 0.5)
    coil = {"@root": [0, -1.8, 1.0], "body": [24, 0, 0], "head": [-18, 0, 0], "right_leg": [-40, 0, 0], "left_leg": [-20, 0, 0],
            "right_shin": [56, 0, 0], "left_shin": [40, 0, 0], "right_arm": [30, 0, 14], "left_arm": [30, 0, -14]}
    lunge = {"@root": [0, 1.4, -4.0], "body": [30, 0, 0], "head": [-26, 0, 0], "right_arm": [-118, 10, 6], "left_arm": [-112, -10, -6],
             "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_leg": [26, 0, 0], "left_leg": [34, 0, 0],
             "left_shin": [20, 0, 0], "coat_tail_r": [30, 0, 0], "coat_tail_l": [30, 0, 0]}
    c.key(0, DROOP)
    c.key(0.12, P_(wings(dict(BROKEN_R, arm=(8, 20, 44)), dict(SAG_L, arm=(16, 10, 58))), coil), E_OUT)
    c.key(0.25, P_(wings(dict(BROKEN_R, arm=(0, 14, 50), hand=(0, 0, -30)), dict(OPEN, arm=(0, 16, 50))), lunge), E_SNAP)
    c.key(0.38, P_(wings(dict(BROKEN_R, arm=(0, 18, 46)), dict(OPEN, arm=(0, 16, 40))), lunge, {"@root": [0, 0.4, -4.4]}), E_OUT)
    c.key(0.5, DROOP, E_IO)

    for cl in clips:
        cl.done(step=0.05 if cl.anim.name.endswith("smitten") else 0.2)
    return f, clips


# --- FX textures --------------------------------------------------------------------------------------------------

def _smooth(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def _seg_dist(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    L2 = dx * dx + dy * dy
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L2)) if L2 else 0.0
    qx, qy = ax + dx * t, ay + dy * t
    return math.hypot(px - qx, py - qy), t


def wing_scorch():
    """Top-down: the wings of a smitten angel burnt into the ground, spread left and right; the body's spot at the
    bottom middle. Solid charcoal feathers (a darker seam between them), ash at the ragged edges, a few embers."""
    W, Hh = 256, 128
    t = Tex(W, Hh, 4401)
    cx, sy = W / 2, 100.0
    feathers = []  # (ax, ay, bx, by, width) as capsules
    for s in (-1, 1):
        # The leading edge: from the shoulder out and up to the wrist, then on to the tip.
        sh = (cx + s * 8, sy - 14)
        wrist = (cx + s * 58, sy - 56)
        tip = (cx + s * 90, sy - 76)

        def edge_at(u):
            if u < 0.6:
                k = u / 0.6
                return sh[0] + (wrist[0] - sh[0]) * k, sh[1] + (wrist[1] - sh[1]) * k
            k = (u - 0.6) / 0.4
            return wrist[0] + (tip[0] - wrist[0]) * k, wrist[1] + (tip[1] - wrist[1]) * k
        feathers.append((sh[0], sh[1], wrist[0], wrist[1], 6.5))
        feathers.append((wrist[0], wrist[1], tip[0], tip[1], 4.5))
        # Coverts: a short row along the edge, filling the arm.
        for i in range(12):
            u = i / 11
            ax, ay = edge_at(u * 0.95)
            ang = math.radians(90 - s * (14 + 50 * u))
            L = 12 + 4 * u
            feathers.append((ax, ay, ax + math.cos(ang) * L, ay + math.sin(ang) * L, 5.0))
        # Flight feathers: secondaries trail back (down the texture); primaries sweep out towards the tip.
        n = 22
        for i in range(n):
            u = i / (n - 1)
            ax, ay = edge_at(u)
            ang = math.radians(90 - s * (4 + 52 * u ** 1.3))
            L = 30 + 20 * u - 12 * u * u + (h01("scl", i, s) - 0.5) * 5
            feathers.append((ax, ay, ax + math.cos(ang) * L, ay + math.sin(ang) * L, 5.2 - 1.4 * u))
    for y in range(Hh):
        for x in range(W):
            d1, d2 = 99.0, 99.0
            for ax, ay, bx, by, wd in feathers:
                d, tt = _seg_dist(x + 0.5, y + 0.5, ax, ay, bx, by)
                d = d - wd * (1 - 0.6 * tt ** 2)
                if d < d1:
                    d1, d2 = d, d1
                elif d < d2:
                    d2 = d
            # the body's mark: a burnt oval
            bd = math.hypot((x + 0.5 - cx) / 10.0, (y + 0.5 - sy) / 19.0) * 10.0 - 10.0
            if bd < d1:
                d1, d2 = bd, 99.0
            n = fbm(x, y, 4402, W, Hh, 4, 16.0)
            edge = d1 + (n - 0.5) * 4.0
            a = 1 - _smooth(-2.0, 2.5, edge)
            if a <= 0.01:
                if h01("ash", x, y) < 0.015 and edge < 8:
                    t.set(x, y, (112, 106, 98, 110))   # loose ash
                continue
            core = 1 - _smooth(-3.0, 0.5, edge)
            col = mix(hexc("#4a423b"), hexc("#0d0a09"), core)
            col = mix(col, hexc("#2a231e"), 0.3 * (n - 0.3))
            if d2 < 0 and abs(d1 - d2) < 0.9:
                col = mix(col, hexc("#3a322c"), 0.55)   # the ash line where two feathers overlap
            if -1.0 < edge < 2.0 and h01("ember", x, y) < 0.04:
                col, a = mix(EMBER[3], EMBER[5], h01("e2", x, y)), 1.0
            alpha = int(255 * a * (0.95 - 0.2 * (n - 0.5)))
            t.set(x, y, (col[0], col[1], col[2], max(0, min(255, alpha))))
    return t


def fall_streak():
    """A falling angel: a white-blue head at the bottom and a long tail fading upward (for additive rendering)."""
    W, Hh = 32, 256
    t = Tex(W, Hh, 4501)
    hx, hy = W / 2, Hh - 18
    for y in range(Hh):
        for x in range(W):
            px, py = x + 0.5, y + 0.5
            u = min(1.0, max(0.0, (hy - py) / (hy - 4)))   # 0 at the head, 1 at the far end of the tail
            if py > hy:
                r = math.hypot(px - hx, (py - hy) * 1.3)
                core = math.exp(-(r / 3.2) ** 2)
                halo = math.exp(-(r / 9.0) ** 2) * 0.6
            else:
                wd = 6.5 * (1 - u) ** 0.8 + 0.8
                wob = 0.6 * math.sin(py * 0.15) * u
                d = abs(px - hx - wob)
                fade = (1 - u) ** 1.25
                flick = 0.8 + 0.4 * fbm(x, y, 4502, W, Hh, 3, 8.0)
                core = math.exp(-(d / (wd * 0.45)) ** 2) * fade * flick
                halo = math.exp(-(d / (wd * 1.2)) ** 2) * fade * 0.55 * flick
                r = math.hypot(px - hx, py - hy)
                core = max(core, math.exp(-(r / 3.2) ** 2))
                halo = max(halo, math.exp(-(r / 9.0) ** 2) * 0.6)
            v = min(1.0, core + halo)
            if v < 0.02:
                continue
            col = mix(hexc("#3a6fd8"), hexc("#bfe2ff"), min(1.0, halo * 1.4))
            col = mix(col, hexc("#ffffff"), min(1.0, core * 1.2))
            t.set(x, y, (col[0], col[1], col[2], int(255 * v)))
    return t


# --- output -------------------------------------------------------------------------------------------------------

def textures():
    out = {}
    m = None
    for name in VESSELS:
        m = rig(name)
        out[name] = m.build(gutter=1, seed=7000)
    return m, out


def generate():
    m, tex = textures()
    m.rig.write(GEO + "angel.geo.json")
    for name, (t, g) in tex.items():
        save(t, "entity", name)
        save(g, "entity", f"{name}_glowmask")
    f, _ = anims()
    f.write(ANIM + "angel.animation.json")
    save(wing_scorch(), "misc", "wing_scorch")
    save(fall_streak(), "misc", "fall_streak")


def preview(out_dir, names=None, clip_times=(("idle", 0.0),)):
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    _, clips = anims()
    by = {c.anim.name.split(".")[-1]: c for c in clips}
    for name in names or VESSELS:
        m = rig(name)
        t, g = m.build(gutter=1, seed=7000)
        for clip, tm in clip_times:
            pose = by[clip].pose_at(tm)
            hidden = ("wings",) if (clip in ("idle", "walk") and not VESSELS[name]["fallen"]) else ()
            preview3d.render(m.rig, t, os.path.join(out_dir, f"{name}_{clip}.png"), pose=pose, hidden=hidden, scale=4)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "preview":
        names = sys.argv[3].split(",") if len(sys.argv) > 3 else None
        clips = [(c.split(":")[0], float(c.split(":")[1])) for c in sys.argv[4].split(",")] if len(sys.argv) > 4 else [("idle", 0.0)]
        preview(sys.argv[2], names, clips)
    else:
        generate()
