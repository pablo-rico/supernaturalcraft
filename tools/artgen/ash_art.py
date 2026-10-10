"""Ash (v0.18), behind the bar of the Roadhouse in Heaven: GeckoLib rig, texture and clips (names in HeavenAssets.ASH_*).

The genius of the Roadhouse, as he was: business in the front (short, spiky, a ragged fringe), party in the back (a mullet
falling past his collar), a scruffy goatee and a lopsided grin. A beaten black leather jacket worn open (a pointed collar,
zips down its edges, the sleeves shoved up his forearms), a faded black band tee with a cracked winged-skull print, worn
blue jeans on a belt with a big silver buckle, scuffed boots, a leather cuff on his left wrist, and the bar towel over his
left shoulder (`towel`).

Props switched by scale (the loops key them, so the renderer does nothing): `glass` (a pint glass, right hand) and `rag`
(the towel bunched in his left hand) only in `wipe_glass`, where the shoulder `towel` is gone; `mouth` (his open mouth: the
teeth and the dark inside) only while he talks or laughs.

Skeleton (Bedrock px; +X is his LEFT; the model faces north), legacy_art's man plus:
  root -- body -- head -- hair_back -- mullet_tail;  head -- brows, mouth
               -- jacket_front_r, jacket_front_l, jacket_back, collar_r, collar_l
               -- towel -- towel_front, towel_back
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_thumb, glass
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_thumb, rag -- rag_tail
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from gabriel_art import open_shell
from legacy_art import (Build, CLAW_L, FIST_L, FIST_R, JACKET, LOOSE, OPEN_L, OPEN_R, POINT_R, SIDES, boot_m, denim_m, done, ears_nose,
                        fix, hair_m, hands, head_cube, leather_m, legs, reach, skeleton, worn)
from michael_art import (ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, aim, to_preview, tone, world_point, write_compact)
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.ash."
CLEAR = (0, 0, 0, 0)

B = Build(hip=12.0, neck=24.0, tw=3.7, aw=3.4, ax=5.5, sx=5.1, sy=23.0, elbow=18.0, wrist=13.6, palm=11.6, lx=1.85, lw=3.5, knee=6.2)

SKIN = Ramp("#5f3f30", "#8d6553", "#b1846c", "#c99c82", "#dab198", "#e8c6ae")
HAIR = Ramp("#22170c", "#3a2914", "#55401f", "#6e5629", "#8a6e36", "#a88b4a")
EYE = hexc("#4a3424")
EYE_WHITE = hexc("#e6ddd1")
LIPS = Ramp("#5a2e25", "#7d4337", "#97594a", "#ab6a5a", "#bd7e6c", "#cf927f")
TEE = Ramp("#0f0f11", "#18181b", "#212125", "#2b2b30", "#37373d", "#45454c")
PRINT = hexc("#c4bba5")
PRINT_RED = hexc("#8f2a22")
DENIM = Ramp("#18243a", "#233453", "#2f456b", "#3d5a85", "#517199", "#6d8bb2")
BOOT = Ramp("#120c08", "#1f150e", "#2e2016", "#3e2c1f", "#4f3a2a", "#634a37")
TOWEL = Ramp("#857e6f", "#a29b8a", "#bdb7a6", "#d2cdbe", "#e3dfd3", "#f2f0e8")
STRIPE = Ramp("#4a120e", "#6b1b15", "#8d251c", "#a83127", "#c04536", "#d2614f")
STEEL = Ramp("#2a2f36", "#454c56", "#68717d", "#8f98a3", "#b8c0c9", "#e4e9ee")
MOUTH = Ramp("#170605", "#2c0d0a", "#46150f", "#5e2018", "#7a2f25", "#94443a")
TEETH = hexc("#e9e2d0")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhshhhsshhhhshhh",
    "sbbbbssssssbbbbs",
    "ssssssssssssssss",
    "sswEEssssssEEwss",
    "ssuuusssssssuuss",
    "sssssssnnsssssss",
    "sssssssnnsssssss",
    "tsssssNnnNssssst",
    "tstsbbbbbbbbstst",
    "ttsssmmmmmmLssst",
    "tttttsggggsstttt",
    "jtttttggggtttttj",
]


# =====================================================================================================
# The rig
# =====================================================================================================

def rig():
    m = Model("ash", 256, 256)
    m.rig.bone("root", (0, 0, 0))
    skeleton(m, B)
    b = B
    m.bone("hair_back", (0, b.neck + 5.5, 3.2), "head")
    m.bone("mullet_tail", (0, b.neck + 0.5, 3.6), "hair_back")
    m.bone("brows", (0, b.neck + 5.3, -4.0), "head")
    m.bone("mouth", (0, b.neck + 2.0, -4.0), "head")
    m.bone("jacket_front_r", (-2.0, b.hip + 1.0, -2.0), "body")
    m.bone("jacket_front_l", (2.0, b.hip + 1.0, -2.0), "body")
    m.bone("jacket_back", (0, b.hip + 1.0, 2.1), "body")
    m.bone("collar_r", (-1.8, b.neck - 0.4, -1.6), "body")
    m.bone("collar_l", (1.8, b.neck - 0.4, -1.6), "body")
    m.bone("towel", (3.4, b.neck + 0.2, 0), "body")
    m.bone("towel_front", (3.4, b.neck - 0.3, -2.6), "towel")
    m.bone("towel_back", (3.4, b.neck - 0.3, 2.6), "towel")
    m.bone("glass", (-b.ax + 2.3, b.palm - 0.4, -0.2), "right_hand")
    m.bone("rag", (b.ax, b.palm - 0.6, -0.2), "left_hand")
    m.bone("rag_tail", (b.ax + 0.6, b.palm - 1.6, 0.2), "rag")
    build_head(m)
    build_body(m)
    build_towel(m)
    build_props(m)
    return m


def build_head(m):
    b = B
    sk = H.skin_mat(SKIN, 18101, 3.2, 0.45)
    hr = hair_m(HAIR, 18102, 2.8)
    stub = H.stubble(sk, HAIR, 18103, 0.35)
    goatee = lambda f, x, y, w, h: tone(HAIR, 2.0 + 0.8 * h01("gt", x, y))
    legend = {"h": hr, "s": sk, "t": stub, "b": HAIR[1], "w": EYE_WHITE, "E": EYE, "u": mix(SKIN[2], hexc("#6b5a66"), 0.2),
              "n": SKIN[4], "N": SKIN[1], "m": LIPS[1], "L": LIPS[3], "g": goatee,
              "j": lambda f, x, y, w, h: shade(stub(f, x, y, w, h), 0.9)}
    sides = H.head_sides(sk, hr, top=5, back=16, temple=4, sideburn=11, beard=stub, beard_from=12)
    head_cube(m, "head", b.neck, fix(FACE), legend, sides)
    ears_nose(m, "head", b.neck, SKIN, nose_h=1.8)
    m.cube("body", (-1.5, b.neck - 0.6, -1.5), (3, 1, 3), stub, density=2, tag="neck")
    # The goatee jutting a little off his chin.
    m.cube("head", (-1.0, b.neck - 0.4, -4.35), (2.0, 1.4, 0.6), lambda f, x, y, w, h: tone(HAIR, 1.8 + 0.9 * h01("gc", f, x, y)),
           density=2, tag="goatee")
    # Bushy brows (their own bone: they go up when he talks and laughs).
    for s in (-1, 1):
        m.cube("brows", (s * 2.2 - 1.3, b.neck + 5.1, -4.25), (2.6, 0.5, 0.3), lambda f, x, y, w, h: HAIR[1 + (x % 3 == 0)],
               density=4, tag="brow", rotation=(0, 0, 6 * s), pivot=(s * 1.2, b.neck + 5.3, -4.2))
    # Business in the front: a shell over the crown, a ragged spiky fringe and spikes standing up on top.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 or (y < 4 and (x * 7) % 5 < 2) else None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, b.neck + 6.0, -4), (8, 2.0, 8), shell, inflate=0.35, density=2, tag="hair")
    spike = lambda f, x, y, w, h: tone(HAIR, (3.6 if f in ("up", "north") else 2.4) + 0.6 * h01("sp", f, x, y) - 0.7 * y / max(1, h))
    for i, (x0, z0, tilt, roll) in enumerate(((-3.0, -3.4, -32, 12), (-1.2, -3.8, -38, -4), (0.8, -3.6, -34, 8), (2.6, -3.2, -30, -14),
                                              (-2.2, -1.2, -20, 18), (0.0, -1.4, -24, 0), (2.2, -1.0, -18, -16), (-1.0, 1.0, -8, 10),
                                              (1.2, 1.2, -6, -10))):
        m.cube("head", (x0 - 0.7, b.neck + 8.0, z0 - 0.7), (1.4, 1.6 + 0.3 * (i % 3), 1.4), none_on(("down",), spike), density=2,
               tag="spike", rotation=(tilt, 0, roll), pivot=(x0, b.neck + 8.0, z0))
    # The fringe falling over his brow in points.
    for i, x0 in enumerate((-3.2, -1.6, 0.2, 1.9)):
        m.cube("head", (x0, b.neck + 5.6, -4.45), (1.4, 1.6 + 0.4 * (i % 2), 0.5),
               lambda f, x, y, w, h: None if f == "north" and y == h - 1 and x != w // 2 else hr(f, x, y, w, h), density=2, tag="fringe",
               rotation=(-12, 0, (i - 1.5) * 8), pivot=(x0 + 0.7, b.neck + 7.2, -4.2))
    # Party in the back: a thick fall to the collar, then the tail of it, layered and ragged, flicking out over the jacket.
    def fall(f, x, y, w, h):
        if f in ("up", "north"):
            return None
        if f != "down" and y >= h - 2 and (x * 5) % 3 == 0:
            return CLEAR
        return hr(f, x, y + 3, w, h)
    m.cube("hair_back", (-4.3, b.neck + 0.2, 2.5), (8.6, 7.6, 1.8), fall, inflate=0.15, density=2, tag="mullet")
    for s in (-1, 1):
        m.cube("hair_back", (s * 4.0 - 0.7, b.neck - 0.4, 0.4), (1.4, 5.0, 2.8), lambda f, x, y, w, h: None if f == "up" else
               (CLEAR if f != "down" and y == h - 1 and x % 2 else hr(f, x, y, w, h)), density=2, tag="mullet_side")

    def tail(f, x, y, w, h):
        if f == "up":
            return None
        if f != "down" and y >= h - 3 and (x * 3 + y) % 4 < 2:
            return CLEAR
        c = hr(f, x, y + 5, w, h)
        return shade(c, 1.08) if f == "south" and (x + y) % 5 == 0 else c
    m.cube("mullet_tail", (-3.9, b.neck - 4.6, 3.0), (7.8, 5.2, 1.4), tail, density=2, tag="mullet_tail", rotation=(-8, 0, 0),
           pivot=(0, b.neck + 0.5, 3.6))
    # His open mouth (shown by the talking clips' scale): teeth along the top, the dark inside.
    def mouth(f, x, y, w, h):
        if y == 0:
            return TEETH if 1 <= x <= w - 2 else LIPS[2]
        if y == h - 1:
            return LIPS[3]
        return MOUTH[1] if 1 <= x <= w - 2 else LIPS[1]
    m.cube("mouth", (-1.5, b.neck + 1.25, -4.06), (3.0, 1.25, 0), mouth, faces=("north",), density=4, tag="mouth")


def tee_print(f, x, y, w, h):
    """The band tee's print, cracked and faded: a winged skull over a lightning bolt in a ring of stars (16 x 22 texels)."""
    cx = (w - 1) / 2
    dx, dy = x - cx, y - 8.0
    crack = h01("tee", x, y) < 0.22
    # The skull: a round cranium, two eye holes, teeth.
    if dx * dx / 9.0 + (dy + 1.0) ** 2 / 7.0 < 1.0:
        if abs(abs(dx) - 1.2) < 0.7 and -1.5 < dy < 0.5:
            return None
        if dy > 1.0 and int(x) % 2 == 0:
            return None
        return None if crack else PRINT
    # Wings either side, three swept feathers.
    for k in range(3):
        if 3.0 + k < abs(dx) < 7.5 - k * 0.5 and abs(dy + 2.5 - k * 1.4 + (abs(dx) - 3) * 0.35) < 0.55:
            return None if crack else PRINT
    # The bolt under it, red.
    bolt = {(0, 11), (1, 11), (0, 12), (-1, 13), (0, 13), (-1, 14), (0, 14), (1, 14), (0, 15), (1, 15), (0, 16)}
    if (int(round(dx)), y) in bolt:
        return None if crack else PRINT_RED
    # A banner of unreadable letters along the top.
    if 1 <= y <= 2 and 2 <= x <= w - 3 and (x % 3) != 2 and h01("lt", x, y) < 0.7:
        return None if crack else PRINT_RED
    return None


def build_body(m):
    b = B
    tee = worn(TEE, 18201, 2.8, 0.7)
    leather = leather_m(JACKET, 18202, 2.9)

    def shirt(f, x, y, w, h):
        c = tee(f, x, y, w, h)
        if f == "north":
            mid = (w - 1) / 2
            if y < 2 and abs(x - mid) < 2.5 - y:
                return TEE[1]                         # the crew neck's band
            p = tee_print(f, x, y - 2, w, h) if 2 <= y < 24 else None
            if p is not None:
                return mix(p, c, 0.25)
        return c
    m.cube("body", (-b.tw, b.hip - 1.2, -2), (2 * b.tw, b.neck - b.hip + 1.2, 4), shirt, density=2, tag="torso")
    m.cube("body", (-1.7, b.neck - 0.5, -1.7), (3.4, 0.6, 3.4), none_on(("down",), lambda f, x, y, w, h: TEE[2]), inflate=0.1, density=2,
           tag="crew_neck")
    m.cube("body", (-b.tw, b.hip + 0.6, -2), (2 * b.tw, b.neck - b.hip - 0.6, 4), open_shell(leather, 2.6, 3.4, 8, lapel=JACKET[3]),
           inflate=0.5, density=2, tag="jacket")
    # The collar standing round the back of the neck, its two points laid over the jacket's shoulders.
    def collar(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 3.0 and (f == "north" or y < 3):
            return None
        return tone(JACKET, 3.6 - y * 0.4)
    m.cube("body", (-b.tw, b.neck - 1.2, -2), (2 * b.tw, 1.8, 4), collar, inflate=0.85, density=2, tag="collar")
    for s, bone in ((-1, "collar_r"), (1, "collar_l")):
        m.cube(bone, (1.8 * s - 1.2, b.neck - 2.6, -3.1), (2.4, 2.4, 0.35), lambda f, x, y, w, h: None if f == "north" and y == h - 1 and
               (x <= 1 if s < 0 else x >= w - 2) else tone(JACKET, 3.8 - 0.8 * y / max(1, h)), density=2, tag="collar_point",
               rotation=(10, 0, -24 * s), pivot=(1.8 * s, b.neck - 0.4, -2.9))
    # The jacket's hem: two fronts and the back, with a waistband; zips down the open edges; zipped side pockets.
    for side, s in (("r", -1), ("l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f in ("up", "south") or f == ("west" if s < 0 else "east"):
                return None
            if y >= h - 1:
                return JACKET[1]
            return leather(f, x, y + 18, w, h + 18)
        m.cube(f"jacket_front_{side}", ((0.0 if s > 0 else -b.tw), b.hip - 0.8, -2), (b.tw, 1.8, 4.0), panel, inflate=0.5, density=2,
               tag="jacket_hem")
        m.cube("body", (s * 2.75 - 0.15, b.hip - 0.3, -2.58), (0.3, 10.2, 0.2), lambda f, x, y, w, h: STEEL[3] if y % 2 else STEEL[1],
               density=4, tag="zip")
        m.cube("body", (s * 3.5 - 0.6, b.hip + 2.4, -2.56), (1.2, 2.4, 0.2), lambda f, x, y, w, h: STEEL[2] if x == 0 else JACKET[1],
               density=4, tag="pocket_zip", rotation=(0, 0, 18 * s), pivot=(s * 3.5, b.hip + 3.6, -2.5))
    m.cube("body", (-0.4, b.neck - 6.0, -2.62), (0.8, 0.9, 0.25), lambda f, x, y, w, h: STEEL[4], density=4, tag="zip_pull")

    def back(f, x, y, w, h):
        if f in ("up", "north"):
            return None
        if y >= h - 1:
            return JACKET[1]
        return leather(f, x, y + 18, w, h + 18)
    m.cube("jacket_back", (-b.tw, b.hip - 0.8, 1.9), (2 * b.tw, 1.8, 0.3), back, inflate=0.5, density=2, tag="jacket_back")
    # Sleeves: leather to the elbow, shoved up and bunched just below it; bare forearms; a leather cuff on the left wrist.
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), leather, inflate=0.3, density=2,
               tag="sleeve")
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.sy - 0.4, -b.aw / 2), (b.aw, 1.2, b.aw), none_on(("down",), lambda f, x, y, w, h: JACKET[4] if f == "up" else JACKET[3]),
               inflate=0.42, density=2, tag="shoulder")
        bunch = lambda f, x, y, w, h: tone(JACKET, 3.4 - (1.2 if (y + x // 2) % 3 == 0 else 0) - 0.6 * y / max(1, h))
        m.cube(f"{side}_forearm", (cx - 1.65, b.elbow - 2.2, -1.65), (3.3, 2.4, 3.3), none_on(("up",), bunch), inflate=0.42, density=2,
               tag="bunched_sleeve")
        hairy = H.skin_mat(SKIN, 18210 + (s > 0), 3.0, 0.5)
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist, -1.5), (3.0, b.elbow - 2.2 - b.wrist, 3.0),
               none_on(("up",), lambda f, x, y, w, h, hairy=hairy: shade(hairy(f, x, y, w, h), 0.92) if h01("fh", f, x, y) < 0.18 else hairy(f, x, y, w, h)),
               density=2, tag="forearm")
    m.cube("left_forearm", (b.ax - 1.55, b.wrist + 0.1, -1.55), (3.1, 1.1, 3.1), none_on(("up", "down"),
           lambda f, x, y, w, h: STEEL[4] if f == "east" and x == w // 2 else tone(JACKET, 3.0 + (y == 0))), inflate=0.12, density=4,
           tag="wrist_cuff")
    hands(m, b, H.skin_mat(SKIN, 18220, 3.2, 0.45), nails=hexc("#c49a86"))
    # The belt with its big buckle; jeans worn pale at the knees; scuffed boots under the cuffs.
    m.cube("body", (-b.tw, b.hip - 1.3, -2), (2 * b.tw, 0.8, 4), none_on(("up", "down"), lambda f, x, y, w, h: tone(BOOT, 2.6 + (y == 0))),
           inflate=0.33, density=2, tag="belt")

    def buckle(f, x, y, w, h):
        if f != "north":
            return STEEL[2]
        d = math.hypot((x - (w - 1) / 2) / (w / 2), (y - (h - 1) / 2) / (h / 2))
        if d > 1.05:
            return None
        if d > 0.75:
            return STEEL[5] if y < h / 2 else STEEL[3]
        return hexc("#b08a3e") if (x + y) % 3 == 0 else STEEL[2]
    m.cube("body", (-1.2, b.hip - 1.6, -2.55), (2.4, 1.4, 0.3), buckle, density=4, tag="buckle")
    legs(m, b, "", denim_m(DENIM, 18230, fade=True, rips=0.05), boot_m(BOOT, 18231), shoe_h=2.0, shoe_len=4.8,
         cuff=lambda f, x, y, w, h: tone(DENIM, 3.0 - (y == 0)))
    for side, s in SIDES:
        # Belt loops, back pockets.
        m.cube(f"{side}_leg", (b.lx * s - 1.2, b.hip - 3.4, 1.82), (2.4, 2.6, 0.2), lambda f, x, y, w, h: DENIM[4] if (x in (0, w - 1) or y == 0)
               else DENIM[2], density=2, tag="back_pocket")


def build_towel(m):
    """The bar towel over his left shoulder: cream with two red stripes, a fringe, folded over the shoulder seam."""
    b = B

    def cloth(f, x, y, w, h, lng=False):
        ty = y if lng else x
        stripe = lng and (h - 1 - y) in (2, 4)
        base = 3.6 - 0.5 * (h01("tw", f, x, y) < 0.3) - (0.4 if f in ("east", "west", "south") else 0)
        c = tone(TOWEL, base)
        if stripe:
            c = tone(STRIPE, 3.0 + 0.6 * h01("st", x, y))
        if y >= h - 1 and lng and x % 2 == 0:
            return None                              # the fringe
        return c
    m.cube("towel", (2.0, b.neck - 0.3, -2.7), (2.8, 0.6, 5.4), lambda f, x, y, w, h: cloth(f, x, y, w, h), inflate=0.35, density=2,
           tag="towel_fold", rotation=(0, 0, -12), pivot=(3.4, b.neck, 0))
    m.cube("towel_front", (2.1, b.neck - 6.5, -2.95), (2.6, 6.2, 0.35), lambda f, x, y, w, h: cloth(f, x, y, w, h, True), density=2,
           tag="towel_front", rotation=(-4, 0, 6), pivot=(3.4, b.neck - 0.3, -2.6))
    m.cube("towel_back", (2.1, b.neck - 8.5, 2.6), (2.6, 8.2, 0.35), lambda f, x, y, w, h: cloth(f, x, y, w, h, True), density=2,
           tag="towel_back", rotation=(6, 0, 6), pivot=(3.4, b.neck - 0.3, 2.6))


def build_props(m):
    b = B
    # The pint glass: two crossed square prisms (an octagon), clear glass, a pale glint, a thick base.
    gx, gy = -b.ax + 2.3, b.palm - 0.4

    def glass(f, x, y, w, h):
        if f == "down":
            return (190, 205, 210, 150)
        if f == "up":
            return None
        if y == 0:
            return (235, 245, 248, 190)              # the rim
        if y >= h - 1:
            return (200, 214, 218, 180)
        if x == 1 and f in ("north", "west"):
            return (250, 252, 252, 200)              # the glint
        return (196, 214, 222, 70 + 10 * (x % 2))
    for rot in (0, 45):
        m.cube("glass", (gx - 1.25, gy - 2.6, -0.2 - 1.25), (2.5, 5.6, 2.5), glass, density=2, tag="glass", rotation=(0, rot, 0),
               pivot=(gx, gy, -0.2))
    m.cube("glass", (gx - 1.0, gy - 2.7, -0.2 - 1.0), (2.0, 0.6, 2.0), lambda f, x, y, w, h: (205, 220, 226, 200), density=2,
           tag="glass_base")
    # The rag bunched in his left fist, a tail of it hanging out with a stripe and the fringe.
    rag = lambda f, x, y, w, h: tone(TOWEL, 3.4 + 0.8 * h01("rg", f, x, y) - (0.6 if f in ("down", "south") else 0))
    for i, (o, sz, rot) in enumerate((((0.2, -1.2, -1.6), (1.8, 2.2, 1.6), (12, 20, 8)), ((-0.6, -1.6, -0.2), (1.6, 2.0, 1.8), (-20, 10, -14)),
                                      ((0.1, -0.4, 0.8), (1.6, 1.6, 1.4), (8, -30, 18)))):
        x0, y0, z0 = b.ax + o[0], b.palm + o[1], o[2]
        m.cube("rag", (x0 - sz[0] / 2, y0 - sz[1] / 2, z0 - sz[2] / 2), sz, rag, density=2, tag="rag", rotation=rot, pivot=(x0, y0, z0))
    m.cube("rag_tail", (b.ax - 0.4, b.palm - 4.6, -0.2), (2.0, 3.2, 0.3), lambda f, x, y, w, h: None if y == h - 1 and x % 2 else
           (tone(STRIPE, 3.0) if y in (2, 3) else rag(f, x, y, w, h)), density=2, tag="rag_tail", rotation=(0, 0, 8),
           pivot=(b.ax + 0.6, b.palm - 1.6, 0.2))


# =====================================================================================================
# Clips
# =====================================================================================================

HIDE = {"%glass": [0.01, 0.01, 0.01], "%rag": [0.01, 0.01, 0.01], "%towel": [1.01, 1.01, 1.01], "%mouth": [0.01, 0.01, 0.01]}
WIPE = {"%glass": [1, 1, 1], "%rag": [1, 1, 1], "%towel": [0.01, 0.01, 0.01], "%mouth": [0.01, 0.01, 0.01]}
OPEN = {"%mouth": [1, 1, 1]}
GRIP = {"right": (-B.ax, B.palm - 0.6, -0.2), "left": (B.ax, B.palm - 0.6, -0.2)}
INDEX_TIP = (-B.ax, B.palm - 1.4, -1.0)


def follow(pose, k=0.5):
    """The jacket's hem, the towel and the mullet follow the body."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])[0]
    head = pose.get("head", [0, 0, 0])[0]
    out["jacket_front_r"] = [min(0.0, rl) * k - body * 0.3, 0, 0]
    out["jacket_front_l"] = [min(0.0, ll) * k - body * 0.3, 0, 0]
    out["jacket_back"] = [-max(0.0, max(rl, ll)) * 0.3 - body * 0.3, 0, 0]
    out["towel_front"] = [-body * 0.6, 0, 0]
    out["towel_back"] = [-body * 0.6, 0, 0]
    out.setdefault("hair_back", [max(-30.0, min(10.0, -head * 0.9 - body * 0.4)), 0, 0])
    return out


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def K(c, t, pose, ease=None, props=HIDE):
        c.key(t, follow(P_(props, pose)), ease)

    # He slouches, weight on his right leg, the left knee loose, the right hand hooked at his belt.
    slouch = P_(LOOSE, {"body": [2, -4, 2], "@root": [-0.3, 0, 0], "right_leg": [0, 4, 3], "left_leg": [-6, -6, -2], "left_shin": [10, 0, 0],
                        "head": [-2, 6, -2], "right_arm": [-8, 0, 12], "right_forearm": [-30, 0, 0], "right_hand": [0, -20, 0],
                        "right_fingers": [-50, 0, 0], "right_index": [-40, 0, 0], "left_arm": [0, 0, -4]})

    # --- idle: weight shifting, a scratch of the goatee... a glance round the room. ---
    c = clip("idle", 4.0, loop=True)
    for t, k, look in ((0, 0, 6), (1.0, 1, 18), (2.0, 0, 4), (3.0, -1, -16), (4.0, 0, 6)):
        K(c, t, P_(slouch, {"body": [2 + 0.8 * abs(k), -4 + 2 * k, 2 - k], "@body": [0, -0.12 * abs(k), 0],
                            "head": [-2 + 2 * abs(k), look, -2 + 2 * k], "brows": [0, 0, 0] if k >= 0 else [0, 0, 0],
                            "@brows": [0, 0.25 if k < 0 else 0, 0]}), E_IO if t else None)
    c.layer("mullet_tail", lambda t: [1.2 * math.sin(t * 2 * math.pi / 4.0), 0, 0])
    c.layer("towel_back", lambda t: [0.8 * math.sin(t * 2 * math.pi / 4.0 + 1), 0, 0])

    # --- wipe_glass: glass held up before his chest, the rag circling inside it; he looks into it. ---
    L = 2.4
    c = clip("wipe_glass", L, loop=True)
    hold = P_(LOOSE, FIST_R, {"body": [6, 0, 0], "head": [26, -10, 8], "right_leg": [0, 0, 2], "left_leg": [-4, 0, -2]})
    hold = reach(r, hold, "right", (-1.2, 14.6, -6.6), GRIP["right"])
    hold = P_(hold, {"glass": aim(r, hold, "glass", (0, 1, 0), (0, 1, 0))})
    top = world_point(r, to_preview(hold), "glass", (-B.ax + 2.3, B.palm + 2.6, -0.2))
    for i in range(9):
        t = L * i / 8
        ph = 2 * math.pi * 2 * i / 8
        target = (top[0] + 0.5 * math.cos(ph), top[1] + 1.4 + 0.2 * math.sin(ph), top[2] + 0.5 * math.sin(ph))
        pose = reach(r, P_(hold, FIST_L, {"left_hand": [0, 0, 0]}), "left", target, GRIP["left"])
        pose = P_(pose, {"rag": [0, 30 * math.sin(ph), 0], "@body": [0, -0.08 * abs(math.sin(ph)), 0],
                         "head": [26 + 1.5 * math.sin(ph), -10, 8]})
        K(c, t, pose, E_IO if i else None, WIPE)
    c.layer("rag_tail", lambda t: [6 * math.sin(4 * math.pi * t / L), 0, 8 * math.cos(4 * math.pi * t / L)])

    # --- talk: an easy explaining hand, the head bobbing, the brows going up with the words. ---
    c = clip("talk", 2.5)
    g0 = P_(OPEN_R, {"right_arm": [-22, 0, 10], "right_forearm": [-64, 0, 0], "right_hand": [0, -30, 0]})
    g1 = P_(OPEN_R, {"right_arm": [-30, 12, 14], "right_forearm": [-80, 0, 0], "right_hand": [-10, -70, 0]})
    g2 = P_(OPEN_R, {"right_arm": [-16, -6, 8], "right_forearm": [-56, 0, 0], "right_hand": [12, -20, 0]})
    K(c, 0, slouch)
    for t, g, hd, br in ((0.35, g0, [2, 8, -2], 0.3), (0.9, g1, [-4, -6, 3], 0.0), (1.4, g2, [4, 12, -3], 0.4), (1.95, g1, [-1, 4, 0], 0.1)):
        K(c, t, P_(slouch, g, OPEN, {"head": hd, "body": [1, -2, 1], "@brows": [0, br, 0]}), E_IO)
    K(c, 2.5, slouch, E_IO)
    c.layer("head", lambda t: [2.5 * math.sin(t * 2 * math.pi * 3 / 2.5) if 0.3 < t < 2.2 else 0, 0, 0])
    c.layer("%mouth", lambda t: [0, -0.6 * max(0, math.sin(t * 2 * math.pi * 5 / 2.5)) if 0.4 < t < 2.1 else 0, 0])

    # --- nod: two slow nods, "yeah, man". ---
    c = clip("nod", 1.0)
    K(c, 0, slouch)
    K(c, 0.25, P_(slouch, {"head": [16, 6, -2], "body": [3, -4, 2]}), E_IO)
    K(c, 0.45, P_(slouch, {"head": [-2, 6, -2]}), E_IO)
    K(c, 0.7, P_(slouch, {"head": [14, 6, -2], "body": [3, -4, 2]}), E_IO)
    K(c, 1.0, slouch, E_IO)

    # --- point: "that way, man": the right arm up and out, the finger at the hunter, a jerk of the chin. ---
    c = clip("point", 1.4)
    aimp = reach(r, P_(slouch, POINT_R, {"right_hand": [0, 0, 0], "head": [-6, -14, 0], "body": [0, -10, 0]}), "right", (-9.0, 24.5, -13.0),
                 INDEX_TIP)
    K(c, 0, slouch)
    K(c, 0.3, P_(aimp, {"@brows": [0, 0.3, 0]}), E_OUT)
    K(c, 0.5, P_(aimp, {"head": [-12, -16, 0], "right_hand": [-12, 0, 0]}), E_SNAP)
    K(c, 0.9, P_(aimp, {"head": [-4, -14, 0]}), E_IO)
    K(c, 1.4, slouch, E_IO)

    # --- laugh: the head thrown back, shoulders shaking, a slap of the hand on his thigh. ---
    c = clip("laugh", 1.8)
    hah = P_(OPEN_R, OPEN_L, OPEN, {"head": [-26, 4, 0], "body": [-8, 0, 0], "right_arm": [-10, 0, 18], "right_forearm": [-30, 0, 0],
                                    "left_arm": [-14, 0, -10], "left_forearm": [-50, 0, 0], "@brows": [0, 0.4, 0], "@root": [0, 0, 0.3]})
    slap = P_(hah, {"head": [10, 4, 0], "body": [16, 0, 0], "right_arm": [-12, 0, 6], "right_forearm": [-10, 0, 0], "@root": [0, -0.4, 0]})
    K(c, 0, slouch)
    K(c, 0.3, hah, E_OUT)
    K(c, 0.8, P_(hah, {"head": [-30, 6, 2]}), E_IO)
    K(c, 1.05, slap, E_SNAP)
    K(c, 1.35, P_(slap, {"head": [4, 0, 0]}), E_OUT)
    K(c, 1.8, slouch, E_IO)
    c.layer("body", lambda t: [2.0 * math.sin(t * 2 * math.pi * 7 / 1.8) if 0.25 < t < 1.6 else 0, 0, 0])
    c.layer("%mouth", lambda t: [0, 0.3 * math.sin(t * 2 * math.pi * 7 / 1.8) if 0.25 < t < 1.6 else 0, 0])

    done(clips)
    return f, clips


def generate():
    m = rig()
    t, _ = m.build(gutter=1, seed=18000)
    m.rig.write(GEO + "ash.geo.json")
    save(t, "entity", "ash")
    f, _ = anims()
    write_compact(f, ANIM + "ash.animation.json")


if __name__ == "__main__":
    generate()
