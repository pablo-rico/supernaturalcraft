"""Amara, the Darkness as the show has her (v0.42, season 11): one humanoid rig for phases II-VI and her dawn visits, five
textures (the baby, the girl, the teen, the woman, the void) with their glowmasks, and every clip. Contract: AmaraAssets
(RIG_FORM, FORM_BONES, TOGGLED, FORM_CLIPS, HIT_TICKS, CLIP_TICKS).

The woman: slim and pale (porcelain skin with a cool shade), long straight dark hair parted in the middle that frames her face
and falls down her back past the waist, dark eyes, red-brown lips; a long black dress, a fitted bodice with a V neckline and
long sleeves, the skirt to her ankles with a back panel that trails (`dress_tail` -- `dress_train`). The glowmask is her power:
her eyes gone black with a violet rim and violet smoke veins on her cheeks and throat.

The ages share the geometry and the UV pack; the texture decides (transparent texels shorten the girl's and the teen's hair,
the girl's dress ends at the knee with short sleeves) and the renderer scales the whole model (Age.scale) and the `head` for
the children (so no clip scales `head`). The baby is the `bundle` alone, under the root.

The bones the renderer toggles (AmaraAssets.TOGGLED) and where they hang:
  root -- bundle -- bundle_body, bundle_head, bundle_smoke              (the newborn in a curl of smoke)
       -- body -- smoke_skirt -- smoke_wisp_0..7                        (below the knees she is smoke)
               -- torso -- tendrils -- tendril_<i>, _2, _3, _4  (i = 0..3) (four smoke tendrils from her back)
                        -- head -- corona -- corona_inner, corona_outer  (the black sun behind her head)
                                -- tears                                (light down her cheeks)
                        -- right_arm -- right_forearm -- right_hand -- chains -- chain_1..6   (smoke chains from her fist)

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north; 29.6 px to the crown = 1.85 blocks):
  root -- body -- torso -- head -- eyes, mouth, hair -- hair_right -- hair_right_2, hair_left -- hair_left_2
                                -- hair_back -- hair_back_2 -- hair_back_3 -- hair_back_4
                        -- right_arm -- right_forearm -- right_hand -- right_fingers, right_thumb   (left the same)
               -- dress -- dress_front, dress_right, dress_left, dress_tail -- dress_train
               -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

from animkit import AnimFile
from chuck_art import FCube, h01
from common import ASSETS, save
from michael_art import Clip, P_, write_compact
from paint import SHADE
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade
from geomodel import Rig

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "amara_form"
A = "animation.amara_form."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

AGES = ("baby", "girl", "teen", "woman", "void")

# --- proportions (slim; 29.6 px to the crown) ----------------------------------------------------------------------
HIP = 13.0        # legs hinge here; the body's pivot
WAIST = 15.0      # the torso bends here
NECK = 22.6       # the head's pivot (its cube sits on it)
HEAD = 7.0
TW, TD = 3.2, 1.7     # half the chest's width and depth
WW, WD = 2.6, 1.5     # half the waist's
AW = 2.4
AX = TW + AW / 2      # the arm's centre line
SY = 21.0             # the shoulder pivot
ARM_TOP = 21.6
ELBOW = 17.0
WRIST = 13.2
PALM = 11.6
LX, LW = 1.5, 2.8
KNEE = 7.0
HEM_Y = 0.6           # the skirt's hem
SKIRT_TOP = 11.6      # where the skirt's panels hinge

SIDES = (("right", -1), ("left", 1))

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#5d4b52", "#8b7880", "#b3a2a6", "#d1c3c3", "#e6dbd8", "#f5eeeb")     # porcelain, cool shade
SKIN_GIRL = Ramp("#62504f", "#917e80", "#baa8a8", "#d8c9c6", "#ebe0dc", "#f8f1ee")
HAIR = Ramp("#050306", "#0c0709", "#140d0e", "#1d1314", "#291b1b", "#3a2826")     # dark brown-black
DRESS = Ramp("#030205", "#08060b", "#0e0b13", "#16121d", "#211b2b", "#342b44")    # black, a cool violet sheen
SHOE = Ramp("#020203", "#060608", "#0c0c0f", "#141418", "#1e1e24", "#34343c")
SOCK = Ramp("#7c7a80", "#a19fa6", "#c2c0c6", "#d8d6dc", "#e8e6ec", "#f6f5f8")
LIPS = Ramp("#3e1a1b", "#5a2526", "#743232", "#8a3e3b", "#a05048", "#b8665a")      # red-brown
LIPS_GIRL = Ramp("#5a3434", "#7a4646", "#94585a", "#a86a68", "#ba7c78", "#cc908a")
EYE_WHITE = hexc("#e9e4e2")
IRIS = Ramp("#0c0605", "#1a0e0b", "#2a1812", "#3b2419", "#553626", "#7a5440")
BROW = hexc("#120b0b")
LASH = hexc("#070405")
BLUSH = hexc("#d8a8a8")
# The power: black eyes, violet rims and smoke.
INK = hexc("#020104")
VIOLET = Ramp("#2a1050", "#4a1f86", "#6a34b8", "#8c55e0", "#b48cff", "#e0ccff")
GOLD = Ramp("#5a3e12", "#8a6420", "#bf9038", "#e6bc5c", "#f8dc96", "#fff6dc")
SMOKE = Ramp("#020104", "#06040a", "#0c0912", "#14101d", "#1f182c", "#2e2440")
# The void: skin and dress turned to night.
NIGHT = Ramp("#010006", "#040312", "#0a0822", "#120f36", "#1c184c", "#2a2466")
NEB = (hexc("#5a2c9c"), hexc("#8a3088"), hexc("#2c46a0"), hexc("#3a8aa8"), hexc("#b04a9a"))
STAR = (hexc("#ffffff"), hexc("#f0e8ff"), hexc("#d4c4ff"), hexc("#b48cff"), hexc("#ffe9b8"))
CLOTH = Ramp("#040306", "#0a080e", "#121018", "#1b1824", "#272234", "#3a334c")    # the baby's swaddle


def tone(ramp, v):
    return ramp[max(0, min(len(ramp) - 1, int(round(v))))]


def rgba(c, a):
    return (c[0], c[1], c[2], max(0, min(255, int(a))))


# =====================================================================================================
# A Model whose materials may ask for the texel's model-space point (coherent patterns across cubes)
# =====================================================================================================

class Pt:
    """A material or glow that also takes the texel's Bedrock point (before bone rotations): fn(face, x, y, w, h, p[, col])."""

    def __init__(self, fn):
        self.fn = fn


def _face_point(c, face, u, v):
    x0, y0, z0 = c.origin
    w, h, d = c.size
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    k = c.density
    u, v = (u + 0.5) / k, (v + 0.5) / k
    if face == "north":
        return (x0 + u, y1 - v, z0)
    if face == "south":
        return (x1 - u, y1 - v, z1)
    if face == "east":
        return (x1, y1 - v, z1 - u)
    if face == "west":
        return (x0, y1 - v, z0 + u)
    if face == "up":
        return (x1 - u, y1, z0 + v)
    return (x1 - u, y0, z1 - v)


class Model:
    def __init__(self, name, w, h):
        self.rig = Rig(name, w, h)
        self.jobs = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, inflate=0.0, faces=None, rotation=None, pivot=None,
             density=2, tag=None, shading=True):
        if isinstance(bone, str):
            bone = self.rig.get(bone)
        c = FCube(origin, size, inflate=inflate, faces=faces, tag=tag, rotation=rotation, pivot=pivot, density=density)
        bone.cubes.append(c)
        self.jobs.append((c, material, glow, shading))
        return c

    def build(self, gutter=1, seed=1):
        self.rig.pack(gutter=gutter)
        t = Tex(self.rig.tex_w, self.rig.tex_h, seed)
        g = Tex(self.rig.tex_w, self.rig.tex_h, seed + 1)
        for c, mat, glow, shading in self.jobs:
            for face, (u, v, w, h) in c.rects.items():
                for y in range(h):
                    for x in range(w):
                        p = _face_point(c, face, x, y) if isinstance(mat, Pt) or isinstance(glow, Pt) else None
                        col = mat.fn(face, x, y, w, h, p) if isinstance(mat, Pt) else mat(face, x, y, w, h)
                        if col is None:
                            continue
                        t.set(u + x, v + y, shade(col, SHADE[face]) if shading and col[3] else col)
                        if glow is None or not col[3]:
                            continue
                        gl = glow.fn(face, x, y, w, h, p, col) if isinstance(glow, Pt) else glow(face, x, y, w, h, col)
                        if gl is not None:
                            g.set(u + x, v + y, gl)
        return t, g


def none_on(faces, mat):
    if isinstance(mat, Pt):
        return Pt(lambda f, x, y, w, h, p: None if f in faces else mat.fn(f, x, y, w, h, p))
    return lambda f, x, y, w, h: None if f in faces else mat(f, x, y, w, h)


def solid(c):
    return lambda f, x, y, w, h: c


# =====================================================================================================
# Looks: one geometry, five skins
# =====================================================================================================

class Look:
    def __init__(self, age):
        self.age = age
        self.void = age == "void"
        self.girl = age == "girl"
        self.teen = age == "teen"
        self.skin_ramp = SKIN_GIRL if self.girl else SKIN
        self.lips = LIPS_GIRL if self.girl else LIPS
        # Lowest model y the back hair reaches (the ends are ragged a texel or two above it).
        self.hair_end = {"girl": 19.8, "teen": 15.2}.get(age, 10.8)
        self.lock_end = {"girl": 20.6, "teen": 18.6}.get(age, 18.0)
        # The girl's dress: knee length, short sleeves, white socks and black shoes.
        self.hem = 6.4 if self.girl else 2.4 if self.teen else HEM_Y

    # --- void helpers -------------------------------------------------------------------------------------------
    def stars(self, p, seed, dens=0.035):
        """A star at this texel (and how bright), from its model point: coherent across faces."""
        if p is None:
            return None
        key = (round(p[0] * 2), round(p[1] * 2), round(p[2] * 2), seed)
        r = h01("star", key)
        if r < dens:
            return STAR[int(h01("sc", key) * len(STAR))], (0.5 + r / dens * 0.5)
        return None

    def nebula(self, p, seed, strength=1.0):
        """A spiral of nebula in her skirt: two twisting arms of violet and magenta cloud wound round a bright core at her
        knees, a blue haze between them. Returns (colour, 0..1 amount)."""
        cx, cy = 0.4, 7.0
        dx, dy = p[0] - cx, (p[1] - cy) * 0.75 + p[2] * 0.2
        r = math.hypot(dx, dy)
        a2 = math.atan2(dy, dx) - r * 0.42
        wx, wy = r * math.cos(a2), r * math.sin(a2)
        n = fbm(32 + wx * 2.6, 32 + wy * 2.6, seed, 64, 64, 3, 5.0)
        arm = 0.5 + 0.5 * math.cos(2 * a2)
        k = max(0.0, min(1.0, (n * 0.7 + arm * 0.65 - 0.62) * 2.4)) * max(0.0, 1.0 - r / 13)
        k = max(k, max(0.0, 1 - r / 1.6) * 0.9) * strength
        if k > 0.7:
            hue = mix(NEB[4], STAR[1], min(1.0, (k - 0.7) * 2.5))       # the bright core
        elif arm > 0.5:
            hue = mix(NEB[0], NEB[1], (arm - 0.5) / 0.5)
        else:
            hue = mix(NEB[2], NEB[0], arm / 0.5)
        return hue, k

    def night(self, seed, base=2.0, neb=0.0, dens=0.035):
        def m(f, x, y, w, h, p):
            n = fbm(x * 1.5 + len(f) * 7, y * 1.5, seed, 64, 64, 2, 6.0)
            c = tone(NIGHT, base + (n - 0.5) * 1.6)
            if neb:
                hue, k = self.nebula(p, seed)
                c = mix(c, hue, min(1.0, k * 1.1 * neb))
            s = self.stars(p, seed, dens)
            if s:
                c = mix(c, s[0], s[1])
            return c
        return Pt(m)

    def night_glow(self, seed, dens=0.035, neb=0.0, rim=True):
        """Stars glow; the nebula glows faintly; a thin violet rim on the cubes' edges keeps her silhouette."""
        def g(f, x, y, w, h, p, col):
            s = self.stars(p, seed, dens)
            if s:
                return rgba(s[0], 255 * s[1])
            if neb:
                hue, k = self.nebula(p, seed)
                if k > 0.2:
                    return rgba(hue, min(200, 260 * (k - 0.2)))
            if rim and f in ("north", "south", "east", "west") and (x == 0 or x == w - 1):
                return rgba(VIOLET[3], 70)
            return None
        return Pt(g)

    # --- materials ------------------------------------------------------------------------------------------------
    def skin(self, seed, base=3.2, amp=0.45):
        if self.void:
            return self.night(seed, 2.2, dens=0.03)
        ramp = self.skin_ramp

        def m(f, x, y, w, h):
            n = fbm(x + len(f) * 3, y, seed, 64, 64, 2, 7.0)
            return tone(ramp, base + (n - 0.5) * amp)
        return m

    def skin_glow(self, seed):
        return self.night_glow(seed, 0.03) if self.void else None

    def dress(self, seed, base=2.4):
        """Black crepe: soft vertical folds catching a cool sheen; the void's is a nebula."""
        if self.void:
            return self.night(seed, 1.6, neb=1.0, dens=0.045)

        def m(f, x, y, w, h):
            if f == "up":
                return tone(DRESS, base + 0.6)
            if f == "down":
                return tone(DRESS, base - 1.4)
            fold = math.sin(x * 1.25 + seed * 0.7) * 0.55 + math.sin(x * 0.45 + 1.3 + seed) * 0.35
            v = base + fold - 0.25 * y / max(1, h)
            if fold > 0.6 and h01("sheen", seed, f, x, y // 3) < 0.6:
                v += 0.7
            return tone(DRESS, v)
        return m

    def dress_glow(self, seed):
        return self.night_glow(seed, 0.045, neb=1.0) if self.void else None

    def hair(self, seed):
        """Straight dark hair: long vertical strands, a dull sheen in bands."""
        def m(f, x, y, w, h):
            n = fbm(x * 3.0, y * 0.35 + len(f) * 3, seed, 64, 64, 2, 6.0)
            v = 1.9 + (n - 0.5) * 1.6
            r = h01("strand", seed, f, x)
            if r < 0.14:
                v += 1.1
            elif r < 0.3:
                v -= 0.7
            if f in ("north", "south", "east", "west") and (y + int(r * 7)) % 9 == 0:
                v += 0.6                                  # the sheen's band
            c = tone(HAIR, v)
            if self.void:
                c = mix(c, VIOLET[0], 0.25 if r < 0.2 else 0.1)
            return c
        return m

    def hair_glow(self, seed):
        if not self.void:
            return None

        def g(f, x, y, w, h, p, col):
            s = self.stars(p, seed + 4, 0.012)
            return rgba(s[0], 255 * s[1]) if s else None
        return Pt(g)

    def cut(self, mat, y_min, ragged=1.0, seed=0):
        """Transparent below model height y_min (ragged ends): shortens the hair per age."""
        def m(f, x, y, w, h, p):
            jag = ragged * h01("tip", seed, f, x)
            if p[1] < y_min + jag:
                return None
            return mat(f, x, y, w, h) if not isinstance(mat, Pt) else mat.fn(f, x, y, w, h, p)
        return Pt(m)


# =====================================================================================================
# The head
# =====================================================================================================

# The face (north side of the skull, 14 x 14 texels; x = 0 is the viewer's left, her right).
FACE_WOMAN = [
    "HHHHHHpHHHHHHH",
    "HHHHHhsShHHHHH",
    "HHHhssSSsshHHH",
    "HHhsssssssshHH",
    "HhsbbbssbbbshH",
    "HdllllsSlllldH",
    "HdwiiwnSwiiwdH",
    "HdseeesSeeesdH",
    "HdcssnSSnsscdH",
    "HdsssNSSNsssdH",
    "HddsquuuuqsddH",
    "HhdssoOOossdhH",
    "HhjdssSSssdjhH",
    "HHjjddddddjjHH",
]
FACE_GIRL = [
    "HHHHHHpHHHHHHH",
    "HHHHhsSSshHHHH",
    "HHhssSSSsshHHH",
    "HhsssssssssshH",
    "HhsbbbssbbbshH",
    "HdllllsSlllldH",
    "HdwiiwsSwiiwdH",
    "HdwiiwsSwiiwdH",
    "HdcccssSsccdcH",
    "HdcssnSSnsscdH",
    "HdsssssssssddH",
    "HddsssuussssdH",
    "HhdsssooSssdhH",
    "HHjddddddddjHH",
]
FACE_TEEN = [
    "HHHHHHpHHHHHHH",
    "HHHHHhsShHHHHH",
    "HHHhssSSsshHHH",
    "HHhsssssssshHH",
    "HhsbbbssbbbshH",
    "HdllllsSlllldH",
    "HdwiiwnSwiiwdH",
    "HdseeesSeeesdH",
    "HdcssnSSnsscdH",
    "HdsssNSSNsssdH",
    "HdssquuuuqssdH",
    "HhdssoOOossdhH",
    "HhjdssSSssdjhH",
    "HHjjddddddjjHH",
]
FACES = {"girl": FACE_GIRL, "teen": FACE_TEEN}


def face_texel(look, sk, hr, x, y, p):
    rows = FACES.get(look.age, FACE_WOMAN)
    ch = rows[y][x]
    if ch in "Hh":
        c = hr("north", x, y, 14, 14)
        return shade(c, 0.78) if ch == "h" else c
    if ch == "p":
        return mix(hr("north", x, y, 14, 14), look.skin_ramp[2], 0.35) if not look.void else tone(NIGHT, 2)
    s = sk.fn("north", x, y, 14, 14, p) if isinstance(sk, Pt) else sk("north", x, y, 14, 14)
    if look.void:
        # Her features in night: a little lighter indigo where light would fall, black where the shadows are.
        lite = {"S": 1.35, "s": 1.0, "c": 1.0, "e": 0.8, "d": 0.7, "j": 0.6, "n": 0.75, "N": 0.5}
        if ch == "b":
            return INK
        if ch == "l":
            return INK
        if ch == "w":
            return tone(NIGHT, 4)
        if ch == "i":
            return VIOLET[2]
        if ch in "uq":
            return mix(VIOLET[0], INK, 0.3)
        if ch in "oO":
            return mix(VIOLET[1], NIGHT[2], 0.45 if ch == "o" else 0.2)
        return shade(s, lite.get(ch, 1.0)) if s[3] else s
    if ch == "S":
        return shade(s, 1.06)
    if ch == "e":
        return mix(shade(s, 0.93), hexc("#9a8aa0"), 0.12)          # a faint cool shadow under the eyes
    if ch == "d":
        return shade(s, 0.86)
    if ch == "j":
        return shade(s, 0.74)
    if ch == "n":
        return shade(s, 0.9)
    if ch == "N":
        return shade(s, 0.72)
    if ch == "c":
        return mix(s, BLUSH, 0.16 if not look.girl else 0.26)
    if ch == "b":
        return BROW
    if ch == "l":
        return LASH
    if ch == "w":
        return mix(EYE_WHITE, s, 0.3)
    if ch == "i":
        return IRIS[2]
    if ch == "u":
        return look.lips[1]                         # the upper lip, in its own shadow
    if ch == "q":
        return shade(s, 0.84)                       # the corners of the mouth
    if ch == "o":
        return mix(look.lips[3], s, 0.55)           # the lower lip, fuller in the middle
    if ch == "O":
        return mix(look.lips[3], look.lips[4], 0.4)
    return s


# Smoke veins on her face when her power shows (glowmask only): threads from the outer corners of the eyes down the cheeks
# to the jaw, the viewer's left side; the right mirrors it. (x, y, strength) on the 14 x 14 face.
VEINS = [(2, 7, 1.0), (2, 8, 1.0), (3, 9, 0.9), (2, 10, 1.0), (2, 11, 0.8), (3, 12, 0.7), (4, 10, 0.5), (1, 9, 0.5),
         (4, 13, 0.4), (2, 13, 0.6)]


def face_glow(look):
    """Her veins of smoke on the face (the woman, the teen, the girl): faint violet threads on the cheeks and jaw."""
    veins = {}
    for x, y, k in VEINS:
        veins[(x, y)] = k
        veins[(13 - x, y)] = k

    def g(f, x, y, w, h, p, col):
        if look.void:
            s = look.stars(p, 40400, 0.02)
            return rgba(s[0], 255 * s[1]) if s else None
        if f != "north":
            return None
        k = veins.get((x, y))
        if k is None:
            return None
        return rgba(mix(VIOLET[1], VIOLET[3], k), 90 + 110 * k)
    return Pt(g)


def build_head(m, look, seed=40100):
    sk = look.skin(seed)
    hr = look.hair(seed + 1)

    def skull(f, x, y, w, h, p):
        if f == "north":
            return face_texel(look, sk, hr, x, y, p)
        if f == "up":
            c = hr(f, x, y, w, h)
            # The parting down the middle of the crown, from the brow halfway back.
            if x in (6, 7) and y < 8 and (x == 6 or y < 3):
                return mix(c, look.skin_ramp[2], 0.3) if not look.void else c
            return c
        if f == "down":
            if y >= 6:
                return hr(f, x, y, w, h)
            s = sk.fn(f, x, y, w, h, p) if isinstance(sk, Pt) else sk(f, x, y, w, h)
            return shade(s, 0.8)
        if f == "south":
            return hr(f, x, y, w, h)
        # The sides are hair (it falls over her ears), a sliver of cheek at the front below the temple.
        fx = x if f == "west" else w - 1 - x
        if fx == 0 and 6 <= y <= 12:
            s = sk.fn(f, x, y, w, h, p) if isinstance(sk, Pt) else sk(f, x, y, w, h)
            return shade(s, 0.8)
        return shade(hr(f, x, y, w, h), 0.92)
    m.cube("head", (-HEAD / 2, NECK, -HEAD / 2), (HEAD, HEAD, HEAD), Pt(skull), face_glow(look), tag="skull")
    # The nose: a small lit ridge.
    m.cube("head", (-0.5, NECK + 2.0, -3.85), (1.0, 1.5, 0.5),
           lambda f, x, y, w, h: shade(look.skin(seed + 2).fn(f, x, y, w, h, (0, NECK + 2, -3.8)) if look.void else
                                       look.skin(seed + 2)(f, x, y, w, h), 1.08 if f == "north" and y < 2 else 0.86 if f == "down" else 0.98),
           tag="nose")
    # The neck, long and slender.
    m.cube("torso", (-1.2, ARM_TOP - 0.4, -1.2), (2.4, NECK - ARM_TOP + 0.9, 2.4), sk, look.skin_glow(seed) or neck_glow(look), tag="neck")


def neck_glow(look):
    """Two threads of smoke up the sides of her throat (glowmask only)."""
    def g(f, x, y, w, h, col):
        if f not in ("north", "east", "west"):
            return None
        lane = 1 if f == "north" else 2
        if x == lane + (y % 3 == 1) or (f == "north" and x == w - 2 - (y % 3 == 2)):
            return rgba(VIOLET[2], 150)
        return None
    return g


def build_eyes(m, look):
    """A shell over the face's eye rows: the eyes in the albedo (dark, human) and in the glowmask (black, rimmed violet)."""
    def which(x):
        if x <= 3:
            return x
        if x >= 6:
            return x - 6
        return None

    def shell(f, x, y, w, h):
        c = which(x)
        if c is None:
            return None
        if y == 0:
            return LASH if not look.void else INK
        if look.void:
            return VIOLET[2] if c in (1, 2) else tone(NIGHT, 4)
        if c in (1, 2):
            # Dark brown irises; a catchlight in each.
            if (c == 1 and x < 6) or (c == 1 and x >= 6):
                return IRIS[1]
            return IRIS[3]
        return EYE_WHITE

    def glow(f, x, y, w, h, col):
        c = which(x)
        if c is None:
            return None
        if look.void:
            if c in (1, 2):
                return rgba(STAR[0], 255) if y == 1 and c == 1 else rgba(VIOLET[5], 255)
            return rgba(VIOLET[3], 255)
        # Black eyes: the whites gone too; the rim of violet at the corners and along the lash line.
        if y == 0:
            return rgba(VIOLET[3], 255) if c in (0, 3) else rgba(VIOLET[1], 255)
        if c in (0, 3):
            return rgba(VIOLET[2], 255)
        return rgba(INK, 255)
    m.cube("eyes", (-2.5, NECK + 3.5, -HEAD / 2 - 0.06), (5.0, 1.0, 0), shell, glow, faces=("north",), tag="eyes", shading=False)


def build_mouth(m, look):
    """Her mouth open (laughing, screaming, drawing a soul in): set a hair's breadth inside the face, so it is hidden at rest;
    a clip pushes the bone forward (`@mouth` z -0.2) to open it, and may stretch it (`%mouth`)."""
    def mat(f, x, y, w, h):
        if y == 0:
            return look.lips[1] if not look.void else VIOLET[0]
        if y == h - 1:
            return look.lips[3] if not look.void else VIOLET[1]
        if x in (0, w - 1):
            return look.lips[2] if not look.void else VIOLET[0]
        if y == 1 and not look.void:
            return hexc("#d8d0cc") if 1 <= x <= w - 2 else hexc("#1a0608")      # a glimpse of teeth
        return hexc("#14050a") if not look.void else INK

    def glow(f, x, y, w, h, col):
        if look.void and 0 < y < h - 1 and 0 < x < w - 1:
            return rgba(VIOLET[2], 200)
        return None
    m.cube("mouth", (-1.0, NECK + 0.5, -HEAD / 2 + 0.08), (2.0, 1.5, 0), mat, glow, faces=("north",), tag="mouth", shading=False)


def build_hair(m, look, seed=40200):
    hr = look.hair(seed)
    hg = look.hair_glow(seed)
    # The crown: a cap over the top and sides, the parting down the middle.
    def cap(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            # Only the edge of the hairline: it sweeps off the parting to either side.
            mid = (w - 1) / 2
            if y >= 1 and abs(x - mid) < 3.5 - y:
                return None
            return hr(f, x, y, w, h)
        if f == "up" and x in (6, 7) and y < 7 and not look.void:
            return mix(hr(f, x, y, w, h), look.skin_ramp[2], 0.22)
        return hr(f, x, y, w, h)
    m.cube("hair", (-HEAD / 2, NECK + 5.5, -HEAD / 2), (HEAD, 1.5, HEAD), cap, hg, inflate=0.3, tag="hair_cap")
    for side, s in SIDES:
        # The curtains: straight down each side of the face from the temple to the jaw, a little in front of it.
        b = m.bone(f"hair_{side}", (s * 3.7, NECK + 6.5, -1.0), "hair")

        def curtain(f, x, y, w, h, p, s=s):
            if f == "up":
                return None
            if p[1] < look.lock_end - 1.0 + h01("tip", seed, f, x) and look.girl:
                return None
            c = hr(f, x, y, w, h)
            inner = (f == "east" and s < 0) or (f == "west" and s > 0)
            return shade(c, 0.7) if inner else c
        m.cube(b, (s * 3.85 - 0.45, NECK - 0.6, -HEAD / 2 - 0.25), (0.9, HEAD + 0.6, 5.2), Pt(curtain), hg, tag="hair_curtain")
        # The locks over the front of her shoulders, down onto her breast.
        b2 = m.bone(f"hair_{side}_2", (s * 3.0, NECK, -2.2), f"hair_{side}", rotation=(0, 0, -5 * s))
        lock = look.cut(hr, look.lock_end, 1.6, seed + (s > 0))
        m.cube(b2, (s * 3.0 - 0.95, 17.4, -2.65), (1.9, NECK - 17.4 + 0.2, 0.8), lock, hg, tag="hair_lock")
    # The back: over the back of the head, then three lengths down her back, each its own bone so it can sway.
    m.cube("hair_back", (-3.65, NECK - 0.6, HEAD / 2 - 0.4), (7.3, HEAD + 0.6, 1.0), none_on(("up",), hr), hg, inflate=0.2,
           tag="hair_back")
    segs = (("hair_back_2", "hair_back", (0, NECK - 0.4, 3.7), (-15, 0, 0), 18.0, 7.0),
            ("hair_back_3", "hair_back_2", (0, 18.0, 3.7), (12, 0, 0), 14.2, 6.8),
            ("hair_back_4", "hair_back_3", (0, 14.2, 3.7), (3, 0, 0), 10.4, 6.4))
    top = NECK - 0.4
    for i, (name, parent, piv, rot, bottom, width) in enumerate(segs):
        m.bone(name, piv, parent, rotation=rot)
        mat = look.cut(hr, look.hair_end, 2.4 if i == 2 else 1.4, seed + 10 + i)
        m.cube(name, (-width / 2, bottom, 3.1), (width, top - bottom + 0.3, 1.0), none_on(("up",), mat), hg, tag="hair_length")
        top = bottom


# =====================================================================================================
# The body and the dress
# =====================================================================================================

def build_torso(m, look, seed=40300):
    dr = look.dress(seed)
    dg = look.dress_glow(seed)
    sk = look.skin(seed + 1)

    def skin_at(f, x, y, w, h, p):
        return sk.fn(f, x, y, w, h, p) if isinstance(sk, Pt) else sk(f, x, y, w, h)

    def chest(f, x, y, w, h, p):
        if f == "north":
            # The V of the neckline, the collarbones in light shade above it.
            mid = (w - 1) / 2
            if abs(x - mid) <= 3.6 - y * 1.25 and y <= 2:
                s = skin_at(f, x, y, w, h, p)
                return shade(s, 0.9) if y == 0 and abs(x - mid) > 1.5 else s
            if y <= 3 and abs(x - mid) <= 4.6 - y * 1.25:
                return tone(DRESS, 4) if not look.void else dr.fn(f, x, y, w, h, p)        # the neckline's edge
        if f == "up":
            # The shoulders are covered; the neck's base and the top of the V are skin.
            mid = (w - 1) / 2
            if abs(x - mid) <= 2.5 and y <= 2:
                return shade(skin_at(f, x, y, w, h, p), 0.92)
        return dr.fn(f, x, y, w, h, p) if isinstance(dr, Pt) else dr(f, x, y, w, h)
    m.cube("torso", (-TW, 18.2, -TD), (2 * TW, ARM_TOP - 18.2, 2 * TD), Pt(chest), dg, tag="chest")

    def bust(f, x, y, w, h, p):
        if f == "up":
            return dr.fn(f, x, y, w, h, p) if isinstance(dr, Pt) else tone(DRESS, 3.4)
        if f == "north" and y == 0 and abs(x - (w - 1) / 2) <= 0.6:
            return skin_at(f, x, y, w, h, p) if not look.void else dr.fn(f, x, y, w, h, p)
        c = dr.fn(f, x, y, w, h, p) if isinstance(dr, Pt) else dr(f, x, y, w, h)
        if f == "north" and not look.void and y == h - 1:
            c = shade(c, 0.7)
        return c
    m.cube("torso", (-2.6, 18.6, -TD - 0.5), (5.2, 2.0, 0.5), Pt(bust), dg, tag="bust")

    def waist(f, x, y, w, h, p):
        c = dr.fn(f, x, y, w, h, p) if isinstance(dr, Pt) else dr(f, x, y, w, h)
        if not look.void and f in ("north", "south", "east", "west") and y == h - 2:
            c = tone(DRESS, 1)                     # the seam where the skirt is gathered at the waist
        return c
    m.cube("torso", (-WW, 14.4, -WD), (2 * WW, 18.2 - 14.4, 2 * WD), Pt(waist), dg, tag="waist")


def build_dress(m, look, seed=40400):
    dr = look.dress(seed)
    dg = look.dress_glow(seed)
    m.bone("dress", (0, SKIRT_TOP, 0), "body")
    # The hips.
    m.cube("dress", (-3.2, 11.0, -1.9), (6.4, 14.8 - 11.0, 3.8), dr, dg, tag="hips")

    def hemmed(mat, s=seed):
        def m_(f, x, y, w, h, p):
            if p[1] < look.hem - 0.01 + (0.3 if h01("hemj", s, f, x) < 0.3 else 0):
                return None
            c = mat.fn(f, x, y, w, h, p) if isinstance(mat, Pt) else mat(f, x, y, w, h)
            if not look.void and f in ("north", "south", "east", "west") and p[1] < look.hem + 0.5:
                c = shade(c, 0.7)                      # the hem
            return c
        return Pt(m_)
    L = SKIRT_TOP - HEM_Y
    m.bone("dress_front", (0, SKIRT_TOP, -1.9), "dress", rotation=(-5, 0, 0))
    m.cube("dress_front", (-3.75, HEM_Y, -2.4), (7.5, L, 0.5), hemmed(dr), dg, tag="skirt_front")
    for side, s in SIDES:
        m.bone(f"dress_{side}", (s * 3.2, SKIRT_TOP, 0), "dress", rotation=(0, 0, 6 * -s))
        x0 = s * 3.2 - 0.5 if s < 0 else s * 3.2
        m.cube(f"dress_{side}", (x0, HEM_Y, -2.45), (0.5, L, 4.9), hemmed(dr, seed + (s > 0)), dg, tag="skirt_side")
        # Gores over the corners, so the flared panels never open a gap.
        cx, cz = s * 3.55, -2.2
        m.cube(f"dress_{side}", (cx - 1.0, HEM_Y, cz - 0.25), (2.0, L, 0.5), hemmed(dr, seed + 3 + (s > 0)), dg, tag="skirt_gore",
               rotation=(-4, 45 * s, 0), pivot=(cx, SKIRT_TOP, cz))
        cz = 2.2
        m.cube(f"dress_{side}", (cx - 1.0, HEM_Y + 0.4, cz - 0.25), (2.0, L - 0.4, 0.5), hemmed(dr, seed + 5 + (s > 0)), dg,
               tag="skirt_gore", rotation=(5, -45 * s, 0), pivot=(cx, SKIRT_TOP, cz))
    # The back panel and its train, longer: it trails a little on the ground behind her.
    m.bone("dress_tail", (0, 13.0, 1.9), "dress", rotation=(6, 0, 0))
    m.cube("dress_tail", (-3.75, 5.0, 1.9), (7.5, 13.0 - 5.0, 0.5), dr, dg, tag="skirt_back")
    m.bone("dress_train", (0, 5.0, 2.15), "dress_tail", rotation=(14, 0, 0))
    m.cube("dress_train", (-3.95, 0.0, 1.9), (7.9, 5.2, 0.5), hemmed(dr, seed + 7), dg, tag="skirt_train")


def build_arms(m, look, seed=40500):
    dr = look.dress(seed)
    dg = look.dress_glow(seed)
    sk = look.skin(seed + 1, 3.3, 0.35)
    skg = look.skin_glow(seed + 1)

    def sleeve_cut(mat, y_bare):
        # The girl's short sleeves: skin below y_bare.
        def m_(f, x, y, w, h, p):
            if look.girl and p[1] < y_bare:
                return sk(f, x, y, w, h)
            return mat.fn(f, x, y, w, h, p) if isinstance(mat, Pt) else mat(f, x, y, w, h)
        return Pt(m_)
    for side, s in SIDES:
        cx = AX * s
        m.bone(f"{side}_arm", (cx, SY, 0), "torso")
        m.bone(f"{side}_forearm", (cx, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (cx, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (cx, PALM, 0), f"{side}_hand")
        m.bone(f"{side}_thumb", (cx - 0.55 * s, PALM + 1.1, -0.7), f"{side}_hand")
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW - 0.2, -AW / 2), (AW, ARM_TOP - ELBOW + 0.2, AW), sleeve_cut(dr, 19.6), dg,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - AW / 2 + 0.1, WRIST + 0.4, -AW / 2 + 0.1), (AW - 0.2, ELBOW - WRIST - 0.4, AW - 0.2),
               none_on(("up",), sleeve_cut(dr, 99)), dg, tag="forearm")
        if not look.girl:
            # The sleeve flares a little at the wrist.
            def cuff(f, x, y, w, h, p):
                if f == "up":
                    return None
                c = dr.fn(f, x, y, w, h, p) if isinstance(dr, Pt) else dr(f, x, y, w, h)
                return shade(c, 0.8) if y == h - 1 and not look.void else c
            m.cube(f"{side}_forearm", (cx - AW / 2, WRIST, -AW / 2), (AW, 1.0, AW), Pt(cuff), dg, inflate=0.2, tag="cuff")
        else:
            m.cube(f"{side}_forearm", (cx - AW / 2, WRIST, -AW / 2), (AW, 1.0, AW), none_on(("up",), sk), skg, tag="wrist")
        m.cube(f"{side}_hand", (cx - 0.85, PALM, -1.0), (1.7, WRIST - PALM + 0.2, 2.0), sk, skg, tag="palm")
        fing = lambda f, x, y, w, h: (shade(sk.fn(f, x, y, w, h, (0, 0, 0)) if isinstance(sk, Pt) else sk(f, x, y, w, h),
                                            0.86 if f in ("north", "south") and x % 2 else 1.0))
        m.cube(f"{side}_fingers", (cx - 0.8, PALM - 1.5, -0.9), (1.6, 1.5, 1.8), fing if not look.void else sk, skg, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 0.55 * s - 0.3, PALM + 0.1, -1.25), (0.6, 1.2, 0.6), sk, skg, tag="thumb")


def build_legs(m, look, seed=40600):
    sk = look.skin(seed, 3.0, 0.3)
    skg = look.skin_glow(seed)
    for side, s in SIDES:
        x0 = LX * s - LW / 2
        m.bone(f"{side}_leg", (LX * s, HIP, 0), "body")
        m.bone(f"{side}_shin", (LX * s, KNEE, 0), f"{side}_leg")
        thigh = sk if look.girl or look.void else (lambda f, x, y, w, h: tone(DRESS, 1.4))
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), thigh, skg, tag="thigh")

        def shin(f, x, y, w, h, p):
            if look.girl and p[1] < 3.2:
                return tone(SOCK, 3.6 - (0.6 if f in ("east", "west") else 0))      # white socks
            if look.void:
                return sk.fn(f, x, y, w, h, p)
            if not look.girl:
                return tone(DRESS, 1.6 + 0.6 * h01("st", f, x, y))                     # black stockings under the dress
            return sk(f, x, y, w, h)
        m.cube(f"{side}_shin", (x0 + 0.1, 1.0, -LW / 2 + 0.1), (LW - 0.2, KNEE - 1.0, LW - 0.2), Pt(shin), skg, tag="shin")

        def shoe(f, x, y, w, h, p):
            if look.void:
                return look.night(seed + 3, 1.2).fn(f, x, y, w, h, p)
            c = tone(SHOE, 2.0 - 0.8 * y / max(1, h))
            if f == "up" or (f in ("north", "east", "west") and y == 0):
                c = SHOE[4]
            if look.girl and f == "up" and y == 3:
                c = SHOE[1]                            # the Mary Jane's strap
            return c
        m.cube(f"{side}_shin", (x0, 0, -2.2), (LW, 1.0, 3.6), Pt(shoe), skg, tag="shoe")


# =====================================================================================================
# What the renderer shows or hides
# =====================================================================================================

def smoke_mat(seed, alpha=235, taper=True, base=2.0, rim_glow=True, rim=110):
    """A plane of smoke: a dark wisp tapering to a ragged point, holes where it thins, a faint violet rim."""
    def mat(f, x, y, w, h):
        t = y / max(1, h - 1)
        c0 = (w - 1) / 2
        hw = (w / 2) * ((1 - 0.85 * t) if taper else 1.0) + 0.3
        wob = math.sin(y * 0.6 + seed) * 0.7
        dx = abs(x - c0 - wob)
        if dx > hw:
            return None
        n = fbm(x * 3 + len(f), y * 1.2, seed, 64, 64, 3, 5.0)
        if n < 0.32 + 0.3 * t:
            return None
        col = tone(SMOKE, base + (n - 0.5) * 2.4)
        if dx > hw - 1.0:
            col = mix(col, VIOLET[1], 0.35)
        return rgba(col, alpha * (1 - 0.3 * t))

    def glow(f, x, y, w, h, col):
        t = y / max(1, h - 1)
        c0 = (w - 1) / 2
        hw = (w / 2) * ((1 - 0.85 * t) if taper else 1.0) + 0.3
        wob = math.sin(y * 0.6 + seed) * 0.7
        if rim_glow and abs(x - c0 - wob) > hw - 1.0:
            return rgba(VIOLET[2], rim * (1 - t))
        return None
    return mat, glow


def smoke_box(seed, base=1.8, alpha=245):
    """A smoke tendril's segment: dark, swirling, ragged where the faces meet, the edges lit faint violet."""
    def mat(f, x, y, w, h):
        n = fbm(x * 2.5 + len(f) * 9, y * 1.4, seed, 64, 64, 3, 6.0)
        edge = f in ("north", "south", "east", "west") and (x == 0 or x == w - 1)
        if edge and n < 0.42:
            return None
        col = tone(SMOKE, base + (n - 0.5) * 2.6)
        if edge:
            col = mix(col, VIOLET[1], 0.4)
        return rgba(col, alpha)

    def glow(f, x, y, w, h, col):
        if f in ("north", "south", "east", "west") and (x == 0 or x == w - 1):
            return rgba(VIOLET[2], 120)
        n = fbm(x * 2.5 + len(f) * 9, y * 1.4, seed, 64, 64, 3, 6.0)
        if n > 0.74:
            return rgba(VIOLET[1], 90)
        return None
    return mat, glow


def build_smoke_skirt(m):
    """Eight wisps round her from the knee down, hanging well below the hem: when she floats she trails smoke."""
    m.bone("smoke_skirt", (0, 6.0, 0), "body")
    n = 8
    for i in range(n):
        a = 2 * math.pi * (i + 0.5) / n
        r = 3.9
        x, z = math.sin(a) * r, -math.cos(a) * r
        b = m.bone(f"smoke_wisp_{i}", (x, 6.5, z), "smoke_skirt")
        mat, glow = smoke_mat(40700 + i, 230)
        ln = 12.5 + 2.0 * h01("wl", i)
        yaw = -math.degrees(a)
        m.cube(b, (x - 2.0, 6.5 - ln, z), (4.0, ln, 0), mat, glow, faces=("north", "south"), rotation=(0, yaw, 0), pivot=(x, 6.5, z),
               tag="wisp", shading=False)
        mat, glow = smoke_mat(40720 + i, 200)
        m.cube(b, (x - 1.4, 6.0 - ln * 0.85, z), (2.8, ln * 0.85, 0), mat, glow, faces=("north", "south"),
               rotation=(0, yaw + 90, 0), pivot=(x, 6.0, z), tag="wisp", shading=False)
    # The core under the hem: a column of smoke where her feet would be.
    mat, glow = smoke_mat(40740, 240)
    for k in range(2):
        m.cube("smoke_skirt", (-3.0, -7.0, 0), (6.0, 9.0, 0), mat, glow, faces=("north", "south"), rotation=(0, 45 + 90 * k, 0),
               pivot=(0, 0, 0), tag="wisp_core", shading=False)


TENDRILS = [  # (root x, root y, rest rotation of the first segment) -- the uppers rise over her shoulders, the lowers fan out
    (-1.3, 20.2, (150, 50, 30)), (1.3, 20.2, (150, -50, -30)),
    (-1.1, 16.6, (62, 0, 50)), (1.1, 16.6, (62, 0, -50)),
]
TENDRIL_SEGS = [(5.6, 2.6), (5.0, 2.1), (4.5, 1.6), (4.0, 1.1)]
# Each segment's bend from the one before (right side; the left mirrors Z): the uppers arc back and out like wings,
# the lowers droop and then lift their tips.
TENDRIL_CURL = {0: [(0, 0, 0), (10, 0, 14), (12, 0, 16), (14, 0, 18)],
                1: [(0, 0, 0), (10, 0, 14), (12, 0, 16), (14, 0, 18)],
                2: [(0, 0, 0), (-10, 0, 10), (-16, 0, -8), (-20, 0, -16)],
                3: [(0, 0, 0), (-10, 0, 10), (-16, 0, -8), (-20, 0, -16)]}


def tendril_name(i, k):
    return f"tendril_{i}" + ("" if k == 0 else f"_{k + 1}")


def build_tendrils(m):
    m.bone("tendrils", (0, 19.0, TD), "torso")
    for i, (x, y, rot) in enumerate(TENDRILS):
        s = -1 if x < 0 else 1
        parent, z = "tendrils", TD + 0.2
        for k, (ln, wd) in enumerate(TENDRIL_SEGS):
            r = rot if k == 0 else (TENDRIL_CURL[i][k][0], 0, TENDRIL_CURL[i][k][2] * -s)
            name = tendril_name(i, k)
            m.bone(name, (x, y, z), parent, rotation=r)
            mat, glow = smoke_box(40800 + i * 10 + k)
            m.cube(name, (x - wd / 2, y - ln - 0.3, z - wd / 2), (wd, ln + 0.5, wd), mat, glow, tag="tendril", shading=False)
            if k == len(TENDRIL_SEGS) - 1:
                mat, glow = smoke_mat(40850 + i, 220)
                m.cube(name, (x - 1.0, y - ln - 3.2, z), (2.0, 3.2, 0), mat, glow, faces=("north", "south"), tag="tendril_tip",
                       shading=False)
                m.cube(name, (x, y - ln - 3.2, z - 1.0), (0, 3.2, 2.0), mat, glow, faces=("east", "west"), tag="tendril_tip",
                       shading=False)
            parent = name
            y -= ln


def build_corona(m):
    """The black sun behind her head: a disc of night with a thin burning rim, short dense rays and long ones."""
    cy, z = NECK + 3.5, HEAD / 2 + 1.4
    m.bone("corona", (0, cy, z), "head")
    m.bone("corona_inner", (0, cy, z), "corona")
    m.bone("corona_outer", (0, cy, z), "corona")
    R = 4.6

    def disc(f, x, y, w, h):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        d = math.hypot(x - cx_, y - cy_) / (w / 2)
        if d > 1.0:
            return None
        if d > 0.86:
            return GOLD[3] if h01("rim", x, y) < 0.5 else VIOLET[4]
        if d > 0.78:
            return mix(INK, VIOLET[1], 0.6)
        return INK

    def disc_glow(f, x, y, w, h, col):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        d = math.hypot(x - cx_, y - cy_) / (w / 2)
        if d > 0.86:
            return rgba(col, 255)
        if d > 0.78:
            return rgba(VIOLET[2], 160)
        return rgba(INK, 255)
    m.cube("corona", (-R, cy - R, z), (2 * R, 2 * R, 0), disc, disc_glow, faces=("north", "south"), tag="corona_disc", shading=False)

    def ray(seed, long):
        def mat(f, x, y, w, h):
            t = 1 - y / max(1, h - 1)                 # 0 at the root, 1 at the tip
            c0 = (w - 1) / 2
            hw = w / 2 * (1 - 0.8 * t) + 0.2
            if abs(x - c0) > hw:
                return None
            if t > 0.75 and h01("rt", seed, x, y) < (t - 0.75) * 3:
                return None
            if abs(x - c0) > hw - 1.0:
                return mix(VIOLET[2], GOLD[3], 0.5 * t) if long else VIOLET[2]
            return mix(INK, SMOKE[3], 0.3 * t)

        def glow(f, x, y, w, h, col):
            t = 1 - y / max(1, h - 1)
            c0 = (w - 1) / 2
            hw = w / 2 * (1 - 0.8 * t) + 0.2
            if abs(x - c0) > hw - 1.0:
                return rgba(col, 200 * (1 - 0.5 * t))
            return rgba(INK, 230 * (1 - 0.6 * t))
        return mat, glow
    for i in range(12):
        a = 30 * i
        mat, glow = ray(40900 + i, False)
        m.cube("corona_inner", (-1.0, cy + R - 0.6, z + 0.05), (2.0, 3.2, 0), mat, glow, faces=("north", "south"),
               rotation=(0, 0, a), pivot=(0, cy, z + 0.05), tag="corona_ray", shading=False)
    for i in range(12):
        a = 30 * i + 15
        ln = 6.5 if i % 2 else 5.0
        mat, glow = ray(40930 + i, True)
        m.cube("corona_outer", (-1.0, cy + R - 0.4, z + 0.1), (2.0, ln, 0), mat, glow, faces=("north", "south"),
               rotation=(0, 0, a), pivot=(0, cy, z + 0.1), tag="corona_ray", shading=False)


def build_tears(m):
    """Two thin streaks of light down her cheeks from the eyes (the reconciliation)."""
    m.bone("tears", (0, NECK + 3.5, -HEAD / 2), "head")
    cols = {1: 0, 8: 0.3}

    def mat(f, x, y, w, h):
        for cx, ph in cols.items():
            wob = round(0.6 * math.sin(y * 0.9 + ph * 7))
            if x == cx + wob and y < h - (1 if cx == 8 else 0):
                return rgba(GOLD[5], 235)
            if y == h - 1 and x == cx + wob:
                return rgba(GOLD[4], 200)
        return None

    def glow(f, x, y, w, h, col):
        return rgba(mix(GOLD[5], STAR[0], 0.5), 255)
    m.cube("tears", (-2.5, NECK + 0.5, -HEAD / 2 - 0.08), (5.0, 3.0, 0), mat, glow, faces=("north",), tag="tears", shading=False)


CHAIN_SEGS = 6
CHAIN_LEN = 6.6      # per segment bone: 6 x 6.6 = ~40 px (2.5 blocks)
LINK = 4.2


def build_chains(m):
    """Smoke chains reaching forward from her right fist, link by link, ~2.5 blocks: dark smoke rings with a violet core.
    They lie along -Z (forward) from the fist at rest with the arm hanging; clips that raise the arm turn `chains` back."""
    hx, hy, hz = -AX, PALM - 0.6, -0.4
    m.bone("chains", (hx, hy, hz), "right_hand", rotation=(8, 0, 0))
    parent = "chains"
    z = hz - 0.6
    idx = 0
    for k in range(CHAIN_SEGS):
        name = f"chain_{k + 1}"
        m.bone(name, (hx, hy, z), parent, rotation=(1.5, 0, 0) if k else None)
        zz = z
        while zz > z - CHAIN_LEN + 0.01:
            vert = idx % 2 == 0

            def ring(f, x, y, w, h, vert=vert, idx=idx):
                # An oval ring: dark smoke outside, the violet ember on its inner edge.
                cx, cy = (w - 1) / 2, (h - 1) / 2
                d = math.hypot((x - cx) / (w / 2), (y - cy) / (h / 2))
                if d > 1.02 or d < 0.42:
                    return None
                if d < 0.6:
                    return VIOLET[3]
                return tone(SMOKE, 2.6 + (0.8 if h01("lk", idx, x, y) < 0.3 else 0))

            def ring_glow(f, x, y, w, h, col, idx=idx):
                cx, cy = (w - 1) / 2, (h - 1) / 2
                d = math.hypot((x - cx) / (w / 2), (y - cy) / (h / 2))
                if d < 0.6:
                    return rgba(VIOLET[4], 255)
                return rgba(VIOLET[1], 110) if h01("lg", idx, x, y) < 0.35 else None
            if vert:
                m.cube(name, (hx, hy - 1.25, zz - LINK), (0, 2.5, LINK), ring, ring_glow, faces=("east", "west"), density=4,
                       tag="chain_link", shading=False)
            else:
                m.cube(name, (hx - 1.25, hy, zz - LINK), (2.5, 0, LINK), ring, ring_glow, faces=("up", "down"), density=4,
                       tag="chain_link", shading=False)
            zz -= LINK - 1.0
            idx += 1
        parent = name
        z -= CHAIN_LEN


def build_bundle(m, look):
    """The newborn: swaddled in black cloth, lying in the air in a curl of smoke, her pale little face in the hood, the head
    end raised as if someone held her. Modelled ~3x life size (the renderer draws the baby at 0.30): ~27 px long, centred
    24 px up (about half a block in the world)."""
    BY = 24.0
    m.bone("bundle", (0, BY, 0), "root", rotation=(0, 0, 12))
    m.bone("bundle_body", (0, BY, 0), "bundle")
    m.bone("bundle_head", (-7.0, BY, 0), "bundle")
    m.bone("bundle_smoke", (0, BY - 3.0, 0), "bundle")
    seed = 41000

    def cloth(f, x, y, w, h, lift=0.0):
        n = fbm(x * 2 + len(f) * 5, y * 2, seed, 64, 64, 2, 5.0)
        v = 2.0 + lift + (n - 0.5) * 1.2
        # The wrapping: broad diagonal bands of folded cloth, a soft sheen on each fold's crest.
        if f in ("north", "south", "up", "down") and (x + y) % 10 in (0, 1):
            v -= 1.0
        elif f in ("north", "south", "up", "down") and (x + y) % 10 == 2:
            v += 1.2
        if f == "up":
            v += 0.5
        if f == "down":
            v -= 0.7
        return tone(CLOTH, v)
    lit = lambda f, x, y, w, h: cloth(f, x, y, w, h, 0.9)
    # The swaddle: an oval cross-section (three overlapping boxes), tapering in two steps to her feet (+X).
    m.cube("bundle_body", (-6.5, BY - 3.5, -3.5), (13.5, 7.0, 7.0), cloth, tag="swaddle")
    m.cube("bundle_body", (-6.0, BY - 4.2, -2.6), (12.5, 8.4, 5.2), cloth, tag="swaddle_round")
    m.cube("bundle_body", (-6.0, BY - 2.6, -4.2), (12.5, 5.2, 8.4), cloth, tag="swaddle_round")
    m.cube("bundle_body", (7.0, BY - 2.8, -2.8), (3.2, 5.6, 5.6), cloth, tag="swaddle_end")
    m.cube("bundle_body", (10.2, BY - 1.8, -1.8), (2.0, 3.6, 3.6), cloth, tag="swaddle_tip")
    # The loose corner of the cloth wrapped over her middle, its edge catching the light.
    m.cube("bundle_body", (-1.0, BY - 4.6, -4.7), (4.0, 9.2, 0.5), lit, rotation=(0, 0, 22), pivot=(1.0, BY, -4.5),
           tag="swaddle_fold")
    m.cube("bundle_body", (-1.0, BY + 4.2, -4.4), (4.0, 0.5, 8.8), lit, rotation=(0, 0, 22), pivot=(1.0, BY + 4.4, 0),
           tag="swaddle_fold_top")
    # The hood round her head, open toward the viewer (north), its brim a soft roll of cloth framing the face.
    hx = -7.0
    m.cube("bundle_head", (hx - 5.0, BY - 4.4, -4.0), (5.6, 8.8, 8.4), none_on(("north",), cloth), tag="hood")
    m.cube("bundle_head", (hx - 4.4, BY + 3.8, -3.4), (4.8, 1.0, 7.0), cloth, tag="hood_crown")
    m.cube("bundle_head", (hx - 5.2, BY + 2.6, -4.9), (6.0, 1.6, 1.4), lit, tag="hood_brim")
    m.cube("bundle_head", (hx - 5.2, BY - 4.4, -4.9), (6.0, 1.6, 1.4), lit, tag="hood_chin")
    m.cube("bundle_head", (hx - 5.6, BY - 4.0, -4.9), (1.2, 8.0, 1.4), lit, tag="hood_side")
    m.cube("bundle_head", (hx - 0.2, BY - 4.0, -4.9), (1.2, 8.0, 1.4), lit, tag="hood_side")
    sk = Look("girl").skin(seed + 1, 3.7, 0.3)
    BABY = [  # 9 x 10 texels (4.5 x 5 px): a wisp of dark hair, big dark eyes, round cheeks, a tiny mouth
        "hHHhhHHHh",
        "sshsssss.",
        "sssssssss",
        "sbssssbss",
        "wkksswkks",
        "skksssksd",
        "csssssssc",
        "sssnsssss",
        "sssquqsss",
        "dsssssssd",
    ]

    def face(f, x, y, w, h):
        s = sk(f, x, y, w, h)
        if f != "north":
            return shade(s, 0.85)
        ch = BABY[y][x] if y < len(BABY) and x < len(BABY[0]) else "s"
        return {"h": HAIR[3], "H": HAIR[2], "b": mix(s, BROW, 0.35), "w": mix(EYE_WHITE, s, 0.2), "k": IRIS[1],
                "c": mix(s, BLUSH, 0.45), "n": shade(s, 0.88), "q": mix(LIPS_GIRL[2], s, 0.5), "u": LIPS_GIRL[2],
                "d": shade(s, 0.88)}.get(ch, s)

    def face_glow(f, x, y, w, h, col):
        if f != "north":
            return None
        ch = BABY[y][x] if y < len(BABY) and x < len(BABY[0]) else "s"
        if ch in "wk":
            return rgba(INK, 255) if ch == "k" else rgba(VIOLET[2], 255)
        return None
    m.cube("bundle_head", (hx - 4.4, BY - 2.8, -4.3), (4.5, 5.0, 4.5), face, face_glow, tag="baby_face")
    # A tiny hand peeking out of the fold.
    m.cube("bundle_body", (-1.8, BY - 0.4, -5.0), (1.4, 1.2, 0.9), sk, tag="baby_hand")
    # The curl of smoke: a crescent cradle under her, wisps hanging from it, its two ends curling up past her head and
    # her feet; a ribbon of smoke winding over the top.
    for i in range(11):
        t = (i / 10) * 2 - 1                   # -1 .. 1 along her length
        x = t * 15.0
        y = BY - 5.6 + 5.5 * t ** 4 + 1.5 * t * t
        a = math.degrees(math.atan(t * 1.8))
        for layer, z in enumerate((-3.0, 0.5, 3.5)):
            if layer == 1 and i % 2:
                continue
            mat, glow = smoke_mat(41100 + i * 3 + layer, 230, taper=True, base=2.2 - 0.3 * layer, rim=170)
            m.cube("bundle_smoke", (x - 2.4, y - 5.5, z), (4.8, 6.0, 0), mat, glow, faces=("north", "south"),
                   rotation=(0, 0, a * 0.9), pivot=(x, y, z), tag="cradle", shading=False)
    for k, (x, a) in enumerate(((-15.5, -40), (15.5, 40), (-13.0, -20), (13.0, 20))):
        mat, glow = smoke_mat(41140 + k, 220, taper=True, base=2.4, rim=190)
        y = BY - 0.5 if abs(x) > 14 else BY - 1.6
        m.cube("bundle_smoke", (x - 1.8, y, -1.0 + k * 0.6), (3.6, 6.5, 0), mat, glow, faces=("north", "south"),
               rotation=(0, 0, 180 + a), pivot=(x, y, 0), tag="curl", shading=False)
    for i in range(4):
        mat, glow = smoke_mat(41150 + i, 200, taper=True, base=2.4, rim=150)
        x = 8.0 - i * 6.5
        m.cube("bundle_smoke", (x - 1.6, BY + 4.8, -1.5 + i), (3.2, 4.5, 0), mat, glow, faces=("north", "south"),
               rotation=(0, 15 * i, 150 + 20 * i), pivot=(x, BY + 4.8, 0), tag="wisp_top", shading=False)


# =====================================================================================================
# The rig
# =====================================================================================================

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("torso", (0, WAIST, 0), "body")
    m.bone("head", (0, NECK, 0), "torso")
    m.bone("eyes", (0, NECK + 4.0, -HEAD / 2), "head")
    m.bone("mouth", (0, NECK + 1.25, -HEAD / 2), "head")
    m.bone("hair", (0, NECK + 7.0, 0), "head")
    m.bone("hair_back", (0, NECK + 7.0, HEAD / 2), "head")


def rig(age="woman"):
    lk = Look(age)
    m = Model(NAME, 512, 256)
    skeleton(m)
    build_head(m, lk)
    build_eyes(m, lk)
    build_mouth(m, lk)
    build_hair(m, lk)
    build_torso(m, lk)
    build_arms(m, lk)
    build_dress(m, lk)
    build_legs(m, lk)
    build_smoke_skirt(m)
    build_tendrils(m)
    build_corona(m)
    build_tears(m)
    build_chains(m)
    build_bundle(m, lk)
    return m


TOGGLED = ("bundle", "smoke_skirt", "tendrils", "corona", "tears", "chains")


def hidden_for(age, show=()):
    """Bones hidden in an age's look for previews (the toggled ones not in `show`; the baby hides the body)."""
    out = [b for b in TOGGLED if b not in show]
    if age == "baby":
        out = [b for b in out if b != "bundle"] + ["body"]
    return tuple(out)



# =====================================================================================================
# Clips
# =====================================================================================================
#
# Base loops key the whole body. Attacks key the upper body, the hair, the tendrils, the chains and the corona, never the
# root, the legs or the dress, so they play over `float` as well as `idle` (the exceptions stamp, kneel or vanish on purpose).
# No clip scales `head` (the renderer scales it for the children).

def sym(**kw):
    """A pose for both sides from the right side's values (the left mirrors Y and Z)."""
    out = {}
    for k, v in kw.items():
        out["right_" + k] = list(v)
        out["left_" + k] = [v[0], -v[1], -v[2]]
    return out


def jit(seed, t, amp, length, ks=(7, 11, 13)):
    """A seamless shake (integer-cycle sines, so a loop of `length` closes)."""
    out = []
    for i in range(3):
        v = 0.0
        for j, k in enumerate(ks):
            ph = h01(seed, i, j) * 2 * math.pi
            v += math.sin(2 * math.pi * k * t / length + ph) / (j + 1)
        out.append(amp * v / 1.8)
    return out


def during(fn, t0, t1, ramp=0.1):
    """An additive layer active only between t0 and t1 (eased in and out over `ramp` seconds)."""
    def g(t):
        if t <= t0 or t >= t1:
            return [0, 0, 0]
        k = min(1.0, (t - t0) / ramp, (t1 - t) / ramp)
        return [v * k for v in fn(t)]
    return g


TENDRIL_BONES = [tendril_name(i, k) for i in range(4) for k in range(4)]
HAIR_BACK = ("hair_back_2", "hair_back_3", "hair_back_4")
WISPS = [f"smoke_wisp_{i}" for i in range(8)]
CHAIN_BONES = [f"chain_{k + 1}" for k in range(CHAIN_SEGS)]


def tendril_wave(c, amp, cycles, length, lag=0.9, seed=0.0):
    """A slow undulation down all four tendrils (integer cycles so loops close)."""
    for i in range(4):
        s = -1 if TENDRILS[i][0] < 0 else 1
        for k in range(4):
            kk = 0.45 + 0.35 * k

            def fn(t, i=i, k=k, s=s, kk=kk):
                ph = 2 * math.pi * cycles * t / length - k * lag + i * 1.3 + seed
                return [amp * kk * math.sin(ph), 0, -s * amp * 0.6 * kk * math.cos(ph + 0.6)]
            c.layer(tendril_name(i, k), fn)


def tendril_pose(spread=0.0, curl=0.0, lift=0.0):
    """Tendrils flung out (spread > 0, flared wide and straight) or drawn in (spread < 0), curled tips, raised roots."""
    out = {}
    for i in range(4):
        s = -1 if TENDRILS[i][0] < 0 else 1
        for k in range(4):
            if k == 0:
                out[tendril_name(i, k)] = [-lift * 30 + spread * 10, 0, -s * spread * 22]
            else:
                out[tendril_name(i, k)] = [curl * -14 + spread * 12, 0, -s * curl * 8 + s * spread * 6]
    return out


def hair_sway(c, amp, cycles, length, seed=0.0):
    for i, b in enumerate(HAIR_BACK):
        c.layer(b, lambda t, i=i: [amp * (0.5 + 0.4 * i) * math.sin(2 * math.pi * cycles * t / length - i * 0.8 + seed), 0,
                                   amp * 0.4 * math.sin(2 * math.pi * cycles * t / length + 1.1 + i)])
    for side, s in SIDES:
        c.layer(f"hair_{side}_2", lambda t, s=s: [amp * 0.6 * math.sin(2 * math.pi * cycles * t / length + 0.4 + seed), 0,
                                                    s * amp * 0.3 * math.sin(2 * math.pi * cycles * t / length + 2.0)])


def dress_sway(c, amp, cycles, length, seed=0.0):
    def w(k, ph):
        return lambda t: [amp * k * math.sin(2 * math.pi * cycles * t / length + ph + seed), 0, 0]
    c.layer("dress_front", w(1.0, 0.0))
    c.layer("dress_tail", w(0.8, 1.2))
    c.layer("dress_train", w(1.3, 0.4))
    for side, s in SIDES:
        c.layer(f"dress_{side}", lambda t, s=s: [0, 0, -s * amp * 0.6 * (0.6 + 0.4 * math.sin(2 * math.pi * cycles * t / length + s + seed))])


def wisp_flutter(c, amp, cycles, length):
    for i, b in enumerate(WISPS):
        c.layer(b, lambda t, i=i: [amp * math.sin(2 * math.pi * cycles * t / length + i * 0.9), 0,
                                   amp * 0.7 * math.sin(2 * math.pi * cycles * t / length + i * 1.7 + 0.5)])


def chain_wave(c, amp, cycles, length, decay=None):
    for k, b in enumerate(CHAIN_BONES):
        def fn(t, k=k):
            e = 1.0 if decay is None else decay(t)
            ph = 2 * math.pi * cycles * t / length - k * 0.9
            return [e * amp * math.sin(ph), e * amp * 0.3 * math.sin(ph + k), 0]
        c.layer(b, fn)


# --- the poses ---------------------------------------------------------------------------------------------------------

# At rest: tall and still, arms loose at her sides, fingers softly curled, the chin a touch down: a level, curious stare.
IDLE = P_(sym(arm=[-2, 0, 5], forearm=[-12, 0, 0], hand=[0, 0, -4], fingers=[12, 0, 0]), {"head": [3, 0, 0]})
# The girl: hands clasped behind her back, weight on one foot, head on one side.
CHILD = P_(sym(arm=[26, 0, -16], forearm=[-30, 0, 0], hand=[0, 0, 10], fingers=[30, 0, 0]),
           {"head": [2, 0, 8], "body": [0, 0, -2], "left_leg": [-4, 0, -2], "left_shin": [10, 0, 0], "torso": [-2, 0, 2]})
# Floating: a hand's breadth off the ground, toes down, arms a little away from her, the skirt drifting.
FLOAT = P_(sym(arm=[-6, 0, 16], forearm=[-16, 0, 0], hand=[0, 0, -10], fingers=[-4, 0, 0]),
           {"@root": [0, 4.0, 0], "right_leg": [-4, 0, 1], "right_shin": [12, 0, 0], "left_leg": [3, 0, -1], "left_shin": [16, 0, 0],
            "head": [-3, 0, 0], "dress_front": [-6, 0, 0], "dress_tail": [8, 0, 0], "dress_train": [14, 0, 0],
            "dress_right": [0, 0, 5], "dress_left": [0, 0, -5]})
# On her knees, sitting back on her heels, the skirt pooled round her, hands in her lap, head bowed.
KNEEL = P_(sym(arm=[-24, 0, -4], forearm=[-40, 0, 0], hand=[10, 0, 0], fingers=[20, 0, 0]),
           {"@root": [0, -5.6, 0], "right_leg": [-12, 0, 3], "left_leg": [-12, 0, -3], "right_shin": [100, 0, 0], "left_shin": [100, 0, 0],
            "torso": [8, 0, 0], "head": [20, 0, 0], "dress_front": [-34, 0, 0], "dress_right": [0, 0, 22], "dress_left": [0, 0, -22],
            "dress_tail": [34, 0, 0], "dress_train": [30, 0, 0], "hair_back_2": [-6, 0, 0], "hair_back_3": [-10, 0, 0]})
# Holding him: both arms round someone a little shorter in front of her, her cheek against his shoulder.
EMBRACE = P_({"right_arm": [-80, 8, 0], "left_arm": [-70, -4, 6], "right_forearm": [0, 0, -52], "left_forearm": [0, 0, 56],
              "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "right_fingers": [20, 0, 0], "left_fingers": [20, 0, 0],
              "torso": [10, 0, 0], "head": [8, -14, 16], "hair_right_2": [-14, 0, 0], "hair_left_2": [-8, 0, 0]})
# Straining against what holds her: hunched, arms crossed tight over her breast, fists clenched.
STRUGGLE = {"right_arm": [-58, 0, -40], "left_arm": [-52, 0, 44], "right_forearm": [-70, 0, -10], "left_forearm": [-74, 0, 12],
            "right_fingers": [80, 0, 0], "left_fingers": [80, 0, 0], "right_thumb": [40, 0, 0], "left_thumb": [40, 0, 0],
            "torso": [18, 0, 0], "head": [14, 0, 0], "body": [4, 0, 0]}
OPEN = {"@mouth": [0, 0, -0.2]}
WIDE = {"@mouth": [0, -0.2, -0.2], "%mouth": [1.15, 1.6, 1]}
FIST = {"right_fingers": [85, 0, 0], "right_thumb": [50, 0, -20]}


def anims():
    f = AnimFile()

    def clip(name, length, loop=False, hold=False):
        return Clip(f, A + name, length, loop=loop, hold=hold)

    # =============================================================== loops
    # idle: still as a held breath; the head turns slowly to study you, tilts, looks away and back. The hair stirs.
    c = clip("idle", 4.0, loop=True)
    yaw, roll = [0, 9, 4, -7, 0], [0, 6, 9, 2, 0]
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = abs(math.sin(math.pi * t / 2))
        c.key(t, P_(IDLE, {"@torso": [0, 0.12 * b, 0], "head": [3 - b, yaw[i], roll[i]], "torso": [-1 * b, 0, 0]}), E_IO if i else None)
    hair_sway(c, 1.5, 1, 4.0)
    dress_sway(c, 1.0, 1, 4.0)
    tendril_wave(c, 7, 1, 4.0)
    wisp_flutter(c, 5, 2, 4.0)
    chain_wave(c, 2.5, 1, 4.0)
    c.done()

    # walk: unhurried, gliding; small steps under the long skirt, the arms barely moving, the train dragging behind.
    c = clip("walk", 1.2, loop=True)
    for t, s in ((0, 1), (0.6, -1), (1.2, 1)):
        c.key(t, P_(IDLE, {"right_leg": [18 * s, 0, 0], "left_leg": [-18 * s, 0, 0], "right_shin": [6 if s < 0 else 18, 0, 0],
                           "left_shin": [18 if s < 0 else 6, 0, 0], "right_arm": [-8 * s - 2, 0, 5], "left_arm": [8 * s - 2, 0, -5],
                           "body": [0, 4 * s, 0], "torso": [-1, -3 * s, 0], "head": [3, -2 * s, 0],
                           "dress_front": [-10, 0, 0], "dress_right": [0, 0, 4 * s], "dress_left": [0, 0, 4 * s],
                           "dress_tail": [10, 0, 0], "dress_train": [16, 0, 0]}), E_IO if t else None)
    c.layer("@body", lambda t: [0, 0.4 * abs(math.sin(2 * math.pi * t / 1.2)), 0])
    hair_sway(c, 3.0, 2, 1.2)
    tendril_wave(c, 9, 2, 1.2, lag=1.2)
    wisp_flutter(c, 8, 2, 1.2)
    chain_wave(c, 3, 1, 1.2)
    c.done()

    # float: hanging in the air, the skirt and the hair drifting as if under water; a slow bob.
    c = clip("float", 3.0, loop=True)
    c.key(0, FLOAT)
    c.key(3.0, FLOAT)
    c.layer("@root", lambda t: [0, 0.7 * math.sin(2 * math.pi * t / 3.0), 0])
    c.layer("right_leg", lambda t: [3 * math.sin(2 * math.pi * t / 3.0 + 0.3), 0, 0])
    c.layer("left_leg", lambda t: [3 * math.sin(2 * math.pi * t / 3.0 + 1.9), 0, 0])
    c.layer("right_arm", lambda t: [0, 0, 3 * math.sin(2 * math.pi * t / 3.0 + 0.5)])
    c.layer("left_arm", lambda t: [0, 0, -3 * math.sin(2 * math.pi * t / 3.0 + 0.5)])
    c.layer("head", lambda t: [1.5 * math.sin(2 * math.pi * t / 3.0 + 1.0), 5 * math.sin(2 * math.pi * t / 3.0), 0])
    hair_sway(c, 4.0, 1, 3.0)
    dress_sway(c, 5.0, 2, 3.0)
    tendril_wave(c, 10, 1, 3.0)
    wisp_flutter(c, 10, 2, 3.0)
    chain_wave(c, 3, 1, 3.0)
    c.done()

    # child_idle: the girl, hands behind her back, rocking heel to toe and side to side, her head tipping the other way.
    c = clip("child_idle", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        s = (1, 0, -1, 0, 1)[i]
        c.key(t, P_(CHILD, {"body": [0, 6 * s, -3 * s], "head": [2, -6 * s, 10 * s], "torso": [-2, 4 * s, 2 * s],
                            "@root": [0, 0.3 if s == 0 else 0, 0],
                            "right_leg": [-6 if s < 0 else 0, 0, 2], "right_shin": [14 if s < 0 else 0, 0, 0],
                            "left_leg": [-6 if s > 0 else 0, 0, -2], "left_shin": [14 if s > 0 else 0, 0, 0],
                            "dress_right": [0, 0, 6 * s], "dress_left": [0, 0, 6 * s], "dress_front": [-4 * abs(s), 0, 0]}),
              E_IO if i else None)
    hair_sway(c, 3.0, 2, 2.0)
    c.done()

    # bundle_idle: the newborn bobbing gently in the air, turning her head, the curl of smoke slowly winding round her.
    c = clip("bundle_idle", 6.0, loop=True)
    c.key(0, {"bundle_smoke": [0, 0, 0]})
    c.key(6.0, {"bundle_smoke": [0, 360, 0]})
    c.layer("@bundle", lambda t: [0, 1.0 * math.sin(2 * math.pi * t / 3.0), 0])
    c.layer("bundle", lambda t: [2 * math.sin(2 * math.pi * t / 6.0 + 0.6), 6 * math.sin(2 * math.pi * t / 6.0),
                                 3 * math.sin(2 * math.pi * t / 3.0)])
    c.layer("bundle_head", lambda t: [6 * math.sin(2 * math.pi * t / 6.0 + 1.5), 14 * math.sin(2 * math.pi * 2 * t / 6.0), 0])
    c.done()

    # kneel_idle: on her knees, head bowed, breathing.
    c = clip("kneel_idle", 3.0, loop=True)
    for i, t in enumerate((0, 0.75, 1.5, 2.25, 3.0)):
        b = math.sin(2 * math.pi * t / 1.5)
        c.key(t, P_(KNEEL, {"torso": [8 + 2 * b, 0, 0], "head": [20 - 2 * b, 3 * math.sin(2 * math.pi * t / 3.0), 0]}), E_IO if i else None)
    hair_sway(c, 1.0, 1, 3.0)
    tendril_wave(c, 4, 1, 3.0)
    c.done()

    # embrace_idle: holding him, swaying a little, her cheek on his shoulder.
    c = clip("embrace_idle", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        s = math.sin(2 * math.pi * t / 4.0)
        c.key(t, P_(EMBRACE, {"body": [0, 3 * s, 1.5 * s], "head": [10, -12 + 2 * s, 18 - 2 * s]}), E_IO if i else None)
    hair_sway(c, 1.0, 1, 4.0)
    dress_sway(c, 1.0, 1, 4.0)
    c.done()

    # struggle_idle: hunched, arms locked across her breast, shaking against what holds her.
    c = clip("struggle_idle", 1.0, loop=True)
    c.key(0, STRUGGLE)
    c.key(1.0, STRUGGLE)
    for b, amp, seed in (("torso", 2.5, 1), ("head", 4.0, 2), ("right_arm", 3.0, 3), ("left_arm", 3.0, 4), ("body", 1.2, 5)):
        c.layer(b, lambda t, amp=amp, seed=seed: jit(("si", seed), t, amp, 1.0))
    tendril_wave(c, 16, 3, 1.0, lag=1.4)
    chain_wave(c, 5, 2, 1.0)
    c.done()

    # =============================================================== visits
    # appear: she forms out of smoke: a low curl that rises and unfolds, arms drifting down to her sides.
    c = clip("appear", 1.5)
    c.key(0, P_(IDLE, {"%body": [0.3, 0.05, 0.3], "torso": [30, 0, 0], "head": [30, 0, 0], **sym(arm=[-40, 0, -30], forearm=[-60, 0, 0])}))
    c.key(0.6, P_(IDLE, {"%body": [0.8, 0.7, 0.8], "torso": [18, 0, 0], "head": [20, 0, 0], **sym(arm=[-20, 0, 30], forearm=[-30, 0, 0])}),
          E_OUT)
    c.key(1.0, P_(IDLE, {"%body": [1.02, 1.04, 1.02], "torso": [-4, 0, 0], "head": [-6, 0, 0], **sym(arm=[-6, 0, 22])}), E_IO)
    c.key(1.5, IDLE, E_IO)
    tendril_wave(c, 14, 1, 1.5)
    c.done()

    # vanish: a last look; the arms lift a little, she turns and unravels into smoke (held: the code hides her).
    c = clip("vanish", 1.2, hold=True)
    c.key(0, IDLE)
    c.key(0.4, P_(IDLE, {"head": [-4, -8, 6], **sym(arm=[-6, 0, 18])}), E_IO)
    c.key(0.8, P_(IDLE, {"body": [0, -60, 0], "%body": [0.7, 0.75, 0.7], "@root": [0, 1.0, 0], "head": [-10, 0, 0],
                         **sym(arm=[-10, 0, 50], forearm=[-10, 0, 0])}), E_IN)
    c.key(1.2, P_(IDLE, {"body": [0, -160, 0], "%body": [0.02, 0.02, 0.02], "@root": [0, 3.0, 0], "head": [-10, 0, 0],
                         **sym(arm=[-10, 0, 70], forearm=[-10, 0, 0])}), E_IN)
    tendril_wave(c, 20, 2, 1.2)
    c.done()

    # talk: words with a small open hand (the left), the head on one side; the lips part twice.
    c = clip("talk", 1.5)

    def hand(k, z):
        return {"left_arm": [-14 * k, 0, -10 * k], "left_forearm": [-60 * k, 10 * k, 0], "left_hand": [0, 70 * k, z], "left_fingers": [-6, 0, 0]}
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, hand(0.8, 6), OPEN, {"head": [0, -6, 6]}), E_IO)
    c.key(0.5, P_(IDLE, hand(1.0, -6), {"head": [2, -8, 9]}), E_OUT)
    c.key(0.8, P_(IDLE, hand(0.9, 4), OPEN, {"head": [0, 2, 10]}), E_IO)
    c.key(1.0, P_(IDLE, hand(0.8, 0), {"head": [2, 2, 8]}), E_IO)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # tilt: the head goes over on one side, curious, and stays a beat.
    c = clip("tilt", 0.8)
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, {"head": [2, 10, 22], "torso": [0, 4, 3]}), E_OUT)
    c.key(0.6, P_(IDLE, {"head": [2, 12, 24], "torso": [0, 4, 3]}))
    c.key(0.8, IDLE, E_IO)
    c.done()

    # reach: a hand held out to the hunter, palm up, the body leaning after it.
    c = clip("reach", 1.5)
    out = P_(IDLE, {"right_arm": [-72, 8, 6], "right_forearm": [-8, 0, 0], "right_hand": [0, -70, 0], "right_fingers": [-8, 0, 0],
                    "torso": [8, 6, 0], "head": [0, 4, 6], "left_arm": [6, 0, -8], "chains": [70, 0, 0]})
    c.key(0, IDLE)
    c.key(0.6, out, E_OUT)
    c.key(1.05, P_(out, {"right_fingers": [10, 0, 0], "right_arm": [-70, 8, 6]}), E_IO)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # feed: she breathes in. A hand rises toward her lips, palm in; on tick 24 her head goes back, mouth open, and the soul
    # pours in; then she lowers her chin, sated.
    c = clip("feed", 2.5)
    hand_up = {"left_arm": [-36, 0, -12], "left_forearm": [-110, 20, 0], "left_hand": [0, 60, 0], "left_fingers": [6, 0, 0],
               "right_arm": [-34, 10, 10], "right_forearm": [-20, 0, 0], "right_hand": [0, -60, 0], "right_fingers": [-6, 0, 0]}
    c.key(0, IDLE)
    c.key(0.7, P_(IDLE, hand_up, {"torso": [6, 0, 0], "head": [6, 0, 0]}), E_IO)
    c.key(1.2, P_(IDLE, hand_up, WIDE, {"torso": [-10, 0, 0], "head": [-22, 0, 0], "left_forearm": [-118, 20, 0]}), E_OUT)
    c.key(1.8, P_(IDLE, hand_up, OPEN, {"torso": [-8, 0, 0], "head": [-18, 0, 0]}), E_IO)
    c.key(2.2, P_(IDLE, {"head": [10, 0, 0], "torso": [2, 0, 0]}), E_IO)
    c.key(2.5, IDLE, E_IO)
    tendril_wave(c, 10, 1, 2.5)
    c.done()

    # laugh: the girl's giggle: a hand to her mouth, shoulders shaking, head tipping back.
    c = clip("laugh", 1.2)
    giggle = {"right_arm": [-40, 0, -20], "right_forearm": [-110, -20, 0], "right_fingers": [10, 0, 0],
              "left_arm": [-10, 0, -6], "left_forearm": [-40, 0, 0]}
    c.key(0, IDLE)
    c.key(0.25, P_(IDLE, giggle, OPEN, {"head": [-10, 0, 6], "torso": [-4, 0, 0]}), E_OUT)
    c.key(0.8, P_(IDLE, giggle, OPEN, {"head": [-14, 0, -4], "torso": [-6, 0, 0]}), E_IO)
    c.key(1.2, IDLE, E_IO)
    c.layer("torso", during(lambda t: jit("lg", t, 3.0, 1.2, (13, 17, 19)), 0.2, 0.9))
    c.layer("head", during(lambda t: jit("lh", t, 3.0, 1.2, (13, 17, 19)), 0.2, 0.9))
    c.done()

    # =============================================================== phase II: the girl
    # giggle_step: she sinks into her own shadow (flat on the ground by tick 8) and rises out of it again.
    c = clip("giggle_step", 1.0)
    flat = P_(IDLE, {"%body": [1.5, 0.02, 1.5], "@root": [0, -0.2, 0], "torso": [20, 0, 0], "head": [20, 0, 0]})
    c.key(0, IDLE)
    c.key(0.18, P_(IDLE, {"%body": [0.92, 1.06, 0.92], "head": [-8, 0, 8], "@root": [0, 0.6, 0]}), E_OUT)
    c.key(0.4, flat, E_IN)
    c.key(0.6, flat)
    c.key(0.85, P_(IDLE, {"%body": [0.95, 1.08, 0.95], "head": [-6, 0, -8]}), E_OUT)
    c.key(1.0, IDLE, E_IO)
    c.done()

    # hide: she curls up small into a knot of smoke and uncurls.
    c = clip("hide", 0.8)
    ball = P_(IDLE, {"@root": [0, -4.0, 0], "%body": [0.6, 0.6, 0.6], "torso": [60, 0, 0], "head": [40, 0, 0],
                     **sym(arm=[-70, 0, -30], forearm=[-80, 0, 0], fingers=[40, 0, 0]),
                     "right_leg": [-70, 0, 0], "left_leg": [-70, 0, 0], "right_shin": [130, 0, 0], "left_shin": [130, 0, 0],
                     "dress_front": [-60, 0, 0], "dress_tail": [40, 0, 0]})
    c.key(0, IDLE)
    c.key(0.3, ball, E_IN)
    c.key(0.5, ball)
    c.key(0.8, IDLE, E_OUT)
    c.done()

    # found: she pops up out of a crouch, arms flung wide -- "found you!" (tick 6) -- and settles.
    c = clip("found", 1.0)
    crouch = P_(IDLE, {"@root": [0, -2.4, 0], "torso": [24, 0, 0], "head": [10, 0, 0], **sym(arm=[-20, 0, -10], forearm=[-40, 0, 0]),
                       "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0], "right_shin": [50, 0, 0], "left_shin": [50, 0, 0],
                       "dress_front": [-20, 0, 0]})
    wide = P_(IDLE, WIDE, {"@root": [0, 2.0, 0], "torso": [-10, 0, 0], "head": [-16, 0, 10],
                           **sym(arm=[-30, 0, 120], forearm=[-10, 0, 0], fingers=[-20, 0, 0]),
                           "right_leg": [6, 0, 4], "left_leg": [-4, 0, -4], "right_shin": [10, 0, 0], "left_shin": [20, 0, 0],
                           "dress_front": [-10, 0, 0], "dress_right": [0, 0, 10], "dress_left": [0, 0, -10]})
    c.key(0, crouch)
    c.key(0.3, wide, E_OUT)
    c.key(0.55, P_(wide, {"@root": [0, 0, 0], "right_leg": [0, 0, 0], "left_leg": [0, 0, 0], "right_shin": [0, 0, 0],
                          "left_shin": [0, 0, 0]}), E_IN)
    c.key(1.0, IDLE, E_IO)
    c.done()

    # toss: an underhand throw (her right): the arm swings back, then forward and up, letting go on tick 12.
    c = clip("toss", 1.2)
    c.key(0, IDLE)
    c.key(0.4, P_(IDLE, {"right_arm": [50, 0, 10], "right_forearm": [-30, 0, 0], "right_fingers": [40, 0, 0], "torso": [8, 16, 0],
                         "head": [0, -10, 0], "left_arm": [-30, 0, -14]}), E_IO)
    c.key(0.6, P_(IDLE, {"right_arm": [-100, 0, 4], "right_forearm": [-10, 0, 0], "right_fingers": [-20, 0, 0], "torso": [-4, -10, 0],
                         "head": [-6, 4, 0], "left_arm": [20, 0, -10]}), E_SNAP)
    c.key(0.8, P_(IDLE, {"right_arm": [-110, 0, 4], "right_fingers": [-20, 0, 0], "torso": [-4, -12, 0], "head": [-6, 0, 8]}), E_OUT)
    c.key(1.2, IDLE, E_IO)
    c.done()

    # tantrum: fists clenched, she stamps (tick 14): the shock ring. Screaming.
    c = clip("tantrum", 1.5)
    fists = sym(arm=[6, 0, 14], forearm=[-10, 0, 0], fingers=[85, 0, 0], thumb=[50, 0, 0])
    up = P_(IDLE, fists, WIDE, {"torso": [12, 0, 0], "head": [-16, 0, 0], "right_leg": [-50, 0, 2], "right_shin": [70, 0, 0],
                                "dress_front": [-30, 0, 0], "dress_right": [0, 0, 6]})
    c.key(0, IDLE)
    c.key(0.5, up, E_OUT)
    c.key(0.7, P_(IDLE, fists, WIDE, {"@root": [0, -0.6, 0], "torso": [20, 0, 0], "head": [-24, 0, 0], "right_leg": [-4, 0, 2],
                                      "right_shin": [4, 0, 0], "dress_front": [-6, 0, 0], "dress_right": [0, 0, 12], "dress_left": [0, 0, -12]}),
          E_SNAP)
    c.key(1.0, P_(IDLE, fists, OPEN, {"torso": [14, 0, 0], "head": [-18, 0, 0]}), E_OUT)
    c.key(1.5, IDLE, E_IO)
    c.layer("head", during(lambda t: jit("tt", t, 5.0, 1.5, (19, 23, 29)), 0.7, 1.2))
    hair_sway(c, 6.0, 3, 1.5)
    c.done()

    # =============================================================== phase III: the woman
    # tk_lift: both hands rise before her, palms up, lifting everything (tick 16); held, then lowered.
    c = clip("tk_lift", 1.5)
    low = P_(IDLE, sym(arm=[-20, 0, 16], forearm=[-50, 0, 0], hand=[0, -80, 0], fingers=[-10, 0, 0]), {"head": [6, 0, 0]})
    high = P_(IDLE, sym(arm=[-62, 0, 26], forearm=[-24, 0, 0], hand=[0, -88, 0], fingers=[-24, 0, 0]),
              {"head": [-12, 0, 0], "torso": [-6, 0, 0]}, tendril_pose(spread=0.6, lift=0.4))
    c.key(0, IDLE)
    c.key(0.4, low, E_IO)
    c.key(0.8, high, E_OUT)
    c.key(1.15, P_(high, {"head": [-16, 0, 0]}))
    c.key(1.5, IDLE, E_IO)
    tendril_wave(c, 12, 1, 1.5)
    c.done()

    # tk_throw: both hands drawn back to her shoulders, then flung forward, fingers spread (tick 10).
    c = clip("tk_throw", 1.0)
    back = P_(IDLE, sym(arm=[-40, 0, 30], forearm=[-110, 0, 0], hand=[0, 0, 0], fingers=[40, 0, 0]),
              {"torso": [-10, 0, 0], "head": [-4, 0, 0]})
    fling = P_(IDLE, sym(arm=[-92, 0, 10], forearm=[-4, 0, 0], hand=[30, 0, 0], fingers=[-30, 0, 0]),
               {"torso": [14, 0, 0], "head": [-6, 0, 0], "hair_back_2": [10, 0, 0], "hair_back_3": [10, 0, 0]})
    c.key(0, IDLE)
    c.key(0.3, back, E_IO)
    c.key(0.5, fling, E_SNAP)
    c.key(0.7, P_(fling, {"torso": [10, 0, 0]}))
    c.key(1.0, IDLE, E_IO)
    c.done()

    # drain: one hand out, clawed, toward the nearest hunter; the other at her open mouth; she leans in and draws (tick 20).
    c = clip("drain", 2.5)
    draw = P_(IDLE, {"right_arm": [-80, 6, 4], "right_forearm": [-6, 0, 0], "right_hand": [20, 0, 0], "right_fingers": [40, 0, 0],
                     "right_thumb": [20, 0, 0],
                     "left_arm": [-40, 0, -10], "left_forearm": [-112, 18, 0], "left_hand": [0, 60, 0], "left_fingers": [0, 0, 0],
                     "torso": [14, 6, 0], "head": [-6, 4, 0], "chains": [80, 0, 0]})
    c.key(0, IDLE)
    c.key(0.6, P_(draw, {"right_fingers": [-10, 0, 0], "torso": [8, 6, 0]}), E_IO)
    c.key(1.0, P_(draw, WIDE), E_OUT)
    c.key(1.9, P_(draw, WIDE, {"right_arm": [-72, 6, 4], "torso": [10, 4, 0], "head": [-10, 4, 0]}), E_IO)
    c.key(2.5, IDLE, E_IO)
    c.layer("right_arm", during(lambda t: jit("dr", t, 2.0, 2.5, (17, 23, 29)), 1.0, 1.9))
    tendril_wave(c, 14, 2, 2.5)
    c.done()

    # strike: a backhand -- the right hand across to her left shoulder, then out (tick 8).
    c = clip("strike", 0.8)
    wind = P_(IDLE, {"right_arm": [-70, 0, -50], "right_forearm": [-60, 0, 0], "right_hand": [0, 0, 20], "torso": [0, 26, 0],
                     "head": [0, -16, 0]})
    hit = P_(IDLE, {"right_arm": [-80, 0, 70], "right_forearm": [-6, 0, 0], "right_hand": [0, 0, -20], "right_fingers": [20, 0, 0],
                    "torso": [4, -28, 0], "head": [0, 14, 0], "left_arm": [10, 0, -14], "hair_back_2": [0, 0, -10]})
    c.key(0, IDLE)
    c.key(0.25, wind, E_IO)
    c.key(0.4, hit, E_SNAP)
    c.key(0.55, P_(hit, {"right_arm": [-74, 0, 74]}))
    c.key(0.8, IDLE, E_IO)
    c.done()

    # combo: three blows -- a backhand (tick 10), a left palm thrust (tick 20), both hands brought down together (tick 30).
    c = clip("combo", 2.0)
    c.key(0, IDLE)
    c.key(0.3, wind, E_IO)
    c.key(0.5, hit, E_SNAP)
    thrust = P_(IDLE, {"left_arm": [-90, -6, 0], "left_forearm": [-2, 0, 0], "left_hand": [70, 0, 0], "left_fingers": [-20, 0, 0],
                       "right_arm": [10, 0, 20], "torso": [10, 24, 0], "head": [0, -16, 0]})
    c.key(0.8, P_(IDLE, {"left_arm": [-30, 0, -20], "left_forearm": [-100, 0, 0], "torso": [0, -20, 0], "right_arm": [-30, 0, 40]}), E_IO)
    c.key(1.0, thrust, E_SNAP)
    over = P_(IDLE, sym(arm=[-170, 0, -8], forearm=[-20, 0, 0], fingers=[60, 0, 0]), {"torso": [-12, 0, 0], "head": [-14, 0, 0]})
    down = P_(IDLE, sym(arm=[-60, 0, -6], forearm=[-10, 0, 0], fingers=[70, 0, 0]), {"torso": [24, 0, 0], "head": [6, 0, 0],
                                                                                     "hair_back_2": [-14, 0, 0]})
    c.key(1.3, over, E_OUT)
    c.key(1.5, down, E_SNAP)
    c.key(1.65, P_(down, {"torso": [20, 0, 0]}))
    c.key(2.0, IDLE, E_IO)
    tendril_wave(c, 12, 2, 2.0)
    c.done()

    # push: the right palm shoved out flat (tick 10), the weight behind it; the air bends.
    c = clip("push", 1.0)
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, {"right_arm": [-30, 0, 20], "right_forearm": [-100, 0, 0], "right_hand": [50, 0, 0], "torso": [-6, -14, 0],
                         "head": [0, 6, 0]}), E_IO)
    shove = P_(IDLE, {"right_arm": [-90, 0, 0], "right_forearm": [-2, 0, 0], "right_hand": [75, 0, 0], "right_fingers": [-25, 0, 0],
                      "left_arm": [16, 0, -12], "torso": [12, 16, 0], "head": [-2, -10, 0]})
    c.key(0.5, shove, E_SNAP)
    c.key(0.7, P_(shove, {"torso": [10, 14, 0]}))
    c.key(1.0, IDLE, E_IO)
    c.done()

    # =============================================================== phase IV: the siblings
    # wound: the hand reaches for her brother, open; the fist closes (tick 18) and wrenches back. The chains run from it.
    c = clip("wound", 2.0)
    reach = P_(IDLE, {"right_arm": [-86, -6, 0], "right_forearm": [-4, 0, 0], "right_hand": [0, 0, 0], "right_fingers": [-20, 0, 0],
                      "torso": [8, 0, 0], "head": [-2, 4, 0], "left_arm": [4, 0, -16], "chains": [88, 0, 0]})
    clench = P_(reach, FIST, {"right_arm": [-80, -6, 0], "right_forearm": [-20, 0, 0], "chains": [99, 0, 0], "torso": [-4, 0, 0],
                              "head": [-8, 4, 0]})
    c.key(0, IDLE)
    c.key(0.6, reach, E_OUT)
    c.key(0.82, P_(reach, {"right_fingers": [30, 0, 0]}), E_IO)
    c.key(0.9, clench, E_SNAP)
    c.key(1.4, P_(clench, {"right_arm": [-70, -6, 0], "right_forearm": [-40, 0, 0], "chains": [109, 0, 0], "torso": [-8, 0, 0]}), E_IO)
    c.key(2.0, IDLE, E_IO)
    chain_wave(c, 6, 3, 2.0, decay=lambda t: 0.3 + 0.7 * max(0.0, 1 - abs(t - 1.0) / 0.8))
    c.done()

    # wall: her left arm sweeps across in front of her and out (tick 14): a wall of smoke rises where it passed.
    c = clip("wall", 1.5)
    c.key(0, IDLE)
    c.key(0.45, P_(IDLE, {"left_arm": [-60, 0, 50], "left_forearm": [-40, 0, 0], "torso": [0, -24, 0],
                          "head": [0, 14, 0], "right_arm": [-10, 0, 14]}), E_IO)
    sweep = P_(IDLE, {"left_arm": [-74, 0, -80], "left_forearm": [-6, 0, 0], "left_hand": [0, 0, 20], "left_fingers": [-20, 0, 0],
                      "torso": [2, 26, 0], "head": [-4, -18, 0], "right_arm": [-10, 0, 10]})
    c.key(0.7, sweep, E_SNAP)
    c.key(1.0, P_(sweep, {"left_arm": [-80, 0, -84], "head": [-8, -16, 0]}))
    c.key(1.5, IDLE, E_IO)
    tendril_wave(c, 14, 2, 1.5)
    c.done()

    # scream: fists at her sides, the body arched, the head thrown back, mouth wide: rage at her brother. Trembling.
    c = clip("scream", 2.0)
    rage = P_(IDLE, WIDE, sym(arm=[18, 0, 22], forearm=[-8, 0, 0], fingers=[85, 0, 0], thumb=[50, 0, 0]),
              {"torso": [-16, 0, 0], "head": [-38, 0, 0], "hair_back_2": [16, 0, 0], "hair_back_3": [8, 0, 0]},
              tendril_pose(spread=1.0, lift=0.4))
    c.key(0, IDLE)
    c.key(0.35, P_(IDLE, {"torso": [16, 0, 0], "head": [16, 0, 0], **sym(arm=[-10, 0, 8], fingers=[60, 0, 0])}), E_IO)
    c.key(0.6, rage, E_SNAP)
    c.key(1.5, P_(rage, {"head": [-34, 0, 4]}))
    c.key(2.0, IDLE, E_IO)
    for b, amp, seed in (("torso", 2.0, 31), ("head", 3.0, 32), ("right_arm", 3.0, 33), ("left_arm", 3.0, 34)):
        c.layer(b, during(lambda t, amp=amp, seed=seed: jit(("sc", seed), t, amp, 2.0, (31, 37, 41)), 0.6, 1.6))
    c.done()

    # =============================================================== phase V: the void
    # crumble: the arms rise out to her sides (gathering), then come down hard and out (tick 24): the ground breaks.
    c = clip("crumble", 2.5)
    rise = P_(IDLE, sym(arm=[-20, 0, 100], forearm=[-10, 0, 0], hand=[0, -60, 0], fingers=[-20, 0, 0]),
              {"torso": [-10, 0, 0], "head": [-14, 0, 0]}, tendril_pose(spread=0.8, lift=0.6))
    smash = P_(IDLE, sym(arm=[-14, 0, 46], forearm=[-4, 0, 0], hand=[0, 0, 0], fingers=[-30, 0, 0]),
               {"torso": [22, 0, 0], "head": [10, 0, 0], "hair_back_2": [-12, 0, 0]}, tendril_pose(spread=1.0, curl=-0.5))
    c.key(0, IDLE)
    c.key(0.9, rise, E_IO)
    c.key(1.05, P_(rise, {"torso": [-12, 0, 0]}))
    c.key(1.2, smash, E_SNAP)
    c.key(1.7, P_(smash, {"torso": [18, 0, 0]}))
    c.key(2.5, IDLE, E_IO)
    c.done()

    # pull: the right hand raised over her, open, closes (tick 20): everything is drawn up to the black sun.
    c = clip("pull", 2.0)
    raised = P_(IDLE, {"right_arm": [-168, 0, 10], "right_forearm": [-10, 0, 0], "right_hand": [0, -40, 0], "right_fingers": [-30, 0, 0],
                       "head": [-24, 0, 0], "torso": [-6, 0, 0], "left_arm": [-10, 0, -20]})
    c.key(0, IDLE)
    c.key(0.7, raised, E_IO)
    c.key(0.9, P_(raised, {"right_fingers": [-40, 0, 0]}))
    c.key(1.0, P_(raised, FIST, {"right_arm": [-160, 0, 10]}), E_SNAP)
    c.key(1.5, P_(raised, FIST, {"right_arm": [-140, 0, 10], "right_forearm": [-40, 0, 0], "head": [-20, 0, 0]}), E_IO)
    c.key(2.0, IDLE, E_IO)
    c.done()

    # starfall: she points up into the sky, then sweeps the hand down to point at you (tick 16): the stars fall there.
    c = clip("starfall", 1.5)
    up_ = P_(IDLE, {"right_arm": [-150, 0, -6], "right_forearm": [0, 0, 0], "right_fingers": [-10, 0, 0], "head": [-24, 0, 0],
                    "torso": [-4, 0, 0]})
    point = P_(IDLE, {"right_arm": [-88, 0, 0], "right_forearm": [0, 0, 0], "right_fingers": [-10, 0, 0], "head": [0, 0, 0],
                      "torso": [6, 0, 0], "left_arm": [6, 0, -10]})
    c.key(0, IDLE)
    c.key(0.5, up_, E_IO)
    c.key(0.65, P_(up_, {"right_arm": [-156, 0, -6]}))
    c.key(0.8, point, E_SNAP)
    c.key(1.1, point)
    c.key(1.5, IDLE, E_IO)
    c.done()

    # ascend: a breath, then she rises into the air, arms opening, and settles into the float.
    c = clip("ascend", 2.0)
    c.key(0, IDLE)
    c.key(0.4, P_(IDLE, {"@root": [0, -0.4, 0], "torso": [6, 0, 0], "head": [8, 0, 0], **sym(arm=[-4, 0, 10])}), E_IO)
    c.key(1.4, P_(FLOAT, {"@root": [0, 7.0, 0], "head": [-16, 0, 0], **sym(arm=[-10, 0, 40], forearm=[-8, 0, 0]),
                          "dress_front": [4, 0, 0], "dress_tail": [-4, 0, 0], "dress_train": [-6, 0, 0],
                          "dress_right": [0, 0, 12], "dress_left": [0, 0, -12]}), E_OUT)
    c.key(2.0, FLOAT, E_IO)
    tendril_wave(c, 12, 1, 2.0)
    wisp_flutter(c, 14, 2, 2.0)
    c.done()

    # =============================================================== phase VI: the dying sun
    # countdown: her hands come together before her breast, palms pressed, head bowed; she holds it, trembling; lets go.
    c = clip("countdown", 3.0)
    pray = P_(IDLE, {"right_arm": [-40, 0, -22], "left_arm": [-40, 0, 22], "right_forearm": [-68, 0, -24], "left_forearm": [-68, 0, 24],
                     "right_hand": [0, 0, -26], "left_hand": [0, 0, 26], "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0],
                     "head": [18, 0, 0], "torso": [4, 0, 0]}, tendril_pose(spread=-0.6, curl=0.6))
    c.key(0, IDLE)
    c.key(0.7, pray, E_IO)
    c.key(2.4, P_(pray, {"head": [22, 0, 0]}))
    c.key(3.0, IDLE, E_IO)
    c.layer("torso", during(lambda t: jit("cd", t, 1.2, 3.0, (23, 29, 31)), 0.8, 2.4))
    c.done()

    # enrage: the corona flares (tick 20): arms flung out and down, head back, the tendrils straight out.
    c = clip("enrage", 2.0)
    flare = P_(IDLE, WIDE, sym(arm=[-6, 0, 64], forearm=[-4, 0, 0], fingers=[-34, 0, 0]),
               {"torso": [-14, 0, 0], "head": [-30, 0, 0], "%corona": [1.6, 1.6, 1.6], "corona_outer": [0, 0, 30],
                "corona_inner": [0, 0, -20]}, tendril_pose(spread=1.0, lift=0.6))
    c.key(0, IDLE)
    c.key(0.75, P_(IDLE, {"torso": [16, 0, 0], "head": [14, 0, 0], **sym(arm=[-30, 0, -16], forearm=[-70, 0, 0], fingers=[70, 0, 0]),
                          "%corona": [0.85, 0.85, 0.85]}, tendril_pose(spread=-0.6, curl=0.8)), E_IO)
    c.key(1.0, flare, E_SNAP)
    c.key(1.5, P_(flare, {"%corona": [1.3, 1.3, 1.3], "corona_outer": [0, 0, 45], "corona_inner": [0, 0, -30]}))
    c.key(2.0, P_(IDLE, {"corona_outer": [0, 0, 60], "corona_inner": [0, 0, -40]}), E_IO)
    c.done()

    # =============================================================== transitions
    # become_girl: out of the mass's shatter: a knot of smoke on the ground that rises, unfolds into the girl and looks up.
    c = clip("become_girl", 3.0)
    knot = P_(IDLE, {"@root": [0, -3.0, 0], "%body": [0.05, 0.05, 0.05], "torso": [70, 0, 0], "head": [40, 0, 0],
                     **sym(arm=[-80, 0, -30], forearm=[-90, 0, 0]),
                     "right_leg": [-80, 0, 0], "left_leg": [-80, 0, 0], "right_shin": [140, 0, 0], "left_shin": [140, 0, 0],
                     "dress_front": [-70, 0, 0], "dress_tail": [50, 0, 0]})
    curled = P_(knot, {"%body": [0.9, 0.9, 0.9], "@root": [0, -4.0, 0]})
    c.key(0, knot)
    c.key(0.9, curled, E_OUT)
    c.key(1.6, P_(IDLE, {"@root": [0, -1.6, 0], "torso": [30, 0, 0], "head": [26, 0, 0], **sym(arm=[-24, 0, -8], forearm=[-40, 0, 0]),
                         "right_leg": [-26, 0, 0], "left_leg": [-26, 0, 0], "right_shin": [40, 0, 0], "left_shin": [40, 0, 0],
                         "dress_front": [-18, 0, 0]}), E_IO)
    c.key(2.3, P_(CHILD, {"head": [-8, 0, 14]}), E_IO)
    c.key(3.0, P_(IDLE, {"head": [0, 0, 8]}), E_IO)
    c.done()

    # become_woman: the girl turns, twice, in a column of smoke, and stands as the woman, arms coming down from the turn.
    c = clip("become_woman", 4.0)
    c.key(0, IDLE)
    c.key(0.6, P_(IDLE, {"head": [10, 0, 0], **sym(arm=[-10, 0, 30])}), E_IO)
    c.key(2.4, P_(IDLE, {"body": [0, 540, 0], "@root": [0, 2.0, 0], "head": [-20, 0, 0], **sym(arm=[-20, 0, 90], forearm=[-10, 0, 0]),
                         "dress_front": [-30, 0, 0], "dress_right": [0, 0, 30], "dress_left": [0, 0, -30], "dress_tail": [30, 0, 0],
                         "dress_train": [20, 0, 0], "hair_back_2": [30, 0, 0], "hair_back_3": [20, 0, 0]}), E_IN)
    c.key(3.3, P_(IDLE, {"body": [0, 720, 0], "@root": [0, 0.6, 0], "head": [-6, 0, 0], **sym(arm=[-6, 0, 24])}), E_OUT)
    c.key(4.0, P_(IDLE, {"body": [0, 720, 0]}), E_IO)
    tendril_wave(c, 14, 2, 4.0)
    c.done()

    # become_void: she spreads her arms slowly, palms up, head back, lifting from the ground, as her skin turns to night.
    c = clip("become_void", 5.0)
    spread = P_(IDLE, sym(arm=[-24, 0, 84], forearm=[-10, 0, 0], hand=[0, -70, 0], fingers=[-20, 0, 0]),
                {"@root": [0, 2.0, 0], "torso": [-14, 0, 0], "head": [-34, 0, 0], "right_leg": [0, 0, 4], "left_leg": [0, 0, -4],
                 "right_shin": [14, 0, 0], "left_shin": [14, 0, 0], "dress_front": [-4, 0, 0], "dress_right": [0, 0, 10],
                 "dress_left": [0, 0, -10], "hair_back_2": [10, 0, 0], "hair_back_3": [8, 0, 0]}, tendril_pose(spread=1.0, lift=0.8))
    c.key(0, IDLE)
    c.key(0.8, P_(IDLE, {"head": [16, 0, 0], "torso": [8, 0, 0], **sym(arm=[-20, 0, -10], forearm=[-50, 0, 0])}), E_IO)
    c.key(2.8, spread, E_IO)
    c.key(4.0, P_(spread, {"head": [-30, 0, 0], "@root": [0, 2.4, 0]}))
    c.key(5.0, IDLE, E_IO)
    hair_sway(c, 4.0, 2, 5.0)
    c.done()

    # become_sun: spent, she sways and sinks to the ground: down onto her knees, one hand on the earth, head hanging (held).
    c = clip("become_sun", 4.0, hold=True)
    spent = P_(KNEEL, {"torso": [34, 0, 0], "head": [34, 0, 0], "right_arm": [-40, 0, 6], "right_forearm": [-4, 0, 0],
                       "right_hand": [60, 0, 0], "right_fingers": [-40, 0, 0], "left_arm": [-20, 0, -6], "left_forearm": [-50, 0, 0],
                       "hair_right_2": [-16, 0, 0], "hair_left_2": [-16, 0, 0]}, tendril_pose(spread=-0.8, curl=1.0))
    c.key(0, IDLE)
    c.key(0.8, P_(IDLE, {"body": [0, 0, 6], "head": [14, 0, -10], "torso": [10, 0, -4]}), E_IO)
    c.key(1.4, P_(IDLE, {"body": [0, 0, -5], "head": [18, 0, 10], "torso": [12, 0, 4]}), E_IO)
    c.key(2.4, P_(KNEEL, {"@root": [0, -2.0, 0], "right_shin": [50, 0, 0], "left_shin": [50, 0, 0], "right_leg": [-30, 0, 3],
                          "left_leg": [-30, 0, -3], "torso": [24, 0, 0], "head": [26, 0, 0], "dress_front": [-30, 0, 0],
                          "dress_tail": [30, 0, 0], "dress_right": [0, 0, 26], "dress_left": [0, 0, -26]}), E_IN)
    c.key(3.2, spent, E_IN)
    c.key(4.0, spent)
    c.done()

    # =============================================================== the end
    # kneel: she goes down onto her knees, the skirt pooling (held).
    c = clip("kneel", 1.5, hold=True)
    c.key(0, IDLE)
    c.key(0.6, P_(IDLE, {"@root": [0, -2.0, 0], "right_leg": [-34, 0, 3], "left_leg": [-34, 0, -3], "right_shin": [56, 0, 0],
                         "left_shin": [56, 0, 0], "torso": [12, 0, 0], "head": [10, 0, 0], "dress_front": [-30, 0, 0],
                         "dress_tail": [26, 0, 0], "dress_right": [0, 0, 20], "dress_left": [0, 0, -20]}), E_IO)
    c.key(1.1, P_(KNEEL, {"@root": [0, -5.9, 0], "torso": [12, 0, 0]}), E_IN)
    c.key(1.5, KNEEL, E_IO)
    c.done()

    # embrace: she steps to him, opens her arms, and holds him; her cheek comes to rest on his shoulder.
    c = clip("embrace", 4.0)
    opened = P_(IDLE, sym(arm=[-60, 0, 46], forearm=[-20, 0, 0], hand=[0, -40, 0], fingers=[-10, 0, 0]),
                {"head": [-4, 0, 0], "torso": [2, 0, 0]})
    c.key(0, IDLE)
    c.key(1.0, opened, E_IO)
    c.key(1.4, P_(opened, {"torso": [6, 0, 0], "head": [4, 0, 4]}), E_IO)
    c.key(2.0, P_(EMBRACE, {"right_forearm": [0, 0, -60], "left_forearm": [0, 0, 54]}), E_IO)
    c.key(2.5, EMBRACE, E_IO)
    c.key(4.0, EMBRACE)
    hair_sway(c, 1.5, 1, 4.0)
    c.done()

    # depart: still holding him, she lifts her face; they rise together into the light and fade (held; hidden at its end).
    c = clip("depart", 6.0, hold=True)
    up = P_(EMBRACE, {"head": [-14, -6, 8], "right_forearm": [0, 0, -60], "left_forearm": [0, 0, 54],
                      "right_leg": [0, 0, 2], "left_leg": [0, 0, -2], "right_shin": [16, 0, 0], "left_shin": [20, 0, 0],
                      "dress_front": [-6, 0, 0], "dress_tail": [8, 0, 0], "dress_train": [14, 0, 0]})
    c.key(0, EMBRACE)
    c.key(1.5, P_(up, {"@root": [0, 1.0, 0]}), E_IO)
    c.key(4.5, P_(up, {"@root": [0, 14.0, 0], "%body": [0.9, 0.9, 0.9]}), E_IN)
    c.key(6.0, P_(up, {"@root": [0, 24.0, 0], "%body": [0.0, 0.0, 0.0]}), E_IN)
    hair_sway(c, 4.0, 2, 6.0)
    c.done()
    return f


# =====================================================================================================
# Output
# =====================================================================================================

def generate():
    geo_written = False
    for age in AGES:
        m = rig(age)
        t, g = m.build(gutter=1, seed=41000)
        if not geo_written:
            m.rig.write(GEO + NAME + ".geo.json")
            geo_written = True
        save(t, "entity", "amara_" + age)
        save(g, "entity", "amara_" + age + "_glowmask")
    a = anims()
    write_compact(a, ANIM + NAME + ".animation.json")
    with open(ANIM + NAME + ".names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")


if __name__ == "__main__":
    generate()
