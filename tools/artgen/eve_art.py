"""Eve, the Mother of All (v0.39): the boss's rig, texture + glowmask and clips (names, bones and timings in
`entity/boss/eve/EveAssets.java`; `EveAssetsTest` checks them).

A young pale woman (29 px to the crown, slim), very long near-black hair down her back and framing her face, amber eyes
that glow (`eyes`: a shell over them, lit in the glowmask). A plain ankle-length dress of undyed hemp, olive-brown, worn
and mud-stained at the hem, split into a front `skirt` and a back `skirt_back` so she can stride; roots knotted round her
waist and wound up her left forearm, bark grown through the bodice; bare pale feet whose toes run out into roots; fingers
that end in root claws. Roaches crawl over her (`roaches`: roach_1..6 on her chest, back and waist); more are in her mouth
(`mouth_roaches`, on the `jaw`, seen when it drops). Two growths the renderer toggles by phase: `bark_mantle` (bark plates
over her shoulders and upper arms, phase II on; its arm plates `mantle_r`/`mantle_l` copy the arms' pose in every clip) and
`root_crown` (a crown of twisted roots, amber in its cracks, phase IV).

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- jaw -- mouth_roaches;  eyes, brows, hair, hair_l, hair_r, root_crown
                       -- hair_back -- hair_tip
               -- waist_roots, roaches -- roach_1..6, bark_mantle -- mantle_r, mantle_l
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_thumb, right_claws  (left the same)
       -- skirt, skirt_back
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import fabric, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "eve"
A = "animation.eve."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

# --- proportions (slim: 29 px to the crown) ------------------------------------------------------------------------
HIP = 12.6
NECK = 21.0
TW = 3.3          # half the torso's width
TD = 1.8          # half its depth
AW = 2.4
AX = 4.5
SY = 20.4
ELBOW = 16.0
WRIST = 12.0
PALM = 10.6
LX = 1.5
LW = 2.6
KNEE = 6.4
HEM = 1.3         # the dress's hem, at the ankles
WAIST = HIP + 1.2

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#6a5856", "#93807c", "#b5a29c", "#cbbab2", "#ddcfc8", "#ece2dc")
HAIR = Ramp("#080605", "#110d0b", "#1a1410", "#251c16", "#33271e", "#463529")
DRESS = Ramp("#231d12", "#352d1c", "#4a4028", "#5e5335", "#736743", "#8a7d55")
MUD = Ramp("#120d08", "#1e160e", "#2c2116", "#3a2c1d", "#4a3a27", "#5c4932")
BARK = Ramp("#140d08", "#22170e", "#332315", "#45311e", "#5a4129", "#715436")
ROOT = Ramp("#120c08", "#20160e", "#302217", "#423020", "#57412b", "#6f5638")
AMBER = Ramp("#4a2003", "#7a3c06", "#b06810", "#e0921e", "#ffbe46", "#ffe39a")
ROACH = Ramp("#0c0603", "#1a0d06", "#2c160b", "#432212", "#5c321b", "#8a5532")
LIPS = Ramp("#6e4a4a", "#8a5e5c", "#a07170", "#b38382", "#c49594", "#d4a8a6")
MOUTH = hexc("#1a0807")

SIDES = (("right", -1), ("left", 1))


def tone(ramp, v):
    return ramp[max(0, min(5, int(round(v))))]


FACE = [   # x = 0 is the viewer's left (her right); rows from the crown down; 0-11 the skull, 12-15 the jaw
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhsshhhhhhh",
    "hhhhsssssssshhhh",
    "hhhssssssssssshh"[:3] + "ssssssssss" + "hhh",
    "hhsbbbbssbbbbshh",
    "hhsUUUssssUUUshh",
    "hhsuwIwsswIwushh",
    "hhsssssnnssssshh",
    "hhssssnNNnsssshh",
    "hhhsssssssssshhh",
    "hhhssssllsssshhh",
    "hhhsssLLLLssshhh",
    "hhhhsssssssshhhh",
    "hhhhhsssssshhhhh",
    "hhhhhhsssshhhhhh",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(seed, base=3.3):
    sk = H.skin_mat(SKIN, seed, base, 0.45)

    def m(f, x, y, w, h):
        c = sk(f, x, y, w, h)
        if h01("vein", seed, f, x, y) < 0.025:
            c = mix(c, hexc("#7f8aa0"), 0.25)          # faint blue veins under pale skin
        return c
    return m


def hair_m(seed, base=1.6):
    """Long, straight, near-black: vertical strands with a cold sheen."""
    def m(face, x, y, w, h):
        n = fbm(x * 3.0 + len(face) * 7, y * 0.35, seed, 64, 64, 2, 6.0)
        v = base + (n - 0.5) * 1.6
        if (x * 5 + y // 7) % 7 == 0:
            v += 0.9
        if face == "up":
            v += 0.4
        if face == "down":
            v -= 0.8
        return tone(HAIR, v)
    return m


def dress_m(seed, base=3.0, mud_from=0.72):
    """Undyed hemp: soft folds, a coarse weave, stains climbing from the hem."""
    cl = fabric(DRESS, seed, base, 0.35)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        if f not in ("up", "down") and (x + 2 * y) % 5 == 0:
            c = shade(c, 0.93)
        if h01("weave", seed, f, x, y) < 0.05:
            c = shade(c, 1.1)
        t = y / max(1, h - 1)
        if f not in ("up", "down") and t > mud_from:
            n = fbm(x * 2 + len(f) * 3, y * 2, seed + 9, 64, 64, 2, 5.0)
            k = min(1.0, (t - mud_from) / (1 - mud_from) * (0.6 + n))
            c = mix(c, tone(MUD, 2.2 + n), k * 0.85)
        if f not in ("up", "down") and fbm(x * 1.5, y * 1.5 + len(f), seed + 3, 64, 64, 2, 4.0) > 0.78:
            c = mix(c, MUD[2], 0.35)                   # old stains on the cloth
        return c
    return m


def bark_m(seed, base=2.6):
    """Bark: deep vertical furrows over ridges."""
    def m(f, x, y, w, h):
        n = fbm(x * 4 + len(f) * 5, y * 0.8, seed, 64, 64, 2, 6.0)
        v = base + (n - 0.5) * 2.2
        if (x + int(n * 4)) % 3 == 0:
            v -= 1.4
        if f == "up":
            v += 0.6
        if h01("lichen", seed, f, x, y) < 0.03:
            return mix(tone(BARK, v), hexc("#5e6a4a"), 0.5)
        return tone(BARK, v)
    return m


def root_m(seed, base=2.6, along="y"):
    """A root: dark, ringed along its length, a lighter bark-skin on top."""
    def m(f, x, y, w, h):
        i = y if along == "y" else x
        v = base + (0.9 if i % 3 == 0 else 0) - (0.7 if f in ("down", "south") else 0)
        v += (fbm(x * 3, y * 3 + len(f), seed, 64, 64, 2, 5.0) - 0.5) * 1.4
        return tone(ROOT, v)
    return m


def crown_glow(seed):
    """The root crown's cracks smoulder amber."""
    def g(f, x, y, w, h, c):
        return AMBER[3] if h01("crack", seed, f, x, y) < 0.14 else None
    return g


def roach_m(f, x, y, w, h):
    if f == "up":
        return ROACH[4] if (x + y) % 3 == 0 else (ROACH[2] if x == w // 2 else ROACH[3])
    if f == "down":
        return ROACH[0]
    return ROACH[1] if y else ROACH[3]


# =====================================================================================================
# Geometry
# =====================================================================================================

ROACHES = [   # (bone pivot, cube origin, cube size): on her chest, back and waist, crawling
    ((1.8, 18.0, -TD - 0.4), (1.4, 17.4, -TD - 0.55), (0.8, 1.3, 0.35)),
    ((-1.6, 15.2, -TD - 0.4), (-2.0, 14.6, -TD - 0.55), (0.8, 1.2, 0.35)),
    ((1.2, 17.0, TD + 0.4), (0.8, 16.4, TD + 0.2), (0.8, 1.3, 0.35)),
    ((TW + 0.4, 14.6, 0.0), (TW + 0.2, 14.0, -0.4), (0.35, 1.2, 0.8)),
    ((-0.8, WAIST - 0.4, -TD - 0.75), (-1.2, WAIST - 1.0, -TD - 0.9), (0.8, 1.2, 0.35)),
    ((-2.4, 20.0, -TD - 0.4), (-2.8, 19.4, -TD - 0.55), (0.8, 1.2, 0.35)),
]


def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("head", (0, NECK, 0), "body")
    m.bone("jaw", (0, NECK + 2.0, 2.0), "head")
    m.bone("mouth_roaches", (0, NECK + 2.0, -2.0), "jaw")
    m.bone("eyes", (0, NECK + 4.2, -4.0), "head")
    m.bone("brows", (0, NECK + 5.2, -4.0), "head")
    m.bone("hair", (0, NECK + 7.0, 0), "head")
    m.bone("hair_l", (3.0, NECK + 5.0, -3.3), "head")
    m.bone("hair_r", (-3.0, NECK + 5.0, -3.3), "head")
    m.bone("hair_back", (0, NECK + 6.0, 4.0), "head")
    m.bone("hair_tip", (0, NECK - 2.4, 4.4), "hair_back")
    m.bone("root_crown", (0, NECK + 7.6, 0), "head")
    m.bone("waist_roots", (0, WAIST, 0), "body")
    m.bone("roaches", (0, HIP, 0), "body")
    for i, (piv, _o, _s) in enumerate(ROACHES):
        m.bone(f"roach_{i + 1}", piv, "roaches")
    m.bone("bark_mantle", (0, NECK, 0), "body")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (AX * s - 0.2 * s, SY, 0), "body")
        m.bone(f"mantle_{side[0]}", (AX * s - 0.2 * s, SY, 0), "bark_mantle")
        m.bone(f"{side}_forearm", (AX * s, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, PALM, -0.2), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 0.8 * s, PALM + 1.0, -1.1), f"{side}_hand")
        m.bone(f"{side}_claws", (AX * s, PALM - 1.0, -0.2), f"{side}_fingers")
    m.bone("skirt", (0, WAIST, -0.2), "root")
    m.bone("skirt_back", (0, WAIST, 0.2), "root")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (LX * s, HIP, 0), "root")
        m.bone(f"{side}_shin", (LX * s, KNEE, 0), f"{side}_leg")


def build_head(m, seed=39001):
    sk = skin_m(seed)
    hr = hair_m(seed + 1)
    shadow = lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.84)
    lg = {"s": sk, "h": hr, "U": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.74), "u": hexc("#3a2010"),
          "w": AMBER[3], "I": AMBER[5], "b": HAIR[2], "n": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.04),
          "N": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.78), "l": LIPS[2], "L": LIPS[4], "d": shadow}

    def face_px(rows, y_off):
        def at(f, x, y, w, h):
            v = lg.get(rows[y + y_off][x], sk)
            return v(f, x, y, w, h) if callable(v) else v
        return at

    def skull(f, x, y, w, h):
        if f == "north":
            return face_px(FACE, 0)(f, x, y, w, h)
        if f == "up":
            return hr(f, x, y, w, h)
        if f == "down":
            # The roof of her mouth at the front, her neck behind.
            return MOUTH if 1 <= y <= h - 2 and 1 <= x <= w - 2 else sk(f, x, y, w, h)
        if f == "south":
            return hr(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x      # 0 at the front
        if y < 3 or fx >= 5:
            return hr(f, x, y, w, h)
        if 5 <= y <= 8 and fx == 4:
            return shade(sk(f, x, y, w, h), 0.86)  # an ear under the hair
        return sk(f, x, y, w, h)
    m.cube("head", (-4, NECK + 2.0, -4), (8, 6, 8), skull, density=2, tag="skull")
    m.cube("head", (-0.6, NECK + 3.0, -4.5), (1.2, 1.6, 0.5),
           lambda f, x, y, w, h: SKIN[5] if f == "north" and y < 2 else (SKIN[2] if f == "down" else SKIN[3]), density=2, tag="nose")

    def jaw(f, x, y, w, h):
        if f == "north":
            return face_px(FACE, 12)(f, x, y, w, h)
        if f == "up":
            # Inside her mouth: the dark throat, a tongue; behind, the neck.
            if 1 <= y <= h - 2 and 1 <= x <= w - 2:
                return hexc("#5a2a28") if 5 <= x <= w - 6 and 4 <= y <= h - 5 else MOUTH
            return sk(f, x, y, w, h)
        if f == "down":
            return shade(sk(f, x, y, w, h), 0.8)
        fx = x if f == "west" else w - 1 - x
        return hr(f, x, y, w, h) if fx >= 5 or f == "south" else sk(f, x, y, w, h)
    m.cube("jaw", (-3.9, NECK, -3.95), (7.8, 2.0, 6.0), jaw, density=2, tag="jaw")
    # Roaches waiting in her mouth (seen only when it opens).
    for i, (x, z) in enumerate(((-1.4, -3.2), (0.6, -2.6), (-0.2, -1.6), (1.5, -3.4))):
        m.cube("mouth_roaches", (x, NECK + 1.95, z), (0.8, 0.35, 1.1), roach_m, density=4, tag="mouth_roach",
               rotation=(0, (i * 47) % 60 - 30, 0), pivot=(x + 0.4, NECK + 2.0, z + 0.5))
    # Hair: a cap over the crown, the long fall behind, two locks framing her face.
    m.cube("hair", (-4, NECK + 6.4, -4), (8, 1.8, 8), none_on(("down",), hr), inflate=0.3, density=2, tag="hair_cap")
    for s in (-1, 1):
        m.cube("hair", (s * 4.0 - 0.6, NECK + 0.5, -3.8), (1.2, 7.4, 7.6), none_on(("up",), hr), inflate=0.25, density=2,
               tag="hair_side")
    back = hair_m(seed + 2, 1.4)
    m.cube("hair_back", (-4.2, NECK - 2.4, 3.6), (8.4, 8.8, 1.4), back, inflate=0.2, density=2, tag="hair_fall")

    def tip(f, x, y, w, h):
        if f == "north":
            return back(f, x, y, w, h)
        if y >= h - 2 and (x * 7) % 3 == 0:
            return None                              # the ends, uneven
        return back(f, x, y, w, h)
    m.cube("hair_tip", (-4.0, NECK - 10.2, 3.9), (8.0, 7.8, 1.2), tip, density=2, tag="hair_tip")
    for bone, s in (("hair_l", 1), ("hair_r", -1)):
        m.cube(bone, (s * 3.0 - 0.7, NECK - 6.6, -3.9), (1.4, 11.6, 1.2), hair_m(seed + 5 + s, 1.8), density=2, tag="lock")
    # Brows: fine and dark, level, set a little high.
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.0, NECK + 5.1, -4.12), (2.0, 0.4, 0.25), solid(HAIR[1]), density=4, tag="brow",
               rotation=(0, 0, 4 * s), pivot=(s * 1.0, NECK + 5.2, -4.1))
    # The eyes' shell: amber, lit.

    def shell(f, x, y, w, h):
        ch = "suwIwsswIwus"[x]
        if ch == "s":
            return None
        return {"u": AMBER[1], "w": AMBER[3], "I": AMBER[5]}[ch]
    m.cube("eyes", (-3.0, NECK + 4.0, -4.06), (6.0, 0.5, 0), shell, lambda f, x, y, w, h, c: c, faces=("north",), density=2, tag="eyes")
    m.cube("body", (-1.5, NECK - 1.0, -1.5), (3.0, 1.4, 3.0), sk, density=2, tag="neck")


def build_crown(m, seed=39050):
    rt = root_m(seed, 2.8)
    glow = crown_glow(seed)
    # The band: four twisted roots round her brow.
    for i, (o, s, rot) in enumerate((((-4.5, NECK + 6.6, -4.6), (9.0, 1.2, 1.0), (0, 0, 4)),
                                     ((-4.5, NECK + 6.8, 3.6), (9.0, 1.2, 1.0), (0, 0, -5)),
                                     ((-4.6, NECK + 6.7, -4.4), (1.0, 1.2, 8.8), (5, 0, 0)),
                                     ((3.6, NECK + 6.5, -4.4), (1.0, 1.2, 8.8), (-4, 0, 0)))):
        piv = (o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2)
        m.cube("root_crown", o, s, rt, glow, density=2, tag="crown_band", rotation=rot, pivot=piv)
    # Tines rising from it, leaning out, the front ones tallest.
    tines = ((-3.4, -4.2, 4.0, 18, 0, -16), (-1.2, -4.4, 5.2, 22, 0, -6), (1.2, -4.4, 5.0, 22, 0, 7), (3.4, -4.2, 3.8, 18, 0, 15),
             (-4.2, -1.0, 3.4, 0, 0, -24), (4.2, -1.0, 3.6, 0, 0, 24), (-3.0, 3.6, 3.0, -16, 0, -14), (3.0, 3.6, 3.2, -16, 0, 14),
             (0.0, 4.2, 2.6, -24, 0, 0))
    for i, (x, z, tall, rx, ry, rz) in enumerate(tines):
        m.cube("root_crown", (x - 0.4, NECK + 7.4, z - 0.4), (0.8, tall, 0.8), rt, glow, density=4, tag="crown_tine",
               rotation=(rx, ry, rz), pivot=(x, NECK + 7.4, z))
        m.cube("root_crown", (x - 0.25, NECK + 7.4 + tall * 0.55, z - 0.25), (0.5, 1.4, 0.5), rt, glow, density=4, tag="crown_twig",
               rotation=(rx * 0.6 + (-20 if i % 2 else 20), ry, rz * 1.6 + (14 if i % 2 else -14)),
               pivot=(x, NECK + 7.4 + tall * 0.55, z))


def build_body(m, seed=39100):
    sk = skin_m(seed)
    dress = dress_m(seed + 1)
    bark = bark_m(seed + 2)
    m.cube("body", (-TW, HIP, -TD), (2 * TW, NECK - HIP, 2 * TD), sk, density=2, tag="torso")

    # The bodice: a square neckline, the cloth gathered under the bust, bark grown through it in patches.
    def bodice(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north" and y < 4 and 2 <= x <= w - 3:
            return None                              # the neckline: her skin shows
        if f in ("north", "east", "west", "south"):
            n = fbm(x * 2 + len(f) * 9, y * 1.5, seed + 7, 64, 64, 2, 4.0)
            if n > 0.66 and y > 3:
                return bark(f, x, y, w, h)
            if y == 8:
                return shade(dress(f, x, y, w, h), 0.78)   # the gathering seam
        return dress(f, x, y + 30, w, h + 60)
    m.cube("body", (-TW, HIP - 0.4, -TD), (2 * TW, NECK - HIP - 0.4, 2 * TD), bodice, inflate=0.25, density=2, tag="bodice")
    m.cube("body", (-TW + 0.6, NECK - 1.2, -TD - 0.32), (2 * TW - 1.2, 0.5, 0.3), lambda f, x, y, w, h: tone(DRESS, 3.6 if f == "up" else 2.6),
           density=4, tag="neckline_edge")
    # A bark patch standing out of the cloth over her right ribs.
    m.cube("body", (-TW - 0.1, 15.0, -TD - 0.45), (2.4, 3.2, 0.5), bark, density=2, tag="bark_patch", rotation=(0, 0, 8),
           pivot=(-TW + 1.0, 16.6, -TD))
    m.cube("body", (1.0, 17.6, TD - 0.05), (2.0, 2.6, 0.55), bark, density=2, tag="bark_patch_back", rotation=(0, 0, -10),
           pivot=(2.0, 18.9, TD))

    # The roots knotted round her waist.
    rt = root_m(seed + 4, 2.5, "x")
    for o, s, rot in (((-TW - 0.6, WAIST - 0.6, -TD - 0.7), (2 * TW + 1.2, 0.9, 0.7), (0, 0, -6)),
                      ((-TW - 0.6, WAIST - 0.2, -TD - 0.6), (2 * TW + 1.2, 0.7, 0.6), (0, 0, 9)),
                      ((-TW - 0.6, WAIST - 0.5, TD - 0.1), (2 * TW + 1.2, 0.9, 0.7), (0, 0, 5)),
                      ((-TW - 0.7, WAIST - 0.5, -TD - 0.6), (0.7, 0.9, 2 * TD + 1.2), (6, 0, 0)),
                      ((TW, WAIST - 0.5, -TD - 0.6), (0.7, 0.9, 2 * TD + 1.2), (-7, 0, 0))):
        piv = (o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2)
        m.cube("waist_roots", o, s, rt, density=2, tag="waist_root", rotation=rot, pivot=piv)
    m.cube("waist_roots", (1.0, WAIST - 1.0, -TD - 1.0), (1.4, 1.4, 1.0), root_m(seed + 5, 3.0), density=2, tag="root_knot")

    # The roaches on her.
    for i, (piv, o, s) in enumerate(ROACHES):
        m.cube(f"roach_{i + 1}", o, s, roach_m, density=4, tag="roach")
        hx, hy, hz = o[0] + s[0] * 0.2, o[1] + s[1], o[2]
        hs = (s[0] * 0.6, 0.4, s[2]) if s[0] < s[2] or s[2] > 0.5 else (s[0] * 0.6, 0.4, s[2])
        m.cube(f"roach_{i + 1}", (hx, hy, hz), hs, solid(ROACH[1]), density=4, tag="roach_head")
        m.cube(f"roach_{i + 1}", (hx + hs[0] * 0.2, hy + 0.4, hz + (hs[2] / 2 if s[2] > 0.5 else 0)), (0.1, 0.6, 0.1),
               solid(ROACH[0]), density=8, tag="antenna", rotation=(0, 0, 20), pivot=(hx, hy + 0.4, hz))


def build_mantle(m, seed=39200):
    bark = bark_m(seed, 2.8)
    # Over her shoulders and the top of her back: the plates the bark grows into.
    m.cube("bark_mantle", (-TW - 0.3, NECK - 1.6, -TD - 0.5), (2 * TW + 0.6, 1.4, 2 * TD + 1.0), bark, density=2, tag="mantle_yoke")
    m.cube("bark_mantle", (-TW + 0.2, NECK - 5.4, TD + 0.1), (2 * TW - 0.4, 4.0, 0.7), bark, density=2, tag="mantle_back",
           rotation=(6, 0, 0), pivot=(0, NECK - 1.4, TD))
    for s in (-1, 1):
        m.cube("bark_mantle", (s * 2.2 - 1.2, NECK - 3.2, -TD - 0.75), (2.4, 2.2, 0.55), bark, density=2, tag="mantle_breast",
               rotation=(-8, 0, -12 * s), pivot=(s * 2.2, NECK - 1.4, -TD - 0.5))
    for bone, s in (("mantle_r", -1), ("mantle_l", 1)):
        cx = AX * s
        m.cube(bone, (cx - AW / 2 - 0.3, SY - 1.2, -AW / 2 - 0.3), (AW + 0.6, 2.4, AW + 0.6), bark, inflate=0.25, density=2,
               tag="mantle_pauldron")
        m.cube(bone, (cx + (0.7 if s > 0 else -1.6) , ELBOW + 0.4, -AW / 2 - 0.2), (0.9, SY - ELBOW - 1.6, AW + 0.4), bark, density=2,
               tag="mantle_plate")
        m.cube(bone, (cx + (1.2 if s > 0 else -1.8), SY + 0.4, -0.5), (0.6, 2.2, 0.6), root_m(seed + 3 + s, 3.0), density=4,
               tag="mantle_spur", rotation=(0, 0, -26 * s), pivot=(cx + 1.2 * s, SY + 0.4, 0))


def build_arms(m, seed=39300):
    dress = dress_m(seed, 2.9, mud_from=2.0)
    hand = skin_m(seed + 1, 3.4)
    rt = root_m(seed + 2, 2.4, "x")
    for side, s in SIDES:
        cx = AX * s

        def sleeve(f, x, y, w, h):
            if f == "up":
                return dress(f, x, y, w, h)
            if y >= h - 1 and (x * 3) % 4 == 0:
                return None                          # the frayed edge
            return dress(f, x, y, w, h)
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW - 0.2, -AW / 2), (AW, SY + 0.6 - ELBOW + 0.2, AW), sleeve, inflate=0.2, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.0, WRIST + 0.2, -1.0), (2.0, ELBOW - WRIST, 2.0), hand, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.15, ELBOW - 1.2, -1.15), (2.3, 1.0, 2.3), none_on(("up",), sleeve), density=2, tag="cuff")
        if s > 0:
            # Her left forearm, wound in roots from the wrist to the elbow.
            for i in range(4):
                y = WRIST + 0.5 + i * 0.95
                m.cube(f"{side}_forearm", (cx - 1.25, y, -1.25), (2.5, 0.55, 2.5), none_on(("up", "down"), rt), density=4,
                       tag="arm_root", rotation=(0, 0, 18 if i % 2 else -18), pivot=(cx, y + 0.3, 0))
            m.cube(f"{side}_forearm", (cx + 0.9, WRIST - 0.8, -0.3), (0.5, 2.0, 0.5), root_m(seed + 6, 2.8), density=4,
                   tag="arm_tendril", rotation=(0, 0, -14), pivot=(cx + 1.1, WRIST + 0.4, 0))
        m.cube(f"{side}_hand", (cx - 0.95, PALM, -1.1), (1.9, WRIST - PALM + 0.2, 2.2), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.9 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_fingers", (cx - 0.9, PALM - 1.2, -1.1), (1.8, 1.2, 2.0), fing, density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 0.7 * s - 0.4, PALM + 0.2, -1.6), (0.8, 1.3, 0.7), hand, density=2, tag="thumb")
        # Root claws: the fingertips run out into dark root, curved.
        for j, z in enumerate((-1.0, -0.45, 0.1, 0.6)):
            m.cube(f"{side}_claws", (cx - 0.2 + (0.1 * s if j % 2 else 0), PALM - 2.6, z - 0.1), (0.4, 1.6, 0.4), root_m(seed + 9 + j, 2.0),
                   density=4, tag="claw", rotation=(-12, 0, 0), pivot=(cx, PALM - 1.1, z))


def build_skirt(m, seed=39400):
    dress = dress_m(seed)
    L = WAIST - HEM

    def front(f, x, y, w, h):
        if f == "up":
            return None
        if f == "north" and (x * 5 + y) % 9 == 0 and y > h * 0.4:
            return shade(dress(f, x, y, w, h), 0.86)
        if f == "north" and y >= h - 1 and x % 4 == 0:
            return None                              # a torn hem
        return dress(f, x, y, w, h)
    m.cube("skirt", (-TW - 0.8, HEM, -TD - 0.7), (2 * TW + 1.6, L, TD + 1.1), front, density=2, tag="skirt_front",
           rotation=(-3, 0, 0), pivot=(0, WAIST, -0.2))

    def back(f, x, y, w, h):
        if f == "up":
            return None
        if f == "south" and y >= h - 1 and (x + 2) % 4 == 0:
            return None
        return dress(f, x, y, w, h)
    m.cube("skirt_back", (-TW - 0.85, HEM - 0.2, -0.6), (2 * TW + 1.7, L + 0.2, TD + 1.3), back, density=2, tag="skirt_back",
           rotation=(3, 0, 0), pivot=(0, WAIST, 0.2))
    # Roots trailing down the front of the dress from the knot at her waist.
    for i, (x, length, rz) in enumerate(((1.6, 5.6, -6), (0.6, 3.8, 4), (2.4, 4.4, 12))):
        m.cube("skirt", (x - 0.25, WAIST - 0.4 - length, -TD - 0.95), (0.5, length, 0.4), root_m(seed + 2 + i, 2.6), density=4,
               tag="skirt_root", rotation=(-4, 0, rz), pivot=(x, WAIST - 0.4, -TD - 0.8))


def build_legs(m, seed=39500):
    for side, s in SIDES:
        sk = skin_m(seed + (s > 0), 3.2)
        x0 = LX * s - LW / 2
        # The dress drapes over her thighs (seen when she crouches or kneels); her shins are bare.
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), dress_m(seed + 4 + (s > 0), 2.8, mud_from=2.0), inflate=0.15,
               density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0 + 0.1, 0.3, -LW / 2 + 0.1), (LW - 0.2, KNEE - 0.3, LW - 0.2), sk, density=2, tag="shin")
        # A bare foot, its toes running out into roots.
        m.cube(f"{side}_shin", (x0 + 0.1, 0, -LW / 2 - 1.2), (LW - 0.2, 0.8, LW + 1.2), lambda f, x, y, w, h, sk=sk: shade(sk(f, x, y, w, h), 0.94),
               density=2, tag="foot")
        for j in range(3):
            tx = x0 + 0.4 + j * 0.8
            m.cube(f"{side}_shin", (tx - 0.2, 0.0, -LW / 2 - 2.4), (0.4, 0.4, 1.4), root_m(seed + 7 + j + (s > 0) * 3, 2.2, "x"),
                   density=4, tag="toe_root", rotation=(0, (j - 1) * 18, 0), pivot=(tx, 0.2, -LW / 2 - 1.2))


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m)
    build_head(m)
    build_crown(m)
    build_body(m)
    build_mantle(m)
    build_arms(m)
    build_skirt(m)
    build_legs(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

def merge(*ps):
    out = {}
    for p in ps:
        out.update(p)
    return out


def follow(pose):
    """The bark mantle's arm plates move with the arms, and her hair hangs (it leans back against whatever tilts her), in every key."""
    p = dict(pose)
    tilt = sum(p.get(b, [0, 0, 0])[0] for b in ("root", "body", "head"))
    comp = max(-70.0, min(70.0, -0.85 * tilt))
    for bone, k in (("hair_back", 1.0), ("hair_l", 0.8), ("hair_r", 0.8)):
        v = list(p.get(bone, [0, 0, 0]))
        p[bone] = [v[0] + comp * k, v[1], v[2]]
    for side in ("right", "left"):
        if side + "_arm" in p:
            p["mantle_" + side[0]] = p[side + "_arm"]
        if "@" + side + "_arm" in p:
            p["@mantle_" + side[0]] = p["@" + side + "_arm"]
    return p


class Clip(H.Clip):
    """H.Clip with the mantle following the arms, and "%bone" keys as scale."""

    def key(self, t, pose, ease=None):
        return super().key(t, follow(pose), ease)

    def done(self, skip=()):
        scaled = []
        for _, p, _ in self.frames:
            for k in p:
                if k.startswith("%") and k not in scaled:
                    scaled.append(k)
        for k in scaled:
            keys = []
            for t, p, e in self.frames:
                v = [round(float(c), 3) for c in p.get(k, [1, 1, 1])]
                keys.append((round(t, 4), v, e) if e else (round(t, 4), v))
            self.anim.scale(k[1:], *keys)
        self.frames = [(t, {k: v for k, v in p.items() if not k.startswith("%")}, e) for t, p, e in self.frames]
        return super().done(skip)


def jitter(seed, t, amp):
    return [amp * (2 * h01(seed, t, i) - 1) for i in range(3)]


# Standing still: hands loose, a little forward of her hips, chin level, weight on one foot.
IDLE = {"right_arm": [-6, 0, 6], "left_arm": [-6, 0, -6], "right_forearm": [-14, 8, 0], "left_forearm": [-14, -8, 0],
        "right_fingers": [12, 0, 0], "left_fingers": [12, 0, 0], "body": [-1, 0, 0], "skirt": [0, 0, 0], "skirt_back": [0, 0, 0]}
# Kneeling on both knees, sitting back a little.
KNEEL = {"@root": [0, -5.3, 0], "right_leg": [-2, 0, 2], "left_leg": [-2, 0, -2], "right_shin": [88, 0, 0], "left_shin": [88, 0, 0],
         "skirt": [-10, 0, 0], "skirt_back": [16, 0, 0],
         "%skirt": [1.0, 0.6, 1.1], "%skirt_back": [1.0, 0.6, 1.0]}


def roach_crawl(t, speed=1.0, amp=1.0):
    """Each roach a little loop along the surface it is on."""
    out = {}
    for i in range(len(ROACHES)):
        ph = t * 2 * math.pi * speed + i * 1.7
        a, b = amp * 1.2 * math.sin(ph), amp * 0.9 * math.sin(ph * 2 + i)
        if i == 3:
            out[f"@roach_{i + 1}"] = [0, a, b]          # on her side: along y and z
        else:
            out[f"@roach_{i + 1}"] = [b, a, 0]
        out[f"roach_{i + 1}"] = [0, 0, 18 * math.cos(ph)]
    return out


def hair_sway(t, amp=1.0):
    ph = t * 2 * math.pi
    return {"hair_back": [3 * amp * math.sin(ph), 0, 1.5 * amp * math.sin(ph * 0.5)], "hair_tip": [5 * amp * math.sin(ph - 0.7), 0, 2 * amp * math.sin(ph * 0.5 - 0.5)],
            "hair_l": [-2 * amp * math.sin(ph + 0.5), 0, 0], "hair_r": [-2 * amp * math.sin(ph + 1.1), 0, 0]}


def anims():
    f = AnimFile()
    # --- loops ---------------------------------------------------------------------------------------------------
    idle = Clip(f, A + "idle", 4.0, loop=True)
    for t in (0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0):
        b = math.sin(t / 4.0 * 2 * math.pi)
        idle.key(t, merge(IDLE, hair_sway(t / 4.0, 0.6), roach_crawl(t / 4.0, 1.0),
                          {"@body": [0, -0.25 * (1 - math.cos(t / 4.0 * 2 * math.pi)), 0], "head": [-2 + 3 * b, 10 * b, 3 * b],
                           "right_forearm": [-14 - 4 * b, 8, 0], "left_fingers": [12 + 10 * b, 0, 0]}),
                 E_IO if t else None)
    idle.done()

    walk = Clip(f, A + "walk", 1.4, loop=True)
    for t, s in ((0, 1), (0.35, 0), (0.7, -1), (1.05, 0), (1.4, 1)):
        walk.key(t, merge({
            "right_leg": [16 * s, 0, 0], "left_leg": [-16 * s, 0, 0], "right_shin": [4 if s <= 0 else 14, 0, 0],
            "left_shin": [14 if s < 0 else 4, 0, 0], "right_arm": [-8 * s, 0, 5], "left_arm": [8 * s, 0, -5],
            "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0], "body": [-1, 5 * s, 0], "head": [-1, -4 * s, 0],
            "skirt": [-8 * abs(s), 4 * s, 0], "skirt_back": [6 * abs(s), 4 * s, 0]}, hair_sway(t / 1.4, 0.8), roach_crawl(t / 1.4, 1.0, 0.6)),
                 E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.35, [0, 0.3, 0], E_OUT), (0.7, [0, 0, 0], E_IN), (1.05, [0, 0.3, 0], E_OUT),
                  (1.4, [0, 0, 0], E_IN))
    walk.done()

    # --- attacks ---------------------------------------------------------------------------------------------------
    # Lash: the right hand drawn back across her body, then a backhand of root claws (tick 12).
    la = Clip(f, A + "lash", 1.3)
    wind = {"right_arm": [-60, -20, -52], "right_forearm": [-70, 30, 0], "right_hand": [0, 0, 20], "right_claws": [-30, 0, 0],
            "body": [0, 28, 0], "head": [-2, -18, 0], "left_arm": [-10, 0, -14], "left_forearm": [-20, 0, 0], "skirt": [-4, 10, 0]}
    strike = {"right_arm": [-76, 10, 62], "right_forearm": [-16, -10, 0], "right_hand": [0, 0, -20], "right_claws": [20, 0, 0],
              "right_fingers": [-10, 0, 0], "body": [0, -30, 0], "head": [-4, 14, 0], "left_arm": [-8, 0, -20], "left_forearm": [-30, 0, 0],
              "skirt": [-6, -12, 0], "skirt_back": [4, -10, 0]}
    la.key(0, IDLE)
    la.key(0.4, wind, E_OUT)
    la.key(0.5, merge(wind, {"right_arm": [-62, -24, -56]}))
    la.key(0.6, strike, E_SNAP)
    la.key(0.85, merge(strike, {"right_arm": [-60, 10, 56]}), E_OUT)
    la.key(1.3, IDLE, E_IO)
    la.done()

    # Root grasp: she crouches and drives her right hand into the earth (tick 18), the left raised, fingers spread.
    rg = Clip(f, A + "root_grasp", 1.8)
    rise = {"right_arm": [-150, 0, 18], "right_forearm": [-30, 0, 0], "right_fingers": [-20, 0, 0], "left_arm": [-20, 0, -40],
            "left_forearm": [-40, 0, 0], "body": [-10, 0, 0], "head": [-16, 0, 0], "@root": [0, 0.4, 0]}
    down = {"@root": [0, -3.2, 0], "body": [44, 0, 0], "head": [-30, 0, 0], "right_arm": [-48, 0, 10], "right_forearm": [-10, 0, 0],
            "right_fingers": [30, 0, 0], "right_claws": [30, 0, 0], "left_arm": [-30, 0, -46], "left_forearm": [-50, 0, 0],
            "left_fingers": [-30, 0, 0], "right_leg": [-60, 0, 6], "right_shin": [70, 0, 0], "left_leg": [-20, 0, -6], "left_shin": [74, 0, 0],
            "skirt": [-40, 0, 0], "skirt_back": [20, 0, 0]}
    rg.key(0, IDLE)
    rg.key(0.55, rise, E_OUT)
    rg.key(0.9, down, E_SNAP)
    rg.key(1.3, merge(down, {"body": [40, 0, 0], "@root": [0, -3.0, 0]}))
    rg.key(1.8, IDLE, E_IO)
    rg.done()

    # Birth: her arms spread wide and low, palms down, her head goes back (tick 22) as her children climb out of the ground.
    bi = Clip(f, A + "birth", 2.2)
    open_ = lambda k, hb: {"right_arm": [-20 * k, 0, 70 * k], "left_arm": [-20 * k, 0, -70 * k], "right_forearm": [-20 * k, 0, 0],
                           "left_forearm": [-20 * k, 0, 0], "right_hand": [0, 0, -30 * k], "left_hand": [0, 0, 30 * k],
                           "right_fingers": [-20 * k, 0, 0], "left_fingers": [-20 * k, 0, 0], "body": [-12 * k, 0, 0], "head": [-hb, 0, 0],
                           "@body": [0, 0.6 * k, 0]}
    bi.key(0, IDLE)
    bi.key(0.5, merge(open_(0.4, 4), {"body": [20, 0, 0], "head": [20, 0, 0], "right_arm": [-30, 0, -10], "left_arm": [-30, 0, 10],
                                      "right_forearm": [-90, 0, 0], "left_forearm": [-90, 0, 0]}), E_IO)
    bi.key(1.0, open_(0.9, 30), E_OUT)
    bi.key(1.1, open_(1.0, 40), E_BACK)
    bi.key(1.7, open_(1.0, 42))
    bi.key(2.2, IDLE, E_IO)
    bi.done()

    # Roach swarm: her jaw drops (tick 14) and the roaches pour out; her right arm sweeps the lane; the jaw shuts at the end.
    rs = Clip(f, A + "roach_swarm", 3.0)
    gape = lambda k, sweep: {"jaw": [40 * k, 0, 0], "head": [-8 * k, sweep * 0.3, 0], "body": [8 * k, sweep * 0.4, 0],
                             "right_arm": [-70, sweep, 30], "right_forearm": [-10, 0, 0], "right_fingers": [-20, 0, 0],
                             "left_arm": [-10, 0, -24], "left_forearm": [-30, 0, 0], "brows": [0, 0, 0],
                             "@mouth_roaches": [0, -0.2 * k, -1.2 * k]}
    rs.key(0, IDLE)
    rs.key(0.5, merge(gape(0.1, 0), {"head": [-12, 0, 0], "body": [-6, 0, 0]}), E_IO)
    rs.key(0.7, gape(1.0, -20), E_SNAP)
    for i, t in enumerate((1.1, 1.5, 1.9, 2.3)):
        g = gape(1.0 + 0.1 * (i % 2), -20 + 13 * (i + 1))
        g["@mouth_roaches"] = [0.3 * (-1) ** i, -0.4, -1.6 - 0.3 * (i % 2)]
        g["mouth_roaches"] = [10 * (-1) ** i, 0, 0]
        rs.key(t, merge(g, roach_crawl(t, 3.0, 1.2)), E_IO)
    rs.key(2.7, gape(0.9, 30), E_IO)
    rs.key(3.0, IDLE, E_IN)
    rs.done()

    # Thorn ring: both fists raised over her head, then driven down at her sides (tick 20).
    tr = Clip(f, A + "thorn_ring", 1.8)
    up = {"right_arm": [-170, 0, -10], "left_arm": [-170, 0, 10], "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
          "right_fingers": [80, 0, 0], "left_fingers": [80, 0, 0], "body": [-14, 0, 0], "head": [-20, 0, 0], "@root": [0, 0.5, 0]}
    slam = {"right_arm": [-10, 0, 34], "left_arm": [-10, 0, -34], "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0],
            "right_fingers": [80, 0, 0], "left_fingers": [80, 0, 0], "body": [18, 0, 0], "head": [8, 0, 0], "@root": [0, -1.4, 0],
            "right_leg": [-20, 0, 8], "left_leg": [-20, 0, -8], "right_shin": [30, 0, 0], "left_shin": [30, 0, 0],
            "skirt": [-10, 0, 0], "skirt_back": [12, 0, 0]}
    tr.key(0, IDLE)
    tr.key(0.7, up, E_OUT)
    tr.key(0.85, merge(up, {"right_arm": [-176, 0, -12], "left_arm": [-176, 0, 12]}))
    tr.key(1.0, slam, E_SNAP)
    tr.key(1.35, merge(slam, {"body": [16, 0, 0]}))
    tr.key(1.8, IDLE, E_IO)
    tr.done()

    # Call: arms raised high and open, her head back, the long cry from tick 24 (her jaw a little open).
    ca = Clip(f, A + "call", 2.5)
    cry = lambda k, j: {"right_arm": [-150 * k, 0, -34 * k], "left_arm": [-150 * k, 0, 34 * k], "right_forearm": [-10 * k, 0, 0],
                        "left_forearm": [-10 * k, 0, 0], "right_fingers": [-25 * k, 0, 0], "left_fingers": [-25 * k, 0, 0],
                        "head": [-34 * k, 0, 0], "body": [-10 * k, 0, 0], "jaw": [j, 0, 0], "@body": [0, 0.5 * k, 0]}
    ca.key(0, IDLE)
    ca.key(0.6, merge(cry(0.4, 0), {"body": [16, 0, 0], "head": [16, 0, 0]}), E_IO)
    ca.key(1.2, cry(1.0, 16), E_BACK)
    for i, t in enumerate((1.5, 1.8, 2.05)):
        ca.key(t, merge(cry(1.0 + 0.04 * (-1) ** i, 18 + 4 * (-1) ** i), roach_crawl(t, 2.5, 1.0)), E_IO)
    ca.key(2.5, IDLE, E_IO)
    ca.done()

    # Root field: she sinks to her knees and lays both palms on the earth (tick 26); the glade erupts round her.
    rf = Clip(f, A + "root_field", 2.5)
    palms = merge(KNEEL, {"body": [46, 0, 0], "head": [-34, 0, 0], "right_arm": [-60, 0, 8], "left_arm": [-60, 0, -8],
                          "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [-30, 0, 0], "left_hand": [-30, 0, 0],
                          "right_fingers": [-40, 0, 0], "left_fingers": [-40, 0, 0], "right_claws": [20, 0, 0], "left_claws": [20, 0, 0]})
    rf.key(0, IDLE)
    rf.key(0.6, merge(KNEEL, {"body": [8, 0, 0], "head": [10, 0, 0], "right_arm": [-30, 0, 10], "left_arm": [-30, 0, -10],
                              "right_forearm": [-50, 0, 0], "left_forearm": [-50, 0, 0]}), E_IO)
    rf.key(1.05, merge(palms, {"body": [36, 0, 0], "right_arm": [-80, 0, 8], "left_arm": [-80, 0, -8]}), E_IO)
    rf.key(1.3, palms, E_SNAP)
    rf.key(1.9, merge(palms, {"head": [-40, 0, 0]}))
    rf.key(2.5, IDLE, E_IO)
    rf.done()

    # --- the changes of phase ------------------------------------------------------------------------------------
    hunch = {"body": [36, 0, 0], "head": [26, 0, 0], "right_arm": [-40, 0, -34], "left_arm": [-40, 0, 34], "right_forearm": [-90, -30, 0],
             "left_forearm": [-90, 30, 0], "right_fingers": [70, 0, 0], "left_fingers": [70, 0, 0], "@root": [0, -1.0, 0],
             "right_leg": [-20, 0, 4], "left_leg": [-20, 0, -4], "right_shin": [30, 0, 0], "left_shin": [30, 0, 0], "skirt": [-12, 0, 0]}
    stand_open = {"right_arm": [-30, 0, 50], "left_arm": [-30, 0, -50], "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
                  "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0], "body": [-10, 0, 0], "head": [-22, 0, 0], "jaw": [8, 0, 0]}

    def transform(name, length, bark_shudders):
        tf = Clip(f, A + name, length)
        tf.key(0, IDLE)
        tf.key(0.6, hunch, E_IO)
        n = bark_shudders
        for i in range(1, n + 1):
            t = round(0.6 + i * (length * 0.55 - 0.6) / n, 2)
            j = jitter(name, i, 5)
            tf.key(t, merge(hunch, {"body": [36 + j[0], j[1], j[2]], "head": [26 + j[2], j[0], 0], "@root": [0, -1.0 - 0.2 * (i % 2), 0]},
                            roach_crawl(t, 3.0)))
        tf.key(round(length * 0.72, 2), stand_open, E_BACK)
        tf.key(round(length * 0.86, 2), merge(stand_open, {"head": [-26, 0, 0]}))
        tf.key(length, IDLE, E_IO)
        return tf

    transform("transform_2", 4.0, 5).done()
    transform("transform_3", 4.0, 5).done()

    # The last: down to her knees, the bark creeping, then she rises with her arms to the sky as the crown grows.
    t4 = Clip(f, A + "transform_4", 8.0)
    t4.key(0, IDLE)
    t4.key(0.8, hunch, E_IO)
    t4.key(1.6, merge(KNEEL, {"body": [40, 0, 0], "head": [30, 0, 0], "right_arm": [-40, 0, -30], "left_arm": [-40, 0, 30],
                              "right_forearm": [-100, -30, 0], "left_forearm": [-100, 30, 0]}), E_IN)
    for i in range(1, 7):
        t = round(1.6 + i * 0.4, 2)
        j = jitter("t4", i, 6)
        t4.key(t, merge(KNEEL, {"body": [40 + j[0], j[1], j[2]], "head": [30 + j[2], j[0], 0], "right_arm": [-40, 0, -30],
                                "left_arm": [-40, 0, 30], "right_forearm": [-100, -30, 0], "left_forearm": [-100, 30, 0]}, roach_crawl(t, 3.0)))
    t4.key(4.6, merge(KNEEL, {"body": [-16, 0, 0], "head": [-40, 0, 0], "right_arm": [-120, 0, -40], "left_arm": [-120, 0, 40],
                              "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "jaw": [16, 0, 0]}), E_BACK)
    t4.key(5.6, {"@root": [0, 0.6, 0], "body": [-14, 0, 0], "head": [-36, 0, 0], "right_arm": [-160, 0, -34], "left_arm": [-160, 0, 34],
                 "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0],
                 "jaw": [18, 0, 0]}, E_OUT)
    t4.key(6.6, merge(stand_open, {"head": [-10, 0, 0], "jaw": [0, 0, 0]}), E_IO)
    t4.key(8.0, IDLE, E_IO)
    t4.done()

    st = Clip(f, A + "stagger", 0.8)
    st.key(0, IDLE)
    st.key(0.15, {"body": [-18, -10, 6], "head": [-24, 10, 0], "right_arm": [-30, 0, 40], "left_arm": [-20, 0, -50], "@root": [0, 0, 0.8],
                  "skirt": [8, 0, 0], "skirt_back": [-4, 0, 0], "jaw": [10, 0, 0]}, E_SNAP)
    st.key(0.4, {"body": [10, 4, -2], "head": [10, -4, 0], "right_arm": [-14, 0, 14], "left_arm": [-14, 0, -14], "@root": [0, 0, 0.4]}, E_OUT)
    st.key(0.8, IDLE, E_IO)
    st.done()

    # Death: she shudders and withers, sinks to her knees (tick 70), the light leaves her eyes, then she falls forward (tick 120).
    dt = Clip(f, A + "death", 8.0, hold=True)
    dt.key(0, IDLE)
    dt.key(0.4, {"body": [-20, 0, 0], "head": [-30, 0, 0], "right_arm": [-30, 0, 40], "left_arm": [-30, 0, -40], "jaw": [20, 0, 0]}, E_SNAP)
    for i in range(1, 9):
        t = round(0.4 + 0.35 * i, 2)
        k = i / 8
        j = jitter("eve-death", i, 3 + 5 * k)
        dt.key(t, {"body": [-20 + 50 * k + j[0], j[1], j[2]], "head": [-30 + 60 * k + j[2], j[0], 0], "right_arm": [-30 + 10 * k, 0, 40 - 50 * k],
                   "left_arm": [-30 + 10 * k, 0, -40 + 50 * k], "right_forearm": [-60 * k, 0, 0], "left_forearm": [-60 * k, 0, 0],
                   "right_fingers": [80 * k, 0, 0], "left_fingers": [80 * k, 0, 0], "jaw": [20 - 10 * k, 0, 0],
                   "@root": [0, -1.0 * k, 0], "right_leg": [-14 * k, 0, 0], "left_leg": [-14 * k, 0, 0],
                   "right_shin": [20 * k, 0, 0], "left_shin": [20 * k, 0, 0]})
    kneel_slump = merge(KNEEL, {"body": [34, 0, 6], "head": [40, 0, -10], "right_arm": [-6, 0, 10], "left_arm": [-6, 0, -12],
                                "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "right_fingers": [60, 0, 0],
                                "left_fingers": [60, 0, 0], "jaw": [10, 0, 0]})
    dt.key(3.5, kneel_slump, E_IN)
    dt.key(4.8, merge(kneel_slump, {"body": [30, 0, 4], "head": [30, 0, -6]}))
    dt.key(6.0, merge(kneel_slump, {"@root": [0, -5.3, -0.4], "root": [12, 0, 0], "body": [50, 0, 6]}), E_IO)
    fallen = {"@root": [0, 2.0, -1.0], "root": [86, 0, 0], "body": [4, 0, 4], "head": [-20, 30, 0], "right_leg": [-6, 0, 2],
              "left_leg": [-6, 0, -2], "right_shin": [14, 0, 0], "left_shin": [10, 0, 0], "skirt": [-20, 0, 0], "skirt_back": [20, 0, 0],
              "%skirt": [1.0, 0.6, 1.1], "%skirt_back": [1.0, 0.6, 1.0], "right_arm": [-150, 0, 20], "left_arm": [-30, 0, -40],
              "right_forearm": [-20, 0, 0], "left_forearm": [-40, 0, 0], "right_fingers": [40, 0, 0], "left_fingers": [40, 0, 0], "jaw": [0, 0, 0]}
    dt.key(6.6, fallen, E_IN)
    dt.key(8.0, fallen)
    dt.done()

    # Emerge: she rises out of the earth (from 30 px under it), hands first, then straightens and looks at you.
    em = Clip(f, A + "emerge", 5.0, hold=True)
    reach = {"right_arm": [-170, 0, 12], "left_arm": [-170, 0, -12], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
             "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0], "head": [-30, 0, 0], "body": [-4, 0, 0]}
    em.key(0, merge(reach, {"@root": [0, -30, 0]}))
    em.key(1.2, merge(reach, {"@root": [0, -18, 0], "right_arm": [-160, 0, 26],
                              "left_arm": [-150, 0, -30]}), E_OUT)
    em.key(2.6, merge(reach, {"@root": [0, -4, 0], "body": [20, 0, 0],
                              "head": [10, 0, 0], "right_arm": [-90, 0, 40], "left_arm": [-90, 0, -40]}), E_OUT)
    em.key(3.4, merge(hunch, {"@root": [0, -1.0, 0]}), E_IO)
    em.key(4.3, merge(stand_open, {"head": [-8, 0, 0], "jaw": [0, 0, 0]}), E_OUT)
    em.key(5.0, merge(IDLE, {"head": [-4, 0, 0]}), E_IO)
    em.done()
    return f


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=39000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    write_compact(anims(), ANIM + NAME + ".animation.json")


if __name__ == "__main__":
    generate()
