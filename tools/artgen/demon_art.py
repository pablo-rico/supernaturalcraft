"""Lesser demons: the black-eyed demon (two vessels) and the demon occultist.

Writes their GeckoLib geometry, animations and skins.
"""

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import box
from humanoid import humanoid
from paint import cloth, noise, paint_cube, set_face_px, stamp_face, vertical_split
from pixelkit import Ramp, Tex, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

PALE_SKIN = Ramp("#4a2c26", "#7d5448", "#a97a69", "#c99b86", "#ddb8a3", "#efd3c1")
TAN_SKIN = Ramp("#3a2118", "#64402f", "#8c5f47", "#ac7c5f", "#c5987a", "#dbb497")
SHIRT = Ramp("#7d7d82", "#a3a3a8", "#c4c4c8", "#dcdce0", "#ececef", "#ffffff")
TIE = Ramp("#1a0406", "#33080d", "#4d0d14", "#661520", "#801e2b", "#9a2a37")
HAIR_DARK = Ramp("#0b0807", "#16100d", "#211813", "#2d2119", "#3a2b20", "#4a372a")
HAIR_BROWN = Ramp("#1a0f08", "#2e1c10", "#432a18", "#583822", "#6e482d", "#85593a")
JACKET_LEATHER = Ramp("#170d08", "#2a170d", "#3e2314", "#53301c", "#6a3e25", "#82502f")
TEE = Ramp("#24262a", "#35383d", "#474b51", "#5a5e65", "#6e737a", "#848990")
SHOE = Ramp("#050505", "#0d0d0e", "#151517", "#1e1e21", "#29292d", "#35353a")
TRIM = P.GOLD

K = P.VOID
GLINT = hexc("#3b3b44")


# --- geometry -----------------------------------------------------------------------------

def demon_rig():
    r, parts = humanoid("black_eyed_demon")
    parts["hair"] = box(r.get("head"), (-4, 29, -4), (8, 3, 8), inflate=0.35, tag="hair")
    r.pack()
    return r, parts


def occultist_rig():
    r, parts = humanoid("demon_occultist")
    parts["hood"] = box(r.get("head"), (-4, 24, -4), (8, 8, 8), inflate=0.6, tag="hood")
    parts["robe"] = box(r.get("body"), (-4, 3, -2), (8, 9, 4), inflate=0.45, tag="robe")
    dagger = r.bone("dagger", (-6, 11, 0), "right_arm")
    parts["dagger_handle"] = box(dagger, (-6.5, 11, -3), (1, 1, 3), tag="dagger_handle")
    parts["dagger_blade"] = box(dagger, (-6.5, 11, -9), (1, 1, 6), tag="dagger_blade")
    parts["dagger_guard"] = box(dagger, (-7, 10.5, -3.5), (2, 2, 1), tag="dagger_guard")
    r.pack()
    return r, parts


# --- skins --------------------------------------------------------------------------------

def head_material(skin, hair, back_rows=6, side_rows=3, seed=1):
    skin_m, hair_m = noise(skin, seed, 3.0, 0.5), noise(hair, seed + 1, 2.6, 1.2, 7.0)

    def m(face, x, y, w, h):
        if face == "up":
            return hair_m(face, x, y, w, h)
        if face == "south" and y < back_rows:
            return hair_m(face, x, y, w, h)
        if face in ("east", "west"):
            # Sideburns: hair reaches lower toward the back of the head.
            back = x >= w - 3 if face == "west" else x < 3
            if y < side_rows + (2 if back else 0):
                return hair_m(face, x, y, w, h)
        if face == "north" and y < 1:
            return hair_m(face, x, y, w, h)
        return skin_m(face, x, y, w, h)
    return m


def black_eyed_face(t, head, skin, hair, mouth="grin", stubble=False):
    pal = {"h": hair[2], "H": hair[4], "b": hair[1], "k": K, "g": GLINT, "s": skin[3], "S": skin[4],
           "n": skin[2], "m": hexc("#3a1416"), "M": hexc("#5e2326"), "t": hexc("#d8cfc0"), "u": skin[2]}
    mouths = {
        "grin": "smttttms",
        "flat": "ssmmmmss",
        "sneer": "sssmmmMs",
    }
    rows = [
        "hHhhhhHh",
        "hshhsshh",
        "sbbssbbs",
        "skgsskgs",
        "skkSSkks",
        "sssnnsss",
        mouths[mouth],
        "ssussuss" if stubble else "ssssssss",
    ]
    stamp_face(t, head, "north", rows, pal)


def suit_body(t, body):
    jacket = cloth(P.COAT, 7, 2.3)
    paint_cube(t, body, jacket)
    pal = {"w": SHIRT[4], "W": SHIRT[5], "s": SHIRT[2], "t": TIE[3], "T": TIE[4], "l": P.COAT[4], "b": P.COAT[0],
           "o": P.GOLD[2]}
    stamp_face(t, body, "north", [
        "lwwttwwl",
        ".lwTtwl.",
        ".lwttwl.",
        "..ltTl..",
        "..lttl..",
        "...tt...",
        "...bo...",
        "........",
        "....o...",
        "........",
        "........",
        "bbbbbbbb",
    ], pal)


def suit_arm(t, arm, skin):
    sleeve = cloth(P.COAT, 9, 2.3)
    hand = noise(skin, 3, 3.0, 0.5)
    cuff = lambda f, x, y, w, h: SHIRT[4]

    def m(face, x, y, w, h):
        if face == "down":
            return hand(face, x, y, w, h)
        if face == "up":
            return sleeve(face, x, y, w, h)
        if y >= h - 3:
            return hand(face, x, y, w, h)
        if y == h - 4:
            return cuff(face, x, y, w, h)
        return sleeve(face, x, y, w, h)
    paint_cube(t, arm, m)


def trouser_leg(t, leg, cloth_ramp, shoe_ramp, seed, shoe_rows=2):
    paint_cube(t, leg, vertical_split(cloth(cloth_ramp, seed, 2.2), noise(shoe_ramp, seed + 1, 2.5, 0.8), shoe_rows))


def paint_suit_demon():
    r, p = demon_rig()
    t = Tex(r.tex_w, r.tex_h, 21)
    skin, hair = PALE_SKIN, HAIR_DARK
    paint_cube(t, p["head"], head_material(skin, hair))
    black_eyed_face(t, p["head"], skin, hair, "grin")
    paint_cube(t, p["hair"], hair_cap(hair, 4))
    suit_body(t, p["body"])
    suit_arm(t, p["right_arm"], skin)
    suit_arm(t, p["left_arm"], skin)
    trouser_leg(t, p["right_leg"], P.COAT, SHOE, 40)
    trouser_leg(t, p["left_leg"], P.COAT, SHOE, 41)
    return r, t


def hair_cap(hair, seed, fringe=True):
    """The slightly inflated hair shell: solid on top, ragged at the bottom edge."""
    base = noise(hair, seed, 2.6, 1.4, 8.0)

    def m(face, x, y, w, h):
        if face == "down":
            return None
        if face == "north":
            # Fringe: only the top row, plus a few strands.
            if y == 0 or (fringe and y == 1 and x in (1, 2, 5)):
                return base(face, x, y, w, h)
            return None
        if face in ("east", "west", "south") and y == h - 1 and (x * 7) % 3 == 0:
            return None
        return base(face, x, y, w, h)
    return m


def paint_drifter_demon():
    r, p = demon_rig()
    t = Tex(r.tex_w, r.tex_h, 22)
    skin, hair = TAN_SKIN, HAIR_BROWN
    paint_cube(t, p["head"], head_material(skin, hair, 5, 2, 3))
    black_eyed_face(t, p["head"], skin, hair, "sneer", stubble=True)
    paint_cube(t, p["hair"], hair_cap(hair, 5))
    # Open leather jacket over a grey tee.
    paint_cube(t, p["body"], cloth(JACKET_LEATHER, 12, 2.6, 0.2))
    pal = {"e": TEE[3], "E": TEE[4], "z": P.STEEL[4], "j": JACKET_LEATHER[1], "c": JACKET_LEATHER[4]}
    stamp_face(t, p["body"], "north", [
        "cjeeeejc",
        "cjeEeejc",
        ".jeeeej.",
        ".jeeeej.",
        ".jeEeej.",
        ".jeeeej.",
        ".jeeeej.",
        "zjeeeejz",
        ".jeeeej.",
        ".jeeeej.",
        ".jeeeej.",
        "jjeeeejj",
    ], pal)
    for side in ("right_arm", "left_arm"):
        sleeve = cloth(JACKET_LEATHER, 13, 2.6, 0.2)
        hand = noise(skin, 6, 3.0, 0.5)
        paint_cube(t, p[side], lambda f, x, y, w, h, s=sleeve, hd=hand:
                   hd(f, x, y, w, h) if (f == "down" or (f not in ("up",) and y >= h - 3)) else s(f, x, y, w, h))
    trouser_leg(t, p["right_leg"], P.DENIM, P.LEATHER, 42, 3)
    trouser_leg(t, p["left_leg"], P.DENIM, P.LEATHER, 43, 3)
    return r, t


def paint_occultist():
    r, p = occultist_rig()
    t = Tex(r.tex_w, r.tex_h, 23)
    skin, hair = PALE_SKIN, HAIR_DARK
    paint_cube(t, p["head"], head_material(skin, hair, 6, 3, 7))
    black_eyed_face(t, p["head"], skin, hair, "flat")
    # Ritual scar across the cheek.
    set_face_px(t, p["head"], "north", 6, 5, hexc("#6e2a2a"))
    set_face_px(t, p["head"], "north", 5, 6, hexc("#6e2a2a"))

    robe = cloth(P.ROBE, 30, 2.5, 0.4)
    trim = P.GOLD

    def hood(face, x, y, w, h):
        if face == "down":
            return None
        if face == "north" and 1 < x < w - 2 and y > 1:
            return None  # face opening
        if face == "north" and (x in (1, w - 2) or y == 1) and not (y <= 1 and (x < 1 or x > w - 2)):
            return trim[2]
        return robe(face, x, y, w, h)
    paint_cube(t, p["hood"], hood)

    def robed_body(face, x, y, w, h):
        if face == "north" and x in (3, 4):
            return trim[3] if y % 3 else trim[1]
        return robe(face, x, y, w, h)
    paint_cube(t, p["body"], robed_body)

    def skirt(face, x, y, w, h):
        if face == "up":
            return None
        if face in ("north", "south", "east", "west") and y == h - 1:
            return trim[2]
        if face == "north" and x in (3, 4):
            return trim[3] if y % 3 else trim[1]
        return robe(face, x, y, w, h)
    paint_cube(t, p["robe"], skirt)
    # Embroidered sigil on the robe back.
    stamp_face(t, p["body"], "south", ["...tt...", "..t..t..", ".t.tt.t.", "..t..t..", "...tt..."],
               {"t": trim[3]}, 0, 3)
    for side in ("right_arm", "left_arm"):
        sleeve = cloth(P.ROBE, 31, 2.5, 0.4)
        hand = noise(skin, 8, 3.0, 0.5)
        paint_cube(t, p[side], lambda f, x, y, w, h, s=sleeve, hd=hand:
                   hd(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 2)) else (trim[2] if (f != "up" and y == h - 3) else s(f, x, y, w, h)))
    for side in ("right_leg", "left_leg"):
        trouser_leg(t, p[side], P.ROBE, SHOE, 50, 2)
    paint_cube(t, p["dagger_handle"], noise(P.WOOD, 3, 2.0, 0.6))
    paint_cube(t, p["dagger_guard"], noise(P.GOLD, 3, 3.0, 0.8))
    paint_cube(t, p["dagger_blade"], lambda f, x, y, w, h: P.STEEL[4] if f in ("up", "east") else P.STEEL[3])
    return r, t


# --- animations ---------------------------------------------------------------------------

def humanoid_anims(prefix):
    f = AnimFile()
    idle = f.new(f"animation.{prefix}.idle", 3.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (1.5, [0, -0.3, 0], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    idle.rot("head", (0, [0, 0, 0]), (1.0, [4, 6, -4], "easeInOutSine"), (2.2, [-2, -4, 3], "easeInOutSine"),
             (3.0, [0, 0, 0], "easeInOutSine"))
    idle.rot("right_arm", (0, [0, 0, 2]), (1.5, [2, 0, 4], "easeInOutSine"), (3.0, [0, 0, 2], "easeInOutSine"))
    idle.rot("left_arm", (0, [0, 0, -2]), (1.5, [2, 0, -4], "easeInOutSine"), (3.0, [0, 0, -2], "easeInOutSine"))

    walk = f.new(f"animation.{prefix}.walk", 1.0, loop=True)
    for bone, sign in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [28 * sign, 0, 0]), (0.5, [-28 * sign, 0, 0], "easeInOutSine"), (1.0, [28 * sign, 0, 0], "easeInOutSine"))
    for bone, sign in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [24 * sign, 0, 0]), (0.5, [-24 * sign, 0, 0], "easeInOutSine"), (1.0, [24 * sign, 0, 0], "easeInOutSine"))
    walk.pos("body", (0, [0, 0, 0]), (0.25, [0, 0.6, 0], "easeOutSine"), (0.5, [0, 0, 0], "easeInSine"),
             (0.75, [0, 0.6, 0], "easeOutSine"), (1.0, [0, 0, 0], "easeInSine"))
    walk.rot("body", (0, [0, 4, 0]), (0.5, [0, -4, 0], "easeInOutSine"), (1.0, [0, 4, 0], "easeInOutSine"))

    attack = f.new(f"animation.{prefix}.attack", 0.55)
    attack.rot("right_arm", (0, [0, 0, 0]), (0.18, [-125, 10, 10], "easeOutQuad"), (0.32, [15, -10, 0], "easeInQuart"),
               (0.55, [0, 0, 0], "easeInOutSine"))
    attack.rot("body", (0, [0, 0, 0]), (0.18, [0, 18, 0], "easeOutQuad"), (0.32, [6, -16, 0], "easeInQuart"),
               (0.55, [0, 0, 0], "easeInOutSine"))
    attack.rot("left_arm", (0, [0, 0, 0]), (0.18, [-20, 0, -10]), (0.55, [0, 0, 0], "easeInOutSine"))

    smoke = f.new(f"animation.{prefix}.smoke_out", 1.2, hold=True)
    smoke.rot("head", (0, [0, 0, 0]), (0.4, [-55, 0, 0], "easeOutBack"), (1.2, [-60, 0, 0]))
    smoke.rot("right_arm", (0, [0, 0, 0]), (0.4, [-10, 0, 40], "easeOutBack"), (1.2, [-10, 0, 45]))
    smoke.rot("left_arm", (0, [0, 0, 0]), (0.4, [-10, 0, -40], "easeOutBack"), (1.2, [-10, 0, -45]))
    smoke.rot("body", (0, [0, 0, 0]), (0.4, [-12, 0, 0], "easeOutBack"), (0.8, [-12, 0, 2]), (0.9, [-12, 0, -2]), (1.2, [-14, 0, 0]))

    trapped = f.new(f"animation.{prefix}.trapped", 0.8, loop=True)
    trapped.rot("right_arm", (0, [-20, 0, 25]), (0.4, [-30, 0, 35], "easeInOutSine"), (0.8, [-20, 0, 25], "easeInOutSine"))
    trapped.rot("left_arm", (0, [-20, 0, -25]), (0.4, [-30, 0, -35], "easeInOutSine"), (0.8, [-20, 0, -25], "easeInOutSine"))
    trapped.rot("body", (0, [0, -6, 0]), (0.2, [0, 6, 0]), (0.4, [0, -6, 0]), (0.6, [0, 6, 0]), (0.8, [0, -6, 0]))
    trapped.rot("head", (0, [8, 0, 0]), (0.4, [14, 0, 0], "easeInOutSine"), (0.8, [8, 0, 0], "easeInOutSine"))
    return f


def occultist_anims():
    f = humanoid_anims("demon_occultist")
    cast = f.new("animation.demon_occultist.cast", 1.0)
    cast.rot("right_arm", (0, [0, 0, 0]), (0.3, [-150, 0, 20], "easeOutBack"), (0.7, [-155, 0, 25]), (1.0, [0, 0, 0], "easeInOutSine"))
    cast.rot("left_arm", (0, [0, 0, 0]), (0.3, [-150, 0, -20], "easeOutBack"), (0.7, [-155, 0, -25]), (1.0, [0, 0, 0], "easeInOutSine"))
    cast.rot("head", (0, [0, 0, 0]), (0.3, [-25, 0, 0], "easeOutBack"), (0.7, [-28, 0, 0]), (1.0, [0, 0, 0], "easeInOutSine"))
    cast.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.5, 0]), (1.0, [0, 0, 0]))
    return f


def generate():
    for name, painter in (("black_eyed_demon", paint_suit_demon), ("black_eyed_demon_drifter", paint_drifter_demon)):
        rig, tex = painter()
        save(tex, "entity", name)
    rig, _ = demon_rig()
    rig.write(GEO + "black_eyed_demon.geo.json")
    humanoid_anims("black_eyed_demon").write(ANIM + "black_eyed_demon.animation.json")

    rig, tex = paint_occultist()
    save(tex, "entity", "demon_occultist")
    rig.write(GEO + "demon_occultist.geo.json")
    occultist_anims().write(ANIM + "demon_occultist.animation.json")
