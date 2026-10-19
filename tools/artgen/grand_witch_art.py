"""The Grand Witch (v0.21): the Grand Coven's matriarch -- GeckoLib rig, texture, glowmask and clips (names in
entity/boss/grandwitch/GrandWitchAssets), the Coven Staff she drops (GeckoLib item + icon)
and her bust on the trophies' pedestal.

Tall (a head over the other witches), old and unbowed: silver hair braided into a crown round her head, one long braid
down her back; a lined, severe face, violet-grey eyes, thin dark lips. Layered robes: a deep plum gown under a black
overrobe open at the front, its edges and hem embroidered in silver with the coven's sigils; a tall flared collar, black
lined with plum; wide bell sleeves; a silver girdle of chains hung with amulets; rings. In her right hand the coven staff:
a twisted blackthorn shaft bound in silver, its head three tines curling round a violet crystal under a silver crescent,
charms hanging from it.

Glow: her eyes, the crystal (always), the amulets' stones.

Skeleton (witchkit's woman, taller, plus):
  root -- body -- head -- crown_braid, braid_1 -- braid_2 -- braid_3;  brows
               -- collar_r, collar_l, collar_back;  girdle -- amulet_r, amulet_l
               -- robe_front_r, robe_front_l, robe_back, robe_side_r, robe_side_l  (the overrobe hangs from the waist)
               -- right_arm -- right_forearm -- sleeve_r;  right_hand -- fingers, index, thumb, staff -- crystal, charm_1, charm_2
               -- left_arm  -- left_forearm  -- sleeve_l;  left_hand  -- fingers, index, thumb
       -- right_leg -- right_shin,  left_leg -- left_shin;  skirt -- skirt_lower
"""

import math

import blockmodel as BM
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from legacy_art import Build, SIDES, done, reach as _reach
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, aim, parent_offset, to_preview, tone, world_point,
                         write_compact)
from pixelkit import Ramp, Tex, hexc, item_outline, mix, shade
from witchkit import (CLAW_L, CLAW_R, CURL_L, CURL_R, EYE_WHITE, FIST_L, FIST_R, GRACE_L, GRACE_R, OPEN_L, OPEN_R, REST, WClip, bell,
                      boot_shoe, face_head, gem, glow_k, gown_follow, gown_walk, nose_ears, satin, slim_hands, velvet, wavy_hair,
                      womans_legs, wskeleton)

A = "animation.grand_witch."
NAME = "grand_witch"

GB = Build(hip=13.0, neck=26.0, tw=3.5, depth=4.0, aw=3.0, ax=5.15, sx=4.7, sy=25.0, elbow=19.8, wrist=15.4, palm=13.6, lx=1.7,
           lw=3.2, knee=6.8, shoe_h=1.0)
WAIST = 18.6
TWW = 3.0

SKIN = Ramp("#6a5049", "#957569", "#b6968a", "#cbaea2", "#dcc3b8", "#e9d6cd")
HAIR = Ramp("#5b5c66", "#7c7e89", "#9ea0aa", "#bdbfc7", "#d8d9df", "#f1f2f5")
IRIS = hexc("#7a6e96")
PUPIL = hexc("#16131c")
LASH = hexc("#2a2228")
LIPS = Ramp("#2e0f1c", "#45172a", "#5c2038", "#712a46", "#863654", "#9b4463")
PLUM = Ramp("#12050d", "#220818", "#340d25", "#481334", "#5e1b45", "#772558")
BLACK = Ramp("#040305", "#09080b", "#100e13", "#18151c", "#221e27", "#2f2a36")
SILVER = Ramp("#2e333b", "#4f5661", "#7a828e", "#a6adb8", "#cfd5dc", "#f4f7fa")
VIOLET = Ramp("#1e0838", "#3a0f66", "#5c1a94", "#8430c4", "#b06aea", "#e8c8ff")
WOOD = Ramp("#0c0806", "#17100b", "#241911", "#322318", "#422f21", "#553d2b")
BONE = Ramp("#6b6152", "#8d816d", "#ab9f88", "#c4b9a1", "#d8cfb9", "#ebe5d4")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hssswssssssswssh",
    "hsbBBBbssbBBBbsh",
    "hsKKKKssssKKKKsh",
    "hswWIEssssEIWwsh",
    "hssuuwssssuuussh",
    "hswsssnnnssswssh",
    "hsswssnnnsswsssh",
    "hcswsNnnnNswscsh",
    "hssswsssssswsssh",
    "hsssslMMMlsssssh",
    "hjssssLLLssssjjh",
    "hjjssssssssssjjh",
]


# =====================================================================================================
# The staff (shared by her rig and the item)
# =====================================================================================================

def wood_m(seed):
    """Twisted blackthorn: spiral grain, knots."""
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return WOOD[2]
        v = 2.6 + 0.9 * math.sin((x + y * 0.6) * 1.3 + seed) + (0.4 if f == "north" else -0.2 if f == "south" else 0)
        if h01("knot", seed, f, x // 2, y // 3) < 0.05:
            v -= 1.6
        return tone(WOOD, v)
    return m


def silver_m(f, x, y, w, h):
    if f == "up":
        return SILVER[5]
    if f == "down":
        return SILVER[1]
    return SILVER[4] if y == 0 else SILVER[3] if (x + y) % 3 else SILVER[2]


def crystal_m(f, x, y, w, h):
    edge = x in (0, w - 1)
    v = 3.2 + 1.2 * math.sin(y * 0.9 + x * 0.4) + (1.0 if edge else 0) + (0.8 if f == "up" else 0)
    return tone(VIOLET, v)


def crystal_glow(f, x, y, w, h, c):
    return shade(c, 1.15)


def staff_parts(m, staff, crystal, x0, z0, y0, k=1.0, charms=None):
    """The coven staff upright with its foot at (x0, y0, z0): 32 px of twisted blackthorn, silver bands, the crystal
    cradle at the top. Returns the crystal's centre."""
    def at(dx, dy, dz):
        return (x0 + dx * k, y0 + dy * k, z0 + dz * k)

    def cube(bone, o, s, mat, glow=None, rot=None, piv=None, density=4, tag="staff"):
        m.cube(bone, at(*o), tuple(v * k for v in s), mat, glow, density=density, tag=tag, rotation=rot,
               pivot=at(*piv) if piv else None)
    wm = wood_m(41000)
    cube(staff, (-0.5, 0.0, -0.5), (1.0, 1.0, 1.0), silver_m, tag="ferrule")
    # The shaft in three segments, each a little turned, so it reads twisted.
    for i, (ya, yb, tw) in enumerate(((0.8, 10.0, 6), (9.8, 19.4, -8), (19.2, 26.4, 10))):
        cube(staff, (-0.7, ya, -0.7), (1.4, yb - ya, 1.4), wm, rot=(0, tw, 2 if i % 2 else -2), piv=(0, ya, 0), tag="shaft")
    for y in (9.4, 13.6, 18.8, 26.0):
        cube(staff, (-0.95, y, -0.95), (1.9, 0.6, 1.9), silver_m, tag="band", rot=(0, 45, 0), piv=(0, y, 0))
    # A leather grip between the bands at the hand.
    cube(staff, (-0.85, 13.0, -0.85), (1.7, 4.0, 1.7), lambda f, x, y, w, h: tone(PLUM, 1.6 + (1.0 if (x + y) % 3 == 0 else 0)),
         tag="grip")
    # Three tines curling up and in round the crystal.
    for i in range(3):
        a = i * 120
        rad = math.radians(a)
        cx, cz = math.sin(rad) * 0.9, -math.cos(rad) * 0.9
        out = (math.sin(rad), -math.cos(rad))
        # Leaning out (lower) then back in (upper): rotate about the axis perpendicular to `out`.
        cube(staff, (cx - 0.35, 26.4, cz - 0.35), (0.7, 3.6, 0.7), wm, rot=(-out[1] * 24, 0, out[0] * 24), piv=(cx, 26.4, cz), tag="tine")
        ux, uz = cx + out[0] * 1.5, cz + out[1] * 1.5
        cube(staff, (ux - 0.3, 29.6, uz - 0.3), (0.6, 3.4, 0.6), wm, rot=(out[1] * 30, 0, -out[0] * 30), piv=(ux, 29.6, uz), tag="tine_tip")
    # The crystal: a long violet bipyramid.
    cy = 30.0
    cube(crystal, (-0.9, cy - 1.8, -0.9), (1.8, 3.6, 1.8), crystal_m, crystal_glow, rot=(0, 45, 0), piv=(0, cy, 0), tag="crystal")
    cube(crystal, (-0.6, cy + 1.8, -0.6), (1.2, 1.2, 1.2), crystal_m, crystal_glow, rot=(0, 45, 0), piv=(0, cy, 0), tag="crystal_tip")
    cube(crystal, (-0.35, cy + 3.0, -0.35), (0.7, 0.7, 0.7), crystal_m, crystal_glow, rot=(0, 45, 0), piv=(0, cy, 0), tag="crystal_tip")
    cube(crystal, (-0.6, cy - 3.0, -0.6), (1.2, 1.2, 1.2), crystal_m, crystal_glow, rot=(0, 45, 0), piv=(0, cy, 0), tag="crystal_tip")
    # The silver crescent riding over it.
    for j, (dx, dy, rz) in enumerate(((-1.1, 0.0, 40), (0.0, 0.45, 0), (1.1, 0.0, -40))):
        cube(staff, (dx - 0.6, 34.4 + dy, -0.25), (1.2, 0.5, 0.5), silver_m, glow_k(0.1), rot=(0, 0, rz), piv=(dx, 34.6 + dy, 0), tag="crescent")
    for s in (-1, 1):
        cube(staff, (1.6 * s - 0.25, 34.6, -0.25), (0.5, 1.0, 0.5), silver_m, glow_k(0.1), rot=(0, 0, -30 * s), piv=(1.6 * s, 34.6, 0),
             tag="crescent_horn")
    # Charms on cords under the cradle: a bone and a little hex bag.
    if charms:
        b1, b2 = charms
        cube(b1, (1.0, 23.6, -0.1), (0.2, 2.8, 0.2), solid(BONE[2]), tag="cord", density=4)
        cube(b1, (0.7, 22.4, -0.4), (0.8, 1.4, 0.8), lambda f, x, y, w, h: BONE[4] if y % 2 else BONE[3], tag="charm_bone", density=4)
        cube(b2, (-1.2, 24.4, -0.1), (0.2, 2.0, 0.2), solid(BONE[2]), tag="cord", density=4)
        cube(b2, (-1.7, 23.0, -0.6), (1.2, 1.5, 1.2), lambda f, x, y, w, h: tone(PLUM, 3.0 + (x + y) % 2), tag="charm_bag", density=4)
    return at(0, cy, 0)


# =====================================================================================================
# The rig
# =====================================================================================================

GRIP = (-GB.ax, GB.palm - 0.6, -0.4)      # where the staff passes through her right fist (rest space)
STAFF_FOOT_Y = GRIP[1] - 15.0             # the staff's foot at rest (the grip 15 px up the shaft)


def rig():
    m = Model(NAME, 512, 512)
    b = GB
    wskeleton(m, b)
    m.bone("crown_braid", (0, b.neck + 6.0, 0), "head")
    m.bone("braid_1", (0, b.neck + 3.0, 4.2), "head")
    m.bone("braid_2", (0, b.neck - 2.6, 4.6), "braid_1")
    m.bone("braid_3", (0, b.neck - 8.0, 4.6), "braid_2")
    m.bone("brows", (0, b.neck + 5.3, -4.0), "head")
    for side, s in SIDES:
        m.bone(f"collar_{side[0]}", (s * 3.0, b.neck - 0.8, 0.4), "body")
        m.bone(f"sleeve_{side[0]}", (b.ax * s, b.elbow - 1.0, 0), f"{side}_forearm")
        m.bone(f"robe_front_{side[0]}", (s * 2.4, WAIST, -2.4), "body")
        m.bone(f"robe_side_{side[0]}", (s * 3.6, WAIST, 0), "body")
        m.bone(f"amulet_{side[0]}", (s * 2.0, b.hip + 0.2, -2.4), "body")
    m.bone("collar_back", (0, b.neck - 0.6, 1.8), "body")
    m.bone("robe_back", (0, WAIST, 2.4), "body")
    m.bone("girdle", (0, b.hip + 0.6, 0), "body")
    m.bone("staff", GRIP, "right_hand")
    m.bone("crystal", (GRIP[0], STAFF_FOOT_Y + 30.0, GRIP[2]), "staff")
    m.bone("charm_1", (GRIP[0] + 1.0, STAFF_FOOT_Y + 26.4, GRIP[2]), "staff")
    m.bone("charm_2", (GRIP[0] - 1.2, STAFF_FOOT_Y + 26.4, GRIP[2]), "staff")
    build_head(m)
    build_body(m)
    build_robe(m)
    staff_parts(m, "staff", "crystal", GRIP[0], GRIP[2], STAFF_FOOT_Y, charms=("charm_1", "charm_2"))
    return m


def build_head(m):
    b = GB
    y0 = b.neck
    sk = H.skin_mat(SKIN, 41101, 3.2, 0.4)
    hr = wavy_hair(HAIR, 41102, 3.0, wave=0.4, shine=0.8)

    def eye_glow(f, x, y, w, h, c):
        if c == IRIS:
            return VIOLET[5]
        if c == PUPIL:
            return VIOLET[3]
        return None
    legend = {"h": hr, "s": sk, "b": HAIR[2], "B": HAIR[3], "K": LASH, "W": EYE_WHITE, "I": IRIS, "E": PUPIL,
              "u": mix(SKIN[2], hexc("#4a3046"), 0.4), "w": mix(SKIN[3], SKIN[1], 0.3), "n": SKIN[4], "N": SKIN[1],
              "c": mix(SKIN[2], LIPS[3], 0.15), "l": LIPS[1], "M": LIPS[2], "L": LIPS[3],
              "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.88)}

    def sides(f, x, y, w, h):
        if f in ("up", "south"):
            return hr(f, x, y, w, h) if f == "up" or y < 13 else sk(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x
        if y < 3 or (fx > 9 and y < 13) or (fx > 5 and y < 5):
            return hr(f, x, y, w, h)
        if 7 <= fx <= 9 and 6 <= y <= 10:
            return shade(sk(f, x, y, w, h), 0.85 if fx == 8 else 1.05)
        return sk(f, x, y, w, h)
    face_head(m, b, FACE, legend, sides, eye_glow)
    nose_ears(m, b, SKIN, nose_h=1.8)
    m.cube("body", (-1.2, y0 - 0.8, -1.2), (2.4, 1.2, 2.4), sk, density=2, tag="neck")
    for s in (-1, 1):
        m.cube("brows", (s * 2.1 - 1.2, y0 + 5.0, -4.12), (2.4, 0.45, 0.2), lambda f, x, y, w, h: HAIR[3] if x % 2 else HAIR[2],
               density=4, tag="brow", rotation=(0, 0, -10 * s), pivot=(s * 1.0, y0 + 5.2, -4.1))
    # Hair drawn straight back from the face under the braided crown.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 5 or y < 3 else None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, y0 + 4.0, -4), (8, 4.0, 8), shell, inflate=0.3, density=2, tag="hair_shell")
    # The crown: a thick braid wound round her head, segments turned to read as plaits.
    braid = lambda f, x, y, w, h: tone(HAIR, 3.0 + (1.4 if (x + y) % 3 == 0 else 0) - (0.6 if f in ("down",) else 0))
    for i in range(12):
        a = math.radians(i * 30)
        rx, rz = math.sin(a) * 4.0, -math.cos(a) * 4.0
        m.cube("crown_braid", (rx - 1.25, y0 + 6.6, rz - 0.85), (2.5, 1.7, 1.7), braid, density=2, tag="crown_plait",
               rotation=(0, -i * 30, 12 if i % 2 else -12), pivot=(rx, y0 + 7.4, rz))
    # A silver pin with a violet stone set in the crown at her brow.
    m.cube("crown_braid", (-0.6, y0 + 7.0, -5.3), (1.2, 1.2, 0.4), gem(VIOLET), glow_k(0.9), density=4, tag="crown_stone")
    m.cube("crown_braid", (-1.0, y0 + 6.7, -5.05), (2.0, 1.8, 0.3), silver_m, glow_k(0.1), density=4, tag="crown_setting")
    # The long braid down her back.
    for bone, (ya, yb) in (("braid_1", (y0 - 2.6, y0 + 3.0)), ("braid_2", (y0 - 8.0, y0 - 2.4)), ("braid_3", (y0 - 13.0, y0 - 7.8))):
        z = 4.3
        wdt = 1.9 if bone != "braid_3" else 1.5
        m.cube(bone, (-wdt / 2, ya, z - 0.8), (wdt, yb - ya, 1.6), braid, density=2, tag="braid")
    m.cube("braid_3", (-0.9, y0 - 13.8, 3.6), (1.8, 1.0, 1.8), solid(SILVER[3]), glow_k(0.1), density=4, tag="braid_clasp")
    m.cube("braid_3", (-0.6, y0 - 15.4, 3.9), (1.2, 1.6, 1.2), lambda f, x, y, w, h: tone(HAIR, 3.6 - y * 0.5), density=2, tag="braid_tuft")


def build_body(m):
    b = GB
    gown = velvet(PLUM, 41201, 2.9, 0.8, 0.45)
    black = velvet(BLACK, 41202, 3.0, 0.7, 0.35)

    def bodice(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north":
            if abs(x - mid) < 1.0 and y % 3 == 1:
                return SILVER[3]
            if abs(x - mid) < 2.2:
                return gown(f, x, y, w, h)
            if abs(x - mid) < 2.8:
                return SILVER[2] if y % 2 else SILVER[1]                     # the overrobe's embroidered edge
        return black(f, x, y, w, h)
    m.cube("body", (-b.tw, WAIST, -2.0), (2 * b.tw, b.neck - WAIST, 4.0), bodice, density=2, tag="chest")
    m.cube("body", (-TWW, b.hip, -1.8), (2 * TWW, WAIST - b.hip + 0.2, 3.6), bodice, density=2, tag="waist")
    m.cube("body", (-2.6, 20.4, -2.5), (5.2, 2.2, 0.6), lambda f, x, y, w, h: None if f == "south" else bodice(f, x + 1, y + 4, w + 2, h),
           density=2, tag="bust", rotation=(-6, 0, 0), pivot=(0, 22.6, -2.2))
    # The tall flared collar: two wings and the back, black lined with plum, edged in silver.
    def wing(f, x, y, w, h):
        if f == "north":
            return tone(PLUM, 3.0 + 0.8 * math.sin(x * 1.5)) if y < h - 1 else SILVER[3]
        if y == 0 or (f in ("east", "west") and x == 0):
            return SILVER[3]
        return tone(BLACK, 3.0 + 0.6 * (x % 2))
    for side, s in SIDES:
        m.cube(f"collar_{side[0]}", (s * 3.9 - 1.7, b.neck - 0.8, 0.2), (3.4, 5.4, 0.4), wing, density=2, tag="collar_wing",
               rotation=(14, -28 * s, 22 * s), pivot=(s * 3.0, b.neck - 0.8, 0.4))
    m.cube("collar_back", (-3.2, b.neck - 0.8, 1.8), (6.4, 5.8, 0.4), lambda f, x, y, w, h: wing("north" if f == "north" else f, x, y, w, h),
           density=2, tag="collar_back", rotation=(18, 0, 0), pivot=(0, b.neck - 0.6, 1.8))
    m.cube("body", (-b.tw - 0.2, b.neck - 1.8, -2.1), (2 * b.tw + 0.4, 1.8, 4.2), none_on(("down",), lambda f, x, y, w, h: tone(BLACK, 3.6 if f == "up" else 2.8)),
           inflate=0.2, density=2, tag="shoulders")
    # The girdle: silver chains slung round her hips, an amulet hanging at each side.
    def chains(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        return SILVER[4] if (x + y) % 2 == 0 else (SILVER[2] if y != 1 else None)
    m.cube("girdle", (-TWW, b.hip - 0.4, -1.8), (2 * TWW, 1.2, 3.6), chains, inflate=0.35, density=4, tag="girdle")
    m.cube("girdle", (-0.8, b.hip - 1.4, -2.3), (1.6, 1.6, 0.4), lambda f, x, y, w, h: VIOLET[4] if 2 <= x <= 3 and 2 <= y <= 3 else SILVER[3],
           lambda f, x, y, w, h, c: VIOLET[5] if c == VIOLET[4] else None, density=4, tag="girdle_clasp")
    for side, s in SIDES:
        a = f"amulet_{side[0]}"
        m.cube(a, (s * 2.0 - 0.1, b.hip - 2.6, -2.5), (0.2, 2.6, 0.2), solid(SILVER[3]), density=4, tag="amulet_chain")
        m.cube(a, (s * 2.0 - 0.6, b.hip - 3.8, -2.7), (1.2, 1.2, 0.4), lambda f, x, y, w, h: VIOLET[4] if (x, y) in ((1, 1), (2, 1), (1, 2), (2, 2)) else SILVER[4],
               lambda f, x, y, w, h, c: VIOLET[5] if c == VIOLET[4] else None, density=4, tag="amulet")
    # Arms: fitted black, the wide bell sleeves hanging from the elbow, lined in plum.
    hand = H.skin_mat(SKIN, 41301, 3.2, 0.4)
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.5 - b.elbow, b.aw), black, inflate=0.18, density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.35, b.wrist, -1.35), (2.7, b.elbow - b.wrist, 2.7), none_on(("up",), black), inflate=0.12, density=2,
               tag="sleeve_lower")

        def bellm(f, x, y, w, h):
            if f == "up":
                return None
            if f == "down":
                return tone(PLUM, 2.6 + (x % 2))                         # the plum lining seen from below
            if y >= h - 1:
                return SILVER[3] if x % 2 else SILVER[1]
            if y == h - 2 and (x % 4 == 1):
                return SILVER[2]
            return tone(BLACK, 3.0 + 0.8 * math.sin(x * 1.1) - 0.4 * y / max(1, h))
        sl = f"sleeve_{side[0]}"
        m.cube(sl, (cx - 2.1, b.wrist - 2.4, -2.1), (4.2, b.elbow - 1.0 - b.wrist + 2.4, 4.2), bellm, density=2, tag="bell_sleeve")
        m.cube(f"{side}_hand", (cx - 1.1, b.palm + 0.2, -1.3), (2.2, 0.35, 2.6), solid(SILVER[4]), glow_k(0.1), density=4, inflate=0.04, tag="ring")
    slim_hands(m, b, hand, nails=BLACK[3])
    m.cube("left_index", (b.ax - 1.0, b.palm - 1.0, -1.3), (2.0, 0.35, 0.75), lambda f, x, y, w, h: VIOLET[4] if x == 3 and f == "north" else SILVER[4],
           lambda f, x, y, w, h, c: VIOLET[5] if c == VIOLET[4] else None, inflate=0.06, density=4, tag="ring")


def build_robe(m):
    b = GB
    gown = velvet(PLUM, 41401, 2.8, 0.8, 0.6)
    black = velvet(BLACK, 41402, 3.0, 0.7, 0.4)

    def sigil_hem(base, rows=4):
        """Silver embroidery: a running band of little sigils above the hem."""
        def mm(f, x, y, w, h):
            if f not in ("up", "down") and y >= h - rows:
                r = y - (h - rows)
                if r == 0 or r == rows - 1:
                    return SILVER[3]
                gx = x % 6
                if (gx, r) in ((1, 1), (2, 2), (3, 1), (4, 2), (2, 1), (3, 2)) and h01("sg", x // 6, f) < 0.8:
                    return SILVER[4]
                return BLACK[1]
            return base(f, x, y, w, h)
        return mm
    # The plum gown beneath, to the floor.
    bell(m, "skirt", b.hip + 0.6, b.knee + 0.4, (3.15, 2.2), (3.9, 2.9), gown, n=3, tag="skirt")
    bell(m, "skirt_lower", b.knee + 0.5, 0.15, (3.95, 2.95), (4.7, 3.5), gown, n=3, tag="skirt_lower", cap=False)
    # The black overrobe: open fronts, sides and back hanging from the waist to the floor, embroidered at the hem.
    L = WAIST - 0.4
    for side, s in SIDES:
        def front(f, x, y, w, h, s=s):
            if f in ("up",):
                return None
            inner = (x == (w - 1 if s < 0 else 0))
            if f == "north" and inner:
                return SILVER[3] if y % 2 else SILVER[2]                         # the embroidered edge
            if f == ("west" if s > 0 else "east"):
                return tone(PLUM, 2.4)
            return sigil_hem(black)(f, x, y, w, h)
        x0 = 0.6 if s > 0 else -4.6
        m.cube(f"robe_front_{side[0]}", (x0 if s > 0 else x0, WAIST - L, -2.9), (4.0, L, 0.5), front, density=2, tag="robe_front",
               rotation=(-5, 0, 0), pivot=(s * 2.4, WAIST, -2.4))
        m.cube(f"robe_side_{side[0]}", (s * 4.1 - 0.25, WAIST - L, -2.6), (0.5, L, 5.2), none_on(("up",), sigil_hem(black)), density=2,
               tag="robe_side", rotation=(0, 0, -6 * s), pivot=(s * 3.6, WAIST, 0))
    m.cube("robe_back", (-4.4, WAIST - L, 2.7), (8.8, L, 0.5), none_on(("up",), sigil_hem(black)), density=2, tag="robe_back",
           rotation=(6, 0, 0), pivot=(0, WAIST, 2.4))
    # A tall woman's long stride under all that: black slippers.
    womans_legs(m, b, solid(BLACK[2]), boot_shoe(BLACK), shoe_len=3.6, shoe_h=1.0)


# =====================================================================================================
# Clips
# =====================================================================================================

def follow(pose):
    def extra(p, out):
        rl = p.get("right_leg", [0, 0, 0])[0]
        ll = p.get("left_leg", [0, 0, 0])[0]
        body = p.get("body", [0, 0, 0])[0]
        head = p.get("head", [0, 0, 0])[0]
        out.setdefault("robe_front_r", [min(0.0, rl) * 0.55 - body * 0.6, 0, 0])
        out.setdefault("robe_front_l", [min(0.0, ll) * 0.55 - body * 0.6, 0, 0])
        out.setdefault("robe_back", [max(0.0, max(rl, ll)) * 0.35 - body * 0.6, 0, 0])
        out.setdefault("robe_side_r", [rl * 0.3 - body * 0.6, 0, 0])
        out.setdefault("robe_side_l", [ll * 0.3 - body * 0.6, 0, 0])
        out.setdefault("braid_1", [max(-30.0, min(20.0, -head * 0.8 - body * 0.6)), 0, 0])
        for side in ("r", "l"):
            arm = p.get("right_arm" if side == "r" else "left_arm", [0, 0, 0])[0]
            fore = p.get("right_forearm" if side == "r" else "left_forearm", [0, 0, 0])[0]
            # The bell sleeve hangs: it turns back against the arm's swing.
            out.setdefault(f"sleeve_{side}", [max(-10.0, min(70.0, -(arm + fore) * 0.45)), 0, 0])
    return gown_follow(pose, k=0.8, extra=extra)


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(WClip(f, A + name, length, loop, hold, follow=follow))
        return clips[-1]

    def staff(pose, d=(0, 1, 0), plant=None):
        """The staff turned in her fist to point along world d; `plant` slides it so its foot rests at that height."""
        base = P_(pose, {"staff": [0, 0, 0], "@staff": [0, 0, 0]})
        rot = aim(r, base, "staff", (0, 1, 0), d)
        out = P_(pose, {"staff": rot})
        if plant is not None:
            foot = world_point(r, to_preview(out), "staff", (GRIP[0], STAFF_FOOT_Y, GRIP[2]))
            off = parent_offset(r, to_preview(out), "staff", (0, plant - foot[1], 0))
            out = P_(out, {"@staff": [round(v, 2) for v in off]})
        return out

    # Upright, the staff planted at her right side, her left hand resting open at her waist.
    stance = staff(P_(REST, GRACE_L, FIST_R, {"right_arm": [-18, 0, 8], "right_forearm": [-36, 0, 0], "right_hand": [0, 0, 0],
                                              "left_arm": [-14, 0, -8], "left_forearm": [-54, 0, 0], "left_hand": [0, 0, -30],
                                              "head": [-4, 0, 0]}), (0, 1, 0), plant=0.0)

    # --- idle: she breathes slow, the head turning to survey, the crystal's charms swaying. ---
    c = clip("idle", 4.0, loop=True)
    for t, look in ((0, 0), (1.4, 18), (2.8, -14), (4.0, 0)):
        c.key(t, P_(stance, {"head": [-4 + abs(look) * 0.1, look, 0], "@body": [0, 0.15 * (1 if t in (1.4, 2.8) else 0), 0]}), E_IO if t else None)
    c.layer("charm_1", lambda t: [0, 0, 6 * math.sin(t * 2 * math.pi / 2.0)])
    c.layer("charm_2", lambda t: [0, 0, 6 * math.sin(t * 2 * math.pi / 2.0 + 1.5)])
    c.layer("braid_3", lambda t: [2 * math.sin(t * 2 * math.pi / 4.0), 0, 0])

    # --- walk: a slow, stately step, the staff planted ahead with each other stride. ---
    n = 8
    L = 1.4
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = gown_walk(n, i % n, stride=16, arm=6, knee=20, bob=0.3, sway=2)
        ph = 2 * math.pi * i / n
        pose = P_(REST, GRACE_L, FIST_R, w, {"right_arm": [-22 - 8 * math.sin(ph), 0, 8], "right_forearm": [-36, 0, 0],
                                              "left_arm": [w["left_arm"][0] - 8, 0, -8], "left_forearm": [-40, 0, 0], "head": [-4, w["head"][1], 0]})
        c.key(L * i / n, staff(pose, (0, 1, -0.12 - 0.1 * math.sin(ph))), E_IO if i else None)
    c.layer("braid_3", lambda t: [4 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("charm_1", lambda t: [0, 0, 10 * math.sin(t * 2 * math.pi * 2 / L)])
    c.layer("charm_2", lambda t: [0, 0, 10 * math.sin(t * 2 * math.pi * 2 / L + 1)])

    # --- shield: the staff planted before her in both hands, head bowed, drawing in her coven's power. ---
    L = 2.4
    c = clip("shield", L, loop=True)
    pl = P_(REST, FIST_R, FIST_L, {"body": [8, 0, 0], "head": [26, 0, 0], "right_arm": [-46, -24, 0], "right_forearm": [-40, 0, 0],
                                   "right_hand": [0, 0, 0]})
    pl = staff(pl, (0, 1, 0), plant=0.0)
    top = world_point(r, to_preview(pl), "staff", (GRIP[0], GRIP[1] + 2.4, GRIP[2]))
    pl = _reach(r, pl, "left", (top[0], top[1], top[2]), (GB.ax, GB.palm - 0.6, -0.3))
    for i in range(5):
        t = L * i / 4
        k = math.sin(2 * math.pi * i / 4)
        c.key(t, P_(pl, {"head": [26 + 3 * k, 0, 0], "@root": [0, -0.15 * (1 + k), 0], "body": [8 + k, 0, 0]}), E_IO if i else None)
    c.layer("skirt_lower", lambda t: [0, 0, 2 * math.sin(t * 2 * math.pi / L)])
    c.layer("robe_back", lambda t: [6 + 4 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("braid_2", lambda t: [3 * math.sin(t * 2 * math.pi * 2 / L + 1), 0, 0])

    # --- frost_hex: the staff swept low across the ground before her, the frost following it (tick 18). ---
    c = clip("frost_hex", 1.7)
    c.key(0, stance)
    gather = staff(P_(stance, {"body": [-6, -30, 0], "head": [0, 20, 0], "right_arm": [-50, -50, 0], "right_forearm": [-40, 0, 0],
                               "left_arm": [-30, 0, -40], "left_forearm": [-30, 0, 0], "@staff": [0, 0, 0]}), (0.6, 0.6, -0.5))
    sweep = staff(P_(stance, OPEN_L, {"body": [18, 34, 0], "head": [10, -20, 0], "right_arm": [-36, 40, 12], "right_forearm": [-14, 0, 0],
                                      "left_arm": [10, 0, -30], "left_forearm": [-10, 0, 0], "right_leg": [-20, 0, 0], "right_shin": [30, 0, 0],
                                      "left_leg": [10, 0, 0], "@root": [0, -1.0, 0], "@staff": [0, 0, 0]}), (-0.55, -0.22, -0.8))
    c.key(0.55, gather, E_OUT)
    c.key(0.75, P_(gather, {"body": [-8, -36, 0]}), E_IO)
    c.key(0.9, sweep, E_SNAP)
    c.key(1.2, P_(sweep, {"body": [16, 30, 0]}), E_OUT)
    c.key(1.7, stance, E_IO)

    # --- inferno: the staff raised high overhead, the crystal flaring, then brought down a touch (tick 20). ---
    c = clip("inferno", 1.9)
    c.key(0, stance)
    rise = staff(P_(stance, CLAW_L, {"body": [-10, 0, 0], "head": [-24, 0, 0], "right_arm": [-170, 0, -10], "right_forearm": [-10, 0, 0],
                                     "left_arm": [-20, 0, -50], "left_forearm": [-30, 0, 0], "@root": [0, 0.4, 0], "@staff": [0, 0, 0]}),
                 (0, 1, 0.1))
    blast = staff(P_(rise, {"body": [4, 0, 0], "head": [-6, 0, 0], "right_arm": [-150, 0, -6], "@root": [0, -0.3, 0], "@staff": [0, 0, 0]}),
                  (0, 0.8, -0.6))
    c.key(0.7, rise, E_OUT)
    c.key(0.9, P_(rise, {"head": [-28, 0, 0]}), E_IO)
    c.key(1.0, blast, E_SNAP)
    c.key(1.35, P_(blast, {"right_arm": [-146, 0, -6]}), E_OUT)
    c.key(1.9, stance, E_IO)

    # --- crush: the left hand reaches out, fingers spread, then clenches -- whatever she holds in it breaks (tick 16). ---
    c = clip("crush", 1.5)
    c.key(0, stance)
    reachp = P_(stance, OPEN_L, {"body": [4, 14, 0], "head": [2, -14, 0], "left_arm": [-84, 10, -10], "left_forearm": [-10, 0, 0],
                                 "left_hand": [0, 0, 0]})
    clench = P_(reachp, FIST_L, {"left_arm": [-80, 6, -8], "left_forearm": [-30, 0, 0], "left_hand": [20, 0, 0], "body": [8, 10, 0],
                                 "@brows": [0, -0.2, 0]})
    c.key(0.5, reachp, E_OUT)
    c.key(0.7, P_(reachp, CLAW_L), E_IO)
    c.key(0.8, clench, E_SNAP)
    c.key(1.1, P_(clench, {"left_forearm": [-40, 0, 0]}), E_OUT)
    c.key(1.5, stance, E_IO)
    c.layer("left_hand", lambda t: [0, 0, 4 * math.sin(t * 60)] if 0.8 < t < 1.1 else [0, 0, 0])

    # --- mass_taglock: arms spread wide, staff held out, head back -- every taglock she holds burns at once (tick 24). ---
    c = clip("mass_taglock", 2.2)
    c.key(0, stance)
    draw = staff(P_(stance, CURL_L, {"body": [10, 0, 0], "head": [20, 0, 0], "right_arm": [-30, -30, -10], "right_forearm": [-70, 0, 0],
                                     "left_arm": [-30, 30, 10], "left_forearm": [-70, 0, 0], "@root": [0, -0.6, 0], "@staff": [0, 0, 0]}),
                 (0.3, 1, -0.2))
    spread = staff(P_(stance, CLAW_L, {"body": [-12, 0, 0], "head": [-26, 0, 0], "right_arm": [-30, 0, 100], "right_forearm": [-10, 0, 0],
                                       "left_arm": [-30, 0, -110], "left_forearm": [-10, 0, 0], "left_hand": [0, 0, 30], "@root": [0, 0.5, 0],
                                       "right_leg": [0, 0, 6], "left_leg": [0, 0, -6], "@staff": [0, 0, 0]}), (-0.5, 1, -0.2))
    c.key(0.7, draw, E_IO)
    c.key(1.1, P_(draw, {"head": [24, 0, 0]}), E_IO)
    c.key(1.2, spread, E_BACK)
    c.key(1.6, P_(spread, {"left_arm": [-34, 0, -114]}), E_IO)
    c.key(2.2, stance, E_IO)

    # --- smoke_veil: she sweeps her robe round her and turns into the smoke (gone at tick 10), turning back in. ---
    c = clip("smoke_veil", 1.1)
    c.key(0, stance)
    wrap = staff(P_(REST, FIST_R, FIST_L, {"body": [6, 0, 0], "head": [12, 0, 0], "right_arm": [-30, -10, 0], "right_forearm": [-40, 0, 0],
                                           "left_arm": [-80, 40, 0], "left_forearm": [-60, 0, 0], "robe_front_l": [-40, 0, -30],
                                           "robe_side_l": [0, 0, -40], "@root": [0, -0.6, 0]}), (0, 1, 0))
    c.key(0.3, P_(wrap, {"root": [0, 120, 0]}), E_IN)
    c.key(0.5, P_(wrap, {"root": [0, 220, 0], "robe_back": [40, 0, 0]}), E_IN)
    c.key(0.7, P_(wrap, {"root": [0, 300, 0]}), E_OUT)
    c.key(1.1, P_(stance, {"root": [0, 360, 0]}), E_IO)

    # --- death: struck, she leans on the staff, sinks to her knees, the staff falling from her hand; holds (tick 80). ---
    c = clip("death", 6.0, hold=True)
    c.key(0, stance)
    hit = staff(P_(stance, CLAW_L, {"body": [-14, 0, 0], "head": [-24, 0, 0], "left_arm": [-60, -20, -10], "left_forearm": [-70, 0, 0],
                                    "@root": [0, 0, 0.6], "@staff": [0, 0, 0]}), (0.2, 1, 0.1))
    lean = staff(P_(REST, FIST_R, OPEN_L, {"body": [24, 0, 6], "head": [20, 0, 0], "right_arm": [-50, -20, 0], "right_forearm": [-30, 0, 0],
                                           "left_arm": [-10, 0, -10], "left_forearm": [-20, 0, 0], "right_leg": [-20, 0, 0], "right_shin": [36, 0, 0],
                                           "@root": [0, -1.4, 0]}), (0, 1, -0.2), plant=0.0)
    kneel = {"@root": [0, -6.2, 0], "right_leg": [-6, 0, 4], "left_leg": [-6, 0, -4], "right_shin": [96, 0, 0], "left_shin": [96, 0, 0],
             "%skirt": [1.2, 0.5, 1.2], "skirt": [0, 0, 0], "skirt_lower": [0, 0, 0]}
    down = P_(REST, CURL_R, OPEN_L, kneel, {"body": [30, 0, 0], "head": [34, 0, 0], "right_arm": [-20, 0, 20], "right_forearm": [-20, 0, 0],
                                            "left_arm": [-30, 0, -10], "left_forearm": [-30, 0, 0], "left_hand": [-30, 0, 0],
                                            "robe_front_r": [-14, 0, 6], "robe_front_l": [-14, 0, -6], "robe_side_r": [0, 0, 14],
                                            "robe_side_l": [0, 0, -14], "robe_back": [24, 0, 0], "%robe_front_r": [1, 0.62, 1],
                                            "%robe_front_l": [1, 0.62, 1], "%robe_side_r": [1, 0.62, 1], "%robe_side_l": [1, 0.62, 1],
                                            "%robe_back": [1, 0.62, 1]})
    down = staff(P_(down, {"right_arm": [-34, 0, 26], "right_forearm": [-30, 0, 0], "@staff": [0, 0, 0]}), (-0.55, 0.8, -0.25), plant=0.0)
    c.key(0.25, hit, E_SNAP)
    c.key(1.4, P_(hit, {"body": [-8, 0, 0]}), E_IO)
    c.key(2.4, lean, E_IO)
    c.key(3.2, P_(lean, {"@root": [0, -2.6, 0], "body": [30, 0, 6]}), E_IO)
    c.key(4.0, down, E_IN)
    c.key(4.5, P_(down, {"body": [34, 0, 0], "head": [40, 0, 0]}), E_OUT)
    c.key(6.0, P_(down, {"body": [36, 0, 0], "head": [44, 0, 0]}), E_IO)

    done(clips)
    return f, clips


# =====================================================================================================
# The Coven Staff (item)
# =====================================================================================================

ITEM = "coven_staff"


def staff_item_rig():
    m = Model(ITEM, 128, 128)
    m.bone("staff", (0, 0, 0))
    m.bone("crystal", (0, 30.0, 0), "staff")
    m.bone("charm_1", (1.0, 26.4, 0), "staff")
    m.bone("charm_2", (-1.2, 26.4, 0), "staff")
    staff_parts(m, "staff", "crystal", 0.0, 0.0, 0.0, charms=("charm_1", "charm_2"))
    lo, hi = m.rig.height_span()
    dy = -(lo + hi) / 2
    m.rig.translate(0, dy, 0)
    for _, c in m.rig.cubes():
        if c.pivot:
            c.pivot = (c.pivot[0], c.pivot[1] + dy, c.pivot[2])
    m.height = hi - lo
    return m


def staff_item_anims():
    f = AnimFile()
    a = f.new(f"animation.{ITEM}.idle", 4.0, loop=True)
    a.rot("crystal", (0, [0, 0, 0]), (4.0, [0, 360, 0]))
    a.pos("crystal", (0, [0, 0, 0]), (2.0, [0, 0.3, 0], "easeInOutSine"), (4.0, [0, 0, 0], "easeInOutSine"))
    a.rot("charm_1", (0, [0, 0, 4]), (2.0, [0, 0, -4], "easeInOutSine"), (4.0, [0, 0, 4], "easeInOutSine"))
    a.rot("charm_2", (0, [0, 0, -5]), (2.0, [0, 0, 5], "easeInOutSine"), (4.0, [0, 0, -5], "easeInOutSine"))
    cast = f.new(f"animation.{ITEM}.cast", 0.8)
    cast.scale("crystal", (0, [1, 1, 1]), (0.12, [1.45, 1.45, 1.45]), (0.8, [1, 1, 1], "easeOutQuad"))
    cast.rot("crystal", (0, [0, 0, 0]), (0.8, [0, 180, 0], "easeOutQuad"))
    return f


ICON = [
    "............s.s.",
    "..........t.sss.",
    "...........tVvt.",
    "..........tVWVt.",
    ".........tVWVvt.",
    "..........vVvt..",
    "........kbtt....",
    ".......kwwk.....",
    "......kwwk......",
    ".....kppk.......",
    "....kwppk.......",
    "...kwwk.........",
    "..kbwk..........",
    ".kwwk...........",
    ".ksk............",
    "..k.............",
]


def staff_icon():
    """The GUI icon: the staff on the diagonal like a vanilla sword, the violet crystal in its tines at the top right,
    the silver crescent over it, a plum grip and silver bands."""
    from common import art
    pal = {"k": hexc("#100b0e"), "w": WOOD[3], "b": SILVER[4], "s": SILVER[5], "t": WOOD[4], "V": VIOLET[4], "v": VIOLET[2],
           "W": VIOLET[5], "p": PLUM[4]}
    t = art(ICON, pal)
    item_outline(t, hexc("#100b0e"))
    return t


def bust():
    """Her bust on the trophies' pedestal: the lined, severe face; silver hair braided into a crown round her head, a
    violet stone set over her brow, the plait knotted at the back where it goes down inside the collar; the tall flared
    collar (black, lined in plum, edged in silver); the plum gown under the black overrobe with its silver-embroidered
    edges and an amulet; a violet crystal on a silver mount at her feet. The stones glow (full-bright faces)."""
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t

    def skin(x, y):
        return tone(SKIN, 3.3 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.6 * y / 15)

    def hair(x, y):
        # Combed back: fine strands running front to back.
        return tone(HAIR, 2.4 + 0.8 * math.sin(x * 2.3 + 0.4 * y) + (0.7 if (x * 7 + y) % 9 == 0 else 0))

    def braid(x, y):
        # A three-strand plait running along u (round the crown): chevrons, a dark crease between each lock.
        k = (x + abs((y % 6) - 2.5) * 1.3) % 4
        if k < 0.9:
            return HAIR[0]
        return tone(HAIR, 5.2 - (k - 0.9) * 0.7)
    rows = [   # x = 0 is the viewer's left
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhsssssssssssshh",
        "hssswwwwwwwwsssh",
        "hsbBBBbssbBBBbsh",
        "hsKKKKssssKKKKsh",
        "wsWIEsssssEIWssw",
        "hsuuussssssuuush",
        "hssssssnnssssssh",
        "hsssswsnnswssssh",
        "hssswsNnnNswsssh",
        "hssswssssssswssh",
        "hssssllMMllssssh",
        "hjsssssssssssjjh",
        "hjjsssssssssjjhh",
        "hhjjssssssssjjhh",
    ]
    legend = {"s": skin, "w": lambda x, y: shade(skin(x, y), 0.87), "b": HAIR[2], "B": HAIR[1], "K": LASH, "W": EYE_WHITE,
              "I": IRIS, "E": PUPIL, "u": mix(SKIN[2], hexc("#4a3a54"), 0.4), "n": SKIN[4], "N": SKIN[1],
              "l": LIPS[0], "M": LIPS[2], "j": lambda x, y: shade(skin(x, y), 0.88), "h": hair}

    def face(x, y):
        v = legend[rows[y][x]]
        return v(x, y) if callable(v) else v

    def robe(x, y):
        return tone(BLACK, 3.0 - y / 14 + 0.6 * math.sin(x * 1.2 + y * 0.5))

    def chest(x, y):
        # (uv rows 2..14 land on the front) The plum gown in a narrowing V, the overrobe's edges embroidered in silver
        # (a running stitch with a sigil dot every few texels), the black overrobe outside, a silver amulet on the gown.
        d = abs(x - 7.5)
        edge = 3.6 - y * 0.12
        if d < edge - 0.9:
            if 6 <= y <= 9 and d < 1.2:
                if y in (6, 9) or d > 0.6:
                    return SILVER[4] if (x + y) % 2 else SILVER[3]
                return VIOLET[4]
            return tone(PLUM, 3.4 - y / 10 + 0.5 * math.sin(x * 1.4 + y * 0.9))
        if d < edge + 0.1:
            return SILVER[5] if y % 3 == 0 else SILVER[3]
        return robe(x, y)

    def collar(x, y):
        # The collar's outside: black with a silver edge all round and a sigil stitched near the top.
        if x in (0, 15) or y in (0, 15):
            return SILVER[4] if (x + y) % 2 else SILVER[3]
        if y == 3 and 3 <= x <= 12 and x % 3 == 0:
            return SILVER[2]
        return robe(x, y)

    def lining(x, y):
        return tone(PLUM, 3.6 - y / 9 + 0.4 * math.sin(x * 0.9 + y * 0.7))

    def silver(x, y):
        return tone(SILVER, 3.2 + 0.9 * math.sin(x * 0.8 + y * 0.4) - (0.8 if y in (0, 15) else 0))

    def crystal(x, y):
        # Facets: a bright streak down one side, the heart pale, darker toward the edges.
        d = abs(x - 6.5) / 8
        v = 4.4 - 2.2 * d - 0.6 * ((x + 2 * y) % 5 == 0) + (1.0 if x in (4, 5) else 0)
        return tone(VIOLET, v)
    for name, fn in (("face", face), ("hair", hair), ("braid", braid), ("skin", skin), ("chest", chest), ("robe", robe),
                     ("collar", collar), ("lining", lining), ("silver", silver), ("crystal", crystal)):
        save(tex16(fn), "block", f"grand_witch_trophy_{name}")
    full = [0, 0, 16, 16]
    F, HR, BR, S, CH, R, CO, LI, AG, CR = ("face", "hair", "braid", "skin", "chest", "robe", "collar", "lining", "silver",
                                            "crystal")
    lift = 0.5   # she stands a head over the other witches: a taller neck under the same head
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": R}),
        # Narrow shoulders under the overrobe, the gown at the front.
        BM.cube("chest", (4.8, 3.4, 6.1), (11.2, 7.5, 9.9), {"north": CH, "*": R}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (4.2, 6.0, 6.3), (11.8, 7.7, 9.7), {"*": R}),
        BM.cube("neck", (7.1, 7.4, 7.1), (8.9, 8.6 + lift, 8.9), {"*": S}),
        # The tall flared collar: a back panel and two side wings leaning out, plum on the inside.
        BM.cube("collar_back", (4.9, 7.2, 9.6), (11.1, 11.6, 10.2), {"north": LI, "*": CO}, uv={"north": full, "south": full},
                rot=("x", 22.5, (8, 7.4, 9.9))),
        BM.cube("collar_r", (4.6, 7.2, 6.8), (5.2, 11.0, 9.7), {"east": LI, "*": CO}, uv={"east": full, "west": full},
                rot=("z", 22.5, (5.0, 7.4, 8.2))),
        BM.cube("collar_l", (10.8, 7.2, 6.8), (11.4, 11.0, 9.7), {"west": LI, "*": CO}, uv={"east": full, "west": full},
                rot=("z", -22.5, (11.0, 7.4, 8.2))),
        # The head, silver hair combed back, the braided crown round it, knotted at the back.
        BM.cube("head", (5.7, 8.4 + lift, 5.7), (10.3, 13.2 + lift, 10.3), {"north": F, "*": HR}, uv={"north": full}),
        BM.cube("hair_cap", (5.5, 11.6 + lift, 5.8), (10.5, 13.5 + lift, 10.5), {"north": None, "down": None, "*": HR}),
        BM.cube("crown_front", (5.6, 12.7 + lift, 5.15), (10.4, 14.3 + lift, 5.7), {"*": BR}),
        BM.cube("crown_back", (5.6, 12.7 + lift, 10.3), (10.4, 14.3 + lift, 10.85), {"*": BR}),
        BM.cube("crown_r", (4.95, 12.7 + lift, 5.15), (5.6, 14.3 + lift, 10.85), {"*": BR}),
        BM.cube("crown_l", (10.4, 12.7 + lift, 5.15), (11.05, 14.3 + lift, 10.85), {"*": BR}),
        BM.cube("setting", (7.35, 12.9 + lift, 4.95), (8.65, 14.1 + lift, 5.2), {"*": AG}),
        BM.cube("crown_stone", (7.6, 13.15 + lift, 4.8), (8.4, 13.85 + lift, 5.0), {"*": CR}, shade=False),
        BM.cube("knot", (7.0, 11.4 + lift, 10.2), (9.0, 13.0 + lift, 11.0), {"*": BR}),
        BM.cube("nose", (7.7, 9.8 + lift, 5.45), (8.3, 10.9 + lift, 5.7), {"*": S}),
        # The crystal on its silver mount at her feet.
        BM.cube("mount", (9.0, 3.0, 4.4), (10.9, 3.5, 6.0), {"*": AG}, rot=("y", 22.5, (9.95, 3.2, 5.2))),
        BM.cube("crystal", (9.5, 3.4, 4.7), (10.4, 5.6, 5.6), {"*": CR}, rot=("y", 45, (9.95, 4.5, 5.15)), shade=False),
        BM.cube("shard", (9.1, 3.3, 5.0), (9.6, 4.6, 5.5), {"*": CR}, rot=("z", 22.5, (9.35, 3.4, 5.25)), shade=False),
    ]
    for el in e:
        if el["name"] in ("crystal", "shard", "crown_stone"):
            el["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    mdl = BM.model({"base": "trophy_base", F: "grand_witch_trophy_face", HR: "grand_witch_trophy_hair", BR: "grand_witch_trophy_braid",
                    S: "grand_witch_trophy_skin", CH: "grand_witch_trophy_chest", R: "grand_witch_trophy_robe",
                    CO: "grand_witch_trophy_collar", LI: "grand_witch_trophy_lining", AG: "grand_witch_trophy_silver",
                    CR: "grand_witch_trophy_crystal"}, e, "trophy_base")
    BM.write("grand_witch_trophy", mdl)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=41000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    sm = staff_item_rig()
    st, sg = sm.build(gutter=1, seed=41900)
    sm.rig.write(ASSETS + "/geo/item/" + ITEM + ".geo.json")
    save(st, "item", ITEM)
    save(sg, "item", ITEM + "_glowmask")
    staff_item_anims().write(ASSETS + "/animations/item/" + ITEM + ".animation.json")
    save(staff_icon(), "item", ITEM + "_icon")
    print(f"{ITEM}: {sm.height:.1f} px tall")
    bust()


if __name__ == "__main__":
    generate()
