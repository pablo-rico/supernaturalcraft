"""Castiel and Godstiel (v0.39): one rig for the story NPC and the boss, four textures (the NPC, the new God, his glowmask and
the soaked phase V skin) and every clip. Contract: GodstielAssets (paths, clips and their lengths, the bones the code reads or
toggles).

Castiel: a slim man of average height (32 px to the crown). A long beige trench coat, open, its tails to just below the knee:
broad notched lapels laid back, a big collar turned up behind the neck, epaulettes, a storm flap on his right breast, double
rows of buttons down the open fronts, a belt hanging loose through its loops, strapped cuffs. Under it a dark navy suit, a
white shirt and a loose blue tie, its knot pulled down and askew. Dark messy hair, stubble, blue eyes, a level stare.

Godstiel wears the same clothes over paler skin, light under it. The bones the renderer shows by phase (GodstielAssets.TOGGLED)
carry everything that changes: the faint halo, shadow wings of smoke with glowing rims, three sets of light cracks, the
Leviathans' black veins, a maw tearing the shirt open (`chest_mouth` and its hinged `chest_jaw`), the face split down to the
collar (`face_mouth`) and two goo tendrils from under the coat.

A toggled set that spans several limbs has one bone per limb: the bone named in the contract plus parts named after it
(`cracks_1_face`, `cracks_1_right_hand`, ...), listed in GodstielAssets.TOGGLED_PARTS. The renderer hides a toggled bone and
its parts together.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- torso -- head -- eyes, brows, hair, face_mouth, halo, cracks_N_face, veins_face
                        -- tie, collar, chest_mouth -- chest_jaw, cracks_N, veins
                        -- wings -- wing_right -- wing_right_tip;  wing_left -- wing_left_tip
                        -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_middle, right_thumb,
                                                                       cracks_N_right_hand, veins_right_hand
                        -- left_arm  -- (the same)
               -- coat_tail_right, coat_tail_left
               -- tentacle_right -- tentacle_right_2 -- _3 -- _4 -- _5   (left the same)
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math
import random

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, composite, h01, none_on, solid
from common import ASSETS, save
from leviathan_art import GOO, GUM, TOOTH, gum_m, tooth_m
from michael_art import Clip, P_, add_p, fabric, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "castiel"
A = "animation.castiel."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

# --- proportions (slim, average height: 32 px to the crown) -------------------------------------------------------
HIP = 12.0
WAIST = 13.0      # the torso bends here
NECK = 24.0
TW = 3.9          # half the torso's width (slim)
TD = 2.0          # half its depth
AW = 3.4          # an arm's width
AX = TW + AW / 2  # the arm's centre line
SX = 5.0          # the shoulder pivot
SY = 22.4
ELBOW = 17.8
WRIST = 13.4
PALM = 11.5
LX = 1.95
LW = 3.8
KNEE = 6.2
SHOE_H = 1.4
HEM = 4.8         # the coat's hem, just below the knee
BELT_Y = 15.6     # the coat's belt

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#5a3a2c", "#8a5f4b", "#b07f67", "#c99a80", "#dcb398", "#ecc9b0")       # Castiel's (allies_art)
SKIN_GOD = Ramp("#5e5452", "#8c7f7d", "#b5a8a5", "#cfc4c1", "#e2dad8", "#f3eeed")   # the vessel drained pale
HAIR = Ramp("#0b0807", "#15100d", "#201813", "#2b2019", "#382a20", "#463428")
TRENCH = Ramp("#5e4f37", "#7f6c4c", "#9c8762", "#b49e77", "#c8b38c", "#dac7a2")
SUIT = Ramp("#080b15", "#0e1424", "#151e35", "#1d2945", "#283757", "#36486e")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
TIE = Ramp("#0d1b3a", "#142a57", "#1d3a75", "#284c93", "#3a63ad", "#557fc4")
SHOE = Ramp("#050505", "#0d0d0e", "#151517", "#1e1e21", "#29292d", "#4a4a52")
BUTTON = Ramp("#2a1c10", "#3c2a18", "#523a22", "#6a4c2e", "#86623e", "#a07a52")
LIPS = Ramp("#4a2a22", "#6a3c30", "#84503f", "#9a5f4c", "#ae705c", "#c08470")
EYE_WHITE = hexc("#e6e1d8")
IRIS = Ramp("#0f2a5c", "#174087", "#2560b0", "#3f8fe0", "#7ab8f2", "#c6e4ff")
# The light: Purgatory's souls, blue-white; the halo's pale gold.
SOUL = Ramp("#2a5ea8", "#3f86d6", "#6fb2f2", "#a8d6ff", "#d6efff", "#ffffff")
HALO = Ramp("#7a6230", "#b0924a", "#d8bd72", "#efd99c", "#fbefc8", "#fffaf0")
SMOKE = Ramp("#07080d", "#0f1119", "#181b27", "#232838", "#30374b", "#414a62")
OIL = hexc("#5b6a8a")             # the goo's wet sheen
OIL_HUE = (hexc("#4a6a7a"), hexc("#6a4a7a"), hexc("#5a7a5a"))   # oily iridescence


def tone(ramp, v):
    return ramp[max(0, min(len(ramp) - 1, int(round(v))))]


SIDES = (("right", -1), ("left", 1))


# =====================================================================================================
# Looks: one geometry, three skins
# =====================================================================================================

class Look:
    """The NPC, the boss and the drowned boss of phase V share every cube; the look decides the colours."""

    def __init__(self, name):
        self.name = name
        self.god = name in ("boss", "soaked")
        self.soaked = name == "soaked"
        self.skin_ramp = SKIN_GOD if self.god else SKIN

    def wet(self, mat, seed, skin=False, veins=True):
        """Phase V: everything soaked black in the reservoir's water, an oily sheen on it, the veins all over."""
        if not self.soaked:
            return mat

        def m(face, x, y, w, h):
            c = mat(face, x, y, w, h)
            if c is None or not c[3]:
                return c
            lum = (c[0] * 0.3 + c[1] * 0.55 + c[2] * 0.15) / 255
            n = fbm(x * 1.3 + len(face) * 9, y * 1.3, seed, 64, 64, 3, 6.0)
            base = tone(GOO, 1.2 + lum * 2.6 + (n - 0.5) * 1.2)
            # A hint of what it was underneath.
            out = mix(base, c, 0.34 if skin else 0.12)
            # Runnels of wet goo down the cloth: vertical streaks catching the light.
            streak = fbm(x * 3.0 + len(face) * 13, y * 0.25, seed + 3, 64, 64, 2, 5.0)
            if streak > 0.68:
                hue = OIL_HUE[int(h01("hue", seed, face, x // 3) * 3)]
                out = mix(out, mix(OIL, hue, 0.5), 0.35 + (streak - 0.68) * 1.5)
            if face == "up":
                out = mix(out, OIL, 0.25)
            if veins:
                v = fbm(x + len(face) * 11, y, seed + 7, 48, 48, 3, 8.0)
                if abs(v - 0.6) < 0.018:
                    out = GOO[0]
                elif abs(v - 0.6) < 0.035:
                    out = mix(out, GOO[0], 0.55)
            return (out[0], out[1], out[2], c[3])
        return m

    def skin(self, seed, base=3.0, amp=0.5):
        mat = H.skin_mat(self.skin_ramp, seed, base, amp)
        if self.god and not self.soaked:
            # Light just under the skin: a faint cool bloom in the mottling.
            def lit(face, x, y, w, h):
                c = mat(face, x, y, w, h)
                n = fbm(x * 2 + len(face) * 5, y * 2, seed + 11, 48, 48, 2, 5.0)
                return mix(c, SOUL[4], 0.1 * max(0.0, n - 0.45) * 2)
            return lit
        return self.wet(mat, seed, skin=True)


# =====================================================================================================
# Small painting helpers
# =====================================================================================================

def crack_lines(seed, w, h, count, length=(5, 11), branch=0.35):
    """Hairline cracks across a w x h face: 4-connected jagged walks (one texel wide) that sometimes fork. `count` may be
    fractional (the rest is a chance of one more). Returns {(x, y): strength 0..1}, 1 where a crack starts."""
    rng = random.Random(seed)
    pts = {}
    n = int(count) + (1 if rng.random() < count - int(count) else 0)

    def walk(x, y, main, n, s):
        side = (main[1], main[0])
        for _ in range(n):
            if 0 <= x < w and 0 <= y < h:
                pts[(x, y)] = max(pts.get((x, y), 0.0), s)
            r = rng.random()
            step = main if r < 0.55 else side if r < 0.78 else (-side[0], -side[1])
            if rng.random() < branch * 0.18 and n > 3:
                walk(x, y, side if rng.random() < 0.5 else (-side[0], -side[1]), max(2, n // 2), s * 0.75)
            x, y = x + step[0], y + step[1]
            s *= 0.93
    for _ in range(n):
        main = rng.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
        walk(rng.randrange(w), rng.randrange(h), main, rng.randint(*length), 1.0)
    return pts


def crack_decal(seed, count, length=(5, 11), faces_off=()):
    """A decal material of glowing cracks (transparent elsewhere) and its glow. `count` cracks per 16 x 16 texels."""
    cache = {}

    def field(face, w, h):
        key = (face, w, h)
        if key not in cache:
            cache[key] = crack_lines(seed + len(face) * 31 + w * 7 + h, w, h, count * (w * h) / 256, length)
        return cache[key]

    def mat(face, x, y, w, h):
        if face in faces_off:
            return None
        s = field(face, w, h).get((x, y))
        if s is None:
            return None
        c = mix(SOUL[1], SOUL[3], s)
        return (c[0], c[1], c[2], 255)

    def glow(face, x, y, w, h, c):
        return mix(c, SOUL[0], 0.3)
    return mat, glow


def vein_decal(seed, amount=0.022):
    """Black goo veins: thin ridges of a noise field, transparent elsewhere. Never glow."""
    def mat(face, x, y, w, h):
        n = fbm(x + len(face) * 11, y, seed, 48, 48, 3, 7.0)
        d = abs(n - 0.58)
        if d < amount:
            return GOO[1]
        if d < amount * 1.8 and h01("vein", seed, face, x, y) < 0.6:
            return (GOO[3][0], GOO[3][1], GOO[3][2], 200)
        return None
    return mat


def goo_mat(seed, base=1.8):
    """Glossy black goo with an oily, faintly iridescent sheen."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face) * 3, y * 2, seed, 48, 48, 2, 5.0)
        c = tone(GOO, base + (n - 0.5) * 2.2)
        if face == "up" or (n > 0.66 and (x + y) % 3 == 0):
            c = mix(c, OIL, 0.4 if face != "up" else 0.28)
        if n > 0.78:
            c = mix(c, OIL_HUE[int(h01("oil", seed, x, y) * 3)], 0.3)
        return c
    return m


# =====================================================================================================
# Geometry
# =====================================================================================================

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("torso", (0, WAIST, 0), "body")
    m.bone("head", (0, NECK, 0), "torso")
    m.bone("eyes", (0, NECK + 3.9, -4.0), "head")
    m.bone("brows", (0, NECK + 4.9, -4.0), "head")
    m.bone("hair", (0, NECK + 7.0, 0), "head")
    m.bone("face_mouth", (0, NECK + 1.4, -3.4), "head")
    m.bone("halo", (0, NECK + 10.0, 0.4), "head", rotation=(-14, 0, 0))
    m.bone("tie", (0, NECK - 2.4, -TD - 0.45), "torso")
    m.bone("collar", (0, NECK - 0.4, 0.6), "torso")
    m.bone("chest_mouth", (0, WAIST + 6.6, -TD - 0.6), "torso")
    m.bone("chest_jaw", (0, WAIST + 5.5, -TD - 0.35), "chest_mouth")
    m.bone("wings", (0, NECK - 3.0, TD + 0.8), "torso")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (SX * s, SY, 0), "torso")
        m.bone(f"{side}_forearm", (AX * s, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, PALM, 0.3), f"{side}_hand")
        m.bone(f"{side}_index", (AX * s, PALM, -1.0), f"{side}_hand")
        m.bone(f"{side}_middle", (AX * s, PALM, -0.35), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 0.9 * s, PALM + 1.2, -1.2), f"{side}_hand")
    for side, s in SIDES:
        m.bone(f"coat_tail_{side}", (s * 1.8, WAIST + 0.2, 0), "body")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (LX * s, HIP, 0), "root")
        m.bone(f"{side}_shin", (LX * s, KNEE, 0), f"{side}_leg")


# --- the head ---------------------------------------------------------------------------------------------------------

# Hair: how far down the front each column of the fringe reaches (rows of the 16-texel face), messy and uneven.
FRINGE = [6, 6, 5, 4, 5, 4, 3, 4, 3, 4, 4, 3, 5, 4, 6, 6]


def face_texel(look, sk, hr, x, y):
    """The face (north side of the skull, 16 x 16; x = 0 is the viewer's left, his right)."""
    stub_d = mix(look.skin_ramp[2], HAIR[2], 0.42)
    stub_l = mix(look.skin_ramp[3], HAIR[3], 0.3)
    c = None
    if y < FRINGE[x]:
        c = hr("north", x, y, 16, 16)
        if y == FRINGE[x] - 1 and x not in (0, 15):
            c = shade(c, 0.8)               # the fringe's tips are in shadow over the brow
        return c
    s = sk("north", x, y, 16, 16)
    # Under the fringe a soft shadow on the forehead.
    if y == FRINGE[x] and x not in (0, 15):
        s = shade(s, 0.86)
    # Brows: dark and straight, the inner ends lower (the level, searching stare).
    if y == 6 and (2 <= x <= 6 or 9 <= x <= 13):
        return HAIR[1] if x in (5, 6, 9, 10) else HAIR[2]
    if y == 5 and x in (2, 3, 12, 13):
        return mix(s, HAIR[2], 0.35)
    # The eyes (the shell in `eyes` covers rows 7..8; painted here too for the profile's sake).
    if y in (7, 8) and (2 <= x <= 5 or 10 <= x <= 13):
        ex = x - 2 if x <= 5 else x - 10
        if ex in (1, 2):
            return IRIS[2 if y == 7 else 3]
        return mix(EYE_WHITE, s, 0.35) if y == 7 else EYE_WHITE
    if y == 7 and x in (1, 6, 9, 14):
        return shade(s, 0.78)               # lid creases
    if y == 9 and (2 <= x <= 5 or 10 <= x <= 13):
        return shade(s, 0.88)               # tired shadows under the eyes
    # The nose: a lit bridge, nostrils.
    if x in (7, 8) and 7 <= y <= 10:
        return shade(s, 1.08 if x == 7 else 0.97)
    if y == 11 and x in (6, 9):
        return shade(s, 0.74)
    if y == 11 and x in (7, 8):
        return shade(s, 0.9)
    # Cheekbones catch the light; the hollows under them are in shade.
    if y == 10 and x in (2, 3, 12, 13):
        s = shade(s, 1.05)
    if y in (11, 12) and x in (1, 2, 13, 14):
        s = shade(s, 0.9)
    # The mouth: a thin, straight, unsmiling line, the lower lip a touch fuller.
    if y == 13 and 5 <= x <= 10:
        return LIPS[1] if 6 <= x <= 9 else LIPS[2]
    if y == 14 and 6 <= x <= 9:
        return mix(LIPS[3], s, 0.4)
    # Stubble: a light shadow on the jaw, the chin and the upper lip, thinning up the cheeks.
    stubble = 0.0
    if y >= 12 or (y == 11 and 4 <= x <= 11):
        stubble = 0.9
    elif y == 11 and (x <= 2 or x >= 13):
        stubble = 0.6
    elif y == 10 and (x <= 0 or x >= 15):
        stubble = 0.5
    if stubble and h01("stubble", x, y) < stubble:
        return mix(s, HAIR[2], 0.32 if h01("stubble2", x, y) < 0.4 else 0.2)
    return s


def build_head(m, look, seed=39001):
    sk = look.skin(seed)
    hr = look.wet(hair_mat(seed + 1), seed + 1, veins=False)

    def skull(f, x, y, w, h):
        if f == "north":
            return face_texel(look, sk, hr, x, y)
        if f == "up":
            return hr(f, x, y, w, h)
        if f == "down":
            return shade(sk(f, x, y, w, h), 0.9)
        if f == "south":
            return hr(f, x, y, w, h) if y < h - 2 else sk(f, x, y, w, h)
        # The side of the head (fx = 0 at the front): it must read as a profile, never as a second face. Hair over the top
        # and the whole back half down to the nape, a short sideburn, the ear set in the hair line, and only a narrow strip
        # of skin at the front (temple, cheek, jaw) kept darker than the face so the two planes separate.
        fx = x if f == "west" else w - 1 - x
        if 6 <= fx <= 8 and 6 <= y <= 10:
            c = shade(sk(f, x, y, w, h), 0.8)
            return shade(c, 0.78) if fx == 7 and 7 <= y <= 9 else c          # the ear
        if y < 5 or (fx >= 8 and y < 13) or (fx >= 6 and y < 6) or (fx in (4, 5) and y < 11):
            return hr(f, x, y, w, h)
        if y >= 11 and fx < 8:
            return shade(mix(sk(f, x, y, w, h), HAIR[2], 0.3), 0.82)          # the stubbled jaw
        return shade(sk(f, x, y, w, h), 0.86)
    m.cube("head", (-4, NECK, -4), (8, 8, 8), skull, density=2, tag="skull")
    m.cube("head", (-0.7, NECK + 2.3, -4.55), (1.4, 2.2, 0.55),
           lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.06 if f == "north" and y < 3 else 0.88 if f == "down" else 0.97),
           density=2, tag="nose")
    m.cube("torso", (-1.8, NECK - 1.2, -1.8), (3.6, 1.6, 3.6), sk, density=2, tag="neck")
    _hair(m, look, hr, seed + 2)


def hair_mat(seed):
    """Dark, messy: short strands going every which way, a dull sheen on the tufts."""
    def m(face, x, y, w, h):
        n = fbm(x * 1.6, y * 1.6 + len(face) * 3, seed, 64, 64, 3, 7.0)
        v = 1.6 + (n - 0.5) * 2.0
        r = h01("strand", seed, face, x, y)
        if r < 0.12:
            v += 1.4                                   # a lit strand
        elif r < 0.22:
            v -= 0.8
        return tone(HAIR, v)
    return m


def _hair(m, look, hr, seed):
    """Messy: a cap over the crown and the back, a ragged fringe, tufts sticking up and out."""
    def cap(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            # Only the ragged lower edge of the fringe pokes below the cap's front.
            return hr(f, x, y, w, h) if y < h - 1 or h01("rag", x) < 0.5 else None
        return hr(f, x, y, w, h)
    m.cube("hair", (-4, NECK + 6.0, -4), (8, 2.0, 8), cap, inflate=0.22, density=2, tag="hair_cap")
    m.cube("hair", (-4, NECK + 2.5, 1.0), (8, 3.5, 3.0), none_on(("up", "down", "north"), hr), inflate=0.2, density=2,
           tag="hair_back")
    # Tufts: little wedges of hair at odd angles over the crown and at the front (never combed).
    tufts = [(-2.6, -3.0, 1.4, -26, 0, 20), (-0.4, -3.3, 1.6, -32, 8, -4), (1.9, -2.9, 1.4, -22, -6, -22),
             (-2.2, -0.2, 1.2, 12, 0, 26), (1.4, 0.4, 1.2, 16, 0, -24), (0.1, 2.2, 1.2, 28, 0, 6), (3.0, -1.0, 1.0, -6, 0, -36),
             (-3.2, 1.8, 1.0, 20, 0, 32)]
    for i, (x, z, ln, rx, ry, rz) in enumerate(tufts):
        m.cube("hair", (x - 0.8, NECK + 7.8, z - 0.8), (1.6, ln * 0.6, 1.6),
               none_on(("down",), lambda f, xx, yy, w, h, i=i: shade(hr(f, xx + i * 3, yy, w, h), 1.12 if f == "up" else 1.0)),
               density=2, tag="hair_tuft", rotation=(rx, ry, rz), pivot=(x, NECK + 7.9, z))
    # A lock falling over his right brow.
    m.cube("hair", (-2.9, NECK + 5.6, -4.32), (2.2, 1.0, 0.45), none_on(("up",), hr), density=2, tag="forelock",
           rotation=(0, 0, -14), pivot=(-1.8, NECK + 6.5, -4.1))


def build_eyes(m, look):
    """The eyes' shell: two texel rows over the face's, painted as the eyes and, on the boss, lit soul-blue."""
    god = look.god

    def shell(f, x, y, w, h):
        col = x % 8 - 2 if x < 8 else x - 10
        if not 0 <= col <= 3 or (x < 8 and x > 5):
            return None
        iris = col in (1, 2)
        if god:
            if iris:
                return SOUL[5] if y == 1 and col == 2 else SOUL[4] if y == 1 else SOUL[3]
            return SOUL[2] if y == 0 else SOUL[3]
        if iris:
            if y == 1 and col == 2:
                return IRIS[5]                     # the catchlight
            return IRIS[2] if y == 0 else IRIS[3]
        return mix(EYE_WHITE, look.skin_ramp[2], 0.45) if y == 0 else EYE_WHITE
    glow = (lambda f, x, y, w, h, c: c) if god else None
    m.cube("eyes", (-3.0, NECK + 3.5, -4.07), (6.0, 1.0, 0), shell, glow, faces=("north",), density=2, tag="eyes")
    # Brows as a separate bone: heavy, straight, the inner ends lowered.
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.2, NECK + 4.85, -4.12), (2.4, 0.5, 0.25), solid(HAIR[1]), density=4, tag="brow",
               rotation=(0, 0, 7 * s), pivot=(s * 0.9, NECK + 5.0, -4.1))


# --- the suit, the shirt, the tie --------------------------------------------------------------------------------------

def build_suit(m, look, seed=39200):
    shirt = look.wet(fabric(SHIRT, seed + 1, 3.6, 0.2), seed + 1)
    suit = look.wet(fabric(SUIT, seed + 2, 3.0, 0.3), seed + 2)
    tie_r = TIE
    m.cube("torso", (-TW, WAIST - 0.4, -TD), (2 * TW, NECK - WAIST + 0.4, 2 * TD),
           lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else suit(f, x, y, w, h), density=2, tag="torso")
    front = H.jacket_front(suit, look.wet(solid(SUIT[4]), seed)( "north", 0, 0, 1, 1), shirt, tie=None, gap=(5, 11),
                           v_depth=9, buttons=SUIT[0], button_rows=(13, 17))
    m.cube("torso", (-TW, WAIST - 0.6, -TD), (2 * TW, NECK - WAIST - 0.6, 2 * TD), none_on(("up", "down"), front), inflate=0.25,
           density=2, tag="jacket")
    # The pelvis: trousers and a belt, on the body so the torso can bend over it.
    tr = look.wet(fabric(SUIT, seed + 3, 2.8, 0.2), seed + 3)

    def pelvis(f, x, y, w, h):
        if y <= 1 and f != "down":
            return look.wet(lambda *a: SHOE[2] if (x + y) % 5 else SHOE[4], seed)(f, x, y, w, h)
        return tr(f, x, y, w, h)
    m.cube("body", (-TW + 0.1, HIP - 1.2, -TD + 0.1), (2 * TW - 0.2, 2.6, 2 * TD - 0.2), pelvis, density=2, tag="pelvis")
    # The shirt collar: open at the throat, one point flipped up (he never quite gets it right).
    for s in (-1, 1):
        m.cube("torso", (0.95 * s - 0.8, NECK - 1.5, -TD - 0.45), (1.6, 1.3, 0.4),
               lambda f, x, y, w, h: tone(SHIRT, 4.4 if y == 0 else 3.6), density=2, tag="shirt_collar",
               rotation=(14 if s < 0 else -12, 0, -30 * s), pivot=(0.95 * s, NECK - 0.2, -TD - 0.3))
    # The tie: the knot pulled loose and down, askew to his left; the blade swinging off the centre line.
    tie = look.wet(lambda f, x, y, w, h: tone(tie_r, 2.6 + (1.2 if (x + y * 2) % 7 == 0 else 0) + (0.8 if x == 1 else 0)
                                              - (0.9 if y >= h - 2 else 0)), seed + 4)
    m.cube("tie", (-0.75, NECK - 3.2, -TD - 0.62), (1.5, 1.2, 0.5), lambda f, x, y, w, h: tone(tie_r, 3.6 if f != "up" else 4.2),
           density=4, tag="tie_knot", rotation=(0, 0, 14), pivot=(0, NECK - 2.6, -TD - 0.45))
    m.cube("tie", (-0.7, NECK - 9.6, -TD - 0.5), (1.4, 6.6, 0.3), tie, density=4, tag="tie_blade", rotation=(-4, 0, 9),
           pivot=(0.3, NECK - 3.1, -TD - 0.4))
    # The tie's tail, the narrow end, peeking out askew behind the blade.
    m.cube("tie", (-0.35, NECK - 6.2, -TD - 0.38), (0.7, 2.6, 0.2), lambda f, x, y, w, h: tone(tie_r, 1.8), density=4,
           tag="tie_tail", rotation=(0, 0, -6), pivot=(0, NECK - 3.4, -TD - 0.4))


# --- the trench coat ---------------------------------------------------------------------------------------------------

def trench_mat(look, seed, base=3.0):
    """Beige gabardine: a fine diagonal twill, soft vertical folds, worn darker at the seams."""
    cl = fabric(TRENCH, seed, base, 0.3)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and (x + y) % 4 == 0:
            c = shade(c, 0.95)
        if h01("weave", seed, f, x, y) < 0.05:
            c = shade(c, 1.06)
        return c
    return look.wet(m, seed)


def build_coat(m, look, seed=39300):
    coat = trench_mat(look, seed)
    dark = look.wet(lambda f, x, y, w, h: TRENCH[1], seed)
    lite = look.wet(lambda f, x, y, w, h: TRENCH[4], seed)
    btn = look.wet(lambda f, x, y, w, h: BUTTON[3] if (x + y) % 2 else BUTTON[2], seed)

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            if x < 4 or x > w - 5:
                if x in (3, w - 4):
                    return lite(f, x, y, w, h)              # the fronts' edges catching the light
                if x in (1, w - 2) and y % 5 == 3 and y > 6:
                    return btn(f, x, y, w, h)               # the double-breasted rows
                # The storm flap on his right breast: its edge and a button.
                if x < 4 and y in (6, 7) and x <= 3:
                    return dark(f, x, y, w, h) if y == 7 else lite(f, x, y, w, h)
                return coat(f, x, y, w, h)
            return None
        if f == "south" and 4 <= y <= 8:
            # The back yoke (the rain cape over the shoulders): its hem.
            return dark(f, x, y, w, h) if y == 8 else coat(f, x, y, w, h)
        return coat(f, x, y, w, h)
    m.cube("torso", (-TW, WAIST + 0.2, -TD), (2 * TW, NECK - WAIST - 0.4, 2 * TD), shell, inflate=0.6, density=2, tag="coat_shell")
    # The belt round the waist through its loops (the buckle hangs open at his left hip), the ends dangling.
    belt = look.wet(lambda f, x, y, w, h: TRENCH[2] if y in (0, h - 1) else TRENCH[3], seed + 1)
    m.cube("torso", (-TW, BELT_Y, -TD), (2 * TW, 1.0, 2 * TD),
           lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 4 <= x < w - 4) else belt(f, x, y, w, h),
           inflate=0.75, density=2, tag="coat_belt")
    for s in (-1, 1):
        m.cube("torso", (s * (TW - 0.4) - 0.5, BELT_Y - 3.2, -TD - 1.05), (1.0, 3.6, 0.3), belt, density=2, tag="belt_end",
               rotation=(-3, 0, 6 * s), pivot=(s * (TW - 0.4), BELT_Y + 0.4, -TD - 0.9))
    # Broad notched lapels laid back over the fronts.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return dark(f, x, y, w, h)
            c = tone(TRENCH, 4.0 - 1.4 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                return TRENCH[5]
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return TRENCH[2]                            # the roll line in shadow
            return c
        m.cube("torso", (3.3 * s - 1.4, NECK - 8.8, -TD - 1.0), (2.8, 8.0, 0.4), look.wet(lapel, seed + 5), density=2, tag="coat_lapel",
               rotation=(4, 0, 9 * s), pivot=(3.0 * s, NECK - 8.8, -TD - 0.8))
        m.cube("torso", (3.9 * s - 1.0, NECK - 1.6, -TD - 1.1), (2.0, 1.1, 0.4), lite, density=2, tag="coat_notch",
               rotation=(0, 0, -32 * s), pivot=(3.9 * s, NECK - 1.1, -TD - 0.95))
        # Epaulettes: a strap over each shoulder, buttoned at the collar end.
        m.cube("torso", (s * (TW + 0.35) - 1.1, NECK - 0.75, -1.2), (2.2, 0.5, 2.4),
               lambda f, x, y, w, h: btn(f, x, y, w, h) if f == "up" and (x == (1 if s < 0 else w - 2)) and y in (2, 3) else
               look.wet(lambda *a: TRENCH[4] if f == "up" else TRENCH[2], seed)(f, x, y, w, h), density=2, tag="epaulette")
    # The big collar, turned up behind the neck.
    def col(f, x, y, w, h):
        if f == "down":
            return TRENCH[1]
        return tone(TRENCH, 4.2 - 1.2 * y / max(1, h))
    col = look.wet(col, seed + 6)
    m.cube("collar", (-3.6, NECK - 1.4, 0.3), (7.2, 2.6, 0.6), col, density=2, tag="collar_back", rotation=(-8, 0, 0),
           pivot=(0, NECK - 0.4, 0.6))
    for s in (-1, 1):
        m.cube("collar", (s * 3.5 - 0.3, NECK - 1.4, -1.9), (0.6, 2.3, 2.6), col, density=2, tag="collar_side",
               rotation=(0, 0, -10 * s), pivot=(s * 3.5, NECK - 0.4, 0))
    # The skirt: each tail is a front panel, a side and half the back (the vent between them), from the waist down past
    # the knee, flaring a little.
    L = WAIST + 0.2 - HEM
    for side, s in SIDES:
        bone = f"coat_tail_{side}"

        def panel(f, x, y, w, h, s=s):
            if f == "south":
                return TRENCH[1]
            c = coat(f, x, y + 22, w, h + 22)
            inner = (x == w - 1) if s < 0 else (x == 0)
            if f == "north" and inner:
                return TRENCH[4]
            if f == "north" and y == h - 1:
                return TRENCH[1]
            if f == "north" and 2 <= y <= 3 and 1 <= (w - 1 - x if s < 0 else x) <= w - 3:
                return TRENCH[0] if y == 3 else TRENCH[4]   # the slash pocket's welt
            if f == "north" and (x == w - 2 if s < 0 else x == 1) and y in (1, 6) and y < h - 2:
                return BUTTON[3]
            return c
        xa, xb = (-TW - 0.75, -0.7) if s < 0 else (0.7, TW + 0.75)
        m.cube(bone, (xa, HEM, -TD - 0.8), (xb - xa, L, 0.5), look.wet(panel, seed + 10 + (s > 0)), density=2, tag="coat_front",
               rotation=(-5, 0, 4 * s), pivot=(s * 1.8, WAIST + 0.2, -TD - 0.55))
        xs = -TW - 0.8 if s < 0 else TW + 0.3
        m.cube(bone, (xs, HEM, -TD - 0.55), (0.5, L, 2 * TD + 0.9), none_on(("up",), coat), density=2, tag="coat_side",
               rotation=(0, 0, 4 * s), pivot=(xs, WAIST, 0))

        def back(f, x, y, w, h, s=s):
            if f == "north":
                return TRENCH[0]
            c = coat(f, x, y + 22, w, h + 22)
            vent = (x == 0) if s < 0 else (x == w - 1)
            if f == "south" and vent and y > h * 0.45:
                return TRENCH[1]
            if f == "south" and y == h - 1:
                return TRENCH[1]
            return c
        xa, xb = (-TW - 0.8, 0.0) if s < 0 else (0.0, TW + 0.8)
        m.cube(bone, (xa, HEM, TD + 0.25), (xb - xa, L, 0.5), look.wet(back, seed + 12 + (s > 0)), density=2, tag="coat_back",
               rotation=(5, 0, 2 * s), pivot=(0, WAIST + 0.2, TD + 0.4))


def build_arms(m, look, seed=39400):
    sleeve = trench_mat(look, seed, 2.9)
    hand = look.skin(seed + 1, 3.1, 0.4)
    cuff = look.wet(lambda f, x, y, w, h: tone(TRENCH, 3.4 - 0.8 * y / max(1, h)) if f not in ("up", "down") else TRENCH[0], seed)
    strap = look.wet(lambda f, x, y, w, h: TRENCH[2] if y in (0, h - 1) else TRENCH[4], seed + 2)
    shirt_cuff = look.wet(lambda f, x, y, w, h: tone(SHIRT, 4.0 - y), seed + 3)
    for side, s in SIDES:
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW - 0.2, -AW / 2), (AW, SY + 0.9 - ELBOW, AW), sleeve, inflate=0.35, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - AW / 2 + 0.1, WRIST + 0.5, -AW / 2 + 0.1), (AW - 0.2, ELBOW - WRIST - 0.3, AW - 0.2),
               none_on(("up",), sleeve), inflate=0.35, density=2, tag="forearm_sleeve")
        m.cube(f"{side}_forearm", (cx - AW / 2 + 0.1, WRIST + 0.3, -AW / 2 + 0.1), (AW - 0.2, 1.4, AW - 0.2), cuff, inflate=0.5,
               density=2, tag="cuff")
        # The cuff strap and its buckle on the outside of the sleeve.
        m.cube(f"{side}_forearm", (cx - 0.6 + s * (AW / 2 + 0.45), WRIST + 0.9, -AW / 2), (1.2 if s > 0 else 1.2, 0.5, AW),
               strap, density=4, tag="cuff_strap", rotation=(0, 0, 0))
        # The white shirt cuff peeking out below.
        m.cube(f"{side}_forearm", (cx - 1.4, WRIST - 0.1, -1.4), (2.8, 0.6, 2.8), shirt_cuff, inflate=0.1, density=2, tag="shirt_cuff")
        m.cube(f"{side}_hand", (cx - 1.2, PALM, -1.4), (2.4, WRIST - PALM + 0.2, 2.8), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.88 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_fingers", (cx - 1.15, PALM - 1.2, -0.1), (2.3, 1.2, 1.4), fing, density=2, tag="fingers")
        m.cube(f"{side}_middle", (cx - 1.15, PALM - 1.35, -0.75), (2.3, 1.35, 0.65), fing, density=4, tag="middle")
        m.cube(f"{side}_index", (cx - 1.15, PALM - 1.25, -1.4), (2.3, 1.25, 0.65), fing, density=4, tag="index")
        m.cube(f"{side}_thumb", (cx - 0.9 * s - 0.45, PALM + 0.2, -1.95), (0.9, 1.6, 0.85), hand, density=2, tag="thumb")


def build_legs(m, look, seed=39500):
    for side, s in SIDES:
        tr = look.wet(fabric(SUIT, seed + (s > 0), 2.9, 0.25, crease=True), seed + (s > 0))
        x0 = LX * s - LW / 2
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), tr, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, SHOE_H, -LW / 2), (LW, KNEE - SHOE_H, LW), tr, density=2, tag="shin")

        def shoe_m(f, x, y, w, h):
            c = tone(SHOE, 2.2 - 0.8 * y / max(1, h))
            if f == "up" or (f in ("north", "east", "west") and y == 0):
                c = SHOE[4]
            if f == "up" and (x + y) % 5 == 0:
                c = SHOE[5]
            return c
        m.cube(f"{side}_shin", (x0 - 0.1, 0, -2.8), (LW + 0.2, SHOE_H, 4.7), look.wet(shoe_m, seed + 7), density=2, tag="shoe")


# --- what the renderer shows by phase ----------------------------------------------------------------------------------

def build_halo(m):
    """A faint ring of light floating over his head: short arcs, translucent, lit in the glowmask."""
    cy, r = NECK + 10.0, 3.9
    n = 14

    def arc(f, x, y, w, h):
        core = f in ("up", "down") and y == h // 2
        c = HALO[4] if core else HALO[3] if f in ("up", "down") else HALO[2]
        return (c[0], c[1], c[2], 190 if core else 120)
    for i in range(n):
        a = 360.0 * i / n
        seg = 2 * math.pi * r / n + 0.25
        m.cube("halo", (-seg / 2, cy - 0.3, 0.4 - r - 0.35), (seg, 0.6, 0.7), arc, lambda f, x, y, w, h, c: shade((c[0], c[1], c[2], 255), 0.8),
               density=4, tag="halo", rotation=(0, a, 0), pivot=(0, cy, 0.4), shading=False)


CRACK_SETS = {1: dict(face=2.2, chest=2.4, hand=2.0, inflate=0.1, seed=39610),
              2: dict(face=2.2, chest=2.4, hand=3.0, inflate=0.14, seed=39620),
              3: dict(face=3.0, chest=3.2, hand=4.0, inflate=0.18, seed=39630)}


def build_cracks(m):
    """Three sets of hairline cracks of light; each set is the torso bone `cracks_N` (chest and neck) and its parts on the
    face and both hands. Every set adds to the ones before it."""
    for n, spec in CRACK_SETS.items():
        k, seed = spec["inflate"], spec["seed"]
        m.bone(f"cracks_{n}", (0, NECK - 3.0, 0), "torso")
        m.bone(f"cracks_{n}_face", (0, NECK + 4.0, 0), "head")
        mat, glow = crack_decal(seed, spec["face"], (4, 9), faces_off=("up", "down", "south"))
        m.cube(f"cracks_{n}_face", (-4, NECK, -4), (8, 8, 8), mat, glow, inflate=k, density=2, tag="cracks_face", shading=False)
        mat, glow = crack_decal(seed + 1, spec["chest"], (5, 12))
        # The chest: over the shirt and jacket in the coat's opening, and round the neck.
        m.cube(f"cracks_{n}", (-TW, WAIST + 1.0, -TD - 0.3 - k), (2 * TW, NECK - WAIST - 1.4, 0), mat, glow, faces=("north",),
               density=2, tag="cracks_chest", shading=False)
        m.cube(f"cracks_{n}", (-1.8, NECK - 1.2, -1.8), (3.6, 1.6, 3.6), mat, glow, inflate=k, density=2, tag="cracks_neck",
               shading=False)
        for side, s in SIDES:
            b = m.bone(f"cracks_{n}_{side}_hand", (AX * s, WRIST, 0), f"{side}_hand")
            mat, glow = crack_decal(seed + 3 + (s > 0), spec["hand"], (3, 6), faces_off=("up",))
            m.cube(b, (AX * s - 1.2, PALM, -1.4), (2.4, WRIST - PALM + 0.2, 2.8), mat, glow, inflate=k, density=2,
                   tag="cracks_hand", shading=False)


def build_veins(m):
    """The Leviathans under his skin: black veins over the face, the throat and chest, the hands. Never lit."""
    m.bone("veins", (0, NECK - 3.0, 0), "torso")
    m.bone("veins_face", (0, NECK + 4.0, 0), "head")
    m.cube("veins_face", (-4, NECK, -4), (8, 8, 8), vein_decal(39701, 0.024), inflate=0.07, density=2, tag="veins_face")
    m.cube("veins", (-1.8, NECK - 1.2, -1.8), (3.6, 1.6, 3.6), vein_decal(39702, 0.03), inflate=0.07, density=2, tag="veins_neck")
    m.cube("veins", (-TW, WAIST + 1.0, -TD - 0.28), (2 * TW, NECK - WAIST - 1.4, 0), vein_decal(39703, 0.022), faces=("north",),
           density=2, tag="veins_chest")
    for side, s in SIDES:
        b = m.bone(f"veins_{side}_hand", (AX * s, WRIST, 0), f"{side}_hand")
        m.cube(b, (AX * s - 1.2, PALM, -1.4), (2.4, WRIST - PALM + 0.2, 2.8), vein_decal(39704 + (s > 0), 0.032), inflate=0.06,
               density=2, tag="veins_hand")


def build_chest_mouth(m, look):
    """A Leviathan maw tearing the shirt open across his chest: ragged cloth round a ring of dark gums, a black throat, needle
    teeth above and on the hinged lower jaw (`chest_jaw`; +X opens it)."""
    gum = gum_m(39801)
    shirt = look.wet(fabric(SHIRT, 39802, 3.4, 0.2), 39802)
    y0, y1 = WAIST + 2.2, WAIST + 9.4
    zf = -TD - 0.62

    def hole(f, x, y, w, h):
        # An irregular, torn oval: the shirt's rags, the gums, the throat.
        cx, cy = (w - 1) / 2, (h - 1) / 2
        ang = math.atan2(y - cy, x - cx)
        rag = 0.12 * math.sin(ang * 7 + 1.3) + 0.08 * math.sin(ang * 13)
        d = math.hypot((x - cx) / (w / 2), (y - cy) / (h / 2)) - rag
        if d > 1.0:
            return None
        if d > 0.86:
            return shade(shirt(f, x, y, w, h), 0.7 if h01("rag", x, y) < 0.5 else 0.9)      # torn cloth, frayed
        if d > 0.66:
            return gum(f, x, y, w, h)
        if d > 0.52:
            return mix(gum(f, x, y, w, h), GOO[0], 0.6)
        return GOO[0] if d < 0.3 else GOO[1]
    m.cube("chest_mouth", (-3.4, y0, zf), (6.8, y1 - y0, 0), hole, faces=("north",), density=2, tag="maw_hole", shading=False)

    def lip(f, x, y, w, h):
        c = gum(f, x, y, w, h)
        if f in ("north", "down") and (y == h - 1 or f == "down") and x % 2 == 0:
            return TOOTH[4]
        return c
    m.cube("chest_mouth", (-2.7, WAIST + 6.5, zf - 0.9), (5.4, 1.4, 1.0), lip, density=2, tag="maw_upper")
    for i in range(7):
        x = -2.4 + i * 0.8
        ln = 1.3 if i in (1, 5) else 1.0 if i % 2 else 0.8
        m.cube("chest_mouth", (x - 0.2, WAIST + 6.5 - ln, zf - 0.75), (0.45, ln, 0.45), tooth_m, density=4, tag="maw_tooth",
               rotation=(0, 0, (i - 3) * 4), pivot=(x, WAIST + 6.5, zf - 0.5))
    for i in range(4):
        # A second row behind the first, the Leviathan way.
        x = -2.0 + i * 1.3
        m.cube("chest_mouth", (x - 0.2, WAIST + 6.5 - 0.9, zf - 0.25), (0.4, 0.9, 0.4), tooth_m, density=4, tag="maw_tooth")

    def jaw(f, x, y, w, h):
        c = gum(f, x, y, w, h)
        if f == "north" and y == 0 and x % 2 == 1:
            return TOOTH[4]
        if f == "down":
            return shade(shirt(f, x, y, w, h), 0.8)
        return c
    m.cube("chest_jaw", (-2.6, WAIST + 4.3, zf - 0.9), (5.2, 1.2, 1.0), jaw, density=2, tag="maw_lower")
    for i in range(6):
        x = -2.2 + i * 0.88
        ln = 1.1 if i in (0, 5) else 0.8
        m.cube("chest_jaw", (x - 0.2, WAIST + 5.5, zf - 0.75), (0.45, ln, 0.45), tooth_m, density=4, tag="jaw_tooth",
               rotation=(0, 0, (i - 2.5) * -5), pivot=(x, WAIST + 5.5, zf - 0.5))
    # A tongue of goo lolling on the lower jaw.
    m.cube("chest_jaw", (-1.0, WAIST + 5.3, zf - 0.6), (2.0, 0.4, 1.0), goo_mat(39803, 2.4), density=2, tag="maw_tongue")


def build_face_mouth(m, look):
    """Phase V: the jaw splits down to the collar. The lower face torn open (a decal over the mouth and chin) and the split
    jaw hanging below the chin in two halves, gums and teeth on their inner faces."""
    sk = look.skin(39901)
    gum = gum_m(39902)

    def gash(f, x, y, w, h):
        # A slit widening downward from under the nose to the chin's edge.
        cx = (w - 1) / 2
        t = y / max(1, h - 1)
        half = 1.2 + 4.2 * t ** 0.7
        d = abs(x - cx)
        if d > half + 0.6:
            return None
        if d > half - 0.4:
            return TOOTH[4] if (y + int(x)) % 2 == 0 else gum(f, x, y, w, h)
        if d > half - 1.4:
            return gum(f, x, y, w, h)
        return GOO[0]
    m.cube("face_mouth", (-3.5, NECK, -4.08), (7.0, 3.0, 0), gash, faces=("north",), density=2, tag="face_gash", shading=False)
    for s in (-1, 1):
        def half(f, x, y, w, h, s=s):
            inner = "east" if s < 0 else "west"
            if f == inner:
                c = gum(f, x, y, w, h)
                fx = w - 1 - x if f == "west" else x
                if fx <= 1 and y % 2 == 0:
                    return TOOTH[4]
                if y <= 1 and x % 2 == 0:
                    return TOOTH[3]
                return c
            if f == "up":
                return gum(f, x, y, w, h)
            c = sk(f, x, y, w, h)
            if h01("stub", f, x, y) < 0.7:
                c = mix(c, HAIR[2], 0.4)
            return c
        x0 = -2.6 if s < 0 else 0.6
        m.cube("face_mouth", (x0, NECK - 2.4, -4.3), (2.0, 2.8, 2.4), half, density=2, tag="face_jaw",
               rotation=(-10, 0, 18 * s), pivot=(s * 1.6, NECK + 0.4, -3.2))
        for i in range(3):
            m.cube("face_mouth", (s * 0.75 - 0.2, NECK - 2.0 + i * 0.85, -4.1), (0.4, 0.4, 0.9), tooth_m, density=4, tag="face_tooth",
                   rotation=(0, 0, 90), pivot=(s * 0.75, NECK - 1.8 + i * 0.85, -3.6))

    def throat(f, x, y, w, h):
        return GOO[0] if (x + y) % 5 else mix(GOO[1], GUM[2], 0.4)
    m.cube("face_mouth", (-0.6, NECK - 2.3, -3.6), (1.2, 2.6, 0), throat, faces=("north",), density=2, tag="face_throat", shading=False)


TENT_SEGS = [(4.6, 2.6), (4.4, 2.3), (4.2, 2.0), (4.0, 1.6), (3.8, 1.1)]
# Rest curl (right side; the left mirrors Y and Z): out from under the back of the coat, sweeping down and out, the tip
# curling up off the floor.
TENT_REST = [(40, 0, 30), (-18, 0, 16), (-22, 0, 14), (-24, 0, 12), (-26, 0, 10)]
TENTACLES = [f"tentacle_{side}" + ("" if i == 0 else f"_{i + 1}") for side, _ in SIDES for i in range(len(TENT_SEGS))]


def build_tentacles(m):
    """Two goo tendrils from under the back of the coat, five segments each (`tentacle_<side>`, `_2` .. `_5`), tapering to a
    hooked, toothed tip."""
    for side, s in SIDES:
        x, y, z = s * 2.0, HIP + 0.8, TD - 0.6
        parent = "body"
        for i, (ln, wd) in enumerate(TENT_SEGS):
            name = f"tentacle_{side}" + ("" if i == 0 else f"_{i + 1}")
            r = TENT_REST[i]
            m.bone(name, (x, y, z), parent, rotation=(r[0], r[1] * -s, r[2] * -s))
            mat = goo_mat(39950 + i * 3 + (s > 0))
            m.cube(name, (x - wd / 2, y - ln - 0.2, z - wd / 2), (wd, ln + 0.4, wd), mat, density=2, tag="tentacle")
            if i == len(TENT_SEGS) - 1:
                # The hooked tip and a crescent of small teeth.
                m.cube(name, (x - 0.35, y - ln - 1.1, z - 0.35), (0.7, 1.0, 0.7), goo_mat(39960, 2.6), density=4, tag="tentacle_tip",
                       rotation=(-30, 0, 0), pivot=(x, y - ln, z))
                for j in range(3):
                    m.cube(name, (x - 0.15 + (j - 1) * 0.35, y - ln + 0.1, z - wd / 2 - 0.4), (0.3, 0.6, 0.3), tooth_m, density=4,
                           tag="tentacle_tooth", rotation=(-50, 0, 0), pivot=(x, y - ln + 0.4, z - wd / 2))
            elif i >= 1:
                # Barbs along the outer side.
                m.cube(name, (x + s * wd / 2 - 0.2, y - ln * 0.6, z - 0.2), (0.4, 0.8, 0.4), tooth_m, density=4, tag="tentacle_barb",
                       rotation=(0, 0, 40 * s), pivot=(x + s * wd / 2, y - ln * 0.6, z))
            parent = name
            y -= ln


WING_OUT, WING_RISE = 28.0, 9.0
FLIGHT, COVERTS = 11, 6


def smoke_feather(seed, alpha=205, broad=False):
    """A feather of shadow: a dark smoky vane you can half see through, a pale soul-blue rim, wisps at the tip."""
    def half(t, w):
        hw = (w - 1) / 2 + 0.3
        if broad:
            return hw * (math.sqrt(max(0.0, 1 - ((t - 0.5) / 0.5) ** 2)) if t > 0.5 else min(1.0, 0.6 + t))
        if t < 0.1:
            return hw * (0.55 + 4.5 * t)
        return hw * (1.0 if t < 0.55 else max(0.08, 1 - (t - 0.55) / 0.45))

    def mat(face, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        hw = half(t, w)
        dx = abs(x - c)
        if dx > hw:
            return None
        if t > 0.72 and h01("wisp", seed, face, x, y) < (t - 0.72) * 1.8:
            return None
        n = fbm(x * 6 + len(face), y * 1.5, seed, 64, 64, 3, 5.0)
        q = dx / max(0.5, hw)
        a = alpha * (0.95 - 0.55 * q) * (1 - 0.45 * t) * (0.7 + 0.6 * n)
        col = mix(SMOKE[1 + int(n * 2.5)], SOUL[0], 0.08 + 0.2 * n * n)
        if q < 0.18 and t < 0.8:
            col, a = SMOKE[1], a + 30
        if dx > hw - 0.9:
            col, a = mix(SOUL[2], SOUL[4], n), max(a, 150)          # the rim of light
        tip = max(0.0, (t - 0.7) / 0.3)
        if tip > 0:
            col = mix(col, SOUL[3], min(1.0, tip))
            a = a * (1 - 0.3 * tip) + 40 * tip
        return (col[0], col[1], col[2], max(18, min(230, int(a))))

    def glow(face, x, y, w, h, col):
        return (col[0], col[1], col[2], 255) if col[2] > 150 else None
    return mat, glow


def build_wings(m):
    """Castiel's shadow wings: one great pair of smoke, the rims lit. Built spread (they are only seen while he judges or
    blazes); `wing_<side>_tip` folds the outer half. The clips sweep them open from folded behind his back."""
    from michael_art import edge_cube, mirror_rot
    for side, s in SIDES:
        root = (s * 1.4, NECK - 3.0, TD + 1.0)
        w = m.bone(f"wing_{side}", root, "wings", rotation=mirror_rot((0, 18, 14), s))
        x0, y0, z0 = root
        mid = (x0 + s * WING_OUT * 0.5, y0 + WING_RISE * 0.6, z0)
        tip = (x0 + s * WING_OUT, y0 + WING_RISE, z0)
        wt = m.bone(f"wing_{side}_tip", mid, w.name)
        seed = 39970 + (s > 0) * 50

        def arm_m(f, x, y, w_, h, s=s, seed=seed):
            t = y / max(1, h - 1)
            q = x / max(1, w_ - 1)
            if (f == "north") == (s < 0):
                q = 1 - q
            if t > 0.5 + 0.4 * (1 - q) or q > 0.98:
                return None
            n = fbm(x * 0.8, y * 4, seed, 64, 64, 3, 5.0)
            if t < 0.22:
                col = mix(SOUL[4], SOUL[2], q)
                return (col[0], col[1], col[2], int(200 - 300 * t))
            col = mix(SMOKE[2 + int(n * 2)], SOUL[0], 0.2 * n)
            return (col[0], col[1], col[2], int(225 - 60 * q))

        def arm_glow(f, x, y, w_, h, c):
            return (c[0], c[1], c[2], 255) if c[2] > 150 else None
        edge_cube(m, w, root, mid, 4.2, s, arm_m, arm_glow, tag="wing_arm", dz=-0.1)
        edge_cube(m, wt, mid, tip, 3.4, s, arm_m, arm_glow, tag="wing_arm", dz=-0.1)
        for i in range(FLIGHT):
            f = i / (FLIGHT - 1)
            t = 0.08 + 0.92 * f
            bone, a, b = (w, root, mid) if t <= 0.5 else (wt, mid, tip)
            u = t / 0.5 if t <= 0.5 else (t - 0.5) / 0.5
            px, py = a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u
            length = (15 + 13 * f) * (0.94 + 0.12 * h01("vl", seed, i))
            angle = 4 + 78 * f ** 1.2 + (h01("va", seed, i) - 0.5) * 6
            mat, glow = smoke_feather(seed + i)
            m.cube(bone, (px - 3.3, py - 0.4 - length, z0 + 0.1 * i), (6.6, length, 0), mat, glow, faces=("north", "south"),
                   density=2, tag="shadow_feather", rotation=(0, 0, -s * angle), pivot=(px, py - 0.4, z0 + 0.1 * i), shading=False)
        for j in range(COVERTS):
            f = j / (COVERTS - 1)
            t = 0.05 + 0.8 * f
            bone, a, b = (w, root, mid) if t <= 0.5 else (wt, mid, tip)
            u = t / 0.5 if t <= 0.5 else (t - 0.5) / 0.5
            px, py = a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u
            ln = 8 + 3 * f
            mat, glow = smoke_feather(seed + 30 + j, 225, broad=True)
            m.cube(bone, (px - 3.5, py - ln, z0 + 1.4), (7.0, ln, 0), mat, glow, faces=("north", "south"), density=2,
                   tag="shadow_covert", rotation=(0, 0, -s * (18 + 40 * f)), pivot=(px, py, z0 + 1.4), shading=False)


# =====================================================================================================
# The rig
# =====================================================================================================

def rig(look="boss"):
    lk = Look(look)
    m = Model(NAME, 512, 256)
    skeleton(m)
    build_head(m, lk)
    build_eyes(m, lk)
    build_suit(m, lk)
    build_coat(m, lk)
    build_arms(m, lk)
    build_legs(m, lk)
    build_halo(m)
    build_cracks(m)
    build_veins(m)
    build_chest_mouth(m, lk)
    build_face_mouth(m, lk)
    build_tentacles(m)
    build_wings(m)
    return m


def toggled_parts():
    """{toggled bone: the other bones that go with it} (GodstielAssets.TOGGLED_PARTS)."""
    out = {f"cracks_{n}": [f"cracks_{n}_face", f"cracks_{n}_right_hand", f"cracks_{n}_left_hand"] for n in CRACK_SETS}
    out["veins"] = ["veins_face", "veins_right_hand", "veins_left_hand"]
    return out


PHASE_HIDDEN = {
    "npc": ("halo", "wings", "cracks_1", "cracks_2", "cracks_3", "veins", "chest_mouth", "face_mouth", "tentacle_left",
            "tentacle_right"),
    "p1": ("wings", "cracks_1", "cracks_2", "cracks_3", "veins", "chest_mouth", "face_mouth", "tentacle_left", "tentacle_right"),
    "p3": ("halo", "wings", "cracks_2", "cracks_3", "veins", "face_mouth", "tentacle_left", "tentacle_right"),
    "p4": ("halo", "wings", "cracks_3", "face_mouth"),
    "p5": ("halo", "wings"),
}


def hidden_for(phase, wings=False):
    """Every bone hidden in a phase's look (a toggled bone with its parts), for previews."""
    parts = toggled_parts()
    out = []
    for b in PHASE_HIDDEN[phase]:
        if wings and b == "wings":
            continue
        out.append(b)
        out.extend(parts.get(b, ()))
    return tuple(out)


# =====================================================================================================
# Clips
# =====================================================================================================
#
# Base loops (idle, walk, hover, struggle_idle, kneel_idle) key the whole body. The one-shot attacks key only the upper body,
# the tentacles and the maws, never the root or the legs, so they play over `hover` as well as over `idle` or `walk`.

def jit(seed, t, amp, length, ks=(7, 11, 13)):
    """A seamless shake: integer-cycle sines (so a loop of `length` closes), per axis a different mix."""
    out = []
    for i in range(3):
        v = 0.0
        for j, k in enumerate(ks):
            ph = h01(seed, i, j) * 2 * math.pi
            v += math.sin(2 * math.pi * k * t / length + ph) / (j + 1)
        out.append(amp * v / 1.8)
    return out


def tent(right, left=None):
    """Tentacle deltas from absolute rotations per segment (the right tentacle's; the left mirrors Y and Z unless given)."""
    left = left or [(r[0], -r[1], -r[2]) for r in right]
    out = {}
    for side, s, rots in (("right", -1, right), ("left", 1, left)):
        for i, r in enumerate(rots):
            rest = TENT_REST[i]
            rest = (rest[0], rest[1] * -s, rest[2] * -s)
            name = f"tentacle_{side}" + ("" if i == 0 else f"_{i + 1}")
            out[name] = [r[k] - rest[k] for k in range(3)]
    return out


TENT_ALL = [f"tentacle_{side}" + ("" if i == 0 else f"_{i + 1}") for side, _ in SIDES for i in range(len(TENT_SEGS))]


def tent_wave(c, amp, cycles, length, lag=0.9, seed=0):
    """A slow undulation down both tentacles (additive layers; integer cycles so loops close)."""
    for side, s in SIDES:
        for i in range(len(TENT_SEGS)):
            name = f"tentacle_{side}" + ("" if i == 0 else f"_{i + 1}")
            k = (0.5 + 0.35 * i)

            def fn(t, i=i, s=s, k=k):
                ph = 2 * math.pi * cycles * t / length - i * lag + (0 if s < 0 else 1.7) + seed
                return [amp * k * math.sin(ph), 0, -s * amp * 0.7 * k * math.cos(ph * 0.5 + 0.4)]
            c.layer(name, fn)


def wing(right, tip=(0, 0, 0), scale=None):
    """Wing deltas from the built (spread) pose, right-wing values; the left mirrors Y and Z."""
    out = {"wing_right": list(right), "wing_left": [right[0], -right[1], -right[2]],
           "wing_right_tip": list(tip), "wing_left_tip": [tip[0], -tip[1], -tip[2]]}
    if scale is not None:
        out["%wing_right"] = [scale] * 3
        out["%wing_left"] = [scale] * 3
    return out


W_FOLD = wing((0, 64, -48), (0, 0, -70), 0.1)     # tucked flat behind the back, nearly nothing (the shadow gathering)
W_HALF = wing((0, 30, -10), (0, 0, -30), 0.8)
W_OPEN = wing((0, 0, 0), (0, 0, 0), 1.0)
W_WIDE = wing((0, -10, 14), (0, 0, 10), 1.12)      # flung wide and raised
W_UP = wing((0, 8, 36), (0, 0, 24), 1.05)          # raised over him for the downstroke


def sway(t, amp=1.0):
    """The coat's tails and the tie stirring (t in cycles)."""
    ph = t * 2 * math.pi
    return {"coat_tail_right": [3 * amp * math.sin(ph), 0, 1.5 * amp * math.sin(ph * 0.5)],
            "coat_tail_left": [3 * amp * math.sin(ph + 0.7), 0, -1.5 * amp * math.sin(ph * 0.5 + 0.4)],
            "tie": [-1.5 * amp * math.sin(ph + 1.1), 0, 1.5 * amp * math.sin(ph)]}


# --- the poses ---------------------------------------------------------------------------------------------------------

# At rest: stiff, arms at his sides, the head a little tilted (his way of studying you).
IDLE = {"right_arm": [-2, 0, 4], "left_arm": [-2, 0, -4], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
        "right_fingers": [22, 0, 0], "left_fingers": [22, 0, 0], "right_index": [12, 0, 0], "left_index": [12, 0, 0],
        "head": [-2, 0, 0]}
# Floating: feet a hand's breadth off the ground, toes down, arms a little away from him, palms open.
HOVER = {"@root": [0, 3.0, 0], "right_leg": [-6, 0, 2], "right_shin": [14, 0, 0], "left_leg": [3, 0, -2], "left_shin": [20, 0, 0],
         "right_arm": [-6, 0, 14], "left_arm": [-6, 0, -14], "right_forearm": [-16, 0, 0], "left_forearm": [-16, 0, 0],
         "right_hand": [0, -20, 0], "left_hand": [0, 20, 0], "right_fingers": [-6, 0, 0], "left_fingers": [-6, 0, 0],
         "head": [-6, 0, 0], "coat_tail_right": [10, 0, 6], "coat_tail_left": [10, 0, -6], "tie": [-12, 0, 4]}
# The inner fight: doubled over, both hands clawing at his temples, elbows forward.
STRUGGLE = {"torso": [26, 0, 0], "head": [18, 0, 0], "right_arm": [-140, -20, 40], "right_forearm": [-120, 0, 0],
            "left_arm": [-140, 20, -40], "left_forearm": [-120, 0, 0], "right_fingers": [55, 0, 0], "left_fingers": [55, 0, 0],
            "right_index": [40, 0, 0], "left_index": [40, 0, 0], "right_middle": [48, 0, 0], "left_middle": [48, 0, 0],
            "body": [6, 0, 0], "coat_tail_right": [-6, 0, 3], "coat_tail_left": [-6, 0, -3], "tie": [-20, 0, 0],
            "chest_jaw": [18, 0, 0]}
# Beaten: on both knees, sitting back a little, head bowed, hands limp on his thighs.
KNEEL = {"@root": [0, -4.5, 0], "right_leg": [-8, 0, 4], "left_leg": [-8, 0, -4], "right_shin": [98, 0, 0], "left_shin": [98, 0, 0],
         "body": [6, 0, 0], "torso": [10, 0, 0], "head": [26, 0, 0], "right_arm": [-20, 0, 6], "left_arm": [-20, 0, -6],
         "right_forearm": [-26, 0, 0], "left_forearm": [-26, 0, 0], "right_fingers": [30, 0, 0], "left_fingers": [30, 0, 0],
         "coat_tail_right": [24, 0, 10], "coat_tail_left": [24, 0, -10], "tie": [14, 0, 0]}


def anims():
    f = AnimFile()

    def clip(name, length, loop=False, hold=False):
        return Clip(f, A + name, length, loop=loop, hold=hold)

    # --- loops -------------------------------------------------------------------------------------------------------
    # idle: still; a slow breath; the head turns to study you and tilts (held), straightens, looks away and back.
    c = clip("idle", 4.0, loop=True)
    yaw = [0, 10, 6, -6, 0]
    roll = [0, 10, 10, 2, 0]
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(math.pi * t / 2)
        c.key(t, P_(IDLE, sway(t / 4.0, 0.5), {"@body": [0, -0.15 * abs(b), 0], "head": [-2 - abs(b), yaw[i], roll[i]],
                                                "right_forearm": [-10 - 2 * abs(b), 0, 0], "left_forearm": [-10 - 2 * abs(b), 0, 0]}),
              E_IO if i else None)
    tent_wave(c, 6, 1, 4.0)
    c.done()

    # walk: an unhurried, upright stride; the arms barely swing (he never quite learned how); the coat flaps behind.
    c = clip("walk", 1.2, loop=True)
    for t, s in ((0, 1), (0.6, -1), (1.2, 1)):
        c.key(t, {"right_leg": [24 * s, 0, 0], "left_leg": [-24 * s, 0, 0], "right_shin": [6 if s < 0 else 22, 0, 0],
                  "left_shin": [22 if s < 0 else 6, 0, 0], "right_arm": [-10 * s, 0, 4], "left_arm": [10 * s, 0, -4],
                  "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0], "right_fingers": [22, 0, 0], "left_fingers": [22, 0, 0],
                  "body": [0, 5 * s, 0], "torso": [-2, -3 * s, 0], "head": [-3, -3 * s, 0],
                  "coat_tail_right": [14 + 8 * s, 0, 3], "coat_tail_left": [14 - 8 * s, 0, -3], "tie": [-6, 0, 3 * s]},
              E_IO if t else None)
    c.anim.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.5, 0], E_OUT), (0.6, [0, 0, 0], E_IN), (0.9, [0, 0.5, 0], E_OUT),
               (1.2, [0, 0, 0], E_IN))
    tent_wave(c, 10, 2, 1.2, lag=1.2)
    c.done()

    # hover: hanging in the air, the coat drifting round his legs as if under water; a slow bob.
    c = clip("hover", 3.0, loop=True)
    c.key(0, HOVER)
    c.key(3.0, HOVER)
    c.layer("@root", lambda t: [0, 0.6 * math.sin(2 * math.pi * t / 3.0), 0])
    c.layer("coat_tail_right", lambda t: [5 * math.sin(2 * math.pi * t / 3.0 + 0.5), 0, 3 * math.sin(2 * math.pi * t / 1.5)])
    c.layer("coat_tail_left", lambda t: [5 * math.sin(2 * math.pi * t / 3.0 + 1.3), 0, -3 * math.sin(2 * math.pi * t / 1.5 + 0.8)])
    c.layer("tie", lambda t: [4 * math.sin(2 * math.pi * t / 3.0 + 2.0), 0, 5 * math.sin(2 * math.pi * t / 3.0)])
    c.layer("right_leg", lambda t: [3 * math.sin(2 * math.pi * t / 3.0 + 0.3), 0, 0])
    c.layer("left_leg", lambda t: [3 * math.sin(2 * math.pi * t / 3.0 + 1.9), 0, 0])
    c.layer("head", lambda t: [1.5 * math.sin(2 * math.pi * t / 3.0 + 1.0), 4 * math.sin(2 * math.pi * t / 3.0), 0])
    tent_wave(c, 9, 1, 3.0)
    c.done()

    # struggle_idle: doubled over, clawing at his head, shaking; the voices inside him fighting.
    c = clip("struggle_idle", 1.0, loop=True)
    c.key(0, STRUGGLE)
    c.key(1.0, STRUGGLE)
    for b, amp, seed in (("torso", 2.5, 1), ("head", 4.0, 2), ("right_arm", 3.0, 3), ("left_arm", 3.0, 4), ("right_forearm", 3.5, 5),
                         ("left_forearm", 3.5, 6), ("body", 1.2, 7), ("chest_jaw", 6.0, 8)):
        c.layer(b, lambda t, amp=amp, seed=seed: jit(("si", seed), t, amp, 1.0))
    tent_wave(c, 14, 3, 1.0, lag=1.4)
    c.done()

    # kneel_idle: on his knees, head bowed, breathing hard.
    c = clip("kneel_idle", 3.0, loop=True)
    for i, t in enumerate((0, 0.75, 1.5, 2.25, 3.0)):
        b = math.sin(2 * math.pi * t / 1.5)
        c.key(t, P_(KNEEL, {"torso": [10 + 2.5 * b, 0, 0], "head": [26 - 2 * b, 2 * math.sin(2 * math.pi * t / 3.0), 0],
                            "@torso": [0, 0.15 * b, 0]}), E_IO if i else None)
    tent_wave(c, 4, 1, 3.0)
    c.done()

    # --- the NPC -------------------------------------------------------------------------------------------------------
    # appear: he drops the last inch out of a flutter of wings (tick 8), the coat swirling round him and settling.
    c = clip("appear", 1.2)
    c.key(0, P_(IDLE, {"@root": [0, 2.4, 0], "coat_tail_right": [-30, 0, 34], "coat_tail_left": [-30, 0, -34],
                       "right_arm": [-4, 0, 22], "left_arm": [-4, 0, -22], "head": [10, 0, 0], "tie": [-30, 0, 10],
                       "right_leg": [-4, 0, 0], "right_shin": [10, 0, 0], "left_leg": [2, 0, 0], "left_shin": [14, 0, 0]}))
    c.key(0.4, P_(IDLE, {"@root": [0, -0.7, 0], "right_leg": [-12, 0, 0], "left_leg": [-12, 0, 0], "right_shin": [22, 0, 0],
                         "left_shin": [22, 0, 0], "coat_tail_right": [18, 0, 12], "coat_tail_left": [18, 0, -12],
                         "right_arm": [-2, 0, 12], "left_arm": [-2, 0, -12], "head": [4, 0, 0], "tie": [8, 0, -4]}), E_IN)
    c.key(0.7, P_(IDLE, {"coat_tail_right": [-6, 0, -3], "coat_tail_left": [-6, 0, 3], "head": [-4, 0, 0], "tie": [-4, 0, 2]}), E_OUT)
    c.key(0.95, P_(IDLE, {"coat_tail_right": [3, 0, 1], "coat_tail_left": [3, 0, -1], "head": [-2, 4, 6]}), E_IO)
    c.key(1.2, IDLE, E_IO)
    c.done()

    # vanish: a last level look, then the flutter (tick 12): the coat kicks out as the air rushes in; held (he is gone).
    c = clip("vanish", 1.0, hold=True)
    c.key(0, IDLE)
    c.key(0.4, P_(IDLE, {"head": [-4, -6, 8]}), E_IO)
    c.key(0.6, P_(IDLE, {"@root": [0, 0.8, 0], "coat_tail_right": [-40, 0, 42], "coat_tail_left": [-40, 0, -42],
                         "right_arm": [-4, 0, 18], "left_arm": [-4, 0, -18], "tie": [-40, 0, 14], "head": [-8, 0, 0]}), E_OUT)
    c.key(1.0, P_(IDLE, {"@root": [0, 1.4, 0], "coat_tail_right": [-46, 0, 48], "coat_tail_left": [-46, 0, -48],
                         "right_arm": [-4, 0, 20], "left_arm": [-4, 0, -20], "tie": [-50, 0, 16], "head": [-8, 0, 0]}))
    c.done()

    # talk: plain words, a small open-hand gesture (tick 10) and the tilt.
    c = clip("talk", 1.5)
    palm = lambda k, hz: {"right_arm": [-16 * k, 0, 8 * k], "right_forearm": [-58 * k, -8 * k, 0], "right_hand": [0, -64 * k, hz],
                          "right_fingers": [6, 0, 0], "right_index": [0, 0, 0]}
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, palm(0.8, -8), {"head": [-2, -6, 4]}), E_IO)
    c.key(0.5, P_(IDLE, palm(1.0, 6), {"head": [2, -8, 8]}), E_OUT)
    c.key(0.9, P_(IDLE, palm(0.9, -4), {"head": [-2, 2, 10]}), E_IO)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # nod: once, grave.
    c = clip("nod", 0.8)
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, {"head": [16, 0, 2]}), E_IO)
    c.key(0.55, P_(IDLE, {"head": [-4, 0, 0]}), E_IO)
    c.key(0.8, IDLE, E_IO)
    c.done()

    # turn_away: he looks off and down, ashamed; holds it; turns back.
    c = clip("turn_away", 1.5)
    away = P_(IDLE, {"head": [20, 42, -6], "torso": [4, 14, 0], "right_arm": [-2, 0, 2], "left_forearm": [-20, 0, 0]})
    c.key(0, IDLE)
    c.key(0.55, away, E_IO)
    c.key(1.1, P_(away, {"head": [24, 46, -6]}), E_IO)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # --- phases I-II: the new God ------------------------------------------------------------------------------------
    # snap: the right hand comes up in front of his shoulder; the snap on tick 10. His face does not change.
    c = clip("snap", 1.0)
    raised = lambda hz, th, mid: {"right_arm": [-40, 0, 30], "right_forearm": [-92, 14, 0], "right_hand": [0, 0, hz],
                                  "right_thumb": [th, 0, 0], "right_middle": [mid, 0, 0], "right_index": [30, 0, 0],
                                  "right_fingers": [70, 0, 0], "head": [-2, -6, 4], "torso": [0, -6, 0]}
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, raised(0, -20, 40)), E_OUT)
    c.key(0.44, P_(IDLE, raised(-6, -30, 50)))
    c.key(0.5, P_(IDLE, raised(16, 20, 95)), E_SNAP)
    c.key(0.7, P_(IDLE, raised(10, 10, 85)))
    c.key(1.0, IDLE, E_IO)
    c.done()

    # judgment: the wings unfurl, both arms rise over him, palms up; on tick 24 they come down and out, palms out, and the
    # heretics burst.
    c = clip("judgment", 2.0)
    arms_up = lambda k: {"right_arm": [-165 * k, 0, -18 * k], "left_arm": [-165 * k, 0, 18 * k], "right_forearm": [-10 * k, 0, 0],
                         "left_forearm": [-10 * k, 0, 0], "right_hand": [0, -40 * k, 0], "left_hand": [0, 40 * k, 0],
                         "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0], "head": [-24 * k, 0, 0], "torso": [-8 * k, 0, 0]}
    out = {"right_arm": [-88, 40, 0], "left_arm": [-88, -40, 0], "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0],
           "right_hand": [62, 0, 0], "left_hand": [62, 0, 0], "right_fingers": [-24, 0, 0], "left_fingers": [-24, 0, 0],
           "right_index": [-20, 0, 0], "left_index": [-20, 0, 0], "head": [-6, 0, 0], "torso": [6, 0, 0],
           "coat_tail_right": [-14, 0, 14], "coat_tail_left": [-14, 0, -14], "tie": [-24, 0, 0]}
    c.key(0, P_(IDLE, W_FOLD))
    c.key(0.5, P_(arms_up(0.6), W_HALF), E_OUT)
    c.key(1.0, P_(arms_up(1.0), W_UP, {"coat_tail_right": [6, 0, 6], "coat_tail_left": [6, 0, -6]}), E_IO)
    c.key(1.2, P_(out, W_WIDE), E_SNAP)
    c.key(1.6, P_(out, W_OPEN, {"head": [-4, 0, 0]}), E_OUT)
    c.key(2.0, P_(IDLE, W_FOLD), E_IO)
    c.done()

    # smite: the right palm thrust out at you (tick 12).
    c = clip("smite", 1.1)
    c.key(0, IDLE)
    c.key(0.35, P_(IDLE, {"right_arm": [14, 0, 16], "right_forearm": [-100, 10, 0], "right_hand": [40, 0, 0], "torso": [-2, -16, 0],
                          "head": [-2, 10, 0], "left_arm": [-6, 0, -8]}), E_OUT)
    hit = P_(IDLE, {"right_arm": [-88, 6, -2], "right_forearm": [-4, 0, 0], "right_hand": [74, 0, 0], "right_fingers": [-12, 0, 0],
                    "right_index": [-12, 0, 0], "torso": [4, 18, 0], "head": [-2, -12, 0], "left_arm": [6, 0, -10],
                    "coat_tail_right": [-8, 0, 4], "tie": [-14, 0, 0]})
    c.key(0.6, hit, E_SNAP)
    c.key(0.85, P_(hit, {"right_arm": [-84, 6, -2]}), E_OUT)
    c.key(1.1, IDLE, E_IO)
    c.done()

    # radiance: the arms gather across his chest, then fling wide, head back (tick 20): the blinding wave. Wings flare.
    c = clip("radiance", 1.8)
    gather = {"right_arm": [-64, 0, -34], "left_arm": [-64, 0, 34], "right_forearm": [-70, -30, 0], "left_forearm": [-70, 30, 0],
              "right_fingers": [60, 0, 0], "left_fingers": [60, 0, 0], "torso": [16, 0, 0], "head": [18, 0, 0],
              "coat_tail_right": [4, 0, -2], "coat_tail_left": [4, 0, 2]}
    flung = {"right_arm": [-26, 0, 96], "left_arm": [-26, 0, -96], "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0],
             "right_hand": [0, 0, 20], "left_hand": [0, 0, -20], "right_fingers": [-26, 0, 0], "left_fingers": [-26, 0, 0],
             "torso": [-16, 0, 0], "head": [-40, 0, 0], "coat_tail_right": [-24, 0, 22], "coat_tail_left": [-24, 0, -22],
             "tie": [-40, 0, 0]}
    c.key(0, P_(IDLE, W_FOLD))
    c.key(0.7, P_(gather, W_HALF, {"%wing_right": [0.6] * 3, "%wing_left": [0.6] * 3}), E_IO)
    c.key(0.85, P_(gather, W_HALF, {"torso": [20, 0, 0], "head": [22, 0, 0]}), E_IN)
    c.key(1.0, P_(flung, W_WIDE), E_SNAP)
    c.key(1.4, P_(flung, W_WIDE, {"head": [-36, 0, 0]}))
    c.key(1.8, P_(IDLE, W_FOLD), E_IO)
    c.done()

    # ascend: a breath, then he lifts off the ground, arms half out, into the hover.
    c = clip("ascend", 1.0)
    c.key(0, IDLE)
    c.key(0.25, P_(IDLE, {"@root": [0, -0.3, 0], "right_arm": [-4, 0, 10], "left_arm": [-4, 0, -10], "head": [6, 0, 0],
                          "coat_tail_right": [6, 0, 2], "coat_tail_left": [6, 0, -2]}), E_IO)
    c.key(0.65, P_(HOVER, {"@root": [0, 3.6, 0], "right_arm": [-8, 0, 30], "left_arm": [-8, 0, -30], "head": [-10, 0, 0],
                           "coat_tail_right": [-4, 0, 14], "coat_tail_left": [-4, 0, -14]}), E_OUT)
    c.key(1.0, HOVER, E_IO)
    c.done()

    # descend: down out of the hover, the knees taking the landing.
    c = clip("descend", 0.8)
    c.key(0, HOVER)
    c.key(0.45, P_(IDLE, {"@root": [0, -0.8, 0], "right_leg": [-14, 0, 0], "left_leg": [-14, 0, 0], "right_shin": [26, 0, 0],
                          "left_shin": [26, 0, 0], "coat_tail_right": [-18, 0, 12], "coat_tail_left": [-18, 0, -12], "head": [6, 0, 0]}),
          E_IN)
    c.key(0.8, IDLE, E_IO)
    c.done()

    # --- phases III-V: the Leviathans ----------------------------------------------------------------------------------
    # crack: he convulses, worse and worse; on tick 20 he arches back rigid as the light splits him; then hunches.
    c = clip("crack", 2.0)
    arch = {"torso": [-24, 0, 0], "head": [-40, 0, 0], "right_arm": [-6, 0, 36], "left_arm": [-6, 0, -36], "right_forearm": [-4, 0, 0],
            "left_forearm": [-4, 0, 0], "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0], "right_index": [-24, 0, 0],
            "left_index": [-24, 0, 0], "chest_jaw": [40, 0, 0], "tie": [-30, 0, 0], "coat_tail_right": [-10, 0, 12],
            "coat_tail_left": [-10, 0, -12]}
    hunch = P_(IDLE, {"torso": [16, 0, 0], "head": [12, 0, 0], "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0],
                      "right_fingers": [50, 0, 0], "left_fingers": [50, 0, 0], "chest_jaw": [10, 0, 0]})
    c.key(0, IDLE)
    c.key(0.3, hunch, E_OUT)
    c.key(0.85, P_(hunch, {"torso": [22, 0, 0], "head": [18, 0, 0]}), E_IO)
    c.key(1.0, P_(IDLE, arch), E_SNAP)
    c.key(1.35, P_(IDLE, arch, {"head": [-36, 0, 6]}))
    c.key(1.7, P_(hunch, {"torso": [10, 0, 0]}), E_IO)
    c.key(2.0, IDLE, E_IO)
    for b, amp, seed in (("torso", 4.0, 11), ("head", 6.0, 12), ("right_arm", 5.0, 13), ("left_arm", 5.0, 14)):
        c.layer(b, lambda t, amp=amp, seed=seed: [v * (min(1.0, t / 0.9) if t < 1.0 else max(0.0, 1 - (t - 1.0) / 0.7))
                                                  for v in jit(("cr", seed), t, amp, 2.0, (17, 23, 29))])
    tent_wave(c, 16, 3, 2.0, lag=1.3)
    c.done()

    # bite: the chest's maw gapes as he rears back, then lunges forward and snaps shut (tick 12).
    c = clip("bite", 1.2)
    c.key(0, IDLE)
    c.key(0.38, P_(IDLE, {"torso": [-14, 0, 0], "head": [-10, 0, 0], "right_arm": [24, 0, 16], "left_arm": [24, 0, -16],
                          "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "chest_jaw": [62, 0, 0],
                          "@chest_mouth": [0, 0.3, 0.3], "tie": [-20, 0, 0]}), E_OUT)
    lunge = P_(IDLE, {"torso": [20, 0, 0], "head": [-12, 0, 0], "right_arm": [36, 0, 22], "left_arm": [36, 0, -22],
                      "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "chest_jaw": [0, 0, 0], "@chest_mouth": [0, 0, -3.2],
                      "tie": [20, 0, 0], "coat_tail_right": [-10, 0, 0], "coat_tail_left": [-10, 0, 0]})
    c.key(0.6, lunge, E_SNAP)
    c.key(0.78, P_(lunge, {"chest_jaw": [14, 0, 0], "@chest_mouth": [0, 0, -2.4]}), E_OUT)
    c.key(1.2, IDLE, E_IO)
    tent_wave(c, 10, 1, 1.2)
    c.done()

    # sweep: the tentacles coil to his left, then lash across in front of him at waist height to his right (tick 16).
    c = clip("sweep", 1.5)
    coil = tent([(-86, -78, 0), (0, -18, 0), (0, -22, 0), (0, -26, 0), (0, -30, 0)],
                [(-86, -92, 0), (0, -16, 0), (0, -20, 0), (0, -24, 0), (0, -28, 0)])
    lash = tent([(-90, 18, 0), (0, 10, 0), (0, 12, 0), (0, 14, 0), (0, 16, 0)],
                [(-90, 4, 0), (0, 10, 0), (0, 12, 0), (0, 14, 0), (0, 16, 0)])
    follow = tent([(-80, 88, 0), (0, 16, 0), (0, 20, 0), (0, 24, 0), (0, 28, 0)],
                  [(-80, 76, 0), (0, 16, 0), (0, 20, 0), (0, 24, 0), (0, 28, 0)])
    c.key(0, IDLE)
    c.key(0.45, P_(IDLE, coil, {"body": [0, -10, 0], "torso": [4, -24, 0], "head": [-2, 18, 0], "right_arm": [-30, 0, -20],
                                "right_forearm": [-30, 0, 0], "left_arm": [8, 0, -26], "coat_tail_right": [-6, 0, -8],
                                "coat_tail_left": [-6, 0, -12]}), E_IO)
    c.key(0.8, P_(IDLE, lash, {"body": [0, 8, 0], "torso": [4, 20, 0], "head": [-2, -12, 0], "right_arm": [8, 0, 26],
                               "left_arm": [-34, 0, 18], "left_forearm": [-30, 0, 0], "coat_tail_right": [-6, 0, 12],
                               "coat_tail_left": [-6, 0, 8]}), E_SNAP)
    c.key(1.05, P_(IDLE, follow, {"body": [0, 12, 0], "torso": [4, 26, 0], "head": [-2, -16, 0], "right_arm": [10, 0, 24],
                                  "left_arm": [-30, 0, 16]}), E_OUT)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # slam: the tentacles rear up over him, curling, and come down on the ground ahead (tick 18).
    c = clip("slam", 1.5)
    rear = tent([(-164, 0, -30), (8, 0, 4), (10, 0, 4), (16, 0, 4), (28, 0, 4)])
    down = tent([(-50, 0, -6), (-8, 0, 0), (-8, 0, 0), (-6, 0, 0), (10, 0, 0)])
    c.key(0, IDLE)
    c.key(0.55, P_(IDLE, rear, {"torso": [-12, 0, 0], "head": [-16, 0, 0], "right_arm": [-20, 0, 24], "left_arm": [-20, 0, -24],
                                "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0]}), E_OUT)
    c.key(0.72, P_(IDLE, rear, {"torso": [-16, 0, 0], "head": [-18, 0, 0], "right_arm": [-24, 0, 26], "left_arm": [-24, 0, -26],
                                "right_forearm": [-34, 0, 0], "left_forearm": [-34, 0, 0]}))
    hit = P_(IDLE, down, {"torso": [22, 0, 0], "head": [-8, 0, 0], "right_arm": [-40, 0, 14], "left_arm": [-40, 0, -14],
                          "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "chest_jaw": [30, 0, 0],
                          "coat_tail_right": [-12, 0, 6], "coat_tail_left": [-12, 0, -6]})
    c.key(0.9, hit, E_SNAP)
    c.key(1.15, P_(hit, {"torso": [18, 0, 0]}))
    c.key(1.5, IDLE, E_IO)
    c.done()

    # retch: he doubles over, then heaves (tick 14): goo spews out of the maw in an arc; shudders.
    c = clip("retch", 1.5)
    heave = P_(IDLE, {"torso": [36, 0, 0], "head": [-22, 0, 0], "right_arm": [26, 0, 22], "left_arm": [26, 0, -22],
                      "right_forearm": [-14, 0, 0], "left_forearm": [-14, 0, 0], "right_fingers": [-20, 0, 0], "left_fingers": [-20, 0, 0],
                      "chest_jaw": [66, 0, 0], "face_mouth": [-14, 0, 0], "tie": [30, 0, 0]})
    c.key(0, IDLE)
    c.key(0.4, P_(IDLE, {"torso": [22, 0, 0], "head": [16, 0, 0], "right_arm": [-34, 0, -10], "left_arm": [-34, 0, 10],
                         "right_forearm": [-70, 0, 0], "left_forearm": [-70, 0, 0], "chest_jaw": [18, 0, 0]}), E_IO)
    c.key(0.7, heave, E_SNAP)
    c.key(1.05, P_(heave, {"torso": [32, 0, 0], "chest_jaw": [58, 0, 0]}))
    c.key(1.5, IDLE, E_IO)
    c.layer("torso", lambda t: [v * (1.0 if 0.7 <= t <= 1.1 else 0.0) for v in jit("rt", t, 2.5, 1.5, (13, 19, 23))])
    c.done()

    # surge: he curls in on himself, then throws his arms down and out, head back (tick 22): goo erupts round him.
    c = clip("surge", 2.0)
    curl = P_(IDLE, {"torso": [26, 0, 0], "head": [20, 0, 0], "right_arm": [-62, 0, -30], "left_arm": [-62, 0, 30],
                     "right_forearm": [-84, -20, 0], "left_forearm": [-84, 20, 0], "right_fingers": [70, 0, 0], "left_fingers": [70, 0, 0],
                     "coat_tail_right": [0, 0, -4], "coat_tail_left": [0, 0, 4]},
              tent([(10, 0, 8), (-30, 0, 30), (-30, 0, 30), (-30, 0, 30), (-30, 0, 30)]))
    burst = P_(IDLE, {"torso": [-18, 0, 0], "head": [-32, 0, 0], "right_arm": [12, 0, 50], "left_arm": [12, 0, -50],
                      "right_forearm": [-2, 0, 0], "left_forearm": [-2, 0, 0], "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0],
                      "right_index": [-26, 0, 0], "left_index": [-26, 0, 0], "chest_jaw": [56, 0, 0], "face_mouth": [-12, 0, 0],
                      "coat_tail_right": [-30, 0, 30], "coat_tail_left": [-30, 0, -30], "tie": [-40, 0, 0]},
               tent([(50, 0, 84), (-6, 0, -6), (-6, 0, -6), (-10, 0, -8), (-14, 0, -10)]))
    c.key(0, IDLE)
    c.key(0.7, curl, E_IO)
    c.key(0.95, P_(curl, {"torso": [30, 0, 0]}), E_IN)
    c.key(1.1, burst, E_SNAP)
    c.key(1.5, P_(burst, {"head": [-28, 0, 0]}))
    c.key(2.0, IDLE, E_IO)
    c.layer("torso", lambda t: [v * (1.0 if 1.1 <= t <= 1.6 else 0.0) for v in jit("su", t, 2.5, 2.0, (21, 27, 33))])
    c.done()

    # struggle: into the inner fight, the hands flying to his head.
    c = clip("struggle", 0.8)
    c.key(0, IDLE)
    c.key(0.22, P_(IDLE, {"torso": [8, 0, 0], "head": [-14, 0, 8], "right_arm": [-80, -10, 30], "left_arm": [-80, 10, -30],
                          "right_forearm": [-80, 0, 0], "left_forearm": [-80, 0, 0], "chest_jaw": [30, 0, 0]}), E_OUT)
    c.key(0.5, P_(STRUGGLE, {"torso": [32, 0, 0], "head": [22, 0, 0]}), E_IO)
    c.key(0.8, STRUGGLE, E_IO)
    c.done()

    # recover: he straightens out of it, hands lowering, and squares his shoulders.
    c = clip("recover", 1.0)
    c.key(0, STRUGGLE)
    c.key(0.4, P_(IDLE, {"torso": [12, 0, 0], "head": [10, 0, 0], "right_arm": [-60, -10, 24], "left_arm": [-60, 10, -24],
                         "right_forearm": [-60, 0, 0], "left_forearm": [-60, 0, 0], "chest_jaw": [10, 0, 0]}), E_IO)
    c.key(0.7, P_(IDLE, {"torso": [-4, 0, 0], "head": [-8, 0, 0]}), E_IO)
    c.key(1.0, IDLE, E_IO)
    c.done()

    # --- the end -------------------------------------------------------------------------------------------------------
    # kneel: beaten, he goes down onto both knees.
    c = clip("kneel", 1.5, hold=True)
    c.key(0, IDLE)
    c.key(0.5, P_(IDLE, {"@root": [0, -1.4, 0], "right_leg": [-30, 0, 4], "left_leg": [-30, 0, -4], "right_shin": [50, 0, 0],
                         "left_shin": [50, 0, 0], "torso": [18, 0, 0], "head": [14, 0, 0], "coat_tail_right": [-14, 0, 8],
                         "coat_tail_left": [-14, 0, -8]}), E_IO)
    c.key(1.05, P_(KNEEL, {"@root": [0, -4.8, 0], "torso": [16, 0, 0], "head": [30, 0, 0]}), E_IN)
    c.key(1.5, KNEEL, E_IO)
    c.done()

    # sink: condemned. A last look up; he falls back into the black water and goes under (the code hides him at the end).
    c = clip("sink", 4.0, hold=True)
    look_up = P_(KNEEL, {"head": [-18, 0, 0], "torso": [0, 0, 0]})
    lying = P_(KNEEL, {"body": [-82, 0, 0], "torso": [-4, 0, 0], "head": [-6, 0, 0], "right_arm": [-10, 0, 46],
                       "left_arm": [-10, 0, -46], "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0], "right_fingers": [10, 0, 0],
                       "left_fingers": [10, 0, 0], "coat_tail_right": [-20, 0, 20], "coat_tail_left": [-20, 0, -20], "tie": [-30, 0, 0]})
    c.key(0, KNEEL)
    c.key(0.8, look_up, E_IO)
    c.key(1.3, look_up)
    c.key(2.0, lying, E_IN)
    c.key(2.4, P_(lying, {"@root": [0, -6.5, 0]}), E_OUT)
    c.key(4.0, P_(lying, {"@root": [0, -26.0, 0], "right_arm": [-40, 0, 70], "left_arm": [-40, 0, -70], "head": [-14, 0, 0]}), E_IN)
    c.done()

    # rise: forgiven. He lifts his head, plants a foot and stands, arms a little out as the goo pours off him.
    c = clip("rise", 2.5)
    c.key(0, KNEEL)
    c.key(0.5, P_(KNEEL, {"head": [-6, 0, 0], "torso": [2, 0, 0]}), E_IO)
    step = P_(KNEEL, {"@root": [0, -3.6, 0], "left_leg": [-82, 0, -4], "left_shin": [80, 0, 0], "right_leg": [-4, 0, 4],
                      "right_shin": [92, 0, 0], "torso": [16, 0, 0], "head": [-2, 0, 0], "left_arm": [-44, 0, -6],
                      "left_forearm": [-34, 0, 0], "coat_tail_left": [-60, 0, -8]})
    c.key(1.1, step, E_IO)
    c.key(1.8, P_(IDLE, {"@root": [0, -0.6, 0], "right_leg": [-10, 0, 0], "left_leg": [-16, 0, 0], "right_shin": [14, 0, 0],
                         "left_shin": [20, 0, 0], "torso": [6, 0, 0], "right_arm": [-6, 0, 22], "left_arm": [-6, 0, -22],
                         "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0], "head": [-10, 0, 0]}), E_OUT)
    c.key(2.2, P_(IDLE, {"right_arm": [-4, 0, 16], "left_arm": [-4, 0, -16], "head": [-6, 0, 0]}), E_IO)
    c.key(2.5, IDLE, E_IO)
    c.done()

    # death: the rematch lost. The Leviathans fight their way out of him -- he clutches himself, convulses harder and harder,
    # the maw tearing wider -- and on tick 100 they burst out: arched back, arms flung wide. Then he drops to his knees and
    # pitches forward onto his face.
    c = clip("death", 7.0, hold=True)
    clutch = P_(IDLE, {"torso": [22, 0, 0], "head": [20, 0, 0], "right_arm": [-40, 0, -14], "left_arm": [-40, 0, 14],
                       "right_forearm": [-80, -20, 0], "left_forearm": [-80, 20, 0], "right_fingers": [60, 0, 0],
                       "left_fingers": [60, 0, 0], "chest_jaw": [16, 0, 0]})
    c.key(0, IDLE)
    c.key(0.35, clutch, E_SNAP)
    c.key(2.0, P_(clutch, {"torso": [14, 0, 0], "head": [-10, 0, 0], "right_arm": [-60, 0, 20], "left_arm": [-60, 0, -20],
                           "right_forearm": [-90, 0, 0], "left_forearm": [-90, 0, 0], "chest_jaw": [34, 0, 0]}), E_IO)
    c.key(3.4, P_(clutch, {"torso": [-8, 0, 0], "head": [-24, 0, 0], "right_arm": [-150, -20, 40], "left_arm": [-150, 20, -40],
                           "right_forearm": [-110, 0, 0], "left_forearm": [-110, 0, 0], "chest_jaw": [50, 0, 0]}), E_IO)
    c.key(4.6, P_(clutch, {"torso": [34, 0, 0], "head": [28, 0, 0], "right_arm": [-30, 0, -20], "left_arm": [-30, 0, 20],
                           "right_forearm": [-100, -30, 0], "left_forearm": [-100, 30, 0], "chest_jaw": [30, 0, 0],
                           "@chest_mouth": [0, 0, 0.6]}), E_IO)
    torn = P_(IDLE, {"torso": [-30, 0, 0], "head": [-50, 0, 0], "right_arm": [16, 0, 84], "left_arm": [16, 0, -84],
                     "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0], "right_fingers": [-34, 0, 0], "left_fingers": [-34, 0, 0],
                     "right_index": [-30, 0, 0], "left_index": [-30, 0, 0], "chest_jaw": [84, 0, 0], "@chest_mouth": [0, 0, -3.0],
                     "face_mouth": [-18, 0, 0], "coat_tail_right": [-34, 0, 30], "coat_tail_left": [-34, 0, -30], "tie": [-50, 0, 0]},
               tent([(-120, 0, 60), (-10, 0, -10), (-12, 0, -12), (-14, 0, -14), (-16, 0, -16)]))
    c.key(5.0, torn, E_SNAP)
    c.key(5.5, P_(torn, {"head": [-46, 0, 4]}))
    slump = P_(KNEEL, {"torso": [20, 0, 0], "head": [40, 0, 0], "right_arm": [0, 0, 8], "left_arm": [0, 0, -8],
                       "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0], "right_fingers": [10, 0, 0], "left_fingers": [10, 0, 0],
                       "chest_jaw": [40, 0, 0]}, tent([(30, 0, 70), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]))
    c.key(6.1, slump, E_IN)
    prone = P_(slump, {"@root": [0, 1.4, 1.0], "root": [88, 0, 0], "right_leg": [0, 0, 2], "left_leg": [0, 0, -2],
                       "right_shin": [6, 0, 0], "left_shin": [6, 0, 0], "body": [0, 0, 0], "torso": [0, 0, 0], "head": [-10, 30, 0],
                       "right_arm": [-170, 0, 16], "left_arm": [-170, 0, -16], "coat_tail_right": [0, 0, 6], "coat_tail_left": [0, 0, -6],
                       "tie": [0, 0, 0]})
    c.key(6.7, prone, E_IN)
    c.key(7.0, prone)
    for b, amp, seed in (("torso", 5.0, 21), ("head", 7.0, 22), ("right_arm", 6.0, 23), ("left_arm", 6.0, 24), ("body", 2.0, 25),
                         ("chest_jaw", 10.0, 26)):
        c.layer(b, lambda t, amp=amp, seed=seed: [v * (0.3 + 0.7 * t / 5.0 if 0.35 <= t <= 5.0 else 0.0)
                                                  for v in jit(("dt", seed), t, amp, 7.0, (61, 79, 97))])
    tent_wave(c, 22, 9, 7.0, lag=1.5)
    c.done()
    return f


def generate():
    for look, name in (("npc", "castiel"), ("boss", "godstiel"), ("soaked", "godstiel_soaked")):
        m = rig(look)
        t, g = m.build(gutter=1, seed=39000)
        if look == "npc":
            m.rig.write(GEO + NAME + ".geo.json")
            save(t, "entity", "castiel")
        else:
            save(t, "entity", name)
            save(g, "entity", name + "_glowmask")
    write_compact(anims(), ANIM + NAME + ".animation.json")


if __name__ == "__main__":
    generate()
