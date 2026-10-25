"""GeckoLib item models for tier III-IV weapons: rig, texture + glowmask, animations, GUI icon.

Item space: pixels, Y up. GeoItemRenderer translates the origin to the item cube's centre
(0.5, 0.51, 0.5), so models are re-centred on the origin. Built upright, blade or head at the top.
"""

import math

import palette as P
import preview3d
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import cloth, noise, paint_cube
from pixelkit import Ramp, Tex, _hash, fbm, hexc, mix

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"

SOUL = hexc("#7ff0ff")
SOUL_DIM = hexc("#3aa7c4")
BONE = Ramp("#5a5246", "#8a8070", "#b3a894", "#cfc6b0", "#e3dccb", "#f4efe2")
DARK_STEEL = Ramp("#0e1014", "#1c2027", "#2b313a", "#3c444f", "#4f5966", "#6a7480")


class Weapon:
    """Rig + paint + glow for one item, painted face by face like the entity models."""

    def __init__(self, name, size=64):
        self.name = name
        self.rig = Rig(name, size, size)
        self.parts = {}
        self.paints = []

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.rig.bone(name, pivot, parent, rotation)

    def cube(self, key, bone, origin, size, material, glow=None, inflate=0.0, faces=None, rotation=None, pivot=None, density=1):
        c = box(bone, origin, size, inflate=inflate, faces=faces, tag=key, rotation=rotation, pivot=pivot, density=density)
        self.parts[key] = c
        self.paints.append((c, material, glow))
        return c

    def build(self):
        # Centre the model on the item cube's centre so in-hand transforms can be derived from
        # vanilla's handheld ones. GeoItemRenderer already lifts the origin by half a block.
        lo, hi = self.rig.height_span()
        self.rig.translate(0, -(lo + hi) / 2, 0)
        self.rig.pack()
        t = Tex(self.rig.tex_w, self.rig.tex_h, 3)
        g = Tex(self.rig.tex_w, self.rig.tex_h, 4)
        for c, material, glow in self.paints:
            paint_cube(t, c, material)
            if glow:
                paint_cube(g, c, glow, shading=False)
                paint_cube(t, c, lambda f, x, y, w, h, gl=glow, m=material: gl(f, x, y, w, h) or m(f, x, y, w, h))
        return t, g


# --- Soul Scythe ------------------------------------------------------------------------------

def soul_scythe():
    w = Weapon("soul_scythe")
    root = w.bone("root", (0, 0, 0))
    wood = noise(P.WOOD, 5, 1.6, 0.8, 9.0)
    w.cube("shaft", root, (-0.5, 0, -0.5), (1, 28, 1), wood)
    w.cube("grip", root, (-1, 4, -1), (2, 5, 2), cloth(P.LEATHER, 6, 2.0, 0.4))
    for i, y in enumerate((11, 17, 23)):
        w.cube(f"ring{i}", root, (-1, y, -1), (2, 1, 2), noise(BONE, 7 + i, 3.0, 0.6))
    head = w.bone("head", (0, 27, 0), "root")
    w.cube("collar", head, (-1.5, 26, -1.5), (3, 3, 3), noise(BONE, 9, 2.8, 0.8))
    steel = noise(DARK_STEEL, 10, 3.0, 1.0, 6.0)

    def edge(face, x, y, wid, hei):
        # The lower (cutting) edge of each blade segment burns with soul-light.
        return SOUL if face in ("north", "south") and y == hei - 1 else (SOUL_DIM if face == "down" else None)
    w.cube("blade1", head, (-9, 27, -0.5), (9, 3, 1), steel, edge)
    w.cube("blade2", head, (-14, 25, -0.5), (5, 3, 1), steel, edge)
    w.cube("blade3", head, (-17, 22, -0.5), (3, 4, 1), steel, edge)
    w.cube("blade4", head, (-18, 18, -0.5), (2, 4, 1), steel, lambda f, x, y, wi, he: SOUL if y >= he - 1 else None)
    wisps = w.bone("wisps", (0, 21, 0), "root")
    for i, (x, z) in enumerate(((3, 0), (-2, 2), (-2, -3))):
        w.cube(f"wisp{i}", wisps, (x, 20 + i, z), (1, 1, 1), lambda f, *a: SOUL, lambda f, *a: SOUL)
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.soul_scythe.idle", 3.0, loop=True)
    idle.rot("wisps", (0, [0, 0, 0]), (3.0, [0, 360, 0]))
    idle.pos("wisps", (0, [0, 0, 0]), (1.5, [0, 1, 0], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    charge = f.new("animation.soul_scythe.charge", 1.0, loop=True)
    charge.rot("wisps", (0, [0, 0, 0]), (1.0, [0, 720, 0]))
    charge.scale("wisps", (0, [1.4, 1.4, 1.4]), (0.5, [1.8, 1.8, 1.8], "easeInOutSine"), (1.0, [1.4, 1.4, 1.4], "easeInOutSine"))
    charge.rot("head", (0, [0, 0, -2]), (0.25, [0, 0, 2]), (0.5, [0, 0, -2]), (0.75, [0, 0, 2]), (1.0, [0, 0, -2]))
    release = f.new("animation.soul_scythe.release", 0.5)
    release.rot("head", (0, [0, 0, 0]), (0.12, [0, 0, 25], "easeOutQuad"), (0.5, [0, 0, 0], "easeInOutSine"))
    release.scale("wisps", (0, [2, 2, 2]), (0.5, [1, 1, 1], "easeOutQuad"))
    return w, t, g, f


# --- Hellfire Greatsword ---------------------------------------------------------------------

def hellfire_greatsword():
    w = Weapon("hellfire_greatsword")
    root = w.bone("root", (0, 0, 0))
    w.cube("pommel", root, (-1, 0, -1), (2, 1, 2), noise(P.GOLD, 11, 2.0, 0.8))
    w.cube("grip", root, (-0.5, 1, -0.5), (1, 6, 1), cloth(P.LEATHER, 12, 1.8, 0.4))
    w.cube("guard", root, (-4, 7, -1), (8, 2, 2), noise(P.GOLD, 13, 2.4, 1.0))
    blackened = noise(P.ASH, 14, 2.4, 1.0, 7.0)
    cracks = {(1, 4), (1, 5), (2, 6), (1, 11), (0, 12), (1, 17), (2, 18)}

    def fire(face, x, y, wi, he):
        if face in ("north", "south") and (x, y) in cracks:
            return P.HELLFIRE[4] if (x + y) % 2 else P.HELLFIRE[5]
        return None
    w.cube("blade", root, (-1.5, 9, -0.5), (3, 20, 1), blackened, fire)
    w.cube("tip", root, (-1, 29, -0.5), (2, 2, 1), blackened, lambda f, x, y, wi, he: P.HELLFIRE[4] if y == 0 else None)
    w.cube("tip2", root, (-0.5, 31, -0.5), (1, 1, 1), blackened)
    embers = w.bone("embers", (0, 18, 0), "root")
    for i, (x, y) in enumerate(((2, 14), (-3, 20), (2, 25), (-2, 12))):
        w.cube(f"ember{i}", embers, (x, y, 0), (1, 1, 1), lambda f, *a: P.HELLFIRE[5], lambda f, *a: P.HELLFIRE[5])
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.hellfire_greatsword.idle", 2.0, loop=True)
    idle.pos("embers", (0, [0, 0, 0]), (2.0, [0, 4, 0]))
    idle.scale("embers", (0, [1, 1, 1]), (1.6, [1, 1, 1]), (2.0, [0.1, 0.1, 0.1]))
    charge = f.new("animation.hellfire_greatsword.charge", 0.6, loop=True)
    charge.pos("embers", (0, [0, 0, 0]), (0.6, [0, 6, 0]))
    charge.scale("embers", (0, [1.6, 1.6, 1.6]), (0.6, [0.4, 0.4, 0.4]))
    slash = f.new("animation.hellfire_greatsword.slash", 0.5)
    slash.scale("embers", (0, [3, 3, 3]), (0.5, [1, 1, 1], "easeOutQuad"))
    return w, t, g, f


# --- Censer of Grace -------------------------------------------------------------------------

def censer_of_grace():
    w = Weapon("censer_of_grace")
    root = w.bone("root", (0, 0, 0))
    steel = noise(P.STEEL, 21, 3.0, 0.8)
    gold = noise(P.GOLD, 22, 3.0, 1.0)
    w.cube("handle", root, (-2, 23, -0.5), (4, 1, 1), steel)
    chain = w.bone("chain", (0, 23, 0), "root")
    for i in range(5):
        w.cube(f"link{i}", chain, (-0.5 + (i % 2) * 0.2, 21 - i * 2, -0.5), (1, 2, 1), steel)
    censer = w.bone("censer", (0, 13, 0), "chain")
    holes = {(1, 1), (4, 1), (2, 2)}

    def glow(face, x, y, wi, he):
        return P.GRACE[5] if face in ("north", "south", "east", "west") and (x % 6, y) in holes else None
    w.cube("foot", censer, (-2, 0, -2), (4, 1, 4), gold)
    w.cube("stem", censer, (-1, 1, -1), (2, 1, 2), gold)
    w.cube("bowl_low", censer, (-2, 2, -2), (4, 1, 4), gold)
    w.cube("bowl", censer, (-3, 3, -3), (6, 3, 6), gold, glow)
    w.cube("rim", censer, (-3, 6, -3), (6, 1, 6), noise(P.GOLD, 23, 4.0, 0.6))
    lid = w.bone("lid", (0, 7, -3), "censer")
    w.cube("dome1", lid, (-2.5, 7, -2.5), (5, 1, 5), gold, lambda f, x, y, wi, he: P.GRACE[4] if f != "down" and (x + y) % 3 == 0 else None)
    w.cube("dome2", lid, (-1.5, 8, -1.5), (3, 2, 3), gold, lambda f, x, y, wi, he: P.GRACE[5] if f in ("north", "south") and x == 1 else None)
    w.cube("finial", lid, (-0.5, 10, -0.5), (1, 3, 1), noise(P.GOLD, 24, 4.0, 0.5))
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.censer_of_grace.idle", 2.4, loop=True)
    idle.rot("chain", (0, [0, 0, -10]), (1.2, [0, 0, 10], "easeInOutSine"), (2.4, [0, 0, -10], "easeInOutSine"))
    beam = f.new("animation.censer_of_grace.beam", 0.4, hold=True)
    beam.rot("lid", (0, [0, 0, 0]), (0.4, [-70, 0, 0], "easeOutBack"))
    beam.rot("chain", (0, [0, 0, 0]), (0.4, [-20, 0, 0], "easeOutQuad"))
    return w, t, g, f


# --- The First Blade --------------------------------------------------------------------------

def first_blade():
    """A jawbone of something enormous, teeth still in it, bound to a wrapped haft."""
    w = Weapon("first_blade")
    root = w.bone("root", (0, 0, 0))
    bone = noise(BONE, 31, 2.8, 1.2, 6.0)
    old_blood = noise(P.BLOOD, 32, 1.8, 1.0)
    w.cube("grip", root, (-1, 0, -1), (2, 7, 2), cloth(P.LEATHER, 33, 1.6, 0.4))
    w.cube("wrap", root, (-1.5, 6, -1.5), (3, 2, 3), old_blood)
    w.cube("jaw", root, (-1.5, 8, -0.5), (3, 14, 1), bone,
           lambda f, x, y, wi, he: P.BLOOD[4] if f in ("north", "south") and x == 0 and y % 4 == 1 else None)
    w.cube("jaw_tip", root, (-1, 22, -0.5), (2, 3, 1), bone)
    teeth = w.bone("teeth", (2, 15, 0), "root")
    for i, y in enumerate(range(10, 22, 3)):
        w.cube(f"tooth{i}", teeth, (1.5, y, -0.5), (1, 2, 1), noise(BONE, 40 + i, 4.0, 0.5))
    w.cube("ramus", root, (-3.5, 18, -0.5), (2, 5, 1), bone)
    pulse = w.bone("pulse", (0, 14, 0), "root")
    w.cube("vein", pulse, (-0.5, 9, -0.6), (1, 12, 0), lambda f, *a: P.BLOOD[5], lambda f, x, y, wi, he: P.BLOOD[5] if y % 2 else None,
           faces=("north", "south"))
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.first_blade.idle", 3.0, loop=True)
    idle.scale("pulse", (0, [1, 1, 1]), (0.2, [1.4, 1, 1]), (0.4, [1, 1, 1]), (3.0, [1, 1, 1]))
    hunger = f.new("animation.first_blade.hunger_pulse", 1.0)
    hunger.scale("pulse", (0, [1, 1, 1]), (0.15, [2.5, 1.1, 1]), (0.3, [1, 1, 1]), (0.45, [2.5, 1.1, 1]), (1.0, [1, 1, 1]))
    hunger.rot("teeth", (0, [0, 0, 0]), (0.15, [0, 0, -8]), (0.3, [0, 0, 0]), (0.45, [0, 0, -8]), (1.0, [0, 0, 0]))
    lunge = f.new("animation.first_blade.lunge", 0.4)
    lunge.scale("pulse", (0, [3, 1.2, 1]), (0.4, [1, 1, 1], "easeOutQuad"))
    return w, t, g, f


# --- The Whispering Codex ----------------------------------------------------------------------

def whispering_codex():
    """A book bound in pale leather with an eye set in its cover; its pages murmur."""
    w = Weapon("whispering_codex")
    root = w.bone("root", (0, 0, 0))
    hide = noise(Ramp("#3a2a26", "#5c4640", "#7d665c", "#9a8274", "#b39b8a", "#cdb6a3"), 51, 2.6, 1.0)
    pages = noise(P.PARCHMENT, 52, 3.4, 0.6)
    w.cube("spine", root, (-0.5, 0, -2), (1, 14, 4), hide)
    back = w.bone("back_cover", (0.5, 0, 2), "root")
    w.cube("back", back, (0.5, 0, 1.5), (9, 14, 1), hide)
    w.cube("pages", root, (0.5, 0.5, -1.5), (8, 13, 3), pages)
    front = w.bone("front_cover", (0.5, 0, -2), "root")
    eye_glow = {(4, 6), (5, 6), (4, 7), (5, 7)}
    w.cube("front", front, (0.5, 0, -2.5), (9, 14, 1), hide,
           lambda f, x, y, wi, he: (P.BLOOD[5] if (x, y) in eye_glow else (hexc("#f0e6c8") if f == "north" and 3 <= x <= 6 and 5 <= y <= 8 else None)) if f == "north" else None)
    w.cube("clasp", front, (8.5, 5, -3), (2, 3, 1), noise(P.STEEL, 53, 3.0, 0.6))
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.whispering_codex.idle", 4.0, loop=True)
    idle.rot("front_cover", (0, [0, 0, 0]), (2.0, [0, -6, 0], "easeInOutSine"), (2.2, [0, -2, 0]), (2.4, [0, -7, 0]), (4.0, [0, 0, 0], "easeInOutSine"))
    cast = f.new("animation.whispering_codex.cast", 1.2)
    cast.rot("front_cover", (0, [0, 0, 0]), (0.3, [0, -110, 0], "easeOutBack"), (0.9, [0, -110, 0]), (1.2, [0, 0, 0], "easeInQuad"))
    cast.rot("back_cover", (0, [0, 0, 0]), (0.3, [0, 20, 0], "easeOutBack"), (0.9, [0, 20, 0]), (1.2, [0, 0, 0], "easeInQuad"))
    return w, t, g, f


# --- Penumbra ------------------------------------------------------------------------------

def penumbra():
    """A blade of night with starlight along its edge; its guard is a small eclipse."""
    w = Weapon("penumbra")
    root = w.bone("root", (0, 0, 0))
    void = Ramp("#020105", "#06040c", "#0d0818", "#170e29", "#24153d", "#3a2160")
    night = noise(void, 61, 2.0, 1.2, 6.0)
    star = hexc("#e8deff")
    violet = hexc("#b48cff")

    def edge(face, x, y, wi, he):
        if face in ("north", "south") and (x == 0 or x == wi - 1):
            return violet if (y * 7 + x) % 5 else star
        if face in ("north", "south") and (x * 13 + y * 7) % 23 == 0:
            return star
        return None
    w.cube("pommel", root, (-1, 0, -1), (2, 2, 2), noise(P.STEEL, 62, 2.0, 0.8))
    w.cube("grip", root, (-0.5, 2, -0.5), (1, 6, 1), cloth(P.LEATHER, 63, 1.2, 0.3))
    guard = w.bone("guard", (0, 9, 0), "root")
    w.cube("disc", guard, (-2.5, 6.5, -0.5), (5, 5, 1), lambda f, *a: hexc("#000000"),
           lambda f, x, y, wi, he: hexc("#ffe9b8") if f in ("north", "south") and (x in (0, wi - 1) or y in (0, he - 1)) else None)
    corona = w.bone("corona", (0, 9, 0), "guard")
    for i, (o, sz) in enumerate([((-4, 8.5, -0.5), (1, 1, 1)), ((3, 8.5, -0.5), (1, 1, 1)), ((-0.5, 12, -0.5), (1, 1, 1)),
                                 ((-0.5, 5, -0.5), (1, 1, 1))]):
        w.cube(f"ray{i}", corona, o, sz, lambda f, *a: hexc("#fff8ec"), lambda f, *a: hexc("#fff8ec"))
    w.cube("blade", root, (-1.5, 11.5, -0.5), (3, 17, 1), night, edge)
    w.cube("tip", root, (-0.5, 28.5, -0.5), (1, 2, 1), night, lambda f, *a: violet)
    t, g = w.build()

    f = AnimFile()
    idle = f.new("animation.penumbra.idle", 3.0, loop=True)
    idle.rot("corona", (0, [0, 0, 0]), (3.0, [0, 0, 90]))
    idle.scale("corona", (0, [1, 1, 1]), (1.5, [1.3, 1.3, 1.3], "easeInOutSine"), (3.0, [1, 1, 1], "easeInOutSine"))
    charge = f.new("animation.penumbra.charge", 1.0, loop=True)
    charge.rot("corona", (0, [0, 0, 0]), (1.0, [0, 0, 360]))
    charge.scale("corona", (0, [1.6, 1.6, 1.6]), (0.5, [2.2, 2.2, 2.2], "easeInOutSine"), (1.0, [1.6, 1.6, 1.6], "easeInOutSine"))
    release = f.new("animation.penumbra.release", 0.5)
    release.scale("corona", (0, [2.6, 2.6, 2.6]), (0.5, [1, 1, 1], "easeOutQuad"))
    return w, t, g, f


# --- Angelic blades ---------------------------------------------------------------------------
# The Angel Blade, the Archangel Blade (Lucifer's) and Gabriel's Blade are one family. The blade is
# a long three-sided pyramid: three tapering planes, each in a bone turned 0/120/240 degrees about the
# axis and tilted in (X first, then Y, like GeckoLib) so their edges meet in a point. The `light`
# bone repeats the pyramid a little larger and inflated, painted entirely in the glowmask; the clips
# scale it (rest 1, idle 0). Faces are painted 4 texels per pixel so the taper and the engravings
# stay fine. Built upright, grip at the bottom: the reverse grip is an item transform in Java.

BLADE_DENSITY = 4
PALE_SILVER = Ramp("#6c7380", "#98a0ac", "#c1c8d1", "#dbe0e7", "#edf0f4", "#ffffff")
SUN_GOLD = Ramp("#5a3a0c", "#8a5c14", "#b8851f", "#ddaf38", "#f3d36a", "#fff6d6")
DARK_WRAP = Ramp("#120d0b", "#1d1613", "#2a201b", "#382b24", "#47372e", "#58463b")

# Enochian-looking marks, 3 x 4 texels (top row first).
GLYPHS = (
    ("#.#", ".#.", ".#.", "##."), ("###", "..#", ".#.", "#.."), ("#..", "##.", "#.#", "#.."),
    (".#.", "#.#", ".#.", ".##"), ("#.#", "#.#", ".#.", "#.#"), ("##.", "#..", "###", "..#"),
    ("..#", ".##", "#.#", "..#"), (".##", "#..", ".#.", "#.."),
)


class BladeLook:
    def __init__(self, name, metal, engrave, light, seed, step=6, top=0.55, border=None):
        self.name = name
        self.metal = metal          # Ramp of the faces: dark flute -> bright ridge
        self.engrave = engrave      # engraving colour (fullbright, so keep it close to the metal)
        self.light = light          # rgb of the light sheath
        self.seed = seed
        self.step = step            # texels from one glyph to the next
        self.top = top              # engravings stop at this fraction of the blade
        self.border = border        # optional engraved line just inside the edges


def _half_width(s, t):
    # The point keeps a two-texel spine so the tip never disappears.
    return s / 2 * (1 - t) + 0.13


def _blade_face(look, s, width_px):
    def m(face, x, y, wi, he):
        px = (x + 0.5) / BLADE_DENSITY - width_px / 2
        t = 1 - (y + 0.5) / he
        hw = _half_width(s, t)
        if abs(px) > hw:
            return None
        d = hw - abs(px)
        if d < 0.26:
            return look.metal[5]
        if d < 0.51:
            return look.metal[4 if t < 0.85 else 5]
        n = fbm(x, y * 3, look.seed, 64, 64, 2, 4.0)
        return look.metal[int(round(1.6 + 1.2 * abs(px) / hw + (n - 0.5) * 1.0))]
    return m


def _blade_glow(look, s, width_px):
    def g(face, x, y, wi, he):
        px = (x + 0.5) / BLADE_DENSITY - width_px / 2
        t = 1 - (y + 0.5) / he
        hw = _half_width(s, t)
        if abs(px) > hw - 0.2 or t > look.top:
            return None
        d = hw - abs(px)
        if look.border and 0.5 <= d < 0.76 and t < look.top + 0.15:
            return look.border
        yy = he - 1 - y - 3
        if yy < 0:
            return None
        slot, ry = divmod(yy, look.step)
        gx = x - (wi // 2 - 2)
        if ry >= 4 or not 0 <= gx < 3:
            return None
        glyph = GLYPHS[int(_hash(slot, wi, look.seed) * len(GLYPHS))]
        return look.engrave if glyph[3 - ry][gx] == "#" else None
    return g


def _light_face(look, s, width_px):
    r, gr, b = look.light

    def m(face, x, y, wi, he):
        px = (x + 0.5) / 2 - width_px / 2
        t = 1 - (y + 0.5) / he
        hw = _half_width(s, t)
        if abs(px) > hw:
            return None
        edge = 1 - min(1.0, (hw - abs(px)) / max(hw, 0.3))
        return (r, gr, b, int(min(235, 120 + 100 * edge + 60 * t)))
    return m


def pyramid(w, parent, prefix, yb, h, s, material, glow=None, inflate=0.0, density=BLADE_DENSITY):
    """Three tilted planes about the Y axis whose triangular masks close into a point at yb + h."""
    width = int(math.ceil(s + 0.4))
    r = s / (2 * math.sqrt(3))                       # inradius of the base triangle
    tilt = -math.degrees(math.asin(r / h))           # Bedrock X: leans each plane's top onto the axis
    for i, a in enumerate((0, 120, 240)):
        b = w.bone(f"{prefix}{i}", (0, yb, 0), parent, rotation=(tilt, a, 0))
        w.cube(f"{prefix}{i}", b, (-width / 2, yb, -r), (width, h, 0), material(width), glow(width) if glow else None,
               inflate, faces=("north",), density=density)


def _wrap(ramp, seed, gap, period=5, slope=1):
    """Strips wound diagonally round a square grip, with a seam (leather edge or wire) between turns."""
    turn = {"east": 0, "north": 1, "west": 2, "south": 3}

    def m(face, x, y, wi, he):
        if face not in turn:
            return ramp[1]
        k = y + slope * (turn[face] * wi + x)
        if k % period == 0:
            return gap
        n = fbm(x * 2, y * 2, seed, 32, 32, 2, 5.0)
        return ramp[int(round(2.4 + (k // period % 2) * 0.6 + (n - 0.5) * 1.0 + (0.6 if k % period == 1 else 0)))]
    return m


def angelic_blade(look, grip_h, blade_h, s, wrap, guard, pommel, grip_inflate=-0.2):
    w = Weapon(look.name, 128)
    root = w.bone("root", (0, 0, 0))
    grip = w.bone("grip", (0, 0, 0), "root")
    y = pommel(w, grip)
    w.cube("wrap", grip, (-1, y, -1), (2, grip_h, 2), wrap, inflate=grip_inflate, density=2)
    y += grip_h
    y = guard(w, root, y)
    blade = w.bone("blade", (0, y, 0), "root")
    pyramid(w, "blade", "face", y, blade_h, s, lambda wd: _blade_face(look, s, wd), lambda wd: _blade_glow(look, s, wd))
    light = w.bone("light", (0, y, 0), "root")
    ls = s + 0.9
    pyramid(w, "light", "glow", y - 0.5, blade_h + 1, ls, lambda wd: _light_face(look, ls, wd),
            lambda wd: _light_face(look, ls, wd), inflate=0.12, density=2)
    return w


def collar(metal, seed, half=1.5, tall=1, inflate=-0.25):
    """A small octagonal ferrule between grip and blade: a square cube and its 45-degree twin."""
    def g(w, root, y):
        mat = noise(metal, seed, 3.0, 0.8)
        w.cube("collar", root, (-half, y, -half), (2 * half, tall, 2 * half), mat, inflate=inflate, density=2)
        b = w.bone("collar45", (0, y, 0), "root", rotation=(0, 45, 0))
        w.cube("collar45", b, (-half, y, -half), (2 * half, tall, 2 * half), noise(metal, seed + 1, 2.6, 0.8), inflate=inflate - 0.12,
               density=2)
        return y + tall
    return g


def cap(metal, seed, inflate=0.05):
    def p(w, grip):
        w.cube("pommel", grip, (-1, 0, -1), (2, 1, 2), noise(metal, seed, 3.2, 0.7), inflate=inflate, density=2)
        return 1
    return p


def angel_blade():
    """The angel blade of the show: a three-sided silver spike, faint Enochian down each face, a small collar and a grip
    wound in old leather strips."""
    look = BladeLook("angel_blade", P.SILVER, hexc("#9fbde3"), (206, 232, 255), 61, step=7, top=0.5)
    w = angelic_blade(look, 4, 10, 1.9, _wrap(P.LEATHER, 62, P.LEATHER[0]), collar(P.SILVER, 63), cap(P.SILVER, 64))
    return w, *w.build(), blade_anims(w.name, look)


def archangel_blade():
    """Lucifer's archangel blade: the same family, longer and paler, engraved densely with a line inside every edge, a
    swept guard and a faceted finial; the grip is dark leather bound with silver wire."""
    look = BladeLook("archangel_blade", PALE_SILVER, hexc("#c4dcfa"), (220, 240, 255), 71, step=5, top=0.6,
                     border=hexc("#a9c8ee"))

    def guard(w, root, y):
        y = collar(PALE_SILVER, 73, 1.5)(w, root, y)
        bar = noise(PALE_SILVER, 74, 3.4, 0.7)
        w.cube("guard", root, (-2, y - 1, -0.5), (4, 1, 1), bar, inflate=-0.15, density=2)
        for side, x in (("l", 1.6), ("r", -2.6)):
            w.cube("quillon_" + side, root, (x, y - 0.3, -0.5), (1, 1, 1), bar, inflate=-0.25, density=2)
        return y

    def finial(w, grip):
        w.cube("pommel", grip, (-1, 1, -1), (2, 1, 2), noise(PALE_SILVER, 75, 3.0, 0.7), inflate=0.2, density=2)
        gem = w.bone("finial", (0, 0.9, 0), "grip", rotation=(45, 45, 0))
        w.cube("finial", gem, (-0.5, 0.4, -0.5), (1, 1, 1), noise(PALE_SILVER, 76, 4.0, 0.6), inflate=0.15, density=2)
        return 2
    wrap = _wrap(DARK_WRAP, 72, PALE_SILVER[2], period=6)
    w = angelic_blade(look, 5, 13, 2.1, wrap, guard, finial)
    return w, *w.build(), blade_anims(w.name, look)


def gabriel_blade():
    """Gabriel's archangel blade: the family in sun-gold, a dark grip bound with gold wire and, because it is his, a
    candy-red tassel swinging from the pommel."""
    look = BladeLook("gabriel_blade", SUN_GOLD, hexc("#ffe7a0"), (255, 236, 190), 81, step=6, top=0.55)
    candy = Ramp("#4a0a10", "#7e1420", "#b01e2c", "#e2283a", "#ff5a66", "#fff8ee")

    def pommel(w, grip):
        w.cube("pommel", grip, (-1, 3, -1), (2, 1, 2), noise(SUN_GOLD, 84, 3.4, 0.7), inflate=0.05, density=2)
        tassel = w.bone("tassel", (0, 3, 0), "grip", rotation=(0, 0, 12))
        w.cube("cord", tassel, (-0.5, 2, -0.5), (1, 1, 1), lambda *a: candy[2], inflate=-0.3, density=2)
        w.cube("bead", tassel, (-0.5, 1.6, -0.5), (1, 1, 1), noise(SUN_GOLD, 85, 4.0, 0.5), inflate=-0.1, density=2)

        def fringe(face, x, y, wi, he):
            if y >= he - 1 and x % 2:
                return None
            return candy[2 + (x % 3 == 1) + (y < 2)]
        for i, a in enumerate((0, 90)):
            b = w.bone(f"fringe{i}", (0, 0, 0), "tassel", rotation=(0, a, 0))
            w.cube(f"fringe{i}", b, (-1, -0.6, 0), (2, 2, 0), fringe, faces=("north",), density=4)
        return 4
    wrap = _wrap(DARK_WRAP, 82, SUN_GOLD[2], period=6)
    w = angelic_blade(look, 4, 12, 2.0, wrap, collar(SUN_GOLD, 83), pommel)
    return w, *w.build(), blade_anims(w.name, look)


def blade_anims(name, look):
    f = AnimFile()
    idle = f.new(f"animation.{name}.idle", 3.0, loop=True)
    idle.scale("light", (0, [0, 0, 0]), (3.0, [0, 0, 0]))
    if name == "gabriel_blade":
        idle.rot("tassel", (0, [0, 0, 0]), (1.5, [0, 0, -14], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    # The light swells over the first second, then breathes; the loop is long so the restart (a fresh
    # flare from nothing) comes rarely, once every 12 s of a held charge.
    charge = f.new(f"animation.{name}.charge", 12.0, loop=True)
    keys = [(0, [0, 0, 0]), (0.5, [0.8, 0.8, 0.8], "easeOutQuad"), (1.0, [1, 1, 1], "easeOutQuad")]
    t = 1.0
    while t < 11.99:
        keys.append((t + 0.5, [1.08, 1.08, 1.08], "easeInOutSine"))
        keys.append((t + 1.0, [1, 1, 1], "easeInOutSine"))
        t += 1.0
    charge.scale("light", *keys)
    flash = f.new(f"animation.{name}.flash", 0.4)
    flash.scale("light", (0, [0, 0, 0]), (0.1, [1.2, 1.2, 1.2], "easeOutQuad"), (0.4, [0, 0, 0], "easeInQuad"))
    return f


ICON_ANGLE = {"censer_of_grace": 0, "whispering_codex": 0}
ICON_HIDDEN = {"angel_blade": ("light",), "archangel_blade": ("light",), "gabriel_blade": ("light",)}
# The angelic blades are slim spikes: the 16-px icon widens blade and grip so they survive the downscale.
_SLIM = {"blade": {"scale": [1.6, 1, 1.6]}, "grip": {"scale": [1.3, 1, 1.3]}}
ICON_POSE = {"angel_blade": _SLIM, "archangel_blade": _SLIM, "gabriel_blade": _SLIM}


MODELS = [soul_scythe, hellfire_greatsword, censer_of_grace, first_blade, whispering_codex, penumbra, angel_blade,
          archangel_blade, gabriel_blade]


def generate():
    for fn in MODELS:
        w, t, g, anims = fn()
        w.rig.write(GEO + w.name + ".geo.json")
        anims.write(ANIM + w.name + ".animation.json")
        save(t, "item", w.name)
        save(g, "item", w.name + "_glowmask")
        save(preview3d.icon(w.rig, t, angle_deg=ICON_ANGLE.get(w.name, -45),
                            hidden=ICON_HIDDEN.get(w.name, ()),
                            pose=ICON_POSE.get(w.name)), "item", w.name + "_icon")
        lo, hi = w.rig.height_span()
        print(f"{w.name}: height {hi - lo:.1f}")
