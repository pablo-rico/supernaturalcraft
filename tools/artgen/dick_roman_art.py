"""Dick Roman (v0.25): the head of the Leviathans in his tailored vessel -- and the maw under the smile.

A tall man (33 px to the crown): dark hair slicked straight back, grey at the temples; a broad, pleased-with-himself smile
(`face`, the front half of his head); a tailored charcoal suit, white shirt, red tie (`tie`, its own bone so it can swing),
a white pocket square, polished black shoes. `maw` (a child of `head`, swapped with `face` by the renderer) is the
Leviathan's vertical maw from `leviathan_art.build_maw`, bigger: two hinged jaws lined with needle teeth, a ring of teeth
round the throat and a long three-part tongue (`tongue`, `tongue_mid`, `tongue_tip`). `pen` is the fountain pen in his
right hand (shown only while `sign_here` plays: every other clip scales it away). `goo_mantle` (phase IV) is a mass of
black goo over his shoulders with four tendrils (`goo_t1..4`, each with a lower segment `goo_t<n>b`) hanging down his back.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- face;  maw -- jaw_left -- teeth_left;  jaw_right -- teeth_right;  teeth;  tongue -- tongue_mid
                                                                                                          -- tongue_tip
               -- tie;  jacket_front_r, jacket_front_l, jacket_back;  goo_mantle -- goo_t1..4 -- goo_t1b..4b
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_thumb, pen  (left: no pen)
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
import leviathan_art as LV
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import fabric, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
A = "animation.dick_roman."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = LV.E_IO, LV.E_OUT, LV.E_IN, LV.E_BACK, LV.E_SNAP

# --- proportions (a tall man: 33 px to the crown) -----------------------------------------------------------------
HIP = 13.0
NECK = 25.0
TW = 4.0
AW = 3.6
AX = 5.9
SX = 5.5
SY = 24.0
ELBOW = 19.0
WRIST = 14.6
PALM = 12.6
LX = 2.0
LW = 3.8
KNEE = 6.8
SHOE_H = 1.6
HEM = 9.6          # the jacket's hem, below the hips

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#4a2f25", "#6e4a3b", "#936853", "#b08369", "#c79c80", "#dbb598")
HAIR = Ramp("#07070a", "#0f0f13", "#18181d", "#222229", "#2f2f38", "#4a4a56")
GREY = Ramp("#4d4d52", "#66666c", "#808086", "#9a9aa0", "#b4b4ba", "#cfcfd4")
SUIT = Ramp("#141518", "#1d1f23", "#272a2f", "#32353b", "#3e4249", "#4d525a")
SHIRT = Ramp("#9aa1aa", "#b5bbc3", "#cbd0d6", "#dde1e6", "#ebeef1", "#f8f9fb")
TIE = Ramp("#2a0306", "#48060c", "#6a0b13", "#8c121b", "#ad1d25", "#cc3a3a")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
LIPS = Ramp("#3b1d18", "#57291f", "#6e352a", "#86443a", "#9a5446", "#b06656")
TEETH = hexc("#f1ede4")
EYE_WHITE, IRIS = hexc("#dcd6cc"), hexc("#2a1a12")
PEN = Ramp("#050506", "#0c0c10", "#16161c", "#22222a", "#34343e", "#55555f")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
GOO = LV.GOO

tone = LV.tone

FACE = [   # x = 0 is the viewer's left (his right)
    "hhhhhhhhhhhhhhhh",
    "hhhhhhHHHhhhhhhh",
    "ghhhhHHhhhhhhhhg",
    "gshhhhhhhhhhhhsg",
    "ssssssssssssssss",
    "sbbbbsssssbbbbbs",
    "ssssBssssssBssss",
    "sswEEwsssswEEwss",
    "ssuuusssssuuusss",
    "sssssssnnsssssss",
    "ssssssnnnnssssss",
    "sslsssNNNNsssls.".replace(".", "s"),
    "sslmmmmmmmmmmlss",
    "ssslttttttttmlss",
    "ssssMMMMMMMMssss",
    "jssssssddsssssss",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def skin_m(seed, base=3.0):
    return H.skin_mat(SKIN, seed, base, 0.55)


def hair_m(seed):
    """Slicked back: long horizontal combed strands with a wet sheen."""
    def m(face, x, y, w, h):
        n = fbm(x * 0.5, y * 3 + len(face), seed, 64, 64, 2, 6.0)
        v = 2.0 + (n - 0.5) * 1.8
        if (y * 3 + x // 4) % 5 == 0:
            v += 1.2                          # a combed highlight
        return tone(HAIR, v)
    return m


def legend(seed):
    sk = skin_m(seed)
    hr = hair_m(seed + 1)
    shadow = lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86)
    return sk, hr, {
        "s": sk, "h": hr, "H": HAIR[5], "g": GREY[2], "b": HAIR[1], "B": SKIN[2], "w": EYE_WHITE, "E": IRIS,
        "u": shadow, "n": SKIN[4], "N": SKIN[1], "l": SKIN[2], "m": LIPS[1], "t": TEETH, "M": LIPS[4], "d": SKIN[2],
        "j": shadow,
    }


# =====================================================================================================
# Geometry
# =====================================================================================================

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("head", (0, NECK, 0), "body")
    m.bone("face", (0, NECK, 0), "head")
    m.bone("maw", (0, NECK, 0), "head")
    m.bone("tie", (0, NECK - 1.2, -2.5), "body")
    m.bone("jacket_front_r", (-2.0, HIP, -2.0), "body")
    m.bone("jacket_front_l", (2.0, HIP, -2.0), "body")
    m.bone("jacket_back", (0, HIP, 2.2), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (SX * s, SY, 0), "body")
        m.bone(f"{side}_forearm", (AX * s, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, PALM, 0), f"{side}_hand")
        m.bone(f"{side}_index", (AX * s, PALM, -1.0), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 1.0 * s, PALM + 1.0, -1.3), f"{side}_hand")
    m.bone("pen", (-AX, PALM - 0.2, -0.4), "right_hand")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_leg", (LX * s, HIP, 0), "root")
        m.bone(f"{side}_shin", (LX * s, KNEE, 0), f"{side}_leg")
    m.bone("goo_mantle", (0, NECK - 1.0, 2.6), "body")
    for i, x in enumerate((-3.0, -1.0, 1.0, 3.0)):
        m.bone(f"goo_t{i + 1}", (x, 18.5 + (0.6 if i in (1, 2) else 0), 3.2), "goo_mantle")
        m.bone(f"goo_t{i + 1}b", (x, 13.5 + (0.6 if i in (1, 2) else 0), 3.2), f"goo_t{i + 1}")


def build_head(m):
    sk, hr, lg = legend(26001)
    # The face: the front half of the head, his smile on it.
    def face(f, x, y, w, h):
        if f == "north":
            ch = FACE[y][x]
            v = lg.get(ch, sk)
            return v(f, x, y, w, h) if callable(v) else v
        if f == "up":
            return hr(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x      # 0 at the front
        if y < 3 or (fx >= w - 3 and y < 7):
            return GREY[2] if (fx >= w - 3 and 3 <= y < 7) else hr(f, x, y, w, h)   # grey at the temples
        return sk(f, x, y, w, h)
    m.cube("face", (-4, NECK, -4), (8, 8, 4), face, density=2, tag="face")
    m.cube("face", (-0.75, NECK + 2.6, -4.6), (1.5, 2.0, 0.6),
           lambda f, x, y, w, h: (SKIN[4] if y < 2 else SKIN[3]) if f == "north" else (SKIN[1] if f == "down" else SKIN[2]),
           density=2, tag="nose")
    # The hair at the front, combed back from the brow.
    m.cube("face", (-4, NECK + 7.0, -4), (8, 1.0, 4), none_on(("down",), hr), inflate=0.35, density=2, tag="hair_front")
    # The maw, and the skull behind both.
    LV.build_maw(m, "maw", NECK, FACE, lg, sk, hr, inflate=0.3, tooth=1.7, seed=26010, tongue_len=(3.4, 5.6),
                 tongue_bones=("tongue", "tongue_mid", "tongue_tip"), skull_parent="head", skull_inflate=0.0)
    # A slicked cap over the top and back of the skull.
    def cap(f, x, y, w, h):
        if f == "down" or f == "north":
            return None
        if f in ("east", "west") and y >= h - 2 and (x % 3 == 0):
            return None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, NECK + 4.5, 0), (8, 3.5, 4), cap, inflate=0.35, density=2, tag="hair_back")
    m.cube("body", (-1.7, NECK - 1.0, -1.7), (3.4, 1.2, 3.4), sk, density=2, tag="neck")


def build_body(m):
    shirt = fabric(SHIRT, 26201, 3.5, 0.12)
    suit = fabric(SUIT, 26202, 3.0, 0.3)

    def tie(f, x, y, w, h):
        return TIE[4] if x == 1 or (y + x) % 6 == 0 else TIE[2]
    m.cube("body", (-TW, HIP, -2), (2 * TW, NECK - HIP, 4), lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else SUIT[1],
           density=2, tag="torso")
    front = H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 10), v_depth=11, buttons=SUIT[0], button_rows=(13, 18))

    def jacket(f, x, y, w, h):
        c = front(f, x, y, w, h)
        if f == "north" and 4 <= y <= 5 and 11 <= x <= 13:
            return SHIRT[5] if y == 4 else SHIRT[3]       # the pocket square
        if f == "north" and y == 6 and 10 <= x <= 14:
            return SUIT[1]                                 # the breast pocket welt
        return c
    m.cube("body", (-TW, HIP - 0.4, -2), (2 * TW, NECK - HIP + 0.2, 4), none_on(("down",), jacket), inflate=0.3, density=2,
           tag="jacket")
    # The red tie: knot and blade, over the shirt in the jacket's V.
    m.cube("tie", (-0.65, NECK - 2.3, -2.75), (1.3, 1.1, 0.5), lambda f, x, y, w, h: TIE[3] if f != "up" else TIE[4], density=4,
           tag="tie_knot")
    m.cube("tie", (-0.6, NECK - 7.2, -2.62), (1.2, 5.0, 0.3), tie, density=4, tag="tie_blade")
    for s in (-1, 1):
        m.cube("body", (0.9 * s - 0.75, NECK - 1.3, -2.45), (1.5, 1.25, 0.4), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, NECK - 0.2, -2.3))
        # Peak lapels laid over the chest.
        def lapel(f, x, y, w, h, s=s):
            c = tone(SUIT, 3.9 - 1.0 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                return SUIT[2]
            return c
        m.cube("body", (2.6 * s - 1.0, 17.0, -2.75), (2.0, 7.4, 0.4), lapel, density=2, tag="lapel", rotation=(3, 0, 12 * s),
               pivot=(1.8 * s, 17.0, -2.6))
        m.cube("body", (s * (TW + 0.2) - 1.0, NECK - 1.1, -2.3), (2.0, 0.9, 4.6), lambda f, x, y, w, h: tone(SUIT, 3.8 if f == "up" else 3.0),
               density=2, tag="shoulder")
    # The jacket below the waist: two front panels meeting at the buttons, the back with a centre vent.
    L = HIP + 0.2 - HEM
    for bone, s in (("jacket_front_r", -1), ("jacket_front_l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f == "south":
                return SUIT[0]
            c = suit(f, x, y + 20, w, h)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return SUIT[4]
            if f == "north" and 2 <= y <= 3 and 1 <= (w - 1 - x if s < 0 else x) <= w - 3:
                return SUIT[1] if y == 3 else SUIT[4]      # flap pocket
            if y >= h - 1:
                return SUIT[1]
            return c
        xa, xb = (-TW - 0.4, 0.0) if s < 0 else (0.0, TW + 0.4)
        m.cube(bone, (xa, HEM, -2.6), (xb - xa, L, 0.4), panel, density=2, tag="jacket_skirt", rotation=(-3, 0, 0),
               pivot=(0, HIP, -2.4))
        xs = -TW - 0.4 if s < 0 else TW
        m.cube(bone, (xs, HEM, -2.4), (0.4, L, 4.8), none_on(("up",), suit), density=2, tag="jacket_side",
               rotation=(0, 0, 2 * s), pivot=(xs, HIP, 0))

    def back(f, x, y, w, h):
        if f == "north":
            return SUIT[0]
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.4:
            return SUIT[0]
        return suit(f, x, y + 20, w, h)
    m.cube("jacket_back", (-TW - 0.4, HEM, 2.2), (2 * TW + 0.8, L, 0.4), back, density=2, tag="jacket_back", rotation=(3, 0, 0),
           pivot=(0, HIP, 2.2))


def build_arms(m):
    suit = fabric(SUIT, 26301, 3.0, 0.3)
    hand = H.skin_mat(SKIN, 26302, 3.1, 0.4)
    shirt = fabric(SHIRT, 26303, 3.7, 0.1)
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.6 - ELBOW, AW), suit, inflate=0.15, density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.7, WRIST + 0.5, -1.7), (3.4, ELBOW - WRIST - 0.5, 3.4), none_on(("up",), suit), inflate=0.15,
               density=2, tag="forearm_sleeve")
        m.cube(f"{side}_forearm", (cx - 1.5, WRIST - 0.1, -1.5), (3.0, 0.7, 3.0),
               none_on(("up",), lambda f, x, y, w, h: GOLD[3] if (f == "west" if s < 0 else f == "east") and x == 2 and y == 0
                       else shirt(f, x, y, w, h)), inflate=0.1, density=2, tag="shirt_cuff")
        m.cube(f"{side}_hand", (cx - 1.3, PALM, -1.45), (2.6, WRIST - PALM, 2.9), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.88 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_fingers", (cx - 1.25, PALM - 1.0, -0.6), (2.5, 1.0, 2.0), fing, density=2, tag="fingers")
        m.cube(f"{side}_index", (cx - 1.25, PALM - 1.1, -1.45), (2.5, 1.1, 0.8), fing, density=2, tag="index")
        m.cube(f"{side}_thumb", (cx - 1.0 * s - 0.5, PALM + 0.2, -2.05), (1.0, 1.5, 0.9), hand, density=2, tag="thumb")
    # The fountain pen, held in the right fist, nib pointing forward.
    m.cube("pen", (-AX - 0.3, PALM - 0.6, -3.8), (0.6, 0.6, 5.2), lambda f, x, y, w, h: GOLD[3] if (f in ("up", "east", "west")
           and (x == 3 if f == "up" else x in (3,))) else tone(PEN, 3.2 if f in ("up", "west") else 2.0), density=4, tag="pen_barrel")
    m.cube("pen", (-AX - 0.2, PALM - 0.5, -4.6), (0.4, 0.4, 0.8), lambda f, x, y, w, h: GOLD[4] if f != "down" else GOLD[2],
           density=4, tag="pen_nib")
    m.cube("pen", (-AX - 0.35, PALM - 0.65, 0.9), (0.7, 0.7, 0.5), solid(GOLD[3]), density=4, tag="pen_cap")


def build_legs(m):
    for side, s in (("right", -1), ("left", 1)):
        tr = fabric(SUIT, 26401 + (s > 0), 2.9, 0.25, crease=True)
        x0 = LX * s - LW / 2
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), tr, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, SHOE_H, -LW / 2), (LW, KNEE - SHOE_H, LW), tr, density=2, tag="shin")

        def shoe(f, x, y, w, h):
            c = tone(SHOE, 2.4 - 0.8 * y / max(1, h))
            if f == "up" or (f in ("north", "east", "west") and y == 0):
                c = SHOE[4]
            if f == "up" and (x + y) % 5 == 0:
                c = SHOE[5]                       # the shine
            return c
        m.cube(f"{side}_shin", (x0 - 0.1, 0, -2.9), (LW + 0.2, SHOE_H, 4.9), shoe, density=2, tag="shoe")


def build_mantle(m):
    goo = LV.goo_m(26501)
    m.cube("goo_mantle", (-4.6, 17.0, 2.2), (9.2, 7.6, 1.2), goo, density=2, tag="mantle")
    m.cube("goo_mantle", (-3.4, 19.0, 3.2), (6.8, 5.0, 0.8), goo, density=2, tag="mantle_heap")
    for s in (-1, 1):
        m.cube("goo_mantle", (s * 4.4 - 1.6, NECK - 1.6, -1.0), (3.2, 1.4, 3.8), goo, density=2, tag="mantle_shoulder")
    for i in range(4):
        for seg, (top, ln, w) in ((f"goo_t{i + 1}", (18.5, 5.4, 1.3)), (f"goo_t{i + 1}b", (13.5, 5.6, 1.0))):
            piv = m.rig.get(seg).pivot
            m.cube(seg, (piv[0] - w / 2, piv[1] - ln, piv[2] - w / 2), (w, ln, w), goo, density=2, tag="tendril")
        m.cube(f"goo_t{i + 1}b", (m.rig.get(f"goo_t{i + 1}b").pivot[0] - 0.7, m.rig.get(f"goo_t{i + 1}b").pivot[1] - 6.4,
                                   2.5), (1.4, 0.9, 1.4), goo, density=2, tag="tendril_tip")


def rig():
    m = Model("dick_roman", 256, 256)
    skeleton(m)
    build_head(m)
    build_body(m)
    build_arms(m)
    build_legs(m)
    build_mantle(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

Clip = H.Clip
OPEN = 105.0


def maw(k=1.0, splay=8.0, tongue=1.0, droop=25.0):
    return {"jaw_left": [0, -OPEN * k, splay * k], "jaw_right": [0, OPEN * k, -splay * k],
            "tongue": [droop * tongue, 0, 0], "@tongue": [0, -0.6 * tongue, -2.8 * tongue],
            "tongue_mid": [8 * tongue, -180 * tongue, 0], "tongue_tip": [14 * tongue, 0, 0]}


def merge(*ps):
    out = {}
    for p in ps:
        out.update(p)
    return out


def jitter(seed, t, amp):
    """A deterministic tremble vector in [-amp, amp]."""
    return [amp * (2 * h01(seed, t, i) - 1) for i in range(3)]


IDLE = {"right_arm": [-8, 0, -6], "left_arm": [-8, 0, 6], "right_forearm": [-38, -18, 0], "left_forearm": [-38, 18, 0],
        "right_hand": [0, 0, 0], "head": [-3, 0, 0]}
MAW_REST = merge(maw(1.0), {"head": [-8, 0, 0], "right_arm": [-6, 0, 6], "left_arm": [-6, 0, -6],
                            "right_forearm": [-14, 0, 0], "left_forearm": [-14, 0, 0]})
POINT = {"right_index": [0, 0, 0], "right_fingers": [70, 0, 0]}


def mantle_sway(t, amp=1.0):
    out = {}
    for i in range(4):
        ph = t * 2 * math.pi / 2.0 + i * 1.3
        out[f"goo_t{i + 1}"] = [6 * amp * math.sin(ph), 0, 5 * amp * math.sin(ph * 0.7 + 1)]
        out[f"goo_t{i + 1}b"] = [9 * amp * math.sin(ph - 0.9), 0, 6 * amp * math.sin(ph * 0.7)]
    return out


def anims():
    f = AnimFile()

    # --- loops ---------------------------------------------------------------------------------------------------
    idle = Clip(f, A + "idle", 4.0, loop=True)
    for t in (0, 1.0, 2.0, 3.0, 4.0):
        b = math.sin(t / 4.0 * 2 * math.pi)
        idle.key(t, merge(IDLE, mantle_sway(t, 0.5), {"@body": [0, -0.25 * (1 - math.cos(t / 4.0 * 2 * math.pi)), 0],
                                                      "head": [-3 + 2 * b, 6 * b, -2 * b]}), E_IO if t else None)
    idle.done()

    walk = Clip(f, A + "walk", 1.1, loop=True)
    for t, s in ((0, 1), (0.55, -1), (1.1, 1)):
        walk.key(t, merge(mantle_sway(t, 1.0), {
            "right_leg": [24 * s, 0, 0], "left_leg": [-24 * s, 0, 0], "right_shin": [6 if s < 0 else 20, 0, 0],
            "left_shin": [20 if s < 0 else 6, 0, 0], "right_arm": [-16 * s, 0, 3], "left_arm": [16 * s, 0, -3],
            "right_forearm": [-14, 0, 0], "left_forearm": [-14, 0, 0], "body": [0, 6 * s, 0], "head": [-2, -5 * s, 0],
            "jacket_front_r": [-6 * s, 0, 0], "jacket_front_l": [6 * s, 0, 0], "jacket_back": [4, 0, 0], "tie": [-4, 0, 3 * s]}),
                 E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.275, [0, 0.5, 0], E_OUT), (0.55, [0, 0, 0], E_IN), (0.825, [0, 0.5, 0], E_OUT),
                  (1.1, [0, 0, 0], E_IN))
    walk.done()

    mi = Clip(f, A + "maw_idle", 2.0, loop=True)
    for t in (0, 0.5, 1.0, 1.5, 2.0):
        b = math.sin(t / 2.0 * 2 * math.pi)
        mi.key(t, merge(MAW_REST, mantle_sway(t, 0.7), {
            "jaw_left": [0, -OPEN - 6 * b, 8 + 3 * b], "jaw_right": [0, OPEN + 6 * b, -8 - 3 * b],
            "tongue": [25 + 8 * b, 6 * b, 0], "tongue_mid": [8 - 6 * b, -180, 4 * b], "tongue_tip": [14 + 10 * b, 0, 0],
            "head": [-8 + 3 * b, 4 * b, 0]}), E_IO if t else None)
    mi.done()

    # --- phase I: the businessman ---------------------------------------------------------------------------------
    # Downsizing: a backhanded sweep of the right arm, landing on tick 18.
    ds = Clip(f, A + "downsizing", 1.5)
    ds.key(0, IDLE)
    ds.key(0.6, {"right_arm": [-75, 0, -55], "right_forearm": [-40, 0, 0], "right_hand": [0, 0, 20], "body": [0, -28, 0],
                 "head": [0, 10, 0], "left_arm": [-10, 0, -8], "right_leg": [-6, 0, 0], "left_leg": [6, 0, 0]}, E_OUT)
    ds.key(0.9, {"right_arm": [-85, 0, 75], "right_forearm": [-8, 0, 0], "right_hand": [0, 0, -25], "body": [0, 26, 0],
                 "head": [0, -6, 0], "left_arm": [-6, 0, -14], "right_leg": [-6, 0, 0], "left_leg": [6, 0, 0],
                 "tie": [0, 0, 12], "jacket_front_r": [-10, 0, 0]}, E_SNAP)
    ds.key(1.1, {"right_arm": [-70, 0, 70], "right_forearm": [-10, 0, 0], "body": [0, 20, 0], "head": [0, -6, 0],
                 "left_arm": [-6, 0, -12], "tie": [0, 0, 4]}, E_OUT)
    ds.key(1.5, IDLE, E_IO)
    ds.done()

    # Sign here: takes the pen from his inside pocket, signs the air, points it at you on tick 22.
    sh = Clip(f, A + "sign_here", 1.8)
    sh.key(0, IDLE)
    sh.key(0.35, {"right_arm": [-28, 0, -34], "right_forearm": [-95, -30, 0], "right_hand": [0, 0, 0], "head": [10, -8, 0],
                  "left_arm": [-4, 0, 4]}, E_IO)
    sign = lambda dz, dx: {"right_arm": [-72, 0, -6], "right_forearm": [-30, 0, 0], "right_hand": [dx, 0, dz],
                           "head": [4, 0, 0], "left_arm": [-4, 0, 4]}
    sh.key(0.55, sign(-14, -10), E_OUT)
    for i, t in enumerate((0.65, 0.75, 0.85, 0.95)):
        sh.key(t, sign(14 if i % 2 == 0 else -12, 8 if i % 2 else -6))
    sh.key(1.1, merge({"right_arm": [-88, 0, 4], "right_forearm": [-4, 0, 0], "right_hand": [-10, 0, 0], "body": [0, 6, 0],
                       "head": [-4, 2, 0], "left_arm": [-4, 0, 4]}, POINT), E_SNAP)
    sh.key(1.45, merge({"right_arm": [-84, 0, 4], "right_forearm": [-6, 0, 0], "right_hand": [-8, 0, 0], "body": [0, 6, 0],
                        "head": [-4, 2, 0], "left_arm": [-4, 0, 4]}, POINT))
    sh.key(1.8, IDLE, E_IO)
    sh.done()

    # Staff meeting: raises his right hand and snaps his fingers on tick 14.
    sm = Clip(f, A + "staff_meeting", 1.5)
    sm.key(0, IDLE)
    raised = lambda hz, th, idx: {"right_arm": [-30, 0, 34], "right_forearm": [-105, -20, 0], "right_hand": [0, 0, hz],
                                  "right_thumb": [th, 0, 0], "right_index": [idx, 0, 0], "right_fingers": [60, 0, 0],
                                  "head": [-6, -10, 0], "left_arm": [-4, 0, 4]}
    sm.key(0.45, raised(0, -20, 30), E_OUT)
    sm.key(0.62, raised(-8, -30, 40))
    sm.key(0.7, raised(18, 20, 90), E_SNAP)
    sm.key(0.95, raised(10, 10, 80))
    sm.key(1.5, IDLE, E_IO)
    sm.done()

    # --- phase II: the maw ----------------------------------------------------------------------------------------
    mo = Clip(f, A + "maw_open", 2.0, hold=True)
    mo.key(0, {})
    mo.key(0.4, merge(maw(0.03, 0, 0), {"head": [16, 0, 0], "body": [8, 0, 0], "right_arm": [0, 0, 6], "left_arm": [0, 0, -6]}), E_IO)
    for i, t in enumerate((0.5, 0.6, 0.7)):
        mo.key(t, merge(maw(0.06, 0, 0), {"head": [16, 5 * (1 if i % 2 else -1), 4], "body": [9, 0, 0], "right_arm": [0, 0, 8],
                                          "left_arm": [0, 0, -8]}))
    mo.key(1.1, merge(maw(1.2, 16, 1.0, 0), {"head": [-30, 0, 0], "body": [-10, 0, 0], "right_arm": [-16, 0, 30],
                                               "left_arm": [-16, 0, -30], "right_forearm": [-30, 0, 0],
                                               "left_forearm": [-30, 0, 0]}), E_BACK)
    mo.key(1.5, merge(maw(1.1, 12, 1.0, 30), {"head": [-14, 0, 0], "body": [-3, 0, 0], "right_arm": [-10, 0, 14],
                                                "left_arm": [-10, 0, -14], "right_forearm": [-20, 0, 0],
                                                "left_forearm": [-20, 0, 0]}), E_IO)
    mo.key(2.0, MAW_REST, E_IO)
    mo.done()

    bt = Clip(f, A + "bite", 1.2)
    bt.key(0, MAW_REST)
    bt.key(0.4, merge(maw(1.3, 18, 1.0, 5), {"head": [-30, 0, 0], "body": [-14, 0, 0], "right_arm": [-30, 0, 20],
                                               "left_arm": [-30, 0, -20], "right_leg": [-6, 0, 0], "left_leg": [10, 0, 0]}), E_OUT)
    bt.key(0.6, merge(maw(0.02, 0, 0.2, 0), {"head": [24, 0, 0], "body": [28, 0, 0], "@body": [0, -0.6, -3.0],
                                              "right_arm": [-60, 0, 14], "left_arm": [-60, 0, -14], "right_leg": [-24, 0, 0],
                                              "left_leg": [18, 0, 0], "left_shin": [22, 0, 0]}), E_SNAP)
    bt.key(0.8, merge(maw(0.08, 0, 0.2, 0), {"head": [18, -8, 0], "body": [20, 0, 0], "@body": [0, -0.4, -2.2],
                                              "right_arm": [-50, 0, 12], "left_arm": [-50, 0, -12], "right_leg": [-18, 0, 0],
                                              "left_leg": [14, 0, 0], "left_shin": [16, 0, 0]}))
    bt.key(1.2, MAW_REST, E_IO)
    bt.done()

    sw = Clip(f, A + "swallow", 1.5)
    sw.key(0, MAW_REST)
    sw.key(0.3, merge(maw(1.35, 20, 1.0, -10), {"head": [12, 0, 0], "body": [22, 0, 0], "@body": [0, -0.4, -2.0],
                                                  "right_arm": [-40, 0, 24], "left_arm": [-40, 0, -24]}), E_OUT)
    sw.key(0.5, merge(maw(0.0, 0, 0.0, 0), {"head": [-6, 0, 0], "body": [6, 0, 0], "right_arm": [-10, 0, 10],
                                             "left_arm": [-10, 0, -10]}), E_SNAP)
    for i, t in enumerate((0.7, 0.85, 1.0)):
        sw.key(t, merge(maw(0.0, 0, 0.0, 0), {"head": [-20 if i % 2 == 0 else 6, 0, 0], "body": [-4 if i % 2 == 0 else 4, 0, 0],
                                               "right_arm": [-6, 0, 10], "left_arm": [-6, 0, -10]}), E_IO)
    sw.key(1.5, MAW_REST, E_IO)
    sw.done()

    sp = Clip(f, A + "spit", 1.0)
    sp.key(0, MAW_REST)
    sp.key(0.25, merge(maw(0.05, 0, 0), {"head": [-22, 0, 0], "body": [-10, 0, 0], "right_arm": [-10, 0, 14],
                                          "left_arm": [-10, 0, -14]}), E_OUT)
    sp.key(0.4, merge(maw(1.3, 22, 1.0, -20), {"head": [18, 0, 0], "body": [14, 0, 0], "@body": [0, 0, -1.5],
                                                 "right_arm": [-20, 0, 20], "left_arm": [-20, 0, -20]}), E_SNAP)
    sp.key(1.0, MAW_REST, E_IO)
    sp.done()

    gs = Clip(f, A + "goo_spew", 1.6)
    gs.key(0, MAW_REST)
    hunch = lambda k, y: merge(maw(0.4 * k, 4), {"head": [24 * k, y, 0], "body": [26 * k, 0, 0],
                                                  "right_arm": [-20 * k, 0, 26 * k], "left_arm": [-20 * k, 0, -26 * k],
                                                  "right_forearm": [-40 * k, 0, 0], "left_forearm": [-40 * k, 0, 0]})
    gs.key(0.3, hunch(1.0, 0), E_OUT)
    gs.key(0.45, hunch(0.7, 0))
    gs.key(0.6, hunch(1.1, 0), E_IN)
    spew = lambda yaw: merge(maw(1.35, 22, 1.0, -25), {"head": [-6, yaw, 0], "body": [12, yaw * 0.4, 0], "@body": [0, 0, -1.2],
                                                        "right_arm": [-26, 0, 34], "left_arm": [-26, 0, -34],
                                                        "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0],
                                                        "right_leg": [-10, 0, 0], "left_leg": [8, 0, 0]})
    gs.key(0.8, spew(-34), E_SNAP)
    gs.key(1.25, spew(34), E_IO)
    gs.key(1.6, MAW_REST, E_IO)
    gs.done()

    # Copy: shudders, his shape crawling, then straightens into someone else on tick 30.
    cp = Clip(f, A + "copy", 2.0)
    cp.key(0, {})
    for i in range(1, 14):
        t = round(0.1 * i, 2)
        k = min(1.0, t / 0.8)
        j = jitter("copy", i, 7 * k)
        cp.key(t, {"body": [10 * k + j[0], j[1], j[2] * 0.6], "head": [16 * k - j[1], j[2] * 1.5, j[0]],
                   "right_arm": [-10 + j[2], 0, 18 * k + j[0]], "left_arm": [-10 - j[1], 0, -18 * k - j[2]],
                   "right_forearm": [-40 * k, 0, 0], "left_forearm": [-40 * k, 0, 0], "@root": [0, -0.8 * k, 0],
                   "right_shin": [10 * k, 0, 0], "left_shin": [10 * k, 0, 0], "right_leg": [-6 * k, 0, 0], "left_leg": [-6 * k, 0, 0]})
    cp.key(1.5, {"body": [-8, 0, 0], "head": [-14, 0, 0], "right_arm": [-6, 0, 22], "left_arm": [-6, 0, -22],
                 "@root": [0, 0.4, 0]}, E_BACK)
    cp.key(2.0, {}, E_IO)
    cp.done()

    # Tentacle slam: both arms overhead, then down into the floor on tick 18.
    ts = Clip(f, A + "tentacle_slam", 1.5)
    ts.key(0, {})
    ts.key(0.6, {"right_arm": [-170, 0, 14], "left_arm": [-170, 0, -14], "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
                 "body": [-14, 0, 0], "head": [-16, 0, 0], "@root": [0, 0.4, 0]}, E_OUT)
    slam = {"right_arm": [-40, 0, 10], "left_arm": [-40, 0, -10], "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0],
            "right_hand": [20, 0, 0], "left_hand": [20, 0, 0], "body": [52, 0, 0], "head": [-20, 0, 0], "@root": [0, -2.6, 0],
            "right_leg": [-40, 0, 6], "left_leg": [-40, 0, -6], "right_shin": [48, 0, 0], "left_shin": [48, 0, 0],
            "jacket_back": [-14, 0, 0], "tie": [-30, 0, 0], **mantle_sway(0.3, 1.6)}
    ts.key(0.9, slam, E_SNAP)
    ts.key(1.15, slam)
    ts.key(1.5, {}, E_IO)
    ts.done()

    # --- phase IV and the end -------------------------------------------------------------------------------------
    ml = Clip(f, A + "melt", 3.0)
    ml.key(0, {})
    sag = lambda k, t: merge(mantle_sway(t, 1.5 * k), {
        "body": [26 * k, 0, 6 * k], "head": [34 * k, 0, -12 * k], "@root": [0, -3.2 * k, 0], "@body": [0, -0.8 * k, 0],
        "right_arm": [-14 * k, 0, -4 * k], "left_arm": [-20 * k, 0, 10 * k], "right_forearm": [-12 * k, 0, 0],
        "left_forearm": [-20 * k, 0, 0], "right_hand": [30 * k, 0, 0], "left_hand": [30 * k, 0, 0],
        "right_leg": [-30 * k, 0, 8 * k], "left_leg": [-26 * k, 0, -8 * k], "right_shin": [44 * k, 0, 0],
        "left_shin": [40 * k, 0, 0], "jacket_front_r": [-10 * k, 0, 0], "jacket_front_l": [-10 * k, 0, 0],
        "tie": [-18 * k, 0, 0], **maw(0.25 * k, 0, 0)})
    ml.key(0.8, sag(0.6, 0.8), E_IN)
    ml.key(1.6, sag(1.0, 1.6), E_IO)
    ml.key(2.2, sag(1.05, 2.2))
    ml.key(3.0, merge(mantle_sway(3.0, 0.5), {}), E_IO)
    ml.done()

    st = Clip(f, A + "stagger", 0.8)
    st.key(0, {})
    st.key(0.15, {"body": [-18, 8, 0], "head": [-24, -10, 6], "right_arm": [-30, 0, 40], "left_arm": [-20, 0, -46],
                  "right_forearm": [-30, 0, 0], "left_forearm": [-40, 0, 0], "right_leg": [14, 0, 0], "left_leg": [-6, 0, 0],
                  "@root": [0, 0, 1.2], "tie": [16, 0, 0]}, E_OUT)
    st.key(0.4, {"body": [-8, 4, 0], "head": [-8, -4, 0], "right_arm": [-14, 0, 20], "left_arm": [-10, 0, -20],
                 "right_leg": [8, 0, 0], "@root": [0, 0, 0.8]}, E_IO)
    st.key(0.8, {}, E_IO)
    st.done()

    # Death: clutches at his chest, convulses ever harder, bursts open from the inside on tick 110, falls to his knees.
    dt = Clip(f, A + "death", 7.0, hold=True)
    dt.key(0, {})
    clutch = {"right_arm": [-40, 0, -30], "left_arm": [-40, 0, 30], "right_forearm": [-80, -30, 0], "left_forearm": [-80, 30, 0],
              "body": [14, 0, 0], "head": [18, 0, 0]}
    dt.key(0.6, clutch, E_OUT)
    for i in range(1, 25):
        t = round(0.6 + 0.2 * i, 2)
        if t >= 5.3:
            break
        k = (t - 0.6) / 4.7
        j = jitter("death", i, 4 + 14 * k)
        arch = min(1.0, max(0.0, (t - 2.8) / 2.0))
        pose = {"right_arm": [-40 + 30 * arch + j[0], 0, -30 + 60 * arch + j[1]],
                "left_arm": [-40 + 30 * arch - j[1], 0, 30 - 60 * arch - j[2]],
                "right_forearm": [-80 + 50 * arch, -30 * (1 - arch), 0], "left_forearm": [-80 + 50 * arch, 30 * (1 - arch), 0],
                "body": [14 - 30 * arch + j[2], j[0] * 0.5, j[1] * 0.4], "head": [18 - 50 * arch + j[1], j[2], j[0]],
                "@root": [0, -1.2 * k, 0], "right_shin": [12 * k, 0, 0], "left_shin": [12 * k, 0, 0],
                "right_leg": [-6 * k, 0, 0], "left_leg": [-6 * k, 0, 0], **maw(0.3 * arch, 0, 0)}
        dt.key(t, pose)
    burst = merge(maw(1.45, 30, 1.0, -30), mantle_sway(5.5, 2.0), {
        "right_arm": [-30, 0, 85], "left_arm": [-30, 0, -85], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
        "right_hand": [-40, 0, 0], "left_hand": [-40, 0, 0], "body": [-24, 0, 0], "head": [-50, 0, 0], "@root": [0, 0.6, 0],
        "jacket_front_r": [-30, 0, -20], "jacket_front_l": [-30, 0, 20], "jacket_back": [30, 0, 0], "tie": [-60, 0, 0]})
    dt.key(5.5, burst, E_SNAP)
    dt.key(5.9, burst)
    fall = merge(maw(0.6, 10, 1.0, 50), mantle_sway(6.5, 1.0), {
        "@root": [0, -5.6, 0], "right_leg": [-6, 0, 4], "left_leg": [-6, 0, -4], "right_shin": [88, 0, 0], "left_shin": [88, 0, 0],
        "body": [34, 0, 4], "head": [40, 0, -10], "right_arm": [-12, 0, 14], "left_arm": [-8, 0, -18],
        "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "jacket_front_r": [-12, 0, 0], "jacket_front_l": [-12, 0, 0],
        "tie": [-30, 0, 0]})
    dt.key(6.5, fall, E_IN)
    dt.key(7.0, fall)
    dt.done()

    # Emerge: steps forward, buttoning his jacket, a tug at the hem.
    em = Clip(f, A + "emerge", 2.5)
    em.key(0, {"right_leg": [-20, 0, 0], "left_leg": [14, 0, 0], "left_shin": [16, 0, 0], "@root": [0, 0, 3.0], "body": [6, 0, 0]})
    em.key(0.5, {"right_leg": [10, 0, 0], "left_leg": [-16, 0, 0], "right_shin": [12, 0, 0], "@root": [0, 0, 1.0]}, E_IO)
    em.key(0.8, {"@root": [0, 0, 0]}, E_IO)
    button = lambda d: {"right_arm": [-22, 0, -18], "left_arm": [-22, 0, 18], "right_forearm": [-70, -40 - d, 0],
                        "left_forearm": [-70, 40 + d, 0], "right_hand": [0, 0, d], "left_hand": [0, 0, -d], "head": [16, 0, 0]}
    em.key(1.2, button(0), E_IO)
    em.key(1.45, button(10))
    em.key(1.65, button(-4))
    em.key(2.0, {"right_arm": [-6, 0, -2], "left_arm": [-6, 0, 2], "right_forearm": [-18, 0, 0], "left_forearm": [-18, 0, 0],
                 "right_hand": [24, 0, 0], "left_hand": [24, 0, 0], "head": [-4, 6, 0], "jacket_front_r": [4, 0, 0],
                 "jacket_front_l": [4, 0, 0]}, E_IO)
    em.key(2.5, IDLE, E_IO)
    em.done()

    # The pen shows only while he signs; everywhere else it is scaled away.
    for a in f.anims:
        if a.name == A + "sign_here":
            a.scale("pen", (0, [1, 1, 1]), (a.length, [1, 1, 1]))
        else:
            a.scale("pen", (0, [0, 0, 0]))
    return f


def generate():
    m = rig()
    t, _ = m.build(gutter=1, seed=26000)
    m.rig.write(GEO + "dick_roman.geo.json")
    save(t, "entity", "dick_roman")
    write_compact(anims(), ANIM + "dick_roman.animation.json")


if __name__ == "__main__":
    generate()
