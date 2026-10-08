"""Raphael's smaller art (v0.16): the Stormcaller (a GeckoLib catalyst, upright and centred like weapon_models, with its
animations and a hand-drawn 16-px inventory icon), the trophy (a block model on the trophies' pedestal: his bust in the
black overcoat, two storm-cloud wings rising behind it) and the unlit holy oil slick (a floor decal).

The Stormcaller: a tall staff of black ironwood bound in silver -- a silver ferrule, a grip of dark leather between silver
collars, a silver wire wound up the upper shaft -- crowned with a cage of four curving silver prongs holding a storm crystal
(pale blue-white, lightning inside it; full bright in the glowmask) with arcs of lightning leaping between the prongs.
Bones: `staff` -- `crystal` (spins), `arcs` (flicker). Clips: idle (loop), zap, heal.
"""

import math
import random

import blockmodel as BM
import preview3d
from animkit import AnimFile
from chuck_art import Model, solid
from common import ASSETS, art, save
from michael_art import tone
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade
from raphael_art import BOLT, CLOUD, COAT, LINING, SHIRT, SKIN, StormFeather, TIE, bolt_texels

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"
WOOD = Ramp("#0b0807", "#140f0c", "#1e1713", "#29201a", "#352a22", "#45372c")
SILVER = Ramp("#3e4652", "#626c7a", "#8b95a3", "#b4bdc8", "#d8dee6", "#f6f9fc")
LEATHER = Ramp("#120d0c", "#1c1513", "#271d1a", "#332622", "#40302a", "#4f3b33")
WHITE = hexc("#ffffff")


# =====================================================================================================
# The Stormcaller
# =====================================================================================================

def wood_m(f, x, y, w, h):
    """Black ironwood: a fine vertical grain, a silver wire wound round it (painted, a turn every 6 texels)."""
    off = {"north": 0, "east": 2, "south": 4, "west": 6}.get(f, 0)
    if f in ("up", "down"):
        return WOOD[2]
    if (y + x + off) % 8 == 0:
        return SILVER[4]
    if (y + x + off) % 8 == 1:
        return SILVER[1]
    v = 2.6 + 0.9 * math.sin(x * 2.3 + math.sin(y * 0.35) * 1.4) + (0.6 if x == 0 else 0)
    return tone(WOOD, v)


def plain_wood(f, x, y, w, h):
    if f in ("up", "down"):
        return WOOD[2]
    return tone(WOOD, 2.6 + 0.9 * math.sin(x * 2.3 + math.sin(y * 0.35) * 1.4) + (0.6 if x == 0 else 0))


def silver_m(f, x, y, w, h):
    if f == "up":
        return SILVER[5]
    if f == "down":
        return SILVER[1]
    t = y / max(1, h - 1)
    v = 4.4 - 2.0 * t + (0.5 if x == 0 else 0) - (0.4 if f in ("east", "west") else 0)
    return tone(SILVER, v)


def leather_m(f, x, y, w, h):
    if f in ("up", "down"):
        return LEATHER[2]
    return LEATHER[3 if (y + (x // 2)) % 3 == 0 else 2] if (y % 3) else LEATHER[1]


def crystal_m(f, x, y, w, h):
    """The storm crystal: pale blue glass, darker at its heart, a jagged bolt inside each face."""
    bolts = bolt_texels(17000 + len(f) * 3, w, h, 1, 0.15, wander=0.6)
    if (x, y) in bolts:
        return WHITE
    t = y / max(1, h - 1)
    d = abs(x - (w - 1) / 2) / max(1, w / 2)
    v = 2.2 + 1.8 * d + 0.6 * math.sin(t * 6 + x)
    return mix(tone(BOLT, v), CLOUD[3], 0.25 * (1 - d))


def crystal_glow(f, x, y, w, h, c):
    return (c[0], c[1], c[2], 255) if c == WHITE else shade(c, 0.75)


def arc_m(seed):
    def m(f, x, y, w, h):
        pts = bolt_texels(seed + len(f), w, h, 1, 0.12, wander=0.7)
        if (x, y) in pts:
            return WHITE if h01s(seed, x, y) else BOLT[4]
        return None
    return m


def h01s(seed, x, y):
    return random.Random(seed * 1000 + x * 31 + y).random() < 0.5


def staff_rig():
    m = Model("raphaels_stormcaller", 256, 256)
    root = m.bone("staff", (0, 0, 0))
    k = 1.0
    # The ferrule, the shaft below the grip, the grip, the shaft above it (wound with silver wire).
    m.cube(root, (-0.9, -17.0, -0.9), (1.8, 1.4, 1.8), silver_m, None, density=4, tag="ferrule")
    m.cube(root, (-0.5, -17.8, -0.5), (1.0, 0.8, 1.0), silver_m, None, density=4, tag="ferrule_tip")
    m.cube(root, (-0.7, -15.6, -0.7), (1.4, 12.0, 1.4), plain_wood, None, density=4, tag="shaft")
    m.cube(root, (-0.85, -3.6, -0.85), (1.7, 6.0, 1.7), leather_m, None, density=4, tag="grip")
    m.cube(root, (-0.7, 2.4, -0.7), (1.4, 8.6, 1.4), wood_m, None, density=4, tag="shaft")
    for y in (-10.0, -4.1, 2.3, 10.6):
        m.cube(root, (-1.0, y, -1.0), (2.0, 0.6, 2.0), silver_m, None, density=4, tag="collar")
    # The head: a flared silver socket, four prongs curving out and back in round the crystal, a finial above.
    m.cube(root, (-1.2, 11.2, -1.2), (2.4, 1.2, 2.4), silver_m, None, density=4, tag="socket", rotation=(0, 45, 0), pivot=(0, 11.2, 0))
    m.cube(root, (-1.6, 12.4, -1.6), (3.2, 0.7, 3.2), silver_m, None, density=4, tag="cup")
    for i, (ax, s) in enumerate((("x", 1), ("x", -1), ("z", 1), ("z", -1))):
        # Lower segment leaning out, upper segment leaning back in.
        if ax == "x":
            lo = dict(origin=(-0.3, 12.8, -0.3 + 1.2 * s), rot=(-28 * s, 0, 0), piv=(0, 12.8, 1.2 * s))
            hi_base = (0, 12.8 + 4.6 * math.cos(math.radians(28)), 1.2 * s + 4.6 * math.sin(math.radians(28)) * s)
            hi = dict(origin=(-0.3, hi_base[1], hi_base[2] - 0.3), rot=(30 * s, 0, 0), piv=hi_base)
        else:
            lo = dict(origin=(-0.3 + 1.2 * s, 12.8, -0.3), rot=(0, 0, 28 * s), piv=(1.2 * s, 12.8, 0))
            hi_base = (1.2 * s + 4.6 * math.sin(math.radians(28)) * s, 12.8 + 4.6 * math.cos(math.radians(28)), 0)
            hi = dict(origin=(hi_base[0] - 0.3, hi_base[1], -0.3), rot=(0, 0, -30 * s), piv=hi_base)
        m.cube(root, lo["origin"], (0.6, 4.6, 0.6), silver_m, lambda f, x, y, w, h, c: shade(c, 0.25), density=4, tag="prong",
               rotation=lo["rot"], pivot=lo["piv"])
        m.cube(root, hi["origin"], (0.6, 4.4, 0.6), silver_m, lambda f, x, y, w, h, c: shade(c, 0.25), density=4, tag="prong",
               rotation=hi["rot"], pivot=hi["piv"])
    m.cube(root, (-0.9, 20.6, -0.9), (1.8, 0.8, 1.8), silver_m, None, density=4, tag="crown_ring", rotation=(0, 45, 0), pivot=(0, 20.6, 0))
    m.cube(root, (-0.35, 21.4, -0.35), (0.7, 2.4, 0.7), silver_m, None, density=4, tag="finial")
    # The crystal (a long bipyramid: a turned prism, smaller prisms tapering top and bottom), free to spin.
    cr = m.bone("crystal", (0, 16.8, 0), "staff")
    m.cube(cr, (-1.2, 14.6, -1.2), (2.4, 4.4, 2.4), crystal_m, crystal_glow, density=4, tag="crystal", rotation=(0, 45, 0), pivot=(0, 16.8, 0))
    m.cube(cr, (-0.8, 19.0, -0.8), (1.6, 1.2, 1.6), crystal_m, crystal_glow, density=4, tag="crystal_tip", rotation=(0, 45, 0), pivot=(0, 16.8, 0))
    m.cube(cr, (-0.8, 13.4, -0.8), (1.6, 1.2, 1.6), crystal_m, crystal_glow, density=4, tag="crystal_tip", rotation=(0, 45, 0), pivot=(0, 16.8, 0))
    # Arcs of lightning leaping round the cage (flat planes, crossed).
    arcs = m.bone("arcs", (0, 16.8, 0), "staff")
    for i, rot in enumerate((0, 45, 90, 135)):
        m.cube(arcs, (-3.2, 13.6, 0), (6.4, 6.4, 0), arc_m(17100 + i * 10), lambda f, x, y, w, h, c: (c[0], c[1], c[2], 255),
               faces=("north", "south"), density=4, tag="arc", rotation=(0, rot, 0), pivot=(0, 16.8, 0))
    # Centred on the origin (GeoItemRenderer lifts it to the block's centre), rotation pivots too.
    lo, hi = m.rig.height_span()
    dy = -(lo + hi) / 2
    m.rig.translate(0, dy, 0)
    for _, c in m.rig.cubes():
        if c.pivot:
            c.pivot = (c.pivot[0], c.pivot[1] + dy, c.pivot[2])
    m.height = hi - lo
    return m


def staff_anims():
    f = AnimFile()
    a = f.new("animation.raphaels_stormcaller.idle", 4.0, loop=True)
    a.rot("crystal", (0, [0, 0, 0]), (4.0, [0, 360, 0]))
    a.pos("crystal", (0, [0, 0, 0]), (2.0, [0, 0.3, 0], "easeInOutSine"), (4.0, [0, 0, 0], "easeInOutSine"))
    a.rot("arcs", (0, [0, 0, 0]), (0.5, [0, 70, 0]), (0.5001, [0, 200, 0]), (1.5, [0, 260, 0]), (1.5001, [0, 20, 0]),
          (2.6, [0, 110, 0]), (2.6001, [0, 300, 0]), (4.0, [0, 360, 0]))
    a.scale("arcs", (0, [1, 1, 1]), (0.5, [0.6, 1, 0.6]), (0.5001, [1.1, 1.1, 1.1]), (1.5, [0.7, 0.9, 0.7]), (1.5001, [1, 1, 1]),
            (2.6, [0.5, 1, 0.5]), (2.6001, [1.05, 1.1, 1.05]), (4.0, [1, 1, 1]))
    z = f.new("animation.raphaels_stormcaller.zap", 0.6)
    z.scale("crystal", (0, [1, 1, 1]), (0.08, [1.5, 1.5, 1.5]), (0.6, [1, 1, 1], "easeOutQuad"))
    z.scale("arcs", (0, [1, 1, 1]), (0.06, [1.8, 1.4, 1.8]), (0.2, [0.4, 1, 0.4]), (0.6, [1, 1, 1], "easeOutQuad"))
    z.rot("staff", (0, [0, 0, 0]), (0.08, [-10, 0, 0]), (0.6, [0, 0, 0], "easeOutQuad"))
    hl = f.new("animation.raphaels_stormcaller.heal", 1.2)
    hl.scale("crystal", (0, [1, 1, 1]), (0.4, [1.3, 1.3, 1.3], "easeInOutSine"), (0.8, [1.3, 1.3, 1.3]), (1.2, [1, 1, 1], "easeInOutSine"))
    hl.rot("crystal", (0, [0, 0, 0]), (1.2, [0, 540, 0], "easeInOutSine"))
    hl.scale("arcs", (0, [1, 1, 1]), (0.3, [0.1, 0.1, 0.1]), (1.0, [0.1, 0.1, 0.1]), (1.2, [1, 1, 1]))
    return f


ICON = [
    "...........KKK..",
    "..........KcCcK.",
    ".........KlCWCaK",
    "........K.KcWCK.",
    "........KsKCcK..",
    ".......KsK.KK...",
    "......KwsK......",
    ".....KwK........",
    "....KwK.........",
    "...KgK..........",
    "...KgK..........",
    "..KgK...........",
    "..KsK...........",
    ".KwK............",
    "KsK.............",
    ".K..............",
]


def staff_icon():
    pal = {"K": hexc("#120e10"), "c": BOLT[2], "C": BOLT[4], "W": WHITE, "l": BOLT[3], "a": BOLT[3], "s": SILVER[4],
           "w": WOOD[4], "g": LEATHER[4]}
    return art(ICON, pal)


# =====================================================================================================
# The trophy: his bust in the overcoat, storm wings behind
# =====================================================================================================

def _tex(fn, seed=0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


BUST_FACE = [   # x = 0 is the viewer's left
    "ssssssssssssssss",
    "ssssssfffsssssss",
    "sssssfffffssssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbBBBssssssBBBbs",
    "ssssBBsKKsBBssss",
    "sswEEwssssw EEwss".replace(" ", ""),
    "ssuuuussssuuuuss",
    "sssssssnnsssssss",
    "sssssssnnsssssss",
    "sssssNNnnNNsssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "jsssssMMMMsssssj",
    "jjssssssssssssjj",
]


def trophy():
    def skin(x, y):
        return tone(SKIN, 3.0 + 0.4 * math.sin(x * 0.9 + y * 0.6) - 0.6 * y / 15)
    pal = {"s": None, "f": SKIN[5], "b": SKIN[1], "B": SKIN[0], "K": SKIN[2], "w": hexc("#cfc8c0"), "E": hexc("#5c8fd0"),
           "u": SKIN[1], "n": SKIN[4], "N": SKIN[0], "m": hexc("#2c1712"), "M": hexc("#4b2a21"), "j": SKIN[2]}

    def face(x, y):
        c = pal.get(BUST_FACE[y][x])
        return c if c is not None else skin(x, y)
    face_t = _tex(face)
    skin_t = _tex(lambda x, y: mix(skin(x, y), SKIN[5], 0.3) if y < 3 else skin(x, y))
    coat_t = _tex(lambda x, y: tone(COAT, 3.2 + 0.4 * math.sin(x * 1.1) - 1.0 * y / 15 + (0.2 if (x + y) % 3 == 0 else 0)))

    def chest(x, y):
        # The open overcoat, the jacket's V, the white shirt and the dark tie (x = 0 is the viewer's left).
        d = abs(x - 7.5)
        if d < 0.6 and y >= 1:
            return TIE[4] if y % 4 == 1 else TIE[2]
        if d < 4.0 - y * 0.22 and y < 13:
            return SHIRT[4] if d > 0.9 else SHIRT[5]
        if d < 4.6 - y * 0.1:
            return tone(Ramp("#11141a", "#1a1e26", "#242a34", "#2f3642", "#3c4452", "#4e5767"), 3.0 - y / 10)
        if d < 5.6 - y * 0.08:
            return COAT[4]                        # the coat's lapel
        return tone(COAT, 3.2 - y / 12)
    chest_t = _tex(chest)

    def wing(x, y):
        # A storm-cloud wing seen from behind: root at the right edge (x=15), billows along the top falling to a long
        # tip at the bottom left; slate cloud, a vein of lightning across it.
        top = (x - 4) * 0.5 if x >= 4 else (4 - x) * 1.1
        if x >= 13:
            top = 4.5 + (x - 13) * 1.6
        top += 0.8 * math.sin(x * 1.7)                       # the lumpy billows
        bottom = 15 - (x / 15) * 5 - (1 if x % 2 else 0)
        if y < top or y > bottom:
            return None
        bolt = {(1, 13), (2, 12), (3, 12), (4, 11), (5, 10), (6, 10), (7, 9), (8, 8), (9, 8), (10, 7), (11, 6), (12, 6), (6, 11),
                (7, 12)}
        if (x, y) in bolt:
            return WHITE if (x + y) % 2 else BOLT[4]
        n = fbm(x * 1.3, y * 1.1, 17201, 64, 64, 2, 5.0)
        v = 4.2 - 2.8 * (y - top) / max(1.0, bottom - top) + 0.8 * n
        return tone(CLOUD, v)
    wing_t = _tex(wing)
    silver_t = _tex(lambda x, y: tone(SILVER, 4.2 - 2.0 * y / 15))
    for name, t in (("face", face_t), ("skin", skin_t), ("coat", coat_t), ("chest", chest_t), ("wing", wing_t), ("silver", silver_t)):
        save(t, "block", f"raphael_trophy_{name}")

    F, S, C, CH, W, AG = "face", "skin", "coat", "chest", "wing", "silver"
    full = [0, 0, 16, 16]
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": AG}),
        # The wings first (behind the shoulders), flat cut-out planes flared back.
        BM.cube("wing_r", (0.0, 3.4, 10.4), (7.2, 17.2, 10.4), {"north": W, "south": W}, uv={"north": [16, 0, 0, 16], "south": full},
                rot=("y", 22.5, (7.2, 8, 10.4))),
        BM.cube("wing_l", (8.8, 3.4, 10.4), (16.0, 17.2, 10.4), {"north": W, "south": W}, uv={"north": full, "south": [16, 0, 0, 16]},
                rot=("y", -22.5, (8.8, 8, 10.4))),
        # Chest and shoulders in the overcoat, the collar standing at the back.
        BM.cube("chest", (4.6, 3.4, 6.0), (11.4, 7.6, 10.0), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (3.6, 6.0, 6.2), (12.4, 7.8, 9.8), {"*": C}),
        BM.cube("collar", (5.6, 7.6, 8.2), (10.4, 8.6, 9.6), {"*": C}),
        BM.cube("neck", (6.9, 7.6, 6.9), (9.1, 8.6, 9.1), {"*": S}),
        # The head, shaved: the face on its north side.
        BM.cube("head", (5.5, 8.4, 5.5), (10.5, 13.6, 10.5), {"north": F, "*": S}, uv={"north": full}),
        BM.cube("crown", (5.9, 13.6, 5.9), (10.1, 13.9, 10.1), {"*": S}),
        BM.cube("ear_r", (5.2, 10.2, 7.6), (5.5, 11.6, 8.6), {"*": S}),
        BM.cube("ear_l", (10.5, 10.2, 7.6), (10.8, 11.6, 8.6), {"*": S}),
        BM.cube("nose", (7.6, 9.9, 5.2), (8.4, 11.0, 5.5), {"*": S}),
    ]
    m = BM.model({"base": "trophy_base", F: "raphael_trophy_face", S: "raphael_trophy_skin", C: "raphael_trophy_coat",
                  CH: "raphael_trophy_chest", W: "raphael_trophy_wing", AG: "raphael_trophy_silver"}, e, "trophy_base")
    BM.write("raphael_trophy", m)


# =====================================================================================================
# The holy oil slick (unlit)
# =====================================================================================================

OIL = Ramp("#0d0904", "#1a1207", "#2a1d0b", "#3d2a10", "#573c17", "#7a5520")


def holy_oil_slick():
    """Dark, glossy holy oil soaked into the boards: amber-black, a slick sheen of gold and oil-rainbow where the light
    catches it, ragged at the edges so neighbouring blocks run together into one ring."""
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 17301, 16, 16, 3, 4.0)
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0 and n < 0.42:
                continue
            v = 0.8 + 2.0 * n
            c = tone(OIL, v)
            # A wet specular streak across the pool.
            if abs((x - y * 0.6) % 11 - 5.5) < 0.6 and n > 0.45:
                c = mix(c, hexc("#c9a75a"), 0.45)
            sheen = fbm(x * 1.5 + 9, y * 0.8, 17302, 16, 16, 2, 3.0)
            if sheen > 0.66:
                # The sheen: gold, shading through green and violet like oil on water.
                hue = (x + y * 0.7 + sheen * 10) % 3
                tint = (hexc("#d6b04a"), hexc("#6a9a5a"), hexc("#8a6ab0"))[int(hue)]
                c = mix(c, tint, min(0.55, (sheen - 0.66) * 2.2))
            if (x * 7 + y * 13) % 37 == 0:
                c = mix(c, hexc("#f2d27a"), 0.5)     # a glint
            t.set(x, y, (c[0], c[1], c[2], 235))
    return t


def generate():
    m = staff_rig()
    t, g = m.build(gutter=1, seed=17000)
    m.rig.write(GEO + "raphaels_stormcaller.geo.json")
    save(t, "item", "raphaels_stormcaller")
    save(g, "item", "raphaels_stormcaller_glowmask")
    staff_anims().write(ANIM + "raphaels_stormcaller.animation.json")
    save(staff_icon(), "item", "raphaels_stormcaller_icon")
    trophy()
    save(holy_oil_slick(), "block", "holy_oil_slick")


if __name__ == "__main__":
    generate()
