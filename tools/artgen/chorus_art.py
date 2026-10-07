"""The Broken Chorus: a whole choir of angels fused into one body after God left.

Ophanim, seraph and cherub in one rig, with damage variants and three textures:
  * ``broken_chorus.png``           the body;
  * ``broken_chorus_glowmask.png``  eyes, flames and the core (always full bright);
  * ``broken_chorus_cracks.png``    the cracks in its marble, tinted by phase in game (gold, orange, red).

Rig (Bedrock pixels, origin at the feet, facing north; 8 px = 1 block at MODEL_SCALE 2):

  root
  +- body_turn            turned by the renderer toward its heading
  |  +- core              core_light (the burning heart) and core_shell (cracked marble eggshell)
  |  +- torso             the fused bodies, with choir masks and four arms
  |  +- heads             face_{man,lion,ox,eagle}_{intact,cracked,broken}
  |  +- halo              a cracked ring with three shards adrift
  |  +- wings             wing_{top,mid,low}_{l,r} -> _mid -> _tip, feather groups _fa/_fb, and stumps
  +- wheels               wheel_{outer,mid,inner}, each with its eyes eye_N (lids, ball, socket)
  +- fragments            what is left of the wheels in the final phase

Bones the renderer drives (and animations must never key): body_turn, wheel_*, eye_* and their
lids, the six wing roots, heads and the face_* groups. Their numbers are mirrored in Java by
ChorusGeometry; ChorusAssetsTest checks the two agree.
"""

import math
import random
import zlib

from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import noise, paint_cube
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
TEX = ASSETS + "/textures/entity/"

# --- shared numbers (ChorusGeometry) ---------------------------------------------------------

SCALE = 2.0
CORE_Y = 64
HEADS_Y = 24
HALO_Y = 100
# name, radius, segments, axial width, radial depth, eyes, first eye angle (rad)
WHEELS = [
    ("outer", 60.0, 24, 8, 6, 5, 0.3),
    ("mid", 46.4, 20, 7, 5, 4, 0.7),
    ("inner", 33.6, 16, 6, 4, 3, 1.1),
]
EYE_OUT = 3.0          # eye centre sits this far outside the ring's centre line
FACES = ["man", "lion", "ox", "eagle"]
FACE_CENTRES = {"man": (0, HEADS_Y, -12), "lion": (-12, HEADS_Y, 0), "ox": (12, HEADS_Y, 0), "eagle": (0, HEADS_Y, 12)}
# level -> (shoulder |x|, y, z, arm segment lengths)
WINGS = {
    "top": (9, 74, 3, (24, 22, 18)),
    "mid": (10, 62, 5, (22, 20, 16)),
    "low": (8, 44, 4, (16, 14, 12)),
}
WING_ORDER = [("top", "l"), ("top", "r"), ("mid", "l"), ("mid", "r"), ("low", "l"), ("low", "r")]

# --- palettes --------------------------------------------------------------------------------

GOLD = Ramp("#3a2307", "#6b440f", "#a06a1c", "#cf9a35", "#efc860", "#fff0b0")
OLD_GOLD = Ramp("#2a1a08", "#4d3210", "#76511c", "#a0752c", "#c79a42", "#e8c46a")
MARBLE = Ramp("#5c5852", "#8a857c", "#b5afa4", "#d6d0c4", "#ece7dc", "#fbf8f0")
FEATHER = Ramp("#7a766f", "#a7a39a", "#cdc8be", "#e5e1d7", "#f5f2ea", "#ffffff")
FUR = Ramp("#4a2a0c", "#7a4614", "#a8661e", "#d08a2c", "#ecb04a", "#ffd47a")
FLAME = [hexc("#fff6c8"), hexc("#ffd66a"), hexc("#ffa53a"), hexc("#f2661c")]
LIGHT = [hexc("#ffffff"), hexc("#fff8e0"), hexc("#ffe9a8"), hexc("#ffd36a")]
IRIS = [hexc("#0a1024"), hexc("#2456b8"), hexc("#4f8ef0"), hexc("#a8d4ff")]
SCLERA = hexc("#f4f6ff")
CRACK = hexc("#fff4d6")          # cracks are drawn near-white; the game tints them by phase
CRACK_DIM = hexc("#c9b88a")
EMBER = [hexc("#ffb347"), hexc("#ff7a1f"), hexc("#c43a0a")]
SHADOW = hexc("#1a1208")
CLEAR = (0, 0, 0, 0)


def _h(*parts):
    """A stable pseudo-random number in [0, 1) from any hashable parts."""
    return random.Random(zlib.crc32(repr(parts).encode())).random()


class Body:
    """A rig plus three paint layers per cube: body, glow (full bright) and cracks (tinted)."""

    def __init__(self, name, size):
        self.rig = Rig(name, size, size)
        self.paints = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, cracks=None, inflate=0.0, faces=None, tag=None,
             rotation=None, pivot=None):
        c = box(bone, origin, size, inflate=inflate, faces=faces, tag=tag, rotation=rotation, pivot=pivot)
        self.paints.append((c, material, glow, cracks))
        return c

    def build(self):
        self.rig.pack(gutter=1)
        t = Tex(self.rig.tex_w, self.rig.tex_h, 3)
        g = Tex(self.rig.tex_w, self.rig.tex_h, 4)
        k = Tex(self.rig.tex_w, self.rig.tex_h, 5)
        for c, material, glow, cracks in self.paints:
            paint_cube(t, c, material)
            if glow:
                paint_cube(g, c, glow, shading=False)
                paint_cube(t, c, lambda f, x, y, w, h, gl=glow, m=material: gl(f, x, y, w, h) or m(f, x, y, w, h))
            if cracks:
                paint_cube(k, c, cracks, shading=False)
                # In the body texture the cracks are dark seams: the glow layer fills them with light.
                paint_cube(t, c, lambda f, x, y, w, h, ck=cracks, m=material:
                           (CRACK_DIM if ck(f, x, y, w, h) else None) or m(f, x, y, w, h))
        return t, g, k


# --- materials -------------------------------------------------------------------------------

def marble(seed):
    base = noise(MARBLE, seed, 3.2, 1.0, 6.0)

    def m(face, x, y, w, h):
        # Grey veins wandering through the stone.
        n = fbm(x * 5 + len(face), y * 5, seed + 11, 40, 40, 2, 3.0)
        if 0.49 < n < 0.51:
            return MARBLE[1]
        return base(face, x, y, w, h)
    return m


def gold(seed, ramp=GOLD, base=3.0):
    b = noise(ramp, seed, base, 1.1, 4.0)

    def m(face, x, y, w, h):
        # Burnished: bright along the top edge of each face, darker toward the bottom.
        if y == 0 and h > 2:
            return ramp[5]
        if y == h - 1 and h > 2:
            return ramp[1]
        return b(face, x, y, w, h)
    return m


def cracks(seed, density=0.5):
    """Lightning-shaped cracks: a random walk from a few seeds per face."""
    cache = {}

    def m(face, x, y, w, h):
        key = (face, w, h)
        if key not in cache:
            rnd = random.Random(zlib.crc32(repr((seed, face, w, h)).encode()))
            pts = set()
            n = max(1, int(round((w * h) ** 0.5 * density / 3)))
            for _ in range(n):
                cx, cy = rnd.randrange(w), rnd.randrange(h)
                for _ in range(rnd.randint(4, 4 + w + h)):
                    pts.add((cx, cy))
                    cx += rnd.choice((-1, 0, 1, 1))
                    cy += rnd.choice((-1, 1, 1, 0))
                    if not (0 <= cx < w and 0 <= cy < h):
                        break
            cache[key] = pts
        return CRACK if (x, y) in cache[key] else None
    return m


def light_material(face, x, y, w, h):
    n = _h("light", face, x // 2, y // 2)
    return LIGHT[0] if n < 0.55 else LIGHT[1] if n < 0.85 else LIGHT[2]


def light_glow(face, x, y, w, h):
    return light_material(face, x, y, w, h)


def feather(seed, flame=0.0):
    """A white feather: darker shaft down the middle, barbs, and (for primaries) a burning tip."""
    def m(face, x, y, w, h):
        if face in ("up", "down"):
            return FEATHER[3]
        v = 3.4 + (fbm(x * 4, y * 2, seed, 32, 32, 2, 3.0) - 0.5) * 1.6
        if w >= 3 and x == w // 2:
            v -= 1.2                        # the rachis
        elif (x + y) % 3 == 0:
            v -= 0.5                        # barbs
        if y == h - 1:
            v -= 0.6
        return FEATHER[int(round(max(0, min(5, v))))]

    def glow(face, x, y, w, h):
        if flame <= 0 or face in ("up", "down"):
            return None
        start = h * (1 - flame)
        if y < start:
            return None
        k = (y - start) / max(1, h - start)
        if _h(seed, face, x, y) < 0.12:
            return None
        return FLAME[min(3, int(k * 3.2 + _h(seed, x, y, "f") * 0.8))]
    return m, glow


def eye_material(face, x, y, w, h):
    if face != "up":
        # The ball's sides: veined white fading into the gold of its setting.
        if y >= h - 2:
            return GOLD[2]
        return mix(SCLERA, hexc("#d8c8c0"), 0.4) if _h("vein", face, x, y) < 0.15 else SCLERA
    return SCLERA


def eye_glow(face, x, y, w, h):
    if face != "up":
        return None
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = math.hypot(x - cx, y - cy) * 7 / max(w, h)
    if d < 0.9:
        return IRIS[0]
    if d < 1.9:
        return IRIS[2]
    if d < 2.8:
        return IRIS[1] if _h("iris", x, y) < 0.6 else IRIS[3]
    return SCLERA


def lid_lashes(face, x, y, w, h):
    # A line of light where the lids meet, so even a closed eye reads as an eye.
    if face == "up" and (y == h - 1 or y == 0) and 1 <= x < w - 1:
        return LIGHT[3]
    return None


def rim_eyes(seed, thickness):
    """'Their rims were full of eyes': little almond eyes along a wheel's outer rim."""
    def glow(face, x, y, w, h):
        if face != "up" or w < 5:
            return None
        period = 7
        k = (y + int(_h(seed) * period)) % period
        cx = (w - 1) / 2
        if k == 2 and abs(x - cx) <= 1:
            return IRIS[1] if x == round(cx) else SCLERA
        if k in (1, 3) and abs(x - cx) <= 0.5:
            return SCLERA
        return None
    return glow


def socket_material(face, x, y, w, h):
    return SHADOW


def socket_glow(face, x, y, w, h):
    if face == "up" and _h("socket", x, y) < 0.45:
        return EMBER[int(_h("socket", y, x) * 3) % 3]
    return None


def ember_glow(seed, chance=0.2):
    def g(face, x, y, w, h):
        if face in ("up", "north", "south") and _h(seed, face, x, y) < chance:
            return EMBER[int(_h(seed, y, x) * 3) % 3]
        return None
    return g


# --- face pixel art (12 x 12, as seen from the front of each head) --------------------------

FACE_ART = {
    "man": [
        "GgGgGgGgGgGg",
        "gggggggggggg",
        "mmmmmmmmmmmm",
        "mmddmmmmddmm",
        "mmeemmmmeemm",
        "mmllmnnmllmm",
        "mmllmnnmllmm",
        "mmmlmnnmlmmm",
        "mmmmmmmmmmmm",
        "mmmmooooommm",
        "mmmmmooommmm",
        "dmmmmmmmmmmd",
    ],
    "lion": [
        "aaAaaaaaaAaa",
        "aAaammmmaaAa",
        "aammmmmmmmaa",
        "ammeemmmeema",
        "ammddmmmddma",
        "ammmmmmmmmma",
        "AammmMMMmmaA",
        "aammMdddMmaa",
        "aAmmMMMMMmAa",
        "aammmoooomaa",
        "aaAammmmaAaa",
        "aaaaAaaAaaaa",
    ],
    "ox": [
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        "mddmmmmmmddm",
        "meemmmmmmeem",
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        "mmMMMMMMMMmm",
        "mMMdMMMMdMMm",
        "mMMdMMMMdMMm",
        "mMMMMMMMMMMm",
        "mmMMooooMMmm",
        "mmmmmmmmmmmm",
    ],
    "eagle": [
        "ffFffffffFff",
        "fFffffffffFf",
        "ffffffffffff",
        "fffeeffeefff",
        "fffddffddfff",
        "ffffffffffff",
        "fffffggfffff",
        "ffffggggffff",
        "fffffggfffff",
        "fFffffffffFf",
        "ffffFffFffff",
        "fffffFFfffff",
    ],
}


def face_material(name, seed):
    art = FACE_ART[name]
    mar = marble(seed)
    fur = noise(FUR, seed + 1, 3.0, 1.2, 3.0)
    feathers = noise(FEATHER, seed + 2, 3.2, 1.0, 3.0)
    outward = {"man": "north", "lion": "west", "ox": "east", "eagle": "south"}[name]

    def m(face, x, y, w, h):
        if face == outward and w == 12 and h == 12:
            ch = art[y][x]
            if ch == "G":
                return GOLD[5]
            if ch == "g":
                return GOLD[3]
            if ch == "d":
                return MARBLE[0]
            if ch == "n":
                return MARBLE[2]
            if ch == "o":
                return SHADOW
            if ch == "M":
                return MARBLE[4]
            if ch in "aA":
                return FUR[5] if ch == "A" else fur(face, x, y, w, h)
            if ch in "fF":
                return FEATHER[1] if ch == "F" else feathers(face, x, y, w, h)
            if ch in "el":
                return MARBLE[3]
        if name == "lion":
            return fur(face, x, y, w, h)
        if name == "eagle":
            return feathers(face, x, y, w, h)
        return mar(face, x, y, w, h)

    def glow(face, x, y, w, h):
        if face == outward and w == 12 and h == 12:
            ch = art[y][x]
            if ch == "e":
                return LIGHT[0] if name == "man" else LIGHT[3]
            if ch == "l":
                return LIGHT[2]       # tears of light
        return None
    return m, glow


# --- the rig ---------------------------------------------------------------------------------

def wheel(b, root):
    """Three gyroscope wheels around the core, rims covered in eyes; real eyes ride on each."""
    eye_index = 0
    for name, r, segs, width, depth, eyes, first in WHEELS:
        wb = b.bone(f"wheel_{name}", (0, CORE_Y, 0), root.name)
        seg_len = int(math.ceil(2 * math.pi * r / segs)) + 1
        for j in range(segs):
            ang = 360.0 * j / segs
            o = (-width / 2, CORE_Y + r - depth / 2, -seg_len / 2)
            g = gold(1000 + j + segs, GOLD if j % 3 else OLD_GOLD)
            b.cube(wb, o, (width, depth, seg_len), g, rim_eyes(2000 + j + segs, width),
                   tag=f"wheel_{name}_s{j}", rotation=(ang, 0, 0), pivot=(0, CORE_Y, 0))
            if j % 2 == 0:
                # Bosses where the spokes would be, standing proud of the rim.
                b.cube(wb, (-width / 2 - 1, CORE_Y + r - depth / 2 - 1, -2), (width + 2, depth + 2, 4),
                       gold(1500 + j + segs, OLD_GOLD, 3.4), tag=f"wheel_{name}_boss{j}", rotation=(ang + 7.5, 0, 0),
                       pivot=(0, CORE_Y, 0))
        for k in range(eyes):
            theta = first + 2 * math.pi * k / eyes
            eye = b.bone(f"eye_{eye_index}", (0, CORE_Y, 0), wb.name, rotation=(math.degrees(theta), 0, 0))
            top = CORE_Y + r
            ball = b.bone(f"eye_{eye_index}_ball", (0, top, 0), eye.name)
            b.cube(ball, (-4.5, top + 0.5, -4.5), (9, 5, 9), eye_material, eye_glow, tag=f"eye{eye_index}")
            # A gold setting cradling the ball.
            b.cube(ball, (-5.5, top - 1, -5.5), (11, 2, 11), gold(3000 + eye_index), tag=f"eye{eye_index}_setting")
            lid_a = b.bone(f"eye_{eye_index}_lid_a", (0, top + 5.5, -5), eye.name)
            b.cube(lid_a, (-5, top + 5, -5), (10, 1, 5), gold(3100 + eye_index, GOLD, 3.6), lid_lashes, tag=f"eye{eye_index}_lida")
            lid_b = b.bone(f"eye_{eye_index}_lid_b", (0, top + 5.5, 5), eye.name)
            b.cube(lid_b, (-5, top + 5, 0), (10, 1, 5), gold(3200 + eye_index, GOLD, 3.6), lid_lashes, tag=f"eye{eye_index}_lidb")
            sock = b.bone(f"eye_{eye_index}_socket", (0, top, 0), eye.name)
            b.cube(sock, (-4, top + 0.5, -4), (8, 3, 8), socket_material, socket_glow, tag=f"eye{eye_index}_socket")
            eye_index += 1
    return eye_index


def core(b, turn):
    c = b.bone("core", (0, CORE_Y, 0), turn.name)
    lt = b.bone("core_light", (0, CORE_Y, 0), c.name)
    for o, s in [((-7, CORE_Y - 7, -7), (14, 14, 14)), ((-9, CORE_Y - 5, -5), (18, 10, 10)),
                 ((-5, CORE_Y - 5, -9), (10, 10, 18)), ((-5, CORE_Y - 9, -5), (10, 18, 10))]:
        b.cube(lt, o, s, light_material, light_glow, tag="core_light")
    shell = b.bone("core_shell", (0, CORE_Y, 0), c.name)
    rnd = random.Random(77)
    # Eight plates of an eggshell that has already cracked open: light shows between them.
    for i in range(8):
        yaw = i * 45 + rnd.uniform(-8, 8)
        pitch = (25 if i % 2 else -20) + rnd.uniform(-6, 6)
        b.cube(shell, (-6, CORE_Y - 6, -13), (12, 12, 3), marble(400 + i), cracks=cracks(410 + i, 0.9),
               tag=f"shell{i}", rotation=(pitch, yaw, rnd.uniform(-10, 10)), pivot=(0, CORE_Y, 0))
    b.cube(shell, (-5, CORE_Y + 9, -5), (10, 3, 10), marble(420), cracks=cracks(421, 0.9), tag="shell_cap")
    b.cube(shell, (-5, CORE_Y - 12, -5), (10, 3, 10), marble(422), cracks=cracks(423, 0.9), tag="shell_base")


def torso(b, turn):
    t = b.bone("torso", (0, 42, 0), turn.name)
    b.cube(t, (-7, 30, -7), (14, 24, 14), marble(500), cracks=cracks(501, 0.6), tag="torso")
    for y in (35, 46):
        b.cube(t, (-7.5, y, -7.5), (15, 2, 15), gold(502 + y), tag=f"band{y}")
    # Masks of the fused choir pressing out of the stone, mouths open in song.
    rnd = random.Random(503)
    for i in range(10):
        side = i % 4
        y = 32 + rnd.randint(0, 17)
        u = rnd.randint(-4, 2)

        def mask(face, x, yy, w, h, k=i):
            if face in ("north", "south", "east", "west") and w == 4 and h == 5:
                if yy == 1 and x in (0, 3):
                    return MARBLE[0]
                if yy == 3 and x in (1, 2):
                    return SHADOW
            return marble(510 + k)(face, x, yy, w, h)

        def mask_glow(face, x, yy, w, h):
            return LIGHT[2] if face in ("north", "south", "east", "west") and w == 4 and h == 5 and yy == 1 and x in (0, 3) else None
        if side == 0:
            o, s = (u, y, -8), (4, 5, 1)
        elif side == 1:
            o, s = (u, y, 7), (4, 5, 1)
        elif side == 2:
            o, s = (7, y, u), (1, 5, 4)
        else:
            o, s = (-8, y, u), (1, 5, 4)
        b.cube(t, o, s, mask, mask_glow, tag=f"mask{i}")
    # Four arms, the hands of a man under the wings on their four sides.
    for i, (sx, sz) in enumerate([(1, 1), (-1, 1), (1, -1), (-1, -1)]):
        arm = b.bone(f"arm_{i}", (sx * 7, 52, sz * 7), t.name)
        rz = -28 * sx
        rx = 18 * sz
        b.cube(arm, (sx * 7 - 1.5, 36, sz * 7 - 1.5), (3, 16, 3), marble(520 + i), cracks=cracks(524 + i, 0.4),
               tag=f"arm{i}", rotation=(rx, 0, rz), pivot=(sx * 7, 52, sz * 7))
        b.cube(arm, (sx * 7 - 2, 31, sz * 7 - 2), (4, 5, 3), marble(530 + i), tag=f"hand{i}", rotation=(rx, 0, rz),
               pivot=(sx * 7, 52, sz * 7))


def heads(b, turn):
    hd = b.bone("heads", (0, HEADS_Y, 0), turn.name)
    b.cube(hd, (-6, HEADS_Y - 6, -6), (12, 14, 12), marble(600), cracks=cracks(601, 0.5), tag="neck_cluster")
    for i, name in enumerate(FACES):
        cx, cy, cz = FACE_CENTRES[name]
        intact = b.bone(f"face_{name}_intact", (cx, cy, cz), hd.name)
        mat, glow = face_material(name, 610 + i * 10)
        if name in ("man", "eagle"):
            o, s = (cx - 6, cy - 6, cz - 5), (12, 12, 10)
        else:
            o, s = (cx - 5, cy - 6, cz - 6), (10, 12, 12)
        b.cube(intact, o, s, mat, glow, cracks(612 + i, 0.25), tag=f"face_{name}")
        jaw = b.bone(f"face_{name}_jaw", (cx, cy - 3, cz), intact.name)
        out = {"man": (0, -1), "lion": (-1, 0), "ox": (1, 0), "eagle": (0, 1)}[name]
        jx, jz = cx + out[0] * 5, cz + out[1] * 5
        if name == "lion":
            # The mane: a gold-fur collar standing out around the face.
            for k in range(10):
                a = k / 10 * math.tau
                ty, tz = cy + math.sin(a) * 7.5, cz + math.cos(a) * 7.5
                b.cube(intact, (cx - 3, ty - 2.5, tz - 2.5), (4, 5, 5), noise(FUR, 640 + k, 3.4, 1.3, 3.0),
                       tag=f"mane{k}", rotation=(math.degrees(a), 0, 0), pivot=(cx, ty, tz))
            b.cube(jaw, (jx - 3, cy - 5, jz - 3), (3, 4, 6), marble(641), tag="lion_muzzle")
        elif name == "ox":
            for sz in (-1, 1):
                b.cube(intact, (cx - 1, cy + 4, cz + sz * 6 - (1 if sz > 0 else 3) + (0 if sz > 0 else 0)), (3, 3, 4),
                       marble(650 + sz), tag=f"horn_base{sz}")
                b.cube(intact, (cx - 1, cy + 6, cz + sz * 9 - 1), (2, 6, 2), gold(652 + sz, OLD_GOLD), tag=f"horn{sz}",
                       rotation=(sz * 20, 0, 0), pivot=(cx, cy + 6, cz + sz * 9))
            b.cube(jaw, (jx, cy - 5, jz - 4), (3, 4, 8), marble(655), tag="ox_muzzle")
        elif name == "eagle":
            b.cube(jaw, (jx - 2, cy - 3, jz), (4, 3, 5), gold(660, GOLD, 3.4), tag="beak")
            b.cube(jaw, (jx - 1, cy - 5, jz + 3), (2, 3, 2), gold(661, GOLD, 2.8), tag="beak_hook")
        else:
            b.cube(intact, (cx - 6.5, cy + 4, cz - 5.5), (13, 2, 11), gold(670), tag="circlet")
            b.cube(jaw, (jx - 3, cy - 6, jz - 1), (6, 2, 2), marble(671), tag="chin")
        cracked = b.bone(f"face_{name}_cracked", (cx, cy, cz), hd.name)
        b.cube(cracked, o, s, lambda *a: CLEAR, cracks=cracks(680 + i, 1.4), inflate=0.15, tag=f"face_{name}_cracks")
        broken = b.bone(f"face_{name}_broken", (cx, cy, cz), hd.name)
        b.cube(broken, (cx - 4, cy - 6, cz - 4), (8, 5, 8), marble(690 + i), ember_glow(691 + i, 0.4), cracks(692 + i, 1.0),
               tag=f"face_{name}_stump")
        rnd = random.Random(700 + i)
        for k in range(3):
            sz = rnd.randint(2, 4)
            b.cube(broken, (cx + rnd.uniform(-7, 5), cy + rnd.uniform(-9, 4), cz + rnd.uniform(-7, 5)), (sz, sz, sz),
                   marble(710 + i * 3 + k), ember_glow(720 + k, 0.3), tag=f"face_{name}_chunk{k}",
                   rotation=(rnd.uniform(-40, 40), rnd.uniform(-40, 40), rnd.uniform(-40, 40)),
                   pivot=(cx, cy, cz))


def halo(b, turn):
    hb = b.bone("halo", (0, HALO_Y, 0), turn.name)
    missing = {2, 3, 9}
    for i in range(16):
        if i in missing:
            continue
        b.cube(hb, (-5, HALO_Y - 1.5, -23.5), (10, 3, 3), gold(800 + i, GOLD, 3.6), lambda f, x, y, w, h: LIGHT[2] if f == "up" else None,
               cracks(810 + i, 0.6), tag=f"halo{i}", rotation=(0, i * 22.5, 0), pivot=(0, HALO_Y, 0))
    for k, i in enumerate(sorted(missing)):
        sh = b.bone(f"halo_shard_{k}", (0, HALO_Y, 0), turn.name)
        b.cube(sh, (-4, HALO_Y + 2 + k * 2, -27), (8, 3, 3), gold(820 + k, GOLD, 3.6), lambda f, x, y, w, h: LIGHT[2] if f == "up" else None,
               tag=f"halo_shard{k}", rotation=(15 * (k - 1), i * 22.5 + 6, 20 - k * 15), pivot=(0, HALO_Y, 0))


def wings(b, turn):
    group = b.bone("wings", (0, CORE_Y, 0), turn.name)
    for level, side in WING_ORDER:
        sx, sy, sz, lens = WINGS[level]
        s = 1 if side == "l" else -1
        sh = (s * sx, sy, sz)
        name = f"wing_{level}_{side}"
        seed = 900 + WING_ORDER.index((level, side)) * 50
        root = b.bone(name, sh, group.name)
        segs = [root]
        x = sh[0]
        for k, seg_name in enumerate(["", "_mid", "_tip"]):
            if k > 0:
                segs.append(b.bone(name + seg_name, (x, sy, sz), segs[-1].name))
            bone = segs[-1]
            length = lens[k]
            thick = 3 if k < 2 else 2
            ox = x if s > 0 else x - length
            b.cube(bone, (ox, sy - thick / 2, sz - thick / 2), (length, thick, thick), gold(seed + k, GOLD, 3.4),
                   tag=f"{name}_arm{k}")
            fa = b.bone(f"{name}_fa{k}", (x, sy, sz), bone.name)
            fb = b.bone(f"{name}_fb{k}", (x, sy, sz), bone.name)
            n = max(3, length // 3)
            for j in range(n):
                u = (j + 0.5) / n
                fx = x + s * u * length
                along = (k + u) / 3                   # 0 at the shoulder, 1 at the wing tip
                # Coverts: short, in front.
                cl = int(9 + along * 6)
                mat, gl = feather(seed + 100 + k * 20 + j)
                b.cube(fa, (fx - 1.5, sy - cl, sz - 1.4), (3, cl, 1), mat, gl, tag=f"{name}_cov{k}_{j}",
                       rotation=(0, 0, -s * along * 12), pivot=(fx, sy, sz))
                # Flight feathers behind them, fanning out toward the tip, primaries ablaze.
                fl = int(16 + along * 16 + (2 if j % 2 else 0))
                fan = along * along * 55
                mat, gl = feather(seed + 200 + k * 20 + j, flame=0.18 + 0.3 * along if k == 2 else 0.12 * along)
                target = fb if j % 2 else fa
                b.cube(target, (fx - 1.5, sy - fl, sz - 0.2), (3, fl, 1), mat, gl, tag=f"{name}_fl{k}_{j}",
                       rotation=(0, 0, -s * fan), pivot=(fx, sy, sz))
            x += s * length
        stump = b.bone(f"{name}_stump", sh, group.name)
        b.cube(stump, (sh[0] if s > 0 else sh[0] - 7, sy - 1.5, sz - 1.5), (7, 3, 3), gold(seed + 40, OLD_GOLD, 2.4),
               ember_glow(seed + 41, 0.5), tag=f"{name}_stump")
        for j in range(3):
            mat, gl = feather(seed + 300 + j, flame=0.6)
            b.cube(stump, (sh[0] + s * (2 + j * 2) - 1, sy - 6 - j, sz - 0.5), (2, 6 + j, 1), mat, gl, tag=f"{name}_stub{j}",
                   rotation=(0, 0, -s * (15 + j * 12)), pivot=(sh[0] + s * (2 + j * 2), sy, sz))


def fragments(b, root):
    fr = b.bone("fragments", (0, CORE_Y, 0), root.name)
    rnd = random.Random(1300)
    for k in range(10):
        r = rnd.uniform(36, 62)
        a = rnd.uniform(0, 360)
        y = CORE_Y + rnd.uniform(-20, 24)
        chunk = b.bone(f"fragment_{k}", (0, CORE_Y, 0), fr.name)
        for j in range(rnd.randint(1, 3)):
            b.cube(chunk, (-4, y - 2, -r - 2 - j * 3), (8, 4, 5), gold(1310 + k * 3 + j, OLD_GOLD if j else GOLD),
                   ember_glow(1340 + k, 0.25), tag=f"frag{k}_{j}", rotation=(rnd.uniform(-30, 30), a + j * 6, rnd.uniform(-40, 40)),
                   pivot=(0, CORE_Y, 0))


def rig():
    b = Body("broken_chorus", 1024)
    root = b.bone("root", (0, 0, 0))
    turn = b.bone("body_turn", (0, CORE_Y, 0), root.name)
    core(b, turn)
    torso(b, turn)
    heads(b, turn)
    halo(b, turn)
    wings(b, turn)
    wb = b.bone("wheels", (0, CORE_Y, 0), root.name)
    wheel(b, wb)
    fragments(b, root)
    return b


# --- animations ------------------------------------------------------------------------------

E = "easeInOutSine"


def anims():
    f = AnimFile()
    idle = f.new("animation.broken_chorus.idle", 8.0, loop=True)
    idle.rot("halo", (0, [0, 0, 0]), (8.0, [0, 360, 0]))
    idle.scale("core_light", (0, [1, 1, 1]), (1.0, [1.12, 1.12, 1.12], E), (2.0, [1, 1, 1], E), (3.0, [1.08, 1.08, 1.08], E),
               (4.0, [1, 1, 1], E), (5.0, [1.12, 1.12, 1.12], E), (6.0, [1, 1, 1], E), (7.0, [1.08, 1.08, 1.08], E), (8.0, [1, 1, 1], E))
    for k in range(3):
        idle.pos(f"halo_shard_{k}", (0, [0, 0, 0]), (2.0 + k, [0, 2 + k, 0], E), (8.0, [0, 0, 0], E))
    for level, side in WING_ORDER:
        n = f"wing_{level}_{side}"
        idle.rot(n + "_mid", (0, [0, 0, 0]), (2.0, [0, 0, 6 if side == "l" else -6], E), (4.0, [0, 0, 0], E),
                 (6.0, [0, 0, 6 if side == "l" else -6], E), (8.0, [0, 0, 0], E))
        idle.rot(n + "_tip", (0, [0, 0, 0]), (2.5, [0, 0, 10 if side == "l" else -10], E), (4.5, [0, 0, 0], E),
                 (6.5, [0, 0, 10 if side == "l" else -10], E), (8.0, [0, 0, 0], E))

    for name in FACES:
        s = f.new(f"animation.broken_chorus.sing_{name}", 1.6)
        axis = {"man": [24, 0, 0], "lion": [0, 0, -24], "ox": [0, 0, 24], "eagle": [-24, 0, 0]}[name]
        s.rot(f"face_{name}_jaw", (0, [0, 0, 0]), (0.3, axis, "easeOutBack"), (1.2, axis), (1.6, [0, 0, 0], E))
        s.scale("core_light", (0, [1, 1, 1]), (0.3, [1.3, 1.3, 1.3], "easeOutBack"), (1.6, [1, 1, 1], E))

    hymn = f.new("animation.broken_chorus.hymn", 7.0)
    for name in FACES:
        axis = {"man": [20, 0, 0], "lion": [0, 0, -20], "ox": [0, 0, 20], "eagle": [-20, 0, 0]}[name]
        hymn.rot(f"face_{name}_jaw", (0, [0, 0, 0]), (0.6, axis, E), (6.4, axis), (7.0, [0, 0, 0], E))
    hymn.scale("halo", (0, [1, 1, 1]), (1.0, [1.25, 1, 1.25], E), (2.0, [1.1, 1, 1.1], E), (3.5, [1.3, 1, 1.3], E),
               (5.0, [1.1, 1, 1.1], E), (6.2, [1.35, 1, 1.35], E), (7.0, [1, 1, 1], E))
    hymn.scale("core_light", (0, [1, 1, 1]), (6.0, [1.45, 1.45, 1.45], "easeInQuad"), (7.0, [1, 1, 1], "easeOutQuad"))

    kneel = f.new("animation.broken_chorus.kneel", 5.0)
    kneel.scale("core_light", (0, [1, 1, 1]), (0.2, [0.6, 0.6, 0.6], E), (0.6, [1.1, 1.1, 1.1]), (0.8, [0.7, 0.7, 0.7]),
                (4.2, [0.8, 0.8, 0.8], E), (5.0, [1, 1, 1], E))
    kneel.rot("halo", (0, [0, 0, 0]), (0.4, [14, 0, -10], "easeOutBack"), (4.4, [10, 0, -8]), (5.0, [0, 0, 0], E))
    for level, side in WING_ORDER:
        n = f"wing_{level}_{side}"
        kneel.rot(n + "_tip", (0, [0, 0, 0]), (0.5, [0, 0, -30 if side == "l" else 30], E), (4.4, [0, 0, -30 if side == "l" else 30]),
                  (5.0, [0, 0, 0], E))

    dive = f.new("animation.broken_chorus.dive", 2.0)
    for level, side in WING_ORDER:
        n = f"wing_{level}_{side}"
        dive.rot(n + "_mid", (0, [0, 0, 0]), (0.6, [0, 30 if side == "l" else -30, 0], E), (1.6, [0, 30 if side == "l" else -30, 0]),
                 (2.0, [0, 0, 0], E))

    for to, length in ((2, 8.0), (3, 10.0), (4, 12.0)):
        t = f.new(f"animation.broken_chorus.transform_{to}", length)
        t.scale("core_light", (0, [1, 1, 1]), (length * 0.4, [1.6, 1.6, 1.6], "easeInQuad"), (length * 0.5, [0.5, 0.5, 0.5], "easeOutQuad"),
                (length, [1, 1, 1], E))
        t.rot("halo", (0, [0, 0, 0]), (length, [0, 720, 0], E))
        for k in range(3):
            t.pos(f"halo_shard_{k}", (0, [0, 0, 0]), (length * 0.5, [0, 6 + k * 2, 0], E), (length, [0, 0, 0], E))

    em = f.new("animation.broken_chorus.emerge", 9.0, hold=True)
    em.scale("root", (0, [0.2, 0.2, 0.2]), (6.0, [1.05, 1.05, 1.05], "easeOutQuad"), (9.0, [1, 1, 1], E))
    em.scale("halo", (0, [0, 0, 0]), (6.0, [0, 0, 0]), (7.0, [1.3, 1.3, 1.3], "easeOutBack"), (9.0, [1, 1, 1], E))

    fi = f.new("animation.broken_chorus.final_idle", 12.0, loop=True)
    fi.rot("fragments", (0, [0, 0, 0]), (12.0, [0, 360, 0]))
    for k in range(10):
        fi.pos(f"fragment_{k}", (0, [0, 0, 0]), (3.0 + k * 0.4, [0, 3 if k % 2 else -3, 0], E), (12.0, [0, 0, 0], E))
    fi.scale("core_light", (0, [1.2, 1.2, 1.2]), (0.5, [1.35, 1.35, 1.35], E), (1.0, [1.2, 1.2, 1.2], E), (6.0, [1.2, 1.2, 1.2]),
             (6.5, [1.4, 1.4, 1.4], E), (7.0, [1.2, 1.2, 1.2], E), (12.0, [1.2, 1.2, 1.2]))

    death = f.new("animation.broken_chorus.death", 20.0, hold=True)
    death.scale("core_light", (0, [1.2, 1.2, 1.2]), (14.0, [2.2, 2.2, 2.2], "easeInQuad"), (17.0, [0.2, 0.2, 0.2], "easeInQuad"),
                (20.0, [0, 0, 0]))
    death.pos("fragments", (0, [0, 0, 0]), (16.0, [0, -24, 0], "easeInQuad"), (20.0, [0, -40, 0]))
    death.scale("fragments", (0, [1, 1, 1]), (16.0, [1.4, 0.8, 1.4], E), (20.0, [0, 0, 0]))
    death.pos("halo", (0, [0, 0, 0]), (10.0, [0, -10, 0], E), (20.0, [0, -60, 0], "easeInQuad"))
    return f


TRIGGERED = ["sing_man", "sing_lion", "sing_ox", "sing_eagle", "hymn", "kneel", "dive", "transform_2", "transform_3",
             "transform_4", "emerge", "death"]


# --- the Choir Echo ---------------------------------------------------------------------------

def echo_rig():
    b = Body("choir_echo", 128)
    root = b.bone("root", (0, 0, 0))
    spin = b.bone("ring", (0, 8, 0), root.name)
    for j in range(10):
        b.cube(spin, (-1.5, 8 + 7, -2.5), (3, 2, 5), gold(1400 + j), tag=f"echo_seg{j}", rotation=(j * 36, 0, 0),
               pivot=(0, 8, 0))
    eye = b.bone("eye", (0, 8, 0), root.name)

    def iris(face, x, y, w, h):
        if face in ("north", "south"):
            return eye_glow("up", x, y, w, h)
        return None
    b.cube(eye, (-2.5, 5.5, -2), (5, 5, 4), eye_material, iris, tag="echo_eye")
    for s in (1, -1):
        wing = b.bone(f"wing_{'l' if s > 0 else 'r'}", (s * 2, 9, 0), root.name)
        mat, gl = feather(1450 + s, flame=0.4)
        b.cube(wing, (s * 2 if s > 0 else s * 2 - 6, 8, -0.5), (6, 4, 1), mat, gl, tag=f"echo_wing{s}",
               rotation=(0, 0, s * 20), pivot=(s * 2, 9, 0))
    return b


def echo_anims():
    f = AnimFile()
    idle = f.new("animation.choir_echo.idle", 2.0, loop=True)
    idle.rot("ring", (0, [0, 0, 0]), (2.0, [360, 0, 0]))
    idle.rot("wing_l", (0, [0, 0, 0]), (0.5, [0, 0, -25], E), (1.0, [0, 0, 0], E), (1.5, [0, 0, -25], E), (2.0, [0, 0, 0], E))
    idle.rot("wing_r", (0, [0, 0, 0]), (0.5, [0, 0, 25], E), (1.0, [0, 0, 0], E), (1.5, [0, 0, 25], E), (2.0, [0, 0, 0], E))
    sing = f.new("animation.choir_echo.sing", 1.0)
    sing.scale("eye", (0, [1, 1, 1]), (0.2, [1.4, 1.4, 1.4], "easeOutBack"), (1.0, [1, 1, 1], E))
    return f


# --- the Seraph Wings (a Curios trinket for the back) ---------------------------------------

# level -> (shoulder |x|, y, arm length, feather lengths); origin is the middle of the upper back
SERAPH = {"top": (1.5, 1, 10, (6, 9)), "mid": (2, -2, 9, (5, 8)), "low": (1.5, -5, 7, (4, 6))}


def seraph_rig():
    b = Body("seraph_wings", 128)
    root = b.bone("back", (0, 0, 0))
    for level, side in WING_ORDER:
        sx, sy, arm, (cov, fl) = SERAPH[level]
        s = 1 if side == "l" else -1
        name = f"wing_{level}_{side}"
        bone = b.bone(name, (s * sx, sy, 2.5), root.name)
        seed = 1600 + WING_ORDER.index((level, side)) * 30
        ox = s * sx if s > 0 else s * sx - arm
        b.cube(bone, (ox, sy - 0.5, 2), (arm, 1, 1), gold(seed, GOLD, 3.4), tag=f"sw_{name}_arm")
        n = arm // 2 + 1
        for j in range(n):
            u = (j + 0.5) / n
            fx = s * sx + s * u * arm
            m1, g1 = feather(seed + 10 + j)
            b.cube(bone, (fx - 1, sy - cov, 2.2), (2, cov, 0), m1, g1, faces=("north", "south"), tag=f"sw_{name}_c{j}",
                   rotation=(0, 0, -s * u * 10), pivot=(fx, sy, 2.2))
            m2, g2 = feather(seed + 20 + j, flame=0.2 + 0.35 * u)
            length = int(fl + u * 4)
            b.cube(bone, (fx - 1, sy - length, 2.6), (2, length, 0), m2, g2, faces=("north", "south"), tag=f"sw_{name}_f{j}",
                   rotation=(0, 0, -s * u * u * 45), pivot=(fx, sy, 2.6))
    return b


def seraph_anims():
    f = AnimFile()
    f.new("animation.seraph_wings.idle", 2.0, loop=True)
    return f


def seraph_sprite():
    """The inventory icon: three pairs of white wings with burning tips, seen from behind."""
    from common import art
    import palette as P
    pal = {"o": P.OUTLINE, "w": FEATHER[5], "W": FEATHER[3], "s": FEATHER[1], "f": FLAME[1], "F": FLAME[2], "g": GOLD[4]}
    return art([
        "................",
        "ff.o........o.ff",
        "Fwwo........owwF",
        ".Wwwo......owwW.",
        "..sWwo.gg.owWs..",
        "fFwwwwogg owwwFf",
        "..sWwwwggwwwWs..",
        "....ssWggWss....",
        "..ffWwwggwwWff..",
        ".FWwws.gg.swwWF.",
        "..Wwso....oswW..",
        "...so......os...",
        "................",
        "................",
        "................",
        "................",
    ], pal)


# --- output ----------------------------------------------------------------------------------

def generate():
    b = rig()
    t, g, k = b.build()
    b.rig.write(GEO + "broken_chorus.geo.json")
    save(t, "entity", "broken_chorus")
    save(g, "entity", "broken_chorus_glowmask")
    save(k, "entity", "broken_chorus_cracks")
    a = anims()
    a.write(ANIM + "broken_chorus.animation.json")
    with open(GEO + "broken_chorus.names.txt", "w") as fh:
        fh.write("\n".join(n.split(".")[-1] for n in a.names()) + "\n")

    e = echo_rig()
    et, eg, _ = e.build()
    e.rig.write(GEO + "choir_echo.geo.json")
    save(et, "entity", "choir_echo")
    save(eg, "entity", "choir_echo_glowmask")
    echo_anims().write(ANIM + "choir_echo.animation.json")

    w = seraph_rig()
    wt, wg, _ = w.build()
    w.rig.write(GEO + "seraph_wings.geo.json")
    save(wt, "entity", "seraph_wings")
    seraph_anims().write(ANIM + "seraph_wings.animation.json")
    save(seraph_sprite(), "item", "seraph_wings")


if __name__ == "__main__":
    generate()


# --- procedural pose (mirrors ChorusGeometry, for previews) -----------------------------------

WHEEL_TILT_Y = [0.0, 1.05, 2.1]
WHEEL_TILT_Z = [0.25, -0.35, 0.5]
WING_LIFT = {"top": 0.55, "mid": 0.05, "low": -0.6}


def preview_pose(t=0.0, spins=(0.0, 1.0, 2.0)):
    """Bone rotations the renderer would set at game time t, as Bedrock degrees for preview3d."""
    pose = {}

    def put(bone, rx, ry, rz):
        pose[bone] = {"rotation": [-math.degrees(rx), -math.degrees(ry), math.degrees(rz)]}
    for i, (name, *_rest) in enumerate(WHEELS):
        put(f"wheel_{name}", spins[i], WHEEL_TILT_Y[i] + 0.35 * math.sin(0.011 * t + i * 2.1),
            WHEEL_TILT_Z[i] + 0.25 * math.sin(0.017 * t + i * 1.3))
    for level, side in WING_ORDER:
        lift = WING_LIFT[level] + 0.15 * math.sin(0.12 * t + (0 if level == "top" else 1 if level == "mid" else 2))
        put(f"wing_{level}_{side}", 0, 0, -lift if side == "l" else lift)
    return pose


def preview(path, t=0.0, hidden_extra=(), scale=3, views=("front", "three_quarter", "side", "back")):
    import preview3d
    b = rig()
    tex, _, _ = b.build()
    hidden = [f"face_{n}_cracked" for n in FACES] + [f"face_{n}_broken" for n in FACES] + \
             [f"eye_{i}_socket" for i in range(12)] + ["fragments"] + [f"wing_{l}_{s}_stump" for l, s in WING_ORDER]
    preview3d.render(b.rig, tex, path, pose=preview_pose(t), hidden=tuple(hidden) + tuple(hidden_extra), scale=scale, views=views)
