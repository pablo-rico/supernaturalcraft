"""The Archangel Raphael (v0.16): his season-five vessel, two pairs of storm-cloud wings, veins of lightning.

A tall Black man (33 px to the crown, a head taller than a hunter), head shaved smooth, a stern, unmoving face: low
brows, a hard mouth. A charcoal suit, white shirt and dark tie under a long black wool overcoat that falls below his
knees (open, wide lapels, a turned collar); polished black shoes. His eyes hold a faint blue-white light (glowmask).

`wings` (shown only while the synced WINGS flag is set): two pairs of STORM-CLOUD wings -- each feather a plane of
dark slate cloud, billowing and soft at the edges, cut through by a jagged vein of lightning (blue-white, full bright
in the glowmask) -- along a leading edge of heaped cloud. Each wing folds at its wrist (`<wing>_tip`) and every flight
feather is its own bone (`<wing>_f00..f09`, children of the wing or its tip) so clips spread, fold and ruffle them.

`veins` (shown only in phase III, with its siblings `veins_<limb>`): a skin-tight shell over every part of him, clear
but for branching lines of lightning (glowmask) -- the grace burning through the vessel -- and his eyes blazing white.
`palm_light` (a disc of lightning on his right palm) the renderer shows only while `smite` plays.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- veins_head
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_middle, right_thumb, palm_light
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_thumb
               -- coat_front_r, coat_front_l, coat_back (the overcoat's skirt, swinging with the legs)
               -- wings -- wing_r1, wing_l1 (upper pair), wing_r2, wing_l2 (lower) -- <wing>_tip -- feathers
               -- veins;  every limb -- veins_<limb>
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math
import random

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from gabriel_art import aim_bone, open_shell, reach
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, LIGHT, SHIRT, TIE, Clip, P_, edge_cube, fabric,
                         mirror_rot, ruffle, tone, write_compact)
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.raphael."

# --- proportions (a tall man: 33 px to the crown) -----------------------------------------------------------------
HIP = 13.0
NECK = 25.0
TW = 4.0        # torso half-width
AW = 3.6        # sleeve width
AX = 5.9        # arm centre |x|
SX = 5.5        # shoulder pivot |x|
SY = 24.0
ELBOW = 19.0
WRIST = 14.6
PALM = 12.6     # bottom of the palm
LX = 2.0
LW = 3.8
KNEE = 6.8
SHOE_H = 1.6
COAT_HEM = 3.6  # the overcoat's hem, below the knees
PIV = {
    "body": (0, HIP, 0),
    "right_arm": (-SX, SY, 0), "right_forearm": (-AX, ELBOW, 0), "right_hand": (-AX, WRIST, 0),
    "left_arm": (SX, SY, 0), "left_forearm": (AX, ELBOW, 0), "left_hand": (AX, WRIST, 0),
    "right_leg": (-LX, HIP, 0), "right_shin": (-LX, KNEE, 0),
    "left_leg": (LX, HIP, 0), "left_shin": (LX, KNEE, 0),
}
GRIP = {"right": (-AX, PALM - 0.4, -0.2), "left": (AX, PALM - 0.4, -0.2)}

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#1c120e", "#2a1b15", "#3b271e", "#4c3328", "#5f4133", "#745242")
LIPS = Ramp("#1e0f0b", "#2c1712", "#3b2019", "#4b2a21", "#5c352b", "#6e4236")
EYE_WHITE = hexc("#cfc8c0")
IRIS = hexc("#1d140f")
EYE_LIGHT = hexc("#bfe3ff")
SUIT = Ramp("#11141a", "#1a1e26", "#242a34", "#2f3642", "#3c4452", "#4e5767")
COAT = Ramp("#060607", "#0c0c0f", "#131317", "#1b1c21", "#25262c", "#323440")
LINING = Ramp("#0b0a10", "#13111b", "#1c1927", "#262234", "#312c42", "#3f3953")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
# The storm: slate cloud, the lightning in it.
CLOUD = Ramp("#14171d", "#1e222a", "#2a2f39", "#383e4a", "#4a515f", "#626a7a")
BOLT = Ramp("#4a78d8", "#6f9cf0", "#9fc4ff", "#c9e0ff", "#e8f3ff", "#ffffff")
WHITE = hexc("#ffffff")


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(seed, shine=False):
    """Dark skin, smooth; `shine` adds the light on a shaved scalp (the top face and the forehead)."""
    base = H.skin_mat(SKIN, seed, 3.0, 0.45)

    def m(f, x, y, w, h):
        c = base(f, x, y, w, h)
        if shine:
            if f == "up":
                d = math.hypot(x - (w - 1) * 0.45, y - (h - 1) * 0.55) / (w / 2)
                c = mix(c, SKIN[5], max(0.0, 0.55 - 0.5 * d))
            elif f in ("east", "west", "south") and y < 3:
                c = mix(c, SKIN[5], 0.25 * (3 - y) / 3)
            elif f == "north" and y < 5:
                d = math.hypot((x - (w - 1) * 0.42) / (w * 0.4), (y - 1.5) / 3.0)
                c = mix(c, SKIN[5], max(0.0, 0.42 * (1 - d)))
        return c
    return m


def wool_m(ramp, seed, base=3.0, folds=0.3):
    """Heavy overcoat wool: a fine twill, soft vertical drape folds, top-lit."""
    def m(f, x, y, w, h):
        if f == "up":
            return tone(ramp, base + 0.5)
        if f == "down":
            return tone(ramp, base - 1.3)
        t = y / max(1, h - 1)
        n = fbm(x * 1.5 + len(f) * 9, y * 0.5, seed, 64, 64, 2, 8.0)
        v = base + 0.45 - 0.75 * t + folds * math.sin(x * 1.1 + seed * 0.7) + (n - 0.5) * 0.35
        if (x + y) % 3 == 0:
            v += 0.18
        if f in ("east", "west"):
            v -= 0.25
        elif f == "south":
            v -= 0.12
        return tone(ramp, v)
    return m


def suit_m(seed, base=3.0, crease=False):
    return fabric(SUIT, seed, base, 0.22, crease=crease)


def shoe_m(seed):
    """Polished black oxfords: a mirror shine on the toe cap, the welt, laces."""
    def m(f, x, y, w, h):
        if f == "down":
            return SHOE[0]
        if f != "up" and y >= h - 1:
            return SHOE[1]
        if f == "up":
            fz = y / max(1, h - 1)
            if 0.45 < fz < 0.8 and 2 <= x <= w - 3 and y % 2 == 0:
                return SHOE[3]
            if fz < 0.3 and abs(x - (w - 1) / 2) < 1.1:
                return SHOE[4] if fz < 0.12 else SHOE[3]
            return SHOE[2 + (fz < 0.4)]
        if f == "north":
            return SHOE[4] if y == 0 and abs(x - (w - 1) / 2) < 1.5 else SHOE[2]
        return SHOE[2] if y else SHOE[4]
    return m


# =====================================================================================================
# Lightning: veins for the feathers and for the burning vessel
# =====================================================================================================

_BOLTS = {}


def bolt_texels(seed, w, h, n=1, branch=0.1, x0=None, wander=0.45, start=0, stop=None, branches_max=3):
    """A jagged bolt (or `n`) running down a w x h face from row `start`: a walk that steps down a row at a time,
    drifting sideways, forking now and then. Returns the set of texels (cached)."""
    key = (seed, w, h, n, branch, x0, wander, start, stop, branches_max)
    if key in _BOLTS:
        return _BOLTS[key]
    rng = random.Random(seed)
    pts = set()
    stop = h if stop is None else stop
    for i in range(n):
        x = rng.uniform(w * 0.25, w * 0.75) if x0 is None else x0
        stack = [(x, start, rng.choice((-1, 1)), stop - start)]
        forks = 0
        while stack:
            x, y, drift, L = stack.pop()
            for _ in range(L):
                if not (0 <= y < h):
                    break
                xi = int(round(x))
                if 0 <= xi < w:
                    pts.add((xi, y))
                if rng.random() < wander:
                    nx = x + drift * rng.choice((1, 1, 2))
                    if 0 <= int(round(nx)) < w:
                        pts.add((int(round(nx)), y))
                    x = nx
                if rng.random() < 0.18:
                    drift = -drift
                x = max(0.0, min(w - 1.0, x))
                y += 1
                if rng.random() < branch and forks < branches_max:
                    forks += 1
                    stack.append((x, y, -drift, max(2, int(L * rng.uniform(0.2, 0.45)))))
    _BOLTS[key] = pts
    return pts


def near(pts, x, y):
    return (x - 1, y) in pts or (x + 1, y) in pts or (x, y - 1) in pts or (x, y + 1) in pts


class Veins:
    """The shell of lightning over a part of him (phase III): clear but for branching lines of light; their cores full
    bright, a pale halo round them. One field per face, seeded by the part and the face."""

    def __init__(self, seed, per=60):
        self.seed, self.per = seed, per

    def pts(self, face, w, h):
        if face == "down":
            return set()
        n = max(1, (w * h) // (self.per * 4))
        if face == "up":
            n = 1
        return bolt_texels(self.seed * 13 + len(face) * 7 + (face == "north") * 101, w, h, n, 0.1, wander=0.35, branches_max=2)

    def mat(self, face, x, y, w, h):
        p = self.pts(face, w, h)
        if (x, y) in p:
            return BOLT[5] if h01("vc", self.seed, face, x, y) < 0.5 else BOLT[4]
        return None

    @staticmethod
    def glow(face, x, y, w, h, c):
        return c


class StormFeather:
    """A feather of storm cloud on a flat plane (y = 0 at the root): dark slate cloud heaped in soft lumps, lighter on
    their tops, dense along the shaft and thinning to wisps at the edges and the tip, and a vein of lightning running
    down it (jagged, forking) -- full bright in the glowmask, with a pale halo. `broad` gives the rounder covert shape
    (a puff of cloud, a fainter vein)."""

    def __init__(self, seed, alpha=225, broad=False, vein=True):
        self.seed, self.alpha, self.broad, self.vein = seed, alpha, broad, vein

    def half(self, t, w):
        hw = (w - 1) / 2 + 0.3
        if self.broad:
            return hw * (math.sqrt(max(0.0, 1 - ((t - 0.45) / 0.55) ** 2)) if t > 0.45 else min(1.0, 0.65 + t))
        if t < 0.1:
            return hw * (0.6 + 4.0 * t)
        return hw * (1.0 if t < 0.5 else max(0.1, 1 - (t - 0.5) / 0.5))

    def bolt(self, w, h):
        if not self.vein:
            return set()
        c = (w - 1) / 2
        return bolt_texels(self.seed, w, h, 1, 0.07 if not self.broad else 0.0, x0=c + (h01("bx", self.seed) - 0.5) * w * 0.3,
                           wander=0.4, start=int(h * 0.12), stop=int(h * (0.85 if not self.broad else 0.55)), branches_max=2)

    def mat(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        hw = self.half(t, w)
        dx = abs(x - c)
        # Lumpy cloud edges: the outline breathes with the noise.
        n = fbm(x * 3 + len(face), y * 0.9, self.seed, 64, 64, 3, 6.0)
        if dx > hw * (0.8 + 0.35 * n):
            return None
        if t > 0.7 and h01("wisp", self.seed, face, x, y) < (t - 0.7) * 2.0:
            return None
        bolt = self.bolt(w, h)
        if (x, y) in bolt:
            return BOLT[5] if t < 0.6 else BOLT[4]
        q = dx / max(0.5, hw)
        # Billows: a slow second noise lights the "tops" of the lumps.
        b = fbm(x * 1.2 + 7, y * 0.45, self.seed + 3, 64, 64, 3, 4.0)
        v = 1.5 + 3.0 * b + (0.5 if self.broad else 0) - 0.7 * q
        col = tone(CLOUD, v)
        if near(bolt, x, y):
            col = mix(col, BOLT[1], 0.35)
        a = self.alpha * (1.0 - 0.4 * q * q) * (1 - 0.3 * t) * (0.8 + 0.35 * n)
        if q < 0.15 and t < 0.8:
            a += 25
        # The leading rim catches the lightning's light, faintly.
        if q > 0.85 and t < 0.6:
            col = mix(col, BOLT[0], 0.25)
        return (col[0], col[1], col[2], max(18, min(235, int(a))))

    @staticmethod
    def glow(face, x, y, w, h, col):
        if col[2] >= 230 and col[0] >= 180:
            return (col[0], col[1], col[2], 255)
        if col[2] > col[0] + 40 and col[2] > 120:
            return shade(col, 0.6)
        return None


def storm_glow(face, x, y, w, h, col):
    return StormFeather.glow(face, x, y, w, h, col)


# =====================================================================================================
# The face (north face of the head, 16 x 16 texels; x = 0 is his right)
# =====================================================================================================

FACE = [
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbbBBssssssBBbbs",
    "ssssBBsKKsBBssss",
    "sswEEwssssw EEwss".replace(" ", ""),
    "ssuuuussssuuuuss",
    "sssssssnnsssssss",
    "sssssssnnsssssss",
    "sssssNNnnNNsssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "jsssssMMMMsssssj",
    "jjssssssssssssjj",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


# =====================================================================================================
# The rig
# =====================================================================================================

FLIGHT, COVERT = 10, 6
TIP_FROM = 5         # flight feathers from this index sit on the wing's tip bone
WING_SPECS = {
    # pair: (root y, root |x|, leading edge (out, rise) px, feather scale, rest rotation of the right wing)
    1: (22.0, 1.4, (32.0, 9.0), 1.0, (0, 22, 12)),
    2: (18.5, 1.6, (26.0, -3.0), 0.8, (0, 30, -12)),
}
LIMB_BONES = ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand", "right_leg", "right_shin",
              "left_leg", "left_shin")
VEIN_BONES = ("veins", "veins_head") + tuple(f"veins_{b}" for b in LIMB_BONES)


def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", PIV["body"], "root")
    m.bone("head", (0, NECK, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", PIV[f"{side}_arm"], "body")
        m.bone(f"{side}_forearm", PIV[f"{side}_forearm"], f"{side}_arm")
        m.bone(f"{side}_hand", PIV[f"{side}_hand"], f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, PALM, 0), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 1.0 * s, PALM + 1.0, -1.3), f"{side}_hand")
    m.bone("right_middle", (-AX, PALM, -0.25), "right_hand")
    m.bone("palm_light", (-AX + 1.4, PALM + 1.0, 0), "right_hand")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_leg", PIV[f"{side}_leg"], "root")
        m.bone(f"{side}_shin", PIV[f"{side}_shin"], f"{side}_leg")
    m.bone("coat_front_r", (-2.0, HIP, -2.0), "body")
    m.bone("coat_front_l", (2.0, HIP, -2.0), "body")
    m.bone("coat_back", (0, HIP, 2.2), "body")
    # The veins: one shell per part, so each follows its limb; the renderer shows them all or none.
    m.bone("veins", (0, HIP, 0), "body")
    m.bone("veins_head", (0, NECK, 0), "head")
    for b in LIMB_BONES:
        m.bone(f"veins_{b}", m.rig.get(b).pivot, b)


def rig():
    m = Model("raphael", 512, 512)
    skeleton(m)
    build_head(m)
    build_body(m)
    build_arms(m)
    build_legs(m)
    build_coat_skirt(m)
    build_veins(m)
    build_wings(m)
    return m


# --- head -------------------------------------------------------------------------------------------------------------

def build_head(m):
    glow = H.glow_faint(0.08)
    sk = skin_m(16101, shine=True)
    plain = skin_m(16102)

    def eye_glow(f, x, y, w, h, c):
        if c == IRIS:
            return shade(EYE_LIGHT, 0.45)       # a faint light behind the eyes, always
        if c == EYE_WHITE:
            return shade(EYE_LIGHT, 0.12)
        return shade(c, 0.08)

    shine = lambda f, x, y, w, h: mix(sk(f, x, y, w, h), SKIN[5], 0.3)
    legend = {"s": sk, "f": shine, "b": SKIN[1], "B": SKIN[0], "K": SKIN[2], "w": EYE_WHITE, "E": IRIS, "u": SKIN[1],
              "n": SKIN[4], "N": SKIN[0], "m": LIPS[1], "M": LIPS[3], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.85)}
    H.head_cube(m, FACE, legend, H.head_sides(sk, None, top=0, back=0, temple=7, sideburn=0), glow=eye_glow, y0=NECK)
    hd = NECK + 8
    # The crown, rounded: a low cap over the top, the back of the skull a little fuller.
    m.cube("head", (-3.5, hd, -3.5), (7, 0.5, 7), none_on(("down",), sk), glow, density=2, tag="crown")
    # A heavy brow ridge (the stern line over his eyes), the nose, the ears, the jaw's line.
    for s in (-1, 1):
        m.cube("head", (s * 2.0 - 1.5, NECK + 5.0, -4.3), (3.0, 0.5, 0.3), lambda f, x, y, w, h: SKIN[3] if f == "up" else SKIN[2],
               glow, density=2, tag="brow", rotation=(0, 0, -9 * s), pivot=(s * 0.5, NECK + 5.0, -4.2))
    m.cube("head", (-0.75, NECK + 2.5, -4.6), (1.5, 2.0, 0.6), lambda f, x, y, w, h: (SKIN[4] if y < 2 else SKIN[3]) if f == "north"
           else (SKIN[0] if f == "down" else SKIN[2]), glow, density=2, tag="nose")
    for s in (-1, 1):
        m.cube("head", (4 * s - 0.25 + (0 if s > 0 else -0.25), NECK + 2.5, -0.5), (0.5, 2.2, 1.5),
               lambda f, x, y, w, h: SKIN[2] if f not in ("east", "west") else SKIN[3], glow, density=2, tag="ear")
    m.cube("body", (-1.7, NECK - 1.0, -1.7), (3.4, 1.2, 3.4), plain, glow, density=2, tag="neck")


# --- torso: shirt, tie, suit jacket, the overcoat ----------------------------------------------------------------------

def build_body(m):
    glow = H.glow_faint(0.08)
    shirt = fabric(SHIRT, 16201, 3.5, 0.12)
    suit = suit_m(16202)
    coat = wool_m(COAT, 16203)

    def tie(f, x, y, w, h):
        return TIE[4] if x == 7 or (y + x) % 7 == 0 else TIE[2]   # a sheen and a faint woven stripe
    # Only the shirt front shows (the rest stays dark, under the jacket).
    m.cube("body", (-TW, HIP, -2), (2 * TW, NECK - HIP, 4), lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else SUIT[1],
           glow, density=2, tag="torso")
    # The suit jacket (buttoned, its V showing shirt and tie).
    front = H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 10), v_depth=11, buttons=SUIT[0], button_rows=(13, 17))
    m.cube("body", (-TW, HIP - 0.4, -2), (2 * TW, NECK - HIP + 0.2, 4), none_on(("down",), front), glow, inflate=0.3, density=2,
           tag="jacket")
    m.cube("body", (-0.6, NECK - 2.2, -2.62), (1.2, 1.0, 0.4), solid(TIE[3]), glow, density=4, tag="tie_knot")
    for s in (-1, 1):
        m.cube("body", (0.9 * s - 0.75, NECK - 1.3, -2.45), (1.5, 1.25, 0.4), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               glow, density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, NECK - 0.2, -2.3))
    # The overcoat over it, worn open: wide in front, straight down.
    m.cube("body", (-TW, HIP - 0.6, -2), (2 * TW, NECK - HIP + 0.4, 4),
           open_shell(coat, 2.3, 2.6, 6, lapel=COAT[4]), glow, inflate=0.8, density=2, tag="coat")
    # Its collar, standing a little at the back of the neck, open at the throat.
    def collar(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 3.4 and (f == "north" or y < h * 0.65):
            return None
        return tone(COAT, 3.6 - 0.8 * y / max(1, h))
    m.cube("body", (-TW, NECK - 1.4, -2), (2 * TW, 2.0, 4), collar, glow, inflate=1.15, density=2, tag="coat_collar")
    # Broad notch lapels of the overcoat laid back over the chest, a seam rolling along their edge.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            c = tone(COAT, 4.3 - 1.0 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = COAT[2]
            if f == "north" and y < 2 and (x >= w - 1 if s < 0 else x == 0):
                return None
            if f == "south":
                return LINING[2]
            return c
        m.cube("body", (3.0 * s - 1.4, 15.0, -3.35), (2.8, 9.0, 0.5), lapel, glow, density=2, tag="coat_lapel",
               rotation=(4, 0, 9 * s), pivot=(2.2 * s, 15.0, -3.1))
        m.cube("body", (3.9 * s - 0.8, 22.3, -3.4), (1.6, 1.4, 0.45), lambda f, x, y, w, h: COAT[4] if f != "south" else LINING[2],
               glow, density=2, tag="lapel_notch", rotation=(0, 0, -35 * s), pivot=(3.9 * s, 22.3, -3.2))
        # Shoulder seams: the coat's set-in shoulders, a little wider than his own.
        m.cube("body", (s * (TW + 0.3) - 1.1, NECK - 1.2, -2.3), (2.2, 1.0, 4.6), lambda f, x, y, w, h: tone(COAT, 3.9 if f == "up" else 3.0),
               glow, density=2, tag="coat_shoulder")
    # A pocket square-less breast pocket welt on the jacket, the coat's flap pockets low on the hips.
    m.cube("body", (1.4, 19.8, -2.42), (2.0, 0.4, 0.2), lambda f, x, y, w, h: SUIT[4] if y == 0 else SUIT[1], glow, density=4,
           tag="breast_pocket")
    m.cube("body", (-TW, 18.5, 2.65), (2 * TW, 0.4, 0.3), lambda f, x, y, w, h: COAT[2], glow, density=2, tag="coat_yoke")


def build_arms(m):
    glow = H.glow_faint(0.08)
    coat = wool_m(COAT, 16301, 3.0, 0.25)
    hand = H.skin_mat(SKIN, 16302, 3.1, 0.4)
    shirt = fabric(SHIRT, 16303, 3.7, 0.1)
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.6 - ELBOW, AW), coat, glow, inflate=0.2, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.7, WRIST + 0.5, -1.7), (3.4, ELBOW - WRIST - 0.5, 3.4), none_on(("up",), coat), glow,
               inflate=0.2, density=2, tag="forearm_sleeve")
        # The sleeve's turned cuff band, and the white shirt cuff a finger's width below it.
        m.cube(f"{side}_forearm", (cx - 1.7, WRIST + 0.5, -1.7), (3.4, 0.8, 3.4), none_on(("up", "down"),
               lambda f, x, y, w, h: COAT[3] if y == 0 else COAT[2]), glow, inflate=0.32, density=2, tag="coat_cuff")
        m.cube(f"{side}_forearm", (cx - 1.5, WRIST - 0.1, -1.5), (3.0, 0.6, 3.0), none_on(("up",), lambda f, x, y, w, h: shirt(f, x, y, w, h)),
               glow, inflate=0.1, density=2, tag="shirt_cuff")
        # The hand: palm, a block of fingers curled under, a thumb at the front.
        m.cube(f"{side}_hand", (cx - 1.3, PALM, -1.45), (2.6, WRIST - PALM, 2.9), hand, glow, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.9 if (f in ("east", "west") and x % 2) else 1.0)
        if side == "right":
            m.cube("right_fingers", (cx - 1.25, PALM - 1.0, -1.4), (2.5, 1.0, 0.8), fing, glow, density=2, tag="index")
            m.cube("right_fingers", (cx - 1.25, PALM - 0.9, 0.2), (2.5, 1.0, 1.2), fing, glow, density=2, tag="ring")
            m.cube("right_middle", (cx - 1.25, PALM - 1.15, -0.6), (2.5, 1.15, 0.8), fing, glow, density=2, tag="middle")
        else:
            m.cube("left_fingers", (cx - 1.25, PALM - 1.0, -1.4), (2.5, 1.0, 2.8), fing, glow, density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 1.0 * s - 0.5, PALM + 0.2, -2.05), (1.0, 1.5, 0.9), hand, glow, density=2, tag="thumb")
    # The disc of lightning on his right palm (the smite): faces his left when the arm hangs.
    pl = m.rig.get("palm_light")

    def disc(f, x, y, w, h):
        r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / (w / 2)
        if r > 1.0:
            return None
        bolts = bolt_texels(16310, w, h, 3, 0.2, wander=0.6)
        if (x, y) in bolts or r < 0.3:
            return WHITE
        return (BOLT[3][0], BOLT[3][1], BOLT[3][2], int(230 - 150 * r))
    m.cube(pl, (-AX + 1.36, PALM - 0.7, -1.9), (0, 3.6, 3.6), disc, lambda f, x, y, w, h, c: (c[0], c[1], c[2], 255),
           faces=("west",), density=4, tag="palm_light")


def build_legs(m):
    glow = H.glow_faint(0.08)
    for side, s in (("right", -1), ("left", 1)):
        tr = suit_m(16401 + (s > 0), 2.9, crease=True)
        x0 = LX * s - LW / 2
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), tr, glow, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, SHOE_H, -LW / 2), (LW, KNEE - SHOE_H, LW), tr, glow, density=2, tag="shin")
        # The trouser hem breaking on the shoe, and the shoe.
        m.cube(f"{side}_shin", (x0, SHOE_H, -LW / 2), (LW, 0.8, LW), none_on(("up", "down"), lambda f, x, y, w, h, tr=tr: shade(tr(f, x, y + 8, w, h), 0.85)),
               glow, inflate=0.18, density=2, tag="hem")
        m.cube(f"{side}_shin", (x0 - 0.1, 0, -2.9), (LW + 0.2, SHOE_H, 4.9), shoe_m(16410 + (s > 0)),
               lambda f, x, y, w, h, c: shade(c, 0.12), density=2, tag="shoe")


def build_coat_skirt(m):
    """The overcoat below the waist: two front panels (each wrapping round its side) and the back with a vent, hanging from
    the hips to below the knees, the lining showing inside."""
    glow = H.glow_faint(0.08)
    L = HIP + 0.2 - COAT_HEM
    for side, s, bone in (("r", -1, "coat_front_r"), ("l", 1, "coat_front_l")):
        coat = wool_m(COAT, 16501 + (s > 0), 2.9, 0.35)

        def front(f, x, y, w, h, coat=coat, s=s):
            if f == "south":
                return tone(LINING, 2.6 - 0.8 * y / max(1, h))
            c = coat(f, x, y, w, h)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return COAT[4]               # the front edge, catching the light
            if y >= h - 1:
                return COAT[1]
            if f == "north" and 2 <= y <= 3 and 2 <= (w - 1 - x if s < 0 else x) <= w - 3:
                return COAT[1] if y == 3 else COAT[3]   # a flap pocket
            return c
        # Front panel: from the side seam to just short of the middle (the coat hangs open).
        xa, xb = (-TW - 1.0, -0.9) if s < 0 else (0.9, TW + 1.0)
        m.cube(bone, (xa, COAT_HEM, -2.95), (xb - xa, L, 0.45), front, glow, density=2, tag="coat_skirt",
               rotation=(-4, 0, 0), pivot=(0, HIP, -2.7))
        # Side panel, wrapping back to the back panel.
        xs = -TW - 1.0 if s < 0 else TW + 0.55
        m.cube(bone, (xs, COAT_HEM, -2.6), (0.45, L, 5.2), none_on(("up",), coat), glow, density=2, tag="coat_side",
               rotation=(0, 0, 3 * s), pivot=(xs, HIP, 0))
    back = wool_m(COAT, 16503, 2.8, 0.4)

    def bk(f, x, y, w, h):
        if f == "north":
            return tone(LINING, 2.4)
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.45:
            return COAT[0]                    # the vent
        if y >= h - 1:
            return COAT[1]
        return back(f, x, y, w, h)
    m.cube("coat_back", (-TW - 1.0, COAT_HEM, 2.6), (2 * TW + 2.0, L, 0.45), bk, glow, density=2, tag="coat_back",
           rotation=(5, 0, 0), pivot=(0, HIP, 2.6))


# --- the veins of light (phase III) ------------------------------------------------------------------------------------

def build_veins(m):
    v = lambda seed, per=170: (Veins(seed, per).mat, Veins.glow)
    mat, g = v(16601)
    m.cube("veins", (-TW, HIP - 0.6, -2), (2 * TW, NECK - HIP + 0.4, 4), mat, g, inflate=0.9, density=4, tag="veins")
    mat, g = v(16602, 90)
    m.cube("veins_head", (-4, NECK, -4), (8, 8, 8), mat, g, inflate=0.06, density=4, tag="veins")
    # His eyes blazing white.
    for x0 in (-2.5, 1.5):
        m.cube("veins_head", (x0, NECK + 3.5, -4.14), (1.0, 0.5, 0), lambda f, x, y, w, h: WHITE, lambda f, x, y, w, h, c: WHITE,
               faces=("north",), density=2, tag="eye_blaze")
        m.cube("veins_head", (x0 - 0.5, NECK + 3.25, -4.16), (2.0, 1.0, 0),
               lambda f, x, y, w, h: None if (y in (1, 2) and x in (1, 2)) else (BOLT[3][0], BOLT[3][1], BOLT[3][2], 150),
               lambda f, x, y, w, h, c: BOLT[2], faces=("north",), density=2, tag="eye_halo")
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        mat, g = v(16610 + (s > 0))
        m.cube(f"veins_{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.6 - ELBOW, AW), mat, g, inflate=0.3, density=4, tag="veins")
        mat, g = v(16612 + (s > 0))
        m.cube(f"veins_{side}_forearm", (cx - 1.7, WRIST - 0.1, -1.7), (3.4, ELBOW - WRIST + 0.1, 3.4), mat, g, inflate=0.42, density=4,
               tag="veins")
        mat, g = v(16614 + (s > 0), 120)
        m.cube(f"veins_{side}_hand", (cx - 1.3, PALM, -1.45), (2.6, WRIST - PALM, 2.9), mat, g, inflate=0.06, density=4, tag="veins")
        mat, g = v(16616 + (s > 0))
        m.cube(f"veins_{side}_leg", (LX * s - LW / 2, KNEE, -LW / 2), (LW, HIP - KNEE, LW), mat, g, inflate=0.08, density=4, tag="veins")
        mat, g = v(16618 + (s > 0))
        m.cube(f"veins_{side}_shin", (LX * s - LW / 2, SHOE_H, -LW / 2), (LW, KNEE - SHOE_H, LW), mat, g, inflate=0.1, density=4,
               tag="veins")


# --- the storm-cloud wings ---------------------------------------------------------------------------------------------

def feather_bone(m, parent, name, root, length, width, angle, s, mat, glow, cup=0.0, twist=0.0, tag="storm_feather"):
    x, y, z = root
    b = m.bone(name, (x, y, z), parent, rotation=(cup, -s * twist, -s * angle))
    m.cube(b, (x - width / 2, y - length, z), (width, length, 0), mat, glow, density=2, tag=tag, faces=("north", "south"))
    return b


def covert(m, bone, root, length, width, angle, s, mat, glow, tilt=0.0, tag="storm_covert"):
    x, y, z = root
    return m.cube(bone, (x - width / 2, y - length, z), (width, length, 0), mat, glow, density=2, tag=tag,
                  rotation=(tilt, 0, -s * angle), pivot=(x, y, z), faces=("north", "south"))


def build_wings(m):
    w_all = m.bone("wings", (0, 20.0, 2.6), "body")
    for pair, (ry, rx, (out, rise), k, rest) in WING_SPECS.items():
        for side, s in (("r", -1), ("l", 1)):
            name = f"wing_{side}{pair}"
            root = (rx * s, ry, 2.9 + 0.2 * pair)
            w = m.bone(name, root, w_all.name, rotation=mirror_rot(rest, s))
            x0, y0, z0 = root
            wx, wy = x0 + s * out * 0.5, y0 + rise * 0.5
            tip = m.bone(f"{name}_tip", (wx, wy, z0), name)
            seed = 16700 + pair * 100 + (0 if s < 0 else 50)

            # The leading edge: a heaped bank of cloud, lit along its top, a thread of lightning inside it.
            def arm_m(f, x, y, w_, h, s=s, seed=seed):
                t = y / max(1, h - 1)
                q = x / max(1, w_ - 1)
                if (f == "north") == (s < 0):
                    q = 1 - q
                n = fbm(x * 0.9, y * 3, seed, 64, 64, 3, 5.0)
                top = 0.08 + 0.35 * fbm(x * 0.6 + 3, 1, seed + 9, 64, 64, 2, 3.0)     # lumpy upper edge
                if t < top or t > 0.62 + 0.3 * (1 - q) + 0.1 * n or q > 0.98:
                    return None
                col = tone(CLOUD, 2.0 + 2.8 * (1 - (t - top) * 2.2) + 0.8 * n)
                if abs(t - (0.42 + 0.08 * math.sin(x * 0.7 + seed))) < 0.05 and h01("arc", seed, x // 4) < 0.3:
                    return BOLT[4]             # lightning flickering through the cloud bank
                return (col[0], col[1], col[2], int(235 - 40 * q))
            for b, a_, b_ in ((w, (x0, y0, z0), (wx, wy, z0)), (tip, (wx, wy, z0), (x0 + s * out, y0 + rise, z0))):
                edge_cube(m, b, a_, b_, 5.0 * k, s, arm_m, storm_glow, tag="wing_arm", dz=-0.15)
            for i in range(FLIGHT):
                f = i / (FLIGHT - 1)
                t = 0.1 + 0.9 * f
                px, py = x0 + s * out * t, y0 + rise * t
                length = (17 + 15 * f) * k * (0.92 + 0.16 * h01("fl", seed, i))
                angle = 5 + 82 * f ** 1.25 + (h01("fa", seed, i) - 0.5) * 6
                parent = tip if i >= TIP_FROM else w
                feather_bone(m, parent.name, f"{name}_f{i:02d}", (px, py - 0.6, z0 + 0.12 * i), length, 7.5 * k, angle, s,
                             StormFeather(seed + i, 232, vein=h01("vein", seed, i) < 0.45).mat, storm_glow, cup=4.0 * f, twist=6.0 * f)
            for j in range(COVERT):
                f = j / (COVERT - 1)
                t = 0.04 + 0.82 * f
                px, py = x0 + s * out * t, y0 + rise * t
                bone = tip if t >= 0.5 else w
                covert(m, bone, (px, py + 0.5, z0 + 1.4), (10 + 4 * f) * k, 8.5 * k, 16 + 42 * f, s,
                       StormFeather(seed + 30 + j, 238, broad=True, vein=j == 2).mat, storm_glow)
                # A second, smaller row of cloud along the edge (the marginal coverts).
                covert(m, bone, (px + s * 1.5, py + 1.2, z0 + 1.9), (6 + 2 * f) * k, 7.0 * k, 10 + 30 * f, s,
                       StormFeather(seed + 50 + j, 238, broad=True, vein=False).mat, storm_glow, tag="storm_marginal")
            # Heaped cloud along the top of the leading edge: billows rising over the wing's arm.
            for j in range(5):
                f = j / 4
                t = 0.08 + 0.8 * f
                px, py = x0 + s * out * t, y0 + rise * t
                bone = tip if t >= 0.5 else w
                covert(m, bone, (px, py - 2.0, z0 - 0.3 - 0.2 * (j % 2)), (9.5 - 3.5 * f + 2.0 * (j % 2)) * k, (11 - 4 * f) * k,
                       180 - 25 * f + (h01("pa", seed, j) - 0.5) * 30, s,
                       StormFeather(seed + 70 + j, 250, broad=True, vein=False).mat, storm_glow, tag="storm_billow")


# =====================================================================================================
# Animations
# =====================================================================================================

WING_BONES = [f"wing_{sd}{p}" for p in (1, 2) for sd in ("r", "l")]
FEATHERS = [f"wing_{sd}{p}_f{i:02d}" for p in (1, 2) for sd in ("r", "l") for i in range(FLIGHT)]

WING_POSES = {
    # right-wing absolute rotations per pair (the left mirrors them) and the tips' fold (degrees, + folds the tip down/in)
    "rest": ({1: (0, 22, 12), 2: (0, 30, -12)}, 0),
    "wide": ({1: (0, 4, 24), 2: (0, 10, -4)}, -6),
    "high": ({1: (0, 14, 46), 2: (0, 20, 16)}, -10),
    "fold": ({1: (0, 80, -25), 2: (0, 85, -40)}, 60),
    "wrap": ({1: (0, -26, 4), 2: (0, -22, -24)}, 30),
    "flick": ({1: (0, 50, 30), 2: (0, 56, 6)}, 30),
}


def wing_pose(m, name, fan=1.0):
    tgt, tipfold = WING_POSES[name]
    out = {}
    for pair in (1, 2):
        for side, s in (("r", -1), ("l", 1)):
            n = f"wing_{side}{pair}"
            rest = m.rig.get(n).rotation
            t = mirror_rot(tgt[pair], s)
            out[n] = [t[i] - rest[i] for i in range(3)]
            out[f"{n}_tip"] = [0, 0, s * tipfold] if tipfold else [0, 0, 0]
    if fan != 1.0:
        for n in FEATHERS:
            rz = m.rig.get(n).rotation[2]
            out[n] = [0, 0, rz * (fan - 1)]
    return out


def wing_breath(c, length, cycles, amp=2.5):
    """The wings breathe (each pair on its own phase), as Molang: they only show from the reveal on."""
    from michael_art import _g
    w = 360.0 * cycles / length
    for i, n in enumerate(WING_BONES):
        s = -1 if n[5] == "r" else 1
        ph = 60 * (i // 2)
        a = amp * (1.0 - 0.2 * (i // 2))

        def fn(t, ph=ph, a=a, s=s):
            v = a * math.sin(math.radians(w * t + ph))
            return [0, -s * v * 0.5, -s * v]
        sine = f"math.sin(query.anim_time*{_g(w)}+{ph})"
        c.mlayer(n, fn, [None, f"{sine}*{_g(round(-s * a * 0.5, 2))}", f"{sine}*{_g(round(-s * a, 2))}"])


FIST_R = {"right_fingers": [-80, 0, 0], "right_middle": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
FIST_L = {"left_fingers": [-80, 0, 0], "left_thumb": [-20, 0, -20]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_middle": [0, 0, 0], "right_thumb": [0, 0, -14]}
OPEN_L = {"left_fingers": [0, 0, 0], "left_thumb": [0, 0, 14]}
# He stands straight, hands loose at his sides, fingers a little curled.
STAND = {"right_fingers": [-25, 0, 0], "right_middle": [-25, 0, 0], "left_fingers": [-25, 0, 0], "right_arm": [0, 0, 3],
         "left_arm": [0, 0, -3]}


def coat_follow(pose, k=0.55):
    """The overcoat's skirt swings with the legs (front panels follow their leg forward; the back trails)."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])[0]
    out["coat_front_r"] = [min(0.0, rl) * k - body * 0.3, 0, 0]
    out["coat_front_l"] = [min(0.0, ll) * k - body * 0.3, 0, 0]
    out["coat_back"] = [-max(0.0, max(rl, ll)) * 0.3 - min(0.0, min(rl, ll)) * 0.25 - body * 0.3, 0, 0]
    return out


def kneel(head=15):
    return {"@root": [0, -5.9, 0], "left_leg": [-82, 0, -4], "left_shin": [82, 0, 0], "right_leg": [6, 0, 6], "right_shin": [86, 0, 0],
            "body": [16, 0, 0], "head": [head, 0, 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []
    W = lambda name, fan=1.0: wing_pose(m, name, fan)

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("palm_light",) + VEIN_BONES))
        return clips[-1]

    def K(c, t, pose, ease=None):
        c.key(t, coat_follow(pose), ease)

    # --- idle: stillness. He barely breathes, the head turning slowly, a hand closing and opening. ---
    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        K(c, t, P_(STAND, {"body": [0.6 * abs(k), 0, 0], "head": [-1 * abs(k), 6 * k, 0], "@body": [0, -0.12 * abs(k), 0]},
                   {"right_fingers": [-25 - 30 * max(0, k), 0, 0], "right_middle": [-25 - 30 * max(0, k), 0, 0]},
                   W("rest")), E_IO if t else None)
    wing_breath(c, 4.0, 2)
    ruffle(c, FEATHERS, 2.5, 2, 61)

    # --- walk: measured, unhurried; the coat swinging round his legs. ---
    L = 1.3
    c = clip("walk", L, loop=True)
    for i in range(5):
        t = L * i / 4
        ph = 2 * math.pi * i / 4
        sn = math.sin(ph)
        K(c, t, P_(STAND, {"right_leg": [-24 * sn, 0, 0], "left_leg": [24 * sn, 0, 0],
                           "right_shin": [30 * max(0, math.sin(ph + 1.2)), 0, 0], "left_shin": [30 * max(0, -math.sin(ph + 1.2)), 0, 0],
                           "right_arm": [14 * sn, 0, 3], "left_arm": [-14 * sn, 0, -3], "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0],
                           "body": [1.5, 3 * sn, 0], "head": [0, -3 * sn, 0], "@root": [0, 0.35 * abs(math.cos(ph)), 0]}, W("rest")),
          E_IO if i else None)
    wing_breath(c, L, 1, 2.0)

    # --- trapped: held in the holy fire. Hunched, arms drawn in, turning to find a way out, flinching from the flames. ---
    c = clip("trapped", 2.4, loop=True)
    held = P_({"body": [10, 0, 0], "head": [8, 0, 0], "right_arm": [-34, 20, 18], "right_forearm": [-62, 0, 0], "left_arm": [-34, -20, -18],
               "left_forearm": [-62, 0, 0], "right_leg": [-8, 0, 4], "left_leg": [4, 0, -4], "right_shin": [10, 0, 0], "@root": [0, -0.6, 0]},
              FIST_R, FIST_L, W("fold", 0.6))
    K(c, 0, held)
    K(c, 0.6, P_(held, {"head": [2, 32, 0], "body": [8, 14, 0]}), E_IO)
    K(c, 1.0, P_(held, {"head": [-10, 20, 0], "body": [-2, 8, 0], "right_arm": [-60, 30, 30]}), E_SNAP)
    K(c, 1.5, P_(held, {"head": [4, -30, 0], "body": [8, -14, 0]}), E_IO)
    K(c, 2.0, P_(held, {"head": [12, -6, 0], "body": [14, -4, 0], "left_arm": [-60, -30, -30]}), E_IO)
    K(c, 2.4, held, E_IO)
    c.layer("body", lambda t: [0.9 * math.sin(t * 2 * math.pi * 6 / 2.4), 0, 0])
    c.layer("head", lambda t: [0, 1.2 * math.sin(t * 2 * math.pi * 7 / 2.4), 0])
    ruffle(c, FEATHERS, 6.0, 4, 62)

    # --- smite: his right hand rises, palm out, and burns (SMITE_WINDUP = 24 ticks = 1.2 s); at 1.2 it closes into a
    # fist and he drives it down -- the burst. ---
    c = clip("smite", 2.0)
    base = P_(STAND, W("rest"))
    raise_ = P_(W("rest"), OPEN_R, {"right_arm": [-135, 0, -24], "right_forearm": [-14, 0, 0],
                                    "body": [-3, -10, 0], "head": [-4, 8, 0], "left_arm": [-6, 0, -6], "left_fingers": [-40, 0, 0],
                                    "right_leg": [6, 0, 0], "left_leg": [-10, 0, 0]})
    # The palm turned to face his foe, the fingers to the sky.
    raise_ = P_(raise_, {"right_hand": aim_bone(r, raise_, "right_hand", (1, 0, 0), (0, 0.2, -1), (0, -1, 0), (0, 1, 0.2))})
    close = P_(raise_, FIST_R, {"right_arm": [-126, 0, -22], "right_forearm": [-30, 0, 0], "body": [4, -10, 0], "head": [2, 8, 0]})
    drive = P_(W("wide"), FIST_R, {"right_arm": [-62, -6, 0], "right_forearm": [-12, 0, 0], "right_hand": [0, -80, 0], "body": [14, -6, 0],
                                   "head": [8, 4, 0], "left_arm": [8, 0, -14], "@root": [0, -0.9, -0.8], "right_leg": [-14, 0, 0],
                                   "left_leg": [-4, 0, 0], "right_shin": [18, 0, 0], "left_shin": [10, 0, 0]})
    K(c, 0, base)
    K(c, 0.35, raise_, E_OUT)
    K(c, 1.1, P_(raise_, {"right_arm": [-139, 0, -26], "head": [-6, 8, 0]}), E_IO)
    K(c, 1.2, close, E_SNAP)
    K(c, 1.32, drive, E_SNAP)
    K(c, 1.55, P_(drive, {"body": [10, -6, 0]}), E_OUT)
    K(c, 2.0, base, E_IO)
    c.layer("right_arm", lambda t: [1.4 * math.sin(t * 95) if 0.4 < t < 1.18 else 0, 0, 0.8 * math.sin(t * 71) if 0.4 < t < 1.18 else 0])

    # --- snap: the hand up by his shoulder, thumb to middle finger; the snap at 0.7; a cold beat. ---
    c = clip("snap", 1.4)
    up = reach(r, {"right_arm": [-34, 0, 30], "right_forearm": [-96, 0, 0], "head": [0, 6, 0], "body": [0, -4, 0],
                   "right_fingers": [-80, 0, 0], "right_middle": [-30, 0, 0], "right_thumb": [-30, 0, 30]}, "right",
               (-8.2, 27.0, -2.5), grip=GRIP["right"])
    up = P_(up, {"right_hand": aim_bone(r, up, "right_hand", (0, -1, 0), (0, 1, 0), (-1, 0, 0), (0.7, 0, -0.7))})
    snapped = P_(up, {"right_arm": [up["right_arm"][0] - 4, up["right_arm"][1], up["right_arm"][2] + 2],
                      "right_hand": [up["right_hand"][0] + 14, up["right_hand"][1], up["right_hand"][2]],
                      "right_middle": [-110, 0, 0], "right_thumb": [10, 0, 0], "head": [3, 4, 0]})
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.45, P_(up, W("rest")), E_OUT)
    K(c, 0.66, P_(up, W("rest"), {"right_middle": [-24, 0, 0], "right_thumb": [-36, 0, 34]}), E_IO)
    K(c, 0.7, P_(snapped, W("wide")), E_SNAP)
    K(c, 1.0, P_(snapped, W("rest")), E_OUT)
    K(c, 1.4, P_(STAND, W("rest")), E_IO)

    # --- call_lightning: both arms rise from his sides, palms up, the head tipping back to the sky (the strike at 1.0). ---
    c = clip("call_lightning", 2.0)
    lift = P_(OPEN_R, OPEN_L, W("high", 1.15), {"right_arm": [-20, 0, 112], "left_arm": [-20, 0, -112], "right_forearm": [-14, 0, 0],
                                                 "left_forearm": [-14, 0, 0], "right_hand": [0, -90, 0], "left_hand": [0, 90, 0],
                                                 "head": [-30, 0, 0], "body": [-6, 0, 0], "@root": [0, 0.4, 0]})
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.75, lift, E_IO)
    K(c, 1.0, P_(lift, {"right_arm": [-24, 0, 124], "left_arm": [-24, 0, -124], "head": [-36, 0, 0]}), E_SNAP)
    K(c, 1.5, P_(lift, {"head": [-32, 0, 0]}), E_IO)
    K(c, 2.0, P_(STAND, W("rest")), E_IO)
    ruffle(c, FEATHERS, 9.0, 4, 63, t0=0.6, t1=1.9)

    # --- heal_channel: his palms held out to his sides towards the threads, head bowed, still; held for 3 s. ---
    c = clip("heal_channel", 3.0)
    chan = P_(OPEN_R, OPEN_L, W("wide", 1.05), {"right_arm": [-46, 30, 56], "left_arm": [-46, -30, -56], "right_forearm": [-20, 0, 0],
                                                 "left_forearm": [-20, 0, 0], "right_hand": [0, -60, 0], "left_hand": [0, 60, 0],
                                                 "head": [16, 0, 0], "body": [2, 0, 0]})
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.6, chan, E_IO)
    K(c, 1.8, P_(chan, {"head": [20, 0, 0], "right_arm": [-50, 30, 60], "left_arm": [-50, -30, -60]}), E_IO)
    K(c, 2.5, chan, E_IO)
    K(c, 3.0, P_(STAND, W("rest")), E_IO)

    # --- blink: a flinch of wings and he is elsewhere -- the body jolts, wings flick in and out. ---
    c = clip("blink", 0.7)
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.12, P_(STAND, W("flick", 0.7), {"body": [8, 0, 0], "head": [6, 0, 0], "@root": [0, -0.5, 0], "right_arm": [-10, 0, 14],
                                           "left_arm": [-10, 0, -14]}), E_SNAP)
    K(c, 0.3, P_(STAND, W("wide", 1.15), {"body": [-4, 0, 0], "head": [-3, 0, 0]}), E_OUT)
    K(c, 0.7, P_(STAND, W("rest")), E_IO)

    # --- thunderclap: hands drawn apart, a single clap before his chest at 0.5, the shock rolling out. ---
    c = clip("thunderclap", 1.3)
    apart = P_(OPEN_R, OPEN_L, W("high"), {"right_arm": [-70, 40, 40], "left_arm": [-70, -40, -40], "right_forearm": [-20, 0, 0],
                                           "left_forearm": [-20, 0, 0], "right_hand": [0, -90, 0], "left_hand": [0, 90, 0], "body": [-6, 0, 0],
                                           "head": [-6, 0, 0]})
    clap = reach(r, {"right_arm": [-80, -20, -6], "right_forearm": [-20, 0, 0], "body": [8, 0, 0], "head": [4, 0, 0],
                     "right_hand": [0, -90, 0]}, "right", (-0.8, 20.0, -9.0), grip=GRIP["right"])
    clap = reach(r, P_(clap, {"left_arm": [-80, 20, 6], "left_forearm": [-20, 0, 0], "left_hand": [0, 90, 0]}), "left", (0.8, 20.0, -9.0),
                 grip=GRIP["left"])
    clap = P_(clap, OPEN_R, OPEN_L, W("wide", 1.2), {"@root": [0, -0.8, -0.6], "right_leg": [-12, 0, 0], "left_leg": [6, 0, 0],
                                                    "right_shin": [14, 0, 0]})
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.38, apart, E_OUT)
    K(c, 0.5, clap, E_SNAP)
    K(c, 0.75, P_(clap, {"body": [10, 0, 0]}), E_OUT)
    K(c, 1.3, P_(STAND, W("rest")), E_IO)

    # --- wings_reveal: the storm grows out of his back, sweeps open past its rest and settles; held. ---
    c = clip("wings_reveal", 2.5, hold=True)
    K(c, 0, P_(STAND, W("fold", 0.5), {"%wings": [0.05, 0.05, 0.05]}))
    K(c, 0.5, P_(STAND, W("fold", 0.6), {"%wings": [0.85, 0.85, 0.85], "body": [10, 0, 0], "head": [14, 0, 0]}), E_OUT)
    K(c, 1.2, P_(OPEN_R, OPEN_L, W("wide", 1.15), {"%wings": [1.12, 1.12, 1.12], "body": [-8, 0, 0], "head": [-12, 0, 0],
                                                   "right_arm": [-14, 0, 34], "left_arm": [-14, 0, -34]}), E_BACK)
    K(c, 1.8, P_(STAND, W("rest"), {"%wings": [1, 1, 1], "head": [-2, 0, 0]}), E_IO)
    K(c, 2.5, P_(STAND, W("rest"), {"%wings": [1, 1, 1]}), E_IO)
    for i, n in enumerate(FEATHERS):
        lag = 0.05 * (i % FLIGHT)
        c.layer(n, lambda t, lag=lag: [12 * math.sin(math.pi * min(1, max(0, (t - 0.8 - lag) / 0.6))), 0, 0])

    # --- stagger: struck; he rocks back a step and glares. ---
    c = clip("stagger", 0.8)
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.12, P_(STAND, W("flick", 0.8), {"body": [-14, 8, 0], "head": [-18, -10, 0], "right_arm": [-20, 0, 24], "left_arm": [-10, 0, -20],
                                           "@root": [0, 0, 1.6], "right_leg": [10, 0, 0], "left_leg": [-6, 0, 0]}), E_SNAP)
    K(c, 0.4, P_(STAND, W("rest"), {"body": [4, 0, 0], "head": [6, 4, 0], "@root": [0, 0, 1.0]}), E_OUT)
    K(c, 0.8, P_(STAND, W("rest")), E_IO)

    # --- death: (6 s, held) struck through; he staggers, falls to his knees, arms flung wide and head thrown back as the
    # light tears out of him (the explosion at ~4.0), the wings flaring huge. ---
    c = clip("death", 6.0, hold=True)
    K(c, 0, P_(STAND, W("rest")))
    K(c, 0.3, P_(W("flick"), {"body": [-16, 0, 0], "head": [-24, 0, 0], "right_arm": [-20, 0, 40], "left_arm": [-20, 0, -40], "@root": [0, 0, 1.6]}),
      E_SNAP)
    K(c, 1.2, P_(W("rest"), {"body": [12, 10, 0], "head": [16, 0, 0], "right_arm": [-4, 0, 10], "left_arm": [-30, 0, -10],
                             "left_forearm": [-40, 0, 0], "right_leg": [-12, 0, 0], "left_leg": [10, 0, 0], "@root": [0, -0.6, 1.2]}), E_IO)
    k2 = P_(kneel(30), {"right_arm": [6, 0, 8], "left_arm": [4, 0, -8], "body": [24, 0, 0], "@root": [0, -5.9, 1.2]})
    K(c, 2.2, P_(k2, W("fold", 0.7)), E_IN)
    burst = P_(kneel(-40), OPEN_R, OPEN_L, W("high", 1.3), {"body": [-18, 0, 0], "right_arm": [-30, 0, 104], "left_arm": [-30, 0, -104],
                                                            "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0], "@root": [0, -5.9, 1.2],
                                                            "%wings": [1.3, 1.3, 1.3]})
    K(c, 3.4, burst, E_OUT)
    K(c, 4.0, P_(burst, {"head": [-46, 0, 0], "%wings": [1.4, 1.4, 1.4]}), E_IO)
    K(c, 6.0, P_(burst, {"head": [-48, 0, 0], "%wings": [1.45, 1.45, 1.45]}))
    c.layer("body", lambda t: [2.0 * math.sin(t * 80) if 3.0 < t < 4.2 else 0, 0, 0])
    c.layer("head", lambda t: [0, 1.6 * math.sin(t * 91) if 3.0 < t < 4.2 else 0, 0])
    ruffle(c, FEATHERS, 12.0, 10, 64, t0=3.0, t1=5.6)

    # --- emerge: he comes down in a bolt: crouched on one knee, a fist to the floor; rises slowly, straightens the coat at
    # his collar, and looks up. ---
    c = clip("emerge", 3.2)
    land = P_(kneel(24), FIST_R, {"body": [30, 0, 0], "right_arm": [-40, 0, 8], "right_forearm": [-10, 0, 0], "left_arm": [-30, 0, -10],
                                  "left_forearm": [-50, 0, 0], "@root": [0, -6.3, 0]})
    land = reach(r, land, "right", (-4.0, -5.0, -5.5), grip=GRIP["right"])
    K(c, 0, P_(land, W("high", 1.2)))
    K(c, 0.3, P_(land, W("wide"), {"body": [26, 0, 0]}), E_OUT)
    K(c, 1.3, P_(kneel(14), W("rest"), {"body": [12, 0, 0], "right_arm": [-10, 0, 8], "left_arm": [-20, 0, -8]}), E_IO)
    tug = reach(r, P_(STAND, {"head": [10, 0, 0], "left_arm": [-30, -20, 0], "left_forearm": [-90, 0, 0]}), "left", (1.2, 22.0, -4.6),
                grip=GRIP["left"])
    K(c, 2.2, P_(tug, W("rest"), FIST_L), E_IO)
    K(c, 2.7, P_(STAND, W("rest"), {"head": [-6, 0, 0]}), E_IO)
    K(c, 3.2, P_(STAND, W("rest")), E_IO)

    for cl in clips:
        cl.done(step=0.05)
    return f, clips


HIT_TIMES = {"smite": 1.2, "snap": 0.7, "call_lightning": 1.0, "thunderclap": 0.5, "blink": 0.12, "death": 4.0, "emerge": 0.0}


def hidden_for(wings=False, veins=False, palm=False):
    h = []
    if not wings:
        h.append("wings")
    if not veins:
        h.extend(VEIN_BONES)
    if not palm:
        h.append("palm_light")
    return tuple(h)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=16000)
    m.rig.write(GEO + "raphael.geo.json")
    save(t, "entity", "raphael")
    save(g, "entity", "raphael_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + "raphael.animation.json")
    garrison()


def garrison():
    """His garrison: the Host's rig painted storm grey and silver (same UV as host_angel_*)."""
    import host_angel_art
    gm = host_angel_art.rig("garrison")
    t, g = gm.build(gutter=1, seed=9000)
    save(t, "entity", "garrison_angel")
    save(g, "entity", "garrison_angel_glowmask")


if __name__ == "__main__":
    generate()
