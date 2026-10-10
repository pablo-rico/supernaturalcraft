"""Zachariah (v0.18): Heaven's middle manager -- his GeckoLib rig, texture, glowmask and every clip (HeavenAssets.ZACHARIAH_*).

A balding, middle-aged man with a smug, patient smile (32 px to the crown, a little thick round the middle): a fringe of
grey-brown hair round the back and sides of a shining bald crown, raised eyebrows, heavy-lidded eyes with a faint gold light
in them (glowmask). A mid-grey two-button suit, a white shirt, a deep burgundy tie with a gold bar, a white pocket square in
three points, a little gilt pin of wings on his lapel, black oxfords. His clipboard (`clipboard`, always in his left hand:
a brown board, a steel clip, a sheaf of forms that flutter, `clipboard_pages`) and his rubber stamp (`stamp`, right hand, shown
only during `stamp` and `termination`: a turned wooden handle, a brass ferrule, the red-inked die).

`wings` (shown from phase III, while the synced flag says so): six great wings of burnt gold, three pairs -- upper, middle,
lower -- each feather its own bone where it moves (`wing_<side><pair>_f<i>`, children of the wing or of its folding tip
`wing_<side><pair>_tip`): the vanes gold, scorched to umber at the root and singed black at the ragged tips, embers in them
(glowmask), a gold shaft down each.
`palm_light` (a disc of gold light on his right palm) only while `smite` plays.

Skeleton (Bedrock px; +X is his LEFT; the model faces north):
  root -- body -- head
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_thumb, stamp, palm_light
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_index, left_thumb, clipboard -- clipboard_pages
               -- jacket_front_r, jacket_front_l, jacket_back
               -- wings -- wing_r1..3, wing_l1..3 -- <wing>_tip, feathers
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from legacy_art import Build, SIDES, reach as _reach, shoe_m, walk_cycle
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, aim, aim2, edge_cube, fabric, mirror_rot, ruffle,
                         to_preview, tone, world_point, write_compact)
from michael_art import Clip as MClip
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.zachariah."
CLEAR = (0, 0, 0, 0)

ZB = Build(hip=12.2, neck=24.3, tw=4.0, aw=3.6, ax=5.75, sx=5.35, sy=23.3, elbow=18.6, wrist=14.3, palm=12.4, lx=1.95, lw=3.7,
           knee=6.4, shoe_h=1.5)

# --- palette --------------------------------------------------------------------------------------------------------
SKIN = Ramp("#6b4637", "#9a6b58", "#bb8a74", "#cfa18a", "#ddb59f", "#e9c9b5")
HAIR = Ramp("#2a241f", "#3b332c", "#4f463d", "#655b50", "#7c7166", "#948a7f")
SUIT = Ramp("#2c2f35", "#3c4048", "#4e535c", "#626872", "#787e88", "#90969f")
SHIRT = Ramp("#9aa1aa", "#b5bbc3", "#cbd0d6", "#dde1e6", "#ebeef1", "#f8f9fb")
TIE = Ramp("#24070b", "#3a0c12", "#52111a", "#6b1823", "#85212e", "#9f2d3b")
OXFORD = Ramp("#050404", "#0b0909", "#131011", "#1c1819", "#2a2526", "#4a4446")
LIPS = Ramp("#5c3029", "#7a4238", "#935549", "#a8665a", "#b9786a", "#c98a7c")
EYE_WHITE = hexc("#e4ddd4")
IRIS = hexc("#4d4a3c")
PUPIL = hexc("#1b1712")
EYE_GOLD = hexc("#ffd98a")
BOARD = Ramp("#2b1a0d", "#432914", "#5c391c", "#764a25", "#8f5c30", "#a8703d")
PAPER = Ramp("#a59f90", "#c2bcac", "#d8d3c4", "#e8e4d7", "#f3f0e6", "#fdfcf6")
INK = hexc("#2a2f3c")
RED_INK = Ramp("#4a0608", "#6e0a0e", "#940f15", "#b5181e", "#cf2a2c", "#e44a43")
STEEL = Ramp("#2a2f36", "#454c56", "#68717d", "#8f98a3", "#b8c0c9", "#e4e9ee")
WOOD = Ramp("#2b170b", "#432410", "#5c3317", "#76431e", "#905427", "#aa6833")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
GOLD = Ramp("#3a2208", "#5e3a10", "#86561a", "#ad7527", "#cf9a3e", "#ecc46a")      # burnt gold
CHAR = Ramp("#0b0806", "#16100b", "#22180f", "#2f2114", "#3e2c19", "#4f381f")       # the scorched roots and tips
EMBER = Ramp("#7a2a06", "#a8420a", "#d0621a", "#ef8a2e", "#ffb554", "#ffe2a0")
LIGHT = Ramp("#c9a75a", "#e0c27a", "#efd99c", "#f8ebc2", "#fdf6e0", "#ffffff")
WHITE = hexc("#ffffff")

GLOW = H.glow_faint(0.05)


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(seed, shine=False):
    base = H.skin_mat(SKIN, seed, 3.1, 0.4)

    def m(f, x, y, w, h):
        c = base(f, x, y, w, h)
        if shine and f == "up":
            d = math.hypot(x - (w - 1) * 0.45, y - (h - 1) * 0.5) / (w / 2)
            c = mix(c, SKIN[5], max(0.0, 0.6 - 0.55 * d))
        return c
    return m


def hair_m(seed):
    """Short grey-brown hair, combed down at the sides: fine vertical strands, greying in flecks."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 10.0)
        v = 3.1 + (n - 0.5) * 0.4 + 0.3 * math.sin(x * 2.2 + n * 3)
        c = tone(HAIR, v)
        if h01("grey", seed, f, x, y) < 0.22:
            c = mix(c, HAIR[5], 0.45)
        return c
    return m


def suit_m(seed, base=3.0, crease=False):
    fab = fabric(SUIT, seed, base, 0.28, crease=crease)

    def m(f, x, y, w, h):
        c = fab(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and (x + y * 2) % 7 == 0:
            c = mix(c, SUIT[4], 0.15)                     # a faint sharkskin weave
        return c
    return m


# =====================================================================================================
# The face (north face of the head, 16 x 16 texels; x = 0 is his right)
# =====================================================================================================

FACE = [
    "ssssssssssssssss",
    "sssssfffffssssss",
    "ssssssssssssssss",
    "sssrrrrrrrrrrsss",
    "ssssssssssssssss",
    "ssbbbBssssBbbbss",
    "ssddddssssddddss",
    "ssWEIwsssswIEWss",
    "ssuuuussssuuuuss",
    "sssssssnnsssssss",
    "ssssssnnnnssssss",
    "sssssNNnnNNsssss",
    "ssgssssssssskgss",
    "sssssmmmmmmmkcss",
    "jsssssppppsssssj",
    "jjssssssssssssjj",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


# =====================================================================================================
# The rig
# =====================================================================================================

FLIGHT, COVERT = 8, 5
TIP_FROM = 4
WING_SPECS = {
    # pair: (root y, root |x|, leading edge (out, rise) px, feather scale, rest rotation of the right wing)
    1: (22.4, 1.3, (26.0, 15.0), 1.0, (0, 24, 10)),
    2: (20.2, 1.5, (30.0, 2.0), 1.0, (0, 30, -2)),
    3: (17.8, 1.4, (24.0, -11.0), 0.82, (0, 36, -12)),
}


def skeleton(m):
    b = ZB
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, b.hip, 0), "root")
    m.bone("head", (0, b.neck, 0), "body")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (b.sx * s, b.sy, 0), "body")
        m.bone(f"{side}_forearm", (b.ax * s, b.elbow, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (b.ax * s, b.wrist, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (b.ax * s, b.palm, 0.2), f"{side}_hand")
        m.bone(f"{side}_index", (b.ax * s, b.palm, -1.0), f"{side}_hand")
        m.bone(f"{side}_thumb", (b.ax * s - 1.0 * s, b.palm + 1.0, -1.3), f"{side}_hand")
    m.bone("stamp", (-b.ax, b.palm - 0.2, -0.2), "right_hand")
    m.bone("palm_light", (-b.ax + 1.4, b.palm + 1.0, 0), "right_hand")
    m.bone("clipboard", (b.ax, b.palm - 0.3, -0.4), "left_hand")
    m.bone("clipboard_pages", (b.ax + 1.25, b.palm + 4.4, -0.4), "clipboard")
    m.bone("jacket_front_r", (-2.1, b.hip, -2.2), "body")
    m.bone("jacket_front_l", (2.1, b.hip, -2.2), "body")
    m.bone("jacket_back", (0, b.hip, 2.2), "body")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (b.lx * s, b.hip, 0), "root")
        m.bone(f"{side}_shin", (b.lx * s, b.knee, 0), f"{side}_leg")


def rig():
    m = Model("zachariah", 512, 512)
    skeleton(m)
    build_head(m)
    build_body(m)
    build_arms(m)
    build_legs(m)
    build_props(m)
    build_wings(m)
    return m


# --- head ---------------------------------------------------------------------------------------------------------------

def build_head(m):
    b = ZB
    y0 = b.neck
    sk = skin_m(19101, shine=True)
    hr = hair_m(19102)

    def eye_glow(f, x, y, w, h, c):
        if c == IRIS:
            return shade(EYE_GOLD, 0.6)                   # the angel looking out of him
        if c == PUPIL:
            return shade(EYE_GOLD, 0.35)
        return shade(c, 0.05)
    legend = {"s": sk, "f": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), SKIN[5], 0.4), "r": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.92),
              "b": HAIR[3], "B": HAIR[2], "d": SKIN[1], "W": EYE_WHITE, "w": mix(EYE_WHITE, SKIN[2], 0.4), "E": IRIS, "I": PUPIL,
              "u": mix(SKIN[1], SKIN[2], 0.6), "n": SKIN[4], "N": SKIN[1], "g": SKIN[2], "k": SKIN[1], "m": LIPS[1], "c": LIPS[2],
              "p": LIPS[3], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.88)}

    def sides(f, x, y, w, h):
        if f == "up":
            return sk(f, x, y, w, h)                      # bald, shining
        if f == "down":
            return sk(f, x, y, w, h)
        if f == "south":
            return hr(f, x, y, w, h) if 4 <= y < 13 else sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x               # 0 at the front
        # The fringe: round the back and down over the ears' tops, thinning towards his temples.
        if fx >= 9 and 4 <= y < 13:
            return hr(f, x, y, w, h)
        if 6 <= fx < 9 and 4 <= y < 7:
            return hr(f, x, y, w, h)
        if 8 <= fx <= 10 and 7 <= y <= 11:
            c = sk(f, x, y, w, h)
            return shade(c, 0.8) if fx == 9 and 8 <= y <= 10 else shade(c, 1.05)
        return sk(f, x, y, w, h)

    def face(f, x, y, w, h):
        if f == "north":
            ch = FACE[y][x]
            v = legend.get(ch, legend["s"])
            return v(f, x, y, w, h) if callable(v) else v
        return sides(f, x, y, w, h)
    m.cube("head", (-4, y0, -4), (8, 8, 8), face, eye_glow, density=2, tag="head")
    # The fringe stands a little proud of his scalp; a fleshy nose; the ears; heavy cheeks.
    def fringe(f, x, y, w, h):
        if f in ("up", "down"):
            return hr(f, x, y, w, h) if f == "up" else None
        if f == "north":
            return None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, y0 + 1.8, 1.2), (8, 2.6, 2.8), fringe, GLOW, inflate=0.2, density=2, tag="fringe")
    m.cube("head", (-0.75, y0 + 2.3, -4.6), (1.5, 2.2, 0.7), lambda f, x, y, w, h: (SKIN[4] if y < 3 else SKIN[3]) if f == "north"
           else (SKIN[1] if f == "down" else SKIN[2]), GLOW, density=2, tag="nose")
    m.cube("head", (-0.95, y0 + 2.2, -4.45), (1.9, 0.7, 0.4), solid(SKIN[2]), GLOW, density=2, tag="nostrils")
    for s in (-1, 1):
        m.cube("head", (4 * s - 0.25 + (0 if s > 0 else -0.3), y0 + 2.6, -0.6), (0.55, 2.4, 1.6),
               lambda f, x, y, w, h: SKIN[2] if f not in ("east", "west") else SKIN[3], GLOW, density=2, tag="ear")
        # Jowls: the softness under his jaw.
        m.cube("head", (s * 2.6 - 1.3, y0 - 0.3, -3.9), (2.6, 0.8, 3.2), lambda f, x, y, w, h: SKIN[2] if f != "down" else SKIN[1], GLOW,
               density=2, tag="jowl")
        # His eyebrows, raised: two little ridges.
        m.cube("head", (s * 2.2 - 1.2, y0 + 5.2, -4.25), (2.4, 0.45, 0.3), lambda f, x, y, w, h: HAIR[3] if f != "down" else HAIR[1],
               GLOW, density=2, tag="brow", rotation=(0, 0, 9 * s), pivot=(s * 1.0, y0 + 5.4, -4.1))
    m.cube("body", (-1.9, y0 - 1.0, -1.8), (3.8, 1.3, 3.6), sk, GLOW, density=2, tag="neck")


# --- body: shirt, tie, jacket ----------------------------------------------------------------------------------------

def build_body(m):
    b = ZB
    shirt = fabric(SHIRT, 19201, 3.6, 0.12)
    suit = suit_m(19202)

    def tie(f, x, y, w, h):
        return TIE[4] if x == 7 or (x + y) % 6 == 0 else TIE[2]
    m.cube("body", (-b.tw, b.hip, -2.1), (2 * b.tw, b.neck - b.hip, 4.2), lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north"
           else SUIT[1], GLOW, density=2, tag="torso")
    # A middle-aged middle: the belly under the jacket.
    m.cube("body", (-3.4, 13.0, -2.75), (6.8, 4.6, 0.8), lambda f, x, y, w, h: tone(SUIT, 3.0 - 0.6 * y / max(1, h)) if f != "south"
           else None, GLOW, inflate=0.2, density=2, tag="belly")
    front = H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 10), v_depth=11, buttons=SUIT[0], button_rows=(15, 19))
    m.cube("body", (-b.tw, b.hip - 0.3, -2.1), (2 * b.tw, b.neck - b.hip + 0.1, 4.2), none_on(("down",), front), GLOW, inflate=0.32,
           density=2, tag="jacket")
    # The tie's knot and bar, the shirt collar, the lapels with their gilt pin, the pocket square.
    m.cube("body", (-0.6, b.neck - 2.1, -2.65), (1.2, 1.0, 0.4), solid(TIE[3]), GLOW, density=4, tag="tie_knot")
    m.cube("body", (-0.75, 19.3, -2.62), (1.5, 0.25, 0.2), lambda f, x, y, w, h: BRASS[5] if x % 3 else BRASS[3],
           lambda f, x, y, w, h, c: shade(c, 0.3), density=4, tag="tie_bar")
    for s in (-1, 1):
        m.cube("body", (0.95 * s - 0.8, b.neck - 1.35, -2.5), (1.6, 1.25, 0.4), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               GLOW, density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.95 * s, b.neck - 0.2, -2.35))

        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return SUIT[1]
            c = tone(SUIT, 4.2 - 1.2 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = SUIT[2]
            return c
        m.cube("body", (2.5 * s - 1.15, 17.4, -2.78), (2.3, 6.4, 0.4), lapel, GLOW, density=2, tag="lapel", rotation=(3, 0, 13 * s),
               pivot=(1.6 * s, 17.4, -2.6))
        m.cube("body", (3.3 * s - 0.75, 23.0, -2.82), (1.5, 1.1, 0.4), solid(SUIT[4]), GLOW, density=2, tag="lapel_notch",
               rotation=(0, 0, -35 * s), pivot=(3.3 * s, 23.0, -2.62))
        m.cube("body", (s * (b.tw + 0.2) - 1.0, b.neck - 1.15, -2.3), (2.0, 0.85, 4.6), lambda f, x, y, w, h: tone(SUIT, 3.9 if f == "up" else 2.8),
               GLOW, density=2, tag="shoulder")
        m.cube("body", (2.3 * s - 1.2, 13.4, -2.98), (2.4, 0.6, 0.3), lambda f, x, y, w, h: SUIT[4] if y == 0 else SUIT[2], GLOW,
               density=4, tag="pocket_flap")
    # The gilt lapel pin: a pair of little wings.
    m.cube("body", (-3.6, 20.6, -3.05), (1.4, 0.6, 0.2), lambda f, x, y, w, h: GOLD[5] if (x + y) % 2 else GOLD[4],
           lambda f, x, y, w, h, c: shade(c, 0.55), density=4, tag="lapel_pin")
    # The breast pocket and the white square folded in three points.
    m.cube("body", (1.5, 20.2, -2.68), (2.0, 0.35, 0.25), solid(SUIT[1]), GLOW, density=4, tag="breast_pocket")
    m.cube("body", (1.65, 20.5, -2.72), (1.7, 0.8, 0.25), lambda f, x, y, w, h: None if f == "north" and y == 0 and x % 3 == 1 else
           (SHIRT[5] if (x + y) % 3 else SHIRT[3]), GLOW, density=4, tag="pocket_square")
    # The jacket's skirt over the seat, in three pieces that swing with his legs.
    L = 2.8
    for bone, s in (("jacket_front_r", -1), ("jacket_front_l", 1)):
        def fr(f, x, y, w, h, s=s):
            if f in ("up",) or f == ("west" if s < 0 else "east") or f == "south":
                return None
            if f == "north" and y >= h - 2 and ((s < 0 and x >= w - 2) or (s > 0 and x <= 1)):
                return None
            return suit(f, x, y + 10, w, h + 10)
        m.cube(bone, ((0.0 if s > 0 else -b.tw), b.hip - L, -2.1), (b.tw, L, 4.2), fr, GLOW, inflate=0.36, density=2, tag="jacket_skirt")

    def bk(f, x, y, w, h):
        if f not in ("north", "south"):
            return None if f in ("up", "east", "west") else SUIT[1]
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > 1:
            return SUIT[0]
        return suit(f, x, y + 10, w, h + 10)
    m.cube("jacket_back", (-b.tw, b.hip - L, 1.95), (2 * b.tw, L, 0.3), bk, GLOW, inflate=0.36, density=2, tag="jacket_back")


def build_arms(m):
    b = ZB
    suit = suit_m(19301)
    shirt = fabric(SHIRT, 19302, 3.7, 0.1)
    hand = H.skin_mat(SKIN, 19303, 3.1, 0.4)
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.7 - b.elbow, b.aw), suit, GLOW, inflate=0.2,
               density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.7, b.wrist + 0.45, -1.7), (3.4, b.elbow - b.wrist - 0.45, 3.4), none_on(("up",), suit), GLOW,
               inflate=0.2, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist - 0.05, -1.5), (3.0, 0.6, 3.0), none_on(("up",), shirt), GLOW, inflate=0.1,
               density=2, tag="shirt_cuff")
        m.cube(f"{side}_forearm", (cx + 1.55 * s - 0.15, b.wrist + 0.1, -0.3), (0.3, 0.4, 0.6), solid(BRASS[4]),
               lambda f, x, y, w, h, c: shade(c, 0.3), density=4, tag="cufflink")
        # Thick hands: palm, the index its own bone, the other fingers together, the thumb.
        m.cube(f"{side}_hand", (cx - 1.35, b.palm, -1.5), (2.7, b.wrist - b.palm, 3.0), hand, GLOW, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.9 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_index", (cx - 1.3, b.palm - 1.45, -1.45), (2.6, 1.45, 0.85), fing, GLOW, density=2, tag="index")
        m.cube(f"{side}_fingers", (cx - 1.3, b.palm - 1.3, -0.6), (2.6, 1.3, 2.0), fing, GLOW, density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 1.0 * s - 0.5, b.palm + 0.2, -2.1), (1.0, 1.5, 0.9), hand, GLOW, density=2, tag="thumb")
    # A gold wedding band, a heavy wristwatch.
    m.cube("left_fingers", (b.ax - 1.35, b.palm - 0.75, 0.4), (2.7, 0.35, 0.5), solid(BRASS[4]), lambda f, x, y, w, h, c: shade(c, 0.2),
           density=4, tag="ring")
    m.cube("left_hand", (b.ax - 1.45, b.wrist - 0.75, -1.6), (2.9, 0.6, 3.2), lambda f, x, y, w, h: BRASS[4] if f == "east" else
           hexc("#2a1a10"), GLOW, inflate=0.1, density=4, tag="watch")


def build_legs(m):
    b = ZB
    for side, s in SIDES:
        tr = suit_m(19401 + (s > 0), 2.9, crease=True)
        x0 = b.lx * s - b.lw / 2
        m.cube(f"{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), tr, GLOW, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, b.shoe_h, -b.lw / 2), (b.lw, b.knee - b.shoe_h, b.lw), tr, GLOW, density=2, tag="shin")
        m.cube(f"{side}_shin", (x0, b.shoe_h, -b.lw / 2), (b.lw, 0.8, b.lw), none_on(("up", "down"), lambda f, x, y, w, h, tr=tr:
               shade(tr(f, x, y + 8, w, h), 0.85)), GLOW, inflate=0.18, density=2, tag="hem")
        m.cube(f"{side}_shin", (x0 - 0.1, 0, -2.9), (b.lw + 0.2, b.shoe_h, 4.9), shoe_m(OXFORD, 19410 + (s > 0)),
               lambda f, x, y, w, h, c: shade(c, 0.1), density=2, tag="shoe")


# --- props ------------------------------------------------------------------------------------------------------------

def build_props(m):
    b = ZB
    # The clipboard: held by its edge in his left hand, its face to his side (the renderer never hides it).
    cb = "clipboard"
    cx = b.ax + 1.3
    top = b.palm + 4.6

    def board(f, x, y, w, h):
        if f in ("east", "west"):
            n = fbm(x * 0.5, y * 3, 19501, 64, 64, 2, 6.0)
            return tone(BOARD, 2.6 + 1.0 * n - (0.4 if f == "west" else 0))
        return BOARD[3 if f == "up" else 1]
    m.cube(cb, (cx - 0.25, top - 10.0, -3.6), (0.5, 10.0, 7.0), board, GLOW, density=2, tag="board")
    m.cube(cb, (cx + 0.2, top - 1.3, -1.6), (0.5, 1.5, 3.0), lambda f, x, y, w, h: STEEL[4] if f in ("up", "east") else STEEL[2],
           lambda f, x, y, w, h, c: shade(c, 0.15), density=4, tag="board_clip")
    m.cube(cb, (cx - 0.75, top - 1.3, -1.6), (0.5, 1.5, 3.0), lambda f, x, y, w, h: STEEL[3], GLOW, density=4, tag="board_clip")

    def form(stamp_mark):
        def mat(f, x, y, w, h):
            if f not in ("east", "west"):
                return PAPER[3]
            fx = x if f == "east" else w - 1 - x
            if y < 3:
                return PAPER[4]
            if 3 <= y <= 4 and 2 <= fx <= w - 3:
                return INK if (fx + y) % 4 else PAPER[3]                  # the heading
            if stamp_mark and 11 <= y <= 14 and 3 <= fx <= 10:
                if y in (11, 14) or fx in (3, 10) or (y in (12, 13) and fx % 2 == 0):
                    return RED_INK[3]                                    # an APPROVED stamp, slightly skew
            if y % 2 == 0 and 2 <= fx <= w - 3 and y < h - 2 and h01("ln", fx // 3, y) < 0.85:
                return mix(PAPER[2], INK, 0.35)                           # lines of typescript
            return PAPER[4] if (x + y) % 9 else PAPER[3]
        return mat
    pg = "clipboard_pages"
    for i, (dz, mark) in enumerate(((0.0, True), (0.3, False))):
        m.cube(pg, (cx + 0.25 + 0.1 * i, top - 9.4, -3.3 + dz), (0.12, 8.4, 6.4), form(mark), GLOW, density=2, tag="form")
    # The stamp in his right fist: a turned wooden knob, a neck, a brass ferrule, the rubber die inked red.
    st = "stamp"
    sx = -b.ax
    y0 = b.palm - 0.2
    m.cube(st, (sx - 0.7, y0 + 0.2, -0.9), (1.4, 2.4, 1.4), lambda f, x, y, w, h: tone(WOOD, 3.4 - 0.4 * y + (0.5 if f == "up" else 0)),
           GLOW, density=4, tag="stamp_handle")
    m.cube(st, (sx - 0.95, y0 + 2.4, -1.15), (1.9, 1.0, 1.9), lambda f, x, y, w, h: WOOD[4] if f == "up" else WOOD[3], GLOW, density=4,
           tag="stamp_knob")
    m.cube(st, (sx - 0.9, y0 - 0.5, -0.9), (1.8, 0.7, 1.8), lambda f, x, y, w, h: BRASS[4] if y == 0 else BRASS[2],
           lambda f, x, y, w, h, c: shade(c, 0.2), density=4, tag="stamp_ferrule")
    m.cube(st, (sx - 1.6, y0 - 1.3, -1.6), (3.2, 0.8, 3.2), lambda f, x, y, w, h: WOOD[2] if f != "down" else None, GLOW, density=4,
           tag="stamp_block")

    def die(f, x, y, w, h):
        # DENIED in reverse on the die (an illegible red block of letters).
        if 2 <= y <= w - 3 and 2 <= x <= w - 3 and (x * 3 + y * 5) % 7 < 4:
            return RED_INK[4]
        return RED_INK[2]
    m.cube(st, (sx - 1.5, y0 - 1.6, -1.5), (3.0, 0.3, 3.0), die, GLOW, density=4, tag="stamp_die")
    # The gold light on his right palm (smite): faces his left when the arm hangs.
    pl = "palm_light"

    def disc(f, x, y, w, h):
        r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / (w / 2)
        if r > 1.0:
            return None
        if r < 0.3:
            return WHITE
        ray = (math.atan2(y - (h - 1) / 2, x - (w - 1) / 2) * 12 / math.pi) % 2 < 0.6
        c = LIGHT[4] if ray else GOLD[5]
        return (c[0], c[1], c[2], int(235 - 120 * r))
    m.cube(pl, (-b.ax + 1.38, b.palm - 0.8, -1.9), (0, 3.8, 3.8), disc, lambda f, x, y, w, h, c: (c[0], c[1], c[2], 255), faces=("west",),
           density=4, tag="palm_light")


# --- the six burnt-gold wings -----------------------------------------------------------------------------------------

class BurntFeather:
    """A feather of burnt gold on a flat plane (y = 0 at the root): umber and char at the root, the vane gold, its barbs
    raked in fine diagonal lines, a bright gold shaft, the ragged tip singed black with embers still glowing in it.
    `broad` for the coverts (rounder, shorter); `s` the side, so the barbs rake outward."""

    def __init__(self, seed, s, broad=False, alpha=255):
        self.seed, self.s, self.broad, self.alpha = seed, s, broad, alpha

    def half(self, t, w):
        hw = (w - 1) / 2 + 0.3
        if self.broad:
            return hw * (math.sqrt(max(0.0, 1 - ((t - 0.4) / 0.6) ** 2)) if t > 0.4 else min(1.0, 0.7 + t))
        if t < 0.08:
            return hw * (0.5 + 6 * t)
        return hw * (1.0 if t < 0.62 else max(0.12, 1 - (t - 0.62) / 0.38 * 0.9))

    def mat(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        hw = self.half(t, w)
        dx = x - c
        if abs(dx) > hw:
            return None
        # The singed, ragged edge.
        if t > 0.55 and abs(dx) > hw - 1.2 and h01("rag", self.seed, face, x, y) < 0.35 + (t - 0.55):
            return None
        q = abs(dx) / max(0.5, hw)
        under = face == "north"
        if abs(dx) < 0.6 and t < 0.92:
            return tone(GOLD, 5.0 - 1.5 * t - (0.7 if under else 0))         # the shaft
        side = 1 if dx * self.s > 0 else -1
        barb = ((y - abs(dx) * (1.4 if side > 0 else 1.0)) % 3) < 1
        v = 3.3 + 0.9 * (1 - q) - 0.4 * t + (-0.55 if barb else 0.15)
        if under:
            v -= 0.6
        col = tone(GOLD, v)
        # Scorched towards the root, singed at the tip and the outer edge.
        root = max(0.0, (0.22 - t) / 0.22)
        if root > 0:
            col = mix(col, CHAR[3], root * 0.75)
        burn = max(0.0, (t - 0.68) / 0.32) + 0.35 * max(0.0, q - 0.75) / 0.25
        n = fbm(x * 3 + len(face), y * 1.4, self.seed, 64, 64, 3, 4.0)
        if burn > 0:
            col = mix(col, CHAR[2], min(1.0, burn * (0.8 + 0.6 * n)))
            if burn > 0.35 and h01("emb", self.seed, face, x, y) < 0.09:
                col = EMBER[3 + int(2 * h01("embc", self.seed, x, y))]
        return (col[0], col[1], col[2], self.alpha)

    @staticmethod
    def glow(face, x, y, w, h, col):
        if col[0] > 230 and col[1] > 120 and col[2] < 140:
            return (col[0], col[1], col[2], 255)                          # embers
        if col[0] > 200 and col[1] > 150:
            return shade(col, 0.35)                                       # the gold shaft and bright vanes
        if col[0] > 150 and col[1] > 100:
            return shade(col, 0.12)
        return None


def feather_bone(m, parent, name, root, length, width, angle, s, mat, glow, cup=0.0, twist=0.0, tag="feather"):
    x, y, z = root
    b = m.bone(name, (x, y, z), parent, rotation=(cup, -s * twist, -s * angle))
    m.cube(b, (x - width / 2, y - length, z), (width, length, 0), mat, glow, density=2, tag=tag, faces=("north", "south"))
    return b


def covert(m, bone, root, length, width, angle, s, mat, glow, tilt=0.0, tag="covert"):
    x, y, z = root
    return m.cube(bone, (x - width / 2, y - length, z), (width, length, 0), mat, glow, density=2, tag=tag,
                  rotation=(tilt, 0, -s * angle), pivot=(x, y, z), faces=("north", "south"))


def build_wings(m):
    w_all = m.bone("wings", (0, 20.0, 2.6), "body")
    for pair, (ry, rx, (out, rise), k, rest) in WING_SPECS.items():
        for side, s in (("r", -1), ("l", 1)):
            name = f"wing_{side}{pair}"
            root = (rx * s, ry, 2.9 + 0.25 * pair)
            w = m.bone(name, root, w_all.name, rotation=mirror_rot(rest, s))
            x0, y0, z0 = root
            wx, wy = x0 + s * out * 0.5, y0 + rise * 0.5
            tip = m.bone(f"{name}_tip", (wx, wy, z0), name)
            seed = 19700 + pair * 100 + (0 if s < 0 else 50)

            # The leading edge: the wing's bone under a ridge of small gold feathers, scorched along its top.
            def arm_m(f, x, y, w_, h, s=s, seed=seed):
                t = y / max(1, h - 1)
                q = x / max(1, w_ - 1)
                if (f == "north") == (s < 0):
                    q = 1 - q
                if t > 0.55 + 0.35 * (1 - q) or q > 0.985:
                    return None
                scale_ = ((x // 2) + (y // 2)) % 2
                col = tone(GOLD, 2.8 + 1.4 * (1 - t) + 0.5 * scale_ - 0.6 * q)
                if t < 0.18:
                    col = mix(col, CHAR[3], 0.4)
                return col
            for bn, a_, b_ in ((w, (x0, y0, z0), (wx, wy, z0)), (tip, (wx, wy, z0), (x0 + s * out, y0 + rise, z0))):
                edge_cube(m, bn, a_, b_, 4.0 * k, s, arm_m, BurntFeather.glow, tag="wing_arm", dz=-0.15)
            for i in range(FLIGHT):
                f = i / (FLIGHT - 1)
                t = 0.1 + 0.9 * f
                px, py = x0 + s * out * t, y0 + rise * t
                length = (16 + 15 * f) * k * (0.93 + 0.14 * h01("fl", seed, i))
                angle = 6 + 80 * f ** 1.25 + (h01("fa", seed, i) - 0.5) * 6
                parent = tip if i >= TIP_FROM else w
                feather_bone(m, parent.name, f"{name}_f{i:02d}", (px, py - 0.6, z0 + 0.14 * i), length, 6.5 * k, angle, s,
                             BurntFeather(seed + i, s).mat, BurntFeather.glow, cup=4.0 * f, twist=7.0 * f)
                # A secondary between each pair of primaries, fixed to the wing (fills the fan when it opens).
                if i < FLIGHT - 1:
                    f2 = (i + 0.5) / (FLIGHT - 1)
                    t2 = 0.1 + 0.9 * f2
                    covert(m, tip if i + 1 >= TIP_FROM else w, (x0 + s * out * t2, y0 + rise * t2 - 0.4, z0 + 0.07 + 0.14 * i),
                           length * 0.82, 6.0 * k, 6 + 80 * f2 ** 1.25, s, BurntFeather(seed + 20 + i, s).mat, BurntFeather.glow,
                           tag="secondary")
            for j in range(COVERT):
                f = j / (COVERT - 1)
                t = 0.04 + 0.8 * f
                px, py = x0 + s * out * t, y0 + rise * t
                bone = tip if t >= 0.5 else w
                covert(m, bone, (px, py + 0.4, z0 + 1.3), (9 + 3 * f) * k, 7.0 * k, 14 + 40 * f, s,
                       BurntFeather(seed + 40 + j, s, broad=True).mat, BurntFeather.glow)
                covert(m, bone, (px + s * 1.2, py + 1.1, z0 + 1.8), (5.5 + 1.5 * f) * k, 6.0 * k, 10 + 30 * f, s,
                       BurntFeather(seed + 60 + j, s, broad=True).mat, BurntFeather.glow, tag="marginal")


# =====================================================================================================
# Animations
# =====================================================================================================

LIMB_FOLLOW = ("jacket_front_r", "jacket_front_l", "jacket_back")
WING_BONES = [f"wing_{sd}{p}" for p in (1, 2, 3) for sd in ("r", "l")]
FEATHERS = [f"wing_{sd}{p}_f{i:02d}" for p in (1, 2, 3) for sd in ("r", "l") for i in range(FLIGHT)]

WING_POSES = {
    # right-wing absolute rotations per pair (the left mirrors them) and the tips' fold (degrees, + folds the tip in)
    "rest": ({1: (0, 24, 10), 2: (0, 30, -2), 3: (0, 36, -12)}, 0),
    "wide": ({1: (0, 4, 22), 2: (0, 8, 4), 3: (0, 12, -8)}, -6),
    "high": ({1: (0, 14, 44), 2: (0, 18, 18), 3: (0, 22, -2)}, -10),
    "fold": ({1: (0, 80, -22), 2: (0, 84, -36), 3: (0, 86, -48)}, 60),
    "forward": ({1: (0, -40, 18), 2: (0, -36, 0), 3: (0, -30, -14)}, 24),
    "back": ({1: (0, 56, 30), 2: (0, 62, 10), 3: (0, 66, -6)}, -14),
}


def wing_pose(m, name, fan=1.0):
    tgt, tipfold = WING_POSES[name]
    out = {}
    for pair in (1, 2, 3):
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


def wing_breath(c, length, cycles, amp=2.0):
    from michael_art import _g
    w = 360.0 * cycles / length
    for i, n in enumerate(WING_BONES):
        s = -1 if n[5] == "r" else 1
        ph = 50 * (i // 2)
        a = amp * (1.0 - 0.15 * (i // 2))

        def fn(t, ph=ph, a=a, s=s):
            v = a * math.sin(math.radians(w * t + ph))
            return [0, -s * v * 0.5, -s * v]
        sine = f"math.sin(query.anim_time*{_g(w)}+{ph})"
        c.mlayer(n, fn, [None, f"{sine}*{_g(round(-s * a * 0.5, 2))}", f"{sine}*{_g(round(-s * a, 2))}"])


def follow(pose):
    """The jacket's skirt swings with his legs."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])[0]
    out.setdefault("jacket_front_r", [min(0.0, rl) * 0.5 - body * 0.3, 0, 0])
    out.setdefault("jacket_front_l", [min(0.0, ll) * 0.5 - body * 0.3, 0, 0])
    out.setdefault("jacket_back", [-max(0.0, max(rl, ll)) * 0.3 - body * 0.3, 0, 0])
    return out


class Clip(MClip):
    def key(self, t, pose, ease=None):
        return super().key(t, follow(pose), ease)


FIST_R = {"right_fingers": [-85, 0, 0], "right_index": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_index": [0, 0, 0], "right_thumb": [0, 0, -14]}
SPREAD_R = {"right_fingers": [6, 0, 10], "right_index": [4, 0, -8], "right_thumb": [0, 0, -30]}
POINT_R = {"right_fingers": [-90, 0, 0], "right_index": [0, 0, 0], "right_thumb": [-30, 0, 20]}
HOLD_L = {"left_fingers": [-70, 0, 0], "left_index": [-60, 0, 0], "left_thumb": [-10, 0, -30]}
LOOSE_R = {"right_fingers": [-25, 0, 0], "right_index": [-18, 0, 0]}


def kneel(head=15):
    return {"@root": [0, -5.6, 0], "left_leg": [-82, 0, -4], "left_shin": [82, 0, 0], "right_leg": [6, 0, 6], "right_shin": [86, 0, 0],
            "body": [16, 0, 0], "head": [head, 0, 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []
    W = lambda name, fan=1.0: wing_pose(m, name, fan)

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("palm_light",)))
        return clips[-1]

    def reach(pose, side, target, grip=None):
        s = -1 if side == "right" else 1
        return _reach(r, pose, side, target, grip or (ZB.ax * s, ZB.palm - 0.6, -0.3))

    # His stance: the clipboard held up in front of his chest in the left hand, reading it, the right hand loose at his side.
    read = reach(P_(HOLD_L, LOOSE_R, {"left_arm": [-30, 0, -10], "right_arm": [0, 0, 4]}), "left", (2.6, 17.6, -5.2))
    read = P_(read, {"left_hand": aim(r, read, "left_hand", (1, 0, 0), (-0.75, 0.25, -0.6))})
    STAND = P_(read, {"head": [10, 6, 0], "right_leg": [0, 0, 1.5], "left_leg": [0, 0, -1.5]}, W("rest"))
    # Carried: the clipboard down at his side, against his leg.
    CARRY = P_(HOLD_L, LOOSE_R, {"left_arm": [0, 0, -3], "left_hand": [0, -80, 0]}, W("rest"))

    # --- idle: reading his forms; the head lifts to regard you over them, a tap of the finger on the page. ---
    c = clip("idle", 4.0, loop=True)
    tap = reach(P_(STAND, POINT_R), "right", (1.8, 18.0, -6.4), grip=(-ZB.ax, ZB.palm - 1.4, -1.0))
    c.key(0, STAND)
    c.key(1.0, P_(STAND, {"head": [12, 4, 0], "body": [0.6, 0, 0]}), E_IO)
    c.key(1.6, P_(STAND, {"head": [-4, -8, 0], "body": [0.4, 0, 0]}), E_IO)
    c.key(2.4, P_(STAND, {"head": [-2, -6, 2]}), E_IO)
    c.key(2.9, P_(tap, {"head": [12, 4, 0]}), E_IO)
    c.key(3.1, P_(tap, {"head": [12, 4, 0], "right_hand": [-18, 0, 0]}), E_SNAP)
    c.key(3.4, P_(tap, {"head": [12, 4, 0]}), E_OUT)
    c.key(4.0, STAND, E_IO)
    c.layer("clipboard_pages", lambda t: [0, 0, 0.6 * math.sin(t * 2 * math.pi / 2.0)])
    wing_breath(c, 4.0, 2)
    ruffle(c, FEATHERS, 2.0, 2, 71)

    # --- walk: an unhurried, self-satisfied stroll, the clipboard swinging a little at his side. ---
    L = 1.3
    c = clip("walk", L, loop=True)
    for i in range(9):
        t = L * i / 8
        wk = walk_cycle(8, i, stride=22, arm=14, knee=28, bob=0.35, sway=3, lean=1.0)
        wk["left_arm"] = [wk["left_arm"][0] * 0.5, 0, -3]
        c.key(t, P_(CARRY, wk, {"left_hand": [0, -80, 0]}), E_IO if i else None)
    c.layer("clipboard", lambda t: [0, 0, -4 * math.sin(2 * math.pi * t / L - 0.9)])
    wing_breath(c, L, 1, 1.6)

    # --- paper_storm: the right arm drawn across his body, then flung out backhand (0.8): the memos fly. ---
    c = clip("paper_storm", 1.8)
    wind = P_(STAND, OPEN_R, {"right_arm": [-50, 50, -40], "right_forearm": [-60, 0, 0], "body": [0, 26, 0], "head": [6, -20, 0],
                             "right_leg": [6, 0, 0], "left_leg": [-6, 0, 0]})
    fling = P_(STAND, SPREAD_R, {"right_arm": [-80, -30, 50], "right_forearm": [-6, 0, 0], "right_hand": [0, -70, 0], "body": [6, -28, 0],
                                 "head": [-4, 18, 0], "right_leg": [-14, 0, 0], "left_leg": [8, 0, 0], "@root": [0, -0.6, -0.8]},
               W("wide", 1.1))
    c.key(0, STAND)
    c.key(0.55, wind, E_OUT)
    c.key(0.72, P_(wind, {"body": [0, 30, 0]}), E_IO)
    c.key(0.8, fling, E_SNAP)
    c.key(1.1, P_(fling, {"body": [4, -26, 0]}), E_OUT)
    c.key(1.8, STAND, E_IO)
    c.layer("clipboard_pages", lambda t: [0, 0, 18 * math.sin(math.pi * min(1, max(0, (t - 0.7) / 0.6)))])

    # --- stamp: the stamp raised high (to 0.8) and slammed down before him (1.0) -- a 3x3 square of DENIED. ---
    c = clip("stamp", 1.6)
    high = reach(P_(STAND, FIST_R, {"body": [-6, 0, 0], "head": [-10, 0, 0]}), "right", (-3.0, 33.0, -4.0))
    slam = reach(P_(STAND, FIST_R, {"body": [24, 0, 0], "head": [14, 0, 0], "@root": [0, -1.4, -0.6], "right_leg": [-18, 0, 0],
                                    "right_shin": [20, 0, 0], "left_leg": [10, 0, 0], "left_shin": [14, 0, 0]}), "right", (-2.0, 8.0, -8.5))
    slam = P_(slam, {"stamp": aim(r, P_(slam, {"stamp": [0, 0, 0]}), "stamp", (0, -1, 0), (0, -1, 0))})
    high = P_(high, {"stamp": aim(r, P_(high, {"stamp": [0, 0, 0]}), "stamp", (0, -1, 0), (0, -0.2, -1))})
    c.key(0, STAND)
    c.key(0.6, high, E_OUT)
    c.key(0.85, P_(high, {"body": [-8, 0, 0]}), E_IO)
    c.key(1.0, slam, E_SNAP)
    c.key(1.2, P_(slam, {"body": [22, 0, 0]}), E_OUT)
    c.key(1.6, STAND, E_IO)

    # --- summon_clerks: he raises a hand and snaps his fingers (0.7) without looking up from his forms. ---
    c = clip("summon_clerks", 1.5)
    up = reach(P_(STAND, {"right_index": [-20, 0, 0], "right_fingers": [-80, 0, 0], "right_thumb": [-30, 0, 30]}), "right",
               (-7.6, 24.6, -3.6), grip=(-ZB.ax, ZB.palm, -0.3))
    up = P_(up, {"right_hand": aim(r, up, "right_hand", (0, -1, 0), (0.3, 1, -0.2))})
    snapped = P_(up, {"right_index": [-30, 0, 0], "right_fingers": [-100, 0, 0], "right_thumb": [10, 0, 0],
                      "right_hand": [up["right_hand"][0] + 12, up["right_hand"][1], up["right_hand"][2]]})
    c.key(0, STAND)
    c.key(0.45, up, E_OUT)
    c.key(0.65, P_(up, {"right_thumb": [-36, 0, 34]}), E_IO)
    c.key(0.7, snapped, E_SNAP)
    c.key(1.0, snapped, E_OUT)
    c.key(1.5, STAND, E_IO)

    # --- precedent: he lifts the top page, finds the line, taps it (1.0): it was already written. ---
    c = clip("precedent", 2.0)
    lift = reach(P_(STAND, {"right_index": [-10, 0, 0], "right_fingers": [-40, 0, 0], "right_thumb": [-30, 0, 20], "head": [16, 4, 0]}),
                 "right", (1.6, 21.0, -6.2), grip=(-ZB.ax, ZB.palm - 1.4, -1.0))
    tapp = reach(P_(STAND, POINT_R, {"head": [16, 4, 0]}), "right", (1.9, 17.0, -6.6), grip=(-ZB.ax, ZB.palm - 1.4, -1.0))
    c.key(0, STAND)
    c.key(0.45, P_(lift, {"clipboard_pages": [0, 0, 0]}), E_OUT)
    c.key(0.7, P_(lift, {"clipboard_pages": [0, 0, -30]}), E_IO)
    c.key(0.9, P_(tapp, {"clipboard_pages": [0, 0, -30]}), E_IO)
    c.key(1.0, P_(tapp, {"right_hand": [-20, 0, 0], "clipboard_pages": [0, 0, -30]}), E_SNAP)
    c.key(1.3, P_(tapp, {"head": [-6, 0, 0], "clipboard_pages": [0, 0, -30]}), E_OUT)
    c.key(2.0, STAND, E_IO)

    # --- shuffle: a sweep of the open right hand from his left to his right (0.8): the cubicles rearrange themselves. ---
    c = clip("shuffle", 1.5)
    s0 = P_(STAND, OPEN_R, {"right_arm": [-70, 40, -10], "right_forearm": [-30, 0, 0], "right_hand": [0, -90, 0], "body": [0, 14, 0],
                            "head": [0, -6, 0]})
    s1 = P_(STAND, OPEN_R, {"right_arm": [-70, -40, 40], "right_forearm": [-10, 0, 0], "right_hand": [0, -90, 0], "body": [0, -16, 0],
                            "head": [-2, 16, 0]})
    c.key(0, STAND)
    c.key(0.5, s0, E_OUT)
    c.key(0.8, s1, E_IO)
    c.key(1.0, P_(s1, {"body": [0, -18, 0]}), E_OUT)
    c.key(1.5, STAND, E_IO)

    # --- termination: the clipboard turned to face you, the notice on it; the stamp brought down on it (1.4). ---
    c = clip("termination", 2.2)
    show = reach(P_(HOLD_L, FIST_R, {"left_arm": [-60, 0, 10], "head": [-4, 0, 0], "body": [-3, 0, 0]}), "left", (1.2, 19.0, -8.0))
    show = P_(show, {"left_hand": aim(r, show, "left_hand", (1, 0, 0), (0, 0.15, -1))})
    show = P_(show, {"right_arm": [-20, 0, 6], "right_forearm": [-40, 0, 0]})
    rise = reach(show, "right", (-1.0, 25.0, -9.0))
    hit = reach(P_(show, {"head": [6, 0, 0], "body": [8, 0, 0]}), "right", (0.2, 19.0, -8.6))
    hit = P_(hit, {"stamp": aim(r, P_(hit, {"stamp": [0, 0, 0]}), "stamp", (0, -1, 0), (0, 0.1, 1))})
    rise = P_(rise, {"stamp": aim(r, P_(rise, {"stamp": [0, 0, 0]}), "stamp", (0, -1, 0), (0, 0.6, 1))})
    c.key(0, STAND)
    c.key(0.6, show, E_OUT)
    c.key(1.1, rise, E_IO)
    c.key(1.3, P_(rise, {"body": [-4, 0, 0]}), E_IO)
    c.key(1.4, hit, E_SNAP)
    c.key(1.7, P_(hit, {"head": [0, 8, 4]}), E_OUT)
    c.key(2.2, STAND, E_IO)

    # --- reassign: a little dismissive flick of the fingers (0.5): you are wanted elsewhere. ---
    c = clip("reassign", 1.2)
    fl0 = P_(STAND, {"right_arm": [-40, 0, 20], "right_forearm": [-50, 0, 0], "right_hand": [30, 0, 0], "right_fingers": [-60, 0, 0],
                     "right_index": [-60, 0, 0], "head": [2, 0, 6]})
    fl1 = P_(fl0, OPEN_R, {"right_hand": [-30, 0, 0], "right_forearm": [-30, 0, 0], "head": [-4, 0, 10]})
    c.key(0, STAND)
    c.key(0.35, fl0, E_OUT)
    c.key(0.5, fl1, E_SNAP)
    c.key(0.75, fl1, E_OUT)
    c.key(1.2, STAND, E_IO)

    # --- wing_buffet: the six wings drawn up and back, then beaten forward together (0.8). ---
    c = clip("wing_buffet", 1.5)
    c.key(0, STAND)
    c.key(0.55, P_(STAND, W("back", 1.15), {"body": [-8, 0, 0], "head": [-6, 0, 0]}), E_OUT)
    c.key(0.8, P_(STAND, W("forward", 0.9), {"body": [12, 0, 0], "head": [6, 0, 0], "@root": [0, -0.6, -0.6]}), E_SNAP)
    c.key(1.05, P_(STAND, W("forward", 0.95), {"body": [10, 0, 0]}), E_OUT)
    c.key(1.5, STAND, E_IO)
    ruffle(c, FEATHERS, 10.0, 3, 72, t0=0.5, t1=1.45)

    # --- smite: his right hand rises, palm out, burning gold (to 1.0); thrust forward at 1.2; the wings flare. ---
    c = clip("smite", 2.2)
    raise_ = reach(P_(STAND, SPREAD_R, {"body": [-3, -8, 0], "head": [-4, 6, 0]}), "right", (-4.0, 27.0, -7.0),
                   grip=(-ZB.ax, ZB.palm + 0.4, 0))
    raise_ = P_(raise_, {"right_hand": aim2(r, raise_, "right_hand", (0, -1, 0), (0, 1, -0.3), (1, 0, 0), (0, 0, -1))})
    thrust = reach(P_(STAND, SPREAD_R, {"body": [12, -4, 0], "head": [2, 4, 0], "@root": [0, -0.8, -1.0], "right_leg": [-16, 0, 0],
                                        "right_shin": [16, 0, 0], "left_leg": [8, 0, 0]}, W("high", 1.1)), "right", (-2.0, 22.0, -11.0),
                   grip=(-ZB.ax, ZB.palm + 0.4, 0))
    thrust = P_(thrust, {"right_hand": aim2(r, thrust, "right_hand", (0, -1, 0), (0, 1, -0.2), (1, 0, 0), (0, 0, -1))})
    c.key(0, STAND)
    c.key(0.5, raise_, E_OUT)
    c.key(1.1, P_(raise_, {"head": [-6, 6, 0]}), E_IO)
    c.key(1.2, thrust, E_SNAP)
    c.key(1.5, P_(thrust, {"body": [10, -4, 0]}), E_OUT)
    c.key(2.2, STAND, E_IO)
    c.layer("right_arm", lambda t: [1.2 * math.sin(t * 90) if 0.5 < t < 1.18 else 0, 0, 0.8 * math.sin(t * 73) if 0.5 < t < 1.18 else 0])

    # --- wings_reveal: the six wings grow out of his back, sweep open past their rest (1.4) and settle; held. ---
    c = clip("wings_reveal", 2.5, hold=True)
    c.key(0, P_(STAND, W("fold", 0.5), {"%wings": [0.05, 0.05, 0.05]}))
    c.key(0.5, P_(STAND, W("fold", 0.6), {"%wings": [0.8, 0.8, 0.8], "body": [8, 0, 0], "head": [12, 0, 0]}), E_OUT)
    c.key(1.4, P_(CARRY, OPEN_R, W("wide", 1.15), {"%wings": [1.1, 1.1, 1.1], "body": [-8, 0, 0], "head": [-10, 0, 0],
                                                    "right_arm": [-14, 0, 30], "left_arm": [-10, 0, -20]}), E_BACK)
    c.key(1.9, P_(STAND, W("rest"), {"%wings": [1, 1, 1], "head": [4, 4, 0]}), E_IO)
    c.key(2.5, P_(STAND, W("rest"), {"%wings": [1, 1, 1]}), E_IO)
    for i, n in enumerate(FEATHERS):
        lag = 0.04 * (i % FLIGHT)
        c.layer(n, lambda t, lag=lag: [12 * math.sin(math.pi * min(1, max(0, (t - 0.9 - lag) / 0.6))), 0, 0])

    # --- stagger: struck; he rocks back, the clipboard clutched to his chest, offended. ---
    c = clip("stagger", 0.8)
    clutch = reach(P_(HOLD_L, {"left_arm": [-40, 0, -10]}), "left", (1.5, 19.0, -4.4))
    c.key(0, STAND)
    c.key(0.1, P_(clutch, W("back", 0.9), {"body": [-14, 8, 0], "head": [-16, -10, 0], "right_arm": [-20, 0, 24], "@root": [0, 0, 1.4],
                                           "right_leg": [10, 0, 0], "left_leg": [-6, 0, 0]}), E_SNAP)
    c.key(0.4, P_(STAND, {"body": [4, 0, 0], "head": [8, 6, 0], "@root": [0, 0, 0.8]}), E_OUT)
    c.key(0.8, STAND, E_IO)

    # --- death (6 s, held): struck through; the clipboard falls from his hand; he drops to his knees (2.2), then rears back,
    # arms flung wide, as the light bursts out of him (4.0); the wings flare. ---
    c = clip("death", 6.0, hold=True)
    c.key(0, STAND)
    c.key(0.3, P_(CARRY, W("back"), {"body": [-16, 0, 0], "head": [-22, 0, 0], "right_arm": [-20, 0, 36], "left_arm": [-20, 0, -36],
                                     "@root": [0, 0, 1.4]}), E_SNAP)
    c.key(1.2, P_(CARRY, W("rest"), {"body": [12, 10, 0], "head": [16, 0, 0], "right_arm": [-4, 0, 10], "left_arm": [-30, 0, -10],
                                     "left_forearm": [-40, 0, 0], "right_leg": [-12, 0, 0], "left_leg": [10, 0, 0], "@root": [0, -0.6, 1.2],
                                     "clipboard": [0, 0, -40], "@clipboard": [0, -2, 0]}), E_IO)
    k2 = P_(kneel(28), {"right_arm": [6, 0, 8], "left_arm": [4, 0, -8], "body": [22, 0, 0], "@root": [0, -5.6, 1.2],
                        "clipboard": [0, 0, -80], "@clipboard": [0, -6, 2], "%clipboard": [0.01, 0.01, 0.01]})
    c.key(2.2, P_(k2, W("fold", 0.7)), E_IN)
    burst = P_(kneel(-40), OPEN_R, {"body": [-18, 0, 0], "right_arm": [-30, 0, 104], "left_arm": [-30, 0, -104], "right_forearm": [-8, 0, 0],
                                    "left_forearm": [-8, 0, 0], "@root": [0, -5.6, 1.2], "clipboard": [0, 0, -80], "@clipboard": [0, -6, 2],
                                    "%clipboard": [0.01, 0.01, 0.01],
                                    "%wings": [1.3, 1.3, 1.3], "left_fingers": [0, 0, 0], "left_index": [0, 0, 0]}, W("high", 1.3))
    c.key(3.4, burst, E_OUT)
    c.key(4.0, P_(burst, {"head": [-46, 0, 0], "%wings": [1.4, 1.4, 1.4]}), E_IO)
    c.key(6.0, P_(burst, {"head": [-48, 0, 0], "%wings": [1.45, 1.45, 1.45]}))
    c.layer("body", lambda t: [2.0 * math.sin(t * 80) if 3.0 < t < 4.2 else 0, 0, 0])
    c.layer("head", lambda t: [0, 1.6 * math.sin(t * 91) if 3.0 < t < 4.2 else 0, 0])
    ruffle(c, FEATHERS, 12.0, 10, 74, t0=3.0, t1=5.6)

    # --- emerge: he is simply there, mid-stride, already reading; at 1.8 he looks up, straightens his tie, and smiles. ---
    c = clip("emerge", 3.0)
    tie = reach(P_(STAND, {"head": [-6, 0, 0], "right_index": [-30, 0, 0], "right_fingers": [-70, 0, 0], "right_thumb": [-30, 0, 30]}),
                "right", (-0.4, 22.6, -4.6), grip=(-ZB.ax, ZB.palm - 0.6, -1.0))
    c.key(0, P_(STAND, {"head": [22, 0, 0]}))
    c.key(1.4, P_(STAND, {"head": [20, 6, 0]}), E_IO)
    c.key(1.8, P_(STAND, {"head": [-4, 0, 0]}), E_OUT)
    c.key(2.3, tie, E_IO)
    c.key(2.5, P_(tie, {"right_hand": [10, 0, 0], "head": [-8, 0, 0]}), E_IO)
    c.key(3.0, STAND, E_IO)

    for cl in clips:
        cl.done(step=0.05)
    return f, clips


def hidden_for(clip=None, wings=False):
    h = []
    if not wings:
        h.append("wings")
    if clip not in ("stamp", "termination"):
        h.append("stamp")
    if clip != "smite":
        h.append("palm_light")
    return tuple(h)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=19000)
    m.rig.write(GEO + "zachariah.geo.json")
    save(t, "entity", "zachariah")
    save(g, "entity", "zachariah_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + "zachariah.animation.json")


if __name__ == "__main__":
    generate()
