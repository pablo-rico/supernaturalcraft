"""Gabriel, the Trickster (v0.14): one detailed rig, five costumes, six golden shadow wings, every clip.

"Welcome to TV Land!" A short, cocky man (about 0.92 of a player's height): light-brown hair slicked back, stubble, a
cheeky smirk, a lollipop never far from his mouth.

One geometry, five textures with the same UV layout (the packing depends only on the cubes; every texture paints every
cube): gabriel_jacket / sweater / tuxedo / lab_coat / suit. The base body is painted in each costume's colours (shirt,
sleeves, trousers, shoes); the pieces of his own clothes that add volume (the olive canvas jacket's shell, collar and
pockets) are painted only on the jacket texture and clear on the others. The other four costumes are bone groups the
renderer shows one at a time (GabrielAssets.COSTUME_BONES), each holding its own copy of the limbs it dresses with the
base pivots (`<costume>_body`, `<costume>_right_arm`, ...); every clip keys those copies exactly like the base limbs
(`Clip`), so the worn costume moves with him:

  costume_sweater   a chunky oatmeal cable-knit cardigan (rust bands, shawl collar, big buttons, suede elbow patches)
                    over a blue oxford shirt, khaki slacks and loafers: the 80s sitcom dad;
  costume_tuxedo    a glittering gold sequin game-show tuxedo, black satin shawl lapels, a big bow tie, a carnation;
                    black trousers with a satin stripe, patent shoes;
  costume_lab_coat  Dr. Sexy, M.D.: a white coat to the knees, stethoscope, pens and a badge; shirt and tie, cowboy boots;
  costume_suit      the commercial spokesman: a sharp cobalt suit, peak lapels, red tie, pocket square.

Skeleton (Bedrock px; +X is his LEFT; the model faces north):
  root -- body -- head -- hair, nurse_cap, eyes_glow
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_middle, right_thumb,
                                                             pie, microphone, defibrillator -- defib_paddle_left
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_thumb, lollipop
               -- wings -- wing_r1..3, wing_l1..3 -- wing_<side><n>_tip
       -- right_leg -- right_shin,  left_leg -- left_shin
       -- costume_<c> -- <c>_body -- <c>_right_arm -- <c>_right_forearm ...  (+ legs/shins for the lab coat)

Props (GabrielAssets.PROP_CLIPS) are hidden by the renderer unless their clip plays; `defib_paddle_left` is a child of
`defibrillator` (so it hides with it) that the defib clip keys onto his left fist. `eyes_glow` is two golden planes over
his eyes (the archangel showing through) the renderer shows only with the wings. The six wings are translucent smoke
with glowing golden rims (gabriel_glowmask.png): the model needs a translucent render type.
"""

import json
import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, _ease, aim, mirror_rot, parent_offset, ruffle,
                         to_preview, tone, world_point, write_compact)
from michael_art import Clip as MClip
from pixelkit import Ramp, fbm, hexc, mix, shade
from rival_hunter_art import knit, leather, worn

A = "animation.gabriel."
CLEAR = (0, 0, 0, 0)
COSTUMES = ("jacket", "sweater", "tuxedo", "lab_coat", "suit")
GROUPS = ("sweater", "tuxedo", "lab_coat", "suit")      # costume_<g> bone groups (the jacket is the base body)
LIMBS = ("body", "right_arm", "right_forearm", "left_arm", "left_forearm", "right_leg", "right_shin", "left_leg", "left_shin")
COPIES = {
    "sweater": LIMBS[:5],
    "tuxedo": LIMBS[:5],
    "lab_coat": LIMBS,
    "suit": LIMBS[:5],
}
PARENT = {"body": None, "right_arm": "body", "right_forearm": "right_arm", "left_arm": "body", "left_forearm": "left_arm",
          "right_leg": None, "right_shin": "right_leg", "left_leg": None, "left_shin": "left_leg"}

# --- proportions (a short man: 29.5 px to the top of his hair) ------------------------------------------------------
HIP = 10.5      # top of the legs / bottom of the torso
NECK = 21.0     # top of the torso, bottom of the head
TW = 3.75       # torso half-width
AW = 3.5        # arm width
AX = 5.6        # arm centre |x|
SX = 5.3        # shoulder pivot |x|
SY = 20.5       # shoulder pivot y
ELBOW = 16.0
WRIST = 12.0
LX = 1.85       # leg centre |x|
LW = 3.6        # leg width
KNEE = 5.5
PIV = {
    "body": (0, HIP, 0),
    "right_arm": (-SX, SY, 0), "right_forearm": (-AX, ELBOW, 0),
    "left_arm": (SX, SY, 0), "left_forearm": (AX, ELBOW, 0),
    "right_leg": (-LX, HIP, 0), "right_shin": (-LX, KNEE, 0),
    "left_leg": (LX, HIP, 0), "left_shin": (LX, KNEE, 0),
}

# --- palette --------------------------------------------------------------------------------------------------------
SKIN = Ramp("#5a3a2a", "#8a5e48", "#b07d62", "#c8967a", "#dbad91", "#ebc5aa")
HAIR = Ramp("#22160b", "#3a2713", "#55381c", "#704f2a", "#8b683a", "#a6824e")
EYE = hexc("#5c4024")
EYE_WHITE = hexc("#e8e0d5")
LIPS = Ramp("#5e2f27", "#7f443a", "#9a5a4d", "#b06e60", "#c48473", "#d49a88")
TEETH = hexc("#efe9dc")
GOLD_EYE = hexc("#ffd86a")

OLIVE = Ramp("#1e1f0f", "#31321a", "#474925", "#5d6031", "#74773f", "#8c8f51")
CORD = Ramp("#22150b", "#352112", "#4a2f1a", "#5f3e23", "#764f2e", "#8d613b")
DARK_SHIRT = Ramp("#0f1012", "#18191c", "#212327", "#2b2e33", "#363a40", "#43474e")
DENIM = Ramp("#131c2e", "#1d2a44", "#29395a", "#364a70", "#475d85", "#5d739b")
BOOT = Ramp("#22160c", "#362313", "#4c321c", "#624226", "#795331", "#91663e")
SOLE = hexc("#16100b")
BELT = Ramp("#140c07", "#22150c", "#331f12", "#452b19", "#593821", "#6e472b")
STEEL = Ramp("#2a2f36", "#454c56", "#68717d", "#8f98a3", "#b8c0c9", "#e4e9ee")

OAT = Ramp("#4f4434", "#6d604b", "#8c7d63", "#a7987b", "#bfb094", "#d6c9ae")
RUST = Ramp("#3a170c", "#5a2412", "#7a351b", "#974725", "#b05b31", "#c67443")
SUEDE = Ramp("#2c1d12", "#43301e", "#5b432c", "#71563a", "#866a4a", "#9b7f5c")
OXFORD = Ramp("#4f6584", "#6c84a6", "#8aa3c3", "#a6bdd8", "#bfd2e7", "#d8e5f3")
KHAKI = Ramp("#4a4130", "#655a42", "#827555", "#9c8f6a", "#b3a680", "#c9bd98")
LOAFER = Ramp("#1f1009", "#311a0e", "#452515", "#5a321c", "#704026", "#874f31")
WOOD = Ramp("#2b190c", "#432713", "#5c371b", "#764824", "#8f5a2f", "#a76d3b")

SEQUIN = Ramp("#3f2805", "#6e4a0d", "#9c6f17", "#c79524", "#e8bd40", "#fff0a6")
SATIN = Ramp("#040405", "#0a0a0d", "#121217", "#1c1c23", "#282832", "#373744")
TUX_SHIRT = Ramp("#9ea2a8", "#bdc1c6", "#d5d8dc", "#e6e8eb", "#f2f3f5", "#fdfdfe")
CARNATION = Ramp("#4a0710", "#760c1a", "#a01526", "#c42235", "#df3a4b", "#f06474")

COAT = Ramp("#8b9199", "#aab0b8", "#c6cbd1", "#dbdfe3", "#ebedf0", "#f9fafb")
SCRUB = Ramp("#4c6b80", "#62869c", "#7aa0b5", "#94b8cb", "#afcdde", "#cbe1ee")
NAVY = Ramp("#0a1020", "#111a33", "#192547", "#22325c", "#2e4273", "#3e558c")
SLACKS = Ramp("#17181b", "#222429", "#2e3137", "#3a3e46", "#484d56", "#585e68")
COWBOY = Ramp("#3a1d0b", "#5a2f13", "#7a431d", "#985828", "#b26e35", "#c98745")
TUBE = Ramp("#0d0d0f", "#17171a", "#222226", "#2d2d33", "#3a3a41", "#4a4a52")

COBALT = Ramp("#0a1640", "#112463", "#1a3488", "#2547aa", "#335ec4", "#4c78da")
RED_TIE = Ramp("#3e0608", "#640b0f", "#8c1218", "#b11c22", "#cf2f31", "#e65049")
SHOE = Ramp("#040405", "#0a0a0c", "#121215", "#1c1c20", "#29292f", "#45454e")

CANDY = (hexc("#e8283c"), hexc("#ffffff"), hexc("#ffb21f"), hexc("#3fb0ff"))
STICK = hexc("#f2ede2")
PIE_TIN = Ramp("#525860", "#737b85", "#98a0aa", "#bcc3cb", "#d9dee3", "#f1f4f6")
CREAM = Ramp("#b8ab95", "#d1c6b0", "#e3dac6", "#efe8d8", "#f8f3e8", "#fffdf7")
CRUST = Ramp("#5e3612", "#7c4a1b", "#9a6026", "#b47733", "#c98f45", "#dca85d")
CHERRY = hexc("#b0101e")
MIC = Ramp("#0c0c0e", "#16161a", "#222228", "#303038", "#42424c", "#5a5a66")
GRILLE = Ramp("#4d535b", "#6f7680", "#959ca6", "#b9bfc7", "#d6dbe0", "#f2f5f7")
PADDLE = Ramp("#2f3238", "#474b53", "#62676f", "#7f858e", "#9fa5ae", "#c3c9d1")
SAFETY = hexc("#f2c418")

SMOKE_G = Ramp("#120b04", "#1f1408", "#2e1f0d", "#3f2c13", "#523a1a", "#684b23")
LIGHT_G = Ramp("#7a4c10", "#b47a1c", "#dea735", "#f5cf66", "#ffe9a6", "#fffbe8")


def tw(ramp, v):
    return tone(ramp, v)


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(seed):
    return H.skin_mat(SKIN, seed, 3.15, 0.4)


def hair_m(seed=7701):
    """Combed straight back: strands running front to back on top, down on the back of the head."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 11.0)
        if f in ("up", "down"):
            strand = math.sin(x * 2.1 + n * 4)          # rows run front->back on the top face
        else:
            strand = math.sin(x * 1.7 + y * 0.3 + n * 5)
        v = 2.9 + (n - 0.5) * 0.6 + strand * 0.45
        if f == "up" and 0.35 < x / max(1, w) < 0.62:
            v += 0.5                                     # the shine of the comb-over
        return HAIR[v]
    return m


def canvas_m(ramp, seed, base=3.0):
    """Hard-wearing canvas: a fine diagonal twill, low-frequency wear, double topstitching at the hems."""
    b = worn(ramp, seed, base, 0.85, scuff=0.08)

    def m(f, x, y, w, h):
        c = b(f, x, y, w, h)
        if f not in ("up", "down") and (x + y) % 3 == 0:
            c = shade(c, 1.07)
        return c
    return m


def denim_m(ramp, seed, fade=True):
    def m(f, x, y, w, h):
        n = fbm(x + len(f) * 3, y, seed, 64, 64, 3, 8.0)
        v = 2.6 + (n - 0.5) * 1.4 + (0.45 if (x - y) % 3 == 0 else 0)
        c = ramp[v]
        if fade and f == "north" and 0.1 < y / max(1, h) < 0.6 and 1 <= x <= w - 2:
            c = mix(c, ramp[5], 0.2)
        if f in ("east", "west") and x == w // 2:
            c = shade(c, 1.15) if y % 2 else shade(c, 0.85)
        return c
    return m


def trouser_m(ramp, seed, base=2.9, stripe=None):
    """Pressed trousers: smooth cloth, a crease down front and back, an optional satin side stripe."""
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return tw(ramp, base - 0.8)
        n = fbm(x + len(f) * 5, y, seed, 64, 64, 2, 9.0)
        v = base + (n - 0.5) * 0.35 + 0.12 * math.sin(x * 0.8 + y * 0.3 + seed) - 0.4 * y / max(1, h)
        if f in ("north", "south") and abs(x - (w - 1) / 2) < 0.6:
            v += 0.7
        if f in ("east", "west"):
            v -= 0.25
            if stripe is not None and abs(x - (w - 1) / 2) < 0.6:
                return stripe[3.2 + 0.6 * math.sin(y * 0.9)]
        return tw(ramp, v)
    return m


def cable_m(ramp, seed, band=None):
    """Chunky cable knit: twisted vertical cables between purl ribs; `band` adds rust zig-zag bands across."""
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return ramp[2.6 + 0.5 * ((x + y) % 2)]
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 10.0)
        col = x % 5
        if col == 0:
            v = 1.7 + n * 0.4                           # the purl channel between cables
        else:
            twist = (y + (col if col < 3 else 5 - col)) % 4
            v = 3.3 - 0.35 * twist + (n - 0.5) * 0.4 + (0.3 if col in (2, 3) else 0)
        c = ramp[v]
        if band is not None:
            yy = y % 24
            if yy in (5, 10):
                c = band[2.3 + n]
            elif 6 <= yy <= 9:
                zig = abs((x % 6) - 3)
                c = band[3.3 + n * 0.4] if abs((yy - 6) - zig * 0.9) < 0.9 else mix(c, band[1], 0.15)
        return c
    return m


def rib_m(ramp, base=2.6):
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return ramp[base - 0.6]
        return ramp[base + (0.7 if x % 2 == 0 else -0.3)]
    return m


def sequin_m(seed, ramp=SEQUIN):
    """Gold sequins: a staggered scale pattern, each disc lit along its top, random sparks of full light."""
    def m(f, x, y, w, h):
        if f == "down":
            return ramp[1.5]
        row = y // 2
        sx = (x + (row % 2)) // 2
        r = h01("seq", seed, f, sx, row)
        top = (y % 2 == 0)
        v = 2.4 + r * 1.4 + (0.5 if top else -0.3)
        if r > 0.93:
            return ramp[5]
        if f == "up":
            v += 0.3
        return ramp[v]
    return m


def sequin_glow(f, x, y, w, h, c):
    """Only the sparks of the sequins hold light."""
    if c[0] > 240 and c[1] > 220:
        return (c[0], c[1], c[2], 255)
    if c[0] > 225 and c[1] > 180:
        return shade(c, 0.35)
    return None


def satin_m(ramp, seed, base=2.6):
    def m(f, x, y, w, h):
        sheen = math.sin(x * 0.7 - y * 0.5 + seed) * 0.6 + (0.8 if f == "up" else 0)
        return ramp[base + sheen]
    return m


def smooth_m(ramp, seed, base=3.0, folds=0.3):
    """Suit cloth: top-lit, soft diagonal folds, no noise."""
    def m(f, x, y, w, h):
        if f == "up":
            return tw(ramp, base + 0.6)
        if f == "down":
            return tw(ramp, base - 1.2)
        t = y / max(1, h - 1)
        v = base + 0.4 - 0.7 * t + folds * math.sin(x * 0.9 + y * 0.35 + seed)
        if f in ("east", "west"):
            v -= 0.25
        elif f == "south":
            v -= 0.15
        return tw(ramp, v)
    return m


def cotton_m(ramp, seed, base=3.3):
    def m(f, x, y, w, h):
        n = fbm(x + len(f) * 7, y, seed, 64, 64, 2, 8.0)
        v = base + (n - 0.5) * 0.6 - 0.4 * y / max(1, h) + 0.25 * math.sin(x * 1.1 + y * 0.4 + seed)
        if f == "down":
            v -= 1.0
        return tw(ramp, v)
    return m


def tooled_m(ramp, seed):
    """Cowboy-boot leather: polished, with pale decorative stitching scrolls."""
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return ramp[1.4]
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 8.0)
        c = ramp[2.8 + (n - 0.5) * 0.9 + 0.5 * (1 - y / max(1, h))]
        if f in ("north", "south", "east", "west"):
            u = x / max(1, w - 1)
            if abs(y - (h * 0.5 + 1.2 * math.sin(u * 6.28))) < 0.5:
                c = mix(c, hexc("#e3c99a"), 0.45)
        return c
    return m


def boot_m(ramp, seed, laces=None, toe_shine=0.0):
    """A shoe or boot on the foot cube: a darker sole band, a lit toe cap, laces on top."""
    def m(f, x, y, w, h):
        if f == "down":
            return SOLE
        if f != "up" and y >= h - 1:
            return SOLE
        n = fbm(x * 3 + len(f), y * 3, seed, 64, 64, 2, 9.0)
        v = 2.8 + (n - 0.5) * 0.7
        if f == "up":
            fz = y / max(1, h - 1)   # 0 at the front
            v += 0.6 * (1 - fz)
            if laces is not None and 0.35 < fz < 0.75 and 2 <= x <= w - 3 and y % 2 == 0:
                return laces
            if toe_shine and fz < 0.25 and abs(x - w / 2) < 1.5:
                return ramp[5]
        if f == "north":
            v += 0.4
            if toe_shine and y == 0:
                return ramp[5]
        return ramp[v]
    return m


# =====================================================================================================
# The face (north face of the head, 16 x 16 texels, x = 0 is his right)
# =====================================================================================================

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhshhhhhhhhhhshh",
    "hssssssssssssssh",
    "ssssssssssbBBbss",
    "sbBBBsssssssssbs",
    "sswEwsssssswEwss",
    "ssuuussssssuuuss",
    "sssssssnnsssssss",
    "tssssssnnsssssst",
    "ttsssssNNssssmtt",
    "tttssmmmTTTmmstt",
    "ttttsslLLlsstttt",
    "jttttttttttttttj",
    "jjttttttttttttjj",
]


def fix(rows):
    return [(r + "s" * 16)[:16] for r in rows]


# =====================================================================================================
# The rig
# =====================================================================================================

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", PIV["body"], "root")
    m.bone("head", (0, NECK, 0), "body")
    m.bone("hair", (0, NECK, 0), "head")
    m.bone("nurse_cap", (0, 29.2, -1.0), "head")
    m.bone("eyes_glow", (0, 25.0, -4.0), "head")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", PIV[f"{side}_arm"], "body")
        m.bone(f"{side}_forearm", PIV[f"{side}_forearm"], f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, 10.4, 0), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 0.9 * s, 11.4, -1.3), f"{side}_hand")
    m.bone("right_middle", (-AX, 10.4, -0.25), "right_hand")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_leg", PIV[f"{side}_leg"], "root")
        m.bone(f"{side}_shin", PIV[f"{side}_shin"], f"{side}_leg")
    for g in GROUPS:
        m.bone(f"costume_{g}", (0, 0, 0), "root")
        for limb in COPIES[g]:
            par = PARENT[limb]
            m.bone(f"{g}_{limb}", PIV[limb], f"{g}_{par}" if par else f"costume_{g}")


def rig(costume="jacket"):
    """The rig painted for one costume (every costume has the same cubes, so the same UV layout)."""
    m = Model("gabriel", 512, 512)
    m.costume = costume
    skeleton(m)
    build_head(m)
    build_base(m, costume)
    build_jacket(m, costume)
    build_sweater(m)
    build_tuxedo(m)
    build_lab_coat(m)
    build_suit(m)
    build_props(m)
    build_wings(m)
    return m


def only(costume, wanted, mat):
    """A piece of one costume's base clothes: `mat` on its texture, clear on the others."""
    if costume == wanted:
        return mat
    return lambda f, x, y, w, h: CLEAR


# --- head ------------------------------------------------------------------------------------------------------------

def build_head(m):
    sk = skin_m(7601)
    hr = hair_m()
    stub = H.stubble(sk, HAIR, 7602, 0.32)
    legend = {"h": hr, "s": sk, "t": stub, "b": HAIR[1], "B": HAIR[2], "w": EYE_WHITE, "E": EYE, "u": SKIN[2],
              "n": SKIN[4], "N": SKIN[1], "m": LIPS[1], "T": TEETH, "l": LIPS[2], "L": LIPS[3],
              "j": lambda f, x, y, w, h: shade(stub(f, x, y, w, h), 0.92)}
    sides = H.head_sides(sk, hr, top=3, back=13, temple=7, sideburn=9, beard=stub, beard_from=11)
    H.head_cube(m, fix(FACE), legend, sides, y0=NECK)
    m.cube("head", (-0.5, 23.5, -4.5), (1.0, 1.5, 0.5), lambda f, x, y, w, h: SKIN[4] if f == "north" and y < 2 else
           (SKIN[1] if f == "down" else SKIN[2.6]), density=2, tag="nose")
    for s in (-1, 1):
        m.cube("head", (4 * s - 0.25 + (0 if s > 0 else -0.25), 23.5, -0.5), (0.5, 2, 1.5), solid(SKIN[2]), density=2, tag="ear")
    m.cube("body", (-1.5, NECK - 0.6, -1.5), (3, 1, 3), sk, density=2, tag="neck")
    # Slicked back: a smooth shell over the crown with a swept ridge at the front and the hair to the nape behind.
    hair = m.rig.get("hair")

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x   # 0 at the front
            return hr(f, x, y, w, h) if y < 2 + (fx > 6) * 2 else None
        return hr(f, x, y, w, h)
    m.cube(hair, (-4, 27.0, -4), (8, 2.0, 8), shell, inflate=0.32, density=2, tag="hair_shell")
    # The swept ridge: a low pompadour combed up from the hairline and back.
    m.cube(hair, (-3.5, 29.0, -4.2), (7, 0.8, 4.5), none_on(("down",), lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.12 if f == "up" else 0.95)),
           density=2, tag="quiff", rotation=(-8, 0, 0), pivot=(0, 29, -4))
    # Down the back to the collar, the ends flicking out.
    m.cube(hair, (-4, 22.5, 2.0), (8, 5.0, 2.0), none_on(("up", "north", "down"), lambda f, x, y, w, h: None if y == h - 1 and
           (x * 3) % 4 == 0 else hr(f, x, y, w, h)), inflate=0.2, density=2, tag="hair_back")
    # The eyes of the archangel (shown with the wings): gold over the irises, the light in the glowmask.
    eg = m.rig.get("eyes_glow")
    for x0 in (-3.0, 1.5):
        m.cube(eg, (x0, 25.0, -4.12), (1.5, 0.5, 0), lambda f, x, y, w, h: GOLD_EYE,
               glow=lambda f, x, y, w, h, c: hexc("#fff2b8"), faces=("north",), density=2, tag="eye_glow")
    # The nurse doubles' cap: a white folded cap with a red cross.
    nc = m.rig.get("nurse_cap")

    def cap(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north" and abs(x - (w - 1) / 2) < 1.6 and abs(y - (h - 1) / 2) < 1.6 and (abs(x - (w - 1) / 2) < 0.6 or abs(y - (h - 1) / 2) < 0.6):
            return hexc("#d4202c")
        return COAT[4.2 - 0.8 * y / max(1, h)] if f != "south" else COAT[3]
    # It sits on top of his quiff (which reaches y 29.8), not in it.
    m.cube(nc, (-2.5, 30.2, -2.8), (5, 2.2, 2.6), cap, inflate=0.1, density=4, tag="nurse_cap", rotation=(-12, 0, 0), pivot=(0, 30.2, -1.5))
    m.cube(nc, (-3.0, 29.7, -1.6), (6, 1.0, 2.2), lambda f, x, y, w, h: COAT[3.4] if f != "down" else None, density=2, tag="cap_band")


# --- base body (every costume paints it in its own colours) ---------------------------------------------------------

def torso_m(costume):
    """The shirt on the torso: what each costume shows in the opening of its jacket."""
    if costume == "jacket":
        cloth = cotton_m(DARK_SHIRT, 7611, 2.9)

        def m(f, x, y, w, h):
            if f == "north" and x in (w // 2 - 1, w // 2) and y < 8:
                return DARK_SHIRT[1] if x == w // 2 - 1 else (STEEL[3] if y in (2, 5) else DARK_SHIRT[3])
            return cloth(f, x, y, w, h)
        return m
    if costume == "sweater":
        cloth = cotton_m(OXFORD, 7612, 3.1)

        def m(f, x, y, w, h):
            if f == "north" and x == w // 2 and y % 4 == 2:
                return OXFORD[5]
            if f == "north" and x == w // 2:
                return OXFORD[2]
            return cloth(f, x, y, w, h)
        return m
    if costume == "tuxedo":
        cloth = cotton_m(TUX_SHIRT, 7613, 3.6)

        def m(f, x, y, w, h):
            if f == "north":
                d = abs(x - (w - 1) / 2)
                if d < 0.6 and y % 4 == 2:
                    return SATIN[2]                     # the studs
                if 1.5 < d < 4.5 and x % 2 == 0:
                    return TUX_SHIRT[2]                 # the pleats
            return cloth(f, x, y, w, h)
        return m
    if costume == "lab_coat":
        cloth = cotton_m(SCRUB, 7614, 3.4)

        def m(f, x, y, w, h):
            if f == "north" and abs(x - (w - 1) / 2) < 1.6 and y < 18:
                return NAVY[2.5 + 0.8 * ((y + x) % 3 == 0)] if y > 0 else NAVY[1]
            return cloth(f, x, y, w, h)
        return m
    cloth = cotton_m(TUX_SHIRT, 7615, 3.5)

    def m(f, x, y, w, h):
        if f == "north" and abs(x - (w - 1) / 2) < 1.6 and y < 17:
            d = abs(x - (w - 1) / 2)
            if y > 14 and d > 1.6 - (17 - y) * 0.5:
                return cloth(f, x, y, w, h)
            return RED_TIE[3.4 - 0.9 * d + 0.4 * ((y // 2) % 2)] if y > 0 else RED_TIE[1]
        return cloth(f, x, y, w, h)
    return m


def sleeve_m(costume):
    return {"jacket": canvas_m(OLIVE, 7621),
            "sweater": cable_m(OAT, 7622),
            "tuxedo": sequin_m(7623),
            "lab_coat": cotton_m(COAT, 7624, 3.6),
            "suit": smooth_m(COBALT, 7625, 3.0)}[costume]


def legs_m(costume, seed):
    return {"jacket": denim_m(DENIM, seed),
            "sweater": trouser_m(KHAKI, seed, 3.0),
            "tuxedo": trouser_m(SATIN, seed, 2.4, stripe=SATIN.tinted(hexc("#9aa0b0"), 0.25)),
            "lab_coat": trouser_m(SLACKS, seed, 2.8),
            "suit": trouser_m(COBALT, seed, 2.9)}[costume]


def shoe_m(costume, seed):
    return {"jacket": boot_m(BOOT, seed, laces=hexc("#c9b48a")),
            "sweater": boot_m(LOAFER, seed, toe_shine=0.3),
            "tuxedo": boot_m(SHOE, seed, toe_shine=1.0),
            "lab_coat": tooled_m(COWBOY, seed),
            "suit": boot_m(SHOE, seed, laces=SHOE[3], toe_shine=1.0)}[costume]


def belt_m(costume):
    if costume == "tuxedo":
        # A black satin cummerbund in pleats.
        return lambda f, x, y, w, h: SATIN[2.2 + (0.9 if y % 2 == 0 else 0)] if f not in ("up", "down") else SATIN[1]
    ramp = BELT

    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return ramp[1]
        c = ramp[2.8 + 0.4 * math.sin(x * 0.7)]
        if f == "north" and abs(x - (w - 1) / 2) < 1.5:
            return STEEL[4] if (y == 0 or y == h - 1 or abs(x - (w - 1) / 2) > 0.9) else STEEL[2]
        return shade(c, 0.8) if y in (0, h - 1) else c
    return m


def build_base(m, costume):
    body = "body"
    m.cube(body, (-TW, HIP, -2), (2 * TW, NECK - HIP, 4), torso_m(costume), density=2, tag="torso")
    m.cube(body, (-TW, HIP - 0.4, -2), (2 * TW, 1.2, 4), belt_m(costume), inflate=0.3, density=2, tag="belt")
    sl = sleeve_m(costume)
    hand = H.skin_mat(SKIN, 7631, 3.2, 0.4)
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.5 - ELBOW, AW), sl, density=2, tag="upper_arm")
        m.cube(f"{side}_forearm", (cx - 1.6, WRIST, -1.6), (3.2, ELBOW - WRIST, 3.2), sl, density=2, tag="forearm")
        # The hand: a palm, a block of fingers curled under, a thumb at the front.
        m.cube(f"{side}_hand", (cx - 1.25, 10.3, -1.4), (2.5, 1.8, 2.8), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.92 if (f in ("east", "west") and x % 2) else 1.0)
        if side == "right":
            m.cube("right_fingers", (cx - 1.2, 9.4, -1.35), (2.4, 1.0, 0.75), fing, density=2, tag="index")
            m.cube("right_fingers", (cx - 1.2, 9.5, 0.15), (2.4, 1.0, 1.2), fing, density=2, tag="ring")
            m.cube("right_middle", (cx - 1.2, 9.3, -0.6), (2.4, 1.1, 0.75), fing, density=2, tag="middle")
        else:
            m.cube("left_fingers", (cx - 1.2, 9.4, -1.35), (2.4, 1.0, 2.7), fing, density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 0.9 * s - 0.5, 10.6, -2.0), (1.0, 1.4, 0.9), hand, density=2, tag="thumb")
        lm = legs_m(costume, 7640 + (s > 0))
        m.cube(f"{side}_leg", (LX * s - LW / 2, KNEE, -LW / 2), (LW, HIP - KNEE, LW), lm, density=2, tag="thigh")
        m.cube(f"{side}_shin", (LX * s - LW / 2, 1.5, -LW / 2), (LW, KNEE - 1.5, LW), lm, density=2, tag="shin")
        m.cube(f"{side}_shin", (LX * s - LW / 2 - 0.1, 0, -2.75), (LW + 0.2, 1.5, 4.6), shoe_m(costume, 7650 + (s > 0)),
               density=2, tag="shoe")
        # The trouser hem breaking over the shoe.
        m.cube(f"{side}_shin", (LX * s - LW / 2, 1.5, -LW / 2), (LW, 0.8, LW), none_on(("up", "down"),
               lambda f, x, y, w, h, lm=lm: shade(lm(f, x, y + 6, w, h), 0.86)), inflate=0.2, density=2, tag="hem")


def open_shell(mat, gap_top, gap_bottom, v_depth, lapel=None, closed_from=None, buttons=None, button_rows=()):
    """A jacket's shell over the torso: open in a V (gap_top wide at the neck narrowing to gap_bottom at v_depth rows
    down), then open straight (or closed from `closed_from`), with an optional lapel colour along the opening's edge.
    No bottom face; the top face has a neck hole."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f == "up":
            if abs(x - mid) <= gap_top + 0.5 and y < h * 0.7:
                return None
            return mat(f, x, y, w, h)
        if f == "north":
            if closed_from is not None and y >= closed_from:
                if buttons is not None and abs(x - mid) < 0.6 and y in button_rows:
                    return buttons
                if abs(x - mid) < 0.6:
                    return shade(mat(f, x, y, w, h), 0.78)
                return mat(f, x, y, w, h)
            half = gap_top + (gap_bottom - gap_top) * min(1.0, y / max(1, v_depth))
            if abs(x - mid) <= half:
                return None
            if lapel is not None and abs(x - mid) <= half + 1.2:
                return lapel(f, x, y, w, h) if callable(lapel) else lapel
        return mat(f, x, y, w, h)
    return m


# --- the jacket (his own clothes: base pieces, clear on the other textures) ------------------------------------------

def build_jacket(m, costume):
    j = lambda mat: only(costume, "jacket", mat)
    cv = canvas_m(OLIVE, 7701)
    cord = lambda f, x, y, w, h: CORD[2.4 + (0.9 if x % 2 == 0 else 0) - 0.4 * y / max(1, h)]

    def edged(f, x, y, w, h):
        c = cv(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and y == h - 1:
            return OLIVE[1]
        if f in ("north", "south", "east", "west") and y == h - 3:
            return shade(c, 1.12)       # topstitching above the hem band
        return c
    m.cube("body", (-TW, HIP - 1.0, -2), (2 * TW, NECK - HIP + 0.6, 4), j(open_shell(edged, 2.2, 1.6, 8, lapel=OLIVE[4])),
           inflate=0.38, density=2, tag="jacket")
    # The corduroy collar, open at the throat.
    def collar(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 3.2 and (f == "north" or y < h * 0.65):
            return None
        return cord(f, x, y, w, h)
    m.cube("body", (-TW, NECK - 1.2, -2), (2 * TW, 1.8, 4), j(collar), inflate=0.75, density=2, tag="collar")
    # Collar points folded down over the shoulders of the jacket.
    for s in (-1, 1):
        m.cube("body", (2.0 * s - 0.75, NECK - 1.9, -2.72), (1.5, 1.2, 0.3), j(lambda f, x, y, w, h: None if f == "north" and y == h - 1 and
               (x == 0 if s > 0 else x == w - 1) else cord(f, x, y, w, h)), density=2, tag="collar_point", rotation=(0, 0, -30 * s),
               pivot=(2.0 * s, NECK - 0.7, -2.6))
    # Two chest pockets with buttoned flaps, two patch pockets low down.
    for s in (-1, 1):
        def flap(f, x, y, w, h):
            if f == "north" and x == w // 2 and y == h - 1:
                return hexc("#2a2418")
            return OLIVE[3.6 if y == 0 else 2.6]
        m.cube("body", (2.1 * s - 1.1, 17.6, -2.62), (2.2, 2.3, 0.3), j(lambda f, x, y, w, h: OLIVE[2.3 + (y == 0)]), density=2,
               tag="chest_pocket")
        m.cube("body", (2.1 * s - 1.2, 19.3, -2.72), (2.4, 0.8, 0.35), j(flap), density=4, tag="pocket_flap")
        m.cube("body", (2.2 * s - 1.2, 11.0, -2.62), (2.4, 2.4, 0.3), j(lambda f, x, y, w, h: OLIVE[2.4] if y else OLIVE[3.6]),
               density=2, tag="hip_pocket")
    # Shoulder epaulets and cuffs.
    for side, s in (("right", -1), ("left", 1)):
        m.cube(f"{side}_arm", (AX * s - 0.8, SY + 0.55, -1.6), (1.6, 0.3, 3.2), j(lambda f, x, y, w, h: OLIVE[3.4] if f == "up" else OLIVE[2]),
               density=2, tag="epaulet")
        m.cube(f"{side}_forearm", (AX * s - 1.6, WRIST, -1.6), (3.2, 0.9, 3.2), j(none_on(("up", "down"), lambda f, x, y, w, h: OLIVE[2.2 + (y == 0)])),
               inflate=0.3, density=2, tag="jacket_cuff")
        m.cube(f"{side}_arm", (AX * s - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.5 - ELBOW, AW), j(none_on(("down",), cv)), inflate=0.18,
               density=2, tag="jacket_sleeve")


# --- costume groups -----------------------------------------------------------------------------------------------------

def sleeves(m, g, mat, cuff, inflate=0.55, cuff_inflate=0.7, cuff_h=0.8, glow=None):
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{g}_{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.5 - ELBOW, AW), none_on(("down",), mat), glow=glow,
               inflate=inflate, density=2, tag="sleeve")
        m.cube(f"{g}_{side}_forearm", (cx - 1.6, WRIST + cuff_h - 0.2, -1.6), (3.2, ELBOW - WRIST - cuff_h + 0.2, 3.2),
               none_on(("up", "down"), mat), glow=glow, inflate=inflate - 0.05, density=2, tag="sleeve")
        if cuff is not None:
            m.cube(f"{g}_{side}_forearm", (cx - 1.6, WRIST, -1.6), (3.2, cuff_h, 3.2), none_on(("up",), cuff), inflate=cuff_inflate,
                   density=2, tag="cuff")


def build_sweater(m):
    g = "sweater"
    body = f"{g}_body"
    kn = cable_m(OAT, 7801, band=RUST)
    knit_plain = cable_m(OAT, 7802)
    hem = rib_m(RUST, 2.8)
    m.cube(body, (-TW, HIP - 0.8, -2), (2 * TW, NECK - HIP + 0.4, 4),
           open_shell(kn, 1.8, 0.4, 11, closed_from=11, buttons=WOOD[4], button_rows=(13, 16, 19)), inflate=0.8, density=2,
           tag="cardigan")
    m.cube(body, (-TW, HIP - 1.0, -2), (2 * TW, 1.2, 4), none_on(("up",), hem), inflate=0.95, density=2, tag="rib_hem")
    # The shawl collar: a thick rolled band up each side of the V and round the back of the neck.
    for s in (-1, 1):
        m.cube(body, (1.7 * s - 0.9, 15.6, -3.3), (1.8, 5.8, 0.9), lambda f, x, y, w, h: OAT[3.6 - 0.6 * (x % 2) - 0.5 * y / max(1, h)],
               density=2, tag="shawl", rotation=(5, 0, 17 * s), pivot=(0.8 * s, 15.6, -3.0))
    m.cube(body, (-3.6, NECK - 0.8, 0.0), (7.2, 1.6, 2.2), lambda f, x, y, w, h: OAT[3.4 - 0.5 * (x % 2)], inflate=0.3, density=2,
           tag="shawl_back")
    # Wooden toggle buttons and the patch pockets.
    for y in (11.6, 13.1):
        m.cube(body, (-0.45, y, -3.05), (0.9, 0.7, 0.3), lambda f, x, yy, w, h: WOOD[3.6 if yy == 0 else 2.4], density=4, tag="button")
    for s in (-1, 1):
        m.cube(body, (2.2 * s - 1.3, 10.6, -3.0), (2.6, 2.6, 0.3), lambda f, x, y, w, h: RUST[2.6 + (0.8 if y == 0 else 0)] if y < 2
               else knit_plain(f, x, y, w, h), density=2, tag="pocket")
    sleeves(m, g, knit_plain, rib_m(RUST, 2.8), inflate=0.6, cuff_inflate=0.75, cuff_h=1.0)
    # Suede elbow patches.
    for side, s in (("right", -1), ("left", 1)):
        m.cube(f"{g}_{side}_arm", (AX * s - 1.0, ELBOW - 0.2, AW / 2 + 0.55), (2.0, 2.4, 0.2),
               lambda f, x, y, w, h: SUEDE[2.6 + 0.5 * h01("su", x, y)], density=2, tag="elbow_patch")


def build_tuxedo(m):
    g = "tuxedo"
    body = f"{g}_body"
    sq = sequin_m(7901)
    lap = satin_m(SATIN, 7902)
    m.cube(body, (-TW, HIP - 0.6, -2), (2 * TW, NECK - HIP + 0.2, 4), open_shell(sq, 2.4, 0.9, 13, closed_from=13,
           buttons=SATIN[3], button_rows=(14,)), glow=sequin_glow, inflate=0.5, density=2, tag="tux")
    # Below the button the fronts cut away over the hips.
    m.cube(body, (-TW, HIP - 1.6, -2), (2 * TW, 1.6, 4), none_on(("up", "down"), lambda f, x, y, w, h: None if f == "north" and
           abs(x - (w - 1) / 2) < 2.5 + y * 1.2 else sq(f, x, y, w, h)), glow=sequin_glow, inflate=0.55, density=2, tag="tux_skirt")
    # Black satin shawl lapels from the shoulder to the button.
    for s in (-1, 1):
        m.cube(body, (1.75 * s - 1.0, 14.2, -2.98), (2.0, 6.6, 0.45), lambda f, x, y, w, h: lap(f, x, y, w, h), density=2, tag="lapel",
               rotation=(4, 0, 15 * s), pivot=(0.9 * s, 14.2, -2.7))
    m.cube(body, (-3.4, NECK - 0.8, 0.2), (6.8, 1.4, 2.0), lap, inflate=0.35, density=2, tag="collar")
    # The bow tie: two wings and a knot.
    bow = lambda f, x, y, w, h: SATIN[2.2 + (0.8 if (f == "north" and (y == 0 or x in (0, w - 1))) else 0)]
    for s in (-1, 1):
        m.cube(body, (1.15 * s - 0.9, NECK - 1.65, -2.95), (1.8, 1.4, 0.5), bow, density=4, tag="bow", rotation=(0, 0, -8 * s),
               pivot=(0, NECK - 1.0, -2.8))
    m.cube(body, (-0.45, NECK - 1.5, -3.1), (0.9, 1.0, 0.5), solid(SATIN[3]), density=4, tag="bow_knot")
    # A red carnation on his left lapel, a pocket square on the right.
    m.cube(body, (1.9, 18.4, -3.35), (1.1, 1.1, 0.6), lambda f, x, y, w, h: CARNATION[2.6 + 1.6 * h01("car", f, x, y)], density=4,
           tag="carnation")
    m.cube(body, (-3.0, 18.6, -2.95), (1.4, 0.7, 0.3), lambda f, x, y, w, h: TUX_SHIRT[5] if y == 0 else TUX_SHIRT[3], density=4,
           tag="pocket_square")
    sleeves(m, g, sq, lambda f, x, y, w, h: TUX_SHIRT[4] if f != "down" else TUX_SHIRT[2], inflate=0.45, cuff_inflate=0.6, cuff_h=0.7,
            glow=sequin_glow)
    for side, s in (("right", -1), ("left", 1)):
        m.cube(f"{g}_{side}_forearm", (AX * s + 0.6 * s - 0.3, WRIST + 0.15, -0.3), (0.6, 0.4, 0.6), solid(SEQUIN[5]),
               glow=lambda f, x, y, w, h, c: c, density=4, tag="cufflink")


def build_lab_coat(m):
    g = "lab_coat"
    body = f"{g}_body"
    ct = cotton_m(COAT, 8001, 3.7)

    def lapel(f, x, y, w, h):
        return COAT[4.4]
    m.cube(body, (-TW, HIP, -2), (2 * TW, NECK - HIP, 4), open_shell(ct, 2.2, 1.2, 10, lapel=lapel, closed_from=10, buttons=COAT[1],
           button_rows=(11, 15, 19)), inflate=0.55, density=2, tag="coat")
    m.cube(body, (-TW, NECK - 1.0, -2), (2 * TW, 1.6, 4), lambda f, x, y, w, h: None if f in ("down",) or (f in ("north", "up") and
           abs(x - (w - 1) / 2) <= 2.6 and (f == "north" or y < h * 0.6)) else COAT[4.0 - 0.4 * y / max(1, h)], inflate=0.75, density=2,
           tag="collar")
    # Notch lapels.
    for s in (-1, 1):
        def lp(f, x, y, w, h, s=s):
            if f == "north" and y < 2 and (x >= w - 1 if s < 0 else x == 0):
                return None
            return COAT[4.5 - 0.7 * y / max(1, h)] if f == "north" else COAT[3]
        m.cube(body, (1.9 * s - 1.0, 15.4, -2.75), (2.0, 5.0, 0.4), lp, density=2, tag="lapel", rotation=(3, 0, 16 * s),
               pivot=(0.9 * s, 15.4, -2.5))
    # Breast pocket with pens, the badge.
    m.cube(body, (1.0, 16.4, -2.65), (2.2, 2.2, 0.3), lambda f, x, y, w, h: COAT[2.4] if y == 0 else COAT[3.6], density=2, tag="pocket")
    for i, col in enumerate((hexc("#1f3f9a"), hexc("#b0151d"))):
        m.cube(body, (1.35 + i * 0.75, 17.9, -2.7), (0.4, 1.4, 0.4), lambda f, x, y, w, h, col=col: col if y else STEEL[4], density=4,
               tag="pen")

    def badge(f, x, y, w, h):
        if f != "north":
            return COAT[2]
        if y == 0:
            return hexc("#1a3a8f")
        if y in (2, 4) and 1 <= x <= w - 2 and (x + y) % 3:
            return hexc("#b0151d") if y == 2 else hexc("#3a3a40")
        return hexc("#fbfbf6")
    m.cube(body, (-3.3, 17.3, -2.7), (2.2, 1.5, 0.25), badge, density=4, tag="badge")
    # The stethoscope: tubing round the back of the neck, down the front either side, the chest piece, ear tips.
    tube = lambda f, x, y, w, h: TUBE[2.6 + (0.7 if f in ("north", "up") else 0)]
    m.cube(body, (-2.6, NECK - 0.2, -0.6), (5.2, 0.5, 2.8), none_on(("down",), tube), density=4, tag="steth_neck")
    for s in (-1, 1):
        L = 4.6 if s > 0 else 3.2
        m.cube(body, (2.5 * s - 0.25, NECK - 0.2 - L, -2.95), (0.5, L, 0.5), tube, density=4, tag="steth_tube",
               rotation=(0, 0, 6 * s), pivot=(2.5 * s, NECK - 0.2, -2.7))
    m.cube(body, (2.0, 15.1, -3.3), (1.4, 1.4, 0.5), lambda f, x, y, w, h: STEEL[5] if f == "north" and (x + y) % 3 == 0 else STEEL[3.5],
           density=4, tag="steth_chest")
    m.cube(body, (-2.85, NECK - 3.8, -3.2), (0.6, 0.6, 0.6), solid(STEEL[4]), density=4, tag="steth_ear")
    # The coat's skirt to the knees: front-and-side panels on the legs (open at the inner face), a back panel.
    for side, s in (("right", -1), ("left", 1)):
        def skirt(f, x, y, w, h, s=s):
            if f in ("up", "down"):
                return None
            if f == ("west" if s > 0 else "east"):
                return None   # between the legs
            if f == "north" and ((s < 0 and x >= w - 1) or (s > 0 and x == 0)):
                return COAT[2]
            c = ct(f, x, y, w, h)
            if y >= h - 1:
                c = shade(c, 0.85)
            return c
        m.cube(f"{g}_{side}_leg", (LX * s - LW / 2, 4.6, -LW / 2), (LW, HIP - 4.6, LW), skirt, inflate=0.7, density=2, tag="coat_skirt")
    m.cube(body, (-TW - 0.3, 4.4, 2.15), (2 * TW + 0.6, HIP - 4.4 + 0.5, 0.5), lambda f, x, y, w, h: None if f in ("up",) else
           (shade(ct(f, x, y, w, h), 0.85) if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.5 else ct(f, x, y, w, h)),
           density=2, tag="coat_back", rotation=(8, 0, 0), pivot=(0, HIP, 2.2))
    sleeves(m, g, ct, lambda f, x, y, w, h: COAT[3.4] if y == 0 else COAT[4], inflate=0.5, cuff_inflate=0.55, cuff_h=0.7)
    # Cowboy boots: tall tooled shafts, a pointed toe, a riding heel.
    tl = tooled_m(COWBOY, 8011)
    for side, s in (("right", -1), ("left", 1)):
        sh = f"{g}_{side}_shin"
        m.cube(sh, (LX * s - LW / 2, 1.4, -LW / 2), (LW, 3.2, LW), none_on(("down",), lambda f, x, y, w, h: COWBOY[1] if f == "up" else tl(f, x, y, w, h)),
               inflate=0.32, density=2, tag="boot_shaft")
        m.cube(sh, (LX * s - 0.9, 0.1, -3.9), (1.8, 1.0, 1.4), lambda f, x, y, w, h: COWBOY[3.6 if f == "up" else 2.6], density=2,
               tag="boot_toe")
        m.cube(sh, (LX * s - 1.2, -0.0, 1.0), (2.4, 0.8, 1.4), solid(COWBOY[1]), density=2, tag="boot_heel")


def build_suit(m):
    g = "suit"
    body = f"{g}_body"
    st = smooth_m(COBALT, 8101, 3.0)
    m.cube(body, (-TW, HIP - 0.8, -2), (2 * TW, NECK - HIP + 0.4, 4), open_shell(st, 2.4, 0.9, 12, closed_from=12, buttons=SHOE[4],
           button_rows=(13, 16)), inflate=0.48, density=2, tag="suit")
    m.cube(body, (-TW, NECK - 1.0, -2), (2 * TW, 1.5, 4), lambda f, x, y, w, h: None if f == "down" or (f in ("north", "up") and
           abs(x - (w - 1) / 2) <= 2.6 and (f == "north" or y < h * 0.6)) else COBALT[3.6], inflate=0.7, density=2, tag="collar")
    # Sharp peak lapels (the peak points up and out), a white shirt collar, the tie's Windsor knot.
    for s in (-1, 1):
        def lp(f, x, y, w, h, s=s):
            c = tw(COBALT, 4.3 - 0.8 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = COBALT[2]
            return c
        m.cube(body, (1.85 * s - 1.05, 14.6, -2.9), (2.1, 6.0, 0.45), lp, density=2, tag="lapel", rotation=(3, 0, 17 * s),
               pivot=(0.9 * s, 14.6, -2.65))
        m.cube(body, (3.0 * s - 0.6, 19.4, -2.95), (1.2, 1.3, 0.4), lambda f, x, y, w, h: COBALT[4.4], density=4, tag="lapel_peak",
               rotation=(0, 0, -38 * s), pivot=(3.0 * s, 19.4, -2.8))
        m.cube(body, (0.9 * s - 0.75, NECK - 1.3, -2.5), (1.5, 1.2, 0.4), lambda f, x, y, w, h: TUX_SHIRT[5] if y == 0 else TUX_SHIRT[4],
               density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, NECK - 0.2, -2.3))
    m.cube(body, (-0.6, NECK - 1.6, -2.65), (1.2, 1.1, 0.5), lambda f, x, y, w, h: RED_TIE[3.4 if y == 0 else 2.8], density=4, tag="tie_knot")
    # The pocket square in three peaks, a gold lapel pin, flap pockets.
    def square(f, x, y, w, h):
        if f == "north" and y == 0 and x % 3 == 1:
            return None
        return TUX_SHIRT[5] if y < 2 else TUX_SHIRT[3]
    m.cube(body, (1.6, 18.0, -2.9), (1.6, 0.8, 0.3), square, density=4, tag="pocket_square")
    m.cube(body, (-3.2, 17.3, -3.25), (0.5, 0.5, 0.3), solid(SEQUIN[4]), density=4, tag="lapel_pin")
    for s in (-1, 1):
        m.cube(body, (2.2 * s - 1.3, 11.2, -2.85), (2.6, 0.6, 0.35), lambda f, x, y, w, h: COBALT[1.5 if y else 3.8], density=4, tag="flap")
    sleeves(m, g, st, lambda f, x, y, w, h: TUX_SHIRT[4] if f != "down" else TUX_SHIRT[2], inflate=0.45, cuff_inflate=0.6, cuff_h=0.6)
    for side, s in (("right", -1), ("left", 1)):
        m.cube(f"{g}_{side}_forearm", (AX * s + 0.6 * s - 0.3, WRIST + 0.1, -0.3), (0.6, 0.4, 0.6), solid(SEQUIN[4]), density=4,
               tag="cufflink")


# --- props ---------------------------------------------------------------------------------------------------------------

def swirl(f, x, y, w, h):
    """A lollipop's rainbow spiral (red, white, orange, blue) on its flat faces, the rim on the sides."""
    if f in ("up", "down"):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = x - cx, y - cy
        r = math.hypot(dx, dy) / (w / 2)
        if r > 1.02:
            return None
        a = math.atan2(dy, dx)
        k = int(((a / (2 * math.pi)) * 4 + r * 3.2) % 4)
        c = CANDY[k]
        if r > 0.85:
            c = shade(c, 0.82)
        if dx < -1 and dy < -1 and r < 0.7:
            c = mix(c, hexc("#ffffff"), 0.35)       # the gloss
        return c
    return shade(CANDY[(x + y) % 2 * 2], 0.85)


def build_props(m):
    # The lollipop, in his left fist: the stick runs along the grip (front-back), the candy at its front end lying flat;
    # with the forearm raised it stands up with the candy facing forward.
    lp = m.bone("lollipop", (AX, 10.9, -0.2), "left_hand")
    m.cube(lp, (AX - 0.2, 10.7, -3.6), (0.4, 0.4, 4.4), solid(STICK), density=4, tag="stick")
    m.cube(lp, (AX - 1.5, 10.65, -6.6), (3.0, 0.5, 3.0), swirl, density=4, tag="candy")
    # The cream pie, on his right palm (the palm faces in when the arm hangs: the cream faces +X, his left).
    pie = m.bone("pie", (-AX + 1.25, 11.0, 0), "right_hand")
    px = -AX + 1.25
    m.cube(pie, (px, 7.8, -3.2), (0.75, 6.4, 6.4), pie_tin_side, density=4, tag="pie_tin")
    m.cube(pie, (px + 0.75, 8.0, -3.0), (0.75, 6.0, 6.0), cream_face, density=4, tag="pie_cream")
    m.cube(pie, (px + 1.5, 10.25, -0.75), (0.5, 1.5, 1.5), lambda f, x, y, w, h: CREAM[5] if (x + y) % 3 else CREAM[3], density=4, tag="dollop")
    m.cube(pie, (px + 2.0, 10.75, -0.25), (0.5, 0.5, 0.5), solid(CHERRY), density=4, tag="cherry")
    # The game-show microphone: a long slim black stick mic, its grille forward of the thumb.
    mic = m.bone("microphone", (-AX, 10.9, -0.2), "right_hand")
    m.cube(mic, (-AX - 0.45, 10.45, -4.6), (0.9, 0.9, 6.4), lambda f, x, y, w, h: MIC[2.4 + (0.9 if f in ("up",) else 0) + (0.5 if y == 0 else 0)],
           density=4, tag="mic_handle")
    m.cube(mic, (-AX - 0.75, 10.15, -6.1), (1.5, 1.5, 1.6), lambda f, x, y, w, h: GRILLE[3.6 - 0.9 * ((x + y) % 2)], density=4, tag="mic_head")
    m.cube(mic, (-AX - 0.55, 10.35, -4.75), (1.1, 1.1, 0.3), solid(hexc("#c41a23")), density=4, tag="mic_band")
    m.cube(mic, (-AX - 0.25, 10.65, 1.8), (0.5, 0.5, 2.4), solid(MIC[1]), density=4, tag="mic_cord")
    # The defibrillator paddles: a grey handle in each fist, the plate under it (electrode down, a yellow button on top).
    d = m.bone("defibrillator", (-AX, 10.9, -0.2), "right_hand")
    build_paddle(m, d, -AX)
    m.cube(d, (-AX - 0.3, 9.8, 1.4), (0.6, 0.6, 3.0), solid(TUBE[2]), density=4, tag="defib_cable")
    dl = m.bone("defib_paddle_left", (AX, 10.9, -0.2), "defibrillator")
    build_paddle(m, dl, AX)


def build_paddle(m, bone, cx):
    m.cube(bone, (cx - 0.5, 10.4, -2.3), (1.0, 1.0, 4.0), lambda f, x, y, w, h: PADDLE[3.4 if f == "up" else 2.4], density=4, tag="paddle_grip")
    m.cube(bone, (cx - 0.3, 11.35, -2.0), (0.6, 0.3, 0.6), solid(SAFETY), density=4, tag="paddle_button")

    def plate(f, x, y, w, h):
        if f == "down":
            return STEEL[4] if (x + y) % 4 else STEEL[5]
        if f == "up":
            return PADDLE[2.8]
        return PADDLE[1.6]
    m.cube(bone, (cx - 1.6, 9.0, -2.5), (3.2, 0.7, 4.4), plate, density=4, tag="paddle_plate")
    m.cube(bone, (cx - 0.4, 9.7, -0.5), (0.8, 0.8, 0.8), solid(PADDLE[2]), density=4, tag="paddle_neck")


def pie_tin_side(f, x, y, w, h):
    if f == "west":
        cx, cy = (w - 1) / 2, (h - 1) / 2
        r = math.hypot(x - cx, y - cy) / (w / 2)
        return PIE_TIN[3.6 - 1.4 * r + 0.4 * ((x + y) % 3 == 0)]
    return PIE_TIN[2.6 + 0.6 * math.sin(x + y)] if (x + y) % 2 else CRUST[3]


def cream_face(f, x, y, w, h):
    if f == "east":
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = x - cx, y - cy
        r = math.hypot(dx, dy) / (w / 2)
        if r > 0.92:
            return CRUST[3.2 + 0.8 * math.sin(math.atan2(dy, dx) * 9)]   # the crimped crust round the rim
        sw = math.sin(r * 9 - math.atan2(dy, dx) * 2)
        return CREAM[3.6 + 0.9 * sw]
    return CREAM[3.2] if y % 2 else CRUST[3.4]


# --- the six golden shadow wings ---------------------------------------------------------------------------------------

WING_SPECS = {
    # pair: (root y, root |x|, leading edge (out, rise) px, feather length scale, rest rotation of the right wing)
    1: (19.6, 1.4, (22.0, 13.0), 1.0, (0, 26, 6)),
    2: (17.8, 1.6, (25.0, 1.0), 0.92, (0, 30, -4)),
    3: (16.0, 1.4, (19.0, -10.0), 0.78, (0, 34, -10)),
}
FLIGHT, COVERT = 10, 5
TIP_FROM = 6     # flight feathers from this index sit on the wing's tip bone


class GoldFeather:
    """A shadow feather of smoke and golden light on a flat plane (y = 0 at the root): a dark amber-smoke core, soft
    alpha falling off towards the tip and the edges, a luminous gold rim and tip, wisps dissolving at the point."""

    def __init__(self, seed, alpha=215, broad=False):
        self.seed, self.alpha, self.broad = seed, alpha, broad

    def half(self, t, w):
        hw = (w - 1) / 2 + 0.3
        if self.broad:
            return hw * (math.sqrt(max(0.0, 1 - ((t - 0.5) / 0.5) ** 2)) if t > 0.5 else min(1.0, 0.6 + t))
        if t < 0.1:
            return hw * (0.55 + 4.5 * t)
        return hw * (1.0 if t < 0.55 else max(0.08, 1 - (t - 0.55) / 0.45))

    def mat(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        hw = self.half(t, w)
        dx = abs(x - c)
        if dx > hw:
            return None
        if t > 0.74 and h01("wisp", self.seed, face, x, y) < (t - 0.74) * 1.8:
            return None
        n = fbm(x * 6 + len(face), y * 1.5, self.seed, 64, 64, 3, 5.0)
        q = dx / max(0.5, hw)
        a = self.alpha * (0.95 - 0.55 * q) * (1 - 0.45 * t) * (0.7 + 0.6 * n)
        col = mix(SMOKE_G[1 + int(n * 2.5)], LIGHT_G[0], 0.18 + 0.3 * n * n)
        if q < 0.12 and t < 0.85:
            col, a = mix(LIGHT_G[0], SMOKE_G[2], 0.55), a + 20      # a faint golden shaft
        if q > 0.8 and t < 0.9:
            col, a = mix(col, LIGHT_G[3], 0.55 * (q - 0.8) / 0.2 + 0.25), a + 30   # the glowing rim
        tip = max(0.0, (t - 0.62) / 0.38)
        if tip > 0:
            col = mix(col, LIGHT_G[4], min(1.0, tip * 1.15))
            a = a * (1 - 0.2 * tip) + 55 * tip
        return (col[0], col[1], col[2], max(18, min(230, int(a))))

    @staticmethod
    def glow(face, x, y, w, h, col):
        if col[0] > 170 and col[1] > 110:
            return (col[0], col[1], col[2], 255)
        if col[0] > 120 and col[1] > 75:
            return shade(col, 0.55)
        return None


def feather_cube(m, bone, root, length, width, angle, s, mat, glow, dz=0.0, tilt=0.0, tag="feather"):
    x, y, z = root
    return m.cube(bone, (x - width / 2, y - length, z + dz), (width, length, 0), mat, glow, density=2, tag=tag,
                  rotation=(tilt, 0, -s * angle), pivot=(x, y, z), faces=("north", "south"))


def build_wings(m):
    w_all = m.bone("wings", (0, 18.0, 2.2), "body")
    for pair, (ry, rx, (out, rise), k, rest) in WING_SPECS.items():
        for side, s in (("r", -1), ("l", 1)):
            name = f"wing_{side}{pair}"
            root = (rx * s, ry, 2.4 + 0.15 * pair)
            w = m.bone(name, root, w_all.name, rotation=mirror_rot(rest, s))
            x0, y0, z0 = root
            # The wing's "wrist": where the tip bone folds, 55 % along the leading edge.
            wx, wy = x0 + s * out * 0.55, y0 + rise * 0.55
            tip = m.bone(f"{name}_tip", (wx, wy, z0), name)
            seed = 9100 + pair * 100 + (0 if s < 0 else 50)

            def arm_m(f, x, y, w_, h, s=s, seed=seed):
                t = y / max(1, h - 1)
                q = x / max(1, w_ - 1)
                if (f == "north") == (s < 0):
                    q = 1 - q
                if t > 0.5 + 0.35 * (1 - q) or q > 0.98:
                    return None
                n = fbm(x * 0.8, y * 4, seed, 64, 64, 3, 5.0)
                if t < 0.22:
                    col = mix(LIGHT_G[4], LIGHT_G[2], q)
                    return (col[0], col[1], col[2], int(200 - 320 * t))
                col = mix(SMOKE_G[2 + int(n * 2)], LIGHT_G[0], 0.25 * n)
                return (col[0], col[1], col[2], int(225 - 50 * q))
            # The leading edge in two strips (one per bone), lit along its top.
            for b, a_, b_ in ((w, (x0, y0, z0), (wx, wy, z0)), (tip, (wx, wy, z0), (x0 + s * out, y0 + rise, z0))):
                L = math.hypot(b_[0] - a_[0], b_[1] - a_[1])
                ang = math.degrees(math.atan2(b_[1] - a_[1], abs(b_[0] - a_[0])))
                xo = a_[0] - L if s < 0 else a_[0]
                m.cube(b, (xo, a_[1] - 2.0 * k, a_[2] - 0.1), (L, 4.0 * k, 0), arm_m, GoldFeather.glow, density=2,
                       faces=("north", "south"), rotation=(0, 0, -s * ang), pivot=a_, tag="wing_arm")
            for i in range(FLIGHT):
                f = i / (FLIGHT - 1)
                t = 0.12 + 0.88 * f
                px, py = x0 + s * out * t, y0 + rise * t
                length = (13 + 12 * f) * k * (0.94 + 0.12 * h01("fl", seed, i))
                angle = 4 + 78 * f ** 1.3 + (h01("fa", seed, i) - 0.5) * 5
                bone = tip if i >= TIP_FROM else w
                feather_cube(m, bone, (px, py - 0.4, z0 + 0.1 * i), length, 7.0 * k, angle, s,
                             GoldFeather(seed + i, 180).mat, GoldFeather.glow, tag="shadow_feather")
            for j in range(COVERT):
                f = j / (COVERT - 1)
                t = 0.06 + 0.75 * f
                px, py = x0 + s * out * t, y0 + rise * t
                bone = tip if t >= 0.55 else w
                feather_cube(m, bone, (px, py, z0 + 1.3), (8 + 3 * f) * k, 6.0 * k, 18 + 40 * f, s,
                             GoldFeather(seed + 30 + j, 200, broad=True).mat, GoldFeather.glow, tag="shadow_covert")


# =====================================================================================================
# Animations
# =====================================================================================================

class Clip(MClip):
    """michael_art's pose clips, with every key on a limb copied to that limb in each costume group."""

    @staticmethod
    def expand(pose):
        out = {}
        for k, v in pose.items():
            out[k] = v
            pre = k[0] if k[0] in "@%" else ""
            b = k[len(pre):]
            if b in LIMBS:
                for g in GROUPS:
                    if b in COPIES[g]:
                        out[f"{pre}{g}_{b}"] = v
        return out

    def key(self, t, pose, ease=None):
        return super().key(t, self.expand(pose), ease)

    def layer(self, name, fn):
        for k in self.expand({name: None}):
            super().layer(k, fn)
        return self


# --- kinematic helpers ------------------------------------------------------------------------------------------------

MOUTH = (0.6, 23.4, -4.4)        # Bedrock world point of his mouth at rest
_AIMS = {}


def _memo(key, fn):
    if key not in _AIMS:
        _AIMS[key] = fn()
    return _AIMS[key]


def _base(pose):
    """Only the base skeleton's channels of a pose (the costume copies never affect the props)."""
    return {k: v for k, v in pose.items() if not any(k.lstrip("@%").startswith(g + "_") for g in GROUPS)}


def aim_bone(r, pose, bone, local, target_dir, local2=None, target2=None):
    """The rotation of `bone` (relative to rest) that points its local vector along a world direction (and, with
    local2/target2, a second vector too), memoised by pose."""
    from michael_art import aim2
    key = (bone, local, target_dir, local2, target2, repr(sorted((k, tuple(v)) for k, v in _base(pose).items())))
    if local2 is None:
        return _memo(key, lambda: aim(r, _base(pose), bone, local, target_dir))
    return _memo(key, lambda: aim2(r, _base(pose), bone, local, target_dir, local2, target2))


def head_point(r, pose, p=MOUTH):
    return world_point(r, to_preview(_base(pose)), "head", p)


def toward(r, pose, bone, pivot, target):
    a = world_point(r, to_preview(_base(pose)), bone, pivot)
    return tuple(target[i] - a[i] for i in range(3))


def with_pop(r, pose, to_mouth=False):
    """The lollipop aimed: the stick up (or at his mouth) with the candy facing forward."""
    if to_mouth:
        d = toward(r, pose, "lollipop", (AX, 10.9, -0.2), head_point(r, pose))
        up = d
        # At his lips the candy turns edge-on to the front: from the front it never covers his eyes.
        face = (1, 0, 0)
    else:
        # Held low by his chest, the stick leaning out to his left: the candy beside him, not in front of his face.
        up = (0.45, 1.0, -0.25)
        face = (0, 0.15, -1)
    rot = aim_bone(r, pose, "lollipop", (0, 0, -1), tuple(round(c, 3) for c in up), (0, 1, 0), face)
    return P_(pose, {"lollipop": rot})


def with_mic(r, pose):
    d = toward(r, pose, "microphone", (-AX, 10.9, -0.2), head_point(r, pose))
    return P_(pose, {"microphone": aim_bone(r, pose, "microphone", (0, 0, -1), tuple(round(c, 3) for c in d))})


def palm_up(r, pose, side="right", forward=(0, 0, -1)):
    """The hand turned palm up, its fingers pointing `forward` (the pie on it)."""
    s = -1 if side == "right" else 1
    return P_(pose, {f"{side}_hand": aim_bone(r, pose, f"{side}_hand", (-s * 1.0, 0, 0), (0, 1, 0), (0, -1, 0), forward)})


def reach(r, pose, side, target, grip=None):
    """Arm and forearm rotations (added to `pose`) that bring the fist of `side` to a world point: coordinate descent
    from the pose's own angles. Returns the merged pose."""
    s = -1 if side == "right" else 1
    grip = grip or (AX * s, 10.9, -0.2)
    names = [(f"{side}_arm", 0), (f"{side}_arm", 1), (f"{side}_arm", 2), (f"{side}_forearm", 0), (f"{side}_forearm", 1)]
    cur = {n: list(pose.get(n, [0, 0, 0])) for n, _ in names}

    def err():
        p = P_(pose, {n: v for n, v in cur.items()})
        q = world_point(r, to_preview(_base(p)), f"{side}_hand", grip)
        return sum((q[i] - target[i]) ** 2 for i in range(3)) + 1e-5 * sum(abs(v) for vv in cur.values() for v in vv)
    key = ("reach", side, target, repr(sorted((k, tuple(v)) for k, v in _base(pose).items())))
    if key in _AIMS:
        return P_(pose, _AIMS[key])
    best = err()
    for step in (16, 8, 4, 2, 1, 0.5):
        improved = True
        while improved:
            improved = False
            for n, i in names:
                for d in (step, -step):
                    cur[n][i] += d
                    e = err()
                    if e < best - 1e-9:
                        best, improved = e, True
                    else:
                        cur[n][i] -= d
    _AIMS[key] = {n: [round(v, 1) for v in vv] for n, vv in cur.items()}
    return P_(pose, _AIMS[key])


def follow_left_fist(c, r):
    """Keys the left paddle onto his left fist, sampled densely (it is a child of the right hand's paddle)."""
    chain = ("root", "body", "left_arm", "left_forearm", "left_hand", "right_arm", "right_forearm", "right_hand", "defibrillator")

    def fn(t):
        pose = {}
        for b in chain:
            pose[b] = {"rotation": c.value(b, t), "position": c.value("@" + b, t)}
        target = world_point(r, pose, "left_hand", (AX, 10.9, -0.2))
        pose["defib_paddle_left"] = {}
        here = world_point(r, pose, "defib_paddle_left", (AX, 10.9, -0.2))
        delta = tuple(target[i] - here[i] for i in range(3))
        return parent_offset(r, pose, "defib_paddle_left", delta)
    c.layer("@defib_paddle_left", fn)


# Fingers.
FIST_R = {"right_fingers": [-80, 0, 0], "right_middle": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
FIST_L = {"left_fingers": [-80, 0, 0], "left_thumb": [-20, 0, -20]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_middle": [0, 0, 0], "right_thumb": [0, 0, -10]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    # The lollipop up by his chest, the candy standing up facing forward, and to his mouth.
    POP_ARM = {"left_arm": [-8, 0, -14], "left_forearm": [-44, 6, 0], "left_hand": [0, 0, 0]}
    MOUTH_ARM = {"left_arm": [-40, 16, -14], "left_forearm": [-96, 40, 0], "left_hand": [0, 0, 0], "head": [-4, -8, 0]}

    def pop(pose, mouth=False):
        if not mouth:
            return with_pop(r, P_(pose, POP_ARM))
        p = P_(pose, MOUTH_ARM)
        mo = head_point(r, p)
        p = reach(r, p, "left", (mo[0] + 0.8, mo[1] - 4.2, mo[2] - 3.4))
        return with_pop(r, p, True)

    def popkeep(pose):
        """A pose that sets its own left arm: the lollipop just keeps standing up."""
        return with_pop(r, pose)

    # --- idle: weight on his right leg, the hip cocked, the lollipop to his mouth once a loop, a smug look round. ---
    c = clip("idle", 4.0, loop=True)
    stand = {"body": [0, 5, -1.5], "right_leg": [0, 0, 2], "left_leg": [-6, -8, -5], "left_shin": [10, 0, 0], "@root": [0.3, 0, 0],
             "right_arm": [-2, 0, 4], "right_forearm": [-12, 0, 0], "head": [0, -6, 3]}
    c.key(0, pop(stand))
    c.key(0.9, pop(P_(stand, {"head": [2, -12, 4]})), E_IO)
    c.key(1.5, pop(P_(stand, {"head": [-4, -4, 2]}), True), E_IO)
    c.key(2.3, pop(P_(stand, {"head": [-6, -2, 2], "body": [-1, 5, -1.5]}), True), E_IO)
    c.key(2.9, pop(P_(stand, {"head": [0, 8, 5]})), E_IO)
    c.key(3.5, pop(P_(stand, {"head": [0, 6, 4]})), E_IO)
    c.key(4.0, pop(stand), E_IO)
    c.layer("@body", lambda t: [0, -0.15 * (1 - math.cos(2 * math.pi * t / 2.0)) / 2, 0])
    wing_breath(c, 4.0, 2)

    # --- walk: a cocky swagger (bounce, rolling shoulders), the lollipop held up. ---
    L = 1.0
    c = clip("walk", L, loop=True)
    for i in range(5):
        t = L * i / 4
        ph = 2 * math.pi * i / 4
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(t, pop({"right_leg": [-30 * sn, 0, 0], "left_leg": [30 * sn, 0, 0],
                      "right_shin": [34 * max(0, math.sin(ph + 1.3)), 0, 0], "left_shin": [34 * max(0, -math.sin(ph + 1.3)), 0, 0],
                      "right_arm": [26 * sn, 0, 5], "right_forearm": [-14 - 10 * max(0, sn), 0, 0],
                      "body": [3, 9 * sn, 2 * sn], "head": [0, -7 * sn, -2 * sn], "@root": [0, 0.6 * abs(cs), 0]}),
              E_IO if i else None)
    wing_breath(c, L, 1, amp=2.0)

    # --- laugh: doubled over slapping his knee, then thrown back -- two big guffaws a loop. ---
    c = clip("laugh", 1.6, loop=True)
    fold = {"body": [24, 0, 0], "head": [-10, 0, 0], "right_arm": [-30, 0, 10], "right_forearm": [-30, 0, 0], "right_leg": [-14, 0, 0],
            "right_shin": [18, 0, 0], "left_leg": [-4, 0, 0], "left_shin": [8, 0, 0], "@root": [0, -0.8, 0], "left_arm": [-24, 0, -14],
            "left_forearm": [-62, 24, 0]}
    slap = P_(fold, {"right_arm": [-12, 0, 6], "right_forearm": [-4, 0, 0], "body": [28, 0, 0]})
    back = {"body": [-12, 0, 0], "head": [-28, 0, 0], "right_arm": [-20, 0, 26], "right_forearm": [-40, 0, 0], "left_arm": [-30, 0, -24],
            "left_forearm": [-72, 24, 0], "@root": [0, 0.2, 0]}
    c.key(0, popkeep(fold))
    c.key(0.2, popkeep(slap), E_IN)
    c.key(0.5, popkeep(fold), E_OUT)
    c.key(0.9, popkeep(back), E_IO)
    c.key(1.25, popkeep(P_(back, {"head": [-32, 6, 0]})), E_IO)
    c.key(1.6, popkeep(fold), E_IO)
    c.layer("body", lambda t: [2.5 * math.sin(2 * math.pi * t * 5 / 1.6), 0, 0])
    c.layer("head", lambda t: [3.0 * math.sin(2 * math.pi * t * 5 / 1.6 + 1.0), 0, 0])
    wing_breath(c, 1.6, 2, amp=3.0)

    # --- throw_pie: the pie up on his palm like a waiter, wound back over his shoulder, flung (release 0.55). ---
    c = clip("throw_pie", 1.2)
    waiter = {"right_arm": [-40, 0, 14], "right_forearm": [-62, -10, 0], "body": [0, -8, 0], "head": [2, 10, 0]}
    wind = {"right_arm": [-150, 20, 34], "right_forearm": [-40, 0, 0], "body": [-8, 30, 0], "head": [0, -22, 0],
            "left_leg": [-20, 0, 0], "right_leg": [14, 0, 0], "@root": [0, -0.3, 0]}
    rel = {"right_arm": [-96, -14, 0], "right_forearm": [-6, 0, 0], "body": [14, -20, 0], "head": [-4, 14, 0],
           "left_leg": [-28, 0, 0], "right_leg": [20, 0, 0], "@root": [0, -0.8, -1.5]}
    fol = {"right_arm": [-40, -30, -10], "right_forearm": [-8, 0, 0], "body": [20, -26, 0], "head": [0, 18, 0],
           "left_leg": [-24, 0, 0], "right_leg": [18, 0, 0], "@root": [0, -0.8, -1.5]}
    c.key(0, popkeep(P_({}, POP_ARM)))
    c.key(0.25, popkeep(P_(palm_up(r, P_(waiter, POP_ARM)), OPEN_R)), E_OUT)
    c.key(0.42, popkeep(P_(palm_up(r, P_(wind, POP_ARM), forward=(0, 0.6, 0.8)), OPEN_R)), E_IO)
    c.key(0.55, popkeep(P_(palm_up(r, P_(rel, POP_ARM), forward=(0, 0.3, -1)), OPEN_R, {"%pie": [1, 1, 1]})), E_SNAP)
    c.key(0.56, popkeep(P_(rel, POP_ARM, OPEN_R, {"%pie": [0, 0, 0]})))
    c.key(0.8, popkeep(P_(fol, POP_ARM, {"%pie": [0, 0, 0]})), E_OUT)
    c.key(1.2, popkeep(P_(POP_ARM, {"%pie": [0, 0, 0]})), E_IO)

    # --- snap: the hand up beside his face, thumb on middle finger, the snap at 0.45, a smug beat. ---
    c = clip("snap", 1.0)
    up = reach(r, {"right_arm": [-30, 0, 40], "right_forearm": [-100, 0, 0], "head": [0, 10, 4], "body": [0, -6, 0],
                   "right_fingers": [-80, 0, 0], "right_middle": [-30, 0, 0], "right_thumb": [-30, 0, 30]}, "right", (-8.0, 24.0, -2.0))
    up = P_(up, {"right_hand": aim_bone(r, up, "right_hand", (0, -1, 0), (0, 1, 0), (-1, 0, 0), (0.7, 0, -0.7))})
    snapped = P_(up, {"right_arm": [up["right_arm"][0] - 4, up["right_arm"][1], up["right_arm"][2] + 2],
                      "right_hand": [up["right_hand"][0] + 14, up["right_hand"][1], up["right_hand"][2]],
                      "right_middle": [-110, 0, 0], "right_thumb": [10, 0, 0], "head": [4, 14, 6]})
    c.key(0, popkeep(POP_ARM))
    c.key(0.3, popkeep(P_(up, POP_ARM)), E_OUT)
    c.key(0.42, popkeep(P_(up, POP_ARM, {"right_middle": [-24, 0, 0], "right_thumb": [-36, 0, 34]})), E_IO)
    c.key(0.45, popkeep(P_(snapped, POP_ARM)), E_SNAP)
    c.key(0.7, popkeep(P_(snapped, POP_ARM, {"head": [2, 12, 5]})), E_OUT)
    c.key(1.0, popkeep(POP_ARM), E_IO)

    # --- host_gesture: the microphone to his mouth, the other arm sweeping out to the studio. ---
    c = clip("host_gesture", 2.0)
    mic = {"right_arm": [-40, 10, 18], "right_forearm": [-92, -20, 0], "head": [-2, 0, 0], "body": [-2, 0, 0]}
    sweep0 = {"left_arm": [-30, -10, -40], "left_forearm": [-40, 10, 0]}
    sweep1 = {"left_arm": [-24, -30, -78], "left_forearm": [-18, 0, 0], "head": [-6, -18, -4], "body": [-4, -10, 0]}
    c.key(0, popkeep(POP_ARM))
    c.key(0.35, with_mic(r, P_(mic, POP_ARM)), E_OUT)
    c.key(0.75, with_pop(r, with_mic(r, P_(mic, sweep0, {"head": [2, 6, 0]}))), E_IO)
    c.key(1.15, with_pop(r, with_mic(r, P_(mic, sweep1))), E_BACK)
    c.key(1.6, with_pop(r, with_mic(r, P_(mic, sweep1, {"head": [-8, -14, -6]}))), E_IO)
    c.key(2.0, popkeep(POP_ARM), E_IO)
    c.layer("head", lambda t: [3 * math.sin(t * 22) if 0.4 < t < 1.7 else 0, 0, 0])

    # --- buzzer_slam: the hand raised high, slammed down on the buzzer (hit 0.45). ---
    c = clip("buzzer_slam", 1.0)
    high = {"right_arm": [-165, 0, -6], "right_forearm": [-30, 0, 0], "body": [-8, -10, 0], "head": [-10, 6, 0], "right_hand": [0, 0, 0]}
    slam = {"body": [20, -4, 0], "head": [-6, 0, 0], "@root": [0, -1.2, -0.6], "right_leg": [-16, 0, 0], "right_shin": [22, 0, 0],
            "left_leg": [-10, 0, 0], "left_shin": [18, 0, 0], "right_arm": [-50, 0, 4], "right_forearm": [-40, 0, 0]}
    slam = reach(r, slam, "right", (-2.5, 10.5, -7.0))
    slam = P_(slam, {"right_hand": aim_bone(r, slam, "right_hand", (1, 0, 0), (0, -1, 0), (0, -1, 0), (0, 0, -1))})
    c.key(0, popkeep(POP_ARM))
    c.key(0.32, popkeep(P_(high, POP_ARM, OPEN_R)), E_OUT)
    c.key(0.45, popkeep(P_(slam, POP_ARM, OPEN_R)), E_SNAP)
    c.key(0.62, popkeep(P_(slam, POP_ARM, OPEN_R, {"body": [20, -4, 0], "head": [-6, 8, 0]})), E_OUT)
    c.key(1.0, popkeep(POP_ARM), E_IO)

    # --- defib: paddles rubbed together, raised -- "CLEAR!" -- and slammed down on the floor (shock 1.35). ---
    c = clip("defib", 2.0)
    rub = {"right_arm": [-50, -26, 10], "right_forearm": [-50, 0, 0], "left_arm": [-50, 26, -10], "left_forearm": [-50, 0, 0],
           "right_hand": [0, 0, 70], "left_hand": [0, 0, -70], "body": [4, 0, 0], "head": [10, 0, 0]}
    rub2 = P_(rub, {"right_arm": [-54, -22, 8], "left_arm": [-46, 30, -12]})
    clear = {"right_arm": [-168, -10, 10], "right_forearm": [-20, 0, 0], "left_arm": [-168, 10, -10], "left_forearm": [-20, 0, 0],
             "right_hand": [0, 0, 60], "left_hand": [0, 0, -60], "body": [-10, 0, 0], "head": [-16, 0, 0], "@root": [0, 0.4, 0]}
    shock = {"right_arm": [-60, -14, 6], "right_forearm": [-20, 0, 0], "left_arm": [-60, 14, -6], "left_forearm": [-20, 0, 0],
             "right_hand": [-40, 0, 20], "left_hand": [-40, 0, -20], "body": [40, 0, 0], "head": [-6, 0, 0], "@root": [0, -3.2, -0.8],
             "right_leg": [-50, 0, 6], "right_shin": [80, 0, 0], "left_leg": [-50, 0, -6], "left_shin": [80, 0, 0]}
    c.key(0, P_(POP_ARM, {"%lollipop": [0, 0, 0]}))
    c.key(0.35, P_(rub, {"%lollipop": [0, 0, 0]}), E_OUT)
    c.key(0.5, P_(rub2, {"%lollipop": [0, 0, 0]}), E_IO)
    c.key(0.65, P_(rub, {"%lollipop": [0, 0, 0]}), E_IO)
    c.key(0.8, P_(rub2, {"%lollipop": [0, 0, 0]}), E_IO)
    c.key(1.1, P_(clear, {"%lollipop": [0, 0, 0]}), E_BACK)
    c.key(1.35, P_(shock, {"%lollipop": [0, 0, 0]}), E_SNAP)
    c.key(1.6, P_(shock, {"body": [36, 0, 0], "%lollipop": [0, 0, 0]}), E_OUT)
    c.key(2.0, P_(POP_ARM, {"%lollipop": [0, 0, 0]}), E_IO)
    follow_left_fist(c, r)

    # --- spokesman_pose: the product (his lollipop) by his grin, a thumbs-up, a wink of a bounce; starts and ends in the
    # pose, held. ---
    c = clip("spokesman_pose", 2.4, hold=True)
    present = {"left_arm": [-30, -10, -36], "left_forearm": [-110, 50, 0], "head": [0, 10, 8], "body": [0, 8, 0],
               "right_arm": [-68, 0, 22], "right_forearm": [-22, 0, 0], "right_fingers": [-90, 0, 0],
               "right_middle": [-90, 0, 0], "right_thumb": [-85, 0, 0], "right_leg": [0, 0, 4], "left_leg": [-4, -12, -6],
               "left_shin": [8, 0, 0]}
    beat = P_(present, {"head": [-4, 14, 12], "body": [-2, 10, 0], "@root": [0, 0.5, 0], "right_arm": [-76, 0, 24]})
    c.key(0, with_pop(r, present))
    c.key(0.5, with_pop(r, beat), E_BACK)
    c.key(0.9, with_pop(r, present), E_IO)
    c.key(1.6, with_pop(r, P_(present, {"head": [0, 8, 6]})), E_IO)
    c.key(2.4, with_pop(r, present), E_IO)

    # --- wings_reveal: the six wings grow out of his back, sweep open past their rest and settle; held. ---
    c = clip("wings_reveal", 2.5, hold=True)
    folded = P_(wing_pose(m, "fold"), {"%wings": [0.05, 0.05, 0.05]})
    c.key(0, popkeep(P_(POP_ARM, folded)))
    c.key(0.5, popkeep(P_(POP_ARM, wing_pose(m, "fold"), {"%wings": [0.85, 0.85, 0.85], "body": [10, 0, 0], "head": [14, 0, 0]})), E_OUT)
    c.key(1.2, popkeep(P_(wing_pose(m, "wide"), {"%wings": [1.12, 1.12, 1.12], "body": [-10, 0, 0], "head": [-16, 0, 0],
                                                    "right_arm": [-20, 0, 50], "left_arm": [-20, 0, -50], "left_forearm": [-30, 0, 0],
                                                    "right_forearm": [-20, 0, 0]})), E_BACK)
    c.key(1.8, popkeep(P_(POP_ARM, wing_pose(m, "rest"), {"%wings": [1, 1, 1], "head": [-4, -6, 3]})), E_IO)
    c.key(2.5, popkeep(P_(POP_ARM, wing_pose(m, "rest"), {"%wings": [1, 1, 1], "head": [0, -6, 3]})), E_IO)

    # --- hit: a flinch. ---
    c = clip("hit", 0.5)
    c.key(0, popkeep(POP_ARM))
    c.key(0.1, popkeep(P_(POP_ARM, {"body": [-12, 8, 0], "head": [-16, -10, 0], "right_arm": [-20, 0, 24], "@root": [0, 0, 1.2]})), E_SNAP)
    c.key(0.5, popkeep(POP_ARM), E_IO)

    # --- death: (4 s, held) he looks down at the blade in his chest, staggers, the light tears out of him with his arms
    # flung wide and the wings flaring; to his knees, slumped. ---
    c = clip("death", 4.0, hold=True)
    c.key(0, popkeep(POP_ARM))
    c.key(0.3, popkeep(P_(POP_ARM, {"body": [10, 0, 0], "head": [30, 0, 0], "right_arm": [-40, 0, -10], "right_forearm": [-50, 0, 0],
                                    "@root": [0, 0, 0.6]})), E_SNAP)
    c.key(1.0, P_({"body": [-8, 6, 0], "head": [-10, 0, 0], "right_arm": [-20, 0, 30], "left_arm": [-20, 0, -30], "@root": [0, 0, 1.6],
                   "right_leg": [10, 0, 0], "left_leg": [-12, 0, 0], "%lollipop": [0, 0, 0]}, wing_pose(m, "rest")), E_IO)
    burst = P_({"body": [-22, 0, 0], "head": [-40, 0, 0], "right_arm": [-30, 0, 100], "left_arm": [-30, 0, -100], "@root": [0, 0.6, 1.6],
                "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "%lollipop": [0, 0, 0]}, wing_pose(m, "wide"),
               {"%wings": [1.25, 1.25, 1.25]})
    c.key(1.7, burst, E_OUT)
    c.key(2.6, P_(burst, {"head": [-44, 0, 0], "%wings": [1.3, 1.3, 1.3]}), E_IO)
    kneel = {"@root": [0, -5.0, 1.6], "right_leg": [-80, 0, 4], "right_shin": [80, 0, 0], "left_leg": [-80, 0, -4], "left_shin": [80, 0, 0],
             "body": [30, 0, 0], "head": [36, 0, 0], "right_arm": [6, 0, 10], "left_arm": [6, 0, -10], "%lollipop": [0, 0, 0]}
    c.key(3.3, P_(kneel, wing_pose(m, "fold"), {"%wings": [0.6, 0.6, 0.6]}), E_IN)
    c.key(4.0, P_(kneel, {"body": [36, 0, 0], "head": [44, 0, 0]}, wing_pose(m, "fold"), {"%wings": [0.02, 0.02, 0.02]}), E_IO)
    c.layer("body", lambda t: [2.0 * math.sin(t * 80) if 1.6 < t < 2.7 else 0, 0, 0])

    # --- emerge: crouched as if behind the curtain, he springs up with his arms wide -- ta-da! -- bows, and back to
    # the lollipop. ---
    c = clip("emerge", 3.0)
    crouch = {"@root": [0, -3.0, 0], "right_leg": [-50, 0, 4], "right_shin": [76, 0, 0], "left_leg": [-50, 0, -4], "left_shin": [76, 0, 0],
              "body": [34, 0, 0], "head": [30, 0, 0], "right_arm": [10, 0, 10], "left_arm": [10, 0, -10]}
    tada = {"@root": [0, 1.4, 0], "body": [-8, 0, 0], "head": [-14, 0, 0], "right_arm": [-30, 0, 70], "left_arm": [-30, 0, -70],
            "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "right_leg": [6, 0, 6], "left_leg": [6, 0, -6]}
    bow = {"body": [40, 0, 0], "head": [10, 0, 0], "right_arm": [-60, 30, 10], "right_forearm": [-70, 0, 0], "left_arm": [20, 0, -20],
           "left_leg": [-6, 0, 0], "right_leg": [10, 0, 0]}
    c.key(0, popkeep(P_(crouch, {"left_forearm": [-40, 10, 0]})))
    c.key(0.25, popkeep(P_(crouch, {"@root": [0, -3.4, 0], "left_forearm": [-40, 10, 0]})), E_IO)
    c.key(0.6, popkeep(tada), E_BACK)
    c.key(0.8, popkeep(P_(tada, {"@root": [0, 0, 0]})), E_IN)
    c.key(1.3, popkeep(P_(tada, {"@root": [0, 0, 0], "head": [-8, 10, 6]})), E_IO)
    c.key(1.8, popkeep(bow), E_IO)
    c.key(2.3, popkeep(P_(bow, {"body": [36, 0, 0]})), E_IO)
    c.key(3.0, popkeep(POP_ARM), E_IO)

    for cl in clips:
        cl.done(step=0.05)
    return f, clips


# --- wing poses -------------------------------------------------------------------------------------------------------

WING_POSES = {
    # right-wing absolute rotations per pair (the left mirrors them) and the tips' fold
    "rest": ({1: (0, 26, 6), 2: (0, 30, -4), 3: (0, 34, -10)}, 0),
    "wide": ({1: (0, 6, 16), 2: (0, 8, 2), 3: (0, 10, -14)}, 0),
    "fold": ({1: (0, 80, -30), 2: (0, 84, -42), 3: (0, 86, -55)}, -70),
}


def wing_pose(m, name):
    tgt, tip = WING_POSES[name]
    out = {}
    for pair in (1, 2, 3):
        for side, s in (("r", -1), ("l", 1)):
            n = f"wing_{side}{pair}"
            rest = m.rig.get(n).rotation
            t = mirror_rot(tgt[pair], s)
            out[n] = [t[i] - rest[i] for i in range(3)]
            out[f"{n}_tip"] = [0, 0, -s * tip] if tip else [0, 0, 0]
    return out


WING_BONES = [f"wing_{sd}{p}" for p in (1, 2, 3) for sd in ("r", "l")]


def wing_breath(c, length, cycles, amp=2.5):
    """The wings breathe on their own (Molang sine, each pair on its own phase) -- they show only in his last channel."""
    w = 360.0 * cycles / length
    for i, n in enumerate(WING_BONES):
        s = -1 if n[5] == "r" else 1
        ph = 50 * (i // 2)
        a = amp * (1.0 - 0.15 * (i // 2))

        def fn(t, ph=ph, a=a, s=s):
            v = a * math.sin(math.radians(w * t + ph))
            return [0, -s * v * 0.6, -s * v]
        from michael_art import _g
        sine = f"math.sin(query.anim_time*{_g(w)}+{ph})"
        c.mlayer(n, fn, [None, f"{sine}*{_g(round(-s * a * 0.6, 2))}", f"{sine}*{_g(round(-s * a, 2))}"])


# =====================================================================================================
# The thrown pie and the party hat (small GeckoLib models, no animation)
# =====================================================================================================

def pie_model():
    """The cream pie in flight: a tin, the cream heaped up in swirls, a dollop and a cherry; flat, cream up, centred on
    the origin (the projectile's position), 7 px across."""
    m = Model("gabriel_pie", 128, 128)
    b = m.bone("pie", (0, 0, 0))

    def tin(f, x, y, w, h):
        if f == "down":
            cx, cy = (w - 1) / 2, (h - 1) / 2
            return PIE_TIN[3.4 - 1.4 * math.hypot(x - cx, y - cy) / (w / 2)]
        return PIE_TIN[2.4 + 0.8 * (x % 2)] if y else CRUST[3.4]

    def cream(f, x, y, w, h):
        if f == "up":
            cx, cy = (w - 1) / 2, (h - 1) / 2
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy) / (w / 2)
            if r > 0.9:
                return CRUST[3.2 + 0.8 * math.sin(math.atan2(dy, dx) * 9)]
            return CREAM[3.6 + 0.9 * math.sin(r * 9 - math.atan2(dy, dx) * 2)]
        return CREAM[3.0 + (y == 0)] if y < h - 1 else CRUST[3]
    m.cube(b, (-3.5, -0.75, -3.5), (7, 1.0, 7), tin, density=4, tag="tin")
    m.cube(b, (-3.25, 0.25, -3.25), (6.5, 0.75, 6.5), cream, density=4, tag="cream")
    m.cube(b, (-1.0, 1.0, -1.0), (2, 0.75, 2), lambda f, x, y, w, h: CREAM[5] if (x + y) % 3 else CREAM[3], density=4, tag="dollop")
    m.cube(b, (-0.5, 1.75, -0.5), (1, 0.75, 1), lambda f, x, y, w, h: CREAM[4.6], density=4, tag="peak")
    m.cube(b, (0.25, 1.5, 0.25), (0.75, 0.75, 0.75), lambda f, x, y, w, h: CHERRY if f != "up" else hexc("#e8404c"), density=4, tag="cherry")
    return m


HAT = (hexc("#e8283c"), hexc("#fff3c2"), hexc("#2f9be8"), hexc("#fff3c2"), hexc("#ffb21f"), hexc("#fff3c2"))


def party_hat_model():
    """The prank's party hat: a striped paper cone (stepped), a pompom, the elastic; pivot at its base (0, 0, 0), drawn
    on a mob's head by the client."""
    m = Model("party_hat", 128, 128)
    b = m.bone("party_hat", (0, 0, 0))
    steps = 7
    for i in range(steps):
        w = 5.0 - i * 0.65
        y = i * 1.0

        def paper(f, x, yy, ww, hh, i=i):
            if f == "down":
                return None if i else hexc("#c8b88a")
            k = ((x + 2 * i) // 2) % 4 if f != "up" else 1
            c = (HAT[0], HAT[1], HAT[2], HAT[1])[k]
            sh = {"north": 1.0, "south": 0.8, "east": 0.88, "west": 0.88, "up": 1.08}.get(f, 1.0)
            return shade(c, sh)
        m.cube(b, (-w / 2, y, -w / 2), (w, 1.0, w), paper, density=4, tag="cone")
    for k in range(6):
        a = k * math.pi / 3
        m.cube(b, (0.45 * math.cos(a) - 0.5, steps + 0.1 + 0.3 * math.sin(a * 2), 0.45 * math.sin(a) - 0.5), (1.0, 1.0, 1.0),
               lambda f, x, y, w, h, k=k: mix(hexc("#ffffff"), hexc("#ffd7f0"), 0.3 * (k % 2)), density=4, tag="pompom")
    m.cube(b, (-0.75, steps, -0.75), (1.5, 1.5, 1.5), lambda f, x, y, w, h: hexc("#fff6fb"), density=4, tag="pompom_core")
    m.cube(b, (-2.5, -0.01, -2.6), (5.0, 0.3, 0.2), solid(hexc("#c8c0b0")), density=4, tag="elastic")
    return m


# =====================================================================================================
# Output and previews
# =====================================================================================================

def textures():
    out = {}
    glow = None
    for c in COSTUMES:
        m = rig(c)
        t, g = m.build(gutter=1, seed=7600)
        out[c] = t
        glow = glow or g
    return out, glow


def generate():
    m = rig("jacket")
    m.build(gutter=1, seed=7600)
    m.rig.write(GEO + "gabriel.geo.json")
    tex, glow = textures()
    for c, t in tex.items():
        save(t, "entity", f"gabriel_{c}")
    save(glow, "entity", "gabriel_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + "gabriel.animation.json")
    for mk, name in ((pie_model, "gabriel_pie"), (party_hat_model, "party_hat")):
        pm = mk()
        pt, _ = pm.build(gutter=1, seed=7700)
        pm.rig.write(GEO + f"{name}.geo.json")
        save(pt, "entity", name)


def hidden_for(costume="jacket", props=(), wings=False, cap=False, eyes=False):
    h = [f"costume_{g}" for g in GROUPS if g != costume]
    for p in ("pie", "microphone", "defibrillator"):
        if p not in props:
            h.append(p)
    if not wings:
        h.append("wings")
    if not cap:
        h.append("nurse_cap")
    if not eyes:
        h.append("eyes_glow")
    return tuple(h)


if __name__ == "__main__":
    generate()
