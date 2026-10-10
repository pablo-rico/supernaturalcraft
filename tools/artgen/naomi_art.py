"""Naomi (v0.18): Heaven's reprogrammer -- her GeckoLib rig, texture, glowmask and every clip (names in HeavenAssets.NAOMI_*).

A composed, severe woman (31 px to the crown, slim, in low heels): auburn hair drawn back hard into a tight bun pinned at the
crown of the back of her head, fair skin, pale blue-grey eyes that never quite blink, a thin closed mouth. A tailored
charcoal skirt suit -- a fitted single-button jacket with notch lapels over an ivory silk blouse, a pencil skirt to the knee
-- sheer stockings and black patent pumps, a pearl at each ear. Over it all, when she works, a crisp white lab coat to the
knee (its own group, `coat`, with its own sleeves and skirt: the renderer may hide it), a clip-on Heaven ID on its pocket.

Props (HeavenAssets.NAOMI_PROP_CLIPS: hidden unless their clip plays):
  drill          a cordless surgical bone drill in her right hand (white body, steel chuck, a long fluted bit `drill_bit`
                 that the clips spin, a cyan status light in the glowmask);
  palm_light_r/l a disc of white light on each palm (the wipe, the strike, the restraint);
  death_light    the grace tearing out of her: beams from her eyes and her mouth (full bright).

Skeleton (Bedrock px; +X is her LEFT; the model faces north):
  root -- body -- head -- hair -- bun;  earring_r, earring_l;  death_light
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_middle, right_thumb,
                                                             palm_light_r, drill -- drill_bit, drill_trigger
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_index, left_middle, left_thumb, palm_light_l
               -- jacket_hem_r, jacket_hem_l, jacket_hem_back;  skirt_front, skirt_back
       -- right_leg -- right_shin -- right_foot,  left_leg -- left_shin -- left_foot
       -- coat -- coat_body -- coat_right_arm -- coat_right_forearm, coat_left_arm -- coat_left_forearm,
                              coat_collar, coat_front_r, coat_front_l, coat_back, coat_side_r, coat_side_l, badge
Every clip keys the coat's copies exactly like her own limbs (`Clip`), so the coat moves with her.
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from legacy_art import Build, SIDES, reach as _reach, walk_cycle
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, aim, aim2, fabric, to_preview, tone, world_point,
                         write_compact)
from michael_art import Clip as MClip
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.naomi."
CLEAR = (0, 0, 0, 0)

# --- proportions (a slim woman in low heels) ------------------------------------------------------------------------
NB = Build(hip=11.6, neck=23.4, tw=3.3, aw=2.9, ax=4.85, sx=4.45, sy=22.5, elbow=17.9, wrist=13.9, palm=12.1, lx=1.7, lw=3.2,
           knee=6.0, shoe_h=1.1)
HS = 7.5            # her head: 7.5 px (15 texels at density 2)
WAIST = 16.6        # where the chest gives way to the waist
TWW = 2.95          # waist half-width
HEEL = 1.1

# --- palette --------------------------------------------------------------------------------------------------------
SKIN = Ramp("#6f4a3f", "#a5786a", "#c99b8a", "#dfb6a4", "#ecc9b9", "#f6ddd0")
HAIR = Ramp("#1e0c06", "#34160b", "#4e2312", "#6a321b", "#864428", "#a35a38")
SUIT = Ramp("#1b1d22", "#26292f", "#33363d", "#41454d", "#52565f", "#666a74")
BLOUSE = Ramp("#a59f92", "#c2bcae", "#d8d2c4", "#e8e3d6", "#f3efe5", "#fcfaf4")
LAB = Ramp("#8f97a1", "#adb4bd", "#c8ced5", "#dde1e6", "#eceff2", "#fafbfc")
PATENT = Ramp("#030304", "#08080a", "#101013", "#1a1a1f", "#2c2c34", "#6a6c78")
STOCKING = hexc("#3c3533")
LIPS = Ramp("#5a2a2a", "#7a3b3a", "#94504c", "#a8605a", "#bb746c", "#cc8a80")
EYE_WHITE = hexc("#ece6e0")
IRIS = hexc("#5d7a8e")
PUPIL = hexc("#26333d")
LASH = hexc("#2a1a14")
PEARL = Ramp("#a7a196", "#c4beb2", "#dad5ca", "#e9e5dc", "#f5f2ec", "#ffffff")
STEEL = Ramp("#2a2f36", "#454c56", "#68717d", "#8f98a3", "#b8c0c9", "#e4e9ee")
PLASTIC = Ramp("#9aa3ab", "#b7bec5", "#cdd3d8", "#dfe3e7", "#eceff1", "#f8fafb")
CYAN = Ramp("#0d5964", "#14808c", "#27a8b3", "#55cdd4", "#94e6ea", "#d6fbfc")
GOLD = Ramp("#5a3a0c", "#8a5c14", "#b8851f", "#ddaf38", "#f3d36a", "#fff1b8")
LIGHT = Ramp("#c9b98a", "#e0d4ad", "#efe7cc", "#f8f3e3", "#fdfbf3", "#ffffff")
WHITE = hexc("#ffffff")

LIMBS = ("body", "right_arm", "right_forearm", "left_arm", "left_forearm")
PARENT = {"body": None, "right_arm": "body", "right_forearm": "right_arm", "left_arm": "body", "left_forearm": "left_arm"}


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(seed):
    return H.skin_mat(SKIN, seed, 3.35, 0.3)


def hair_m(seed, base=2.6):
    """Sleek, drawn hard back: fine strands run front to back on top, sweep back along the sides and up the nape to the bun;
    a soft band of shine across the crown. Little noise: it is glossy, every hair in place."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 11.0)
        if f in ("up", "down"):
            strand = math.sin(x * 2.6 + n * 2)
            v = base + 0.2 * strand + (n - 0.5) * 0.25
            if 0.3 < y / max(1, h) < 0.55:
                v += 0.8 - 1.6 * abs(y / max(1, h) - 0.42)        # the shine across the crown
        elif f == "south":
            # Drawn up from the nape towards the bun: strands converge on the top middle.
            dx = (x - (w - 1) / 2) / max(1.0, w / 2)
            strand = math.sin(dx * 9 / max(0.35, 1.3 - y / max(1, h)) + n * 2)
            v = base - 0.2 + 0.2 * strand - 0.5 * y / max(1, h)
        else:
            strand = math.sin(y * 2.4 + x * 0.3 + n * 2)
            v = base + 0.2 * strand + 0.5 * (1 - y / max(1, h)) - 0.3
        return tone(HAIR, v)
    return m


def suit_m(seed, base=3.0, crease=False):
    """Fine charcoal wool, top-lit, faint pinstripe."""
    fab = fabric(SUIT, seed, base, 0.22, crease=crease)

    def m(f, x, y, w, h):
        c = fab(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and x % 5 == 2:
            c = mix(c, SUIT[5], 0.18)
        return c
    return m


def blouse_m(seed):
    """Ivory silk: a soft sheen that runs diagonally."""
    def m(f, x, y, w, h):
        v = 3.7 + 0.5 * math.sin(x * 0.8 - y * 0.45 + seed) - 0.5 * y / max(1, h)
        return tone(BLOUSE, v - (0.4 if f in ("east", "west", "south") else 0))
    return m


def lab_m(seed, base=3.8):
    """Starched white cotton: crisp, a few pressed creases, top-lit."""
    def m(f, x, y, w, h):
        if f == "up":
            return tone(LAB, base + 0.6)
        if f == "down":
            return tone(LAB, base - 1.4)
        n = fbm(x + len(f) * 5, y, seed, 64, 64, 2, 9.0)
        v = base + 0.3 - 0.6 * y / max(1, h) + (n - 0.5) * 0.4 + 0.3 * math.sin(x * 0.7 + seed)
        if f in ("east", "west"):
            v -= 0.35
        elif f == "south":
            v -= 0.2
        if f != "up" and y == h - 1:
            v -= 0.6
        return tone(LAB, v)
    return m


def stocking_m(seed):
    """Sheer stockings: her skin through a smoky veil, a sheen down the front of the shin."""
    sk = skin_m(seed)

    def m(f, x, y, w, h):
        c = mix(sk(f, x, y, w, h), STOCKING, 0.46)
        if f == "north" and abs(x - (w - 1) / 2) < 0.8:
            c = mix(c, SKIN[5], 0.25)
        return c
    return m


def pump_m(f, x, y, w, h):
    """Black patent leather: a mirror glint on the toe and along the top line."""
    if f == "down":
        return PATENT[0]
    if f == "up":
        fz = y / max(1, h - 1)
        if fz < 0.3 and abs(x - (w - 1) / 2) < 1.2:
            return PATENT[5] if fz < 0.15 else PATENT[4]
        return PATENT[3]
    if f == "north":
        return PATENT[5] if y == 0 and abs(x - (w - 1) / 2) < 1.0 else PATENT[2]
    return PATENT[4] if y == 0 else PATENT[2]


def glow_none(f, x, y, w, h, c):
    return None


GLOW = H.glow_faint(0.05)


# =====================================================================================================
# The face (north face of the head, 15 x 15 texels; x = 0 is her right)
# =====================================================================================================

FACE = [
    "hhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhh",
    "hhhssssssssshhh",
    "hsssssssssssssh",
    "scbBbsssssbBbcs",
    "sKKKKsssssKKKKs",
    "sWIEwssssswEIWs",
    "suuuusssssuuuus",
    "sssssssnsssssss",
    "sssssssnsssssss",
    "ssssssNnNssssss",
    "ssssslMMMlsssss",
    "jsssssLLLsssssj",
    "jjsssssssssssjj",
]
assert all(len(r) == 15 for r in FACE), [len(r) for r in FACE]


# =====================================================================================================
# The rig
# =====================================================================================================

def skeleton(m):
    b = NB
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, b.hip, 0), "root")
    m.bone("head", (0, b.neck, 0), "body")
    m.bone("hair", (0, b.neck, 0), "head")
    m.bone("bun", (0, b.neck + 3.9, 4.0), "hair")
    for side, s in SIDES:
        m.bone(f"earring_{side[0]}", (3.85 * s, b.neck + 2.4, -0.1), "head")
    m.bone("death_light", (0, b.neck + 3.0, -3.8), "head")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (b.sx * s, b.sy, 0), "body")
        m.bone(f"{side}_forearm", (b.ax * s, b.elbow, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (b.ax * s, b.wrist, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (b.ax * s, b.palm, 0.35), f"{side}_hand")
        m.bone(f"{side}_middle", (b.ax * s, b.palm, -0.35), f"{side}_hand")
        m.bone(f"{side}_index", (b.ax * s, b.palm, -1.0), f"{side}_hand")
        m.bone(f"{side}_thumb", (b.ax * s - 0.9 * s, b.palm + 1.0, -1.2), f"{side}_hand")
        m.bone(f"palm_light_{side[0]}", (b.ax * s, b.palm + 0.8, 0), f"{side}_hand")
    m.bone("drill", (-b.ax, b.palm - 0.4, -0.2), "right_hand")
    m.bone("drill_bit", (-b.ax, b.palm + 1.6, -6.8), "drill")
    m.bone("drill_trigger", (-b.ax, b.palm + 0.4, -1.3), "drill")
    m.bone("jacket_hem_r", (-1.8, b.hip, -1.9), "body")
    m.bone("jacket_hem_l", (1.8, b.hip, -1.9), "body")
    m.bone("jacket_hem_back", (0, b.hip, 1.9), "body")
    m.bone("skirt_front", (0, b.hip, -1.7), "body")
    m.bone("skirt_back", (0, b.hip, 1.7), "body")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (b.lx * s, b.hip, 0), "root")
        m.bone(f"{side}_shin", (b.lx * s, b.knee, 0), f"{side}_leg")
        m.bone(f"{side}_foot", (b.lx * s, b.shoe_h, 0.6), f"{side}_shin")
    # The lab coat: its own copy of the torso and arms (same pivots), and the skirt hanging from its body.
    m.bone("coat", (0, 0, 0), "root")
    for limb in LIMBS:
        piv = m.rig.get(limb).pivot
        par = PARENT[limb]
        m.bone(f"coat_{limb}", piv, f"coat_{par}" if par else "coat")
    m.bone("coat_collar", (0, b.neck - 0.4, 0.6), "coat_body")
    m.bone("coat_front_r", (-2.0, b.hip, -2.3), "coat_body")
    m.bone("coat_front_l", (2.0, b.hip, -2.3), "coat_body")
    m.bone("coat_side_r", (-NB.tw - 0.6, b.hip, 0), "coat_body")
    m.bone("coat_side_l", (NB.tw + 0.6, b.hip, 0), "coat_body")
    m.bone("coat_back", (0, b.hip, 2.4), "coat_body")
    m.bone("badge", (-2.2, 20.6, -2.9), "coat_body")


def rig():
    m = Model("naomi", 512, 512)
    skeleton(m)
    build_head(m)
    build_body(m)
    build_arms(m)
    build_legs(m)
    build_coat(m)
    build_drill(m)
    build_lights(m)
    return m


# --- head ---------------------------------------------------------------------------------------------------------------

def build_head(m):
    b = NB
    y0 = b.neck
    sk = skin_m(18101)
    hr = hair_m(18102)

    def eye_glow(f, x, y, w, h, c):
        if c == IRIS:
            return shade(hexc("#cfe8ff"), 0.3)        # an angel's eyes: a faint light behind them
        return shade(c, 0.05)
    legend = {"h": hr, "s": sk, "c": mix(HAIR[3], SKIN[3], 0.55), "b": mix(HAIR[3], SKIN[2], 0.3), "B": HAIR[2], "K": LASH, "W": EYE_WHITE, "w": mix(EYE_WHITE, SKIN[3], 0.4),
              "I": IRIS, "E": PUPIL, "u": mix(SKIN[2], SKIN[3], 0.5), "n": SKIN[4], "N": SKIN[1], "C": mix(SKIN[3], LIPS[4], 0.25),
              "l": LIPS[2], "M": LIPS[3], "L": LIPS[4], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}

    def sides(f, x, y, w, h):
        if f == "up":
            return hr(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        if f == "south":
            return hr(f, x, y, w, h) if y < h - 3 else (sk(f, x, y, w, h) if y > h - 2 else shade(hr(f, x, y, w, h), 0.85))
        fx = x if f == "west" else w - 1 - x          # 0 at the front
        # Hair over the top rows, swept back over the temple, covering the back half; the ear clear below the sweep.
        if y < 3 or (fx > 9 and y < h - 3) or (fx > 4 and y < 5):
            return hr(f, x, y, w, h)
        if 6 <= fx <= 8 and 6 <= y <= 9:
            c = sk(f, x, y, w, h)
            return shade(c, 0.82) if fx == 7 and 7 <= y <= 8 else shade(c, 1.04)
        return sk(f, x, y, w, h)

    def face(f, x, y, w, h):
        if f == "north":
            ch = FACE[y][x] if y < len(FACE) and x < len(FACE[y]) else "s"
            v = legend.get(ch, legend["s"])
            return v(f, x, y, w, h) if callable(v) else v
        return sides(f, x, y, w, h)
    m.cube("head", (-HS / 2, y0, -HS / 2), (HS, HS, HS), face, eye_glow, density=2, tag="head")
    # The nose (fine, straight), cheekbones catching the light, the ears close to her head.
    m.cube("head", (-0.5, y0 + 2.4, -HS / 2 - 0.45), (1.0, 1.6, 0.5), lambda f, x, y, w, h: SKIN[5] if f == "north" and y < 2 else
           (SKIN[1] if f == "down" else SKIN[3]), GLOW, density=2, tag="nose")
    for s in (-1, 1):
        m.cube("head", (HS / 2 * s - 0.2 + (0 if s > 0 else -0.25), y0 + 2.6, -0.4), (0.45, 2.0, 1.4),
               lambda f, x, y, w, h: SKIN[2] if f not in ("east", "west") else SKIN[3], GLOW, density=2, tag="ear")
    m.cube("body", (-1.3, y0 - 0.8, -1.3), (2.6, 1.2, 2.6), sk, GLOW, density=2, tag="neck")
    # The hair: a sleek shell over the crown, drawn hard back, thicker at the back of the head where it gathers.
    hair = "hair"

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if y < 2 + (fx > 4) * 2 + (fx > 9) * 3 else None
        return hr(f, x, y, w, h) if f == "up" or y < h - 1 else None
    m.cube(hair, (-HS / 2, y0 + HS - 2.5, -HS / 2), (HS, 2.5, HS), shell, GLOW, inflate=0.28, density=2, tag="hair_shell")
    # Swept up and back from the forehead: a smooth roll of hair over the hairline, parted a little to her left.
    def roll(f, x, y, w, h):
        if f == "down":
            return None
        c = hr(f, x, y, w, h)
        if f == "up" and x == int(w * 0.62):
            return HAIR[1]                                  # the parting
        if f == "north":
            return shade(c, 1.1 if y == 0 else 0.95)
        return c
    m.cube(hair, (-HS / 2 + 0.25, y0 + HS - 0.35, -HS / 2 - 0.3), (HS - 0.5, 0.6, 3.4), roll, GLOW, density=2, tag="hair_roll",
           rotation=(-12, 0, 0), pivot=(0, y0 + HS - 0.6, -HS / 2 + 3.0))
    # The hair gathered up the back of the head into the bun.
    m.cube(hair, (-HS / 2, y0 + 1.6, HS / 2 - 1.0), (HS, 4.4, 1.0), none_on(("north", "down"), hr), GLOW, inflate=0.3, density=2,
           tag="hair_back")
    # The bun: a tight coil low on the back of her head (a core with two turned layers rounding it), two dark pins through it.
    bun = "bun"
    bx, by, bz = 0.0, y0 + 3.9, HS / 2 + 0.25

    def coil(f, x, y, w, h):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        a = math.atan2(y - cy, x - cx)
        r = math.hypot((x - cx) / max(1.0, w / 2), (y - cy) / max(1.0, h / 2))
        v = 2.5 + 0.75 * math.cos(r * 9.0 + a) + (0.6 if f == "up" else 0) - (0.9 if f == "down" else 0)
        if f in ("north", "south") and r > 0.92:
            v -= 0.8
        return tone(HAIR, v + 0.4 * (1 - r))
    m.cube(bun, (bx - 1.5, by - 1.3, bz), (3.0, 2.6, 1.8), coil, GLOW, density=2, tag="bun")
    m.cube(bun, (bx - 1.1, by - 1.6, bz + 0.2), (2.2, 3.2, 1.4), coil, GLOW, density=2, tag="bun_round")
    m.cube(bun, (bx - 1.8, by - 1.0, bz + 0.2), (3.6, 2.0, 1.4), coil, GLOW, density=2, tag="bun_round")
    m.cube(bun, (bx - 1.0, by - 1.0, bz + 1.4), (2.0, 2.0, 0.6), coil, GLOW, density=2, tag="bun_crown", rotation=(0, 0, 45),
           pivot=(bx, by, bz + 1.4))
    for i, (dx, dy, rz) in enumerate(((-0.3, 0.5, 28), (0.4, -0.2, -38))):
        m.cube(bun, (bx + dx - 2.1, by + dy - 0.12, bz + 1.0), (4.2, 0.25, 0.25), lambda f, x, y, w, h: HAIR[0] if x % 5 else STEEL[2],
               GLOW, density=4, tag="hair_pin", rotation=(0, 0, rz), pivot=(bx + dx, by + dy, bz + 1.1))
    # A pearl at each ear, on a short drop.
    for side, s in SIDES:
        e = f"earring_{side[0]}"
        ex = (HS / 2 + 0.2) * s
        m.cube(e, (ex - 0.1, y0 + 2.2, -0.15), (0.2, 0.3, 0.2), solid(GOLD[3]), H.glow_faint(0.2), density=4, tag="earring_hook")
        m.cube(e, (ex - 0.25, y0 + 1.7, -0.3), (0.5, 0.5, 0.5), lambda f, x, y, w, h: PEARL[5] if f == "up" or (x == 0 and y == 0) else
               PEARL[3], H.glow_faint(0.15), density=4, tag="pearl")


# --- torso: blouse, jacket ---------------------------------------------------------------------------------------------

def jacket_shell(mat, v_depth, gap_top, closed_from, buttons_at=(), lapel=None):
    """The jacket over the chest: a V down `v_depth` rows over the blouse, closed below `closed_from`."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f == "up":
            return None if abs(x - mid) <= gap_top + 0.5 and y < h * 0.7 else mat(f, x, y, w, h)
        if f == "north":
            if y < v_depth:
                half = gap_top * (1 - y / v_depth) + 0.4
                if abs(x - mid) <= half:
                    return None
                if lapel is not None and abs(x - mid) <= half + 1.6:
                    return lapel
            if y >= closed_from and abs(x - mid) < 0.6:
                if y in buttons_at:
                    return SUIT[0]
                return shade(mat(f, x, y, w, h), 0.78)
        return mat(f, x, y, w, h)
    return m


V_TOP, V_ROWS = 3.6, 11     # the jacket's V: half-width at the collar (texels), its depth (rows)


def v_half(y):
    """Half-width of the jacket's V (px) at height y."""
    rows = (NB.neck - y) * 2
    if rows >= V_ROWS:
        return -1.0
    return (V_TOP * (1 - rows / V_ROWS) + 0.4) / 2


def build_body(m):
    b = NB
    blouse = blouse_m(18201)
    suit = suit_m(18202)
    # The blouse (shows in the jacket's V), chest and waist.
    m.cube("body", (-b.tw, WAIST, -2.1), (2 * b.tw, b.neck - WAIST, 4.2), lambda f, x, y, w, h: blouse(f, x, y, w, h) if f == "north"
           else SUIT[1], GLOW, density=2, tag="chest")
    m.cube("body", (-TWW, b.hip, -1.8), (2 * TWW, WAIST - b.hip, 3.6), solid(SUIT[1]), GLOW, density=2, tag="waist")
    # A soft bust under the jacket: the chest a little fuller at the front, the jacket's V running on down it.
    def bust(f, x, y, w, h):
        if f == "south":
            return None
        if f == "up":
            return SUIT[4]
        c = tone(SUIT, 3.5 - 1.0 * y / max(1, h))
        if f == "north":
            half = v_half(20.9 - (y + 0.5) / 2)
            d = abs((x + 0.5) / 2 - w / 4)
            if d <= half:
                return blouse(f, x, y, w, h)
            if d <= half + 0.8:
                return SUIT[5] if d <= half + 0.3 else SUIT[4]
        return c
    m.cube("body", (-2.9, 18.3, -2.65), (5.8, 2.6, 0.7), bust, GLOW, inflate=0.2, density=2, tag="bust", rotation=(-8, 0, 0),
           pivot=(0, 20.9, -2.2))
    # The jacket: fitted over the chest (a V to the button), closed and nipped at the waist, flaring over the hips.
    m.cube("body", (-b.tw, WAIST, -2.1), (2 * b.tw, b.neck - WAIST, 4.2),
           jacket_shell(suit, V_ROWS, V_TOP, 99, lapel=SUIT[4]), GLOW, inflate=0.3, density=2, tag="jacket_chest")
    m.cube("body", (-TWW, b.hip - 0.2, -1.8), (2 * TWW, WAIST - b.hip + 0.3, 3.6),
           jacket_shell(suit_m(18203), 0, 0, 0, buttons_at=(6,)), GLOW, inflate=0.3, density=2, tag="jacket_waist")
    # Darts at the waist: the seams that shape it.
    for s in (-1, 1):
        m.cube("body", (1.6 * s - 0.1, b.hip + 0.6, -2.12), (0.2, WAIST - b.hip, 0.1), solid(SUIT[1]), GLOW, density=4, tag="dart")
    # The blouse collar spread over the lapels, a fine pleat down its front.
    for s in (-1, 1):
        m.cube("body", (1.0 * s - 0.8, b.neck - 1.5, -2.6), (1.6, 1.3, 0.35), lambda f, x, y, w, h: BLOUSE[5] if y == 0 else BLOUSE[4],
               GLOW, density=2, tag="blouse_collar", rotation=(16, 0, -34 * s), pivot=(1.0 * s, b.neck - 0.2, -2.4))
    m.cube("body", (-0.2, 19.6, -2.45), (0.4, 3.4, 0.1), lambda f, x, y, w, h: BLOUSE[2] if y % 3 else PEARL[4], GLOW, density=4,
           tag="blouse_placket")
    # Notch lapels laid back over the chest; shoulder line squared a little.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return SUIT[1]
            c = tone(SUIT, 4.6 - 1.2 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = SUIT[2]
            return c
        m.cube("body", (2.0 * s - 0.8, 17.6, -3.05), (1.6, 5.4, 0.3), lapel, GLOW, density=2, tag="lapel", rotation=(-6, 0, 16 * s),
               pivot=(1.2 * s, 17.6, -2.9))
        m.cube("body", (2.7 * s - 0.6, 22.3, -2.66), (1.2, 0.9, 0.35), solid(SUIT[4]), GLOW, density=2, tag="lapel_notch",
               rotation=(0, 0, -35 * s), pivot=(2.7 * s, 22.3, -2.5))
        m.cube("body", (s * (b.tw + 0.15) - 0.85, b.neck - 1.0, -2.25), (1.7, 0.7, 4.5),
               lambda f, x, y, w, h: tone(SUIT, 3.9 if f == "up" else 2.8), GLOW, density=2, tag="shoulder")
        # Welt pockets low on the jacket.
        m.cube("body", (1.8 * s - 0.9, 12.6, -2.12), (1.8, 0.3, 0.2), lambda f, x, y, w, h: SUIT[4] if y == 0 else SUIT[1], GLOW,
               density=4, tag="welt")
    # The jacket's short peplum over the hips, in three pieces that swing with her legs.
    L = 1.6
    for bone, s in (("jacket_hem_r", -1), ("jacket_hem_l", 1)):
        def fr(f, x, y, w, h, s=s):
            if f in ("up",) or f == ("west" if s < 0 else "east") or f == "south":
                return None
            if f == "north" and y >= h - 2 and ((s < 0 and x >= w - 2) or (s > 0 and x <= 1)):
                return None                       # cut away at the front
            return suit(f, x, y + 12, w, h + 12)
        m.cube(bone, ((0.0 if s > 0 else -TWW - 0.3), b.hip - L, -1.9), (TWW + 0.3, L, 3.8), fr, GLOW, inflate=0.32, density=2,
               tag="jacket_hem")
    m.cube("jacket_hem_back", (-TWW - 0.3, b.hip - L, 1.6), (2 * TWW + 0.6, L, 0.3),
           lambda f, x, y, w, h: None if f in ("up", "east", "west") else (SUIT[1] if f == "north" else suit(f, x, y + 12, w, h + 12)),
           GLOW, inflate=0.32, density=2, tag="jacket_hem")
    # The pencil skirt between the legs (front and back panels close the gap when she steps).
    sk = suit_m(18204, 2.8)
    for bone, z, face in (("skirt_front", -1.85, "north"), ("skirt_back", 1.55, "south")):
        m.cube(bone, (-1.4, b.knee - 0.6, z), (2.8, b.hip - b.knee + 0.4, 0.3),
               lambda f, x, y, w, h, face=face: sk(f, x, y, w, h) if f == face else (SUIT[1] if f not in ("up", "down") else None),
               GLOW, density=2, tag="skirt_panel")


def build_arms(m):
    b = NB
    suit = suit_m(18301)
    blouse = blouse_m(18302)
    hand = H.skin_mat(SKIN, 18303, 3.3, 0.3)
    nail = mix(SKIN[4], LIPS[4], 0.35)
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"{side}_arm", (cx - hw, b.elbow, -hw), (b.aw, b.sy + 0.6 - b.elbow, b.aw), suit, GLOW, inflate=0.15, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.35, b.wrist + 0.4, -1.35), (2.7, b.elbow - b.wrist - 0.4, 2.7), none_on(("up",), suit), GLOW,
               inflate=0.15, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.25, b.wrist - 0.05, -1.25), (2.5, 0.55, 2.5), none_on(("up",), blouse), GLOW, inflate=0.1,
               density=2, tag="blouse_cuff")
        # A slender hand: the palm, three fingers (index, middle, the last two together), the thumb.
        m.cube(f"{side}_hand", (cx - 1.05, b.palm, -1.25), (2.1, b.wrist - b.palm, 2.5), hand, GLOW, density=2, tag="palm")
        fing = lambda f, x, y, w, h: nail if (f == "down" or (f != "up" and y == h - 1)) else shade(hand(f, x, y, w, h), 0.94 if x % 2 else 1.0)
        m.cube(f"{side}_index", (cx - 1.0, b.palm - 1.5, -1.25), (2.0, 1.5, 0.6), fing, GLOW, density=2, tag="index")
        m.cube(f"{side}_middle", (cx - 1.0, b.palm - 1.6, -0.6), (2.0, 1.6, 0.6), fing, GLOW, density=2, tag="middle")
        m.cube(f"{side}_fingers", (cx - 1.0, b.palm - 1.35, 0.05), (2.0, 1.35, 1.15), fing, GLOW, density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 0.9 * s - 0.4, b.palm + 0.1, -1.8), (0.8, 1.4, 0.75), hand, GLOW, density=2, tag="thumb")
    # A slim watch on her left wrist (she keeps time).
    m.cube("left_hand", (b.ax - 1.2, b.wrist - 0.55, -1.35), (2.4, 0.45, 2.7), lambda f, x, y, w, h: STEEL[4] if f == "east" else STEEL[2],
           GLOW, inflate=0.06, density=4, tag="watch")


def build_legs(m):
    b = NB
    for side, s in SIDES:
        sk = suit_m(18401 + (s > 0), 2.9)
        x0 = b.lx * s - b.lw / 2
        # The pencil skirt on the thigh (to just below the knee), stockings below.
        m.cube(f"{side}_leg", (x0, b.knee - 0.7, -b.lw / 2), (b.lw, b.hip - b.knee + 0.7, b.lw),
               lambda f, x, y, w, h, sk=sk: (SUIT[0] if (f != "up" and y >= h - 1) else sk(f, x, y, w, h)), GLOW, inflate=0.22,
               density=2, tag="skirt")
        st = stocking_m(18410 + (s > 0))
        m.cube(f"{side}_shin", (b.lx * s - 1.2, b.shoe_h + 0.2, -1.2), (2.4, b.knee - b.shoe_h - 0.2, 2.4), st, GLOW, density=2,
               tag="calf")
        # The calf rounds out at the back.
        m.cube(f"{side}_shin", (b.lx * s - 1.0, 3.0, 0.4), (2.0, 2.4, 1.1), st, GLOW, density=2, tag="calf_back")
        # The pump: a pointed toe, the vamp, a slim heel under the back.
        foot = f"{side}_foot"
        fx = b.lx * s
        m.cube(foot, (fx - 1.2, HEEL * 0.35, -3.6), (2.4, 0.75, 2.6), pump_m, lambda f, x, y, w, h, c: shade(c, 0.1), density=2,
               tag="toe", rotation=(-8, 0, 0), pivot=(fx, HEEL, -1.0))
        m.cube(foot, (fx - 0.55, 0.0, -4.2), (1.1, 0.6, 0.8), pump_m, lambda f, x, y, w, h, c: shade(c, 0.1), density=4, tag="toe_point")
        m.cube(foot, (fx - 1.25, HEEL * 0.6, -1.4), (2.5, 0.9, 2.6), pump_m, lambda f, x, y, w, h, c: shade(c, 0.1), density=2,
               tag="vamp")
        m.cube(foot, (fx - 0.5, 0.0, 0.6), (1.0, HEEL + 0.1, 0.8), pump_m, lambda f, x, y, w, h, c: shade(c, 0.1), density=4, tag="heel")


# --- the lab coat (its own group) ---------------------------------------------------------------------------------------

def build_coat(m):
    b = NB
    lab = lab_m(18501)

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f == "up":
            return None if abs(x - mid) <= 3.0 and y < h * 0.75 else lab(f, x, y, w, h)
        if f == "north":
            # Worn open: the opening widens from the collar down, the front edges catching the light.
            half = 2.4 + 0.9 * y / max(1, h)
            if abs(x - mid) <= half:
                return None
            if abs(x - mid) <= half + 1.0:
                return LAB[5]
        return lab(f, x, y, w, h)
    m.cube("coat_body", (-b.tw, b.hip - 0.3, -2.1), (2 * b.tw, b.neck - b.hip + 0.1, 4.2), shell, GLOW, inflate=0.75, density=2,
           tag="coat")
    # Broad notch lapels, the collar standing round the back of the neck.
    for s in (-1, 1):
        def lp(f, x, y, w, h, s=s):
            if f == "north" and y < 2 and (x >= w - 1 if s < 0 else x == 0):
                return None
            return tone(LAB, 4.6 - 0.9 * y / max(1, h)) if f == "north" else LAB[2]
        m.cube("coat_body", (2.7 * s - 1.05, 16.0, -3.05), (2.1, 6.6, 0.4), lp, GLOW, density=2, tag="coat_lapel",
               rotation=(4, 0, 10 * s), pivot=(2.0 * s, 16.0, -2.9))

    def collar(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 3.0 and (f == "north" or y < h * 0.6):
            return None
        return tone(LAB, 4.2 - 0.8 * y / max(1, h))
    m.cube("coat_collar", (-b.tw, b.neck - 1.5, -2.1), (2 * b.tw, 1.9, 4.2), collar, GLOW, inflate=1.0, density=2, tag="coat_collar")
    # A breast pocket with two pens; the badge clipped to it.
    m.cube("coat_body", (1.0, 18.8, -2.92), (2.2, 2.0, 0.25), lambda f, x, y, w, h: LAB[2] if y == 0 else LAB[4], GLOW, density=2,
           tag="coat_pocket")
    for i, col in enumerate((hexc("#22324f"), STEEL[4])):
        m.cube("coat_body", (1.35 + i * 0.8, 20.4, -2.97), (0.35, 1.3, 0.35), lambda f, x, y, w, h, col=col: col if y else STEEL[5],
               GLOW, density=4, tag="pen")

    def badge(f, x, y, w, h):
        if f != "north":
            return PLASTIC[2]
        if y == 0:
            return GOLD[3]
        if 2 <= y <= 5 and 1 <= x <= 3:
            return mix(SKIN[3], HAIR[3], 0.4 if y < 3 else 0.0)    # her photograph
        if y in (2, 4) and 5 <= x <= w - 2 and (x + y) % 3:
            return SUIT[2]
        if y == h - 2 and 1 <= x <= w - 2:
            return GOLD[4] if x % 2 else GOLD[2]                  # the gilt sigil strip
        return PLASTIC[5]
    m.cube("badge", (-3.2, 18.4, -3.0), (2.0, 2.4, 0.15), badge, lambda f, x, y, w, h, c: shade(c, 0.4) if c in (GOLD[4], GOLD[3]) else None,
           density=4, tag="badge")
    m.cube("badge", (-2.45, 20.6, -3.0), (0.5, 0.5, 0.2), solid(STEEL[4]), GLOW, density=4, tag="badge_clip")
    # The skirt of the coat to the knee: two fronts hanging open, the sides, the back with a vent.
    L = b.hip + 0.3 - 4.4
    for bone, s in (("coat_front_r", -1), ("coat_front_l", 1)):
        def fr(f, x, y, w, h, s=s):
            if f == "south":
                return LAB[2]
            if f == "up":
                return None
            c = lab(f, x, y, w, h)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return LAB[5]
            if f == "north" and 3 <= y <= 4 and 1 <= (w - 1 - x if s < 0 else x) <= w - 2:
                return LAB[2] if y == 3 else LAB[3]            # the patch pocket's mouth
            return c
        xa, xb = (-b.tw - 0.9, -0.9) if s < 0 else (0.9, b.tw + 0.9)
        m.cube(bone, (xa, 4.4, -2.75), (xb - xa, L, 0.4), fr, GLOW, density=2, tag="coat_skirt", rotation=(-3, 0, 0),
               pivot=(0, b.hip, -2.5))
        for yb in (9.6, 6.8):
            # Buttons down her left front, the buttonholes on the right.
            xb = 1.35 if s > 0 else -1.45
            m.cube(bone, (xb - 0.3, yb, -2.95), (0.6, 0.6, 0.2), solid(LAB[1]) if s > 0 else solid(LAB[3]), GLOW, density=4,
                   tag="coat_button", rotation=(-3, 0, 0), pivot=(0, b.hip, -2.5))
    for bone, s in (("coat_side_r", -1), ("coat_side_l", 1)):
        xs = -b.tw - 0.95 if s < 0 else b.tw + 0.55
        m.cube(bone, (xs, 4.4, -2.5), (0.4, L, 5.0), none_on(("up",), lab), GLOW, density=2, tag="coat_side", rotation=(0, 0, 3 * s),
               pivot=(xs, b.hip, 0))

    def bk(f, x, y, w, h):
        if f == "north":
            return LAB[2]
        if f == "up":
            return None
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.55:
            return LAB[1]                                       # the vent
        if f == "south" and y == 1 and 3 <= x <= w - 4:
            return LAB[3]                                       # the half-belt
        return lab(f, x, y, w, h)
    m.cube("coat_back", (-b.tw - 0.9, 4.4, 2.4), (2 * b.tw + 1.8, L, 0.4), bk, GLOW, density=2, tag="coat_back", rotation=(4, 0, 0),
           pivot=(0, b.hip, 2.4))
    # The coat's sleeves, a little long, the cuff turned.
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"coat_{side}_arm", (cx - hw, b.elbow, -hw), (b.aw, b.sy + 0.7 - b.elbow, b.aw), none_on(("down",), lab), GLOW,
               inflate=0.5, density=2, tag="coat_sleeve")
        m.cube(f"coat_{side}_forearm", (cx - 1.35, b.wrist + 0.7, -1.35), (2.7, b.elbow - b.wrist - 0.7, 2.7), none_on(("up", "down"), lab),
               GLOW, inflate=0.48, density=2, tag="coat_sleeve")
        m.cube(f"coat_{side}_forearm", (cx - 1.35, b.wrist + 0.1, -1.35), (2.7, 0.7, 2.7),
               none_on(("up",), lambda f, x, y, w, h: LAB[5] if y == 0 else LAB[4]), GLOW, inflate=0.55, density=2, tag="coat_cuff")
        # The shoulder seam.
        m.cube("coat_body", (s * (b.tw + 0.2) - 1.0, b.neck - 1.2, -2.4), (2.0, 0.8, 4.8),
               lambda f, x, y, w, h: tone(LAB, 4.6 if f == "up" else 3.4), GLOW, inflate=0.2, density=2, tag="coat_shoulder")


# --- the drill ----------------------------------------------------------------------------------------------------------

def build_drill(m):
    """A cordless surgical bone drill, held like a pistol: the grip in her fist, the body over her knuckles running forward,
    a steel nose and chuck, a long fluted bit. White and pale grey, steel, a cyan status light."""
    b = NB
    cx = -b.ax
    y0 = b.palm - 0.4         # bottom of the fist
    d = "drill"

    def body_m(f, x, y, w, h):
        if f == "up":
            return PLASTIC[5]
        if f == "down":
            return PLASTIC[1]
        v = 4.2 - 1.4 * y / max(1, h) - (0.4 if f in ("east", "west") else 0)
        c = tone(PLASTIC, v)
        if f in ("east", "west") and y == h // 2 and 2 <= x <= w - 3:
            return PLASTIC[1]                                     # the seam of the housing
        if f in ("east", "west") and 1 <= y <= 2 and 1 <= x <= 4 and (x + y) % 2 == 0:
            return PLASTIC[2]                                     # vent slots at the back
        return c
    # The grip, through her fist; the battery pack below it.
    m.cube(d, (cx - 0.7, y0 - 1.0, -1.0), (1.4, 3.2, 1.8), lambda f, x, y, w, h: PLASTIC[2 + (y % 2)] if f in ("north", "south") else
           tone(PLASTIC, 3.2 - y * 0.2), GLOW, density=4, tag="drill_grip", rotation=(-10, 0, 0), pivot=(cx, y0 + 0.8, -0.2))
    m.cube(d, (cx - 0.95, y0 - 2.1, -1.6), (1.9, 1.2, 2.8), lambda f, x, y, w, h: SUIT[3] if y else SUIT[4], GLOW, density=4,
           tag="drill_battery")
    m.cube(d, (cx - 0.95, y0 - 1.0, -1.6), (1.9, 0.2, 2.8), solid(CYAN[2]), lambda f, x, y, w, h, c: c, density=4, tag="drill_band")
    # The body over the knuckles, running forward.
    m.cube(d, (cx - 0.9, y0 + 2.0, -3.4), (1.8, 1.9, 5.0), body_m, GLOW, density=4, tag="drill_body")
    m.cube(d, (cx - 0.75, y0 + 3.9, -1.8), (1.5, 0.35, 2.6), solid(PLASTIC[4]), GLOW, density=4, tag="drill_spine")
    # The status light on its back.
    m.cube(d, (cx - 0.45, y0 + 2.6, 1.6), (0.9, 0.6, 0.1), solid(CYAN[4]), lambda f, x, y, w, h, c: c, density=4, tag="drill_light")
    # The nose and the chuck.
    m.cube(d, (cx - 0.65, y0 + 2.3, -4.6), (1.3, 1.3, 1.2), lambda f, x, y, w, h: STEEL[4] if f != "down" else STEEL[2], GLOW,
           density=4, tag="drill_nose")
    m.cube(d, (cx - 0.45, y0 + 2.5, -6.4), (0.9, 0.9, 1.8), lambda f, x, y, w, h: STEEL[3 + (x % 2)] if f in ("up", "down", "east", "west")
           else STEEL[4], GLOW, density=4, tag="drill_chuck")
    # The trigger (its own bone, pressed in the clips), the bit.
    m.cube("drill_trigger", (cx - 0.3, y0 + 0.8, -1.7), (0.6, 1.1, 0.5), solid(STEEL[3]), GLOW, density=4, tag="drill_trigger")

    def bit_m(f, x, y, w, h):
        if f in ("north", "south"):
            return STEEL[5]
        return STEEL[5] if (x + y) % 3 == 0 else STEEL[3] if (x + y) % 3 == 1 else STEEL[2]
    m.cube("drill_bit", (cx - 0.2, y0 + 2.75, -10.8), (0.4, 0.4, 4.4), bit_m, lambda f, x, y, w, h, c: shade(c, 0.15), density=4,
           tag="drill_bit")
    m.cube("drill_bit", (cx - 0.1, y0 + 2.85, -11.5), (0.2, 0.2, 0.7), solid(STEEL[5]), lambda f, x, y, w, h, c: shade(c, 0.3),
           density=4, tag="drill_tip")


# --- light ----------------------------------------------------------------------------------------------------------------

def build_lights(m):
    b = NB
    full = lambda f, x, y, w, h, c: (c[0], c[1], c[2], 255)

    def disc(seed):
        def mat(f, x, y, w, h):
            r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / (w / 2)
            if r > 1.0:
                return None
            if r < 0.35:
                return WHITE
            ray = (math.atan2(y - (h - 1) / 2, x - (w - 1) / 2) * 8 / math.pi + seed) % 2 < 0.5
            c = LIGHT[4] if ray else LIGHT[2]
            return (c[0], c[1], c[2], int(240 - 120 * r))
        return mat
    for side, s in SIDES:
        cx = b.ax * s
        # The palm faces in towards her when the arm hangs.
        face = "west" if s < 0 else "east"
        xp = cx + 1.1 if s < 0 else cx - 1.1
        m.cube(f"palm_light_{side[0]}", (xp, b.palm - 0.9, -1.8), (0, 3.6, 3.6), disc(s), full, faces=(face,), density=4, tag="palm_light")
    # Her death: the grace pouring out of her eyes and her mouth -- a bright plane over each, and beams flaring forward.
    dl = "death_light"
    y0 = b.neck
    z = -HS / 2 - 0.08
    for x0 in (-2.6, 1.1):
        m.cube(dl, (x0 - 0.2, y0 + 3.6, z), (1.9, 1.0, 0), lambda f, x, y, w, h: WHITE, full, faces=("north",), density=2, tag="eye_light")
    m.cube(dl, (-0.9, y0 + 1.0, z), (1.8, 0.8, 0), lambda f, x, y, w, h: WHITE, full, faces=("north",), density=2, tag="mouth_light")

    def beam(f, x, y, w, h):
        # Along its length (x on the side faces, y on up/down) the beam fades and widens out.
        t = (w - 1 - x) / max(1, w - 1) if f in ("east", "west") else 1 - y / max(1, h - 1)
        if f in ("north", "south"):
            return None
        a = int(250 * (1 - t) ** 1.4)
        if a < 30:
            return None
        c = mix(WHITE, LIGHT[3], t)
        return (c[0], c[1], c[2], a)
    for cx, cy, wd, ht, tag in ((-1.65, y0 + 4.1, 1.6, 1.0, "eye_beam"), (2.05, y0 + 4.1, 1.6, 1.0, "eye_beam"),
                                (0.0, y0 + 1.4, 1.6, 0.8, "mouth_beam")):
        for rz in (0, 90):
            m.cube(dl, (cx - wd / 2, cy - ht / 2, z - 7.0), (wd, ht, 7.0), beam, full, density=2, tag=tag, rotation=(0, 0, rz),
                   pivot=(cx, cy, z))


# =====================================================================================================
# Animations
# =====================================================================================================

class Clip(MClip):
    """michael_art's pose clips, every key on a limb copied to the coat's copy of it."""

    @staticmethod
    def expand(pose):
        out = {}
        for k, v in pose.items():
            out[k] = v
            pre = k[0] if k[0] in "@%" else ""
            bn = k[len(pre):]
            if bn in LIMBS:
                out[f"{pre}coat_{bn}"] = v
        return out

    def key(self, t, pose, ease=None):
        return super().key(t, self.expand(follow(pose)), ease)

    def layer(self, name, fn):
        for k in self.expand({name: None}):
            super().layer(k, fn)
        return self


def follow(pose):
    """The skirts swing with her legs: the jacket's peplum, the skirt's panels, the coat's skirt (fronts follow their leg
    forward, the back trails, the panels between her legs take the average)."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])[0]
    for k, v in (("jacket_hem_r", [min(0.0, rl) * 0.5 - body * 0.3, 0, 0]), ("jacket_hem_l", [min(0.0, ll) * 0.5 - body * 0.3, 0, 0]),
                 ("jacket_hem_back", [-max(0.0, max(rl, ll)) * 0.3 - body * 0.3, 0, 0]),
                 ("skirt_front", [min(rl, ll) * 0.55 + max(rl, ll) * 0.15 - body * 0.6, 0, 0]),
                 ("skirt_back", [max(rl, ll) * 0.55 + min(rl, ll) * 0.15 - body * 0.6, 0, 0]),
                 ("coat_front_r", [min(0.0, rl) * 0.6 - body * 0.35, 0, 0]), ("coat_front_l", [min(0.0, ll) * 0.6 - body * 0.35, 0, 0]),
                 ("coat_back", [-max(0.0, max(rl, ll)) * 0.35 - min(0.0, min(rl, ll)) * 0.2 - body * 0.35, 0, 0]),
                 ("coat_side_r", [rl * 0.3 - body * 0.3, 0, 0]), ("coat_side_l", [ll * 0.3 - body * 0.3, 0, 0])):
        if k not in pose:
            out[k] = v
    return out


FIST_R = {"right_fingers": [-85, 0, 0], "right_middle": [-85, 0, 0], "right_index": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
GRIP_R = {"right_fingers": [-85, 0, 0], "right_middle": [-85, 0, 0], "right_index": [-35, 0, 0], "right_thumb": [-25, 0, 24]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_middle": [0, 0, 0], "right_index": [0, 0, 0], "right_thumb": [0, 0, -16]}
OPEN_L = {"left_fingers": [0, 0, 0], "left_middle": [0, 0, 0], "left_index": [0, 0, 0], "left_thumb": [0, 0, 16]}
SPREAD_R = {"right_fingers": [8, 0, 10], "right_middle": [4, 0, 0], "right_index": [4, 0, -10], "right_thumb": [0, 0, -30]}
SPREAD_L = {"left_fingers": [8, 0, -10], "left_middle": [4, 0, 0], "left_index": [4, 0, 10], "left_thumb": [0, 0, 30]}
POINT_L = {"left_fingers": [-90, 0, 0], "left_middle": [-90, 0, 0], "left_index": [0, 0, 0], "left_thumb": [-30, 0, -20]}
LOOSE = {"right_fingers": [-25, 0, 0], "right_middle": [-20, 0, 0], "right_index": [-14, 0, 0], "left_fingers": [-25, 0, 0],
         "left_middle": [-20, 0, 0], "left_index": [-14, 0, 0]}
GRIP_PT = (-NB.ax, NB.palm - 0.6, -0.3)       # rest-space point in the right hand where the drill is held


def kneel(head=10):
    return {"@root": [0, -5.0, 0], "left_leg": [-80, 0, -4], "left_shin": [80, 0, 0], "left_foot": [10, 0, 0], "right_leg": [4, 0, 5],
            "right_shin": [86, 0, 0], "right_foot": [-40, 0, 0], "body": [12, 0, 0], "head": [head, 0, 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def reach(pose, side, target, grip=None):
        s = -1 if side == "right" else 1
        return _reach(r, pose, side, target, grip or (NB.ax * s, NB.palm - 0.6, -0.3))

    def drill_aim(pose, d=(0, 0, -1)):
        """The drill turned in her fist so its bit points along world direction d."""
        return P_(pose, {"drill": aim(r, P_(pose, {"drill": [0, 0, 0]}), "drill", (0, 0, -1), d)})

    def spin(c, t0, t1, turns):
        c.layer("drill_bit", lambda t: [0, 0, 360.0 * turns * min(1.0, max(0.0, (t - t0) / (t1 - t0)))] if t > t0 else [0, 0, 0])

    # Composed: hands folded before her waist, weight on one leg, chin level.
    clasp = reach(P_(LOOSE, {"right_arm": [-10, 0, 8], "left_arm": [-10, 0, -8]}), "right", (-0.9, 13.6, -3.3))
    clasp = reach(clasp, "left", (0.9, 13.9, -3.1))
    clasp = P_(clasp, {"right_hand": [0, 30, 0], "left_hand": [0, -40, 0], "right_fingers": [-40, 0, 0], "right_middle": [-40, 0, 0],
                       "right_index": [-35, 0, 0], "left_fingers": [-30, 0, 0], "left_middle": [-30, 0, 0], "left_index": [-25, 0, 0]})
    STAND = P_(clasp, {"right_leg": [0, 0, 1.5], "left_leg": [0, 0, -1], "head": [-2, 0, 0]})

    # --- idle: still as a statue; a slow breath, the head turning to watch, one blink of the fingers. ---
    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        c.key(t, P_(STAND, {"body": [0.5 * abs(k), 0, 0], "@body": [0, -0.1 * abs(k), 0], "head": [-2 - 1 * abs(k), 9 * k, 1.5 * k]}),
              E_IO if t else None)

    # --- walk: a short, precise stride in the pencil skirt, heels, arms swinging little. ---
    L = 1.2
    c = clip("walk", L, loop=True)
    for i in range(9):
        t = L * i / 8
        w = walk_cycle(8, i, stride=17, arm=9, knee=24, bob=0.25, sway=2, lean=1.0)
        ph = 2 * math.pi * i / 8
        w["right_foot"] = [-14 * max(0, math.sin(ph + 0.6)), 0, 0]
        w["left_foot"] = [-14 * max(0, -math.sin(ph + 0.6)), 0, 0]
        w["body"] = [1.0, 4 * math.sin(ph), 0]
        c.key(t, P_(LOOSE, w), E_IO if i else None)

    # --- recalibrate (loop): at the console -- forearms out, fingers tapping in a run, head bowed to the screen. ---
    c = clip("recalibrate", 2.4, loop=True)
    desk = P_(LOOSE, {"body": [10, 0, 0], "head": [14, 0, 0], "right_leg": [2, 0, 0], "left_leg": [-4, 0, 0]})
    desk = reach(desk, "right", (-2.2, 15.2, -7.2))
    desk = reach(desk, "left", (2.2, 15.2, -7.2))
    desk = P_(desk, {"right_hand": [-30, 0, 0], "left_hand": [-30, 0, 0], "right_fingers": [-30, 0, 0], "right_middle": [-30, 0, 0],
                     "right_index": [-25, 0, 0], "left_fingers": [-30, 0, 0], "left_middle": [-30, 0, 0], "left_index": [-25, 0, 0]})
    for t, hd in ((0, [14, 0, 0]), (0.6, [12, -8, 0]), (1.2, [15, 4, 0]), (1.8, [12, 9, 0]), (2.4, [14, 0, 0])):
        c.key(t, P_(desk, {"head": hd}), E_IO if t else None)
    for n, (bone, ph) in enumerate((("right_index", 0.0), ("right_middle", 0.13), ("right_fingers", 0.31), ("left_index", 0.52),
                                    ("left_middle", 0.7), ("left_fingers", 0.88))):
        c.layer(bone, lambda t, ph=ph: [-22 * max(0.0, math.sin(2 * math.pi * (t / 0.4 + ph))) ** 3, 0, 0])
    c.layer("right_hand", lambda t: [0, 6 * math.sin(2 * math.pi * t / 1.2), 0])
    c.layer("left_hand", lambda t: [0, -5 * math.sin(2 * math.pi * t / 0.8 + 1), 0])

    # --- palm_strike: she steps in, the right hand drawn back, then laid flat on the forehead (hit at 1.0). ---
    c = clip("palm_strike", 1.8)
    draw = P_(OPEN_R, {"right_arm": [-40, 30, 30], "right_forearm": [-70, 0, 0], "right_hand": [20, 0, 0], "body": [-4, -14, 0],
                       "head": [2, 10, 0], "left_arm": [-8, 0, -10], "right_leg": [-12, 0, 0], "left_leg": [6, 0, 0]})
    strike = reach(P_(SPREAD_R, {"body": [10, 12, 0], "head": [-4, -8, 0], "left_arm": [12, 0, -14], "right_leg": [-26, 0, 0],
                                 "left_leg": [14, 0, 0], "right_shin": [10, 0, 0], "left_shin": [14, 0, 0], "@root": [0, -0.6, -2.0]}),
                   "right", (-1.2, 24.2, -9.6), grip=(-NB.ax, NB.palm + 0.4, 0))
    strike = P_(strike, {"right_hand": aim2(r, strike, "right_hand", (0, -1, 0), (0, 1, -0.25), (1, 0, 0), (0, 0, -1))})
    c.key(0, STAND)
    c.key(0.5, draw, E_OUT)
    c.key(0.9, P_(draw, {"right_arm": [-46, 34, 34], "body": [-6, -16, 0]}), E_IO)
    c.key(1.0, strike, E_SNAP)
    c.key(1.3, P_(strike, {"body": [12, 12, 0]}), E_OUT)
    c.key(1.8, STAND, E_IO)

    # --- restraint: her left hand rises, palm out, fingers spread -- "Be still." -- the field closes (1.0) and she holds it,
    # the hand trembling with the effort of it, to 3.0. ---
    c = clip("restraint", 3.5)
    hold = reach(P_(SPREAD_L, LOOSE, {"body": [0, 10, 0], "head": [-2, -6, 0], "right_arm": [-6, 0, 8], "right_leg": [-6, 0, 0],
                                      "left_leg": [4, 0, 0]}), "left", (3.0, 23.0, -10.5), grip=(NB.ax, NB.palm + 0.4, 0))
    hold = P_(hold, {"left_hand": aim(r, hold, "left_hand", (0, -1, 0), (0, 0.6, -1))})
    c.key(0, STAND)
    c.key(0.55, P_(hold, {"left_forearm": [hold["left_forearm"][0] - 20, 0, 0]}), E_OUT)
    c.key(1.0, P_(hold, {"head": [2, -6, 0]}), E_SNAP)
    c.key(3.0, P_(hold, {"head": [4, -6, 0], "body": [2, 10, 0]}), E_IO)
    c.key(3.5, STAND, E_IO)
    c.layer("left_hand", lambda t: [1.4 * math.sin(t * 61), 0, 1.1 * math.sin(t * 47)] if 1.0 < t < 3.0 else [0, 0, 0])
    c.layer("left_forearm", lambda t: [0.8 * math.sin(t * 53), 0, 0] if 1.0 < t < 3.0 else [0, 0, 0])

    # --- strap_in: the drill up in her right hand; her left forefinger points at the chair, then flicks down: "Sit." ---
    c = clip("strap_in", 1.7)
    armed = drill_aim(reach(P_(GRIP_R, LOOSE, {"right_arm": [-30, 0, 10]}), "right", (-5.0, 20.0, -5.0)), (0.1, 0.9, -0.4))
    armed = P_(armed, GRIP_R)
    point = reach(P_(armed, POINT_L, {"body": [0, 18, 0], "head": [-4, 14, 0]}), "left", (5.0, 21.0, -11.0), grip=(NB.ax, NB.palm - 1.4, -1.0))
    point = P_(point, {"left_hand": [-10, 0, 0]})
    flick = P_(point, {"left_hand": [30, 0, 0], "left_forearm": [point["left_forearm"][0] + 10, point["left_forearm"][1], 0],
                       "head": [4, 14, 0]})
    c.key(0, STAND)
    c.key(0.45, armed, E_OUT)
    c.key(0.85, point, E_IO)
    c.key(1.0, flick, E_SNAP)
    c.key(1.25, P_(flick, {"head": [2, 12, 0]}), E_OUT)
    c.key(1.7, STAND, E_IO)
    spin(c, 0.35, 1.5, 4)

    # --- drill_lance: drawn back at the hip, the drill level; a lunge (0.75) that drives it forward; back. ---
    c = clip("drill_lance", 1.4)
    back = reach(P_(GRIP_R, LOOSE, {"body": [4, -24, 0], "head": [-2, 18, 0], "left_arm": [-30, 0, -20], "left_forearm": [-30, 0, 0],
                                    "right_leg": [10, 0, 0], "left_leg": [-16, 0, 0], "left_shin": [16, 0, 0], "@root": [0, -0.6, 0.8]}),
                 "right", (-5.8, 15.5, 1.5), grip=GRIP_PT)
    back = drill_aim(back, (0, 0.05, -1))
    lunge = reach(P_(GRIP_R, LOOSE, {"body": [20, 8, 0], "head": [-14, -6, 0], "left_arm": [20, 0, -26], "right_leg": [-40, 0, 0],
                                     "right_shin": [34, 0, 0], "left_leg": [26, 0, 0], "left_shin": [20, 0, 0], "left_foot": [-20, 0, 0],
                                     "@root": [0, -1.6, -3.4]}),
                  "right", (-2.4, 18.0, -13.0), grip=GRIP_PT)
    lunge = drill_aim(lunge, (0.05, -0.1, -1))
    c.key(0, STAND)
    c.key(0.45, back, E_OUT)
    c.key(0.65, P_(back, {"body": [6, -28, 0]}), E_IO)
    c.key(0.75, lunge, E_SNAP)
    c.key(0.95, P_(lunge, {"body": [22, 8, 0]}), E_OUT)
    c.key(1.4, STAND, E_IO)
    spin(c, 0.3, 1.2, 9)
    c.layer("@drill_trigger", lambda t: [0, 0, 0.3] if 0.3 < t < 1.2 else [0, 0, 0])

    # --- wipe: both hands rise slowly before her, palms out and burning (to 1.4), then thrust apart -- the white flash at 1.5;
    # her head back, eyes closed to it. ---
    c = clip("wipe", 2.3)
    rise = reach(P_(SPREAD_R, SPREAD_L, {"body": [-2, 0, 0], "head": [-4, 0, 0]}), "right", (-4.2, 21.0, -9.0), grip=(-NB.ax, NB.palm + 0.4, 0))
    rise = reach(rise, "left", (4.2, 21.0, -9.0), grip=(NB.ax, NB.palm + 0.4, 0))
    rise = P_(rise, {"right_hand": aim2(r, rise, "right_hand", (0, -1, 0), (0, 1, -0.2), (1, 0, 0), (0, 0, -1)),
                     "left_hand": aim2(r, rise, "left_hand", (0, -1, 0), (0, 1, -0.2), (-1, 0, 0), (0, 0, -1))})
    flash = P_(SPREAD_R, SPREAD_L, {"right_arm": [-24, 0, 112], "left_arm": [-24, 0, -112], "right_forearm": [-10, 0, 0],
                                    "left_forearm": [-10, 0, 0], "right_hand": [0, -90, 0], "left_hand": [0, 90, 0], "body": [-10, 0, 0],
                                    "head": [-26, 0, 0], "@root": [0, 0.3, 0], "right_leg": [4, 0, 4], "left_leg": [4, 0, -4]})
    c.key(0, STAND)
    c.key(0.5, P_(rise, {"right_arm": [rise["right_arm"][0] + 30, rise["right_arm"][1], rise["right_arm"][2]],
                         "left_arm": [rise["left_arm"][0] + 30, rise["left_arm"][1], rise["left_arm"][2]]}), E_OUT)
    c.key(1.4, rise, E_IO)
    c.key(1.5, flash, E_SNAP)
    c.key(1.8, P_(flash, {"head": [-20, 0, 0]}), E_OUT)
    c.key(2.3, STAND, E_IO)
    c.layer("right_hand", lambda t: [0.9 * math.sin(t * 70), 0, 0] if 0.6 < t < 1.45 else [0, 0, 0])
    c.layer("left_hand", lambda t: [0.9 * math.sin(t * 66 + 1), 0, 0] if 0.6 < t < 1.45 else [0, 0, 0])

    # --- call_guards: the right hand up by her shoulder, a snap of the fingers (0.8); her eyes never leave you. ---
    c = clip("call_guards", 1.5)
    up = reach(P_(LOOSE, {"right_index": [-20, 0, 0], "right_middle": [-10, 0, 0], "right_fingers": [-80, 0, 0], "right_thumb": [-30, 0, 30],
                          "head": [0, -6, 0], "body": [0, 6, 0]}), "right", (-6.6, 23.6, -3.6), grip=(-NB.ax, NB.palm, -0.3))
    up = P_(up, {"right_hand": aim(r, up, "right_hand", (0, -1, 0), (0.3, 1, -0.2))})
    snapped = P_(up, {"right_middle": [-100, 0, 0], "right_index": [-30, 0, 0], "right_thumb": [10, 0, 0], "head": [3, -6, 0],
                      "right_hand": [up["right_hand"][0] + 12, up["right_hand"][1], up["right_hand"][2]]})
    c.key(0, STAND)
    c.key(0.5, up, E_OUT)
    c.key(0.75, P_(up, {"right_middle": [-20, 0, 0], "right_thumb": [-36, 0, 34]}), E_IO)
    c.key(0.8, snapped, E_SNAP)
    c.key(1.05, snapped, E_OUT)
    c.key(1.5, STAND, E_IO)

    # --- test: she turns a little aside and presents the room with an open left hand -- "Begin." (1.0) -- then folds her hands
    # again and watches. ---
    c = clip("test", 2.0)
    present = P_(OPEN_L, {"left_arm": [-40, -30, -36], "left_forearm": [-20, 0, 0], "left_hand": [0, 50, 0], "body": [0, -16, 0],
                          "head": [-2, 22, 4], "right_arm": [-6, 0, 10], "right_leg": [0, 0, 3], "left_leg": [-6, 0, -6]})
    c.key(0, STAND)
    c.key(0.6, P_(present, {"left_arm": [-30, -20, -24]}), E_OUT)
    c.key(1.0, present, E_IO)
    c.key(1.4, P_(present, {"head": [2, 14, 2]}), E_IO)
    c.key(2.0, STAND, E_IO)

    # --- stagger: struck, she rocks back half a step, a hand rising to her temple, and recovers her composure at once. ---
    c = clip("stagger", 0.8)
    c.key(0, STAND)
    c.key(0.1, P_(LOOSE, {"body": [-14, 8, 0], "head": [-18, -10, 0], "right_arm": [-20, 0, 24], "left_arm": [-10, 0, -20],
                          "@root": [0, 0, 1.4], "right_leg": [10, 0, 0], "left_leg": [-6, 0, 0]}), E_SNAP)
    c.key(0.4, P_(STAND, {"body": [4, 0, 0], "head": [6, 4, 0], "@root": [0, 0, 0.8]}), E_OUT)
    c.key(0.8, STAND, E_IO)

    # --- death (6 s, held): struck through; she sways, sinks to her knees (2.0), her hands open at her sides, head tipping back;
    # at 3.2 the light tears out of her eyes and mouth and pours until 5.5. ---
    c = clip("death", 6.0, hold=True)
    c.key(0, P_(STAND, {"%death_light": [0.01, 0.01, 0.01]}))
    c.key(0.3, P_(LOOSE, {"body": [-12, 0, 0], "head": [-18, 0, 0], "right_arm": [-16, 0, 30], "left_arm": [-16, 0, -30],
                          "@root": [0, 0, 1.2], "%death_light": [0.01, 0.01, 0.01]}), E_SNAP)
    c.key(1.2, P_(LOOSE, {"body": [10, 6, 0], "head": [12, 0, 0], "right_arm": [-4, 0, 8], "left_arm": [-24, 0, -8],
                          "left_forearm": [-40, 0, 0], "right_leg": [-10, 0, 0], "left_leg": [8, 0, 0], "@root": [0, -0.5, 1.0],
                          "%death_light": [0.01, 0.01, 0.01]}), E_IO)
    k2 = P_(kneel(24), OPEN_R, OPEN_L, {"right_arm": [6, 0, 10], "left_arm": [4, 0, -10], "body": [18, 0, 0], "@root": [0, -5.0, 1.0],
                                        "%death_light": [0.01, 0.01, 0.01]})
    c.key(2.0, k2, E_IN)
    up_ = P_(kneel(-38), OPEN_R, OPEN_L, {"body": [-12, 0, 0], "right_arm": [-14, 0, 40], "left_arm": [-14, 0, -40],
                                          "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "@root": [0, -5.0, 1.0],
                                          "%death_light": [0.01, 0.01, 0.01]})
    c.key(3.0, up_, E_IO)
    c.key(3.2, P_(up_, {"head": [-44, 0, 0], "%death_light": [1, 1, 1]}), E_SNAP)
    c.key(5.5, P_(up_, {"head": [-46, 0, 0], "%death_light": [1.2, 1.2, 1.35]}), E_IO)
    c.key(6.0, P_(up_, {"head": [-46, 0, 0], "%death_light": [0.01, 0.01, 0.01]}), E_IN)
    c.layer("body", lambda t: [1.6 * math.sin(t * 77) if 3.2 < t < 5.6 else 0, 0, 0])
    c.layer("head", lambda t: [0, 1.4 * math.sin(t * 89) if 3.2 < t < 5.6 else 0, 0])

    # --- emerge: she is simply there -- head bowed, hands folded; she lifts her head (1.8), smooths the front of her coat with
    # both hands, and settles. ---
    c = clip("emerge", 3.0)
    bowed = P_(STAND, {"head": [26, 0, 0], "body": [4, 0, 0]})
    smooth = reach(P_(OPEN_R, OPEN_L, {"head": [-4, 0, 0]}), "right", (-1.6, 18.5, -3.9), grip=(-NB.ax, NB.palm + 0.4, -0.3))
    smooth = reach(smooth, "left", (1.6, 18.5, -3.9), grip=(NB.ax, NB.palm + 0.4, -0.3))
    smooth2 = reach(smooth, "right", (-1.9, 14.5, -3.6), grip=(-NB.ax, NB.palm + 0.4, -0.3))
    smooth2 = reach(smooth2, "left", (1.9, 14.5, -3.6), grip=(NB.ax, NB.palm + 0.4, -0.3))
    c.key(0, bowed)
    c.key(1.2, bowed, E_IO)
    c.key(1.8, P_(STAND, {"head": [-6, 0, 0]}), E_OUT)
    c.key(2.2, smooth, E_IO)
    c.key(2.6, smooth2, E_IO)
    c.key(3.0, STAND, E_IO)

    for cl in clips:
        cl.done(step=0.05)
    return f, clips


def hidden_for(clip=None, coat=True):
    """What the renderer hides while `clip` plays (for previews)."""
    from_assets = {"drill": ("strap_in", "drill_lance"), "palm_light_r": ("palm_strike", "wipe"),
                   "palm_light_l": ("restraint", "wipe"), "death_light": ("death",)}
    h = [b for b, cl in from_assets.items() if clip not in cl]
    if not coat:
        h.append("coat")
    return tuple(h)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=18000)
    m.rig.write(GEO + "naomi.geo.json")
    save(t, "entity", "naomi")
    save(g, "entity", "naomi_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + "naomi.animation.json")


if __name__ == "__main__":
    generate()
