"""GeckoLib item models for tier III-IV weapons: rig, texture + glowmask, animations, GUI icon.

Item space: pixels, Y up. GeoItemRenderer translates the origin to the item cube's centre
(0.5, 0.51, 0.5), so models are re-centred on the origin. Built upright, blade or head at the top.
"""

import palette as P
import preview3d
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import cloth, noise, paint_cube
from pixelkit import Ramp, Tex, hexc, mix

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

    def cube(self, key, bone, origin, size, material, glow=None, inflate=0.0, faces=None):
        c = box(bone, origin, size, inflate=inflate, faces=faces, tag=key)
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


ICON_ANGLE = {"censer_of_grace": 0, "whispering_codex": 0}


MODELS = [soul_scythe, hellfire_greatsword, censer_of_grace, first_blade, whispering_codex, penumbra]


def generate():
    for fn in MODELS:
        w, t, g, anims = fn()
        w.rig.write(GEO + w.name + ".geo.json")
        anims.write(ANIM + w.name + ".animation.json")
        save(t, "item", w.name)
        save(g, "item", w.name + "_glowmask")
        save(preview3d.icon(w.rig, t, angle_deg=ICON_ANGLE.get(w.name, -45)), "item", w.name + "_icon")
        lo, hi = w.rig.height_span()
        print(f"{w.name}: height {hi - lo:.1f}")
