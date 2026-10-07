"""Chuck Shurley, the Author, as a man: GeckoLib geometry, one texture atlas and every clip.

One rig, three outfits. The shared skeleton carries only what never changes (his head, neck, hands and the
whiskey glass); each outfit is a top-level group the renderer shows alone (ChuckBones.OUTFIT_*):

  outfit_robe     -- at home: a worn burgundy bathrobe (shawl collar, belt, patch pockets) over striped
                     pyjamas, and suede slippers;
  outfit_flannel  -- chapter 1: an open plaid flannel shirt over a grey tee, sleeves rolled, jeans, boots;
  outfit_suit     -- chapter 2: a cream linen jacket (notch lapels, flap pockets) over an open white shirt,
                     cream trousers, loafers.

Every group holds its own copy of the limb chain (robe_body, robe_right_arm, robe_right_forearm, ...) with the
same pivots as the shared one, and every clip keys all four chains identically (`Clip`), so whichever outfit is
shown moves with the body.

Also home to the helpers the other Author modules share (FCube, Model, composite previews).
"""

import math
import random
import zlib

from animkit import AnimFile
from common import ASSETS, save
from geomodel import Cube, Rig
from lucifer_art import crack_field
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
CLEAR = (0, 0, 0, 0)
SHADE = {"up": 1.1, "north": 1.0, "east": 0.88, "west": 0.88, "south": 0.8, "down": 0.66}


# =====================================================================================================
# Shared helpers (chuck_divine_art, author_hand_art and typewriter_key_art use them too)
# =====================================================================================================

def h01(*parts):
    """A stable pseudo-random number in [0, 1) from any hashable parts."""
    return random.Random(zlib.crc32(repr(parts).encode())).random()


class FCube(Cube):
    """A cube whose size may be fractional (Bedrock allows it); each face still takes whole texels
    (the size is snapped to a multiple of 1/density)."""

    def __init__(self, origin, size, **kw):
        super().__init__(origin, (0, 0, 0), **kw)
        # Snapped to whole texels, so painted texels and geometry agree.
        self.size = tuple(round(float(v) * self.density) / self.density for v in size)

    def _tx(self):
        return tuple(int(round(v * self.density)) for v in self.size)

    def _face_dims(self):
        w, h, d = self._tx()
        return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}

    @property
    def unwrap_size(self):
        if self.faces and len(self.faces) == 1:
            return self._face_dims()[self.faces[0]]
        w, h, d = self._tx()
        return 2 * d + 2 * w, d + h

    def face_list(self):
        dims = self._face_dims()
        faces = self.faces or ("north", "south", "east", "west", "up", "down")
        return [f for f in faces if dims[f][0] > 0 and dims[f][1] > 0]

    def layout(self):
        u, v = self.box
        if self.faces and len(self.faces) == 1:
            fw, fh = self._face_dims()[self.faces[0]]
            self.rects = {self.faces[0]: (u, v, fw, fh)} if fw > 0 and fh > 0 else {}
            return
        w, h, d = self._tx()
        rects = {
            "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
            "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
        }
        self.rects = {f: r for f, r in rects.items() if f in self.face_list()}


class Model:
    """A rig plus, per cube, a material (albedo) and an optional glow (the glowmask).

    material(face, x, y, w, h) -> colour or None (x right, y down, as seen from outside; texels).
    glow: None, a float (glow = albedo x f) or glow(face, x, y, w, h, albedo) -> colour or None.
    """

    def __init__(self, name, w, h):
        self.rig = Rig(name, w, h)
        self.jobs = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, inflate=0.0, faces=None, rotation=None, pivot=None,
             density=1, tag=None, shading=True):
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
                        col = mat(face, x, y, w, h)
                        if col is None:
                            continue
                        t.set(u + x, v + y, shade(col, SHADE[face]) if shading and col[3] else col)
                        if glow is None or not col[3]:
                            continue
                        if callable(glow):
                            gl = glow(face, x, y, w, h, col)
                        else:
                            gl = shade(col, glow) if glow > 0 else None
                        if gl is not None:
                            g.set(u + x, v + y, gl)
        return t, g

    def bone_count(self):
        return len(self.rig.bones)


def composite(t, g, base=0.55):
    """What the light looks like in game (albedo lit dimly + the additive glow), for previews only."""
    out = Tex(t.w, t.h, 0)
    for y in range(t.h):
        for x in range(t.w):
            a = t.rows[y][x]
            if not a[3]:
                continue
            b = g.rows[y][x]
            out.rows[y][x] = tuple(min(255, int(a[i] * base + (b[i] if b[3] else 0))) for i in range(3)) + (a[3],)
    return out


def cracks_layer(model, seed, color, density=3):
    """A layer of glowing cracks over every cube (drawn by the renderer with ChuckLook.crack() as alpha)."""
    k = Tex(model.rig.tex_w, model.rig.tex_h, seed)
    for i, (c, *_rest) in enumerate(model.jobs):
        for face, (u, v, w, h) in c.rects.items():
            for (x, y) in crack_field(seed + i * 13 + len(face), w, h, max(1, density * w * h // 60)):
                k.set(u + x, v + y, color)
    return k


# --- materials --------------------------------------------------------------------------------------

def tone(ramp, seed, base=3.0, amp=1.2, scale=6.0):
    """Mottled fill from a ramp."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face) * 5, y * 2, seed, 64, 64, 2, scale)
        return ramp[base + (n - 0.5) * amp * 2]
    return m


def solid(c):
    return lambda face, x, y, w, h: c


def none_on(faces, mat):
    return lambda face, x, y, w, h: None if face in faces else mat(face, x, y, w, h)


# =====================================================================================================
# The man
# =====================================================================================================

SKIN = Ramp("#5c3b2c", "#86594a", "#a9775f", "#c28f74", "#d4a68a", "#e3bda2")
HAIR = Ramp("#0f0a07", "#1a120c", "#271b12", "#352619", "#453222", "#58412d")
BEARD = Ramp("#1c130d", "#2b1f15", "#3b2b1e", "#4c3828", "#5f4733", "#745940")
GREY = hexc("#8d8479")
EYE_WHITE, IRIS, PUPIL = hexc("#e6ddd2"), hexc("#5a3a22"), hexc("#1c120b")
LIPS = hexc("#9a5f52")
# Bathrobe (worn burgundy terry), pyjamas (pale blue stripes), slippers.
ROBE = Ramp("#26100f", "#3a1918", "#4f2421", "#64302b", "#7a3f37", "#915145")
PJ = Ramp("#5b6878", "#728093", "#8b99ab", "#a6b2c2", "#bfc9d6", "#d8dfe8")
PJ_STRIPE = hexc("#3d4a68")
SLIPPER = Ramp("#2b1e14", "#3d2b1d", "#523a27", "#674b33", "#7d5d40", "#94714f")
# Flannel (muted tartan), tee, jeans, boots.
FL_BASE = Ramp("#141c26", "#1d2836", "#283749", "#33475c", "#405a72", "#4f6d88")
FL_GREEN, FL_RED, FL_YEL = hexc("#2f4a35"), hexc("#7a2a24"), hexc("#b49a52")
TEE = Ramp("#3b3c3e", "#4c4d50", "#5e6064", "#717378", "#85878c", "#9a9ca1")
DENIM = Ramp("#141c2c", "#1d2940", "#283855", "#344869", "#44597b", "#5a6f8e")
BOOT = Ramp("#1f140c", "#2e1e12", "#40291a", "#553724", "#6b4730", "#83593e")
# The suit: cream linen, white shirt, brown loafers.
LINEN = Ramp("#7d7258", "#9b8f72", "#b6aa8b", "#cbc0a2", "#ddd3b8", "#ece5cf")
SHIRT = Ramp("#8d939b", "#a9afb7", "#c2c8cf", "#d6dbe1", "#e6eaee", "#f4f6f8")
LOAFER = Ramp("#1e110a", "#2d1a10", "#3f2516", "#53311d", "#683e25", "#7e4c2e")
GLASS = Ramp("#5d6a6c", "#7d8b8d", "#a2b0b1", "#c3cfd0", "#dde6e6", "#f3f8f8")
WHISKEY = Ramp("#4a1c05", "#6b2c08", "#8f420d", "#b25d16", "#cf7c24", "#e69c3d")

OUTFITS = ("robe", "flannel", "suit")
LIMBS = ("body", "right_arm", "right_forearm", "left_arm", "left_forearm", "right_leg", "right_shin", "left_leg",
         "left_shin")
# Shared pivots (Bedrock px; +X is his LEFT).
PIV = {
    "body": (0, 12, 0),
    "right_arm": (-5.5, 22, 0), "right_forearm": (-5.5, 17, 0),
    "left_arm": (5.5, 22, 0), "left_forearm": (5.5, 17, 0),
    "right_leg": (-2, 12, 0), "right_shin": (-2, 6, 0),
    "left_leg": (2, 12, 0), "left_shin": (2, 6, 0),
}
PARENT = {"body": None, "right_arm": "body", "right_forearm": "right_arm", "left_arm": "body",
          "left_forearm": "left_arm", "right_leg": None, "right_shin": "right_leg", "left_leg": None,
          "left_shin": "left_leg"}
FINGERS = ("index", "middle", "ring", "thumb")


def cloth(ramp, seed, base=2.8, amp=0.9, weave=0.25, hem=True):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 7, y, seed, 64, 64, 2, 8.0)
        v = base + (n - 0.5) * amp * 2 + (weave if (x + y) % 2 == 0 else 0)
        if hem and face in ("north", "south", "east", "west") and y == h - 1:
            v -= 0.9
        return ramp[v]
    return m


def terry(ramp, seed):
    """Towelling: a nubbly loop texture."""
    def m(face, x, y, w, h):
        n = fbm(x * 3 + len(face), y * 3, seed, 64, 64, 2, 16.0)
        v = 2.9 + (n - 0.5) * 2.2 + (0.35 if (x * 7 + y * 3) % 5 == 0 else 0) - (0.4 if (x + 2 * y) % 7 == 0 else 0)
        return ramp[v]
    return m


def stripes(ramp, seed, stripe, period=5):
    base = cloth(ramp, seed, 3.3, 0.5, 0.15)

    def m(face, x, y, w, h):
        if face in ("up", "down"):
            return base(face, x, y, w, h)
        c = base(face, x, y, w, h)
        return mix(c, stripe, 0.65) if x % period == 0 else c
    return m


def tartan(seed):
    """A muted blue / green tartan with red and ochre overchecks (2 texels per px)."""
    rng = random.Random(seed)

    def band(i):
        i %= 16
        if i in (0, 1, 2, 3):
            return "g"
        if i == 8:
            return "r"
        if i == 12:
            return "y"
        return "b"

    def m(face, x, y, w, h):
        a, b = band(x), band(y)
        n = fbm(x, y + len(face) * 9, seed, 64, 64, 2, 10.0)
        base = FL_BASE[2.6 + (n - 0.5) * 1.4]
        c = base
        if a == "g" and b == "g":
            c = shade(FL_GREEN, 0.75)
        elif a == "g" or b == "g":
            c = mix(base, FL_GREEN, 0.6)
        if a == "r" or b == "r":
            c = mix(c, FL_RED, 0.7 if (x + y) % 2 == 0 else 0.45)
        if a == "y" or b == "y":
            c = mix(c, FL_YEL, 0.45 if (x + y) % 2 else 0.25)
        if (x + y) % 2 == 0:
            c = shade(c, 1.07)
        if rng.random() < 0.01:
            c = shade(c, 0.85)
        return c
    return m


def denim(seed):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 3, y, seed, 64, 64, 3, 8.0)
        v = 2.6 + (n - 0.5) * 1.6
        if (x - y) % 3 == 0:
            v += 0.5  # twill
        c = DENIM[v]
        # Faded thighs and knees, a seam down the outer side.
        if face == "north" and h > 8 and 0.25 < y / h < 0.6 and 2 <= x <= w - 3:
            c = mix(c, DENIM[5], 0.25)
        if face in ("east", "west") and x == w // 2:
            c = shade(c, 1.18) if y % 2 else shade(c, 0.85)
        return c
    return m


def linen(seed, ramp=LINEN):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 5, y, seed, 64, 64, 2, 7.0)
        slub = fbm(x * 4, y, seed + 3, 64, 64, 1, 16.0)
        v = 3.2 + (n - 0.5) * 0.9 + (0.3 if slub > 0.72 else 0) - (0.2 if (y * 3 + x) % 7 == 0 else 0)
        return ramp[v]
    return m


def skin(seed, stubble=False):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 3, y, seed, 64, 64, 2, 7.0)
        c = SKIN[3.1 + (n - 0.5) * 0.8]
        if stubble and (x * 5 + y * 3) % 7 == 0:
            c = shade(c, 0.9)
        return c
    return m


def scruff(seed):
    """A short beard: dark hair with skin showing through here and there."""
    base = hair_mat(seed, BEARD, 0.06)

    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        r = h01("scruff", seed, face, x, y)
        return mix(c, SKIN[2], 0.35) if r < 0.22 else c
    return m


def hair_mat(seed, ramp=HAIR, grey=0.0):
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, 12.0)
        strand = math.sin(x * 1.7 + y * 0.6 + n * 6)
        c = ramp[2.3 + (n - 0.5) * 1.2 + strand * 0.55]
        if grey and h01(seed, face, x, y) < grey:
            c = mix(c, GREY, 0.6)
        return c
    return m


class Man(Model):
    def __init__(self):
        super().__init__("chuck", 512, 512)

    def limb(self, prefix, name):
        return self.rig.get(f"{prefix}_{name}" if prefix else name)


def rig():
    m = Man()
    m.bone("root", (0, 0, 0))
    for name in LIMBS:
        m.bone(name, PIV[name], PARENT[name] or "root")
    head = m.bone("head", (0, 24, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_hand", (5.5 * s, 12, 0), f"{side}_forearm")
        for f in FINGERS:
            if f == "thumb":
                m.bone(f"{side}_thumb", (5.0 * s, 11, -1.4), f"{side}_hand", rotation=(-10, 0, -12 * s))
            else:
                z = {"index": -1.0, "middle": 0.0, "ring": 1.0}[f]
                m.bone(f"{side}_{f}", (5.5 * s, 9.5, z), f"{side}_hand", rotation=(-12, 0, 0))
    m.bone("glass", (-5.5, 9.2, -1.6), "right_hand")
    for o in OUTFITS:
        m.bone(f"outfit_{o}", (0, 0, 0), "root")
        for name in LIMBS:
            par = PARENT[name]
            m.bone(f"{o}_{name}", PIV[name], f"{o}_{par}" if par else f"outfit_{o}")

    build_head(m, head)
    build_hands(m)
    build_glass(m)
    build_robe(m)
    build_flannel(m)
    build_suit(m)
    return m


# --- head ----------------------------------------------------------------------------------------

FACE = [  # north face of the head, 16x16 (2 texels per px); x=0 is his right
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "shhhhhhhhhhsshhs",
    "ssssssdSSdssssss",
    "sbbbbbsSSsbbbbbs",
    "ssllllsSSsllllss",
    "sswiiwsSSswiiwss",
    "sssuussSSssuusss",
    "sdsssssSSsssssds",
    "sBssssdSSdssssBs",
    "BBsssdddddssssBB",
    "BBBBmmmmmmmmBBBB",
    "BBBBBsMMMMsBBBBB",
    "BBBBBBDDDDBBBBBB",
    "BBBBBBBBBBBBBBBB",
]


def build_head(m, head):
    sk, hr, bd = skin(11, True), hair_mat(12), scruff(13)

    def face_px(ch, face, x, y, w, h):
        if ch == "h":
            return hr(face, x, y, w, h)
        if ch in "B":
            return bd(face, x, y, w, h)
        if ch == "D":
            return shade(bd(face, x, y, w, h), 0.8)
        if ch == "m":
            return bd(face, x, y, w, h)
        if ch == "M":
            return LIPS
        if ch == "b":
            return HAIR[1]
        if ch == "l":
            return mix(SKIN[1], SKIN[2], 0.5)
        if ch == "w":
            return EYE_WHITE
        if ch == "i":
            return IRIS
        if ch == "p":
            return PUPIL
        if ch == "u":
            return mix(SKIN[2], hexc("#6e5060"), 0.35)
        if ch == "d":
            return SKIN[2]
        if ch == "S":
            return SKIN[4]
        return sk(face, x, y, w, h)

    def head_mat(face, x, y, w, h):
        if face == "north":
            return face_px(FACE[y][x], face, x, y, w, h)
        if face == "up":
            return hr(face, x, y, w, h)
        if face == "down":
            return bd(face, x, y, w, h) if y < 10 else sk(face, x, y, w, h)
        if face == "south":
            return hr(face, x, y, w, h) if y < 12 else sk(face, x, y, w, h)
        # Sides: x runs front->back on west (his right) and back->front on east.
        fx = x if face == "west" else w - 1 - x  # 0 at the front
        if y < 4 or fx > 10 or (y < 7 and fx > 7):
            return hr(face, x, y, w, h)
        if y >= 9 and fx < 9:
            return bd(face, x, y, w, h)
        if fx >= 7 and y < 11:
            return hr(face, x, y, w, h)  # sideburn
        return sk(face, x, y, w, h)

    m.cube(head, (-4, 24, -4), (8, 8, 8), head_mat, density=2, tag="head")
    m.cube("body", (-1.5, 23, -1.5), (3, 1.5, 3), sk, density=2, tag="neck")

    # Hair: a messy crown shell, a fringe swept to his right, and a few tufts out of place.
    def crown(face, x, y, w, h):
        if face == "down":
            return None
        if face == "north":
            return hr(face, x, y, w, h) if y < 2 or (y == 2 and x in (1, 2, 5, 9, 10)) else None
        if face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x
            return hr(face, x, y, w, h) if y < 3 or fx > 4 else None
        return hr(face, x, y, w, h)
    m.cube(head, (-4, 29, -4), (8, 3, 8), crown, inflate=0.45, density=2, tag="hair")
    m.cube(head, (-4, 25.5, 1), (8, 4.5, 3), none_on(("north", "up", "down"), hr), inflate=0.35, density=2, tag="hair_back")
    rng = random.Random(5)
    for k, (x, z, ry, rz) in enumerate(((-2.5, -3, 20, 14), (0.5, -3.2, -15, -8), (2.5, -0.5, 35, -16), (-2.5, 1.5, -40, 18))):
        L = 2.5 if k < 2 else 2
        m.cube(head, (x - 0.5, 31.7, z - L / 2), (1, 0.5, L), hair_mat(30 + k), inflate=0.1, density=2, tag="tuft",
               rotation=(rng.uniform(-10, 10), ry, rz), pivot=(x, 31.8, z))

    # Beard: a scruffy shell over the jaw, the moustache and a chin that juts a little.
    def beard(face, x, y, w, h):
        if face == "up":
            return None
        if face == "south":
            return None
        n = h01("beard", face, x, y)
        if face == "north":
            # Short and trimmed: along the jaw and over the chin, open at the mouth; ragged upper edge.
            if y < 2:
                return bd(face, x, y, w, h) if (x < 2 or x > 13) and n > 0.3 else None
            if y < 4 and 2 <= x <= 13:
                return None
            if y == 4 and 5 <= x <= 10:
                return None
        if face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x
            if fx > 12:
                return None
            if y < 2 and fx > 9:
                return None
        c = bd(face, x, y, w, h)
        if n < 0.08:
            return None
        return shade(c, 0.9 + 0.2 * n)
    m.cube(head, (-4, 24, -4), (8, 3.5, 8), beard, inflate=0.16, density=2, tag="beard")
    m.cube(head, (-1.5, 23.6, -4.6), (3, 1.5, 1), bd, density=2, tag="chin")
    m.cube(head, (-1.5, 26.5, -4.25), (3, 0.5, 0.5), lambda f, x, y, w, h: shade(bd(f, x, y, w, h), 1.15), density=2, tag="moustache")
    # Nose, ears.
    m.cube(head, (-0.5, 26.5, -4.75), (1, 2, 0.75), lambda f, x, y, w, h: SKIN[3] if f != "down" else SKIN[1],
           density=4, tag="nose")
    for s in (-1, 1):
        m.cube(head, (4.0 if s > 0 else -4.5, 26.5, -0.5),
               (0.5, 2, 1.5), lambda f, x, y, w, h: SKIN[2] if (f in ("east", "west") and 1 <= x <= 3 and 1 <= y <= 5)
               else SKIN[3], density=2, tag="ear")


# --- hands ---------------------------------------------------------------------------------------

def build_hands(m):
    sk = skin(21)

    def knuckled(face, x, y, w, h):
        c = sk(face, x, y, w, h)
        return shade(c, 0.88) if face in ("north", "south", "east", "west") and y == h - 1 else c

    for side, s in (("right", -1), ("left", 1)):
        hand = m.rig.get(f"{side}_hand")
        # Palm: thin in X (the palm faces his thigh), 3 px front to back.
        m.cube(hand, (5.5 * s - 0.75, 9.5, -1.5), (1.5, 2.5, 3), knuckled, density=2, tag="palm")
        for f in ("index", "middle", "ring"):
            z = {"index": -1.0, "middle": 0.0, "ring": 1.0}[f]
            b = m.rig.get(f"{side}_{f}")

            def finger(face, x, y, w, h, f=f):
                c = sk(face, x, y, w, h)
                if f == "ring" and face in ("east", "west") and x == w // 2:
                    c = shade(c, 0.8)  # ring and little finger, side by side
                if y >= h - 1:
                    c = mix(c, hexc("#e8c4b0"), 0.4)  # nails
                return c
            m.cube(b, (5.5 * s - 0.5, 7.5, z - 0.5), (1, 2, 1), finger, density=2, tag="finger")
        t = m.rig.get(f"{side}_thumb")
        m.cube(t, (5.0 * s - 0.5, 9.0, -2.0), (1, 2, 1), sk, density=2, tag="thumb")


def build_glass(m):
    g = m.rig.get("glass")

    def tumbler(face, x, y, w, h):
        if face == "up":
            d = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
            return GLASS[4] if d > (w / 2 - 1.5) else WHISKEY[3]
        if face == "down":
            return GLASS[2]
        if y < 3:
            return GLASS[5] if x in (0, w - 1) else mix(GLASS[3], hexc("#ffffff"), 0.2)
        c = WHISKEY[2.5 + 1.5 * (1 - y / h)]
        if x == 1:
            c = mix(c, hexc("#ffe8b0"), 0.5)  # a highlight down the glass
        if y == h - 1:
            c = GLASS[3]
        return c
    m.cube(g, (-6.5, 7.6, -2.6), (2, 2.5, 2), tumbler, density=4, tag="glass")
    m.cube(g, (-6.0, 9.4, -2.1), (1, 0.5, 1), solid(hexc("#e8f4f6")), density=2, tag="ice")


# --- outfits -------------------------------------------------------------------------------------

def arm_box(s, y0, h, inflate):
    return (5.5 * s - 1.5, y0, -1.5), (3, h, 3), inflate


def leg_box(s, y0, h):
    return (2 * s - 2 + (0.0 if s > 0 else 0.0), y0, -2), (4, h, 4)


def build_robe(m):
    o = "robe"
    body = m.limb(o, "body")
    rb, pj = terry(ROBE, 41), stripes(PJ, 42, PJ_STRIPE)
    # Pyjama top under the robe (seen in the V of the collar).
    m.cube(body, (-4, 12, -2), (8, 12, 4), pj, inflate=0.12, density=2, tag="pj_top")

    def robe_torso(face, x, y, w, h):
        if face == "down":
            return None
        if face == "up":
            return None if 4 <= x <= 11 and y < 5 else rb(face, x, y, w, h)
        if face == "north":
            # The wrap: a deep V from the neck to the belt, the left panel over the right.
            half = max(0, 8 - y * 0.62)
            if abs(x - 7.5) < half:
                return None
            c = rb(face, x, y, w, h)
            if abs(abs(x - 7.5) - half) < 1.2:
                c = shade(c, 0.75)
            return c
        return rb(face, x, y, w, h)
    m.cube(body, (-4, 12, -2), (8, 12, 4), robe_torso, inflate=0.55, density=2, tag="robe")
    # Shawl collar: a rolled band up each side of the V and round the back of the neck.
    for s in (-1, 1):
        m.cube(body, (2.3 * s - 0.75, 16.5, -2.9), (1.5, 8, 0.5), lambda f, x, y, w, h: shade(rb(f, x, y, w, h), 1.15),
               density=2, tag="shawl", rotation=(4, 0, 18 * s), pivot=(0, 16.5, -2.6))
    m.cube(body, (-3.5, 23.6, 0.6), (7, 1.5, 1.5), lambda f, x, y, w, h: shade(rb(f, x, y, w, h), 1.12), density=2,
           tag="collar_back")
    # Belt, knot and the two loose ends.
    m.cube(body, (-4, 16, -2), (8, 1.5, 4), lambda f, x, y, w, h: shade(rb(f, x, y, w, h), 1.25 if y else 1.4), inflate=0.85,
           density=2, tag="belt")
    m.cube(body, (0.4, 15.6, -3.3), (1.5, 2, 0.75), lambda f, x, y, w, h: shade(rb(f, x, y, w, h), 1.12), density=2,
           tag="knot")
    for k, (x, rz) in enumerate(((0.5, 10), (1.6, -6))):
        m.cube(body, (x, 12.0, -3.2), (1, 4, 0.5), rb, density=2, tag="belt_end", rotation=(6, 0, rz), pivot=(x + 0.5, 16, -3))
    # Patch pockets.
    for s in (-1, 1):
        def pocket(face, x, y, w, h):
            c = shade(rb(face, x, y, w, h), 0.95)
            return shade(c, 0.72) if y == 0 or x in (0, w - 1) else c
        m.cube(body, (2.4 * s - 1.25, 12.6, -2.8), (2.5, 2.5, 0.5), pocket, density=2, tag="pocket")
    # The robe's skirt: behind, from the waist to the calves; at the front it rides on the thighs.
    m.cube(body, (-4.5, 4.5, 1.2), (9, 7.5, 1.5), none_on(("up",), rb), density=2, tag="robe_back")
    for side, s in (("right", -1), ("left", 1)):
        leg = m.limb(o, f"{side}_leg")
        shin = m.limb(o, f"{side}_shin")
        m.cube(leg, (2 * s - 2, 6, -2), (4, 6, 4), pj, inflate=0.12, density=2, tag="pj_leg")

        def skirt(face, x, y, w, h, s=s):
            if face in ("up", "down"):
                return None
            if face == ("west" if s > 0 else "east"):
                return None  # between the legs
            c = rb(face, x, y, w, h)
            return shade(c, 0.8) if y == h - 1 else c
        m.cube(leg, (2 * s - 2, 4.5, -2), (4, 7.5, 4), skirt, inflate=0.7, density=2, tag="skirt")

        def pj_shin(face, x, y, w, h):
            c = pj(face, x, y, w, h)
            return shade(c, 0.85) if y >= h - 2 else c
        m.cube(shin, (2 * s - 2, 1.5, -2), (4, 4.5, 4), pj_shin, inflate=0.12, density=2, tag="pj_shin")
        m.cube(shin, (2 * s - 1.5, 1.0, -1.5), (3, 0.5, 3), skin(22), density=2, tag="ankle")

        def slipper(face, x, y, w, h):
            n = fbm(x * 3, y * 3, 50 + s, 64, 64, 2, 14.0)
            c = SLIPPER[2.8 + (n - 0.5) * 1.5]
            if face in ("north", "south", "east", "west") and y >= h - 1:
                return hexc("#c7b79c")  # the felt sole
            if face == "up" and y < h - 4:
                return SLIPPER[1]  # the opening
            return c
        m.cube(shin, (2 * s - 2.25, 0, -3), (4.5, 1.5, 5.5), slipper, density=2, tag="slipper")
        # Sleeves: loose, the forearm widening to a turned-back cuff.
        arm, fore = m.limb(o, f"{side}_arm"), m.limb(o, f"{side}_forearm")
        m.cube(arm, (5.5 * s - 1.5, 16.5, -1.5), (3, 7, 3), none_on(("down",), rb), inflate=0.55, density=2, tag="sleeve")
        m.cube(fore, (5.5 * s - 1.5, 12.5, -1.5), (3, 4.5, 3), none_on(("up", "down"), rb), inflate=0.75, density=2,
               tag="sleeve")
        m.cube(fore, (5.5 * s - 1.5, 12.0, -1.5), (3, 1.5, 3),
               lambda f, x, y, w, h: None if f in ("up",) else (shade(rb(f, x, y, w, h), 1.15) if f != "down" else ROBE[0]),
               inflate=1.0, density=2, tag="cuff")


def build_flannel(m):
    o = "flannel"
    body = m.limb(o, "body")
    pl, tee = tartan(61), cloth(TEE, 62, 2.9, 0.6, 0.1)

    def tee_m(face, x, y, w, h):
        if face == "north" and y < 2 and 5 <= x <= 10:
            return TEE[1] if y == 0 or x in (5, 10) else skin(23)(face, x, y, w, h)
        return tee(face, x, y, w, h)
    m.cube(body, (-4, 12, -2), (8, 12, 4), tee_m, inflate=0.12, density=2, tag="tee")

    def shirt(face, x, y, w, h):
        if face == "down":
            return None
        if face == "up":
            return None if 4 <= x <= 11 and y < 5 else pl(face, x, y, w, h)
        if face == "north":
            gap = 2.2 + max(0, 4 - y) * 0.6  # open, wider at the collar
            if abs(x - 7.5) < gap:
                return None
            c = pl(face, x, y, w, h)
            edge = abs(abs(x - 7.5) - gap)
            if edge < 1.0:
                c = shade(c, 0.85)
                if x < 8 and y % 5 == 3:
                    return hexc("#d9d2c0")  # buttons on the placket
            return c
        return pl(face, x, y, w, h)
    m.cube(body, (-4, 11, -2), (8, 13, 4), shirt, inflate=0.5, density=2, tag="flannel")
    # Collar points and the yoke at the back, chest pockets with their flaps.
    for s in (-1, 1):
        m.cube(body, (2.6 * s - 1, 22.6, -2.9), (2, 2, 0.5), lambda f, x, y, w, h: shade(pl(f, x, y, w, h), 1.08),
               density=2, tag="collar", rotation=(12, 0, -28 * s), pivot=(2.6 * s, 24, -2.6))

        def pocket(face, x, y, w, h):
            c = pl(face, x, y + 3, w, h)
            if y < 2:
                c = shade(c, 1.12 if y == 0 else 0.7)
            if y == 1 and x == w // 2:
                c = hexc("#d9d2c0")
            return c
        m.cube(body, (2.4 * s - 1.25, 18, -2.75), (2.5, 3, 0.5), pocket, density=2, tag="pocket")
    m.cube(body, (-4, 23, 0.5), (8, 1.5, 2), lambda f, x, y, w, h: shade(pl(f, x, y, w, h), 1.06), inflate=0.6,
           density=2, tag="collar_back")
    for side, s in (("right", -1), ("left", 1)):
        arm, fore = m.limb(o, f"{side}_arm"), m.limb(o, f"{side}_forearm")
        m.cube(arm, (5.5 * s - 1.5, 16.5, -1.5), (3, 7, 3), none_on(("down",), pl), inflate=0.45, density=2, tag="sleeve")
        # Rolled to below the elbow: a thick fold, then bare forearm.
        m.cube(fore, (5.5 * s - 1.5, 14.5, -1.5), (3, 2.5, 3), none_on(("up",), lambda f, x, y, w, h:
               shade(pl(f, x + 5, y * 2, w, h), 1.1 if y % 3 else 0.8)), inflate=0.75, density=2, tag="roll")

        def forearm(face, x, y, w, h):
            c = skin(24)(face, x, y, w, h)
            if face in ("east", "west", "south") and h01("hair", face, x, y) < 0.12:
                c = shade(c, 0.78)  # hairy forearms
            return c
        m.cube(fore, (5.5 * s - 1.25, 12, -1.25), (2.5, 3, 2.5), forearm, density=2, tag="forearm")
        leg, shin = m.limb(o, f"{side}_leg"), m.limb(o, f"{side}_shin")
        dn = denim(70 + s)
        m.cube(leg, (2 * s - 2, 6, -2), (4, 6, 4), dn, inflate=0.15, density=2, tag="jeans")

        def jean_shin(face, x, y, w, h, dn=dn):
            c = dn(face, x, y, w, h)
            return shade(c, 0.8 if y >= h - 1 else 1.0)
        m.cube(shin, (2 * s - 2, 1.5, -2), (4, 4.5, 4), jean_shin, inflate=0.15, density=2, tag="jeans")

        def boot(face, x, y, w, h):
            n = fbm(x * 2, y * 2, 80 + s, 64, 64, 2, 10.0)
            c = BOOT[2.6 + (n - 0.5) * 1.6]
            if y >= h - 1 and face != "up":
                return hexc("#1a120c")
            if face == "up" and 2 <= y <= 5 and x % 3 == 1:
                return hexc("#c9b48a")  # laces
            if face == "north" and y < 2:
                c = shade(c, 1.15)  # scuffed toe
            return c
        m.cube(shin, (2 * s - 2.2, 0, -2.9), (4.4, 2, 5.2), boot, density=2, tag="boot")
    # The untucked shirt tail hangs over the hips.
    m.cube(body, (-4, 10, -2), (8, 2, 4), lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 6 <= x <= 9)
           else pl(f, x, y + 4, w, h), inflate=0.62, density=2, tag="tail")


def build_suit(m):
    o = "suit"
    body = m.limb(o, "body")
    ln, sh = linen(91), cloth(SHIRT, 92, 3.2, 0.5, 0.1)

    def shirt(face, x, y, w, h):
        if face == "north":
            # Two buttons undone: a V of skin at the throat.
            if y < 5 and abs(x - 7.5) < 2.5 - y * 0.45:
                return skin(25)(face, x, y, w, h)
            if x == 8 and y % 4 == 2 and y > 5:
                return hexc("#f8f8f2")
        return sh(face, x, y, w, h)
    m.cube(body, (-4, 12, -2), (8, 12, 4), shirt, inflate=0.12, density=2, tag="shirt")
    for s in (-1, 1):
        m.cube(body, (1.6 * s - 1, 23.0, -2.6), (2, 1.5, 0.5), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               density=2, tag="shirt_collar", rotation=(18, 0, -32 * s), pivot=(1.6 * s, 24, -2.4))

    def jacket(face, x, y, w, h):
        if face == "down":
            return None
        if face == "up":
            return None if 4 <= x <= 11 and y < 5 else ln(face, x, y, w, h)
        c = ln(face, x, y, w, h)
        if face == "north":
            # Open to the button at the waist, the fronts parting again below it.
            if y < 15:
                half = max(0.0, 5.5 - y * 0.36)
            else:
                half = (y - 15) * 0.5
            if abs(x - 7.5) < half:
                return None
            if abs(abs(x - 7.5) - half) < 1:
                c = shade(c, 0.82)
            if y == 15 and x in (7, 8):
                return hexc("#5b4a33")  # the button
        if face == "south" and x in (7, 8) and y > 21:
            return shade(c, 0.6)  # the back vent
        return c
    m.cube(body, (-4, 9, -2), (8, 15, 4), jacket, inflate=0.72, density=2, tag="jacket")
    # Notch lapels and the collar.
    for s in (-1, 1):
        def lapel(face, x, y, w, h):
            c = shade(ln(face, x, y, w, h), 1.1)
            if face == "north" and (x == 0 if s < 0 else x == w - 1):
                c = shade(c, 0.8)
            if face == "north" and y < 2 and (x >= w - 1 if s < 0 else x == 0):
                return None  # the notch
            return c
        m.cube(body, (2.6 * s - 1, 17.2, -3.05), (2, 6.5, 0.5), lapel, density=2, tag="lapel",
               rotation=(3, 0, 16 * s), pivot=(1.2 * s, 17.2, -2.8))
    m.cube(body, (-3.8, 23.5, 0.4), (7.6, 1.5, 2), lambda f, x, y, w, h: shade(ln(f, x, y, w, h), 1.08), inflate=0.65,
           density=2, tag="collar_back")
    # Breast pocket (with a folded square), flap pockets at the hips.
    m.cube(body, (1.5, 19.6, -2.85), (2, 0.5, 0.25), solid(LINEN[2]), density=4, tag="breast_pocket")
    m.cube(body, (1.8, 20.1, -2.85), (1.5, 0.5, 0.25), solid(hexc("#8c3a32")), density=4, tag="pocket_square")
    for s in (-1, 1):
        m.cube(body, (2.4 * s - 1.5, 12.6, -2.9), (3, 0.75, 0.5), lambda f, x, y, w, h: shade(ln(f, x, y, w, h), 0.9 if y else 1.1),
               density=4, tag="flap")
    for side, s in (("right", -1), ("left", 1)):
        arm, fore = m.limb(o, f"{side}_arm"), m.limb(o, f"{side}_forearm")
        m.cube(arm, (5.5 * s - 1.5, 16.5, -1.5), (3, 7, 3), none_on(("down",), ln), inflate=0.6, density=2, tag="sleeve")
        m.cube(fore, (5.5 * s - 1.5, 12.5, -1.5), (3, 4.5, 3), none_on(("up",),
               lambda f, x, y, w, h: shade(ln(f, x, y, w, h), 0.85) if y >= h - 1 else ln(f, x, y, w, h)),
               inflate=0.6, density=2, tag="sleeve")
        m.cube(fore, (5.5 * s - 1.5, 12.0, -1.5), (3, 1, 3), none_on(("up",), sh), inflate=0.3, density=2, tag="cuff")
        leg, shin = m.limb(o, f"{side}_leg"), m.limb(o, f"{side}_shin")
        tr = linen(95 + s)

        def trouser(face, x, y, w, h, tr=tr):
            c = tr(face, x, y, w, h)
            if face in ("north", "south") and x == w // 2:
                c = shade(c, 1.12)  # the crease
            return c
        m.cube(leg, (2 * s - 2, 6, -2), (4, 6, 4), trouser, inflate=0.15, density=2, tag="trousers")
        m.cube(shin, (2 * s - 2, 1.5, -2), (4, 4.5, 4), trouser, inflate=0.15, density=2, tag="trousers")

        def loafer(face, x, y, w, h):
            n = fbm(x * 2, y * 2, 98 + s, 64, 64, 2, 10.0)
            c = LOAFER[3.0 + (n - 0.5) * 1.2]
            if y >= h - 1 and face != "up":
                return hexc("#140b06")
            if face == "up" and y < 3:
                return shade(c, 0.6)
            if face in ("north", "east", "west") and y == 0:
                c = shade(c, 1.2)
            return c
        m.cube(shin, (2 * s - 2.2, 0, -2.9), (4.4, 1.5, 5.2), loafer, density=2, tag="loafer")


# =====================================================================================================
# Animations
# =====================================================================================================

E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"
A = "animation.chuck."


class Clip:
    """Wraps an Anim: keys on a limb go to the shared bone and to its copy in every outfit."""

    def __init__(self, anim):
        self.a = anim

    def _names(self, bone):
        return [bone] + [f"{o}_{bone}" for o in OUTFITS] if bone in LIMBS else [bone]

    def rot(self, bone, *keys):
        for b in self._names(bone):
            self.a.rot(b, *keys)
        return self

    def pos(self, bone, *keys):
        for b in self._names(bone):
            self.a.pos(b, *keys)
        return self

    def scale(self, bone, *keys):
        for b in self._names(bone):
            self.a.scale(b, *keys)
        return self


def _hold(c, bone, t_end, v):
    return (t_end, v)


# Poses (Bedrock degrees), reused across clips.
GLASS_ARM = [-8, 0, 4]          # right upper arm, holding his drink
GLASS_FORE = [-48, 12, 0]       # the forearm raised, the glass at his belt
SNAP_FINGERS = {"middle": [-70, 0, 0], "ring": [-95, 0, 0], "index": [-30, 0, 0], "thumb": [-10, 0, 40]}


def seated(c, t, lean=0.0, ease=None):
    """Keys the seated legs and lowered root at time t (the hips rest about 6 px above his feet)."""
    k = (lambda v: (t, v, ease) if ease else (t, v))
    c.pos("root", k([0, -4, 0]))
    c.rot("right_leg", k([-84, 0, 3]))
    c.rot("left_leg", k([-84, 0, -3]))
    c.rot("right_shin", k([72, 0, 0]))
    c.rot("left_shin", k([72, 0, 0]))
    c.rot("body", k([lean, 0, 0]))


def anims():
    f = AnimFile()

    def new(name, length, loop=False, hold=False):
        return Clip(f.new(A + name, length, loop, hold))

    # --- loops ------------------------------------------------------------------------------------
    idle = new("idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.15, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("body", (0, [2, 0, 0]), (2.0, [3, 0, 0.6], E_IO), (4.0, [2, 0, 0], E_IO))  # a writer's stoop
    idle.rot("head", (0, [4, 0, 0]), (1.3, [6, -9, 3], E_IO), (2.8, [2, 7, -2], E_IO), (4.0, [4, 0, 0], E_IO))
    idle.rot("right_arm", (0, GLASS_ARM), (2.0, [-10, 0, 5], E_IO), (4.0, GLASS_ARM, E_IO))
    idle.rot("right_forearm", (0, GLASS_FORE), (2.0, [-52, 14, 0], E_IO), (4.0, GLASS_FORE, E_IO))
    idle.rot("right_hand", (0, [44, -10, 0]), (4.0, [44, -10, 0]))
    idle.rot("left_arm", (0, [2, 0, -3]), (2.0, [-2, 0, -4], E_IO), (4.0, [2, 0, -3], E_IO))
    idle.rot("left_forearm", (0, [-8, 0, 0]), (4.0, [-8, 0, 0]))
    for fn in ("index", "middle", "ring"):
        idle.rot("right_" + fn, (0, [-60, 0, 0]), (4.0, [-60, 0, 0]))
    idle.rot("right_thumb", (0, [-20, 0, 30]), (4.0, [-20, 0, 30]))

    walk = new("walk", 1.2, loop=True)
    for leg, shin, s in (("right_leg", "right_shin", 1), ("left_leg", "left_shin", -1)):
        walk.rot(leg, (0, [22 * s, 0, 0]), (0.6, [-22 * s, 0, 0], E_IO), (1.2, [22 * s, 0, 0], E_IO))
        # The knee bends as the leg swings forward.
        a, b = (0.3, 0.9) if s > 0 else (0.9, 0.3)
        walk.rot(shin, (0, [4, 0, 0]), (a, [28, 0, 0], E_IO), (0.6 if s > 0 else 0.6, [4, 0, 0], E_IO), (1.2, [4, 0, 0], E_IO))
    walk.rot("left_arm", (0, [-16, 0, -3]), (0.6, [16, 0, -3], E_IO), (1.2, [-16, 0, -3], E_IO))
    walk.rot("left_forearm", (0, [-14, 0, 0]), (0.6, [-6, 0, 0], E_IO), (1.2, [-14, 0, 0], E_IO))
    walk.rot("right_arm", (0, [-2, 0, 4]), (0.6, [-14, 0, 4], E_IO), (1.2, [-2, 0, 4], E_IO))
    walk.rot("right_forearm", (0, GLASS_FORE), (1.2, GLASS_FORE))
    walk.rot("right_hand", (0, [44, -10, 0]), (1.2, [44, -10, 0]))
    for fn in ("index", "middle", "ring"):
        walk.rot("right_" + fn, (0, [-60, 0, 0]), (1.2, [-60, 0, 0]))
    walk.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.35, 0], E_OUT), (0.6, [0, 0, 0], E_IN), (0.9, [0, 0.35, 0], E_OUT),
             (1.2, [0, 0, 0], E_IN))
    walk.rot("body", (0, [3, 4, 0]), (0.6, [3, -4, 0], E_IO), (1.2, [3, 4, 0], E_IO))
    walk.rot("head", (0, [3, -3, 0]), (0.6, [3, 3, 0], E_IO), (1.2, [3, -3, 0], E_IO))

    sit = new("sit", 6.0, loop=True)
    seated(sit, 0, -6)
    seated(sit, 6.0, -6)
    sit.rot("body", (0, [-6, 0, 0]), (3.0, [-7, 0, 1], E_IO), (6.0, [-6, 0, 0], E_IO))
    sit.rot("head", (0, [6, 0, 0]), (2.0, [10, -14, 4], E_IO), (3.8, [8, -12, 4]), (5.0, [3, 4, 0], E_IO), (6.0, [6, 0, 0], E_IO))
    # Glass in hand on the armrest, the other hand on his thigh; a sip halfway through.
    sit.rot("right_arm", (0, [-22, 0, 6]), (2.4, [-22, 0, 6]), (3.0, [-60, 20, 4], E_IO), (3.8, [-60, 20, 4]),
            (4.4, [-22, 0, 6], E_IO), (6.0, [-22, 0, 6]))
    sit.rot("right_forearm", (0, [-50, 10, 0]), (2.4, [-50, 10, 0]), (3.0, [-100, 30, 0], E_IO), (3.8, [-100, 30, 0]),
            (4.4, [-50, 10, 0], E_IO), (6.0, [-50, 10, 0]))
    sit.rot("right_hand", (0, [50, -10, 0]), (2.4, [50, -10, 0]), (3.0, [70, -30, 0], E_IO), (3.8, [80, -30, 0]),
            (4.4, [50, -10, 0], E_IO), (6.0, [50, -10, 0]))
    for fn in ("index", "middle", "ring"):
        sit.rot("right_" + fn, (0, [-60, 0, 0]), (6.0, [-60, 0, 0]))
    sit.rot("left_arm", (0, [-34, 0, -4]), (6.0, [-34, 0, -4]))
    sit.rot("left_forearm", (0, [-30, 0, 0]), (6.0, [-30, 0, 0]))
    sit.rot("left_hand", (0, [40, 0, 0]), (6.0, [40, 0, 0]))

    st = new("sit_type", 1.6, loop=True)
    seated(st, 0, 12)
    seated(st, 1.6, 12)
    st.rot("body", (0, [12, 0, 0]), (0.8, [13, 0, 0.8], E_IO), (1.6, [12, 0, 0], E_IO))
    st.rot("head", (0, [14, 0, 0]), (0.4, [17, -3, 0], E_IO), (1.0, [13, 4, 0], E_IO), (1.6, [14, 0, 0], E_IO))
    st.scale("glass", (0, [0, 0, 0]), (1.6, [0, 0, 0]))  # set down on the desk while he types
    for side, s, ph in (("right", -1, 0.0), ("left", 1, 0.4)):
        st.rot(f"{side}_arm", (0, [-26, 0, 6 * s * -1]), (1.6, [-26, 0, 6 * s * -1]))
        st.rot(f"{side}_forearm", (0, [-62, 4 * s, 0]), (0.2 + ph, [-58, 4 * s, 0], E_OUT), (0.4 + ph, [-62, 4 * s, 0], E_IN),
               (1.6, [-62, 4 * s, 0]))
        st.rot(f"{side}_hand", (0, [30, 0, 8 * s]), (1.6, [30, 0, 8 * s]))
        # Fingers peck at the keys, out of step.
        for k, fn in enumerate(("index", "middle", "ring")):
            t0 = (ph + k * 0.37) % 1.2
            st.rot(f"{side}_{fn}", (0, [-30, 0, 0]), (t0, [-30, 0, 0]), (t0 + 0.1, [-62, 0, 0], E_SNAP),
                   (t0 + 0.24, [-30, 0, 0], E_OUT), (1.6, [-30, 0, 0]))
    # The carriage return, once a loop: the left hand flicks out.
    st.rot("left_arm", (0, [-26, 0, -6]), (1.2, [-26, 0, -6]), (1.35, [-30, -10, -24], E_OUT), (1.6, [-26, 0, -6], E_IO))

    talk = new("talk", 3.0, loop=True)
    talk.rot("head", (0, [2, 0, 0]), (0.6, [-4, 8, 2], E_IO), (1.2, [5, -4, -3], E_IO), (2.0, [-2, -10, 0], E_IO),
             (3.0, [2, 0, 0], E_IO))
    talk.rot("body", (0, [2, 0, 0]), (1.5, [1, -6, 0], E_IO), (3.0, [2, 0, 0], E_IO))
    talk.rot("left_arm", (0, [-24, 0, -10]), (0.6, [-38, -10, -18], E_IO), (1.2, [-20, 6, -8], E_IO),
             (2.0, [-42, -14, -22], E_IO), (3.0, [-24, 0, -10], E_IO))
    talk.rot("left_forearm", (0, [-40, -20, 0]), (0.6, [-55, -30, 0], E_IO), (1.2, [-35, -10, 0], E_IO),
             (2.0, [-60, -36, 0], E_IO), (3.0, [-40, -20, 0], E_IO))
    talk.rot("left_hand", (0, [0, 0, -40]), (1.0, [10, 0, -55], E_IO), (2.0, [0, 0, -35], E_IO), (3.0, [0, 0, -40], E_IO))
    talk.rot("right_arm", (0, GLASS_ARM), (1.5, [-12, 0, 6], E_IO), (3.0, GLASS_ARM, E_IO))
    talk.rot("right_forearm", (0, GLASS_FORE), (3.0, GLASS_FORE))
    talk.rot("right_hand", (0, [44, -10, 0]), (3.0, [44, -10, 0]))
    for fn in ("index", "middle", "ring"):
        talk.rot("right_" + fn, (0, [-60, 0, 0]), (3.0, [-60, 0, 0]))

    # --- one-shots --------------------------------------------------------------------------------
    rise = new("rise", 1.6)
    seated(rise, 0, -6)
    rise.pos("root", (0, [0, -4, 0]), (0.5, [0, -3.5, 0], E_IO), (1.2, [0, 0, 0], E_OUT), (1.6, [0, 0, 0]))
    rise.rot("body", (0, [-6, 0, 0]), (0.5, [28, 0, 0], E_IO), (1.2, [6, 0, 0], E_IO), (1.6, [2, 0, 0], E_IO))
    for leg, shin in (("right_leg", "right_shin"), ("left_leg", "left_shin")):
        rise.rot(leg, (0, [-84, 0, 0]), (0.5, [-80, 0, 0]), (1.2, [-8, 0, 0], E_OUT), (1.6, [0, 0, 0], E_IO))
        rise.rot(shin, (0, [72, 0, 0]), (0.5, [76, 0, 0]), (1.2, [10, 0, 0], E_OUT), (1.6, [0, 0, 0], E_IO))
    for arm in ("right_arm", "left_arm"):
        rise.rot(arm, (0, [-22, 0, 0]), (0.5, [-10, 0, 0], E_IO), (1.6, [0, 0, 0], E_IO))
    rise.rot("head", (0, [6, 0, 0]), (0.5, [-8, 0, 0], E_IO), (1.6, [0, 0, 0], E_IO))

    emerge = new("emerge", 3.4)
    # He sets his shoulders, tugs his clothes straight, rolls his neck and opens his hands: let's begin.
    emerge.rot("head", (0, [8, 0, 0]), (0.6, [18, 0, 0], E_IO), (1.6, [6, 0, 0]), (2.0, [-6, 0, 18], E_IO),
               (2.4, [-6, 0, -18], E_IO), (2.8, [-4, 0, 0], E_IO), (3.4, [0, 0, 0], E_IO))
    for side, s in (("right", -1), ("left", 1)):
        emerge.rot(f"{side}_arm", (0, [0, 0, 0]), (0.6, [-38, 0, 20 * s], E_IO), (1.0, [-34, 0, 18 * s]),
                   (1.4, [-20, 0, 10 * s], E_IO), (2.8, [-18, 0, 30 * -s], E_BACK), (3.4, [0, 0, 0], E_IO))
        emerge.rot(f"{side}_forearm", (0, [0, 0, 0]), (0.6, [-100, 30 * -s, 0], E_IO), (1.0, [-96, 30 * -s, 0]),
                   (1.4, [-40, 0, 0], E_IO), (2.8, [-30, 0, 0]), (3.4, [0, 0, 0], E_IO))
        emerge.rot(f"{side}_hand", (0, [0, 0, 0]), (0.6, [0, 0, 0]), (1.0, [30, 0, 0], E_IO), (1.4, [0, 0, 0]),
                   (2.8, [0, 0, 50 * -s], E_IO), (3.4, [0, 0, 0], E_IO))
    emerge.rot("body", (0, [6, 0, 0]), (0.6, [-4, 0, 0], E_IO), (1.4, [-2, 0, 0]), (3.4, [0, 0, 0], E_IO))
    emerge.scale("glass", (0, [1, 1, 1]), (3.4, [1, 1, 1]))

    snap = new("snap", 2.0)
    # The hand rises before his face, thumb against middle finger: a beat, then the snap.
    snap.rot("right_arm", (0, [0, 0, 0]), (0.5, [-20, 0, 58], E_OUT), (1.0, [-22, 0, 60]), (1.05, [-24, 0, 63], E_SNAP),
             (1.5, [-22, 0, 60]), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_forearm", (0, [0, 0, 0]), (0.5, [-10, 0, 92], E_OUT), (1.0, [-12, 0, 96]), (1.05, [-6, 0, 88], E_SNAP),
             (1.5, [-10, 0, 92]), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_hand", (0, [0, 0, 0]), (0.5, [0, -70, 0], E_OUT), (1.0, [0, -70, 0]), (1.05, [14, -70, 0], E_SNAP),
             (1.5, [0, -70, 0]), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_middle", (0, [0, 0, 0]), (0.5, [-40, 0, 0], E_OUT), (1.0, [-44, 0, 0]), (1.05, [-110, 0, 0], E_SNAP),
             (1.5, [-110, 0, 0]), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_thumb", (0, [0, 0, 0]), (0.5, [-30, 0, 40], E_OUT), (1.0, [-34, 0, 44]), (1.05, [-10, 0, 10], E_SNAP),
             (1.5, [-10, 0, 10]), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_index", (0, [0, 0, 0]), (0.5, [-20, 0, 0], E_OUT), (2.0, [0, 0, 0], E_IO))
    snap.rot("right_ring", (0, [0, 0, 0]), (0.5, [-100, 0, 0], E_OUT), (1.5, [-100, 0, 0]), (2.0, [0, 0, 0], E_IO))
    snap.rot("head", (0, [0, 0, 0]), (0.5, [4, -12, 0], E_OUT), (1.05, [8, -14, 0], E_SNAP), (2.0, [0, 0, 0], E_IO))
    snap.rot("body", (0, [0, 0, 0]), (0.5, [0, -8, 0], E_OUT), (2.0, [0, 0, 0], E_IO))
    snap.scale("glass", (0, [0, 0, 0]), (2.0, [0, 0, 0]))

    point = new("point", 1.6)
    point.rot("left_arm", (0, [0, 0, 0]), (0.35, [-86, -12, 0], E_OUT), (1.2, [-86, -12, 0]), (1.6, [0, 0, 0], E_IO))
    point.rot("left_forearm", (0, [0, 0, 0]), (0.35, [-6, 0, 0], E_OUT), (1.2, [-6, 0, 0]), (1.6, [0, 0, 0], E_IO))
    point.rot("left_hand", (0, [0, 0, 0]), (0.35, [0, -80, 0], E_OUT), (1.2, [0, -80, 0]), (1.6, [0, 0, 0], E_IO))
    for fn, v in (("middle", -100), ("ring", -105), ("thumb", -40)):
        point.rot("left_" + fn, (0, [0, 0, 0]), (0.3, [v, 0, 0], E_OUT), (1.2, [v, 0, 0]), (1.6, [0, 0, 0], E_IO))
    point.rot("head", (0, [0, 0, 0]), (0.35, [-4, 10, 0], E_OUT), (1.2, [-4, 10, 0]), (1.6, [0, 0, 0], E_IO))
    point.rot("body", (0, [0, 0, 0]), (0.35, [0, 12, 0], E_OUT), (1.2, [0, 12, 0]), (1.6, [0, 0, 0], E_IO))

    shove = new("shove", 1.0)
    for side in ("right", "left"):
        shove.rot(f"{side}_arm", (0, [0, 0, 0]), (0.25, [-50, 0, 0], E_OUT), (0.4, [-88, 0, 0], E_SNAP), (0.6, [-88, 0, 0]),
                  (1.0, [0, 0, 0], E_IO))
        shove.rot(f"{side}_forearm", (0, [0, 0, 0]), (0.25, [-80, 0, 0], E_OUT), (0.4, [-4, 0, 0], E_SNAP), (0.6, [-4, 0, 0]),
                  (1.0, [0, 0, 0], E_IO))
        shove.rot(f"{side}_hand", (0, [0, 0, 0]), (0.4, [-70, 0, 0], E_SNAP), (0.6, [-70, 0, 0]), (1.0, [0, 0, 0], E_IO))
    shove.rot("body", (0, [0, 0, 0]), (0.25, [-6, 0, 0], E_OUT), (0.4, [14, 0, 0], E_SNAP), (1.0, [0, 0, 0], E_IO))
    shove.rot("right_leg", (0, [0, 0, 0]), (0.4, [18, 0, 0], E_SNAP), (1.0, [0, 0, 0], E_IO))
    shove.rot("left_leg", (0, [0, 0, 0]), (0.4, [-22, 0, 0], E_SNAP), (1.0, [0, 0, 0], E_IO))
    shove.scale("glass", (0, [0, 0, 0]), (1.0, [0, 0, 0]))

    back = new("backhand", 1.0)
    back.rot("left_arm", (0, [0, 0, 0]), (0.35, [-70, 50, 70], E_OUT), (0.5, [-70, -60, -10], E_SNAP), (0.7, [-60, -60, -10]),
             (1.0, [0, 0, 0], E_IO))
    back.rot("left_forearm", (0, [0, 0, 0]), (0.35, [-70, 0, 0], E_OUT), (0.5, [-10, 0, 0], E_SNAP), (1.0, [0, 0, 0], E_IO))
    back.rot("body", (0, [0, 0, 0]), (0.35, [0, -24, 0], E_OUT), (0.5, [0, 30, 0], E_SNAP), (0.7, [0, 28, 0]), (1.0, [0, 0, 0], E_IO))
    back.rot("head", (0, [0, 0, 0]), (0.35, [0, 20, 0], E_OUT), (0.5, [0, -20, 0], E_SNAP), (1.0, [0, 0, 0], E_IO))

    throw = new("throw_glass", 1.4)
    throw.rot("right_arm", (0, GLASS_ARM), (0.45, [30, 0, 30], E_OUT), (0.6, [-110, 0, 10], E_SNAP), (0.8, [-80, 0, 6]),
              (1.4, [0, 0, 0], E_IO))
    throw.rot("right_forearm", (0, GLASS_FORE), (0.45, [-90, 0, 0], E_OUT), (0.6, [-10, 0, 0], E_SNAP), (1.4, [0, 0, 0], E_IO))
    throw.rot("right_hand", (0, [44, -10, 0]), (0.45, [60, 0, 0]), (0.6, [-30, 0, 0], E_SNAP), (1.4, [0, 0, 0], E_IO))
    throw.rot("body", (0, [0, 0, 0]), (0.45, [-6, -24, 0], E_OUT), (0.6, [12, 20, 0], E_SNAP), (1.4, [0, 0, 0], E_IO))
    throw.rot("left_arm", (0, [0, 0, 0]), (0.45, [-40, 0, -20], E_OUT), (0.6, [20, 0, -10], E_SNAP), (1.4, [0, 0, 0], E_IO))
    # The glass leaves his hand at the snap of the throw (the projectile takes over).
    throw.scale("glass", (0, [1, 1, 1]), (0.58, [1, 1, 1]), (0.6, [0, 0, 0]), (1.4, [0, 0, 0]))

    tair = new("type_air", 2.0)
    tair.scale("glass", (0, [0, 0, 0]), (2.0, [0, 0, 0]))
    tair.rot("body", (0, [0, 0, 0]), (0.3, [8, 0, 0], E_OUT), (1.7, [8, 0, 0]), (2.0, [0, 0, 0], E_IO))
    tair.rot("head", (0, [0, 0, 0]), (0.3, [10, 0, 0], E_OUT), (1.7, [8, 0, 0]), (2.0, [0, 0, 0], E_IO))
    for side, s, ph in (("right", -1, 0.0), ("left", 1, 0.12)):
        tair.rot(f"{side}_arm", (0, [0, 0, 0]), (0.3, [-40, 0, 8 * -s], E_OUT), (1.7, [-40, 0, 8 * -s]), (2.0, [0, 0, 0], E_IO))
        keys = [(0, [0, 0, 0]), (0.3, [-55, 0, 0], E_OUT)]
        for k in range(6):
            t0 = 0.4 + ph + k * 0.2
            keys += [(t0, [-48, 0, 0], E_SNAP), (t0 + 0.1, [-56, 0, 0], E_OUT)]
        keys += [(2.0, [0, 0, 0], E_IO)]
        tair.rot(f"{side}_forearm", *keys)
        for fn in ("index", "middle", "ring"):
            tair.rot(f"{side}_{fn}", (0, [0, 0, 0]), (0.3, [-35, 0, 0]), (1.7, [-35, 0, 0]), (2.0, [0, 0, 0], E_IO))

    bs = new("backspace", 1.2)
    bs.scale("glass", (0, [0, 0, 0]), (1.2, [0, 0, 0]))
    bs.rot("right_arm", (0, [0, 0, 0]), (0.3, [-70, 0, 6], E_OUT), (0.45, [-50, 0, 6], E_SNAP), (0.8, [-50, 0, 6]),
           (1.2, [0, 0, 0], E_IO))
    bs.rot("right_forearm", (0, [0, 0, 0]), (0.3, [-60, 0, 0], E_OUT), (0.45, [-30, 0, 0], E_SNAP), (1.2, [0, 0, 0], E_IO))
    bs.rot("right_hand", (0, [0, 0, 0]), (0.3, [0, 90, 0], E_OUT), (0.45, [-30, 90, 0], E_SNAP), (1.2, [0, 0, 0], E_IO))
    for fn in ("middle", "ring"):
        bs.rot("right_" + fn, (0, [0, 0, 0]), (0.3, [-100, 0, 0]), (0.9, [-100, 0, 0]), (1.2, [0, 0, 0], E_IO))
    bs.rot("head", (0, [0, 0, 0]), (0.3, [6, 0, 0], E_OUT), (0.6, [6, 12, 0], E_IO), (0.8, [6, -12, 0], E_IO),
           (1.2, [0, 0, 0], E_IO))

    rw = new("rewrite", 2.0)
    rw.scale("glass", (0, [1, 1, 1]), (2.0, [1, 1, 1]))
    rw.rot("left_arm", (0, [0, 0, 0]), (0.4, [-80, 30, -30], E_OUT), (0.8, [-80, -30, 10], E_IO), (1.1, [-80, 30, -30], E_IO),
           (1.5, [-90, 0, -10], E_IO), (2.0, [0, 0, 0], E_IO))
    rw.rot("left_forearm", (0, [0, 0, 0]), (0.4, [-30, 0, 0]), (1.5, [-30, 0, 0]), (2.0, [0, 0, 0], E_IO))
    rw.rot("left_hand", (0, [0, 0, 0]), (0.4, [-70, 0, 0], E_OUT), (1.5, [-70, 0, 0]), (2.0, [0, 0, 0], E_IO))
    rw.rot("head", (0, [0, 0, 0]), (0.4, [-6, 8, 0], E_OUT), (1.5, [-2, -6, 0], E_IO), (2.0, [0, 0, 0], E_IO))
    rw.rot("body", (0, [0, 0, 0]), (0.8, [0, -10, 0], E_IO), (1.1, [0, 10, 0], E_IO), (2.0, [0, 0, 0], E_IO))

    t2 = new("transform_2", 3.0)
    # Arms cross, a full turn, and he comes out of it in the suit straightening his lapels.
    t2.rot("body", (0, [0, 0, 0]), (0.5, [10, 0, 0], E_OUT), (1.6, [6, 360, 0], E_IO), (2.0, [-4, 360, 0], E_OUT),
           (3.0, [0, 360, 0], E_IO))
    for side, s in (("right", -1), ("left", 1)):
        t2.rot(f"{side}_arm", (0, [0, 0, 0]), (0.5, [-70, 40 * -s, 0], E_OUT), (1.2, [-60, 40 * -s, 0]),
               (1.6, [-20, 0, 60 * -s], E_BACK), (2.2, [-30, 0, 20 * s], E_IO), (2.6, [-30, 0, 20 * s]), (3.0, [0, 0, 0], E_IO))
        t2.rot(f"{side}_forearm", (0, [0, 0, 0]), (0.5, [-60, 0, 0], E_OUT), (1.2, [-60, 0, 0]), (1.6, [0, 0, 0], E_BACK),
               (2.2, [-100, 30 * -s, 0], E_IO), (2.6, [-90, 30 * -s, 0]), (3.0, [0, 0, 0], E_IO))
    t2.rot("head", (0, [0, 0, 0]), (0.5, [20, 0, 0], E_OUT), (1.6, [-10, 0, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    t2.scale("glass", (0, [0, 0, 0]), (3.0, [0, 0, 0]))

    t3 = new("transform_3", 4.0, hold=True)
    t3.scale("glass", (0, [0, 0, 0]), (4.0, [0, 0, 0]))
    t3.pos("root", (0, [0, 0, 0]), (1.0, [0, -1, 0], E_OUT), (4.0, [0, 6, 0], E_IN))
    t3.rot("body", (0, [0, 0, 0]), (1.0, [16, 0, 0], E_OUT), (2.0, [-22, 0, 0], E_BACK), (4.0, [-28, 0, 0], E_IO))
    t3.rot("head", (0, [0, 0, 0]), (1.0, [24, 0, 0], E_OUT), (2.0, [-50, 0, 0], E_BACK), (4.0, [-58, 0, 0], E_IO))
    for side, s in (("right", -1), ("left", 1)):
        t3.rot(f"{side}_arm", (0, [0, 0, 0]), (1.0, [-20, 0, 20 * -s * -1], E_OUT), (2.0, [-20, 0, 92 * -s], E_BACK),
               (4.0, [-30, 0, 104 * -s], E_IO))
        t3.rot(f"{side}_forearm", (0, [0, 0, 0]), (1.0, [-40, 0, 0], E_OUT), (2.0, [-6, 0, 0], E_BACK), (4.0, [0, 0, 0]))
        t3.rot(f"{side}_hand", (0, [0, 0, 0]), (2.0, [0, 0, 0]), (4.0, [0, 0, 30 * -s], E_IO))
        for fn in ("index", "middle", "ring", "thumb"):
            t3.rot(f"{side}_{fn}", (0, [0, 0, 0]), (2.0, [16, 0, 0], E_OUT), (4.0, [24, 0, 0]))
        t3.rot(f"{side}_leg", (0, [0, 0, 0]), (2.0, [6, 0, 4 * s], E_IO), (4.0, [10, 0, 6 * s], E_IO))
        t3.rot(f"{side}_shin", (0, [0, 0, 0]), (4.0, [14, 0, 0], E_IO))
    t3.scale("root", (0, [1, 1, 1]), (2.0, [1, 1, 1]), (4.0, [1.06, 1.12, 1.06], E_IN))

    death = new("death", 3.0, hold=True)
    death.pos("root", (0, [0, 0, 0]), (1.4, [0, -6, 0], E_IN), (3.0, [0, -6, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (1.4, [-80, 0, 0], E_OUT), (3.0, [-80, 0, 0]))
    death.rot("left_leg", (0, [0, 0, 0]), (1.4, [-10, 0, 0], E_OUT), (3.0, [-10, 0, 0]))
    death.rot("right_shin", (0, [0, 0, 0]), (1.4, [80, 0, 0], E_OUT), (3.0, [80, 0, 0]))
    death.rot("left_shin", (0, [0, 0, 0]), (1.4, [84, 0, 0], E_OUT), (3.0, [84, 0, 0]))
    death.rot("body", (0, [0, 0, 0]), (1.4, [20, 0, 0], E_OUT), (3.0, [26, 0, 0]))
    death.rot("head", (0, [0, 0, 0]), (1.4, [30, 0, 0], E_OUT), (3.0, [34, 0, 0]))
    for side in ("right_arm", "left_arm"):
        death.rot(side, (0, [0, 0, 0]), (1.4, [-10, 0, 0], E_OUT), (3.0, [-6, 0, 0]))
    death.scale("glass", (0, [0, 0, 0]), (3.0, [0, 0, 0]))
    return f


# =====================================================================================================
# Output and previews
# =====================================================================================================

def generate():
    m = rig()
    t, _ = m.build(seed=901)
    m.rig.write(GEO + "chuck.geo.json")
    save(t, "entity", "chuck")
    save(cracks_layer(m, 903, hexc("#fff4d0")), "entity", "chuck_cracks")
    anims().write(ANIM + "chuck.animation.json")


def preview(path, outfit="flannel", anim=None, t=0.0, scale=8, views=("front", "three_quarter", "side", "back")):
    import json
    import preview3d
    m = rig()
    tex, _ = m.build(seed=901)
    hidden = tuple(f"outfit_{o}" for o in OUTFITS if o != outfit)
    pose = None
    if anim:
        a = anims()
        data = {"animations": {x.name: x.to_json() for x in a.anims}}
        pose = preview3d.pose_from(json.loads(json.dumps(data)), A + anim, t)
    preview3d.render(m.rig, tex, path, pose=pose, hidden=hidden, scale=scale, views=views)


if __name__ == "__main__":
    generate()
