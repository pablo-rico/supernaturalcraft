"""Amara, the Darkness: her model, texture, glowmask and animations, and the art of her arena.

One rig holds both bodies. ``mass`` (phases 1-3) is a lump of night around a black-sun ``core``,
wrapped in six ``shell_*`` slabs that open in phase 3; four ``ring_*`` anchors orbit it (the Java
model moves them onto their hit boxes) and six ``tentacle_*`` reach the ground in phase 2, each
ending in a glowing ``cyst_*``. ``form`` is her final shape: a tall, slender silhouette of void.

Coordinates are Bedrock pixels with the origin at her feet; the renderer scales the mass by SCALE
(and the final form by half that), so world sizes in AmaraEntity are pixels * SCALE / 16. GeckoLib mirrors X and the renderer
turns her 180 degrees (she keeps yaw 0), so a world offset (dx, dz) is Bedrock (dx, -dz).
"""

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

VOID = Ramp("#020105", "#06040c", "#0d0818", "#170e29", "#24153d", "#3a2160")
OBSIDIAN = Ramp("#07050c", "#110c1c", "#1b142c", "#281d3f", "#3a2b57", "#4f3c73")
CYST = Ramp("#12040e", "#2a0820", "#4d0f3a", "#7a1a5e", "#a8358a", "#e07ad0")
STAR = [hexc("#ffffff"), hexc("#e8deff"), hexc("#c8b4ff"), hexc("#b48cff")]
VIOLET = hexc("#b48cff")
CORONA = [hexc("#fff8ec"), hexc("#ffe9b8"), hexc("#e0c8ff")]

SCALE = 2.2                    # AmaraEntity.MODEL_SCALE: the renderer draws the mass this much larger
MASS_C = 5.28 * 16 / SCALE     # AmaraEntity.MASS_CENTER, in model pixels
CYST_RING = 6.5 * 16 / SCALE   # AmaraEntity.CYST_RING, in model pixels


def _h(*parts):
    """A stable pseudo-random number in [0, 1) from any hashable parts."""
    return random.Random(zlib.crc32(repr(parts).encode())).random()


class Body:
    """Rig plus per-cube materials and glow, painted once packed."""

    def __init__(self, name, size):
        self.rig = Rig(name, size, size)
        self.paints = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, inflate=0.0, faces=None, tag=None):
        c = box(bone, origin, size, inflate=inflate, faces=faces, tag=tag)
        self.paints.append((c, material, glow))
        return c

    def build(self):
        self.rig.pack(gutter=1)
        t = Tex(self.rig.tex_w, self.rig.tex_h, 3)
        g = Tex(self.rig.tex_w, self.rig.tex_h, 4)
        for c, material, glow in self.paints:
            paint_cube(t, c, material)
            if glow:
                paint_cube(g, c, glow, shading=False)
                paint_cube(t, c, lambda f, x, y, w, h, gl=glow, m=material: gl(f, x, y, w, h) or m(f, x, y, w, h))
        return t, g


# --- materials -----------------------------------------------------------------------------

RIM = hexc("#4a2a86")


def night(seed, density=0.03, rim=0.45):
    """Void-black, slowly rippling, with stars caught in it and a faint violet rim at the edges, so
    her silhouette still reads against an eclipsed sky."""
    base = noise(VOID, seed, 2.0, 1.4, 7.0)

    def stars(face, x, y, w, h):
        r = _h(seed, face, x, y)
        if r < density:
            return STAR[int(r / density * len(STAR)) % len(STAR)]
        edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
        if edge and w > 3 and h > 3 and _h(seed, face, x, y, "rim") < rim:
            return RIM
        return None
    return base, stars


def obsidian_glyphs(seed):
    base = noise(OBSIDIAN, seed, 2.4, 1.2, 4.0)

    def glyphs(face, x, y, w, h):
        if face not in ("north", "south"):
            return None
        # Short broken strokes along the bar, like writing older than words.
        if (y == h // 2) and _h(seed, x // 2) < 0.55 and x % 2 == 0:
            return VIOLET
        return None
    return base, glyphs


def core_material(face, x, y, w, h):
    return hexc("#000000")


def core_glow(face, x, y, w, h):
    """A black disc with a burning rim: the eclipsed sun inside her."""
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = math.hypot(x - cx, y - cy) / (w / 2)
    if d > 0.98:
        return CORONA[2]
    if d > 0.78:
        return CORONA[0] if _h("core", face, x, y) < 0.7 else CORONA[1]
    return None


def cyst_material(seed):
    base = noise(CYST, seed, 1.8, 1.4, 3.0)

    def glow(face, x, y, w, h):
        # Veins of sick light under the skin.
        n = fbm(x * 4, y * 4, seed + len(face), 32, 32, 2, 2.5)
        return CYST[5] if 0.47 < n < 0.53 else (CYST[4] if 0.44 < n < 0.56 else None)
    return base, glow


# --- the rig -------------------------------------------------------------------------------

def rig():
    b = Body("amara", 512)
    root = b.bone("root", (0, 0, 0))
    mass = b.bone("mass", (0, MASS_C, 0), "root")
    c = MASS_C

    core = b.bone("core", (0, c, 0), "mass")
    b.cube(core, (-7, c - 7, -7), (14, 14, 14), core_material, core_glow, tag="core")

    # Six slabs close around the core; their edges leave the lumpy outline of a dark star.
    slabs = {
        "shell_n": ((-15, c - 17, -23), (30, 34, 8)),
        "shell_s": ((-15, c - 17, 15), (30, 34, 8)),
        "shell_e": ((15, c - 15, -15), (8, 30, 30)),
        "shell_w": ((-23, c - 15, -15), (8, 30, 30)),
        "shell_up": ((-14, c + 14, -14), (28, 9, 28)),
        "shell_down": ((-13, c - 22, -13), (26, 8, 26)),
    }
    for i, (name, (o, s)) in enumerate(slabs.items()):
        cx, cy, cz = o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2
        bone = b.bone(name, (cx, cy, cz), "mass")
        mat, stars = night(10 + i)
        b.cube(bone, o, s, mat, stars, tag=name)
        # Lumps on each slab so she never reads as a box.
        rnd = random.Random(40 + i)
        for k in range(4):
            sz = rnd.randint(7, 12)
            jx = rnd.uniform(-0.35, 0.35)
            jy = rnd.uniform(-0.35, 0.35)
            if s[2] <= 9 and s[0] > 9:      # north/south slabs bulge along z
                lo = (cx + jx * s[0] - sz / 2, cy + jy * s[1] - sz / 2, (o[2] - sz + 4) if o[2] < 0 else (o[2] + s[2] - 4))
            elif s[0] <= 9:                  # east/west along x
                lo = ((o[0] - sz + 4) if o[0] < 0 else (o[0] + s[0] - 4), cy + jy * s[1] - sz / 2, cz + jx * s[2] - sz / 2)
            else:                            # top/bottom along y
                lo = (cx + jx * s[0] - sz / 2, (o[1] + s[1] - 4) if o[1] > c else (o[1] - sz + 4), cz + jy * s[2] - sz / 2)
            m2, st2 = night(60 + i * 7 + k)
            b.cube(bone, lo, (sz, sz, sz), m2, st2, tag=f"{name}_lump{k}")
    # Corner masses fill the seams between the slabs.
    corners = b.bone("corners", (0, c, 0), "mass")
    for i, (sx, sy, sz) in enumerate([(sx, sy, sz) for sx in (-1, 1) for sy in (-1, 1) for sz in (-1, 1)]):
        m, st = night(90 + i)
        b.cube(corners, (sx * 15 - 7, c + sy * 13 - 7, sz * 15 - 7), (14, 14, 14), m, st, tag=f"corner{i}")
    # A torn veil of darkness hanging below her.
    veil = b.bone("veil", (0, c - 22, 0), "mass")
    rnd = random.Random(7)
    for i in range(10):
        a = i / 10 * math.tau + rnd.uniform(-0.2, 0.2)
        r = rnd.uniform(6, 14)
        length = rnd.randint(6, 14)
        m, st = night(120 + i, 0.03)
        b.cube(veil, (math.cos(a) * r - 1, c - 22 - length, math.sin(a) * r - 1), (2, length, 2), m, st, tag=f"veil{i}")
    # Motes of night drifting around her.
    motes = b.bone("motes", (0, c, 0), "mass")
    for i in range(8):
        a = i / 8 * math.tau
        r = 30 + (i % 3) * 4
        m, st = night(140 + i, 0.2)
        b.cube(motes, (math.cos(a) * r - 1.5, c + math.sin(i * 1.7) * 10 - 1.5, math.sin(a) * r - 1.5), (3, 3, 3), m, st, tag=f"mote{i}")

    # Anchors: square rings of glyph-cut obsidian, built around the origin; Java places them.
    for i in range(4):
        ring = b.bone(f"ring_{i}", (0, 0, 0), "root")
        mat, glyphs = obsidian_glyphs(200 + i)
        for k, (o, s) in enumerate([((-11, 8, -1.5), (22, 3, 3)), ((-11, -11, -1.5), (22, 3, 3)),
                                    ((-11, -8, -1.5), (3, 16, 3)), ((8, -8, -1.5), (3, 16, 3))]):
            b.cube(ring, o, s, mat, glyphs, tag=f"ring{i}_{k}")
        gm, gg = obsidian_glyphs(210 + i)
        b.cube(ring, (-2, -2, -2), (4, 4, 4), gm, lambda f, *a: VIOLET, tag=f"ring{i}_eye")

    # Tentacles: chains of shrinking voxels from her underside to a cyst on the ground.
    for i in range(6):
        a = i * math.pi / 3 + math.pi / 6
        wx, wz = math.cos(a), math.sin(a)
        bx, bz = wx, -wz                               # world → Bedrock (see the module doc)
        start = (bx * 14, c - 16, bz * 14)
        end = (bx * CYST_RING, 4, bz * CYST_RING)
        ctrl = (bx * 34, c - 6, bz * 34)
        t_bone = b.bone(f"tentacle_{i}", start, "root")
        steps = 14
        for k in range(steps):
            u = k / (steps - 1)
            # Quadratic Bezier: up and out, then down onto the cyst.
            px = (1 - u) ** 2 * start[0] + 2 * (1 - u) * u * ctrl[0] + u * u * end[0]
            py = (1 - u) ** 2 * start[1] + 2 * (1 - u) * u * ctrl[1] + u * u * end[1]
            pz = (1 - u) ** 2 * start[2] + 2 * (1 - u) * u * ctrl[2] + u * u * end[2]
            sz = max(2, round(7 - u * 4))
            m, st = night(300 + i * 20 + k, 0.03)
            b.cube(t_bone, (px - sz / 2, py - sz / 2, pz - sz / 2), (sz, sz, sz), m, st, tag=f"t{i}_{k}")
        cyst = b.bone(f"cyst_{i}", end, f"tentacle_{i}")
        cm, cg = cyst_material(400 + i)
        b.cube(cyst, (end[0] - 5, end[1] - 4, end[2] - 5), (10, 6, 10), cm, cg, tag=f"cyst{i}")
        cm2, cg2 = cyst_material(410 + i)
        b.cube(cyst, (end[0] - 3, end[1] + 2, end[2] - 3), (6, 4, 6), cm2, cg2, tag=f"cyst{i}_dome")

    form_bones(b)
    return b


def form_bones(b):
    """Her last shape: five blocks of void in the outline of a woman, crowned, haloed by an eclipse."""
    form = b.bone("form", (0, 0, 0), "root")
    torso = b.bone("form_torso", (0, 36, 0), "form")
    b.cube(torso, (-5, 38, -2.5), (10, 22, 5), *night(500, 0.025), tag="f_torso")
    b.cube(torso, (-3.5, 34, -2), (7, 5, 4), *night(501, 0.02), tag="f_waist")
    b.cube(torso, (-4.5, 31, -2.5), (9, 3, 5), *night(504, 0.02), tag="f_hips")
    head = b.bone("form_head", (0, 60, 0), "form_torso")
    b.cube(head, (-1, 60, -1), (2, 2, 2), *night(502, 0.0), tag="f_neck")

    def eyes(face, x, y, w, h):
        if face == "north" and y == 4 and x in (1, 5):
            return hexc("#ffffff")
        return None
    hm, hs = night(503, 0.02)
    b.cube(head, (-3.5, 62, -3.5), (7, 9, 7), hm, lambda f, x, y, w, h: eyes(f, x, y, w, h) or hs(f, x, y, w, h), tag="f_head")
    crown = b.bone("form_crown", (0, 71, 0), "form_head")
    for i in range(7):
        a = (i - 3) * 0.33
        hgt = 9 - abs(i - 3) * 1.5
        b.cube(crown, (math.sin(a) * 3.5 - 0.5, 70.5, -math.cos(a) * 1.5 - 0.5), (1, round(hgt), 1), *night(510 + i, 0.15), tag=f"f_spike{i}")
    # The eclipse behind her head: a ring of black stones rimmed in corona.
    halo = b.bone("form_halo", (0, 66, 6), "form_head")
    for i in range(14):
        a = i / 14 * math.tau
        b.cube(halo, (math.cos(a) * 11 - 1.5, 66 + math.sin(a) * 11 - 1.5, 6), (3, 3, 1), core_material,
               lambda f, x, y, w, h, k=i: CORONA[k % 3] if f in ("north", "south") else None, tag=f"f_halo{i}")
    hair = b.bone("form_hair", (0, 68, 3), "form_head")
    for i in range(6):
        length = 16 + (i * 5) % 11
        b.cube(hair, (-3.5 + i * 1.3, 68 - length, 3 + (i % 2)), (1, length, 1), *night(520 + i, 0.05), tag=f"f_hair{i}")
    for side, sx in (("left", 1), ("right", -1)):
        arm = b.bone(f"form_arm_{side}", (sx * 7, 58, 0), "form_torso")
        b.cube(arm, (sx * 7 - 1, 27, -1), (2, 32, 2), *night(530 + sx, 0.03), tag=f"f_arm_{side}")
        for k in range(3):
            b.cube(arm, (sx * 7 - 1 + k * 0.7 * sx, 19, -1 + k * 0.6), (1, 8, 1), *night(533 + sx * 3 + k, 0.0), tag=f"f_claw_{side}{k}")
        leg = b.bone(f"form_leg_{side}", (sx * 2.6, 33, 0), "form")
        b.cube(leg, (sx * 2.6 - 2, 17, -2), (4, 16, 4), *night(540 + sx, 0.02), tag=f"f_thigh_{side}")
        b.cube(leg, (sx * 2.6 - 1.5, 0, -1.5), (3, 17, 3), *night(542 + sx, 0.02), tag=f"f_shin_{side}")


# --- animations ----------------------------------------------------------------------------

SHELL_OUT = {
    "shell_n": ([0, 0, -9], [-14, 0, 0]),
    "shell_s": ([0, 0, 9], [14, 0, 0]),
    "shell_e": ([9, 0, 0], [0, 0, -14]),
    "shell_w": ([-9, 0, 0], [0, 0, 14]),
    "shell_up": ([0, 9, 0], [0, 0, 0]),
    "shell_down": ([0, -5, 0], [0, 0, 0]),
}


def _open(a, t0, t1, amount=1.0, ease="easeOutBack"):
    for bone, (pos, rot) in SHELL_OUT.items():
        a.pos(bone, (t0, [0, 0, 0]), (t1, [v * amount for v in pos], ease))
        a.rot(bone, (t0, [0, 0, 0]), (t1, [v * amount for v in rot], ease))


def _held_open(a, amount=1.0):
    for bone, (pos, rot) in SHELL_OUT.items():
        a.pos(bone, (0, [v * amount for v in pos]), (a.length, [v * amount for v in pos]))
        a.rot(bone, (0, [v * amount for v in rot]), (a.length, [v * amount for v in rot]))


def anims():
    f = AnimFile()
    idle = f.new("animation.amara.idle", 4.0, loop=True)
    idle.scale("mass", (0, [1, 1, 1]), (2.0, [1.04, 0.97, 1.04], "easeInOutSine"), (4.0, [1, 1, 1], "easeInOutSine"))
    idle.rot("motes", (0, [0, 0, 0]), (4.0, [0, 90, 0]))
    idle.rot("veil", (0, [0, 0, 0]), (2.0, [3, 0, -3], "easeInOutSine"), (4.0, [0, 0, 0], "easeInOutSine"))
    for i in range(6):
        idle.rot(f"tentacle_{i}", (0, [0, 0, 0]), (2.0 + i * 0.1, [0, 0, 4 if i % 2 else -4], "easeInOutSine"), (4.0, [0, 0, 0], "easeInOutSine"))
        idle.scale(f"cyst_{i}", (0, [1, 1, 1]), (0.6 + i * 0.1, [1.15, 1.1, 1.15], "easeInOutSine"), (1.6, [1, 1, 1], "easeInOutSine"),
                   (4.0, [1, 1, 1]))

    io = f.new("animation.amara.idle_open", 4.0, loop=True)
    _held_open(io)
    io.scale("core", (0, [1, 1, 1]), (1.0, [1.18, 1.18, 1.18], "easeInOutSine"), (2.0, [1, 1, 1], "easeInOutSine"),
             (3.0, [1.18, 1.18, 1.18], "easeInOutSine"), (4.0, [1, 1, 1], "easeInOutSine"))
    io.rot("core", (0, [0, 0, 0]), (4.0, [0, 180, 0]))
    io.rot("motes", (0, [0, 0, 0]), (4.0, [0, 180, 0]))

    fi = f.new("animation.amara.form_idle", 3.0, loop=True)
    fi.rot("form_hair", (0, [0, 0, 0]), (1.5, [8, 0, 0], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    fi.rot("form_arm_left", (0, [0, 0, -4]), (1.5, [0, 0, -7], "easeInOutSine"), (3.0, [0, 0, -4], "easeInOutSine"))
    fi.rot("form_arm_right", (0, [0, 0, 4]), (1.5, [0, 0, 7], "easeInOutSine"), (3.0, [0, 0, 4], "easeInOutSine"))
    fi.pos("form", (0, [0, 0, 0]), (1.5, [0, 1.5, 0], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    fi.rot("form_halo", (0, [0, 0, 0]), (3.0, [0, 0, 120]))

    em = f.new("animation.amara.emerge", 6.0, hold=True)
    em.scale("mass", (0, [0.05, 0.05, 0.05]), (1.5, [0.2, 0.6, 0.2], "easeInQuad"), (4.0, [1.15, 1.15, 1.15], "easeOutBack"),
             (6.0, [1, 1, 1], "easeInOutSine"))
    em.rot("mass", (0, [0, 0, 0]), (4.0, [0, 540, 0], "easeOutQuad"), (6.0, [0, 540, 0]))
    for i in range(4):
        em.scale(f"ring_{i}", (0, [0, 0, 0]), (3.5 + i * 0.3, [0, 0, 0]), (4.5 + i * 0.3, [1, 1, 1], "easeOutBack"), (6.0, [1, 1, 1]))
    _open(em, 4.4, 5.0, 0.6, "easeOutQuad")
    for bone, (pos, rot) in SHELL_OUT.items():
        em.pos(bone, (5.0, [v * 0.6 for v in pos]), (6.0, [0, 0, 0], "easeInQuad"))
        em.rot(bone, (5.0, [v * 0.6 for v in rot]), (6.0, [0, 0, 0], "easeInQuad"))

    ex = f.new("animation.amara.expose", 1.0, hold=True)
    _open(ex, 0, 1.0, 0.7)
    ex.scale("core", (0, [1, 1, 1]), (0.3, [1.5, 1.5, 1.5], "easeOutQuad"), (1.0, [1.2, 1.2, 1.2]))
    cl = f.new("animation.amara.close", 1.0)
    for bone, (pos, rot) in SHELL_OUT.items():
        cl.pos(bone, (0, [v * 0.7 for v in pos]), (1.0, [0, 0, 0], "easeInBack"))
        cl.rot(bone, (0, [v * 0.7 for v in rot]), (1.0, [0, 0, 0], "easeInBack"))

    t2 = f.new("animation.amara.transform_2", 3.0)
    t2.scale("mass", (0, [1, 1, 1]), (0.8, [0.7, 1.3, 0.7], "easeInQuad"), (1.6, [1.3, 0.8, 1.3], "easeOutBack"), (3.0, [1, 1, 1], "easeInOutSine"))
    for i in range(6):
        t2.scale(f"tentacle_{i}", (0, [0, 0, 0]), (1.4 + i * 0.12, [0, 0, 0]), (2.4 + i * 0.12, [1, 1, 1], "easeOutBack"))
    t3 = f.new("animation.amara.transform_3", 3.0)
    _open(t3, 1.0, 2.2, 1.0)
    t3.scale("core", (0, [1, 1, 1]), (1.0, [0.6, 0.6, 0.6], "easeInQuad"), (2.2, [1.6, 1.6, 1.6], "easeOutBack"), (3.0, [1, 1, 1]))
    t4 = f.new("animation.amara.transform_4", 4.0)
    t4.scale("form", (0, [0.1, 0.1, 0.1]), (2.0, [0.4, 1.2, 0.4], "easeInQuad"), (3.2, [1.05, 1.05, 1.05], "easeOutBack"), (4.0, [1, 1, 1]))
    t4.rot("form", (0, [0, 720, 0]), (3.2, [0, 0, 0], "easeOutQuad"))

    de = f.new("animation.amara.death", 6.0, hold=True)
    for bone in ("mass", "form"):
        de.scale(bone, (0, [1, 1, 1]), (2.0, [1.2, 0.8, 1.2], "easeInOutSine"), (5.0, [0.4, 1.4, 0.4], "easeInQuad"),
                 (6.0, [0, 0, 0], "easeInBack"))
        de.rot(bone, (0, [0, 0, 0]), (6.0, [0, 900, 0], "easeInQuad"))
    _open(de, 0.5, 2.0, 1.2)

    def short(name, length):
        return f.new("animation.amara." + name, length)

    a = short("cast", 1.0)
    a.scale("core", (0, [1, 1, 1]), (0.4, [1.4, 1.4, 1.4], "easeOutQuad"), (1.0, [1, 1, 1]))
    _open(a, 0, 0.4, 0.25)
    a = short("orbit", 1.0)
    a.rot("mass", (0, [0, 0, 0]), (1.0, [0, 360, 0], "easeInOutSine"))
    a = short("well", 1.5)
    a.scale("mass", (0, [1, 1, 1]), (0.8, [0.75, 0.75, 0.75], "easeInQuad"), (1.1, [1.2, 1.2, 1.2], "easeOutBack"), (1.5, [1, 1, 1]))
    a = short("lance", 1.75)
    a.pos("shell_n", (0, [0, 0, 0]), (1.2, [0, 0, -6], "easeInQuad"), (1.75, [0, 0, 0], "easeOutQuad"))
    a.scale("core", (0, [1, 1, 1]), (1.6, [1.6, 1.6, 1.6], "easeInQuad"), (1.75, [1, 1, 1]))
    a = short("rain", 1.25)
    a.pos("mass", (0, [0, 0, 0]), (0.8, [0, 10, 0], "easeOutQuad"), (1.25, [0, 0, 0], "easeInBack"))
    a = short("snuff", 1.5)
    a.scale("mass", (0, [1, 1, 1]), (1.3, [0.85, 0.85, 0.85], "easeInQuad"), (1.5, [1.25, 1.25, 1.25], "easeOutBack"))
    a.rot("motes", (0, [0, 0, 0]), (1.5, [0, -540, 0], "easeInQuad"))
    a = short("spawn", 1.5)
    _open(a, 0, 0.8, 0.4)
    for name, length in (("slam", 1.5), ("sweep", 1.5), ("grasp", 1.5), ("spikes", 1.5), ("bloom", 1.5)):
        a = short(name, length)
        for i in range(6):
            a.rot(f"tentacle_{i}", (0, [0, 0, 0]), (length * 0.6, [0, 0, 18 if i % 2 else -18], "easeInQuad"),
                  (length, [0, 0, 0], "easeOutBack"))
    for name, length in (("flare", 2.0), ("totality", 3.0), ("collapse", 2.5)):
        a = short(name, length)
        a.scale("core", (0, [1, 1, 1]), (length * 0.8, [2.0, 2.0, 2.0], "easeInQuad"), (length, [1, 1, 1], "easeOutQuad"))
        a.rot("core", (0, [0, 0, 0]), (length, [0, 720, 0], "easeInQuad"))
    a = short("step", 1.0)
    a.scale("form", (0, [1, 1, 1]), (0.4, [0.2, 1.4, 0.2], "easeInQuad"), (0.6, [0.2, 1.4, 0.2]), (1.0, [1, 1, 1], "easeOutBack"))
    a = short("unmake", 2.0)
    a.rot("form_arm_left", (0, [0, 0, 0]), (1.0, [0, 0, -150], "easeOutQuad"), (2.0, [0, 0, 0], "easeInQuad"))
    a.rot("form_arm_right", (0, [0, 0, 0]), (1.0, [0, 0, 150], "easeOutQuad"), (2.0, [0, 0, 0], "easeInQuad"))
    a.rot("form_head", (0, [0, 0, 0]), (1.0, [-25, 0, 0], "easeOutQuad"), (2.0, [0, 0, 0]))
    return f


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
    n = 128
    t = Tex(n, n, 6)
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            d = math.hypot(dx * 0.9, dy * 1.0)
            a = max(0.0, (d - 0.45) / 0.75)
            if a <= 0:
                continue
            a = min(1.0, a) ** 1.6
            wisp = fbm(x * 2, y * 2, 9, 64, 64, 3, 6.0)
            c = mix(hexc("#1a0b2e"), hexc("#000000"), min(1, a + 0.2))
            t.set(x, y, (c[0], c[1], c[2], int(255 * min(1, a * (0.8 + wisp * 0.5)))))
    save(t, "misc", "consumption_vignette")


def tentacle_texture():
    """Night with a crawling violet seam and stars, tiling along the tentacle."""
    t = Tex(16, 64, 11)
    for y in range(64):
        for x in range(16):
            n = fbm(x * 3, y * 3, 12, 48, 192, 3, 5.0)
            c = VOID[min(5, int(1 + n * 3))]
            if abs((x + y // 3) % 16 - 8) < 1:
                c = RIM
            if _h("tent", x, y) < 0.025:
                c = STAR[int(_h("tc", x, y) * 4) % 4]
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
            c = VOID[1 + int(fbm(x * 4, y * 4, 22, 64, 64, 2, 3.0) * 3)]
            if _h("orb", x, y) < 0.05:
                c = STAR[int(_h("os", x, y) * 4) % 4]
            orb.set(x, y, c)
    corona = Tex(16, 16, 23)
    for y in range(16):
        for x in range(16):
            corona.set(x, y, CORONA[(x + y) % 3] if (x + y) % 5 else hexc("#ffffff"))
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
    import json
    with open(ASSETS + "/models/block/eclipse_trophy.json", "w") as fh:
        json.dump(model, fh, indent=1)
        fh.write("\n")


def generate():
    eclipse_trophy()
    tentacle_texture()
    energy_ring()
    b = rig()
    t, g = b.build()
    b.rig.write(GEO + "amara.geo.json")
    save(t, "entity", "amara")
    save(g, "entity", "amara_glowmask")
    a = anims()
    a.write(ANIM + "amara.animation.json")
    with open(ASSETS + "/animations/entity/amara.names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")
    light_well_textures()
    void_mote()
    consumption_vignette()
