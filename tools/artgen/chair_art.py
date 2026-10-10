"""v0.18 Naomi's reprogramming chair: rig with straps and drill gantry, texture, glowmask, animations (HeavenAssets.CHAIR_*).

A clinical dentist's chair, too clean: a chrome pedestal on a round floor plate, a hydraulic column, a seat, backrest and
leg rest padded in pale cyan-white leather with stitched channels, chrome trim and armrests. Black leather straps with steel
buckles hold the wrists (on the armrests), the ankles (on the leg rest), the chest and the forehead. Behind the backrest a
chrome post rises to an articulated gantry: a boom reaching over the sitter, an elbow knuckle and the drill head hanging
from it -- a white housing ringed with cyan light, a chrome chuck and a long thin bit -- and a round surgical lamp beside it.

The sitter sits facing north (-Z) on a seat whose top is 8 px up (HeavenAssets.CHAIR_SEAT_HEIGHT), hips over z = 0, legs
straight forward along the leg rest (vanilla's riding pose).

Skeleton (Bedrock px; +X is the chair's LEFT; it faces north):
  chair -- base
        -- seat -- backrest (reclined 15 deg) -- headrest
                -- legrest, armrest_r, armrest_l
                -- straps       -- strap_chest, strap_head, strap_wrist_r/l, strap_ankle_r/l   (buckled shut)
                -- straps_open  -- open_chest_r/l, open_head_r/l, open_wrist_r/l, open_ankle_r/l (each a hanging flap)
        -- gantry -- gantry_boom -- gantry_elbow -- drill -- ring_light, drill_bit
                                 -- lamp
`straps` and `straps_open` are toggled by the renderer (shut while someone is strapped in).
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import E_IN, E_IO, E_OUT, E_SNAP, Clip, P_, tone
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
A = "animation.reprogramming_chair."

LEATHER = Ramp("#4f6c73", "#7a9aa1", "#a1bfc5", "#c1d9dd", "#d9eaec", "#edf7f8")
CHROME = Ramp("#22262b", "#454c55", "#727b86", "#a3acb7", "#cfd6de", "#f3f6f9")
PLASTIC = Ramp("#6f767e", "#959ca4", "#b7bdc4", "#d2d7dc", "#e6e9ec", "#f7f8f9")
STRAP = Ramp("#070504", "#110c09", "#1d1510", "#2a1f18", "#3a2b21", "#4d3a2d")
RUBBER = Ramp("#0b0c0e", "#141619", "#1d2024", "#272b30", "#33383e", "#41474e")
CYAN = Ramp("#1f6f86", "#2f95b0", "#4fbfd8", "#86def0", "#bff1fa", "#effdff")
WARM = Ramp("#7a6a3c", "#a8925a", "#cdb880", "#e6d6a6", "#f4ead0", "#fffbef")

SEAT_Y = 8.0          # top of the seat cushion
BACK_Z = 3.0          # where the backrest meets the seat
RECLINE = 15.0
SIT_HIP = 10.4        # a riding player's hips (feet attachment 0.6 below the seat point)


def recline_z(y):
    """The backrest's front face at height y."""
    return BACK_Z + (y - SEAT_Y) * math.tan(math.radians(RECLINE))


# =====================================================================================================
# Materials
# =====================================================================================================

def padded(seed, channels=6, along="y", base=3.2):
    """Leather upholstery: top-lit, stitched channels (a dark seam with a pale stitch every other texel), soft pillowing
    between seams, piping round the edges."""
    def m(f, x, y, w, h):
        if f == "down":
            return LEATHER[1]
        u, v, L = (x, y, w) if along == "y" else (y, x, h)
        n = fbm(x * 2 + len(f) * 5, y * 2, seed, 64, 64, 2, 7.0)
        if f in ("north", "up") or (f == "south" and along == "y"):
            period = max(4, L // channels)
            k = u % period
            pillow = math.sin(math.pi * k / period)
            val = base - 0.3 + 0.8 * pillow + (n - 0.5) * 0.15 - 0.5 * (v / max(1, (h if along == "y" else w)))
            if k == 0:
                return LEATHER[2]
            if k in (1, period - 1) and v % 2 == 0:
                return LEATHER[3]           # the stitching either side of the seam
        else:
            val = base - 0.3 + (n - 0.5) * 0.15
        # Piping: a rolled edge round every face.
        if x == 0 or y == 0 or x == w - 1 or y == h - 1:
            return LEATHER[4] if (y == 0 and f != "up") else LEATHER[2]
        return tone(LEATHER, val)
    return m


def chrome_m(f, x, y, w, h):
    """Polished chrome: a bright streak down each face, a dark reflected horizon, hard top light."""
    if f == "up":
        return CHROME[5]
    if f == "down":
        return CHROME[1]
    t = x / max(1, w - 1)
    v = 2.6 + 2.2 * math.exp(-((t - 0.3) / 0.13) ** 2) - 1.4 * math.exp(-((t - 0.72) / 0.1) ** 2)
    if f in ("east", "west"):
        v -= 0.3
    return tone(CHROME, v)


def chrome_v(f, x, y, w, h):
    """Chrome on a horizontal bar: the streak runs along it."""
    if f in ("up",):
        return CHROME[5] if y % 3 else CHROME[4]
    if f == "down":
        return CHROME[1]
    t = y / max(1, h - 1)
    return tone(CHROME, 4.3 - 2.6 * t)


def plastic_m(seed, base=3.6):
    def m(f, x, y, w, h):
        if f == "up":
            return tone(PLASTIC, base + 0.8)
        if f == "down":
            return tone(PLASTIC, base - 1.6)
        n = fbm(x * 2, y * 2 + len(f), seed, 64, 64, 2, 9.0)
        v = base + 0.5 - 0.9 * y / max(1, h) + (n - 0.5) * 0.25
        if f in ("east", "west"):
            v -= 0.3
        if y == h - 1:
            v -= 0.6
        return tone(PLASTIC, v)
    return m


def strap_m(seed, buckle_at=None, holes=True):
    """A leather strap: stitched edges, punched holes along it; `buckle_at` = (axis, index) of a steel buckle across it."""
    def m(f, x, y, w, h):
        n = fbm(x * 3 + len(f), y * 3, seed, 64, 64, 2, 9.0)
        along = max(w, h)
        u, v, L, W = (x, y, w, h) if w >= h else (y, x, h, w)
        if buckle_at is not None and abs(u - buckle_at * L) < 1.6:
            if abs(u - buckle_at * L) > 0.8 or v in (0, W - 1):
                return CHROME[4] if (u + v) % 2 else CHROME[3]     # the buckle frame
            return CHROME[1]
        if W >= 3 and v in (0, W - 1):
            return STRAP[3] if u % 2 else STRAP[1]               # stitched edges
        if holes and W >= 3 and v == W // 2 and u % 4 == 2 and f in ("north", "up", "south", "east", "west"):
            return STRAP[0]
        return tone(STRAP, 2.4 + (n - 0.5) * 0.9 + (0.6 if f == "up" else 0))
    return m


def light_m(core):
    def m(f, x, y, w, h):
        return CYAN[core] if (x + y) % 3 else CYAN[core - 1]
    return m


def full_glow(f, x, y, w, h, c):
    return (c[0], c[1], c[2], 255)


def faint(k):
    return lambda f, x, y, w, h, c: shade(c, k)


# =====================================================================================================
# The rig
# =====================================================================================================

def cyl(m, bone, c, d, h, mat, glow=None, density=2, tag="cyl", y_axis=True):
    """An octagonal column: a square and the same square turned 45 deg about its own axis."""
    cx, cy, cz = c
    s = d * 0.86
    if y_axis:
        m.cube(bone, (cx - d / 2, cy, cz - d / 2), (d, h, d), mat, glow, density=density, tag=tag)
        m.cube(bone, (cx - s / 2, cy, cz - s / 2), (s, h, s), mat, glow, density=density, tag=tag, rotation=(0, 45, 0), pivot=(cx, cy, cz))
    else:  # along Z
        m.cube(bone, (cx - d / 2, cy - d / 2, cz), (d, d, h), mat, glow, density=density, tag=tag)
        m.cube(bone, (cx - s / 2, cy - s / 2, cz), (s, s, h), mat, glow, density=density, tag=tag, rotation=(0, 0, 45), pivot=(cx, cy, cz))


def skeleton(m):
    m.bone("chair", (0, 0, 0))
    m.bone("base", (0, 0, 0), "chair")
    m.bone("seat", (0, SEAT_Y, 0), "chair")
    m.bone("backrest", (0, SEAT_Y, BACK_Z), "seat", rotation=(-RECLINE, 0, 0))
    m.bone("headrest", (0, 25.0, BACK_Z), "backrest")
    m.bone("legrest", (0, SEAT_Y - 0.3, -5.0), "seat", rotation=(8, 0, 0))
    for side, s in (("r", -1), ("l", 1)):
        m.bone(f"armrest_{side}", (6.4 * s, 10.0, 0), "seat")
    m.bone("straps", (0, SEAT_Y, 0), "seat")
    m.bone("straps_open", (0, SEAT_Y, 0), "seat")
    m.bone("gantry", (0, 6.0, 9.5), "chair")
    m.bone("gantry_boom", (0, 41.0, 9.5), "gantry")
    m.bone("gantry_elbow", (0, 41.0, -1.5), "gantry_boom")
    m.bone("drill", (0, 39.5, -1.5), "gantry_elbow")
    m.bone("ring_light", (0, 34.5, -1.5), "drill")
    m.bone("drill_bit", (0, 33.0, -1.5), "drill")
    m.bone("lamp", (3.5, 41.0, 4.0), "gantry_boom")


def build_base(m):
    b = "base"
    # The round floor plate: two squares crossed (an octagon), a chamfered rim, rubber feet.
    for rot in (0, 45):
        m.cube(b, (-6.5, 0, -6.5), (13, 0.8, 13), lambda f, x, y, w, h: chrome_v(f, x, y, w, h) if f != "up" else
               (CHROME[4] if (x + y) % 9 else CHROME[5]), faint(0.0), density=2, tag="plate", rotation=(0, rot, 0), pivot=(0, 0, 0))
    m.cube(b, (-4.5, 0.8, -4.5), (9, 0.5, 9), lambda f, x, y, w, h: CHROME[3] if f != "up" else CHROME[4], None, density=2,
           tag="plate_step", rotation=(0, 22.5, 0), pivot=(0, 0, 0))
    # The hydraulic column: a white shroud, the chrome ram above it, a collar under the seat.
    cyl(m, b, (0, 1.3, 0), 4.4, 3.2, plastic_m(19001, 3.4), None, tag="column")
    m.cube(b, (-2.4, 4.2, -2.4), (4.8, 0.4, 4.8), solid(CHROME[2]), None, density=2, tag="column_band")
    cyl(m, b, (0, 4.6, 0), 2.6, 2.4, chrome_m, None, tag="ram")
    m.cube(b, (-3.2, 6.6, -3.2), (6.4, 0.6, 6.4), chrome_v, None, density=2, tag="collar")
    # A foot pedal on the floor plate, a cable running back from it.
    m.cube(b, (-5.6, 0.8, -6.2), (2.6, 0.6, 1.8), lambda f, x, y, w, h: RUBBER[3] if f == "up" and x % 2 else RUBBER[2], None,
           density=4, tag="pedal")
    m.cube(b, (-4.6, 0.8, -4.4), (0.5, 0.35, 5.0), solid(RUBBER[1]), None, density=4, tag="pedal_cable")


def build_seat(m):
    s = "seat"
    # Under-frame: a chrome plate the seat sits on, a white shroud round its edge.
    m.cube(s, (-5.0, 6.8, -5.4), (10, 0.5, 9.2), chrome_v, None, density=2, tag="seat_frame")
    m.cube(s, (-4.6, 7.0, -5.0), (9.2, 0.4, 8.4), plastic_m(19002, 3.0), None, inflate=0.1, density=2, tag="seat_shroud")
    # The cushion, a rolled front edge.
    m.cube(s, (-4.8, 7.3, -5.0), (9.6, SEAT_Y - 7.3, 8.2), padded(19003, 4, "y"), None, density=2, tag="seat_cushion")
    m.cube(s, (-4.8, 7.2, -5.6), (9.6, 0.8, 0.8), lambda f, x, y, w, h: LEATHER[4] if f == "up" else LEATHER[2], None, density=2,
           tag="seat_roll")
    # Backrest: the cushion in its reclined bone, a white shell behind it, the headrest on a chrome stem.
    br = "backrest"
    m.cube(br, (-4.6, SEAT_Y, BACK_Z), (9.2, 16.0, 1.6), padded(19004, 4, "y"), None, density=2, tag="back_cushion")
    m.cube(br, (-4.2, SEAT_Y + 1.0, BACK_Z + 1.6), (8.4, 14.0, 0.8), plastic_m(19005, 3.3), None, density=2, tag="back_shell")
    for sd in (-1, 1):
        m.cube(br, (4.6 * sd - 0.35, SEAT_Y + 0.5, BACK_Z + 0.2), (0.7, 15.0, 1.4), chrome_m, None, density=2, tag="back_rail")
    m.cube(br, (-0.6, SEAT_Y + 15.0, BACK_Z + 0.6), (1.2, 2.6, 0.8), chrome_m, None, density=4, tag="head_stem")
    hr = "headrest"
    m.cube(hr, (-3.0, 25.0, BACK_Z - 0.1), (6.0, 3.6, 1.8), padded(19006, 3, "y"), None, density=2, tag="head_cushion")
    # Two wings either side of the head: the head is held still.
    for sd in (-1, 1):
        m.cube(hr, (3.0 * sd - 0.5 + (0 if sd > 0 else -0.6), 25.2, BACK_Z - 1.6), (1.1, 3.2, 1.8), padded(19007 + sd, 2, "y"), None,
               density=2, tag="head_wing", rotation=(0, -18 * sd, 0), pivot=(3.0 * sd, 25.2, BACK_Z))
    # Leg rest: long and nearly flat (the legs lie straight along it), its end rolled up.
    lr = "legrest"
    m.cube(lr, (-4.4, SEAT_Y - 1.3, -13.0), (8.8, 1.3, 8.0), padded(19008, 4, "x"), None, density=2, tag="leg_cushion")
    m.cube(lr, (-4.0, SEAT_Y - 1.8, -12.6), (8.0, 0.5, 7.4), plastic_m(19009, 3.0), None, density=2, tag="leg_shell")
    m.cube(lr, (-4.4, SEAT_Y - 1.4, -13.9), (8.8, 1.9, 1.0), lambda f, x, y, w, h: LEATHER[4] if f == "up" else LEATHER[2], None,
           density=2, tag="leg_end")
    # Armrests: a chrome arm out of the seat's side, a padded rest on top, the wrist cuff's anchor plate.
    for side, sd in (("r", -1), ("l", 1)):
        a = f"armrest_{side}"
        x0 = 6.4 * sd
        m.cube(a, (x0 - 0.4, SEAT_Y - 0.6, 0.6), (0.8, 2.6, 0.8), chrome_m, None, density=4, tag="arm_post")
        m.cube(a, (4.9 * sd - (1.6 if sd > 0 else 0), SEAT_Y - 0.6, 0.6), (1.6, 0.6, 0.8), chrome_v, None, density=4, tag="arm_strut")
        m.cube(a, (x0 - 1.7, 9.0, -5.2), (3.4, 1.2, 7.2), padded(19010 + sd, 2, "x"), None, density=2, tag="arm_pad")
        m.cube(a, (x0 - 1.8, 8.7, -5.3), (3.6, 0.35, 7.4), chrome_v, None, density=2, tag="arm_trim")
    # A side tray with instruments on the right armrest, a cyan readout on the left.
    m.cube("armrest_r", (-10.4, 9.4, -4.8), (2.4, 0.25, 3.4), chrome_v, None, density=4, tag="tray")
    for i in range(3):
        m.cube("armrest_r", (-10.1 + i * 0.7, 9.65, -4.4), (0.25, 0.2, 2.4), solid(CHROME[4 + (i % 2)]), None, density=4, tag="instrument")
    m.cube("armrest_l", (8.15, 9.3, -4.6), (0.3, 1.6, 2.6), lambda f, x, y, w, h: (CYAN[3] if (y % 3 == 1 and x > 1) else RUBBER[1])
           if f == "east" else RUBBER[2], lambda f, x, y, w, h, c: full_glow(f, x, y, w, h, c) if c[2] > 150 else None, density=4,
           tag="readout")


# --- straps ------------------------------------------------------------------------------------------------------------

def band(m, bone, x0, x1, y0, h, z0, z1, seed, buckle=0.5, rotation=None, pivot=None, tag="strap"):
    """A band round a box (x0..x1, z0..z1) at height y0: an open sleeve (no top or bottom)."""
    return m.cube(bone, (x0, y0, z0), (x1 - x0, h, z1 - z0), none_on(("up", "down"), strap_m(seed, buckle)), None, density=4,
                  tag=tag, rotation=rotation, pivot=pivot)


def build_straps(m):
    sb = "straps"
    # Bones for each band (same pivots as their open flaps), so the renderer can swap the groups cleanly.
    head_y, chest_y = 27.6, 17.4
    m.bone("strap_chest", (0, chest_y, 0), sb)
    m.bone("strap_head", (0, head_y, 0), sb)
    for side, s in (("r", -1), ("l", 1)):
        m.bone(f"strap_wrist_{side}", (6.2 * s, 10.6, 0), sb)
        m.bone(f"strap_ankle_{side}", (2.0 * s, 9.2, -10.0), sb)
    # Chest: round the sitter's chest (x -4..4, z -2..2) and back to the reclined backrest.
    zb = recline_z(chest_y) + 0.3
    band(m, "strap_chest", -4.7, 4.7, chest_y, 1.5, -2.7, zb, 19101, 0.5)
    m.cube("strap_chest", (-0.9, chest_y - 0.2, -3.0), (1.8, 1.9, 0.35), lambda f, x, y, w, h: CHROME[5] if (x in (0, w - 1) or y in (0, h - 1))
           else CHROME[1], None, density=4, tag="buckle")
    # Forehead: across the brow to the headrest.
    zh = recline_z(head_y) + 0.6
    band(m, "strap_head", -4.5, 4.5, head_y, 1.1, -4.5, zh, 19102, 0.5)
    m.cube("strap_head", (-0.8, head_y - 0.15, -4.85), (1.6, 1.4, 0.35), lambda f, x, y, w, h: CHROME[5] if (x in (0, w - 1) or y in (0, h - 1))
           else CHROME[1], None, density=4, tag="buckle")
    for side, s in (("r", -1), ("l", 1)):
        # Wrists: round the hanging forearm (x 4..8) and the armrest under it.
        x0, x1 = (4.0 * s, 8.4 * s) if s > 0 else (8.4 * s, 4.0 * s)
        band(m, f"strap_wrist_{side}", x0 - 0.2, x1 + 0.2, 10.6, 1.4, -2.5, 2.5, 19110 + s, 0.25)
        # Ankles: over each leg lying along the leg rest, its ends into the rest's sides.
        lx0, lx1 = (0.0, 4.4) if s > 0 else (-4.4, 0.0)
        band(m, f"strap_ankle_{side}", lx0 - 0.1, lx1 + 0.1, 7.0, 5.4, -11.0, -9.4, 19120 + s, 0.5)


def flap(m, bone, parent, root, length, width, seed, rest, tag="strap_flap", thick=0.4):
    """One end of an undone strap hanging from its anchor: a bone at the anchor, the flap hanging down (-Y) and turned by
    `rest`; a buckle at its tip."""
    x, y, z = root
    b = m.bone(bone, root, parent, rotation=rest)
    m.cube(b, (x - width / 2, y - length, z - thick / 2), (width, length, thick), strap_m(seed, None), None, density=4, tag=tag)
    m.cube(b, (x - width / 2 - 0.1, y - length - 0.2, z - thick / 2 - 0.1), (width + 0.2, 1.0, thick + 0.2),
           lambda f, xx, yy, w, h: CHROME[5] if (xx in (0, w - 1) or yy in (0, h - 1)) else CHROME[1], None, density=4, tag="flap_buckle")
    return b


# Where each open flap hangs from, and the rotation that brings it shut (relative to its rest).
OPEN = {}


def build_open_straps(m):
    so = "straps_open"
    chest_y, head_y = 17.4, 27.6
    for side, s in (("r", -1), ("l", 1)):
        sw = 1 if s > 0 else -1
        # Chest and forehead: each end hangs down the backrest's side from where it is riveted.
        zc = recline_z(chest_y) + 0.2
        flap(m, f"open_chest_{side}", so, (4.9 * s, chest_y + 1.4, zc), 7.0, 1.5, 19130 + s, (6, 0, 4 * s))
        OPEN[f"open_chest_{side}"] = [-96, 10 * s, -4 * s]
        zh = recline_z(head_y) + 0.4
        flap(m, f"open_head_{side}", so, (4.7 * s, head_y + 1.1, zh), 6.4, 1.1, 19132 + s, (4, 0, 3 * s))
        OPEN[f"open_head_{side}"] = [-94, 8 * s, -3 * s]
        # Wrists: the strap hangs off the armrest's outer edge.
        flap(m, f"open_wrist_{side}", so, (8.3 * s, 10.2, 0.0), 4.2, 1.4, 19134 + s, (0, 0, 8 * s))
        OPEN[f"open_wrist_{side}"] = [0, 0, 100 * sw]
        # Ankles: down the leg rest's sides.
        flap(m, f"open_ankle_{side}", so, (4.6 * s, 7.4, -10.2), 4.6, 1.6, 19136 + s, (0, 0, 6 * s))
        OPEN[f"open_ankle_{side}"] = [0, 0, 100 * sw]
    # The rivets the open ends hang from (part of the open group: the shut bands cover them).
    for side, s in (("r", -1), ("l", 1)):
        for p in ((4.9 * s, chest_y + 1.4, recline_z(chest_y) + 0.2), (4.7 * s, head_y + 1.1, recline_z(head_y) + 0.4)):
            m.cube(so, (p[0] - 0.35, p[1] - 0.35, p[2] - 0.35), (0.7, 0.7, 0.7), solid(CHROME[4]), None, density=4, tag="rivet")


# --- the gantry, the drill, the lamp -----------------------------------------------------------------------------------

def build_gantry(m):
    g = "gantry"
    # An arm out of the column, back behind the backrest, then the post straight up.
    m.cube(g, (-0.9, 5.4, 1.5), (1.8, 1.4, 8.4), chrome_v, None, density=2, tag="gantry_foot")
    m.cube(g, (-1.3, 5.0, 8.5), (2.6, 2.2, 2.6), plastic_m(19201, 3.2), None, density=2, tag="gantry_joint")
    cyl(m, g, (0, 7.2, 9.5), 1.8, 32.6, chrome_m, None, tag="gantry_post")
    # Cable clips up the post.
    for y in (14, 22, 30):
        m.cube(g, (-1.15, y, 8.35), (2.3, 0.6, 2.3), solid(PLASTIC[3]), None, density=4, tag="clip")
    m.cube(g, (-0.3, 7.2, 10.5), (0.6, 32.0, 0.6), solid(RUBBER[2]), None, density=2, tag="cable")
    # The boom over the sitter, a white housing over a chrome beam, a counterweight behind.
    bm = "gantry_boom"
    m.cube(bm, (-1.6, 39.8, 8.2), (3.2, 2.6, 2.6), plastic_m(19202, 3.4), None, density=2, tag="boom_hub")
    m.cube(bm, (-0.8, 40.3, -1.5), (1.6, 1.4, 10.0), chrome_v, None, density=2, tag="boom_beam")
    m.cube(bm, (-1.2, 41.2, 0.5), (2.4, 0.9, 7.6), plastic_m(19203, 3.8), None, density=2, tag="boom_cover")
    m.cube(bm, (-1.3, 39.6, 10.8), (2.6, 3.0, 2.6), plastic_m(19204, 2.8), None, density=2, tag="counterweight")
    # The elbow: a knuckle with a white cap and a cyan ring.
    el = "gantry_elbow"
    cyl(m, el, (0, 39.5, -1.5), 2.6, 2.6, plastic_m(19205, 3.6), None, tag="knuckle")
    m.cube(el, (-1.45, 40.3, -2.95), (2.9, 0.4, 2.9), light_m(4), full_glow, density=4, tag="knuckle_ring")
    # The drill head: a white housing tapering to a chrome chuck; a cyan ring light round its lower lip; vents.
    d = "drill"
    def housing(f, x, y, w, h):
        c = plastic_m(19206, 3.8)(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and 1 <= y <= 3 and x % 2 == 1:
            return PLASTIC[1]                   # vent slots
        if f == "north" and y == h // 2 and abs(x - (w - 1) / 2) < 1:
            return CYAN[4]                      # status pip
        return c
    # A chrome telescoping stem up into the knuckle: it slides out as the head comes down.
    cyl(m, d, (0, 38.6, -1.5), 1.2, 3.4, chrome_m, None, density=4, tag="drill_stem")
    cyl(m, d, (0, 35.4, -1.5), 3.0, 4.1, housing, lambda f, x, y, w, h, c: full_glow(f, x, y, w, h, c) if c == CYAN[4] else None,
        tag="drill_housing")
    cyl(m, d, (0, 34.8, -1.5), 2.2, 0.7, chrome_m, None, density=4, tag="drill_nose")
    rl = "ring_light"
    for rot in (0, 45):
        m.cube(rl, (-1.9, 34.4, -3.4), (3.8, 0.5, 3.8), light_m(4), full_glow, density=4, tag="ring", rotation=(0, rot, 0),
               pivot=(0, 34.4, -1.5))
    cyl(m, d, (0, 33.5, -1.5), 1.3, 1.3, chrome_m, None, density=4, tag="chuck")
    # The bit: long and thin, a spiral flute painted down it.
    def flute(f, x, y, w, h):
        k = (y + {"north": 0, "east": 1, "south": 2, "west": 3}.get(f, 0)) % 4
        return CHROME[5] if k == 0 else (CHROME[2] if k == 2 else CHROME[3])
    m.cube("drill_bit", (-0.25, 29.6, -1.75), (0.5, 3.9, 0.5), flute, faint(0.0), density=4, tag="bit")
    m.cube("drill_bit", (-0.15, 29.2, -1.65), (0.3, 0.4, 0.3), solid(CHROME[5]), None, density=4, tag="bit_tip")
    # The surgical lamp: a round white dish on a short arm, its face full of light.
    lp = "lamp"
    m.cube(lp, (3.0, 39.6, 3.6), (0.8, 1.6, 0.8), chrome_m, None, density=4, tag="lamp_arm")
    for rot in (0, 45):
        m.cube(lp, (1.4, 37.6, 2.0), (4.2, 2.0, 4.2), lambda f, x, y, w, h: (WARM[4] if (x + y) % 2 else WARM[5]) if f == "down"
               else plastic_m(19207, 3.7)(f, x, y, w, h),
               lambda f, x, y, w, h, c: (c[0], c[1], c[2], 255) if f == "down" else None, density=2, tag="lamp_dish",
               rotation=(0, rot, 0), pivot=(3.5, 38.6, 4.1))


def rig():
    m = Model("reprogramming_chair", 512, 256)
    skeleton(m)
    build_base(m)
    build_seat(m)
    build_straps(m)
    build_open_straps(m)
    build_gantry(m)
    return m


# =====================================================================================================
# Animations
# =====================================================================================================

DRILL_DROP = 3.4      # px the drill comes down over the struggle (the bit's tip ends at the sitter's brow)


def spin_keys(c, t0, t1, rate0, rate1, n=12):
    """Keys the bit spinning (deg/s ramping from rate0 to rate1) between t0 and t1."""
    ang = 0.0
    prev = t0
    out = [(t0, 0.0)]
    for i in range(1, n + 1):
        t = t0 + (t1 - t0) * i / n
        r = rate0 + (rate1 - rate0) * (i - 0.5) / n
        ang += r * (t - prev)
        prev = t
        out.append((t, ang))
    return out


def anims():
    m = rig()
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    flaps = sorted(OPEN)
    shut = {n: OPEN[n] for n in flaps}

    # idle: the gantry hums -- the drill head drifting a hair, the ring light breathing, the open straps stirring.
    c = clip("idle", 4.0, loop=True)
    for t in (0, 1, 2, 3, 4.0):
        k = math.sin(math.pi * t / 2)
        pose = {"@drill": [0, 0.2 * k, 0], "%ring_light": [1 + 0.05 * abs(k), 1, 1 + 0.05 * abs(k)], "gantry_elbow": [0, 2 * k, 0]}
        for i, n in enumerate(flaps):
            pose[n] = [1.5 * math.sin(math.pi * t / 2 + i), 0, 1.0 * math.sin(math.pi * t / 2 + i * 0.7)]
        c.key(t, pose, E_IO if t else None)

    # strap: every undone strap whips round and buckles shut (show `straps` from the end).
    c = clip("strap", 0.6, hold=True)
    c.key(0, {n: [0, 0, 0] for n in flaps})
    c.key(0.3, {n: [v * 0.8 for v in shut[n]] for n in flaps}, E_OUT)
    c.key(0.45, {n: [v * 1.05 for v in shut[n]] for n in flaps}, E_SNAP)
    c.key(0.6, shut, E_OUT)

    # drill_down: the boom dips, the head comes down to the brow, the bit spinning up to a scream; held at the bottom.
    c = clip("drill_down", 3.5, hold=True)
    for t, ang in spin_keys(c, 0.0, 3.5, 200, 2600, 14):
        x = min(1.0, t / 3.2)
        e = x * x * (3 - 2 * x)
        c.key(t, {"@drill": [0, -DRILL_DROP * e, 0], "gantry_boom": [-2.5 * e, 0, 0], "gantry_elbow": [2.5 * e, 0, 0],
                  "drill_bit": [0, ang, 0], "%ring_light": [1 + 0.15 * e, 1, 1 + 0.15 * e]})

    # release: the buckles burst, the straps fly open (overshooting and swinging), the drill lifts away.
    c = clip("release", 0.8)
    c.key(0, P_(shut, {"@drill": [0, -DRILL_DROP, 0], "gantry_boom": [-2.5, 0, 0], "gantry_elbow": [2.5, 0, 0]}))
    c.key(0.18, P_({n: [-v * 0.25 for v in shut[n]] for n in flaps}, {"@drill": [0, -1.0, 0], "gantry_boom": [1.0, 0, 0]}), E_SNAP)
    c.key(0.45, P_({n: [v * 0.1 for v in shut[n]] for n in flaps}, {"@drill": [0, 0.3, 0]}), E_OUT)
    c.key(0.8, {n: [0, 0, 0] for n in flaps}, E_IO)

    for cl in clips:
        cl.done(step=0.05)
    return f, clips


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=19000)
    m.rig.write(GEO + "reprogramming_chair.geo.json")
    save(t, "entity", "reprogramming_chair")
    save(g, "entity", "reprogramming_chair_glowmask")
    f, _ = anims()
    f.write(ANIM + "reprogramming_chair.animation.json")


if __name__ == "__main__":
    generate()
