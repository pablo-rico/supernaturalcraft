"""Amara, the Darkness: phase I's mass (model, texture, glowmask, animations) and the art of her arena.

The mass is the Darkness before Creation: a lump of living night around a black-sun ``core`` (a sphere of nothing
inside a crossed corona), wrapped in six ``shell_*`` plates of lumpy night that open when the core is exposed, the
seams filled by ``corners``. Layered smoke veils drift off her (``veil`` hangs below, ``veil_skirt`` rings her waist,
``veil_crown`` rises from her top) and ``motes`` of night orbit her. Four ``ring_*`` of glyph-cut obsidian are built
around the origin (the Java model moves them onto their hit boxes every frame: never animate their position). Six
``tentacle_*`` reach the ground, each in three bending segments (``tentacle_<i>`` > ``_mid`` > ``_tip``), and end
in a glowing ``cyst_*`` (a child of ``tentacle_<i>`` itself, so a bending tip never drags it off its hit box).
``form`` is her last shape and is also drawn alone, small, for her shades and Chuck's ink echo: a slender woman of
night in a long dress, crowned, with a black-sun halo (``form_halo``, hidden by the shades and the echo).

Frozen bones (AmaraAssets.MASS_FROZEN) keep their names and parents; added bones live under ``mass``, ``ring_*``,
``tentacle_*`` or ``form`` so every renderer that hides those hides them too:
    mass > veil_skirt, veil_crown, motes_inner, core > core_corona
    ring_<i> > ring_<i>_eye
    tentacle_<i> > tentacle_<i>_mid > tentacle_<i>_tip
    form > form_skirt

Coordinates are Bedrock pixels with the origin at her feet; the renderer scales the mass by SCALE, so world sizes in
AmaraEntity are pixels * SCALE / 16. GeckoLib mirrors X and the renderer turns her 180 degrees (she keeps yaw 0), so a
world offset (dx, dz) is Bedrock (dx, -dz). Each tentacle is built pointing north in its own frame and turned onto its
spoke by a rest yaw, so its clips bend it in that frame: -X lifts it, +X drives it down, Y swings it round the mass.
"""

import json
import math
import random
import zlib

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import noise, paint_cube
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

VOID = Ramp("#020105", "#05030b", "#0b0716", "#130c24", "#1e1336", "#2e1d50")
NEBULA = [hexc("#24104a"), hexc("#3a1a6e"), hexc("#4c1858"), hexc("#16285a")]
OBSIDIAN = Ramp("#050309", "#0d0915", "#171124", "#231a38", "#342852", "#4a3a72")
CYST = Ramp("#12040e", "#2a0820", "#4d0f3a", "#7a1a5e", "#a8358a", "#e07ad0")
SMOKE = Ramp("#07050c", "#0f0b18", "#181226", "#231a36", "#30244a", "#40325f")
STAR = [hexc("#ffffff"), hexc("#efe8ff"), hexc("#cdb8ff"), hexc("#a98cff")]
VIOLET = hexc("#b48cff")
VIOLET_HI = hexc("#d9c4ff")
RIM = hexc("#5a33a6")
RIM_HI = hexc("#8a5ce0")
CORONA = [hexc("#fffaf0"), hexc("#ffe8b0"), hexc("#ffc977"), hexc("#e6b8ff"), hexc("#9a6ae8")]
GOLD_SEAM = [hexc("#ffeec2"), hexc("#ffc56a"), hexc("#c9803a")]
BLACK = hexc("#000000")
CLEAR = (0, 0, 0, 0)
# The palette of the old mass, kept for the trophy and the tentacle effect texture (they did not change).
OLD_VOID = Ramp("#020105", "#06040c", "#0d0818", "#170e29", "#24153d", "#3a2160")
OLD_STAR = [hexc("#ffffff"), hexc("#e8deff"), hexc("#c8b4ff"), hexc("#b48cff")]
OLD_CORONA = [hexc("#fff8ec"), hexc("#ffe9b8"), hexc("#e0c8ff")]
OLD_RIM = hexc("#4a2a86")

SCALE = 2.2                    # AmaraEntity.MODEL_SCALE: the renderer draws the mass this much larger
MASS_C = 5.28 * 16 / SCALE     # AmaraEntity.MASS_CENTER, in model pixels
CYST_RING = 6.5 * 16 / SCALE   # AmaraEntity.CYST_RING, in model pixels
ANCHOR_ORBIT = 7.5             # AmaraEntity.ANCHOR_ORBIT (blocks), for the previews only

TENTACLE_ROOT = 14.0           # spoke radius of a tentacle's pivot
TENTACLE_Y = MASS_C - 16       # its height


def _h(*parts):
    """A stable pseudo-random number in [0, 1) from any hashable parts."""
    return random.Random(zlib.crc32(repr(parts).encode())).random()


def tentacle_angle(i):
    """World angle of spoke i (AmaraEntity.partOffset: i * 60 + 30 degrees, measured from +x toward +z)."""
    return i * math.pi / 3 + math.pi / 6


def tentacle_yaw(i):
    """The rest yaw (Bedrock degrees) that turns a tentacle built pointing north onto spoke i."""
    return math.degrees(tentacle_angle(i)) - 90


class Body:
    """Rig plus per-cube materials and glow, painted once packed."""

    def __init__(self, name, w, h=None):
        self.rig = Rig(name, w, h or w)
        self.paints = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, inflate=0.0, faces=None, tag=None, rotation=None, pivot=None):
        c = box(bone, origin, size, inflate=inflate, faces=faces, tag=tag, rotation=rotation, pivot=pivot)
        self.paints.append((c, material, glow))
        return c

    def build(self):
        self.rig.pack(gutter=1)
        t = Tex(self.rig.tex_w, self.rig.tex_h, 3)
        g = Tex(self.rig.tex_w, self.rig.tex_h, 4)
        for c, material, glow in self.paints:
            paint_cube(t, c, material)
            if glow:
                paint_cube(g, c, lambda f, x, y, w, h, gl=glow, m=material: _glow_px(gl, m, f, x, y, w, h), shading=False)
                paint_cube(t, c, lambda f, x, y, w, h, gl=glow, m=material: _over(gl(f, x, y, w, h), m(f, x, y, w, h)))
        return t, g


def _over(glow, base):
    """The glow wins where it shows, but never paints a texel the material leaves clear (cut-out holes stay holes)."""
    if base is None or base[3] == 0:
        return base
    return glow or base


def _glow_px(glow, material, f, x, y, w, h):
    base = material(f, x, y, w, h)
    if base is None or base[3] == 0:
        return None
    return glow(f, x, y, w, h)


# --- materials -----------------------------------------------------------------------------

def smoothstep(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def night(seed, density=0.018, rim=0.3, nebula=0.7, seam=False):
    """Void-black, lit faintly from above by her corona (the top of every lump a step brighter, its underside lost),
    a slow nebula drifting through it (violet, a breath of magenta and blue), stars caught in it (a few big ones with
    a cross glint) and a broken violet rim along the top edges, so her lumps and silhouette still read against an
    eclipsed sky. seam: a jagged crack of the black sun's light across that (outward) face."""
    off = seed * 13

    def base(face, x, y, w, h):
        n = fbm(x * 2 + off % 17, y * 2 + len(face), seed, 64, 64, 3, 4.0)
        v = 1.2 + n * 1.8
        if face in ("north", "south", "east", "west") and h > 2:
            v += 0.9 - 1.6 * (y / (h - 1))          # lit from above
        elif face == "up":
            v += 0.8
        else:
            v -= 1.0
        c = VOID[int(round(max(0, min(5, v))))]
        m = fbm(x * 1.5 + 5, y * 1.5 + off % 11, seed + 7, 64, 64, 2, 2.5)
        band = smoothstep(0.55, 0.78, m) * nebula
        if band > 0 and face != "down":
            neb = NEBULA[int(_h(seed, face) * len(NEBULA)) % len(NEBULA)]
            c = mix(c, neb, band * 0.6)
        return c

    def glow(face, x, y, w, h):
        r = _h(seed, face, x, y)
        if r < density:
            return STAR[1 + int(r / density * 3) % 3]
        if w >= 8 and h >= 8 and _h(seed, face, x // 6, y // 6, "big") < 0.06:
            # A big star with a cross glint, one in a few 6x6 cells.
            cx, cy = x % 6 - 3, y % 6 - 3
            if cx == 0 and cy == 0:
                return STAR[0]
            if cx == 0 and abs(cy) == 1 or cy == 0 and abs(cx) == 1:
                return STAR[3]
        if seam and face == seam and w > 6 and h > 6:
            s = _seam(seed, x, y, w, h)
            if s:
                return s
        if face in ("north", "south", "east", "west") and w > 3 and h > 3 and y == 0:
            if _h(seed, face, x // 2, "rim") < rim * 2:
                return RIM_HI if _h(seed, face, x, "hi") < 0.3 else RIM
        if face == "up" and w > 3 and h > 3 and (x in (0, w - 1) or y in (0, h - 1)) and _h(seed, face, x, y, "rim") < rim:
            return RIM
        return None
    return base, glow


def _seam(seed, x, y, w, h):
    """A crack of light: a jagged line wandering across the face, gold-white at its heart."""
    rng = random.Random(seed * 7 + w * 31 + h)
    y0 = rng.uniform(0.3, 0.7) * h
    slope = rng.uniform(-0.4, 0.4)
    yy = y0 + slope * (x - w / 2) + (fbm(x * 4, seed, seed + 3, 64, 64, 2, 4.0) - 0.5) * h * 0.5
    d = abs(y - yy)
    if 0.12 * w < x < 0.88 * w:
        if d < 0.6:
            return GOLD_SEAM[0]
        if d < 1.3:
            return GOLD_SEAM[1] if _h(seed, x, y, "s") < 0.7 else CORONA[3]
    return None


def obsidian_glyphs(seed):
    """Glassy black stone, a cold sheen across it, glyphs cut in and burning violet on the broad faces."""
    def base(face, x, y, w, h):
        n = fbm(x * 2, y * 2 + len(face), seed, 48, 48, 2, 3.0)
        v = 1.5 + (n - 0.5) * 1.4
        if (x + y) % 11 == 0:
            v += 1.5           # a glassy sheen streak
        if face == "up":
            v += 0.6
        return OBSIDIAN[int(round(v))]

    def glyphs(face, x, y, w, h):
        if face in ("north", "south") and w >= 5:
            k = (x + seed) % 5
            g = int(_h(seed, face, x // 5) * 6)
            # Little runes, one per five texels: a vertical stroke and a hook that differ per rune.
            if h >= 3 and y == h // 2 and k in (1, 2, 3) and g % 2 == 0:
                return VIOLET
            if h >= 3 and k == 2 and (y == h // 2 - 1 and g in (1, 2, 3) or y == h // 2 + 1 and g in (3, 4, 5)):
                return VIOLET_HI if g == 3 else VIOLET
            if k == 2 and g == 1 and y == h // 2:
                return VIOLET_HI
        if x == 0 or x == w - 1:
            return RIM if _h(seed, face, y) < 0.5 else None
        return None
    return base, glyphs


def core_material(face, x, y, w, h):
    """The black sun: nothing at all, the faintest violet breath at the edge."""
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = math.hypot(x - cx, y - cy) / (max(w, h) / 2)
    return mix(BLACK, hexc("#0c0618"), smoothstep(0.6, 1.0, d))


def core_glow(face, x, y, w, h):
    """A few motes of light caught on the black sun, nothing else: the corona round it does the burning."""
    return CORONA[3] if _h("core", face, x, y) < 0.01 else None


def corona_plane(seed, inner, outer):
    """A disc of corona around a hole the size of the black sun: streamers flaring outward in long thin tongues,
    white-gold at the inner edge, rose-violet at the tips; the hole and the gaps are cut out."""
    def mat(face, x, y, w, h):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = x - cx, y - cy
        d = math.hypot(dx, dy)
        if d < inner:
            return CLEAR
        a = math.atan2(dy, dx)
        # Streamer length per angle: a few long tongues over a ragged ring.
        k = (a / math.tau + 0.5) * 24
        s = fbm(k * 4, seed, seed, 96, 32, 2, 6.0)
        tongue = inner + 2.0 + (outer - inner - 2.0) * (s ** 0.7) * (1.0 if int(k) % 3 == 0 else 0.5 if int(k) % 3 == 1 else 0.3)
        if d > tongue:
            return CLEAR
        t = (d - inner) / max(1.0, tongue - inner)
        if t < 0.25:
            return CORONA[0]
        if t < 0.5:
            return CORONA[1]
        if t < 0.75:
            return CORONA[2] if (x + y) % 2 else CORONA[1]
        return CORONA[3] if t < 0.92 else CORONA[4]
    return mat, lambda face, x, y, w, h: mat(face, x, y, w, h)


def cyst_material(seed):
    def base(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 48, 48, 2, 3.0)
        v = 1.6 + (n - 0.5) * 1.6
        if face in ("north", "south", "east", "west") and h > 2:
            v += 0.6 - 1.2 * y / (h - 1)
        elif face == "up":
            v += 0.5
        return CYST[int(round(max(0, min(4, v))))]

    def glow(face, x, y, w, h):
        # Veins of sick light under the skin, brightest along their spine.
        n = fbm(x * 4, y * 4, seed + len(face), 32, 32, 2, 2.5)
        if 0.48 < n < 0.52:
            return CYST[5]
        if 0.45 < n < 0.55:
            return CYST[4]
        return None
    return base, glow


def maw_glow(seed):
    """The cyst's crown: a slit eye of magenta light."""
    base, veins = cyst_material(seed)

    def glow(face, x, y, w, h):
        if face == "up":
            cx, cy = (w - 1) / 2, (h - 1) / 2
            dx, dy = abs(x - cx), abs(y - cy)
            if dx < 0.8 and dy < h * 0.42:
                return hexc("#fff0fb")
            if dx < 1.8 and dy < h * 0.32:
                return CYST[5]
        return veins(face, x, y, w, h)
    return base, glow


def smoke(seed, fade="down", rim=0.4):
    """A ragged sheet of smoke for the cut-out renderer: dense near its root, torn into strands and holes toward its
    free edge (fade = the free edge), a dim violet edge where light catches it."""
    def alpha(x, y, w, h):
        if fade == "down":
            t = y / max(1, h - 1)
            col = x / max(1, w - 1)
        elif fade == "up":
            t = 1 - y / max(1, h - 1)
            col = x / max(1, w - 1)
        else:  # outward: both ends of x
            t = abs(x - (w - 1) / 2) / max(1, (w - 1) / 2)
            col = y / max(1, h - 1)
        n = fbm(x * 5 + seed, y * 2 + seed * 3, seed, 64, 64, 3, 6.0)
        strand = fbm(col * 40 + seed, 3, seed + 1, 40, 8, 2, 8.0)
        keep = 1.15 - t * 1.25 + (n - 0.5) * 0.9 + (strand - 0.5) * 1.1 * t
        side = min(x, w - 1 - x) / max(1, w / 2) if fade in ("down", "up") else 1
        keep -= (1 - min(1, side * 3)) * 0.5
        return keep > 0.25

    def mat(face, x, y, w, h):
        if not alpha(x, y, w, h):
            return CLEAR
        n = fbm(x * 3 + seed, y * 3, seed + 5, 48, 48, 3, 5.0)
        v = 1.2 + (n - 0.5) * 2.4
        if fade == "down":
            v += 0.8 - y / max(1, h - 1) * 1.4
        elif fade == "up":
            v += y / max(1, h - 1) * 0.8 - 0.4
        return SMOKE[int(round(max(0, min(5, v))))]

    def glow(face, x, y, w, h):
        if not alpha(x, y, w, h):
            return None
        edge = any(not alpha(x + dx, y + dy, w, h) if 0 <= x + dx < w and 0 <= y + dy < h else True
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and _h(seed, x, y, "e") < rim:
            return RIM
        if _h(seed, x, y, "st") < 0.004:
            return STAR[2]
        return None
    return mat, glow


def form_night(seed, density=0.02, rim=0.35, y0=None, span=80.0):
    """Her skin and dress: night without the mass's top light (so stacked pieces never band), a little nebula, stars,
    a clean violet contour down the sides so the silhouette reads small. y0: the piece's top in model pixels, for a
    gentle body-wide gradient (lighter at the shoulders, lost at the hem)."""
    def base(face, x, y, w, h):
        n = fbm(x * 2 + seed % 13, y * 2 + len(face), seed, 64, 64, 2, 3.0)
        v = 1.4 + (n - 0.5) * 1.2
        if y0 is not None and face in ("north", "south", "east", "west"):
            v += ((y0 - y) / span - 0.45) * 1.6
        if face == "up":
            v += 0.4
        elif face == "down":
            v -= 0.8
        c = VOID[int(round(max(0, min(5, v))))]
        m = fbm(x * 1.5, y * 1.5 + seed % 7, seed + 3, 64, 64, 2, 2.5)
        if m > 0.62 and face != "down":
            c = mix(c, NEBULA[seed % len(NEBULA)], min(1, (m - 0.62) * 3) * 0.35)
        return c

    def glow(face, x, y, w, h):
        r = _h(seed, face, x, y)
        if r < density:
            return STAR[1 + int(r / density * 3) % 3]
        if face in ("north", "south") and (x == 0 or x == w - 1) and h > 2 and w > 1:
            return RIM if _h(seed, face, x, y, "frim") < rim else None
        return None
    return base, glow


def dress(seed, y0, hem=False):
    """The dress: night with long vertical folds, the hem torn into smoke at the bottom tier."""
    base, glow = form_night(seed, 0.012, 0.6, y0=y0)

    def mat(face, x, y, w, h):
        if hem and face in ("north", "south", "east", "west"):
            n = fbm(x * 6 + seed, 1, seed, 64, 8, 2, 8.0)
            if y > h - 1 - int(n * 3) and _h(seed, face, x, y) < 0.75:
                return CLEAR
        c = base(face, x, y, w, h)
        if face in ("north", "south", "east", "west") and w > 4:
            fold = (x * 7 // max(1, w)) % 2
            c = mix(c, VOID[0], 0.35) if fold == 0 and x % 2 == 0 else c
        return c
    return mat, glow


# --- geometry helpers ----------------------------------------------------------------------

def blob(b, bone, ctr, r, mat, glow, tag, rot=None, k=0.7):
    """A rounded lump: three crossed boxes (a chamfered ball), optionally turned as one about its centre."""
    rx, ry, rz = r
    parts = [(2 * rx, 2 * ry * k, 2 * rz * k), (2 * rx * k, 2 * ry, 2 * rz * k), (2 * rx * k, 2 * ry * k, 2 * rz)]
    for j, (sx, sy, sz) in enumerate(parts):
        sx, sy, sz = max(1, round(sx)), max(1, round(sy)), max(1, round(sz))
        o = (ctr[0] - sx / 2, ctr[1] - sy / 2, ctr[2] - sz / 2)
        b.cube(bone, o, (sx, sy, sz), mat, glow, tag=f"{tag}_{j}", rotation=rot, pivot=ctr if rot else None)


def plane(b, bone, ctr, w, h, mat, glow, tag, rot=(0, 0, 0)):
    """A sheet (one double-sided face) w x h centred at ctr, turned about its centre: smoke, corona, veils."""
    o = (ctr[0] - w / 2, ctr[1] - h / 2, ctr[2])
    b.cube(bone, o, (w, h, 0), mat, glow, faces=("north",), tag=tag, rotation=rot, pivot=ctr)


def _rand_rot(rng, amt):
    return (rng.uniform(-amt, amt), rng.uniform(-amt, amt), rng.uniform(-amt, amt))


SHELL_DIRS = {
    "shell_n": ((0, 0, -1), "north"),
    "shell_s": ((0, 0, 1), "south"),
    "shell_e": ((1, 0, 0), "east"),
    "shell_w": ((-1, 0, 0), "west"),
    "shell_up": ((0, 1, 0), "up"),
    "shell_down": ((0, -1, 0), "down"),
}


def _perp(d):
    """Two unit vectors across a shell's face."""
    if d[1] != 0:
        return (1, 0, 0), (0, 0, 1)
    if d[0] != 0:
        return (0, 1, 0), (0, 0, 1)
    return (1, 0, 0), (0, 1, 0)


# --- the rig -------------------------------------------------------------------------------

def rig():
    b = Body("amara", 512, 1024)
    b.bone("root", (0, 0, 0))
    b.bone("mass", (0, MASS_C, 0), "root")
    c = MASS_C
    C = (0, c, 0)

    _core(b, c)
    _shells(b, c)

    _veils(b, c)
    _motes(b, c)
    _rings(b)
    for i in range(6):
        _tentacle(b, i)
    form_bones(b)
    return b


def _core(b, c):
    core = b.bone("core", (0, c, 0), "mass")
    blob(b, core, (0, c, 0), (6.5, 6.5, 6.5), core_material, core_glow, "core", k=0.72)
    # The corona: crossed discs of flame round the black sun, so it burns from every side.
    corona = b.bone("core_corona", (0, c, 0), "core")
    # Three, one square to each pair of opposite plates: through each opening one ring shows whole, the other two
    # edge-on (and so invisible).
    for j, rot in enumerate([(0, 0, 0), (0, 90, 0), (90, 0, 0)]):
        mat, glow = corona_plane(600 + j, 6.0, 14.5)
        plane(b, corona, (0, c, 0), 31, 31, mat, glow, f"corona{j}", rot=rot)


def _shells(b, c):
    """Six plates under a crust of lumps: lumps round a sphere (a Fibonacci spiral), each joining the plate it faces
    or, if it sits between plates, the corners."""
    bones = {}
    for i, (name, (d, outward)) in enumerate(SHELL_DIRS.items()):
        dx, dy, dz = d
        centre = (dx * 14, c + dy * 14, dz * 14)
        bones[name] = b.bone(name, centre, "mass")
        r = [9.5, 9.5, 9.5]
        r[[abs(dx), abs(dy), abs(dz)].index(1)] = 5.0
        m, st = night(10 + i, seam=outward if name != "shell_down" else False)
        blob(b, bones[name], centre, tuple(r), m, st, name, k=0.74)
    corners = b.bone("corners", (0, c, 0), "mass")
    rng = random.Random(40)

    def lumps(n, radius, size, tag, squash=1.0):
        golden = math.pi * (3 - math.sqrt(5))
        for k in range(n):
            yv = 1 - (k + 0.5) / n * 2
            rr = math.sqrt(1 - yv * yv)
            th = golden * k + 0.7
            d = (math.cos(th) * rr, yv * squash, math.sin(th) * rr)
            ax = max(range(3), key=lambda j: abs(d[j]))
            dominant = abs(d[ax]) / math.sqrt(sum(v * v for v in d))
            sz = rng.uniform(*size)
            rad = radius + rng.uniform(-1.2, 1.2)
            if dominant > 0.72:
                name = [("shell_w", "shell_e"), ("shell_down", "shell_up"), ("shell_n", "shell_s")][ax][d[ax] > 0]
                bone = bones[name]
            else:
                # Between the plates: pushed out and smaller, so an open shell bares the core.
                bone = corners
                rad += 3.0
                sz *= 0.75
            ctr = (d[0] * rad, c + d[1] * rad, d[2] * rad)
            m, st = night(60 + k * 3 + len(tag), rim=0.35)
            blob(b, bone, ctr, (sz, sz * rng.uniform(0.75, 1.0), sz), m, st, f"{tag}{k}", rot=_rand_rot(rng, 30))
    for i, (sx, sy, sz) in enumerate([(sx, sy, sz) for sx in (-1, 1) for sy in (-1, 1) for sz in (-1, 1)]):
        m, st = night(90 + i, rim=0.3)
        blob(b, corners, (sx * 11.5, c + sy * 11, sz * 11.5), (6.5, 6.0, 6.5), m, st, f"corner{i}", rot=_rand_rot(rng, 20))
    lumps(26, 15.5, (6.5, 8.5), "lump", 0.92)
    lumps(22, 20.0, (2.8, 4.6), "wart", 0.9)


def _veils(b, c):
    # Below: torn curtains of smoke hanging off her underside, and a few strands.
    veil = b.bone("veil", (0, c - 22, 0), "mass")
    rng = random.Random(7)
    for i in range(9):
        a = i / 9 * math.tau + rng.uniform(-0.15, 0.15)
        r = rng.uniform(8, 15)
        hgt = rng.randint(16, 26)
        wdt = rng.randint(8, 12)
        top = c - 15 - rng.uniform(0, 4)
        ctr = (math.cos(a) * r, top - hgt / 2, math.sin(a) * r)
        mat, glow = smoke(120 + i, "down", 0.12)
        plane(b, veil, ctr, wdt, hgt, mat, glow, f"veil{i}", rot=(rng.uniform(-14, 14), 90 - math.degrees(a), 0))
    for i in range(5):
        a = (i + 0.5) / 5 * math.tau
        r = rng.uniform(4, 10)
        length = rng.randint(10, 18)
        m, st = night(130 + i, 0.03)
        b.cube(veil, (math.cos(a) * r - 1, c - 18 - length, math.sin(a) * r - 1), (2, length, 2), m, st, tag=f"strand{i}",
               rotation=(rng.uniform(-10, 10), 0, rng.uniform(-10, 10)), pivot=(math.cos(a) * r, c - 18, math.sin(a) * r))

    # Round her waist: a skirt of smoke peeling off her, twelve identical sheets so its turn loops seamlessly.
    skirt = b.bone("veil_skirt", (0, c, 0), "mass")
    mat, glow = smoke(150, "down", 0.12)
    for i in range(12):
        a = i / 12 * math.tau
        r = 20.5
        ctr = (math.cos(a) * r, c - 13, math.sin(a) * r)
        plane(b, skirt, ctr, 14, 16, mat, glow, f"skirt{i}", rot=(-12, 90 - math.degrees(a), 0))

    # On top: smoke rising off her like heat off a road.
    crown = b.bone("veil_crown", (0, c + 18, 0), "mass")
    for i in range(5):
        a = i / 5 * math.tau + 0.3
        r = 5 + (i % 3) * 3
        hgt = 9 + (i % 2) * 5
        ctr = (math.cos(a) * r, c + 17 + hgt / 2, math.sin(a) * r)
        mat2, glow2 = smoke(160 + i, "up", 0.2)
        plane(b, crown, ctr, 7, hgt, mat2, glow2, f"crown{i}", rot=(0, 90 - math.degrees(a), (i % 2) * 14 - 7))


def _motes(b, c):
    """Motes of night drifting around her: black diamonds with a star in them, a period of two so the turn loops."""
    motes = b.bone("motes", (0, c, 0), "mass")
    for i in range(8):
        a = i / 8 * math.tau
        r = 31 if i % 2 else 36
        y = c + (7 if i % 2 else -5)
        m, st = night(140 + i, 0.25, 0.9, nebula=0.0)
        ctr = (math.cos(a) * r, y, math.sin(a) * r)
        b.cube(motes, (ctr[0] - 1.5, ctr[1] - 1.5, ctr[2] - 1.5), (3, 3, 3), m,
               lambda f, x, y_, w, h: STAR[1] if (x, y_) == (1, 1) else RIM if (x + y_) % 2 == 0 and f == "up" else None,
               tag=f"mote{i}", rotation=(0, 45, 35), pivot=ctr)
    inner = b.bone("motes_inner", (0, c, 0), "mass")
    for i in range(6):
        a = i / 6 * math.tau
        ctr = (math.cos(a) * 27, c + (11 if i % 2 else 2), math.sin(a) * 27)
        b.cube(inner, (ctr[0] - 1, ctr[1] - 1, ctr[2] - 1), (2, 2, 2), *night(170 + i, 0.0, 1.0, nebula=0.0),
               tag=f"imote{i}", rotation=(45, 45, 0), pivot=ctr)


def _rings(b):
    """Anchors: rings of glyph-cut obsidian round an eye of violet light, built around the origin."""
    for i in range(4):
        ring = b.bone(f"ring_{i}", (0, 0, 0), "root")
        n = 14
        r = 10.0
        for k in range(n):
            a = k / n * math.tau
            ctr = (math.cos(a) * r, math.sin(a) * r, 0)
            mat, glyphs = obsidian_glyphs(200 + i * 20 + k)
            # Each segment is a block laid along the ring's tangent.
            b.cube(ring, (ctr[0] - 2.5, ctr[1] - 1.5, -1.5), (5, 3, 3), mat, glyphs, tag=f"ring{i}_{k}",
                   rotation=(0, 0, -math.degrees(a) - 90), pivot=ctr)
        # Four teeth pointing out, four in.
        for k in range(4):
            a = k / 4 * math.tau + math.pi / 4
            mat, _ = obsidian_glyphs(260 + i * 4 + k)
            out = (math.cos(a) * 13.0, math.sin(a) * 13.0, 0)
            b.cube(ring, (out[0] - 1, out[1] - 2, -1), (2, 4, 2), mat, lambda f, x, y, w, h: VIOLET if y == 0 else None,
                   tag=f"ring{i}_tooth{k}", rotation=(0, 0, 90 - math.degrees(a)), pivot=out)
        eye = b.bone(f"ring_{i}_eye", (0, 0, 0), f"ring_{i}")
        em, _ = obsidian_glyphs(290 + i)
        b.cube(eye, (-2.5, -2.5, -1), (5, 5, 2), em, lambda f, x, y, w, h: _eye_px(f, x, y, w, h), tag=f"ring{i}_eye",
               rotation=(0, 0, 45), pivot=(0, 0, 0))
        for k in range(4):
            a = k / 4 * math.tau
            sp = (math.cos(a) * 5.5, math.sin(a) * 5.5, 0)
            b.cube(eye, (sp[0] - 0.5, sp[1] - 2, -0.5), (1, 4, 1), em, lambda f, x, y, w, h: VIOLET if y in (0, h - 1) else RIM,
                   tag=f"ring{i}_spoke{k}", rotation=(0, 0, 90 - math.degrees(a)), pivot=sp)


def _eye_px(f, x, y, w, h):
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = max(abs(x - cx), abs(y - cy))
    if f in ("north", "south"):
        return VIOLET_HI if d < 1 else VIOLET if d < 2 else RIM_HI
    return VIOLET


def _bez(p0, p1, p2, u):
    return tuple((1 - u) ** 2 * p0[k] + 2 * (1 - u) * u * p1[k] + u * u * p2[k] for k in range(3))


def _bez_d(p0, p1, p2, u):
    return tuple(2 * (1 - u) * (p1[k] - p0[k]) + 2 * u * (p2[k] - p1[k]) for k in range(3))


MID_U, TIP_U = 0.42, 0.74


def tentacle_points(i):
    """The pivot (model space) and the canonical curve of tentacle i (pointing north from its pivot)."""
    a = tentacle_angle(i)
    bx, bz = math.cos(a), -math.sin(a)                   # world -> Bedrock (see the module doc)
    P0 = (bx * TENTACLE_ROOT, TENTACLE_Y, bz * TENTACLE_ROOT)
    reach = CYST_RING - TENTACLE_ROOT
    start = (0, 3, 5)
    ctrl = (0, 15, -reach * 0.55)
    end = (0, 7.5 - TENTACLE_Y, -reach)
    return P0, start, ctrl, end


def _tentacle(b, i):
    P0, s, k1, e = tentacle_points(i)

    def at(p):
        return (P0[0] + p[0], P0[1] + p[1], P0[2] + p[2])

    yaw = tentacle_yaw(i)
    t_bone = b.bone(f"tentacle_{i}", P0, "root", rotation=(0, yaw, 0))
    mid = b.bone(f"tentacle_{i}_mid", at(_bez(s, k1, e, MID_U)), f"tentacle_{i}")
    tip = b.bone(f"tentacle_{i}_tip", at(_bez(s, k1, e, TIP_U)), f"tentacle_{i}_mid")
    steps = 22
    for k in range(steps):
        u = k / (steps - 1)
        p = at(_bez(s, k1, e, u))
        d = _bez_d(s, k1, e, u)
        ln = math.hypot(d[1], d[2])
        rx = -math.degrees(math.atan2(d[1], -d[2]))
        sz = 8.0 - u * 5.2
        seg_len = max(3, round(ln / (steps - 1) + 2.5))
        side = max(2, round(sz))
        bone = t_bone if u < MID_U else mid if u < TIP_U else tip
        m, st = night(300 + i * 40 + k, 0.02, 0.12)
        b.cube(bone, (p[0] - side / 2, p[1] - side / 2, p[2] - seg_len / 2), (side, side, seg_len), m, st,
               tag=f"t{i}_{k}", rotation=(rx, 0, 0), pivot=p)
        # A thinner strand twisting round the main one, and barbs along its back.
        if 2 <= k < steps - 3 and k % 2 == 0:
            tw = u * 3 * math.pi + i
            off = (math.cos(tw) * side * 0.55, math.sin(tw) * side * 0.55, 0)
            q = (p[0] + off[0], p[1] + off[1], p[2])
            m2, st2 = night(340 + i * 40 + k, 0.08, 0.8, nebula=0.0)
            b.cube(bone, (q[0] - 1, q[1] - 1, q[2] - 2), (2, 2, 4), m2, st2, tag=f"t{i}_tw{k}", rotation=(rx, 0, 0), pivot=q)
        if 3 <= k < steps - 2 and k % 3 == 0:
            q = (p[0], p[1] + side / 2 + 1, p[2])
            b.cube(bone, (q[0] - 0.5, q[1] - 1.5, q[2] - 0.5), (1, 3, 1), *night(380 + i * 40 + k, 0.0, 0.0, nebula=0.0),
                   tag=f"t{i}_barb{k}", rotation=(rx - 35, 0, 0), pivot=q)
        if 4 <= k < steps - 2 and k % 4 == 1:
            q = (p[0], p[1] - side / 2, p[2])
            cm, cg = cyst_material(390 + i * 40 + k)
            b.cube(bone, (q[0] - 1, q[1] - 0.5, q[2] - 1), (2, 1, 2), cm, lambda f, x, y, w, h: CYST[5], tag=f"t{i}_sucker{k}",
                   rotation=(rx, 0, 0), pivot=q)
    # The cyst sits on the ground at the end of the spoke, rooted by tendrils.
    end = at(e)
    ground = (end[0], 0, end[2])
    cyst = b.bone(f"cyst_{i}", (ground[0], 4, ground[2]), f"tentacle_{i}")
    cm, cg = cyst_material(400 + i)
    blob(b, cyst, (ground[0], 4.2, ground[2]), (6.0, 4.2, 6.0), cm, cg, f"cyst{i}", k=0.72)
    dm, dg = maw_glow(410 + i)
    blob(b, cyst, (ground[0], 8.3, ground[2]), (3.6, 2.3, 3.6), dm, dg, f"cyst{i}_dome", k=0.75)
    rng = random.Random(420 + i)
    for k in range(5):
        a = k / 5 * math.tau + rng.uniform(-0.3, 0.3)
        ln = rng.randint(6, 10)
        q = (ground[0] + math.cos(a) * (5 + ln / 2), 0.6, ground[2] + math.sin(a) * (5 + ln / 2))
        rm, rg = cyst_material(430 + i * 5 + k)
        b.cube(cyst, (q[0] - ln / 2, 0, q[2] - 0.75), (ln, 1, 2), rm, lambda f, x, y, w, h: CYST[4] if x % 3 == 1 else None,
               tag=f"cyst{i}_root{k}", rotation=(0, -math.degrees(a), 0), pivot=q)


def cyst_world(i):
    """Where cyst i's centre lands once its tentacle's rest yaw is applied (for the self-check)."""
    P0, s, k1, e = tentacle_points(i)
    yaw = math.radians(tentacle_yaw(i))
    # Same maths as preview3d: Bedrock -> Java x flip, Ry(-yaw), back.
    x, z = -e[0], e[2]
    a = -yaw
    x2, z2 = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    return (P0[0] - x2, P0[2] + z2)


def form_bones(b):
    """Her last shape, also drawn alone for her shades and the ink echo: a slender woman of night in a long dress,
    long hair down her back, a crown of thorns of dark, a black sun behind her head."""
    form = b.bone("form", (0, 0, 0), "root")
    skirt = b.bone("form_skirt", (0, 34, 0), "form")
    # An A-line falling from the hips, in two-pixel tiers (low enough that the ink echo's pen never rules a line
    # across each one) widening to the hem.
    for j in range(14):
        y0 = 26 - j * 2
        w, d = 8 + round(j * 9 / 13), 6 + round(j * 7 / 13)
        m, g = dress(560 + j, y0 + 2, hem=(j == 13))
        b.cube(skirt, (-w / 2, y0, -d / 2), (w, 2 if j else 10, d), m, g, tag=f"f_skirt{j}")
    # The train trailing behind her (+Z is her back), torn into smoke.
    tm, tg = smoke(570, "down", 0.6)
    plane(b, skirt, (0, 3, 9.5), 13, 7, tm, tg, "f_train", rot=(-70, 0, 0))
    for k, sx in enumerate((-1, 1)):
        sm, sg = smoke(572 + k, "down", 0.6)
        plane(b, skirt, (sx * 8.6, 6, 1.5), 9, 12, sm, sg, f"f_tatter{k}", rot=(0, 90, sx * -8))

    torso = b.bone("form_torso", (0, 36, 0), "form")
    b.cube(torso, (-2.5, 36, -2), (5, 5, 4), *form_night(500, y0=41), tag="f_waist")
    b.cube(torso, (-3.5, 41, -2.25), (7, 5, 4.5), *form_night(501, y0=46), tag="f_ribs")
    b.cube(torso, (-4.5, 46, -2.5), (9, 6, 5), *form_night(502, y0=52), tag="f_chest")
    b.cube(torso, (-3.5, 47, -3.2), (7, 3, 1), *form_night(506, 0.0, 0.0, y0=50), tag="f_bust")
    b.cube(torso, (-5.5, 52, -2.25), (11, 2, 4.5), *form_night(503, y0=54), tag="f_shoulders")
    b.cube(torso, (-1.25, 54, -1.25), (2.5, 4, 2.5), *form_night(504, 0.0, y0=58), tag="f_neck")
    head = b.bone("form_head", (0, 58, 0), "form_torso")

    def eyes(face, x, y, w, h):
        if face == "north" and y == 3 and x in (1, 4):
            return hexc("#ffffff")
        if face == "north" and y == 3 and x in (2, 3):
            return None
        return None
    hm, hs = form_night(505, 0.0, 0.5)
    b.cube(head, (-2.5, 58, -3), (5, 2, 5), hm, hs, tag="f_jaw")
    b.cube(head, (-3, 60, -3.25), (6, 6, 6.5), hm, lambda f, x, y, w, h: eyes(f, x, y, w, h) or hs(f, x, y, w, h), tag="f_head")
    crown = b.bone("form_crown", (0, 66, 0), "form_head")
    for i in range(9):
        a = (i - 4) * 17.0
        hgt = 9 - abs(i - 4) * 1.2
        base = (math.sin(math.radians(a)) * 3.2, 66, -math.cos(math.radians(a)) * 1.2 - 0.5)
        b.cube(crown, (base[0] - 0.5, 66, base[2] - 0.5), (1, round(hgt), 1), *form_night(510 + i, 0.0, 0.0),
               tag=f"f_spike{i}", rotation=(-8, 0, a * 0.9), pivot=base)
        b.cube(crown, (base[0] - 0.5, 66 + round(hgt) - 1, base[2] - 0.5), (1, 1, 1), *form_night(519, 0.0, 0.0),
               tag=f"f_spiketip{i}", rotation=(-8, 0, a * 0.9), pivot=base)
    # The eclipse behind her head: a ring of black stones rimmed in corona, rays between them.
    halo = b.bone("form_halo", (0, 64, 6.5), "form_head")
    n = 18
    for i in range(n):
        a = i / n * math.tau
        ctr = (math.cos(a) * 11, 64 + math.sin(a) * 11, 6.5)
        b.cube(halo, (ctr[0] - 2, ctr[1] - 1, 6), (4, 2, 1), core_material,
               lambda f, x, y, w, h: CORONA[1] if f in ("north", "south") and y == 0 else CORONA[3] if f in ("north", "south") else None,
               tag=f"f_halo{i}", rotation=(0, 0, -math.degrees(a) - 90), pivot=ctr)
        ray = (math.cos(a + math.pi / n) * 14, 64 + math.sin(a + math.pi / n) * 14, 7)
        ln = 4 if i % 2 else 6
        b.cube(halo, (ray[0] - 0.5, ray[1] - ln / 2, 7), (1, ln, 0), lambda f, x, y, w, h: CORONA[0] if y >= h - 2 else CORONA[2],
               lambda f, x, y, w, h: CORONA[0] if y >= h - 2 else CORONA[2],
               faces=("north",), tag=f"f_ray{i}", rotation=(0, 0, 90 - math.degrees(a + math.pi / n)), pivot=ray)
    hair = b.bone("form_hair", (0, 66, 3), "form_head")
    hm2, hg2 = form_night(520, 0.03, 0.4)
    b.cube(hair, (-3.5, 65.5, -3.5), (7, 2, 7.5), hm2, hg2, tag="f_hair_top")
    b.cube(hair, (-3.5, 56, 3), (7, 10, 2), hm2, hg2, tag="f_hair_back")
    for i in range(7):
        x0 = -3.5 + i
        length = 18 + (i * 7) % 9 - abs(i - 3)
        b.cube(hair, (x0, 56 - length, 3.5 + (i % 2) * 0.5), (1, length, 1.5), *form_night(521 + i, 0.03, 0.3),
               tag=f"f_hair{i}", rotation=(6 + (i % 3) * 2, 0, -(i - 3) * 1.5), pivot=(x0 + 0.5, 56, 4))
    for k, sx in enumerate((-1, 1)):
        b.cube(hair, (sx * 3.2 - 0.75, 50, -2.5), (1.5, 11, 2), *form_night(530 + k, 0.03, 0.3), tag=f"f_lock{k}",
               rotation=(4, 0, sx * -4), pivot=(sx * 3.2, 61, -1.5))
    for side, sx in (("left", 1), ("right", -1)):
        arm = b.bone(f"form_arm_{side}", (sx * 5.8, 53, 0), "form_torso")
        am, ag = form_night(540 + sx, 0.02)
        b.cube(arm, (sx * 6.2 - 1, 43, -1), (2, 10, 2), am, ag, tag=f"f_upper_{side}", rotation=(0, 0, -sx * 6), pivot=(sx * 5.8, 53, 0))
        b.cube(arm, (sx * 7.3 - 1, 33, -1.6), (2, 10.5, 2), am, ag, tag=f"f_fore_{side}", rotation=(-6, 0, -sx * 3), pivot=(sx * 7.3, 43.5, -0.6))
        b.cube(arm, (sx * 7.6 - 1, 29, -2.3), (2, 4, 1.5), am, ag, tag=f"f_hand_{side}")
        for k in range(3):
            fx = sx * 7.6 - 0.9 + k * 0.8
            b.cube(arm, (fx, 23, -2.1), (1, 6, 1), *form_night(546 + sx * 3 + k, 0.0, 0.0), tag=f"f_claw_{side}{k}",
                   rotation=(-6, 0, sx * (k - 1) * 5), pivot=(fx + 0.5, 29, -1.6))
        leg = b.bone(f"form_leg_{side}", (sx * 2.2, 33, 0), "form")
        b.cube(leg, (sx * 2.2 - 1.5, 17, -1.5), (3, 16, 3), *form_night(550 + sx, 0.02), tag=f"f_thigh_{side}")
        b.cube(leg, (sx * 2.2 - 1, 1, -1), (2, 16, 2), *form_night(552 + sx, 0.02), tag=f"f_shin_{side}")
        b.cube(leg, (sx * 2.2 - 1, 0, -7.5), (2, 1.5, 4), *form_night(554 + sx, 0.0), tag=f"f_foot_{side}")


# --- animations ----------------------------------------------------------------------------

SHELL_OUT = {
    # Open, the four side plates fall outward like petals (their tops tipping away, sinking below the core) and the
    # top lifts off: through every side opening the black sun shows whole.
    "shell_n": ([0, -17, -11], [62, 0, 0]),
    "shell_s": ([0, -17, 11], [-62, 0, 0]),
    "shell_e": ([11, -17, 0], [0, 0, 62]),
    "shell_w": ([-11, -17, 0], [0, 0, -62]),
    "shell_up": ([0, 12, 0], [0, 35, 0]),
    "shell_down": ([0, -7, 0], [0, -20, 0]),
}
SHELLS = list(SHELL_OUT)
TENTACLES = range(6)


def _sv(bone, amount):
    pos, rot = SHELL_OUT[bone]
    return [v * amount for v in pos], [v * amount for v in rot]


def shells(a, *keys):
    """Keys (t, amount[, ease]) for every shell: amount 0 = closed, 1 = open as in idle_open, below 0 = clenched."""
    for bone in SHELLS:
        pk, rk = [], []
        for k in keys:
            p, r = _sv(bone, k[1])
            pk.append((k[0], p) + tuple(k[2:]))
            rk.append((k[0], r) + tuple(k[2:]))
        a.pos(bone, *pk)
        a.rot(bone, *rk)


def _open(a, t0, t1, amount=1.0, ease="easeOutBack"):
    shells(a, (t0, 0), (t1, amount, ease))


def ten(a, i, rot=None, scale=None, mid=None, tip=None):
    """Keys for tentacle i: its own rotation and scale, and the bends of its two segments."""
    if rot:
        a.rot(f"tentacle_{i}", *rot)
    if scale:
        a.scale(f"tentacle_{i}", *scale)
    if mid:
        a.rot(f"tentacle_{i}_mid", *mid)
    if tip:
        a.rot(f"tentacle_{i}_tip", *tip)


Z3 = [0, 0, 0]
ONE = [1, 1, 1]


def s3(k):
    return [k, k, k]


def anims():
    f = AnimFile()
    _loops(f)
    _phase_one(f)
    _tentacle_attacks(f)
    _core_pulses(f)
    _transitions(f)
    _form_clips(f)
    return f


def _loops(f):
    idle = f.new("animation.amara.idle", 4.0, loop=True)
    idle.scale("mass", (0, ONE), (2.0, [1.035, 0.97, 1.035], "easeInOutSine"), (4.0, ONE, "easeInOutSine"))
    idle.pos("mass", (0, Z3), (2.0, [0, 1.2, 0], "easeInOutSine"), (4.0, Z3, "easeInOutSine"))
    # The plates shift against each other like something breathing under a sheet.
    for j, bone in enumerate(SHELLS):
        p, _ = _sv(bone, 0.08)
        t0 = 0.5 + j * 0.5
        idle.pos(bone, (0, Z3), (t0, p, "easeInOutSine"), (min(4.0, t0 + 1.6), Z3, "easeInOutSine"), (4.0, Z3))
    idle.rot("corners", (0, Z3), (2.0, [0, 3, 0], "easeInOutSine"), (4.0, Z3, "easeInOutSine"))
    idle.rot("motes", (0, Z3), (4.0, [0, 90, 0]))
    idle.rot("motes_inner", (0, Z3), (4.0, [0, -60, 0]))
    idle.rot("veil_skirt", (0, Z3), (4.0, [0, 30, 0]))
    idle.rot("veil", (0, Z3), (2.0, [3, 0, -3], "easeInOutSine"), (4.0, Z3, "easeInOutSine"))
    idle.scale("veil_crown", (0, ONE), (1.0, [1, 1.12, 1], "easeInOutSine"), (2.0, ONE, "easeInOutSine"),
               (3.0, [1, 1.08, 1], "easeInOutSine"), (4.0, ONE, "easeInOutSine"))
    idle.rot("core_corona", (0, Z3), (4.0, [0, 90, 0]))
    for i in TENTACLES:
        ph = i * 0.33
        ten(idle, i,
            mid=[(0, [2, 0, 0]), (1.0 + ph, [-3, 0, 2], "easeInOutSine"), (2.0 + ph, [2, 0, -2], "easeInOutSine"), (4.0, [2, 0, 0], "easeInOutSine")],
            tip=[(0, [-2, 0, 0]), (1.5 + ph * 0.5, [3, 0, -3], "easeInOutSine"), (2.5 + ph * 0.5, [-3, 0, 3], "easeInOutSine"), (4.0, [-2, 0, 0], "easeInOutSine")])
        f_ = 0.4 + i * 0.27
        idle.scale(f"cyst_{i}", (0, ONE), (f_, [1.14, 1.1, 1.14], "easeInOutSine"), (f_ + 0.9, ONE, "easeInOutSine"),
                   (f_ + 2.0, [1.08, 1.06, 1.08], "easeInOutSine"), (4.0, ONE, "easeInOutSine"))
    for i in range(4):
        idle.rot(f"ring_{i}_eye", (0, Z3), (4.0, [0, 0, 180 if i % 2 else -180]))

    io = f.new("animation.amara.idle_open", 4.0, loop=True)
    for bone in SHELLS:
        p, r = _sv(bone, 1.0)
        p2, r2 = _sv(bone, 1.1)
        io.pos(bone, (0, p), (2.0, p2, "easeInOutSine"), (4.0, p, "easeInOutSine"))
        io.rot(bone, (0, r), (2.0, r2, "easeInOutSine"), (4.0, r, "easeInOutSine"))
    io.scale("core", (0, ONE), (1.0, s3(1.15), "easeInOutSine"), (2.0, ONE, "easeInOutSine"),
             (3.0, s3(1.15), "easeInOutSine"), (4.0, ONE, "easeInOutSine"))
    io.rot("core", (0, Z3), (4.0, [0, 180, 0]))
    io.rot("core_corona", (0, Z3), (4.0, [0, 0, 90]))
    io.scale("core_corona", (0, ONE), (0.5, s3(1.12), "easeOutQuad"), (2.0, s3(0.96), "easeInOutSine"),
             (2.5, s3(1.12), "easeOutQuad"), (4.0, ONE, "easeInOutSine"))
    io.rot("motes", (0, Z3), (4.0, [0, 180, 0]))
    io.rot("motes_inner", (0, Z3), (4.0, [0, -120, 0]))
    io.rot("veil_skirt", (0, Z3), (4.0, [0, 60, 0]))
    io.scale("veil_crown", (0, ONE), (2.0, [1, 1.25, 1], "easeInOutSine"), (4.0, ONE, "easeInOutSine"))
    io.rot("corners", (0, Z3), (2.0, [0, 4, 0], "easeInOutSine"), (4.0, Z3, "easeInOutSine"))

    fi = f.new("animation.amara.form_idle", 3.0, loop=True)
    fi.rot("form_hair", (0, Z3), (1.5, [6, 0, 0], "easeInOutSine"), (3.0, Z3, "easeInOutSine"))
    fi.rot("form_arm_left", (0, [0, 0, -3]), (1.5, [-3, 0, -6], "easeInOutSine"), (3.0, [0, 0, -3], "easeInOutSine"))
    fi.rot("form_arm_right", (0, [0, 0, 3]), (1.5, [-3, 0, 6], "easeInOutSine"), (3.0, [0, 0, 3], "easeInOutSine"))
    fi.pos("form", (0, Z3), (1.5, [0, 1.5, 0], "easeInOutSine"), (3.0, Z3, "easeInOutSine"))
    fi.rot("form_skirt", (0, Z3), (1.5, [2, 0, 0], "easeInOutSine"), (3.0, Z3, "easeInOutSine"))
    fi.rot("form_head", (0, Z3), (1.5, [-2, 0, 0], "easeInOutSine"), (3.0, Z3, "easeInOutSine"))
    fi.rot("form_halo", (0, Z3), (3.0, [0, 0, 40]))


def _phase_one(f):
    em = f.new("animation.amara.emerge", 6.0, hold=True)
    em.scale("mass", (0, s3(0.05)), (1.5, [0.2, 0.6, 0.2], "easeInQuad"), (4.0, s3(1.15), "easeOutBack"),
             (6.0, ONE, "easeInOutSine"))
    em.rot("mass", (0, Z3), (4.0, [0, 540, 0], "easeOutQuad"), (6.0, [0, 540, 0]))
    em.scale("veil_skirt", (0, s3(0.2)), (3.0, s3(0.4)), (4.6, s3(1.2), "easeOutBack"), (6.0, ONE))
    em.scale("core_corona", (0, s3(0)), (4.2, s3(0)), (4.8, s3(1.6), "easeOutQuad"), (6.0, ONE, "easeInOutSine"))
    for i in range(4):
        em.scale(f"ring_{i}", (0, s3(0)), (3.5 + i * 0.3, s3(0)), (4.5 + i * 0.3, ONE, "easeOutBack"), (6.0, ONE))
    shells(em, (0, 0), (4.4, 0), (5.0, 0.6, "easeOutQuad"), (6.0, 0, "easeInQuad"))

    ex = f.new("animation.amara.expose", 1.0, hold=True)
    shells(ex, (0, 0), (0.15, -0.12, "easeInQuad"), (1.0, 1.0, "easeOutBack"))
    ex.scale("core", (0, ONE), (0.3, s3(1.5), "easeOutQuad"), (1.0, s3(1.15)))
    ex.scale("core_corona", (0, s3(0.5)), (0.35, s3(1.5), "easeOutQuad"), (1.0, ONE))
    cl = f.new("animation.amara.close", 1.0)
    shells(cl, (0, 1.0), (0.8, -0.1, "easeInBack"), (1.0, 0))
    cl.scale("core_corona", (0, ONE), (0.8, s3(0.6), "easeInQuad"), (1.0, ONE))

    a = f.new("animation.amara.cast", 1.0)
    a.scale("core", (0, ONE), (0.4, s3(1.4), "easeOutQuad"), (1.0, ONE))
    shells(a, (0, 0), (0.4, 0.25, "easeOutQuad"), (1.0, 0, "easeInOutSine"))
    a.scale("veil_crown", (0, ONE), (0.4, [1, 1.5, 1], "easeOutQuad"), (1.0, ONE))
    a = f.new("animation.amara.orbit", 1.0)
    a.rot("mass", (0, Z3), (1.0, [0, 360, 0], "easeInOutSine"))
    a.rot("motes", (0, Z3), (1.0, [0, 270, 0], "easeInOutSine"))
    a = f.new("animation.amara.well", 1.5)
    a.scale("mass", (0, ONE), (0.8, s3(0.75), "easeInQuad"), (1.1, s3(1.2), "easeOutBack"), (1.5, ONE))
    a.scale("veil_skirt", (0, ONE), (0.8, s3(0.6), "easeInQuad"), (1.1, s3(1.4), "easeOutBack"), (1.5, ONE))
    a.rot("veil_skirt", (0, Z3), (1.5, [0, -120, 0], "easeInQuad"))
    a = f.new("animation.amara.lance", 1.75)
    a.pos("shell_n", (0, Z3), (1.2, [0, 0, -6], "easeInQuad"), (1.75, Z3, "easeOutQuad"))
    a.rot("shell_n", (0, Z3), (1.2, [-12, 0, 0], "easeInQuad"), (1.75, Z3, "easeOutQuad"))
    a.scale("core", (0, ONE), (1.6, s3(1.6), "easeInQuad"), (1.75, ONE))
    a.scale("core_corona", (0, ONE), (1.5, [0.6, 0.6, 1.8], "easeInQuad"), (1.75, ONE))
    a = f.new("animation.amara.rain", 1.25)
    a.pos("mass", (0, Z3), (0.8, [0, 10, 0], "easeOutQuad"), (1.25, Z3, "easeInBack"))
    a.scale("veil", (0, ONE), (0.8, [1, 0.6, 1], "easeOutQuad"), (1.25, [1, 1.3, 1], "easeInQuad"))
    a = f.new("animation.amara.snuff", 1.5)
    a.scale("mass", (0, ONE), (1.3, s3(0.85), "easeInQuad"), (1.5, s3(1.25), "easeOutBack"))
    a.rot("motes", (0, Z3), (1.5, [0, -540, 0], "easeInQuad"))
    a.scale("motes", (0, ONE), (1.3, s3(0.6), "easeInQuad"), (1.5, s3(1.3), "easeOutBack"))
    a = f.new("animation.amara.spawn", 1.5)
    shells(a, (0, 0), (0.8, 0.4, "easeOutBack"), (1.5, 0, "easeInOutSine"))
    a.scale("veil", (0, ONE), (0.8, [1.3, 1.5, 1.3], "easeOutQuad"), (1.5, ONE))


def _tentacle_attacks(f):
    # slam (windup 25 ticks): the front tentacle rears high over her, curling back, and crashes at 1.25 s; its
    # neighbours rear half as high a beat behind; the rest brace against the ground.
    a = f.new("animation.amara.slam", 1.5)
    for i in TENTACLES:
        if i == 1:
            ten(a, i, rot=[(0, Z3), (0.85, [-95, 0, 0], "easeOutQuad"), (1.05, [-100, 0, 0]), (1.25, [14, 0, 0], "easeInExpo"),
                           (1.5, Z3, "easeOutQuad")],
                mid=[(0, Z3), (0.85, [-25, 0, 0], "easeOutQuad"), (1.1, [-30, 0, 0]), (1.25, [10, 0, 0], "easeInExpo"), (1.5, Z3)],
                tip=[(0, Z3), (0.85, [-35, 0, 0], "easeOutQuad"), (1.1, [-40, 0, 0]), (1.25, [15, 0, 0], "easeInExpo"), (1.5, Z3)],
                scale=[(0, ONE), (0.85, [1, 1, 1.15], "easeOutQuad"), (1.25, ONE, "easeInExpo"), (1.5, ONE)])
        elif i in (0, 2):
            ten(a, i, rot=[(0, Z3), (0.3, Z3), (1.0, [-45, (12 if i == 0 else -12), 0], "easeOutQuad"),
                           (1.3, [10, 0, 0], "easeInExpo"), (1.5, Z3, "easeOutQuad")],
                tip=[(0, Z3), (1.0, [-25, 0, 0], "easeOutQuad"), (1.3, [10, 0, 0], "easeInExpo"), (1.5, Z3)])
        else:
            ten(a, i, rot=[(0, Z3), (0.9, [6, 0, 0], "easeInOutSine"), (1.25, [10, 0, 0], "easeInQuad"), (1.5, Z3, "easeOutQuad")])
    a.pos("mass", (0, Z3), (1.0, [0, 3, 0], "easeOutQuad"), (1.25, [0, -2, 0], "easeInExpo"), (1.5, Z3, "easeOutQuad"))
    a.rot("mass", (0, Z3), (1.0, [-6, 0, 0], "easeOutQuad"), (1.25, [5, 0, 0], "easeInExpo"), (1.5, Z3, "easeOutQuad"))

    # sweep (windup 30, active 40): every tentacle winds back the same way, lifts off the ground and swings round her
    # in one long turn (1.5 to 3.0 s), the tips trailing.
    a = f.new("animation.amara.sweep", 3.0)
    for i in TENTACLES:
        d = i * 0.03
        ten(a, i, rot=[(0, Z3), (1.3, [-22, -40, 0], "easeInOutSine"), (1.5 + d, [-26, -45, 0]), (2.4 + d, [-26, 70, 0], "easeInOutQuad"),
                       (2.7, [-12, 80, 0], "easeOutQuad"), (3.0, Z3, "easeInOutSine")],
            mid=[(0, Z3), (1.5, [0, 15, 0], "easeInOutSine"), (2.4 + d, [0, -25, 0], "easeInOutQuad"), (3.0, Z3, "easeInOutSine")],
            tip=[(0, Z3), (1.5, [0, 20, 0], "easeInOutSine"), (2.5 + d, [0, -35, 0], "easeInOutQuad"), (3.0, Z3, "easeInOutSine")])
    a.rot("mass", (0, Z3), (1.5, [0, -15, 0], "easeInOutSine"), (2.4, [0, 30, 0], "easeInOutQuad"), (3.0, Z3, "easeInOutSine"))
    a.rot("veil_skirt", (0, Z3), (1.5, [0, -20, 0]), (2.6, [0, 90, 0], "easeOutQuad"), (3.0, [0, 90, 0]))

    # grasp (windup 20, then held): the three front tentacles reach out and toward each other, stretching, and at
    # 1.0 s clench shut like fingers round the hunter; they hold the grip and let go at the end.
    a = f.new("animation.amara.grasp", 2.0)
    turn = {0: 22, 1: 0, 2: -22}
    for i in TENTACLES:
        if i in turn:
            ten(a, i, rot=[(0, Z3), (0.7, [-38, turn[i], 0], "easeOutQuad"), (1.0, [-18, turn[i] * 1.2, 0], "easeInQuad"),
                           (1.7, [-18, turn[i] * 1.2, 0]), (2.0, Z3, "easeInOutSine")],
                scale=[(0, ONE), (0.7, [1, 1, 1.3], "easeOutQuad"), (1.0, [1, 1, 1.2]), (1.7, [1, 1, 1.2]), (2.0, ONE, "easeInOutSine")],
                mid=[(0, Z3), (0.7, [-12, 0, 0], "easeOutQuad"), (1.0, [40, 0, 0], "easeInBack"), (1.7, [44, 0, 0]), (2.0, Z3)],
                tip=[(0, Z3), (0.7, [-18, 0, 0], "easeOutQuad"), (1.0, [65, 0, 0], "easeInBack"), (1.7, [70, 0, 0]), (2.0, Z3)])
        else:
            ten(a, i, rot=[(0, Z3), (0.8, [8, 0, 0], "easeInOutSine"), (1.7, [8, 0, 0]), (2.0, Z3)])
    a.pos("mass", (0, Z3), (0.7, [0, 0, -3], "easeOutQuad"), (1.0, [0, 0, 1], "easeInQuad"), (2.0, Z3, "easeInOutSine"))

    # spikes (windup 30, active 36): every tentacle draws up and back like a scorpion's tail, tips straight, and at
    # 1.5 s drives into the ground; they stay buried while the spikes burrow, then tear free.
    a = f.new("animation.amara.spikes", 2.5)
    for i in TENTACLES:
        d = (i % 3) * 0.04
        ten(a, i, rot=[(0, Z3), (1.2 + d, [-55, 0, 0], "easeOutQuad"), (1.42 + d, [-60, 0, 0]), (1.55 + d, [32, 0, 0], "easeInExpo"),
                       (2.15, [30, 0, 0]), (2.5, Z3, "easeOutBack")],
            mid=[(0, Z3), (1.2, [-20, 0, 0], "easeOutQuad"), (1.55 + d, [-8, 0, 0], "easeInExpo"), (2.15, [-8, 0, 0]), (2.5, Z3)],
            tip=[(0, Z3), (1.2, [-30, 0, 0], "easeOutQuad"), (1.55 + d, [-20, 0, 0], "easeInExpo"), (2.15, [-20, 0, 0]), (2.5, Z3)],
            scale=[(0, ONE), (1.2, [1, 1, 0.9]), (1.55 + d, [1, 1, 1.15], "easeInExpo"), (2.15, [1, 1, 1.15]), (2.5, ONE)])
    a.pos("mass", (0, Z3), (1.2, [0, 6, 0], "easeOutQuad"), (1.55, [0, -3, 0], "easeInExpo"), (2.15, [0, -2, 0]), (2.5, Z3))

    # bloom (windup 30): the tentacles open round her like the petals of a black flower, lifting and curling outward,
    # snap fully open at 1.5 s, and fold back down.
    a = f.new("animation.amara.bloom", 2.25)
    for i in TENTACLES:
        ten(a, i, rot=[(0, Z3), (1.2, [-60, 8, 0], "easeInOutSine"), (1.5, [-82, 12, 0], "easeOutBack"), (1.8, [-80, 12, 0]),
                       (2.25, Z3, "easeInOutSine")],
            mid=[(0, Z3), (1.2, [-10, 0, 0], "easeInOutSine"), (1.5, [30, 0, 0], "easeOutBack"), (1.8, [28, 0, 0]), (2.25, Z3)],
            tip=[(0, Z3), (1.2, [-10, 0, 0], "easeInOutSine"), (1.5, [45, 0, 0], "easeOutBack"), (1.8, [42, 0, 0]), (2.25, Z3)])
    a.scale("mass", (0, ONE), (1.2, s3(0.92), "easeInOutSine"), (1.5, s3(1.12), "easeOutBack"), (2.25, ONE))
    a.rot("mass", (0, Z3), (1.5, [0, 30, 0], "easeInOutSine"), (2.25, Z3))
    a.scale("veil_skirt", (0, ONE), (1.5, s3(1.35), "easeOutBack"), (2.25, ONE))


def _core_pulses(f):
    """Phase III, the core open (idle_open holds the shells at 1): each starts and ends there."""
    # flare (windup 30): the core swells, the corona blazes and the sun spins one whole turn; the shells lean out.
    a = f.new("animation.amara.flare", 2.0)
    a.scale("core", (0, ONE), (1.0, s3(1.5), "easeInOutSine"), (1.5, s3(1.9), "easeOutBack"), (2.0, ONE, "easeInOutSine"))
    a.rot("core", (0, Z3), (1.5, [0, 360, 0], "easeInOutQuad"), (2.0, [0, 360, 0]))
    a.scale("core_corona", (0, ONE), (1.4, s3(1.2), "easeInQuad"), (1.6, s3(2.0), "easeOutExpo"), (2.0, ONE, "easeInOutSine"))
    a.rot("core_corona", (0, Z3), (2.0, [0, -180, 90], "easeInOutSine"))
    shells(a, (0, 1.0), (1.5, 1.35, "easeOutBack"), (2.0, 1.0, "easeInOutSine"))

    # totality (windup 60): the plates close in tighter than ever, the core shrinking to nothing behind them; at 3.0 s
    # everything is thrown wide open at once, the corona flaring huge, and settles back.
    a = f.new("animation.amara.totality", 4.0)
    shells(a, (0, 1.0), (1.0, 0, "easeInQuad"), (2.8, -0.2, "easeInOutSine"), (3.0, 1.8, "easeOutExpo"), (3.4, 1.7),
           (4.0, 1.0, "easeInOutSine"))
    a.rot("corners", (0, Z3), (2.8, [0, 0, 0]), (3.0, [0, 25, 0], "easeOutExpo"), (4.0, Z3, "easeInOutSine"))
    a.scale("corners", (0, ONE), (2.8, s3(0.92), "easeInOutSine"), (3.0, s3(1.25), "easeOutExpo"), (4.0, ONE))
    a.scale("core", (0, ONE), (2.6, s3(0.5), "easeInQuad"), (3.0, s3(2.0), "easeOutExpo"), (4.0, ONE, "easeInOutSine"))
    a.scale("core_corona", (0, ONE), (2.6, s3(0.2), "easeInQuad"), (3.0, s3(2.4), "easeOutExpo"), (4.0, ONE, "easeInOutSine"))
    a.scale("motes", (0, ONE), (2.8, s3(0.7), "easeInQuad"), (3.0, s3(1.5), "easeOutExpo"), (4.0, ONE))
    a.scale("veil_skirt", (0, ONE), (2.8, s3(0.75)), (3.0, s3(1.6), "easeOutExpo"), (4.0, ONE))
    a.pos("mass", (0, Z3), (2.8, [0, -2, 0], "easeInOutSine"), (3.0, [0, 3, 0], "easeOutExpo"), (4.0, Z3))

    # collapse (windup 50): everything is sucked inward and spun tight round the core (motes, veils, plates, corners);
    # at 2.5 s it bursts outward and swings back into place.
    a = f.new("animation.amara.collapse", 3.5)
    shells(a, (0, 1.0), (2.3, -0.35, "easeInCubic"), (2.5, 2.0, "easeOutExpo"), (3.5, 1.0, "easeInOutSine"))
    a.scale("corners", (0, ONE), (2.3, s3(0.7), "easeInCubic"), (2.5, s3(1.3), "easeOutExpo"), (3.5, ONE, "easeInOutSine"))
    a.rot("corners", (0, Z3), (2.3, [0, 90, 0], "easeInCubic"), (3.5, [0, 90, 0]))
    a.scale("motes", (0, ONE), (2.3, s3(0.35), "easeInCubic"), (2.5, s3(1.6), "easeOutExpo"), (3.5, ONE, "easeInOutSine"))
    a.rot("motes", (0, Z3), (2.3, [0, 540, 0], "easeInCubic"), (3.5, [0, 720, 0], "easeOutQuad"))
    a.scale("motes_inner", (0, ONE), (2.3, s3(0.3), "easeInCubic"), (2.5, s3(1.7), "easeOutExpo"), (3.5, ONE))
    a.scale("veil_skirt", (0, ONE), (2.3, s3(0.45), "easeInCubic"), (2.5, s3(1.7), "easeOutExpo"), (3.5, ONE))
    a.rot("veil_skirt", (0, Z3), (2.3, [0, -270, 0], "easeInCubic"), (3.5, [0, -360, 0], "easeOutQuad"))
    a.scale("veil", (0, ONE), (2.3, [0.5, 0.4, 0.5], "easeInCubic"), (2.5, [1.4, 1.5, 1.4], "easeOutExpo"), (3.5, ONE))
    a.scale("veil_crown", (0, ONE), (2.3, s3(0.2), "easeInCubic"), (2.5, [1.2, 2.0, 1.2], "easeOutExpo"), (3.5, ONE))
    a.scale("core", (0, ONE), (2.3, s3(0.4), "easeInCubic"), (2.5, s3(2.2), "easeOutExpo"), (3.5, ONE, "easeInOutSine"))
    a.scale("core_corona", (0, ONE), (2.3, s3(0.1), "easeInCubic"), (2.5, s3(2.6), "easeOutExpo"), (3.5, ONE, "easeInOutSine"))


def _transitions(f):
    t2 = f.new("animation.amara.transform_2", 3.0)
    t2.scale("mass", (0, ONE), (0.8, [0.7, 1.3, 0.7], "easeInQuad"), (1.6, [1.3, 0.8, 1.3], "easeOutBack"), (3.0, ONE, "easeInOutSine"))
    for i in TENTACLES:
        d = i * 0.05
        # The tentacles uncoil out of her underside and slap down onto their cysts.
        ten(t2, i, scale=[(0, s3(0)), (1.3 + d, s3(0)), (2.0 + d, ONE, "easeOutBack")],
            rot=[(0, [-70, 0, 0]), (1.3 + d, [-70, 0, 0]), (2.2 + d, [6, 0, 0], "easeInQuad"), (2.6 + d, Z3, "easeOutQuad")],
            mid=[(0, [-60, 0, 0]), (1.3 + d, [-60, 0, 0]), (2.2 + d, [5, 0, 0], "easeOutQuad"), (2.6 + d, Z3)],
            tip=[(0, [-80, 0, 0]), (1.3 + d, [-80, 0, 0]), (2.3 + d, [8, 0, 0], "easeOutQuad"), (2.7 + d, Z3)])
    t3 = f.new("animation.amara.transform_3", 3.0)
    shells(t3, (0, 0), (1.0, -0.2, "easeInQuad"), (2.2, 1.25, "easeOutBack"), (3.0, 1.0, "easeInOutSine"))
    t3.scale("core", (0, ONE), (1.0, s3(0.6), "easeInQuad"), (2.2, s3(1.6), "easeOutBack"), (3.0, ONE))
    t3.scale("core_corona", (0, ONE), (1.0, s3(0.3), "easeInQuad"), (2.2, s3(1.9), "easeOutExpo"), (3.0, ONE))
    t4 = f.new("animation.amara.transform_4", 4.0)
    t4.scale("form", (0, s3(0.1)), (2.0, [0.4, 1.2, 0.4], "easeInQuad"), (3.2, s3(1.05), "easeOutBack"), (4.0, ONE))
    t4.rot("form", (0, [0, 720, 0]), (3.2, Z3, "easeOutQuad"))
    t4.scale("form_halo", (0, s3(0)), (3.0, s3(0)), (3.6, s3(1.2), "easeOutBack"), (4.0, ONE))

    de = f.new("animation.amara.death", 6.0, hold=True)
    for bone in ("mass", "form"):
        de.scale(bone, (0, ONE), (2.0, [1.2, 0.8, 1.2], "easeInOutSine"), (5.0, [0.4, 1.4, 0.4], "easeInQuad"),
                 (6.0, s3(0), "easeInBack"))
        de.rot(bone, (0, Z3), (6.0, [0, 900, 0], "easeInQuad"))
    shells(de, (0, 0), (0.5, 0), (2.0, 1.4, "easeOutBack"), (5.0, 0.6))
    de.scale("core_corona", (0, ONE), (2.0, s3(1.8), "easeOutQuad"), (5.0, s3(0.4)), (6.0, s3(0)))

    # shatter (60 ticks, the moment at 40): the mass shudders and swells, crunches in, bursts outward at 2.0 s (the
    # plates and lumps flung out, everything scaled up), then pours inward and is gone to a point by the end.
    sh = f.new("animation.amara.shatter", 3.0, hold=True)
    sh.scale("mass", (0, ONE), (1.0, s3(1.1), "easeInOutSine"), (1.5, s3(0.82), "easeInCubic"), (2.0, s3(1.45), "easeOutExpo"),
             (2.25, s3(1.3), "easeOutQuad"), (2.85, s3(0.06), "easeInCubic"), (3.0, s3(0)))
    sh.rot("mass", (0, Z3), (1.0, [3, 20, -3], "easeInOutSine"), (1.5, [0, 40, 0]), (2.0, [0, 70, 0], "easeOutExpo"),
           (3.0, [0, 520, 0], "easeInCubic"))
    sh.pos("mass", (0, Z3), (0.2, [0.6, 0, 0]), (0.4, [-0.6, 0.4, 0]), (0.6, [0.5, 0, -0.3]), (0.8, [-0.4, 0.3, 0.3]),
           (1.0, [0.3, 0, 0]), (1.5, [0, -1, 0]), (2.0, [0, 2, 0], "easeOutExpo"), (3.0, [0, -6, 0], "easeInCubic"))
    # The plates fly straight out along their own axes, tumbling, and are drawn back past the core.
    for j, bone in enumerate(SHELLS):
        d = SHELL_DIRS[bone][0]
        tumble = [(-1) ** j * 40, 25 * (j % 3 - 1), (-1) ** (j // 2) * 35]

        def out(k):
            return [d[0] * k, d[1] * k, d[2] * k]
        sh.pos(bone, (0, Z3), (1.0, out(1.0), "easeInOutSine"), (1.5, out(-1.5), "easeInCubic"), (2.0, out(24), "easeOutExpo"),
               (2.3, out(26), "easeOutQuad"), (2.9, out(-8), "easeInCubic"), (3.0, out(-8)))
        sh.rot(bone, (0, Z3), (1.5, Z3), (2.0, [v * 0.6 for v in tumble], "easeOutExpo"), (2.3, tumble, "easeOutQuad"),
               (3.0, [v * 3 for v in tumble], "easeInCubic"))
        sh.scale(bone, (0, ONE), (1.5, ONE), (2.0, s3(1.25), "easeOutExpo"), (2.9, s3(0.4), "easeInCubic"), (3.0, s3(0.4)))
    sh.scale("corners", (0, ONE), (1.5, s3(0.85)), (2.0, s3(1.4), "easeOutExpo"), (2.9, s3(0.3), "easeInCubic"), (3.0, s3(0.3)))
    sh.rot("corners", (0, Z3), (2.0, [0, 30, 0], "easeOutExpo"), (3.0, [0, -200, 0], "easeInCubic"))
    for bone, k in (("motes", 1.8), ("motes_inner", 2.0), ("veil_skirt", 1.7), ("veil", 1.5), ("veil_crown", 1.6)):
        sh.scale(bone, (0, ONE), (1.5, s3(0.7), "easeInCubic"), (2.0, s3(k), "easeOutExpo"), (2.75, s3(0.1), "easeInCubic"), (3.0, s3(0)))
    sh.rot("motes", (0, Z3), (2.0, [0, 60, 0]), (3.0, [0, 900, 0], "easeInCubic"))
    sh.rot("veil_skirt", (0, Z3), (2.0, [0, 40, 0]), (3.0, [0, -600, 0], "easeInCubic"))
    sh.scale("core", (0, ONE), (1.0, s3(1.3), "easeInOutSine"), (1.5, s3(0.6), "easeInCubic"), (2.0, s3(2.4), "easeOutExpo"),
             (3.0, s3(0.2), "easeInCubic"))
    sh.scale("core_corona", (0, ONE), (1.0, s3(1.4)), (1.5, s3(0.3), "easeInCubic"), (2.0, s3(3.0), "easeOutExpo"),
             (2.8, s3(0.0), "easeInCubic"), (3.0, s3(0)))
    for i in range(4):
        sh.scale(f"ring_{i}", (0, ONE), (1.6, s3(1.2), "easeOutQuad"), (2.6, s3(0), "easeInBack"), (3.0, s3(0)))
    for i in TENTACLES:
        sh.scale(f"tentacle_{i}", (0, ONE), (1.8, ONE), (2.6, s3(0), "easeInBack"), (3.0, s3(0)))


def _form_clips(f):
    a = f.new("animation.amara.step", 1.0)
    a.scale("form", (0, ONE), (0.4, [0.2, 1.4, 0.2], "easeInQuad"), (0.6, [0.2, 1.4, 0.2]), (1.0, ONE, "easeOutBack"))
    a.rot("form_hair", (0, Z3), (0.4, [25, 0, 0], "easeInQuad"), (1.0, Z3, "easeOutQuad"))
    a.scale("form_skirt", (0, ONE), (0.4, [1.4, 0.8, 1.4], "easeInQuad"), (0.6, [1.4, 0.8, 1.4]), (1.0, ONE, "easeOutBack"))
    a = f.new("animation.amara.unmake", 2.0)
    a.rot("form_arm_left", (0, Z3), (1.0, [0, 0, -150], "easeOutQuad"), (2.0, Z3, "easeInQuad"))
    a.rot("form_arm_right", (0, Z3), (1.0, [0, 0, 150], "easeOutQuad"), (2.0, Z3, "easeInQuad"))
    a.rot("form_head", (0, Z3), (1.0, [-25, 0, 0], "easeOutQuad"), (2.0, Z3))
    a.scale("form_halo", (0, ONE), (1.0, s3(1.5), "easeOutQuad"), (2.0, ONE))
    a.rot("form_halo", (0, Z3), (2.0, [0, 0, 180], "easeInOutSine"))
    a.rot("form_skirt", (0, Z3), (1.0, [-6, 0, 0], "easeOutQuad"), (2.0, Z3))


# --- her arena ------------------------------------------------------------------------------

def light_well_textures():
    gold = Ramp("#3a2a12", "#5c4320", "#8a6a2e", "#b8913f", "#dcb860", "#f6e3a0")
    stone = noise(P.STONE if hasattr(P, "STONE") else Ramp("#3a3a40", "#4c4c54", "#5f5f68", "#74747c", "#8a8a92", "#a0a0a8"), 7, 2.4, 1.0)

    def side(lit):
        t = Tex(16, 16, 1)
        for y in range(16):
            for x in range(16):
                c = stone("north", x, y, 16, 16)
                if y in (0, 1, 14, 15) or x in (0, 15):
                    c = gold[3 if (x + y) % 3 else 2]
                elif 5 <= y <= 9 and 5 <= x <= 10:
                    # The sun-sigil: lit, it shines; out, it is a cold socket.
                    c = (gold[5] if lit else gold[1]) if (x - 7.5) ** 2 + (y - 7) ** 2 < 6 else c
                t.set(x, y, c)
        return t

    def top(lit):
        t = Tex(16, 16, 2)
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - 7.5, y - 7.5)
                if r > 6.5:
                    c = gold[3 if (x + y) % 2 else 4]
                elif lit:
                    n = fbm(x * 5, y * 5, 3, 32, 32, 2, 3.0)
                    c = mix(hexc("#fff3c4"), hexc("#ffb040"), min(1, r / 6.5 + (n - 0.5) * 0.6))
                else:
                    c = mix(hexc("#1a1612"), hexc("#3a3430"), fbm(x * 5, y * 5, 4, 32, 32, 2, 3.0))
                t.set(x, y, c)
        return t

    bottom = Tex(16, 16, 3)
    for y in range(16):
        for x in range(16):
            bottom.set(x, y, gold[1] if (x + y) % 4 else gold[2])
    save(side(True), "block", "light_well_side")
    save(side(False), "block", "light_well_side_out")
    save(top(True), "block", "light_well_top_lit")
    save(top(False), "block", "light_well_top")
    save(bottom, "block", "light_well_bottom")


def void_mote():
    for i in range(4):
        t = Tex(8, 8, 5)
        r = 3.6 - i * 0.6
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                if d < r:
                    k = d / r
                    c = mix(hexc("#3a2160"), hexc("#06030c"), k)
                    t.set(x, y, (c[0], c[1], c[2], int(255 * (1 - k * 0.6))))
        if i < 3:
            t.set(3, 3, hexc("#b48cff"))
        save(t, "particle", f"void_mote_{i}")


def consumption_vignette():
    """The dark closing in at the screen's edges: tendrils of smoke reaching inward from the rim, a violet bruise where
    they thin, black at the edge."""
    n = 128
    t = Tex(n, n, 6)
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            d = math.hypot(dx * 0.92, dy)
            ang = math.atan2(dy, dx)
            # Tendrils: the edge of the dark reaches further in along a few angles, wavering.
            reach = fbm((ang / math.tau + 0.5) * 64, d * 6, 9, 64, 64, 3, 8.0)
            tendril = max(0.0, math.sin(ang * 7 + d * 5) * 0.5 + 0.5) ** 3 * 0.18
            edge = 0.42 - (reach - 0.5) * 0.35 - tendril
            a = (d - edge) / 0.72
            if a <= 0:
                continue
            a = min(1.0, a) ** 1.5
            wisp = fbm(x * 2, y * 2, 10, 64, 64, 3, 6.0)
            c = mix(hexc("#2a1048"), hexc("#000000"), min(1, a * 1.3 + 0.1))
            alpha = min(1.0, a * (0.75 + wisp * 0.55))
            t.set(x, y, (c[0], c[1], c[2], int(255 * alpha)))
    save(t, "misc", "consumption_vignette")


def tentacle_texture():
    """Night with a crawling violet seam and stars, tiling along the tentacle."""
    t = Tex(16, 64, 11)
    for y in range(64):
        for x in range(16):
            n = fbm(x * 3, y * 3, 12, 48, 192, 3, 5.0)
            c = OLD_VOID[min(5, int(1 + n * 3))]
            if abs((x + y // 3) % 16 - 8) < 1:
                c = OLD_RIM
            if _h("tent", x, y) < 0.025:
                c = OLD_STAR[int(_h("tc", x, y) * 4) % 4]
            t.set(x, y, c)
    save(t, "effect", "tentacle")


def energy_ring():
    """A soft band of light, brightest at its outer edge (v = 0), fading inward."""
    t = Tex(32, 16, 12)
    for y in range(16):
        for x in range(32):
            k = 1 - y / 15
            flicker = 0.75 + 0.25 * fbm(x * 4, y * 2, 13, 128, 64, 2, 4.0)
            t.set(x, y, (255, 255, 255, int(255 * (k ** 1.5) * flicker)))
    save(t, "effect", "energy_ring")


def eclipse_trophy():
    """Her likeness in miniature: a black orb ringed in corona on the trophies' black pedestal."""
    orb = Tex(16, 16, 21)
    for y in range(16):
        for x in range(16):
            c = OLD_VOID[1 + int(fbm(x * 4, y * 4, 22, 64, 64, 2, 3.0) * 3)]
            if _h("orb", x, y) < 0.05:
                c = OLD_STAR[int(_h("os", x, y) * 4) % 4]
            orb.set(x, y, c)
    corona = Tex(16, 16, 23)
    for y in range(16):
        for x in range(16):
            corona.set(x, y, OLD_CORONA[(x + y) % 3] if (x + y) % 5 else hexc("#ffffff"))
    save(orb, "block", "eclipse_trophy_orb")
    save(corona, "block", "eclipse_trophy_corona")

    def el(name, frm, to, tex, uv=None):
        faces = {}
        for f in ("north", "south", "east", "west", "up", "down"):
            faces[f] = {"uv": uv or [0, 0, 16, 16], "texture": "#" + tex}
        return {"name": name, "from": frm, "to": to, "faces": faces}
    elements = [
        el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
        el("stem", [7, 3, 7], [9, 6, 9], "base", [7, 7, 9, 9]),
        el("orb", [5, 6, 5], [11, 12, 11], "orb", [5, 5, 11, 11]),
        el("ring_top", [4, 12, 7.5], [12, 13, 8.5], "corona", [0, 0, 8, 1]),
        el("ring_bottom", [4, 5, 7.5], [12, 6, 8.5], "corona", [0, 1, 8, 2]),
        el("ring_left", [4, 6, 7.5], [5, 12, 8.5], "corona", [0, 2, 1, 8]),
        el("ring_right", [11, 6, 7.5], [12, 12, 8.5], "corona", [1, 2, 2, 8]),
        el("ray_up", [7.5, 13, 7.5], [8.5, 15, 8.5], "corona", [2, 2, 3, 4]),
        el("ray_left", [2, 8.5, 7.5], [4, 9.5, 8.5], "corona", [3, 2, 5, 3]),
        el("ray_right", [12, 8.5, 7.5], [14, 9.5, 8.5], "corona", [5, 2, 7, 3]),
    ]
    model = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
             "textures": {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base",
                          "orb": "supernaturalcraft:block/eclipse_trophy_orb", "corona": "supernaturalcraft:block/eclipse_trophy_corona"},
             "elements": elements}
    with open(ASSETS + "/models/block/eclipse_trophy.json", "w") as fh:
        json.dump(model, fh, indent=1)
        fh.write("\n")


FROZEN = ["root", "mass", "core", "shell_n", "shell_s", "shell_e", "shell_w", "shell_up", "shell_down", "veil", "motes",
          "corners", "ring_0", "ring_1", "ring_2", "ring_3",
          "tentacle_0", "tentacle_1", "tentacle_2", "tentacle_3", "tentacle_4", "tentacle_5",
          "cyst_0", "cyst_1", "cyst_2", "cyst_3", "cyst_4", "cyst_5",
          "form", "form_torso", "form_head", "form_crown", "form_halo", "form_hair",
          "form_arm_left", "form_arm_right", "form_leg_left", "form_leg_right"]
PARENTS = {"mass": "root", "core": "mass", "veil": "mass", "motes": "mass", "corners": "mass", "form": "root",
           "form_torso": "form", "form_head": "form_torso", "form_crown": "form_head", "form_halo": "form_head",
           "form_hair": "form_head", "form_arm_left": "form_torso", "form_arm_right": "form_torso",
           "form_leg_left": "form", "form_leg_right": "form",
           **{s: "mass" for s in SHELLS}, **{f"ring_{i}": "root" for i in range(4)},
           **{f"tentacle_{i}": "root" for i in range(6)}, **{f"cyst_{i}": f"tentacle_{i}" for i in range(6)}}
MASS_LOOPS = ["idle", "idle_open", "form_idle"]
MASS_TRIGGERED = ["emerge", "expose", "close", "transform_2", "transform_3", "transform_4", "death",
                  "cast", "orbit", "well", "lance", "rain", "snuff", "spawn",
                  "slam", "sweep", "grasp", "spikes", "bloom", "flare", "totality", "collapse", "step", "unmake", "shatter"]
# Bones a renderer that hides mass/rings/tentacles still shows: they must all be form's.
SHOWN_ALONE = ("form",)


def check(b, a):
    """The contract with AmaraAssets: frozen bones and parents, every clip, only real bones animated, cysts on their
    hit boxes, and every added bone under a bone the renderers hide."""
    bones = {x.name: x for x in b.rig.bones}
    for name in FROZEN:
        assert name in bones, "missing frozen bone " + name
        if name in PARENTS:
            assert bones[name].parent == PARENTS[name], f"{name} parent {bones[name].parent} != {PARENTS[name]}"
    names = set(a.names())
    for clip in MASS_LOOPS + MASS_TRIGGERED:
        assert "animation.amara." + clip in names, "missing clip " + clip
    for anim in a.anims:
        for bone in anim.bones:
            assert bone in bones, f"{anim.name} animates unknown bone {bone}"
        for i in range(4):
            assert "position" not in anim.bones.get(f"ring_{i}", {}), f"{anim.name} moves ring_{i} (Java places it)"
    roots = {"mass", "form"} | {f"ring_{i}" for i in range(4)} | {f"tentacle_{i}" for i in range(6)}
    for x in b.rig.bones:
        if x.name in FROZEN:
            continue
        p = x
        while p.parent and p.name not in roots:
            p = bones[p.parent]
        assert p.name in roots, f"added bone {x.name} is not under mass, a ring, a tentacle or form"
    for i in range(6):
        wx, wz = cyst_world(i)
        a_ = tentacle_angle(i)
        want = (math.cos(a_) * CYST_RING, -math.sin(a_) * CYST_RING)
        assert abs(wx - want[0]) < 0.5 and abs(wz - want[1]) < 0.5, f"cyst_{i} at {(wx, wz)}, hit box at {want}"
        assert abs(bones[f"cyst_{i}"].pivot[1] - 4) < 1e-6


def generate():
    eclipse_trophy()
    tentacle_texture()
    energy_ring()
    b = rig()
    t, g = b.build()
    a = anims()
    check(b, a)
    b.rig.write(GEO + "amara.geo.json")
    save(t, "entity", "amara")
    save(g, "entity", "amara_glowmask")
    a.write(ANIM + "amara.animation.json")
    with open(ASSETS + "/animations/entity/amara.names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")
    light_well_textures()
    void_mote()
    consumption_vignette()
