"""The hellhound: a huge, gaunt black dog, ribs showing, embers for eyes and fire in its jaws.

Writes its geometry, its skin (+ glowmask), the "haze" it is drawn with while unseen, and its
animations. Model faces north; +X is its LEFT (see geomodel conventions).
"""

import math
import random

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import noise, paint_cube, set_face_px
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

HIDE = Ramp("#040303", "#0b0808", "#141010", "#1e1717", "#2a2020", "#382a29")
RIB = Ramp("#2a2420", "#3e352f", "#554a42", "#6c5f55", "#85766a", "#a09080")
CLAW = Ramp("#0a0806", "#1a1510", "#2c241c", "#40352a", "#5a4b3c", "#76644f")
EYE = hexc("#ff3b12")
EMBER = [hexc("#ff4a0a"), hexc("#ff8a1e"), hexc("#ffc04a")]


def rig():
    r = Rig("hellhound", 128, 64)
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, 13, 0), "root")
    p = {
        "chest": box(body, (-4.5, 8.5, -9), (9, 9, 6), tag="chest"),
        "torso": box(body, (-3.5, 10, -3), (7, 7, 11), tag="torso"),
    }
    neck = r.bone("neck", (0, 15.5, -8.5), "body", rotation=(12, 0, 0))
    p["neck"] = box(neck, (-2.5, 13.5, -13), (5, 5, 6), tag="neck")
    head = r.bone("head", (0, 18, -12), "neck")
    p["skull"] = box(head, (-3.5, 15, -18), (7, 6, 7), tag="skull")
    p["snout"] = box(head, (-2, 15, -22.5), (4, 3, 5), tag="snout")
    p["ear_r"] = box(head, (-3.5, 21, -14), (2, 3, 1), tag="ear")
    p["ear_l"] = box(head, (1.5, 21, -14), (2, 3, 1), tag="ear")
    jaw = r.bone("jaw", (0, 15, -17), "head")
    p["jaw"] = box(jaw, (-2, 13.5, -22), (4, 1.5, 5), tag="jaw")
    for name, x, z in (("leg_front_right", -3, -6), ("leg_front_left", 3, -6), ("leg_hind_right", -2.5, 6), ("leg_hind_left", 2.5, 6)):
        leg = r.bone(name, (x, 12, z), "root")
        hind = "hind" in name
        p[name] = box(leg, (x - 1.5, 0, z - 1.5), (3, 12, 3), tag="leg")
        if hind:
            p[name + "_thigh"] = box(leg, (x - 1.75, 6, z - 2), (3.5, 6, 4.5), tag="thigh")
        else:
            p[name + "_shoulder"] = box(leg, (x - 1.75, 7, z - 2), (3.5, 5, 4), tag="thigh")
    tail = r.bone("tail", (0, 16, 8), "body", rotation=(-38, 0, 0))
    p["tail"] = box(tail, (-1, 15, 8), (2, 2, 9), tag="tail")
    r.pack()
    return r, p


def paint():
    r, p = rig()
    t = Tex(r.tex_w, r.tex_h, 71)
    g = Tex(r.tex_w, r.tex_h, 72)
    hide = noise(HIDE, 73, 2.4, 1.3, 6.0)
    for key, cube in p.items():
        paint_cube(t, cube, hide)

    def glow(cube, face, x, y, c):
        set_face_px(t, cube, face, x, y, c)
        set_face_px(g, cube, face, x, y, c)

    # Ribs down both flanks of the chest and torso.
    for key in ("chest", "torso"):
        for face in ("east", "west"):
            u, v, w, h = p[key].rects[face]
            for x in range(1, w - 1, 2):
                for y in range(2, h - 2):
                    t.set(u + x, v + y, RIB[1 + (y % 2)])
    # A ridge of bone down the spine.
    for key in ("chest", "torso", "neck"):
        u, v, w, h = p[key].rects["up"]
        for y in range(h):
            t.set(u + w // 2, v + y, RIB[3] if y % 2 else RIB[1])
    # Embers in the cracks of its hide.
    rng = random.Random(74)
    for key in ("chest", "torso", "neck", "skull"):
        for face, (u, v, w, h) in p[key].rects.items():
            for _ in range(max(1, (w * h) // 22)):
                glow(p[key], face, rng.randrange(w), rng.randrange(h), EMBER[rng.randrange(3)])
    # Eyes, and fire dripping from the jaws.
    for x in (1, 5):
        glow(p["skull"], "north", x, 2, EYE)
        glow(p["skull"], "north", x + 1, 2, hexc("#b3121a"))
    for face in ("north", "east", "west"):
        u, v, w, h = p["jaw"].rects[face]
        for x in range(w):
            if x % 2 == 0:
                glow(p["jaw"], face, x, 0, EMBER[2])
        u, v, w, h = p["snout"].rects[face]
        for x in range(w):
            if x % 2 == 1:
                glow(p["snout"], face, x, h - 1, hexc("#e8e0d0"))  # teeth
    for key in ("leg_front_right", "leg_front_left", "leg_hind_right", "leg_hind_left"):
        for face, (u, v, w, h) in p[key].rects.items():
            if face in ("up",):
                continue
            for x in range(w):
                t.set(u + x, v + h - 1, CLAW[3] if x % 2 else CLAW[1])
    return r, t, g


def haze():
    """What an unseen hound is drawn with: bent, shimmering air (alpha is scaled again in the renderer)."""
    t = Tex(128, 64, 81)
    for y in range(64):
        for x in range(128):
            n = fbm(x, y, 81, 128, 64, 3, 8.0)
            wave = 0.5 + 0.5 * math.sin((x + y * 0.6) * 0.45 + n * 6)
            a = int(80 + 170 * wave * n)
            c = mix(hexc("#ffe8e0"), hexc("#ff9a6a"), n)
            t.set(x, y, (c[0], c[1], c[2], max(0, min(255, a))))
    return t


E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_SNAP = "easeInQuart"


def anims():
    f = AnimFile()
    A = "animation.hellhound."
    idle = f.new(A + "idle", 3.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (1.5, [0, -0.4, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [0, 0, 0]), (0.8, [8, -10, 0], E_IO), (1.6, [6, 12, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    idle.rot("tail", (0, [0, -6, 0]), (1.5, [6, 6, 0], E_IO), (3.0, [0, -6, 0], E_IO))
    idle.rot("jaw", (0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.3, [10, 0, 0], E_OUT), (2.8, [0, 0, 0], E_IO))

    walk = f.new(A + "walk", 1.0, loop=True)
    for bone, phase in (("leg_front_right", 0), ("leg_hind_left", 0), ("leg_front_left", 1), ("leg_hind_right", 1)):
        a, b = (28, -28) if phase == 0 else (-28, 28)
        walk.rot(bone, (0, [a, 0, 0]), (0.5, [b, 0, 0], E_IO), (1.0, [a, 0, 0], E_IO))
    walk.rot("head", (0, [0, 0, 0]), (0.5, [4, 0, 0], E_IO), (1.0, [0, 0, 0], E_IO))
    walk.rot("tail", (0, [0, -8, 0]), (0.5, [0, 8, 0], E_IO), (1.0, [0, -8, 0], E_IO))

    run = f.new(A + "run", 0.6, loop=True)
    for bone, off, amp in (("leg_front_right", 0.0, 50), ("leg_front_left", 0.08, 50), ("leg_hind_right", 0.3, 55), ("leg_hind_left", 0.38, 55)):
        keys = []
        for i in range(4):
            tt = i * 0.15
            ph = ((tt + off) / 0.6) * math.tau
            keys.append((tt, [round(amp * math.sin(ph), 1), 0, 0]))
        keys.append((0.6, keys[0][1]))
        run.rot(bone, *keys)
    run.rot("body", (0, [4, 0, 0]), (0.3, [-4, 0, 0], E_IO), (0.6, [4, 0, 0], E_IO))
    run.pos("body", (0, [0, 0, 0]), (0.15, [0, 1.0, 0], E_OUT), (0.3, [0, 0, 0]), (0.45, [0, 1.0, 0], E_OUT), (0.6, [0, 0, 0]))
    run.rot("head", (0, [10, 0, 0]), (0.3, [2, 0, 0], E_IO), (0.6, [10, 0, 0], E_IO))
    run.rot("tail", (0, [-20, 0, 0]), (0.3, [-10, 0, 0], E_IO), (0.6, [-20, 0, 0], E_IO))

    bite = f.new(A + "bite", 0.5)
    bite.rot("head", (0, [0, 0, 0]), (0.15, [-15, 0, 0], E_OUT), (0.25, [12, 0, 0], E_SNAP), (0.5, [0, 0, 0], E_IO))
    bite.rot("neck", (0, [0, 0, 0]), (0.15, [-10, 0, 0], E_OUT), (0.25, [15, 0, 0], E_SNAP), (0.5, [0, 0, 0], E_IO))
    bite.rot("jaw", (0, [0, 0, 0]), (0.15, [35, 0, 0], E_OUT), (0.25, [0, 0, 0], E_SNAP), (0.5, [0, 0, 0]))
    return f


def generate():
    r, t, g = paint()
    r.write(GEO + "hellhound.geo.json")
    save(t, "entity", "hellhound")
    save(g, "entity", "hellhound_glowmask")
    save(haze(), "entity", "hellhound_haze")
    anims().write(ANIM + "hellhound.animation.json")
